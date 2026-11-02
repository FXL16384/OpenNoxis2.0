package noxisclient.api.utils.entity;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import noxisclient.api.utils.Wrapper;
import noxisclient.api.utils.math.PredictUtil;
import noxisclient.asm.accessors.ILivingEntity;

public class CopyPlayerEntity extends PlayerEntity {
   private final boolean onGround;

   public CopyPlayerEntity(PlayerEntity player) {
      this(player, false, 0.0, 0, 0, false, false, false, false);
   }

   public CopyPlayerEntity(PlayerEntity player, boolean effect) {
      super(Wrapper.mc.world, player.getBlockPos(), player.getYaw(), new GameProfile(player.getGameProfile().getId(), player.getGameProfile().getName()));
      this.copyPositionAndRotation(player);
      this.prevX = player.prevX;
      this.prevZ = player.prevZ;
      this.prevY = player.prevY;
      this.bodyYaw = player.bodyYaw;
      this.headYaw = player.headYaw;
      this.handSwingProgress = player.handSwingProgress;
      this.handSwingTicks = player.handSwingTicks;
      this.limbAnimator.setSpeed(player.limbAnimator.getSpeed());
      this.limbAnimator.pos = player.limbAnimator.getPos();
      ((ILivingEntity)this).setLeaningPitch(((ILivingEntity)player).getLeaningPitch());
      ((ILivingEntity)this).setLastLeaningPitch(((ILivingEntity)player).getLeaningPitch());
      this.touchingWater = player.isTouchingWater();
      this.setSneaking(player.isSneaking());
      this.setPose(player.getPose());
      this.setFlag(7, player.isFallFlying());
      this.onGround = player.isOnGround();
      this.setOnGround(this.onGround);
      this.setVelocity(player.getVelocity());
      this.getInventory().clone(player.getInventory());
      if (effect) {
         for (StatusEffectInstance se : new ArrayList<>(player.getStatusEffects())) {
            this.addStatusEffect(se);
         }
      }

      this.setAbsorptionAmountUnclamped(player.getAbsorptionAmount());
      this.setHealth(player.getHealth());
      this.setBoundingBox(player.getBoundingBox());
   }

   public CopyPlayerEntity(
      PlayerEntity player, boolean effect, double maxMotionY, int ticks, int simulation, boolean step, boolean doubleStep, boolean jump, boolean inBlockPause
   ) {
      this(player, effect);
      if (ticks > 0) {
         this.setPosition(PredictUtil.getPos(player, maxMotionY, ticks, simulation, step, doubleStep, jump, inBlockPause));
      }
   }

   @Override
   public boolean isOnGround() {
      return this.onGround;
   }

   @Override
   public boolean isSpectator() {
      return false;
   }

   @Override
   public boolean isCreative() {
      return false;
   }
}
