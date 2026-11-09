package com.rpgcore.network;

import com.rpgcore.RPGCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ToggleCrawlPayload(int entityId, boolean crawling) implements CustomPacketPayload {

    public ToggleCrawlPayload(boolean crawling) {
        this(-1, crawling);
    }

    public static final Type<ToggleCrawlPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RPGCore.MODID, "toggle_crawl"));

    public static final StreamCodec<ByteBuf, ToggleCrawlPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ToggleCrawlPayload::entityId,
            ByteBufCodecs.BOOL, ToggleCrawlPayload::crawling,
            ToggleCrawlPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}