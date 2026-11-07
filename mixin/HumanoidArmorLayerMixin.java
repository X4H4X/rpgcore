package com.rpgcore.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rpgcore.client.CameraSystem;
import com.rpgcore.client.PeasantClothesRenderer;
import com.rpgcore.data.BackpackItem;
import com.rpgcore.data.HoodedCloakItem;
import com.rpgcore.data.PeasantClothesItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> extends RenderLayer<T, M> {

    public HumanoidArmorLayerMixin(RenderLayerParent<T, M> renderer) {
        super(renderer);
    }

    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    private void rpgcore$renderCustomApparelOnArmorStandAndHideHelmetInFirstPerson(
            PoseStack poseStack, MultiBufferSource buffer, T entity,
            EquipmentSlot slot, int packedLight, A pModel, CallbackInfo ci) {

        Minecraft mc = Minecraft.getInstance();
        boolean isFirstPersonWorld = CameraSystem.renderingFirstPersonBody && entity == mc.player;

        // 1. В мире от 1-го лица (даже когда открыт ESC или Инвентарь!) всегда скрываем шлем на голове!
        if (isFirstPersonWorld && slot == EquipmentSlot.HEAD) {
            pModel.head.visible = false;
            pModel.hat.visible = false;
            ci.cancel();
            return;
        }

        // 2. Если на Стойку для брони (ArmorStand), моба или в слот брони игрока надета наша одежда / рюкзак / плащ:
        // отменяем стандартную белую кожаную броню (исправлен баг с image_78b3dd.png!) и рисуем нашу 3D-модель!
        ItemStack stack = entity.getItemBySlot(slot);
        if (!stack.isEmpty() && stack.getItem() instanceof PeasantClothesItem clothes) {
            M parentModel = this.getParentModel();
            if (clothes instanceof BackpackItem bp) {
                PeasantClothesRenderer.renderBackpack(poseStack, buffer, entity, bp.getBackpackTier(), packedLight, parentModel);
            } else if (clothes instanceof HoodedCloakItem) {
                PeasantClothesRenderer.renderCloak(poseStack, buffer, entity, true, isFirstPersonWorld, 0.0F, 0.0F, packedLight, parentModel);
            } else {
                PeasantClothesRenderer.renderPiece(poseStack, buffer, entity, clothes.getUnderArmorSlotIndex(), packedLight, parentModel);
            }
            ci.cancel();
        }
    }

    @Inject(method = "setPartVisibility", at = @At("TAIL"))
    private void rpgcore$forceHideHelmetPartsInFirstPerson(A model, EquipmentSlot slot, CallbackInfo ci) {
        if (CameraSystem.renderingFirstPersonBody) {
            model.head.visible = false;
            model.hat.visible = false;
        }
    }
}