package com.rpgcore.network;

import com.rpgcore.RPGCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SlotActionPayload(int action, int slotIndex) implements CustomPacketPayload {

    public static final int ACTION_SELECT_OR_CREATE = 0;
    public static final int ACTION_DELETE_SLOT = 1;
    public static final int ACTION_REQUEST_OPEN_MENU = 2;
    public static final int ACTION_PREVIEW_BIOME = 3;

    public static final Type<SlotActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RPGCore.MODID, "slot_action"));

    public static final StreamCodec<ByteBuf, SlotActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SlotActionPayload::action,
            ByteBufCodecs.VAR_INT, SlotActionPayload::slotIndex,
            SlotActionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}