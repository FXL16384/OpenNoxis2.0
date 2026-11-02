package noxisclient.mod.commands.impl;

import java.util.List;
import noxisclient.mod.commands.Command;
import noxisclient.mod.modules.impl.client.ClientSetting;

public class PrefixCommand extends Command {
   public PrefixCommand() {
      super("prefix", "[prefix]");
   }

   @Override
   public void runCommand(String[] parameters) {
      if (parameters.length == 0) {
         this.sendUsage();
      } else if (parameters[0].startsWith("/")) {
         this.sendChatMessage("§fPlease specify keyCodec valid §bprefix.");
      } else {
         ClientSetting.INSTANCE.prefix.setValue(parameters[0]);
         this.sendChatMessage("§bPrefix §fset to §e" + parameters[0]);
      }
   }

   @Override
   public String[] getAutocorrect(int count, List<String> seperated) {
      return null;
   }
}
