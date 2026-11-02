package noxisclient.core.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffectInstance;
import noxisclient.Alien;
import noxisclient.api.events.impl.Render2DEvent;
import noxisclient.api.events.impl.Render3DEvent;
import noxisclient.api.utils.Wrapper;
import noxisclient.api.utils.render.Render2DUtil;
import noxisclient.api.utils.render.TextUtil;
import noxisclient.mod.Mod;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.client.AntiCheat;
import noxisclient.mod.modules.impl.client.ClickGui;
import noxisclient.mod.modules.impl.client.ClientSetting;
import noxisclient.mod.modules.impl.client.ColorsModule;
import noxisclient.mod.modules.impl.client.EcCache;
import noxisclient.mod.modules.impl.client.Fonts;
import noxisclient.mod.modules.impl.client.HudEditor;
import noxisclient.mod.modules.impl.client.IRC;
import noxisclient.mod.modules.impl.client.ItemsCounter;
import noxisclient.mod.modules.impl.client.Socials;
import noxisclient.mod.modules.impl.client.TextRadar;
import noxisclient.mod.modules.impl.client.TitleScreen;
import noxisclient.mod.modules.impl.combat.AntiCrawl;
import noxisclient.mod.modules.impl.combat.AntiPhase;
import noxisclient.mod.modules.impl.combat.AntiRegear;
import noxisclient.mod.modules.impl.combat.AntiTK;
import noxisclient.mod.modules.impl.combat.AntiWeak;
import noxisclient.mod.modules.impl.combat.Aura;
import noxisclient.mod.modules.impl.combat.AutoAnchor;
import noxisclient.mod.modules.impl.combat.AutoCrystal;
import noxisclient.mod.modules.impl.combat.AutoMine;
import noxisclient.mod.modules.impl.combat.AutoPush;
import noxisclient.mod.modules.impl.combat.AutoRegear;
import noxisclient.mod.modules.impl.combat.AutoTrap;
import noxisclient.mod.modules.impl.combat.AutoWeb;
import noxisclient.mod.modules.impl.combat.AutoXP;
import noxisclient.mod.modules.impl.combat.Blocker;
import noxisclient.mod.modules.impl.combat.Burrow;
import noxisclient.mod.modules.impl.combat.CevBreaker;
import noxisclient.mod.modules.impl.combat.Criticals;
import noxisclient.mod.modules.impl.combat.HoleFiller;
import noxisclient.mod.modules.impl.combat.Mainhand;
import noxisclient.mod.modules.impl.combat.Offhand;
import noxisclient.mod.modules.impl.combat.Panic;
import noxisclient.mod.modules.impl.combat.PistonCrystal;
import noxisclient.mod.modules.impl.combat.Quiver;
import noxisclient.mod.modules.impl.combat.SelfTrap;
import noxisclient.mod.modules.impl.combat.Surround;
import noxisclient.mod.modules.impl.exploit.AntiHunger;
import noxisclient.mod.modules.impl.exploit.AntiPacket;
import noxisclient.mod.modules.impl.exploit.Blink;
import noxisclient.mod.modules.impl.exploit.BowBomb;
import noxisclient.mod.modules.impl.exploit.ChorusControl;
import noxisclient.mod.modules.impl.exploit.ChunkESP;
import noxisclient.mod.modules.impl.exploit.Clip;
import noxisclient.mod.modules.impl.exploit.InfiniteTrident;
import noxisclient.mod.modules.impl.exploit.MaceSpoof;
import noxisclient.mod.modules.impl.exploit.NoResourcePack;
import noxisclient.mod.modules.impl.exploit.PacketControl;
import noxisclient.mod.modules.impl.exploit.Phase;
import noxisclient.mod.modules.impl.exploit.PingSpoof;
import noxisclient.mod.modules.impl.exploit.RocketExtend;
import noxisclient.mod.modules.impl.exploit.ServerLagger;
import noxisclient.mod.modules.impl.exploit.SkinFlicker;
import noxisclient.mod.modules.impl.exploit.TeleportLogger;
import noxisclient.mod.modules.impl.exploit.XCarry;
import noxisclient.mod.modules.impl.hud.ArmorHud;
import noxisclient.mod.modules.impl.hud.ArrayListHud;
import noxisclient.mod.modules.impl.hud.BackpackContents;
import noxisclient.mod.modules.impl.hud.ChatPosition;
import noxisclient.mod.modules.impl.hud.CoordsHud;
import noxisclient.mod.modules.impl.hud.DirectionHud;
import noxisclient.mod.modules.impl.hud.HUD;
import noxisclient.mod.modules.impl.hud.PlayerModel;
import noxisclient.mod.modules.impl.hud.PotionHud;
import noxisclient.mod.modules.impl.hud.ServerInfoHud;
import noxisclient.mod.modules.impl.hud.SpeedHud;
import noxisclient.mod.modules.impl.hud.Totem;
import noxisclient.mod.modules.impl.hud.WaterMarkHud;
import noxisclient.mod.modules.impl.hud.WelcomerHud;
import noxisclient.mod.modules.impl.misc.AutoEC;
import noxisclient.mod.modules.impl.misc.AutoLog;
import noxisclient.mod.modules.impl.misc.AutoReconnect;
import noxisclient.mod.modules.impl.misc.AutoSay;
import noxisclient.mod.modules.impl.misc.BedCrafter;
import noxisclient.mod.modules.impl.misc.BoxThieves;
import noxisclient.mod.modules.impl.misc.ChatAppend;
import noxisclient.mod.modules.impl.misc.ExtraTab;
import noxisclient.mod.modules.impl.misc.FakePlayer;
import noxisclient.mod.modules.impl.misc.KillEffect;
import noxisclient.mod.modules.impl.misc.LavaFiller;
import noxisclient.mod.modules.impl.misc.NoRotate;
import noxisclient.mod.modules.impl.misc.NoSound;
import noxisclient.mod.modules.impl.misc.NoTerrainScreen;
import noxisclient.mod.modules.impl.misc.Nuker;
import noxisclient.mod.modules.impl.misc.PacketLogger;
import noxisclient.mod.modules.impl.misc.PotionTracker;
import noxisclient.mod.modules.impl.misc.ShulkerViewer;
import noxisclient.mod.modules.impl.misc.Spammer;
import noxisclient.mod.modules.impl.misc.Tips;
import noxisclient.mod.modules.impl.movement.AntiVoid;
import noxisclient.mod.modules.impl.movement.AutoHole;
import noxisclient.mod.modules.impl.movement.AutoWalk;
import noxisclient.mod.modules.impl.movement.BlockStrafe;
import noxisclient.mod.modules.impl.movement.ElytraFly;
import noxisclient.mod.modules.impl.movement.EntityControl;
import noxisclient.mod.modules.impl.movement.FastFall;
import noxisclient.mod.modules.impl.movement.FastSwim;
import noxisclient.mod.modules.impl.movement.FastWeb;
import noxisclient.mod.modules.impl.movement.Flatten;
import noxisclient.mod.modules.impl.movement.Fly;
import noxisclient.mod.modules.impl.movement.HoleSnap;
import noxisclient.mod.modules.impl.movement.MovementSync;
import noxisclient.mod.modules.impl.movement.NoJumpDelay;
import noxisclient.mod.modules.impl.movement.NoSlow;
import noxisclient.mod.modules.impl.movement.PacketFly;
import noxisclient.mod.modules.impl.movement.SafeWalk;
import noxisclient.mod.modules.impl.movement.Speed;
import noxisclient.mod.modules.impl.movement.Sprint;
import noxisclient.mod.modules.impl.movement.Step;
import noxisclient.mod.modules.impl.movement.Strafe;
import noxisclient.mod.modules.impl.movement.TargetFollow;
import noxisclient.mod.modules.impl.movement.VClip;
import noxisclient.mod.modules.impl.movement.Velocity;
import noxisclient.mod.modules.impl.player.AirPlace;
import noxisclient.mod.modules.impl.player.AntiEffects;
import noxisclient.mod.modules.impl.player.AutoArmor;
import noxisclient.mod.modules.impl.player.AutoPotion;
import noxisclient.mod.modules.impl.player.AutoTool;
import noxisclient.mod.modules.impl.player.FireworkBoost;
import noxisclient.mod.modules.impl.player.FreeLook;
import noxisclient.mod.modules.impl.player.Freecam;
import noxisclient.mod.modules.impl.player.InteractTweaks;
import noxisclient.mod.modules.impl.player.Macros;
import noxisclient.mod.modules.impl.player.NoFall;
import noxisclient.mod.modules.impl.player.PacketEat;
import noxisclient.mod.modules.impl.player.PacketMine;
import noxisclient.mod.modules.impl.player.Replenish;
import noxisclient.mod.modules.impl.player.TimerModule;
import noxisclient.mod.modules.impl.player.Yaw;
import noxisclient.mod.modules.impl.render.Ambience;
import noxisclient.mod.modules.impl.render.AnimationsModule;
import noxisclient.mod.modules.impl.render.AspectRatio;
import noxisclient.mod.modules.impl.render.BreakESP;
import noxisclient.mod.modules.impl.render.CameraClip;
import noxisclient.mod.modules.impl.render.Chams;
import noxisclient.mod.modules.impl.render.Crosshair;
import noxisclient.mod.modules.impl.render.ESP;
import noxisclient.mod.modules.impl.render.Fov;
import noxisclient.mod.modules.impl.render.HighLight;
import noxisclient.mod.modules.impl.render.HoleESP;
import noxisclient.mod.modules.impl.render.LogoutSpots;
import noxisclient.mod.modules.impl.render.MaceEffect;
import noxisclient.mod.modules.impl.render.NameTags;
import noxisclient.mod.modules.impl.render.NoRender;
import noxisclient.mod.modules.impl.render.PhaseESP;
import noxisclient.mod.modules.impl.render.PlaceRender;
import noxisclient.mod.modules.impl.render.PopChams;
import noxisclient.mod.modules.impl.render.ShaderModule;
import noxisclient.mod.modules.impl.render.SkyBox;
import noxisclient.mod.modules.impl.render.SpearFix;
import noxisclient.mod.modules.impl.render.Stickman;
import noxisclient.mod.modules.impl.render.TotemParticle;
import noxisclient.mod.modules.impl.render.Tracers;
import noxisclient.mod.modules.impl.render.Trajectories;
import noxisclient.mod.modules.impl.render.ViewModel;
import noxisclient.mod.modules.impl.render.Xray;
import noxisclient.mod.modules.impl.render.Zoom;
import noxisclient.mod.modules.settings.Setting;
import noxisclient.mod.modules.settings.impl.BindSetting;
import org.lwjgl.opengl.GL11;

