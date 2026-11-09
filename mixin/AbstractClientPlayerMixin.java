package com.rpgcore.mixin;

import com.rpgcore.client.CustomSkinManager;
import com.rpgcore.data.Gender;
import com.rpgcore.data.ModAttachments;
import com.rpgcore.data.PlayerAppearanceData;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {

    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void rpgcore$replaceSkinAndRemoveLauncherCape(CallbackInfoReturnable<PlayerSkin> cir) {
        AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
        PlayerAppearanceData data = self.getData(ModAttachments.APPEARANCE);
        ResourceLocation customSkin = CustomSkinManager.getOrCreateSkin(data);
        PlayerSkin.Model model = (data.gender() == Gender.FEMALE) ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE;

        // Передаем null вместо capeTexture и elytraTexture, чтобы плащ из лаунчера (TLauncher/Ely.by/Mojang) НИКОГДА не отображался!
        cir.setReturnValue(new PlayerSkin(
                customSkin,
                null,
                null,
                null,
                model,
                true
        ));
    }
}