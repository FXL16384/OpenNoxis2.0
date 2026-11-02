package noxisclient.asm.mixins;

import net.minecraft.block.BlockState;
import net.minecraft.block.CobwebBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import noxisclient.api.utils.Wrapper;
import noxisclient.mod.modules.impl.movement.FastWeb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({CobwebBlock.class})
public class MixinCobwebBlock {
   @Inject(
      at = {@At("HEAD")},
      method = {"onEntityCollision"},
      cancellable = true
   )
   private void onEntityCollisionHead(BlockState state, World world, BlockPos pos, Entity entity, CallbackInfo ci) {
      if (entity == Wrapper.mc.player) {
         FastWeb fastWeb = FastWeb.INSTANCE;
         if (fastWeb != null && fastWeb.isOn()) {
            if (Wrapper.mc.options.sneakKey.isPressed() || !fastWeb.onlySneak.getValue()) {
               FastWeb.Mode mode = fastWeb.mode.getValue();
               if (mode != FastWeb.Mode.Vanilla && mode != FastWeb.Mode.Strict) {
                  Vec3d originalMultiplier = new Vec3d(0.25, 0.05, 0.25);
                  Vec3d modifier = fastWeb.getWebSlowdownModifier(pos, originalMultiplier);
                  if (modifier == null) {
                     ci.cancel();
                  } else if (modifier != originalMultiplier) {
                     ci.cancel();
                     entity.slowMovement(state, modifier);
                  }
               }
            }
         }
      }
   }
}
