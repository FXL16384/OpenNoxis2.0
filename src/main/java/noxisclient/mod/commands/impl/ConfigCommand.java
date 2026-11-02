package noxisclient.mod.commands.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import noxisclient.Alien;
import noxisclient.core.Manager;
import noxisclient.core.impl.CommandManager;
import noxisclient.core.impl.ConfigManager;
import noxisclient.mod.commands.Command;
import noxisclient.mod.modules.impl.client.Fonts;

public class ConfigCommand extends Command {
   public ConfigCommand() {
      super("config", "[load/save] [config]");
   }

   @Override
   public void runCommand(String[] parameters) {
      if (parameters.length == 0) {
         this.sendUsage();
      } else {
         String var2 = parameters[0].toLowerCase(Locale.ROOT);
         switch (var2) {
            case "load":
               this.loadConfig(parameters);
               break;
            case "save":
               this.saveConfig(parameters);
               break;
            default:
               this.sendUsage();
         }
      }
   }

   public static void loadConfigByName(String configName) {
      String normalized = normalizeConfigName(configName);
      File configFile = resolveConfigFile(normalized);
      if (!configFile.exists()) {
         CommandManager.sendMessage("§c配置不存在 §f" + normalized);
      } else {
         File previous = ConfigManager.options;

         try {
            ConfigManager.options = configFile;
            Alien.CONFIG = new ConfigManager();
            Alien.CONFIG.load();
            ConfigManager.options = Manager.getFile("options.txt");
            Alien.save();
            Fonts.INSTANCE.refresh();
            CommandManager.sendMessage("§aLoaded config §f" + normalized);
         } finally {
            ConfigManager.options = previous;
         }
      }
   }

   public static void saveConfigByName(String configName) {
      if (configName != null && !configName.trim().isEmpty()) {
         String normalized = normalizeConfigName(configName);
         File folder = Manager.getFile("cfg");
         if (!folder.exists()) {
            folder.mkdirs();
         }

         File previous = ConfigManager.options;

         try {
            ConfigManager.options = new File(folder, normalized + ".cfg");
            Alien.save();
            CommandManager.sendMessage("§aSaved config §f" + normalized);
         } finally {
            ConfigManager.options = previous;
         }
      } else {
         CommandManager.sendMessage("§c配置名不能为");
      }
   }

   private void loadConfig(String[] parameters) {
      if (parameters.length < 2) {
         this.sendChatMessage("§c用法: " + Alien.getPrefix() + "config load <配置>");
         this.sendAvailableConfigs();
      } else {
         loadConfigByName(parameters[1]);
      }
   }

   private void saveConfig(String[] parameters) {
      if (parameters.length < 2) {
         this.sendChatMessage("§fSaving...");
         Alien.save();
         this.sendChatMessage("§aSaved current config");
      } else {
         saveConfigByName(parameters[1]);
      }
   }

   private File getConfigFolder() {
      return Manager.getFile("cfg");
   }

   private static File resolveConfigFile(String configName) {
      return new File(Manager.getFile("cfg"), configName.endsWith(".cfg") ? configName : configName + ".cfg");
   }

   private static String normalizeConfigName(String input) {
      String name = input.trim();
      if (name.toLowerCase(Locale.ROOT).endsWith(".cfg")) {
         name = name.substring(0, name.length() - 4);
      }

      return name;
   }

   private void sendAvailableConfigs() {
      ArrayList<String> configs = this.getConfigNames();
      if (configs.isEmpty()) {
         this.sendChatMessage("§7可用配置: §8");
      } else {
         this.sendChatMessage("§7可用配置: §f" + String.join("§7, §f", configs));
      }
   }

   private ArrayList<String> getConfigNames() {
      File folder = this.getConfigFolder();
      File[] files = folder.listFiles(filex -> filex.isFile() && filex.getName().toLowerCase(Locale.ROOT).endsWith(".cfg"));
      ArrayList<String> configs = new ArrayList<>();
      if (files == null) {
         return configs;
      } else {
         Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

         for (File file : files) {
            String name = file.getName();
            configs.add(name.substring(0, name.length() - 4));
         }

         return configs;
      }
   }

   @Override
   public String[] getAutocorrect(int count, List<String> seperated) {
      if (seperated.isEmpty()) {
         return null;
      } else if (count == 1) {
         return new String[]{"load", "save"};
      } else if (seperated.size() < 2) {
         return null;
      } else {
         String action = seperated.get(1).toLowerCase(Locale.ROOT);
         if (!"load".equals(action) && !"save".equals(action) && !action.startsWith("l") && !action.startsWith("s")) {
            return null;
         } else {
            String input = seperated.size() >= 3 ? seperated.get(seperated.size() - 1).toLowerCase(Locale.ROOT) : "";
            if (seperated.size() == 2) {
               input = "";
            }

            ArrayList<String> correct = new ArrayList<>();

            for (String name : this.getConfigNames()) {
               if (input.isEmpty() || name.toLowerCase(Locale.ROOT).startsWith(input)) {
                  correct.add(name);
               }
            }

            return correct.isEmpty() ? null : correct.toArray(new String[0]);
         }
      }
   }
}
