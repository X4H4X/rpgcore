package com.rpgcore.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ExtraEquipmentData {

    public static final int SLOT_HAT_UNDER = 0;
    public static final int SLOT_SHIRT_UNDER = 1;
    public static final int SLOT_PANTS_UNDER = 2;
    public static final int SLOT_SHOES_UNDER = 3;
    public static final int SLOT_CAPE = 4;
    public static final int SLOT_BACKPACK = 5;

    // 9 слотов Дворфа в середине инвентаря (индексы 6..14)
    public static final int DWARF_SLOT_START = 6;
    public static final int DWARF_SLOT_COUNT = 9;

    // До 33 слотов внутри надетого рюкзака слева от инвентаря (индексы 15..47)
    public static final int BACKPACK_STORAGE_START = 15;
    public static final int MAX_BACKPACK_SLOTS = 33;

    public static final int TOTAL_SLOTS = 48;

    // Клиентский флаг: развернута ли панель рюкзака слева от инвентаря (кнопка свернуть/развернуть)
    public static boolean clientBackpackExpanded = true;

    private final NonNullList<ItemStack> stacks = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);

    public ExtraEquipmentData() {
    }

    public ExtraEquipmentData(List<ItemStack> list) {
        for (int i = 0; i < Math.min(TOTAL_SLOTS, list.size()); i++) {
            stacks.set(i, list.get(i).copy());
        }
    }

    public ItemStack getStack(int slot) {
        if (slot < 0 || slot >= TOTAL_SLOTS) return ItemStack.EMPTY;
        return stacks.get(slot);
    }

    public void setStack(int slot, ItemStack stack) {
        if (slot >= 0 && slot < TOTAL_SLOTS) {
            stacks.set(slot, stack);
        }
    }

    public int getActiveBackpackSlotCount() {
        ItemStack bag = getStack(SLOT_BACKPACK);
        if (!bag.isEmpty() && bag.getItem() instanceof BackpackItem bp) {
            return bp.getSlotCount();
        }
        return 0;
    }

    public List<ItemStack> toList() {
        List<ItemStack> list = new ArrayList<>(TOTAL_SLOTS);
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            list.add(stacks.get(i).copy());
        }
        return list;
    }

    public static final Codec<ExtraEquipmentData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ItemStack.OPTIONAL_CODEC.listOf().fieldOf("items").forGetter(ExtraEquipmentData::toList)
    ).apply(inst, ExtraEquipmentData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ExtraEquipmentData> STREAM_CODEC = StreamCodec.of(
            (buf, val) -> {
                for (int i = 0; i < TOTAL_SLOTS; i++) {
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, val.getStack(i));
                }
            },
            buf -> {
                ExtraEquipmentData data = new ExtraEquipmentData();
                for (int i = 0; i < TOTAL_SLOTS; i++) {
                    data.setStack(i, ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
                }
                return data;
            }
    );
}