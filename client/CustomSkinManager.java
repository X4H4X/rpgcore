package com.rpgcore.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.rpgcore.RPGCore;
import com.rpgcore.data.Gender;
import com.rpgcore.data.PlayerAppearanceData;
import com.rpgcore.data.RaceType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class CustomSkinManager {

    private static final Map<Integer, ResourceLocation> SKIN_CACHE = new HashMap<>();
    private static final Map<Integer, ResourceLocation> EXTRAS_CACHE = new HashMap<>();
    private static ResourceLocation BLANK_ARMOR_TEX = null;

    public static ResourceLocation getBlankArmorTexture() {
        if (BLANK_ARMOR_TEX != null) return BLANK_ARMOR_TEX;
        NativeImage img = new NativeImage(64, 32, true);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 64; x++) {
                img.setPixelRGBA(x, y, 0x00000000);
            }
        }
        DynamicTexture tex = new DynamicTexture(img);
        BLANK_ARMOR_TEX = ResourceLocation.fromNamespaceAndPath(RPGCore.MODID, "blank_armor_layer");
        Minecraft.getInstance().getTextureManager().register(BLANK_ARMOR_TEX, tex);
        return BLANK_ARMOR_TEX;
    }

    public static ResourceLocation getOrCreateSkin(PlayerAppearanceData data) {
        int hash = computeVisualHash(data);
        ResourceLocation existing = SKIN_CACHE.get(hash);
        if (existing != null) return existing;

        ResourceLocation created = generateDynamicSkin(data, hash);
        SKIN_CACHE.put(hash, created);
        return created;
    }

    public static ResourceLocation getOrCreateExtrasTexture(PlayerAppearanceData data) {
        int hash = computeVisualHash(data);
        ResourceLocation existing = EXTRAS_CACHE.get(hash);
        if (existing != null) return existing;

        ResourceLocation created = generateDynamicExtras(data, hash);
        EXTRAS_CACHE.put(hash, created);
        return created;
    }

    private static int computeVisualHash(PlayerAppearanceData d) {
        int result = d.race().ordinal();
        result = 31 * result + d.gender().ordinal();
        result = 31 * result + d.skinColorIndex();
        result = 31 * result + Math.floorMod(d.faceVariant(), 5);
        result = 31 * result + d.eyeColorIndex();
        result = 31 * result + d.hairStyle();
        result = 31 * result + d.hairColorIndex();
        result = 31 * result + d.beardStyle();
        return result;
    }

    private static ResourceLocation generateDynamicSkin(PlayerAppearanceData data, int hash) {
        NativeImage img = new NativeImage(64, 64, true);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                img.setPixelRGBA(x, y, 0x00000000);
            }
        }

        int baseSkin = data.getResolvedSkinColor();
        boolean dragonScales = (data.race() == RaceType.DRAGONBORN);

        // 1. Чистая, мягкая базовая заливка кожи (или чешуи у Драконорожденного)
        paintSkinRegion(img, 0, 0, 32, 16, baseSkin, dragonScales);
        paintSkinRegion(img, 16, 16, 24, 16, baseSkin, dragonScales);
        paintSkinRegion(img, 40, 16, 16, 16, baseSkin, dragonScales);
        paintSkinRegion(img, 32, 48, 16, 16, baseSkin, dragonScales);
        paintSkinRegion(img, 0, 16, 16, 16, baseSkin, dragonScales);
        paintSkinRegion(img, 16, 48, 16, 16, baseSkin, dragonScales);

        // 2. Плавные теневые очертания мускулатуры и тела
        paintSoftBodyShading(img, baseSkin, data.gender(), data.race());

        if (data.race() == RaceType.MUTANT) {
            paintMutantBodyMarks(img, baseSkin);
        }

        if (data.gender() == Gender.MALE && data.race() != RaceType.DRAGONBORN) {
            int b = Math.floorMod(data.beardStyle(), 10);
            if (b == 1 || b == 2) {
                paintSmoothStubble(img, baseSkin, data.getResolvedHairColor(), b == 2);
            }
        }

        // 3. Отрисовка 5 уникальных лиц (без полосы на лбу у Орка и без выхода бровей на 3D-слой)
        paintFace(img, data);

        // 4. Одежда (белье) + объемные 3D-элементы на втором слое (ремни, манжеты, гребни чешуи)
        paintUnderwearAnd3DElements(img, data, baseSkin, dragonScales);

        DynamicTexture tex = new DynamicTexture(img);
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(RPGCore.MODID, "skin_v6_fixed_" + Integer.toHexString(hash));
        Minecraft.getInstance().getTextureManager().register(loc, tex);
        return loc;
    }

    private static void paintSkinRegion(NativeImage img, int sx, int sy, int w, int h, int baseHex, boolean dragonScales) {
        int scaleGroove = warmShade(baseHex, 0.76F);
        int scaleMid = warmShade(baseHex, 0.92F);
        int scaleLight = warmShade(baseHex, 1.06F);
        int scaleTip = warmShade(baseHex, 1.14F);

        for (int dy = 0; dy < h; dy++) {
            float vGrad = 1.03F - (dy / (float) Math.max(1, h - 1)) * 0.07F;
            for (int dx = 0; dx < w; dx++) {
                int pixelCol;
                if (dragonScales) {
                    int row = dy / 2;
                    int ly = dy % 2;
                    int lx = (dx + (row & 1)) % 2;

                    if (ly == 0 && lx == 0) {
                        pixelCol = scaleGroove;
                    } else if (ly == 0) {
                        pixelCol = scaleMid;
                    } else if (lx == 1) {
                        pixelCol = scaleTip;
                    } else {
                        pixelCol = scaleLight;
                    }
                } else {
                    int subtle = ((dx * 13 + dy * 29) & 3) - 1;
                    float factor = vGrad + subtle * 0.007F;
                    pixelCol = warmShade(baseHex, factor);
                }
                img.setPixelRGBA(sx + dx, sy + dy, toAbgr(pixelCol));
            }
        }
    }

    private static void paintSoftBodyShading(NativeImage img, int baseHex, Gender gender, RaceType race) {
        int softShadow = warmShade(baseHex, 0.86F);
        int deepShadow = warmShade(baseHex, 0.79F);
        int softLight = warmShade(baseHex, 1.05F);

        boolean isDragon = (race == RaceType.DRAGONBORN);

        if (isDragon) {
            int bellyLight = blend(baseHex, 0xFFF5DFC8, 0.20F);
            int bellyGroove = warmShade(baseHex, 0.80F);
            for (int y = 20; y <= 27; y++) {
                boolean segLine = (y == 20 || y == 23 || y == 26);
                for (int x = 21; x <= 26; x++) {
                    tintPixel(img, x, y, segLine ? bellyGroove : bellyLight, 0.65F);
                }
                tintPixel(img, 23, y, bellyGroove, 0.30F);
                tintPixel(img, 24, y, bellyGroove, 0.30F);
            }
        } else {
            tintPixel(img, 21, 20, softLight, 0.50F);
            tintPixel(img, 26, 20, softLight, 0.50F);
            tintPixel(img, 23, 20, softShadow, 0.55F);
            tintPixel(img, 24, 20, softShadow, 0.55F);

            if (gender == Gender.MALE) {
                for (int x : new int[]{21, 22, 25, 26}) {
                    tintPixel(img, x, 21, softLight, 0.45F);
                    tintPixel(img, x, 22, softLight, 0.35F);
                    tintPixel(img, x, 23, deepShadow, 0.55F);
                }
                for (int y = 21; y <= 27; y++) {
                    tintPixel(img, 23, y, softShadow, 0.45F);
                    tintPixel(img, 24, y, softShadow, 0.45F);
                }
                for (int y : new int[]{24, 26}) {
                    tintPixel(img, 22, y, softLight, 0.35F);
                    tintPixel(img, 25, y, softLight, 0.35F);
                }
                for (int y : new int[]{25, 27}) {
                    tintPixel(img, 22, y, softShadow, 0.48F);
                    tintPixel(img, 25, y, softShadow, 0.48F);
                }
            } else {
                for (int y = 25; y <= 27; y++) {
                    tintPixel(img, 20, y, deepShadow, 0.45F);
                    tintPixel(img, 27, y, deepShadow, 0.45F);
                    tintPixel(img, 23, y, softShadow, 0.25F);
                    tintPixel(img, 24, y, softShadow, 0.25F);
                }
            }
        }

        for (int y = 20; y < 28; y++) {
            tintPixel(img, 20, y, softShadow, 0.45F);
            tintPixel(img, 27, y, softShadow, 0.45F);
            for (int sx = 16; sx < 20; sx++) tintPixel(img, sx, y, softShadow, 0.40F);
            for (int sx = 28; sx < 32; sx++) tintPixel(img, sx, y, softShadow, 0.40F);
        }

        for (int y = 20; y < 28; y++) {
            tintPixel(img, 35, y, softShadow, 0.50F);
            tintPixel(img, 36, y, softShadow, 0.50F);
            tintPixel(img, 32, y, softShadow, 0.40F);
            tintPixel(img, 39, y, softShadow, 0.40F);
            if (y >= 21 && y <= 23) {
                tintPixel(img, 33, y, softLight, 0.35F);
                tintPixel(img, 34, y, softLight, 0.35F);
                tintPixel(img, 37, y, softLight, 0.35F);
                tintPixel(img, 38, y, softLight, 0.35F);
            }
        }

        shadeLimbSmooth(img, 40, 16, softShadow, deepShadow, softLight);
        shadeLimbSmooth(img, 32, 48, softShadow, deepShadow, softLight);
        shadeLimbSmooth(img, 0, 16, softShadow, deepShadow, softLight);
        shadeLimbSmooth(img, 16, 48, softShadow, deepShadow, softLight);

        // Тонкое боковое затенение висков (без полосы поперек лба!)
        for (int y = 9; y <= 14; y++) {
            tintPixel(img, 8, y, softShadow, 0.25F);
            tintPixel(img, 15, y, softShadow, 0.25F);
        }
    }

    private static void shadeLimbSmooth(NativeImage img, int bx, int by, int softShadow, int deepShadow, int softLight) {
        for (int dy = 4; dy < 16; dy++) {
            int y = by + dy;
            tintPixel(img, bx + 4, y, softShadow, 0.28F);
            tintPixel(img, bx + 7, y, softShadow, 0.28F);

            if (dy == 8) {
                tintPixel(img, bx + 5, y, softShadow, 0.35F);
                tintPixel(img, bx + 6, y, softShadow, 0.35F);
            } else if (dy == 5 || dy == 6 || dy == 10) {
                tintPixel(img, bx + 5, y, softLight, 0.30F);
                tintPixel(img, bx + 6, y, softLight, 0.25F);
            }

            for (int dx = 0; dx < 4; dx++) tintPixel(img, bx + dx, y, softShadow, 0.30F);
            for (int dx = 8; dx < 12; dx++) tintPixel(img, bx + dx, y, softShadow, 0.30F);
            for (int dx = 12; dx < 16; dx++) tintPixel(img, bx + dx, y, deepShadow, 0.32F);
        }
    }

    private static void paintMutantBodyMarks(NativeImage img, int skinHex) {
        int veinCol = blend(skinHex, 0xFF3A2847, 0.38F);
        int scarCol = blend(skinHex, 0xFF8C4242, 0.42F);
        img.setPixelRGBA(21, 20, toAbgr(veinCol));
        img.setPixelRGBA(22, 21, toAbgr(veinCol));
        img.setPixelRGBA(25, 21, toAbgr(scarCol));
        img.setPixelRGBA(24, 22, toAbgr(scarCol));
        img.setPixelRGBA(23, 23, toAbgr(scarCol));
    }

    private static void paintSmoothStubble(NativeImage img, int skinHex, int hairHex, boolean heavy) {
        float baseAlpha = heavy ? 0.38F : 0.22F;
        for (int x = 8; x <= 15; x++) {
            for (int y = 13; y <= 15; y++) {
                float mask = 0.0F;
                if (y == 15) mask = 0.90F;
                else if (y == 14) {
                    if (x == 8 || x == 9 || x == 14 || x == 15) mask = 0.75F;
                    else if (x == 10 || x == 13) mask = 0.55F;
                } else if (x == 8 || x == 15 || (x >= 10 && x <= 13)) {
                    mask = 0.50F;
                }
                if (mask > 0.0F) {
                    img.setPixelRGBA(x, y, toAbgr(blend(skinHex, hairHex, baseAlpha * mask)));
                }
            }
        }
    }

    private static void paintUnderwearAnd3DElements(NativeImage img, PlayerAppearanceData data, int baseSkin, boolean dragonScales) {
        Gender gender = data.gender();
        int linenDark = 0xFF4D4034;
        int linenBase = 0xFF665646;
        int linenHighlight = 0xFF7A6856;
        int leatherDark = 0xFF2E2016;
        int leatherMid = 0xFF453020;
        int leatherLight = 0xFF5C412C;
        int goldBuckle = 0xFFD9B146;
        int goldShadow = 0xFF9E7B24;

        for (int x = 16; x < 40; x++) {
            img.setPixelRGBA(x, 28, toAbgr(leatherDark));
            for (int y = 29; y < 32; y++) {
                int c = (y == 31) ? linenDark : ((x % 4 == 0) ? linenDark : ((x % 4 == 2) ? linenHighlight : linenBase));
                img.setPixelRGBA(x, y, toAbgr(c));
            }
        }

        for (int x = 28; x < 36; x++) {
            for (int y = 16; y < 20; y++) {
                int c = ((x + y) % 2 == 0) ? linenDark : linenBase;
                img.setPixelRGBA(x, y, toAbgr(c));
            }
        }

        for (int dx = 4; dx < 8; dx++) {
            for (int dy = 0; dy < 4; dy++) {
                int c = ((dx + dy) % 2 == 0) ? linenDark : linenBase;
                img.setPixelRGBA(dx, 16 + dy, toAbgr(c));
                img.setPixelRGBA(16 + dx, 48 + dy, toAbgr(c));
            }
        }

        for (int x = 0; x < 16; x++) {
            for (int y = 20; y < 25; y++) {
                int c = (y == 24) ? leatherDark : ((x % 4 == 0) ? linenDark : ((x % 4 == 2) ? linenHighlight : linenBase));
                img.setPixelRGBA(x, y, toAbgr(c));
                img.setPixelRGBA(16 + x, 32 + y, toAbgr(c));
            }
        }

        if (gender == Gender.FEMALE) {
            for (int x = 16; x < 40; x++) {
                for (int y = 19; y <= 25; y++) {
                    int c;
                    if (y == 19 || y == 25) c = leatherDark;
                    else c = (x % 3 == 0) ? linenDark : ((x % 3 == 1) ? linenHighlight : linenBase);
                    img.setPixelRGBA(x, y, toAbgr(c));
                }
            }
        }

        int jY = 16;
        for (int x = 16; x < 40; x++) {
            int beltCol = ((x + 1) % 3 == 0) ? leatherLight : leatherMid;
            if (x == 23 || x == 24) {
                beltCol = goldBuckle;
            } else if (x == 22 || x == 25) {
                beltCol = goldShadow;
            }
            img.setPixelRGBA(x, jY + 28, toAbgr(beltCol));
        }
        img.setPixelRGBA(23, jY + 29, toAbgr(goldShadow));
        img.setPixelRGBA(24, jY + 29, toAbgr(goldShadow));
        img.setPixelRGBA(20, jY + 29, toAbgr(leatherDark));
        img.setPixelRGBA(27, jY + 29, toAbgr(leatherDark));

        if (gender == Gender.FEMALE) {
            for (int x = 20; x <= 27; x++) {
                img.setPixelRGBA(x, jY + 19, toAbgr(leatherMid));
                img.setPixelRGBA(x, jY + 25, toAbgr(leatherMid));
            }
        }

        for (int dx = 0; dx < 16; dx++) {
            int cuff = (dx % 2 == 0) ? leatherMid : leatherDark;
            img.setPixelRGBA(dx, 32 + 8, toAbgr(cuff));
            img.setPixelRGBA(dx, 48 + 8, toAbgr(cuff));
        }

        if (dragonScales) {
            int ridgeCol = warmShade(baseSkin, 1.15F);
            int ridgeDark = warmShade(baseSkin, 0.72F);
            for (int y = 20; y < 27; y += 2) {
                img.setPixelRGBA(35, jY + y, toAbgr(ridgeCol));
                img.setPixelRGBA(36, jY + y, toAbgr(ridgeDark));
            }
            for (int dy : new int[]{9, 11, 13}) {
                img.setPixelRGBA(40 + 5, 32 + dy, toAbgr(ridgeCol));
                img.setPixelRGBA(40 + 6, 32 + dy, toAbgr(ridgeDark));
                img.setPixelRGBA(48 + 5, 48 + dy, toAbgr(ridgeDark));
                img.setPixelRGBA(48 + 6, 48 + dy, toAbgr(ridgeCol));

                img.setPixelRGBA(5, 32 + dy, toAbgr(ridgeCol));
                img.setPixelRGBA(6, 32 + dy, toAbgr(ridgeDark));
                img.setPixelRGBA(5, 48 + dy, toAbgr(ridgeDark));
                img.setPixelRGBA(6, 48 + dy, toAbgr(ridgeCol));
            }
        }
    }

    /**
     * Отрисовка 5 вариантов лиц для каждой расы.
     * Все брови расположены на Y = 11 (непосредственно над глазами Y = 12),
     * поэтому ни одна 3D-прическа больше не пересекает брови!
     */
    private static void paintFace(NativeImage img, PlayerAppearanceData data) {
        int skin = data.getResolvedSkinColor();
        int eyeCol = data.getResolvedEyeColor();
        int hairCol = data.getResolvedHairColor();
        int faceIdx = Math.floorMod(data.faceVariant(), 5);
        boolean isFemale = (data.gender() == Gender.FEMALE);
        RaceType race = data.race();

        int softShade = warmShade(skin, 0.88F);
        int noseShade = warmShade(skin, 0.82F);
        int softLight = warmShade(skin, 1.06F);

        int sclera = switch (race) {
            case DRAGONBORN -> 0xFFFFF3D6;
            case MUTANT -> (faceIdx == 4) ? 0xFF1E1A24 : 0xFFF2EAD8;
            case ORC -> 0xFFF5ECE0;
            default -> 0xFFFFFFFF;
        };

        int browCol = (race == RaceType.DRAGONBORN)
                ? warmShade(skin, 0.65F)
                : adjust(hairCol, isFemale ? 0.82F : 0.72F);

        img.setPixelRGBA(11, 12, toAbgr(softLight));
        img.setPixelRGBA(12, 12, toAbgr(softShade));
        img.setPixelRGBA(11, 13, toAbgr(noseShade));
        img.setPixelRGBA(12, 13, toAbgr(noseShade));

        if (isFemale) {
            int lashCol = 0xFF24181C;
            img.setPixelRGBA(8, 11, toAbgr(lashCol));
            img.setPixelRGBA(15, 11, toAbgr(lashCol));

            // Женские брови ровно на Y = 11 над глазами (не залезают под челку!)
            img.setPixelRGBA(9, 11, toAbgr(browCol));
            img.setPixelRGBA(10, 11, toAbgr(browCol));
            img.setPixelRGBA(13, 11, toAbgr(browCol));
            img.setPixelRGBA(14, 11, toAbgr(browCol));

            img.setPixelRGBA(9, 12, toAbgr(sclera));
            img.setPixelRGBA(10, 12, toAbgr(eyeCol));
            img.setPixelRGBA(13, 12, toAbgr(eyeCol));
            img.setPixelRGBA(14, 12, toAbgr(sclera));

            int blush = blend(skin, 0xFFE88A96, 0.22F);
            img.setPixelRGBA(9, 13, toAbgr(blush));
            img.setPixelRGBA(14, 13, toAbgr(blush));

            int lipTint = switch (faceIdx) {
                case 0 -> 0xFFD4727A;
                case 1 -> 0xFFB84E5C;
                case 2 -> 0xFFDE8276;
                case 3 -> 0xFF9E4256;
                default -> 0xFFC46870;
            };
            int lipCol = blend(skin, lipTint, 0.45F);
            img.setPixelRGBA(11, 14, toAbgr(lipCol));
            img.setPixelRGBA(12, 14, toAbgr(lipCol));
        } else {
            // Мужские брови аккуратно на Y = 11 над глазами (Y = 12), чтобы не пересекаться с 3D-челкой на Y = 9..10!
            img.setPixelRGBA(9, 11, toAbgr(browCol));
            img.setPixelRGBA(10, 11, toAbgr(browCol));
            img.setPixelRGBA(13, 11, toAbgr(browCol));
            img.setPixelRGBA(14, 11, toAbgr(browCol));

            img.setPixelRGBA(9, 12, toAbgr(sclera));
            img.setPixelRGBA(10, 12, toAbgr(eyeCol));
            img.setPixelRGBA(13, 12, toAbgr(eyeCol));
            img.setPixelRGBA(14, 12, toAbgr(sclera));

            int mouth = warmShade(skin, 0.76F);
            img.setPixelRGBA(11, 14, toAbgr(mouth));
            img.setPixelRGBA(12, 14, toAbgr(mouth));
        }

        int scarCol = blend(skin, 0xFF8A383C, 0.48F);

        switch (race) {
            case HUMAN -> {
                switch (faceIdx) {
                    case 0 -> {
                        img.setPixelRGBA(10, 13, toAbgr(softLight));
                        img.setPixelRGBA(13, 13, toAbgr(softLight));
                    }
                    case 1 -> {
                        img.setPixelRGBA(13, 10, toAbgr(scarCol));
                        img.setPixelRGBA(13, 11, toAbgr(scarCol));
                        img.setPixelRGBA(13, 13, toAbgr(scarCol));
                        img.setPixelRGBA(13, 14, toAbgr(scarCol));
                    }
                    case 2 -> {
                        int freckle = warmShade(skin, 0.80F);
                        img.setPixelRGBA(9, 13, toAbgr(freckle));
                        img.setPixelRGBA(10, 13, toAbgr(freckle));
                        img.setPixelRGBA(11, 12, toAbgr(freckle));
                        img.setPixelRGBA(13, 13, toAbgr(freckle));
                        img.setPixelRGBA(14, 13, toAbgr(freckle));
                    }
                    case 3 -> {
                        int warpaint = blend(skin, 0xFF1B304D, 0.58F);
                        img.setPixelRGBA(8, 12, toAbgr(warpaint));
                        img.setPixelRGBA(11, 12, toAbgr(warpaint));
                        img.setPixelRGBA(12, 12, toAbgr(warpaint));
                        img.setPixelRGBA(15, 12, toAbgr(warpaint));
                    }
                    case 4 -> {
                        img.setPixelRGBA(9, 13, toAbgr(softShade));
                        img.setPixelRGBA(10, 13, toAbgr(softShade));
                        img.setPixelRGBA(13, 13, toAbgr(softShade));
                        img.setPixelRGBA(14, 13, toAbgr(softShade));
                        img.setPixelRGBA(12, 14, toAbgr(scarCol));
                        img.setPixelRGBA(12, 15, toAbgr(scarCol));
                    }
                }
            }

            case ELF -> {
                int elvenTattoo = blend(skin, 0xFF2E855E, 0.50F);
                int starGold = blend(skin, 0xFFFFDF78, 0.72F);
                switch (faceIdx) {
                    case 0 -> {
                        img.setPixelRGBA(9, 13, toAbgr(softLight));
                        img.setPixelRGBA(14, 13, toAbgr(softLight));
                    }
                    case 1 -> {
                        img.setPixelRGBA(8, 11, toAbgr(elvenTattoo));
                        img.setPixelRGBA(8, 13, toAbgr(elvenTattoo));
                        img.setPixelRGBA(9, 14, toAbgr(elvenTattoo));
                        img.setPixelRGBA(15, 11, toAbgr(elvenTattoo));
                        img.setPixelRGBA(15, 13, toAbgr(elvenTattoo));
                        img.setPixelRGBA(14, 14, toAbgr(elvenTattoo));
                    }
                    case 2 -> {
                        img.setPixelRGBA(11, 10, toAbgr(starGold));
                        img.setPixelRGBA(12, 10, toAbgr(starGold));
                    }
                    case 3 -> {
                        int moonMark = blend(skin, 0xFF7B52AB, 0.52F);
                        img.setPixelRGBA(9, 13, toAbgr(moonMark));
                        img.setPixelRGBA(9, 14, toAbgr(moonMark));
                        img.setPixelRGBA(14, 13, toAbgr(moonMark));
                        img.setPixelRGBA(14, 14, toAbgr(moonMark));
                    }
                    case 4 -> {
                        img.setPixelRGBA(10, 11, toAbgr(scarCol));
                        img.setPixelRGBA(11, 12, toAbgr(scarCol));
                        img.setPixelRGBA(12, 13, toAbgr(scarCol));
                        img.setPixelRGBA(13, 14, toAbgr(scarCol));
                    }
                }
            }

            case MUTANT -> {
                int vein = blend(skin, 0xFF281236, 0.55F);
                switch (faceIdx) {
                    case 0 -> {
                        img.setPixelRGBA(9, 13, toAbgr(vein));
                        img.setPixelRGBA(10, 14, toAbgr(vein));
                        img.setPixelRGBA(14, 13, toAbgr(vein));
                        img.setPixelRGBA(13, 14, toAbgr(vein));
                    }
                    case 1 -> {
                        img.setPixelRGBA(13, 10, toAbgr(scarCol));
                        img.setPixelRGBA(14, 11, toAbgr(scarCol));
                        img.setPixelRGBA(13, 13, toAbgr(scarCol));
                        img.setPixelRGBA(14, 14, toAbgr(scarCol));
                    }
                    case 2 -> {
                        for (int x : new int[]{9, 10, 13, 14}) {
                            img.setPixelRGBA(x, 13, toAbgr(vein));
                        }
                    }
                    case 3 -> {
                        img.setPixelRGBA(8, 13, toAbgr(scarCol));
                        img.setPixelRGBA(9, 14, toAbgr(scarCol));
                        img.setPixelRGBA(10, 14, toAbgr(scarCol));
                    }
                    case 4 -> {
                        img.setPixelRGBA(8, 11, toAbgr(vein));
                        img.setPixelRGBA(8, 12, toAbgr(vein));
                        img.setPixelRGBA(15, 11, toAbgr(vein));
                        img.setPixelRGBA(15, 12, toAbgr(vein));
                    }
                }
            }

            case DWARF -> {
                switch (faceIdx) {
                    case 0 -> {
                        img.setPixelRGBA(8, 11, toAbgr(browCol));
                        img.setPixelRGBA(15, 11, toAbgr(browCol));
                    }
                    case 1 -> {
                        img.setPixelRGBA(11, 11, toAbgr(scarCol));
                        img.setPixelRGBA(12, 11, toAbgr(scarCol));
                    }
                    case 2 -> {
                        int woad = blend(skin, 0xFF285EA8, 0.58F);
                        img.setPixelRGBA(8, 12, toAbgr(woad));
                        img.setPixelRGBA(8, 13, toAbgr(woad));
                        img.setPixelRGBA(9, 13, toAbgr(woad));
                        img.setPixelRGBA(15, 12, toAbgr(woad));
                        img.setPixelRGBA(15, 13, toAbgr(woad));
                        img.setPixelRGBA(14, 13, toAbgr(woad));
                    }
                    case 3 -> {
                        int patch = 0xFF241C18;
                        int strap = 0xFF3E2E22;
                        img.setPixelRGBA(8, 11, toAbgr(strap));
                        img.setPixelRGBA(9, 11, toAbgr(patch));
                        img.setPixelRGBA(10, 11, toAbgr(patch));
                        img.setPixelRGBA(9, 12, toAbgr(patch));
                        img.setPixelRGBA(10, 12, toAbgr(patch));
                        img.setPixelRGBA(11, 11, toAbgr(strap));
                    }
                    case 4 -> {
                        int rune = blend(skin, 0xFFE5B53B, 0.70F);
                        img.setPixelRGBA(11, 10, toAbgr(rune));
                        img.setPixelRGBA(12, 10, toAbgr(rune));
                    }
                }
            }

            case DRAGONBORN -> {
                switch (faceIdx) {
                    case 0 -> {
                        int crest = blend(skin, 0xFFFFE5B8, 0.42F);
                        img.setPixelRGBA(11, 10, toAbgr(crest));
                        img.setPixelRGBA(12, 10, toAbgr(crest));
                    }
                    case 1 -> {
                        int glow = blend(skin, eyeCol, 0.65F);
                        img.setPixelRGBA(9, 13, toAbgr(glow));
                        img.setPixelRGBA(10, 13, toAbgr(glow));
                        img.setPixelRGBA(13, 13, toAbgr(glow));
                        img.setPixelRGBA(14, 13, toAbgr(glow));
                    }
                    case 2 -> {
                        int hornPlate = warmShade(skin, 0.68F);
                        for (int x : new int[]{8, 9, 14, 15}) {
                            img.setPixelRGBA(x, 13, toAbgr(hornPlate));
                        }
                    }
                    case 3 -> {
                        int scaleScar = blend(skin, 0xFFFFD0D0, 0.52F);
                        img.setPixelRGBA(13, 10, toAbgr(scaleScar));
                        img.setPixelRGBA(13, 11, toAbgr(scaleScar));
                        img.setPixelRGBA(13, 13, toAbgr(scaleScar));
                    }
                    case 4 -> {
                        int elder = blend(skin, 0xFFFFD778, 0.52F);
                        img.setPixelRGBA(8, 11, toAbgr(elder));
                        img.setPixelRGBA(8, 12, toAbgr(elder));
                        img.setPixelRGBA(15, 11, toAbgr(elder));
                        img.setPixelRGBA(15, 12, toAbgr(elder));
                    }
                }
            }

            case ORC -> {
                switch (faceIdx) {
                    case 0 -> {
                        // Полностью чистый лоб у типа 1 лица Орка (без полосок!)
                        img.setPixelRGBA(9, 13, toAbgr(softShade));
                        img.setPixelRGBA(14, 13, toAbgr(softShade));
                    }
                    case 1 -> {
                        int bonePaint = blend(skin, 0xFFEAE6D8, 0.68F);
                        img.setPixelRGBA(8, 12, toAbgr(bonePaint));
                        img.setPixelRGBA(9, 13, toAbgr(bonePaint));
                        img.setPixelRGBA(15, 12, toAbgr(bonePaint));
                        img.setPixelRGBA(14, 13, toAbgr(bonePaint));
                    }
                    case 2 -> {
                        int bloodPaint = blend(skin, 0xFFB82424, 0.68F);
                        img.setPixelRGBA(9, 13, toAbgr(bloodPaint));
                        img.setPixelRGBA(10, 13, toAbgr(bloodPaint));
                        img.setPixelRGBA(13, 13, toAbgr(bloodPaint));
                        img.setPixelRGBA(14, 13, toAbgr(bloodPaint));
                    }
                    case 3 -> {
                        img.setPixelRGBA(9, 10, toAbgr(scarCol));
                        img.setPixelRGBA(10, 12, toAbgr(0xFFDCE4E8));
                        img.setPixelRGBA(10, 13, toAbgr(scarCol));
                    }
                    case 4 -> {
                        int ashPaint = blend(skin, 0xFF1F2226, 0.68F);
                        img.setPixelRGBA(8, 12, toAbgr(ashPaint));
                        img.setPixelRGBA(15, 12, toAbgr(ashPaint));
                    }
                }
            }
        }
    }

    private static ResourceLocation generateDynamicExtras(PlayerAppearanceData data, int hash) {
        NativeImage img = new NativeImage(64, 64, true);
        int hairCol = data.getResolvedHairColor();
        int skinCol = data.getResolvedSkinColor();

        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 64; x++) {
                float strand = ((x % 4 == 0) ? 0.86F : ((x % 4 == 2) ? 1.08F : 0.97F)) * (1.05F - ((y % 16) / 16.0F) * 0.12F);
                img.setPixelRGBA(x, y, toAbgr(warmShade(hairCol, strand)));
            }
        }

        paintSkinRegion(img, 0, 32, 32, 32, skinCol, true);
        int nostrilCol = warmShade(skinCol, 0.45F);
        img.setPixelRGBA(3, 35, toAbgr(nostrilCol));
        img.setPixelRGBA(6, 35, toAbgr(nostrilCol));

        int hornRoot = blend(skinCol, 0xFF2B221C, 0.72F);
        int hornMid = blend(skinCol, 0xFFC2AA88, 0.82F);
        int hornTip = 0xFFF5ECE0;

        for (int y = 32; y < 64; y++) {
            int dy = y - 32;
            float progress = dy / 31.0F;
            for (int x = 32; x < 64; x++) {
                int dx = x - 32;
                int baseGrad = (progress < 0.45F)
                        ? blend(hornRoot, hornMid, progress / 0.45F)
                        : blend(hornMid, hornTip, (progress - 0.45F) / 0.55F);

                boolean ring = ((dy + (dx % 2)) % 3 == 0);
                float shade = ring ? 0.86F : (((dy % 3) == 1) ? 1.16F : 0.98F);
                img.setPixelRGBA(x, y, toAbgr(adjust(baseGrad, shade)));
            }
        }

        DynamicTexture tex = new DynamicTexture(img);
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(RPGCore.MODID, "extras_v6_fixed_" + Integer.toHexString(hash));
        Minecraft.getInstance().getTextureManager().register(loc, tex);
        return loc;
    }

    private static void tintPixel(NativeImage img, int x, int y, int targetRgb, float ratio) {
        int abgr = img.getPixelRGBA(x, y);
        int r = abgr & 0xFF;
        int g = (abgr >> 8) & 0xFF;
        int b = (abgr >> 16) & 0xFF;
        int currentRgb = (r << 16) | (g << 8) | b;
        img.setPixelRGBA(x, y, toAbgr(blend(currentRgb, targetRgb, ratio)));
    }

    private static int warmShade(int rgb, float factor) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        float rf = r * factor + (factor < 1.0F ? (1.0F - factor) * 14.0F : (factor - 1.0F) * 18.0F);
        float gf = g * factor;
        float bf = b * factor - (factor < 1.0F ? (1.0F - factor) * 10.0F : 0.0F);

        int nr = Math.min(255, Math.max(0, Math.round(rf)));
        int ng = Math.min(255, Math.max(0, Math.round(gf)));
        int nb = Math.min(255, Math.max(0, Math.round(bf)));
        return (nr << 16) | (ng << 8) | nb;
    }

    private static int blend(int c1, int c2, float ratio) {
        float inv = 1.0F - ratio;
        int r = Math.round(((c1 >> 16) & 0xFF) * inv + ((c2 >> 16) & 0xFF) * ratio);
        int g = Math.round(((c1 >> 8) & 0xFF) * inv + ((c2 >> 8) & 0xFF) * ratio);
        int b = Math.round((c1 & 0xFF) * inv + (c2 & 0xFF) * ratio);
        return (r << 16) | (g << 8) | b;
    }

    private static int adjust(int rgb, float factor) {
        int r = Math.min(255, Math.max(0, Math.round(((rgb >> 16) & 0xFF) * factor)));
        int g = Math.min(255, Math.max(0, Math.round(((rgb >> 8) & 0xFF) * factor)));
        int b = Math.min(255, Math.max(0, Math.round((rgb & 0xFF) * factor)));
        return (r << 16) | (g << 8) | b;
    }

    private static int toAbgr(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return 0xFF000000 | (b << 16) | (g << 8) | r;
    }
}