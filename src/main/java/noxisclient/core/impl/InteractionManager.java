package noxisclient.core.impl;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import noxisclient.Alien;
import noxisclient.api.utils.Wrapper;
import noxisclient.api.utils.world.BlockUtil;
import noxisclient.mod.modules.Module;

public final class InteractionManager implements Wrapper {
   private final Map<Integer, Integer> placedOnEntities = new ConcurrentHashMap<>();

   public boolean canPlace(BlockPos pos, Block block) {
      BlockState state = block.getDefaultState();
      VoxelShape shape = state.getOutlineShape(mc.world, pos, ShapeContext.absent()).offset((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
      if (!shape.isEmpty()) {
         for (Entity entity : mc.world.getOtherEntities(null, shape.getBoundingBox())) {
            if (!entity.isSpectator()
               && entity.isAlive()
               && VoxelShapes.matchesAnywhere(shape, VoxelShapes.cuboid(entity.getBoundingBox()), BooleanBiFunction.AND)
               && (!(entity instanceof ItemFrameEntity) || this.placedOnEntities.getOrDefault(entity.getId(), 0) > 0)) {
               return false;
            }
         }
      }

      return true;
   }

   public boolean placeBlock(BlockPos pos, int slot, boolean strictDirection, boolean clientSwing, InteractionManager.RotationCallback rotationCallback) {
      return this.placeBlock(pos, slot, strictDirection, clientSwing, rotationCallback, false);
   }

   public boolean placeBlock(
      BlockPos pos, int slot, boolean strictDirection, boolean clientSwing, InteractionManager.RotationCallback rotationCallback, boolean airPlace
   ) {
      return this.placeBlock(pos, slot, strictDirection, clientSwing, true, airPlace, rotationCallback);
   }

   public boolean placeBlock(
      BlockPos pos, int slot, boolean strictDirection, boolean clientSwing, boolean packet, InteractionManager.RotationCallback rotationCallback
   ) {
      return this.placeBlock(pos, slot, strictDirection, clientSwing, packet, false, rotationCallback);
   }

   public boolean placeBlock(
      BlockPos pos,
      int slot,
      boolean strictDirection,
      boolean clientSwing,
      boolean packet,
      boolean airPlace,
      InteractionManager.RotationCallback rotationCallback
   ) {
      if (mc.player == null || mc.world == null) {
         return false;
      } else if (this.isEntityBlockingPlacement(pos)) {
         return false;
      } else {
         Direction direction = this.getInteractDirectionInternal(pos, strictDirection);
         if (airPlace && direction == null) {
            direction = Direction.DOWN;
            return this.placeBlock(pos, direction, slot, clientSwing, false, packet, rotationCallback);
         } else if (direction == null) {
            return false;
         } else {
            BlockPos neighbor = pos.offset(direction.getOpposite());
            BlockUtil.placedPos.add(pos);
            return this.placeBlock(neighbor, direction, slot, clientSwing, false, packet, rotationCallback);
         }
      }
   }

   public boolean placeBlock(
      BlockPos pos, Direction direction, int slot, boolean clientSwing, boolean grimAirPlace, InteractionManager.RotationCallback rotationCallback
   ) {
      return this.placeBlock(pos, direction, slot, clientSwing, grimAirPlace, true, rotationCallback);
   }

   public boolean placeBlock(
      BlockPos pos,
      Direction direction,
      int slot,
      boolean clientSwing,
      boolean grimAirPlace,
      boolean packet,
      InteractionManager.RotationCallback rotationCallback
   ) {
      Vec3d hitVec = pos.toCenterPos()
         .add(new Vec3d((double)direction.getVector().getX(), (double)direction.getVector().getY(), (double)direction.getVector().getZ()).multiply(0.5));
      return this.placeBlock(new BlockHitResult(hitVec, direction, pos, false), slot, clientSwing, grimAirPlace, packet, rotationCallback);
   }

   public boolean placeBlock(
      BlockHitResult hitResult, int slot, boolean clientSwing, boolean grimAirPlace, InteractionManager.RotationCallback rotationCallback
   ) {
      return this.placeBlock(hitResult, slot, clientSwing, grimAirPlace, true, rotationCallback);
   }

   public boolean placeBlock(
      BlockHitResult hitResult, int slot, boolean clientSwing, boolean grimAirPlace, boolean packet, InteractionManager.RotationCallback rotationCallback
   ) {
      if (mc.player != null && mc.world != null) {
         boolean spoofing = slot != Alien.INVENTORY.getServerSlot();
         if (spoofing) {
            Alien.INVENTORY.setSlot(slot);
         }

         if (grimAirPlace) {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
         }

         boolean rotating = rotationCallback != null;
         if (rotating) {
            float[] angles = RotationManager.getRotation(mc.player.getEyePos(), hitResult.getPos());
            rotationCallback.handleRotation(true, angles);
         }

         boolean result = this.placeBlockImmediately(hitResult, grimAirPlace ? Hand.OFF_HAND : Hand.MAIN_HAND, clientSwing, packet);
         if (rotating) {
            float[] angles = RotationManager.getRotation(mc.player.getEyePos(), hitResult.getPos());
            rotationCallback.handleRotation(false, angles);
         }

         if (grimAirPlace) {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
         }

         if (spoofing) {
            Alien.INVENTORY.syncToClient();
         }

         return result;
      } else {
         return false;
      }
   }

   public boolean placeBlockImmediately(BlockHitResult result, Hand hand, boolean clientSwing, boolean packet) {
      ActionResult actionResult = packet ? this.placeBlockPacket(result, hand) : mc.interactionManager.interactBlock(mc.player, hand, result);
      if (actionResult.isAccepted() && actionResult.shouldSwingHand()) {
         if (clientSwing) {
            mc.player.swingHand(Hand.MAIN_HAND);
         } else {
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
         }
      }

      return actionResult.isAccepted();
   }

   public ActionResult placeBlockPacket(BlockHitResult hitResult, Hand hand) {
      Module.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(hand, hitResult, id));
      return ActionResult.SUCCESS;
   }

   public Direction getInteractDirection(BlockPos blockPos, boolean strictDirection) {
      Direction direction = this.getInteractDirectionInternal(blockPos, strictDirection);
      return direction == null ? Direction.UP : direction;
   }

   public Direction getInteractDirectionInternal(BlockPos blockPos, boolean strictDirection) {
      return this.getInteractDirectionInternal(blockPos, strictDirection, false);
   }

   public Direction getInteractDirectionInternal(BlockPos blockPos, boolean strictDirection, boolean grimV3) {
      Set<Direction> validDirections = this.getPlaceDirectionsNCP(mc.player.getEyePos(), blockPos.toCenterPos());
      Direction interactDirection = null;

      for (Direction direction : Direction.values()) {
         BlockPos neighbor = blockPos.offset(direction);
         Direction side = direction.getOpposite();
         BlockState state = mc.world.getBlockState(neighbor);
         if (!state.isAir()
            && state.getFluidState().isEmpty()
            && state.getBlock() != Blocks.ANVIL
            && state.getBlock() != Blocks.CHIPPED_ANVIL
            && state.getBlock() != Blocks.DAMAGED_ANVIL
            && (!grimV3 || !BlockUtil.isClickable(state.getBlock()))
            && (!strictDirection || validDirections.contains(side))
            && (!grimV3 || BlockUtil.grimStrictDirectionCheck(neighbor, side, mc.world, mc.player))) {
            interactDirection = direction;
            break;
         }
      }

      return interactDirection == null ? null : interactDirection.getOpposite();
   }

   public Direction getPlaceDirectionNCP(BlockPos blockPos, boolean visible) {
      Vec3d eyePos = new Vec3d(mc.player.getX(), mc.player.getY() + (double)mc.player.getStandingEyeHeight(), mc.player.getZ());
      if ((double)blockPos.getX() == eyePos.getX() && (double)blockPos.getY() == eyePos.getY() && (double)blockPos.getZ() == eyePos.getZ()) {
         return Direction.DOWN;
      } else {
         for (Direction direction : this.getPlaceDirectionsNCP(eyePos, blockPos.toCenterPos())) {
            if (!visible || mc.world.isAir(blockPos.offset(direction))) {
               return direction;
            }
         }

         return Direction.UP;
      }
   }

   public Set<Direction> getPlaceDirectionsNCP(Vec3d eyePos, Vec3d blockPos) {
      return this.getPlaceDirectionsNCP(eyePos.x, eyePos.y, eyePos.z, blockPos.x, blockPos.y, blockPos.z);
   }

   public Set<Direction> getPlaceDirectionsNCP(double x, double y, double z, double dx, double dy, double dz) {
      double xdiff = x - dx;
      double ydiff = y - dy;
      double zdiff = z - dz;
      Set<Direction> dirs = new HashSet<>(6);
      if (ydiff > 0.5) {
         dirs.add(Direction.UP);
      } else if (ydiff < -0.5) {
         dirs.add(Direction.DOWN);
      } else {
         dirs.add(Direction.UP);
         dirs.add(Direction.DOWN);
      }

      if (xdiff > 0.5) {
         dirs.add(Direction.EAST);
      } else if (xdiff < -0.5) {
         dirs.add(Direction.WEST);
      } else {
         dirs.add(Direction.EAST);
         dirs.add(Direction.WEST);
      }

      if (zdiff > 0.5) {
         dirs.add(Direction.SOUTH);
      } else if (zdiff < -0.5) {
         dirs.add(Direction.NORTH);
      } else {
         dirs.add(Direction.SOUTH);
         dirs.add(Direction.NORTH);
      }

      return dirs;
   }

   public boolean isInEyeRange(BlockPos pos) {
      return (double)pos.getY() > mc.player.getY() + (double)mc.player.getStandingEyeHeight();
   }

   private boolean isEntityBlockingPlacement(BlockPos pos) {
      BlockState airState = Blocks.AIR.getDefaultState();
      VoxelShape shape = airState.getOutlineShape(mc.world, pos, ShapeContext.absent()).offset((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
      if (shape.isEmpty()) {
         return false;
      } else {
         Iterator var4 = mc.world.getOtherEntities(null, shape.getBoundingBox()).iterator();

         while (true) {
            if (!var4.hasNext()) {
               return false;
            }

            Entity entity = (Entity)var4.next();
            if (!entity.isSpectator()
               && entity.isAlive()
               && VoxelShapes.matchesAnywhere(shape, VoxelShapes.cuboid(entity.getBoundingBox()), BooleanBiFunction.AND)) {
               if (!(entity instanceof ItemFrameEntity)) {
                  break;
               }

               this.placedOnEntities.compute(entity.getId(), (k, attempts) -> attempts != null ? attempts + 1 : 1);
               if (this.placedOnEntities.getOrDefault(entity.getId(), 0) > 0) {
                  break;
               }
            }
         }

         return true;
      }
   }

   public interface RotationCallback {
      void handleRotation(boolean var1, float[] var2);
   }
}
