package noxisclient.asm.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import noxisclient.Alien;
import noxisclient.api.events.Event;
import noxisclient.api.events.impl.MoveEvent;
import noxisclient.api.events.impl.MovedEvent;
import noxisclient.api.events.impl.MovementSlowdownEvent;
import noxisclient.api.events.impl.SendMovementPacketsEvent;
import noxisclient.api.events.impl.SetCurrentHandEvent;
import noxisclient.api.events.impl.TickEvent;
import noxisclient.api.events.impl.TickMovementEvent;
import noxisclient.asm.accessors.IClientPlayerEntity;
import noxisclient.core.impl.CommandManager;
import noxisclient.core.impl.RotationManager;
import noxisclient.mod.modules.impl.client.AntiCheat;
import noxisclient.mod.modules.impl.client.ClientSetting;
import noxisclient.mod.modules.impl.exploit.PacketControl;
import noxisclient.mod.modules.impl.movement.Velocity;
import noxisclient.mod.modules.impl.render.SpearFix;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientPlayerEntity.class})
public abstract class MixinClientPlayerEntity extends AbstractClientPlayerEntity {
   @Shadow
   public Input field_3913;
   @Final
   @Shadow
   protected MinecraftClient field_3937;
   @Unique
   private float preYaw;
   @Unique
   private float prePitch;
   @Unique
   private boolean rotation = false;
   @Shadow
   private double field_3926;
   @Shadow
   private double field_3940;
   @Shadow
   private double field_3924;
   @Shadow
   private int field_3923;
   @Shadow
   private float field_3941;
   @Shadow
   private float field_3925;

   public MixinClientPlayerEntity(ClientWorld world, GameProfile profile) {
      super(world, profile);
   }

   @Inject(
      method = {"pushOutOfBlocks"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onPushOutOfBlocksHook(double x, double d, CallbackInfo info) {
      if (Velocity.INSTANCE.isOn() && Velocity.INSTANCE.blockPush.getValue()) {
         info.cancel();
      }
   }

   @Inject(
      at = {@At("HEAD")},
      method = {"tickNausea"},
      cancellable = true
   )
   private void updateNausea(CallbackInfo ci) {
      if (ClientSetting.INSTANCE.portalGui()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"move"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"
      )},
      cancellable = true
   )
   public void onMoveHook(MovementType movementType, Vec3d movement, CallbackInfo ci) {
      MoveEvent event = MoveEvent.get(movement.x, movement.y, movement.z);
      Alien.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         ci.cancel();
      } else if (event.modify) {
         ci.cancel();
         super.move(movementType, new Vec3d(event.getX(), event.getY(), event.getZ()));
         Alien.EVENT_BUS.post(MovedEvent.INSTANCE);
      }
   }

   @Inject(
      method = {"move"},
      at = {@At("TAIL")}
   )
   public void onMoveReturnHook(MovementType movementType, Vec3d movement, CallbackInfo ci) {
      Alien.EVENT_BUS.post(MovedEvent.INSTANCE);
   }

   @Shadow
   @Override
   public abstract float getPitch(float var1);

   @Shadow
   @Override
   public abstract float getYaw(float var1);

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   private void tickHead(CallbackInfo ci) {
      try {
         Alien.EVENT_BUS.post(TickEvent.get(Event.Stage.Pre));
      } catch (Exception var3) {
         var3.printStackTrace();
         if (ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage("§4An error has occurred (ClientPlayerEntity.tick() [HEAD]) Message: [" + var3.getMessage() + "]");
         }
      }
   }

   @Inject(
      method = {"tick"},
      at = {@At("RETURN")}
   )
   private void tickReturn(CallbackInfo ci) {
      try {
         Alien.EVENT_BUS.post(TickEvent.get(Event.Stage.Post));
      } catch (Exception var3) {
         var3.printStackTrace();
         if (ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage("§4An error has occurred (ClientPlayerEntity.tick() [RETURN]) Message: [" + var3.getMessage() + "]");
         }
      }
   }

