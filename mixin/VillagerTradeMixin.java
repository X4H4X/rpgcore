package com.rpgcore.mixin;

import com.rpgcore.data.ModAttachments;
import com.rpgcore.data.RaceType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerTradeMixin {

    @Inject(method = "updateSpecialPrices", at = @At("RETURN"))
    private void rpgcore$applyRaceTradePrices(Player player, CallbackInfo ci) {
        RaceType race = player.getData(ModAttachments.APPEARANCE).race();
        Villager self = (Villager) (Object) this;

        if (race == RaceType.HUMAN) {
            // Человек (Дипломат): скидка 15%
            for (MerchantOffer offer : self.getOffers()) {
                int base = offer.getBaseCostA().getCount();
                int discount = Math.max(1, Math.round(base * 0.15F));
                offer.addToSpecialPriceDiff(-discount);
            }
        } else if (race == RaceType.MUTANT) {
            // Мутант (Изгой): наценка 15%
            for (MerchantOffer offer : self.getOffers()) {
                int base = offer.getBaseCostA().getCount();
                int markup = Math.max(1, Math.round(base * 0.15F));
                offer.addToSpecialPriceDiff(markup);
            }
        }
    }
}