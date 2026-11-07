package com.rpgcore.network;

import com.rpgcore.RPGCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ToggleHoodPayload(int entityId, boolean hoodUp) implements CustomPacketPayload {

    public static final Type<ToggleHoodPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RPGCore.MODID, "toggle_hood"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleHoodPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ToggleHoodPayload::entityId,
            ByteBufCodecs.BOOL, ToggleHoodPayload::hoodUp,
            ToggleHoodPayload::new
    );

    public ToggleHoodPayload(boolean hoodUp) {
        this(-1, hoodUp);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}