   @Inject(
      method = {"sendMovementPackets"},
      at = {@At("HEAD")}
   )
   private void onSendMovementPacketsHead(CallbackInfo info) {
      this.rotation();
      if (PacketControl.INSTANCE.isOn()
         && PacketControl.INSTANCE.positionSync.getValue()
         && this.field_3923 >= PacketControl.INSTANCE.positionDelay.getValueInt() - 1) {
         (((IClientPlayerEntity)(Object)this)).setTicksSinceLastPositionPacketSent(50);
      }

      if (RotationManager.snapBack) {
         (((IClientPlayerEntity)(Object)this)).setTicksSinceLastPositionPacketSent(50);
         (((IClientPlayerEntity)(Object)this)).setLastYaw(999.0F);
         RotationManager.snapBack = false;
      } else {
         if (AntiCheat.INSTANCE.fullPackets.getValue()) {
            double d = this.getX() - this.field_3926;
            double e = this.getY() - this.field_3940;
            double f = this.getZ() - this.field_3924;
            double g = (double)(this.getYaw() - this.field_3941);
            double h = (double)(this.getPitch() - this.field_3925);
            boolean bl3 = g != 0.0 || h != 0.0;
            if (AntiCheat.INSTANCE.force.getValue() || !(MathHelper.squaredMagnitude(d, e, f) > MathHelper.square(2.0E-4)) && this.field_3923 >= 19 || bl3) {
               (((IClientPlayerEntity)(Object)this)).setTicksSinceLastPositionPacketSent(50);
               (((IClientPlayerEntity)(Object)this)).setLastYaw(999.0F);
            }
         }
      }
   }

   @Inject(
      method = {"tick"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V",
         ordinal = 0
      )}
   )
   private void onTickHasVehicleBeforeSendPackets(CallbackInfo info) {
      this.rotation();
   }

   @Unique
   private void rotation() {
      this.rotation = true;
      this.preYaw = this.getYaw();
      this.prePitch = this.getPitch();
      SendMovementPacketsEvent event = SendMovementPacketsEvent.get(this.getYaw(), this.getPitch());
      Alien.EVENT_BUS.post(event);
      Alien.ROTATION.rotationYaw = event.getYaw();
      Alien.ROTATION.rotationPitch = event.getPitch();
      this.setYaw(event.getYaw());
      this.setPitch(event.getPitch());
   }

   @Inject(
      method = {"sendMovementPackets"},
      at = {@At("TAIL")}
   )
   private void onSendMovementPacketsTail(CallbackInfo info) {
      if (this.rotation) {
         this.setYaw(this.preYaw);
         this.setPitch(this.prePitch);
         this.rotation = false;
      }
   }

   @Inject(
      method = {"tick"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V",
         ordinal = 1,
         shift = Shift.AFTER
      )}
   )
   private void onTickHasVehicleAfterSendPackets(CallbackInfo info) {
      if (this.rotation) {
         this.setYaw(this.preYaw);
         this.setPitch(this.prePitch);
         this.rotation = false;
      }
   }

   @Inject(
      method = {"tickMovement"},
      at = {@At("HEAD")}
   )
   private void tickMovement(CallbackInfo ci) {
      Alien.EVENT_BUS.post(TickMovementEvent.INSTANCE);
   }

   @Inject(
      method = {"tickMovement"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/input/Input;tick(ZF)V",
         shift = Shift.AFTER
      )}
   )
   private void hookTickMovementPost(CallbackInfo ci) {
      MovementSlowdownEvent movementSlowdownEvent = new MovementSlowdownEvent(this.field_3913);
      Alien.EVENT_BUS.post(movementSlowdownEvent);
   }

   @Inject(
      method = {"tickMovement"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z",
         shift = Shift.AFTER
      )},
      require = 0
   )
   private void spearFix$removeUseSlowdown(CallbackInfo ci) {
      if (SpearFix.INSTANCE != null && SpearFix.INSTANCE.shouldFixUseMovement(this)) {
         this.field_3913.movementForward *= 5.0F;
         this.field_3913.movementSideways *= 5.0F;
      }
   }

   @ModifyExpressionValue(
      method = {"canStartSprinting"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"
      )},
      require = 0
   )
   private boolean spearFix$allowSprinting(boolean original) {
      return SpearFix.INSTANCE != null && SpearFix.INSTANCE.shouldAllowSprint(this) ? false : original;
   }

   @Inject(
      method = {"setCurrentHand"},
      at = {@At("HEAD")}
   )
   private void hookSetCurrentHand(Hand hand, CallbackInfo ci) {
      SetCurrentHandEvent setCurrentHandEvent = new SetCurrentHandEvent(hand);
      Alien.EVENT_BUS.post(setCurrentHandEvent);
      ItemStack stack = this.getStackInHand(hand);
      if (SpearFix.INSTANCE != null && !this.isUsingItem() && !stack.isEmpty()) {
         SpearFix.INSTANCE.playUseSound(stack);
      }
   }
}
