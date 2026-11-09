package com.rpgcore.mixin;

import com.rpgcore.data.ExtraEquipmentData;
import com.rpgcore.data.ExtraInventorySlot;
import com.rpgcore.data.ModAttachments;
import com.rpgcore.data.RaceType;
import com.rpgcore.network.SetExtraSlotPayload;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin {

    @Inject(method = "selectTab", at = @At("RETURN"))
    private void rpgcore$fixSurvivalTabInCreative(CreativeModeTab tab, CallbackInfo ci) {
        if (tab.getType() != CreativeModeTab.Type.INVENTORY) return;

        CreativeModeInventoryScreen self = (CreativeModeInventoryScreen) (Object) this;
        AbstractContainerMenu menu = self.getMenu();
        Player player = Minecraft.getInstance().player;
        boolean isDwarf = player != null && player.getData(ModAttachments.APPEARANCE).race() == RaceType.DWARF;

        for (Slot wrapper : menu.slots) {
            Slot target = rpgcore$unwrapTargetSlot(wrapper);

            if (!(target instanceof ExtraInventorySlot)) {
                int idx = target.getContainerSlot();
                if (idx == 39) rpgcore$setPos(wrapper, 9, 6);
                else if (idx == 38) rpgcore$setPos(wrapper, 9, 24);
                else if (idx == 37) rpgcore$setPos(wrapper, 27, 6);
                else if (idx == 36) rpgcore$setPos(wrapper, 27, 24);
                else if (idx == 40) rpgcore$setPos(wrapper, 45, 24);
                else if (isDwarf) {
                    if (idx >= 9 && idx < 36) {
                        int row = (idx - 9) / 9;
                        int col = (idx - 9) % 9;
                        rpgcore$setPos(wrapper, 9 + col * 18, 72 + row * 18);
                    } else if (idx >= 0 && idx < 9) {
                        rpgcore$setPos(wrapper, 9 + idx * 18, 130);
                    }
                }
            }

            if (target instanceof ExtraInventorySlot extra) {
                int eIdx = extra.getExtraSlotIndex();
                switch (eIdx) {
                    case ExtraEquipmentData.SLOT_HAT_UNDER -> rpgcore$setPos(wrapper, 115, 6);
                    case ExtraEquipmentData.SLOT_SHIRT_UNDER -> rpgcore$setPos(wrapper, 115, 24);
                    case ExtraEquipmentData.SLOT_PANTS_UNDER -> rpgcore$setPos(wrapper, 133, 6);
                    case ExtraEquipmentData.SLOT_SHOES_UNDER -> rpgcore$setPos(wrapper, 133, 24);
                    case ExtraEquipmentData.SLOT_CAPE -> rpgcore$setPos(wrapper, 151, 6);
                    case ExtraEquipmentData.SLOT_BACKPACK -> rpgcore$setPos(wrapper, 151, 24);
                    default -> {
                        if (eIdx >= ExtraEquipmentData.DWARF_SLOT_START && eIdx < ExtraEquipmentData.BACKPACK_STORAGE_START) {
                            int d = eIdx - ExtraEquipmentData.DWARF_SLOT_START;
                            rpgcore$setPos(wrapper, 9 + d * 18, 50);
                        } else if (eIdx >= ExtraEquipmentData.BACKPACK_STORAGE_START) {
                            int b = eIdx - ExtraEquipmentData.BACKPACK_STORAGE_START;
                            int col = b % 3;
                            int row = b / 3;
                            rpgcore$setPos(wrapper, -62 + col * 18, 6 + row * 18);
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void rpgcore$handleShiftClickEquipInCreative(Slot slot, int slotId, int mouseButton, ClickType type, CallbackInfo ci) {
        if (type != ClickType.QUICK_MOVE || slot == null || !slot.hasItem()) return;
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        Slot unwrappedClicked = rpgcore$unwrapTargetSlot(slot);
        int menuSlotIdx = unwrappedClicked.index;

        if (menuSlotIdx >= 0 && menuSlotIdx < player.inventoryMenu.slots.size()) {
            ItemStack moved = player.inventoryMenu.quickMoveStack(player, menuSlotIdx);
            if (!moved.isEmpty() && Minecraft.getInstance().getConnection() != null) {
                // Синхронизируем с сервером как ванильные слоты (1..45), так и наши кастомные ExtraInventorySlot!
                for (int i = 1; i < player.inventoryMenu.slots.size(); i++) {
                    Slot s = player.inventoryMenu.slots.get(i);
                    if (s instanceof ExtraInventorySlot ex) {
                        PacketDistributor.sendToServer(new SetExtraSlotPayload(ex.getExtraSlotIndex(), ex.getItem().copy()));
                    } else if (i <= 45) {
                        Minecraft.getInstance().getConnection().send(
                                new ServerboundSetCreativeModeSlotPacket(i, s.getItem().copy())
                        );
                    }
                }
                ci.cancel();
            }
        }
    }

    @Unique
    private static Slot rpgcore$unwrapTargetSlot(Slot slot) {
        try {
            for (Field f : slot.getClass().getDeclaredFields()) {
                if (Slot.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Object inner = f.get(slot);
                    if (inner instanceof Slot target) {
                        return target;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return slot;
    }

    @Unique
    private static void rpgcore$setPos(Slot slot, int newX, int newY) {
        ((SlotAccessor) slot).rpgcore$setX(newX);
        ((SlotAccessor) slot).rpgcore$setY(newY);
    }
}