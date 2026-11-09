package com.rpgcore.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.rpgcore.data.Gender;
import com.rpgcore.data.ModAttachments;
import com.rpgcore.data.PlayerAppearanceData;
import com.rpgcore.data.RaceType;
import com.rpgcore.network.SaveAppearancePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Quaternionf;

import java.lang.reflect.Field;
import java.util.List;

public class CharacterCreationScreen extends Screen {

    private static Field vehicleField;

    static {
        try {
            for (Field f : Entity.class.getDeclaredFields()) {
                if (f.getType() == Entity.class) {
                    f.setAccessible(true);
                    vehicleField = f;
                    break;
                }
            }
        } catch (Exception ignored) {
        }
    }

    private EditBox firstNameBox;
    private EditBox lastNameBox;

    private RaceType selectedRace = RaceType.HUMAN;
    private Gender selectedGender = Gender.MALE;
    private boolean appearanceExpanded = true;

    private int skinColorIdx = 0;
    private int faceVariant = 0; // 0..4 (5 уникальных лиц)
    private boolean orcTusksEnabled = true; // Кастомизация клыков ТОЛЬКО у Орков!
    private int eyeColorIdx = 0;
    private int hairStyle = 2;
    private int hairColorIdx = 0;
    private int beardStyle = 0;

    private String warningText = "";
    private String savedFirstName = "";
    private String savedLastName = "";

    private boolean confirmModalOpen = false;

    private float modelYaw = 180.0F;
    private float modelPitch = 0.0F;
    private float zoomFactor = 1.0F;
    private boolean draggingModel = false;

