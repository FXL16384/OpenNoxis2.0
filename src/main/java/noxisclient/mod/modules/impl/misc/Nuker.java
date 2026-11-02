package noxisclient.mod.modules.impl.misc;

import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import noxisclient.Alien;
import noxisclient.api.events.impl.UpdateEvent;
import noxisclient.api.utils.world.BlockUtil;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.player.PacketMine;
import noxisclient.mod.modules.impl.render.PlaceRender;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class Nuker extends Module {
   private final SliderSetting range = this.add(new SliderSetting("Range", 4.0, 0.0, 8.0, 0.1));
   private final BooleanSetting down = this.add(new BooleanSetting("Down", false));
   private final BooleanSetting sand = this.add(new BooleanSetting("Sand", false));
   private final SliderSetting breaks = this.add(new SliderSetting("Breaks", 10, 0, 20, this.sand::getValue));

   public Nuker() {
      super("Nuker", Module.Category.Misc);
      this.setChinese("范围挖掘");
   }

   public void onUpdate(UpdateEvent event) {
      if (PacketMine.getBreakPos() == null || mc.world.isAir(PacketMine.getBreakPos())) {
         if (this.sand.getValue()) {
            if (!mc.player.isOnGround()) {
               return;
            }

            int elementCodec = 0;

            for (BlockPos sand : BlockUtil.getSphere(this.range.getValueFloat(), mc.player.getEyePos())) {
               Direction side;
               if ((Blocks.SAND == mc.world.getBlockState(sand).getBlock() || Blocks.RED_SAND == mc.world.getBlockState(sand).getBlock())
                  && (side = BlockUtil.getClickSideStrict(sand)) != null) {
                  PlaceRender.INSTANCE.create(sand);
                  Alien.ROTATION.snapAt(sand.toCenterPos());
                  sendSequencedPacket(id -> new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, sand, side, id));
                  sendSequencedPacket(id -> new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, sand, side, id));
                  sendSequencedPacket(id -> new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, sand, side, id));
                  Alien.ROTATION.snapBack();
                  if ((double)(++elementCodec) >= this.breaks.getValue()) {
                     return;
                  }
               }
            }
         } else {
            BlockPos pos = this.getBlock();
            if (pos != null) {
               PacketMine.INSTANCE.mine(pos);
            }
         }
      }
   }

   private BlockPos getBlock() {
      BlockPos down = null;

      for (BlockPos pos : BlockUtil.getSphere(this.range.getValueFloat(), mc.player.getEyePos())) {
         if (!mc.world.isAir(pos) && !PacketMine.unbreakable(pos) && BlockUtil.getClickSideStrict(pos) != null) {
            if (!((double)pos.getY() < mc.player.getY())) {
               return pos;
            }

            if (down == null && this.down.getValue()) {
               down = pos;
            }
         }
      }

      return down;
   }
}
