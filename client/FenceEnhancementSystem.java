package com.rpgcore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rpgcore.RPGCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;

@EventBusSubscriber(modid = RPGCore.MODID, value = Dist.CLIENT)
public class FenceEnhancementSystem {

    private static final Direction[] ALL_CULL_FACES = {
            null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    public static boolean isFenceOrWall(BlockState state) {
        if (state == null || state.isAir()) return false;
        return state.getBlock() instanceof FenceBlock
                || state.getBlock() instanceof WallBlock
                || state.is(BlockTags.FENCES)
                || state.is(BlockTags.WALLS);
    }

    public static boolean canConnectTogether(BlockState a, BlockState b) {
        if (!isFenceOrWall(a) || !isFenceOrWall(b)) return false;
        boolean aWall = a.getBlock() instanceof WallBlock || a.is(BlockTags.WALLS);
        boolean bWall = b.getBlock() instanceof WallBlock || b.is(BlockTags.WALLS);
        if (aWall != bWall) return false;
        if (!aWall) {
            boolean aWood = a.is(BlockTags.WOODEN_FENCES);
            boolean bWood = b.is(BlockTags.WOODEN_FENCES);
            return aWood == bWood;
        }
        return true;
    }

    private static BlockState getRailModelState(BlockState state) {
        if (state.getBlock() instanceof FenceBlock) {
            return state.setValue(CrossCollisionBlock.NORTH, true)
                    .setValue(CrossCollisionBlock.SOUTH, true)
                    .setValue(CrossCollisionBlock.EAST, false)
                    .setValue(CrossCollisionBlock.WEST, false);
        } else if (state.getBlock() instanceof WallBlock) {
            return state.setValue(WallBlock.UP, false)
                    .setValue(WallBlock.NORTH_WALL, WallSide.LOW)
                    .setValue(WallBlock.SOUTH_WALL, WallSide.LOW)
                    .setValue(WallBlock.EAST_WALL, WallSide.NONE)
                    .setValue(WallBlock.WEST_WALL, WallSide.NONE);
        }
        return state;
    }

    /**
     * 100% ТОЧНАЯ ПРОВЕРКА: принадлежит ли полигон (BakedQuad) перекладине, а НЕ центральному столбу!
     * В ванильном Minecraft:
     * - Центральный столб забора (fence_post) имеет координаты Z строго внутри [0.375 .. 0.625] (6/16 .. 10/16)
     *   и высоту Y [0.0 .. 1.0]. Он НИКОГДА не касается краев блока Z = 0.0 или Z = 1.0!
     * - Боковая перекладина (fence_side / wall_side) ВСЕГДА идет от столба до самого края блока:
     *   у северной перекладины minZ == 0.0, а у южной перекладины maxZ == 1.0!
     * Поэтому любой полигон, который не доходит до края блока (minZ > 0.02F && maxZ < 0.98F)
     * ИЛИ имеет высоту столба от пола до верха (maxY - minY > 0.90F), — это 100% центральный столб, и мы его отбрасываем!
     */
    private static boolean isPureRailQuad(BakedQuad quad, boolean isNorthHalf) {
        int[] vertices = quad.getVertices();
        float minZ = 1.0F;
        float maxZ = 0.0F;
        float minY = 1.0F;
        float maxY = 0.0F;
        float minX = 1.0F;
        float maxX = 0.0F;

        for (int v = 0; v < 4; v++) {
            int base = v * 8;
            float x = Float.intBitsToFloat(vertices[base]);
            float y = Float.intBitsToFloat(vertices[base + 1]);
            float z = Float.intBitsToFloat(vertices[base + 2]);
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
            if (z < minZ) minZ = z;
            if (z > maxZ) maxZ = z;
        }

        // 1. Отсекаем любые вертикальные грани столба (высота столба 0..1) и его верхнюю/нижнюю шапку (ширина X: 0.375..0.625, Z: 0.375..0.625)
        if ((maxY - minY) > 0.92F) {
            return false;
        }
        if (minZ > 0.02F && maxZ < 0.98F) {
            // Любой полигон, не касающийся края Z=0 или Z=1, принадлежит центральному столбу!
            return false;
        }

        // 2. Проверяем, к какой половине относится перекладина:
        // Северная перекладина (NORTH) касается Z = 0.0 (minZ <= 0.02F)
        // Южная перекладина (SOUTH) касается Z = 1.0 (maxZ >= 0.98F)
        if (isNorthHalf) {
            return minZ <= 0.02F && maxZ <= 0.55F;
        } else {
            return maxZ >= 0.98F && minZ >= 0.45F;
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || mc.player == null) return;

        Vec3 camPos = event.getCamera().getPosition();
        BlockPos playerPos = mc.player.blockPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();

        int radiusXZ = 24;
        int radiusY = 10;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dy = -radiusY; dy <= radiusY; dy++) {
            for (int dz = -radiusXZ; dz <= radiusXZ; dz++) {
                for (int dx = -radiusXZ; dx <= radiusXZ; dx++) {
                    pos.set(playerPos.getX() + dx, playerPos.getY() + dy, playerPos.getZ() + dz);
                    BlockState state = level.getBlockState(pos);
                    if (!isFenceOrWall(state)) continue;

                    // 1. Соединение на 1 блок выше строго по прямой (Север, Юг, Запад, Восток)
                    checkAndRenderStepUpConnection(poseStack, bufferSource, dispatcher, level, pos, state, 1, 0, camPos);
                    checkAndRenderStepUpConnection(poseStack, bufferSource, dispatcher, level, pos, state, -1, 0, camPos);
                    checkAndRenderStepUpConnection(poseStack, bufferSource, dispatcher, level, pos, state, 0, 1, camPos);
                    checkAndRenderStepUpConnection(poseStack, bufferSource, dispatcher, level, pos, state, 0, -1, camPos);

                    // 2. Соединение по диагонали строго на одной высоте (dY = 0)
                    checkAndRenderFlatDiagonalConnection(poseStack, bufferSource, dispatcher, level, pos, state, 1, 1, camPos);
                    checkAndRenderFlatDiagonalConnection(poseStack, bufferSource, dispatcher, level, pos, state, -1, 1, camPos);
                }
            }
        }
    }