public class ModuleManager implements Wrapper {
   private final ArrayList<Module> modules = new ArrayList<>();
   private final ArrayList<ModuleManager.ToggleBanner> toggleBanners = new ArrayList<>();
   private boolean notificationMouseWasDown = false;
   private boolean interactiveToggleInBanner = false;

   public ArrayList<Module> getModules() {
      return this.modules;
   }

   public ModuleManager() {
      this.init();
   }

   public void init() {
      this.addModule(new AntiTK());
      this.addModule(new Panic());
      this.addModule(new TargetFollow());
      this.addModule(new AutoHole());
      this.addModule(new AutoEC());
      this.addModule(new BoxThieves());
      this.addModule(new Fonts());
      this.addModule(new Socials());
      this.addModule(new NoTerrainScreen());
      this.addModule(new AutoCrystal());
      this.addModule(new Ambience());
      this.addModule(new AntiHunger());
      this.addModule(new AntiVoid());
      this.addModule(new AutoWalk());
      this.addModule(new VClip());
      this.addModule(new ExtraTab());
      this.addModule(new AntiWeak());
      this.addModule(new BedCrafter());
      this.addModule(new AspectRatio());
      this.addModule(new ChunkESP());
      this.addModule(new Aura());
      this.addModule(new PistonCrystal());
      this.addModule(new ElytraFly());
      this.addModule(new AutoAnchor());
      this.addModule(new PhaseESP());
      this.addModule(new AutoArmor());
      this.addModule(new AutoMine());
      this.addModule(new AutoLog());
      this.addModule(new AutoSay());
      this.addModule(new SelfTrap());
      this.addModule(new noxisclient.mod.modules.impl.player.InventoryManager());
      this.addModule(new AutoXP());
      this.addModule(new AutoPotion());
      this.addModule(new AutoPush());
      this.addModule(new Offhand());
      this.addModule(new Mainhand());
      this.addModule(new Nuker());
      this.addModule(new AutoTrap());
      this.addModule(new AutoWeb());
      this.addModule(new Blink());
      this.addModule(new ChorusControl());
      this.addModule(new BlockStrafe());
      this.addModule(new FastSwim());
      this.addModule(new Blocker());
      this.addModule(new Quiver());
      this.addModule(new BowBomb());
      this.addModule(new BreakESP());
      this.addModule(new Burrow());
      this.addModule(new MaceSpoof());
      this.addModule(new CameraClip());
      this.addModule(new ChatAppend());
      this.addModule(new ClickGui());
      this.addModule(new InfiniteTrident());
      this.addModule(new ColorsModule());
      this.addModule(new EcCache());
      this.addModule(new AutoRegear());
      this.addModule(new LavaFiller());
      this.addModule(new AntiPhase());
      this.addModule(new Clip());
      this.addModule(new AntiCheat());
      this.addModule(new IRC());
      this.addModule(new ItemsCounter());
      this.addModule(new Fov());
      this.addModule(new Criticals());
      this.addModule(new CevBreaker());
      this.addModule(new Crosshair());
      this.addModule(new Chams());
      this.addModule(new AntiPacket());
      this.addModule(new AutoReconnect());
      this.addModule(new ESP());
      this.addModule(new HoleESP());
      this.addModule(new Tracers());
      this.addModule(new MovementSync());
      this.addModule(new PacketLogger());
      this.addModule(new TeleportLogger());
      this.addModule(new SkinFlicker());
      this.addModule(new EntityControl());
      this.addModule(new NameTags());
      this.addModule(new ShulkerViewer());
      this.addModule(new PingSpoof());
      this.addModule(new FakePlayer());
      this.addModule(new Spammer());
      this.addModule(new HighLight());
      this.addModule(new FastFall());
      this.addModule(new FastWeb());
      this.addModule(new Flatten());
      this.addModule(new FireworkBoost());
      this.addModule(new Fly());
      this.addModule(new Yaw());
      this.addModule(new Freecam());
      this.addModule(new FreeLook());
      this.addModule(new TimerModule());
      this.addModule(new Tips());
      this.addModule(new ClientSetting());
      this.addModule(new TitleScreen());
      this.addModule(new TextRadar());
      this.addModule(new ArmorHud());
      this.addModule(new Totem());
      this.addModule(new HUD());
      this.addModule(new HudEditor());
      this.addModule(new ArrayListHud());
      this.addModule(new WaterMarkHud());
      this.addModule(new SpeedHud());
      this.addModule(new WelcomerHud());
      this.addModule(new PotionHud());
      this.addModule(new ServerInfoHud());
      this.addModule(new PlayerModel());
      this.addModule(new DirectionHud());
      this.addModule(new CoordsHud());
      this.addModule(new ChatPosition());
      this.addModule(new BackpackContents());
      this.addModule(new NoResourcePack());
      this.addModule(new RocketExtend());
      this.addModule(new HoleFiller());
      this.addModule(new HoleSnap());
      this.addModule(new LogoutSpots());
      this.addModule(new AutoTool());
      this.addModule(new Trajectories());
      this.addModule(new KillEffect());
      this.addModule(new Macros());
      this.addModule(new AntiEffects());
      this.addModule(new NoFall());
      this.addModule(new NoRender());
      this.addModule(new NoSlow());
      this.addModule(new NoSound());
      this.addModule(new NoRotate());
      this.addModule(new AirPlace());
      this.addModule(new Xray());
      this.addModule(new PacketEat());
      this.addModule(new PacketFly());
      this.addModule(new PacketMine());
      this.addModule(new PacketControl());
      this.addModule(new Phase());
      this.addModule(new PlaceRender());
      this.addModule(new InteractTweaks());
      this.addModule(new PopChams());
      this.addModule(new ShaderModule());
      this.addModule(new SkyBox());
      this.addModule(new SpearFix());
      this.addModule(new MaceEffect());
      this.addModule(new Stickman());
      this.addModule(new Replenish());
      this.addModule(new ServerLagger());
      this.addModule(new AntiCrawl());
      this.addModule(new AntiRegear());
      this.addModule(new SafeWalk());
      this.addModule(new NoJumpDelay());
      this.addModule(new Speed());
      this.addModule(new Sprint());
      this.addModule(new Strafe());
      this.addModule(new Step());
      this.addModule(new Surround());
      this.addModule(new TotemParticle());
      this.addModule(new PotionTracker());
      this.addModule(new Velocity());
      this.addModule(new ViewModel());
      this.addModule(new XCarry());
      this.addModule(new Zoom());
      this.addModule(new AnimationsModule());
      this.modules.sort(Comparator.comparing(Mod::getName));
   }

