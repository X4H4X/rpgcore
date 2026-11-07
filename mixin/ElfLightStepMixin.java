package com.rpgcore.mixin;

import com.rpgcore.data.ModAttachments;
import com.rpgcore.data.RaceType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class ElfLightStepMixin {

    @Inject(method = "dampensVibrations", at = @At("HEAD"), cancellable = true)
    private void rpgcore$elfSilentStep(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player) {
            if (player.getData(ModAttachments.APPEARANCE).race() == RaceType.ELF) {
                cir.setReturnValue(true);
            }
        }
    }
}