package noxisclient;

import java.io.File;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import noxisclient.api.events.eventbus.EventBus;
import noxisclient.api.events.impl.InitEvent;
import noxisclient.core.impl.AntiAliasManager;
import noxisclient.core.impl.BlurManager;
import noxisclient.core.impl.BreakManager;
import noxisclient.core.impl.CleanerManager;
import noxisclient.core.impl.CommandManager;
import noxisclient.core.impl.ConfigManager;
import noxisclient.core.impl.EnemyManager;
import noxisclient.core.impl.FPSManager;
import noxisclient.core.impl.FriendManager;
import noxisclient.core.impl.HoleManager;
import noxisclient.core.impl.InteractionManager;
import noxisclient.core.impl.InventoryManager;
import noxisclient.core.impl.ModuleManager;
import noxisclient.core.impl.PlayerManager;
import noxisclient.core.impl.PopManager;
import noxisclient.core.impl.RotationManager;
import noxisclient.core.impl.ServerManager;
import noxisclient.core.impl.ShaderManager;
import noxisclient.core.impl.ThreadManager;
import noxisclient.core.impl.TimerManager;
import noxisclient.core.impl.TradeManager;
import noxisclient.core.impl.XrayManager;
import noxisclient.license.GateRuntime;
import noxisclient.mod.modules.impl.client.ClientSetting;

public class Vitality implements ModInitializer {
   public static final String VERSION = "2.0";
   public static final String NAME = "Noxis 2.0";
   public static final String CONFIG_FOLDER = "Noxis";
   public static final EventBus EVENT_BUS = new EventBus();
   public static HoleManager HOLE;
   public static InteractionManager INTERACT;
   public static InventoryManager INVENTORY;
   public static PlayerManager PLAYER;
   public static TradeManager TRADE;
   public static CleanerManager CLEANER;
   public static XrayManager XRAY;
   public static ModuleManager MODULE;
   public static CommandManager COMMAND;
   public static ConfigManager CONFIG;
   public static RotationManager ROTATION;
   public static BreakManager BREAK;
   public static PopManager POP;
   public static FriendManager FRIEND;
   public static EnemyManager ENEMY;
   public static TimerManager TIMER;
   public static BlurManager BLUR;
   public static FPSManager FPS;
   public static ServerManager SERVER;
   public static ThreadManager THREAD;
   public static ShaderManager SHADER;
   public static AntiAliasManager ANTI_ALIAS;
   public static boolean loaded = false;
   public static long initTime;
   public static String userId;

   public static String getPrefix() {
      return ClientSetting.INSTANCE == null ? "." : ClientSetting.INSTANCE.prefix.getValue();
   }

   public static void save() {
      if (CONFIG != null) {
         CONFIG.save();
      }

      if (CLEANER != null) {
         CLEANER.save();
      }

      if (FRIEND != null) {
         FRIEND.save();
      }

      if (ENEMY != null) {
         ENEMY.save();
      }

      if (XRAY != null) {
         XRAY.save();
      }

      if (TRADE != null) {
         TRADE.save();
      }

      System.out.println("[Noxis] Saved");
   }

   private void register() {
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
         if (loaded) {
            save();
         }
      }));
   }

   @Override
   public void onInitialize() {
      GateRuntime.requireActive();
      this.register();
      this.initializeCore();
      GateRuntime.arm();
   }

   private synchronized void initializeCore() {
      if (!loaded) {
         MODULE = new ModuleManager();
         Alien.MODULE = MODULE;
         CONFIG = new ConfigManager();
         HOLE = new HoleManager();
         INVENTORY = new InventoryManager();
         INTERACT = new InteractionManager();
         COMMAND = new CommandManager();
         FRIEND = new FriendManager();
         ENEMY = new EnemyManager();
         XRAY = new XrayManager();
         CLEANER = new CleanerManager();
         TRADE = new TradeManager();
         ROTATION = new RotationManager();
         BREAK = new BreakManager();
         PLAYER = new PlayerManager();
         POP = new PopManager();
         TIMER = new TimerManager();
         BLUR = new BlurManager();
         FPS = new FPSManager();
         SERVER = new ServerManager();
         SHADER = new ShaderManager();
         ANTI_ALIAS = new AntiAliasManager();
         Alien.CONFIG = CONFIG;
         Alien.HOLE = HOLE;
         Alien.INVENTORY = INVENTORY;
         Alien.INTERACT = INTERACT;
         Alien.MODULE = MODULE;
         Alien.COMMAND = COMMAND;
         Alien.FRIEND = FRIEND;
         Alien.ENEMY = ENEMY;
         Alien.XRAY = XRAY;
         Alien.CLEANER = CLEANER;
         Alien.TRADE = TRADE;
         Alien.ROTATION = ROTATION;
         Alien.BREAK = BREAK;
         Alien.PLAYER = PLAYER;
         Alien.POP = POP;
         Alien.TIMER = TIMER;
         Alien.BLUR = BLUR;
         Alien.FPS = FPS;
         Alien.SERVER = SERVER;
         Alien.SHADER = SHADER;
         Alien.ANTI_ALIAS = ANTI_ALIAS;
         CONFIG.load();
         THREAD = new ThreadManager();
         Alien.THREAD = THREAD;
         Alien.initTime = initTime = System.currentTimeMillis();
         loaded = true;
         Alien.loaded = true;
         EVENT_BUS.post(new InitEvent());
         File folder = new File(MinecraftClient.getInstance().runDirectory.getPath() + File.separator + "Noxis".toLowerCase() + File.separator + "cfg");
         if (!folder.exists()) {
            folder.mkdirs();
         }
      }
   }

   private static void clearManagers() {
      HOLE = null;
      INTERACT = null;
      INVENTORY = null;
      PLAYER = null;
      TRADE = null;
      CLEANER = null;
      XRAY = null;
      MODULE = null;
      COMMAND = null;
      CONFIG = null;
      ROTATION = null;
      BREAK = null;
      POP = null;
      FRIEND = null;
      ENEMY = null;
      TIMER = null;
      BLUR = null;
      FPS = null;
      SERVER = null;
      THREAD = null;
      SHADER = null;
      ANTI_ALIAS = null;
      Alien.HOLE = null;
      Alien.INTERACT = null;
      Alien.INVENTORY = null;
      Alien.PLAYER = null;
      Alien.TRADE = null;
      Alien.CLEANER = null;
      Alien.XRAY = null;
      Alien.MODULE = null;
      Alien.COMMAND = null;
      Alien.CONFIG = null;
      Alien.ROTATION = null;
      Alien.BREAK = null;
      Alien.POP = null;
      Alien.FRIEND = null;
      Alien.ENEMY = null;
      Alien.TIMER = null;
      Alien.BLUR = null;
      Alien.FPS = null;
      Alien.SERVER = null;
      Alien.THREAD = null;
      Alien.SHADER = null;
      Alien.ANTI_ALIAS = null;
   }
}
