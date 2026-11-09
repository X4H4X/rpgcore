package com.rpgcore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.rpgcore.data.BackpackItem;
import com.rpgcore.data.ExtraEquipmentData;
import com.rpgcore.data.Gender;
import com.rpgcore.data.HoodedCloakItem;
import com.rpgcore.data.PeasantClothesItem;
import com.rpgcore.data.PlayerAppearanceData;
import com.rpgcore.data.PlayerCorpseEntity;
import com.rpgcore.data.RaceType;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

import java.util.List;

public class PlayerCorpseRenderer extends EntityRenderer<PlayerCorpseEntity> {

    private final PlayerModel<?> wideModel;
    private final PlayerModel<?> slimModel;
    private final HumanoidArmorModel<?> innerArmorModel;
    private final HumanoidArmorModel<?> outerArmorModel;

    // 3D-элементы внешности из RaceExtraLayer, чтобы труп на 100% соответствовал кастомному персонажу!
    private final RaceExtraLayer dummyRaceLayer;

    public PlayerCorpseRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.wideModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
        this.slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        this.innerArmorModel = new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR));
        this.outerArmorModel = new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
        this.dummyRaceLayer = new RaceExtraLayer(null);
    }

    @Override
    public void render(PlayerCorpseEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        PlayerAppearanceData data = entity.getAppearanceData();
        boolean isFemale = (data.gender() == Gender.FEMALE);
        PlayerModel<?> model = isFemale ? this.slimModel : this.wideModel;

        float h = data.race().getBaseHeight() * data.gender().getHeightScale();
        float w = data.race().getBaseWidth() * data.gender().getWidthScale();

        poseStack.pushPose();
        // Укладываем персонажа НА СПИНУ ЛИЦОМ ВВЕРХ (исправлен image_3e1303.jpg!)
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getYRot()));
        poseStack.translate(0.0F, 0.15F * w, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.scale(-w, -h, w);
        poseStack.translate(0.0F, -1.35F, 0.0F);

        model.setAllVisible(true);
        model.young = false;
        // Естественная поза погибшего путника
        model.head.setPos(0.0F, 0.0F, 0.0F);
        model.head.xRot = -0.08F;
        model.head.yRot = 0.22F;
        model.head.zRot = 0.0F;
        model.hat.copyFrom(model.head);

        model.body.setPos(0.0F, 0.0F, 0.0F);
        model.body.xRot = 0.0F;
        model.body.yRot = 0.0F;
        model.body.zRot = 0.0F;
        model.jacket.copyFrom(model.body);

        model.leftArm.setPos(5.0F, isFemale ? 2.5F : 2.0F, 0.0F);
        model.leftArm.xRot = -0.06F;
        model.leftArm.yRot = 0.0F;
        model.leftArm.zRot = -0.24F;
        model.leftSleeve.copyFrom(model.leftArm);

        model.rightArm.setPos(-5.0F, isFemale ? 2.5F : 2.0F, 0.0F);
        model.rightArm.xRot = 0.08F;
        model.rightArm.yRot = 0.0F;
        model.rightArm.zRot = 0.28F;
        model.rightSleeve.copyFrom(model.rightArm);

        model.leftLeg.setPos(1.9F, 12.0F, 0.0F);
        model.leftLeg.xRot = 0.04F;
        model.leftLeg.yRot = 0.0F;
        model.leftLeg.zRot = 0.09F;
        model.leftPants.copyFrom(model.leftLeg);

        model.rightLeg.setPos(-1.9F, 12.0F, 0.0F);
        model.rightLeg.xRot = -0.04F;
        model.rightLeg.yRot = 0.0F;
        model.rightLeg.zRot = -0.11F;
        model.rightPants.copyFrom(model.rightLeg);

        // 1. Базовое тело со скином расы, лица и глаз
        ResourceLocation skin = CustomSkinManager.getOrCreateSkin(data);
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(skin));
        model.renderToBuffer(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);

        // 2. Рендерим все кастомные 3D-детали внешности (причёску, женский бюст, уши, рога, хвост, бороду) и одежду/плащ/рюкзак!
        ItemStack headArmor = entity.getCorpseArmor(EquipmentSlot.HEAD);
        ItemStack chestArmor = entity.getCorpseArmor(EquipmentSlot.CHEST);
        ItemStack legsArmor = entity.getCorpseArmor(EquipmentSlot.LEGS);
        ItemStack feetArmor = entity.getCorpseArmor(EquipmentSlot.FEET);

        ItemStack underHat = entity.getCorpseExtraSlot(ExtraEquipmentData.SLOT_HAT_UNDER);
        ItemStack underShirt = entity.getCorpseExtraSlot(ExtraEquipmentData.SLOT_SHIRT_UNDER);
        ItemStack underPants = entity.getCorpseExtraSlot(ExtraEquipmentData.SLOT_PANTS_UNDER);
        ItemStack underShoes = entity.getCorpseExtraSlot(ExtraEquipmentData.SLOT_SHOES_UNDER);
        ItemStack capeStack = entity.getCorpseExtraSlot(ExtraEquipmentData.SLOT_CAPE);
        ItemStack backpackStack = entity.getCorpseExtraSlot(ExtraEquipmentData.SLOT_BACKPACK);

        this.dummyRaceLayer.renderCorpseAppearanceAndApparel(
                poseStack, buffer, packedLight, model, data,
                headArmor, chestArmor, legsArmor, feetArmor,
                underHat, underShirt, underPants, underShoes, capeStack, backpackStack
        );

        // 3. Рендерим надетую на труп ванильную/модовую броню
        renderCorpseArmorPiece(poseStack, buffer, packedLight, model, headArmor, EquipmentSlot.HEAD);
        renderCorpseArmorPiece(poseStack, buffer, packedLight, model, chestArmor, EquipmentSlot.CHEST);
        renderCorpseArmorPiece(poseStack, buffer, packedLight, model, legsArmor, EquipmentSlot.LEGS);
        renderCorpseArmorPiece(poseStack, buffer, packedLight, model, feetArmor, EquipmentSlot.FEET);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void renderCorpseArmorPiece(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                        PlayerModel<?> parentModel, ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem armorItem)) return;
        if (armorItem instanceof PeasantClothesItem) return;

        boolean inner = (slot == EquipmentSlot.LEGS);
        HumanoidArmorModel armorModel = inner ? this.innerArmorModel : this.outerArmorModel;
        ((HumanoidModel) parentModel).copyPropertiesTo(armorModel);

        armorModel.setAllVisible(false);
        switch (slot) {
            case HEAD -> {
                armorModel.head.visible = true;
                armorModel.hat.visible = true;
            }
            case CHEST -> {
                armorModel.body.visible = true;
                armorModel.rightArm.visible = true;
                armorModel.leftArm.visible = true;
            }
            case LEGS -> {
                armorModel.body.visible = true;
                armorModel.rightLeg.visible = true;
                armorModel.leftLeg.visible = true;
            }
            case FEET -> {
                armorModel.rightLeg.visible = true;
                armorModel.leftLeg.visible = true;
            }
            default -> {}
        }

        Holder<ArmorMaterial> matHolder = armorItem.getMaterial();
        int defaultColor = stack.is(ItemTags.DYEABLE) ? FastColor.ARGB32.opaque(DyedItemColor.getOrDefault(stack, -6265536)) : -1;

        for (ArmorMaterial.Layer layer : matHolder.value().layers()) {
            ResourceLocation tex = layer.texture(inner);
            int color = layer.dyeable() ? defaultColor : -1;
            VertexConsumer vc = ItemRenderer.getArmorFoilBuffer(buffer, RenderType.armorCutoutNoCull(tex), stack.hasFoil());
            armorModel.renderToBuffer(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, color);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(PlayerCorpseEntity entity) {
        return CustomSkinManager.getOrCreateSkin(entity.getAppearanceData());
    }
}