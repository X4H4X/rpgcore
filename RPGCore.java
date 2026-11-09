package com.rpgcore;

import com.rpgcore.data.ClanSavedData;
import com.rpgcore.data.ExtraEquipmentData;
import com.rpgcore.data.ModAttachments;
import com.rpgcore.data.ModItemsAndBlocks;
import com.rpgcore.data.PlayerAppearanceData;
import com.rpgcore.data.PlayerCorpseEntity;
import com.rpgcore.data.RPGCoreResourceGenerator;
import com.rpgcore.data.RaceType;
import com.rpgcore.data.TreeChopManager;
import com.rpgcore.network.CorpseWaypointPayload;
import com.rpgcore.network.ModNetworking;
import com.rpgcore.network.SyncAppearancePayload;
import com.rpgcore.network.SyncExtraEquipmentPayload;
import com.rpgcore.network.ToggleCrawlPayload;
import com.rpgcore.network.ToggleHoodPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod(RPGCore.MODID)
public class RPGCore {
    public static final String MODID = "rpgcore";

    private static final ResourceLocation RACE_HEALTH_ID = ResourceLocation.fromNamespaceAndPath(MODID, "orc_health_bonus");
    private static final ResourceLocation OLD_DWARF_ARMOR_ID = ResourceLocation.fromNamespaceAndPath(MODID, "dwarf_armor_bonus");
    private static final ResourceLocation OLD_RACE_ARMOR_ID = ResourceLocation.fromNamespaceAndPath(MODID, "race_armor");
    private static final ResourceLocation DRAGONBORN_ARMOR_ID = ResourceLocation.fromNamespaceAndPath(MODID, "dragonborn_scale_armor");
    private static final ResourceLocation RACE_STEP_HEIGHT_ID = ResourceLocation.fromNamespaceAndPath(MODID, "race_step_height");
    private static final ResourceLocation RACE_BLOCK_REACH_ID = ResourceLocation.fromNamespaceAndPath(MODID, "race_block_reach");
    private static final ResourceLocation RACE_ENTITY_REACH_ID = ResourceLocation.fromNamespaceAndPath(MODID, "race_entity_reach");
    private static final ResourceLocation RACE_SAFE_FALL_ID = ResourceLocation.fromNamespaceAndPath(MODID, "race_safe_fall");
    private static final ResourceLocation DWARF_MINING_SPEED_ID = ResourceLocation.fromNamespaceAndPath(MODID, "dwarf_mining_speed");
    private static final ResourceLocation ELF_MOVE_SPEED_ID = ResourceLocation.fromNamespaceAndPath(MODID, "elf_move_speed");

    private record DeathMarkerInfo(int x, int y, int z, String dimension) {}
    private static final Map<UUID, DeathMarkerInfo> LAST_DEATH_MARKERS = new ConcurrentHashMap<>();

