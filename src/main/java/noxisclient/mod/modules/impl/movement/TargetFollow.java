package noxisclient.mod.modules.impl.movement;

import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import noxisclient.Alien;
import noxisclient.api.events.impl.KeyboardInputEvent;
import noxisclient.api.events.impl.MoveEvent;
import noxisclient.api.events.impl.RotationEvent;
import noxisclient.api.events.impl.UpdateEvent;
import noxisclient.api.events.impl.UpdateRotateEvent;
import noxisclient.api.utils.math.MathUtil;
import noxisclient.core.impl.RotationManager;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.client.AntiCheat;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class TargetFollow extends Module {
   public static TargetFollow INSTANCE;
   private final SliderSetting range = this.add(new SliderSetting("Range", 50.0, 1.0, 250.0, 1.0));
   private final BooleanSetting onlyElytra = this.add(new BooleanSetting("OnlyElytra", true));
   private final BooleanSetting autoMove = this.add(new BooleanSetting("AutoMove", true));
   private final BooleanSetting autoMovementSync = this.add(new BooleanSetting("AutoMovementSync", true));
   private PlayerEntity lastTarget = null;
   private PlayerEntity currentTarget = null;
   private float targetYaw = 0.0F;
   private float targetPitch = 0.0F;
   private boolean wasMovementSyncEnabled = false;

   public TargetFollow() {
      super("Follower", Module.Category.Movement);
      INSTANCE = this;
      this.setChinese("自动追人");
   }

   @Override
   public void onEnable() {
      if (!nullCheck()) {
         if (this.autoMovementSync.getValue()) {
            this.wasMovementSyncEnabled = MovementSync.INSTANCE.isOn();
            if (!this.wasMovementSyncEnabled) {
               MovementSync.INSTANCE.enable();
            }
         }

         if (ElytraFly.INSTANCE.isOn() && ElytraFly.INSTANCE.mode.is(ElytraFly.Mode.Control)) {
            this.disable();
         }
      }
   }

   @Override
   public void onDisable() {
      if (this.autoMovementSync.getValue() && !this.wasMovementSyncEnabled) {
         MovementSync.INSTANCE.disable();
      }

      this.wasMovementSyncEnabled = false;
      this.currentTarget = null;
      if (!nullCheck()) {
         this.syncForwardKeyWithKeyboard();
      }
   }

   public void onUpdate(UpdateEvent event) {
      if (!nullCheck() && this.currentTarget != null && this.autoMove.getValue()) {
         boolean shouldTrack = !this.onlyElytra.getValue() || ElytraFly.INSTANCE.isFallFlying();
         if (shouldTrack) {
            this.forceForwardInput();
         }
      }
   }

   public void onRotation(UpdateRotateEvent event) {
      if (!nullCheck()) {
         PlayerEntity target = this.getClosestPlayer(this.range.getValue());
         boolean shouldTrack = !this.onlyElytra.getValue() || ElytraFly.INSTANCE.isFallFlying();
         this.currentTarget = target;
         this.lastTarget = target;
         if (target != null && shouldTrack) {
            Vec3d pos = MathUtil.getClosestPointToBox(mc.player.getEyePos(), target.getBoundingBox());
            float[] rotations = RotationManager.getRotation(pos);
            this.targetYaw = rotations[0];
            this.targetPitch = rotations[1];
            event.setYaw(rotations[0]);
            event.setPitch(rotations[1]);
            if (this.autoMove.getValue()) {
               this.forceForwardInput();
            }
         }
      }
   }

   private void forceForwardInput() {
      mc.options.forwardKey.setPressed(true);
      mc.player.input.movementForward = 1.0F;
      mc.player.input.pressingForward = true;
   }

   private PlayerEntity getClosestPlayer(double range) {
      PlayerEntity target = null;
      double distance = range;

      for (Entity entity : Alien.THREAD.getEntities()) {
         if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity)entity;
            if (entity != mc.player && !Alien.FRIEND.isFriend(player.getName().getString()) && (double)mc.player.distanceTo(entity) <= range) {
               if (target == null) {
                  target = player;
                  distance = (double)mc.player.distanceTo(entity);
               } else if ((double)mc.player.distanceTo(entity) < distance) {
                  target = player;
                  distance = (double)mc.player.distanceTo(entity);
               }
            }
         }
      }

      return target;
   }

   public void onMove(MoveEvent event) {
      if (!nullCheck() && this.currentTarget != null && this.autoMove.getValue() && !AntiCheat.INSTANCE.grimServer.getValue()) {
         boolean shouldTrack = !this.onlyElytra.getValue() || ElytraFly.INSTANCE.isFallFlying();
         if (shouldTrack && mc.player.isOnGround()) {
            float yaw = mc.player.getYaw();
            double speed = 0.2873;
            double x = (double)(-MathHelper.sin(yaw * (float) (Math.PI / 180.0))) * speed;
            double z = (double)MathHelper.cos(yaw * (float) (Math.PI / 180.0)) * speed;
            event.setX(x);
            event.setZ(z);
         }
      }
   }

   public void onKeyInput(KeyboardInputEvent event) {
      if (!nullCheck() && this.currentTarget != null && this.autoMove.getValue() && AntiCheat.INSTANCE.grimServer.getValue()) {
         boolean shouldTrack = !this.onlyElytra.getValue() || ElytraFly.INSTANCE.isFallFlying();
         if (shouldTrack) {
            mc.player.input.movementSideways = 0.0F;
            mc.player.input.movementForward = 1.0F;
         }
      }
   }

   public void onRotateEvent(RotationEvent event) {
      if (!nullCheck() && this.currentTarget != null && AntiCheat.INSTANCE.grimServer.getValue()) {
         boolean shouldTrack = !this.onlyElytra.getValue() || ElytraFly.INSTANCE.isFallFlying();
         if (shouldTrack) {
            Vec3d pos = MathUtil.getClosestPointToBox(mc.player.getEyePos(), this.currentTarget.getBoundingBox());
            event.setTarget(pos, 1.0F, 1.0F);
         }
      }
   }

   private void syncForwardKeyWithKeyboard() {
      boolean forwardPressed = this.isForwardKeyPhysicallyPressed();
      mc.options.forwardKey.setPressed(forwardPressed);
      mc.player.input.pressingForward = forwardPressed;
      mc.player.input.movementForward = forwardPressed ? 1.0F : 0.0F;
   }

   private boolean isForwardKeyPhysicallyPressed() {
      return InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.forwardKey.getDefaultKey().getCode());
   }

   public boolean shouldControlFireworkBoost() {
      return this.isOn() && !nullCheck() && this.currentTarget != null && (!this.onlyElytra.getValue() || ElytraFly.INSTANCE.isFallFlying());
   }

   public Vec3d getFireworkBoostVector() {
      float pitchRad = this.targetPitch * (float) (Math.PI / 180.0);
      float yawRad = -this.targetYaw * (float) (Math.PI / 180.0);
      float yawCos = MathHelper.cos(yawRad);
      float yawSin = MathHelper.sin(yawRad);
      float pitchCos = MathHelper.cos(pitchRad);
      float pitchSin = MathHelper.sin(pitchRad);
      return new Vec3d((double)(yawSin * pitchCos), (double)(-pitchSin), (double)(yawCos * pitchCos));
   }
}
