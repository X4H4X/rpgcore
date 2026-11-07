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

    /**
     * Полностью прозрачная текстура слоя брони 64x32, чтобы крестьянская одежда и плащи
     * никогда не рендерились как ванильная серая/кожаная броня на стойке и игроке!
     */
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
        result = 31 * result + d.faceVariant();
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

        paintSkinRegion(img, 0, 0, 32, 16, baseSkin, dragonScales);
        paintSkinRegion(img, 16, 16, 24, 16, baseSkin, dragonScales);
        paintSkinRegion(img, 40, 16, 16, 16, baseSkin, dragonScales);
        paintSkinRegion(img, 32, 48, 16, 16, baseSkin, dragonScales);
        paintSkinRegion(img, 0, 16, 16, 16, baseSkin, dragonScales);
        paintSkinRegion(img, 16, 48, 16, 16, baseSkin, dragonScales);

        if (data.race() == RaceType.MUTANT) {
            paintMutantBodyMarks(img, baseSkin);
        }

        if (data.gender() == Gender.MALE && data.race() != RaceType.DRAGONBORN) {
            int b = Math.floorMod(data.beardStyle(), 10);
            if (b == 1 || b == 2) {
                paintSmoothStubble(img, baseSkin, data.getResolvedHairColor(), b == 2);
            }
        }

        paintFace(img, data);
        paintUnderwear(img, data.gender());

        DynamicTexture tex = new DynamicTexture(img);
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(RPGCore.MODID, "skin_" + Integer.toHexString(hash));
        Minecraft.getInstance().getTextureManager().register(loc, tex);
        return loc;
    }

    private static void paintSkinRegion(NativeImage img, int sx, int sy, int w, int h, int baseHex, boolean dragonScales) {
        for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
                float factor;
                if (dragonScales) {
                    int sxMod = (dx + (dy % 2) * 2) % 4;
                    int syMod = dy % 3;
                    if (syMod == 0 || sxMod == 0) factor = 0.78F;
                    else if (syMod == 1 && sxMod == 2) factor = 1.14F;
                    else factor = 0.98F;
                } else {
                    int noise = ((dx * 17 + dy * 31) & 7);
                    factor = 0.97F + (noise / 7.0F) * 0.05F;
                    if (dy == h - 1 || dx == 0) factor *= 0.93F;
                }
                img.setPixelRGBA(sx + dx, sy + dy, toAbgr(adjust(baseHex, factor)));
            }
        }
    }

    private static void paintMutantBodyMarks(NativeImage img, int skinHex) {
        int veinCol = blend(skinHex, 0xFF28202B, 0.35F);
        int scarCol = blend(skinHex, 0xFF5C2E2E, 0.38F);
        img.setPixelRGBA(21, 20, toAbgr(veinCol));
        img.setPixelRGBA(22, 21, toAbgr(veinCol));
        img.setPixelRGBA(25, 20, toAbgr(veinCol));
        img.setPixelRGBA(24, 21, toAbgr(scarCol));
        img.setPixelRGBA(23, 22, toAbgr(scarCol));
    }

    private static void paintSmoothStubble(NativeImage img, int skinHex, int hairHex, boolean heavy) {
        float baseAlpha = heavy ? 0.42F : 0.22F;
        for (int x = 8; x <= 15; x++) {
            for (int y = 12; y <= 15; y++) {
                float mask = 0.0F;
                if (y == 15) mask = 1.0F;
                else if (y == 14) {
                    if (x == 8 || x == 9 || x == 14 || x == 15) mask = 0.85F;
                    else if (x == 10 || x == 13) mask = 0.65F;
                } else if (y == 13) {
                    if (x == 8 || x == 15) mask = 0.70F;
                    else if (x >= 10 && x <= 13) mask = 0.75F;
                } else if (y == 12 && (x == 8 || x == 15)) {
                    mask = 0.55F;
                }
                if (mask > 0.0F) {
                    float micro = (((x * 3 + y * 5) & 3) - 1.5F) * 0.03F;
                    float t = Math.min(0.75F, Math.max(0.05F, baseAlpha * mask + micro));
                    img.setPixelRGBA(x, y, toAbgr(blend(skinHex, hairHex, t)));
                }
            }
        }
        for (int y = 11; y <= 15; y++) {
            float yFactor = (y >= 13) ? 0.85F : 0.55F;
            for (int x : new int[]{6, 7, 16, 17}) {
                img.setPixelRGBA(x, y, toAbgr(blend(skinHex, hairHex, baseAlpha * yFactor)));
            }
        }
    }

    private static void paintUnderwear(NativeImage img, Gender gender) {
        int linenBase = 0xFF786856;
        int linenShade = 0xFF635545;
        int linenHighlight = 0xFF877663;
        int beltCol = 0xFF3D3024;

        // 1. Пояс и низ торса спереди/сзади/по бокам
        for (int x = 16; x < 40; x++) {
            img.setPixelRGBA(x, 28, toAbgr(beltCol));
            for (int y = 29; y < 32; y++) {
                int c = (y == 31) ? linenShade : ((x % 4 == 0) ? linenShade : ((x % 4 == 2) ? linenHighlight : linenBase));
                img.setPixelRGBA(x, y, toAbgr(c));
            }
        }

        // 2. ИСПРАВЛЕНО (image_9a1d49.jpg): Нижняя крышка куба ТОРСА (x = 28..35, y = 16..19),
        // чтобы при сидении в лодке низ туловища никогда не светился голым телом или пустотой!
        for (int x = 28; x < 36; x++) {
            for (int y = 16; y < 20; y++) {
                int c = ((x + y) % 2 == 0) ? linenShade : linenBase;
                img.setPixelRGBA(x, y, toAbgr(c));
            }
        }

        // 3. ИСПРАВЛЕНО (image_9a1d49.jpg): Верхние крышки кубов ПРАВОЙ и ЛЕВОЙ НОГИ
        // (Правая нога верх: x = 4..7, y = 16..19; Левая нога верх: x = 20..23, y = 48..51).
        // Когда ноги сгибаются на 90 градусов в лодке, эти грани становятся видны сверху/сзади!
        for (int dx = 4; dx < 8; dx++) {
            for (int dy = 0; dy < 4; dy++) {
                int c = ((dx + dy) % 2 == 0) ? linenShade : linenBase;
                img.setPixelRGBA(dx, 16 + dy, toAbgr(c));       // Верх правой ноги
                img.setPixelRGBA(16 + dx, 48 + dy, toAbgr(c));  // Верх левой ноги
            }
        }

        // 4. Боковые стороны бедер (удлинены до y = 20..25 для плотного перекрытия сгиба)
        for (int x = 0; x < 16; x++) {
            for (int y = 20; y < 25; y++) {
                int c = (y == 24) ? beltCol : ((x % 4 == 0) ? linenShade : linenBase);
                img.setPixelRGBA(x, y, toAbgr(c));
                img.setPixelRGBA(16 + x, 32 + y, toAbgr(c));
            }
        }

        if (gender == Gender.FEMALE) {
            for (int x = 16; x < 40; x++) {
                for (int y = 18; y <= 26; y++) {
                    int c;
                    if (y == 18 || y == 26) c = beltCol;
                    else if (y == 19 || y == 25) c = linenShade;
                    else c = (x % 3 == 0) ? linenShade : ((x % 3 == 1) ? linenHighlight : linenBase);
                    img.setPixelRGBA(x, y, toAbgr(c));
                }
            }
        }
    }

    private static void paintFace(NativeImage img, PlayerAppearanceData data) {
        int skin = data.getResolvedSkinColor();
        int eyeCol = data.getResolvedEyeColor();
        int hairCol = data.getResolvedHairColor();
        int faceIdx = Math.floorMod(data.faceVariant(), 10);
        boolean isFemale = (data.gender() == Gender.FEMALE);
        boolean isMutant = (data.race() == RaceType.MUTANT);
        boolean isDragon = (data.race() == RaceType.DRAGONBORN);

        int sclera = isDragon ? 0xFFFDF8E8 : (isMutant ? 0xFFEAE2D2 : 0xFFFDFDFD);
        int browCol = isDragon ? adjust(skin, 0.55F) : adjust(hairCol, isFemale ? 0.86F : 0.78F);

        if (isFemale) {
            if (isDragon) {
                img.setPixelRGBA(9, 10, toAbgr(browCol));
                img.setPixelRGBA(10, 9, toAbgr(browCol));
                img.setPixelRGBA(13, 9, toAbgr(browCol));
                img.setPixelRGBA(14, 10, toAbgr(browCol));

                int dragonLash = 0xFF140D0B;
                img.setPixelRGBA(8, 11, toAbgr(dragonLash));
                img.setPixelRGBA(9, 11, toAbgr(dragonLash));
                img.setPixelRGBA(10, 11, toAbgr(blend(skin, dragonLash, 0.55F)));
                img.setPixelRGBA(13, 11, toAbgr(blend(skin, dragonLash, 0.55F)));
                img.setPixelRGBA(14, 11, toAbgr(dragonLash));
                img.setPixelRGBA(15, 11, toAbgr(dragonLash));

                img.setPixelRGBA(9, 12, toAbgr(0xFFFFFFFF));
                img.setPixelRGBA(10, 12, toAbgr(eyeCol));
                img.setPixelRGBA(13, 12, toAbgr(eyeCol));
                img.setPixelRGBA(14, 12, toAbgr(0xFFFFFFFF));

                int scaleBlush = blend(skin, 0xFFFFB5B5, 0.35F);
                img.setPixelRGBA(8, 13, toAbgr(scaleBlush));
                img.setPixelRGBA(9, 13, toAbgr(scaleBlush));
                img.setPixelRGBA(14, 13, toAbgr(scaleBlush));
                img.setPixelRGBA(15, 13, toAbgr(scaleBlush));

                int dragonLips = blend(skin, 0xFFD85A6C, 0.48F);
                img.setPixelRGBA(11, 14, toAbgr(dragonLips));
                img.setPixelRGBA(12, 14, toAbgr(dragonLips));
            } else {
                img.setPixelRGBA(9, 10, toAbgr(browCol));
                img.setPixelRGBA(10, 10, toAbgr(browCol));
                img.setPixelRGBA(13, 10, toAbgr(browCol));
                img.setPixelRGBA(14, 10, toAbgr(browCol));

                int upperLid = blend(skin, 0xFF2B1E1A, 0.32F);
                img.setPixelRGBA(9, 11, toAbgr(upperLid));
                img.setPixelRGBA(10, 11, toAbgr(upperLid));
                img.setPixelRGBA(13, 11, toAbgr(upperLid));
                img.setPixelRGBA(14, 11, toAbgr(upperLid));

                img.setPixelRGBA(9, 12, toAbgr(sclera));
                img.setPixelRGBA(10, 12, toAbgr(eyeCol));
                img.setPixelRGBA(13, 12, toAbgr(eyeCol));
                img.setPixelRGBA(14, 12, toAbgr(sclera));

                int blush = isMutant ? blend(skin, 0xFFB87A7A, 0.16F) : blend(skin, 0xFFE58C8C, 0.24F);
                img.setPixelRGBA(8, 13, toAbgr(blush));
                img.setPixelRGBA(9, 13, toAbgr(blush));
                img.setPixelRGBA(14, 13, toAbgr(blush));
                img.setPixelRGBA(15, 13, toAbgr(blush));

                int lipTint = switch (faceIdx % 5) {
                    case 0 -> 0xFFC96B72;
                    case 1 -> 0xFFB8545C;
                    case 2 -> 0xFFD47A6A;
                    case 3 -> 0xFFA3424A;
                    default -> 0xFFC26E6E;
                };
                int lipCol = blend(skin, lipTint, 0.48F);
                img.setPixelRGBA(11, 14, toAbgr(lipCol));
                img.setPixelRGBA(12, 14, toAbgr(lipCol));
            }
        } else {
            int browY = (faceIdx % 3 == 0) ? 11 : 10;
            img.setPixelRGBA(9, browY, toAbgr(browCol));
            img.setPixelRGBA(10, browY, toAbgr(browCol));
            img.setPixelRGBA(13, browY, toAbgr(browCol));
            img.setPixelRGBA(14, browY, toAbgr(browCol));

            img.setPixelRGBA(9, 12, toAbgr(sclera));
            img.setPixelRGBA(10, 12, toAbgr(eyeCol));
            img.setPixelRGBA(13, 12, toAbgr(eyeCol));
            img.setPixelRGBA(14, 12, toAbgr(sclera));

            int nose = adjust(skin, 0.82F);
            img.setPixelRGBA(11, 13, toAbgr(nose));
            img.setPixelRGBA(12, 13, toAbgr(nose));

            int mouth = adjust(skin, 0.74F);
            img.setPixelRGBA(11, 14, toAbgr(mouth));
            img.setPixelRGBA(12, 14, toAbgr(mouth));
        }

        int scar = blend(skin, 0xFF6E2B2B, 0.38F);
        if (!isFemale && (faceIdx == 1 || faceIdx == 4 || faceIdx == 7)) {
            img.setPixelRGBA(13, 9, toAbgr(scar));
            img.setPixelRGBA(13, 10, toAbgr(scar));
            img.setPixelRGBA(13, 13, toAbgr(scar));
        } else if (isFemale && (faceIdx == 2 || faceIdx == 7) && !isDragon) {
            int freckle = adjust(skin, 0.84F);
            img.setPixelRGBA(10, 13, toAbgr(freckle));
            img.setPixelRGBA(13, 13, toAbgr(freckle));
        }
    }

    private static ResourceLocation generateDynamicExtras(PlayerAppearanceData data, int hash) {
        NativeImage img = new NativeImage(64, 64, true);
        int hairCol = data.getResolvedHairColor();
        int skinCol = data.getResolvedSkinColor();

        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 64; x++) {
                float strand = ((x % 4 == 0) ? 0.88F : ((x % 4 == 2) ? 1.08F : 0.97F)) * (1.04F - ((y % 16) / 16.0F) * 0.14F);
                img.setPixelRGBA(x, y, toAbgr(adjust(hairCol, strand)));
            }
        }

        paintSkinRegion(img, 0, 32, 32, 32, skinCol, true);
        int nostrilCol = adjust(skinCol, 0.45F);
        img.setPixelRGBA(3, 35, toAbgr(nostrilCol));
        img.setPixelRGBA(6, 35, toAbgr(nostrilCol));

        int hornRoot = blend(skinCol, 0xFF3A2E25, 0.65F);
        int hornMid = blend(skinCol, 0xFFB89F7D, 0.78F);
        int hornTip = 0xFFEADBC4;

        for (int y = 32; y < 64; y++) {
            int dy = y - 32;
            float progress = dy / 31.0F;
            for (int x = 32; x < 64; x++) {
                int dx = x - 32;
                int baseGrad = (progress < 0.45F)
                        ? blend(hornRoot, hornMid, progress / 0.45F)
                        : blend(hornMid, hornTip, (progress - 0.45F) / 0.55F);

                boolean growthRingRidge = ((dy + (dx % 2)) % 3 == 0);
                boolean groove = (dx % 4 == 0);

                float shade = 1.0F;
                if (growthRingRidge) shade *= 0.80F;
                else if ((dy % 3) == 1) shade *= 1.10F;
                if (groove) shade *= 0.88F;

                img.setPixelRGBA(x, y, toAbgr(adjust(baseGrad, shade)));
            }
        }

        DynamicTexture tex = new DynamicTexture(img);
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(RPGCore.MODID, "extras_" + Integer.toHexString(hash));
        Minecraft.getInstance().getTextureManager().register(loc, tex);
        return loc;
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