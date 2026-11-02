package noxisclient.mod.modules.impl.movement;

import java.awt.Color;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import noxisclient.Alien;
import noxisclient.api.events.impl.KeyboardInputEvent;
import noxisclient.api.events.impl.MoveEvent;
import noxisclient.api.events.impl.RotationEvent;
import noxisclient.api.events.impl.UpdateEvent;
import noxisclient.api.events.impl.UpdateRotateEvent;
import noxisclient.api.utils.math.Timer;
import noxisclient.api.utils.render.ColorUtil;
import noxisclient.api.utils.render.Render3DUtil;
import noxisclient.core.impl.RotationManager;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.client.AntiCheat;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.ColorSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class AutoHole extends Module {
   private final EnumSetting<AutoHole.Page> page = this.add(new EnumSetting<>("Page", AutoHole.Page.General));
   private final SliderSetting range = this.add(new SliderSetting("Range", 10.0, 1.0, 50.0, 1.0, () -> this.page.getValue() == AutoHole.Page.General));
   private final BooleanSetting onlyElytra = this.add(new BooleanSetting("OnlyElytra", true, () -> this.page.getValue() == AutoHole.Page.General));
   private final BooleanSetting autoMove = this.add(new BooleanSetting("AutoMove", true, () -> this.page.getValue() == AutoHole.Page.General));
   private final BooleanSetting autoMovementSync = this.add(new BooleanSetting("AutoMovementSync", true, () -> this.page.getValue() == AutoHole.Page.General));
   private final BooleanSetting bedrockOnly = this.add(new BooleanSetting("BedrockOnly", true, () -> this.page.getValue() == AutoHole.Page.General));
   private final SliderSetting updateDelay = this.add(new SliderSetting("UpdateDelay", 100, 10, 500, () -> this.page.getValue() == AutoHole.Page.General));
   private final SliderSetting timeoutTicks = this.add(new SliderSetting("TimeoutTicks", 100, 10, 500, () -> this.page.getValue() == AutoHole.Page.General));
   private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true, () -> this.page.getValue() == AutoHole.Page.General));
   private final BooleanSetting renderTarget = this.add(new BooleanSetting("RenderTarget", true, () -> this.page.getValue() == AutoHole.Page.Render));
   private final ColorSetting targetColor = this.add(
      new ColorSetting("TargetColor", new Color(0, 255, 0, 150), () -> this.page.getValue() == AutoHole.Page.Render).injectBoolean(true)
   );
   private final SliderSetting renderHeight = this.add(
      new SliderSetting("RenderHeight", 1.0, 0.1, 3.0, 0.1, () -> this.page.getValue() == AutoHole.Page.Render)
   );
   private final BooleanSetting renderLine = this.add(new BooleanSetting("RenderLine", true, () -> this.page.getValue() == AutoHole.Page.Render));
   private BlockPos currentHole = null;
   private boolean wasMovementSyncEnabled = false;
   private int enabledTicks = 0;
   private int stuckTicks = 0;
   private final Timer holeUpdateTimer = new Timer();
   private boolean wasForwardKeyPressed = false;
   private float wasMovementForward = 0.0F;
   private boolean wasPressingForward = false;
   private float wasMovementSideways = 0.0F;

   public AutoHole() {
      super("AutoHole", Module.Category.Movement);
      this.setChinese("自动进坑");
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

         this.wasForwardKeyPressed = mc.options.forwardKey.isPressed();
         this.wasMovementForward = mc.player.input.movementForward;
         this.wasPressingForward = mc.player.input.pressingForward;
         this.wasMovementSideways = mc.player.input.movementSideways;
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
      this.currentHole = null;
      this.stuckTicks = 0;
      this.enabledTicks = 0;
      mc.options.forwardKey.setPressed(this.wasForwardKeyPressed);
      mc.player.input.movementForward = this.wasMovementForward;
      mc.player.input.pressingForward = this.wasPressingForward;
      mc.player.input.movementSideways = this.wasMovementSideways;
   }

   public void onUpdate(UpdateEvent event) {
   }

   public void onRotation(UpdateRotateEvent event) {
      if (!nullCheck()) {
         if (this.holeUpdateTimer.passedMs((double)((long)this.updateDelay.getValue()))) {
            BlockPos hole = this.getClosestHole(this.range.getValueFloat());
            this.currentHole = hole;
            this.holeUpdateTimer.reset();
         }

         if (this.currentHole == null) {
            this.disable();
            return;
         }

         this.enabledTicks++;
         if ((double)this.enabledTicks > this.timeoutTicks.getValue() - 1.0) {
            this.disable();
            return;
         }

         boolean shouldTrack = !this.onlyElytra.getValue() || ElytraFly.INSTANCE.isFallFlying();
         if (this.currentHole != null && shouldTrack) {
            Vec3d pos = new Vec3d((double)this.currentHole.getX() + 0.5, (double)this.currentHole.getY() + 0.5, (double)this.currentHole.getZ() + 0.5);
            if (Alien.HOLE.isDoubleHole(this.currentHole)) {
               Direction facing = Alien.HOLE.is3Block(this.currentHole);
               if (facing != null) {
                  pos = pos.add(
                     new Vec3d((double)facing.getVector().getX() * 0.5, (double)facing.getVector().getY() * 0.5, (double)facing.getVector().getZ() * 0.5)
                  );
               }
            }

            float[] rotations = RotationManager.getRotation(pos);
            event.setYaw(rotations[0]);
            event.setPitch(rotations[1]);
            double distance = mc.player.getPos().distanceTo(pos);
            if (this.autoDisable.getValue()
               && Math.abs(mc.player.getX() - pos.x) < 0.25
               && Math.abs(mc.player.getZ() - pos.z) < 0.25
               && mc.player.getY() <= (double)this.currentHole.getY() + 0.8) {
               this.disable();
               return;
            }

            this.stuckTicks = mc.player.horizontalCollision ? ++this.stuckTicks : 0;
            if (this.autoDisable.getValue() && this.stuckTicks > 8) {
               this.disable();
               return;
            }

            if (this.autoMove.getValue()) {
               mc.options.forwardKey.setPressed(true);
               mc.player.input.movementForward = 1.0F;
               mc.player.input.pressingForward = true;
            }
         }
      }
   }

   private BlockPos getClosestHole(float range) {
      return Alien.HOLE.getHole(range, true, false, false);
   }

   public void onMove(MoveEvent event) {
      if (!nullCheck() && this.currentHole != null && this.autoMove.getValue() && !AntiCheat.INSTANCE.grimServer.getValue()) {
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
      if (!nullCheck() && this.currentHole != null && this.autoMove.getValue() && AntiCheat.INSTANCE.grimServer.getValue()) {
         boolean shouldTrack = !this.onlyElytra.getValue() || ElytraFly.INSTANCE.isFallFlying();
         if (shouldTrack) {
            mc.player.input.movementSideways = 0.0F;
            mc.player.input.movementForward = 1.0F;
         }
      }
   }

   public void onRotateEvent(RotationEvent event) {
      if (!nullCheck() && this.currentHole != null && AntiCheat.INSTANCE.grimServer.getValue()) {
         boolean shouldTrack = !this.onlyElytra.getValue() || ElytraFly.INSTANCE.isFallFlying();
         if (shouldTrack) {
            Vec3d pos = new Vec3d((double)this.currentHole.getX() + 0.5, (double)this.currentHole.getY() + 0.5, (double)this.currentHole.getZ() + 0.5);
            event.setTarget(pos, 1.0F, 1.0F);
         }
      }
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      if (!nullCheck() && this.isOn() && this.renderTarget.getValue() && this.currentHole != null) {
         Box box = new Box(
            (double)this.currentHole.getX(),
            (double)this.currentHole.getY(),
            (double)this.currentHole.getZ(),
            (double)(this.currentHole.getX() + 1),
            (double)this.currentHole.getY() + this.renderHeight.getValue(),
            (double)(this.currentHole.getZ() + 1)
         );
         Render3DUtil.drawFill(matrixStack, box, this.targetColor.getValue());
         Render3DUtil.drawBox(matrixStack, box, ColorUtil.injectAlpha(this.targetColor.getValue(), 255));
         if (this.renderLine.getValue()) {
            Vec3d playerPos = mc.player.getPos().add(0.0, (double)(mc.player.getEyeHeight(mc.player.getPose()) / 2.0F), 0.0);
            Vec3d holeCenter = new Vec3d((double)this.currentHole.getX() + 0.5, (double)this.currentHole.getY() + 0.5, (double)this.currentHole.getZ() + 0.5);
            Render3DUtil.drawLine(playerPos, holeCenter, this.targetColor.getValue());
         }
      }
   }

   private static enum Page {
      General,
      Render;
   }
}
