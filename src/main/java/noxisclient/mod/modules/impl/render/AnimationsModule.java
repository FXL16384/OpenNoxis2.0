package noxisclient.mod.modules.impl.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket;
import noxisclient.api.events.impl.ClientTickEvent;
import noxisclient.api.events.impl.PacketEvent;
import noxisclient.api.events.impl.TickEvent;
import noxisclient.api.utils.math.AnimateUtil;
import noxisclient.api.utils.math.MathUtil;
import noxisclient.asm.accessors.IBundlePacket;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class AnimationsModule extends Module {
   public static AnimationsModule INSTANCE;
   public final BooleanSetting noSwitchAnimation = this.add(new BooleanSetting("NoSwitchAnimation", false));
   public final BooleanSetting oldSwingAnimation = this.add(new BooleanSetting("OldSwingAnimation", false));
   public final BooleanSetting swingSpeed = this.add(new BooleanSetting("SwingSpeed", false));
   public final SliderSetting swingFactor = this.add(new SliderSetting("SwingFactor", 6, 1, 20, () -> this.swingSpeed.getValue()));
   public final BooleanSetting selfOnly = this.add(new BooleanSetting("SelfOnly", true, () -> this.swingSpeed.getValue()));
   public final BooleanSetting eatTransform = this.add(new BooleanSetting("EatTransform", false));
   public final SliderSetting eatTransformFactor = this.add(new SliderSetting("EatTransformFactor", 1.0, 0.0, 1.0, 0.01, () -> this.eatTransform.getValue()));
   public final BooleanSetting noLimbSwing = this.add(new BooleanSetting("NoLimbSwing", false));
   public final BooleanSetting noInterpolation = this.add(new BooleanSetting("NoInterpolation", false, () -> this.noLimbSwing.getValue()));
   public final BooleanSetting noBob = this.add(new BooleanSetting("NoBob", false));
   public final BooleanSetting motionCamera = this.add(new BooleanSetting("MotionCamera", false).setParent().injectTask(this::resetMotionCamera));
   public final BooleanSetting noFirstPerson = this.add(new BooleanSetting("NoFirstPerson", true, this.motionCamera::isOpen));
   public final SliderSetting firstPersonSpeed = this.add(new SliderSetting("FirstPersonSpeed", 0.6, 0.0, 1.0, 0.01, this.motionCamera::isOpen));
   public final SliderSetting speed = this.add(new SliderSetting("Speed", 0.3, 0.0, 1.0, 0.01, this.motionCamera::isOpen));
   private double fakeX;
   private double fakeY;
   private double fakeZ;
   private double prevFakeX;
   private double prevFakeY;
   private double prevFakeZ;
   private boolean motionCameraInitialized;

   public AnimationsModule() {
      super("Animations", Module.Category.Render);
      this.setChinese("动画");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.resetMotionCamera();
   }

   public boolean shouldNoSwitchAnimation() {
      return this.isOn() && this.noSwitchAnimation.getValue();
   }

   public boolean shouldOldSwingAnimation() {
      return this.isOn() && this.oldSwingAnimation.getValue();
   }

   public boolean shouldModifySwingSpeed(LivingEntity entity) {
      return this.isOn() && this.swingSpeed.getValue() && (!this.selfOnly.getValue() || entity == mc.player);
   }

   public int getSwingSpeed() {
      return this.swingFactor.getValueInt();
   }

   public boolean shouldTransformEat() {
      return this.isOn() && this.eatTransform.getValue();
   }

   public float getEatTransformFactor() {
      return this.eatTransformFactor.getValueFloat();
   }

   public boolean shouldCancelLimbSwing() {
      return this.isOn() && this.noLimbSwing.getValue();
   }

   public boolean shouldNoInterpolation() {
      return this.shouldCancelLimbSwing() && this.noInterpolation.getValue();
   }

   public boolean shouldNoBob() {
      return this.isOn() && this.noBob.getValue();
   }

   public boolean isMotionCameraActive() {
      return this.isOn() && this.motionCamera.getValue() && (!this.noFirstPerson.getValue() || !mc.options.getPerspective().isFirstPerson());
   }

   public void resetMotionCamera() {
      if (this.motionCamera.getValue() && !nullCheck()) {
         this.fakeX = mc.player.getX();
         this.fakeY = mc.player.getY() + (double)mc.player.getEyeHeight(mc.player.getPose());
         this.fakeZ = mc.player.getZ();
         this.prevFakeX = this.fakeX;
         this.prevFakeY = this.fakeY;
         this.prevFakeZ = this.fakeZ;
         this.motionCameraInitialized = true;
      }
   }

   public double getFakeX() {
      return MathUtil.interpolate(this.prevFakeX, this.fakeX, (double)mc.getRenderTickCounter().getTickDelta(true));
   }

   public double getFakeY() {
      return MathUtil.interpolate(this.prevFakeY, this.fakeY, (double)mc.getRenderTickCounter().getTickDelta(true));
   }

   public double getFakeZ() {
      return MathUtil.interpolate(this.prevFakeZ, this.fakeZ, (double)mc.getRenderTickCounter().getTickDelta(true));
   }

   public void onTick(TickEvent event) {
      if (!event.isPre() && mc.player != null && this.shouldNoBob()) {
         mc.player.horizontalSpeed *= 0.0F;
      }
   }

   public void onUpdate(ClientTickEvent event) {
      if (!event.isPre() && !nullCheck() && this.motionCamera.getValue()) {
         if (!this.motionCameraInitialized) {
            this.resetMotionCamera();
         } else {
            this.prevFakeX = this.fakeX;
            this.prevFakeY = this.fakeY;
            this.prevFakeZ = this.fakeZ;
            double speed = mc.options.getPerspective().isFirstPerson() ? this.firstPersonSpeed.getValue() : this.speed.getValue();
            this.fakeX = AnimateUtil.animate(this.fakeX, mc.player.getX(), speed);
            this.fakeY = AnimateUtil.animate(this.fakeY, mc.player.getY() + (double)mc.player.getEyeHeight(mc.player.getPose()), speed);
            this.fakeZ = AnimateUtil.animate(this.fakeZ, mc.player.getZ(), speed);
         }
      }
   }

   public void onPacketReceive(PacketEvent.Receive event) {
      if (mc.player != null && this.shouldOldSwingAnimation()) {
         Packet<?> packet = event.getPacket();
         if (packet instanceof BundleS2CPacket bundle) {
            List<Packet<? super ClientPlayPacketListener>> packets = new ArrayList<>();

            for (Packet<? super ClientPlayPacketListener> bundledPacket : bundle.getPackets()) {
               if (!this.isSelfSwingPacket(bundledPacket)) {
                  packets.add(bundledPacket);
               }
            }

            ((IBundlePacket)bundle).setPackets(packets);
         } else if (this.isSelfSwingPacket(packet)) {
            event.cancel();
         }
      }
   }

   private boolean isSelfSwingPacket(Packet<?> packet) {
      return !(packet instanceof EntityAnimationS2CPacket animation)
         ? false
         : animation.getEntityId() == mc.player.getId() && (animation.getAnimationId() == 0 || animation.getAnimationId() == 3);
   }
}
