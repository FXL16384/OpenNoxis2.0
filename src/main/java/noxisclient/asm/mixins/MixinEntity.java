package noxisclient.asm.mixins;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import noxisclient.Alien;
import noxisclient.api.events.impl.LookDirectionEvent;
import noxisclient.api.events.impl.SlowMovementEvent;
import noxisclient.api.events.impl.SprintEvent;
import noxisclient.api.events.impl.VelocityMultiplierEvent;
import noxisclient.api.utils.Wrapper;
import noxisclient.mod.modules.impl.movement.Velocity;
import noxisclient.mod.modules.impl.render.NoRender;
import noxisclient.mod.modules.impl.render.ShaderModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(
   value = {Entity.class},
   priority = 900
)
public abstract class MixinEntity {
   @Shadow
   private static final int field_29976 = 3;
   @Shadow
   protected Vec3d field_17046;
   @Shadow
   public float field_6017;

   @Inject(
      method = {"changeLookDirection"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookChangeLookDirection(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
      if (Entity.class.cast(this) == Wrapper.mc.player) {
         LookDirectionEvent lookDirectionEvent = LookDirectionEvent.get(Entity.class.cast(this), cursorDeltaX, cursorDeltaY);
         Alien.EVENT_BUS.post(lookDirectionEvent);
         if (lookDirectionEvent.isCancelled()) {
            ci.cancel();
         }
      }
   }

   @Inject(
      at = {@At("HEAD")},
      method = {"isInvisibleTo(Lnet/minecraft/entity/player/PlayerEntity;)Z"},
      cancellable = true
   )
   private void onIsInvisibleCheck(PlayerEntity message, CallbackInfoReturnable<Boolean> cir) {
      if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.invisible.getValue()) {
         cir.setReturnValue(false);
      }
   }

   @Inject(
      method = {"isGlowing"},
      at = {@At("HEAD")},
      cancellable = true
   )
   void isGlowingHook(CallbackInfoReturnable<Boolean> cir) {
      if (ShaderModule.INSTANCE.isOn()) {
         cir.setReturnValue(ShaderModule.INSTANCE.shouldRender(Entity.class.cast(this)));
      }
   }

   @ModifyArgs(
      method = {"pushAwayFrom"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/Entity;addVelocity(DDD)V"
      )
   )
   private void pushAwayFromHook(Args args) {
      if (Entity.class.cast(this) == MinecraftClient.getInstance().player && Velocity.INSTANCE.isOn() && Velocity.INSTANCE.entityPush.getValue()) {
         args.set(0, 0.0);
         args.set(1, 0.0);
         args.set(2, 0.0);
      }
   }

   @Inject(
      method = {"isOnFire"},
      at = {@At("HEAD")},
      cancellable = true
   )
   void isOnFireHook(CallbackInfoReturnable<Boolean> cir) {
      if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.fireEntity.getValue()) {
         cir.setReturnValue(false);
      }
   }

   @Inject(
      method = {"setSprinting"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void setSprintingHook(boolean sprinting, CallbackInfo ci) {
      if (Entity.class.cast(this) == MinecraftClient.getInstance().player) {
         SprintEvent event = SprintEvent.get();
         Alien.EVENT_BUS.post(event);
         if (event.isCancelled()) {
            ci.cancel();
            sprinting = event.isSprint();
            this.method_5729(3, sprinting);
         }
      }
   }

   @Inject(
      method = {"slowMovement"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookSlowMovement(BlockState state, Vec3d multiplier, CallbackInfo ci) {
      if (Entity.class.cast(this) == MinecraftClient.getInstance().player) {
         SlowMovementEvent slowMovementEvent = new SlowMovementEvent(state);
         Alien.EVENT_BUS.post(slowMovementEvent);
         if (slowMovementEvent.isCancelled()) {
            ci.cancel();
            this.field_6017 = 0.0F;
            this.field_17046 = multiplier.multiply((double)slowMovementEvent.getMultiplier());
         }
      }
   }

   @Redirect(
      method = {"getVelocityMultiplier"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/block/BlockState;getBlock()Lnet/minecraft/block/Block;"
      ),
      require = 0
   )
   private Block hookGetVelocityMultiplier(BlockState instance) {
      if (Entity.class.cast(this) != MinecraftClient.getInstance().player) {
         return instance.getBlock();
      } else {
         VelocityMultiplierEvent velocityMultiplierEvent = new VelocityMultiplierEvent(instance);
         Alien.EVENT_BUS.post(velocityMultiplierEvent);
         return velocityMultiplierEvent.isCancelled() ? Blocks.DIRT : instance.getBlock();
      }
   }

   @Shadow
   protected void method_5729(int index, boolean value) {
   }

   @Shadow
   public abstract Box method_5829();
}
