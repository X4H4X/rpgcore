package com.rpgcore.network;

import com.rpgcore.RPGCore;
import com.rpgcore.data.CharacterSlotsData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record SyncSlotsMenuPayload(
        boolean openMenuScreen,
        int activeSlot,
        List<CharacterSlotsData.ClientSlotSummary> slots
) implements CustomPacketPayload {

    public static final Type<SyncSlotsMenuPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RPGCore.MODID, "sync_slots_menu"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSlotsMenuPayload> STREAM_CODEC = StreamCodec.of(
            (buf, val) -> {
                buf.writeBoolean(val.openMenuScreen);
                buf.writeVarInt(val.activeSlot);
                buf.writeVarInt(val.slots.size());
                for (var s : val.slots) {
                    CharacterSlotsData.ClientSlotSummary.STREAM_CODEC.encode(buf, s);
                }
            },
            buf -> {
                boolean open = buf.readBoolean();
                int active = buf.readVarInt();
                int count = buf.readVarInt();
                List<CharacterSlotsData.ClientSlotSummary> list = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    list.add(CharacterSlotsData.ClientSlotSummary.STREAM_CODEC.decode(buf));
                }
                return new SyncSlotsMenuPayload(open, active, list);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}