package noxisclient.api.utils.path;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import noxisclient.api.utils.Wrapper;
import noxisclient.mod.modules.impl.client.BaritoneModule;

public class BaritoneUtil implements Wrapper {
   public static boolean loaded;

   public static void gotoPos(BlockPos pos) {
      if (loaded) {
         BaritoneModule.gotoPos(pos);
      }
   }

   public static void forward() {
      if (loaded) {
         BaritoneModule.forward();
      }
   }

   public static void mine(Block block) {
      if (loaded) {
         BaritoneModule.mine(block);
      }
   }

   public static boolean isPathing() {
      return loaded ? BaritoneModule.isPathing() : false;
   }

   public static void cancelEverything() {
      if (loaded) {
         BaritoneModule.cancelEverything();
      }
   }

   public static boolean isActive() {
      return loaded ? BaritoneModule.isActive() : false;
   }

   static {
      for (Package pkg : Package.getPackages()) {
         if (pkg.getName().contains("baritone.api")) {
            loaded = true;
            break;
         }
      }
   }
}
