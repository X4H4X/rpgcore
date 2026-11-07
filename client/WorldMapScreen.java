package com.rpgcore.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.rpgcore.RPGCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class WorldMapScreen extends Screen {

    private static DynamicTexture mapTexture;
    private static ResourceLocation mapLocation;
    private static int currentTexW = 0;
    private static int currentTexH = 0;

    private double centerX;
    private double centerZ;
    private float zoom = 1.0F;
    private boolean dragging = false;

    private EditBox wpNameBox;
    private int selectedColorIdx = 0;
    private int pendingWpX;
    private int pendingWpZ;
    private boolean creatingWaypoint = false;

    private int winLeft, winTop, winWidth, winHeight;
    private int mapX, mapY, mapViewW, mapViewH;
    private int sidePanelX;
    private final int sidePanelW = 144;

    public WorldMapScreen() {
        super(Component.literal("Карта мира"));
    }

    private String clipText(String text, int maxW) {
        if (this.font.width(text) <= maxW) return text;
        return this.font.plainSubstrByWidth(text, Math.max(10, maxW - this.font.width("..."))) + "...";
    }

    @Override
    public void renderBackground(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    protected void init() {
        super.init();
        WorldMapManager.ensureLoaded();

        // ПУНКТ 9: Разворачиваем окно карты НА ВЕСЬ ЭКРАН (с аккуратным отступом 10px от краев монитора)!
        this.winLeft = 10;
        this.winTop = 10;
        this.winWidth = Math.max(320, this.width - 20);
        this.winHeight = Math.max(200, this.height - 20);

        this.mapX = this.winLeft + 10;
        this.mapY = this.winTop + 26;
        this.mapViewW = Math.max(160, this.winWidth - this.sidePanelW - 28);
        this.mapViewH = Math.max(120, this.winHeight - 46);
        this.sidePanelX = this.mapX + this.mapViewW + 10;

        Player p = Minecraft.getInstance().player;
        if (p != null && !creatingWaypoint && (this.centerX == 0 && this.centerZ == 0)) {
            this.centerX = p.getX();
            this.centerZ = p.getZ();
        }

        if (creatingWaypoint) {
            this.wpNameBox = new EditBox(this.font, sidePanelX, winTop + 42, sidePanelW - 8, 16, Component.literal("Название метки"));
            this.wpNameBox.setMaxLength(24);
            this.wpNameBox.setHint(Component.literal("§7Название..."));
            this.addRenderableWidget(this.wpNameBox);

            for (int i = 0; i < WorldMapManager.WAYPOINT_PALETTE.length; i++) {
                final int idx = i;
                int cx = sidePanelX + (i % 4) * 34;
                int cy = winTop + 66 + (i / 4) * 20;
                this.addRenderableWidget(Button.builder(Component.literal(""), b -> {
                    this.selectedColorIdx = idx;
                }).bounds(cx, cy, 28, 16).build());
            }

            this.addRenderableWidget(Button.builder(Component.literal("§aСохранить"), b -> {
                String name = this.wpNameBox.getValue().trim();
                if (name.isEmpty()) name = "Метка";
                String dim = Minecraft.getInstance().level != null
                        ? Minecraft.getInstance().level.dimension().location().toString() : "minecraft:overworld";
                WorldMapManager.WAYPOINTS.add(new WorldMapManager.Waypoint(
                        name, pendingWpX, pendingWpZ, WorldMapManager.WAYPOINT_PALETTE[selectedColorIdx], dim
                ));
                WorldMapManager.saveWaypoints();
                creatingWaypoint = false;
                rebuildWidgets();
            }).bounds(sidePanelX, winTop + 112, 66, 18).build());

            this.addRenderableWidget(Button.builder(Component.literal("§cОтмена"), b -> {
                creatingWaypoint = false;
                rebuildWidgets();
            }).bounds(sidePanelX + 70, winTop + 112, 66, 18).build());
        } else {
            this.addRenderableWidget(Button.builder(Component.literal("§e+ Метка здесь"), b -> {
                if (Minecraft.getInstance().player != null) {
                    this.pendingWpX = Mth.floor(Minecraft.getInstance().player.getX());
                    this.pendingWpZ = Mth.floor(Minecraft.getInstance().player.getZ());
                    this.creatingWaypoint = true;
                    rebuildWidgets();
                }
            }).bounds(sidePanelX, winTop + 28, sidePanelW - 8, 18).build());

            String dim = Minecraft.getInstance().level != null
                    ? Minecraft.getInstance().level.dimension().location().toString() : "";
            int maxRows = Math.max(4, (this.winHeight - 85) / 21);
            int row = 0;
            for (int i = 0; i < WorldMapManager.WAYPOINTS.size(); i++) {
                WorldMapManager.Waypoint wp = WorldMapManager.WAYPOINTS.get(i);
                if (!wp.dimension.equals(dim)) continue;
                if (row >= maxRows) break;
                final int wpIndex = i;
                int ry = winTop + 68 + row * 21;

                String shortLabel = clipText(wp.name, sidePanelW - 38);
                this.addRenderableWidget(Button.builder(Component.literal(shortLabel), b -> {
                    this.centerX = wp.x;
                    this.centerZ = wp.z;
                }).bounds(sidePanelX, ry, sidePanelW - 30, 17).build());

                this.addRenderableWidget(Button.builder(Component.literal("§c✕"), b -> {
                    WorldMapManager.WAYPOINTS.remove(wpIndex);
                    WorldMapManager.saveWaypoints();
                    rebuildWidgets();
                }).bounds(sidePanelX + sidePanelW - 27, ry, 19, 17).build());

                row++;
            }
        }
    }

    private void updateMapCanvas() {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || mapViewW <= 0 || mapViewH <= 0) return;

        if (mapTexture == null || currentTexW != mapViewW || currentTexH != mapViewH) {
            if (mapTexture != null) {
                mapTexture.close();
            }
            currentTexW = mapViewW;
            currentTexH = mapViewH;
            mapTexture = new DynamicTexture(currentTexW, currentTexH, false);
            mapLocation = ResourceLocation.fromNamespaceAndPath(RPGCore.MODID, "world_map_fullscreen_" + currentTexW + "x" + currentTexH);
            mc.getTextureManager().register(mapLocation, mapTexture);
        }

        NativeImage img = mapTexture.getPixels();
        if (img == null) return;

        WorldMapManager.beginFrameScan();
        double scale = 1.0D / this.zoom;

        for (int py = 0; py < currentTexH; py++) {
            int wz = Mth.floor(this.centerZ + (py - currentTexH / 2.0D) * scale);
            for (int px = 0; px < currentTexW; px++) {
                int wx = Mth.floor(this.centerX + (px - currentTexW / 2.0D) * scale);
                int col = WorldMapManager.sampleSurfaceAbgrFast(level, wx, wz);
                img.setPixelRGBA(px, py, col);
            }
        }
        mapTexture.upload();
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        SkyrimThemeRenderer.renderBlurredDarkBackground(this, gui, partialTick);
        SkyrimThemeRenderer.drawRpgFrame(gui, winLeft, winTop, winWidth, winHeight, SkyrimThemeRenderer.HUD_BORDER_GOLD);

        updateMapCanvas();

        if (mapLocation != null) {
            gui.blit(mapLocation, mapX, mapY, mapViewW, mapViewH, 0, 0, currentTexW, currentTexH, currentTexW, currentTexH);
            gui.renderOutline(mapX - 1, mapY - 1, mapViewW + 2, mapViewH + 2, SkyrimThemeRenderer.HUD_BORDER_GOLD);
        }

        gui.drawString(this.font, "§6§lКарта мира §7(ЛКМ — двигать, Колесико — зум, ПКМ — новая метка)", winLeft + 10, winTop + 10, 0xFFFFFFFF, true);

        // Вертикальный разделитель боковой панели меток
        gui.fill(sidePanelX - 5, winTop + 8, sidePanelX - 4, winTop + winHeight - 8, 0xFF363942);
        gui.drawString(this.font, creatingWaypoint ? "§6Новая метка:" : "§6Метки:", sidePanelX, winTop + 12, 0xFFFFFFFF, true);
        if (!creatingWaypoint) {
            gui.drawString(this.font, "§7Список точек:", sidePanelX, winTop + 54, 0xFFCCCCCC, false);
        }

        double scale = 1.0D / this.zoom;
        String dim = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.dimension().location().toString() : "";

        String hoveredWaypointInfo = null;

        for (WorldMapManager.Waypoint wp : WorldMapManager.WAYPOINTS) {
            if (!wp.dimension.equals(dim)) continue;
            int sx = mapX + (int) Math.round(mapViewW / 2.0D + (wp.x - this.centerX) / scale);
            int sy = mapY + (int) Math.round(mapViewH / 2.0D + (wp.z - this.centerZ) / scale);
            if (sx >= mapX + 4 && sx <= mapX + mapViewW - 4 && sy >= mapY + 4 && sy <= mapY + mapViewH - 4) {
                gui.fill(sx - 3, sy - 3, sx + 3, sy + 3, wp.colorHex | 0xFF000000);
                gui.renderOutline(sx - 4, sy - 4, 8, 8, 0xFF0A0B0F);

                if (mouseX >= sx - 5 && mouseX <= sx + 5 && mouseY >= sy - 5 && mouseY <= sy + 5) {
                    hoveredWaypointInfo = wp.name + " §7(" + wp.x + ", " + wp.z + ")";
                }
            }
        }

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            int px = mapX + (int) Math.round(mapViewW / 2.0D + (player.getX() - this.centerX) / scale);
            int pz = mapY + (int) Math.round(mapViewH / 2.0D + (player.getZ() - this.centerZ) / scale);
            if (px >= mapX + 4 && px <= mapX + mapViewW - 4 && pz >= mapY + 4 && pz <= mapY + mapViewH - 4) {
                gui.fill(px - 2, pz - 2, px + 3, pz + 3, 0xFFFFFFFF);
                gui.renderOutline(px - 3, pz - 3, 7, 7, 0xFF0A0B0F);
            }
        }

        if (mouseX >= mapX && mouseX < mapX + mapViewW && mouseY >= mapY && mouseY < mapY + mapViewH) {
            int hoverWX = Mth.floor(this.centerX + (mouseX - mapX - mapViewW / 2.0D) * scale);
            int hoverWZ = Mth.floor(this.centerZ + (mouseY - mapY - mapViewH / 2.0D) * scale);
            String coordStr = "Курсор: X: " + hoverWX + "  Z: " + hoverWZ + "  (Масштаб: " + String.format("%.1fx", this.zoom) + ")";
            gui.drawString(this.font, clipText(coordStr, mapViewW), mapX, mapY + mapViewH + 5, 0xFFD4AF37, true);
        }

        super.render(gui, mouseX, mouseY, partialTick);

        if (creatingWaypoint) {
            for (int i = 0; i < WorldMapManager.WAYPOINT_PALETTE.length; i++) {
                int cx = sidePanelX + (i % 4) * 34;
                int cy = winTop + 66 + (i / 4) * 20;
                gui.fill(cx + 2, cy + 2, cx + 26, cy + 14, WorldMapManager.WAYPOINT_PALETTE[i]);
                if (i == selectedColorIdx) {
                    gui.renderOutline(cx, cy, 28, 16, 0xFFFFFFFF);
                }
            }
        }

        if (hoveredWaypointInfo != null) {
            gui.renderTooltip(this.font, Component.literal(hoveredWaypointInfo), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= mapX && mouseX < mapX + mapViewW && mouseY >= mapY && mouseY < mapY + mapViewH) {
            if (button == 1) {
                double scale = 1.0D / this.zoom;
                this.pendingWpX = Mth.floor(this.centerX + (mouseX - mapX - mapViewW / 2.0D) * scale);
                this.pendingWpZ = Mth.floor(this.centerZ + (mouseY - mapY - mapViewH / 2.0D) * scale);
                this.creatingWaypoint = true;
                rebuildWidgets();
                return true;
            } else if (button == 0) {
                this.dragging = true;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) this.dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.dragging && button == 0) {
            double scale = 1.0D / this.zoom;
            this.centerX -= dragX * scale;
            this.centerZ -= dragY * scale;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0) {
            this.zoom = Math.min(6.0F, this.zoom * 1.25F);
        } else if (scrollY < 0) {
            this.zoom = Math.max(0.25F, this.zoom / 1.25F);
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}