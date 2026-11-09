package com.rpgcore.data;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClanData {

    public enum ClanRank {
        FOUNDER("Основатель", "Главный"),
        RIGHT_HAND("Правая рука", "Помощник"),
        NOVICE("Послушник", "Начальный ранг");

        private final String tag;
        private final String roleDescription;

        ClanRank(String tag, String roleDescription) {
            this.tag = tag;
            this.roleDescription = roleDescription;
        }

        public String getTag() { return tag; }
        public String getRoleDescription() { return roleDescription; }
    }

    public record MemberEntry(UUID uuid, String rpName, ClanRank rank) {
        public static final StreamCodec<ByteBuf, MemberEntry> STREAM_CODEC = StreamCodec.of(
                (buf, val) -> {
                    ByteBufCodecs.STRING_UTF8.encode(buf, val.uuid.toString());
                    ByteBufCodecs.STRING_UTF8.encode(buf, val.rpName);
                    ByteBufCodecs.STRING_UTF8.encode(buf, val.rank.name());
                },
                buf -> new MemberEntry(
                        UUID.fromString(ByteBufCodecs.STRING_UTF8.decode(buf)),
                        ByteBufCodecs.STRING_UTF8.decode(buf),
                        ClanRank.valueOf(ByteBufCodecs.STRING_UTF8.decode(buf))
                )
        );
    }

    public record PendingApproval(UUID targetUuid, String targetName, String invitedByName) {
        public static final StreamCodec<ByteBuf, PendingApproval> STREAM_CODEC = StreamCodec.of(
                (buf, val) -> {
                    ByteBufCodecs.STRING_UTF8.encode(buf, val.targetUuid.toString());
                    ByteBufCodecs.STRING_UTF8.encode(buf, val.targetName);
                    ByteBufCodecs.STRING_UTF8.encode(buf, val.invitedByName);
                },
                buf -> new PendingApproval(
                        UUID.fromString(ByteBufCodecs.STRING_UTF8.decode(buf)),
                        ByteBufCodecs.STRING_UTF8.decode(buf),
                        ByteBufCodecs.STRING_UTF8.decode(buf)
                )
        );
    }

    public record DirectInvite(String clanId, String clanName, String clanTag, int colorHex, String inviterName) {
        public static final StreamCodec<ByteBuf, DirectInvite> STREAM_CODEC = StreamCodec.of(
                (buf, val) -> {
                    ByteBufCodecs.STRING_UTF8.encode(buf, val.clanId);
                    ByteBufCodecs.STRING_UTF8.encode(buf, val.clanName);
                    ByteBufCodecs.STRING_UTF8.encode(buf, val.clanTag);
                    ByteBufCodecs.VAR_INT.encode(buf, val.colorHex);
                    ByteBufCodecs.STRING_UTF8.encode(buf, val.inviterName);
                },
                buf -> new DirectInvite(
                        ByteBufCodecs.STRING_UTF8.decode(buf),
                        ByteBufCodecs.STRING_UTF8.decode(buf),
                        ByteBufCodecs.STRING_UTF8.decode(buf),
                        ByteBufCodecs.VAR_INT.decode(buf),
                        ByteBufCodecs.STRING_UTF8.decode(buf)
                )
        );
    }

    public record AllyCompassPos(UUID uuid, String rpName, double x, double z, int colorHex) {
        public static final StreamCodec<ByteBuf, AllyCompassPos> STREAM_CODEC = StreamCodec.of(
                (buf, val) -> {
                    ByteBufCodecs.STRING_UTF8.encode(buf, val.uuid.toString());
                    ByteBufCodecs.STRING_UTF8.encode(buf, val.rpName);
                    buf.writeDouble(val.x);
                    buf.writeDouble(val.z);
                    ByteBufCodecs.VAR_INT.encode(buf, val.colorHex);
                },
                buf -> new AllyCompassPos(
                        UUID.fromString(ByteBufCodecs.STRING_UTF8.decode(buf)),
                        ByteBufCodecs.STRING_UTF8.decode(buf),
                        buf.readDouble(),
                        buf.readDouble(),
                        ByteBufCodecs.VAR_INT.decode(buf)
                )
        );
    }

    public static final int[] PALETTE_COLORS = {
            0xFFD4AF37, // Золотой
            0xFFC62828, // Алый
            0xFF2E7D32, // Изумрудный
            0xFF1565C0, // Сапфировый
            0xFF6A1B9A, // Аметистовый
            0xFF00838F, // Бирюзовый
            0xFFEF6C00, // Янтарный
            0xFFAD1457, // Рубиновый
            0xFF558B2F, // Лесной
            0xFF4527A0, // Индиго
            0xFF8D6E63, // Бронзовый
            0xFFCFD8DC  // Серебряный
    };

    private String clanId;
    private String clanName;
    private String clanTag; // Обязательный тег из 3-4 букв
    private int colorHex;
    private boolean friendlyFire;
    private final List<MemberEntry> members;
    private final List<PendingApproval> pendingApprovals;

    public ClanData(String clanId, String clanName, String clanTag, int colorHex, boolean friendlyFire,
                    List<MemberEntry> members, List<PendingApproval> pendingApprovals) {
        this.clanId = clanId;
        this.clanName = clanName;
        this.clanTag = clanTag;
        this.colorHex = colorHex;
        this.friendlyFire = friendlyFire;
        this.members = new ArrayList<>(members);
        this.pendingApprovals = new ArrayList<>(pendingApprovals);
    }

    public String getClanId() { return clanId; }
    public String getClanName() { return clanName; }
    public String getClanTag() { return clanTag; }
    public int getColorHex() { return colorHex; }
    public boolean isFriendlyFire() { return friendlyFire; }
    public void setFriendlyFire(boolean friendlyFire) { this.friendlyFire = friendlyFire; }
    public List<MemberEntry> getMembers() { return members; }
    public List<PendingApproval> getPendingApprovals() { return pendingApprovals; }

    public ClanRank getRankOf(UUID uuid) {
        for (MemberEntry m : members) {
            if (m.uuid().equals(uuid)) return m.rank();
        }
        return null;
    }

    public int countRightHands() {
        int count = 0;
        for (MemberEntry m : members) {
            if (m.rank() == ClanRank.RIGHT_HAND) count++;
        }
        return count;
    }

    public static final StreamCodec<ByteBuf, ClanData> STREAM_CODEC = StreamCodec.of(
            (buf, val) -> {
                ByteBufCodecs.STRING_UTF8.encode(buf, val.clanId);
                ByteBufCodecs.STRING_UTF8.encode(buf, val.clanName);
                ByteBufCodecs.STRING_UTF8.encode(buf, val.clanTag);
                ByteBufCodecs.VAR_INT.encode(buf, val.colorHex);
                ByteBufCodecs.BOOL.encode(buf, val.friendlyFire);
                ByteBufCodecs.VAR_INT.encode(buf, val.members.size());
                for (MemberEntry m : val.members) MemberEntry.STREAM_CODEC.encode(buf, m);
                ByteBufCodecs.VAR_INT.encode(buf, val.pendingApprovals.size());
                for (PendingApproval p : val.pendingApprovals) PendingApproval.STREAM_CODEC.encode(buf, p);
            },
            buf -> {
                String id = ByteBufCodecs.STRING_UTF8.decode(buf);
                String name = ByteBufCodecs.STRING_UTF8.decode(buf);
                String tag = ByteBufCodecs.STRING_UTF8.decode(buf);
                int col = ByteBufCodecs.VAR_INT.decode(buf);
                boolean ff = ByteBufCodecs.BOOL.decode(buf);
                int mSize = ByteBufCodecs.VAR_INT.decode(buf);
                List<MemberEntry> members = new ArrayList<>(mSize);
                for (int i = 0; i < mSize; i++) members.add(MemberEntry.STREAM_CODEC.decode(buf));
                int pSize = ByteBufCodecs.VAR_INT.decode(buf);
                List<PendingApproval> pending = new ArrayList<>(pSize);
                for (int i = 0; i < pSize; i++) pending.add(PendingApproval.STREAM_CODEC.decode(buf));
                return new ClanData(id, name, tag, col, ff, members, pending);
            }
    );
}