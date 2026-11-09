package com.rpgcore.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.rpgcore.RPGCore;
import com.rpgcore.data.BackpackItem;
import com.rpgcore.data.ExtraEquipmentData;
import com.rpgcore.data.Gender;
import com.rpgcore.data.HoodedCloakItem;
import com.rpgcore.data.ModAttachments;
import com.rpgcore.data.PlayerAppearanceData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.WeakHashMap;

public class PeasantClothesRenderer {

    private static ResourceLocation CLOTHES_TEX;
    private static ResourceLocation CLOAK_TEX;
    private static final ResourceLocation[] BACKPACK_TEX = new ResourceLocation[5];

    // Хранилище плавной инерции плаща для каждого существа/игрока (чтобы плащ парил на ветру, а не прыгал!)
    private static final WeakHashMap<LivingEntity, Float> SMOOTH_CLOAK_PITCH = new WeakHashMap<>();

    private static final ModelPart HAT_CROWN;
    private static final ModelPart HAT_RIBBON;
    private static final ModelPart HAT_BRIM;

    private static final ModelPart SHIRT_TORSO;
    private static final ModelPart SHIRT_FEMALE_BUST;
    private static final ModelPart SHIRT_LEFT_SLEEVE_WIDE;
    private static final ModelPart SHIRT_RIGHT_SLEEVE_WIDE;
    private static final ModelPart SHIRT_LEFT_SLEEVE_SLIM;
    private static final ModelPart SHIRT_RIGHT_SLEEVE_SLIM;

    private static final ModelPart PANTS_WAIST;
    private static final ModelPart PANTS_LEFT_LEG;
    private static final ModelPart PANTS_RIGHT_LEG;

    private static final ModelPart SHOE_LEFT;
    private static final ModelPart SHOE_RIGHT;

    private static final ModelPart BACKPACK_MAIN;
    private static final ModelPart BACKPACK_UPGRADE_TRIM;

    private static final ModelPart CLOAK_HOOD_UP;
    private static final ModelPart CLOAK_HOOD_DOWN;
    private static final ModelPart CLOAK_TORSO_MANTLE;
    private static final ModelPart CLOAK_LEFT_SHOULDER;
    private static final ModelPart CLOAK_RIGHT_SHOULDER;
    private static final ModelPart CLOAK_BACK_DRAPE;
    private static final ModelPart CLOAK_BACK_OVER_BAG;

    static {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 1. Соломенная шляпа
        root.addOrReplaceChild("hat_crown",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -8.8F, -4.0F, 8.0F, 3.2F, 8.0F, new CubeDeformation(0.35F)),
                PartPose.ZERO);
        root.addOrReplaceChild("hat_ribbon",
                CubeListBuilder.create()
                        .texOffs(64, 0).addBox(-4.0F, -6.6F, -4.0F, 8.0F, 1.1F, 8.0F, new CubeDeformation(0.40F)),
                PartPose.ZERO);
        root.addOrReplaceChild("hat_brim",
                CubeListBuilder.create()
                        .texOffs(0, 16).addBox(-6.5F, -5.6F, -6.5F, 13.0F, 0.8F, 13.0F),
                PartPose.ZERO);

