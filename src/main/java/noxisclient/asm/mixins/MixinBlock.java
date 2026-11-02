package noxisclient.asm.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemConvertible;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import noxisclient.mod.modules.impl.render.Xray;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Block.class})
public abstract class MixinBlock implements ItemConvertible {
   @Inject(
      at = {@At("HEAD")},
      method = {"getVelocityMultiplier()F"},
      cancellable = true
   )
   private void onGetVelocityMultiplier(CallbackInfoReturnable<Float> cir) {
   }

   @ModifyReturnValue(
      method = {"shouldDrawSide"},
      at = {@At("RETURN")}
   )
   private static boolean onShouldDrawSide(boolean original, BlockState state, BlockView world, BlockPos pos, Direction side, BlockPos blockPos) {
      Xray xray = Xray.INSTANCE;
      return xray.isOn() ? xray.modifyDrawSide(state, world, pos, side, original) : original;
   }
}
