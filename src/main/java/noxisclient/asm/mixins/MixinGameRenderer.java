package noxisclient.asm.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import noxisclient.Alien;
import noxisclient.api.utils.render.TextUtil;
import noxisclient.api.utils.world.InteractUtil;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.player.Freecam;
import noxisclient.mod.modules.impl.player.InteractTweaks;
import noxisclient.mod.modules.impl.render.AspectRatio;
import noxisclient.mod.modules.impl.render.Chams;
import noxisclient.mod.modules.impl.render.Fov;
import noxisclient.mod.modules.impl.render.HighLight;
import noxisclient.mod.modules.impl.render.NoRender;
import noxisclient.mod.modules.impl.render.Zoom;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({GameRenderer.class})
public class MixinGameRenderer {
   @Shadow
   @Final
   MinecraftClient field_4015;
   @Shadow
   private float field_4019;
   @Shadow
   private float field_3999;
   @Shadow
   private boolean field_4001;
   @Shadow
   private float field_4005;
   @Shadow
   private float field_3988;
   @Shadow
   private float field_4004;
   @Shadow
   private float field_4025;
   @Unique
   private Entity cameraEntity;
   @Unique
   private float originalYaw;
   @Unique
   private float originalPitch;

   @Shadow
   private static HitResult method_56154(HitResult hitResult, Vec3d cameraPos, double interactionRange) {
      Vec3d vec3d = hitResult.getPos();
      if (!vec3d.isInRange(cameraPos, interactionRange)) {
         Vec3d vec3d2 = hitResult.getPos();
         Direction direction = Direction.getFacing(vec3d2.x - cameraPos.x, vec3d2.y - cameraPos.y, vec3d2.z - cameraPos.z);
         return BlockHitResult.createMissed(vec3d2, direction, BlockPos.ofFloored(vec3d2));
      } else {
         return hitResult;
      }
   }