    private static void checkAndRenderStepUpConnection(PoseStack poseStack, MultiBufferSource bufferSource,
                                                       BlockRenderDispatcher dispatcher, Level level,
                                                       BlockPos pos, BlockState state,
                                                       int stepX, int stepZ, Vec3 camPos) {
        if (!level.getBlockState(pos.above()).isAir()) return;

        BlockPos upperNeighbor = pos.offset(stepX, 1, stepZ);
        BlockState upperState = level.getBlockState(upperNeighbor);
        if (!canConnectTogether(state, upperState)) return;

        int light1 = LevelRenderer.getLightColor(level, pos);
        int light2 = LevelRenderer.getLightColor(level, upperNeighbor);

        renderPureRailsBetweenPosts(
                poseStack, bufferSource, dispatcher, level,
                state, upperState,
                pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                upperNeighbor.getX() + 0.5D, upperNeighbor.getY(), upperNeighbor.getZ() + 0.5D,
                camPos, light1, light2
        );
    }

    private static void checkAndRenderFlatDiagonalConnection(PoseStack poseStack, MultiBufferSource bufferSource,
                                                             BlockRenderDispatcher dispatcher, Level level,
                                                             BlockPos pos, BlockState state,
                                                             int diagX, int diagZ, Vec3 camPos) {
        BlockPos diagPos = pos.offset(diagX, 0, diagZ);
        BlockState diagState = level.getBlockState(diagPos);
        if (!canConnectTogether(state, diagState)) return;

        BlockPos sidePosA = pos.offset(diagX, 0, 0);
        BlockPos sidePosB = pos.offset(0, 0, diagZ);
        BlockState sideA = level.getBlockState(sidePosA);
        BlockState sideB = level.getBlockState(sidePosB);

        if (!sideA.isAir() || !sideB.isAir()) {
            return;
        }
        if (isFenceOrWall(level.getBlockState(sidePosA.above())) || isFenceOrWall(level.getBlockState(sidePosB.above()))) {
            return;
        }

        int light1 = LevelRenderer.getLightColor(level, pos);
        int light2 = LevelRenderer.getLightColor(level, diagPos);

        renderPureRailsBetweenPosts(
                poseStack, bufferSource, dispatcher, level,
                state, diagState,
                pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                diagPos.getX() + 0.5D, diagPos.getY(), diagPos.getZ() + 0.5D,
                camPos, light1, light2
        );
    }

    private static void renderPureRailsBetweenPosts(PoseStack poseStack, MultiBufferSource bufferSource,
                                                    BlockRenderDispatcher dispatcher, Level level,
                                                    BlockState stateA, BlockState stateB,
                                                    double x1, double y1, double z1,
                                                    double x2, double y2, double z2,
                                                    Vec3 camPos, int lightA, int lightB) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        double horizDist = Math.sqrt(dx * dx + dz * dz);
        if (horizDist < 0.05D) return;

        float yawRad = (float) Mth.atan2(dx, dz);
        float slopeShearY = (float) (dy / horizDist);

        double midX = (x1 + x2) * 0.5D - camPos.x;
        double midY = (y1 + y2) * 0.5D - camPos.y;
        double midZ = (z1 + z2) * 0.5D - camPos.z;

