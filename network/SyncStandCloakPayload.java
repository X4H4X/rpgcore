package com.rpgcore.network;

import com.rpgcore.RPGCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncStandCloakPayload(int standEntityId, boolean hasCloak) implements CustomPacketPayload {

    public static final Type<SyncStandCloakPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RPGCore.MODID, "sync_stand_cloak"));

    public static final StreamCodec<ByteBuf, SyncStandCloakPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncStandCloakPayload::standEntityId,
            ByteBufCodecs.BOOL, SyncStandCloakPayload::hasCloak,
            SyncStandCloakPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}