package noxisclient.mod.commands.impl;

import java.util.List;
import noxisclient.mod.commands.Command;

public class GcCommand extends Command {
   public GcCommand() {
      super("gc", "");
   }

   @Override
   public void runCommand(String[] parameters) {
      System.gc();
   }

   @Override
   public String[] getAutocorrect(int count, List<String> seperated) {
      return null;
   }
}
