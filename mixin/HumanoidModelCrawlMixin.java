package com.rpgcore.mixin;

import com.rpgcore.data.ModAttachments;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class HumanoidModelCrawlMixin<T extends LivingEntity> extends HumanoidModel<T> {

    @Shadow @Final public ModelPart leftSleeve;
    @Shadow @Final public ModelPart rightSleeve;
    @Shadow @Final public ModelPart leftPants;
    @Shadow @Final public ModelPart rightPants;
    @Shadow @Final public ModelPart jacket;

    public HumanoidModelCrawlMixin(ModelPart root) {
        super(root);
    }

    // Перед расчётом анимации обнуляем ванильный swimAmount на суше, чтобы игра не включала гребок пловца!
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("HEAD"))
    private void rpgcore$disableVanillaSwimInterpolations(T entity, float limbSwing, float limbSwingAmount,
                                                          float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (entity instanceof Player player && player.getData(ModAttachments.CRAWLING) && !player.isInWater()) {
            this.swimAmount = 0.0F;
        }
    }

    // В самом конце PlayerModel.setupAnim задаём собственную анимацию ползания по-пластунски и копируем её в рукава/штанины
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void rpgcore$applyTrueGroundCrawlAnim(T entity, float limbSwing, float limbSwingAmount,
                                                  float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof Player player)) return;
        if (!player.getData(ModAttachments.CRAWLING) || player.isInWater()) return;

        // Так как в Pose.SWIMMING весь корпус игрока уже повернут горизонтально на -90 градусов (лицом вниз),
        // в локальных координатах модели:
        // xRot = -1.35F поднимает лицо вперёд к горизонту (чтобы голова НЕ смотрела макушкой вперёд и не зарывалась в траву!)
        float moveSpeed = Mth.clamp(limbSwingAmount * 1.5F, 0.0F, 1.0F);
        float cycle = limbSwing * 0.9F;
        float sin = Mth.sin(cycle) * moveSpeed;
        float cos = Mth.cos(cycle) * moveSpeed;

        // 1. Голова поднята вперёд — персонаж смотрит перед собой, а не в землю!
        this.head.xRot = -1.25F + (headPitch * Mth.DEG_TO_RAD) * 0.35F;
        this.head.yRot = (netHeadYaw * Mth.DEG_TO_RAD) * 0.55F;
        this.head.zRot = -sin * 0.08F;
        this.hat.copyFrom(this.head);

        // 2. Корпус слегка извивается влево-вправо при перехвате локтей и коленей
        this.body.xRot = 0.0F;
        this.body.yRot = sin * 0.16F;
        this.body.zRot = cos * 0.08F;
        this.jacket.copyFrom(this.body);

        // 3. Руки вытянуты вперёд перед головой (xRot около -2.6 рад) и поочерёдно подтягивают тело по земле:
        // когда левая рука тянется вперёд и опирается о землю, правая подтягивается к плечу
        this.leftArm.xRot = -2.65F + sin * 0.65F;
        this.leftArm.yRot = 0.22F + cos * 0.15F;
        this.leftArm.zRot = -0.18F;
        this.leftSleeve.copyFrom(this.leftArm);

        this.rightArm.xRot = -2.65F - sin * 0.65F;
        this.rightArm.yRot = -0.22F + cos * 0.15F;
        this.rightArm.zRot = 0.18F;
        this.rightSleeve.copyFrom(this.rightArm);

        // 4. Ноги разведены чуть в стороны и поочерёдно отталкиваются коленями по земле (в противофазе с руками)
        this.leftLeg.xRot = -sin * 0.50F;
        this.leftLeg.yRot = -0.14F;
        this.leftLeg.zRot = -0.10F - Math.max(0.0F, -sin) * 0.12F;
        this.leftPants.copyFrom(this.leftLeg);

        this.rightLeg.xRot = sin * 0.50F;
        this.rightLeg.yRot = 0.14F;
        this.rightLeg.zRot = 0.10F + Math.max(0.0F, sin) * 0.12F;
        this.rightPants.copyFrom(this.rightLeg);
    }
}