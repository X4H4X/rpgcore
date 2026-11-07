package com.rpgcore.network;

import com.rpgcore.RPGCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncDownedPayload(int entityId, int downedTicks) implements CustomPacketPayload {

    public static final Type<SyncDownedPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RPGCore.MODID, "sync_downed"));

    public static final StreamCodec<ByteBuf, SyncDownedPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncDownedPayload::entityId,
            ByteBufCodecs.VAR_INT, SyncDownedPayload::downedTicks,
            SyncDownedPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}