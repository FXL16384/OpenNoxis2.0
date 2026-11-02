package noxisclient.asm.mixins;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import noxisclient.Vitality;
import noxisclient.api.utils.Wrapper;
import noxisclient.api.utils.combat.CombatUtil;
import noxisclient.mod.modules.impl.client.ClientSetting;
import noxisclient.mod.modules.impl.combat.SelfTrap;
import noxisclient.mod.modules.impl.player.InteractTweaks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({World.class})
public abstract class MixinWorld {
   @Inject(
      method = {"getBlockState"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void blockStateHook(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
      if (Wrapper.mc.world != null && Wrapper.mc.world.isInBuildLimit(pos)) {
         if (SelfTrap.airList.contains(pos)) {
            cir.setReturnValue(Blocks.AIR.getDefaultState());
            return;
         }

         if (!ClientSetting.INSTANCE.mioCompatible.getValue()) {
            boolean canUseCombatModify = CombatUtil.modifyThread == null || CombatUtil.modifyThread == Thread.currentThread();
            boolean terrainIgnore = canUseCombatModify && CombatUtil.terrainIgnore;
            BlockPos modifyPos = canUseCombatModify ? CombatUtil.modifyPos : null;
            BlockState modifyBlockState = canUseCombatModify ? CombatUtil.modifyBlockState : null;
            if (terrainIgnore || modifyPos != null) {
               WorldChunk worldChunk2 = Wrapper.mc.world.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
               BlockState tempState2 = worldChunk2.getBlockState(pos);
               if (modifyPos != null && modifyBlockState != null && pos.equals(modifyPos)) {
                  cir.setReturnValue(modifyBlockState);
                  return;
               }

               if (terrainIgnore) {
                  if (Vitality.HOLE.isHard(tempState2.getBlock())) {
                     return;
                  }

                  cir.setReturnValue(Blocks.AIR.getDefaultState());
               }
            } else if (InteractTweaks.INSTANCE.isActive
               && Wrapper.mc.world.getChunk(pos.getX() >> 4, pos.getZ() >> 4).getBlockState(pos).getBlock() == Blocks.BEDROCK) {
               cir.setReturnValue(Blocks.AIR.getDefaultState());
            }
         }
      }
   }
}
