package com.rpgcore.network;

import com.rpgcore.RPGCore;
import com.rpgcore.data.PlayerAppearanceData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncAppearancePayload(int entityId, boolean forceOpenCreator, PlayerAppearanceData data) implements CustomPacketPayload {

    public static final Type<SyncAppearancePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RPGCore.MODID, "sync_appearance"));

    public static final StreamCodec<ByteBuf, SyncAppearancePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncAppearancePayload::entityId,
            ByteBufCodecs.BOOL, SyncAppearancePayload::forceOpenCreator,
            PlayerAppearanceData.STREAM_CODEC, SyncAppearancePayload::data,
            SyncAppearancePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}