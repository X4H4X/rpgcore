package com.rpgcore.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractButton.class)
public abstract class AbstractWidgetMixin {

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void rpgcore$renderSkyrimMinimalistButton(GuiGraphics gui, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        AbstractButton self = (AbstractButton) (Object) this;
        int x = self.getX();
        int y = self.getY();
        int w = self.getWidth();
        int h = self.getHeight();

        int bg = !self.active ? 0x8808080A : (self.isHoveredOrFocused() ? 0xCC1E2028 : 0xB80C0D10);
        int border = !self.active ? 0xFF2C2E33 : (self.isHoveredOrFocused() ? 0xFF9EA4B0 : 0xFF484B52);

        gui.fill(x, y, x + w, y + h, bg);
        gui.renderOutline(x, y, w, h, border);

        int textColor = self.getFGColor() | Mth.ceil(255.0F) << 24;
        self.renderString(gui, Minecraft.getInstance().font, textColor);
        ci.cancel();
    }
}