package com.rpgcore.network;

import com.rpgcore.RPGCore;
import com.rpgcore.data.PlayerAppearanceData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SaveAppearancePayload(PlayerAppearanceData data) implements CustomPacketPayload {

    public static final Type<SaveAppearancePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RPGCore.MODID, "save_appearance"));

    public static final StreamCodec<ByteBuf, SaveAppearancePayload> STREAM_CODEC = StreamCodec.composite(
            PlayerAppearanceData.STREAM_CODEC, SaveAppearancePayload::data,
            SaveAppearancePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}