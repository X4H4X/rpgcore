package com.rpgcore.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.rpgcore.data.ClanData;
import com.rpgcore.data.CorpseMenu;
import com.rpgcore.data.ExtraEquipmentData;
import com.rpgcore.data.ExtraInventorySlot;
import com.rpgcore.data.ModAttachments;
import com.rpgcore.data.OrcGiantBedBlock;
import com.rpgcore.data.PlayerAppearanceData;
import com.rpgcore.data.RaceType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SkyrimThemeRenderer {

    private static Field scrollOffsField;
    private static Field selectedTabField;
    private static Field searchBoxField;
    private static Field vehicleField;
    private static Field clanScreenClanField;
    private static Method blurMethod;

    public static final int HUD_BG_DARK = 0xE60A0B0F;
    public static final int HUD_BORDER_STEEL = 0xFF363942;
    public static final int HUD_BORDER_GOLD = 0xFF8C7032;
    public static final int HUD_CORNER_RIVET = 0xFFD4AF37;

    private static final ItemStack ICON_FOOD_STACK = new ItemStack(Items.COOKED_BEEF);
    private static final ItemStack ICON_ARMOR_STACK = new ItemStack(Items.IRON_CHESTPLATE);
    private static final ItemStack ICON_MOUNT_STACK = new ItemStack(Items.SADDLE);

    public static boolean showManaIndicator = false;
    public static int clientManaValue = 0;

    public static int clientReviveProgressTicks = 0;

    public static class PickupNotification {
        public final Item item;
        public final ItemStack iconStack;
        public int addedCount;
        public int totalCount;
        public int ticksLeft;
        public static final int MAX_TICKS = 75;

        public PickupNotification(ItemStack stack, int added, int total) {
            this.item = stack.getItem();
            this.iconStack = stack.copyWithCount(1);
            this.addedCount = added;
            this.totalCount = total;
            this.ticksLeft = MAX_TICKS;
        }
    }

    private static final List<PickupNotification> PICKUP_LOG = new ArrayList<>();

    private static int dayBannerNumber = 1;
    private static int dayBannerTicks = 0;
    private static final int DAY_BANNER_MAX_TICKS = 130;

    public static void notifyItemPickedUp(ItemStack stack, int added, int totalInInventory) {
        if (stack.isEmpty() || added <= 0) return;
        for (PickupNotification existing : PICKUP_LOG) {
            if (existing.item == stack.getItem()) {
                existing.addedCount += added;
                existing.totalCount = totalInInventory;
                existing.ticksLeft = PickupNotification.MAX_TICKS;
                return;
            }
        }
        if (PICKUP_LOG.size() >= 6) {
            PICKUP_LOG.removeFirst();
        }
        PICKUP_LOG.add(new PickupNotification(stack, added, totalInInventory));
    }

    public static void triggerNewDayBanner(int dayNumber) {
        int targetDay = Math.max(1, dayNumber);
        if (dayBannerTicks > 0 && dayBannerNumber == targetDay) {
            return;
        }
        dayBannerNumber = targetDay;
        dayBannerTicks = DAY_BANNER_MAX_TICKS;
    }

    public static void tickHudNotifications() {
        Iterator<PickupNotification> it = PICKUP_LOG.iterator();
        while (it.hasNext()) {
            PickupNotification n = it.next();
            n.ticksLeft--;
            if (n.ticksLeft <= 0) {
                it.remove();
            }
        }
        if (dayBannerTicks > 0) {
            dayBannerTicks--;
        }
        if (clientReviveProgressTicks > 0) {
            clientReviveProgressTicks--;
        }
    }

    private static final String[] ICON_HAT = {
            "................",
            "................",
            ".....######.....",
            "....#......#....",
            "....#......#....",
            "....########....",
            "..############..",
            ".#............#.",
            ".##############.",
            "................"
    };

    private static final String[] ICON_SHIRT = {
            "...###....###...",
            "..#...####...#..",
            ".#............#.",
            "#..#........#..#",
            "#..#........#..#",
            ".###........###.",
            "...#........#...",
            "...##########...",
            "...#........#...",
            "...##########..."
    };

    private static final String[] ICON_PANTS = {
            "...##########...",
            "...#........#...",
            "...##########...",
            "...#...##...#...",
            "...#..#..#..#...",
            "...#..#..#..#...",
            "...#..#..#..#...",
            "...#..#..#..#...",
            "...#..#..#..#...",
            "...###....###..."
    };

    private static final String[] ICON_SHOES = {
            "................",
            "..####....####..",
            "..#..#....#..#..",
            "..#..#....#..#..",
            "..#..#....#..#..",
            ".##..#....#..##.",
            "#....#....#....#",
            "######....######",
            "................"
    };

    private static final String[] ICON_CAPE = {
            "....########....",
            "...#..####..#...",
            "...#........#...",
            "..#..........#..",
            "..#..........#..",
            "..#..........#..",
            ".#............#.",
            ".#............#.",
            ".#...#....#...#.",
            "..###.####.###.."
    };

    private static final String[] ICON_BACKPACK = {
            ".....######.....",
            "....#......#....",
            "..############..",
            ".#..########..#.",
            ".#..#......#..#.",
            ".#..########..#.",
            ".#............#.",
            ".#...######...#.",
            ".#............#.",
            "..############.."
    };

    static {
        try {
            for (Field f : CreativeModeInventoryScreen.class.getDeclaredFields()) {
                if (f.getType() == float.class) {
                    f.setAccessible(true);
                    scrollOffsField = f;
                } else if (f.getType() == CreativeModeTab.class) {
                    f.setAccessible(true);
                    selectedTabField = f;
                } else if (f.getType() == EditBox.class) {
                    f.setAccessible(true);
                    searchBoxField = f;
                }
            }
        } catch (Exception ignored) {
        }

        try {
            for (Field f : Entity.class.getDeclaredFields()) {
                if (f.getType() == Entity.class) {
                    f.setAccessible(true);
                    vehicleField = f;
                    break;
                }
            }
        } catch (Exception ignored) {
        }

        try {
            for (Field f : ClanScreen.class.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) && f.getType() == ClanData.class) {
                    f.setAccessible(true);
                    clanScreenClanField = f;
                    break;
                }
            }
        } catch (Exception ignored) {
        }

        try {
            for (Method m : Screen.class.getDeclaredMethods()) {
                if (m.getName().contains("Blurred") || m.getName().equals("renderBlurredBackground")) {
                    m.setAccessible(true);
                    blurMethod = m;
                    break;
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static ClanData getActiveClientClan() {
        if (clanScreenClanField != null) {
            try {
                return (ClanData) clanScreenClanField.get(null);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    public static void renderBlurredDarkBackground(Screen screen, GuiGraphics gui, float partialTick) {
        try {
            if (blurMethod != null) {
                if (blurMethod.getParameterCount() == 1) {
                    blurMethod.invoke(screen, partialTick);
                } else if (blurMethod.getParameterCount() == 0) {
                    blurMethod.invoke(screen);
                }
            } else {
                Minecraft.getInstance().gameRenderer.processBlurEffect(partialTick);
                Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
            }
        } catch (Throwable ignored) {
        }

        int w = screen.width;
        int h = screen.height;
        gui.fillGradient(0, 0, w, h, 0xE6161820, 0xF20D0F14);
        gui.fillGradient(0, 0, w, 28, 0x88050608, 0x00050608);
        gui.fillGradient(0, h - 28, w, h, 0x00050608, 0x88050608);
    }

    public static void renderStandingDoll(GuiGraphics gui, int x1, int y1, int x2, int y2, int scale,
                                          float yOffset, float mouseX, float mouseY, LivingEntity entity) {
        Entity savedVehicle = null;
        Pose savedPose = entity.getPose();
        try {
            if (vehicleField != null) {
                savedVehicle = (Entity) vehicleField.get(entity);
                vehicleField.set(entity, null);
            }
            entity.setPose(Pose.STANDING);
            InventoryScreen.renderEntityInInventoryFollowsMouse(gui, x1, y1, x2, y2, scale, yOffset, mouseX, mouseY, entity);
        } catch (Throwable t) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(gui, x1, y1, x2, y2, scale, yOffset, mouseX, mouseY, entity);
        } finally {
            try {
                if (vehicleField != null && savedVehicle != null) {
                    vehicleField.set(entity, savedVehicle);
                }
            } catch (Throwable ignored) {
            }
            entity.setPose(savedPose);
        }
    }

    public static void drawRpgFrame(GuiGraphics gui, int x, int y, int w, int h, int accentBorderCol) {
        gui.fill(x, y, x + w, y + h, HUD_BG_DARK);
        if (w > 4 && h > 4) {
            gui.fill(x + 1, y + 1, x + w - 1, y + 2, 0x44FFFFFF);
            gui.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0x88000000);
        }
        gui.renderOutline(x, y, w, h, accentBorderCol);
        gui.fill(x, y, x + 1, y + 1, HUD_CORNER_RIVET);
        gui.fill(x + w - 1, y, x + w, y + 1, HUD_CORNER_RIVET);
        gui.fill(x, y + h - 1, x + 1, y + h, HUD_CORNER_RIVET);
        gui.fill(x + w - 1, y + h - 1, x + w, y + h, HUD_CORNER_RIVET);
    }

    public static void drawSegmentedCapsuleBar(GuiGraphics gui, int x, int y, int w, int h,
                                               float ratio, int segments,
                                               int borderTint, int darkEdge, int mainCol, int softCenter, int subtleTop) {
        ratio = Mth.clamp(ratio, 0.0F, 1.0F);

        gui.fill(x + 2, y + h, x + w - 2, y + h + 1, 0x66000000);

        gui.fill(x + 1, y, x + w - 1, y + h, borderTint);
        gui.fill(x, y + 1, x + w, y + h - 1, borderTint);

        int inX = x + 1;
        int inY = y + 1;
        int inW = w - 2;
        int inH = h - 2;
        gui.fillGradient(inX, inY, inX + inW, inY + inH, 0xFF0C0E12, 0xFF1A1E26);
        gui.fill(inX, inY, inX + inW, inY + 1, 0x77000000);

        int filledW = Math.round(inW * ratio);
        if (filledW > 0) {
            drawSegmentedFillOnly(gui, inX, inY, inW, inH, 0, filledW, segments, darkEdge, mainCol, softCenter, subtleTop);
        }

        if (segments > 1) {
            for (int i = 1; i < segments; i++) {
                int sepX = inX + (inW * i) / segments;
                gui.fill(sepX, inY, sepX + 1, inY + inH, 0xD00B0D11);
            }
        }

        gui.fill(inX, inY, inX + 1, inY + inH, 0x55000000);
        gui.fill(inX + inW - 1, inY, inX + inW, inY + inH, 0x55000000);
    }

    private static void drawSegmentedFillOnly(GuiGraphics gui, int inX, int inY, int inW, int inH,
                                              int startPx, int endPx, int segments,
                                              int darkEdge, int mainCol, int softCenter, int subtleTop) {
        int clampStart = Mth.clamp(startPx, 0, inW);
        int clampEnd = Mth.clamp(endPx, 0, inW);
        if (clampEnd <= clampStart) return;

        for (int s = 0; s < segments; s++) {
            int segLeft = (inW * s) / segments;
            int segRight = (inW * (s + 1)) / segments - (s < segments - 1 ? 1 : 0);

            int drawL = Math.max(segLeft, clampStart);
            int drawR = Math.min(segRight, clampEnd);
            if (drawR <= drawL) continue;

            int absL = inX + drawL;
            int absR = inX + drawR;
            int segW = absR - absL;

            gui.fill(absL, inY, absR, inY + inH, darkEdge);

            if (inH >= 4) {
                gui.fillGradient(absL, inY + 1, absR, inY + inH - 1, mainCol, darkEdge);
            } else if (inH >= 2) {
                gui.fill(absL, inY, absR, inY + inH - 1, mainCol);
            }

            int coreL = absL + (drawL == segLeft && segW > 2 ? 1 : 0);
            int coreR = absR - (drawR == segRight && segW > 2 ? 1 : 0);

            if (coreR > coreL) {
                if (inH >= 5) {
                    gui.fillGradient(coreL, inY + 1, coreR, inY + inH - 1, subtleTop, mainCol);
                    gui.fill(coreL, inY + 1, coreR, inY + inH - 2, softCenter);
                } else if (inH >= 3) {
                    gui.fillGradient(coreL, inY + 1, coreR, inY + inH - 1, softCenter, mainCol);
                } else {
                    gui.fill(coreL, inY, coreR, inY + inH - 1, softCenter);
                }
            }
        }
    }

    public static void renderContainerWindow(GuiGraphics gui, AbstractContainerScreen<?> screen,
                                             int left, int top, int width, int height, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        boolean isInv = (screen instanceof InventoryScreen);
        boolean isCorpse = (screen instanceof CorpseLootScreen);
        boolean isCreative = (screen instanceof CreativeModeInventoryScreen);
        boolean isCreativeSurvTab = isCreative && isSurvivalInventoryTabSelected();
        boolean isDwarf = mc.player != null && mc.player.getData(ModAttachments.APPEARANCE).race() == RaceType.DWARF;

        if (isInv) {
            screen.renderables.forEach(r -> {
                if (r instanceof ImageButton btn) {
                    btn.visible = false;
                    btn.active = false;
                }
            });
        }

        int topExtend = (isInv || isCorpse) ? 34 : 0;
        int bottomExtend = (isCreativeSurvTab && isDwarf) ? 18 : 0;

        if (isCreative && screen instanceof CreativeModeInventoryScreen creativeScreen) {
            renderCreativeTabsAndScrollbar(gui, creativeScreen, left, top, height, mouseX, mouseY, bottomExtend);
        }

        int boxLeft = left - 4;
        int boxTop = top - 4 - topExtend;
        int boxWidth = width + 8;
        int boxHeight = height + 8 + topExtend + bottomExtend;

        gui.fill(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight, 0xD80A0B0F);
        gui.renderOutline(boxLeft, boxTop, boxWidth, boxHeight, 0xFF45484F);
        gui.renderOutline(boxLeft + 1, boxTop + 1, boxWidth - 2, boxHeight - 2, 0x448C7032);

        if (mc.player != null && (isInv || isCreativeSurvTab)) {
            int bagSlots = mc.player.getData(ModAttachments.EXTRA_EQUIPMENT).getActiveBackpackSlotCount();
            if (bagSlots > 0) {
                int btnX = left - 16;
                int btnY = top + 45;
                boolean btnHov = (mouseX >= btnX && mouseX < btnX + 12 && mouseY >= btnY && mouseY < btnY + 22);

                gui.fill(btnX, btnY, btnX + 12, btnY + 22, btnHov ? 0xEE222630 : 0xDD101218);
                gui.renderOutline(btnX, btnY, 12, 22, btnHov ? 0xFFD4AF37 : 0xFF45484F);
                String arrow = ExtraEquipmentData.clientBackpackExpanded ? "▶" : "◀";
                gui.drawString(mc.font, arrow, btnX + 3, btnY + 7, 0xFFD4AF37, false);

                if (ExtraEquipmentData.clientBackpackExpanded) {
                    int rows = (bagSlots + 2) / 3;
                    int bpLeft = left + (isInv ? -69 : -67);
                    int bpTop = top + (isInv ? -33 : 1);
                    int bpW = 62;
                    int bpH = rows * 18 + 8;

                    gui.fill(bpLeft, bpTop, bpLeft + bpW, bpTop + bpH, 0xD80A0B0F);
                    gui.renderOutline(bpLeft, bpTop, bpW, bpH, 0xFF45484F);
                }
            }
        }

        if (mc.player != null) {
            boolean isOrc = (mc.player.getData(ModAttachments.APPEARANCE).race() == RaceType.ORC);
            float dollYOffset = isOrc ? -0.42F : 0.0625F;

            ClientEvents.renderingInventoryDoll = true;
            try {
                if (isInv) {
                    int dollX1 = left + 45;
                    int dollY1 = top - 28;
                    int dollX2 = left + 75;
                    int dollY2 = top + 44;
                    gui.fill(dollX1, dollY1, dollX2, dollY2, 0x88050508);
                    gui.renderOutline(dollX1, dollY1, dollX2 - dollX1, dollY2 - dollY1, 0xFF3A3C42);
                    renderStandingDoll(gui, dollX1, dollY1, dollX2, dollY2, 26, dollYOffset, mouseX, mouseY, mc.player);
                } else if (isCorpse) {
                    int dollX1 = left + 63;
                    int dollY1 = top - 27;
                    int dollX2 = left + 78;
                    int dollY2 = top + 11;
                    gui.fill(dollX1, dollY1, dollX2, dollY2, 0x88050508);
                    gui.renderOutline(dollX1, dollY1, dollX2 - dollX1, dollY2 - dollY1, 0xFF3A3C42);
                    renderStandingDoll(gui, dollX1, dollY1, dollX2, dollY2, 15, dollYOffset, mouseX, mouseY, mc.player);

                    gui.fill(left + 6, top + 96, left + width - 6, top + 98, 0xFF6E5B2C);
                } else if (isCreativeSurvTab) {
                    int dollX1 = left + 65;
                    int dollY1 = top + 5;
                    int dollX2 = left + 111;
                    int dollY2 = top + 43;
                    gui.fill(dollX1, dollY1, dollX2, dollY2, 0x88050508);
                    gui.renderOutline(dollX1, dollY1, dollX2 - dollX1, dollY2 - dollY1, 0xFF3A3C42);
                    renderStandingDoll(gui, dollX1, dollY1, dollX2, dollY2, 16, dollYOffset, mouseX, mouseY, mc.player);
                }
            } finally {
                ClientEvents.renderingInventoryDoll = false;
            }
        }

        for (Slot slot : screen.getMenu().slots) {
            if (!slot.isActive()) continue;
            int sx = left + slot.x - 1;
            int sy = top + slot.y - 1;
            boolean hovered = (mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18);

            Slot unwrapped = unwrapSlot(slot);
            boolean isDwarfSlot = (unwrapped instanceof ExtraInventorySlot ex && ex.isDwarfStorageSlot());
            boolean isBagSlot = (unwrapped instanceof ExtraInventorySlot ex && ex.isBackpackStorageSlot());

            int bg = hovered ? 0xAA242730 : (isDwarfSlot ? 0xAA16130E : (isBagSlot ? 0xAA12151C : 0x99101116));
            int border = hovered ? 0xFFD4AF37 : (isDwarfSlot ? 0xFF7A633A : (isBagSlot ? 0xFF4A5262 : 0xFF383A40));

            gui.fill(sx, sy, sx + 18, sy + 18, bg);
            gui.renderOutline(sx, sy, 18, 18, border);

            if (unwrapped instanceof ExtraInventorySlot extraSlot
                    && !extraSlot.isDwarfStorageSlot()
                    && !extraSlot.isBackpackStorageSlot()
                    && !slot.hasItem()) {
                drawSlotSilhouetteIcon(gui, sx + 1, sy + 1, extraSlot.getExtraSlotIndex());
            }

            if (unwrapped instanceof CorpseMenu.CorpseTakeOnlySlot corpseSlot
                    && corpseSlot.getIconExtraIndex() >= 0
                    && !slot.hasItem()) {
                drawSlotSilhouetteIcon(gui, sx + 1, sy + 1, corpseSlot.getIconExtraIndex());
            }
        }

        if (isCreative && screen instanceof CreativeModeInventoryScreen creativeScreen) {
            renderCreativeSearchBoxAndScrollbar(gui, creativeScreen, left, top);
        }
    }

    private static Slot unwrapSlot(Slot slot) {
        if (slot instanceof ExtraInventorySlot) return slot;
        try {
            for (Field f : slot.getClass().getDeclaredFields()) {
                if (Slot.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Object inner = f.get(slot);
                    if (inner instanceof Slot target) return target;
                }
            }
        } catch (Exception ignored) {
        }
        return slot;
    }

    private static void drawSlotSilhouetteIcon(GuiGraphics gui, int x, int y, int extraSlotIndex) {
        String[] pattern = switch (extraSlotIndex) {
            case ExtraEquipmentData.SLOT_HAT_UNDER -> ICON_HAT;
            case ExtraEquipmentData.SLOT_SHIRT_UNDER -> ICON_SHIRT;
            case ExtraEquipmentData.SLOT_PANTS_UNDER -> ICON_PANTS;
            case ExtraEquipmentData.SLOT_SHOES_UNDER -> ICON_SHOES;
            case ExtraEquipmentData.SLOT_CAPE -> ICON_CAPE;
            case ExtraEquipmentData.SLOT_BACKPACK -> ICON_BACKPACK;
            default -> null;
        };
        if (pattern == null) return;

        int iconCol = 0xFF4B4E56;
        int startY = y + 3;
        for (int row = 0; row < pattern.length; row++) {
            String line = pattern[row];
            for (int col = 0; col < line.length(); col++) {
                if (line.charAt(col) == '#') {
                    gui.fill(x + col, startY + row, x + col + 1, startY + row + 1, iconCol);
                }
            }
        }
    }

    public static boolean isSurvivalInventoryTabSelected() {
        try {
            if (selectedTabField != null) {
                CreativeModeTab tab = (CreativeModeTab) selectedTabField.get(null);
                return tab != null && tab.getType() == CreativeModeTab.Type.INVENTORY;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static void renderCreativeTabsAndScrollbar(GuiGraphics gui, CreativeModeInventoryScreen screen,
                                                       int left, int top, int height, int mouseX, int mouseY, int bottomExtend) {
        var page = screen.getCurrentPage();
        CreativeModeTab selected = null;
        try {
            if (selectedTabField != null) selected = (CreativeModeTab) selectedTabField.get(null);
        } catch (Exception ignored) {
        }

        for (CreativeModeTab tab : page.getVisibleTabs()) {
            boolean isTop = page.isTop(tab);
            int col = page.getColumn(tab);
            int tx = left + getTabX(tab, col);
            int ty = isTop ? (top - 28) : (top + height + bottomExtend);
            int tw = 26;
            int th = 28;

            boolean isSel = (tab == selected);
            boolean hov = (mouseX >= tx && mouseX < tx + tw && mouseY >= ty && mouseY < ty + th);

            int bg = isSel ? 0xEE181A22 : (hov ? 0xCC14161D : 0x990A0B0E);
            int border = isSel ? 0xFFD4AF37 : (hov ? 0xFF7A7E85 : 0xFF3A3C42);

            gui.fill(tx, ty, tx + tw, ty + th, bg);
            gui.renderOutline(tx, ty, tw, th, border);

            int iconX = tx + 5;
            int iconY = ty + 6;
            gui.renderItem(tab.getIconItem(), iconX, iconY);
        }
    }

    private static int getTabX(CreativeModeTab tab, int col) {
        if (tab.isAlignedRight()) {
            return 195 - 27 * (7 - col);
        }
        return col > 0 ? col * 26 + col : 0;
    }

    private static void renderCreativeSearchBoxAndScrollbar(GuiGraphics gui, CreativeModeInventoryScreen screen, int left, int top) {
        try {
            if (searchBoxField != null) {
                EditBox search = (EditBox) searchBoxField.get(screen);
                if (search != null && search.isVisible()) {
                    Font font = Minecraft.getInstance().font;
                    int sx = search.getX() - 3;
                    int sy = search.getY() - 2;
                    int sw = search.getWidth() + 6;
                    int sh = search.getHeight() + 4;

                    gui.fill(sx, sy, sx + sw, sy + sh, 0xEE12141B);
                    gui.renderOutline(sx, sy, sw, sh, search.isFocused() ? 0xFFD4AF37 : 0xFF5A5E68);

                    String val = search.getValue();
                    if (val.isEmpty() && !search.isFocused()) {
                        gui.drawString(font, "§7Поиск...", search.getX(), search.getY(), 0xFF777777, false);
                    } else {
                        String cursor = (search.isFocused() && (System.currentTimeMillis() / 500) % 2 == 0) ? "_" : "";
                        String display = font.plainSubstrByWidth(val, search.getWidth() - 8) + cursor;
                        gui.drawString(font, display, search.getX(), search.getY(), 0xFFFFFFFF, true);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        if (!isSurvivalInventoryTabSelected()) {
            int sbX = left + 174;
            int sbY = top + 17;
            int sbW = 14;
            int sbH = 112;

            gui.fill(sbX, sbY, sbX + sbW, sbY + sbH, 0xAA08090C);
            gui.renderOutline(sbX, sbY, sbW, sbH, 0xFF3A3C42);

            float scroll = 0.0F;
            try {
                if (scrollOffsField != null) scroll = scrollOffsField.getFloat(screen);
            } catch (Exception ignored) {
            }

            int knobH = 15;
            int knobY = sbY + 1 + (int) ((float) (sbH - knobH - 2) * Mth.clamp(scroll, 0.0F, 1.0F));
            gui.fill(sbX + 1, knobY, sbX + sbW - 1, knobY + knobH, 0xEE22252E);
            gui.renderOutline(sbX + 1, knobY, sbW - 2, knobH, 0xFFC5A03F);
        }
    }

    public static void renderSkyrimHud(GuiGraphics gui, Player player) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        boolean poisoned = player.hasEffect(MobEffects.POISON);
        boolean withered = player.hasEffect(MobEffects.WITHER);
        boolean toxicHp = poisoned || withered;

        int hudX = 8;
        int hudY = 8;

        int frameSize = 32;
        gui.fill(hudX - 1, hudY - 1, hudX + frameSize + 1, hudY + frameSize + 1, 0xAA000000);
        gui.fillGradient(hudX, hudY, hudX + frameSize, hudY + frameSize, 0xFF1A1D26, 0xFF0C0E13);
        gui.renderOutline(hudX, hudY, frameSize, frameSize, HUD_BORDER_STEEL);
        gui.renderOutline(hudX + 1, hudY + 1, frameSize - 2, frameSize - 2, HUD_BORDER_GOLD);

        drawPortraitCornerOrnament(gui, hudX - 1, hudY - 1, true, true);
        drawPortraitCornerOrnament(gui, hudX + frameSize - 3, hudY - 1, false, true);
        drawPortraitCornerOrnament(gui, hudX - 1, hudY + frameSize - 3, true, false);
        drawPortraitCornerOrnament(gui, hudX + frameSize - 3, hudY + frameSize - 3, false, false);

        renderPlayerHeadPortrait(gui, player, hudX + 3, hudY + 3);

        String lvlStr = String.valueOf(player.experienceLevel);
        float lvlScale = 0.78F;
        int lvlTextW = Math.round(font.width(lvlStr) * lvlScale);
        int badgeW = Math.max(14, lvlTextW + 7);
        int badgeH = 10;
        int badgeX = hudX + (frameSize - badgeW) / 2;
        int badgeY = hudY + frameSize - 5;

        gui.fill(badgeX - 1, badgeY - 1, badgeX + badgeW + 1, badgeY + badgeH + 1, 0xFF07080B);
        gui.fillGradient(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH, 0xFF181324, 0xFF0E0B16);
        gui.renderOutline(badgeX, badgeY, badgeW, badgeH, 0xFF64229E);
        gui.fill(badgeX, badgeY, badgeX + 1, badgeY + 1, HUD_CORNER_RIVET);
        gui.fill(badgeX + badgeW - 1, badgeY, badgeX + badgeW, badgeY + 1, HUD_CORNER_RIVET);

        gui.pose().pushPose();
        gui.pose().translate(hudX + frameSize / 2.0F - (font.width(lvlStr) * lvlScale) / 2.0F, badgeY + 2.0F, 0.0F);
        gui.pose().scale(lvlScale, lvlScale, 1.0F);
        gui.drawString(font, lvlStr, 0, 0, 0xFFD48CFF, true);
        gui.pose().popPose();

        int rightStartX = hudX + frameSize + 6;
        int nameY = hudY + 1;

        PlayerAppearanceData appData = player.getData(ModAttachments.APPEARANCE);
        String rpName = appData.getFullRpName().trim();
        if (rpName.isEmpty()) {
            rpName = player.getGameProfile().getName();
        }

        gui.drawString(font, "§f" + rpName, rightStartX, nameY, 0xFFFFFFFF, true);

        ClanData myClan = getActiveClientClan();
        if (myClan != null && myClan.getClanTag() != null && !myClan.getClanTag().isEmpty()) {
            int tagX = rightStartX + font.width(rpName) + 5;
            String tagText = "[" + myClan.getClanTag() + "]";
            gui.drawString(font, tagText, tagX, nameY, myClan.getColorHex() | 0xFF000000, true);
        } else {
            String fullDisplay = player.getDisplayName().getString();
            if (fullDisplay.startsWith("[") && fullDisplay.contains("]")) {
                String tagPart = fullDisplay.substring(0, fullDisplay.indexOf(']') + 1);
                int tagX = rightStartX + font.width(rpName) + 5;
                gui.drawString(font, tagPart, tagX, nameY, 0xFFD4AF37, true);
            }
        }

        int hpBarY = hudY + 11;
        int barW = 148;
        int hpBarH = 7;

        float hp = player.getHealth();
        float maxHp = Math.max(1.0F, player.getMaxHealth());
        float absorption = player.getAbsorptionAmount();
        float hpRatio = Mth.clamp(hp / maxHp, 0.0F, 1.0F);
        float absRatio = Mth.clamp(absorption / maxHp, 0.0F, 1.0F);

        int innerW = barW - 2;
        int filledHpW = Math.round(innerW * hpRatio);
        int filledAbsW = Math.round(innerW * absRatio);

        float pulse = 0.5F + 0.5F * Mth.sin(player.tickCount * 0.22F);
        int hpBorderCol = 0xFF1F1013;
        if (poisoned) {
            hpBorderCol = FastColor.ARGB32.lerp(pulse, 0xFF143318, 0xFF287A2C);
        } else if (withered) {
            hpBorderCol = FastColor.ARGB32.lerp(pulse, 0xFF221429, 0xFF522D61);
        }

        if (poisoned) {
            drawSegmentedCapsuleBar(gui, rightStartX, hpBarY, barW, hpBarH, hpRatio, 8,
                    hpBorderCol, 0xFF144218, 0xFF237D28, 0xFF349E3A, 0xFF42B348);
        } else if (withered) {
            drawSegmentedCapsuleBar(gui, rightStartX, hpBarY, barW, hpBarH, hpRatio, 8,
                    hpBorderCol, 0xFF1F1226, 0xFF3E264A, 0xFF563866, 0xFF68467A);
        } else {
            drawSegmentedCapsuleBar(gui, rightStartX, hpBarY, barW, hpBarH, hpRatio, 8,
                    hpBorderCol, 0xFF4D0D12, 0xFF991B21, 0xFFBD282F, 0xFFCC373E);
        }

        if (filledAbsW > 0) {
            int absStart = Math.max(0, Math.min(innerW - filledAbsW, filledHpW));
            drawSegmentedFillOnly(gui, rightStartX + 1, hpBarY + 1, innerW, hpBarH - 2,
                    absStart, absStart + filledAbsW, 8,
                    0xFF593D08, 0xFFB37E19, 0xFFD49B2A, 0xFFE0AC3E);
        }

        if (toxicHp) {
            drawDetailedToxicOverlay(gui, rightStartX + 1, hpBarY + 1, innerW, hpBarH - 2, filledHpW, player.tickCount, withered);
        }

        int dispHp = (hp > 0.0F) ? Math.max(1, Mth.ceil(hp + absorption)) : 0;
        String hpText = dispHp + " / " + Math.round(maxHp);
        gui.pose().pushPose();
        float textScale = 0.68F;
        float textX = rightStartX + (barW - font.width(hpText) * textScale) / 2.0F;
        float textY = hpBarY + 1.1F;
        gui.pose().translate(textX, textY, 0);
        gui.pose().scale(textScale, textScale, 1.0F);
        gui.drawString(font, hpText, 0, 0, toxicHp ? 0xFFD8FFD4 : 0xFFEAEAEA, true);
        gui.pose().popPose();

        int statsRowY = hpBarY + hpBarH + 3;
        int curStatX = rightStartX;

        int foodVal = player.getFoodData().getFoodLevel();
        boolean hungerToxic = player.hasEffect(MobEffects.HUNGER);
        int foodColor = hungerToxic ? 0xFF8CD147 : 0xFFE6C89C;
        curStatX = drawItemStatBadge(gui, font, ICON_FOOD_STACK, String.valueOf(foodVal), curStatX, statsRowY, foodColor);

        if (showManaIndicator) {
            curStatX = drawManaStatBadge(gui, font, String.valueOf(clientManaValue), curStatX, statsRowY);
        }

        int armorVal = player.getArmorValue();
        curStatX = drawItemStatBadge(gui, font, ICON_ARMOR_STACK, String.valueOf(armorVal), curStatX, statsRowY, 0xFFD8DEE9);

        if (player.getVehicle() instanceof LivingEntity mount) {
            int mountHp = Math.max(1, Mth.ceil(mount.getHealth()));
            int mountMax = Math.round(mount.getMaxHealth());
            String mountStr = mountHp + "/" + mountMax;
            drawItemStatBadge(gui, font, ICON_MOUNT_STACK, mountStr, curStatX, statsRowY, 0xFFF59E56);
        }

        // Отрисовка наложенных на игрока эффектов слева вверху чуть ниже HUD с HP
        renderStatusEffectsUnderHud(gui, font, player, hudX, hudY + frameSize + 8);

        int downedTicks = player.getData(ModAttachments.DOWNED_TICKS);
        if (downedTicks > 0) {
            renderDownedBleedoutOverlay(gui, font, sw, sh, downedTicks, pulse);
        }

        if (clientReviveProgressTicks > 0) {
            renderReviveHoldProgress(gui, font, sw, sh, clientReviveProgressTicks);
        }

        renderPickupNotifications(gui, font, sw, sh);

        if (dayBannerTicks > 0) {
            renderNewDayBanner(gui, font, sw);
        }

        renderCompass(gui, font, player, sw);
        WorldMapManager.renderCircularMinimap(gui, player, sw);
        renderHotbarAndBarBottom(gui, font, player, sw, sh);
    }

    /**
     * Все накладываемые эффекты на игрока отображаются слева вверху чуть ниже HUD с HP:
     * уменьшенные аккуратные иконки (12x12 внутри 18x18 рамки) с таймером и римской цифрой/уровнем!
     */
    private static void renderStatusEffectsUnderHud(GuiGraphics gui, Font font, Player player, int startX, int startY) {
        var effects = player.getActiveEffects();
        if (effects.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        var mobEffectTextures = mc.getMobEffectTextures();

        int index = 0;
        int boxSize = 18;
        int gap = 4;

        for (MobEffectInstance inst : effects) {
            if (!inst.showIcon()) continue;

            int col = index % 6;
            int row = index / 6;
            int x = startX + col * (boxSize + gap);
            int y = startY + row * (boxSize + 10);

            boolean beneficial = inst.getEffect().value().isBeneficial();
            int borderCol = beneficial ? 0xAA4E6B3A : 0xAA8A2E2E;

            gui.fill(x, y, x + boxSize, y + boxSize, 0xCC0D0F14);
            gui.renderOutline(x, y, boxSize, boxSize, borderCol);

            float alpha = 1.0F;
            if (!inst.isInfiniteDuration() && inst.getDuration() <= 100) {
                alpha = 0.55F + 0.45F * Mth.sin(player.tickCount * 0.4F);
            }

            var sprite = mobEffectTextures.get(inst.getEffect());
            RenderSystem.enableBlend();
            gui.setColor(1.0F, 1.0F, 1.0F, alpha);
            gui.blit(x + 3, y + 3, 0, 12, 12, sprite);
            gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);

            if (inst.getAmplifier() > 0) {
                String ampStr = String.valueOf(inst.getAmplifier() + 1);
                gui.pose().pushPose();
                float sc = 0.62F;
                gui.pose().translate(x + boxSize - font.width(ampStr) * sc - 1.5F, y + 1.5F, 10.0F);
                gui.pose().scale(sc, sc, 1.0F);
                gui.drawString(font, ampStr, 0, 0, 0xFFFFD760, true);
                gui.pose().popPose();
            }

            String durStr;
            if (inst.isInfiniteDuration()) {
                durStr = "∞";
            } else {
                int totalSec = Math.max(0, inst.getDuration() / 20);
                int mins = totalSec / 60;
                int secs = totalSec % 60;
                durStr = String.format("%d:%02d", mins, secs);
            }

            gui.pose().pushPose();
            float durScale = 0.60F;
            float tx = x + (boxSize - font.width(durStr) * durScale) / 2.0F;
            gui.pose().translate(tx, y + boxSize + 1.5F, 0.0F);
            gui.pose().scale(durScale, durScale, 1.0F);
            gui.drawString(font, durStr, 0, 0, 0xFFD0D4DC, true);
            gui.pose().popPose();

            index++;
        }
    }

    private static void renderReviveHoldProgress(GuiGraphics gui, Font font, int sw, int sh, int progressTicks) {
        float ratio = Mth.clamp(progressTicks / 100.0F, 0.0F, 1.0F);
        float secsLeft = Math.max(0.0F, (100 - progressTicks) / 20.0F);
        String label = String.format("§aПоднятие раненого... §e(%.1f сек)", secsLeft);

        int barW = 140;
        int barX = sw / 2 - barW / 2;
        int barY = sh / 2 + 18;

        gui.drawCenteredString(font, label, sw / 2, barY - 11, 0xFFFFFFFF);
        drawSegmentedCapsuleBar(gui, barX, barY, barW, 6, ratio, 5,
                0xFF0E2414, 0xFF0B421A, 0xFF19A83E, 0xFF42F06E, 0xFF85FF9E);
    }

    private static void renderPickupNotifications(GuiGraphics gui, Font font, int sw, int sh) {
        if (PICKUP_LOG.isEmpty()) return;

        int baseX = sw - 12;
        int baseY = sh - 46;

        for (int i = 0; i < PICKUP_LOG.size(); i++) {
            PickupNotification n = PICKUP_LOG.get(PICKUP_LOG.size() - 1 - i);
            int rowY = baseY - i * 16;

            float fade = (n.ticksLeft < 20) ? (n.ticksLeft / 20.0F) : 1.0F;
            int alpha = Mth.clamp((int) (235 * fade), 10, 235) << 24;

            String itemName = n.iconStack.getHoverName().getString();
            String text = "+" + n.addedCount + " " + itemName + " §7(Всего: " + n.totalCount + ")";

            float textScale = 0.82F;
            int textW = Math.round(font.width(text) * textScale);
            int iconX = baseX - textW - 15;

            gui.pose().pushPose();
            gui.pose().translate(iconX, rowY - 2.0F, 0.0F);
            gui.pose().scale(0.75F, 0.75F, 1.0F);
            gui.renderItem(n.iconStack, 0, 0);
            gui.pose().popPose();

            gui.pose().pushPose();
            gui.pose().translate(baseX - textW, rowY + 1.0F, 0.0F);
            gui.pose().scale(textScale, textScale, 1.0F);
            gui.drawString(font, text, 0, 0, alpha | 0xF2E6C8, true);
            gui.pose().popPose();
        }
    }

    private static void renderNewDayBanner(GuiGraphics gui, Font font, int sw) {
        float progress = 1.0F - (dayBannerTicks / (float) DAY_BANNER_MAX_TICKS);
        float alphaFactor;
        if (progress < 0.18F) {
            alphaFactor = progress / 0.18F;
        } else if (progress > 0.78F) {
            alphaFactor = (1.0F - progress) / 0.22F;
        } else {
            alphaFactor = 1.0F;
        }
        alphaFactor = Mth.clamp(alphaFactor, 0.0F, 1.0F);
        if (alphaFactor <= 0.02F) return;

        int textAlpha = (int) (250 * alphaFactor) << 24;
        int subAlpha = (int) (200 * alphaFactor) << 24;
        int bgAlpha = (int) (185 * alphaFactor) << 24;
        int goldLineAlpha = (int) (215 * alphaFactor) << 24;

        int bannerW = 240;
        int bannerH = 34;
        int bannerY = 30;
        int cx = sw / 2;
        int left = cx - bannerW / 2;
        int right = cx + bannerW / 2;

        gui.fillGradient(left, bannerY, right, bannerY + bannerH, bgAlpha | 0x0A0B0F, (int) (75 * alphaFactor) << 24 | 0x0A0B0F);

        gui.fill(left + 10, bannerY, right - 10, bannerY + 1, goldLineAlpha | 0xC5A03F);
        gui.fill(left + 26, bannerY + 2, right - 26, bannerY + 3, (int) (110 * alphaFactor) << 24 | 0x8C7032);

        gui.fill(left + 26, bannerY + bannerH - 3, right - 26, bannerY + bannerH - 2, (int) (110 * alphaFactor) << 24 | 0x8C7032);
        gui.fill(left + 10, bannerY + bannerH - 1, right - 10, bannerY + bannerH, goldLineAlpha | 0xC5A03F);

        gui.fill(left + 14, bannerY + bannerH / 2 - 2, left + 18, bannerY + bannerH / 2 + 2, goldLineAlpha | 0xFFD4AF37);
        gui.fill(right - 18, bannerY + bannerH / 2 - 2, right - 14, bannerY + bannerH / 2 + 2, goldLineAlpha | 0xFFD4AF37);

        String dayTitle = "ДЕНЬ " + dayBannerNumber;
        gui.pose().pushPose();
        float titleScale = 1.85F;
        gui.pose().translate(cx - (font.width(dayTitle) * titleScale) / 2.0F, bannerY + 5.5F, 0.0F);
        gui.pose().scale(titleScale, titleScale, 1.0F);
        gui.drawString(font, dayTitle, 0, 0, textAlpha | 0xFFE082, true);
        gui.pose().popPose();

        String subTitle = "Наступил новый рассвет";
        gui.pose().pushPose();
        float subScale = 0.78F;
        gui.pose().translate(cx - (font.width(subTitle) * subScale) / 2.0F, bannerY + 23.0F, 0.0F);
        gui.pose().scale(subScale, subScale, 1.0F);
        gui.drawString(font, subTitle, 0, 0, subAlpha | 0xD0D4DC, true);
        gui.pose().popPose();
    }

    private static void drawPortraitCornerOrnament(GuiGraphics gui, int x, int y, boolean left, boolean top) {
        gui.fill(x, y, x + 4, y + 4, 0xFF0A0B0F);
        gui.renderOutline(x, y, 4, 4, HUD_BORDER_GOLD);
        int px = left ? x + 1 : x + 2;
        int py = top ? y + 1 : y + 2;
        gui.fill(px, py, px + 1, py + 1, HUD_CORNER_RIVET);
    }

    private static void renderPlayerHeadPortrait(GuiGraphics gui, Player player, int x, int y) {
        int size = 26;
        PlayerAppearanceData data = player.getData(ModAttachments.APPEARANCE);
        ResourceLocation skinLoc = CustomSkinManager.getOrCreateSkin(data);
        gui.blit(skinLoc, x, y, size, size, 8.0F, 8.0F, 8, 8, 64, 64);

        gui.fillGradient(x, y, x + size, y + 4, 0x55000000, 0x00000000);
        gui.fillGradient(x, y + size - 4, x + size, y + size, 0x00000000, 0x66000000);
    }

    private static int drawItemStatBadge(GuiGraphics gui, Font font, ItemStack iconStack, String valueText,
                                         int x, int y, int textColor) {
        gui.pose().pushPose();
        gui.pose().translate(x, y - 0.5F, 0.0F);
        float iconScale = 0.68F;
        gui.pose().scale(iconScale, iconScale, 1.0F);
        gui.renderItem(iconStack, 0, 0);
        gui.pose().popPose();

        int textX = x + 12;
        gui.drawString(font, valueText, textX, y + 2, textColor, true);
        return textX + font.width(valueText) + 10;
    }

    private static int drawManaStatBadge(GuiGraphics gui, Font font, String manaText, int x, int y) {
        int ix = x + 1;
        int iy = y + 1;
        gui.fill(ix + 3, iy, ix + 5, iy + 9, 0xFF1A4B8C);
        gui.fill(ix + 1, iy + 2, ix + 7, iy + 7, 0xFF2874D4);
        gui.fill(ix + 2, iy + 1, ix + 6, iy + 8, 0xFF429EFA);
        gui.fill(ix + 3, iy + 2, ix + 5, iy + 6, 0xFF99D6FF);

        int textX = x + 10;
        gui.drawString(font, manaText, textX, y + 2, 0xFF66B8FF, true);
        return textX + font.width(manaText) + 10;
    }

    private static void renderDownedBleedoutOverlay(GuiGraphics gui, Font font, int sw, int sh, int downedTicks, float pulse) {
        int vignetteAlpha = (int) (35 + 45 * pulse) << 24;
        gui.fillGradient(0, 0, sw, 36, vignetteAlpha | 0x660000, 0x00660000);
        gui.fillGradient(0, sh - 36, sw, sh, 0x00660000, vignetteAlpha | 0x660000);

        int boxW = 210;
        int boxX = sw / 2 - boxW / 2;
        int boxY = sh - 68;

        int secsLeft = (downedTicks + 19) / 20;
        float timeRatio = Mth.clamp(downedTicks / 1200.0F, 0.0F, 1.0F);

        String title = "§c§lВЫ ТЯЖЕЛО РАНЕНЫ! §7(Осталось: §e" + secsLeft + " сек§7)";
        String sub = "§fДождитесь помощи или нажмите §c§l[T]§f, чтобы сдаться";

        gui.drawCenteredString(font, title, sw / 2, boxY - 12, 0xFFFFFFFF);
        drawSegmentedCapsuleBar(gui, boxX, boxY, boxW, 6, timeRatio, 10,
                0xFF2B090C, 0xFF590A10, 0xFFB31924, 0xFFE0323E, 0xFFFF5964);

        gui.pose().pushPose();
        float sc = 0.80F;
        gui.pose().translate(sw / 2.0F - (font.width(sub) * sc) / 2.0F, boxY + 9.0F, 0.0F);
        gui.pose().scale(sc, sc, 1.0F);
        gui.drawString(font, sub, 0, 0, 0xFFFFFFFF, true);
        gui.pose().popPose();
    }

    private static void drawDetailedToxicOverlay(GuiGraphics gui, int x, int y, int w, int h, int filledW, int ticks, boolean wither) {
        int veinMain = wither ? 0xCC9A4CE8 : 0xCC62FF3B;
        int veinDark = wither ? 0xAA4A1578 : 0xAA1F7A14;
        int bubbleCol = wither ? 0xEEB87CFF : 0xEEB6FF82;

        gui.fill(x, y, x + w, y + 1, veinDark);
        gui.fill(x, y + h - 1, x + w, y + h, veinDark);

        for (int vx = 3; vx < w - 4; vx += 7) {
            int wave = ((vx * 13 + ticks / 3) & 3);
            boolean fromTop = ((vx / 7) % 2 == 0);
            int rootY = fromTop ? y : (y + h - 1);
            int dir = fromTop ? 1 : -1;

            gui.fill(x + vx, rootY, x + vx + 1, rootY + dir * Math.min(h, 2 + (wave & 1)), veinMain);
            if (h >= 4 && wave >= 1) {
                int branchX = x + vx + ((wave == 2) ? -1 : 1);
                int branchY = rootY + dir;
                gui.fill(branchX, branchY, branchX + 1, branchY + dir, veinDark);
            }
        }

        if (filledW > 6 && h >= 4) {
            for (int i = 0; i < 6; i++) {
                int bx = x + 2 + ((i * 19 + ticks / 2) % Math.max(1, filledW - 4));
                int by = y + h - 1 - ((ticks / 3 + i * 5) % Math.max(1, h - 1));
                gui.fill(bx, by, bx + 1, by + 1, bubbleCol);
            }
        }
    }

    private static void renderCompass(GuiGraphics gui, Font font, Player player, int sw) {
        int compW = 180;
        int compH = 13;
        int cx = sw / 2;
        int cy = 6;
        int left = cx - compW / 2;
        int right = cx + compW / 2;

        gui.fillGradient(left, cy, right, cy + compH, 0x990A0B0F, 0x080A0B0F);

        gui.fill(left + 12, cy, right - 12, cy + 1, 0x888C7032);
        gui.fill(left, cy, left + 12, cy + 1, 0x338C7032);
        gui.fill(right - 12, cy, right, cy + 1, 0x338C7032);

        gui.fillGradient(left, cy, left + 1, cy + compH - 2, 0x888C7032, 0x008C7032);
        gui.fillGradient(right - 1, cy, right, cy + compH - 2, 0x888C7032, 0x008C7032);

        gui.fill(cx - 1, cy - 1, cx + 2, cy + 1, 0xCCD4AF37);
        gui.fill(cx, cy + 1, cx + 1, cy + 2, 0xCCFFE580);

        float pt = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        float playerYaw = Mth.wrapDegrees(Mth.rotLerp(pt, player.yRotO, player.getYRot()));

        for (int deg = -180; deg < 180; deg += 15) {
            if (deg % 45 == 0) continue;
            float diff = Mth.wrapDegrees(deg - playerYaw);
            if (Math.abs(diff) <= 82.0F) {
                int tx = cx + Math.round((diff / 82.0F) * (compW / 2.0F - 10));
                int tickH = (deg % 30 == 0) ? 2 : 1;
                gui.fill(tx, cy + 1, tx + 1, cy + 1 + tickH, 0x557A7E88);
            }
        }

        String[] dirs = {"Ю", "ЮЗ", "З", "СЗ", "С", "СВ", "В", "ЮВ"};
        float[] angles = {0F, 45F, 90F, 135F, 180F, -135F, -90F, -45F};

        for (int i = 0; i < dirs.length; i++) {
            float diff = Mth.wrapDegrees(angles[i] - playerYaw);
            if (Math.abs(diff) <= 82.0F) {
                int px = cx + Math.round((diff / 82.0F) * (compW / 2.0F - 10));
                boolean isCardinal = (i % 2 == 0);
                float edgeFade = 1.0F - (Math.abs(diff) / 90.0F) * 0.45F;
                int alpha = Math.clamp((int) (220 * edgeFade), 60, 220) << 24;
                int baseRgb = (i == 4) ? 0xFF5E5E : (isCardinal ? 0xF2D47E : 0xB8BDC8);

                gui.fill(px, cy + 1, px + 1, cy + 2, isCardinal ? 0x99D4AF37 : 0x44585C66);
                gui.drawString(font, dirs[i], px - font.width(dirs[i]) / 2, cy + 3, alpha | baseRgb, true);
            }
        }

        for (ClanData.AllyCompassPos ally : ClanScreen.clientCompassAllies) {
            double dx = ally.x() - player.getX();
            double dz = ally.z() - player.getZ();
            float targetYaw = (float) (Mth.atan2(-dx, dz) * (180.0D / Math.PI));
            float diff = Mth.wrapDegrees(targetYaw - playerYaw);
            float clamped = Mth.clamp(diff, -84.0F, 84.0F);
            int mx = cx + Math.round((clamped / 84.0F) * (compW / 2.0F - 8));
            int col = ally.colorHex() | 0xCC000000;

            gui.fill(mx - 2, cy + 4, mx + 2, cy + 9, col);
            gui.renderOutline(mx - 3, cy + 3, 6, 7, 0x990A0B0F);
        }

        WorldMapManager.ensureLoaded();
        if (Minecraft.getInstance().level != null) {
            String dim = Minecraft.getInstance().level.dimension().location().toString();
            for (WorldMapManager.Waypoint wp : WorldMapManager.WAYPOINTS) {
                if (!wp.dimension.equals(dim)) continue;
                double dx = wp.x - player.getX();
                double dz = wp.z - player.getZ();
                float targetYaw = (float) (Mth.atan2(-dx, dz) * (180.0D / Math.PI));
                float diff = Mth.wrapDegrees(targetYaw - playerYaw);
                if (Math.abs(diff) <= 84.0F) {
                    int mx = cx + Math.round((diff / 84.0F) * (compW / 2.0F - 8));
                    int col = wp.colorHex | 0xDD000000;

                    gui.fill(mx, cy - 4, mx + 1, cy + 2, 0xCCFFFFFF);
                    gui.fill(mx + 1, cy - 4, mx + 5, cy - 1, col);
                }
            }
        }
    }

    private static void renderHotbarAndBarBottom(GuiGraphics gui, Font font, Player player, int sw, int sh) {
        int barW = 184;
        int barH = 22;
        int startX = sw / 2 - barW / 2;
        int startY = sh - barH - 2;

        if (player instanceof LocalPlayer lp && lp.jumpableVehicle() != null && lp.getJumpRidingScale() > 0.0F) {
            int expW = 184;
            int expX = sw / 2 - expW / 2;
            int expY = startY - 7;
            float jumpRatio = Mth.clamp(lp.getJumpRidingScale(), 0.0F, 1.0F);
            drawSegmentedCapsuleBar(gui, expX, expY, expW, 5, jumpRatio, 10,
                    0xFF21180A, 0xFF593D08, 0xFFB3861B, 0xFFD4A32F, 0xFFE0B442);
        }

        drawRpgFrame(gui, startX, startY, barW, barH, HUD_BORDER_GOLD);

        for (int i = 0; i < 9; i++) {
            int sx = startX + 2 + i * 20;
            int sy = startY + 1;

            gui.fill(sx + 1, sy + 1, sx + 19, sy + 19, 0x99101218);
            gui.renderOutline(sx + 1, sy + 1, 18, 18, 0xFF282B33);

            var stack = player.getInventory().items.get(i);
            if (!stack.isEmpty()) {
                // Если в слоте лежит Большая кровать Орка — рисуем ее иконку чуть больше обычной кровати (1.26x)!
                if (stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof OrcGiantBedBlock) {
                    gui.pose().pushPose();
                    gui.pose().translate(sx + 10.0F, sy + 10.0F, 0.0F);
                    gui.pose().scale(1.26F, 1.26F, 1.0F);
                    gui.pose().translate(-8.0F, -8.0F, 0.0F);
                    gui.renderItem(stack, 0, 0);
                    gui.pose().popPose();
                } else {
                    gui.renderItem(stack, sx + 2, sy + 2);
                }
                gui.renderItemDecorations(font, stack, sx + 2, sy + 2);
            }
            if (player.getInventory().selected == i) {
                gui.renderOutline(sx, sy, 20, 20, 0xFFE5C148);
                gui.renderOutline(sx + 1, sy + 1, 18, 18, 0xAA8C7032);
            }
        }
    }
}