    public CharacterCreationScreen() {
        super(Component.literal("Кастомизация персонажа"));
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            PlayerAppearanceData cur = mc.player.getData(ModAttachments.APPEARANCE);
            this.selectedRace = cur.race();
            this.selectedGender = cur.gender();
            this.skinColorIdx = cur.skinColorIndex();
            int rawFace = cur.faceVariant();
            this.faceVariant = Math.floorMod(rawFace, 5);
            this.orcTusksEnabled = (rawFace < 5);
            this.eyeColorIdx = cur.eyeColorIndex();
            this.hairStyle = cur.hairStyle();
            this.hairColorIdx = cur.hairColorIndex();
            this.beardStyle = cur.beardStyle();
        }
    }

    private int getEncodedFaceVariant() {
        int baseFace = Math.floorMod(this.faceVariant, 5);
        if (this.selectedRace == RaceType.ORC && !this.orcTusksEnabled) {
            return baseFace + 5; // 5..9 означает "Клыки отключены" для Орка
        }
        return baseFace;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    protected void init() {
        if (firstNameBox != null) savedFirstName = firstNameBox.getValue();
        if (lastNameBox != null) savedLastName = lastNameBox.getValue();
        this.clearWidgets();

        if (this.confirmModalOpen) {
            int dialogW = 310;
            int dialogH = 114;
            int dx = (this.width - dialogW) / 2;
            int dy = (this.height - dialogH) / 2;

            int btnW = 130;
            this.addRenderableWidget(Button.builder(Component.literal("§a§lДа, подтвердить"), b -> {
                finalizeCharacterCreation();
            }).bounds(dx + 16, dy + dialogH - 28, btnW, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("§7Вернуться"), b -> {
                this.confirmModalOpen = false;
                init();
            }).bounds(dx + dialogW - btnW - 16, dy + dialogH - 28, btnW, 20).build());
            return;
        }

        int leftX = 6;
        int btnW = 148;
        int y = 18;

        this.firstNameBox = new EditBox(this.font, leftX, y, btnW, 18, Component.literal("Имя (РП)..."));
        this.firstNameBox.setHint(Component.literal("§7Имя (РП)..."));
        this.firstNameBox.setMaxLength(16);
        this.firstNameBox.setValue(savedFirstName);
        this.addRenderableWidget(this.firstNameBox);
        y += 22;

        this.lastNameBox = new EditBox(this.font, leftX, y, btnW, 18, Component.literal("Фамилия (РП)..."));
        this.lastNameBox.setHint(Component.literal("§7Фамилия (РП)..."));
        this.lastNameBox.setMaxLength(18);
        this.lastNameBox.setValue(savedLastName);
        this.addRenderableWidget(this.lastNameBox);
        y += 26;

        this.addRenderableWidget(Button.builder(
                Component.literal("Раса: " + selectedRace.getTitle()),
                b -> {
                    RaceType[] vals = RaceType.values();
                    selectedRace = vals[(selectedRace.ordinal() + 1) % vals.length];
                    skinColorIdx = 0;
                    eyeColorIdx = 0;
                    faceVariant = 0;
                    applyPreview();
                    init();
                }).bounds(leftX, y, btnW, 18).build());
        y += 21;

        this.addRenderableWidget(Button.builder(
                Component.literal("Пол: " + selectedGender.getTitle()),
                b -> {
                    selectedGender = (selectedGender == Gender.MALE) ? Gender.FEMALE : Gender.MALE;
                    hairStyle = 0;
                    applyPreview();
                    init();
                }).bounds(leftX, y, btnW, 18).build());
        y += 22;

        String arrow = appearanceExpanded ? "▼ " : "▶ ";
        this.addRenderableWidget(Button.builder(
                Component.literal(arrow + "Внешность"),
                b -> {
                    appearanceExpanded = !appearanceExpanded;
                    init();
                }).bounds(leftX, y, btnW, 18).build());
        y += 21;

        if (appearanceExpanded) {
            int subX = leftX + 4;
            int subW = btnW - 4;

            if (selectedRace != RaceType.DRAGONBORN) {
                int maxHair = (selectedGender == Gender.FEMALE) ? 5 : 7;
                String hairTitle = (selectedGender == Gender.FEMALE)
                        ? PlayerAppearanceData.FEMALE_HAIR_NAMES[Math.floorMod(hairStyle, 5)]
                        : PlayerAppearanceData.MALE_HAIR_NAMES[Math.floorMod(hairStyle, 7)];

                this.addRenderableWidget(Button.builder(
                        Component.literal("Прическа: " + hairTitle),
                        b -> {
                            hairStyle = (hairStyle + 1) % maxHair;
                            applyPreview();
                            init();
                        }).bounds(subX, y, subW, 17).build());
                y += 19;

                String hCol = PlayerAppearanceData.HAIR_COLOR_NAMES[Math.floorMod(hairColorIdx, PlayerAppearanceData.HAIR_COLOR_NAMES.length)];
                this.addRenderableWidget(Button.builder(
                        Component.literal("Волосы: " + hCol),
                        b -> {
                            hairColorIdx = (hairColorIdx + 1) % PlayerAppearanceData.HAIR_COLOR_NAMES.length;
                            applyPreview();
                            init();
                        }).bounds(subX, y, subW, 17).build());
                y += 19;

                if (selectedGender == Gender.MALE) {
                    String beardName = PlayerAppearanceData.BEARD_NAMES[Math.floorMod(beardStyle, 10)];
                    this.addRenderableWidget(Button.builder(
                            Component.literal("Борода: " + beardName),
                            b -> {
                                beardStyle = (beardStyle + 1) % 10;
                                applyPreview();
                                init();
                            }).bounds(subX, y, subW, 17).build());
                    y += 19;
                }
            }

            String skinTitle = selectedRace.getSkinName(selectedGender, skinColorIdx);
            this.addRenderableWidget(Button.builder(
                    Component.literal("Кожа: " + skinTitle),
                    b -> {
                        skinColorIdx = (skinColorIdx + 1) % selectedRace.getSkinColors().length;
                        applyPreview();
                        init();
                    }).bounds(subX, y, subW, 17).build());
            y += 19;

            // СТРОГО 5 УНИКАЛЬНЫХ ВАРИАЦИЙ ЛИЦ У КАЖДОЙ РАСЫ И ПОЛА!
            this.addRenderableWidget(Button.builder(
                    Component.literal("Лицо: Тип " + ((faceVariant % 5) + 1) + " / 5"),
                    b -> {
                        faceVariant = (faceVariant + 1) % 5;
                        applyPreview();
                        init();
                    }).bounds(subX, y, subW, 17).build());
            y += 19;

            String eyeTitle = selectedRace.getEyeColorName(eyeColorIdx);
            this.addRenderableWidget(Button.builder(
                    Component.literal("Глаза: " + eyeTitle),
                    b -> {
                        eyeColorIdx = (eyeColorIdx + 1) % selectedRace.getEyeColors().length;
                        applyPreview();
                        init();
                    }).bounds(subX, y, subW, 17).build());
            y += 19;

            // ТОЛЬКО У ОРКОВ: кастомизация клыков (добавить или убрать)
            if (selectedRace == RaceType.ORC) {
                String tusksLabel = orcTusksEnabled ? "Клыки: §aЕсть" : "Клыки: §cНет";
                this.addRenderableWidget(Button.builder(
                        Component.literal(tusksLabel),
                        b -> {
                            orcTusksEnabled = !orcTusksEnabled;
                            applyPreview();
                            init();
                        }).bounds(subX, y, subW, 17).build());
            }
        }

        int rightW = 190;
        int rightX = this.width - rightW;
        this.addRenderableWidget(Button.builder(
                Component.literal("Подтвердить"),
                b -> tryOpenConfirmDialog()
        ).bounds(rightX + 58, this.height - 24, rightW - 64, 18).build());

        applyPreview();
    }

    private void applyPreview() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            String f = (firstNameBox != null) ? firstNameBox.getValue() : savedFirstName;
            String l = (lastNameBox != null) ? lastNameBox.getValue() : savedLastName;
            PlayerAppearanceData preview = new PlayerAppearanceData(
                    false, f, l,
                    selectedRace,
                    selectedGender,
                    skinColorIdx,
                    getEncodedFaceVariant(),
                    eyeColorIdx,
                    hairStyle,
                    hairColorIdx,
                    beardStyle
            );
            mc.player.setData(ModAttachments.APPEARANCE, preview);
            mc.player.refreshDimensions();
        }
    }

    private void tryOpenConfirmDialog() {
        String f = firstNameBox.getValue().trim();
        String l = lastNameBox.getValue().trim();
        if (f.length() < 2 || l.length() < 2) {
            this.warningText = "§cВведите РП Имя и Фамилию (минимум по 2 буквы)!";
            return;
        }
        this.savedFirstName = f;
        this.savedLastName = l;
        this.warningText = "";
        this.confirmModalOpen = true;
        init();
    }

    private void finalizeCharacterCreation() {
        PlayerAppearanceData confirmed = new PlayerAppearanceData(
                true, savedFirstName.trim(), savedLastName.trim(), selectedRace, selectedGender,
                skinColorIdx, getEncodedFaceVariant(), eyeColorIdx, hairStyle, hairColorIdx, beardStyle
        );
        PacketDistributor.sendToServer(new SaveAppearancePayload(confirmed));
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        SkyrimThemeRenderer.renderBlurredDarkBackground(this, gui, partialTick);

        int leftPanelW = 162;
        gui.fill(0, 0, leftPanelW, this.height, 0xCC0D0E12);
        gui.fill(leftPanelW, 0, leftPanelW + 1, this.height, 0xFF3E4046);

        int rightPanelW = 190;
        int rightPanelX = this.width - rightPanelW;
        int rightPanelBottom = this.height - 30;
        gui.fill(rightPanelX, 0, this.width, rightPanelBottom, 0xCC0D0E12);
        gui.fill(rightPanelX - 1, 0, rightPanelX, rightPanelBottom, 0xFF3E4046);
        gui.fill(rightPanelX - 1, rightPanelBottom, this.width, rightPanelBottom + 1, 0xFF3E4046);

        int tx = rightPanelX + 10;
        int ty = 14;
        gui.drawString(this.font, "§l" + selectedRace.getTitle(), tx, ty, 0xFFFFFFFF, true);
        ty += 14;

        gui.drawString(this.font, "§7Здоровье: §c" + (int) selectedRace.getMaxHealth() + " HP", tx, ty, 0xFFFFFFFF, true);
        ty += 16;

        for (String line : selectedRace.getBuffs()) {
            if (!line.isEmpty()) {
                gui.drawString(this.font, line, tx, ty, 0xFF55FF55, true);
            }
            ty += 10;
        }
        ty += 8;

        for (String line : selectedRace.getDebuffs()) {
            gui.drawString(this.font, line, tx, ty, 0xFFFF5555, true);
            ty += 10;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            int centerX1 = leftPanelW + 8;
            int centerX2 = rightPanelX - 8;
            float baseScale = Math.min(62.0F, (this.height - 50) / 3.4F);
            float finalScale = baseScale * this.zoomFactor;

            renderInteractiveCharacterDoll(gui, centerX1, 12, centerX2, this.height - 28, finalScale, mc.player);

            String controlHint = "§8Зажать ЛКМ — вращать  |  Колесико — приблизить/отдалить";
            float hintScale = 0.72F;
            float hintW = this.font.width(controlHint) * hintScale;
            float hintX = (centerX1 + centerX2) / 2.0F - hintW / 2.0F;
            gui.pose().pushPose();
            gui.pose().translate(hintX, this.height - 12.0F, 0.0F);
            gui.pose().scale(hintScale, hintScale, 1.0F);
            gui.drawString(this.font, controlHint, 0, 0, 0xFF888888, false);
            gui.pose().popPose();
        }

        if (!warningText.isEmpty()) {
            gui.drawCenteredString(this.font, warningText, (leftPanelW + rightPanelX) / 2, this.height - 26, 0xFFFF5555);
        }

        if (this.confirmModalOpen) {
            gui.pose().pushPose();
            gui.pose().translate(0.0F, 0.0F, 500.0F);

            gui.fill(0, 0, this.width, this.height, 0xCC050608);

            int dialogW = 310;
            int dialogH = 114;
            int dx = (this.width - dialogW) / 2;
            int dy = (this.height - dialogH) / 2;

            SkyrimThemeRenderer.drawRpgFrame(gui, dx, dy, dialogW, dialogH, SkyrimThemeRenderer.HUD_BORDER_GOLD);

            gui.drawCenteredString(this.font, "§6§lПодтверждение выбора", this.width / 2, dy + 10, 0xFFFFFFFF);

            String msg = "Точно уверены в конечном варианте персонажа «§e" + savedFirstName + " " + savedLastName
                    + "§f» (§6" + selectedRace.getTitle() + "§f)? В дальнейшем расу и внешность будет невозможно изменить, только создавать нового персонажа.";
            List<FormattedCharSequence> lines = this.font.split(Component.literal(msg), dialogW - 24);
            int lineY = dy + 28;
            for (FormattedCharSequence line : lines) {
                gui.drawCenteredString(this.font, line, this.width / 2, lineY, 0xFFE0E0E0);
                lineY += 11;
            }

            super.render(gui, mouseX, mouseY, partialTick);
            gui.pose().popPose();
            return;
        }

        super.render(gui, mouseX, mouseY, partialTick);
    }

    private void renderInteractiveCharacterDoll(GuiGraphics gui, int x1, int y1, int x2, int y2,
                                                float scale, LivingEntity entity) {
        Entity savedVehicle = null;
        Pose savedPose = entity.getPose();

        float prevBodyRot = entity.yBodyRot;
        float prevBodyRotO = entity.yBodyRotO;
        float prevYRot = entity.getYRot();
        float prevYRotO = entity.yRotO;
        float prevXRot = entity.getXRot();
        float prevXRotO = entity.xRotO;
        float prevHeadRot = entity.yHeadRot;
        float prevHeadRotO = entity.yHeadRotO;

        ClientEvents.renderingInventoryDoll = true;
        gui.enableScissor(x1, y1, x2, y2);
        try {
            if (vehicleField != null) {
                savedVehicle = (Entity) vehicleField.get(entity);
                vehicleField.set(entity, null);
            }
            entity.setPose(Pose.STANDING);

            entity.yBodyRot = this.modelYaw;
            entity.yBodyRotO = this.modelYaw;
            entity.setYRot(this.modelYaw);
            entity.yRotO = this.modelYaw;
            entity.yHeadRot = this.modelYaw;
            entity.yHeadRotO = this.modelYaw;
            entity.setXRot(0.0F);
            entity.xRotO = 0.0F;

            float raceH = selectedRace.getBaseHeight() * selectedGender.getHeightScale();
            float raceW = selectedRace.getBaseWidth() * selectedGender.getWidthScale();

            Quaternionf poseRot = new Quaternionf()
                    .rotateZ((float) Math.PI)
                    .rotateX(this.modelPitch * Mth.DEG_TO_RAD);
            Quaternionf camOrientation = new Quaternionf().rotateX(-this.modelPitch * Mth.DEG_TO_RAD);

            gui.pose().pushPose();
            float footY = y2 - 28.0F;
            gui.pose().translate((x1 + x2) / 2.0D, footY, 350.0D);
            gui.pose().scale(scale * raceW, scale * raceH, -scale * raceW);
            gui.pose().mulPose(poseRot);

            Lighting.setupForEntityInInventory();
            EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            camOrientation.conjugate();
            dispatcher.overrideCameraOrientation(camOrientation);
            dispatcher.setRenderShadow(false);

            RenderSystem.runAsFancy(() -> {
                dispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, gui.pose(), gui.bufferSource(), 15728880);
            });
            gui.flush();
            dispatcher.setRenderShadow(true);
            gui.pose().popPose();
            Lighting.setupFor3DItems();
        } catch (Throwable ignored) {
        } finally {
            gui.disableScissor();
            try {
                if (vehicleField != null && savedVehicle != null) {
                    vehicleField.set(entity, savedVehicle);
                }
            } catch (Throwable ignored) {
            }
            entity.setPose(savedPose);
            entity.yBodyRot = prevBodyRot;
            entity.yBodyRotO = prevBodyRotO;
            entity.setYRot(prevYRot);
            entity.yRotO = prevYRotO;
            entity.setXRot(prevXRot);
            entity.xRotO = prevXRotO;
            entity.yHeadRot = prevHeadRot;
            entity.yHeadRotO = prevHeadRotO;
            ClientEvents.renderingInventoryDoll = false;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (this.confirmModalOpen) return false;
        int leftPanelW = 162;
        int rightPanelX = this.width - 190;
        if (button == 0 && mouseX > leftPanelW && mouseX < rightPanelX) {
            this.draggingModel = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.draggingModel = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!this.confirmModalOpen && this.draggingModel && button == 0) {
            this.modelYaw -= (float) dragX * 1.35F;
            this.modelPitch = Mth.clamp(this.modelPitch - (float) dragY * 0.65F, -22.0F, 22.0F);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.confirmModalOpen) return false;
        int leftPanelW = 162;
        int rightPanelX = this.width - 190;
        if (mouseX > leftPanelW && mouseX < rightPanelX) {
            this.zoomFactor = Mth.clamp(this.zoomFactor + (float) scrollY * 0.12F, 0.55F, 2.25F);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}