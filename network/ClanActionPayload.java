package com.rpgcore.network;

import com.rpgcore.RPGCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ClanActionPayload(int action, String strArg, int intArg) implements CustomPacketPayload {

    public static final int ACTION_CREATE = 0;
    public static final int ACTION_INVITE_PLAYER = 1;
    public static final int ACTION_ACCEPT_INVITE = 2;
    public static final int ACTION_DECLINE_INVITE = 3;
    public static final int ACTION_APPROVE_PENDING = 4;
    public static final int ACTION_REJECT_PENDING = 5;
    public static final int ACTION_TOGGLE_RIGHT_HAND = 6;
    public static final int ACTION_KICK_OR_LEAVE = 7;
    public static final int ACTION_TOGGLE_FF = 8;

    public static final Type<ClanActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RPGCore.MODID, "clan_action"));

    public static final StreamCodec<ByteBuf, ClanActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ClanActionPayload::action,
            ByteBufCodecs.STRING_UTF8, ClanActionPayload::strArg,
            ByteBufCodecs.VAR_INT, ClanActionPayload::intArg,
            ClanActionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}