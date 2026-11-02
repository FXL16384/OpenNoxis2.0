package noxisclient.mod.commands.impl;

import java.util.List;
import net.minecraft.item.ItemStack;
import noxisclient.mod.commands.Command;
import noxisclient.mod.modules.impl.misc.ShulkerViewer;

public class PeekCommand extends Command {
   private static final ItemStack[] ITEMS = new ItemStack[27];

   public PeekCommand() {
      super("peek", "");
   }

   @Override
   public void runCommand(String[] parameters) {
      ShulkerViewer.openContainer(mc.player.getMainHandStack(), ITEMS, true);
   }

   @Override
   public String[] getAutocorrect(int count, List<String> seperated) {
      return null;
   }
}
