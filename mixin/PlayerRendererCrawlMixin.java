package com.rpgcore.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rpgcore.client.CameraSystem;
import com.rpgcore.data.ModAttachments;
import com.rpgcore.data.PlayerAppearanceData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererCrawlMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public PlayerRendererCrawlMixin(EntityRendererProvider.Context context, PlayerModel<AbstractClientPlayer> model, float shadowRadius) {
        super(context, model, shadowRadius);
    }

    @Unique
    private static boolean rpgcore$isFirstPersonWorldRender(AbstractClientPlayer player) {
        return CameraSystem.renderingFirstPersonBody && player == Minecraft.getInstance().player;
    }

    @Inject(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V", at = @At("HEAD"), cancellable = true)
    private void rpgcore$setupHorizontalGroundCrawlRotation(AbstractClientPlayer player, PoseStack poseStack,
                                                            float bob, float yBodyRot, float partialTick, float scale, CallbackInfo ci) {
        if (player.getData(ModAttachments.CRAWLING) && !player.isInWater()) {
            PlayerAppearanceData data = player.getData(ModAttachments.APPEARANCE);
            float h = data.race().getBaseHeight() * data.gender().getHeightScale();
            float w = data.race().getBaseWidth() * data.gender().getWidthScale();

            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yBodyRot));
            poseStack.translate(0.0F, 0.16F * w, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            poseStack.scale(w, h, w);
            poseStack.translate(0.0F, -1.22F, 0.0F);
            ci.cancel();
        }
    }

    @Inject(
            method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD")
    )
    private void rpgcore$prepareFirstPersonBodyRender(AbstractClientPlayer player, float entityYaw, float partialTicks,
                                                      PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (!rpgcore$isFirstPersonWorldRender(player)) return;

        poseStack.pushPose();

        PlayerAppearanceData data = player.getData(ModAttachments.APPEARANCE);
        float scaleW = data.race().getBaseWidth() * data.gender().getWidthScale();
        float scaleH = data.race().getBaseHeight() * data.gender().getHeightScale();
        boolean crawlingOnLand = player.getData(ModAttachments.CRAWLING) && !player.isInWater();
        float swimAmt = player.getSwimAmount(partialTicks);
        boolean swimmingInWater = (swimAmt > 0.01F || player.isVisuallySwimming() || player.isInWater()) && !crawlingOnLand;

        float viewYaw = Mth.rotLerp(partialTicks, player.yRotO, player.getYRot());
        player.yBodyRot = viewYaw;
        player.yBodyRotO = viewYaw;

        float bodyYawRad = viewYaw * Mth.DEG_TO_RAD;

        if (swimmingInWater && swimAmt > 0.01F) {
            double backSwim = (0.32D + 0.62D * swimAmt) * scaleW;
            double downSwim = -0.58D * swimAmt * scaleH;
            double offsetX = Math.sin(bodyYawRad) * backSwim;
            double offsetZ = -Math.cos(bodyYawRad) * backSwim;
            poseStack.translate(offsetX, downSwim, offsetZ);
        } else {
            double backDist = (crawlingOnLand ? 0.34D : 0.28D) * scaleW;
            double offsetX = Math.sin(bodyYawRad) * backDist;
            double offsetZ = -Math.cos(bodyYawRad) * backDist;

            // ИСПРАВЛЕНИЕ ОТРЫВА ТЕЛА ОРКА ОТ КАМЕРЫ (image_97d569.png):
            // Синхронизируем высоту плеч 3D-модели (1.62 * scaleH) с реальной высотой глаз камеры (player.getEyeHeight()),
            // чтобы при взгляде вниз плечи всегда находились ровно под камерой!
            double offsetY = 0.0D;
            if (!crawlingOnLand) {
                float modelEyeY = (player.isCrouching() ? 1.27F : 1.62F) * scaleH;
                offsetY = player.getEyeHeight() - modelEyeY;
            }

            poseStack.translate(offsetX, offsetY, offsetZ);
        }
    }

    @Inject(method = "setModelProperties", at = @At("TAIL"))
    private void rpgcore$hideOnlyHeadKeepModelArmsInFirstPerson(AbstractClientPlayer player, CallbackInfo ci) {
        if (!rpgcore$isFirstPersonWorldRender(player)) return;

        PlayerModel<AbstractClientPlayer> model = this.getModel();
        model.head.visible = false;
        model.hat.visible = false;

        model.leftArm.visible = true;
        model.leftSleeve.visible = true;
        model.rightArm.visible = true;
        model.rightSleeve.visible = true;
    }

    @Inject(
            method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN")
    )
    private void rpgcore$finishFirstPersonBodyRender(AbstractClientPlayer player, float entityYaw, float partialTicks,
                                                     PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (!rpgcore$isFirstPersonWorldRender(player)) return;

        PlayerModel<AbstractClientPlayer> model = this.getModel();
        model.head.visible = true;
        model.hat.visible = true;
        poseStack.popPose();
    }
}