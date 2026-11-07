package com.rpgcore.network;

import com.rpgcore.RPGCore;
import com.rpgcore.data.ExtraEquipmentData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncExtraEquipmentPayload(int entityId, ExtraEquipmentData equipment) implements CustomPacketPayload {

    public static final Type<SyncExtraEquipmentPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RPGCore.MODID, "sync_extra_eq"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncExtraEquipmentPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncExtraEquipmentPayload::entityId,
            ExtraEquipmentData.STREAM_CODEC, SyncExtraEquipmentPayload::equipment,
            SyncExtraEquipmentPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}