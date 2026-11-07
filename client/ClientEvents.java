package com.rpgcore.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.rpgcore.RPGCore;
import com.rpgcore.data.ExtraEquipmentData;
import com.rpgcore.data.HoodedCloakItem;
import com.rpgcore.data.ModAttachments;
import com.rpgcore.data.ModItemsAndBlocks;
import com.rpgcore.data.PlayerAppearanceData;
import com.rpgcore.network.GiveUpPayload;
import com.rpgcore.network.ModNetworking;
import com.rpgcore.network.SlotActionPayload;
import com.rpgcore.network.ToggleCrawlPayload;
import com.rpgcore.network.ToggleHoodPayload;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = RPGCore.MODID, value = Dist.CLIENT)
public class ClientEvents {

    public static final String KEY_CATEGORY = "RPG Core — Управление модом";
    public static boolean renderingInventoryDoll = false;

    // Для отслеживания подобранных предметов в инвентарь (Пункт 6)
    private static final Object2IntOpenHashMap<Item> PREV_INV_COUNTS = new Object2IntOpenHashMap<>();
    private static boolean invSnapshotInitialized = false;
    private static int invMuteTicks = 0;

    // Для отслеживания наступления 06:00 утра и показа плашки "ДЕНЬ X" (Пункт 7)
    private static long lastAnnouncedDayNumber = -1L;

    public static final KeyMapping CLAN_KEY = new KeyMapping(
            "Меню гильдии",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_I,
            KEY_CATEGORY
    );

    public static final KeyMapping MAP_KEY = new KeyMapping(
            "Открыть карту мира",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            KEY_CATEGORY
    );

    public static final KeyMapping CRAWL_KEY = new KeyMapping(
            "Лечь / Ползти на животе",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            KEY_CATEGORY
    );

    public static final KeyMapping HOOD_TOGGLE_KEY = new KeyMapping(
            "Надеть / Снять капюшон плаща",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            KEY_CATEGORY
    );

    public static final KeyMapping SHOULDER_SWITCH_KEY = new KeyMapping(
            "Смена плеча камеры (от 3-го лица)",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_BRACKET,
            KEY_CATEGORY
    );

