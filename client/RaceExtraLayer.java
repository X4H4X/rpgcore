package com.rpgcore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.rpgcore.data.BackpackItem;
import com.rpgcore.data.ExtraEquipmentData;
import com.rpgcore.data.Gender;
import com.rpgcore.data.HoodedCloakItem;
import com.rpgcore.data.ModAttachments;
import com.rpgcore.data.PeasantClothesItem;
import com.rpgcore.data.PlayerAppearanceData;
import com.rpgcore.data.RaceType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.neoforge.client.ClientHooks;

import java.util.List;

public class RaceExtraLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private final ModelPart chestMesh;
    private final ModelPart armoredChestMesh;

    private final ModelPart elfEarLeft;
    private final ModelPart elfEarRight;

    private final ModelPart orcTusks;
    private final ModelPart orcEarLeft;
    private final ModelPart orcEarRight;

    private final ModelPart dragonSnoutMale;
    private final ModelPart dragonSnoutFemale;
    private final ModelPart dragonHorns;
    private final ModelPart dragonSpines;
    private final ModelPart dragonTail;

    private final ModelPart[] maleHairMeshes = new ModelPart[7];
    private final ModelPart[] femaleHairMeshes = new ModelPart[5];
    private final ModelPart[] beardMeshes = new ModelPart[10];

    public RaceExtraLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer) {
        super(renderer);

        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("chest",
                CubeListBuilder.create()
                        .texOffs(18, 19).addBox(-3.5F, 1.5F, -2.6F, 7.0F, 2.0F, 2.2F)
                        .texOffs(18, 20).addBox(-3.5F, 2.2F, -3.2F, 7.0F, 2.5F, 2.6F),
                PartPose.ZERO);

        root.addOrReplaceChild("armored_chest",
                CubeListBuilder.create()
                        .texOffs(16, 17).addBox(-3.4F, 1.1F, -3.2F, 6.8F, 2.0F, 3.2F)
                        .texOffs(16, 19).addBox(-3.5F, 2.1F, -3.9F, 7.0F, 3.0F, 3.8F),
                PartPose.ZERO);

        root.addOrReplaceChild("elf_ear_left",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(3.8F, -4.6F, -1.2F, 1.2F, 1.4F, 3.4F)
                        .texOffs(0, 0).addBox(4.3F, -5.1F, 1.4F, 0.9F, 1.0F, 2.6F),
                PartPose.rotation(0.15F, 0.28F, 0.0F));

        root.addOrReplaceChild("elf_ear_right",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-5.0F, -4.6F, -1.2F, 1.2F, 1.4F, 3.4F)
                        .texOffs(0, 0).addBox(-5.2F, -5.1F, 1.4F, 0.9F, 1.0F, 2.6F),
                PartPose.rotation(0.15F, -0.28F, 0.0F));

        root.addOrReplaceChild("orc_tusks",
                CubeListBuilder.create()
                        .texOffs(32, 54).addBox(-2.6F, -2.2F, -4.55F, 1.0F, 1.8F, 0.8F)
                        .texOffs(32, 54).addBox(1.6F, -2.2F, -4.55F, 1.0F, 1.8F, 0.8F),
                PartPose.ZERO);

        root.addOrReplaceChild("orc_ear_left",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(3.8F, -4.5F, -1.0F, 1.2F, 1.6F, 2.4F),
                PartPose.rotation(0.10F, 0.22F, 0.0F));

        root.addOrReplaceChild("orc_ear_right",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-5.0F, -4.5F, -1.0F, 1.2F, 1.6F, 2.4F),
                PartPose.rotation(0.10F, -0.22F, 0.0F));

        root.addOrReplaceChild("dragon_snout_male",
                CubeListBuilder.create()
                        .texOffs(0, 32).addBox(-2.5F, -3.2F, -5.8F, 5.0F, 2.0F, 2.0F)
                        .texOffs(0, 38).addBox(-2.0F, -1.3F, -5.5F, 4.0F, 1.0F, 1.6F),
                PartPose.ZERO);

        root.addOrReplaceChild("dragon_snout_female",
                CubeListBuilder.create()
                        .texOffs(0, 32).addBox(-1.8F, -2.9F, -5.2F, 3.6F, 1.6F, 1.4F)
                        .texOffs(0, 38).addBox(-1.4F, -1.4F, -4.9F, 2.8F, 0.8F, 1.1F),
                PartPose.ZERO);

        root.addOrReplaceChild("dragon_horns",
                CubeListBuilder.create()
                        .texOffs(32, 33).addBox(-4.1F, -8.4F, -2.4F, 1.7F, 1.7F, 2.8F)
                        .texOffs(32, 42).addBox(-4.35F, -9.3F, 0.1F, 1.4F, 1.4F, 3.0F)
                        .texOffs(32, 52).addBox(-4.5F, -10.1F, 2.8F, 1.0F, 1.0F, 2.6F)
                        .texOffs(32, 33).addBox(2.4F, -8.4F, -2.4F, 1.7F, 1.7F, 2.8F)
                        .texOffs(32, 42).addBox(2.95F, -9.3F, 0.1F, 1.4F, 1.4F, 3.0F)
                        .texOffs(32, 52).addBox(3.5F, -10.1F, 2.8F, 1.0F, 1.0F, 2.6F)
                        .texOffs(32, 50).addBox(-4.5F, -5.2F, -0.5F, 0.7F, 0.9F, 2.2F)
                        .texOffs(32, 50).addBox(3.8F, -5.2F, -0.5F, 0.7F, 0.9F, 2.2F),
                PartPose.rotation(0.18F, 0.0F, 0.0F));

        root.addOrReplaceChild("dragon_spines",
                CubeListBuilder.create()
                        .texOffs(32, 46).addBox(-0.3F, 1.0F, 1.9F, 0.6F, 1.5F, 1.1F)
                        .texOffs(32, 46).addBox(-0.3F, 3.2F, 1.9F, 0.6F, 1.4F, 1.0F)
                        .texOffs(32, 46).addBox(-0.3F, 5.4F, 1.9F, 0.6F, 1.3F, 0.9F)
                        .texOffs(32, 46).addBox(-0.3F, 7.5F, 1.9F, 0.6F, 1.1F, 0.8F),
                PartPose.ZERO);

        root.addOrReplaceChild("dragon_tail",
                CubeListBuilder.create()
                        .texOffs(0, 40).addBox(-1.5F, -1.2F, -0.2F, 3.0F, 3.0F, 4.0F)
                        .texOffs(32, 46).addBox(-0.3F, -2.1F, 0.3F, 0.6F, 1.0F, 1.2F)
                        .texOffs(32, 46).addBox(-0.3F, -2.0F, 2.0F, 0.6F, 0.9F, 1.2F)
                        .texOffs(0, 47).addBox(-1.2F, -0.4F, 3.4F, 2.4F, 2.4F, 4.5F)
                        .texOffs(32, 46).addBox(-0.3F, -1.2F, 4.0F, 0.6F, 0.9F, 1.2F)
                        .texOffs(32, 46).addBox(-0.3F, -1.1F, 5.8F, 0.6F, 0.8F, 1.2F)
                        .texOffs(0, 54).addBox(-0.9F, 0.4F, 7.5F, 1.8F, 1.8F, 4.0F)
                        .texOffs(32, 48).addBox(-0.3F, -0.3F, 8.2F, 0.6F, 0.8F, 2.2F),
                PartPose.offsetAndRotation(0.0F, 10.5F, 2.0F, -0.58F, 0.0F, 0.0F));

        root.addOrReplaceChild("male_hair_0", CubeListBuilder.create(), PartPose.ZERO);

        root.addOrReplaceChild("male_hair_1",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.2F, -8.6F, -4.2F, 2.4F, 1.0F, 8.6F)
                        .texOffs(0, 0).addBox(-0.9F, -10.8F, -3.8F, 1.8F, 2.4F, 7.8F)
                        .texOffs(0, 0).addBox(-0.6F, -11.8F, -2.8F, 1.2F, 1.2F, 6.2F)
                        .texOffs(0, 0).addBox(-1.0F, -8.2F, 3.5F, 2.0F, 5.8F, 1.6F)
                        .texOffs(0, 0).addBox(-0.7F, -7.5F, 4.8F, 1.4F, 4.5F, 1.0F),
                PartPose.ZERO);

        root.addOrReplaceChild("male_hair_2",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.3F, -8.75F, -4.3F, 8.6F, 2.1F, 8.6F)
                        .texOffs(0, 0).addBox(-3.6F, -9.1F, -3.6F, 7.2F, 0.5F, 7.4F)
                        .texOffs(0, 0).addBox(-4.2F, -6.8F, -4.4F, 4.8F, 1.0F, 0.6F)
                        .texOffs(0, 0).addBox(-4.45F, -6.8F, -3.5F, 0.8F, 4.0F, 7.7F)
                        .texOffs(0, 0).addBox(3.65F, -6.8F, -3.5F, 0.8F, 4.0F, 7.7F)
                        .texOffs(0, 0).addBox(-4.2F, -6.8F, 3.45F, 8.4F, 5.0F, 0.95F),
                PartPose.ZERO);

        root.addOrReplaceChild("male_hair_3",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.35F, -8.85F, -4.25F, 8.7F, 2.2F, 8.6F)
                        .texOffs(0, 0).addBox(-4.3F, -6.8F, -4.4F, 2.0F, 2.0F, 0.7F)
                        .texOffs(0, 0).addBox(2.3F, -6.8F, -4.4F, 2.0F, 2.0F, 0.7F)
                        .texOffs(0, 0).addBox(-4.55F, -6.8F, -3.7F, 1.0F, 6.0F, 7.9F)
                        .texOffs(0, 0).addBox(3.55F, -6.8F, -3.7F, 1.0F, 6.0F, 7.9F)
                        .texOffs(0, 0).addBox(-4.25F, -6.8F, 3.45F, 8.5F, 6.6F, 1.1F),
                PartPose.ZERO);

        root.addOrReplaceChild("male_hair_4",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.3F, -8.75F, -4.2F, 8.6F, 2.1F, 8.5F)
                        .texOffs(0, 0).addBox(-4.45F, -6.8F, -3.5F, 0.8F, 4.4F, 7.7F)
                        .texOffs(0, 0).addBox(3.65F, -6.8F, -3.5F, 0.8F, 4.4F, 7.7F)
                        .texOffs(0, 0).addBox(-4.2F, -6.8F, 3.4F, 8.4F, 5.0F, 0.95F)
                        .texOffs(0, 0).addBox(-1.5F, -5.0F, 4.1F, 3.0F, 2.2F, 1.5F)
                        .texOffs(0, 0).addBox(-1.2F, -3.0F, 4.3F, 2.4F, 4.2F, 1.3F),
                PartPose.ZERO);

        root.addOrReplaceChild("male_hair_5",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.4F, -8.9F, -4.25F, 8.8F, 2.3F, 8.6F)
                        .texOffs(0, 0).addBox(-4.35F, -6.8F, -4.35F, 1.8F, 2.8F, 0.75F)
                        .texOffs(0, 0).addBox(2.55F, -6.8F, -4.35F, 1.8F, 2.8F, 0.75F)
                        .texOffs(0, 0).addBox(-4.6F, -6.8F, -3.6F, 1.1F, 8.0F, 7.8F)
                        .texOffs(0, 0).addBox(3.5F, -6.8F, -3.6F, 1.1F, 8.0F, 7.8F)
                        .texOffs(0, 0).addBox(-4.3F, -6.8F, 3.45F, 8.6F, 8.5F, 1.2F),
                PartPose.ZERO);

        root.addOrReplaceChild("male_hair_6",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.35F, -8.8F, -4.25F, 8.7F, 2.2F, 8.6F)
                        .texOffs(0, 0).addBox(-4.45F, -6.8F, -3.4F, 0.9F, 4.8F, 7.6F)
                        .texOffs(0, 0).addBox(3.55F, -6.8F, -3.4F, 0.9F, 4.8F, 7.6F)
                        .texOffs(0, 0).addBox(-4.25F, -6.8F, 3.4F, 8.5F, 5.8F, 1.0F)
                        .texOffs(0, 0).addBox(-4.5F, -6.8F, -4.35F, 1.2F, 5.8F, 1.0F)
                        .texOffs(0, 0).addBox(3.3F, -6.8F, -4.35F, 1.2F, 5.8F, 1.0F),
                PartPose.ZERO);

        root.addOrReplaceChild("fem_hair_0",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.4F, -8.85F, -4.3F, 8.8F, 2.25F, 8.6F)
                        .texOffs(0, 0).addBox(-3.6F, -9.25F, -3.6F, 7.2F, 0.5F, 7.4F)
                        .texOffs(0, 0).addBox(-4.25F, -6.8F, -4.4F, 3.5F, 1.1F, 0.65F)
                        .texOffs(0, 0).addBox(0.75F, -6.8F, -4.4F, 3.5F, 1.1F, 0.65F)
                        .texOffs(0, 0).addBox(-4.55F, -5.8F, -4.3F, 1.05F, 8.2F, 1.2F)
                        .texOffs(0, 0).addBox(3.5F, -5.8F, -4.3F, 1.05F, 8.2F, 1.2F)
                        .texOffs(0, 0).addBox(-4.6F, -6.8F, -3.3F, 1.05F, 9.5F, 7.5F)
                        .texOffs(0, 0).addBox(3.55F, -6.8F, -3.3F, 1.05F, 9.5F, 7.5F)
                        .texOffs(0, 0).addBox(-4.35F, -6.8F, 3.45F, 8.7F, 10.5F, 1.3F)
                        .texOffs(0, 0).addBox(-3.5F, 3.5F, 3.5F, 7.0F, 2.0F, 1.1F),
                PartPose.ZERO);

        root.addOrReplaceChild("fem_hair_1",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.35F, -8.8F, -4.25F, 8.7F, 2.2F, 8.5F)
                        .texOffs(0, 0).addBox(-3.5F, -9.15F, -3.4F, 7.0F, 0.45F, 7.2F)
                        .texOffs(0, 0).addBox(-4.45F, -6.8F, -3.3F, 0.9F, 5.0F, 7.5F)
                        .texOffs(0, 0).addBox(3.55F, -6.8F, -3.3F, 0.9F, 5.0F, 7.5F)
                        .texOffs(0, 0).addBox(-4.25F, -6.8F, 3.4F, 8.5F, 5.5F, 1.0F)
                        .texOffs(0, 0).addBox(-4.2F, -6.8F, -4.38F, 3.0F, 1.0F, 0.6F)
                        .texOffs(0, 0).addBox(1.2F, -6.8F, -4.38F, 3.0F, 1.0F, 0.6F)
                        .texOffs(0, 0).addBox(-4.5F, -5.8F, -4.25F, 0.95F, 6.5F, 1.1F)
                        .texOffs(0, 0).addBox(3.55F, -5.8F, -4.25F, 0.95F, 6.5F, 1.1F)
                        .texOffs(0, 0).addBox(-1.8F, -7.2F, 4.0F, 3.6F, 2.6F, 1.6F)
                        .texOffs(0, 0).addBox(-2.0F, -5.0F, 4.4F, 4.0F, 5.2F, 1.6F)
                        .texOffs(0, 0).addBox(-1.4F, 0.0F, 4.5F, 2.8F, 3.2F, 1.3F),
                PartPose.ZERO);

        root.addOrReplaceChild("fem_hair_2",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.4F, -8.85F, -4.3F, 8.8F, 2.25F, 8.6F)
                        .texOffs(0, 0).addBox(-3.6F, -9.2F, -3.5F, 7.2F, 0.45F, 7.2F)
                        .texOffs(0, 0).addBox(-4.2F, -6.8F, -4.42F, 4.6F, 1.15F, 0.65F)
                        .texOffs(0, 0).addBox(1.2F, -6.8F, -4.42F, 3.0F, 0.95F, 0.65F)
                        .texOffs(0, 0).addBox(-4.6F, -6.8F, -3.9F, 1.1F, 6.6F, 8.1F)
                        .texOffs(0, 0).addBox(3.5F, -6.8F, -3.9F, 1.1F, 6.6F, 8.1F)
                        .texOffs(0, 0).addBox(-4.3F, -6.8F, 3.45F, 8.6F, 6.8F, 1.15F),
                PartPose.ZERO);

        root.addOrReplaceChild("fem_hair_3",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.35F, -8.8F, -4.25F, 8.7F, 2.2F, 8.5F)
                        .texOffs(0, 0).addBox(-3.5F, -9.15F, -3.5F, 7.0F, 0.45F, 7.2F)
                        .texOffs(0, 0).addBox(-4.5F, -6.8F, -3.5F, 0.95F, 5.5F, 7.7F)
                        .texOffs(0, 0).addBox(3.55F, -6.8F, -3.5F, 0.95F, 5.5F, 7.7F)
                        .texOffs(0, 0).addBox(-4.25F, -6.8F, 3.4F, 8.5F, 6.2F, 1.05F)
                        .texOffs(0, 0).addBox(-4.2F, -6.8F, -4.4F, 3.2F, 1.05F, 0.6F)
                        .texOffs(0, 0).addBox(1.0F, -6.8F, -4.4F, 3.2F, 1.05F, 0.6F)
                        .texOffs(0, 0).addBox(-4.55F, -2.2F, -3.8F, 1.2F, 3.5F, 1.4F)
                        .texOffs(0, 0).addBox(-4.4F, 1.1F, -4.0F, 1.1F, 3.2F, 1.2F)
                        .texOffs(0, 0).addBox(-4.25F, 4.1F, -3.9F, 0.9F, 1.8F, 1.0F)
                        .texOffs(0, 0).addBox(3.35F, -2.2F, -3.8F, 1.2F, 3.5F, 1.4F)
                        .texOffs(0, 0).addBox(3.3F, 1.1F, -4.0F, 1.1F, 3.2F, 1.2F)
                        .texOffs(0, 0).addBox(3.35F, 4.1F, -3.9F, 0.9F, 1.8F, 1.0F),
                PartPose.ZERO);

        root.addOrReplaceChild("fem_hair_4",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.35F, -8.8F, -4.25F, 8.7F, 2.2F, 8.5F)
                        .texOffs(0, 0).addBox(-3.6F, -9.2F, -3.5F, 7.2F, 0.5F, 7.4F)
                        .texOffs(0, 0).addBox(-4.5F, -6.8F, -3.4F, 0.95F, 5.2F, 7.6F)
                        .texOffs(0, 0).addBox(3.55F, -6.8F, -3.4F, 0.95F, 5.2F, 7.6F)
                        .texOffs(0, 0).addBox(-4.25F, -6.8F, 3.4F, 8.5F, 5.8F, 1.05F)
                        .texOffs(0, 0).addBox(-4.1F, -6.8F, -4.38F, 2.8F, 0.95F, 0.55F)
                        .texOffs(0, 0).addBox(1.3F, -6.8F, -4.38F, 2.8F, 0.95F, 0.55F)
                        .texOffs(0, 0).addBox(-4.5F, -5.8F, -4.2F, 0.95F, 6.2F, 1.0F)
                        .texOffs(0, 0).addBox(3.55F, -5.8F, -4.2F, 0.95F, 6.2F, 1.0F)
                        .texOffs(0, 0).addBox(-2.2F, -6.2F, 4.1F, 4.4F, 3.8F, 1.6F)
                        .texOffs(0, 0).addBox(-1.6F, -5.6F, 5.4F, 3.2F, 2.6F, 0.8F),
                PartPose.ZERO);

        root.addOrReplaceChild("beard_0", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("beard_1", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("beard_2", CubeListBuilder.create(), PartPose.ZERO);

        root.addOrReplaceChild("beard_3",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.5F, -2.5F, -4.4F, 5.0F, 1.0F, 0.7F)
                        .texOffs(0, 0).addBox(-2.8F, -1.8F, -4.35F, 1.2F, 1.0F, 0.6F)
                        .texOffs(0, 0).addBox(1.6F, -1.8F, -4.35F, 1.2F, 1.0F, 0.6F),
                PartPose.ZERO);

        root.addOrReplaceChild("beard_4",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.5F, -2.5F, -4.4F, 5.0F, 1.0F, 0.7F)
                        .texOffs(0, 0).addBox(-2.5F, -1.5F, -4.35F, 0.9F, 1.2F, 0.6F)
                        .texOffs(0, 0).addBox(1.6F, -1.5F, -4.35F, 0.9F, 1.2F, 0.6F)
                        .texOffs(0, 0).addBox(-2.0F, -0.8F, -4.45F, 4.0F, 1.3F, 1.2F)
                        .texOffs(0, 0).addBox(-1.4F, 0.3F, -4.4F, 2.8F, 1.0F, 1.0F),
                PartPose.ZERO);

        root.addOrReplaceChild("beard_5",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.25F, -4.0F, -3.8F, 0.7F, 4.0F, 2.4F)
                        .texOffs(0, 0).addBox(3.55F, -4.0F, -3.8F, 0.7F, 4.0F, 2.4F)
                        .texOffs(0, 0).addBox(-2.6F, -2.5F, -4.4F, 5.2F, 1.0F, 0.7F)
                        .texOffs(0, 0).addBox(-4.2F, -1.5F, -4.35F, 8.4F, 1.8F, 2.8F)
                        .texOffs(0, 0).addBox(-3.2F, 0.2F, -4.35F, 6.4F, 1.0F, 2.2F),
                PartPose.ZERO);

        root.addOrReplaceChild("beard_6",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.35F, -4.5F, -3.8F, 0.9F, 4.5F, 3.0F)
                        .texOffs(0, 0).addBox(3.45F, -4.5F, -3.8F, 0.9F, 4.5F, 3.0F)
                        .texOffs(0, 0).addBox(-4.25F, -1.6F, -4.4F, 8.5F, 2.2F, 3.2F)
                        .texOffs(0, 0).addBox(-3.4F, 0.5F, -4.45F, 6.8F, 1.5F, 2.6F),
                PartPose.ZERO);

        root.addOrReplaceChild("beard_7",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.35F, -4.2F, -3.8F, 0.9F, 4.2F, 3.0F)
                        .texOffs(0, 0).addBox(3.45F, -4.2F, -3.8F, 0.9F, 4.2F, 3.0F)
                        .texOffs(0, 0).addBox(-2.8F, -2.6F, -4.5F, 5.6F, 1.2F, 0.9F)
                        .texOffs(0, 0).addBox(-4.3F, -1.6F, -4.45F, 8.6F, 2.5F, 3.2F)
                        .texOffs(0, 0).addBox(-3.6F, 0.8F, -4.4F, 7.2F, 2.0F, 2.6F)
                        .texOffs(0, 0).addBox(-2.6F, 2.6F, -4.3F, 5.2F, 1.2F, 2.0F),
                PartPose.ZERO);

        root.addOrReplaceChild("beard_8",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.4F, -4.5F, -3.8F, 0.95F, 4.5F, 3.2F)
                        .texOffs(0, 0).addBox(3.45F, -4.5F, -3.8F, 0.95F, 4.5F, 3.2F)
                        .texOffs(0, 0).addBox(-3.0F, -2.6F, -4.55F, 6.0F, 1.3F, 1.0F)
                        .texOffs(0, 0).addBox(-4.35F, -1.6F, -4.5F, 8.7F, 2.8F, 3.4F)
                        .texOffs(0, 0).addBox(-3.8F, 1.0F, -4.45F, 7.6F, 2.4F, 2.8F)
                        .texOffs(0, 0).addBox(-3.0F, 3.2F, -4.35F, 6.0F, 2.0F, 2.2F)
                        .texOffs(0, 0).addBox(-2.0F, 5.0F, -4.25F, 4.0F, 1.4F, 1.8F),
                PartPose.ZERO);

        root.addOrReplaceChild("beard_9",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.45F, -4.5F, -3.8F, 1.0F, 4.5F, 3.4F)
                        .texOffs(0, 0).addBox(3.45F, -4.5F, -3.8F, 1.0F, 4.5F, 3.4F)
                        .texOffs(0, 0).addBox(-3.2F, -2.7F, -4.6F, 6.4F, 1.4F, 1.1F)
                        .texOffs(0, 0).addBox(-4.4F, -1.6F, -4.55F, 8.8F, 3.0F, 3.5F)
                        .texOffs(0, 0).addBox(-4.0F, 1.2F, -4.5F, 8.0F, 2.8F, 3.0F)
                        .texOffs(0, 0).addBox(-3.4F, 3.8F, -4.4F, 6.8F, 2.5F, 2.4F)
                        .texOffs(0, 0).addBox(-2.6F, 6.1F, -4.3F, 5.2F, 2.0F, 2.0F),
                PartPose.ZERO);

        ModelPart baked32 = root.bake(64, 32);
        ModelPart baked64 = root.bake(64, 64);

        this.chestMesh = baked64.getChild("chest");
        this.armoredChestMesh = baked32.getChild("armored_chest");
        this.elfEarLeft = baked64.getChild("elf_ear_left");
        this.elfEarRight = baked64.getChild("elf_ear_right");

        this.orcTusks = baked64.getChild("orc_tusks");
        this.orcEarLeft = baked64.getChild("orc_ear_left");
        this.orcEarRight = baked64.getChild("orc_ear_right");

        this.dragonSnoutMale = baked64.getChild("dragon_snout_male");
        this.dragonSnoutFemale = baked64.getChild("dragon_snout_female");
        this.dragonHorns = baked64.getChild("dragon_horns");
        this.dragonSpines = baked64.getChild("dragon_spines");
        this.dragonTail = baked64.getChild("dragon_tail");

        for (int i = 0; i < 7; i++) {
            this.maleHairMeshes[i] = baked64.getChild("male_hair_" + i);
        }
        for (int i = 0; i < 5; i++) {
            this.femaleHairMeshes[i] = baked64.getChild("fem_hair_" + i);
        }
        for (int i = 0; i < 10; i++) {
            this.beardMeshes[i] = baked64.getChild("beard_" + i);
        }
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {

        if (player.isInvisible()) return;

        boolean firstPerson = CameraSystem.renderingFirstPersonBody && player == Minecraft.getInstance().player;

        // Если игрок в нокауте или сидит в вагонетке — выставляем кастомную позу суставов и рисуем тело скина
        // (с обязательным скрытием головы при виде от 1-го лица, чтобы камера не заглядывала внутрь головы!)
        if (player.getData(ModAttachments.DOWNED_TICKS) > 0 || (player.isPassenger() && player.getVehicle() instanceof AbstractMinecart)) {
            ClientEvents.applyBattleRoyaleDownedLimbs(player, this.getParentModel(), limbSwing, limbSwingAmount, ageInTicks);
            this.getParentModel().setAllVisible(true);
            if (firstPerson) {
                this.getParentModel().head.visible = false;
                this.getParentModel().hat.visible = false;
            }
            ResourceLocation skin = CustomSkinManager.getOrCreateSkin(player.getData(ModAttachments.APPEARANCE));
            VertexConsumer bodyVc = buffer.getBuffer(RenderType.entityTranslucent(skin));
            this.getParentModel().renderToBuffer(poseStack, bodyVc, packedLight, OverlayTexture.NO_OVERLAY);
        } else if (firstPerson) {
            this.getParentModel().head.visible = false;
            this.getParentModel().hat.visible = false;
        }

        PlayerAppearanceData data = player.getData(ModAttachments.APPEARANCE);
        ExtraEquipmentData extraEq = player.getData(ModAttachments.EXTRA_EQUIPMENT);

        ItemStack headArmor = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chestArmor = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legsArmor = player.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack feetArmor = player.getItemBySlot(EquipmentSlot.FEET);

        ItemStack underHat = extraEq.getStack(ExtraEquipmentData.SLOT_HAT_UNDER);
        ItemStack underShirt = extraEq.getStack(ExtraEquipmentData.SLOT_SHIRT_UNDER);
        ItemStack underPants = extraEq.getStack(ExtraEquipmentData.SLOT_PANTS_UNDER);
        ItemStack underShoes = extraEq.getStack(ExtraEquipmentData.SLOT_SHOES_UNDER);
        ItemStack capeStack = extraEq.getStack(ExtraEquipmentData.SLOT_CAPE);
        ItemStack backpackStack = extraEq.getStack(ExtraEquipmentData.SLOT_BACKPACK);

        boolean wearingCloak = !capeStack.isEmpty() && capeStack.getItem() instanceof HoodedCloakItem;
        boolean hoodUp = wearingCloak && player.getData(ModAttachments.HOOD_UP);

        if (!firstPerson && !hoodUp && headArmor.isEmpty() && !underHat.isEmpty() && underHat.getItem() instanceof PeasantClothesItem) {
            PeasantClothesRenderer.renderPiece(poseStack, buffer, player, ExtraEquipmentData.SLOT_HAT_UNDER, packedLight, this.getParentModel());
        }
        if (chestArmor.isEmpty() && !underShirt.isEmpty() && underShirt.getItem() instanceof PeasantClothesItem) {
            PeasantClothesRenderer.renderPiece(poseStack, buffer, player, ExtraEquipmentData.SLOT_SHIRT_UNDER, packedLight, this.getParentModel());
        }
        if (legsArmor.isEmpty() && !underPants.isEmpty() && underPants.getItem() instanceof PeasantClothesItem) {
            PeasantClothesRenderer.renderPiece(poseStack, buffer, player, ExtraEquipmentData.SLOT_PANTS_UNDER, packedLight, this.getParentModel());
        }
        if (feetArmor.isEmpty() && !underShoes.isEmpty() && underShoes.getItem() instanceof PeasantClothesItem) {
            PeasantClothesRenderer.renderPiece(poseStack, buffer, player, ExtraEquipmentData.SLOT_SHOES_UNDER, packedLight, this.getParentModel());
        }

        boolean wearingBackpack = !backpackStack.isEmpty() && backpackStack.getItem() instanceof BackpackItem;
        if (wearingBackpack && backpackStack.getItem() instanceof BackpackItem bp) {
            PeasantClothesRenderer.renderBackpack(poseStack, buffer, player, bp.getBackpackTier(), packedLight, this.getParentModel());
        }

        if (wearingCloak) {
            PeasantClothesRenderer.renderCloak(poseStack, buffer, player, hoodUp, firstPerson, limbSwing, limbSwingAmount, packedLight, this.getParentModel());
        }

        boolean wearingPeasantShirt = (chestArmor.getItem() instanceof PeasantClothesItem)
                || (chestArmor.isEmpty() && !underShirt.isEmpty() && underShirt.getItem() instanceof PeasantClothesItem);

        if (!firstPerson && !hoodUp && headArmor.isEmpty() && (data.race() == RaceType.ELF || data.race() == RaceType.ORC)) {
            ResourceLocation skinLoc = CustomSkinManager.getOrCreateSkin(data);
            VertexConsumer skinVc = buffer.getBuffer(RenderType.entityCutoutNoCull(skinLoc));
            poseStack.pushPose();
            this.getParentModel().head.translateAndRotate(poseStack);
            if (data.race() == RaceType.ELF) {
                this.elfEarLeft.render(poseStack, skinVc, packedLight, OverlayTexture.NO_OVERLAY);
                this.elfEarRight.render(poseStack, skinVc, packedLight, OverlayTexture.NO_OVERLAY);
            } else {
                this.orcEarLeft.render(poseStack, skinVc, packedLight, OverlayTexture.NO_OVERLAY);
                this.orcEarRight.render(poseStack, skinVc, packedLight, OverlayTexture.NO_OVERLAY);
            }
            poseStack.popPose();
        }

        if (data.gender() == Gender.FEMALE && !wearingPeasantShirt) {
            if (chestArmor.isEmpty()) {
                ResourceLocation skinLoc = CustomSkinManager.getOrCreateSkin(data);
                VertexConsumer skinVc = buffer.getBuffer(RenderType.entityCutoutNoCull(skinLoc));

                poseStack.pushPose();
                this.getParentModel().body.translateAndRotate(poseStack);
                poseStack.mulPose(Axis.XP.rotationDegrees(-8.0F));
                this.chestMesh.render(poseStack, skinVc, packedLight, OverlayTexture.NO_OVERLAY);
                poseStack.popPose();
            } else if (chestArmor.getItem() instanceof ArmorItem armorItem) {
                Holder<ArmorMaterial> matHolder = armorItem.getMaterial();
                List<ArmorMaterial.Layer> layers = matHolder.value().layers();
                int defaultColor = chestArmor.is(ItemTags.DYEABLE) ? FastColor.ARGB32.opaque(DyedItemColor.getOrDefault(chestArmor, -6265536)) : -1;

                poseStack.pushPose();
                this.getParentModel().body.translateAndRotate(poseStack);
                poseStack.mulPose(Axis.XP.rotationDegrees(-6.0F));
                for (ArmorMaterial.Layer layer : layers) {
                    ResourceLocation armorTex = ClientHooks.getArmorTexture(player, chestArmor, layer, false, EquipmentSlot.CHEST);
                    int color = layer.dyeable() ? defaultColor : -1;
                    VertexConsumer armorVc = ItemRenderer.getArmorFoilBuffer(buffer, RenderType.armorCutoutNoCull(armorTex), chestArmor.hasFoil());
                    this.armoredChestMesh.render(poseStack, armorVc, packedLight, OverlayTexture.NO_OVERLAY, color);
                }
                poseStack.popPose();
            }
        }

        ResourceLocation extrasLoc = CustomSkinManager.getOrCreateExtrasTexture(data);
        VertexConsumer extrasVc = buffer.getBuffer(RenderType.entityCutoutNoCull(extrasLoc));

        if (!firstPerson && data.race() == RaceType.ORC && data.faceVariant() < 5) {
            poseStack.pushPose();
            this.getParentModel().head.translateAndRotate(poseStack);
            this.orcTusks.render(poseStack, extrasVc, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }

        if (data.race() == RaceType.DRAGONBORN) {
            if (!firstPerson) {
                boolean isFemale = (data.gender() == Gender.FEMALE);
                poseStack.pushPose();
                this.getParentModel().head.translateAndRotate(poseStack);
                if (isFemale) {
                    this.dragonSnoutFemale.render(poseStack, extrasVc, packedLight, OverlayTexture.NO_OVERLAY);
                } else {
                    this.dragonSnoutMale.render(poseStack, extrasVc, packedLight, OverlayTexture.NO_OVERLAY);
                }
                if (headArmor.isEmpty() && !hoodUp) {
                    poseStack.pushPose();
                    if (isFemale) {
                        poseStack.scale(0.88F, 0.92F, 0.92F);
                    }
                    this.dragonHorns.render(poseStack, extrasVc, packedLight, OverlayTexture.NO_OVERLAY);
                    poseStack.popPose();
                }
                poseStack.popPose();
            }

            poseStack.pushPose();
            this.getParentModel().body.translateAndRotate(poseStack);
            if (chestArmor.isEmpty() && !wearingPeasantShirt && !wearingBackpack && !wearingCloak) {
                this.dragonSpines.render(poseStack, extrasVc, packedLight, OverlayTexture.NO_OVERLAY);
            }
            this.dragonTail.yRot = Mth.sin(ageInTicks * 0.07F) * 0.10F;
            this.dragonTail.render(poseStack, extrasVc, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
            return;
        }

        if (firstPerson) return;

        boolean wearingAnyHat = !headArmor.isEmpty() || !underHat.isEmpty() || hoodUp;
        if (!wearingAnyHat) {
            poseStack.pushPose();
            this.getParentModel().head.translateAndRotate(poseStack);
            if (data.gender() == Gender.FEMALE) {
                int hIdx = Math.floorMod(data.hairStyle(), 5);
                this.femaleHairMeshes[hIdx].render(poseStack, extrasVc, packedLight, OverlayTexture.NO_OVERLAY);
            } else {
                int hIdx = Math.floorMod(data.hairStyle(), 7);
                if (hIdx > 0) {
                    this.maleHairMeshes[hIdx].render(poseStack, extrasVc, packedLight, OverlayTexture.NO_OVERLAY);
                }
            }
            poseStack.popPose();
        }

        if (data.gender() == Gender.MALE) {
            int bIdx = Math.floorMod(data.beardStyle(), 10);
            if (bIdx >= 3) {
                poseStack.pushPose();
                this.getParentModel().head.translateAndRotate(poseStack);
                this.beardMeshes[bIdx].render(poseStack, extrasVc, packedLight, OverlayTexture.NO_OVERLAY);
                poseStack.popPose();
            }
        }
    }

    public void renderCorpseAppearanceAndApparel(
            PoseStack poseStack, MultiBufferSource buffer, int packedLight,
            PlayerModel<?> model, PlayerAppearanceData data,
            ItemStack headArmor, ItemStack chestArmor, ItemStack legsArmor, ItemStack feetArmor,
            ItemStack underHat, ItemStack underShirt, ItemStack underPants, ItemStack underShoes,
            ItemStack capeStack, ItemStack backpackStack) {

        boolean isFemale = (data.gender() == Gender.FEMALE);
        boolean wearingCloak = !capeStack.isEmpty() && capeStack.getItem() instanceof HoodedCloakItem;
        boolean hoodUp = wearingCloak;

        if (!hoodUp && headArmor.isEmpty() && !underHat.isEmpty() && underHat.getItem() instanceof PeasantClothesItem) {
            PeasantClothesRenderer.renderPiece(poseStack, buffer, null, ExtraEquipmentData.SLOT_HAT_UNDER, packedLight, model, isFemale);
        }
        if (chestArmor.isEmpty() && !underShirt.isEmpty() && underShirt.getItem() instanceof PeasantClothesItem) {
            PeasantClothesRenderer.renderPiece(poseStack, buffer, null, ExtraEquipmentData.SLOT_SHIRT_UNDER, packedLight, model, isFemale);
        }
        if (legsArmor.isEmpty() && !underPants.isEmpty() && underPants.getItem() instanceof PeasantClothesItem) {
            PeasantClothesRenderer.renderPiece(poseStack, buffer, null, ExtraEquipmentData.SLOT_PANTS_UNDER, packedLight, model, isFemale);
        }
        if (feetArmor.isEmpty() && !underShoes.isEmpty() && underShoes.getItem() instanceof PeasantClothesItem) {
            PeasantClothesRenderer.renderPiece(poseStack, buffer, null, ExtraEquipmentData.SLOT_SHOES_UNDER, packedLight, model, isFemale);
        }

        if (!backpackStack.isEmpty() && backpackStack.getItem() instanceof BackpackItem bp) {
            PeasantClothesRenderer.renderBackpack(poseStack, buffer, null, bp.getBackpackTier(), packedLight, model);
        }
        if (wearingCloak) {
            PeasantClothesRenderer.renderCloak(poseStack, buffer, null, true, false, 0.0F, 0.0F, packedLight, model);
        }

        boolean wearingPeasantShirt = (chestArmor.getItem() instanceof PeasantClothesItem)
                || (chestArmor.isEmpty() && !underShirt.isEmpty() && underShirt.getItem() instanceof PeasantClothesItem);

        ResourceLocation skinLoc = CustomSkinManager.getOrCreateSkin(data);
        VertexConsumer skinVc = buffer.getBuffer(RenderType.entityCutoutNoCull(skinLoc));

        if (!hoodUp && headArmor.isEmpty() && (data.race() == RaceType.ELF || data.race() == RaceType.ORC)) {
            poseStack.pushPose();
            model.head.translateAndRotate(poseStack);
            if (data.race() == RaceType.ELF) {
                this.elfEarLeft.render(poseStack, skinVc, packedLight, OverlayTexture.NO_OVERLAY);
                this.elfEarRight.render(poseStack, skinVc, packedLight, OverlayTexture.NO_OVERLAY);
            } else {
                this.orcEarLeft.render(poseStack, skinVc, packedLight, OverlayTexture.NO_OVERLAY);
                this.orcEarRight.render(poseStack, skinVc, packedLight, OverlayTexture.NO_OVERLAY);
            }
            poseStack.popPose();
        }

        if (isFemale && !wearingPeasantShirt) {
            if (chestArmor.isEmpty()) {
                poseStack.pushPose();
                model.body.translateAndRotate(poseStack);
                poseStack.mulPose(Axis.XP.rotationDegrees(-8.0F));
                this.chestMesh.render(poseStack, skinVc, packedLight, OverlayTexture.NO_OVERLAY);
                poseStack.popPose();
            } else if (chestArmor.getItem() instanceof ArmorItem armorItem) {
                Holder<ArmorMaterial> matHolder = armorItem.getMaterial();
                List<ArmorMaterial.Layer> layers = matHolder.value().layers();
                int defaultColor = chestArmor.is(ItemTags.DYEABLE) ? FastColor.ARGB32.opaque(DyedItemColor.getOrDefault(chestArmor, -6265536)) : -1;

                poseStack.pushPose();
                model.body.translateAndRotate(poseStack);
                poseStack.mulPose(Axis.XP.rotationDegrees(-6.0F));
                for (ArmorMaterial.Layer layer : layers) {
                    ResourceLocation armorTex = layer.texture(false);
                    int color = layer.dyeable() ? defaultColor : -1;
                    VertexConsumer armorVc = ItemRenderer.getArmorFoilBuffer(buffer, RenderType.armorCutoutNoCull(armorTex), chestArmor.hasFoil());
                    this.armoredChestMesh.render(poseStack, armorVc, packedLight, OverlayTexture.NO_OVERLAY, color);
                }
                poseStack.popPose();
            }
        }

        ResourceLocation extrasLoc = CustomSkinManager.getOrCreateExtrasTexture(data);
        VertexConsumer extrasVc = buffer.getBuffer(RenderType.entityCutoutNoCull(extrasLoc));

        if (data.race() == RaceType.ORC && data.faceVariant() < 5) {
            poseStack.pushPose();
            model.head.translateAndRotate(poseStack);
            this.orcTusks.render(poseStack, extrasVc, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }

        if (data.race() == RaceType.DRAGONBORN) {
            poseStack.pushPose();
            model.head.translateAndRotate(poseStack);
            if (isFemale) this.dragonSnoutFemale.render(poseStack, extrasVc, packedLight, OverlayTexture.NO_OVERLAY);
            else this.dragonSnoutMale.render(poseStack, extrasVc, packedLight, OverlayTexture.NO_OVERLAY);
            if (headArmor.isEmpty() && !hoodUp) {
                this.dragonHorns.render(poseStack, extrasVc, packedLight, OverlayTexture.NO_OVERLAY);
            }
            poseStack.popPose();

            poseStack.pushPose();
            model.body.translateAndRotate(poseStack);
            this.dragonTail.render(poseStack, extrasVc, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
            return;
        }

        boolean wearingAnyHat = !headArmor.isEmpty() || !underHat.isEmpty() || hoodUp;
        if (!wearingAnyHat) {
            poseStack.pushPose();
            model.head.translateAndRotate(poseStack);
            if (isFemale) {
                int hIdx = Math.floorMod(data.hairStyle(), 5);
                this.femaleHairMeshes[hIdx].render(poseStack, extrasVc, packedLight, OverlayTexture.NO_OVERLAY);
            } else {
                int hIdx = Math.floorMod(data.hairStyle(), 7);
                if (hIdx > 0) this.maleHairMeshes[hIdx].render(poseStack, extrasVc, packedLight, OverlayTexture.NO_OVERLAY);
            }
            poseStack.popPose();
        }

        if (!isFemale) {
            int bIdx = Math.floorMod(data.beardStyle(), 10);
            if (bIdx >= 3) {
                poseStack.pushPose();
                model.head.translateAndRotate(poseStack);
                this.beardMeshes[bIdx].render(poseStack, extrasVc, packedLight, OverlayTexture.NO_OVERLAY);
                poseStack.popPose();
            }
        }
    }
}