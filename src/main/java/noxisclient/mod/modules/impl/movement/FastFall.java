package noxisclient.mod.modules.impl.movement;

import java.util.HashMap;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import noxisclient.Alien;
import noxisclient.api.events.impl.MoveEvent;
import noxisclient.api.events.impl.PacketEvent;
import noxisclient.api.events.impl.TimerEvent;
import noxisclient.api.events.impl.UpdateEvent;
import noxisclient.api.utils.math.Timer;
import noxisclient.api.utils.player.MovementUtil;
import noxisclient.api.utils.world.BlockPosX;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class FastFall extends Module {
   private final EnumSetting<FastFall.Mode> mode = this.add(new EnumSetting<>("Mode", FastFall.Mode.Fast));
   private final BooleanSetting noLag = this.add(new BooleanSetting("NoLag", true, () -> this.mode.getValue() == FastFall.Mode.Fast));
   private final BooleanSetting useTimerSetting = this.add(new BooleanSetting("UseTimer", false));
   private final SliderSetting timer = this.add(new SliderSetting("Timer", 2.5, 1.0, 8.0, 0.1, this.useTimerSetting::getValue));
   private final BooleanSetting anchor = this.add(new BooleanSetting("Anchor", true));
   private final SliderSetting height = this.add(new SliderSetting("Height", 10.0, 1.0, 20.0, 0.5));
   private final Timer lagTimer = new Timer();
   boolean onGround = false;
   private boolean useTimer;

   public FastFall() {
      super("FastFall", "Miyagi son simulator", Module.Category.Movement);
      this.setChinese("快速坠落");
   }

   @Override
   public void onDisable() {
      this.useTimer = false;
   }

   @Override
   public String getInfo() {
      return this.mode.getValue().name();
   }

   public void onMove(MoveEvent event) {
      if (!nullCheck()) {
         if (mc.player.isOnGround() && this.anchor.getValue() && this.traceDown() != 0 && (double)this.traceDown() <= this.height.getValue() && this.trace()) {
            event.setX(event.getX() * 0.05);
            event.setZ(event.getZ() * 0.05);
         }
      }
   }

   public void onUpdate(UpdateEvent event) {
      if ((!(this.height.getValue() > 0.0) || !((double)this.traceDown() > this.height.getValue()))
         && !mc.player.isInsideWall()
         && !mc.player.isSubmergedInWater()
         && !mc.player.isInLava()
         && !mc.player.isHoldingOntoLadder()
         && this.lagTimer.passed(1000L)
         && !mc.player.isFallFlying()
         && !Fly.INSTANCE.isOn()
         && !nullCheck()) {
         if (!Alien.PLAYER.isInWeb(mc.player)) {
            if (mc.player.isOnGround() && this.mode.getValue() == FastFall.Mode.Fast) {
               MovementUtil.setMotionY(MovementUtil.getMotionY() - (double)(this.noLag.getValue() ? 0.62F : 1.0F));
            }

            if (this.useTimerSetting.getValue()) {
               if (!mc.player.isOnGround()) {
                  if (this.onGround) {
                     this.useTimer = true;
                  }

                  if (MovementUtil.getMotionY() >= 0.0) {
                     this.useTimer = false;
                  }

                  this.onGround = false;
               } else {
                  this.useTimer = false;
                  MovementUtil.setMotionY(-0.08);
                  this.onGround = true;
               }
            } else {
               this.useTimer = false;
            }
         }
      }
   }

   public void onTimer(TimerEvent event) {
      if (!nullCheck()) {
         if (!mc.player.isOnGround() && this.useTimer) {
            event.set(this.timer.getValueFloat());
         }
      }
   }

   public void onPacket(PacketEvent.Receive event) {
      if (!nullCheck() && event.getPacket() instanceof PlayerPositionLookS2CPacket) {
         this.lagTimer.reset();
      }
   }

   private int traceDown() {
      int retval = 0;

      for (int tracey = (int)Math.round(mc.player.getY()) - 1; tracey >= 0; tracey--) {
         BlockHitResult trace = mc.world
            .raycast(
               new RaycastContext(
                  mc.player.getPos(), new Vec3d(mc.player.getX(), (double)tracey, mc.player.getZ()), ShapeType.COLLIDER, FluidHandling.NONE, mc.player
               )
            );
         if (trace != null && trace.getType() == Type.BLOCK) {
            return retval;
         }

         retval++;
      }

      return retval;
   }

   private boolean trace() {
      Box bbox = mc.player.getBoundingBox();
      Vec3d basepos = bbox.getCenter();
      double minX = bbox.minX;
      double minZ = bbox.minZ;
      double maxX = bbox.maxX;
      double maxZ = bbox.maxZ;
      HashMap<Vec3d, Vec3d> positions = new HashMap<>();
      positions.put(basepos, new Vec3d(basepos.x, basepos.y - 1.0, basepos.z));
      positions.put(new Vec3d(minX, basepos.y, minZ), new Vec3d(minX, basepos.y - 1.0, minZ));
      positions.put(new Vec3d(maxX, basepos.y, minZ), new Vec3d(maxX, basepos.y - 1.0, minZ));
      positions.put(new Vec3d(minX, basepos.y, maxZ), new Vec3d(minX, basepos.y - 1.0, maxZ));
      positions.put(new Vec3d(maxX, basepos.y, maxZ), new Vec3d(maxX, basepos.y - 1.0, maxZ));

      for (Vec3d key : positions.keySet()) {
         RaycastContext context = new RaycastContext(key, positions.get(key), ShapeType.COLLIDER, FluidHandling.NONE, mc.player);
         BlockHitResult result = mc.world.raycast(context);
         if (result != null && result.getType() == Type.BLOCK) {
            return false;
         }
      }

      BlockState state = mc.world.getBlockState(new BlockPosX(mc.player.getX(), mc.player.getY() - 1.0, mc.player.getZ()));
      return state.isAir();
   }

   private static enum Mode {
      Fast,
      None;
   }
}
