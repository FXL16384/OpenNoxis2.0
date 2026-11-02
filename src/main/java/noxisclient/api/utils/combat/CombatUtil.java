package noxisclient.api.utils.combat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import noxisclient.Vitality;
import noxisclient.api.utils.Wrapper;
import noxisclient.api.utils.math.MathUtil;
import noxisclient.api.utils.math.Timer;
import noxisclient.api.utils.player.EntityUtil;
import noxisclient.api.utils.world.BlockUtil;
import noxisclient.mod.modules.impl.client.AntiCheat;

public class CombatUtil implements Wrapper {
   public static final Timer breakTimer = new Timer();
   public static boolean terrainIgnore = false;
   public static BlockPos modifyPos;
   public static BlockState modifyBlockState = Blocks.AIR.getDefaultState();
   public static Thread modifyThread;

   public static List<PlayerEntity> getEnemies(double range) {
      ArrayList<PlayerEntity> list = new ArrayList<>();

      for (AbstractClientPlayerEntity player : Vitality.THREAD.getPlayers()) {
         if (isValid(player, range)) {
            list.add(player);
         }
      }

      return list;
   }

   public static void attackCrystal(BlockPos pos, boolean rotate, boolean eatingPause) {
      attackCrystal(new Box(pos), rotate, eatingPause);
   }

   public static void attackCrystal(Box box, boolean rotate, boolean eatingPause) {
      for (EndCrystalEntity entity : BlockUtil.getEndCrystals(box)) {
         attackWithDelay(entity, rotate, eatingPause);
      }
   }

   public static void attackWithDelay(Entity entity, boolean rotate, boolean usingPause) {
      if (breakTimer.passed((long)(AntiCheat.INSTANCE.attackDelay.getValue() * 1000.0))) {
         if (!usingPause || !mc.player.isUsingItem()) {
            attack(entity, rotate);
         }
      }
   }

   public static void attack(Entity entity, boolean rotate) {
      if (entity != null) {
         Vec3d attackVec = MathUtil.getClosestPointToBox(mc.player.getEyePos(), entity.getBoundingBox());
         if (mc.player.getEyePos().distanceTo(attackVec) > AntiCheat.INSTANCE.ieRange.getValue()) {
            return;
         }

         breakTimer.reset();
         if (rotate && AntiCheat.INSTANCE.attackRotate.getValue()) {
            Vitality.ROTATION.lookAt(attackVec);
         }

         mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
         mc.player.resetLastAttackedTicks();
         EntityUtil.swingHand(Hand.MAIN_HAND, AntiCheat.INSTANCE.attackSwing.getValue());
         if (rotate && AntiCheat.INSTANCE.attackRotate.getValue()) {
            Vitality.ROTATION.snapBack();
         }
      }
   }

   public static boolean isntValid(Entity entity, double range) {
      return !isValid(entity, range);
   }

   public static boolean isValid(Entity entity, double range) {
      PlayerEntity player;
      boolean invalid = entity == null
         || !entity.isAlive()
         || entity.equals(mc.player)
         || entity instanceof PlayerEntity && Vitality.FRIEND.isFriend(player = (PlayerEntity)entity)
         || mc.player.getPos().distanceTo(entity.getPos()) > range;
      return !invalid;
   }

   public static boolean isValid(Entity entity) {
      PlayerEntity player;
      boolean invalid = entity == null
         || !entity.isAlive()
         || entity.equals(mc.player)
         || entity instanceof PlayerEntity && Vitality.FRIEND.isFriend(player = (PlayerEntity)entity);
      return !invalid;
   }

   public static PlayerEntity getClosestEnemy(double getDistance) {
      PlayerEntity closest = null;

      for (PlayerEntity player : getEnemies(getDistance)) {
         if (closest == null) {
            closest = player;
         } else if (mc.player.squaredDistanceTo(player.getPos()) < mc.player.squaredDistanceTo(closest)) {
            closest = player;
         }
      }

      return closest;
   }
}