   @Inject(
      method = {"showFloatingItem"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onShowFloatingItem(ItemStack floatingItem, CallbackInfo info) {
      if (floatingItem.getItem() == Items.TOTEM_OF_UNDYING && NoRender.INSTANCE != null && NoRender.INSTANCE.isOn() && NoRender.INSTANCE.totem.getValue()) {
         info.cancel();
      }
   }

   @Redirect(
      method = {"renderWorld"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/math/MathHelper;lerp(FFF)F"
      ),
      require = 0
   )
   private float applyCameraTransformationsMathHelperLerpProxy(float delta, float first, float second) {
      return NoRender.INSTANCE != null && NoRender.INSTANCE.isOn() && NoRender.INSTANCE.nausea.getValue() ? 0.0F : MathHelper.lerp(delta, first, second);
   }

   @Inject(
      method = {"tiltViewWhenHurt"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void tiltViewWhenHurtHook(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
      if (NoRender.INSTANCE != null && NoRender.INSTANCE.isOn() && NoRender.INSTANCE.hurtCam.getValue()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"shouldRenderBlockOutline"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void hookOutline(CallbackInfoReturnable<Boolean> cir) {
      if (HighLight.INSTANCE != null && HighLight.INSTANCE.isOn()) {
         cir.setReturnValue(false);
      }
   }

   @Inject(
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z",
         opcode = 180,
         ordinal = 0
      )},
      method = {"renderWorld"}
   )
   void render3dHook(RenderTickCounter tickCounter, CallbackInfo ci) {
      if (!Module.nullCheck() && Alien.MODULE != null && Alien.FPS != null) {
         Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
         MatrixStack matrixStack = new MatrixStack();
         matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
         matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
         TextUtil.lastProjMat.set(RenderSystem.getProjectionMatrix());
         TextUtil.lastModMat.set(RenderSystem.getModelViewMatrix());
         TextUtil.lastWorldSpaceMatrix.set(matrixStack.peek().getPositionMatrix());
         Alien.FPS.record();
         Alien.MODULE.render3D(matrixStack);
      }
   }

   @Inject(
      at = {@At("TAIL")},
      method = {"renderWorld"}
   )
   void render3dTail(RenderTickCounter tickCounter, CallbackInfo ci) {
      if (Chams.INSTANCE != null && Chams.INSTANCE.isOn() && Chams.INSTANCE.hand.booleanValue) {
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   @Inject(
      method = {"getFov(Lnet/minecraft/client/render/Camera;FZ)D"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void getFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> ci) {
      if (!this.field_4001 && (Fov.INSTANCE != null && Fov.INSTANCE.isOn() || Zoom.on)) {
         double d = 70.0;
         if (changingFov) {
            if (Fov.INSTANCE != null && Fov.INSTANCE.isOn()) {
               double fov = Fov.INSTANCE.fov.getValue();
               if (Zoom.on) {
                  ci.setReturnValue(Math.min(Math.max(fov - Zoom.INSTANCE.currentFov, 1.0), 177.0));
               } else {
                  ci.setReturnValue(fov);
               }

               return;
            }

            d = (double)this.field_4015.options.getFov().getValue().intValue();
            d *= (double)MathHelper.lerp(tickDelta, this.field_3999, this.field_4019);
            if (Zoom.on) {
               d = Math.min(Math.max(d - Zoom.INSTANCE.currentFov, 1.0), 177.0);
            }
         } else if (Fov.INSTANCE != null && Fov.INSTANCE.isOn()) {
            ci.setReturnValue(Fov.INSTANCE.itemFov.getValue());
            return;
         }

         if (camera.getFocusedEntity() instanceof LivingEntity && ((LivingEntity)camera.getFocusedEntity()).isDead()) {
            float f = Math.min((float)((LivingEntity)camera.getFocusedEntity()).deathTime + tickDelta, 20.0F);
            d /= (double)((1.0F - 500.0F / (f + 500.0F)) * 2.0F + 1.0F);
         }

         CameraSubmersionType cameraSubmersionType;
         if ((cameraSubmersionType = camera.getSubmersionType()) == CameraSubmersionType.LAVA || cameraSubmersionType == CameraSubmersionType.WATER) {
            d *= MathHelper.lerp(this.field_4015.options.getFovEffectScale().getValue(), 1.0, 0.85714287F);
         }

         ci.setReturnValue(d);
      }
   }

   @Inject(
      method = {"getNightVisionStrength"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void getNightVisionStrengthHook(LivingEntity entity, float tickDelta, CallbackInfoReturnable<Float> cir) {
      StatusEffectInstance statusEffectInstance = entity.getStatusEffect(StatusEffects.NIGHT_VISION);
      if (statusEffectInstance == null) {
         cir.setReturnValue(1.0F);
      }
   }

   @Inject(
      method = {"renderWorld"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/GameRenderer;renderHand(Lnet/minecraft/client/render/Camera;FLorg/joml/Matrix4f;)V",
         shift = Shift.AFTER
      )}
   )
   public void postRender3dHook(RenderTickCounter tickCounter, CallbackInfo ci) {
      if (Alien.SHADER != null) {
         Alien.SHADER.renderShaders();
      }
   }

   @Inject(
      method = {"getBasicProjectionMatrix"},
      at = {@At("TAIL")},
      cancellable = true
   )
   public void getBasicProjectionMatrixHook(double fov, CallbackInfoReturnable<Matrix4f> cir) {
      if (AspectRatio.INSTANCE != null && AspectRatio.INSTANCE.isOn()) {
         MatrixStack matrixStack = new MatrixStack();
         matrixStack.peek().getPositionMatrix().identity();
         if (this.field_4005 != 1.0F) {
            matrixStack.translate(this.field_3988, -this.field_4004, 0.0F);
            matrixStack.scale(this.field_4005, this.field_4005, 1.0F);
         }

         matrixStack.peek()
            .getPositionMatrix()
            .mul(
               new Matrix4f()
                  .setPerspective((float)(fov * (float) (Math.PI / 180.0)), AspectRatio.INSTANCE.ratio.getValueFloat(), 0.05F, this.field_4025 * 4.0F)
            );
         cir.setReturnValue(matrixStack.peek().getPositionMatrix());
      }
   }

   @Inject(
      method = {"findCrosshairTarget"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void findCrosshairTarget(
      Entity camera, double blockInteractionRange, double entityInteractionRange, float tickDelta, CallbackInfoReturnable<HitResult> cir
   ) {
      if (Freecam.INSTANCE != null && Freecam.INSTANCE.isOn()) {
         cir.setReturnValue(
            InteractUtil.getRtxTarget(
               Freecam.INSTANCE.getFakeYaw(),
               Freecam.INSTANCE.getFakePitch(),
               Freecam.INSTANCE.getFakeX(),
               Freecam.INSTANCE.getFakeY(),
               Freecam.INSTANCE.getFakeZ()
            )
         );
      } else {
         double d = Math.max(blockInteractionRange, entityInteractionRange);
         double e = MathHelper.square(d);
         Vec3d vec3d = camera.getCameraPosVec(tickDelta);
         if (InteractTweaks.INSTANCE != null) {
            InteractTweaks.INSTANCE.isActive = InteractTweaks.INSTANCE.ghostHand();
            HitResult hitResult = camera.raycast(d, tickDelta, false);
            InteractTweaks.INSTANCE.isActive = false;
            double f = hitResult.getPos().squaredDistanceTo(vec3d);
            if (hitResult.getType() != Type.MISS) {
               e = f;
               d = Math.sqrt(f);
            }

            Vec3d vec3d2 = camera.getRotationVec(tickDelta);
            Vec3d vec3d3 = vec3d.add(vec3d2.x * d, vec3d2.y * d, vec3d2.z * d);
            Box box = camera.getBoundingBox().stretch(vec3d2.multiply(d)).expand(1.0, 1.0, 1.0);
            if (!InteractTweaks.INSTANCE.noEntityTrace()) {
               EntityHitResult entityHitResult = ProjectileUtil.raycast(camera, vec3d, vec3d3, box, entity -> !entity.isSpectator() && entity.canHit(), e);
               cir.setReturnValue(
                  entityHitResult != null && entityHitResult.getPos().squaredDistanceTo(vec3d) < f
                     ? method_56154(entityHitResult, vec3d, entityInteractionRange)
                     : method_56154(hitResult, vec3d, blockInteractionRange)
               );
            } else {
               cir.setReturnValue(method_56154(hitResult, vec3d, blockInteractionRange));
            }
         }
      }
   }
}
