package noxisclient.mod.commands.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import noxisclient.Alien;
import noxisclient.mod.commands.Command;
import noxisclient.mod.modules.Module;

public class BindsCommand extends Command {
   public BindsCommand() {
      super("binds", "");
   }

   @Override
   public void runCommand(String[] parameters) {
      ArrayList<String> list = new ArrayList<>();

      for (Module x : Alien.MODULE.getModules()) {
         if (x.getBindSetting().getValue() != -1) {
            list.add(("§f" + x.getDisplayName() + " §7- §r" + x.getBindSetting().getKeyString()));
         }
      }

      Iterator<String> temp = list.iterator();
      int i = 0;
      StringBuilder string = new StringBuilder();

      while (temp.hasNext()) {
         if (i == 0) {
            string = new StringBuilder(temp.next());
         } else {
            string.append("§7, ").append(temp.next());
         }

         if (++i >= 3 || !temp.hasNext()) {
            this.sendChatMessage(string.toString());
            i = 0;
         }
      }
   }

   @Override
   public String[] getAutocorrect(int count, List<String> seperated) {
      return null;
   }
}
