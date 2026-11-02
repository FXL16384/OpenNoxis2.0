package noxisclient.mod.commands.impl;

import java.util.ArrayList;
import java.util.List;
import noxisclient.Alien;
import noxisclient.mod.commands.Command;

public class EnemyCommand extends Command {
   public EnemyCommand() {
      super("enemy", "[name/reset/list] | [add/remove] [name]");
   }

   @Override
   public void runCommand(String[] parameters) {
      if (parameters.length == 0) {
         this.sendUsage();
      } else {
         String isEnemy = parameters[0];
         switch (isEnemy) {
            case "reset":
               Alien.ENEMY.enemyList.clear();
               Alien.ENEMY.save();
               this.sendChatMessage("§fEnemys list got reset");
               return;
            case "list":
               this.printList();
               return;
            case "add":
               if (parameters.length != 2) {
                  this.sendUsage();
                  return;
               }

               Alien.ENEMY.add(parameters[1]);
               Alien.FRIEND.remove(parameters[1]);
               this.saveLists();
               this.sendChatMessage("§f" + parameters[1] + " §chas been marked as an enemy");
               return;
            case "remove":
            case "del":
               if (parameters.length != 2) {
                  this.sendUsage();
                  return;
               }

               Alien.ENEMY.remove(parameters[1]);
               Alien.ENEMY.save();
               this.sendChatMessage("§f" + parameters[1] + " §aisn't an enemy");
               return;
            default:
               if (parameters.length == 1) {
                  Alien.ENEMY.enemy(parameters[0]);
                  boolean isEnemyx = Alien.ENEMY.enemyList.contains(parameters[0]);
                  if (isEnemyx) {
                     Alien.FRIEND.remove(parameters[0]);
                  }

                  this.saveLists();
                  this.sendChatMessage("§f" + parameters[0] + (isEnemyx ? " §cis an enemy" : " §aisn't an enemy"));
               } else {
                  this.sendUsage();
               }
         }
      }
   }

   private void printList() {
      if (Alien.ENEMY.enemyList.isEmpty()) {
         this.sendChatMessage("§fEnemys list is empty");
      } else {
         StringBuilder line = new StringBuilder();
         int count = 0;
         boolean firstLine = true;

         for (String name : Alien.ENEMY.enemyList) {
            if (count > 0) {
               line.append(", ");
            }

            line.append(name);
            if (++count >= 4) {
               this.sendChatMessage((firstLine ? "§cEnemys " : "§c") + line);
               line = new StringBuilder();
               count = 0;
               firstLine = false;
            }
         }

         if (count > 0) {
            this.sendChatMessage((firstLine ? "§cEnemys " : "§c") + line);
         }
      }
   }

   private void saveLists() {
      Alien.ENEMY.save();
      Alien.FRIEND.save();
   }

   @Override
   public String[] getAutocorrect(int count, List<String> separated) {
      if (count != 1) {
         return null;
      } else {
         String input = separated.getLast().toLowerCase();
         ArrayList<String> matches = new ArrayList<>();

         for (String option : List.of("add", "remove", "list", "reset")) {
            if (input.equalsIgnoreCase(Alien.getPrefix() + "enemy") || option.startsWith(input)) {
               matches.add(option);
            }
         }

         return matches.toArray(String[]::new);
      }
   }
}
