package com.rpgcore.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rpgcore.client.CameraSystem;
import com.rpgcore.client.WorldMapManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererFirstPersonMixin {

    @Shadow @Final private Minecraft minecraft;

    @Redirect(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;isDetached()Z",
                    ordinal = 0
            )
    )
    private boolean rpgcore$allowLocalPlayerEntityInFirstPerson(Camera camera) {
        if (camera.isDetached()) {
            return true;
        }
        if (CameraSystem.isFirstPersonBodyEnabled()
                && this.minecraft.player != null
                && !this.minecraft.player.isSpectator()
                && !this.minecraft.player.isSleeping()) {
            return true;
        }
        return false;
    }

    @Inject(method = "renderEntity", at = @At("HEAD"))
    private void rpgcore$onRenderEntityPre(Entity entity, double camX, double camY, double camZ,
                                           float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, CallbackInfo ci) {
        if (CameraSystem.isFirstPersonBodyEnabled()
                && entity == this.minecraft.player
                && !this.minecraft.gameRenderer.getMainCamera().isDetached()) {
            CameraSystem.renderingFirstPersonBody = true;
        }
    }

    @Inject(method = "renderEntity", at = @At("RETURN"))
    private void rpgcore$onRenderEntityPost(Entity entity, double camX, double camY, double camZ,
                                            float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, CallbackInfo ci) {
        if (entity == this.minecraft.player) {
            CameraSystem.renderingFirstPersonBody = false;
        }
    }

    // Как только в мире ставится, ломается или меняется любой блок (например, игрок строит дом) —
    // мгновенно обновляем этот участок на миникарте и пускаем частицы строительства!
    @Inject(method = "blockChanged", at = @At("HEAD"))
    private void rpgcore$onWorldBlockChanged(BlockGetter level, BlockPos pos, BlockState oldState, BlockState newState, int flags, CallbackInfo ci) {
        if (!oldState.is(newState.getBlock())) {
            WorldMapManager.onBlockChanged(pos, newState);
        }
    }
}