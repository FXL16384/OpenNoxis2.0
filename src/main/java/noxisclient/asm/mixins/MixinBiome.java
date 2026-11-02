package noxisclient.asm.mixins;

import java.awt.Color;
import net.minecraft.world.biome.Biome;
import noxisclient.mod.modules.impl.render.Ambience;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Biome.class})
public class MixinBiome {
   @Inject(
      method = {"getFogColor"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookGetFogColor(CallbackInfoReturnable<Integer> cir) {
      if (Ambience.INSTANCE.isOn() && Ambience.INSTANCE.dimensionColor.booleanValue) {
         Color color = Ambience.INSTANCE.dimensionColor.getValue();
         cir.setReturnValue(color.getRGB());
      }
   }
}
