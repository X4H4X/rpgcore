package com.rpgcore.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClanSavedData extends SavedData {

    private static final String DATA_NAME = "rpgcore_clans_v1";
    private final Map<String, ClanData> clans = new HashMap<>();
    private final Map<UUID, List<ClanData.DirectInvite>> invitesByTarget = new HashMap<>();

    public static ClanSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new Factory<>(ClanSavedData::new, ClanSavedData::load),
                DATA_NAME
        );
    }

    public ClanData getPlayerClan(UUID playerUuid) {
        for (ClanData c : clans.values()) {
            if (c.getRankOf(playerUuid) != null) return c;
        }
        return null;
    }

    public ClanData getClanById(String id) {
        return clans.get(id);
    }

    public Collection<ClanData> getAllClans() {
        return clans.values();
    }

    public List<ClanData.DirectInvite> getInvitesFor(UUID targetUuid) {
        return invitesByTarget.computeIfAbsent(targetUuid, k -> new ArrayList<>());
    }

    public void addDirectInvite(UUID targetUuid, ClanData.DirectInvite invite) {
        List<ClanData.DirectInvite> list = getInvitesFor(targetUuid);
        list.removeIf(i -> i.clanId().equals(invite.clanId()));
        list.add(invite);
        setDirty();
    }

    public ClanData createClan(UUID founderUuid, String founderName, String clanName, String clanTag, int colorHex) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        List<ClanData.MemberEntry> members = new ArrayList<>();
        members.add(new ClanData.MemberEntry(founderUuid, founderName, ClanData.ClanRank.FOUNDER));
        ClanData clan = new ClanData(id, clanName, clanTag.toUpperCase(), colorHex, false, members, new ArrayList<>());
        clans.put(id, clan);
        invitesByTarget.remove(founderUuid);
        setDirty();
        return clan;
    }

    public void removeClan(String clanId) {
        clans.remove(clanId);
        for (List<ClanData.DirectInvite> list : invitesByTarget.values()) {
            list.removeIf(i -> i.clanId().equals(clanId));
        }
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (ClanData c : clans.values()) {
            CompoundTag ct = new CompoundTag();
            ct.putString("id", c.getClanId());
            ct.putString("name", c.getClanName());
            ct.putString("tag", c.getClanTag());
            ct.putInt("color", c.getColorHex());
            ct.putBoolean("ff", c.isFriendlyFire());

            ListTag ml = new ListTag();
            for (ClanData.MemberEntry m : c.getMembers()) {
                CompoundTag mt = new CompoundTag();
                mt.putUUID("uuid", m.uuid());
                mt.putString("name", m.rpName());
                mt.putString("rank", m.rank().name());
                ml.add(mt);
            }
            ct.put("members", ml);

            ListTag pl = new ListTag();
            for (ClanData.PendingApproval p : c.getPendingApprovals()) {
                CompoundTag pt = new CompoundTag();
                pt.putUUID("uuid", p.targetUuid());
                pt.putString("name", p.targetName());
                pt.putString("by", p.invitedByName());
                pl.add(pt);
            }
            ct.put("pending", pl);
            list.add(ct);
        }
        tag.put("clans", list);
        return tag;
    }

    public static ClanSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        ClanSavedData data = new ClanSavedData();
        ListTag list = tag.getList("clans", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag ct = list.getCompound(i);
            String id = ct.getString("id");
            String name = ct.getString("name");
            String cTag = ct.contains("tag") ? ct.getString("tag") : name.substring(0, Math.min(3, name.length())).toUpperCase();
            int col = ct.getInt("color");
            boolean ff = ct.getBoolean("ff");

            List<ClanData.MemberEntry> members = new ArrayList<>();
            ListTag ml = ct.getList("members", Tag.TAG_COMPOUND);
            for (int j = 0; j < ml.size(); j++) {
                CompoundTag mt = ml.getCompound(j);
                members.add(new ClanData.MemberEntry(
                        mt.getUUID("uuid"),
                        mt.getString("name"),
                        ClanData.ClanRank.valueOf(mt.getString("rank"))
                ));
            }

            List<ClanData.PendingApproval> pending = new ArrayList<>();
            ListTag pl = ct.getList("pending", Tag.TAG_COMPOUND);
            for (int j = 0; j < pl.size(); j++) {
                CompoundTag pt = pl.getCompound(j);
                pending.add(new ClanData.PendingApproval(
                        pt.getUUID("uuid"),
                        pt.getString("name"),
                        pt.getString("by")
                ));
            }

            data.clans.put(id, new ClanData(id, name, cTag, col, ff, members, pending));
        }
        return data;
    }
}