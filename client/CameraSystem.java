package com.rpgcore.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class CameraSystem {

    private static boolean rightShoulder = false;
    private static float currentShoulderOffset = -1.0F;

    public static boolean renderingFirstPersonBody = false;

    // Включен ли рендер 3D-тела от 1-го лица (можно переключать в Настройках игры)
    private static boolean firstPersonBodyEnabled = true;
    private static boolean configLoaded = false;

    public static boolean isFirstPersonBodyEnabled() {
        ensureLoaded();
        return firstPersonBodyEnabled;
    }

    public static void setFirstPersonBodyEnabled(boolean enabled) {
        firstPersonBodyEnabled = enabled;
        saveConfig();
    }

    public static void toggleFirstPersonBody() {
        setFirstPersonBodyEnabled(!isFirstPersonBodyEnabled());
    }

    public static void toggleShoulder() {
        rightShoulder = !rightShoulder;
    }

    public static boolean isRightShoulder() {
        return rightShoulder;
    }

    public static float updateAndGetShoulderFactor(float partialTick) {
        float target = rightShoulder ? 1.0F : -1.0F;
        currentShoulderOffset = Mth.lerp(0.14F, currentShoulderOffset, target);
        return currentShoulderOffset;
    }

    private static void ensureLoaded() {
        if (configLoaded) return;
        configLoaded = true;
        try {
            File file = new File(Minecraft.getInstance().gameDirectory, "rpgcore_client.txt");
            if (file.exists()) {
                String content = Files.readString(file.toPath(), StandardCharsets.UTF_8).trim();
                firstPersonBodyEnabled = !content.contains("firstPersonBody=false");
            }
        } catch (Exception ignored) {
        }
    }

    private static void saveConfig() {
        try {
            File file = new File(Minecraft.getInstance().gameDirectory, "rpgcore_client.txt");
            Files.writeString(file.toPath(), "firstPersonBody=" + firstPersonBodyEnabled + "\n", StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }
}