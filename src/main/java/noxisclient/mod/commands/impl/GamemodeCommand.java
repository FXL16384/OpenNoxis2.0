package noxisclient.mod.commands.impl;

import java.util.List;
import net.minecraft.world.GameMode;
import noxisclient.mod.commands.Command;

public class GamemodeCommand extends Command {
   public GamemodeCommand() {
      super("gamemode", "[gamemode]");
   }

   @Override
   public void runCommand(String[] parameters) {
      if (parameters.length == 0) {
         this.sendUsage();
      } else {
         String moduleName = parameters[0];
         if (moduleName.equalsIgnoreCase("survival")) {
            mc.interactionManager.setGameMode(GameMode.SURVIVAL);
         } else if (moduleName.equalsIgnoreCase("creative")) {
            mc.interactionManager.setGameMode(GameMode.CREATIVE);
         } else if (moduleName.equalsIgnoreCase("adventure")) {
            mc.interactionManager.setGameMode(GameMode.ADVENTURE);
         } else if (moduleName.equalsIgnoreCase("spectator")) {
            mc.interactionManager.setGameMode(GameMode.SPECTATOR);
         }
      }
   }

   @Override
   public String[] getAutocorrect(int count, List<String> seperated) {
      return count == 1 ? new String[]{"survival", "creative", "adventure", "spectator"} : null;
   }
}