   public void onKeyReleased(int eventKey) {
      if (eventKey != -1 && eventKey != 0) {
         this.handleKeyEvent(eventKey, false);
      }
   }

   public void onKeyPressed(int eventKey) {
      if (eventKey != -1 && eventKey != 0) {
         this.handleKeyEvent(eventKey, true);
      }
   }

   private void handleKeyEvent(int key, boolean isPressed) {
      for (Module module : this.modules) {
         BindSetting bindSetting = module.getBindSetting();
         if (bindSetting.getValue() == key) {
            if (isPressed && mc.currentScreen == null) {
               module.toggle();
               bindSetting.holding = true;
            } else if (!isPressed && bindSetting.isHoldEnable() && bindSetting.holding) {
               module.toggle();
               bindSetting.holding = false;
            }
         }

         for (Setting setting : module.getSettings()) {
            BindSetting bind;
            if (setting instanceof BindSetting && (bind = (BindSetting)setting).getValue() == key) {
               bind.setPressed(isPressed);
            }
         }
      }
   }

   public void onLogin() {
      for (Module module : this.modules) {
         if (module.isOn()) {
            module.onLogin();
         }
      }
   }

   public void onLogout() {
      for (Module module : this.modules) {
         if (module.isOn()) {
            module.onLogout();
         }
      }
   }

