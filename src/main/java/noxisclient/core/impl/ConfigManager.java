package noxisclient.core.impl;

import com.google.common.base.Splitter;
import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import noxisclient.Alien;
import noxisclient.api.events.impl.Render2DEvent;
import noxisclient.core.Manager;
import noxisclient.mod.modules.HudElement;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.client.Socials;
import noxisclient.mod.modules.impl.client.TitleScreen;
import noxisclient.mod.modules.settings.Setting;
import noxisclient.mod.modules.settings.impl.BindSetting;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.ColorSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;
import noxisclient.mod.modules.settings.impl.StringSetting;
import org.apache.commons.io.IOUtils;

public class ConfigManager extends Manager {
   private static final String BUNDLED_DEFAULT_CONFIG = "/noxisclient/options.txt";
   public static File options = getFile("options.txt");
   private final Hashtable<String, String> settings = new Hashtable<>();
   private final List<ConfigManager.PendingHudElement> pendingHudElements = new ArrayList<>();
   private boolean pendingHudListenerRegistered;

   public ConfigManager() {
      this.read();
   }

   public void load() {
      for (Module module : Alien.MODULE.getModules()) {
         for (Setting setting : module.getSettings()) {
            String line = this.getLoadLine(module, setting);
            Objects.requireNonNull(setting);
            if (setting instanceof BooleanSetting s) {
               s.setValueWithoutTask(Alien.CONFIG.getBoolean(line, s.getDefaultValue()));
            } else if (setting instanceof SliderSetting s) {
               s.setValue((double)Alien.CONFIG.getFloat(line, (float)s.getDefaultValue()));
            } else if (setting instanceof BindSetting s) {
               s.setValue(Alien.CONFIG.getInt(line, s.getDefaultValue()));
               s.setHoldEnable(Alien.CONFIG.getBoolean(line + "_hold"));
            } else if (setting instanceof EnumSetting s) {
               s.loadSetting(Alien.CONFIG.getString(line));
            } else if (setting instanceof ColorSetting) {
               ColorSetting s = (ColorSetting)setting;
               s.setValue(new Color(Alien.CONFIG.getInt(line, s.getDefaultValue().getRGB()), true));
               s.setSync(Alien.CONFIG.getBoolean(line + "Sync", s.getDefaultSync()));
               if (s.injectBoolean) {
                  s.booleanValue = Alien.CONFIG.getBoolean(line + "Boolean", s.getDefaultBooleanValue());
               }
            } else if (setting instanceof StringSetting s) {
               s.setValue(Alien.CONFIG.getString(line, s.getDefaultValue()));
            }
         }

         this.loadHudElement(module);
         boolean enabledByDefault = module instanceof Socials || module instanceof TitleScreen;
         String stateLine = this.getStateLoadLine(module);
         module.setState(Alien.CONFIG.getBoolean(stateLine, enabledByDefault));
         if (module instanceof Socials socials) {
            socials.syncState();
         }
      }
   }

   private String getLoadLine(Module module, Setting setting) {
      String current = module.getName() + "_" + setting.getName();
      if (this.settings.containsKey(current)) {
         return current;
      } else if (module instanceof Socials) {
         String var10 = setting.getName();

         String legacy = switch (var10) {
            case "AddFriend" -> "FriendNotify_AddFriend";
            case "Key", "Drawn" -> "FriendNotify_" + setting.getName();
            default -> null;
         };
         return legacy != null && this.settings.containsKey(legacy) ? legacy : current;
      } else if ("Mainhand".equals(module.getName())) {
         String var9 = setting.getName();

         String legacy = switch (var9) {
            case "SwitchEat", "Sword", "Pickaxe", "Spear", "IgnoreSpear" -> "InteractTweaks_" + setting.getName();
            default -> null;
         };
         return legacy != null && this.settings.containsKey(legacy) ? legacy : current;
      } else if ("AutoSay".equals(module.getName())) {
         String var5 = setting.getName();

         String legacy = switch (var5) {
            case "AiChat" -> "AiChat_state";
            case "AutoEZ" -> "AutoEZ_state";
            case "AutoCommand" -> "AutoKit_state";
            case "EZType" -> "AutoEZ_Type";
            case "EZMessage" -> "AutoEZ_Message";
            case "EZRange" -> "AutoEZ_Range";
            case "RandomLength" -> "AutoEZ_Random";
            case "Command" -> "AutoKit_Command";
            default -> null;
         };
         return legacy != null && this.settings.containsKey(legacy) ? legacy : current;
      } else {
         return current;
      }
   }

   private String getStateLoadLine(Module module) {
      String current = module.getName() + "_state";
      if (this.settings.containsKey(current)) {
         return current;
      } else {
         return module instanceof Socials && this.settings.containsKey("FriendNotify_state") ? "FriendNotify_state" : current;
      }
   }

   public void save() {
      PrintWriter printwriter = null;

      try {
         printwriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(options), StandardCharsets.UTF_8));

