package noxisclient.mod.commands.impl;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Blocks;
import noxisclient.Vitality;
import noxisclient.core.impl.PlayerManager;
import noxisclient.mod.commands.Command;
import noxisclient.mod.gui.windows.WindowsScreen;
import noxisclient.mod.gui.windows.impl.ItemSelectWindow;

public class XrayCommand extends Command {
   public XrayCommand() {
      super("xray", "[\"\"/name/reset/clear/list] | [add/remove] [name]");
   }

   @Override
   public void runCommand(String[] parameters) {
      if (parameters.length == 0) {
         PlayerManager.screenToOpen = new WindowsScreen(new ItemSelectWindow(Vitality.XRAY));
      } else {
         String var2 = parameters[0];
         switch (var2) {
            case "reset":
               Vitality.XRAY.clear();
               Vitality.XRAY.add(Blocks.DIAMOND_ORE.getTranslationKey());
               Vitality.XRAY.add(Blocks.DEEPSLATE_DIAMOND_ORE.getTranslationKey());
               Vitality.XRAY.add(Blocks.GOLD_ORE.getTranslationKey());
               Vitality.XRAY.add(Blocks.NETHER_GOLD_ORE.getTranslationKey());
               Vitality.XRAY.add(Blocks.IRON_ORE.getTranslationKey());
               Vitality.XRAY.add(Blocks.DEEPSLATE_IRON_ORE.getTranslationKey());
               Vitality.XRAY.add(Blocks.REDSTONE_ORE.getTranslationKey());
               Vitality.XRAY.add(Blocks.EMERALD_ORE.getTranslationKey());
               Vitality.XRAY.add(Blocks.DEEPSLATE_EMERALD_ORE.getTranslationKey());
               Vitality.XRAY.add(Blocks.DEEPSLATE_REDSTONE_ORE.getTranslationKey());
               Vitality.XRAY.add(Blocks.COAL_ORE.getTranslationKey());
               Vitality.XRAY.add(Blocks.DEEPSLATE_COAL_ORE.getTranslationKey());
               Vitality.XRAY.add(Blocks.ANCIENT_DEBRIS.getTranslationKey());
               Vitality.XRAY.add(Blocks.NETHER_QUARTZ_ORE.getTranslationKey());
               Vitality.XRAY.add(Blocks.LAPIS_ORE.getTranslationKey());
               Vitality.XRAY.add(Blocks.DEEPSLATE_LAPIS_ORE.getTranslationKey());
               this.sendChatMessage("§fBlocks list got reset");
               return;
            case "clear":
               Vitality.XRAY.clear();
               this.sendChatMessage("§fBlocks list got clear");
               return;
            case "list":
               if (Vitality.XRAY.getList().isEmpty()) {
                  this.sendChatMessage("§fBlocks list is empty");
                  return;
               }

               for (String name : Vitality.XRAY.getList()) {
                  this.sendChatMessage("§a" + name);
               }

               return;
            case "add":
               if (parameters.length == 2) {
                  Vitality.XRAY.add(parameters[1]);
                  this.sendChatMessage("§f" + parameters[1] + (Vitality.XRAY.inWhitelist(parameters[1]) ? " §ahas been added" : " §chas been removed"));
                  return;
               }

               this.sendUsage();
               return;
            case "remove":
               if (parameters.length == 2) {
                  Vitality.XRAY.remove(parameters[1]);
                  this.sendChatMessage("§f" + parameters[1] + (Vitality.XRAY.inWhitelist(parameters[1]) ? " §ahas been added" : " §chas been removed"));
                  return;
               }

               this.sendUsage();
               return;
            default:
               if (parameters.length == 1) {
                  this.sendChatMessage("§f" + parameters[0] + (Vitality.XRAY.inWhitelist(parameters[0]) ? " §ais in whitelist" : " §cisn't in whitelist"));
               } else {
                  this.sendUsage();
               }
         }
      }
   }

   @Override
   public String[] getAutocorrect(int count, List<String> seperated) {
      if (count != 1) {
         return null;
      } else {
         String input = seperated.getLast().toLowerCase();
         ArrayList<String> correct = new ArrayList<>();

         for (String x : List.of("add", "remove", "list", "reset", "clear")) {
            if (input.equalsIgnoreCase(Vitality.getPrefix() + "xray") || x.toLowerCase().startsWith(input)) {
               correct.add(x);
            }
         }

         int numCmds = correct.size();
         String[] commands = new String[numCmds];
         int i = 0;

         for (String xx : correct) {
            commands[i++] = xx;
         }

         return commands;
      }
   }
}
