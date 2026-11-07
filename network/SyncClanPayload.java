package com.rpgcore.network;

import com.rpgcore.RPGCore;
import com.rpgcore.data.ClanData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record SyncClanPayload(
        Optional<ClanData> myClan,
        List<ClanData.DirectInvite> myInvites,
        List<OnlineCandidate> onlineCandidates,
        List<ClanData.AllyCompassPos> alliesOnCompass
) implements CustomPacketPayload {

    public record OnlineCandidate(UUID uuid, String rpName) {
        public static final StreamCodec<ByteBuf, OnlineCandidate> STREAM_CODEC = StreamCodec.of(
                (buf, val) -> {
                    ByteBufCodecs.STRING_UTF8.encode(buf, val.uuid.toString());
                    ByteBufCodecs.STRING_UTF8.encode(buf, val.rpName);
                },
                buf -> new OnlineCandidate(
                        UUID.fromString(ByteBufCodecs.STRING_UTF8.decode(buf)),
                        ByteBufCodecs.STRING_UTF8.decode(buf)
                )
        );
    }

    public static final Type<SyncClanPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RPGCore.MODID, "sync_clan"));

    public static final StreamCodec<ByteBuf, SyncClanPayload> STREAM_CODEC = StreamCodec.of(
            (buf, val) -> {
                ByteBufCodecs.BOOL.encode(buf, val.myClan.isPresent());
                val.myClan.ifPresent(c -> ClanData.STREAM_CODEC.encode(buf, c));

                ByteBufCodecs.VAR_INT.encode(buf, val.myInvites.size());
                for (var inv : val.myInvites) ClanData.DirectInvite.STREAM_CODEC.encode(buf, inv);

                ByteBufCodecs.VAR_INT.encode(buf, val.onlineCandidates.size());
                for (var cand : val.onlineCandidates) OnlineCandidate.STREAM_CODEC.encode(buf, cand);

                ByteBufCodecs.VAR_INT.encode(buf, val.alliesOnCompass.size());
                for (var ally : val.alliesOnCompass) ClanData.AllyCompassPos.STREAM_CODEC.encode(buf, ally);
            },
            buf -> {
                boolean hasClan = ByteBufCodecs.BOOL.decode(buf);
                Optional<ClanData> clan = hasClan ? Optional.of(ClanData.STREAM_CODEC.decode(buf)) : Optional.empty();

                int invCount = ByteBufCodecs.VAR_INT.decode(buf);
                List<ClanData.DirectInvite> invites = new ArrayList<>(invCount);
                for (int i = 0; i < invCount; i++) invites.add(ClanData.DirectInvite.STREAM_CODEC.decode(buf));

                int candCount = ByteBufCodecs.VAR_INT.decode(buf);
                List<OnlineCandidate> cands = new ArrayList<>(candCount);
                for (int i = 0; i < candCount; i++) cands.add(OnlineCandidate.STREAM_CODEC.decode(buf));

                int allyCount = ByteBufCodecs.VAR_INT.decode(buf);
                List<ClanData.AllyCompassPos> allies = new ArrayList<>(allyCount);
                for (int i = 0; i < allyCount; i++) allies.add(ClanData.AllyCompassPos.STREAM_CODEC.decode(buf));

                return new SyncClanPayload(clan, invites, cands, allies);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}