package com.rpgcore.data;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class HoodedCloakItem extends PeasantClothesItem {

    public HoodedCloakItem(Properties properties) {
        super(ArmorMaterials.LEATHER, ArmorItem.Type.CHESTPLATE, ApparelType.CAPE, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("Нажмите [O], чтобы надеть/снять капюшон").withStyle(ChatFormatting.GOLD));
        tooltipComponents.add(Component.literal("Слот: Плащ (поверх брони и одежды)").withStyle(ChatFormatting.GRAY));
    }
}