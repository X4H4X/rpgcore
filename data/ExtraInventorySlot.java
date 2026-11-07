package com.rpgcore.data;

import com.rpgcore.network.ModNetworking;
import com.rpgcore.network.SetExtraSlotPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class ExtraInventorySlot extends Slot {

    private final Player owner;
    private final int slotIndex;
    private final boolean dwarfStorageSlot;
    private final boolean backpackStorageSlot;

    public ExtraInventorySlot(Player owner, int slotIndex, int x, int y) {
        this(owner, slotIndex, x, y, false, false);
    }

    public ExtraInventorySlot(Player owner, int slotIndex, int x, int y, boolean dwarfStorageSlot) {
        this(owner, slotIndex, x, y, dwarfStorageSlot, false);
    }

    public ExtraInventorySlot(Player owner, int slotIndex, int x, int y, boolean dwarfStorageSlot, boolean backpackStorageSlot) {
        // У каждого слота свой независимый SimpleContainer(1) — никаких конфликтов между слотами!
        super(new SimpleContainer(1), 0, x, y);
        this.owner = owner;
        this.slotIndex = slotIndex;
        this.dwarfStorageSlot = dwarfStorageSlot;
        this.backpackStorageSlot = backpackStorageSlot;
    }

    public int getExtraSlotIndex() {
        return slotIndex;
    }

    public boolean isDwarfStorageSlot() {
        return dwarfStorageSlot;
    }

    public boolean isBackpackStorageSlot() {
        return backpackStorageSlot;
    }

    private ExtraEquipmentData getData() {
        return owner.getData(ModAttachments.EXTRA_EQUIPMENT);
    }

    @Override
    public boolean isActive() {
        if (dwarfStorageSlot) {
            return owner.getData(ModAttachments.APPEARANCE).race() == RaceType.DWARF;
        }
        if (backpackStorageSlot) {
            if (owner.level().isClientSide && !ExtraEquipmentData.clientBackpackExpanded) {
                return false;
            }
            int bagIdx = slotIndex - ExtraEquipmentData.BACKPACK_STORAGE_START;
            return bagIdx >= 0 && bagIdx < getData().getActiveBackpackSlotCount();
        }
        return true;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (dwarfStorageSlot) {
            return owner.getData(ModAttachments.APPEARANCE).race() == RaceType.DWARF;
        }
        if (backpackStorageSlot) {
            if (stack.getItem() instanceof BackpackItem) return false;
            int bagIdx = slotIndex - ExtraEquipmentData.BACKPACK_STORAGE_START;
            return bagIdx >= 0 && bagIdx < getData().getActiveBackpackSlotCount();
        }
        if (stack.getItem() instanceof PeasantClothesItem clothes) {
            return clothes.getUnderArmorSlotIndex() == this.slotIndex;
        }
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        if (slotIndex == ExtraEquipmentData.SLOT_BACKPACK) {
            ExtraEquipmentData data = getData();
            for (int i = 0; i < ExtraEquipmentData.MAX_BACKPACK_SLOTS; i++) {
                if (!data.getStack(ExtraEquipmentData.BACKPACK_STORAGE_START + i).isEmpty()) {
                    return false;
                }
            }
        }
        return super.mayPickup(player);
    }

    @Override
    public ItemStack getItem() {
        return getData().getStack(slotIndex);
    }

    @Override
    public void set(ItemStack stack) {
        ItemStack copy = stack.copy();
        getData().setStack(slotIndex, copy);
        this.setChanged();
        // Если изменение произошло на клиенте в режиме Креатива — сразу отправляем серверу точный предмет слота!
        if (owner.level().isClientSide && owner.isCreative()) {
            PacketDistributor.sendToServer(new SetExtraSlotPayload(this.slotIndex, copy));
        }
    }

    @Override
    public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
        this.set(newStack);
    }

    @Override
    public void setChanged() {
        if (!owner.level().isClientSide && owner instanceof ServerPlayer sp) {
            ModNetworking.broadcastExtraEquipment(sp);
        }
    }

    @Override
    public int getMaxStackSize() {
        return (dwarfStorageSlot || backpackStorageSlot) ? 64 : 1;
    }

    @Override
    public ItemStack remove(int amount) {
        ItemStack cur = getItem();
        if (cur.isEmpty()) return ItemStack.EMPTY;
        ItemStack split = cur.split(amount);
        getData().setStack(slotIndex, cur);
        this.setChanged();
        if (owner.level().isClientSide && owner.isCreative()) {
            PacketDistributor.sendToServer(new SetExtraSlotPayload(this.slotIndex, cur.copy()));
        }
        return split;
    }
}