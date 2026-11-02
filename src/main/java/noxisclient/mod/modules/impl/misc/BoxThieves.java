package noxisclient.mod.modules.impl.misc;

import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import noxisclient.api.events.impl.UpdateEvent;
import noxisclient.api.utils.world.BlockUtil;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class BoxThieves extends Module {
   private final SliderSetting range = this.add(new SliderSetting("Range", 5.0, 1.0, 10.0, 0.1).setSuffix("m"));

   public BoxThieves() {
      super("BoxThieves", Module.Category.Misc);
      this.setChinese("潜影盒检");
   }

   public void onUpdate(UpdateEvent event) {
      if (mc.player != null && mc.world != null) {
         for (BlockPos pos : BlockUtil.getSphere((float)this.range.getValue())) {
            if (mc.world.getBlockState(pos).getBlock() == Blocks.SHULKER_BOX) {
               Direction clickSide = BlockUtil.getClickSide(pos);
               if (clickSide != null) {
                  BlockUtil.clickBlock(pos, clickSide, false);
                  break;
               }
            }
         }
      }
   }
}
