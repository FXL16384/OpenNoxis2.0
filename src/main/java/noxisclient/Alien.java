package noxisclient;

import noxisclient.api.events.eventbus.EventBus;
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

public class Alien {
   public static final String NAME = "Noxis 2.0";
   public static final String VERSION = "2.0";
   public static final EventBus EVENT_BUS = Vitality.EVENT_BUS;
   public static HoleManager HOLE = Vitality.HOLE;
   public static InteractionManager INTERACT = Vitality.INTERACT;
   public static InventoryManager INVENTORY = Vitality.INVENTORY;
   public static PlayerManager PLAYER = Vitality.PLAYER;
   public static TradeManager TRADE = Vitality.TRADE;
   public static CleanerManager CLEANER = Vitality.CLEANER;
   public static XrayManager XRAY = Vitality.XRAY;
   public static ModuleManager MODULE = Vitality.MODULE;
   public static CommandManager COMMAND = Vitality.COMMAND;
   public static ConfigManager CONFIG = Vitality.CONFIG;
   public static RotationManager ROTATION = Vitality.ROTATION;
   public static BreakManager BREAK = Vitality.BREAK;
   public static PopManager POP = Vitality.POP;
   public static FriendManager FRIEND = Vitality.FRIEND;
   public static EnemyManager ENEMY = Vitality.ENEMY;
   public static TimerManager TIMER = Vitality.TIMER;
   public static BlurManager BLUR = Vitality.BLUR;
   public static FPSManager FPS = Vitality.FPS;
   public static ServerManager SERVER = Vitality.SERVER;
   public static ThreadManager THREAD = Vitality.THREAD;
   public static ShaderManager SHADER = Vitality.SHADER;
   public static AntiAliasManager ANTI_ALIAS = Vitality.ANTI_ALIAS;
   public static boolean loaded = Vitality.loaded;
   public static long initTime = Vitality.initTime;

   public static String getPrefix() {
      return Vitality.getPrefix();
   }

   public static void save() {
      Vitality.save();
   }
}
