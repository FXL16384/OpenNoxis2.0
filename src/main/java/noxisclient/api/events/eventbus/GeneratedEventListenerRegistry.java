package noxisclient.api.events.eventbus;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import noxisclient.api.events.impl.AmbientOcclusionEvent;
import noxisclient.api.events.impl.BlockActivateEvent;
import noxisclient.api.events.impl.BlockSlipperinessEvent;
import noxisclient.api.events.impl.ChunkOcclusionEvent;
import noxisclient.api.events.impl.ClickBlockEvent;
import noxisclient.api.events.impl.ClientTickEvent;
import noxisclient.api.events.impl.DeathEvent;
import noxisclient.api.events.impl.DoAttackEvent;
import noxisclient.api.events.impl.EntitySpawnEvent;
import noxisclient.api.events.impl.EntitySpawnedEvent;
import noxisclient.api.events.impl.EntityVelocityUpdateEvent;
import noxisclient.api.events.impl.FireworkShooterRotationEvent;
import noxisclient.api.events.impl.GameLeftEvent;
import noxisclient.api.events.impl.HeldItemRendererEvent;
import noxisclient.api.events.impl.InitEvent;
import noxisclient.api.events.impl.InteractBlockEvent;
import noxisclient.api.events.impl.InteractItemEvent;
import noxisclient.api.events.impl.InventoryS2CPacketEvent;
import noxisclient.api.events.impl.JumpEvent;
import noxisclient.api.events.impl.KeyboardInputEvent;
import noxisclient.api.events.impl.LerpToEvent;
import noxisclient.api.events.impl.LookDirectionEvent;
import noxisclient.api.events.impl.MoveEvent;
import noxisclient.api.events.impl.MovedEvent;
import noxisclient.api.events.impl.MovementSlowdownEvent;
import noxisclient.api.events.impl.OpenScreenEvent;
import noxisclient.api.events.impl.PacketEvent;
import noxisclient.api.events.impl.ParticleEvent;
import noxisclient.api.events.impl.PlaySoundEvent;
import noxisclient.api.events.impl.PreRender2DEvent;
import noxisclient.api.events.impl.RemoveEntityEvent;
import noxisclient.api.events.impl.RemoveFireworkEvent;
import noxisclient.api.events.impl.Render2DEvent;
import noxisclient.api.events.impl.Render3DEvent;
import noxisclient.api.events.impl.RenderBlockEntityEvent;
import noxisclient.api.events.impl.RenderEntityEvent;
import noxisclient.api.events.impl.RotationEvent;
import noxisclient.api.events.impl.S2CCloseScreenEvent;
import noxisclient.api.events.impl.SendMessageEvent;
import noxisclient.api.events.impl.SendMovementPacketsEvent;
import noxisclient.api.events.impl.ServerConnectBeginEvent;
import noxisclient.api.events.impl.ServerRotationEvent;
import noxisclient.api.events.impl.SetCurrentHandEvent;
import noxisclient.api.events.impl.SlowMovementEvent;
import noxisclient.api.events.impl.SprintEvent;
import noxisclient.api.events.impl.SteppedOnSlimeBlockEvent;
import noxisclient.api.events.impl.StrafeFixEvent;
import noxisclient.api.events.impl.TickEntityEvent;
import noxisclient.api.events.impl.TickEvent;
import noxisclient.api.events.impl.TickMovementEvent;
import noxisclient.api.events.impl.TimerEvent;
import noxisclient.api.events.impl.TotemEvent;
import noxisclient.api.events.impl.TotemParticleEvent;
import noxisclient.api.events.impl.TravelEvent;
import noxisclient.api.events.impl.UpdateEvent;
import noxisclient.api.events.impl.UpdateRotateEvent;
import noxisclient.api.events.impl.VelocityMultiplierEvent;
import noxisclient.api.render.satin.ReloadableShaderEffectManager;
import noxisclient.core.impl.BreakManager;
import noxisclient.core.impl.ConfigManager;
import noxisclient.core.impl.InventoryManager;
import noxisclient.core.impl.PlayerManager;
import noxisclient.core.impl.PopManager;
import noxisclient.core.impl.RotationManager;
import noxisclient.core.impl.ServerManager;
import noxisclient.core.impl.ThreadManager;
import noxisclient.impl.event.ResolutionEvent;
import noxisclient.mod.commands.impl.PingCommand;
import noxisclient.mod.modules.impl.client.BaritoneModule;
import noxisclient.mod.modules.impl.client.ClickGui;
import noxisclient.mod.modules.impl.client.HudEditor;
import noxisclient.mod.modules.impl.client.TextRadar;
import noxisclient.mod.modules.impl.combat.AntiCrawl;
import noxisclient.mod.modules.impl.combat.AntiPhase;
import noxisclient.mod.modules.impl.combat.AntiRegear;
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
import noxisclient.mod.modules.impl.hud.ArrayListHud;
import noxisclient.mod.modules.impl.hud.HUD;
import noxisclient.mod.modules.impl.hud.PacketHud;
import noxisclient.mod.modules.impl.hud.PingHud;
import noxisclient.mod.modules.impl.hud.PlayerModel;
import noxisclient.mod.modules.impl.misc.AiChatFeature;
import noxisclient.mod.modules.impl.misc.AutoEC;
import noxisclient.mod.modules.impl.misc.AutoLog;
import noxisclient.mod.modules.impl.misc.AutoReconnect;
import noxisclient.mod.modules.impl.misc.AutoSay;
import noxisclient.mod.modules.impl.misc.BedCrafter;
import noxisclient.mod.modules.impl.misc.BoxThieves;
import noxisclient.mod.modules.impl.misc.ChatAppend;
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
import noxisclient.mod.modules.impl.movement.FastFall;
import noxisclient.mod.modules.impl.movement.FastSwim;
import noxisclient.mod.modules.impl.movement.FastWeb;
import noxisclient.mod.modules.impl.movement.Flatten;
import noxisclient.mod.modules.impl.movement.Fly;
import noxisclient.mod.modules.impl.movement.HoleSnap;
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
import noxisclient.mod.modules.impl.player.AutoArmor;
import noxisclient.mod.modules.impl.player.AutoPearl;
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
import noxisclient.mod.modules.impl.render.Chams;
import noxisclient.mod.modules.impl.render.ESP;
import noxisclient.mod.modules.impl.render.LogoutSpots;
import noxisclient.mod.modules.impl.render.MaceEffect;
import noxisclient.mod.modules.impl.render.MotionCamera;
import noxisclient.mod.modules.impl.render.NameTags;
import noxisclient.mod.modules.impl.render.NoBob;
import noxisclient.mod.modules.impl.render.NoRender;
import noxisclient.mod.modules.impl.render.PhaseESP;
import noxisclient.mod.modules.impl.render.PopChams;
import noxisclient.mod.modules.impl.render.TotemParticle;
import noxisclient.mod.modules.impl.render.ViewModel;
import noxisclient.mod.modules.impl.render.Xray;
import noxisclient.mod.modules.impl.render.Zoom;

