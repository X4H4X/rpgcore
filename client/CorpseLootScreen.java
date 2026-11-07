package com.rpgcore.client;

import com.rpgcore.data.CorpseMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class CorpseLootScreen extends AbstractContainerScreen<CorpseMenu> {

    public CorpseLootScreen(CorpseMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 184;
    }

    @Override
    protected void init() {
        super.init();
        // Смещаем окно чуть ниже центра, чтобы верхняя шапка с куклой и кнопкой идеально помещалась на экране
        this.topPos = (this.height - this.imageHeight) / 2 + 18;

        int btnW = 156; // Достаточная ширина, чтобы надпись "✦ Забрать и надеть всё" не обрезалась!
        int btnH = 18;
        int btnX = this.leftPos + (this.imageWidth - btnW) / 2;
        int btnY = this.topPos - 56;

        this.addRenderableWidget(Button.builder(
                Component.literal("§e✦ Забрать и надеть всё ✦"),
                btn -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                    }
                }
        ).bounds(btnX, btnY, btnW, btnH).build());
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
    }
}