package noxisclient.asm.mixins;

import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import noxisclient.mod.modules.impl.player.FreeLook;
import noxisclient.mod.modules.impl.player.Freecam;
import noxisclient.mod.modules.impl.render.AnimationsModule;
import noxisclient.mod.modules.impl.render.CameraClip;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin({Camera.class})
public abstract class MixinCamera {
   @Shadow
   private boolean field_18719;

   @Shadow
   protected abstract float method_19318(float var1);

   @ModifyArgs(
      method = {"update"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/Camera;moveBy(FFF)V",
         ordinal = 0
      )
   )
   private void modifyCameraDistance(Args args) {
      if (CameraClip.INSTANCE.isOn()) {
         args.set(0, -this.method_19318((float)CameraClip.INSTANCE.getDistance()));
      }
   }

   @Inject(
      method = {"clipToSpace"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onClipToSpace(float f, CallbackInfoReturnable<Float> cir) {
      if (CameraClip.INSTANCE.isOn()) {
         cir.setReturnValue((float)CameraClip.INSTANCE.getDistance());
      }
   }

   @Inject(
      method = {"update"},
      at = {@At("TAIL")}
   )
   private void updateHook(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
      if (Freecam.INSTANCE.isOn()) {
         this.field_18719 = true;
      }
   }

   @ModifyArgs(
      method = {"update"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"
      )
   )
   private void setRotationHook(Args args) {
      if (Freecam.INSTANCE.isOn()) {
         args.setAll(Freecam.INSTANCE.getFakeYaw(), Freecam.INSTANCE.getFakePitch());
      } else if (FreeLook.INSTANCE.isOn()) {
         args.setAll(FreeLook.INSTANCE.getFakeYaw(), FreeLook.INSTANCE.getFakePitch());
      }
   }

   @ModifyArgs(
      method = {"update"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V"
      )
   )
   private void setPosHook(Args args) {
      if (Freecam.INSTANCE.isOn()) {
         args.setAll(Freecam.INSTANCE.getFakeX(), Freecam.INSTANCE.getFakeY(), Freecam.INSTANCE.getFakeZ());
      } else if (AnimationsModule.INSTANCE != null && AnimationsModule.INSTANCE.isMotionCameraActive()) {
         args.setAll(AnimationsModule.INSTANCE.getFakeX(), AnimationsModule.INSTANCE.getFakeY(), AnimationsModule.INSTANCE.getFakeZ());
      }
   }
}
