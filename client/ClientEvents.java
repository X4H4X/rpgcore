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
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
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

    private static final Object2IntOpenHashMap<Item> PREV_INV_COUNTS = new Object2IntOpenHashMap<>();
    private static boolean invSnapshotInitialized = false;
    private static int invMuteTicks = 0;

    private static ClientLevel lastTrackedLevel = null;
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

            PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
            resetModelPivots(model);

            if (renderingInventoryDoll) {
                model.setAllVisible(true);
                return;
            }

            boolean isFirstPersonSelf = (player == mc.player)
                    && (CameraSystem.renderingFirstPersonBody || mc.options.getCameraType().isFirstPerson());

            PlayerAppearanceData data = player.getData(ModAttachments.APPEARANCE);
            float h = data.race().getBaseHeight() * data.gender().getHeightScale();
            float w = data.race().getBaseWidth() * data.gender().getWidthScale();

            if (isFirstPersonSelf) {
                model.head.xScale = 0.0F;
                model.head.yScale = 0.0F;
                model.head.zScale = 0.0F;
                model.hat.xScale = 0.0F;
                model.hat.yScale = 0.0F;
                model.hat.zScale = 0.0F;

                float pt = event.getPartialTick();
                float bodyYawRad = Mth.rotLerp(pt, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
                float backOffset = 0.24F * w;
                event.getPoseStack().translate(
                        Mth.sin(bodyYawRad) * backOffset,
                        0.0F,
                        -Mth.cos(bodyYawRad) * backOffset
                );
            }

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
                model.setAllVisible(false);
            } else {
                model.setAllVisible(true);
                if (isFirstPersonSelf) {
                    model.head.visible = false;
                    model.hat.visible = false;
                }
            }

            if (player.getPose() == Pose.SLEEPING) {
                // ИСПРАВЛЕНО (Screenshot_24.png): В кровати ванильный PlayerRenderer поворачивает модель горизонтально вдоль направления кровати!
                // Чтобы Дворф (h = 0.84, w = 1.12) не растягивался в длину на 1.12x как человек, применяем масштаб роста h строго вдоль оси кровати!
                net.minecraft.core.Direction bedDir = player.getBedOrientation();
                if (bedDir != null) {
                    boolean alongX = (bedDir.getAxis() == net.minecraft.core.Direction.Axis.X);
                    float scaleX = alongX ? h : w;
                    float scaleZ = alongX ? w : h;
                    event.getPoseStack().scale(scaleX, w * 0.92F, scaleZ);
                } else {
                    event.getPoseStack().scale(h, w * 0.92F, h);
                }
            } else if (!player.getData(ModAttachments.CRAWLING) || player.isInWater()) {
                event.getPoseStack().scale(w, h, w);
            }
        }
    }

    public static void resetModelPivots(PlayerModel<?> model) {
        model.head.setPos(0.0F, 0.0F, 0.0F);
        model.head.zRot = 0.0F;
        model.head.xScale = 1.0F;
        model.head.yScale = 1.0F;
        model.head.zScale = 1.0F;
        model.hat.copyFrom(model.head);
        model.hat.xScale = 1.0F;
        model.hat.yScale = 1.0F;
        model.hat.zScale = 1.0F;

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

        float neckY = 13.5F;
        float neckZ = -5.5F;
        float spineAngle = 1.32F + breath;

        model.head.setPos(0.0F, neckY, neckZ);
        model.head.xRot = 0.18F + breath * 1.5F;
        model.head.yRot = Mth.clamp(model.head.yRot, -0.6F, 0.6F);
        model.head.zRot = 0.0F;
        model.hat.copyFrom(model.head);

        model.body.setPos(0.0F, neckY, neckZ);
        model.body.xRot = spineAngle;
        model.body.yRot = 0.0F;
        model.body.zRot = 0.0F;
        model.jacket.copyFrom(model.body);

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

        float pelvisY = neckY + Mth.cos(spineAngle) * 11.2F;
        float pelvisZ = neckZ + Mth.sin(spineAngle) * 11.2F - 1.2F;

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
                || name.equals(VanillaGuiLayers.EXPERIENCE_LEVEL)
                || name.equals(VanillaGuiLayers.EFFECTS)) {
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

    /**
     * Считает полный баланс предметов игрока во ВСЕХ его собственных слотах:
     * - Инвентарь и быстрые слоты (0..35)
     * - Броня и левая рука
     * - Предмет, который сейчас зажат на курсоре мыши (containerMenu.getCarried() / inventoryMenu.getCarried())
     * - Все дополнительные слоты ExtraEquipmentData (ряд Дворфа, слоты Рюкзака, одежда)!
     * Благодаря этому перекладывание вещей между инвентарем, быстрыми слотами, рюкзаком и слотами Дворфа
     * НЕ меняет общую сумму и НИКОГДА не вызывает ложных всплывающих уведомлений!
     */
    private static Object2IntOpenHashMap<Item> computeTotalPlayerOwnedItems(Minecraft mc) {
        Object2IntOpenHashMap<Item> total = new Object2IntOpenHashMap<>();
        if (mc.player == null) return total;

        for (ItemStack s : mc.player.getInventory().items) {
            if (!s.isEmpty()) total.addTo(s.getItem(), s.getCount());
        }
        for (ItemStack s : mc.player.getInventory().armor) {
            if (!s.isEmpty()) total.addTo(s.getItem(), s.getCount());
        }
        for (ItemStack s : mc.player.getInventory().offhand) {
            if (!s.isEmpty()) total.addTo(s.getItem(), s.getCount());
        }

        // Предмет, взятый мышкой при перетаскивании между слотами
        ItemStack carried = mc.player.containerMenu != null ? mc.player.containerMenu.getCarried() : ItemStack.EMPTY;
        if (!carried.isEmpty()) {
            total.addTo(carried.getItem(), carried.getCount());
        }

        // Все слоты ExtraEquipmentData (включая доп. ряд Дворфа и слоты Рюкзака!)
        ExtraEquipmentData extraEq = mc.player.getData(ModAttachments.EXTRA_EQUIPMENT);
        for (int i = 0; i < ExtraEquipmentData.TOTAL_SLOTS; i++) {
            ItemStack s = extraEq.getStack(i);
            if (!s.isEmpty()) {
                total.addTo(s.getItem(), s.getCount());
            }
        }

        return total;
    }

    private static void trackInventoryPickupsAndDayBanner(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            invSnapshotInitialized = false;
            lastTrackedLevel = null;
            lastAnnouncedDayNumber = -1L;
            return;
        }

        if (mc.level != lastTrackedLevel) {
            lastTrackedLevel = mc.level;
            lastAnnouncedDayNumber = -1L;
            invSnapshotInitialized = false;
        }

        // Пока открыт ЛЮБОЙ экран инвентаря/контейнера (включая свой инвентарь, рюкзак, сундук или меню слотов) —
        // мы просто обновляем снимок предметов без показа всплывающих строк при перетаскивании мышкой!
        if (mc.screen instanceof AbstractContainerScreen<?>
                || mc.screen instanceof CharacterSlotSelectionScreen
                || mc.screen instanceof CharacterCreationScreen) {
            invMuteTicks = 6;
        }

        Object2IntOpenHashMap<Item> currentCounts = computeTotalPlayerOwnedItems(mc);

        if (invMuteTicks > 0) {
            invMuteTicks--;
            PREV_INV_COUNTS.clear();
            PREV_INV_COUNTS.putAll(currentCounts);
            invSnapshotInitialized = true;
        } else {
            if (invSnapshotInitialized) {
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
        }

        long dayTime = mc.level.getDayTime();
        long currentDayNumber = (dayTime / 24000L) + 1L;

        if (lastAnnouncedDayNumber == -1L || currentDayNumber < lastAnnouncedDayNumber) {
            lastAnnouncedDayNumber = currentDayNumber;
        } else if (currentDayNumber > lastAnnouncedDayNumber) {
            lastAnnouncedDayNumber = currentDayNumber;
            SkyrimThemeRenderer.triggerNewDayBanner((int) currentDayNumber);
            mc.player.playSound(SoundEvents.BELL_BLOCK, 0.70F, 0.85F);
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

    @SubscribeEvent
    public static void onRegisterBlockColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block event) {
        event.register(
                (state, level, pos, tintIndex) -> (level != null && pos != null)
                        ? net.minecraft.client.renderer.BiomeColors.getAverageGrassColor(level, pos)
                        : net.minecraft.world.level.GrassColor.getDefaultColor(),
                ModItemsAndBlocks.GRASS_SLAB.get()
        );
    }

    @SubscribeEvent
    public static void onRegisterItemColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        event.register(
                (stack, tintIndex) -> net.minecraft.world.level.GrassColor.getDefaultColor(),
                ModItemsAndBlocks.GRASS_SLAB.get()
        );
    }
}