package com.rpgcore.network;

import com.rpgcore.RPGCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CorpseWaypointPayload(boolean add, int x, int y, int z, String dimension) implements CustomPacketPayload {

    public static final Type<CorpseWaypointPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RPGCore.MODID, "corpse_waypoint"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CorpseWaypointPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, CorpseWaypointPayload::add,
            ByteBufCodecs.VAR_INT, CorpseWaypointPayload::x,
            ByteBufCodecs.VAR_INT, CorpseWaypointPayload::y,
            ByteBufCodecs.VAR_INT, CorpseWaypointPayload::z,
            ByteBufCodecs.STRING_UTF8, CorpseWaypointPayload::dimension,
            CorpseWaypointPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}