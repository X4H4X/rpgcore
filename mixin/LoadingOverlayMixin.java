package com.rpgcore.mixin;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {

    @Shadow @Final private ReloadInstance reload;
    @Shadow @Final private Consumer<Optional<Throwable>> onFinish;
    @Shadow private float currentProgress;
    @Shadow private long fadeOutStart;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void rpgcore$renderCustomDarkLoading(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        int sw = graphics.guiWidth();
        int sh = graphics.guiHeight();
        long now = Util.getMillis();

        float fadeOut = this.fadeOutStart > -1L ? (float) (now - this.fadeOutStart) / 1000.0F : -1.0F;

        if (fadeOut >= 1.0F) {
            if (mc.screen != null) {
                mc.screen.render(graphics, 0, 0, partialTick);
            }
            int bgAlpha = Mth.ceil((1.0F - Mth.clamp(fadeOut - 1.0F, 0.0F, 1.0F)) * 255.0F);
            graphics.fill(0, 0, sw, sh, FastColor.ARGB32.color(bgAlpha, 18, 19, 24));
        } else {
            // Сплошной тёмно-серый фон в стиле нашего HUD (#121318) без красного цвета и лисы!
            graphics.fill(0, 0, sw, sh, 0xFF121318);
        }

        float actual = this.reload.getActualProgress();
        this.currentProgress = Mth.clamp(this.currentProgress * 0.95F + actual * 0.05F, 0.0F, 1.0F);

        if (fadeOut < 1.0F) {
            int barW = 220;
            int barH = 8;
            int barX = (sw - barW) / 2;
            int barY = sh / 2;

            graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF0A0B0E);
            graphics.renderOutline(barX, barY, barW, barH, 0xFF4B4E56);

            int filled = Math.round((barW - 4) * this.currentProgress);
            if (filled > 0) {
                graphics.fill(barX + 2, barY + 2, barX + 2 + filled, barY + barH - 2, 0xFFFFFFFF);
            }
        }

        if (fadeOut >= 2.0F) {
            mc.setOverlay(null);
        }

        if (this.fadeOutStart == -1L && this.reload.isDone()) {
            try {
                this.reload.checkExceptions();
                this.onFinish.accept(Optional.empty());
            } catch (Throwable t) {
                this.onFinish.accept(Optional.of(t));
            }
            this.fadeOutStart = Util.getMillis();
            if (mc.screen != null) {
                mc.screen.init(mc, sw, sh);
            }
        }

        ci.cancel();
    }
}