         for (Module module : Alien.MODULE.getModules()) {
            for (Setting setting : module.getSettings()) {
               String line = module.getName() + "_" + setting.getName();
               Objects.requireNonNull(setting);
               if (setting instanceof BooleanSetting s) {
                  printwriter.println(line + ":" + s.getValue());
               } else if (setting instanceof SliderSetting s) {
                  printwriter.println(line + ":" + s.getValue());
               } else if (setting instanceof BindSetting s) {
                  printwriter.println(line + ":" + s.getValue());
                  printwriter.println(line + "_hold:" + s.isHoldEnable());
               } else if (setting instanceof EnumSetting s) {
                  printwriter.println(line + ":" + s.getValue().name());
               } else if (setting instanceof ColorSetting) {
                  ColorSetting s = (ColorSetting)setting;
                  printwriter.println(line + ":" + s.getValue().getRGB());
                  printwriter.println(line + "Sync:" + s.sync);
                  if (s.injectBoolean) {
                     printwriter.println(line + "Boolean:" + s.booleanValue);
                  }
               } else if (setting instanceof StringSetting s) {
                  printwriter.println(line + ":" + s.getValue());
               }
            }

            this.saveHudElement(printwriter, module);
            printwriter.println(module.getName() + "_state:" + module.isOn());
         }