final class GeneratedEventListenerRegistry {
   private GeneratedEventListenerRegistry() {
   }

   static List<IListener> listenersFor(Object subscriber) {
      List<IListener> listeners = new ArrayList<>();
      if (subscriber instanceof ReloadableShaderEffectManager listener0) {
         listeners.add(new DirectListener<>(ResolutionEvent.class, 0, false, listener0::onResolution));
      }

      if (subscriber instanceof BreakManager listener1) {
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener1::onPacket));
         listeners.add(new DirectListener<>(ServerConnectBeginEvent.class, 0, false, listener1::onServerConnectBegin));
         listeners.add(new DirectListener<>(ClientTickEvent.class, 0, false, listener1::onTick));
      }

      if (subscriber instanceof ConfigManager listener2) {
         listeners.add(new DirectListener<>(Render2DEvent.class, 999, false, listener2::onRender2D));
      }

      if (subscriber instanceof InventoryManager listener3) {
         listeners.add(new DirectListener<>(DeathEvent.class, 0, false, listener3::onDeath));
         listeners.add(new DirectListener<>(GameLeftEvent.class, 0, false, listener3::onGameLeft));
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, -1000, false, listener3::onPacketInbound));
         listeners.add(new DirectListener<>(PacketEvent.Send.class, 1000, false, listener3::onPacketOutbound));
         listeners.add(new DirectListener<>(ClientTickEvent.class, 0, false, listener3::onTick));
      }

      if (subscriber instanceof PlayerManager listener4) {
         listeners.add(new DirectListener<>(BlockActivateEvent.class, 0, false, listener4::onBlockActivate));
         listeners.add(new DirectListener<>(GameLeftEvent.class, 0, false, listener4::onLogout));
         listeners.add(new DirectListener<>(OpenScreenEvent.class, 0, false, listener4::onOpenScreenEvent));
      }

      if (subscriber instanceof PopManager listener5) {
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener5::onPacketReceive));
      }

      if (subscriber instanceof RotationManager listener6) {
         listeners.add(new DirectListener<>(DoAttackEvent.class, 0, false, listener6::doAttack));
         listeners.add(new DirectListener<>(FireworkShooterRotationEvent.class, 0, false, listener6::onFirework));
         listeners.add(new DirectListener<>(InteractBlockEvent.class, 0, false, listener6::onInteract));
         listeners.add(new DirectListener<>(InteractItemEvent.class, 0, false, listener6::onInteract));
         listeners.add(new DirectListener<>(JumpEvent.class, 0, false, listener6::onJump));
         listeners.add(new DirectListener<>(KeyboardInputEvent.class, -999, false, listener6::onKeyInput));
         listeners.add(new DirectListener<>(UpdateRotateEvent.class, -200, false, listener6::onLastRotation));
         listeners.add(new DirectListener<>(PacketEvent.Sent.class, -999, false, listener6::onPacketSend));
         listeners.add(new DirectListener<>(TickMovementEvent.class, 0, false, listener6::onTickMovement));
         listeners.add(new DirectListener<>(TickEvent.class, 0, false, listener6::onUpdateWalkingPost));
         listeners.add(new DirectListener<>(TravelEvent.class, 0, false, listener6::travel));
         listeners.add(new DirectListener<>(SendMovementPacketsEvent.class, 0, false, listener6::update));
         listeners.add(new DirectListener<>(TickMovementEvent.class, 999, false, listener6::update));
      }

      if (subscriber instanceof ServerManager listener7) {
         listeners.add(new DirectListener<>(GameLeftEvent.class, 0, false, listener7::onLeft));
         listeners.add(new DirectListener<>(PacketEvent.Send.class, -200, false, listener7::onPacket));
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 999, false, listener7::onPacketReceive));
         listeners.add(new DirectListener<>(PacketEvent.Send.class, 0, false, listener7::onPacketSend));
      }

      if (subscriber instanceof ThreadManager listener8) {
         listeners.add(new DirectListener<>(ClientTickEvent.class, 200, false, listener8::onEvent));
      }

      if (subscriber instanceof PingCommand listener9) {
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener9::onPacketReceive));
      }

      if (subscriber instanceof BaritoneModule listener10) {
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener10::onUpdate));
      }

      if (subscriber instanceof ClickGui listener11) {
         listeners.add(new DirectListener<>(ClientTickEvent.class, 0, false, listener11::onUpdate));
      }

      if (subscriber instanceof ClickGui.FadeOut listener12) {
         listeners.add(new DirectListener<>(Render2DEvent.class, -99999, false, listener12::onRender2D));
      }

      if (subscriber instanceof HudEditor listener13) {
         listeners.add(new DirectListener<>(TickEvent.class, 0, false, listener13::onTick));
      }

      if (subscriber instanceof TextRadar listener14) {
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener14::onPacketReceive));
         listeners.add(new DirectListener<>(TotemEvent.class, 0, false, listener14::onTotemPop));
      }

      if (subscriber instanceof AntiCrawl listener15) {
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener15::onUpdate));
      }

      if (subscriber instanceof AntiPhase listener16) {
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener16::onUpdate));
      }

      if (subscriber instanceof AntiRegear listener17) {
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener17::onUpdate));
      }

      if (subscriber instanceof AntiWeak listener18) {
         listeners.add(new DirectListener<>(PacketEvent.Send.class, 200, false, listener18::onPacketSend));
      }

      if (subscriber instanceof Aura listener19) {
         listeners.add(new DirectListener<>(PacketEvent.Send.class, 0, false, listener19::onPacket));
         listeners.add(new DirectListener<>(RotationEvent.class, 0, false, listener19::onRotate));
         listeners.add(new DirectListener<>(ClientTickEvent.class, 0, false, listener19::onTick));
      }

      if (subscriber instanceof AutoAnchor listener20) {
         listeners.add(new DirectListener<>(RotationEvent.class, 0, false, listener20::onRotate));
         listeners.add(new DirectListener<>(ClientTickEvent.class, 0, false, listener20::onTick));
      }

      if (subscriber instanceof AutoAnchor.AnchorRender listener21) {
         listeners.add(new DirectListener<>(Render3DEvent.class, 0, false, listener21::onRender3D));
      }

      if (subscriber instanceof AutoCrystal listener22) {
         listeners.add(new DirectListener<>(EntitySpawnedEvent.class, 0, false, listener22::onEntity));
         listeners.add(new DirectListener<>(PacketEvent.Send.class, -199, false, listener22::onPacketSend));
         listeners.add(new DirectListener<>(RotationEvent.class, 0, false, listener22::onRotate));
         listeners.add(new DirectListener<>(ClientTickEvent.class, 0, false, listener22::onTick));
      }

      if (subscriber instanceof AutoCrystal.CrystalRender listener23) {
         listeners.add(new DirectListener<>(Render3DEvent.class, 0, false, listener23::onRender3D));
      }

      if (subscriber instanceof AutoMine listener24) {
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener24::onUpdate));
      }

      if (subscriber instanceof AutoPush listener25) {
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener25::onUpdate));
      }

      if (subscriber instanceof AutoRegear listener26) {
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener26::onUpdate));
      }

      if (subscriber instanceof AutoTrap listener27) {
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener27::onUpdate));
      }

      if (subscriber instanceof AutoWeb listener28) {
         listeners.add(new DirectListener<>(RotationEvent.class, 0, false, listener28::onRotate));
         listeners.add(new DirectListener<>(ClientTickEvent.class, 0, false, listener28::onTick));
      }

      if (subscriber instanceof AutoXP listener29) {
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener29::onUpdate));
      }

      if (subscriber instanceof Blocker listener30) {
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener30::onUpdate));
      }

      if (subscriber instanceof Burrow listener31) {
         listeners.add(new DirectListener<>(ClientTickEvent.class, 0, false, listener31::onUpdate));
      }

      if (subscriber instanceof CevBreaker listener32) {
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener32::onUpdate));
      }

      if (subscriber instanceof Criticals listener33) {
         listeners.add(new DirectListener<>(PacketEvent.Send.class, 0, false, listener33::onPacketSend));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener33::onUpdate));
      }

      if (subscriber instanceof HoleFiller listener34) {
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener34::onUpdate));
      }

      if (subscriber instanceof Mainhand listener35) {
         listeners.add(new DirectListener<>(InteractBlockEvent.class, 0, false, listener35::onInteractBlock));
         listeners.add(new DirectListener<>(ClientTickEvent.class, 0, false, listener35::onTick));
      }

      if (subscriber instanceof Offhand listener36) {
         listeners.add(new DirectListener<>(InventoryS2CPacketEvent.class, 0, false, listener36::onInventory));
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener36::onPacket));
         listeners.add(new DirectListener<>(ClientTickEvent.class, 0, false, listener36::onTick));
         listeners.add(new DirectListener<>(TotemEvent.class, 0, false, listener36::onTotem));
      }

      if (subscriber instanceof PistonCrystal listener37) {
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener37::onUpdate));
      }

      if (subscriber instanceof Quiver listener38) {
         listeners.add(new DirectListener<>(UpdateRotateEvent.class, 0, false, listener38::onRotate));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener38::onUpdate));
      }

      if (subscriber instanceof SelfTrap listener39) {
         listeners.add(new DirectListener<>(OpenScreenEvent.class, 0, false, listener39::onOpenScreen));
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener39::onPacketInbound));
         listeners.add(new DirectListener<>(ClientTickEvent.class, 0, false, listener39::onPlayerTick));
      }

      if (subscriber instanceof Surround listener40) {
         listeners.add(new DirectListener<>(OpenScreenEvent.class, 0, false, listener40::onOpenScreen));
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener40::onPacketInbound));
         listeners.add(new DirectListener<>(ClientTickEvent.class, 0, false, listener40::onPlayerTick));
      }

      if (subscriber instanceof AntiHunger listener41) {
         listeners.add(new DirectListener<>(PacketEvent.Send.class, -100, false, listener41::onPacketSend));
      }

      if (subscriber instanceof AntiPacket listener42) {
         listeners.add(new DirectListener<>(S2CCloseScreenEvent.class, 0, false, listener42::onCloseScreen));
         listeners.add(new DirectListener<>(LerpToEvent.class, 0, false, listener42::onEvent));
         listeners.add(new DirectListener<>(InventoryS2CPacketEvent.class, 0, false, listener42::onInventory));
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener42::onPacketReceive));
         listeners.add(new DirectListener<>(PacketEvent.Send.class, 0, false, listener42::onPacketSend));
         listeners.add(new DirectListener<>(ServerConnectBeginEvent.class, 0, false, listener42::onServerConnectBegin));
         listeners.add(new DirectListener<>(TickEvent.class, 0, false, listener42::onTick));
      }

      if (subscriber instanceof Blink listener43) {
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener43::onPacketEvent));
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener43::onReceivePacket));
         listeners.add(new DirectListener<>(PacketEvent.Send.class, 0, false, listener43::onSendPacket));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener43::onUpdate));
      }

      if (subscriber instanceof BowBomb listener44) {
         listeners.add(new DirectListener<>(UpdateRotateEvent.class, 0, false, listener44::onMotion));
         listeners.add(new DirectListener<>(PacketEvent.Send.class, 0, false, listener44::onPacketSend));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener44::onUpdate));
      }

      if (subscriber instanceof ChorusControl listener45) {
         listeners.add(new DirectListener<>(MoveEvent.class, -200, false, listener45::onMove));
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener45::onPacketReceive));
         listeners.add(new DirectListener<>(PacketEvent.Send.class, 0, false, listener45::onPacketSend));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener45::onUpdate));
      }

      if (subscriber instanceof ChunkESP listener46) {
         listeners.add(new DirectListener<>(GameLeftEvent.class, 0, false, listener46::onGameLeft));
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener46::onReadPacket));
         listeners.add(new DirectListener<>(Render3DEvent.class, 0, false, listener46::onRender));
         listeners.add(new DirectListener<>(OpenScreenEvent.class, 0, false, listener46::onScreenOpen));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener46::onUpdate));
      }

      if (subscriber instanceof Clip listener47) {
         listeners.add(new DirectListener<>(PacketEvent.Send.class, 0, false, listener47::onPacket));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener47::onUpdate));
      }

      if (subscriber instanceof InfiniteTrident listener48) {
         listeners.add(new DirectListener<>(PacketEvent.Send.class, 201, false, listener48::onSendPacket));
         listeners.add(new DirectListener<>(PacketEvent.Sent.class, 201, false, listener48::onSendPacket));
      }

      if (subscriber instanceof MaceSpoof listener49) {
         listeners.add(new DirectListener<>(PacketEvent.Send.class, 200, false, listener49::onPacketSend));
      }

      if (subscriber instanceof NoResourcePack listener50) {
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener50::onPacketReceive));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener50::onUpdate));
      }

      if (subscriber instanceof PacketControl listener51) {
         listeners.add(new DirectListener<>(PacketEvent.Send.class, 0, false, listener51::onPacketSend));
         listeners.add(new DirectListener<>(SendMovementPacketsEvent.class, -999, false, listener51::onRotation));
      }

      if (subscriber instanceof Phase listener52) {
         listeners.add(new DirectListener<>(RotationEvent.class, 0, false, listener52::onRotate));
         listeners.add(new DirectListener<>(ClientTickEvent.class, 0, false, listener52::onTick));
      }

      if (subscriber instanceof PingSpoof listener53) {
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener53::onPacketReceive));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener53::onUpdate));
      }

      if (subscriber instanceof RocketExtend listener54) {
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener54::onPacketInbound));
         listeners.add(new DirectListener<>(PacketEvent.Send.class, 0, false, listener54::onPacketOutbound));
         listeners.add(new DirectListener<>(RemoveFireworkEvent.class, 0, false, listener54::onRemoveFirework));
         listeners.add(new DirectListener<>(ClientTickEvent.class, 0, false, listener54::onTick));
      }

      if (subscriber instanceof ServerLagger listener55) {
         listeners.add(new DirectListener<>(GameLeftEvent.class, 0, false, listener55::onLogout));
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener55::onPacketInbound));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener55::onUpdate));
      }

      if (subscriber instanceof SkinFlicker listener56) {
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener56::onUpdate));
      }

      if (subscriber instanceof TeleportLogger listener57) {
         listeners.add(new DirectListener<>(DeathEvent.class, 0, false, listener57::onDeath));
         listeners.add(new DirectListener<>(PlaySoundEvent.class, 0, false, listener57::onPlaySound));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener57::onUpdate));
      }

      if (subscriber instanceof XCarry listener58) {
         listeners.add(new DirectListener<>(PacketEvent.Send.class, 0, false, listener58::onPacketSend));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener58::onUpdate));
      }

      if (subscriber instanceof ArrayListHud listener59) {
         listeners.add(new DirectListener<>(ClientTickEvent.class, -999, false, listener59::onTick));
      }

      if (subscriber instanceof ArrayListHud.InitHandler listener60) {
         listeners.add(new DirectListener<>(InitEvent.class, 0, false, listener60::onInit));
      }

      if (subscriber instanceof HUD listener61) {
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener61::onReceive));
         listeners.add(new DirectListener<>(PacketEvent.Send.class, 0, false, listener61::onSend));
      }

      if (subscriber instanceof PacketHud listener62) {
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener62::onReceive));
         listeners.add(new DirectListener<>(PacketEvent.Send.class, 0, false, listener62::onSend));
      }

      if (subscriber instanceof PingHud listener63) {
         listeners.add(new DirectListener<>(ClientTickEvent.class, 0, false, listener63::onTick));
      }

      if (subscriber instanceof PlayerModel listener64) {
         listeners.add(new DirectListener<>(Render2DEvent.class, 0, false, listener64::onRender2D));
      }

      if (subscriber instanceof AiChatFeature listener65) {
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener65::onPacketReceive));
      }

      if (subscriber instanceof AutoEC listener66) {
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener66::onUpdate));
      }

      if (subscriber instanceof AutoLog listener67) {
         listeners.add(new DirectListener<>(TotemEvent.class, 0, false, listener67::onPop));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener67::onUpdate));
      }

      if (subscriber instanceof AutoReconnect listener68) {
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener68::onPacketReceive));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener68::onUpdate));
      }

      if (subscriber instanceof AutoReconnect.StaticListener listener69) {
         listeners.add(new DirectListener<>(ServerConnectBeginEvent.class, 0, false, listener69::onGameJoined));
      }

      if (subscriber instanceof AutoSay listener70) {
         listeners.add(new DirectListener<>(DeathEvent.class, 0, false, listener70::onDeath));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener70::onUpdate));
      }

      if (subscriber instanceof BedCrafter listener71) {
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener71::onUpdate));
      }

      if (subscriber instanceof BoxThieves listener72) {
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener72::onUpdate));
      }

      if (subscriber instanceof ChatAppend listener73) {
         listeners.add(new DirectListener<>(SendMessageEvent.class, 0, false, listener73::onSendMessage));
      }

      if (subscriber instanceof FakePlayer listener74) {
         listeners.add(new DirectListener<>(PacketEvent.Send.class, 0, false, listener74::onAttack));
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener74::onPacketReceive));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener74::onUpdate));
      }

      if (subscriber instanceof KillEffect listener75) {
         listeners.add(new DirectListener<>(DeathEvent.class, 0, false, listener75::onPlayerDeath));
         listeners.add(new DirectListener<>(Render3DEvent.class, 0, false, listener75::onRender3D));
         listeners.add(new DirectListener<>(ClientTickEvent.class, 0, false, listener75::onTick));
      }

      if (subscriber instanceof LavaFiller listener76) {
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener76::onUpdate));
      }

      if (subscriber instanceof NoRotate listener77) {
         listeners.add(new DirectListener<>(ServerRotationEvent.class, 0, false, listener77::onServerRotation));
      }

      if (subscriber instanceof NoSound listener78) {
         listeners.add(new DirectListener<>(PlaySoundEvent.class, 0, false, listener78::onPlaySound));
      }

      if (subscriber instanceof NoTerrainScreen listener79) {
         listeners.add(new DirectListener<>(ClientTickEvent.class, 0, false, listener79::onEvent));
      }

      if (subscriber instanceof Nuker listener80) {
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener80::onUpdate));
      }

      if (subscriber instanceof PacketLogger listener81) {
         listeners.add(new DirectListener<>(PacketEvent.Send.class, -999999, false, listener81::onPacketSend));
         listeners.add(new DirectListener<>(EntityVelocityUpdateEvent.class, 999999, false, listener81::velocity));
      }

      if (subscriber instanceof PotionTracker listener82) {
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener82::onPacketReceive));
         listeners.add(new DirectListener<>(TotemEvent.class, 0, false, listener82::onTotemPop));
      }

      if (subscriber instanceof ShulkerViewer listener83) {
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener83::onUpdate));
      }

      if (subscriber instanceof Spammer listener84) {
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener84::onUpdate));
      }

      if (subscriber instanceof Tips listener85) {
         listeners.add(new DirectListener<>(EntitySpawnEvent.class, 0, false, listener85::onAddEntity));
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener85::onPacketEvent));
         listeners.add(new DirectListener<>(DeathEvent.class, 0, false, listener85::onPlayerDeath));
         listeners.add(new DirectListener<>(RemoveEntityEvent.class, 0, false, listener85::onRemoveEntity));
         listeners.add(new DirectListener<>(TotemEvent.class, 0, false, listener85::onTotem));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener85::onUpdate));
      }

      if (subscriber instanceof AntiVoid listener86) {
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener86::onUpdate));
      }

      if (subscriber instanceof AutoHole listener87) {
         listeners.add(new DirectListener<>(KeyboardInputEvent.class, -999, false, listener87::onKeyInput));
         listeners.add(new DirectListener<>(MoveEvent.class, 0, false, listener87::onMove));
         listeners.add(new DirectListener<>(RotationEvent.class, 0, false, listener87::onRotateEvent));
         listeners.add(new DirectListener<>(UpdateRotateEvent.class, -9999, false, listener87::onRotation));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener87::onUpdate));
      }

      if (subscriber instanceof AutoWalk listener88) {
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener88::onUpdate));
      }

      if (subscriber instanceof BlockStrafe listener89) {
         listeners.add(new DirectListener<>(MoveEvent.class, 0, false, listener89::onMove));
      }

      if (subscriber instanceof ElytraFly listener90) {
         listeners.add(new DirectListener<>(TravelEvent.class, 0, false, listener90::onMove));
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener90::onPacketReceive));
         listeners.add(new DirectListener<>(PacketEvent.Send.class, 0, false, listener90::onPacketSend));
         listeners.add(new DirectListener<>(MoveEvent.class, 0, false, listener90::onPlayerMove));
         listeners.add(new DirectListener<>(UpdateRotateEvent.class, -9999, false, listener90::onRotation));
         listeners.add(new DirectListener<>(ClientTickEvent.class, 0, false, listener90::onTick));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener90::onUpdate));
         listeners.add(new DirectListener<>(TravelEvent.class, 0, false, listener90::travel));
      }

      if (subscriber instanceof ElytraFly.FireWorkTweak listener91) {
         listeners.add(new DirectListener<>(ClientTickEvent.class, 0, false, listener91::onTick));
      }

      if (subscriber instanceof FastFall listener92) {
         listeners.add(new DirectListener<>(MoveEvent.class, -100, false, listener92::onMove));
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener92::onPacket));
         listeners.add(new DirectListener<>(TimerEvent.class, 0, false, listener92::onTimer));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener92::onUpdate));
      }

      if (subscriber instanceof FastSwim listener93) {
         listeners.add(new DirectListener<>(KeyboardInputEvent.class, 0, false, listener93::onKeyboardInput));
         listeners.add(new DirectListener<>(MoveEvent.class, 0, false, listener93::onMove));
      }

      if (subscriber instanceof FastWeb listener94) {
         listeners.add(new DirectListener<>(TimerEvent.class, -100, false, listener94::onTimer));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener94::onUpdate));
      }

      if (subscriber instanceof Flatten listener95) {
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener95::onUpdate));
      }

      if (subscriber instanceof Fly listener96) {
         listeners.add(new DirectListener<>(KeyboardInputEvent.class, -100, false, listener96::keyboard));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener96::onUpdate));
      }

      if (subscriber instanceof HoleSnap listener97) {
         listeners.add(new DirectListener<>(KeyboardInputEvent.class, -999, false, listener97::onKeyInput));
         listeners.add(new DirectListener<>(MoveEvent.class, 0, false, listener97::onMove));
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener97::onReceivePacket));
         listeners.add(new DirectListener<>(RotationEvent.class, 0, false, listener97::onRotate));
         listeners.add(new DirectListener<>(TimerEvent.class, -99, false, listener97::onTimer));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener97::onUpdate));
      }

      if (subscriber instanceof NoJumpDelay listener98) {
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener98::onUpdate));
      }

      if (subscriber instanceof NoSlow listener99) {
         listeners.add(new DirectListener<>(BlockSlipperinessEvent.class, 0, false, listener99::onBlockSlipperiness));
         listeners.add(new DirectListener<>(MovementSlowdownEvent.class, 0, false, listener99::onMovementSlowdown));
         listeners.add(new DirectListener<>(PacketEvent.Send.class, 0, false, listener99::onPacketOutbound));
         listeners.add(new DirectListener<>(SetCurrentHandEvent.class, 0, false, listener99::onSetCurrentHand));
         listeners.add(new DirectListener<>(SlowMovementEvent.class, 0, false, listener99::onSlowMovement));
         listeners.add(new DirectListener<>(SteppedOnSlimeBlockEvent.class, 0, false, listener99::onSteppedOnSlimeBlock));
         listeners.add(new DirectListener<>(StrafeFixEvent.class, 0, false, listener99::onStrafeFix));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener99::onUpdate));
         listeners.add(new DirectListener<>(VelocityMultiplierEvent.class, 0, false, listener99::onVelocityMultiplier));
      }

      if (subscriber instanceof PacketFly listener100) {
         listeners.add(new DirectListener<>(MoveEvent.class, 0, false, listener100::invoke));
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener100::invoke));
         listeners.add(new DirectListener<>(TickEvent.class, 0, false, listener100::invoke));
         listeners.add(new DirectListener<>(PacketEvent.Send.class, 0, false, listener100::onPacket));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener100::onUpdate));
      }

      if (subscriber instanceof SafeWalk listener101) {
         listeners.add(new DirectListener<>(KeyboardInputEvent.class, -200, false, listener101::keyboard));
         listeners.add(new DirectListener<>(MoveEvent.class, -100, false, listener101::onMove));
      }

      if (subscriber instanceof Speed listener102) {
         listeners.add(new DirectListener<>(MoveEvent.class, 0, false, listener102::invoke));
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 100, false, listener102::invoke));
         listeners.add(new DirectListener<>(MovedEvent.class, 0, false, listener102::onMove));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener102::onUpdate));
      }

      if (subscriber instanceof Sprint listener103) {
         listeners.add(new DirectListener<>(TickMovementEvent.class, 0, false, listener103::onMove));
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener103::onPacket));
         listeners.add(new DirectListener<>(UpdateRotateEvent.class, -100, false, listener103::rotate));
         listeners.add(new DirectListener<>(SprintEvent.class, 0, false, listener103::sprint));
         listeners.add(new DirectListener<>(TickEvent.class, 0, false, listener103::tick));
      }

      if (subscriber instanceof Step listener104) {
         listeners.add(new DirectListener<>(TickEvent.class, 0, false, listener104::onStep));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener104::onUpdate));
      }

      if (subscriber instanceof Strafe listener105) {
         listeners.add(new DirectListener<>(MoveEvent.class, 0, false, listener105::onStrafe));
      }

      if (subscriber instanceof TargetFollow listener106) {
         listeners.add(new DirectListener<>(KeyboardInputEvent.class, -999, false, listener106::onKeyInput));
         listeners.add(new DirectListener<>(MoveEvent.class, 0, false, listener106::onMove));
         listeners.add(new DirectListener<>(RotationEvent.class, 0, false, listener106::onRotateEvent));
         listeners.add(new DirectListener<>(UpdateRotateEvent.class, -9999, false, listener106::onRotation));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener106::onUpdate));
      }

      if (subscriber instanceof VClip listener107) {
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener107::onUpdate));
      }

      if (subscriber instanceof Velocity listener108) {
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener108::onReceivePacket));
         listeners.add(new DirectListener<>(UpdateRotateEvent.class, 0, false, listener108::onRotate));
         listeners.add(new DirectListener<>(TickEvent.class, 0, false, listener108::onUpdate));
         listeners.add(new DirectListener<>(EntityVelocityUpdateEvent.class, 0, false, listener108::onVelocity));
      }

      if (subscriber instanceof AirPlace listener109) {
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener109::onUpdate));
      }

      if (subscriber instanceof AutoArmor listener110) {
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener110::onUpdate));
      }

      if (subscriber instanceof AutoPearl listener111) {
         listeners.add(new DirectListener<>(RotationEvent.class, 0, false, listener111::onRotate));
         listeners.add(new DirectListener<>(TickEvent.class, 0, false, listener111::onUpdate));
      }

      if (subscriber instanceof AutoPotion listener112) {
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener112::onUpdate));
      }

      if (subscriber instanceof AutoPotion.AutoPotTick listener113) {
         listeners.add(new DirectListener<>(ClientTickEvent.class, 0, false, listener113::onTick));
      }

      if (subscriber instanceof AutoTool listener114) {
         listeners.add(new DirectListener<>(KeyEvent.class, 0, false, listener114::onKey));
         listeners.add(new DirectListener<>(PlaySoundEvent.class, 0, false, listener114::onPlaySound));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener114::onUpdate));
      }

      if (subscriber instanceof FireworkBoost listener115) {
         listeners.add(new DirectListener<>(ClientTickEvent.class, 0, false, listener115::onTick));
      }

      if (subscriber instanceof FreeLook listener116) {
         listeners.add(new DirectListener<>(LookDirectionEvent.class, 0, false, listener116::onLookDirection));
      }

      if (subscriber instanceof Freecam listener117) {
         listeners.add(new DirectListener<>(KeyboardInputEvent.class, 0, false, listener117::onKeyboardInput));
         listeners.add(new DirectListener<>(UpdateRotateEvent.class, 200, false, listener117::onRotate));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener117::onUpdate));
      }

      if (subscriber instanceof InteractTweaks listener118) {
         listeners.add(new DirectListener<>(PacketEvent.Send.class, 0, false, listener118::onPacket));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener118::onUpdate));
      }

      if (subscriber instanceof noxisclient.mod.modules.impl.player.InventoryManager listener119) {
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener119::onUpdate));
      }

      if (subscriber instanceof Macros listener120) {
         listeners.add(new DirectListener<>(RotationEvent.class, 0, false, listener120::onRotate));
         listeners.add(new DirectListener<>(TickEvent.class, 0, false, listener120::onTick));
      }

      if (subscriber instanceof NoFall listener121) {
         listeners.add(new DirectListener<>(PacketEvent.Send.class, 999999, false, listener121::onPacketSend));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener121::onUpdate));
      }

      if (subscriber instanceof PacketEat listener122) {
         listeners.add(new DirectListener<>(PacketEvent.Send.class, 0, false, listener122::onPacket));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener122::onUpdate));
      }

      if (subscriber instanceof PacketMine listener123) {
         listeners.add(new DirectListener<>(ClickBlockEvent.class, 0, false, listener123::onClickBlock));
         listeners.add(new DirectListener<>(EntitySpawnEvent.class, 0, false, listener123::onEntityAdd));
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener123::onPacketReceive));
         listeners.add(new DirectListener<>(PacketEvent.Send.class, 0, false, listener123::onPacketSend));
         listeners.add(new DirectListener<>(ClientTickEvent.class, 0, false, listener123::onTick));
      }

      if (subscriber instanceof Replenish listener124) {
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener124::onUpdate));
      }

      if (subscriber instanceof TimerModule listener125) {
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener125::onReceivePacket));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener125::onUpdate));
      }

      if (subscriber instanceof Yaw listener126) {
         listeners.add(new DirectListener<>(LookDirectionEvent.class, 0, false, listener126::onLookDirection));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener126::onUpdate));
      }

      if (subscriber instanceof Ambience listener127) {
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener127::onReceivePacket));
         listeners.add(new DirectListener<>(PreRender2DEvent.class, 0, false, listener127::onRender2D));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener127::onUpdate));
      }

      if (subscriber instanceof AnimationsModule listener128) {
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener128::onPacketReceive));
         listeners.add(new DirectListener<>(TickEvent.class, 0, false, listener128::onTick));
         listeners.add(new DirectListener<>(ClientTickEvent.class, 0, false, listener128::onUpdate));
      }

      if (subscriber instanceof Chams listener129) {
         listeners.add(new DirectListener<>(Render3DEvent.class, Integer.MIN_VALUE, false, listener129::onRender3D));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener129::onUpdate));
      }

      if (subscriber instanceof ESP listener130) {
         listeners.add(new DirectListener<>(EntitySpawnedEvent.class, 0, false, listener130::onReceivePacket));
      }

      if (subscriber instanceof LogoutSpots listener131) {
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener131::onPacketReceive));
         listeners.add(new DirectListener<>(RemoveEntityEvent.class, 0, false, listener131::onRemoveEntity));
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener131::onUpdate));
      }

      if (subscriber instanceof MaceEffect listener132) {
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener132::onPacketReceive));
         listeners.add(new DirectListener<>(PacketEvent.Send.class, 0, false, listener132::onPacketSend));
      }

      if (subscriber instanceof MotionCamera listener133) {
         listeners.add(new DirectListener<>(ClientTickEvent.class, 0, false, listener133::onUpdate));
      }

      if (subscriber instanceof NameTags listener134) {
         listeners.add(new DirectListener<>(Render3DEvent.class, 0, false, listener134::onRender3D));
      }

      if (subscriber instanceof NoBob listener135) {
         listeners.add(new DirectListener<>(TickEvent.class, 0, false, listener135::onTick));
      }

      if (subscriber instanceof NoRender listener136) {
         listeners.add(new DirectListener<>(PacketEvent.Receive.class, 0, false, listener136::onPacketReceive));
         listeners.add(new DirectListener<>(ParticleEvent.class, 0, false, listener136::onParticle));
         listeners.add(new DirectListener<>(RenderEntityEvent.class, 0, false, listener136::onRender));
         listeners.add(new DirectListener<>(TickEntityEvent.class, 0, false, listener136::onRender));
      }

      if (subscriber instanceof PhaseESP listener137) {
         listeners.add(new DirectListener<>(UpdateEvent.class, 0, false, listener137::onUpdate));
      }

      if (subscriber instanceof PopChams listener138) {
         listeners.add(new DirectListener<>(TotemEvent.class, 0, false, listener138::onTotemPop));
      }

      if (subscriber instanceof TotemParticle listener139) {
         listeners.add(new DirectListener<>(TotemParticleEvent.class, 0, false, listener139::idk));
      }

      if (subscriber instanceof ViewModel listener140) {
         listeners.add(new DirectListener<>(HeldItemRendererEvent.class, 0, false, listener140::onHeldItemRender));
      }

      if (subscriber instanceof Xray listener141) {
         listeners.add(new DirectListener<>(AmbientOcclusionEvent.class, 0, false, listener141::onAmbientOcclusion));
         listeners.add(new DirectListener<>(ChunkOcclusionEvent.class, 0, false, listener141::onChunkOcclusion));
         listeners.add(new DirectListener<>(RenderBlockEntityEvent.class, 0, false, listener141::onRenderBlockEntity));
      }

      if (subscriber instanceof Zoom.ZoomAnim listener142) {
         listeners.add(new DirectListener<>(Render3DEvent.class, 0, false, listener142::onRender3D));
      }

      return listeners;
   }

   static List<IListener> staticListenersFor(Class<?> subscriberClass) {
      List<IListener> listeners = new ArrayList<>();
      return listeners;
   }
}