   public void onRender2D(DrawContext drawContext) {
      for (Module module : this.modules) {
         if (module.isOn()) {
            try {
               module.onRender2D(drawContext, mc.getRenderTickCounter().getTickDelta(true));
            } catch (Exception var7) {
               var7.printStackTrace();
               if (ClientSetting.INSTANCE.debug.getValue()) {
                  CommandManager.sendMessage("§4An error has occurred (" + module.getName() + " [onRender2D]) Message: [" + var7.getMessage() + "]");
               }
            }
         }
      }

      try {
         Alien.EVENT_BUS.post(Render2DEvent.get(drawContext, mc.getRenderTickCounter().getTickDelta(true)));
      } catch (Exception var6) {
         var6.printStackTrace();
         if (ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage("§4An error has occurred (Render3DEvent) Message: [" + var6.getMessage() + "]");
         }
      }

      if (!(mc.currentScreen instanceof HandledScreen)) {
         try {
            AnimationManager.render(drawContext);
         } catch (Exception var5) {
            var5.printStackTrace();
         }
      }

      this.renderToggleBanners(drawContext);
   }

   public void render3D(MatrixStack matrices) {
      GL11.glEnable(2848);

      for (Module module : this.modules) {
         if (module.isOn()) {
            try {
               module.onRender3D(matrices);
            } catch (Exception var6) {
               var6.printStackTrace();
               if (ClientSetting.INSTANCE.debug.getValue()) {
                  CommandManager.sendMessage("§4An error has occurred (" + module.getName() + " [onRender3D]) Message: [" + var6.getMessage() + "]");
               }
            }
         }
      }

      try {
         Alien.EVENT_BUS.post(Render3DEvent.get(matrices, mc.getRenderTickCounter().getTickDelta(true)));
      } catch (Exception var5) {
         var5.printStackTrace();
         if (ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage("§4An error has occurred (Render3DEvent) Message: [" + var5.getMessage() + "]");
         }
      }

      GL11.glDisable(2848);
   }

