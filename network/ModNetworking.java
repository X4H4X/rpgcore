package com.rpgcore.network;

import com.mojang.brigadier.Command;
import com.rpgcore.RPGCore;
import com.rpgcore.client.CharacterSlotSelectionScreen;
import com.rpgcore.client.SkyrimThemeRenderer;
import com.rpgcore.data.BackpackItem;
import com.rpgcore.data.CharacterSlotsData;
import com.rpgcore.data.ClanData;
import com.rpgcore.data.ClanSavedData;
import com.rpgcore.data.ExtraEquipmentData;
import com.rpgcore.data.HoodedCloakItem;
import com.rpgcore.data.ModAttachments;
import com.rpgcore.data.ModItemsAndBlocks;
import com.rpgcore.data.PlayerAppearanceData;
import com.rpgcore.data.RaceType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = RPGCore.MODID)
public class ModNetworking {

    public static Runnable clientOpenCreatorHook = () -> {};
    public static Consumer<SyncClanPayload> clientClanSyncHook = p -> {};
    public static Consumer<SyncSlotsMenuPayload> clientSlotsMenuHook = p -> {};

    private static final ResourceLocation HEALTH_MOD_ID = ResourceLocation.fromNamespaceAndPath(RPGCore.MODID, "race_max_health");
    private static final ResourceLocation SPEED_MOD_ID = ResourceLocation.fromNamespaceAndPath(RPGCore.MODID, "race_move_speed");
    private static final ResourceLocation ATTACK_MOD_ID = ResourceLocation.fromNamespaceAndPath(RPGCore.MODID, "race_attack_damage");
    private static final ResourceLocation ARMOR_MOD_ID = ResourceLocation.fromNamespaceAndPath(RPGCore.MODID, "race_natural_armor");
    private static final ResourceLocation KNOCKBACK_MOD_ID = ResourceLocation.fromNamespaceAndPath(RPGCore.MODID, "race_knockback_res");
    private static final ResourceLocation DOWNED_SLOW_MOD_ID = ResourceLocation.fromNamespaceAndPath(RPGCore.MODID, "downed_crawl_slow");

    // 60 секунд = 1200 тиков
    public static final int MAX_DOWNED_TICKS = 1200;
    // 5 секунд удержания ПКМ = 100 тиков
    public static final int REVIVE_HOLD_TICKS = 100;

    public static void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(RPGCore.MODID).versioned("1.0.0");

