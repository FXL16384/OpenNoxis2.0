package noxisclient.asm.mixins;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import noxisclient.mod.modules.impl.render.SpearFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Item.class})
public abstract class MixinItem {
   @Inject(
      method = {"getMaxUseTime"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void spearFix$getMaxUseTime(ItemStack stack, LivingEntity user, CallbackInfoReturnable<Integer> cir) {
      if (SpearFix.INSTANCE != null && SpearFix.INSTANCE.shouldUseAnimate(stack)) {
         cir.setReturnValue(72000);
      }
   }
}
