package com.rpgcore.data;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class CharacterSlotsData {

    public static final int MAX_SLOTS = 3;

    public static class SlotEntry {
        public boolean created = false;
        public PlayerAppearanceData appearance = PlayerAppearanceData.DEFAULT;
        public ExtraEquipmentData extraEquipment = new ExtraEquipmentData();
        public NonNullList<ItemStack> visualGear = NonNullList.withSize(6, ItemStack.EMPTY);
        public long currency = 0L;
        public String skillsSummary = "Нет изученных веток";
        public CompoundTag serverStateTag = new CompoundTag();

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("Created", created);
            PlayerAppearanceData.CODEC.encodeStart(NbtOps.INSTANCE, appearance)
                    .result()
                    .ifPresent(t -> tag.put("Appearance", t));
            ExtraEquipmentData.CODEC.encodeStart(NbtOps.INSTANCE, extraEquipment)
                    .result()
                    .ifPresent(t -> tag.put("ExtraEqPreview", t));

            ListTag gearList = new ListTag();
            for (int i = 0; i < 6; i++) {
                CompoundTag itemTag = new CompoundTag();
                ItemStack st = visualGear.get(i);
                if (!st.isEmpty()) {
                    ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, st)
                            .result()
                            .ifPresent(t -> itemTag.put("Stack", t));
                }
                gearList.add(itemTag);
            }
            tag.put("VisualGear", gearList);

            tag.putLong("Currency", currency);
            tag.putString("SkillsSummary", skillsSummary);
            tag.put("ServerState", serverStateTag.copy());
            return tag;
        }

        public static SlotEntry fromTag(CompoundTag tag) {
            SlotEntry entry = new SlotEntry();
            entry.created = tag.getBoolean("Created");
            if (tag.contains("Appearance")) {
                PlayerAppearanceData.CODEC.parse(NbtOps.INSTANCE, tag.get("Appearance"))
                        .result()
                        .ifPresent(app -> entry.appearance = app);
            }
            if (tag.contains("ExtraEqPreview")) {
                ExtraEquipmentData.CODEC.parse(NbtOps.INSTANCE, tag.get("ExtraEqPreview"))
                        .result()
                        .ifPresent(eq -> entry.extraEquipment = eq);
            }
            if (tag.contains("VisualGear", Tag.TAG_LIST)) {
                ListTag gearList = tag.getList("VisualGear", Tag.TAG_COMPOUND);
                for (int i = 0; i < Math.min(6, gearList.size()); i++) {
                    CompoundTag itemTag = gearList.getCompound(i);
                    if (itemTag.contains("Stack")) {
                        final int idx = i;
                        ItemStack.CODEC.parse(NbtOps.INSTANCE, itemTag.get("Stack"))
                                .result()
                                .ifPresent(st -> entry.visualGear.set(idx, st));
                    }
                }
            }
            entry.currency = tag.getLong("Currency");
            entry.skillsSummary = tag.contains("SkillsSummary") ? tag.getString("SkillsSummary") : "Нет изученных веток";
            if (tag.contains("ServerState", Tag.TAG_COMPOUND)) {
                entry.serverStateTag = tag.getCompound("ServerState").copy();
            }
            return entry;
        }
    }

    public record ClientSlotSummary(
            int slotIndex,
            boolean created,
            PlayerAppearanceData appearance,
            ExtraEquipmentData extraEquipment,
            List<ItemStack> visualGear,
            long currency,
            String skillsSummary
    ) {
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientSlotSummary> STREAM_CODEC = StreamCodec.of(
                (buf, val) -> {
                    buf.writeVarInt(val.slotIndex);
                    buf.writeBoolean(val.created);
                    PlayerAppearanceData.STREAM_CODEC.encode(buf, val.appearance);
                    ExtraEquipmentData.STREAM_CODEC.encode(buf, val.extraEquipment);
                    for (int i = 0; i < 6; i++) {
                        ItemStack st = (i < val.visualGear.size()) ? val.visualGear.get(i) : ItemStack.EMPTY;
                        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, st);
                    }
                    buf.writeVarLong(val.currency);
                    buf.writeUtf(val.skillsSummary);
                },
                buf -> {
                    int idx = buf.readVarInt();
                    boolean cr = buf.readBoolean();
                    PlayerAppearanceData app = PlayerAppearanceData.STREAM_CODEC.decode(buf);
                    ExtraEquipmentData eq = ExtraEquipmentData.STREAM_CODEC.decode(buf);
                    List<ItemStack> gear = new ArrayList<>(6);
                    for (int i = 0; i < 6; i++) {
                        gear.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
                    }
                    long cur = buf.readVarLong();
                    String skills = buf.readUtf();
                    return new ClientSlotSummary(idx, cr, app, eq, gear, cur, skills);
                }
        );
    }

    private int activeSlot = -1;
    private final SlotEntry[] slots = new SlotEntry[]{
            new SlotEntry(), new SlotEntry(), new SlotEntry()
    };

    public CharacterSlotsData() {
    }

    public int getActiveSlot() {
        return activeSlot;
    }

    public void setActiveSlot(int activeSlot) {
        this.activeSlot = activeSlot;
    }

    public SlotEntry getSlot(int index) {
        if (index < 0 || index >= MAX_SLOTS) return slots[0];
        return slots[index];
    }

    public void deleteSlot(int index) {
        if (index >= 0 && index < MAX_SLOTS) {
            slots[index] = new SlotEntry();
            if (activeSlot == index) {
                activeSlot = -1;
            }
        }
    }

    public List<ClientSlotSummary> toClientSummaries() {
        List<ClientSlotSummary> list = new ArrayList<>(MAX_SLOTS);
        for (int i = 0; i < MAX_SLOTS; i++) {
            SlotEntry s = slots[i];
            list.add(new ClientSlotSummary(
                    i, s.created, s.appearance, s.extraEquipment,
                    new ArrayList<>(s.visualGear), s.currency, s.skillsSummary
            ));
        }
        return list;
    }

    public void saveCurrentPlayerToActiveSlot(ServerPlayer player) {
        if (activeSlot < 0 || activeSlot >= MAX_SLOTS) return;
        SlotEntry slot = slots[activeSlot];
        PlayerAppearanceData currentApp = player.getData(ModAttachments.APPEARANCE);
        if (!currentApp.isCreated()) return;

        slot.created = true;
        slot.appearance = currentApp;
        slot.skillsSummary = querySkillsSummaryFromTreeMod(player);

        ExtraEquipmentData currentExtra = player.getData(ModAttachments.EXTRA_EQUIPMENT);
        ExtraEquipmentData copiedExtra = new ExtraEquipmentData();
        for (int i = 0; i < ExtraEquipmentData.TOTAL_SLOTS; i++) {
            copiedExtra.setStack(i, currentExtra.getStack(i).copy());
        }
        slot.extraEquipment = copiedExtra;

        slot.visualGear.set(0, player.getItemBySlot(EquipmentSlot.HEAD).copy());
        slot.visualGear.set(1, player.getItemBySlot(EquipmentSlot.CHEST).copy());
        slot.visualGear.set(2, player.getItemBySlot(EquipmentSlot.LEGS).copy());
        slot.visualGear.set(3, player.getItemBySlot(EquipmentSlot.FEET).copy());
        slot.visualGear.set(4, player.getItemBySlot(EquipmentSlot.MAINHAND).copy());
        slot.visualGear.set(5, player.getItemBySlot(EquipmentSlot.OFFHAND).copy());

        CompoundTag state = new CompoundTag();
        ListTag invList = new ListTag();
        player.getInventory().save(invList);
        state.put("Inventory", invList);

        ExtraEquipmentData.CODEC.encodeStart(NbtOps.INSTANCE, currentExtra)
                .result()
                .ifPresent(t -> state.put("ExtraEq", t));

        state.putFloat("Health", player.getHealth());
        state.putInt("FoodLevel", player.getFoodData().getFoodLevel());
        state.putFloat("FoodSat", player.getFoodData().getSaturationLevel());
        state.putInt("XpLevel", player.experienceLevel);
        state.putFloat("XpProg", player.experienceProgress);
        state.putInt("XpTotal", player.totalExperience);

        state.putDouble("PosX", player.getX());
        state.putDouble("PosY", player.getY());
        state.putDouble("PosZ", player.getZ());
        state.putFloat("Yaw", player.getYRot());
        state.putFloat("Pitch", player.getXRot());

        saveExternalSkillsTreeToTag(player, state);
        slot.serverStateTag = state;
    }

    public void loadSlotIntoPlayer(ServerPlayer player, int targetSlot) {
        if (targetSlot < 0 || targetSlot >= MAX_SLOTS) return;
        if (this.activeSlot >= 0 && this.activeSlot < MAX_SLOTS && this.activeSlot != targetSlot) {
            saveCurrentPlayerToActiveSlot(player);
        }

        this.activeSlot = targetSlot;
        SlotEntry slot = slots[targetSlot];

        if (!slot.created || !slot.appearance.isCreated()) {
            // ИСПРАВЛЕНО (Пункт 5): При создании НОВОГО персонажа очищаем старые данные
            // и телепортируем его на начальную точку спавна мира (чтобы он НЕ появлялся там, где вышел прошлый перс!)
            player.stopRiding();
            player.getInventory().clearContent();
            player.setData(ModAttachments.EXTRA_EQUIPMENT, new ExtraEquipmentData());
            player.setData(ModAttachments.APPEARANCE, PlayerAppearanceData.DEFAULT);
            player.setData(ModAttachments.DOWNED_TICKS, 0);
            player.experienceLevel = 0;
            player.experienceProgress = 0.0F;
            player.totalExperience = 0;
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(5.0F);
            clearExternalSkillsTree(player);

            ServerLevel overworld = player.server.overworld();
            BlockPos worldSpawn = overworld.getSharedSpawnPos();
            overworld.getChunk(worldSpawn.getX() >> 4, worldSpawn.getZ() >> 4);
            BlockPos topPos = overworld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, worldSpawn);
            int safeY = Math.max(overworld.getMinBuildHeight() + 1, topPos.getY());

            player.setDeltaMovement(Vec3.ZERO);
            player.fallDistance = 0.0F;
            player.teleportTo(overworld, topPos.getX() + 0.5D, safeY, topPos.getZ() + 0.5D, 0.0F, 0.0F);
            return;
        }

        player.setData(ModAttachments.APPEARANCE, slot.appearance);
        CompoundTag state = slot.serverStateTag;

        if (!state.isEmpty()) {
            if (state.contains("Inventory", Tag.TAG_LIST)) {
                player.getInventory().load(state.getList("Inventory", Tag.TAG_COMPOUND));
            }
            if (state.contains("ExtraEq")) {
                ExtraEquipmentData.CODEC.parse(NbtOps.INSTANCE, state.get("ExtraEq"))
                        .result()
                        .ifPresent(eq -> player.setData(ModAttachments.EXTRA_EQUIPMENT, eq));
            }
            player.experienceLevel = state.getInt("XpLevel");
            player.experienceProgress = state.getFloat("XpProg");
            player.totalExperience = state.getInt("XpTotal");

            if (state.contains("FoodLevel")) {
                player.getFoodData().setFoodLevel(state.getInt("FoodLevel"));
                player.getFoodData().setSaturation(state.getFloat("FoodSat"));
            }
            if (state.contains("PosX")) {
                player.stopRiding();
                player.teleportTo(player.server.overworld(), state.getDouble("PosX"), state.getDouble("PosY"), state.getDouble("PosZ"), state.getFloat("Yaw"), state.getFloat("Pitch"));
            }
            loadExternalSkillsTreeFromTag(player, state);
        }

        slot.skillsSummary = querySkillsSummaryFromTreeMod(player);
    }

    public static String querySkillsSummaryFromTreeMod(ServerPlayer player) {
        try {
            Class<?> attClass = Class.forName("com.rpgskillstree.data.SkillsAttachments");
            Object supplier = attClass.getField("PLAYER_SKILLS").get(null);
            Object attType = supplier.getClass().getMethod("get").invoke(supplier);
            Object skillsData = null;
            for (Method m : player.getClass().getMethods()) {
                if (m.getName().equals("getData") && m.getParameterCount() == 1) {
                    skillsData = m.invoke(player, attType);
                    break;
                }
            }
            if (skillsData == null) return "Нет изученных веток";

            Class<?> branchEnumClass = Class.forName("com.rpgskillstree.data.SkillBranch");
            Object[] branches = branchEnumClass.getEnumConstants();
            Method countMethod = skillsData.getClass().getMethod("getUnlockedCountInBranch", branchEnumClass);

            StringBuilder sb = new StringBuilder();
            for (Object b : branches) {
                int count = (int) countMethod.invoke(skillsData, b);
                if (count > 0) {
                    String title;
                    try {
                        title = (String) branchEnumClass.getMethod("getTitle", boolean.class).invoke(b, true);
                    } catch (NoSuchMethodException e) {
                        title = (String) branchEnumClass.getMethod("getTitle").invoke(b);
                    }
                    if (!sb.isEmpty()) sb.append(", ");
                    sb.append(title).append(" (").append(count).append(")");
                }
            }
            return sb.isEmpty() ? "Нет изученных веток" : sb.toString();
        } catch (Throwable ignored) {
            return "Нет изученных веток";
        }
    }

    private static void saveExternalSkillsTreeToTag(ServerPlayer player, CompoundTag state) {
        try {
            Class<?> attClass = Class.forName("com.rpgskillstree.data.SkillsAttachments");
            Object supplier = attClass.getField("PLAYER_SKILLS").get(null);
            Object attType = supplier.getClass().getMethod("get").invoke(supplier);
            Object skillsData = null;
            for (Method m : player.getClass().getMethods()) {
                if (m.getName().equals("getData") && m.getParameterCount() == 1) {
                    skillsData = m.invoke(player, attType);
                    break;
                }
            }
            if (skillsData != null) {
                Class<?> dataClass = Class.forName("com.rpgskillstree.data.PlayerSkillsData");
                @SuppressWarnings("unchecked")
                Codec<Object> codec = (Codec<Object>) dataClass.getField("CODEC").get(null);
                codec.encodeStart(NbtOps.INSTANCE, skillsData).result().ifPresent(t -> state.put("SkillsTree", t));
            }
        } catch (Throwable ignored) {
        }
    }

    private static void loadExternalSkillsTreeFromTag(ServerPlayer player, CompoundTag state) {
        if (!state.contains("SkillsTree")) {
            clearExternalSkillsTree(player);
            return;
        }
        try {
            Class<?> attClass = Class.forName("com.rpgskillstree.data.SkillsAttachments");
            Object supplier = attClass.getField("PLAYER_SKILLS").get(null);
            Object attType = supplier.getClass().getMethod("get").invoke(supplier);
            Class<?> dataClass = Class.forName("com.rpgskillstree.data.PlayerSkillsData");
            @SuppressWarnings("unchecked")
            Codec<Object> codec = (Codec<Object>) dataClass.getField("CODEC").get(null);
            codec.parse(NbtOps.INSTANCE, state.get("SkillsTree")).result().ifPresent(decoded -> {
                try {
                    for (Method m : player.getClass().getMethods()) {
                        if (m.getName().equals("setData") && m.getParameterCount() == 2) {
                            m.invoke(player, attType, decoded);
                            break;
                        }
                    }
                } catch (Throwable ignored) {
                }
            });
        } catch (Throwable ignored) {
        }
    }

    private static void clearExternalSkillsTree(ServerPlayer player) {
        try {
            Class<?> attClass = Class.forName("com.rpgskillstree.data.SkillsAttachments");
            Object supplier = attClass.getField("PLAYER_SKILLS").get(null);
            Object attType = supplier.getClass().getMethod("get").invoke(supplier);
            Class<?> dataClass = Class.forName("com.rpgskillstree.data.PlayerSkillsData");
            Object fresh = dataClass.getDeclaredConstructor().newInstance();
            for (Method m : player.getClass().getMethods()) {
                if (m.getName().equals("setData") && m.getParameterCount() == 2) {
                    m.invoke(player, attType, fresh);
                    break;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public CompoundTag saveToCompound() {
        CompoundTag root = new CompoundTag();
        root.putInt("ActiveSlot", activeSlot);
        ListTag list = new ListTag();
        for (int i = 0; i < MAX_SLOTS; i++) {
            list.add(slots[i].toTag());
        }
        root.put("Slots", list);
        return root;
    }

    public static CharacterSlotsData loadFromCompound(CompoundTag root) {
        CharacterSlotsData data = new CharacterSlotsData();
        data.activeSlot = root.contains("ActiveSlot") ? root.getInt("ActiveSlot") : -1;
        if (root.contains("Slots", Tag.TAG_LIST)) {
            ListTag list = root.getList("Slots", Tag.TAG_COMPOUND);
            for (int i = 0; i < Math.min(MAX_SLOTS, list.size()); i++) {
                data.slots[i] = SlotEntry.fromTag(list.getCompound(i));
            }
        }
        return data;
    }

    public static final Codec<CharacterSlotsData> CODEC = CompoundTag.CODEC.xmap(
            CharacterSlotsData::loadFromCompound,
            CharacterSlotsData::saveToCompound
    );
}