package com.rpgcore.network;

import com.rpgcore.RPGCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record CreativeSetExtraSlotPayload(int extraSlotIndex, ItemStack stack) implements CustomPacketPayload {

    public static final Type<CreativeSetExtraSlotPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RPGCore.MODID, "creative_set_extra_slot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CreativeSetExtraSlotPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CreativeSetExtraSlotPayload::extraSlotIndex,
            ItemStack.OPTIONAL_STREAM_CODEC, CreativeSetExtraSlotPayload::stack,
            CreativeSetExtraSlotPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}