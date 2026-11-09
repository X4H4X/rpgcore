package com.rpgcore.client;

import com.rpgcore.RPGCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@EventBusSubscriber(modid = RPGCore.MODID, value = Dist.CLIENT)
public class SmoothInventoryAnimator {

    private static class SlotSnapshot {
        final int slotIndex;
        final int x;
        final int y;
        final ItemStack stack;

        SlotSnapshot(int slotIndex, int x, int y, ItemStack stack) {
            this.slotIndex = slotIndex;
            this.x = x;
            this.y = y;
            this.stack = stack.copy();
        }
    }

    private static class FlyingItemAnim {
        final ItemStack stack;
        final float startX;
        final float startY;
        final float endX;
        final float endY;
        final long startTimeMs;
        static final long DURATION_MS = 190L;

        FlyingItemAnim(ItemStack stack, float startX, float startY, float endX, float endY) {
            this.stack = stack.copy();
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.startTimeMs = System.currentTimeMillis();
        }
    }

    private static AbstractContainerScreen<?> trackedScreen = null;
    private static final List<SlotSnapshot> PREV_SLOTS = new ArrayList<>();
    private static final List<FlyingItemAnim> ACTIVE_ANIMS = new ArrayList<>();

    @SubscribeEvent
    public static void onContainerRenderPost(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
            trackedScreen = null;
            PREV_SLOTS.clear();
            ACTIVE_ANIMS.clear();
            return;
        }

        int guiLeft = screen.getGuiLeft();
        int guiTop = screen.getGuiTop();
        var slots = screen.getMenu().slots;

        if (trackedScreen != screen || PREV_SLOTS.size() != slots.size()) {
            trackedScreen = screen;
            captureSnapshot(slots);
            ACTIVE_ANIMS.clear();
            return;
        }

        // Ищем слоты, из которых предмет убыл, и слоты, в которые такой же предмет прибыл в этом кадре
        List<SlotSnapshot> removedList = new ArrayList<>();
        List<SlotSnapshot> addedList = new ArrayList<>();

        for (int i = 0; i < slots.size(); i++) {
            Slot curSlot = slots.get(i);
            if (!curSlot.isActive()) continue;

            ItemStack prevStack = PREV_SLOTS.get(i).stack;
            ItemStack curStack = curSlot.getItem();

            if (!prevStack.isEmpty() && (curStack.isEmpty() || !ItemStack.isSameItemSameComponents(prevStack, curStack) || curStack.getCount() < prevStack.getCount())) {
                int diff = prevStack.getCount() - (ItemStack.isSameItemSameComponents(prevStack, curStack) ? curStack.getCount() : 0);
                ItemStack moved = prevStack.copyWithCount(Math.max(1, diff));
                removedList.add(new SlotSnapshot(i, curSlot.x, curSlot.y, moved));
            } else if (!curStack.isEmpty() && (prevStack.isEmpty() || !ItemStack.isSameItemSameComponents(prevStack, curStack) || curStack.getCount() > prevStack.getCount())) {
                int diff = curStack.getCount() - (ItemStack.isSameItemSameComponents(prevStack, curStack) ? prevStack.getCount() : 0);
                ItemStack moved = curStack.copyWithCount(Math.max(1, diff));
                addedList.add(new SlotSnapshot(i, curSlot.x, curSlot.y, moved));
            }
        }

        // Сопоставляем убывшие и прибывшие слоты одного предмета -> запускаем плавный перелет!
        for (SlotSnapshot rem : removedList) {
            for (Iterator<SlotSnapshot> it = addedList.iterator(); it.hasNext(); ) {
                SlotSnapshot add = it.next();
                if (add.slotIndex != rem.slotIndex && ItemStack.isSameItemSameComponents(rem.stack, add.stack)) {
                    ACTIVE_ANIMS.add(new FlyingItemAnim(
                            add.stack,
                            guiLeft + rem.x,
                            guiTop + rem.y,
                            guiLeft + add.x,
                            guiTop + add.y
                    ));
                    it.remove();
                    break;
                }
            }
        }

        captureSnapshot(slots);

        // Рендерим летящие предметы с плавной кривой Ease-Out Cubic и легким подпрыгиванием масштаба
        if (!ACTIVE_ANIMS.isEmpty()) {
            GuiGraphics gui = event.getGuiGraphics();
            Minecraft mc = Minecraft.getInstance();
            long now = System.currentTimeMillis();

            Iterator<FlyingItemAnim> animIt = ACTIVE_ANIMS.iterator();
            while (animIt.hasNext()) {
                FlyingItemAnim anim = animIt.next();
                float rawT = (now - anim.startTimeMs) / (float) FlyingItemAnim.DURATION_MS;
                if (rawT >= 1.0F) {
                    animIt.remove();
                    continue;
                }

                // Плавное торможение в конце перелета (Cubic Ease-Out)
                float inv = 1.0F - rawT;
                float easeT = 1.0F - inv * inv * inv;

                float curX = Mth.lerp(easeT, anim.startX, anim.endX);
                // Легкая дуга вверх в середине полета
                float arcOffset = -Mth.sin(rawT * (float) Math.PI) * 10.0F;
                float curY = Mth.lerp(easeT, anim.startY, anim.endY) + arcOffset;
                float scale = 1.0F + Mth.sin(rawT * (float) Math.PI) * 0.18F;

                gui.pose().pushPose();
                gui.pose().translate(curX + 8.0F, curY + 8.0F, 320.0F);
                gui.pose().scale(scale, scale, 1.0F);
                gui.pose().translate(-8.0F, -8.0F, 0.0F);
                gui.renderItem(anim.stack, 0, 0);
                gui.renderItemDecorations(mc.font, anim.stack, 0, 0);
                gui.pose().popPose();
            }
        }
    }

    private static void captureSnapshot(List<Slot> slots) {
        PREV_SLOTS.clear();
        for (int i = 0; i < slots.size(); i++) {
            Slot s = slots.get(i);
            PREV_SLOTS.add(new SlotSnapshot(i, s.x, s.y, s.getItem()));
        }
    }
}