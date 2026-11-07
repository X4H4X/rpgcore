package com.rpgcore.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.rpgcore.RPGCore;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class WorldMapManager {

    public static class Waypoint {
        public String name;
        public int x;
        public int z;
        public int colorHex;
        public String dimension;

        public Waypoint(String name, int x, int z, int colorHex, String dimension) {
            this.name = name;
            this.x = x;
            this.z = z;
            this.colorHex = colorHex;
            this.dimension = dimension;
        }
    }

    public static class MapBuildParticle {
        public final double worldX;
        public final double worldZ;
        public final float vx;
        public final float vz;
        public final int colorHex;
        public int age;
        public final int maxAge;

        public MapBuildParticle(double worldX, double worldZ, float vx, float vz, int colorHex, int maxAge) {
            this.worldX = worldX;
            this.worldZ = worldZ;
            this.vx = vx;
            this.vz = vz;
            this.colorHex = colorHex;
            this.age = 0;
            this.maxAge = maxAge;
        }
    }

    public static final int[] WAYPOINT_PALETTE = {
            0xFFFF5252,
            0xFFFFD740,
            0xFF69F0AE,
            0xFF40C4FF,
            0xFFE040FB,
            0xFFFFAB40,
            0xFFFFFFFF,
            0xFF8D6E63
    };

    public static final List<Waypoint> WAYPOINTS = new ArrayList<>();
    private static final List<MapBuildParticle> BUILD_PARTICLES = new ArrayList<>();
    private static boolean loaded = false;

    private static final int MINIMAP_TEX_SIZE = 128;
    private static DynamicTexture minimapTexture;
    private static ResourceLocation minimapLocation;

    private static final Long2ObjectOpenHashMap<int[]> CHUNK_ABGR_CACHE = new Long2ObjectOpenHashMap<>();
    private static String cachedDimension = "";
    private static final BlockPos.MutableBlockPos SHARED_POS = new BlockPos.MutableBlockPos();

    private static int chunksBuiltThisFrame = 0;
    private static boolean anyChunkSkippedThisFrame = false;
    private static int refreshTicker = 0;

    public static void beginFrameScan() {
        chunksBuiltThisFrame = 0;
        anyChunkSkippedThisFrame = false;
    }

    public static boolean wasAnyChunkSkipped() {
        return anyChunkSkippedThisFrame;
    }

    public static void onBlockChanged(BlockPos pos, BlockState state) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        CHUNK_ABGR_CACHE.remove(ChunkPos.asLong(chunkX, chunkZ));
        CHUNK_ABGR_CACHE.remove(ChunkPos.asLong(chunkX, chunkZ + 1));
        CHUNK_ABGR_CACHE.remove(ChunkPos.asLong(chunkX + 1, chunkZ));

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        int baseRgb = state.isAir() ? 0xFFE5C148 : state.getMapColor(mc.level, pos).col;
        if (baseRgb == 0) baseRgb = 0xFFFFD760;

        synchronized (BUILD_PARTICLES) {
            if (BUILD_PARTICLES.size() > 160) {
                BUILD_PARTICLES.subList(0, 40).clear();
            }
            for (int i = 0; i < 6; i++) {
                float angle = (float) (i * (Math.PI / 3.0) + (System.nanoTime() & 7) * 0.2F);
                float speed = 0.12F + (i % 2) * 0.08F;
                int col = (i % 2 == 0) ? 0xFFFFE580 : (0xFF000000 | baseRgb);
                BUILD_PARTICLES.add(new MapBuildParticle(
                        pos.getX() + 0.5D,
                        pos.getZ() + 0.5D,
                        Mth.cos(angle) * speed,
                        Mth.sin(angle) * speed,
                        col,
                        18 + (i * 3)
                ));
            }
        }
    }

    public static void tickLiveMapUpdates(Player player) {
        refreshTicker++;
        if (refreshTicker % 12 == 0) {
            int cx = player.blockPosition().getX() >> 4;
            int cz = player.blockPosition().getZ() >> 4;
            int rx = ((refreshTicker / 12) % 3) - 1;
            int rz = (((refreshTicker / 36) % 3)) - 1;
            CHUNK_ABGR_CACHE.remove(ChunkPos.asLong(cx + rx, cz + rz));
        }

        synchronized (BUILD_PARTICLES) {
            Iterator<MapBuildParticle> it = BUILD_PARTICLES.iterator();
            while (it.hasNext()) {
                MapBuildParticle p = it.next();
                p.age++;
                if (p.age >= p.maxAge) {
                    it.remove();
                }
            }
        }
    }

    public static void setDeathCorpseWaypoint(int x, int y, int z, String dimension) {
        ensureLoaded();
        String corpseTitle = "☠ Труп (" + y + " выс.)";
        WAYPOINTS.removeIf(w -> w.name.startsWith("☠ Труп"));
        WAYPOINTS.add(new Waypoint(corpseTitle, x, z, 0xFFFF3B3B, dimension));
        saveWaypoints();
    }

    public static void removeDeathCorpseWaypoint() {
        ensureLoaded();
        if (WAYPOINTS.removeIf(w -> w.name.startsWith("☠ Труп"))) {
            saveWaypoints();
        }
    }

    public static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try {
            File file = new File(Minecraft.getInstance().gameDirectory, "rpgcore_waypoints.txt");
            if (!file.exists()) return;
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            WAYPOINTS.clear();
            for (String line : lines) {
                String[] parts = line.split("\\|", 5);
                if (parts.length == 5) {
                    WAYPOINTS.add(new Waypoint(
                            parts[0],
                            Integer.parseInt(parts[1]),
                            Integer.parseInt(parts[2]),
                            Integer.parseInt(parts[3]),
                            parts[4]
                    ));
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static void saveWaypoints() {
        try {
            File file = new File(Minecraft.getInstance().gameDirectory, "rpgcore_waypoints.txt");
            List<String> lines = new ArrayList<>();
            for (Waypoint wp : WAYPOINTS) {
                lines.add(wp.name.replace("|", " ") + "|" + wp.x + "|" + wp.z + "|" + wp.colorHex + "|" + wp.dimension);
            }
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    public static int sampleSurfaceAbgrFast(Level level, int worldX, int worldZ) {
        String dim = level.dimension().location().toString();
        if (!dim.equals(cachedDimension)) {
            cachedDimension = dim;
            CHUNK_ABGR_CACHE.clear();
        }

        int chunkX = worldX >> 4;
        int chunkZ = worldZ >> 4;
        long chunkKey = ChunkPos.asLong(chunkX, chunkZ);

        int[] chunkData = CHUNK_ABGR_CACHE.get(chunkKey);
        if (chunkData == null) {
            LevelChunk chunk = (LevelChunk) level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
            if (chunk == null) {
                return 0xFF101218;
            }
            if (chunksBuiltThisFrame >= 32) {
                anyChunkSkippedThisFrame = true;
                return 0xFF101218;
            }
            chunksBuiltThisFrame++;
            chunkData = buildWholeChunkAbgrDetailed(level, chunk, chunkX << 4, chunkZ << 4);
            if (CHUNK_ABGR_CACHE.size() > 6144) {
                CHUNK_ABGR_CACHE.clear();
            }
            CHUNK_ABGR_CACHE.put(chunkKey, chunkData);
        }

        return chunkData[((worldZ & 15) << 4) | (worldX & 15)];
    }

    /**
     * Высокодетализированная генерация карты чанка (как на референсе image_d6b8ad.png):
     * считывает настоящие биомные цвета травы, листвы и воды, рендерит просвечивающее дно у берегов,
     * объемные кроны деревьев и мягкий градиентный рельеф высоты (Hillshading)!
     */
    private static int[] buildWholeChunkAbgrDetailed(Level level, LevelChunk chunk, int baseX, int baseZ) {
        int[] result = new int[256];
        int minBuildY = level.getMinBuildHeight();
        int[] heights = new int[256];
        int[] waterDepths = new int[256];
        int[] baseColors = new int[256];
        boolean[] isFoliage = new boolean[256];

        for (int lz = 0; lz < 16; lz++) {
            int wz = baseZ + lz;
            for (int lx = 0; lx < 16; lx++) {
                int wx = baseX + lx;
                int idx = (lz << 4) | lx;
                int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, lx, lz);

                if (y <= minBuildY) {
                    heights[idx] = minBuildY;
                    baseColors[idx] = 0x101218;
                    continue;
                }

                SHARED_POS.set(wx, y, wz);
                BlockState state = chunk.getBlockState(SHARED_POS);
                MapColor mapCol = state.getMapColor(level, SHARED_POS);

                int steps = 0;
                while ((mapCol == MapColor.NONE || state.isAir()) && y > minBuildY && steps < 20) {
                    y--;
                    steps++;
                    SHARED_POS.setY(y);
                    state = chunk.getBlockState(SHARED_POS);
                    mapCol = state.getMapColor(level, SHARED_POS);
                }

                heights[idx] = y;
                Block block = state.getBlock();
                int rgb = mapCol.col;

                // 1. Натуральные биомные оттенки травы, листвы и воды (вместо кислотных цветов ванильной карты!)
                FluidState fluid = state.getFluidState();
                if (!fluid.isEmpty() || state.is(Blocks.WATER) || state.is(Blocks.KELP) || state.is(Blocks.SEAGRASS)) {
                    int biomeWater = BiomeColors.getAverageWaterColor(level, SHARED_POS);
                    int wDepth = 0;
                    int checkY = y;
                    BlockState floorState = Blocks.SAND.defaultBlockState();
                    while (checkY > minBuildY && wDepth < 18) {
                        SHARED_POS.set(wx, checkY, wz);
                        BlockState bs = chunk.getBlockState(SHARED_POS);
                        if (bs.getFluidState().isEmpty() && !bs.is(Blocks.WATER) && !bs.is(Blocks.KELP) && !bs.is(Blocks.SEAGRASS)) {
                            floorState = bs;
                            break;
                        }
                        wDepth++;
                        checkY--;
                    }
                    waterDepths[idx] = Math.max(1, wDepth);

                    // Мягкая лазурно-синяя вода с просвечивающим дном на мелководье (как на референсе!)
                    int floorRgb = floorState.getMapColor(level, SHARED_POS).col;
                    if (floorRgb == 0) floorRgb = 0xC2B280;
                    int richWater = blendRgb(biomeWater, 0x2B5B9E, 0.52F);
                    if (wDepth <= 2) {
                        rgb = blendRgb(richWater, floorRgb, 0.34F);
                    } else if (wDepth <= 4) {
                        rgb = blendRgb(richWater, floorRgb, 0.14F);
                    } else {
                        rgb = blendRgb(richWater, 0x16325C, Math.min(0.55F, (wDepth - 4) * 0.045F));
                    }
                } else if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS)
                        || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN) || state.is(Blocks.MOSS_BLOCK)) {
                    int biomeGrass = BiomeColors.getAverageGrassColor(level, SHARED_POS);
                    // Приглушаем яркость до благородного природного оттенка
                    rgb = blendRgb(biomeGrass, 0x4E7D34, 0.32F);
                } else if (state.is(BlockTags.LEAVES) || state.is(Blocks.VINE)) {
                    int biomeFoliage = BiomeColors.getAverageFoliageColor(level, SHARED_POS);
                    // Листва деревьев чуть темнее и глубже луговой травы для читаемости крон леса
                    rgb = blendRgb(biomeFoliage, 0x2E5420, 0.42F);
                    isFoliage[idx] = true;
                } else if (state.is(Blocks.SAND) || state.is(Blocks.SANDSTONE) || state.is(Blocks.SMOOTH_SANDSTONE)) {
                    rgb = blendRgb(rgb, 0xD9C282, 0.45F);
                } else if (state.is(Blocks.DIRT_PATH) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)) {
                    rgb = blendRgb(rgb, 0x8A7356, 0.35F);
                } else if (block == Blocks.STONE || block == Blocks.ANDESITE || block == Blocks.COBBLESTONE) {
                    rgb = blendRgb(rgb, 0x70767E, 0.40F);
                }

                baseColors[idx] = (rgb == 0) ? 0x181A22 : rgb;
            }
        }

        // 2. Мягкое объемное затенение рельефа (северо-западный источник света + кроны деревьев)
        for (int lz = 0; lz < 16; lz++) {
            int wz = baseZ + lz;
            for (int lx = 0; lx < 16; lx++) {
                int wx = baseX + lx;
                int idx = (lz << 4) | lx;
                int y = heights[idx];
                int rgb = baseColors[idx];
                int wDepth = waterDepths[idx];

                int northY = (lz > 0) ? heights[((lz - 1) << 4) | lx] : y;
                int southY = (lz < 15) ? heights[((lz + 1) << 4) | lx] : y;
                int westY = (lx > 0) ? heights[(lz << 4) | (lx - 1)] : y;
                int eastY = (lx < 15) ? heights[(lz << 4) | (lx + 1)] : y;

                float shade = 1.0F;
                if (wDepth > 0) {
                    // Мягкий градиент глубины реки/моря без резкого шума
                    shade = Mth.clamp(1.06F - wDepth * 0.022F, 0.68F, 1.06F);
                } else {
                    float gradN = (y - northY) + (southY - y) * 0.5F;
                    float gradW = (y - westY) + (eastY - y) * 0.5F;
                    float slope = (gradN + gradW) * 0.055F;
                    shade = Mth.clamp(1.0F + slope, 0.74F, 1.20F);

                    if (isFoliage[idx]) {
                        // Мягкая текстура крон деревьев, как на image_d6b8ad.png
                        int canopyPattern = ((wx * 13 + wz * 29) & 7) - 3;
                        shade += canopyPattern * 0.018F;
                        if (y < northY || y < westY) {
                            shade *= 0.86F;
                        }
                    } else {
                        int subtleGrain = ((wx * 19 + wz * 37) & 3) - 1;
                        shade += subtleGrain * 0.006F;
                    }
                }

                int r = Mth.clamp(Math.round(((rgb >> 16) & 0xFF) * shade), 0, 255);
                int g = Mth.clamp(Math.round(((rgb >> 8) & 0xFF) * shade), 0, 255);
                int b = Mth.clamp(Math.round((rgb & 0xFF) * shade), 0, 255);

                result[idx] = 0xFF000000 | (b << 16) | (g << 8) | r;
            }
        }
        return result;
    }

    private static int blendRgb(int c1, int c2, float ratio) {
        int r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        int r = Math.round(r1 + (r2 - r1) * ratio);
        int g = Math.round(g1 + (g2 - g1) * ratio);
        int b = Math.round(b1 + (b2 - b1) * ratio);
        return (r << 16) | (g << 8) | b;
    }

    /**
     * Билинейная интерполяция цветов карты при вращении миникарты,
     * чтобы рельеф внутри круга выглядел гладким и четким без ступенчатых пикселей (как на image_d6b8ad.png)!
     */
    private static int sampleBilinearAbgr(Level level, float worldX, float worldZ) {
        int x0 = Mth.floor(worldX);
        int z0 = Mth.floor(worldZ);
        float fx = worldX - x0;
        float fz = worldZ - z0;

        int c00 = sampleSurfaceAbgrFast(level, x0, z0);
        int c10 = sampleSurfaceAbgrFast(level, x0 + 1, z0);
        int c01 = sampleSurfaceAbgrFast(level, x0, z0 + 1);
        int c11 = sampleSurfaceAbgrFast(level, x0 + 1, z0 + 1);

        int r = Math.round(Mth.lerp(fz, Mth.lerp(fx, c00 & 0xFF, c10 & 0xFF), Mth.lerp(fx, c01 & 0xFF, c11 & 0xFF)));
        int g = Math.round(Mth.lerp(fz, Mth.lerp(fx, (c00 >> 8) & 0xFF, (c10 >> 8) & 0xFF), Mth.lerp(fx, (c01 >> 8) & 0xFF, (c11 >> 8) & 0xFF)));
        int b = Math.round(Mth.lerp(fz, Mth.lerp(fx, (c00 >> 16) & 0xFF, (c10 >> 16) & 0xFF), Mth.lerp(fx, (c01 >> 16) & 0xFF, (c11 >> 16) & 0xFF)));

        return 0xFF000000 | (b << 16) | (g << 8) | r;
    }

    private static void updateMinimapTextureSmooth(Level level, double smoothX, double smoothZ, float smoothYaw) {
        Minecraft mc = Minecraft.getInstance();
        if (minimapTexture == null) {
            minimapTexture = new DynamicTexture(MINIMAP_TEX_SIZE, MINIMAP_TEX_SIZE, false);
            minimapLocation = ResourceLocation.fromNamespaceAndPath(RPGCore.MODID, "fast_minimap_128");
            mc.getTextureManager().register(minimapLocation, minimapTexture);
        }

        NativeImage img = minimapTexture.getPixels();
        if (img == null) return;

        beginFrameScan();

        float yawRad = (smoothYaw + 180.0F) * Mth.DEG_TO_RAD;
        float cos = Mth.cos(yawRad);
        float sin = Mth.sin(yawRad);

        float half = MINIMAP_TEX_SIZE / 2.0F;
        float mapRadius = half - 5.0F;
        float scale = 0.62F;

        for (int y = 0; y < MINIMAP_TEX_SIZE; y++) {
            float dy = y - half + 0.5F;
            for (int x = 0; x < MINIMAP_TEX_SIZE; x++) {
                float dx = x - half + 0.5F;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                if (dist > half - 0.5F) {
                    img.setPixelRGBA(x, y, 0x00000000);
                } else if (dist >= mapRadius + 3.2F) {
                    // Внешний черный пиксельный контур обода (как на image_d6b8ad.png)
                    img.setPixelRGBA(x, y, 0xF008090D);
                } else if (dist >= mapRadius + 0.8F) {
                    // Металлическое кольцо-обод в цветах нашего HUD (с мягким верхним бликом и нижней тенью)
                    float light = (-dy / half) * 0.25F + 0.85F;
                    int ringRgb = ((int) (dx + dy) & 1) == 0 ? 0x4E5361 : 0x3D414D;
                    int r = Mth.clamp(Math.round(((ringRgb >> 16) & 0xFF) * light), 0, 255);
                    int g = Mth.clamp(Math.round(((ringRgb >> 8) & 0xFF) * light), 0, 255);
                    int b = Mth.clamp(Math.round((ringRgb & 0xFF) * light), 0, 255);
                    img.setPixelRGBA(x, y, 0xFF000000 | (b << 16) | (g << 8) | r);
                } else if (dist >= mapRadius - 0.6F) {
                    // Внутренняя тонкая темная с золотистым отливом окантовка стекла миникарты
                    img.setPixelRGBA(x, y, 0xFF1A1610);
                } else {
                    float wx = (float) smoothX + (dx * cos - dy * sin) * scale;
                    float wz = (float) smoothZ + (dx * sin + dy * cos) * scale;
                    int col = sampleBilinearAbgr(level, wx, wz);

                    // Мягкая виньетка по внутреннему краю круга (как на image_d6b8ad.png)
                    if (dist >= mapRadius - 4.5F) {
                        float vignette = 1.0F - ((dist - (mapRadius - 4.5F)) / 4.5F) * 0.28F;
                        col = darkenAbgr(col, vignette);
                    }
                    img.setPixelRGBA(x, y, col);
                }
            }
        }

        minimapTexture.upload();
    }

    private static int darkenAbgr(int abgr, float factor) {
        int b = Mth.clamp(Math.round(((abgr >> 16) & 0xFF) * factor), 0, 255);
        int g = Mth.clamp(Math.round(((abgr >> 8) & 0xFF) * factor), 0, 255);
        int r = Mth.clamp(Math.round((abgr & 0xFF) * factor), 0, 255);
        return 0xFF000000 | (b << 16) | (g << 8) | r;
    }

    // =========================================================================
    // ПУНКТ 9: КРУГЛАЯ МИНИКАРТА В СТИЛЕ image_d6b8ad.png В ЦВЕТАХ НАШЕГО HUD
    // =========================================================================
    public static void renderCircularMinimap(GuiGraphics gui, Player player, int screenWidth) {
        ensureLoaded();
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        float pt = mc.getTimer().getGameTimeDeltaPartialTick(true);
        double smoothX = Mth.lerp(pt, player.xo, player.getX());
        double smoothY = Mth.lerp(pt, player.yo, player.getY());
        double smoothZ = Mth.lerp(pt, player.zo, player.getZ());
        float smoothYaw = Mth.rotLerp(pt, player.yRotO, player.getYRot());

        updateMinimapTextureSmooth(level, smoothX, smoothZ, smoothYaw);
        if (minimapLocation == null) return;

        Font font = mc.font;
        int radius = 38;
        int size = radius * 2;
        int centerX = screenWidth - radius - 10;
        int centerY = radius + 8;

        // 1. Отрисовка гладкого круга миникарты
        gui.blit(minimapLocation, centerX - radius, centerY - radius, size, size, 0, 0, MINIMAP_TEX_SIZE, MINIMAP_TEX_SIZE, MINIMAP_TEX_SIZE, MINIMAP_TEX_SIZE);

        // 2. Тонкое полупрозрачное перекрестие осей внутри круга (как на image_d6b8ad.png)
        gui.fill(centerX - radius + 6, centerY, centerX + radius - 6, centerY + 1, 0x2EFFFFFF);
        gui.fill(centerX, centerY - radius + 6, centerX + 1, centerY + radius - 6, 0x2EFFFFFF);

        float yawRad = (smoothYaw + 180.0F) * Mth.DEG_TO_RAD;
        float cos = Mth.cos(yawRad);
        float sin = Mth.sin(yawRad);

        // 3. Точки меток и соклановцев внутри миникарты
        String dim = level.dimension().location().toString();
        for (Waypoint wp : WAYPOINTS) {
            if (!wp.dimension.equals(dim)) continue;
            double wdx = (wp.x - smoothX) / 1.05D;
            double wdz = (wp.z - smoothZ) / 1.05D;
            double sx = wdx * cos + wdz * sin;
            double sy = -wdx * sin + wdz * cos;
            double dist = Math.sqrt(sx * sx + sy * sy);
            if (dist > radius - 7) {
                sx = (sx / dist) * (radius - 7);
                sy = (sy / dist) * (radius - 7);
            }
            int mx = centerX + (int) Math.round(sx);
            int my = centerY + (int) Math.round(sy);
            gui.fill(mx - 2, my - 2, mx + 2, my + 2, wp.colorHex | 0xFF000000);
            gui.renderOutline(mx - 3, my - 3, 6, 6, 0xFF0A0B0F);
        }

        for (var ally : ClanScreen.clientCompassAllies) {
            double adx = (ally.x() - smoothX) / 1.05D;
            double adz = (ally.z() - smoothZ) / 1.05D;
            double sx = adx * cos + adz * sin;
            double sy = -adx * sin + adz * cos;
            double dist = Math.sqrt(sx * sx + sy * sy);
            if (dist <= radius - 7) {
                int mx = centerX + (int) Math.round(sx);
                int my = centerY + (int) Math.round(sy);
                gui.fill(mx - 1, my - 1, mx + 2, my + 2, ally.colorHex() | 0xFF000000);
            }
        }

        // 4. Объемный центральный курсор-стрелка игрока (точь-в-точь как на image_d6b8ad.png!)
        drawStylizedPlayerArrow(gui, centerX, centerY);

        // 5. Четыре квадратных шильдика сторон света (С / В / Ю / З) со скругленной рамкой в цветах нашего HUD
        String[] cNames = {"N", "E", "S", "W"};
        float[] cAngles = {180F, -90F, 0F, 90F};
        for (int i = 0; i < 4; i++) {
            float a = (cAngles[i] - smoothYaw - 90.0F) * Mth.DEG_TO_RAD;
            int lx = centerX + Math.round(Mth.cos(a) * (radius - 2.0F));
            int ly = centerY + Math.round(Mth.sin(a) * (radius - 2.0F));

            drawCardinalBadge(gui, font, cNames[i], lx, ly, i == 0);
        }

        // 6. Ненавязчивая полупрозрачная плашка координат X / Y / Z под миникартой
        int bx = Mth.floor(smoothX);
        int by = Mth.floor(smoothY);
        int bz = Mth.floor(smoothZ);
        String coordsText = "§7X: §f" + bx + "  §7Y: §f" + by + "  §7Z: §f" + bz;

        float textScale = 0.70F;
        int textW = Math.round(font.width(coordsText) * textScale);
        int plateW = Math.max(68, textW + 10);
        int plateH = 10;
        int plateX = centerX - plateW / 2;
        int plateY = centerY + radius + 5;

        // Мягкий полупрозрачный фон с тонкими скобками в стиле нашего нового компаса
        gui.fillGradient(plateX, plateY, plateX + plateW, plateY + plateH, 0x990D0F14, 0x440D0F14);
        gui.fill(plateX + 6, plateY, plateX + plateW - 6, plateY + 1, 0x778C7032);
        gui.fill(plateX, plateY + 1, plateX + 1, plateY + plateH - 1, 0x6645484F);
        gui.fill(plateX + plateW - 1, plateY + 1, plateX + plateW, plateY + plateH - 1, 0x6645484F);

        gui.pose().pushPose();
        gui.pose().translate(centerX - (font.width(coordsText) * textScale) / 2.0F, plateY + 2.2F, 0);
        gui.pose().scale(textScale, textScale, 1.0F);
        gui.drawString(font, coordsText, 0, 0, 0xFFD8DEE9, true);
        gui.pose().popPose();
    }

    /**
     * Рисует объемную бело-серебристую стрелку-курсор в центре миникарты (как на image_d6b8ad.png)
     */
    private static void drawStylizedPlayerArrow(GuiGraphics gui, int cx, int cy) {
        // Темный внешний контур стрелки
        gui.fill(cx - 1, cy - 5, cx + 2, cy - 3, 0xFF08090C);
        gui.fill(cx - 2, cy - 3, cx + 3, cy - 1, 0xFF08090C);
        gui.fill(cx - 3, cy - 1, cx + 4, cy + 1, 0xFF08090C);
        gui.fill(cx - 4, cy + 1, cx + 5, cy + 4, 0xFF08090C);
        gui.fill(cx - 5, cy + 3, cx - 1, cy + 5, 0xFF08090C);
        gui.fill(cx + 2, cy + 3, cx + 6, cy + 5, 0xFF08090C);

        // Светлое левое крыло стрелки (#F4F6FA) и слегка затененное правое крыло (#B8C0CC) для 3D-граней!
        gui.fill(cx, cy - 4, cx + 1, cy + 2, 0xFFF4F6FA);
        gui.fill(cx - 1, cy - 2, cx, cy + 3, 0xFFF4F6FA);
        gui.fill(cx - 2, cy, cx - 1, cy + 3, 0xFFE6EAF0);
        gui.fill(cx - 3, cy + 2, cx - 2, cy + 4, 0xFFD8DEE8);

        gui.fill(cx + 1, cy - 2, cx + 2, cy + 3, 0xFFC2C9D6);
        gui.fill(cx + 2, cy, cx + 3, cy + 3, 0xFFAEB6C4);
        gui.fill(cx + 3, cy + 2, cx + 4, cy + 4, 0xFF9BA3B2);
    }

    /**
     * Рисует скругленный шильдик стороны света (N / E / S / W) на ободе миникарты в цветах HUD
     */
    private static void drawCardinalBadge(GuiGraphics gui, Font font, String label, int cx, int cy, boolean isNorth) {
        int bw = 11;
        int bh = 11;
        int x = cx - bw / 2;
        int y = cy - bh / 2;

        // Скругленная черная внешняя форма
        gui.fill(x + 1, y, x + bw - 1, y + bh, 0xF20A0B0F);
        gui.fill(x, y + 1, x + bw, y + bh - 1, 0xF20A0B0F);

        // Внутренняя темно-стальная плашка с верхней фаской
        gui.fillGradient(x + 1, y + 1, x + bw - 1, y + bh - 1, 0xFF282C36, 0xFF14161D);
        int borderCol = isNorth ? 0xFFB88A38 : 0xFF454954;
        gui.fill(x + 1, y + 1, x + bw - 1, y + 2, borderCol);

        int textCol = isNorth ? 0xFFFFE082 : 0xFFE6E8EC;
        gui.pose().pushPose();
        float sc = 0.78F;
        gui.pose().translate(cx - (font.width(label) * sc) / 2.0F + 0.2F, cy - 2.8F, 0.0F);
        gui.pose().scale(sc, sc, 1.0F);
        gui.drawString(font, label, 0, 0, textCol, true);
        gui.pose().popPose();
    }
}