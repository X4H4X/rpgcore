package com.rpgcore.data;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CorpseMenu extends AbstractContainerMenu {

    private final PlayerCorpseEntity corpse;
    private final Container corpseContainer;

    public CorpseMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(containerId, playerInv, getCorpseFromBuf(playerInv.player, extraData));
    }

    private static PlayerCorpseEntity getCorpseFromBuf(Player player, FriendlyByteBuf buf) {
        int id = buf.readVarInt();
        Entity ent = player.level().getEntity(id);
        return (ent instanceof PlayerCorpseEntity c) ? c : null;
    }

    public CorpseMenu(int containerId, Inventory playerInv, PlayerCorpseEntity corpseEntity) {
        super(ModItemsAndBlocks.CORPSE_MENU.get(), containerId);
        this.corpse = corpseEntity;

        if (corpseEntity != null) {
            this.corpseContainer = new Container() {
                @Override public int getContainerSize() { return PlayerCorpseEntity.TOTAL_SLOTS; }
                @Override public boolean isEmpty() { return corpseEntity.isEmptyCorpse(); }
                @Override public ItemStack getItem(int slot) { return corpseEntity.getItems().get(slot); }
                @Override public ItemStack removeItem(int slot, int amount) {
                    ItemStack s = corpseEntity.getItems().get(slot);
                    if (s.isEmpty()) return ItemStack.EMPTY;
                    ItemStack split = s.split(amount);
                    corpseEntity.syncVisualEquipmentToClients();
                    return split;
                }
                @Override public ItemStack removeItemNoUpdate(int slot) {
                    ItemStack s = corpseEntity.getItems().get(slot);
                    corpseEntity.getItems().set(slot, ItemStack.EMPTY);
                    corpseEntity.syncVisualEquipmentToClients();
                    return s;
                }
                @Override public void setItem(int slot, ItemStack stack) {
                    corpseEntity.getItems().set(slot, stack);
                    corpseEntity.syncVisualEquipmentToClients();
                }
                @Override public void setChanged() { corpseEntity.syncVisualEquipmentToClients(); }
                @Override public boolean stillValid(Player p) { return corpseEntity.isAlive() && p.distanceToSqr(corpseEntity) <= 64.0D; }
                @Override public void clearContent() { corpseEntity.getItems().clear(); }
            };
        } else {
            this.corpseContainer = new SimpleContainer(PlayerCorpseEntity.TOTAL_SLOTS);
        }

        // 1. Броня трупа в сетке 2x2 + Щит — ТОЧНО КАК В ИНВЕНТАРЕ ИГРОКА (image_78b37e.png)!
        // 39=шлем (8, -26), 38=нагрудник (8, -8), 37=поножи (26, -26), 36=ботинки (26, -8), 40=щит (44, -8)
        this.addSlot(new CorpseTakeOnlySlot(this.corpseContainer, 39, 8, -26, -1));
        this.addSlot(new CorpseTakeOnlySlot(this.corpseContainer, 38, 8, -8, -1));
        this.addSlot(new CorpseTakeOnlySlot(this.corpseContainer, 37, 26, -26, -1));
        this.addSlot(new CorpseTakeOnlySlot(this.corpseContainer, 36, 26, -8, -1));
        this.addSlot(new CorpseTakeOnlySlot(this.corpseContainer, 40, 44, -8, -1));

        // 2. Одежда крестьянина, Плащ и Рюкзак в сетке 3x2 справа от куклы — ТОЧНО КАК В ИНВЕНТАРЕ ИГРОКА!
        // 41=Шляпа (80, -26), 42=Рубаха (80, -8), 43=Портки (98, -26), 44=Башмаки (98, -8), 45=Плащ (116, -26), 46=Рюкзак (116, -8)
        this.addSlot(new CorpseTakeOnlySlot(this.corpseContainer, 41, 80, -26, ExtraEquipmentData.SLOT_HAT_UNDER));
        this.addSlot(new CorpseTakeOnlySlot(this.corpseContainer, 42, 80, -8, ExtraEquipmentData.SLOT_SHIRT_UNDER));
        this.addSlot(new CorpseTakeOnlySlot(this.corpseContainer, 43, 98, -26, ExtraEquipmentData.SLOT_PANTS_UNDER));
        this.addSlot(new CorpseTakeOnlySlot(this.corpseContainer, 44, 98, -8, ExtraEquipmentData.SLOT_SHOES_UNDER));
        this.addSlot(new CorpseTakeOnlySlot(this.corpseContainer, 45, 116, -26, ExtraEquipmentData.SLOT_CAPE));
        this.addSlot(new CorpseTakeOnlySlot(this.corpseContainer, 46, 116, -8, ExtraEquipmentData.SLOT_BACKPACK));

        // 3. Бонусные карманы Дворфа (47..55) — активны только если в них есть вещи
        for (int i = 0; i < 9; i++) {
            final int slotIdx = 47 + i;
            this.addSlot(new CorpseTakeOnlySlot(this.corpseContainer, slotIdx, 180 + (i % 3) * 18, -26 + (i / 3) * 18, -1) {
                @Override public boolean isActive() { return this.hasItem(); }
            });
        }

        // 4. Содержимое Рюкзака (56..70) слева от окна — активны только если в них есть вещи
        for (int i = 0; i < 15; i++) {
            final int slotIdx = 56 + i;
            this.addSlot(new CorpseTakeOnlySlot(this.corpseContainer, slotIdx, -62 + (i % 3) * 18, -26 + (i / 3) * 18, -1) {
                @Override public boolean isActive() { return this.hasItem(); }
            });
        }

        // 5. Основной инвентарь трупа (3 ряда по 9 слотов: 9..35) и хотбар трупа (0..8)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new CorpseTakeOnlySlot(this.corpseContainer, col + (row + 1) * 9, 8 + col * 18, 18 + row * 18, -1));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new CorpseTakeOnlySlot(this.corpseContainer, col, 8 + col * 18, 76, -1));
        }

        // 6. Хотбар и инвентарь самого игрока снизу (компактно, чтобы окно полностью помещалось на экране!)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + (row + 1) * 9, 8 + col * 18, 104 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 162));
        }
    }

    public PlayerCorpseEntity getCorpse() {
        return corpse;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0 && player instanceof ServerPlayer sp && this.corpse != null) {
            this.corpse.quickRestoreToOwner(sp);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stackInSlot = slot.getItem();
        ItemStack copy = stackInSlot.copy();

        int corpseSlotCount = PlayerCorpseEntity.TOTAL_SLOTS;
        if (index < corpseSlotCount) {
            if (!this.moveItemStackTo(stackInSlot, corpseSlotCount, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stackInSlot.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.corpseContainer.stillValid(player);
    }

    public static class CorpseTakeOnlySlot extends Slot {
        private final int iconExtraIndex;

        public CorpseTakeOnlySlot(Container container, int slot, int x, int y, int iconExtraIndex) {
            super(container, slot, x, y);
            this.iconExtraIndex = iconExtraIndex;
        }

        public int getIconExtraIndex() {
            return iconExtraIndex;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}