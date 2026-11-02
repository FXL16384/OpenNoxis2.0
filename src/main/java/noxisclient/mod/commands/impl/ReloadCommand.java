package noxisclient.mod.commands.impl;

import java.util.List;
import noxisclient.Vitality;
import noxisclient.core.impl.ConfigManager;
import noxisclient.mod.commands.Command;

public class ReloadCommand extends Command {
   public ReloadCommand() {
      super("reload", "");
   }

   @Override
   public void runCommand(String[] parameters) {
      this.sendChatMessage("§fReloading..");
      Vitality.CONFIG = new ConfigManager();
      Vitality.CONFIG.load();
      Vitality.CLEANER.read();
      Vitality.XRAY.read();
      Vitality.TRADE.read();
      Vitality.FRIEND.read();
   }

   @Override
   public String[] getAutocorrect(int count, List<String> seperated) {
      return null;
   }
}
