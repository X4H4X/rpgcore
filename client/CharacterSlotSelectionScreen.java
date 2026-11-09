package com.rpgcore.client;

import com.rpgcore.data.CharacterSlotsData;
import com.rpgcore.data.ExtraEquipmentData;
import com.rpgcore.data.ModAttachments;
import com.rpgcore.data.PlayerAppearanceData;
import com.rpgcore.data.RaceType;
import com.rpgcore.network.SlotActionPayload;
import com.rpgcore.network.SyncSlotsMenuPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CharacterSlotSelectionScreen extends Screen {

    public static List<CharacterSlotsData.ClientSlotSummary> clientSlots = new ArrayList<>();
    public static int clientActiveSlot = -1;
    public static boolean pendingOpenOnLoad = false;

    private int winLeft, winTop, winWidth, winHeight;
    private int cardW, cardH, cardGap, cardsStartX, cardsY;

    // Индекс слота, для которого сейчас открыто модальное окно подтверждения удаления (-1 = окно закрыто)
    private int modalDeleteSlot = -1;

    private static final String[] SLOT_TITLES = {
            "Слот I",
            "Слот II",
            "Слот III"
    };

    public CharacterSlotSelectionScreen() {
        super(Component.literal("Выбор персонажа"));
        ensureDefaultClientSlots();
    }

    public static boolean isViewingEmptySlot() {
        return false;
    }

    private static void ensureDefaultClientSlots() {
        if (clientSlots.size() < CharacterSlotsData.MAX_SLOTS) {
            clientSlots.clear();
            for (int i = 0; i < CharacterSlotsData.MAX_SLOTS; i++) {
                clientSlots.add(new CharacterSlotsData.ClientSlotSummary(
                        i, false, PlayerAppearanceData.DEFAULT, new ExtraEquipmentData(),
                        Collections.emptyList(), 0L, "Нет изученных веток"
                ));
            }
        }
    }

    public static void handleSyncPacket(SyncSlotsMenuPayload payload) {
        clientActiveSlot = payload.activeSlot();
        clientSlots = new ArrayList<>(payload.slots());
        Minecraft mc = Minecraft.getInstance();
        if (payload.openMenuScreen()) {
            pendingOpenOnLoad = true;
            if (mc.level != null && !(mc.screen instanceof ReceivingLevelScreen)) {
                pendingOpenOnLoad = false;
                mc.setScreen(new CharacterSlotSelectionScreen());
            }
        } else {
            pendingOpenOnLoad = false;
            if (mc.screen instanceof CharacterSlotSelectionScreen screen) {
                screen.modalDeleteSlot = -1;
                screen.rebuildWidgets();
            }
        }
    }

    public static void checkPendingOpen(Minecraft mc) {
        if (pendingOpenOnLoad && mc.player != null && mc.level != null && mc.screen == null) {
            pendingOpenOnLoad = false;
            mc.setScreen(new CharacterSlotSelectionScreen());
        }
    }

    @Override
    public void renderBackground(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
    }

    private String clipText(String text, int maxW) {
        if (this.font.width(text) <= maxW) return text;
        return this.font.plainSubstrByWidth(text, Math.max(10, maxW - this.font.width("..."))) + "...";
    }

    @Override
    protected void init() {
        super.init();
        ensureDefaultClientSlots();

        this.winWidth = Math.min(440, this.width - 24);
        this.winHeight = Math.min(248, this.height - 20);
        this.winLeft = (this.width - this.winWidth) / 2;
        this.winTop = (this.height - this.winHeight) / 2;

        this.cardGap = 12;
        this.cardW = (this.winWidth - 32 - this.cardGap * 2) / 3;
        this.cardH = this.winHeight - 68;
        this.cardsStartX = this.winLeft + 16;
        this.cardsY = this.winTop + 30;

        // Если открыто модальное диалоговое окно подтверждения удаления — рендерим ТОЛЬКО кнопки диалога!
        if (this.modalDeleteSlot >= 0 && this.modalDeleteSlot < CharacterSlotsData.MAX_SLOTS) {
            int dialogW = 290;
            int dialogH = 104;
            int dx = (this.width - dialogW) / 2;
            int dy = (this.height - dialogH) / 2;

            int btnW = 122;
            this.addRenderableWidget(Button.builder(Component.literal("§c§lДа, удалить"), btn -> {
                int target = this.modalDeleteSlot;
                this.modalDeleteSlot = -1;
                PacketDistributor.sendToServer(new SlotActionPayload(SlotActionPayload.ACTION_DELETE_SLOT, target));
                rebuildWidgets();
            }).bounds(dx + 16, dy + dialogH - 28, btnW, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("§aОтмена"), btn -> {
                this.modalDeleteSlot = -1;
                rebuildWidgets();
            }).bounds(dx + dialogW - btnW - 16, dy + dialogH - 28, btnW, 20).build());
            return;
        }

        // Обычные кнопки слотов (активны, когда модальное окно закрыто)
        for (int i = 0; i < CharacterSlotsData.MAX_SLOTS; i++) {
            final int slotIdx = i;
            CharacterSlotsData.ClientSlotSummary summary = clientSlots.get(i);
            int cx = cardsStartX + i * (cardW + cardGap);
            boolean isCreated = summary.created() && summary.appearance().isCreated();

            if (!isCreated) {
                int plusSize = 44;
                int plusX = cx + (cardW - plusSize) / 2;
                int plusY = cardsY + (cardH - plusSize) / 2;

                this.addRenderableWidget(Button.builder(Component.literal("§6§l+"), btn -> {
                    PacketDistributor.sendToServer(new SlotActionPayload(SlotActionPayload.ACTION_SELECT_OR_CREATE, slotIdx));
                }).bounds(plusX, plusY, plusSize, plusSize).build());
            } else {
                int btnW = cardW - 12;
                int playY = cardsY + cardH - 40;
                int delY = cardsY + cardH - 20;

                // Вход в мир ТОЛЬКО по этой кнопке!
                this.addRenderableWidget(Button.builder(Component.literal("§a▶ Войти в мир"), btn -> {
                    PacketDistributor.sendToServer(new SlotActionPayload(SlotActionPayload.ACTION_SELECT_OR_CREATE, slotIdx));
                }).bounds(cx + 6, playY, btnW, 17).build());

                // Кнопка "Удалить" открывает отдельное модальное окно с предупреждением!
                this.addRenderableWidget(Button.builder(Component.literal("§7Удалить"), btn -> {
                    this.modalDeleteSlot = slotIdx;
                    rebuildWidgets();
                }).bounds(cx + 6, delY, btnW, 16).build());
            }
        }

        int exitW = 190;
        this.addRenderableWidget(Button.builder(Component.literal("§cВыйти в главное меню"), btn -> {
            disconnectToMainMenu();
        }).bounds(this.winLeft + (this.winWidth - exitW) / 2, this.winTop + this.winHeight - 28, exitW, 20).build());
    }

    private void disconnectToMainMenu() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            boolean singleplayer = mc.isLocalServer();
            mc.level.disconnect();
            if (singleplayer) {
                mc.disconnect(new GenericMessageScreen(Component.literal("Сохранение мира...")));
                mc.setScreen(new TitleScreen());
            } else {
                mc.disconnect();
                mc.setScreen(new JoinMultiplayerScreen(new TitleScreen()));
            }
        } else {
            mc.setScreen(new TitleScreen());
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        SkyrimThemeRenderer.renderBlurredDarkBackground(this, gui, partialTick);

        SkyrimThemeRenderer.drawRpgFrame(gui, winLeft, winTop, winWidth, winHeight, SkyrimThemeRenderer.HUD_BORDER_GOLD);

        String title = "§6§lВыбор персонажа";
        gui.drawString(this.font, title, winLeft + (winWidth - this.font.width(title)) / 2, winTop + 11, 0xFFFFFFFF, true);

        CharacterSlotsData.ClientSlotSummary hoveredSlot = null;

        for (int i = 0; i < CharacterSlotsData.MAX_SLOTS; i++) {
            CharacterSlotsData.ClientSlotSummary slot = clientSlots.get(i);
            int cx = cardsStartX + i * (cardW + cardGap);
            int cy = cardsY;

            boolean isHovered = (this.modalDeleteSlot < 0)
                    && (mouseX >= cx && mouseX < cx + cardW && mouseY >= cy && mouseY < cy + cardH);
            if (isHovered) {
                hoveredSlot = slot;
            }

            boolean isCreated = slot.created() && slot.appearance().isCreated();
            boolean isActive = (i == clientActiveSlot && isCreated);
            int borderCol = isHovered ? 0xFFD4AF37 : (isActive ? SkyrimThemeRenderer.HUD_BORDER_GOLD : SkyrimThemeRenderer.HUD_BORDER_STEEL);

            SkyrimThemeRenderer.drawRpgFrame(gui, cx, cy, cardW, cardH, borderCol);

            int stageX = cx + 5;
            int stageY = cy + 20;
            int stageW = cardW - 10;
            int stageH = isCreated ? (cardH - 66) : (cardH - 28);

            gui.fillGradient(stageX, stageY, stageX + stageW, stageY + stageH, 0xFF14171F, 0xFF0C0E13);
            gui.renderOutline(stageX, stageY, stageW, stageH, 0xFF282C36);

            String slotHeader = isCreated
                    ? clipText("§e" + slot.appearance().getFullRpName(), cardW - 10)
                    : ("§7" + SLOT_TITLES[i]);
            gui.drawString(this.font, slotHeader, cx + (cardW - this.font.width(slotHeader)) / 2, cy + 7, 0xFFFFFFFF, true);

            if (isCreated) {
                int lookX = (this.modalDeleteSlot < 0) ? mouseX : (stageX + stageW / 2);
                int lookY = (this.modalDeleteSlot < 0) ? mouseY : (stageY + stageH / 3);
                renderSlotCharacterInGear(gui, slot, stageX, stageY, stageW, stageH, lookX, lookY);

                String raceBadge = "§7" + slot.appearance().race().getTitle();
                gui.drawString(this.font, raceBadge, stageX + (stageW - this.font.width(raceBadge)) / 2, stageY + stageH - 11, 0xFFCCCCCC, true);
            } else {
                String emptySub = "§8Пустой слот";
                gui.drawString(this.font, emptySub, stageX + (stageW - this.font.width(emptySub)) / 2, stageY + stageH - 14, 0xFF777777, false);
            }
        }

        // Если открыто модальное окно подтверждения удаления персонажа — рисуем его поверх всего экрана!
        if (this.modalDeleteSlot >= 0 && this.modalDeleteSlot < CharacterSlotsData.MAX_SLOTS) {
            gui.pose().pushPose();
            gui.pose().translate(0.0F, 0.0F, 400.0F);

            // Затемнение заднего плана под модальным окном
            gui.fill(0, 0, this.width, this.height, 0xBB050608);

            int dialogW = 290;
            int dialogH = 104;
            int dx = (this.width - dialogW) / 2;
            int dy = (this.height - dialogH) / 2;

            SkyrimThemeRenderer.drawRpgFrame(gui, dx, dy, dialogW, dialogH, 0xFFD32F2F);

            CharacterSlotsData.ClientSlotSummary targetSlot = clientSlots.get(this.modalDeleteSlot);
            String charName = targetSlot.appearance().getFullRpName();

            gui.drawCenteredString(this.font, "§c§lПодтверждение удаления", this.width / 2, dy + 12, 0xFFFFFFFF);
            gui.drawCenteredString(this.font, "§fВы действительно хотите удалить персонажа", this.width / 2, dy + 30, 0xFFFFFFFF);
            gui.drawCenteredString(this.font, "§e«" + charName + "»§f?", this.width / 2, dy + 42, 0xFFFFFFFF);
            gui.drawCenteredString(this.font, "§cДействие будет необратимо!", this.width / 2, dy + 56, 0xFFFF5555);

            super.render(gui, mouseX, mouseY, partialTick);
            gui.pose().popPose();
            return;
        }

        super.render(gui, mouseX, mouseY, partialTick);

        if (hoveredSlot != null) {
            List<FormattedCharSequence> tooltip = new ArrayList<>();
            if (hoveredSlot.created() && hoveredSlot.appearance().isCreated()) {
                PlayerAppearanceData app = hoveredSlot.appearance();
                tooltip.add(Component.literal("§6§l" + app.getFullRpName()).getVisualOrderText());
                tooltip.add(Component.literal("§7Раса: §f" + app.race().getTitle() + " §8(" + app.gender().getTitle() + ")").getVisualOrderText());

                String skillsText = (hoveredSlot.skillsSummary() == null || hoveredSlot.skillsSummary().isEmpty())
                        ? "Нет изученных веток"
                        : hoveredSlot.skillsSummary();
                for (FormattedCharSequence line : this.font.split(Component.literal("§7Прокачанные ветки: §a" + skillsText), 210)) {
                    tooltip.add(line);
                }

                tooltip.add(Component.literal("§7Валюта: §e" + hoveredSlot.currency() + " монет").getVisualOrderText());
            } else {
                tooltip.add(Component.literal("§6§l" + SLOT_TITLES[hoveredSlot.slotIndex()]).getVisualOrderText());
                tooltip.add(Component.literal("§7Статус: §8Свободный слот").getVisualOrderText());
                tooltip.add(Component.literal("§a▶ Нажмите [+], чтобы создать персонажа!").getVisualOrderText());
            }
            gui.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    private void renderSlotCharacterInGear(GuiGraphics gui, CharacterSlotsData.ClientSlotSummary slot,
                                           int stageX, int stageY, int stageW, int stageH, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        PlayerAppearanceData origApp = mc.player.getData(ModAttachments.APPEARANCE);
        ExtraEquipmentData origExtra = mc.player.getData(ModAttachments.EXTRA_EQUIPMENT);
        ItemStack origHead = mc.player.getItemBySlot(EquipmentSlot.HEAD).copy();
        ItemStack origChest = mc.player.getItemBySlot(EquipmentSlot.CHEST).copy();
        ItemStack origLegs = mc.player.getItemBySlot(EquipmentSlot.LEGS).copy();
        ItemStack origFeet = mc.player.getItemBySlot(EquipmentSlot.FEET).copy();
        ItemStack origMain = mc.player.getItemBySlot(EquipmentSlot.MAINHAND).copy();
        ItemStack origOff = mc.player.getItemBySlot(EquipmentSlot.OFFHAND).copy();

        ClientEvents.renderingInventoryDoll = true;
        try {
            mc.player.setData(ModAttachments.APPEARANCE, slot.appearance());
            mc.player.setData(ModAttachments.EXTRA_EQUIPMENT, slot.extraEquipment());

            List<ItemStack> g = slot.visualGear();
            if (g.size() >= 6) {
                mc.player.setItemSlot(EquipmentSlot.HEAD, g.get(0));
                mc.player.setItemSlot(EquipmentSlot.CHEST, g.get(1));
                mc.player.setItemSlot(EquipmentSlot.LEGS, g.get(2));
                mc.player.setItemSlot(EquipmentSlot.FEET, g.get(3));
                mc.player.setItemSlot(EquipmentSlot.MAINHAND, g.get(4));
                mc.player.setItemSlot(EquipmentSlot.OFFHAND, g.get(5));
            }

            boolean isOrc = (slot.appearance().race() == RaceType.ORC);
            float yOff = isOrc ? -0.35F : 0.0625F;
            int scale = isOrc ? 34 : 40;

            SkyrimThemeRenderer.renderStandingDoll(
                    gui,
                    stageX + 4, stageY + 4,
                    stageX + stageW - 4, stageY + stageH - 12,
                    scale, yOff, mouseX, mouseY, mc.player
            );
        } finally {
            mc.player.setData(ModAttachments.APPEARANCE, origApp);
            mc.player.setData(ModAttachments.EXTRA_EQUIPMENT, origExtra);
            mc.player.setItemSlot(EquipmentSlot.HEAD, origHead);
            mc.player.setItemSlot(EquipmentSlot.CHEST, origChest);
            mc.player.setItemSlot(EquipmentSlot.LEGS, origLegs);
            mc.player.setItemSlot(EquipmentSlot.FEET, origFeet);
            mc.player.setItemSlot(EquipmentSlot.MAINHAND, origMain);
            mc.player.setItemSlot(EquipmentSlot.OFFHAND, origOff);
            ClientEvents.renderingInventoryDoll = false;
        }
    }

    // ИСПРАВЛЕНО: Убран вход в мир при клике по 3D-модельке персонажа! Теперь вход работает ТОЛЬКО по кнопке "Войти в мир"!
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}