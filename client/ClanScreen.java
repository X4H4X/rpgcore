package com.rpgcore.client;

import com.rpgcore.data.ClanData;
import com.rpgcore.network.ClanActionPayload;
import com.rpgcore.network.SyncClanPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ClanScreen extends Screen {

    public static ClanData clientClan = null;
    public static List<ClanData.DirectInvite> clientMyInvites = new ArrayList<>();
    public static List<SyncClanPayload.OnlineCandidate> clientCandidates = new ArrayList<>();
    public static List<ClanData.AllyCompassPos> clientCompassAllies = new ArrayList<>();

    private EditBox nameField;
    private EditBox tagField;
    private String savedNameText = "";
    private String savedTagText = "";
    private int selectedColorIdx = 0;

    private int winLeft, winTop;
    private final int winWidth = 400;
    private final int winHeight = 224;

    public ClanScreen() {
        super(Component.literal("Гильдия"));
    }

    public static void updateFromNetwork(SyncClanPayload payload) {
        boolean hadClanBefore = (clientClan != null);
        int oldInvitesSize = clientMyInvites.size();
        int oldMembersSize = (clientClan != null) ? clientClan.getMembers().size() : -1;
        int oldCandidatesSize = clientCandidates.size();
        int oldPendingSize = (clientClan != null) ? clientClan.getPendingApprovals().size() : -1;
        boolean oldFf = (clientClan != null) && clientClan.isFriendlyFire();

        clientClan = payload.myClan().orElse(null);
        clientMyInvites = new ArrayList<>(payload.myInvites());
        clientCandidates = new ArrayList<>(payload.onlineCandidates());
        clientCompassAllies = new ArrayList<>(payload.alliesOnCompass());

        boolean hasClanNow = (clientClan != null);
        int newMembersSize = hasClanNow ? clientClan.getMembers().size() : -1;
        int newPendingSize = hasClanNow ? clientClan.getPendingApprovals().size() : -1;
        boolean newFf = hasClanNow && clientClan.isFriendlyFire();

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ClanScreen screen) {
            // ИСПРАВЛЕНО (image_97d1e9.png): НЕ пересоздаем виджеты каждые 1.5 сек, пока игрок вводит название или тег гильдии!
            // Перестраиваем кнопки только если реально изменился состав гильдии или список приглашений!
            if (hadClanBefore != hasClanNow
                    || oldInvitesSize != clientMyInvites.size()
                    || oldMembersSize != newMembersSize
                    || oldCandidatesSize != clientCandidates.size()
                    || oldPendingSize != newPendingSize
                    || oldFf != newFf) {
                screen.rebuildWidgets();
            }
        }
    }

    public boolean isEditingText() {
        return (this.nameField != null && this.nameField.isFocused())
                || (this.tagField != null && this.tagField.isFocused())
                || (this.getFocused() instanceof EditBox);
    }

    private String clipText(String text, int maxPixelWidth) {
        if (this.font.width(text) <= maxPixelWidth) {
            return text;
        }
        return this.font.plainSubstrByWidth(text, Math.max(12, maxPixelWidth - this.font.width("..."))) + "...";
    }

    // Отключаем ванильный шейдер размытия (Blur) из Minecraft 1.21.1!
    @Override
    public void renderBackground(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    protected void init() {
        if (this.nameField != null) {
            this.savedNameText = this.nameField.getValue();
        }
        if (this.tagField != null) {
            this.savedTagText = this.tagField.getValue();
        }

        super.init();
        this.winLeft = (this.width - this.winWidth) / 2;
        this.winTop = (this.height - this.winHeight) / 2;

        UUID myId = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getUUID() : null;

        if (clientClan == null) {
            this.nameField = new EditBox(this.font, winLeft + 16, winTop + 44, 155, 18, Component.literal("Название гильдии"));
            this.nameField.setMaxLength(20);
            this.nameField.setHint(Component.literal("§7Название гильдии"));
            this.nameField.setValue(this.savedNameText);
            this.nameField.setResponder(val -> this.savedNameText = val);
            this.addRenderableWidget(this.nameField);

            this.tagField = new EditBox(this.font, winLeft + 16, winTop + 82, 95, 18, Component.literal("Тег"));
            this.tagField.setMaxLength(4);
            this.tagField.setHint(Component.literal("§7ТЕГ (3-4 симв.)"));
            this.tagField.setValue(this.savedTagText);
            this.tagField.setResponder(val -> this.savedTagText = val);
            this.addRenderableWidget(this.tagField);

            for (int i = 0; i < ClanData.PALETTE_COLORS.length; i++) {
                final int idx = i;
                int cx = winLeft + 16 + (i % 6) * 24;
                int cy = winTop + 118 + (i / 6) * 22;
                this.addRenderableWidget(Button.builder(Component.literal(""), b -> {
                    this.selectedColorIdx = idx;
                }).bounds(cx, cy, 20, 18).build());
            }

            this.addRenderableWidget(Button.builder(Component.literal("§eСоздать гильдию"), b -> {
                String name = this.nameField.getValue().trim();
                String tag = this.tagField.getValue().trim();
                if (!name.isEmpty() && !tag.isEmpty()) {
                    PacketDistributor.sendToServer(new ClanActionPayload(
                            ClanActionPayload.ACTION_CREATE,
                            name + "|" + tag,
                            ClanData.PALETTE_COLORS[selectedColorIdx]
                    ));
                }
            }).bounds(winLeft + 16, winTop + 172, 155, 22).build());

            int invY = winTop + 44;
            for (int i = 0; i < Math.min(5, clientMyInvites.size()); i++) {
                ClanData.DirectInvite inv = clientMyInvites.get(i);
                int rowY = invY + i * 28;
                this.addRenderableWidget(Button.builder(Component.literal("§aПринять"), b -> {
                    PacketDistributor.sendToServer(new ClanActionPayload(
                            ClanActionPayload.ACTION_ACCEPT_INVITE, inv.clanId(), 0
                    ));
                }).bounds(winLeft + 286, rowY, 50, 16).build());

                this.addRenderableWidget(Button.builder(Component.literal("§cОтказ"), b -> {
                    PacketDistributor.sendToServer(new ClanActionPayload(
                            ClanActionPayload.ACTION_DECLINE_INVITE, inv.clanId(), 0
                    ));
                }).bounds(winLeft + 340, rowY, 46, 16).build());
            }
        } else {
            ClanData.ClanRank myRank = (myId != null) ? clientClan.getRankOf(myId) : null;
            boolean isFounder = (myRank == ClanData.ClanRank.FOUNDER);
            boolean isRightHand = (myRank == ClanData.ClanRank.RIGHT_HAND);

            if (isFounder) {
                String ffLabel = "Урон по своим: " + (clientClan.isFriendlyFire() ? "§cВКЛ" : "§aВЫКЛ");
                this.addRenderableWidget(Button.builder(Component.literal(ffLabel), b -> {
                    PacketDistributor.sendToServer(new ClanActionPayload(
                            ClanActionPayload.ACTION_TOGGLE_FF, "", 0
                    ));
                }).bounds(winLeft + 14, winTop + 28, 136, 18).build());

                this.addRenderableWidget(Button.builder(Component.literal("§cРаспустить гильдию"), b -> {
                    if (myId != null) {
                        PacketDistributor.sendToServer(new ClanActionPayload(
                                ClanActionPayload.ACTION_KICK_OR_LEAVE, myId.toString(), 0
                        ));
                    }
                }).bounds(winLeft + 266, winTop + 28, 120, 18).build());
            } else {
                this.addRenderableWidget(Button.builder(Component.literal("§cПокинуть гильдию"), b -> {
                    if (myId != null) {
                        PacketDistributor.sendToServer(new ClanActionPayload(
                                ClanActionPayload.ACTION_KICK_OR_LEAVE, myId.toString(), 0
                        ));
                    }
                }).bounds(winLeft + 266, winTop + 28, 120, 18).build());
            }

            int memStartY = winTop + 70;
            List<ClanData.MemberEntry> members = clientClan.getMembers();
            for (int i = 0; i < Math.min(6, members.size()); i++) {
                ClanData.MemberEntry m = members.get(i);
                int ry = memStartY + i * 22;
                if (isFounder && !Objects.equals(m.uuid(), myId)) {
                    String rhLabel = (m.rank() == ClanData.ClanRank.RIGHT_HAND) ? "§eСнять" : "§7Зам.";
                    this.addRenderableWidget(Button.builder(Component.literal(rhLabel), b -> {
                        PacketDistributor.sendToServer(new ClanActionPayload(
                                ClanActionPayload.ACTION_TOGGLE_RIGHT_HAND, m.uuid().toString(), 0
                        ));
                    }).bounds(winLeft + 156, ry - 2, 38, 15).build());

                    this.addRenderableWidget(Button.builder(Component.literal("§cИзгнать"), b -> {
                        PacketDistributor.sendToServer(new ClanActionPayload(
                                ClanActionPayload.ACTION_KICK_OR_LEAVE, m.uuid().toString(), 0
                        ));
                    }).bounds(winLeft + 196, ry - 2, 48, 15).build());
                }
            }

            if (isFounder || isRightHand) {
                int candY = winTop + 70;
                for (int i = 0; i < Math.min(4, clientCandidates.size()); i++) {
                    SyncClanPayload.OnlineCandidate cand = clientCandidates.get(i);
                    int ry = candY + i * 20;
                    this.addRenderableWidget(Button.builder(Component.literal("§a+"), b -> {
                        PacketDistributor.sendToServer(new ClanActionPayload(
                                ClanActionPayload.ACTION_INVITE_PLAYER, cand.uuid().toString(), 0
                        ));
                    }).bounds(winLeft + 364, ry - 2, 22, 15).build());
                }
            }

            if (isFounder && !clientClan.getPendingApprovals().isEmpty()) {
                int appY = winTop + 168;
                for (int i = 0; i < Math.min(2, clientClan.getPendingApprovals().size()); i++) {
                    ClanData.PendingApproval p = clientClan.getPendingApprovals().get(i);
                    int ry = appY + i * 20;
                    this.addRenderableWidget(Button.builder(Component.literal("§a✓"), b -> {
                        PacketDistributor.sendToServer(new ClanActionPayload(
                                ClanActionPayload.ACTION_APPROVE_PENDING, p.targetUuid().toString(), 0
                        ));
                    }).bounds(winLeft + 344, ry - 2, 18, 15).build());

                    this.addRenderableWidget(Button.builder(Component.literal("§c✕"), b -> {
                        PacketDistributor.sendToServer(new ClanActionPayload(
                                ClanActionPayload.ACTION_REJECT_PENDING, p.targetUuid().toString(), 0
                        ));
                    }).bounds(winLeft + 366, ry - 2, 18, 15).build());
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        SkyrimThemeRenderer.drawRpgFrame(gui, winLeft, winTop, winWidth, winHeight, SkyrimThemeRenderer.HUD_BORDER_GOLD);

        String hoveredFullText = null;

        if (clientClan == null) {
            gui.drawString(this.font, "§6§lСоздание гильдии", winLeft + 16, winTop + 14, 0xFFFFFFFF, true);
            gui.drawString(this.font, "§7Название гильдии:", winLeft + 16, winTop + 32, 0xFFCCCCCC, false);
            gui.drawString(this.font, "§7Тег гильдии:", winLeft + 16, winTop + 70, 0xFFCCCCCC, false);
            gui.drawString(this.font, "§7Цвет знамени и меток:", winLeft + 16, winTop + 105, 0xFFCCCCCC, false);

            gui.fill(winLeft + 195, winTop + 12, winLeft + 196, winTop + winHeight - 12, 0xFF363942);
            gui.drawString(this.font, "§6§lПриглашения в гильдии:", winLeft + 206, winTop + 14, 0xFFFFFFFF, true);

            if (clientMyInvites.isEmpty()) {
                gui.drawString(this.font, "§8Нет активных приглашений", winLeft + 206, winTop + 46, 0xFF888888, false);
            } else {
                int invY = winTop + 44;
                for (int i = 0; i < Math.min(5, clientMyInvites.size()); i++) {
                    ClanData.DirectInvite inv = clientMyInvites.get(i);
                    int rowY = invY + i * 28;
                    String fullTitle = "[" + inv.clanTag() + "] " + inv.clanName();
                    String clipped = clipText(fullTitle, 76);
                    gui.drawString(this.font, clipped, winLeft + 206, rowY + 4, inv.colorHex() | 0xFF000000, true);
                    if (mouseX >= winLeft + 206 && mouseX <= winLeft + 282 && mouseY >= rowY && mouseY <= rowY + 16) {
                        hoveredFullText = fullTitle + " §7(от " + inv.inviterName() + ")";
                    }
                }
            }
        } else {
            String header = "[" + clientClan.getClanTag() + "] " + clientClan.getClanName();
            gui.drawString(this.font, clipText(header, 240), winLeft + 14, winTop + 12, clientClan.getColorHex() | 0xFF000000, true);

            gui.fill(winLeft + 250, winTop + 52, winLeft + 251, winTop + winHeight - 12, 0xFF363942);

            gui.drawString(this.font, "§6Участники гильдии:", winLeft + 14, winTop + 54, 0xFFFFFFFF, true);
            gui.drawString(this.font, "§fПригласить:", winLeft + 258, winTop + 54, 0xFFFFFFFF, true);

            UUID myId = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getUUID() : null;
            boolean iAmFounder = (myId != null && clientClan.getRankOf(myId) == ClanData.ClanRank.FOUNDER);

            int memStartY = winTop + 70;
            List<ClanData.MemberEntry> members = clientClan.getMembers();
            for (int i = 0; i < Math.min(6, members.size()); i++) {
                ClanData.MemberEntry m = members.get(i);
                int ry = memStartY + i * 22;

                String roleBadge = "";
                if (m.rank() == ClanData.ClanRank.FOUNDER) {
                    roleBadge = " §6[Основатель]";
                } else if (m.rank() == ClanData.ClanRank.RIGHT_HAND) {
                    roleBadge = " §e[Зам.]";
                }

                boolean hasActionBtns = iAmFounder && !Objects.equals(m.uuid(), myId);
                int maxRowWidth = hasActionBtns ? 138 : 230;
                int availForName = Math.max(30, maxRowWidth - this.font.width(roleBadge));

                String clippedName = clipText(m.rpName(), availForName);
                String displayLine = clippedName + roleBadge;

                gui.drawString(this.font, displayLine, winLeft + 14, ry + 2, 0xFFE6E8EC, true);

                if (mouseX >= winLeft + 14 && mouseX <= winLeft + 14 + maxRowWidth && mouseY >= ry && mouseY <= ry + 14) {
                    if (!clippedName.equals(m.rpName())) {
                        hoveredFullText = m.rpName() + roleBadge;
                    }
                }
            }

            int candY = winTop + 70;
            if (clientCandidates.isEmpty()) {
                gui.drawString(this.font, "§8Нет игроков рядом", winLeft + 258, candY + 2, 0xFF888888, false);
            } else {
                for (int i = 0; i < Math.min(4, clientCandidates.size()); i++) {
                    SyncClanPayload.OnlineCandidate cand = clientCandidates.get(i);
                    int ry = candY + i * 20;
                    String clippedCand = clipText(cand.rpName(), 100);
                    gui.drawString(this.font, clippedCand, winLeft + 258, ry + 2, 0xFFCCCCCC, false);
                    if (mouseX >= winLeft + 258 && mouseX <= winLeft + 360 && mouseY >= ry && mouseY <= ry + 14 && !clippedCand.equals(cand.rpName())) {
                        hoveredFullText = cand.rpName();
                    }
                }
            }

            gui.drawString(this.font, "§6Ждут одобрения:", winLeft + 258, winTop + 154, 0xFFFFFFFF, true);
            if (clientClan.getPendingApprovals().isEmpty()) {
                gui.drawString(this.font, "§8Заявок нет", winLeft + 258, winTop + 170, 0xFF888888, false);
            } else {
                int appY = winTop + 168;
                for (int i = 0; i < Math.min(2, clientClan.getPendingApprovals().size()); i++) {
                    ClanData.PendingApproval p = clientClan.getPendingApprovals().get(i);
                    int ry = appY + i * 20;
                    String clippedTarget = clipText(p.targetName(), 82);
                    gui.drawString(this.font, clippedTarget, winLeft + 258, ry + 2, 0xFFE6E8EC, false);
                    if (mouseX >= winLeft + 258 && mouseX <= winLeft + 340 && mouseY >= ry && mouseY <= ry + 14) {
                        hoveredFullText = p.targetName() + " §7(пригл. " + p.invitedByName() + ")";
                    }
                }
            }
        }

        super.render(gui, mouseX, mouseY, partialTick);

        if (clientClan == null) {
            for (int i = 0; i < ClanData.PALETTE_COLORS.length; i++) {
                int cx = winLeft + 16 + (i % 6) * 24;
                int cy = winTop + 118 + (i / 6) * 22;
                gui.fill(cx + 2, cy + 2, cx + 18, cy + 16, ClanData.PALETTE_COLORS[i]);
                if (i == selectedColorIdx) {
                    gui.renderOutline(cx, cy, 20, 18, 0xFFFFD700);
                }
            }
        }

        if (hoveredFullText != null) {
            gui.renderTooltip(this.font, Component.literal(hoveredFullText), mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}