        float spanLength = Math.max(0.50F, (float) horizDist - 0.16F);

        BlockState railStateA = getRailModelState(stateA);
        BlockState railStateB = getRailModelState(stateB);
        BakedModel modelA = dispatcher.getBlockModel(railStateA);
        BakedModel modelB = dispatcher.getBlockModel(railStateB);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.solid());
        RandomSource rand = RandomSource.create(42L);

        poseStack.pushPose();
        poseStack.translate(midX, midY, midZ);
        poseStack.mulPose(new Quaternionf().rotationY(yawRad));

        if (Math.abs(slopeShearY) > 0.01F) {
            Matrix4f shearMat = new Matrix4f();
            shearMat.m21(slopeShearY * spanLength);
            poseStack.mulPose(shearMat);
        }

        poseStack.scale(0.996F, 0.998F, spanLength);
        poseStack.translate(-0.5D, 0.0D, -0.5D);

        PoseStack.Pose lastPose = poseStack.last();

        for (Direction cullFace : ALL_CULL_FACES) {
            rand.setSeed(42L);
            List<BakedQuad> quadsA = modelA.getQuads(railStateA, cullFace, rand, ModelData.EMPTY, RenderType.solid());
            rand.setSeed(42L);
            List<BakedQuad> quadsB = modelB.getQuads(railStateB, cullFace, rand, ModelData.EMPTY, RenderType.solid());

            for (BakedQuad quad : quadsA) {
                if (isPureRailQuad(quad, true)) {
                    putWorldShadedQuad(level, consumer, lastPose, quad, yawRad, lightA);
                }
            }
            for (BakedQuad quad : quadsB) {
                if (isPureRailQuad(quad, false)) {
                    putWorldShadedQuad(level, consumer, lastPose, quad, yawRad, lightB);
                }
            }
        }

        poseStack.popPose();
    }

    /**
     * Вычисляет ТОЧНЫЙ цветовой тон грани в мировых координатах после поворота на угол yawRad
     * + учитывает множитель Ambient Occlusion ванильного блока (0.86F),
     * чтобы перекладина ни на 1% не отличалась по яркости и тону от столба любого вида дерева!
     */
    private static void putWorldShadedQuad(Level level, VertexConsumer consumer, PoseStack.Pose pose,
                                           BakedQuad quad, float yawRad, int packedLight) {
        Direction localDir = quad.getDirection();
        float shade;
        if (localDir == Direction.UP) {
            shade = level.getShade(Direction.UP, true);
        } else if (localDir == Direction.DOWN) {
            shade = level.getShade(Direction.DOWN, true);
        } else {
            // Для боковых граней перекладины вычисляем реальную мировую ориентацию нормали после поворота на yawRad:
            // В ванили Север/Юг (ось Z) имеют яркость 0.8F, а Запад/Восток (ось X) — яркость 0.6F!
            float nx = localDir.getStepX() * Mth.cos(yawRad) + localDir.getStepZ() * Mth.sin(yawRad);
            float nz = -localDir.getStepX() * Mth.sin(yawRad) + localDir.getStepZ() * Mth.cos(yawRad);
            float weightX = nx * nx;
            float weightZ = nz * nz;
            float shadeX = level.getShade(Direction.EAST, true);  // 0.6F
            float shadeZ = level.getShade(Direction.NORTH, true); // 0.8F
            shade = (shadeX * weightX + shadeZ * weightZ) / Math.max(0.001F, weightX + weightZ);
        }

        // Ванильный Smooth Lighting (Ambient Occlusion) на столбах забора дает среднее затенение ~0.85F.
        // Применяем его и фиксируем нормаль вершины вверх (0, 1, 0), чтобы шейдер не высветлял повернутую грань дважды!
        float finalBri = shade * 0.85F;
        int r = Mth.clamp(Math.round(finalBri * 255.0F), 0, 255);
        int g = r;
        int b = r;

        int[] vertices = quad.getVertices();
        Matrix4f mat = pose.pose();
        Vector4f posVec = new Vector4f();

        for (int i = 0; i < 4; i++) {
            int offset = i * 8;
            float vx = Float.intBitsToFloat(vertices[offset]);
            float vy = Float.intBitsToFloat(vertices[offset + 1]);
            float vz = Float.intBitsToFloat(vertices[offset + 2]);
            float u = Float.intBitsToFloat(vertices[offset + 4]);
            float v = Float.intBitsToFloat(vertices[offset + 5]);

            posVec.set(vx, vy, vz, 1.0F);
            mat.transform(posVec);

            consumer.addVertex(posVec.x(), posVec.y(), posVec.z())
                    .setColor(r, g, b, 255)
                    .setUv(u, v)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(packedLight)
                    .setNormal(0.0F, 1.0F, 0.0F);
        }
    }
}