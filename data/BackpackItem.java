package com.rpgcore.data;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class BackpackItem extends PeasantClothesItem {

    public enum Tier {
        LEATHER(0, 9, "Кожаный", 0xFF8C6239, 0xFF593A22),
        IRON(1, 15, "Железный", 0xFFD8D8D8, 0xFF7A7E85),
        GOLD(2, 21, "Золотой", 0xFFF5C842, 0xFFA87E1E),
        DIAMOND(3, 27, "Алмазный", 0xFF4AEDD9, 0xFF228C82),
        NETHERITE(4, 33, "Незеритовый", 0xFF5A5459, 0xFF332D32);

        private final int level;
        private final int slotCount;
        private final String title;
        private final int trimLight;
        private final int trimDark;

        Tier(int level, int slotCount, String title, int trimLight, int trimDark) {
            this.level = level;
            this.slotCount = slotCount;
            this.title = title;
            this.trimLight = trimLight;
            this.trimDark = trimDark;
        }

        public int getLevel() { return level; }
        public int getSlotCount() { return slotCount; }
        public String getTitle() { return title; }
        public int getTrimLight() { return trimLight; }
        public int getTrimDark() { return trimDark; }
    }

    private final Tier backpackTier;

    public BackpackItem(Tier backpackTier, Properties properties) {
        super(ArmorMaterials.LEATHER, ArmorItem.Type.CHESTPLATE, ApparelType.BACKPACK, properties);
        this.backpackTier = backpackTier;
    }

    public Tier getBackpackTier() {
        return backpackTier;
    }

    public int getSlotCount() {
        return backpackTier.getSlotCount();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("Вместимость: " + backpackTier.getSlotCount() + " слотов").withStyle(ChatFormatting.GOLD));
        tooltipComponents.add(Component.literal("Слот: Рюкзак").withStyle(ChatFormatting.GRAY));
    }
}