         IOUtils.closeQuietly((Writer)printwriter);
      } catch (Exception var11) {
         var11.printStackTrace();
         System.out.println("[Vitality] Failed to save settings");
      } finally {
         IOUtils.closeQuietly((Writer)printwriter);
      }
   }

   private boolean isChatModule(Module module) {
      return "Chat".equals(module.getName()) && module.getCategory() == Module.Category.HUD;
   }

   private void loadHudElement(Module module) {
      if (module instanceof HudElement hud) {
         String prefix = module.getName() + "_hud";
         if (this.isChatModule(module) && !this.hasHudPositionData(prefix) && this.hasHudPositionData("ChatPosition_hud")) {
            prefix = "ChatPosition_hud";
         }

         if ("HUD".equals(module.getName()) && !this.hasHudPositionData(prefix)) {
            String legacyPrefix = this.findLegacyHudPrefix();
            if (legacyPrefix != null) {
               prefix = legacyPrefix;
            }
         }

         String xKey = prefix + "X";
         String yKey = prefix + "Y";
         String alignKey = prefix + "Align";
         String rightXKey = prefix + "RightX";
         if (this.settings.containsKey(xKey) || this.settings.containsKey(yKey) || this.settings.containsKey(alignKey) || this.settings.containsKey(rightXKey)) {
            Integer x = this.getOptionalInt(xKey);
            Integer y = this.getOptionalInt(yKey);
            Integer rightX = this.getOptionalInt(rightXKey);
            HudElement.AlignMode alignMode = hud.getAlignMode();
            String alignName = Alien.CONFIG.getString(alignKey, alignMode.name());

            try {
               alignMode = HudElement.AlignMode.valueOf(alignName);
            } catch (Exception var14) {
            }

            if (this.shouldDeferHudElementLoad(hud, alignMode)) {
               this.queuePendingHudElement(hud, x, rightX, y, alignMode);
               if (x != null && alignMode == HudElement.AlignMode.LEFT && hud.getAlignMode() == HudElement.AlignMode.LEFT) {
                  hud.setElementX(x);
               }

               if (y != null) {
                  hud.setElementY(y);
               }
            } else {
               hud.setAlignMode(alignMode);
               if (alignMode == HudElement.AlignMode.RIGHT && rightX != null) {
                  hud.setRightAnchorX(rightX);
               } else if (x != null) {
                  hud.setElementX(x);
               }

               if (y != null) {
                  hud.setElementY(y);
               }
            }
         }
      }
   }

   private boolean hasHudPositionData(String prefix) {
      return this.settings.containsKey(prefix + "X")
         || this.settings.containsKey(prefix + "Y")
         || this.settings.containsKey(prefix + "Align")
         || this.settings.containsKey(prefix + "RightX");
   }

   private String findLegacyHudPrefix() {
      String[] prefixes = new String[]{"FPS_hud", "Ping_hud", "TPS_hud", "Time_hud", "Packets_hud"};

      for (String prefix : prefixes) {
         if (this.hasHudPositionData(prefix)) {
            return prefix;
         }
      }

      return null;
   }

   private boolean shouldDeferHudElementLoad(HudElement hud, HudElement.AlignMode alignMode) {
      return alignMode == HudElement.AlignMode.RIGHT || hud.getAlignMode() == HudElement.AlignMode.RIGHT;
   }

   private void queuePendingHudElement(HudElement hud, Integer x, Integer rightX, Integer y, HudElement.AlignMode alignMode) {
      this.pendingHudElements.removeIf(pending -> pending.hud == hud);
      this.pendingHudElements.add(new ConfigManager.PendingHudElement(hud, x, rightX, y, alignMode));
      if (!this.pendingHudListenerRegistered) {
         Alien.EVENT_BUS.subscribe(this);
         this.pendingHudListenerRegistered = true;
      }
   }

   public void onRender2D(Render2DEvent event) {
      this.applyPendingHudElements();
   }

   private void applyPendingHudElements() {
      if (this.pendingHudElements.isEmpty()) {
         this.unregisterPendingHudListener();
      } else {
         List<ConfigManager.PendingHudElement> pendingElements = new ArrayList<>(this.pendingHudElements);
         this.pendingHudElements.clear();

         for (ConfigManager.PendingHudElement pending : pendingElements) {
            try {
               pending.hud.setAlignMode(pending.alignMode);
               if (pending.alignMode == HudElement.AlignMode.RIGHT && pending.rightX != null) {
                  pending.hud.setRightAnchorX(pending.rightX);
               } else if (pending.x != null) {
                  pending.hud.setElementX(pending.x);
               }

               if (pending.y != null) {
                  pending.hud.setElementY(pending.y);
               }
            } catch (Exception var5) {
               var5.printStackTrace();
               System.out.println("[Vitality] Failed to apply deferred HUD config");
            }
         }

         this.unregisterPendingHudListener();
      }
   }

   private void unregisterPendingHudListener() {
      if (this.pendingHudListenerRegistered) {
         Alien.EVENT_BUS.unsubscribe(this);
         this.pendingHudListenerRegistered = false;
      }
   }

   private Integer getOptionalInt(String setting) {
      String value = this.settings.get(setting);
      return value != null && this.isInteger(value) ? Integer.parseInt(value) : null;
   }

   private void saveHudElement(PrintWriter printwriter, Module module) {
      if (module instanceof HudElement hud) {
         String prefix = module.getName() + "_hud";
         printwriter.println(prefix + "X:" + hud.getElementX());
         printwriter.println(prefix + "Y:" + hud.getElementY());
         printwriter.println(prefix + "Align:" + hud.getAlignMode().name());
         printwriter.println(prefix + "RightX:" + hud.getRightAnchorX());
      }
   }

   public void read() {
      Splitter COLON_SPLITTER = Splitter.on(':');
      this.settings.clear();
      this.pendingHudElements.clear();
      this.unregisterPendingHudListener();

      try {
         File configFile = options;
         if (!configFile.exists()) {
            try (InputStream defaultConfig = ConfigManager.class.getResourceAsStream("/noxisclient/options.txt")) {
               if (defaultConfig != null) {
                  Files.copy(defaultConfig, configFile.toPath());
                  System.out.println("[Vitality] Copied bundled default config");
               }
            } catch (Exception var11) {
               System.out.println("[Vitality] Failed to copy bundled default config: " + var11.getMessage());
            }
         }

         if (!configFile.exists()) {
            File defaultConfigx = new File("noxisclient/options.txt");
            if (defaultConfigx.exists()) {
               try {
                  Files.copy(defaultConfigx.toPath(), configFile.toPath());
                  System.out.println("[Vitality] Copied default config from " + defaultConfigx.getAbsolutePath());
               } catch (Exception var8) {
                  System.out.println("[Vitality] Failed to copy default config: " + var8.getMessage());
               }
            }
         }

         if (!configFile.exists()) {
            return;
         }

         for (String s : IOUtils.readLines(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
            try {
               Iterator iterator = COLON_SPLITTER.limit(2).split(s).iterator();
               this.settings.put((String)iterator.next(), (String)iterator.next());
            } catch (Exception var7) {
               System.out.println("Skipping bad option: " + s);
            }
         }
      } catch (Exception var12) {
         var12.printStackTrace();
         System.out.println("[Vitality] Failed to load settings");
      }
   }

   public int getInt(String setting, int defaultValue) {
      String s = this.settings.get(setting);
      return s != null && this.isInteger(s) ? Integer.parseInt(s) : defaultValue;
   }

   public float getFloat(String setting, float defaultValue) {
      String s = this.settings.get(setting);
      return s != null && this.isFloat(s) ? Float.parseFloat(s) : defaultValue;
   }

   public boolean getBoolean(String setting) {
      String s = this.settings.get(setting);
      return Boolean.parseBoolean(s);
   }

   public boolean getBoolean(String setting, boolean defaultValue) {
      if (this.settings.get(setting) != null) {
         String s = this.settings.get(setting);
         return Boolean.parseBoolean(s);
      } else {
         return defaultValue;
      }
   }

   public String getString(String setting) {
      return this.settings.get(setting);
   }

   public String getString(String setting, String defaultValue) {
      return this.settings.get(setting) == null ? defaultValue : this.settings.get(setting);
   }

   public boolean isInteger(String str) {
      Pattern pattern = Pattern.compile("^[-+]?[\\d]*$");
      return pattern.matcher(str).matches();
   }

   public boolean isFloat(String str) {
      String pattern = "^[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?$";
      return str.matches(pattern);
   }

   private static final class PendingHudElement {
      private final HudElement hud;
      private final Integer x;
      private final Integer rightX;
      private final Integer y;
      private final HudElement.AlignMode alignMode;

      private PendingHudElement(HudElement hud, Integer x, Integer rightX, Integer y, HudElement.AlignMode alignMode) {
         this.hud = hud;
         this.x = x;
         this.rightX = rightX;
         this.y = y;
         this.alignMode = alignMode;
      }
   }
}