    public RPGCore(IEventBus modEventBus) {
        RPGCoreResourceGenerator.ensureResourcesGenerated();
        ModAttachments.register(modEventBus);
        ModItemsAndBlocks.register(modEventBus);
        modEventBus.addListener(ModNetworking::registerPayloads);
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        TreeChopManager.onBlockBreak(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerDeathCreateCorpse(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;

        try {
            ExtraEquipmentData extraEq = player.getData(ModAttachments.EXTRA_EQUIPMENT);
            List<ItemStack> extraList = extraEq.toList();

            PlayerCorpseEntity corpse = new PlayerCorpseEntity(ModItemsAndBlocks.PLAYER_CORPSE.get(), player.level());
            double spawnY = Math.max(player.level().getMinBuildHeight() + 1.0D, player.getY());
            corpse.setPos(player.getX(), spawnY, player.getZ());
            corpse.initFromDeadPlayer(player);

            int invSize = Math.min(36, player.getInventory().items.size());
            for (int i = 0; i < invSize; i++) {
                ItemStack s = player.getInventory().items.get(i);
                if (!s.isEmpty()) {
                    corpse.getItems().set(i, s.copy());
                    player.getInventory().items.set(i, ItemStack.EMPTY);
                }
            }

            int armorSize = Math.min(4, player.getInventory().armor.size());
            for (int i = 0; i < armorSize; i++) {
                ItemStack s = player.getInventory().armor.get(i);
                if (!s.isEmpty()) {
                    corpse.getItems().set(36 + i, s.copy());
                    player.getInventory().armor.set(i, ItemStack.EMPTY);
                }
            }

            if (!player.getInventory().offhand.isEmpty()) {
                ItemStack off = player.getInventory().offhand.get(0);
                if (!off.isEmpty()) {
                    corpse.getItems().set(40, off.copy());
                    player.getInventory().offhand.set(0, ItemStack.EMPTY);
                }
            }

            int maxExtra = Math.min(extraList.size(), PlayerCorpseEntity.TOTAL_SLOTS - 41);
            for (int i = 0; i < maxExtra; i++) {
                ItemStack s = extraList.get(i);
                if (s != null && !s.isEmpty()) {
                    corpse.getItems().set(41 + i, s.copy());
                    extraEq.setStack(i, ItemStack.EMPTY);
                }
            }

            player.level().addFreshEntity(corpse);

            BlockPos deathPos = player.blockPosition();
            String dim = player.level().dimension().location().toString();
            DeathMarkerInfo marker = new DeathMarkerInfo(deathPos.getX(), deathPos.getY(), deathPos.getZ(), dim);
            LAST_DEATH_MARKERS.put(player.getUUID(), marker);

            PacketDistributor.sendToPlayer(
                    player,
                    new CorpseWaypointPayload(true, marker.x(), marker.y(), marker.z(), marker.dimension())
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && !player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            event.getDrops().clear();
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            PlayerAppearanceData data = sp.getData(ModAttachments.APPEARANCE);
            ExtraEquipmentData extraEq = sp.getData(ModAttachments.EXTRA_EQUIPMENT);
            applyRaceAttributesAndDimensions(sp, data);
            PacketDistributor.sendToPlayer(sp, new SyncAppearancePayload(sp.getId(), !data.isCreated(), data));
            PacketDistributor.sendToPlayer(sp, new SyncExtraEquipmentPayload(sp.getId(), extraEq));
            PacketDistributor.sendToPlayer(sp, new ToggleHoodPayload(sp.getId(), sp.getData(ModAttachments.HOOD_UP)));
            ClanSavedData.get(sp.server.overworld());
        }
    }

    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer tracker) {
            PacketDistributor.sendToPlayer(tracker, new SyncAppearancePayload(target.getId(), false, target.getData(ModAttachments.APPEARANCE)));
            PacketDistributor.sendToPlayer(tracker, new SyncExtraEquipmentPayload(target.getId(), target.getData(ModAttachments.EXTRA_EQUIPMENT)));
            PacketDistributor.sendToPlayer(tracker, new ToggleCrawlPayload(target.getId(), target.getData(ModAttachments.CRAWLING)));
            PacketDistributor.sendToPlayer(tracker, new ToggleHoodPayload(target.getId(), target.getData(ModAttachments.HOOD_UP)));
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            sp.setData(ModAttachments.CRAWLING, false);
            PlayerAppearanceData data = sp.getData(ModAttachments.APPEARANCE);
            applyRaceAttributesAndDimensions(sp, data);
            sp.setHealth(sp.getMaxHealth());
            PacketDistributor.sendToPlayer(sp, new SyncAppearancePayload(sp.getId(), false, data));
            PacketDistributor.sendToPlayer(sp, new SyncExtraEquipmentPayload(sp.getId(), sp.getData(ModAttachments.EXTRA_EQUIPMENT)));

            DeathMarkerInfo marker = LAST_DEATH_MARKERS.get(sp.getUUID());
            if (marker != null) {
                PacketDistributor.sendToPlayer(
                        sp,
                        new CorpseWaypointPayload(true, marker.x(), marker.y(), marker.z(), marker.dimension())
                );
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer sp && sp.tickCount % 30 == 0 && sp.getServer() != null) {
            ClanSavedData.get(sp.server.overworld());
        }
    }

    @SubscribeEvent
    public void onEntitySize(EntityEvent.Size event) {
        if (event.getEntity() instanceof Player player) {
            if (player.level().isClientSide() && com.rpgcore.client.ClientEvents.renderingInventoryDoll) {
                event.setNewSize(EntityDimensions.scalable(0.6F, 1.8F).withEyeHeight(1.62F));
                return;
            }

            PlayerAppearanceData data = player.getData(ModAttachments.APPEARANCE);
            float h = data.race().getBaseHeight() * data.gender().getHeightScale();
            float w = data.race().getBaseWidth() * data.gender().getWidthScale();

            if (event.getPose() == Pose.SWIMMING || player.getData(ModAttachments.CRAWLING)) {
                event.setNewSize(EntityDimensions.scalable(0.6F * w, 0.6F).withEyeHeight(0.4F));
            } else if (event.getPose() == Pose.CROUCHING) {
                float boxH = Math.min(2.45F, 1.5F * h);
                float boxW = Math.min(0.85F, 0.6F * w);
                float eyeH = (data.race() == RaceType.ORC) ? 1.85F : (1.27F * h);
                event.setNewSize(EntityDimensions.fixed(boxW, boxH).withEyeHeight(eyeH));
            } else {
                // ЖЕСТКИЙ РАСЧЕТ ОТ КОНСТАНТ (1.8F и 0.6F), чтобы размер НИКОГДА не умножался дважды до 5 блоков!
                // Для Орка: высота хитбокса ровно 2.82 блока (проходит под 3 блоками!), глаза на 2.35 блока.
                float boxH = (data.race() == RaceType.ORC) ? 2.82F : (1.8F * h);
                float boxW = (data.race() == RaceType.ORC) ? 0.82F : (0.6F * w);
                float eyeH = (data.race() == RaceType.ORC) ? 2.35F : (1.62F * h);

                event.setNewSize(EntityDimensions.fixed(boxW, boxH).withEyeHeight(eyeH));
            }
        }
    }

    public static void applyRaceAttributesAndDimensions(Player player, PlayerAppearanceData data) {
        if (!player.level().isClientSide()) {
            // КРИТИЧНО: Очищаем все возможные модификаторы Attributes.SCALE из NBT игрока,
            // из-за которых рост Орка умножался сам на себя и давал 5 блоков высоты!
            AttributeInstance scaleInst = player.getAttribute(Attributes.SCALE);
            if (scaleInst != null) {
                scaleInst.removeModifiers();
                scaleInst.setBaseValue(1.0D);
            }

            RaceType race = data.race();
            float h = race.getBaseHeight() * data.gender().getHeightScale();

            double healthBonus = race.getMaxHealth() - 20.0D;
            updateAttribute(player, Attributes.MAX_HEALTH, RACE_HEALTH_ID,
                    healthBonus, AttributeModifier.Operation.ADD_VALUE);

            updateAttribute(player, Attributes.ARMOR, OLD_DWARF_ARMOR_ID, 0.0D, AttributeModifier.Operation.ADD_VALUE);
            updateAttribute(player, Attributes.ARMOR, OLD_RACE_ARMOR_ID, 0.0D, AttributeModifier.Operation.ADD_VALUE);
            updateAttribute(player, Attributes.ARMOR, DRAGONBORN_ARMOR_ID,
                    race == RaceType.DRAGONBORN ? 4.0D : 0.0D, AttributeModifier.Operation.ADD_VALUE);

            updateAttribute(player, Attributes.STEP_HEIGHT, RACE_STEP_HEIGHT_ID,
                    h > 1.2F ? 0.4D : 0.0D, AttributeModifier.Operation.ADD_VALUE);

            updateAttribute(player, Attributes.BLOCK_INTERACTION_RANGE, RACE_BLOCK_REACH_ID,
                    h > 1.2F ? 1.0D : (h < 0.9F ? -0.25D : 0.0D), AttributeModifier.Operation.ADD_VALUE);

            updateAttribute(player, Attributes.ENTITY_INTERACTION_RANGE, RACE_ENTITY_REACH_ID,
                    h > 1.2F ? 0.8D : 0.0D, AttributeModifier.Operation.ADD_VALUE);

            updateAttribute(player, Attributes.SAFE_FALL_DISTANCE, RACE_SAFE_FALL_ID,
                    race == RaceType.ELF ? 2.0D : (h > 1.2F ? 1.5D : 0.0D), AttributeModifier.Operation.ADD_VALUE);

            updateAttribute(player, Attributes.BLOCK_BREAK_SPEED, DWARF_MINING_SPEED_ID,
                    race == RaceType.DWARF ? 0.25D : 0.0D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

            updateAttribute(player, Attributes.MOVEMENT_SPEED, ELF_MOVE_SPEED_ID,
                    race == RaceType.ELF ? 0.10D : 0.0D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }
        player.refreshDimensions();
    }

    private static void updateAttribute(Player player, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr,
                                        ResourceLocation id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(id);
        if (Math.abs(amount) > 0.0001D) {
            inst.addPermanentModifier(new AttributeModifier(id, amount, op));
        }
    }
}