        registrar.playToClient(
                SyncAppearancePayload.TYPE,
                SyncAppearancePayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.level == null) return;
                    Entity entity = mc.level.getEntity(payload.entityId());
                    if (entity instanceof Player player) {
                        player.setData(ModAttachments.APPEARANCE, payload.data());
                        player.refreshDimensions();
                    }
                    if (payload.forceOpenCreator() && mc.player != null && payload.entityId() == mc.player.getId()) {
                        CharacterSlotSelectionScreen.pendingOpenOnLoad = false;
                        clientOpenCreatorHook.run();
                    } else if (mc.player != null && payload.entityId() == mc.player.getId() && payload.data().isCreated()) {
                        if (mc.screen instanceof CharacterSlotSelectionScreen) {
                            mc.setScreen(null);
                        }
                    }
                })
        );

        registrar.playToClient(
                CorpseWaypointPayload.TYPE,
                CorpseWaypointPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (payload.add()) {
                        com.rpgcore.client.WorldMapManager.setDeathCorpseWaypoint(payload.x(), payload.y(), payload.z(), payload.dimension());
                    } else {
                        com.rpgcore.client.WorldMapManager.removeDeathCorpseWaypoint();
                    }
                })
        );

        registrar.playToClient(
                SyncExtraEquipmentPayload.TYPE,
                SyncExtraEquipmentPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.level == null) return;
                    Entity entity = mc.level.getEntity(payload.entityId());
                    if (entity instanceof Player player) {
                        player.setData(ModAttachments.EXTRA_EQUIPMENT, payload.equipment());
                    }
                })
        );

        registrar.playToClient(
                SyncClanPayload.TYPE,
                SyncClanPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> clientClanSyncHook.accept(payload))
        );

        registrar.playToServer(
                SaveAppearancePayload.TYPE,
                SaveAppearancePayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer sp) {
                        PlayerAppearanceData prevData = sp.getData(ModAttachments.APPEARANCE);
                        boolean isFirstCreation = !prevData.isCreated();

                        PlayerAppearanceData d = payload.data();
                        if (d.firstName().trim().isEmpty() || d.lastName().trim().isEmpty()) {
                            return;
                        }
                        PlayerAppearanceData saved = new PlayerAppearanceData(
                                true,
                                d.firstName().trim(),
                                d.lastName().trim(),
                                d.race(),
                                d.gender(),
                                d.skinColorIndex(),
                                d.faceVariant(),
                                d.eyeColorIndex(),
                                d.hairStyle(),
                                d.hairColorIndex(),
                                d.beardStyle()
                        );
                        sp.setData(ModAttachments.APPEARANCE, saved);

                        if (saved.race() != RaceType.DWARF) {
                            ExtraEquipmentData eq = sp.getData(ModAttachments.EXTRA_EQUIPMENT);
                            boolean changed = false;
                            for (int i = 0; i < ExtraEquipmentData.DWARF_SLOT_COUNT; i++) {
                                int idx = ExtraEquipmentData.DWARF_SLOT_START + i;
                                ItemStack st = eq.getStack(idx);
                                if (!st.isEmpty()) {
                                    sp.getInventory().placeItemBackInInventory(st.copy());
                                    eq.setStack(idx, ItemStack.EMPTY);
                                    changed = true;
                                }
                            }
                            if (changed) broadcastExtraEquipment(sp);
                        }

                        CharacterSlotsData slotsData = sp.getData(ModAttachments.CHARACTER_SLOTS);
                        if (slotsData.getActiveSlot() < 0) {
                            slotsData.setActiveSlot(0);
                        }
                        slotsData.saveCurrentPlayerToActiveSlot(sp);
                        PacketDistributor.sendToPlayer(sp, new SyncSlotsMenuPayload(false, slotsData.getActiveSlot(), slotsData.toClientSummaries()));

                        applyStatsAndSize(sp, true);
                        sp.refreshDisplayName();
                        sp.refreshTabListName();
                        broadcastAppearance(sp, false);
                        syncAllClans(sp.server);

                        if (isFirstCreation) {
                            sp.sendSystemMessage(Component.literal("§6Вы выбрали расу §e" + saved.race().getTitle() + "§6, добро пожаловать!"));
                        }
                    }
                })
        );

        registrar.playToServer(
                ClanActionPayload.TYPE,
                ClanActionPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer sp) {
                        handleClanAction(sp, payload);
                    }
                })
        );

        registrar.playToServer(
                SetExtraSlotPayload.TYPE,
                SetExtraSlotPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer sp) {
                        int idx = payload.extraSlotIndex();
                        if (idx >= 0 && idx < ExtraEquipmentData.TOTAL_SLOTS) {
                            sp.getData(ModAttachments.EXTRA_EQUIPMENT).setStack(idx, payload.stack().copy());
                            broadcastExtraEquipment(sp);
                        }
                    }
                })
        );

        registrar.playBidirectional(
                ToggleCrawlPayload.TYPE,
                ToggleCrawlPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.flow().isServerbound()) {
                        if (ctx.player() instanceof ServerPlayer sp) {
                            if (sp.getData(ModAttachments.DOWNED_TICKS) > 0) return;
                            sp.setData(ModAttachments.CRAWLING, payload.crawling());
                            sp.setPose(payload.crawling() ? Pose.SWIMMING : Pose.STANDING);
                            sp.refreshDimensions();
                            PacketDistributor.sendToPlayersTrackingEntity(sp, new ToggleCrawlPayload(sp.getId(), payload.crawling()));
                        }
                    } else {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.level != null) {
                            Entity ent = mc.level.getEntity(payload.entityId());
                            if (ent instanceof Player p && p != mc.player) {
                                p.setData(ModAttachments.CRAWLING, payload.crawling());
                                p.setPose(payload.crawling() ? Pose.SWIMMING : Pose.STANDING);
                                p.refreshDimensions();
                            }
                        }
                    }
                })
        );

        registrar.playBidirectional(
                ToggleHoodPayload.TYPE,
                ToggleHoodPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.flow().isServerbound()) {
                        if (ctx.player() instanceof ServerPlayer sp) {
                            sp.setData(ModAttachments.HOOD_UP, payload.hoodUp());
                            PacketDistributor.sendToPlayersTrackingEntity(sp, new ToggleHoodPayload(sp.getId(), payload.hoodUp()));
                        }
                    } else {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.level != null) {
                            Entity ent = mc.level.getEntity(payload.entityId());
                            if (ent instanceof Player p && p != mc.player) {
                                p.setData(ModAttachments.HOOD_UP, payload.hoodUp());
                            }
                        }
                    }
                })
        );

        registrar.playToClient(
                SyncSlotsMenuPayload.TYPE,
                SyncSlotsMenuPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> clientSlotsMenuHook.accept(payload))
        );

        registrar.playToServer(
                SlotActionPayload.TYPE,
                SlotActionPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;
                    CharacterSlotsData slotsData = sp.getData(ModAttachments.CHARACTER_SLOTS);

                    if (payload.action() == SlotActionPayload.ACTION_REQUEST_OPEN_MENU) {
                        slotsData.saveCurrentPlayerToActiveSlot(sp);
                        PacketDistributor.sendToPlayer(sp, new SyncSlotsMenuPayload(true, slotsData.getActiveSlot(), slotsData.toClientSummaries()));

                    } else if (payload.action() == SlotActionPayload.ACTION_DELETE_SLOT) {
                        slotsData.deleteSlot(payload.slotIndex());
                        PacketDistributor.sendToPlayer(sp, new SyncSlotsMenuPayload(true, slotsData.getActiveSlot(), slotsData.toClientSummaries()));

                    } else if (payload.action() == SlotActionPayload.ACTION_SELECT_OR_CREATE) {
                        int idx = payload.slotIndex();
                        slotsData.loadSlotIntoPlayer(sp, idx);
                        CharacterSlotsData.SlotEntry entry = slotsData.getSlot(idx);

                        if (entry.created && entry.appearance.isCreated()) {
                            applyStatsAndSize(sp, false);
                            sp.refreshDisplayName();
                            sp.refreshTabListName();
                            broadcastAppearance(sp, false);
                            broadcastExtraEquipment(sp);
                            PacketDistributor.sendToPlayer(sp, new SyncSlotsMenuPayload(false, slotsData.getActiveSlot(), slotsData.toClientSummaries()));
                        } else {
                            broadcastAppearance(sp, true);
                        }
                    }
                })
        );

        registrar.playToClient(
                SyncStandCloakPayload.TYPE,
                SyncStandCloakPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.level != null && mc.level.getEntity(payload.standEntityId()) instanceof ArmorStand stand) {
                        stand.setData(ModAttachments.STAND_CLOAK, payload.hasCloak());
                    }
                })
        );

        registrar.playToClient(
                SyncDownedPayload.TYPE,
                SyncDownedPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    Minecraft mc = Minecraft.getInstance();
                    // Если entityId == -1, то downedTicks передает прогресс 5-секундного поднятия (0..100 тиков) для HUD!
                    if (payload.entityId() == -1) {
                        SkyrimThemeRenderer.clientReviveProgressTicks = payload.downedTicks();
                        return;
                    }
                    if (mc.level != null && mc.level.getEntity(payload.entityId()) instanceof Player p) {
                        p.setData(ModAttachments.DOWNED_TICKS, payload.downedTicks());
                        if (payload.downedTicks() > 0) {
                            p.setPose(Pose.CROUCHING);
                            if (p == mc.player && mc.screen != null && !(mc.screen instanceof CharacterSlotSelectionScreen)) {
                                mc.setScreen(null);
                            }
                        } else {
                            p.setPose(Pose.STANDING);
                            if (p == mc.player) {
                                SkyrimThemeRenderer.clientReviveProgressTicks = 0;
                            }
                        }
                        p.refreshDimensions();
                    }
                })
        );

        registrar.playToServer(
                GiveUpPayload.TYPE,
                GiveUpPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer sp && sp.getData(ModAttachments.DOWNED_TICKS) > 0) {
                        setPlayerDownedState(sp, 0);
                        sp.hurt(sp.damageSources().genericKill(), Float.MAX_VALUE);
                    }
                })
        );
    }

    public static void setPlayerDownedState(ServerPlayer sp, int ticks) {
        sp.setData(ModAttachments.DOWNED_TICKS, ticks);
        sp.getPersistentData().putInt("rpgcore_downed_hits", 0);
        sp.getPersistentData().putInt("rpgcore_revive_prog", 0);
        sp.getPersistentData().remove("rpgcore_reviver_id");

        AttributeInstance moveSpeed = sp.getAttribute(Attributes.MOVEMENT_SPEED);
        if (moveSpeed != null) {
            moveSpeed.removeModifier(DOWNED_SLOW_MOD_ID);
            if (ticks > 0) {
                moveSpeed.addTransientModifier(new AttributeModifier(
                        DOWNED_SLOW_MOD_ID, -0.75D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                ));
            }
        }
        if (ticks > 0) {
            sp.closeContainer();
            sp.setData(ModAttachments.CRAWLING, false);
            sp.setPose(Pose.CROUCHING);
            sp.setShiftKeyDown(false);
        } else {
            sp.setPose(Pose.STANDING);
            sp.setShiftKeyDown(false);
        }
        sp.refreshDimensions();
        sp.refreshDisplayName();
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(sp, new SyncDownedPayload(sp.getId(), ticks));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPreventFatalDamageToKnockdown(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;

        int currentDowned = sp.getData(ModAttachments.DOWNED_TICKS);

        if (currentDowned > 0) {
            Entity attacker = event.getSource().getEntity();
            if (attacker != null) {
                int hits = sp.getPersistentData().getInt("rpgcore_downed_hits") + 1;
                sp.getPersistentData().putInt("rpgcore_downed_hits", hits);

                if (hits >= 5) {
                    setPlayerDownedState(sp, 0);
                    event.setNewDamage(Float.MAX_VALUE);
                } else {
                    event.setNewDamage(0.0F);
                    sp.setHealth(Math.max(1.0F, (5 - hits) * 2.0F));
                    sp.invulnerableTime = 12;
                    sp.level().playSound(null, sp.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.0F, 0.85F);
                }
            } else {
                event.setNewDamage(0.0F);
            }
            return;
        }

        float currentHp = sp.getHealth() + sp.getAbsorptionAmount();
        if (event.getNewDamage() >= currentHp - 0.5F) {
            event.setNewDamage(0.0F);
            sp.setHealth(10.0F);
            sp.invulnerableTime = 30;
            sp.removeAllEffects();
            setPlayerDownedState(sp, MAX_DOWNED_TICKS);
            sp.level().playSound(null, sp.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.0F, 0.75F);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerFatalDeathBackup(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            setPlayerDownedState(sp, 0);
            return;
        }

        int currentDowned = sp.getData(ModAttachments.DOWNED_TICKS);
        if (currentDowned <= 0) {
            event.setCanceled(true);
            sp.setHealth(10.0F);
            sp.invulnerableTime = 30;
            sp.removeAllEffects();
            setPlayerDownedState(sp, MAX_DOWNED_TICKS);
        } else {
            setPlayerDownedState(sp, 0);
        }
    }

    @SubscribeEvent
    public static void onDownedDamageProtection(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            int downed = sp.getData(ModAttachments.DOWNED_TICKS);
            if (downed > MAX_DOWNED_TICKS - 30 && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                event.setCanceled(true);
                return;
            }
        }
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && attacker.getData(ModAttachments.DOWNED_TICKS) > 0) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDownedRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().getData(ModAttachments.DOWNED_TICKS) > 0) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDownedRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity().getData(ModAttachments.DOWNED_TICKS) > 0) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDownedEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getEntity().getData(ModAttachments.DOWNED_TICKS) > 0) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onDownedOpenContainer(PlayerContainerEvent.Open event) {
        if (event.getEntity() instanceof ServerPlayer sp && sp.getData(ModAttachments.DOWNED_TICKS) > 0) {
            sp.closeContainer();
        }
    }

    @SubscribeEvent
    public static void onDownedPlayerAttack(AttackEntityEvent event) {
        if (event.getEntity().getData(ModAttachments.DOWNED_TICKS) > 0) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDownedBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (event.getEntity().getData(ModAttachments.DOWNED_TICKS) > 0) {
            event.setCanceled(true);
        }
    }

    /**
     * ПУНКТ 5: Зажатие ПКМ по раненому игроку обновляет таймер взаимодействия ("последний тик зажатия ПКМ"),
     * а само поднятие занимает ровно 5 секунд (100 тиков) непрерывного удержания ПКМ!
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onReviveDownedPlayer(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity().getData(ModAttachments.DOWNED_TICKS) > 0) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }

        if (!(event.getTarget() instanceof ServerPlayer downedTarget)) return;
        int downedTicks = downedTarget.getData(ModAttachments.DOWNED_TICKS);
        if (downedTicks <= 0) return;

        Player reviver = event.getEntity();
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        if (!reviver.level().isClientSide() && reviver instanceof ServerPlayer spReviver) {
            downedTarget.getPersistentData().putUUID("rpgcore_reviver_id", spReviver.getUUID());
            downedTarget.getPersistentData().putInt("rpgcore_revive_hold_timeout", 8);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("race")
                        .then(Commands.literal("gui")
                                .executes(ctx -> {
                                    ServerPlayer sp = ctx.getSource().getPlayerOrException();
                                    broadcastAppearance(sp, true);
                                    return Command.SINGLE_SUCCESS;
                                }))
        );
    }

    private static void handleClanAction(ServerPlayer sp, ClanActionPayload pkt) {
        ClanSavedData db = ClanSavedData.get(sp.serverLevel());
        UUID myId = sp.getUUID();
        String myName = sp.getData(ModAttachments.APPEARANCE).getFullRpName();
        ClanData myClan = db.getPlayerClan(myId);

        switch (pkt.action()) {
            case ClanActionPayload.ACTION_CREATE -> {
                if (myClan != null) return;
                String[] parts = pkt.strArg().split("\\|", 2);
                if (parts.length < 2) return;
                String name = parts[0].trim();
                String tag = parts[1].trim().toUpperCase();
                if (name.length() < 2 || tag.length() < 3 || tag.length() > 4) {
                    sp.sendSystemMessage(Component.literal("§cДля создания гильдии введите название и ТЕГ из 3-4 букв!"));
                    return;
                }
                db.createClan(myId, myName, name, tag, pkt.intArg());
                sp.sendSystemMessage(Component.literal("§aГильдия «" + name + "» [" + tag + "] успешно создана!"));
            }
            case ClanActionPayload.ACTION_INVITE_PLAYER -> {
                if (myClan == null) return;
                UUID targetId = UUID.fromString(pkt.strArg());
                if (db.getPlayerClan(targetId) != null) return;

                ServerPlayer targetSp = sp.server.getPlayerList().getPlayer(targetId);
                String targetName = (targetSp != null)
                        ? targetSp.getData(ModAttachments.APPEARANCE).getFullRpName()
                        : "Игрок";

                ClanData.ClanRank myRank = myClan.getRankOf(myId);
                if (myRank == ClanData.ClanRank.FOUNDER || myRank == ClanData.ClanRank.RIGHT_HAND) {
                    db.addDirectInvite(targetId, new ClanData.DirectInvite(
                            myClan.getClanId(), myClan.getClanName(), myClan.getClanTag(), myClan.getColorHex(), myName
                    ));
                    if (targetSp != null) {
                        targetSp.sendSystemMessage(Component.literal("§eВас пригласили в гильдию «" + myClan.getClanName() + "» [" + myClan.getClanTag() + "]! Нажмите [I], чтобы принять."));
                    }
                } else {
                    myClan.getPendingApprovals().removeIf(p -> p.targetUuid().equals(targetId));
                    myClan.getPendingApprovals().add(new ClanData.PendingApproval(targetId, targetName, myName));
                    db.setDirty();
                    sp.sendSystemMessage(Component.literal("§7Заявка на приглашение " + targetName + " отправлена на одобрение Основателю."));
                }
            }
            case ClanActionPayload.ACTION_APPROVE_PENDING -> {
                if (myClan == null) return;
                ClanData.ClanRank myRank = myClan.getRankOf(myId);
                if (myRank != ClanData.ClanRank.FOUNDER && myRank != ClanData.ClanRank.RIGHT_HAND) return;

                UUID targetId = UUID.fromString(pkt.strArg());
                ClanData.PendingApproval found = null;
                for (var p : myClan.getPendingApprovals()) {
                    if (p.targetUuid().equals(targetId)) {
                        found = p;
                        break;
                    }
                }
                if (found != null) {
                    myClan.getPendingApprovals().remove(found);
                    db.addDirectInvite(targetId, new ClanData.DirectInvite(
                            myClan.getClanId(), myClan.getClanName(), myClan.getClanTag(), myClan.getColorHex(), found.invitedByName()
                    ));
                    db.setDirty();
                    ServerPlayer targetSp = sp.server.getPlayerList().getPlayer(targetId);
                    if (targetSp != null) {
                        targetSp.sendSystemMessage(Component.literal("§aРуководство гильдии «" + myClan.getClanName() + "» одобрило ваше приглашение! Нажмите [I]."));
                    }
                }
            }
            case ClanActionPayload.ACTION_REJECT_PENDING -> {
                if (myClan == null) return;
                ClanData.ClanRank myRank = myClan.getRankOf(myId);
                if (myRank != ClanData.ClanRank.FOUNDER && myRank != ClanData.ClanRank.RIGHT_HAND) return;
                UUID targetId = UUID.fromString(pkt.strArg());
                myClan.getPendingApprovals().removeIf(p -> p.targetUuid().equals(targetId));
                db.setDirty();
            }
            case ClanActionPayload.ACTION_ACCEPT_INVITE -> {
                if (myClan != null) return;
                ClanData targetClan = db.getClanById(pkt.strArg());
                if (targetClan != null) {
                    targetClan.getMembers().add(new ClanData.MemberEntry(myId, myName, ClanData.ClanRank.NOVICE));
                    db.getInvitesFor(myId).clear();
                    db.setDirty();
                }
            }
            case ClanActionPayload.ACTION_DECLINE_INVITE -> {
                db.getInvitesFor(myId).removeIf(i -> i.clanId().equals(pkt.strArg()));
                db.setDirty();
            }
            case ClanActionPayload.ACTION_TOGGLE_RIGHT_HAND -> {
                if (myClan == null || myClan.getRankOf(myId) != ClanData.ClanRank.FOUNDER) return;
                UUID targetId = UUID.fromString(pkt.strArg());
                for (int i = 0; i < myClan.getMembers().size(); i++) {
                    ClanData.MemberEntry m = myClan.getMembers().get(i);
                    if (m.uuid().equals(targetId) && m.rank() != ClanData.ClanRank.FOUNDER) {
                        if (m.rank() == ClanData.ClanRank.NOVICE) {
                            if (myClan.countRightHands() >= 3) {
                                sp.sendSystemMessage(Component.literal("§cВ гильдии может быть максимум 3 заместителя!"));
                                return;
                            }
                            myClan.getMembers().set(i, new ClanData.MemberEntry(m.uuid(), m.rpName(), ClanData.ClanRank.RIGHT_HAND));
                        } else {
                            myClan.getMembers().set(i, new ClanData.MemberEntry(m.uuid(), m.rpName(), ClanData.ClanRank.NOVICE));
                        }
                        db.setDirty();
                        break;
                    }
                }
            }
            case ClanActionPayload.ACTION_KICK_OR_LEAVE -> {
                if (myClan == null) return;
                UUID targetId = UUID.fromString(pkt.strArg());
                ClanData.ClanRank myRank = myClan.getRankOf(myId);
                if (targetId.equals(myId)) {
                    if (myRank == ClanData.ClanRank.FOUNDER) {
                        db.removeClan(myClan.getClanId());
                    } else {
                        myClan.getMembers().removeIf(m -> m.uuid().equals(myId));
                        db.setDirty();
                    }
                } else if (myRank == ClanData.ClanRank.FOUNDER) {
                    myClan.getMembers().removeIf(m -> m.uuid().equals(targetId));
                    db.setDirty();
                }
            }
            case ClanActionPayload.ACTION_TOGGLE_FF -> {
                if (myClan != null && myClan.getRankOf(myId) == ClanData.ClanRank.FOUNDER) {
                    myClan.setFriendlyFire(!myClan.isFriendlyFire());
                    db.setDirty();
                }
            }
        }

        for (ServerPlayer p : sp.server.getPlayerList().getPlayers()) {
            p.refreshDisplayName();
            p.refreshTabListName();
        }
        syncAllClans(sp.server);
    }

    public static void syncAllClans(MinecraftServer server) {
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            sendClanSyncTo(sp);
        }
    }

    public static void sendClanSyncTo(ServerPlayer sp) {
        ClanSavedData db = ClanSavedData.get(sp.serverLevel());
        ClanData myClan = db.getPlayerClan(sp.getUUID());
        List<ClanData.DirectInvite> invites = db.getInvitesFor(sp.getUUID());

        List<SyncClanPayload.OnlineCandidate> candidates = new ArrayList<>();
        List<ClanData.AllyCompassPos> allies = new ArrayList<>();

        for (ServerPlayer other : sp.server.getPlayerList().getPlayers()) {
            String rpName = other.getData(ModAttachments.APPEARANCE).getFullRpName();
            if (db.getPlayerClan(other.getUUID()) == null && !other.getUUID().equals(sp.getUUID())) {
                candidates.add(new SyncClanPayload.OnlineCandidate(other.getUUID(), rpName));
            }
            if (myClan != null && !other.getUUID().equals(sp.getUUID())
                    && myClan.getRankOf(other.getUUID()) != null
                    && other.level().dimension().equals(sp.level().dimension())) {
                allies.add(new ClanData.AllyCompassPos(
                        other.getUUID(), rpName, other.getX(), other.getZ(), myClan.getColorHex()
                ));
            }
        }

        PacketDistributor.sendToPlayer(sp, new SyncClanPayload(
                Optional.ofNullable(myClan), invites, candidates, allies
        ));
    }

    /**
     * ПУНКТ 7: Кастомная длительность времени суток:
     * - День (0..12000 ванильных единиц) идет 30 реальных минут (36000 тиков -> +1 единица каждые 3 тика).
     * - Ночь (12000..24000 ванильных единиц) идет 15 реальных минут (18000 тиков -> +2 единицы каждые 3 тика).
     */
    /**
     * ПУНКТ 7 + ИСПРАВЛЕНИЕ СНА В КРОВАТИ (Пункт 3):
     * - День идет 30 реальных минут, Ночь идет 15 реальных минут.
     * - Если игроки легли в кровать и проспали ночь (isSleepingLongEnough),
     *   время автоматически переводится на 06:00 утра нового дня, игроки просыпаются
     *   и на экране загорается плашка "ДЕНЬ X"!
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int ticks = server.getTickCount();

        if (ticks % 10 == 0) {
            syncAllClans(server);
        }

        ServerLevel overworld = server.overworld();
        if (overworld.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
            overworld.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        }

        // Проверяем, спят ли все игроки в мире достаточно долго (100 тиков в кровати),
        // так как при RULE_DAYLIGHT = false ванильный ServerLevel не перематывает время сна сам!
        List<ServerPlayer> players = overworld.players();
        if (!players.isEmpty()) {
            boolean allSleepingReady = true;
            int sleepingCount = 0;
            for (ServerPlayer sp : players) {
                if (!sp.isSpectator()) {
                    if (sp.isSleeping() && sp.getSleepTimer() >= 98) {
                        sleepingCount++;
                    } else {
                        allSleepingReady = false;
                        break;
                    }
                }
            }

            if (allSleepingReady && sleepingCount > 0) {
                long curDayTime = overworld.getDayTime();
                // Вычисляем ровно 06:00 утра (кратное 24000) следующего игрового дня!
                long nextMorningDayTime = ((curDayTime / 24000L) + 1L) * 24000L;
                overworld.setDayTime(nextMorningDayTime);

                // Очищаем погоду после сна, как в ванильном Minecraft, и будим всех спящих
                if (overworld.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE)) {
                    overworld.setWeatherParameters(6000, 0, false, false);
                }
                for (ServerPlayer sp : players) {
                    if (sp.isSleeping()) {
                        sp.stopSleepInBed(false, false);
                    }
                }

                // Мгновенно синхронизируем новое утро (06:00) всем клиентам, чтобы сработала плашка "ДЕНЬ X"!
                ClientboundSetTimePacket morningPkt = new ClientboundSetTimePacket(
                        overworld.getGameTime(), nextMorningDayTime, false
                );
                for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
                    sp.connection.send(morningPkt);
                }
                return;
            }
        }

        if (ticks % 3 == 0) {
            long curDayTime = overworld.getDayTime();
            long timeOfDay = curDayTime % 24000L;
            long step = (timeOfDay < 12000L) ? 1L : 2L;
            long nextDayTime = curDayTime + step;
            overworld.setDayTime(nextDayTime);

            ClientboundSetTimePacket timePkt = new ClientboundSetTimePacket(
                    overworld.getGameTime(), nextDayTime, false
            );
            for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
                sp.connection.send(timePkt);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        PlayerAppearanceData data = player.getData(ModAttachments.APPEARANCE);

        // ПУНКТ 2: Эльф проходит сквозь кусты сладких ягод без замедления (как на клиенте, так и на сервере)
        if (data.race() == RaceType.ELF) {
            if (player.level().getBlockState(player.blockPosition()).is(Blocks.SWEET_BERRY_BUSH)
                    || player.level().getBlockState(player.blockPosition().above()).is(Blocks.SWEET_BERRY_BUSH)) {
                player.resetFallDistance();
            }
        }

        if (player.level().isClientSide) return;

        if (player instanceof ServerPlayer sp) {
            int downed = sp.getData(ModAttachments.DOWNED_TICKS);
            if (downed > 0) {
                // ПУНКТ 5: Проверяем, удерживает ли сейчас другой игрок ПКМ по раненому (5 секунд = 100 тиков)
                int holdTimeout = sp.getPersistentData().getInt("rpgcore_revive_hold_timeout");
                ServerPlayer activeReviver = null;
                if (holdTimeout > 0 && sp.getPersistentData().hasUUID("rpgcore_reviver_id")) {
                    UUID revId = sp.getPersistentData().getUUID("rpgcore_reviver_id");
                    ServerPlayer candidate = sp.server.getPlayerList().getPlayer(revId);
                    if (candidate != null && candidate.isAlive()
                            && candidate.getData(ModAttachments.DOWNED_TICKS) <= 0
                            && candidate.distanceToSqr(sp) <= 16.0D) {
                        activeReviver = candidate;
                    }
                }

                if (activeReviver != null) {
                    sp.getPersistentData().putInt("rpgcore_revive_hold_timeout", holdTimeout - 1);
                    int prog = sp.getPersistentData().getInt("rpgcore_revive_prog") + 1;
                    sp.getPersistentData().putInt("rpgcore_revive_prog", prog);

                    if (prog % 2 == 0) {
                        PacketDistributor.sendToPlayer(sp, new SyncDownedPayload(-1, prog));
                        PacketDistributor.sendToPlayer(activeReviver, new SyncDownedPayload(-1, prog));
                    }

                    if (prog >= REVIVE_HOLD_TICKS) {
                        // Прошло ровно 5 секунд непрерывного зажатия ПКМ — раненый поднят!
                        PacketDistributor.sendToPlayer(sp, new SyncDownedPayload(-1, 0));
                        PacketDistributor.sendToPlayer(activeReviver, new SyncDownedPayload(-1, 0));
                        setPlayerDownedState(sp, 0);
                        sp.setHealth(Math.max(4.0F, sp.getMaxHealth() * 0.30F));
                        sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));

                        sp.level().playSound(null, sp.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.5F, 1.35F);

                        String reviverName = activeReviver.getData(ModAttachments.APPEARANCE).getFullRpName();
                        String targetName = sp.getData(ModAttachments.APPEARANCE).getFullRpName();
                        sp.sendSystemMessage(Component.literal("§aИгрок §e" + reviverName + " §aподнял вас на ноги!"));
                        activeReviver.sendSystemMessage(Component.literal("§aВы подняли раненого игрока §e" + targetName + "§a!"));
                        return;
                    }
                } else {
                    int prevProg = sp.getPersistentData().getInt("rpgcore_revive_prog");
                    if (prevProg > 0) {
                        sp.getPersistentData().putInt("rpgcore_revive_prog", 0);
                        PacketDistributor.sendToPlayer(sp, new SyncDownedPayload(-1, 0));
                        if (sp.getPersistentData().hasUUID("rpgcore_reviver_id")) {
                            ServerPlayer oldRev = sp.server.getPlayerList().getPlayer(sp.getPersistentData().getUUID("rpgcore_reviver_id"));
                            if (oldRev != null) {
                                PacketDistributor.sendToPlayer(oldRev, new SyncDownedPayload(-1, 0));
                            }
                        }
                    }
                }

                downed--;
                sp.setData(ModAttachments.DOWNED_TICKS, downed);
                sp.setPose(Pose.CROUCHING);
                sp.setSprinting(false);

                if (sp.getMaxHealth() > sp.getData(ModAttachments.APPEARANCE).race().getMaxHealth()) {
                    applyStatsAndSize(sp, false);
                }
                if (sp.getHealth() > 10.0F) {
                    sp.setHealth(10.0F);
                }

                if (downed <= 0) {
                    setPlayerDownedState(sp, 0);
                    sp.hurt(sp.damageSources().genericKill(), Float.MAX_VALUE);
                    return;
                } else if (downed % 20 == 0) {
                    PacketDistributor.sendToPlayersTrackingEntityAndSelf(sp, new SyncDownedPayload(sp.getId(), downed));
                }
            }
        }

        if (data.race() == RaceType.MUTANT) {
            if (player.hasEffect(MobEffects.POISON)) player.removeEffect(MobEffects.POISON);
            if (player.hasEffect(MobEffects.HUNGER)) player.removeEffect(MobEffects.HUNGER);
            if (player.tickCount % 40 == 0) {
                int light = player.level().getMaxLocalRawBrightness(player.blockPosition());
                if (light <= 7) {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 320, 0, true, false, false));
                }
            }
        } else if (data.race() == RaceType.DRAGONBORN) {
            if (player.getRemainingFireTicks() > 0) {
                player.clearFire();
            }
        } else if (data.race() == RaceType.ORC) {
            if (player.getHealth() <= player.getMaxHealth() * 0.40F && player.tickCount % 20 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 1, true, false, true));
            }
        }
    }

    @SubscribeEvent
    public static void onFoodEaten(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            if (sp.getData(ModAttachments.APPEARANCE).race() == RaceType.DRAGONBORN) {
                ItemStack foodStack = event.getItem();
                FoodProperties food = foodStack.get(DataComponents.FOOD);
                if (food != null && !isMeatFood(foodStack)) {
                    sp.getPersistentData().putInt("rpgcore_pre_food", sp.getFoodData().getFoodLevel());
                    sp.getPersistentData().putFloat("rpgcore_pre_sat", sp.getFoodData().getSaturationLevel());
                } else {
                    sp.getPersistentData().remove("rpgcore_pre_food");
                }
            }
        }
    }

    @SubscribeEvent
    public static void onFoodFinished(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer sp && sp.getPersistentData().contains("rpgcore_pre_food")) {
            int preFood = sp.getPersistentData().getInt("rpgcore_pre_food");
            float preSat = sp.getPersistentData().getFloat("rpgcore_pre_sat");
            sp.getPersistentData().remove("rpgcore_pre_food");
            sp.getPersistentData().remove("rpgcore_pre_sat");

            int gainedFood = Math.max(0, sp.getFoodData().getFoodLevel() - preFood);
            int reducedFood = Math.max(1, gainedFood / 3);
            sp.getFoodData().setFoodLevel(Math.min(20, preFood + reducedFood));
            sp.getFoodData().setSaturation(preSat + 0.2F);
        }
    }

    private static boolean isMeatFood(ItemStack stack) {
        return stack.is(ItemTags.MEAT)
                || stack.is(Items.COOKED_BEEF) || stack.is(Items.BEEF)
                || stack.is(Items.COOKED_PORKCHOP) || stack.is(Items.PORKCHOP)
                || stack.is(Items.COOKED_MUTTON) || stack.is(Items.MUTTON)
                || stack.is(Items.COOKED_CHICKEN) || stack.is(Items.CHICKEN)
                || stack.is(Items.COOKED_RABBIT) || stack.is(Items.RABBIT)
                || stack.is(Items.RABBIT_STEW)
                || stack.is(Items.COOKED_COD) || stack.is(Items.COD)
                || stack.is(Items.COOKED_SALMON) || stack.is(Items.SALMON)
                || stack.is(Items.ROTTEN_FLESH);
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (event.getEntity().getData(ModAttachments.APPEARANCE).race() == RaceType.DWARF) {
            event.setNewSpeed(event.getNewSpeed() * 1.25F);
        }
    }

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (event.getSource().getDirectEntity() instanceof AbstractArrow
                && event.getSource().getEntity() instanceof Player shooter) {
            if (shooter.getData(ModAttachments.APPEARANCE).race() == RaceType.ELF) {
                event.setNewDamage(event.getNewDamage() * 1.25F);
            }
        }
        if (event.getEntity() instanceof Player victim
                && victim.getData(ModAttachments.APPEARANCE).race() == RaceType.ELF
                && event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
            event.setNewDamage(event.getNewDamage() * 1.20F);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            RaceType race = player.getData(ModAttachments.APPEARANCE).race();
            if (race == RaceType.DRAGONBORN && event.getSource().is(DamageTypeTags.IS_FIRE)) {
                event.setCanceled(true);
                return;
            }
            if (race == RaceType.ELF && event.getSource().is(DamageTypes.SWEET_BERRY_BUSH)) {
                event.setCanceled(true);
                return;
            }
        }
        if (event.getEntity() instanceof ServerPlayer victim && event.getSource().getEntity() instanceof ServerPlayer attacker) {
            if (victim.equals(attacker)) return;
            ClanSavedData db = ClanSavedData.get(victim.serverLevel());
            ClanData victimClan = db.getPlayerClan(victim.getUUID());
            if (victimClan != null && victimClan.getRankOf(attacker.getUUID()) != null && !victimClan.isFriendlyFire()) {
                event.setCanceled(true);
            }
        }
    }

    public static void broadcastAppearance(ServerPlayer sp, boolean forceOpen) {
        PlayerAppearanceData data = sp.getData(ModAttachments.APPEARANCE);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(sp, new SyncAppearancePayload(sp.getId(), forceOpen, data));
    }

    public static void broadcastExtraEquipment(ServerPlayer sp) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(sp, new SyncExtraEquipmentPayload(
                sp.getId(), sp.getData(ModAttachments.EXTRA_EQUIPMENT)
        ));
    }

    public static void applyStatsAndSize(Player player, boolean heal) {
        PlayerAppearanceData data = player.getData(ModAttachments.APPEARANCE);
        RaceType race = data.race();

        AttributeInstance maxHp = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHp != null) {
            List<ResourceLocation> toRemove = new ArrayList<>();
            for (AttributeModifier mod : maxHp.getModifiers()) {
                if (mod.id().getNamespace().equals(RPGCore.MODID)) {
                    toRemove.add(mod.id());
                }
            }
            for (ResourceLocation id : toRemove) {
                maxHp.removeModifier(id);
            }

            double diff = race.getMaxHealth() - 20.0D;
            if (diff != 0) {
                maxHp.addPermanentModifier(new AttributeModifier(HEALTH_MOD_ID, diff, AttributeModifier.Operation.ADD_VALUE));
            }
            if (heal || player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }

        AttributeInstance moveSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (moveSpeed != null) {
            moveSpeed.removeModifier(SPEED_MOD_ID);
            if (race == RaceType.ELF) {
                moveSpeed.addPermanentModifier(new AttributeModifier(SPEED_MOD_ID, 0.10D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        }

        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage != null) {
            attackDamage.removeModifier(ATTACK_MOD_ID);
            if (race == RaceType.MUTANT) {
                attackDamage.addPermanentModifier(new AttributeModifier(ATTACK_MOD_ID, 2.0D, AttributeModifier.Operation.ADD_VALUE));
            } else if (race == RaceType.DRAGONBORN) {
                attackDamage.addPermanentModifier(new AttributeModifier(ATTACK_MOD_ID, 1.5D, AttributeModifier.Operation.ADD_VALUE));
            } else if (race == RaceType.ORC) {
                attackDamage.addPermanentModifier(new AttributeModifier(ATTACK_MOD_ID, 3.0D, AttributeModifier.Operation.ADD_VALUE));
            }
        }

        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.removeModifier(ARMOR_MOD_ID);
            if (race == RaceType.DRAGONBORN) {
                armor.addPermanentModifier(new AttributeModifier(ARMOR_MOD_ID, 4.0D, AttributeModifier.Operation.ADD_VALUE));
            }
        }

        AttributeInstance kbRes = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kbRes != null) {
            kbRes.removeModifier(KNOCKBACK_MOD_ID);
            if (race == RaceType.ORC) {
                kbRes.addPermanentModifier(new AttributeModifier(KNOCKBACK_MOD_ID, 0.40D, AttributeModifier.Operation.ADD_VALUE));
            }
        }

        player.refreshDimensions();
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            PlayerAppearanceData data = sp.getData(ModAttachments.APPEARANCE);
            CharacterSlotsData slotsData = sp.getData(ModAttachments.CHARACTER_SLOTS);

            if (data.isCreated() && !slotsData.getSlot(0).created && slotsData.getActiveSlot() < 0) {
                slotsData.setActiveSlot(0);
                slotsData.saveCurrentPlayerToActiveSlot(sp);
            }

            applyStatsAndSize(sp, false);
            sp.refreshDisplayName();
            sp.refreshTabListName();
            broadcastAppearance(sp, false);
            broadcastExtraEquipment(sp);
            syncAllClans(sp.server);

            PacketDistributor.sendToPlayer(sp, new SyncSlotsMenuPayload(
                    true, slotsData.getActiveSlot(), slotsData.toClientSummaries()
            ));
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer viewer) {
            PacketDistributor.sendToPlayer(viewer, new SyncAppearancePayload(
                    target.getId(), false, target.getData(ModAttachments.APPEARANCE)
            ));
            PacketDistributor.sendToPlayer(viewer, new SyncExtraEquipmentPayload(
                    target.getId(), target.getData(ModAttachments.EXTRA_EQUIPMENT)
            ));
            PacketDistributor.sendToPlayer(viewer, new ToggleCrawlPayload(
                    target.getId(), target.getData(ModAttachments.CRAWLING)
            ));
            PacketDistributor.sendToPlayer(viewer, new SyncDownedPayload(
                    target.getId(), target.getData(ModAttachments.DOWNED_TICKS)
            ));
        }
        if (event.getTarget() instanceof ArmorStand stand && event.getEntity() instanceof ServerPlayer viewer) {
            if (stand.getData(ModAttachments.STAND_CLOAK)) {
                PacketDistributor.sendToPlayer(viewer, new SyncStandCloakPayload(stand.getId(), true));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            sp.setData(ModAttachments.CRAWLING, false);
            setPlayerDownedState(sp, 0);
            applyStatsAndSize(sp, true);
            broadcastAppearance(sp, false);
            broadcastExtraEquipment(sp);
            sendClanSyncTo(sp);
        }
    }

    @SubscribeEvent
    public static void onEntitySize(EntityEvent.Size event) {
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
            } else if (event.getPose() == Pose.CROUCHING || player.getData(ModAttachments.DOWNED_TICKS) > 0) {
                float boxH = Math.min(2.45F, 1.5F * h);
                float boxW = Math.min(0.85F, 0.6F * w);
                float eyeH = (data.race() == RaceType.ORC) ? 1.85F : (1.27F * h);
                event.setNewSize(EntityDimensions.fixed(boxW, boxH).withEyeHeight(eyeH));
            } else {
                float boxH = (data.race() == RaceType.ORC) ? 2.82F : (1.8F * h);
                float boxW = (data.race() == RaceType.ORC) ? 0.82F : (0.6F * w);
                float eyeH = (data.race() == RaceType.ORC) ? 2.35F : (1.62F * h);
                event.setNewSize(EntityDimensions.fixed(boxW, boxH).withEyeHeight(eyeH));
            }
        }
    }

    @SubscribeEvent
    public static void onXpGain(PlayerXpEvent.XpChange event) {
        PlayerAppearanceData data = event.getEntity().getData(ModAttachments.APPEARANCE);
        if (data.race() == RaceType.HUMAN && event.getAmount() > 0) {
            event.setAmount(event.getAmount() * 2);
        }
    }

    @SubscribeEvent
    public static void onNameFormat(PlayerEvent.NameFormat event) {
        PlayerAppearanceData data = event.getEntity().getData(ModAttachments.APPEARANCE);
        if (data.isCreated()) {
            if (event.getEntity() instanceof ServerPlayer sp) {
                ClanSavedData db = ClanSavedData.get(sp.serverLevel());
                ClanData clan = db.getPlayerClan(sp.getUUID());
                if (clan != null) {
                    event.setDisplayname(
                            Component.literal("[" + clan.getClanTag() + "] ").withColor(clan.getColorHex())
                                    .append(Component.literal(data.getFullRpName()).withColor(0xFFFFFFFF))
                    );
                    return;
                }
            }
            event.setDisplayname(Component.literal(data.getFullRpName()));
        }
    }

    @SubscribeEvent
    public static void onTabListName(PlayerEvent.TabListNameFormat event) {
        PlayerAppearanceData data = event.getEntity().getData(ModAttachments.APPEARANCE);
        if (data.isCreated() && event.getEntity() instanceof ServerPlayer sp) {
            ClanSavedData db = ClanSavedData.get(sp.serverLevel());
            ClanData clan = db.getPlayerClan(sp.getUUID());
            if (clan != null) {
                event.setDisplayName(
                        Component.literal("[" + clan.getClanTag() + "] ").withColor(clan.getColorHex())
                                .append(Component.literal(data.getFullRpName()).withColor(0xFFFFFFFF))
                );
                return;
            }
            event.setDisplayName(Component.literal(data.getFullRpName()));
        }
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer sp = event.getPlayer();
        PlayerAppearanceData data = sp.getData(ModAttachments.APPEARANCE);
        if (data.isCreated()) {
            event.setCanceled(true);
            ClanSavedData db = ClanSavedData.get(sp.serverLevel());
            ClanData clan = db.getPlayerClan(sp.getUUID());
            Component prefix = Component.empty();
            if (clan != null) {
                prefix = Component.literal("[" + clan.getClanTag() + "] ").withColor(clan.getColorHex());
            }
            Component msg = Component.empty()
                    .append(prefix)
                    .append(Component.literal("§e" + data.getFullRpName() + "§7: §f"))
                    .append(event.getMessage());
            sp.server.getPlayerList().broadcastSystemMessage(msg, false);
        }
    }

    @SubscribeEvent
    public static void onInteractArmorStand(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getTarget() instanceof ArmorStand stand)) return;
        Player player = event.getEntity();
        ItemStack held = player.getItemInHand(event.getHand());

        if (held.getItem() instanceof BackpackItem) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            if (!player.level().isClientSide()) {
                player.sendSystemMessage(Component.literal("§cРюкзаки нельзя вешать на стойку для брони!"));
            }
            return;
        }

        if (held.getItem() instanceof HoodedCloakItem) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (!player.level().isClientSide() && !stand.getData(ModAttachments.STAND_CLOAK)) {
                stand.setData(ModAttachments.STAND_CLOAK, true);
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                stand.level().playSound(null, stand.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
                PacketDistributor.sendToPlayersTrackingEntity(stand, new SyncStandCloakPayload(stand.getId(), true));
            }
            return;
        }

        if (held.isEmpty() && stand.getData(ModAttachments.STAND_CLOAK) && player.isShiftKeyDown()) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (!player.level().isClientSide()) {
                stand.setData(ModAttachments.STAND_CLOAK, false);
                ItemStack cloakStack = new ItemStack(ModItemsAndBlocks.HOODED_CLOAK.get());
                if (!player.getInventory().add(cloakStack)) {
                    stand.spawnAtLocation(cloakStack);
                }
                stand.level().playSound(null, stand.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.BLOCKS, 1.0F, 0.9F);
                PacketDistributor.sendToPlayersTrackingEntity(stand, new SyncStandCloakPayload(stand.getId(), false));
            }
        }
    }

    @SubscribeEvent
    public static void onArmorStandDeath(LivingDropsEvent event) {
        if (event.getEntity() instanceof ArmorStand stand) {
            if (stand.getData(ModAttachments.STAND_CLOAK)) {
                stand.setData(ModAttachments.STAND_CLOAK, false);
                stand.spawnAtLocation(new ItemStack(ModItemsAndBlocks.HOODED_CLOAK.get()));
            }
        }
    }
}