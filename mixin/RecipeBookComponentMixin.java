package com.rpgcore.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.List;

@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookComponentMixin {

    @Shadow private int xOffset;
    @Shadow private int width;
    @Shadow private int height;
    @Shadow private EditBox searchBox;
    @Shadow protected StateSwitchingButton filterButton;
    @Shadow @Final private List<RecipeBookTabButton> tabButtons;
    @Shadow @Final private RecipeBookPage recipeBookPage;

    @Shadow public abstract boolean isVisible();

    @Unique private static Field rpgcore$buttonsField;

    static {
        try {
            for (Field f : RecipeBookPage.class.getDeclaredFields()) {
                if (List.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    rpgcore$buttonsField = f;
                    break;
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void rpgcore$renderSkyrimRecipeBook(GuiGraphics gui, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!this.isVisible()) {
            ci.cancel();
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int left = (this.width - 147) / 2 - this.xOffset;
        int top = (this.height - 166) / 2;

        gui.pose().pushPose();
        gui.pose().translate(0.0F, 0.0F, 100.0F);

        // 1. Боковые вкладки категорий рецептов слева от книги
        for (RecipeBookTabButton tab : this.tabButtons) {
            if (!tab.visible) continue;
            int tx = tab.getX();
            int ty = tab.getY();
            int tw = tab.getWidth();
            int th = tab.getHeight();
            boolean sel = tab.isStateTriggered();
            boolean hov = tab.isHoveredOrFocused();

            gui.fill(tx, ty, tx + tw, ty + th, sel ? 0xEE181A22 : (hov ? 0xCC14161D : 0xB80A0B0E));
            gui.renderOutline(tx, ty, tw, th, sel ? 0xFFD4AF37 : 0xFF3E4046);
            gui.renderItem(tab.getCategory().getIconItems().get(0), tx + 6, ty + 5);
            if (tab.getCategory().getIconItems().size() > 1) {
                gui.renderItem(tab.getCategory().getIconItems().get(1), tx + 10, ty + 6);
            }
        }

        // 2. Главное черно-прозрачное окно книги рецептов
        gui.fill(left, top, left + 147, top + 166, 0xCC0A0A0E);
        gui.renderOutline(left, top, 147, 166, 0xFF45484F);

        // 3. Поле поиска рецептов
        if (this.searchBox != null) {
            int sx = left + 10;
            int sy = top + 11;
            int sw = 94;
            int sh = 18;
            gui.fill(sx, sy, sx + sw, sy + sh, 0xEE12141B);
            gui.renderOutline(sx, sy, sw, sh, this.searchBox.isFocused() ? 0xFFD4AF37 : 0xFF484B52);

            String val = this.searchBox.getValue();
            if (val.isEmpty() && !this.searchBox.isFocused()) {
                gui.drawString(font, "§7Поиск...", sx + 5, sy + 5, 0xFF777777, false);
            } else {
                String cur = (this.searchBox.isFocused() && (System.currentTimeMillis() / 500) % 2 == 0) ? "_" : "";
                gui.drawString(font, font.plainSubstrByWidth(val, sw - 10) + cur, sx + 5, sy + 5, 0xFFFFFFFF, true);
            }
        }

        // 4. Кнопка фильтра «Показать только доступные для крафта»
        if (this.filterButton != null && this.filterButton.visible) {
            int fx = this.filterButton.getX();
            int fy = this.filterButton.getY();
            int fw = this.filterButton.getWidth();
            int fh = this.filterButton.getHeight();
            boolean onlyCraftable = this.filterButton.isStateTriggered();
            boolean hov = this.filterButton.isHoveredOrFocused();

            gui.fill(fx, fy, fx + fw, fy + fh, hov ? 0xEE1E2028 : 0xCC12141B);
            gui.renderOutline(fx, fy, fw, fh, onlyCraftable ? 0xFF55FF55 : (hov ? 0xFFD4AF37 : 0xFF484B52));
            String icon = onlyCraftable ? "§a✓" : "§7Все";
            gui.drawCenteredString(font, icon, fx + fw / 2, fy + (fh - 8) / 2, 0xFFFFFFFF);
        }

        // 5. Ячейки рецептов внутри книги
        try {
            if (rpgcore$buttonsField != null && mc.level != null) {
                @SuppressWarnings("unchecked")
                List<RecipeButton> buttons = (List<RecipeButton>) rpgcore$buttonsField.get(this.recipeBookPage);
                var registryAccess = mc.level.registryAccess();

                for (RecipeButton btn : buttons) {
                    if (!btn.visible) continue;
                    int bx = btn.getX();
                    int by = btn.getY();
                    int bw = btn.getWidth();
                    int bh = btn.getHeight();
                    boolean craftable = btn.getCollection().hasCraftable();
                    boolean hov = btn.isHoveredOrFocused();

                    int bg = craftable ? (hov ? 0xDD222834 : 0xCC12151C) : (hov ? 0xCC281414 : 0xAA160C0C);
                    int border = hov ? 0xFFD4AF37 : (craftable ? 0xFF484B52 : 0xFF6E2828);

                    gui.fill(bx, by, bx + bw, by + bh, bg);
                    gui.renderOutline(bx, by, bw, bh, border);

                    if (btn.getRecipe() != null) {
                        ItemStack resultStack = btn.getRecipe().value().getResultItem(registryAccess);
                        gui.renderFakeItem(resultStack, bx + 4, by + 4);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        gui.pose().popPose();
        ci.cancel();
    }
}