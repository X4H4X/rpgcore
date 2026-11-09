package com.rpgcore.mixin;

import com.rpgcore.data.PeasantClothesItem;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.inventory.ArmorSlot")
public abstract class ArmorSlotMixin extends Slot {

    public ArmorSlotMixin() {
        super(null, 0, 0, 0);
    }

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void rpgcore$forbidClothesInArmorSlots(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!stack.isEmpty() && stack.getItem() instanceof PeasantClothesItem) {
            cir.setReturnValue(false);
        }
    }
}