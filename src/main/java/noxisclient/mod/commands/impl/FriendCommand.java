package noxisclient.mod.commands.impl;

import java.util.ArrayList;
import java.util.List;
import noxisclient.Alien;
import noxisclient.mod.commands.Command;
import noxisclient.mod.modules.impl.client.Socials;

public class FriendCommand extends Command {
   public FriendCommand() {
      super("friend", "[name/reset/list] | [add/remove] [name]");
   }

   @Override
   public void runCommand(String[] parameters) {
      if (parameters.length == 0) {
         this.sendUsage();
      } else {
         String wasFriend = parameters[0];
         switch (wasFriend) {
            case "reset":
               Alien.FRIEND.friendList.clear();
               Alien.FRIEND.save();
               this.sendChatMessage("§fFriends list got reset");
               return;
            case "list":
               if (Alien.FRIEND.friendList.isEmpty()) {
                  this.sendChatMessage("§fFriends list is empty");
                  return;
               } else {
                  StringBuilder friends = new StringBuilder();
                  int time = 0;
                  boolean first = true;
                  boolean start = true;

                  for (String name : Alien.FRIEND.friendList) {
                     if (!first) {
                        friends.append(", ");
                     }

                     friends.append(name);
                     first = false;
                     if (++time > 3) {
                        this.sendChatMessage((start ? "§eFriends §a" : "§a") + friends);
                        friends = new StringBuilder();
                        start = false;
                        first = true;
                        time = 0;
                     }
                  }

                  if (first) {
                     this.sendChatMessage("§a" + friends);
                  }

                  return;
               }
            case "add":
               if (parameters.length == 2) {
                  boolean wasFriendx = Alien.FRIEND.friendList.contains(parameters[1]);
                  Alien.FRIEND.add(parameters[1]);
                  Alien.ENEMY.remove(parameters[1]);
                  Alien.FRIEND.save();
                  Alien.ENEMY.save();
                  this.sendChatMessage(
                     "§f" + parameters[1] + (Alien.FRIEND.friendList.contains(parameters[1]) ? " §ahas been friended" : " §chas been unfriended")
                  );
                  if (Socials.INSTANCE != null) {
                     Socials.INSTANCE.notifyFriendAdded(parameters[1], wasFriendx);
                  }

                  return;
               }

               this.sendUsage();
               return;
            case "remove":
            case "del":
               if (parameters.length == 2) {
                  boolean wasFriendx = Alien.FRIEND.friendList.contains(parameters[1]);
                  Alien.FRIEND.remove(parameters[1]);
                  Alien.FRIEND.save();
                  this.sendChatMessage(
                     "§f" + parameters[1] + (Alien.FRIEND.friendList.contains(parameters[1]) ? " §ahas been friended" : " §chas been unfriended")
                  );
                  return;
               }

               this.sendUsage();
               return;
            default:
               if (parameters.length == 1) {
                  boolean wasFriendx = Alien.FRIEND.friendList.contains(parameters[0]);
                  Alien.FRIEND.friend(parameters[0]);
                  boolean isNowFriend = Alien.FRIEND.friendList.contains(parameters[0]);
                  if (isNowFriend) {
                     Alien.ENEMY.remove(parameters[0]);
                  }

                  Alien.FRIEND.save();
                  Alien.ENEMY.save();
                  this.sendChatMessage("§f" + parameters[0] + (isNowFriend ? " §ais friended" : " §cisn't friended"));
                  if (isNowFriend && Socials.INSTANCE != null) {
                     Socials.INSTANCE.notifyFriendAdded(parameters[0], wasFriendx);
                  }
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

         for (String x : List.of("add", "remove", "list", "reset")) {
            if (input.equalsIgnoreCase(Alien.getPrefix() + "friend") || x.toLowerCase().startsWith(input)) {
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
