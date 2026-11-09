package com.rpgcore.mixin;

import com.rpgcore.client.SkyrimThemeRenderer;
import com.rpgcore.data.ExtraEquipmentData;
import com.rpgcore.data.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class SkyrimContainerScreenMixin {

    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageWidth;
    @Shadow protected int imageHeight;

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void rpgcore$renderSkyrimContainerBg(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        guiGraphics.fill(0, 0, self.width, self.height, 0x88050508);
        SkyrimThemeRenderer.renderContainerWindow(
                guiGraphics, self,
                this.leftPos, this.topPos,
                this.imageWidth, this.imageHeight,
                mouseX, mouseY
        );
        ci.cancel();
    }

    // Перехватываем вызов renderLabels внутри render(), чтобы подавить тёмную надпись «Создание» в InventoryScreen!
    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderLabels(Lnet/minecraft/client/gui/GuiGraphics;II)V"
            )
    )
    private void rpgcore$redirectRenderLabels(AbstractContainerScreen<?> instance, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Не вызываем ванильный renderLabels вообще — никаких кривых надписей поверх слотов!
    }

    @Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
    private void rpgcore$allowClicksInLeftBackpackPanel(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton, CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getData(ModAttachments.EXTRA_EQUIPMENT).getActiveBackpackSlotCount() > 0) {
            if (mouseX >= guiLeft - 74 && mouseX <= guiLeft && mouseY >= guiTop - 36 && mouseY <= guiTop + this.imageHeight + 24) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void rpgcore$onBackpackToggleClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        boolean isInv = (self instanceof InventoryScreen);
        boolean isCreativeSurv = (self instanceof CreativeModeInventoryScreen) && SkyrimThemeRenderer.isSurvivalInventoryTabSelected();
        if (!isInv && !isCreativeSurv) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        int bagSlots = mc.player.getData(ModAttachments.EXTRA_EQUIPMENT).getActiveBackpackSlotCount();
        if (bagSlots <= 0) return;

        int btnX = this.leftPos - 16;
        int btnY = this.topPos + 45;
        if (mouseX >= btnX && mouseX < btnX + 12 && mouseY >= btnY && mouseY < btnY + 22) {
            ExtraEquipmentData.clientBackpackExpanded = !ExtraEquipmentData.clientBackpackExpanded;
            cir.setReturnValue(true);
        }
    }
}