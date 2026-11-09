package com.rpgcore.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rpgcore.RPGCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@EventBusSubscriber(modid = RPGCore.MODID, value = Dist.CLIENT)
public class FallingLeavesSystem {

    private static final ResourceLocation WHITE_TEX =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    private static class FallingLeaf {
        double x, y, z;
        double prevX, prevY, prevZ;
        double vx, vy, vz;
        float rotX, rotY, rotZ;
        float rotSpeedX, rotSpeedY, rotSpeedZ;
        float swayPhase;
        float scale;
        int colorRgb;
        int age;
        int maxAge;
        boolean onGround;

        FallingLeaf(double x, double y, double z, int colorRgb, RandomSource rand) {
            this.x = this.prevX = x;
            this.y = this.prevY = y;
            this.z = this.prevZ = z;
            this.vy = -0.028D - rand.nextDouble() * 0.018D;
            this.vx = (rand.nextDouble() - 0.5D) * 0.02D;
            this.vz = (rand.nextDouble() - 0.5D) * 0.02D;
            this.rotX = rand.nextFloat() * 360.0F;
            this.rotY = rand.nextFloat() * 360.0F;
            this.rotZ = rand.nextFloat() * 360.0F;
            this.rotSpeedX = (rand.nextFloat() - 0.5F) * 5.0F;
            this.rotSpeedY = (rand.nextFloat() - 0.5F) * 7.0F;
            this.rotSpeedZ = (rand.nextFloat() - 0.5F) * 5.0F;
            this.swayPhase = rand.nextFloat() * 6.28F;
            this.scale = 0.032F + rand.nextFloat() * 0.014F;
            this.colorRgb = colorRgb;
            this.age = 0;
            this.maxAge = 120 + rand.nextInt(80);
            this.onGround = false;
        }
    }

    private static final List<FallingLeaf> LEAVES = new ArrayList<>();
    private static final RandomSource RANDOM = RandomSource.create();

    /**
     * Точное определение цвета листвы под каждый вид дерева (дуб, береза, ель, вишня, азалия, мангр, тропическое, акация, темный дуб)
     */
    public static int resolveLeafColor(Level level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        int baseColor;

        if (block == Blocks.BIRCH_LEAVES) {
            baseColor = 0x80A755; // Классический цвет березовой листвы
        } else if (block == Blocks.SPRUCE_LEAVES) {
            baseColor = 0x619961; // Хвойный цвет еловой листвы
        } else if (block == Blocks.CHERRY_LEAVES) {
            baseColor = (RANDOM.nextInt(3) == 0) ? 0xF29BC0 : 0xFAD1E6; // Нежно-розовые лепестки вишни
        } else if (block == Blocks.AZALEA_LEAVES) {
            baseColor = 0x5E8F32;
        } else if (block == Blocks.FLOWERING_AZALEA_LEAVES) {
            baseColor = (RANDOM.nextInt(3) == 0) ? 0xBD65A4 : 0x5E8F32;
        } else {
            // Для дуба, тропического дерева, акации, темного дуба и мангра берем точный биомный оттенок листвы
            // и умножаем на базовый тон текстуры блока листьев (чтобы листья не были слишком кислотными)
            int biomeFoliage = BiomeColors.getAverageFoliageColor(level, pos);
            baseColor = darkenOrTint(biomeFoliage, 0.86F);
        }

        // Небольшая естественная вариация оттенка между отдельными листочками (±6%)
        float variation = 0.94F + RANDOM.nextFloat() * 0.12F;
        return darkenOrTint(baseColor, variation);
    }