    static {
        ModNetworking.clientOpenCreatorHook = () -> {
            Minecraft mc = Minecraft.getInstance();
            if (!(mc.screen instanceof CharacterCreationScreen)) {
                mc.setScreen(new CharacterCreationScreen());
            }
        };
        ModNetworking.clientClanSyncHook = ClanScreen::updateFromNetwork;
        ModNetworking.clientSlotsMenuHook = CharacterSlotSelectionScreen::handleSyncPacket;
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(CLAN_KEY);
        event.register(MAP_KEY);
        event.register(CRAWL_KEY);
        event.register(HOOD_TOGGLE_KEY);
        event.register(SHOULDER_SWITCH_KEY);
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skinType : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skinType);
            if (renderer != null) {
                renderer.addLayer(new RaceExtraLayer(renderer));
            }
        }
        ArmorStandRenderer standRenderer = event.getRenderer(EntityType.ARMOR_STAND);
        if (standRenderer != null) {
            standRenderer.addLayer(new ArmorStandApparelLayer(standRenderer));
        }
    }

    @SubscribeEvent
    public static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModItemsAndBlocks.PLAYER_CORPSE.get(), PlayerCorpseRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModItemsAndBlocks.CORPSE_MENU.get(), CorpseLootScreen::new);
    }

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (event.getEntity() instanceof AbstractClientPlayer player) {
            if (renderingInventoryDoll || Minecraft.getInstance().screen instanceof CharacterSlotSelectionScreen) {
                event.setCanRender(net.neoforged.neoforge.common.util.TriState.FALSE);
                return;
            }
            PlayerAppearanceData data = player.getData(ModAttachments.APPEARANCE);
            String rpgName = (data.firstName() + " " + data.lastName()).trim();
            String baseName = rpgName.isEmpty() ? player.getGameProfile().getName() : rpgName;

            int downedTicks = player.getData(ModAttachments.DOWNED_TICKS);
            if (downedTicks > 0) {
                int secs = (downedTicks + 19) / 20;
                event.setContent(Component.literal("§c§l[Ранен: " + secs + "с] §e(Зажать ПКМ — Поднять) §f" + baseName));
            } else if (!rpgName.isEmpty()) {
                event.setContent(Component.literal(rpgName));
            }
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (CameraSystem.isFirstPersonBodyEnabled()) {
            if (event.getItemStack().is(Items.FILLED_MAP)) {
                return;
            }
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof OptionsScreen || event.getScreen() instanceof VideoSettingsScreen) {
            int btnW = 160;
            int btnH = 20;
            int btnX = event.getScreen().width - btnW - 8;
            int btnY = 6;

            event.addListener(Button.builder(
                    getFirstPersonButtonLabel(),
                    btn -> {
                        CameraSystem.toggleFirstPersonBody();
                        btn.setMessage(getFirstPersonButtonLabel());
                    }
            ).bounds(btnX, btnY, btnW, btnH).build());
        }

        if (event.getScreen() instanceof PauseScreen pauseScreen) {
            Button disconnectBtn = null;
            int maxY = -1;
            for (var listener : pauseScreen.children()) {
                if (listener instanceof Button btn && btn.getWidth() >= 190 && btn.getY() > maxY) {
                    maxY = btn.getY();
                    disconnectBtn = btn;
                }
            }
            if (disconnectBtn != null) {
                int bx = disconnectBtn.getX();
                int by = disconnectBtn.getY();
                int bw = disconnectBtn.getWidth();
                int bh = disconnectBtn.getHeight();
                event.removeListener(disconnectBtn);
                event.addListener(Button.builder(Component.literal("§eМеню персонажей (Выход)"), b -> {
                    PacketDistributor.sendToServer(new SlotActionPayload(
                            SlotActionPayload.ACTION_REQUEST_OPEN_MENU, 0
                    ));
                }).bounds(bx, by, bw, bh).build());
            }
        }
    }

    @SubscribeEvent
    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof WorldMapScreen && MAP_KEY.matches(event.getKeyCode(), event.getScanCode())) {
            mc.setScreen(null);
            event.setCanceled(true);
        } else if (mc.screen instanceof ClanScreen s && !s.isEditingText() && CLAN_KEY.matches(event.getKeyCode(), event.getScanCode())) {
            mc.setScreen(null);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.screen == null && event.getAction() == GLFW.GLFW_PRESS) {
            if (event.getKey() == GLFW.GLFW_KEY_T && mc.player.getData(ModAttachments.DOWNED_TICKS) > 0) {
                PacketDistributor.sendToServer(new GiveUpPayload());
            }
        }
    }

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getData(ModAttachments.DOWNED_TICKS) > 0
                && event.getNewScreen() instanceof net.minecraft.client.gui.screens.ChatScreen) {
            if (InputConstants.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_T)) {
                event.setCanceled(true);
            }
        }
    }

    private static Component getFirstPersonButtonLabel() {
        boolean on = CameraSystem.isFirstPersonBodyEnabled();
        return Component.literal("Тело от 1-го лица: " + (on ? "§aВКЛ" : "§cВЫКЛ"));
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (event.getEntity() instanceof AbstractClientPlayer player) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof CharacterSlotSelectionScreen && player == mc.player && CharacterSlotSelectionScreen.isViewingEmptySlot()) {
                event.setCanceled(true);
                return;
            }

            event.getPoseStack().pushPose();

            // Сначала всегда сбрасываем кости общей модели к стандартным точкам!
            resetModelPivots(event.getRenderer().getModel());

            boolean firstPersonSelf = CameraSystem.renderingFirstPersonBody && player == mc.player;

            if (player.getData(ModAttachments.DOWNED_TICKS) > 0 || (player.isPassenger() && player.getVehicle() instanceof AbstractMinecart)) {
                event.getRenderer().getModel().setAllVisible(false);
            } else {
                event.getRenderer().getModel().setAllVisible(true);
                // ИСПРАВЛЕНО (Screenshot_16.png): При рендере тела от 1-го лица жестко скрываем голову и шляпу,
                // чтобы при опускании камеры вниз никогда не видеть лицо и глаза изнутри!
                if (firstPersonSelf) {
                    event.getRenderer().getModel().head.visible = false;
                    event.getRenderer().getModel().hat.visible = false;
                }
            }

            PlayerAppearanceData data = player.getData(ModAttachments.APPEARANCE);
            float h = data.race().getBaseHeight() * data.gender().getHeightScale();
            float w = data.race().getBaseWidth() * data.gender().getWidthScale();

            if (player.isPassenger()) {
                Entity vehicle = player.getVehicle();
                float hipScaleFix = -(h - 1.0F) * 0.64F;
                float vehicleBaseAdjust;

                if (vehicle instanceof Boat) {
                    vehicleBaseAdjust = -0.38F + hipScaleFix * 0.55F;
                } else if (vehicle instanceof AbstractMinecart) {
                    vehicleBaseAdjust = -0.44F + hipScaleFix * 0.65F;
                } else if (vehicle instanceof AbstractHorse) {
                    vehicleBaseAdjust = -0.18F + hipScaleFix * 0.85F;
                } else {
                    vehicleBaseAdjust = -0.32F + hipScaleFix * 0.60F;
                }

                event.getPoseStack().translate(0.0F, vehicleBaseAdjust, 0.0F);
            }

            if (player.getData(ModAttachments.DOWNED_TICKS) > 0 || (player.isPassenger() && player.getVehicle() instanceof AbstractMinecart)) {
                event.getRenderer().getModel().setAllVisible(false);
            } else {
                event.getRenderer().getModel().setAllVisible(true);
            }

            if (!player.getData(ModAttachments.CRAWLING) || player.isInWater()) {
                event.getPoseStack().scale(w, h, w);
            }
        }
    }

    /**
     * Сбрасывает смещения точек крепления костей модели после рендера раненого игрока,
     * чтобы у обычных стоящих игроков и кукол в меню кости всегда стояли на своих местах!
     */
    public static void resetModelPivots(PlayerModel<?> model) {
        model.head.setPos(0.0F, 0.0F, 0.0F);
        model.head.zRot = 0.0F;
        model.hat.copyFrom(model.head);

        model.body.setPos(0.0F, 0.0F, 0.0F);
        model.body.xRot = 0.0F;
        model.body.yRot = 0.0F;
        model.body.zRot = 0.0F;
        model.jacket.copyFrom(model.body);

        model.rightArm.setPos(-5.0F, 2.0F, 0.0F);
        model.leftArm.setPos(5.0F, 2.0F, 0.0F);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);

        model.rightLeg.setPos(-1.9F, 12.0F, 0.0F);
        model.leftLeg.setPos(1.9F, 12.0F, 0.0F);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);
    }

    /**
     * ПУНКТ 1 (Screenshot_15.jpg):
     * Идеально связанная геометрия позы НА ЧЕТВЕРЕНЬКАХ (All-Fours Crawl):
     * - Плечи и шея находятся спереди на высоте ладоней (Y = 13.5, Z = -5.5),
     * - Спина наклонена почти горизонтально назад (body.xRot = 1.32 рад ≈ 75°),
     * - Таз прикреплен СТРОГО к концу спины (Y = 13.5 + 12*cos(1.32) = 16.5, Z = -5.5 + 12*sin(1.32) = +6.1),
     *   поэтому туловище и ноги образуют единое целое без разрывов!
     * - Обе руки упираются вертикально вниз в землю (xRot = 0.0) и поочередно переставляются при ползании!
     */
    public static void applyBattleRoyaleDownedLimbs(AbstractClientPlayer player, PlayerModel<?> model, float limbSwing, float limbSwingAmount, float ageInTicks) {
        if (player.isPassenger() && player.getVehicle() instanceof AbstractMinecart) {
            resetModelPivots(model);
            model.rightLeg.xRot = -0.92F;
            model.leftLeg.xRot = -0.92F;
            model.rightLeg.yRot = 0.12F;
            model.leftLeg.yRot = -0.12F;
            model.rightLeg.z = -1.8F;
            model.leftLeg.z = -1.8F;
            model.rightLeg.y = 10.5F;
            model.leftLeg.y = 10.5F;
            model.rightPants.copyFrom(model.rightLeg);
            model.leftPants.copyFrom(model.leftLeg);
            return;
        }

        if (player.getData(ModAttachments.DOWNED_TICKS) <= 0) {
            return;
        }

        float crawlSwing = Mth.cos(limbSwing * 0.85F) * 0.38F * Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        float breath = Mth.sin(ageInTicks * 0.10F) * 0.03F;

        // Точка шеи/плеч спереди на высоте вытянутых в землю рук (земля в координатах модели = Y: 24.0)
        float neckY = 13.5F;
        float neckZ = -5.5F;
        float spineAngle = 1.32F + breath; // ~75 градусов наклон спины (почти горизонтально!)

        // 1. Голова прикреплена к шее спереди и смотрит вперед-вниз
        model.head.setPos(0.0F, neckY, neckZ);
        model.head.xRot = 0.18F + breath * 1.5F;
        model.head.yRot = Mth.clamp(model.head.yRot, -0.6F, 0.6F);
        model.head.zRot = 0.0F;
        model.hat.copyFrom(model.head);

        // 2. Горизонтальная спина от шеи (Y=13.5, Z=-5.5) назад к тазу
        model.body.setPos(0.0F, neckY, neckZ);
        model.body.xRot = spineAngle;
        model.body.yRot = 0.0F;
        model.body.zRot = 0.0F;
        model.jacket.copyFrom(model.body);

        // 3. Обе руки растут ровно из плеч (Y=14.0, Z=-4.8) и упираются ВЕРТИКАЛЬНО ВНИЗ в землю (до Y=24.0)!
        model.rightArm.setPos(-5.0F, neckY + 0.5F, neckZ + 0.7F);
        model.rightArm.xRot = -0.05F + crawlSwing;
        model.rightArm.yRot = 0.0F;
        model.rightArm.zRot = 0.06F;
        model.rightSleeve.copyFrom(model.rightArm);

        model.leftArm.setPos(5.0F, neckY + 0.5F, neckZ + 0.7F);
        model.leftArm.xRot = -0.05F - crawlSwing;
        model.leftArm.yRot = 0.0F;
        model.leftArm.zRot = -0.06F;
        model.leftSleeve.copyFrom(model.leftArm);

        // 4. Вычисляем точную точку таза в самом конце наклоненной спины длиной 11.5px, чтобы НЕ БЫЛО РАЗРЫВА!
        float pelvisY = neckY + Mth.cos(spineAngle) * 11.2F;
        float pelvisZ = neckZ + Mth.sin(spineAngle) * 11.2F - 1.2F;

        // Ноги растут из таза, опираются коленями в землю и отталкиваются назад (xRot ≈ 1.05 рад)
        model.rightLeg.setPos(-2.0F, pelvisY, pelvisZ);
        model.rightLeg.xRot = 1.05F - crawlSwing * 0.85F;
        model.rightLeg.yRot = 0.08F;
        model.rightLeg.zRot = 0.0F;
        model.rightPants.copyFrom(model.rightLeg);

        model.leftLeg.setPos(2.0F, pelvisY, pelvisZ);
        model.leftLeg.xRot = 1.05F + crawlSwing * 0.85F;
        model.leftLeg.yRot = -0.08F;
        model.leftLeg.zRot = 0.0F;
        model.leftPants.copyFrom(model.leftLeg);
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (event.getEntity() instanceof AbstractClientPlayer player) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof CharacterSlotSelectionScreen && player == mc.player && CharacterSlotSelectionScreen.isViewingEmptySlot()) {
                return;
            }
            resetModelPivots(event.getRenderer().getModel());
            event.getRenderer().getModel().setAllVisible(true);
            event.getPoseStack().popPose();
        }
    }

    @SubscribeEvent
    public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
        var name = event.getName();
        if (name.equals(VanillaGuiLayers.PLAYER_HEALTH)
                || name.equals(VanillaGuiLayers.FOOD_LEVEL)
                || name.equals(VanillaGuiLayers.ARMOR_LEVEL)
                || name.equals(VanillaGuiLayers.HOTBAR)
                || name.equals(VanillaGuiLayers.VEHICLE_HEALTH)
                || name.equals(VanillaGuiLayers.JUMP_METER)
                || name.equals(VanillaGuiLayers.EXPERIENCE_BAR)
                || name.equals(VanillaGuiLayers.EXPERIENCE_LEVEL)) {
            event.setCanceled(true);
            if (name.equals(VanillaGuiLayers.HOTBAR)) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && !mc.options.hideGui
                        && !(mc.screen instanceof CharacterCreationScreen)
                        && !(mc.screen instanceof CharacterSlotSelectionScreen)) {
                    SkyrimThemeRenderer.renderSkyrimHud(event.getGuiGraphics(), mc.player);
                }
            }
        }
    }

    private static void trackInventoryPickupsAndDayBanner(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            invSnapshotInitialized = false;
            lastAnnouncedDayNumber = -1L;
            return;
        }

        // Если открыто меню выбора слотов или кастомизации — временно глушим уведомления о загрузке инвентаря слота
        if (mc.screen instanceof CharacterSlotSelectionScreen || mc.screen instanceof CharacterCreationScreen) {
            invMuteTicks = 30;
            invSnapshotInitialized = false;
        }
        if (invMuteTicks > 0) {
            invMuteTicks--;
        }

        // 1. ПУНКТ 6: Проверяем изменения количества предметов в инвентаре
        Object2IntOpenHashMap<Item> currentCounts = new Object2IntOpenHashMap<>();
        for (ItemStack stack : mc.player.getInventory().items) {
            if (!stack.isEmpty()) {
                currentCounts.addTo(stack.getItem(), stack.getCount());
            }
        }
        for (ItemStack stack : mc.player.getInventory().offhand) {
            if (!stack.isEmpty()) {
                currentCounts.addTo(stack.getItem(), stack.getCount());
            }
        }

        if (invSnapshotInitialized && invMuteTicks <= 0) {
            for (Object2IntMap.Entry<Item> entry : currentCounts.object2IntEntrySet()) {
                Item item = entry.getKey();
                int newTotal = entry.getIntValue();
                int oldTotal = PREV_INV_COUNTS.getOrDefault(item, 0);
                if (newTotal > oldTotal) {
                    int added = newTotal - oldTotal;
                    SkyrimThemeRenderer.notifyItemPickedUp(new ItemStack(item), added, newTotal);
                }
            }
        }

        PREV_INV_COUNTS.clear();
        PREV_INV_COUNTS.putAll(currentCounts);
        invSnapshotInitialized = true;

        // 2. ПУНКТ 7: Проверяем наступление 06:00 утра по игровому времени (dayTime % 24000 == 0..35)
        long dayTime = mc.level.getDayTime();
        long currentDayNumber = (dayTime / 24000L) + 1L;
        long timeOfDay = dayTime % 24000L;

        if (lastAnnouncedDayNumber == -1L) {
            lastAnnouncedDayNumber = currentDayNumber;
        } else if (currentDayNumber > lastAnnouncedDayNumber && timeOfDay >= 0 && timeOfDay <= 60) {
            lastAnnouncedDayNumber = currentDayNumber;
            SkyrimThemeRenderer.triggerNewDayBanner((int) currentDayNumber);
            mc.player.playSound(SoundEvents.BELL_BLOCK, 0.65F, 0.85F);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        CharacterSlotSelectionScreen.checkPendingOpen(mc);
        trackInventoryPickupsAndDayBanner(mc);
        SkyrimThemeRenderer.tickHudNotifications();

        if (mc.player.getData(ModAttachments.DOWNED_TICKS) > 0) {
            mc.player.setSprinting(false);
            if (mc.player.input != null) {
                mc.player.input.shiftKeyDown = false;
                mc.player.input.jumping = false;
            }
        }

        WorldMapManager.tickLiveMapUpdates(mc.player);

        while (CLAN_KEY.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new ClanScreen());
            } else if (mc.screen instanceof ClanScreen) {
                mc.setScreen(null);
            }
        }

        while (MAP_KEY.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new WorldMapScreen());
            } else if (mc.screen instanceof WorldMapScreen) {
                mc.setScreen(null);
            }
        }

        while (CRAWL_KEY.consumeClick()) {
            if (mc.screen == null && mc.player.getData(ModAttachments.DOWNED_TICKS) <= 0) {
                boolean nextCrawl = !mc.player.getData(ModAttachments.CRAWLING);
                mc.player.setData(ModAttachments.CRAWLING, nextCrawl);
                mc.player.setPose(nextCrawl ? Pose.SWIMMING : Pose.STANDING);
                mc.player.refreshDimensions();
                PacketDistributor.sendToServer(new ToggleCrawlPayload(mc.player.getId(), nextCrawl));
            }
        }

        while (SHOULDER_SWITCH_KEY.consumeClick()) {
            if (mc.screen == null) {
                CameraSystem.toggleShoulder();
            }
        }

        while (HOOD_TOGGLE_KEY.consumeClick()) {
            if (mc.screen == null) {
                ItemStack cape = mc.player.getData(ModAttachments.EXTRA_EQUIPMENT).getStack(ExtraEquipmentData.SLOT_CAPE);
                if (!cape.isEmpty() && cape.getItem() instanceof HoodedCloakItem) {
                    boolean nextHood = !mc.player.getData(ModAttachments.HOOD_UP);
                    mc.player.setData(ModAttachments.HOOD_UP, nextHood);
                    PacketDistributor.sendToServer(new ToggleHoodPayload(mc.player.getId(), nextHood));
                }
            }
        }

        if (mc.player.getData(ModAttachments.CRAWLING) && mc.player.input.jumping && !mc.player.isInWater()) {
            mc.player.setData(ModAttachments.CRAWLING, false);
            PacketDistributor.sendToServer(new ToggleCrawlPayload(mc.player.getId(), false));
        }
    }
}