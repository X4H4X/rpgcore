package com.rpgcore.data;

import com.rpgcore.network.CorpseWaypointPayload;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PlayerCorpseEntity extends Entity {

    public static final int TOTAL_SLOTS = 71;

    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(PlayerCorpseEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> OWNER_NAME =
            SynchedEntityData.defineId(PlayerCorpseEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<CompoundTag> APPEARANCE_TAG =
            SynchedEntityData.defineId(PlayerCorpseEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<CompoundTag> VISUAL_EQUIP_TAG =
            SynchedEntityData.defineId(PlayerCorpseEntity.class, EntityDataSerializers.COMPOUND_TAG);

    private final NonNullList<ItemStack> items = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
    private boolean hadItemsInitially = false;

    public PlayerCorpseEntity(EntityType<? extends PlayerCorpseEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = false;
        this.setInvulnerable(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER_UUID, Optional.empty());
        builder.define(OWNER_NAME, "Путник");
        builder.define(APPEARANCE_TAG, new CompoundTag());
        builder.define(VISUAL_EQUIP_TAG, new CompoundTag());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (APPEARANCE_TAG.equals(key)) {
            this.setBoundingBox(this.makeBoundingBox());
        }
    }

    /**
     * Переопределяем setPos, чтобы ванильный Entity.setPos() не сбрасывал наш точный прямоугольный хитбокс
     * обратно в маленький квадратик у ног!
     */
    @Override
    public void setPos(double x, double y, double z) {
        this.setPosRaw(x, y, z);
        this.setBoundingBox(this.makeBoundingBox());
    }

    /**
     * Строит компактный прямоугольный хитбокс РОВНО по контуру лежащей 3D-модели трупа
     * (в PlayerCorpseRenderer модель повернута на 180 - yRot и наклонена на -90 градусов вдоль оси взгляда).
     */
    @Override
    protected AABB makeBoundingBox() {
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();

        if (this.entityData == null) {
            return new AABB(x - 0.35D, y, z - 0.35D, x + 0.35D, y + 0.45D, z + 0.35D);
        }

        PlayerAppearanceData app = getAppearanceData();
        float hScale = app.race().getBaseHeight() * app.gender().getHeightScale();
        float wScale = app.race().getBaseWidth() * app.gender().getWidthScale();

        // Длина модели от стоп до макушки головы
        double bodyLen = 1.65D * hScale;
        // Полуширина плеч/туловища (компактно по модели, чтобы нельзя было кликать по пустой траве вдали!)
        double halfShoulder = 0.28D * wScale;
        double height = 0.42D * wScale;

        float rad = this.getYRot() * Mth.DEG_TO_RAD;
        double dirX = -Mth.sin(rad);
        double dirZ = Mth.cos(rad);

        // В рендере точка (x, z) находится у ног/голеней, а голова лежит в направлении (-dirX, -dirZ) либо (+dirX, +dirZ)
        // Покрываем точно ось лежащего тела с минимальным отступом по ширине плеч
        double endX1 = x + dirX * bodyLen * 0.85D;
        double endZ1 = z + dirZ * bodyLen * 0.85D;
        double endX2 = x - dirX * bodyLen * 0.85D;
        double endZ2 = z - dirZ * bodyLen * 0.85D;

        double minX = Math.min(endX1, endX2) - halfShoulder;
        double maxX = Math.max(endX1, endX2) + halfShoulder;
        double minZ = Math.min(endZ1, endZ2) - halfShoulder;
        double maxZ = Math.max(endZ1, endZ2) + halfShoulder;

        return new AABB(minX, y, minZ, maxX, y + height, maxZ);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(0.6F, 0.42F);
    }

    @Override
    public float getPickRadius() {
        return 0.05F;
    }

    public void initFromDeadPlayer(Player player) {
        this.entityData.set(OWNER_UUID, Optional.of(player.getUUID()));
        PlayerAppearanceData app = player.getData(ModAttachments.APPEARANCE);
        String rpgName = (app.firstName() + " " + app.lastName()).trim();
        if (rpgName.isEmpty()) {
            rpgName = player.getGameProfile().getName();
        }
        this.entityData.set(OWNER_NAME, rpgName);

        PlayerAppearanceData.CODEC.encodeStart(NbtOps.INSTANCE, app)
                .result()
                .ifPresent(tag -> {
                    if (tag instanceof CompoundTag ct) {
                        this.entityData.set(APPEARANCE_TAG, ct);
                    }
                });

        this.setYRot(player.getYRot());
        this.setBoundingBox(this.makeBoundingBox());
    }

    public void syncVisualEquipmentToClients() {
        if (this.level().isClientSide()) return;
        CompoundTag equipTag = new CompoundTag();
        ListTag list = new ListTag();
        for (int i = 36; i <= 46; i++) {
            ItemStack s = this.items.get(i);
            if (!s.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                list.add(s.save(this.registryAccess(), itemTag));
            }
        }
        equipTag.put("Equip", list);
        this.entityData.set(VISUAL_EQUIP_TAG, equipTag);
    }

    public ItemStack getSyncedCorpseSlot(int slotIndex) {
        if (!this.level().isClientSide()) {
            return (slotIndex >= 0 && slotIndex < TOTAL_SLOTS) ? this.items.get(slotIndex) : ItemStack.EMPTY;
        }
        CompoundTag equipTag = this.entityData.get(VISUAL_EQUIP_TAG);
        if (equipTag == null || !equipTag.contains("Equip", Tag.TAG_LIST)) return ItemStack.EMPTY;
        ListTag list = equipTag.getList("Equip", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag itemTag = list.getCompound(i);
            if (itemTag.getInt("Slot") == slotIndex) {
                return ItemStack.parse(this.registryAccess(), itemTag).orElse(ItemStack.EMPTY);
            }
        }
        return ItemStack.EMPTY;
    }

    public ItemStack getCorpseArmor(EquipmentSlot slot) {
        return switch (slot) {
            case FEET -> getSyncedCorpseSlot(36);
            case LEGS -> getSyncedCorpseSlot(37);
            case CHEST -> getSyncedCorpseSlot(38);
            case HEAD -> getSyncedCorpseSlot(39);
            default -> ItemStack.EMPTY;
        };
    }

    public ItemStack getCorpseExtraSlot(int extraSlot) {
        return getSyncedCorpseSlot(41 + extraSlot);
    }

    public PlayerAppearanceData getAppearanceData() {
        CompoundTag ct = this.entityData.get(APPEARANCE_TAG);
        if (ct != null && !ct.isEmpty()) {
            return PlayerAppearanceData.CODEC.parse(NbtOps.INSTANCE, ct)
                    .result()
                    .orElse(PlayerAppearanceData.DEFAULT);
        }
        return PlayerAppearanceData.DEFAULT;
    }

    public String getOwnerName() {
        return this.entityData.get(OWNER_NAME);
    }

    public Optional<UUID> getOwnerUUID() {
        return this.entityData.get(OWNER_UUID);
    }

    public NonNullList<ItemStack> getItems() {
        return this.items;
    }

    public boolean isEmptyCorpse() {
        for (ItemStack s : this.items) {
            if (!s.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount == 1 || this.tickCount == 5) {
            this.setBoundingBox(this.makeBoundingBox());
        }
        if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.04D, 0.0D));
        }
        if (this.isInWater() || this.isInLava()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.8D, 0.5D, 0.8D));
        } else {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.8D, 0.98D, 0.8D));
        }

        if (!this.onGround()) {
            this.move(MoverType.SELF, this.getDeltaMovement());
        } else {
            this.setDeltaMovement(Vec3.ZERO);
        }

        if (this.getY() < this.level().getMinBuildHeight() + 1) {
            this.setPos(this.getX(), this.level().getMinBuildHeight() + 1, this.getZ());
            this.setDeltaMovement(Vec3.ZERO);
        }

        if (!this.level().isClientSide()) {
            if (this.tickCount == 1 || this.tickCount % 10 == 0) {
                syncVisualEquipmentToClients();
            }
            if (!isEmptyCorpse()) {
                hadItemsInitially = true;
            } else if (hadItemsInitially && this.tickCount % 5 == 0) {
                notifyOwnerCorpseCleared();
                this.discard();
            }
        }
    }

    public void notifyOwnerCorpseCleared() {
        if (this.level().isClientSide()) return;
        getOwnerUUID().ifPresent(uuid -> {
            Player owner = this.level().getServer() != null ? this.level().getServer().getPlayerList().getPlayer(uuid) : null;
            if (owner instanceof ServerPlayer sp) {
                PacketDistributor.sendToPlayer(sp, new CorpseWaypointPayload(false, 0, 0, 0, ""));
            }
        });
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.getData(ModAttachments.DOWNED_TICKS) > 0) {
            return InteractionResult.FAIL;
        }
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        syncVisualEquipmentToClients();
        if (player instanceof ServerPlayer sp) {
            sp.openMenu(new SimpleMenuProvider(
                    (containerId, playerInv, p) -> new CorpseMenu(containerId, playerInv, this),
                    Component.literal("Останки: " + getOwnerName())
            ), buf -> buf.writeVarInt(this.getId()));
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult interactAt(Player player, Vec3 vec, InteractionHand hand) {
        return interact(player, hand);
    }

    public void quickRestoreToOwner(ServerPlayer player) {
        ExtraEquipmentData extraEq = player.getData(ModAttachments.EXTRA_EQUIPMENT);
        List<ItemStack> currentExtraList = extraEq.toList();
        int maxExtraSlots = Math.min(currentExtraList.size(), TOTAL_SLOTS - 41);

        for (int i = 0; i < maxExtraSlots; i++) {
            ItemStack corpseItem = this.items.get(41 + i);
            if (!corpseItem.isEmpty()) {
                ItemStack current = extraEq.getStack(i);
                if (current.isEmpty()) {
                    extraEq.setStack(i, corpseItem.copy());
                    this.items.set(41 + i, ItemStack.EMPTY);
                } else if (player.getInventory().add(corpseItem.copy())) {
                    this.items.set(41 + i, ItemStack.EMPTY);
                }
            }
        }

        EquipmentSlot[] armorSlots = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
        for (int i = 0; i < 4; i++) {
            ItemStack armorStack = this.items.get(36 + i);
            if (!armorStack.isEmpty()) {
                EquipmentSlot eqSlot = armorSlots[i];
                if (player.getItemBySlot(eqSlot).isEmpty()) {
                    player.setItemSlot(eqSlot, armorStack.copy());
                    this.items.set(36 + i, ItemStack.EMPTY);
                } else if (player.getInventory().add(armorStack.copy())) {
                    this.items.set(36 + i, ItemStack.EMPTY);
                }
            }
        }

        ItemStack offhand = this.items.get(40);
        if (!offhand.isEmpty()) {
            if (player.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty()) {
                player.setItemSlot(EquipmentSlot.OFFHAND, offhand.copy());
                this.items.set(40, ItemStack.EMPTY);
            } else if (player.getInventory().add(offhand.copy())) {
                this.items.set(40, ItemStack.EMPTY);
            }
        }

        for (int i = 0; i < 36; i++) {
            ItemStack invStack = this.items.get(i);
            if (!invStack.isEmpty()) {
                if (player.getInventory().items.get(i).isEmpty()) {
                    player.getInventory().items.set(i, invStack.copy());
                    this.items.set(i, ItemStack.EMPTY);
                } else if (player.getInventory().add(invStack.copy())) {
                    this.items.set(i, ItemStack.EMPTY);
                }
            }
        }

        syncVisualEquipmentToClients();
        player.level().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS, 1.0F, 1.0F);

        if (isEmptyCorpse()) {
            notifyOwnerCorpseCleared();
            player.closeContainer();
            this.discard();
        }
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        player.inventoryMenu.sendAllDataToRemote();

        com.rpgcore.network.SyncExtraEquipmentPayload sync = new com.rpgcore.network.SyncExtraEquipmentPayload(player.getId(), extraEq);
        PacketDistributor.sendToPlayer(player, sync);
        PacketDistributor.sendToPlayersTrackingEntity(player, sync);
    }

    @Override public boolean fireImmune() { return true; }
    @Override public boolean ignoreExplosion(Explosion explosion) { return true; }
    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public boolean isPickable() { return true; }
    @Override public boolean canBeCollidedWith() { return false; }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("OwnerUUID")) {
            this.entityData.set(OWNER_UUID, Optional.of(tag.getUUID("OwnerUUID")));
        }
        this.entityData.set(OWNER_NAME, tag.getString("OwnerName"));
        if (tag.contains("AppearanceTag", Tag.TAG_COMPOUND)) {
            this.entityData.set(APPEARANCE_TAG, tag.getCompound("AppearanceTag"));
        }

        for (int i = 0; i < TOTAL_SLOTS; i++) {
            this.items.set(i, ItemStack.EMPTY);
        }
        ListTag list = tag.getList("CorpseItems", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag itemTag = list.getCompound(i);
            int slot = itemTag.getInt("Slot");
            if (slot >= 0 && slot < TOTAL_SLOTS) {
                ItemStack.parse(this.registryAccess(), itemTag).ifPresent(s -> this.items.set(slot, s));
            }
        }
        this.hadItemsInitially = !isEmptyCorpse();
        syncVisualEquipmentToClients();
        this.setBoundingBox(this.makeBoundingBox());
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        getOwnerUUID().ifPresent(uuid -> tag.putUUID("OwnerUUID", uuid));
        tag.putString("OwnerName", getOwnerName());
        tag.put("AppearanceTag", this.entityData.get(APPEARANCE_TAG));

        ListTag list = new ListTag();
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            ItemStack s = this.items.get(i);
            if (!s.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                list.add(s.save(this.registryAccess(), itemTag));
            }
        }
        tag.put("CorpseItems", list);
    }
}