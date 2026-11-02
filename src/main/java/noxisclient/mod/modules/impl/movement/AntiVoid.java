package noxisclient.mod.modules.impl.movement;

import net.minecraft.block.Blocks;
import noxisclient.api.events.impl.UpdateEvent;
import noxisclient.api.utils.player.MovementUtil;
import noxisclient.api.utils.world.BlockPosX;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class AntiVoid extends Module {
   private final SliderSetting voidHeight = this.add(new SliderSetting("VoidHeight", -64.0, -64.0, 319.0, 1.0));
   private final SliderSetting height = this.add(new SliderSetting("Height", 100.0, -40.0, 256.0, 1.0));

   public AntiVoid() {
      super("AntiVoid", "Allows you to fly over void blocks", Module.Category.Movement);
      this.setChinese("反虚空");
   }

   public void onUpdate(UpdateEvent event) {
      boolean isVoid = true;

      for (int i = (int)mc.player.getY(); i > this.voidHeight.getValueInt() - 1; i--) {
         if (!mc.world.getBlockState(new BlockPosX(mc.player.getX(), (double)i, mc.player.getZ())).isAir()
            && mc.world.getBlockState(new BlockPosX(mc.player.getX(), (double)i, mc.player.getZ())).getBlock() != Blocks.VOID_AIR) {
            isVoid = false;
            break;
         }
      }

      if (mc.player.getY() < this.height.getValue() + this.voidHeight.getValue() && isVoid) {
         MovementUtil.setMotionY(0.0);
      }
   }
}
