package com.rpgcore.mixin;

import com.rpgcore.data.ModAttachments;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerCrawlMixin {

    @Inject(method = "updatePlayerPose", at = @At("TAIL"))
    private void rpgcore$controlCrawlingPoseStrictlyByKey(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (self.isSpectator() || self.isPassenger() || self.isFallFlying() || self.isSleeping()) {
            return;
        }

        boolean manualCrawl = self.getData(ModAttachments.CRAWLING);
        if (manualCrawl) {
            // Игрок сам нажал клавишу Z — включаем режим ползания
            self.setPose(Pose.SWIMMING);
        } else if (self.getPose() == Pose.SWIMMING && !self.isInWaterOrBubble() && !self.isInLava()) {
            // Если игрок НЕ нажимал Z и находится на суше, запрещаем ванильному Minecraft
            // автоматически укладывать персонажа при подходе к дырам в 1 блок или закрытии люка!
            self.setSwimming(false);
            self.setPose(self.isShiftKeyDown() ? Pose.CROUCHING : Pose.STANDING);
        }
    }
}