        // 2. Летняя рубаха крестьянина
        root.addOrReplaceChild("shirt_torso",
                CubeListBuilder.create()
                        .texOffs(0, 40).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.26F)),
                PartPose.ZERO);

        root.addOrReplaceChild("shirt_female_bust",
                CubeListBuilder.create()
                        .texOffs(64, 40).addBox(-3.6F, 1.5F, -2.6F, 7.2F, 2.0F, 2.2F)
                        .texOffs(64, 46).addBox(-3.6F, 2.2F, -3.2F, 7.2F, 2.4F, 2.6F),
                PartPose.ZERO);

        root.addOrReplaceChild("shirt_left_sleeve_wide",
                CubeListBuilder.create()
                        .texOffs(32, 40).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.26F)),
                PartPose.ZERO);
        root.addOrReplaceChild("shirt_right_sleeve_wide",
                CubeListBuilder.create()
                        .texOffs(32, 40).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.26F)),
                PartPose.ZERO);

        root.addOrReplaceChild("shirt_left_sleeve_slim",
                CubeListBuilder.create()
                        .texOffs(48, 40).addBox(-1.0F, -2.0F, -2.0F, 3.0F, 8.0F, 4.0F, new CubeDeformation(0.26F)),
                PartPose.ZERO);
        root.addOrReplaceChild("shirt_right_sleeve_slim",
                CubeListBuilder.create()
                        .texOffs(48, 40).addBox(-2.0F, -2.0F, -2.0F, 3.0F, 8.0F, 4.0F, new CubeDeformation(0.26F)),
                PartPose.ZERO);

        // 3. Портки
        root.addOrReplaceChild("pants_waist",
                CubeListBuilder.create()
                        .texOffs(0, 72).addBox(-4.0F, 9.0F, -2.0F, 8.0F, 3.0F, 4.0F, new CubeDeformation(0.22F)),
                PartPose.ZERO);
        root.addOrReplaceChild("pants_left_leg",
                CubeListBuilder.create()
                        .texOffs(32, 72).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 9.2F, 4.0F, new CubeDeformation(0.22F)),
                PartPose.ZERO);
        root.addOrReplaceChild("pants_right_leg",
                CubeListBuilder.create()
                        .texOffs(32, 72).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 9.2F, 4.0F, new CubeDeformation(0.22F)),
                PartPose.ZERO);

        // 4. Башмаки
        root.addOrReplaceChild("shoe_left",
                CubeListBuilder.create()
                        .texOffs(0, 100).addBox(-2.0F, 8.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.28F)),
                PartPose.ZERO);
        root.addOrReplaceChild("shoe_right",
                CubeListBuilder.create()
                        .texOffs(0, 100).addBox(-2.0F, 8.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.28F)),
                PartPose.ZERO);

        // 5. 3D-Модель Рюкзака
        root.addOrReplaceChild("backpack_main",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.2F, 1.4F, 2.0F, 6.4F, 7.6F, 3.4F)
                        .texOffs(0, 0).addBox(-2.8F, 0.8F, 2.0F, 5.6F, 0.8F, 3.2F)
                        .texOffs(0, 14).addBox(-3.4F, 0.9F, 1.9F, 6.8F, 2.4F, 3.8F)
                        .texOffs(0, 14).addBox(-3.1F, 3.1F, 4.8F, 6.2F, 1.1F, 1.0F)
                        .texOffs(48, 0).addBox(-1.4F, 0.1F, 2.4F, 2.8F, 0.8F, 0.7F)
                        .texOffs(24, 0).addBox(-2.4F, 4.7F, 5.1F, 4.8F, 3.7F, 1.1F)
                        .texOffs(0, 14).addBox(-2.5F, 4.4F, 5.0F, 5.0F, 1.0F, 1.3F)
                        .texOffs(24, 8).addBox(-4.2F, 3.8F, 2.3F, 1.1F, 4.4F, 2.6F)
                        .texOffs(0, 14).addBox(-4.35F, 3.5F, 2.2F, 1.3F, 1.1F, 2.8F)
                        .texOffs(24, 8).addBox(3.1F, 3.8F, 2.3F, 1.1F, 4.4F, 2.6F)
                        .texOffs(0, 14).addBox(3.05F, 3.5F, 2.2F, 1.3F, 1.1F, 2.8F)
                        .texOffs(48, 0).addBox(-2.3F, 1.0F, 5.45F, 0.9F, 7.8F, 0.45F)
                        .texOffs(48, 0).addBox(1.4F, 1.0F, 5.45F, 0.9F, 7.8F, 0.45F)
                        .texOffs(0, 24).addBox(-3.5F, 8.9F, 2.2F, 7.0F, 2.1F, 2.3F)
                        .texOffs(48, 0).addBox(-2.3F, 8.75F, 2.05F, 0.8F, 2.4F, 2.6F)
                        .texOffs(48, 0).addBox(1.5F, 8.75F, 2.05F, 0.8F, 2.4F, 2.6F)
                        .texOffs(48, 0).addBox(-3.3F, -0.15F, -2.25F, 1.1F, 8.2F, 4.4F)
                        .texOffs(48, 0).addBox(2.2F, -0.15F, -2.25F, 1.1F, 8.2F, 4.4F)
                        .texOffs(48, 16).addBox(-2.2F, 3.2F, -2.3F, 4.4F, 0.8F, 0.4F),
                PartPose.ZERO);

        root.addOrReplaceChild("backpack_upgrade_trim",
                CubeListBuilder.create()
                        .texOffs(0, 34).addBox(-2.45F, 3.4F, 5.55F, 1.2F, 1.4F, 0.55F)
                        .texOffs(0, 34).addBox(1.25F, 3.4F, 5.55F, 1.2F, 1.4F, 0.55F)
                        .texOffs(12, 34).addBox(-0.6F, 4.9F, 6.0F, 1.2F, 1.3F, 0.45F)
                        .texOffs(0, 40).addBox(-3.55F, 0.75F, 1.8F, 1.3F, 1.4F, 4.0F)
                        .texOffs(0, 40).addBox(2.25F, 0.75F, 1.8F, 1.3F, 1.4F, 4.0F)
                        .texOffs(0, 48).addBox(-3.35F, 7.7F, 1.9F, 1.2F, 1.4F, 3.7F)
                        .texOffs(0, 48).addBox(2.15F, 7.7F, 1.9F, 1.2F, 1.4F, 3.7F)
                        .texOffs(12, 34).addBox(-4.45F, 3.8F, 3.2F, 0.4F, 0.8F, 0.8F)
                        .texOffs(12, 34).addBox(4.05F, 3.8F, 3.2F, 0.4F, 0.8F, 0.8F)
                        .texOffs(12, 34).addBox(-0.5F, 3.05F, -2.4F, 1.0F, 1.1F, 0.5F),
                PartPose.ZERO);

        // 6. Капюшон и полотно плаща
        root.addOrReplaceChild("cloak_hood_up",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-5.45F, -10.1F, -4.85F, 10.9F, 1.5F, 10.35F)
                        .texOffs(0, 0).addBox(-4.6F, -10.6F, -4.1F, 9.2F, 0.6F, 9.2F)
                        .texOffs(36, 0).addBox(-5.1F, -9.4F, -5.55F, 10.2F, 1.15F, 1.1F)
                        .texOffs(0, 12).addBox(-5.45F, -8.8F, -4.65F, 1.35F, 9.1F, 10.15F)
                        .texOffs(22, 12).addBox(4.10F, -8.8F, -4.65F, 1.35F, 9.1F, 10.15F)
                        .texOffs(0, 30).addBox(-5.45F, -8.8F, 4.15F, 10.9F, 9.1F, 1.4F)
                        .texOffs(36, 16).addBox(-3.8F, -7.8F, 5.35F, 7.6F, 7.2F, 1.2F)
                        .texOffs(36, 26).addBox(-2.6F, -5.6F, 6.25F, 5.2F, 4.8F, 1.0F),
                PartPose.ZERO);

        root.addOrReplaceChild("cloak_hood_down",
                CubeListBuilder.create()
                        .texOffs(0, 20).addBox(-5.0F, -1.0F, -1.8F, 1.6F, 2.0F, 5.2F)
                        .texOffs(0, 20).addBox(3.4F, -1.0F, -1.8F, 1.6F, 2.0F, 5.2F)
                        .texOffs(36, 16).addBox(-4.4F, -0.8F, 2.0F, 8.8F, 3.0F, 2.8F)
                        .texOffs(36, 26).addBox(-3.2F, 1.8F, 2.6F, 6.4F, 2.2F, 2.2F),
                PartPose.ZERO);

        root.addOrReplaceChild("cloak_torso_mantle",
                CubeListBuilder.create()
                        .texOffs(0, 32).addBox(-4.9F, -0.6F, -2.4F, 9.8F, 5.2F, 5.2F, new CubeDeformation(0.35F))
                        .texOffs(0, 40).addBox(-4.5F, -0.4F, -3.1F, 3.0F, 1.8F, 1.2F)
                        .texOffs(0, 40).addBox(1.5F, -0.4F, -3.1F, 3.0F, 1.8F, 1.2F)
                        .texOffs(52, 34).addBox(-1.0F, -0.1F, -3.4F, 2.0F, 1.4F, 0.6F),
                PartPose.ZERO);

        root.addOrReplaceChild("cloak_left_shoulder",
                CubeListBuilder.create()
                        .texOffs(0, 48).addBox(-1.2F, -2.5F, -2.6F, 5.2F, 3.8F, 5.2F, new CubeDeformation(0.55F)),
                PartPose.ZERO);

        root.addOrReplaceChild("cloak_right_shoulder",
                CubeListBuilder.create()
                        .texOffs(24, 48).addBox(-4.0F, -2.5F, -2.6F, 5.2F, 3.8F, 5.2F, new CubeDeformation(0.55F)),
                PartPose.ZERO);

        root.addOrReplaceChild("cloak_back_drape",
                CubeListBuilder.create()
                        .texOffs(64, 0).addBox(-5.0F, 0.0F, -0.3F, 10.0F, 19.2F, 1.1F)
                        .texOffs(92, 0).addBox(-5.5F, 0.4F, -1.3F, 1.2F, 17.2F, 1.9F)
                        .texOffs(92, 0).addBox(4.3F, 0.4F, -1.3F, 1.2F, 17.2F, 1.9F)
                        .texOffs(104, 0).addBox(-2.2F, 1.0F, 0.5F, 4.4F, 18.5F, 0.6F),
                PartPose.offset(0.0F, 0.4F, 3.15F));

        root.addOrReplaceChild("cloak_back_over_bag",
                CubeListBuilder.create()
                        .texOffs(64, 0).addBox(-5.2F, -0.2F, -0.8F, 10.4F, 2.2F, 4.4F)
                        .texOffs(64, 0).addBox(-5.2F, 0.8F, 2.5F, 10.4F, 18.8F, 1.2F)
                        .texOffs(92, 0).addBox(-5.6F, 0.4F, -1.2F, 1.3F, 17.8F, 4.6F)
                        .texOffs(92, 0).addBox(4.3F, 0.4F, -1.2F, 1.3F, 17.8F, 4.6F)
                        .texOffs(104, 0).addBox(-2.2F, 1.2F, 3.5F, 4.4F, 18.0F, 0.6F),
                PartPose.offset(0.0F, 0.4F, 3.15F));

        ModelPart baked = root.bake(128, 128);
        HAT_CROWN = baked.getChild("hat_crown");
        HAT_RIBBON = baked.getChild("hat_ribbon");
        HAT_BRIM = baked.getChild("hat_brim");
        SHIRT_TORSO = baked.getChild("shirt_torso");
        SHIRT_FEMALE_BUST = baked.getChild("shirt_female_bust");
        SHIRT_LEFT_SLEEVE_WIDE = baked.getChild("shirt_left_sleeve_wide");
        SHIRT_RIGHT_SLEEVE_WIDE = baked.getChild("shirt_right_sleeve_wide");
        SHIRT_LEFT_SLEEVE_SLIM = baked.getChild("shirt_left_sleeve_slim");
        SHIRT_RIGHT_SLEEVE_SLIM = baked.getChild("shirt_right_sleeve_slim");
        PANTS_WAIST = baked.getChild("pants_waist");
        PANTS_LEFT_LEG = baked.getChild("pants_left_leg");
        PANTS_RIGHT_LEG = baked.getChild("pants_right_leg");
        SHOE_LEFT = baked.getChild("shoe_left");
        SHOE_RIGHT = baked.getChild("shoe_right");
        BACKPACK_MAIN = baked.getChild("backpack_main");
        BACKPACK_UPGRADE_TRIM = baked.getChild("backpack_upgrade_trim");

        CLOAK_HOOD_UP = baked.getChild("cloak_hood_up");
        CLOAK_HOOD_DOWN = baked.getChild("cloak_hood_down");
        CLOAK_TORSO_MANTLE = baked.getChild("cloak_torso_mantle");
        CLOAK_LEFT_SHOULDER = baked.getChild("cloak_left_shoulder");
        CLOAK_RIGHT_SHOULDER = baked.getChild("cloak_right_shoulder");
        CLOAK_BACK_DRAPE = baked.getChild("cloak_back_drape");
        CLOAK_BACK_OVER_BAG = baked.getChild("cloak_back_over_bag");
    }

    public static ResourceLocation getOrCreateCloakTexture() {
        if (CLOAK_TEX != null) return CLOAK_TEX;

        NativeImage img = new NativeImage(128, 128, true);
        int clothBase = 0xFF302C29;
        int clothLight = 0xFF3B3632;
        int clothFold = 0xFF24201E;
        int leatherTrim = 0xFF4F331F;
        int goldBrooch = 0xFFD4AF37;
        int goldDark = 0xFF8C6D1F;

        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                int hash = (x * 23 + y * 41 + (x ^ y) * 11) & 15;
                float grain = 0.95F + (hash / 15.0F) * 0.10F;

                int c = ((x + y) % 3 == 0) ? clothLight : clothBase;

                if (x % 5 == 0) {
                    c = clothFold;
                }

                if (y == 18 || y == 19 || y == 39 || y == 49) {
                    c = (x % 2 == 0) ? goldDark : leatherTrim;
                }

                if (x >= 52 && x < 64 && y >= 34 && y < 42) {
                    c = ((x + y) % 2 == 0) ? goldBrooch : goldDark;
                }

                img.setPixelRGBA(x, y, toAbgr(adjustRgb(c, grain)));
            }
        }

        DynamicTexture tex = new DynamicTexture(img);
        CLOAK_TEX = ResourceLocation.fromNamespaceAndPath(RPGCore.MODID, "hooded_cloak_3d_v5");
        Minecraft.getInstance().getTextureManager().register(CLOAK_TEX, tex);
        return CLOAK_TEX;
    }

    private static boolean isPlayerRolling(LivingEntity entity, HumanoidModel<?> parentModel) {
        if (entity == null) return false;
        if (Math.abs(parentModel.body.xRot) > 0.85F) return true;
        if (entity.getPersistentData().getBoolean("rpganimations_rolling")
                || entity.getPersistentData().getInt("DodgeRollTicks") > 0) {
            return true;
        }
        return false;
    }

    /**
     * ПУНКТ 1: Естественная аэродинамическая физика плаща с плавной инерцией!
     * Плащ больше НЕ подпрыгивает в такт каждому шагу ноги, а плавно поднимается потоком ветра
     * от скорости движения (SMOOTH_CLOAK_PITCH) и мягко опускается при остановке.
     */
    public static void renderCloak(PoseStack poseStack, MultiBufferSource buffer, LivingEntity entity,
                                   boolean hoodUp, boolean hideHeadInFirstPerson, float limbSwing, float limbSwingAmount,
                                   int packedLight, HumanoidModel<?> parentModel) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(getOrCreateCloakTexture()));

        poseStack.pushPose();
        parentModel.body.translateAndRotate(poseStack);
        CLOAK_TORSO_MANTLE.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);

        if (!hoodUp) {
            CLOAK_HOOD_DOWN.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
        }

        boolean wearingBackpack = false;
        if (entity instanceof AbstractClientPlayer p) {
            wearingBackpack = !p.getData(ModAttachments.EXTRA_EQUIPMENT).getStack(ExtraEquipmentData.SLOT_BACKPACK).isEmpty();
        }

        float clampedSpeed = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        // Целевой угол подъема зависит только от скорости движения (без дерганий от фазы шага ног!)
        float targetPitch = 0.08F + clampedSpeed * 0.58F;

        if (entity != null) {
            if (entity.isSprinting()) {
                targetPitch += 0.16F;
            }
            if (entity.isCrouching()) {
                targetPitch += 0.18F;
            }
            if (entity.getDeltaMovement().y < -0.08D) {
                targetPitch += Mth.clamp((float) (-entity.getDeltaMovement().y) * 0.45F, 0.0F, 0.35F);
            }
            if (entity.isPassenger()) {
                targetPitch = Mth.clamp(targetPitch, 0.30F, 0.52F);
            }
            if (isPlayerRolling(entity, parentModel)) {
                targetPitch = 0.10F;
            }
        }

        targetPitch = Mth.clamp(targetPitch, 0.08F, 0.95F);

        // Плавно интерполируем инерцию ткани плаща (0.09F на подъем, 0.06F на плавное опускание)
        float finalWave;
        if (entity != null) {
            float prevPitch = SMOOTH_CLOAK_PITCH.getOrDefault(entity, targetPitch);
            float lerpSpeed = (targetPitch > prevPitch) ? 0.09F : 0.055F;
            float smoothed = Mth.lerp(lerpSpeed, prevPitch, targetPitch);
            SMOOTH_CLOAK_PITCH.put(entity, smoothed);

            // Очень тонкая воздушная рябь ткани на ветру (всего ±0.025 рад вместо прыжков!)
            float gentleBreeze = Mth.sin(entity.tickCount * 0.10F + limbSwing * 0.35F) * (0.015F + clampedSpeed * 0.025F);
            finalWave = smoothed + gentleBreeze;
        } else {
            finalWave = targetPitch;
        }

        float sideSway = (entity != null && !isPlayerRolling(entity, parentModel))
                ? Mth.sin(entity.tickCount * 0.08F + limbSwing * 0.4F) * 0.025F * clampedSpeed
                : 0.0F;

        if (wearingBackpack) {
            CLOAK_BACK_OVER_BAG.xRot = finalWave * 0.78F;
            CLOAK_BACK_OVER_BAG.zRot = sideSway;
            CLOAK_BACK_OVER_BAG.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
        } else {
            CLOAK_BACK_DRAPE.xRot = finalWave;
            CLOAK_BACK_DRAPE.zRot = sideSway;
            CLOAK_BACK_DRAPE.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
        }
        poseStack.popPose();

        poseStack.pushPose();
        parentModel.leftArm.translateAndRotate(poseStack);
        CLOAK_LEFT_SHOULDER.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();

        poseStack.pushPose();
        parentModel.rightArm.translateAndRotate(poseStack);
        CLOAK_RIGHT_SHOULDER.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();

        if (hoodUp && !hideHeadInFirstPerson) {
            poseStack.pushPose();
            parentModel.head.translateAndRotate(poseStack);
            CLOAK_HOOD_UP.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
    }

    public static ResourceLocation getOrCreateBackpackTexture(BackpackItem.Tier tier) {
        int idx = tier.getLevel();
        if (BACKPACK_TEX[idx] != null) return BACKPACK_TEX[idx];

        NativeImage img = new NativeImage(128, 128, true);

        int leatherBase = 0xFF78492A;
        int leatherShade = 0xFF5E371E;
        int leatherDark = 0xFF472814;
        int flapLight = 0xFF8A5634;
        int stitchCol = 0xFF9E704C;
        int strapCol = 0xFF331C0E;
        int strapEdge = 0xFF241208;

        int rollCloth = 0xFF8C7D65;
        int rollShade = 0xFF70624E;

        int trimLight = tier.getTrimLight();
        int trimDark = tier.getTrimDark();
        int trimSpec = adjustRgb(trimLight, 1.18F);

        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 128; x++) {
                int hash = (x * 19 + y * 37 + (x ^ y) * 7) & 15;
                float grain = 0.96F + (hash / 15.0F) * 0.08F;

                int c;
                if (y >= 32) {
                    float grad = 0.92F + ((y % 8) / 7.0F) * 0.16F;
                    int baseMetal = (x % 6 == 0 || y % 6 == 0) ? trimDark : trimLight;
                    if ((x + y) % 7 == 1) baseMetal = trimSpec;
                    c = adjustRgb(baseMetal, grad);
                } else if (x >= 48) {
                    c = adjustRgb((x % 4 == 0) ? strapEdge : strapCol, grain);
                } else if (y >= 24) {
                    int clothBase = (x % 3 == 0) ? rollShade : rollCloth;
                    c = adjustRgb(clothBase, grain);
                } else if (y >= 14) {
                    if (y == 21) {
                        c = (x % 2 == 0) ? stitchCol : leatherDark;
                    } else {
                        c = adjustRgb(flapLight, grain);
                    }
                } else {
                    float vShade = 1.04F - (y / 14.0F) * 0.12F;
                    int bodyCol = (x % 8 == 0 || y == 13) ? leatherShade : leatherBase;
                    c = adjustRgb(bodyCol, grain * vShade);
                }

                if (idx == 0 && x >= 48 && (y == 5 || y == 6)) {
                    c = 0xFFB88A32;
                }

                img.setPixelRGBA(x, y, toAbgr(c));
            }
        }

        DynamicTexture tex = new DynamicTexture(img);
        BACKPACK_TEX[idx] = ResourceLocation.fromNamespaceAndPath(RPGCore.MODID, "backpack_hd_v2_tier_" + idx);
        Minecraft.getInstance().getTextureManager().register(BACKPACK_TEX[idx], tex);
        return BACKPACK_TEX[idx];
    }

    private static int adjustRgb(int rgb, float factor) {
        int r = Math.min(255, Math.max(0, Math.round(((rgb >> 16) & 0xFF) * factor)));
        int g = Math.min(255, Math.max(0, Math.round(((rgb >> 8) & 0xFF) * factor)));
        int b = Math.min(255, Math.max(0, Math.round((rgb & 0xFF) * factor)));
        return (r << 16) | (g << 8) | b;
    }

    public static void renderBackpack(PoseStack poseStack, MultiBufferSource buffer, LivingEntity entity,
                                      BackpackItem.Tier tier, int packedLight, HumanoidModel<?> parentModel) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(getOrCreateBackpackTexture(tier)));
        poseStack.pushPose();
        parentModel.body.translateAndRotate(poseStack);

        if (entity instanceof AbstractClientPlayer p) {
            var cape = p.getData(ModAttachments.EXTRA_EQUIPMENT).getStack(ExtraEquipmentData.SLOT_CAPE);
            if (!cape.isEmpty() && cape.getItem() instanceof HoodedCloakItem) {
                poseStack.translate(0.0F, 0.08F, -0.05F);
                poseStack.scale(0.86F, 0.88F, 0.84F);
            }
        }

        BACKPACK_MAIN.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
        if (tier.getLevel() >= 1) {
            BACKPACK_UPGRADE_TRIM.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
        }
        poseStack.popPose();
    }

    public static ResourceLocation getOrCreateTexture() {
        if (CLOTHES_TEX != null) return CLOTHES_TEX;
        NativeImage img = new NativeImage(128, 128, true);

        for (int y = 0; y < 40; y++) {
            for (int x = 0; x < 128; x++) {
                if (y < 16 && x >= 64) {
                    int rib = (y == 4) ? 0xFF6B1812 : ((y == 5) ? 0xFFB8352A : 0xFF94241B);
                    img.setPixelRGBA(x, y, toAbgr(rib));
                } else {
                    int wave = ((x + (y % 2) * 2) % 4);
                    int c = (wave == 0) ? 0xFFC79F42 : ((wave == 2) ? 0xFFEDD076 : 0xFFDFB85B);
                    img.setPixelRGBA(x, y, toAbgr(c));
                }
            }
        }

        for (int y = 40; y < 72; y++) {
            for (int x = 0; x < 128; x++) {
                int dy = y - 40;
                int weave = ((x + dy) % 2 == 0) ? 0xFFE6DEC8 : 0xFFDDD4BE;
                if (x % 4 == 0 && x < 24) weave = 0xFFD4C9B2;
                int c = weave;

                if (x < 24) {
                    if (dy == 12 || dy == 13) {
                        c = (dy == 12) ? 0xFF4A2E1B : 0xFF382112;
                        if (x == 7 || x == 8) c = (dy == 12) ? 0xFFE5C148 : 0xFFB89228;
                    } else if (dy == 15) {
                        c = 0xFFC8BCA4;
                    }
                    if (dy == 4 && (x == 6 || x == 9)) c = 0xFF8C7A64;
                    if (dy == 4 && (x == 7 || x == 8)) c = 0xFF5C4735;
                    if (dy == 5 && (x == 7 || x == 8)) c = 0xFF75604B;
                    if (dy == 6 && (x == 7 || x == 8)) c = 0xFF8C7A64;
                }
                if (x >= 32 && x < 64 && dy == 11) c = 0xFFC4B79E;
                if (x >= 64) c = (dy >= 9) ? 0xFFD1C5AE : weave;

                img.setPixelRGBA(x, y, toAbgr(c));
            }
        }

        for (int y = 72; y < 100; y++) {
            for (int x = 0; x < 128; x++) {
                int dy = y - 72;
                int c = ((x + dy) % 2 == 0) ? 0xFF6E5B48 : 0xFF665442;
                if (x % 4 == 1) c = 0xFF594838;
                if (dy == 12) c = 0xFF4D3E30;
                img.setPixelRGBA(x, y, toAbgr(c));
            }
        }

        for (int y = 100; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                int dy = y - 100;
                int c;
                if (dy == 4) c = 0xFFD8CEB8;
                else if (dy == 7) c = 0xFF2B180D;
                else c = ((x + dy) % 2 == 0) ? 0xFF6B3E23 : 0xFF5E351D;
                img.setPixelRGBA(x, y, toAbgr(c));
            }
        }

        DynamicTexture tex = new DynamicTexture(img);
        CLOTHES_TEX = ResourceLocation.fromNamespaceAndPath(RPGCore.MODID, "peasant_clothes_v3");
        Minecraft.getInstance().getTextureManager().register(CLOTHES_TEX, tex);
        return CLOTHES_TEX;
    }

    public static void renderPiece(PoseStack poseStack, MultiBufferSource buffer, LivingEntity entity,
                                   int slotIndex, int packedLight, HumanoidModel<?> parentModel) {
        boolean isFemale = false;
        if (entity instanceof AbstractClientPlayer player) {
            PlayerAppearanceData data = player.getData(ModAttachments.APPEARANCE);
            isFemale = (data.gender() == Gender.FEMALE);
        }
        renderPiece(poseStack, buffer, entity, slotIndex, packedLight, parentModel, isFemale);
    }

    public static void renderPiece(PoseStack poseStack, MultiBufferSource buffer, LivingEntity entity,
                                   int slotIndex, int packedLight, HumanoidModel<?> parentModel, boolean isFemale) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(getOrCreateTexture()));

        switch (slotIndex) {
            case ExtraEquipmentData.SLOT_HAT_UNDER -> {
                poseStack.pushPose();
                parentModel.head.translateAndRotate(poseStack);
                HAT_CROWN.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
                HAT_RIBBON.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
                HAT_BRIM.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
                poseStack.popPose();
            }
            case ExtraEquipmentData.SLOT_SHIRT_UNDER -> {
                poseStack.pushPose();
                parentModel.body.translateAndRotate(poseStack);
                SHIRT_TORSO.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
                if (isFemale) {
                    poseStack.pushPose();
                    poseStack.mulPose(Axis.XP.rotationDegrees(-8.0F));
                    SHIRT_FEMALE_BUST.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
                    poseStack.popPose();
                }
                poseStack.popPose();

                poseStack.pushPose();
                parentModel.leftArm.translateAndRotate(poseStack);
                if (isFemale) SHIRT_LEFT_SLEEVE_SLIM.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
                else SHIRT_LEFT_SLEEVE_WIDE.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
                poseStack.popPose();

                poseStack.pushPose();
                parentModel.rightArm.translateAndRotate(poseStack);
                if (isFemale) SHIRT_RIGHT_SLEEVE_SLIM.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
                else SHIRT_RIGHT_SLEEVE_WIDE.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
                poseStack.popPose();
            }
            case ExtraEquipmentData.SLOT_PANTS_UNDER -> {
                poseStack.pushPose();
                parentModel.body.translateAndRotate(poseStack);
                PANTS_WAIST.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
                poseStack.popPose();

                poseStack.pushPose();
                parentModel.leftLeg.translateAndRotate(poseStack);
                PANTS_LEFT_LEG.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
                poseStack.popPose();

                poseStack.pushPose();
                parentModel.rightLeg.translateAndRotate(poseStack);
                PANTS_RIGHT_LEG.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
                poseStack.popPose();
            }
            case ExtraEquipmentData.SLOT_SHOES_UNDER -> {
                poseStack.pushPose();
                parentModel.leftLeg.translateAndRotate(poseStack);
                SHOE_LEFT.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
                poseStack.popPose();

                poseStack.pushPose();
                parentModel.rightLeg.translateAndRotate(poseStack);
                SHOE_RIGHT.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
                poseStack.popPose();
            }
        }
    }

    private static int toAbgr(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return 0xFF000000 | (b << 16) | (g << 8) | r;
    }
}