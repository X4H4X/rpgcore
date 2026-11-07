package com.rpgcore.mixin;

import com.rpgcore.data.ExtraEquipmentData;
import com.rpgcore.data.ExtraInventorySlot;
import com.rpgcore.data.ModAttachments;
import com.rpgcore.data.PeasantClothesItem;
import com.rpgcore.data.RaceType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends RecipeBookMenu<CraftingInput, CraftingRecipe> {

    public InventoryMenuMixin(MenuType<?> menuType, int containerId) {
        super(menuType, containerId);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void rpgcore$setupVerticalInventoryAndApparelSlots(Inventory playerInventory, boolean active, Player owner, CallbackInfo ci) {
        if (this.slots.size() > 45) {
            rpgcore$setSlotPos(this.slots.get(5), 8, -28);  // Шлем
            rpgcore$setSlotPos(this.slots.get(6), 8, -10);  // Нагрудник
            rpgcore$setSlotPos(this.slots.get(7), 8, 8);    // Поножи
            rpgcore$setSlotPos(this.slots.get(8), 8, 26);   // Ботинки
            rpgcore$setSlotPos(this.slots.get(45), 26, 26); // Вторая рука (щит)

            rpgcore$setSlotPos(this.slots.get(0), 154, 0);
            rpgcore$setSlotPos(this.slots.get(1), 116, -10);
            rpgcore$setSlotPos(this.slots.get(2), 134, -10);
            rpgcore$setSlotPos(this.slots.get(3), 116, 8);
            rpgcore$setSlotPos(this.slots.get(4), 134, 8);
        }

        // 1. 4 слота под Одежду (индексы меню 46..49)
        this.addSlot(new ExtraInventorySlot(owner, ExtraEquipmentData.SLOT_HAT_UNDER, 78, -28));
        this.addSlot(new ExtraInventorySlot(owner, ExtraEquipmentData.SLOT_SHIRT_UNDER, 78, -10));
        this.addSlot(new ExtraInventorySlot(owner, ExtraEquipmentData.SLOT_PANTS_UNDER, 78, 8));
        this.addSlot(new ExtraInventorySlot(owner, ExtraEquipmentData.SLOT_SHOES_UNDER, 78, 26));

        // 2. Слот Плаща (индекс 50) и слот Рюкзака (индекс 51)
        this.addSlot(new ExtraInventorySlot(owner, ExtraEquipmentData.SLOT_CAPE, 96, -28));
        this.addSlot(new ExtraInventorySlot(owner, ExtraEquipmentData.SLOT_BACKPACK, 96, -10));

        // 3. 9 слотов Дворфа в середине инвентаря (Y = 62, индексы меню 52..60)
        for (int i = 0; i < ExtraEquipmentData.DWARF_SLOT_COUNT; i++) {
            this.addSlot(new ExtraInventorySlot(
                    owner,
                    ExtraEquipmentData.DWARF_SLOT_START + i,
                    8 + i * 18,
                    62,
                    true,
                    false
            ));
        }

        // 4. СЛОТЫ РЮКЗАКА СЛЕВА ОТ ИНВЕНТАРЯ (схема image_d45fcb.jpg):
        // Индексы меню 61..93 (до 33 слотов: сетка по 3 столбца слева от основного окна: X = -64, -46, -28)
        for (int i = 0; i < ExtraEquipmentData.MAX_BACKPACK_SLOTS; i++) {
            int col = i % 3;
            int row = i / 3;
            int bx = -64 + col * 18;
            int by = -28 + row * 18;
            this.addSlot(new ExtraInventorySlot(
                    owner,
                    ExtraEquipmentData.BACKPACK_STORAGE_START + i,
                    bx,
                    by,
                    false,
                    true
            ));
        }
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void rpgcore$handleApparelArmorAndBackpackShiftClick(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index < 0 || index >= this.slots.size()) return;
        Slot clickedSlot = this.slots.get(index);
        if (clickedSlot == null || !clickedSlot.hasItem()) return;

        ItemStack stackInSlot = clickedSlot.getItem();

        // А. Клик Shift+ЛКМ по дополнительным слотам (индексы >= 46: Одежда 46..51, Дворф 52..60, Рюкзак 61..93)
        if (index >= 46) {
            ItemStack copy = stackInSlot.copy();

            // Если в слоте Дворфа или в слоте Рюкзака (index >= 52) лежит ОДЕЖДА или РЮКЗАК — по Shift+ЛКМ сразу НАДЕВАЕМ её!
            if (index >= 52 && stackInSlot.getItem() instanceof PeasantClothesItem clothes) {
                int targetSlot = 46 + clothes.getUnderArmorSlotIndex();
                if (targetSlot >= 46 && targetSlot < 52 && !this.slots.get(targetSlot).hasItem()) {
                    this.slots.get(targetSlot).set(stackInSlot.split(1));
                    clickedSlot.set(stackInSlot);
                    cir.setReturnValue(copy);
                    return;
                }
            }
            // Если в слоте Дворфа/Рюкзака лежит обычная БРОНЯ — сразу надеваем в слоты брони (5..8)
            else if (index >= 52 && stackInSlot.getItem() instanceof ArmorItem) {
                EquipmentSlot eqSlot = player.getEquipmentSlotForItem(stackInSlot);
                if (eqSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                    int armorMenuIdx = 8 - eqSlot.getIndex();
                    if (armorMenuIdx >= 5 && armorMenuIdx <= 8 && !this.slots.get(armorMenuIdx).hasItem()) {
                        this.slots.get(armorMenuIdx).set(stackInSlot.split(1));
                        clickedSlot.set(stackInSlot);
                        cir.setReturnValue(copy);
                        return;
                    }
                }
            }

            // Иначе переносим предмет в основной инвентарь/хотбар (9..45)
            if (!this.moveItemStackTo(stackInSlot, 9, 45, true)) {
                cir.setReturnValue(ItemStack.EMPTY);
                return;
            }
            clickedSlot.set(stackInSlot);
            cir.setReturnValue(copy);
            return;
        }

        // Б. Клик Shift+ЛКМ по ОДЕЖДЕ или РЮКЗАКУ в основном инвентаре или хотбаре (9..44):
        // ВСЕГДА надеваем в слот одежды/рюкзака (46..51) (в том числе за Дворфа!)
        if (stackInSlot.getItem() instanceof PeasantClothesItem clothes) {
            int targetMenuSlot = 46 + clothes.getUnderArmorSlotIndex();
            if (targetMenuSlot >= 46 && targetMenuSlot < 52) {
                Slot apparelSlot = this.slots.get(targetMenuSlot);
                if (!apparelSlot.hasItem() && apparelSlot.mayPlace(stackInSlot)) {
                    ItemStack copy = stackInSlot.copy();
                    apparelSlot.set(stackInSlot.split(1));
                    clickedSlot.setChanged();
                    cir.setReturnValue(copy);
                    return;
                }
            }
        }

        // В. Клик Shift+ЛКМ по БРОНЕ или ЩИТУ в инвентаре/хотбаре (9..44): надеваем в слоты брони (5..8) или щита (45)
        if (!(stackInSlot.getItem() instanceof PeasantClothesItem)) {
            EquipmentSlot eqSlot = player.getEquipmentSlotForItem(stackInSlot);
            if (eqSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                int armorMenuIdx = 8 - eqSlot.getIndex();
                if (armorMenuIdx >= 5 && armorMenuIdx <= 8) {
                    Slot armorSlot = this.slots.get(armorMenuIdx);
                    if (!armorSlot.hasItem() && armorSlot.mayPlace(stackInSlot)) {
                        ItemStack copy = stackInSlot.copy();
                        armorSlot.set(stackInSlot.split(1));
                        clickedSlot.setChanged();
                        cir.setReturnValue(copy);
                        return;
                    }
                }
            } else if (stackInSlot.getItem() instanceof ShieldItem && !this.slots.get(45).hasItem()) {
                ItemStack copy = stackInSlot.copy();
                this.slots.get(45).set(stackInSlot.split(1));
                clickedSlot.setChanged();
                cir.setReturnValue(copy);
                return;
            }
        }

        // Г. Перемещение обычных предметов между инвентарем (9..35), хотбаром (36..44), слотами Дворфа (52..60) и РЮКЗАКОМ (61..93):
        // Если в основном инвентаре закончились слоты — предмет по Shift+ЛКМ автоматически помещается в РЮКЗАК!
        if (index >= 9 && index < 45) {
            ItemStack copy = stackInSlot.copy();
            boolean isDwarf = player.getData(ModAttachments.APPEARANCE).race() == RaceType.DWARF;
            int bagSlots = player.getData(ModAttachments.EXTRA_EQUIPMENT).getActiveBackpackSlotCount();

            boolean moved = false;
            if (index < 36) {
                // Из основного инвентаря (9..35) -> сначала пробуем в хотбар (36..45), а если хотбар полон -> в Дворфа (52..61) или в РЮКЗАК (61..61+bagSlots)!
                moved = this.moveItemStackTo(stackInSlot, 36, 45, false);
                if (!moved && isDwarf) {
                    moved = this.moveItemStackTo(stackInSlot, 52, 61, false);
                }
                if (!moved && bagSlots > 0) {
                    moved = this.moveItemStackTo(stackInSlot, 61, 61 + bagSlots, false);
                }
            } else {
                // Из хотбара (36..44) -> сначала в основной инвентарь (9..36), если полон -> в слоты Дворфа (52..61), если и они полны -> в РЮКЗАК (61..61+bagSlots)!
                moved = this.moveItemStackTo(stackInSlot, 9, 36, false);
                if (!moved && isDwarf) {
                    moved = this.moveItemStackTo(stackInSlot, 52, 61, false);
                }
                if (!moved && bagSlots > 0) {
                    moved = this.moveItemStackTo(stackInSlot, 61, 61 + bagSlots, false);
                }
            }

            if (moved) {
                clickedSlot.setChanged();
                cir.setReturnValue(copy);
            } else {
                cir.setReturnValue(ItemStack.EMPTY);
            }
        }
    }

    @Unique
    private static void rpgcore$setSlotPos(Slot slot, int newX, int newY) {
        ((SlotAccessor) slot).rpgcore$setX(newX);
        ((SlotAccessor) slot).rpgcore$setY(newY);
    }
}