package com.rpgcore.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSliderButton.class)
public abstract class AbstractSliderButtonMixin extends AbstractWidget {

    @Shadow protected double value;

    public AbstractSliderButtonMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void rpgcore$renderSkyrimMinimalistSlider(GuiGraphics gui, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        int x = this.getX();
        int y = this.getY();
        int w = this.getWidth();
        int h = this.getHeight();

        // 1. Черно-прозрачный фон ползунка в стиле Skyrim
        int bg = !this.active ? 0x8808080A : (this.isHoveredOrFocused() ? 0xCC161820 : 0xB80C0D10);
        int border = !this.active ? 0xFF2C2E33 : (this.isHoveredOrFocused() ? 0xFF9EA4B0 : 0xFF484B52);

        gui.fill(x, y, x + w, y + h, bg);

        // 2. Внутренняя полоса заполнения и каретка ползунка
        int fillW = (int) (this.value * (double) (w - 4));
        if (fillW > 0) {
            gui.fill(x + 2, y + 2, x + 2 + fillW, y + h - 2, 0x663A404D);
        }

        int handleX = x + (int) (this.value * (double) (w - 8));
        int handleCol = this.isHoveredOrFocused() ? 0xFFD4AF37 : 0xFF7A7E85;
        gui.fill(handleX, y + 1, handleX + 8, y + h - 1, 0xEE1E2026);
        gui.renderOutline(handleX, y + 1, 8, h - 2, handleCol);

        // 3. Внешняя тонкая рамка и текст по центру
        gui.renderOutline(x, y, w, h, border);

        int textColor = this.getFGColor() | Mth.ceil(this.alpha * 255.0F) << 24;
        this.renderScrollingString(gui, Minecraft.getInstance().font, 2, textColor);
        ci.cancel();
    }
}