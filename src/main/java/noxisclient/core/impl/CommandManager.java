package noxisclient.core.impl;

import java.util.HashMap;
import net.minecraft.text.Text;
import noxisclient.Alien;
import noxisclient.api.interfaces.IChatHudHook;
import noxisclient.api.utils.Wrapper;
import noxisclient.mod.commands.Command;
import noxisclient.mod.commands.impl.BindCommand;
import noxisclient.mod.commands.impl.BindsCommand;
import noxisclient.mod.commands.impl.CccuuuInfoCommand;
import noxisclient.mod.commands.impl.CleanerCommand;
import noxisclient.mod.commands.impl.ClipCommand;
import noxisclient.mod.commands.impl.ConfigCommand;
import noxisclient.mod.commands.impl.EnemyCommand;
import noxisclient.mod.commands.impl.EsuCommand;
import noxisclient.mod.commands.impl.FakePlayerCommand;
import noxisclient.mod.commands.impl.FriendCommand;
import noxisclient.mod.commands.impl.GamemodeCommand;
import noxisclient.mod.commands.impl.GcCommand;
import noxisclient.mod.commands.impl.KitCommand;
import noxisclient.mod.commands.impl.PeekCommand;
import noxisclient.mod.commands.impl.PingCommand;
import noxisclient.mod.commands.impl.PrefixCommand;
import noxisclient.mod.commands.impl.RejoinCommand;
import noxisclient.mod.commands.impl.ReloadCommand;
import noxisclient.mod.commands.impl.TCommand;
import noxisclient.mod.commands.impl.TeleportCommand;
import noxisclient.mod.commands.impl.ToggleCommand;
import noxisclient.mod.commands.impl.TradeCommand;
import noxisclient.mod.commands.impl.WatermarkCommand;
import noxisclient.mod.commands.impl.XrayCommand;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.client.ClientSetting;
import noxisclient.mod.modules.impl.client.Socials;

public class CommandManager implements Wrapper {
   private final HashMap<String, Command> commands = new HashMap<>();

   public CommandManager() {
      this.init();
   }

   public void init() {
      this.registerCommand(new BindCommand());
      this.registerCommand(new BindsCommand());
      this.registerCommand(new CccuuuInfoCommand());
      this.registerCommand(new ConfigCommand());
      this.registerCommand(new CleanerCommand());
      this.registerCommand(new ClipCommand());
      this.registerCommand(new EsuCommand());
      this.registerCommand(new FakePlayerCommand());
      this.registerCommand(new FriendCommand());
      this.registerCommand(new EnemyCommand());
      this.registerCommand(new XrayCommand());
      this.registerCommand(new GamemodeCommand());
      this.registerCommand(new KitCommand());
      this.registerCommand(new KitCommand("kitmode"));
      this.registerCommand(new PingCommand());
      this.registerCommand(new PrefixCommand());
      this.registerCommand(new RejoinCommand());
      this.registerCommand(new ReloadCommand());
      this.registerCommand(new TeleportCommand());
      this.registerCommand(new TCommand());
      this.registerCommand(new ToggleCommand());
      this.registerCommand(new TradeCommand());
      this.registerCommand(new WatermarkCommand());
      this.registerCommand(new PeekCommand());
      this.registerCommand(new GcCommand());
   }

   public static void sendMessage(String message) {
      mc.execute(() -> {
         if (!Module.nullCheck()) {
            if (ClientSetting.INSTANCE.messageStyle.getValue() == ClientSetting.Style.Earth) {
               mc.inGameHud.getChatHud().addMessage(Text.of(message));
            } else if (ClientSetting.INSTANCE.messageStyle.getValue() == ClientSetting.Style.Moon) {
               mc.inGameHud.getChatHud().addMessage(Text.of("§f[§b" + ClientSetting.INSTANCE.hackName.getValue() + "§f] " + message));
            } else {
               ((IChatHudHook)mc.inGameHud.getChatHud()).alienClient$addMessage(Text.of(ClientSetting.INSTANCE.hackName.getValue() + "§f " + message));
            }
         }
      });
   }

   public static void sendMessageId(String message, int id) {
      mc.execute(
         () -> {
            if (!Module.nullCheck()) {
               if (ClientSetting.INSTANCE.messageStyle.getValue() == ClientSetting.Style.Earth) {
                  ((IChatHudHook)mc.inGameHud.getChatHud()).alienClient$addMessage(Text.of(message), id);
               } else if (ClientSetting.INSTANCE.messageStyle.getValue() == ClientSetting.Style.Moon) {
                  ((IChatHudHook)mc.inGameHud.getChatHud())
                     .alienClient$addMessage(Text.of("§f[§b" + ClientSetting.INSTANCE.hackName.getValue() + "§f] " + message), id);
               } else {
                  ((IChatHudHook)mc.inGameHud.getChatHud()).alienClient$addMessage(Text.of(ClientSetting.INSTANCE.hackName.getValue() + "§f " + message), id);
               }
            }
         }
      );
   }

   public static void sendChatMessageWidthIdNoSync(String message, int id) {
      mc.execute(() -> {
         if (!Module.nullCheck()) {
            ((IChatHudHook)mc.inGameHud.getChatHud()).alienClient$addMessageOutSync(Text.of("§f" + message), id);
         }
      });
   }

   private void registerCommand(Command command) {
      this.commands.put(command.getName(), command);
   }

   public Command getCommandBySyntax(String string) {
      return this.commands.get(this.resolveCommandName(string));
   }

   public HashMap<String, Command> getCommands() {
      return this.commands;
   }

   public void command(String[] commandIn) {
      Command command = this.commands.get(this.resolveCommandName(commandIn[0].substring(Alien.getPrefix().length()).toLowerCase()));
      if (command == null) {
         sendMessage("§4Invalid Command!");
      } else {
         String[] parameterList = new String[commandIn.length - 1];
         System.arraycopy(commandIn, 1, parameterList, 0, commandIn.length - 1);
         if (parameterList.length == 1 && parameterList[0].equals("help")) {
            command.sendUsage();
            return;
         }

         command.runCommand(parameterList);
      }
   }

   private String resolveCommandName(String commandName) {
      if ("f".equalsIgnoreCase(commandName)) {
         return Socials.INSTANCE != null && Socials.INSTANCE.isFriendsEnabled() ? "friend" : commandName;
      } else if (!"e".equalsIgnoreCase(commandName)) {
         return commandName;
      } else {
         return Socials.INSTANCE != null && Socials.INSTANCE.isEnemysEnabled() ? "enemy" : commandName;
      }
   }
}
