package com.rpgcore.network;

import com.rpgcore.RPGCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record GiveUpPayload() implements CustomPacketPayload {

    public static final Type<GiveUpPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RPGCore.MODID, "give_up"));

    public static final StreamCodec<ByteBuf, GiveUpPayload> STREAM_CODEC = StreamCodec.unit(new GiveUpPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}