    private static int darkenOrTint(int rgb, float factor) {
        int r = Mth.clamp(Math.round(((rgb >> 16) & 0xFF) * factor), 0, 255);
        int g = Mth.clamp(Math.round(((rgb >> 8) & 0xFF) * factor), 0, 255);
        int b = Mth.clamp(Math.round((rgb & 0xFF) * factor), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused()) return;
        Level level = mc.level;
        if (level == null || mc.player == null) {
            LEAVES.clear();
            return;
        }

        BlockPos playerPos = mc.player.blockPosition();
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();

        // Спавним падающие листья под кронами деревьев в радиусе 22 блоков
        if (LEAVES.size() < 260) {
            for (int i = 0; i < 18; i++) {
                int dx = RANDOM.nextInt(45) - 22;
                int dy = RANDOM.nextInt(21) - 6;
                int dz = RANDOM.nextInt(45) - 22;

                checkPos.set(playerPos.getX() + dx, playerPos.getY() + dy, playerPos.getZ() + dz);
                BlockState state = level.getBlockState(checkPos);

                if (state.is(BlockTags.LEAVES)) {
                    BlockPos belowPos = checkPos.below();
                    if (level.getBlockState(belowPos).isAir()) {
                        double lx = checkPos.getX() + 0.15D + RANDOM.nextDouble() * 0.70D;
                        double ly = checkPos.getY() - 0.05D;
                        double lz = checkPos.getZ() + 0.15D + RANDOM.nextDouble() * 0.70D;
                        int leafColor = resolveLeafColor(level, checkPos, state);
                        LEAVES.add(new FallingLeaf(lx, ly, lz, leafColor, RANDOM));
                    }
                }
            }
        }

        // Обновляем полет и кружение листьев на ветру
        float windTime = level.getGameTime() * 0.04F;
        double globalWindX = Mth.sin(windTime) * 0.012D;
        double globalWindZ = Mth.cos(windTime * 0.7F) * 0.012D;

        Iterator<FallingLeaf> it = LEAVES.iterator();
        while (it.hasNext()) {
            FallingLeaf leaf = it.next();
            leaf.prevX = leaf.x;
            leaf.prevY = leaf.y;
            leaf.prevZ = leaf.z;
            leaf.age++;

            if (leaf.age >= leaf.maxAge) {
                it.remove();
                continue;
            }

            if (!leaf.onGround) {
                float sway = leaf.age * 0.11F + leaf.swayPhase;
                double swayX = Mth.sin(sway) * 0.022D + globalWindX;
                double swayZ = Mth.cos(sway * 0.85F) * 0.022D + globalWindZ;

                leaf.x += leaf.vx + swayX;
                leaf.y += leaf.vy;
                leaf.z += leaf.vz + swayZ;

                leaf.rotX += leaf.rotSpeedX + Mth.sin(sway) * 2.5F;
                leaf.rotY += leaf.rotSpeedY;
                leaf.rotZ += leaf.rotSpeedZ + Mth.cos(sway) * 2.5F;

                checkPos.set(Mth.floor(leaf.x), Mth.floor(leaf.y - 0.06D), Mth.floor(leaf.z));
                BlockState hitState = level.getBlockState(checkPos);
                if (!hitState.isAir() && !hitState.is(BlockTags.LEAVES)) {
                    if (!hitState.getFluidState().isEmpty()) {
                        // На воде листок плавно качается на поверхности
                        leaf.onGround = true;
                        leaf.y = checkPos.getY() + 0.92D;
                    } else if (hitState.isSolidRender(level, checkPos) || !hitState.getCollisionShape(level, checkPos).isEmpty()) {
                        leaf.onGround = true;
                        leaf.y = checkPos.getY() + 1.02D;
                        leaf.rotX = 90.0F;
                        leaf.rotZ = 0.0F;
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || LEAVES.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        Vec3 camPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(WHITE_TEX));

        BlockPos.MutableBlockPos lightPos = new BlockPos.MutableBlockPos();

        for (FallingLeaf leaf : LEAVES) {
            double lx = Mth.lerp(pt, leaf.prevX, leaf.x) - camPos.x;
            double ly = Mth.lerp(pt, leaf.prevY, leaf.y) - camPos.y;
            double lz = Mth.lerp(pt, leaf.prevZ, leaf.z) - camPos.z;

            if (lx * lx + ly * ly + lz * lz > 32 * 32) continue;

            lightPos.set(Mth.floor(leaf.x), Mth.floor(leaf.y), Mth.floor(leaf.z));
            int light = LevelRenderer.getLightColor(level, lightPos);

            float rx = leaf.onGround ? 90.0F : (leaf.rotX + leaf.rotSpeedX * pt);
            float ry = leaf.rotY + (leaf.onGround ? 0.0F : leaf.rotSpeedY * pt);
            float rz = leaf.onGround ? 0.0F : (leaf.rotZ + leaf.rotSpeedZ * pt);

            int r = (leaf.colorRgb >> 16) & 0xFF;
            int g = (leaf.colorRgb >> 8) & 0xFF;
            int b = leaf.colorRgb & 0xFF;

            int rDark = Math.max(0, Math.round(r * 0.78F));
            int gDark = Math.max(0, Math.round(g * 0.78F));
            int bDark = Math.max(0, Math.round(b * 0.78F));

            poseStack.pushPose();
            poseStack.translate(lx, ly, lz);
            poseStack.mulPose(new Quaternionf().rotationXYZ(
                    rx * Mth.DEG_TO_RAD,
                    ry * Mth.DEG_TO_RAD,
                    rz * Mth.DEG_TO_RAD
            ));
            poseStack.scale(leaf.scale, leaf.scale, leaf.scale);

            PoseStack.Pose pose = poseStack.last();
            // Рисуем ступенчатый пиксельный листок из 3 сегментов (точь-в-точь как на image_a6c61f.jpg!)
            drawPixelQuad(consumer, pose, -1.5F, -0.5F, 0.5F, 1.5F, r, g, b, light);
            drawPixelQuad(consumer, pose, -0.5F, -1.5F, 1.5F, 0.5F, rDark, gDark, bDark, light);
            drawPixelQuad(consumer, pose, -2.2F, 0.2F, -0.5F, 2.0F, rDark, gDark, bDark, light);

            poseStack.popPose();
        }
    }

    private static void drawPixelQuad(VertexConsumer vc, PoseStack.Pose pose,
                                      float x1, float y1, float x2, float y2,
                                      int r, int g, int b, int light) {
        vc.addVertex(pose, x1, y1, 0.0F).setColor(r, g, b, 255).setUv(0.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 1.0F, 0.0F);
        vc.addVertex(pose, x2, y1, 0.0F).setColor(r, g, b, 255).setUv(1.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 1.0F, 0.0F);
        vc.addVertex(pose, x2, y2, 0.0F).setColor(r, g, b, 255).setUv(1.0F, 1.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 1.0F, 0.0F);
        vc.addVertex(pose, x1, y2, 0.0F).setColor(r, g, b, 255).setUv(0.0F, 1.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}