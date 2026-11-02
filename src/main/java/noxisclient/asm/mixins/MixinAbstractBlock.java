package noxisclient.asm.mixins;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import noxisclient.Vitality;
import noxisclient.api.events.impl.AmbientOcclusionEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AbstractBlock.class})
public abstract class MixinAbstractBlock {
   @Inject(
      method = {"getAmbientOcclusionLightLevel"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onGetAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos, CallbackInfoReturnable<Float> info) {
      AmbientOcclusionEvent event = Vitality.EVENT_BUS.post(AmbientOcclusionEvent.get());
      if (event.lightLevel != -1.0F) {
         info.setReturnValue(event.lightLevel);
      }
   }
}