   public void showToggleBanner(Module module, boolean enabled) {
   }

   private void renderToggleBanners(DrawContext ctx) {
      this.toggleBanners.clear();
   }

   private void renderPotionListLegacy(DrawContext ctx) {
      if (mc.player != null) {
         int margin = 14;
         int startX = margin + 2;
         int startY = margin + 92;
         int pillH = 14;
         int pillPad = 6;
         int idx = 0;

         for (StatusEffectInstance se : mc.player.getStatusEffects()) {
            String name = se.getEffectType().value().getName().getString();
            int ticks = se.getDuration();
            int totalSec = Math.max(0, ticks / 20);
            int mm = totalSec / 60;
            int ss = totalSec % 60;
            String time = String.format("%d:%02d", mm, ss);
            String text = name + " " + time;
            int tw = Fonts.useClickGuiFont() ? (int)FontManager.ui.getWidth(text) : (int)TextUtil.getWidth(text);
            int pillW = tw + pillPad * 2;
            int y = startY + idx * (pillH + 4);
            int keyCodec = 180;
            Render2DUtil.drawRoundedRect(ctx.getMatrices(), (float)startX, (float)y, (float)pillW, (float)pillH, 4.0F, new Color(255, 255, 255, keyCodec));
            Render2DUtil.drawRoundedStroke(ctx.getMatrices(), (float)startX, (float)y, (float)pillW, (float)pillH, 4.0F, new Color(220, 224, 230, 160), 48);
            int tx = startX + pillPad;
            double ty = (double)y + (double)((float)pillH - (Fonts.useClickGuiFont() ? FontManager.ui.getFontHeight() : TextUtil.getHeight())) / 2.0;
            TextUtil.drawString(ctx, text, (double)tx, ty, new Color(30, 30, 30).getRGB(), Fonts.useClickGuiFont());
            if (++idx >= 5) {
               break;
            }
         }
      }
   }

   private void renderPotionBanners(DrawContext ctx) {
   }

   private String formatDuration(int ticks) {
      int totalSec = Math.max(0, ticks / 20);
      int mm = totalSec / 60;
      int ss = totalSec % 60;
      return String.format("%d:%02d", mm, ss);
   }

   private Module getModuleByDisplayName(String name) {
      for (Module m : this.modules) {
         if (m.getDisplayName().equalsIgnoreCase(name) || m.getName().equalsIgnoreCase(name)) {
            return m;
         }
      }

      return null;
   }

   public void addModule(Module module) {
      this.modules.add(module);
   }

   public Module getModuleByName(String string) {
      for (Module module : this.modules) {
         if (module.getName().equalsIgnoreCase(string)) {
            return module;
         }
      }

      return null;
   }

   private static class ToggleBanner {
      public final String name;
      public final boolean enabled;
      public final long start;

      public ToggleBanner(String name, boolean enabled, long start) {
         this.name = name;
         this.enabled = enabled;
         this.start = start;
      }
   }
}
