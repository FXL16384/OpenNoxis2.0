package noxisclient.mod.commands.impl;

import java.util.List;
import noxisclient.mod.commands.Command;

public class MouseButtonCommand extends Command {
   public MouseButtonCommand() {
      super("mousebutton", "");
   }

   @Override
   public void runCommand(String[] args) {
      this.sendChatMessage("§fMouse button debug enabled. Press mouse buttons to see their values.");
   }

   @Override
   public String[] getAutocorrect(int i, List<String> list) {
      return new String[0];
   }
}
