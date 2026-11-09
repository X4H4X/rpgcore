package com.rpgcore.network;

import com.rpgcore.RPGCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record SetExtraSlotPayload(int extraSlotIndex, ItemStack stack) implements CustomPacketPayload {

    public static final Type<SetExtraSlotPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RPGCore.MODID, "set_extra_slot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetExtraSlotPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SetExtraSlotPayload::extraSlotIndex,
            ItemStack.OPTIONAL_STREAM_CODEC, SetExtraSlotPayload::stack,
            SetExtraSlotPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}