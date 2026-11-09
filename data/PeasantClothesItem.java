package com.rpgcore.data;

import com.rpgcore.network.SyncExtraEquipmentPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PeasantClothesItem extends ArmorItem {

    public enum ApparelType {
        HAT(ExtraEquipmentData.SLOT_HAT_UNDER, EquipmentSlot.HEAD, "Головной убор (слот одежды)"),
        SHIRT(ExtraEquipmentData.SLOT_SHIRT_UNDER, EquipmentSlot.CHEST, "Рубаха (слот под нагрудником)"),
        PANTS(ExtraEquipmentData.SLOT_PANTS_UNDER, EquipmentSlot.LEGS, "Портки (слот под поножами)"),
        SHOES(ExtraEquipmentData.SLOT_SHOES_UNDER, EquipmentSlot.FEET, "Обувь (слот под ботинками)"),
        CAPE(ExtraEquipmentData.SLOT_CAPE, EquipmentSlot.CHEST, "Плащ (слот плаща)"),
        BACKPACK(ExtraEquipmentData.SLOT_BACKPACK, EquipmentSlot.CHEST, "Рюкзак (слот рюкзака)");

        private final int underArmorSlot;
        private final EquipmentSlot armorSlot;
        private final String slotDescription;

        ApparelType(int underArmorSlot, EquipmentSlot armorSlot, String slotDescription) {
            this.underArmorSlot = underArmorSlot;
            this.armorSlot = armorSlot;
            this.slotDescription = slotDescription;
        }

        public int getUnderArmorSlot() {
            return underArmorSlot;
        }

        public EquipmentSlot getArmorSlot() {
            return armorSlot;
        }

        public String getSlotDescription() {
            return slotDescription;
        }
    }

    private final ApparelType apparelType;

    public PeasantClothesItem(Holder<ArmorMaterial> material, Type type, ApparelType apparelType, Properties properties) {
        super(material, type, properties);
        this.apparelType = apparelType;
    }

    public ApparelType getApparelType() {
        return apparelType;
    }

    public int getUnderArmorSlotIndex() {
        return apparelType.getUnderArmorSlot();
    }

    /**
     * ИСПРАВЛЕНО (image_9a1d45.jpg): Запрещаем ванильному слою брони рисовать серую кожаную текстуру
     * на стойке для брони и на игроке! Вместо неё работает наш кастомный 3D-рендер одежды.
     */
    @Override
    public @Nullable ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        if (entity.level().isClientSide()) {
            return com.rpgcore.client.CustomSkinManager.getBlankArmorTexture();
        }
        return null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack heldStack = player.getItemInHand(hand);
        int targetExtraSlot = this.apparelType.getUnderArmorSlot();

        ExtraEquipmentData extraEq = player.getData(ModAttachments.EXTRA_EQUIPMENT);
        ItemStack currentlyEquipped = extraEq.getStack(targetExtraSlot);

        if (!level.isClientSide()) {
            extraEq.setStack(targetExtraSlot, heldStack.copyWithCount(1));
            if (currentlyEquipped.isEmpty()) {
                heldStack.shrink(1);
            } else {
                player.setItemInHand(hand, currentlyEquipped.copy());
            }

            level.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 1.0F, 1.0F);

            if (player instanceof ServerPlayer sp) {
                sp.inventoryMenu.broadcastChanges();
                SyncExtraEquipmentPayload sync = new SyncExtraEquipmentPayload(sp.getId(), extraEq);
                PacketDistributor.sendToPlayer(sp, sync);
                PacketDistributor.sendToPlayersTrackingEntity(sp, sync);
            }
        } else {
            extraEq.setStack(targetExtraSlot, heldStack.copyWithCount(1));
        }

        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal(apparelType.getSlotDescription()).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("ПКМ — надеть в слот снаряжения").withStyle(ChatFormatting.DARK_GRAY));
    }
}