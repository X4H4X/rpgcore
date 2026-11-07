package com.rpgcore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rpgcore.data.ExtraEquipmentData;
import com.rpgcore.data.HoodedCloakItem;
import com.rpgcore.data.ModAttachments;
import com.rpgcore.data.PeasantClothesItem;
import net.minecraft.client.model.ArmorStandArmorModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;

public class ArmorStandApparelLayer extends RenderLayer<ArmorStand, ArmorStandArmorModel> {

    public ArmorStandApparelLayer(RenderLayerParent<ArmorStand, ArmorStandArmorModel> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       ArmorStand stand, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {

        ItemStack head = stand.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chest = stand.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs = stand.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack feet = stand.getItemBySlot(EquipmentSlot.FEET);

        boolean hasCloak = stand.getData(ModAttachments.STAND_CLOAK) || (chest.getItem() instanceof HoodedCloakItem);

        // 1. Шляпа крестьянина на стойке
        if (head.getItem() instanceof PeasantClothesItem pc && pc.getApparelType() == PeasantClothesItem.ApparelType.HAT) {
            PeasantClothesRenderer.renderPiece(poseStack, buffer, stand, ExtraEquipmentData.SLOT_HAT_UNDER, packedLight, this.getParentModel(), false);
        }

        // 2. Рубаха крестьянина на стойке
        if (chest.getItem() instanceof PeasantClothesItem pc && pc.getApparelType() == PeasantClothesItem.ApparelType.SHIRT) {
            PeasantClothesRenderer.renderPiece(poseStack, buffer, stand, ExtraEquipmentData.SLOT_SHIRT_UNDER, packedLight, this.getParentModel(), false);
        }

        // 3. Портки крестьянина на стойке
        if (legs.getItem() instanceof PeasantClothesItem pc && pc.getApparelType() == PeasantClothesItem.ApparelType.PANTS) {
            PeasantClothesRenderer.renderPiece(poseStack, buffer, stand, ExtraEquipmentData.SLOT_PANTS_UNDER, packedLight, this.getParentModel(), false);
        }

        // 4. Башмаки крестьянина на стойке
        if (feet.getItem() instanceof PeasantClothesItem pc && pc.getApparelType() == PeasantClothesItem.ApparelType.SHOES) {
            PeasantClothesRenderer.renderPiece(poseStack, buffer, stand, ExtraEquipmentData.SLOT_SHOES_UNDER, packedLight, this.getParentModel(), false);
        }

        // 5. Плащ поверх любой брони или одежды на стойке!
        if (hasCloak) {
            boolean hoodUp = head.isEmpty();
            PeasantClothesRenderer.renderCloak(poseStack, buffer, stand, hoodUp, false, 0.0F, 0.0F, packedLight, this.getParentModel());
        }
    }
}