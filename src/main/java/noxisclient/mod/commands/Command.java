package noxisclient.mod.commands;

import java.util.List;
import java.util.Objects;
import noxisclient.Vitality;
import noxisclient.api.utils.Wrapper;
import noxisclient.core.impl.CommandManager;

public abstract class Command implements Wrapper {
   protected final String name;
   protected final String syntax;

   public Command(String name, String syntax) {
      this.name = Objects.requireNonNull(name);
      this.syntax = Objects.requireNonNull(syntax);
   }

   public String getName() {
      return this.name;
   }

   public String getSyntax() {
      return this.syntax;
   }

   public abstract void runCommand(String[] var1);

   public abstract String[] getAutocorrect(int var1, List<String> var2);

   public void sendUsage() {
      this.sendChatMessage("§4Parameter error §r" + Vitality.getPrefix() + this.getName() + " §7" + this.getSyntax());
   }

   public void sendChatMessage(String message) {
      CommandManager.sendMessage(message);
   }
}
