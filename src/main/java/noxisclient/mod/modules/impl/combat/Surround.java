package noxisclient.mod.modules.impl.combat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import noxisclient.Alien;
import noxisclient.api.events.impl.ClientTickEvent;
import noxisclient.api.events.impl.OpenScreenEvent;
import noxisclient.api.events.impl.PacketEvent;
import noxisclient.api.utils.world.BlockUtil;
import noxisclient.core.impl.InventoryManager;
import noxisclient.core.impl.RotationManager;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.player.PacketMine;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class Surround extends Module {
   public static Surround INSTANCE;
   private static final BlockState DEFAULT_OBSIDIAN_STATE = Blocks.OBSIDIAN.getDefaultState();
   private static final String OWNER = "Surround";
   private final EnumSetting<Surround.Timing> timing = this.add(new EnumSetting<>("Timing", Surround.Timing.VANILLA));
   private final BooleanSetting prePlaceExplosion = this.add(new BooleanSetting("PrePlace-Explosions", false, () -> this.timing.is(Surround.Timing.SEQUENTIAL)));
   private final BooleanSetting prePlaceTick = this.add(new BooleanSetting("PrePlace-Tick", false, () -> this.timing.is(Surround.Timing.SEQUENTIAL)));
   private final SliderSetting placeRange = this.add(new SliderSetting("PlaceRange", 4.0, 0.0, 6.0));
   private final BooleanSetting multitask = this.add(new BooleanSetting("Multitask", false));
   private final BooleanSetting strictDirection = this.add(new BooleanSetting("StrictDirection", false));
   private final BooleanSetting grimV3 = this.add(new BooleanSetting("GrimV3", false));
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", false));
   private final BooleanSetting attack = this.add(new BooleanSetting("Attack", true));
   private final BooleanSetting extend = this.add(new BooleanSetting("Extend", true));
   private final BooleanSetting head = this.add(new BooleanSetting("CoverHead", false));
   private final BooleanSetting mineExtend = this.add(new BooleanSetting("MineExtend", false));
   private final BooleanSetting support = this.add(new BooleanSetting("Support", false));
   private final SliderSetting shiftTicks = this.add(new SliderSetting("ShiftTicks", 2, 1, 10));
   private final SliderSetting shiftDelay = this.add(new SliderSetting("ShiftDelay", 1.0, 0.0, 5.0));
   private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true));
   private int blocksPlaced;
   private List<BlockPos> surround = new ArrayList<>();
   private List<BlockPos> placements = new ArrayList<>();
   private final Map<BlockPos, Long> packets = new HashMap<>();
   private final Map<BlockPos, List<BlockPos>> helperPaths = new HashMap<>();
   private int suppressContainerOpenTicks;
   private boolean bypassNoInteract;
   private double prevY;

   public Surround() {
      super("Surround", "Surrounds feet with obsidian", Module.Category.Combat);
      this.setChinese("围脚");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      if (mc.player != null) {
         this.prevY = mc.player.getY();
      }

      this.suppressContainerOpenTicks = 0;
      this.bypassNoInteract = false;
   }

   @Override
   public void onDisable() {
      this.surround.clear();
      this.placements.clear();
      this.packets.clear();
      this.helperPaths.clear();
      this.suppressContainerOpenTicks = 0;
      this.bypassNoInteract = false;
      Alien.INVENTORY.syncToClient("Surround");
   }

   public void onPlayerTick(ClientTickEvent event) {
      if (!nullCheck() && !event.isPost()) {
         if (this.suppressContainerOpenTicks > 0) {
            this.suppressContainerOpenTicks--;
         }

         this.blocksPlaced = 0;
         if (SelfTrap.INSTANCE == null || !SelfTrap.INSTANCE.isOn()) {
            if (!this.autoDisable.getValue() || !(mc.player.getY() - this.prevY > 0.5) && !(mc.player.fallDistance > 1.5F)) {
               if (!this.multitask.getValue() && mc.player.isUsingItem()) {
                  this.surround.clear();
                  this.placements.clear();
               } else {
                  Surround.BlockSlot blockSlot = this.getResistantBlockItem();
                  if (blockSlot != null && blockSlot.slot() != -1) {
                     this.surround = this.getSurround(mc.player);
                     if (!this.surround.isEmpty()) {
                        if (this.attack.getValue()) {
                           this.attackBlockingCrystals(this.surround);
                        }

                        this.placements = this.getPlacementsFromSurround(this.surround);
                        if (!this.placements.isEmpty()) {
                           this.placements.sort(Comparator.comparingInt(Vec3i::getY));

                           while (this.blocksPlaced < this.shiftTicks.getValueInt() && this.blocksPlaced < this.placements.size()) {
                              this.placeBlock(this.placements.get(this.blocksPlaced), blockSlot.slot());
                           }

                           if (this.rotate.getValue()) {
                              Alien.ROTATION.snapBack();
                           }
                        }
                     }
                  } else {
                     this.surround.clear();
                     this.placements.clear();
                  }
               }
            } else {
               this.disable();
            }
         }
      }
   }

   public void onPacketInbound(PacketEvent.Receive event) {
      if (!nullCheck()) {
         if (event.getPacket() instanceof BundleS2CPacket packet) {
            for (Packet<?> packet1 : packet.getPackets()) {
               this.handlePackets(packet1);
            }
         } else {
            this.handlePackets(event.getPacket());
         }
      }
   }

   private void handlePackets(Packet<?> serverPacket) {
      if (this.timing.getValue() == Surround.Timing.SEQUENTIAL) {
         if (serverPacket instanceof BlockUpdateS2CPacket packet) {
            BlockState blockState = packet.getState();
            BlockPos targetPos = packet.getPos();
            if (this.surround.contains(targetPos)) {
               if (blockState.isReplaceable() && mc.world.canPlace(DEFAULT_OBSIDIAN_STATE, targetPos, ShapeContext.absent())) {
                  Surround.BlockSlot blockSlot = this.getResistantBlockItem();
                  if (blockSlot != null && blockSlot.slot() != -1) {
                     this.placeBlock(targetPos, blockSlot.slot());
                  }
               } else if (this.isBlastResistant(blockState)) {
                  this.packets.remove(targetPos);
               }
            }
         }

         if (this.blocksPlaced <= this.shiftTicks.getValueInt() * 2) {
            if (serverPacket instanceof ExplosionS2CPacket packetx && this.prePlaceExplosion.getValue()) {
               BlockPos pos = BlockPos.ofFloored(packetx.getX(), packetx.getY(), packetx.getZ());
               if (this.surround.contains(pos)) {
                  Surround.BlockSlot blockSlot = this.getResistantBlockItem();
                  if (blockSlot != null && blockSlot.slot() != -1) {
                     this.placeBlock(pos, blockSlot.slot());
                  }
               }
            }

            if (serverPacket instanceof EntitySpawnS2CPacket packetxx
               && packetxx.getEntityType().equals(EntityType.END_CRYSTAL)
               && this.prePlaceTick.getValue()) {
               for (BlockPos pos : this.surround) {
                  if (pos.equals(BlockPos.ofFloored(packetxx.getX(), packetxx.getY(), packetxx.getZ()))) {
                     Surround.BlockSlot blockSlot = this.getResistantBlockItem();
                     if (blockSlot != null && blockSlot.slot() != -1) {
                        this.placeBlock(pos, blockSlot.slot());
                     }
                     break;
                  }
               }
            }
         }
      }
   }

   private void placeBlock(BlockPos pos, int slot) {
      Direction direction = this.getInteractDirection(pos);
      if (direction != null) {
         BlockPos neighbor = pos.offset(direction.getOpposite());
         BlockUtil.placedPos.add(pos);
         boolean placed = this.grimV3.getValue() && mc.world.getBlockState(neighbor).getBlock() == Blocks.ENDER_CHEST
            ? this.placeNoInteract(pos, neighbor, direction, slot)
            : Alien.INTERACT.placeBlock(neighbor, direction, slot, false, false, true, (state, angles) -> {
               if (this.rotate.getValue() && state) {
                  Alien.ROTATION.snapAt(angles[0], angles[1]);
               }
            });
         if (!this.grimV3.getValue() || placed) {
            this.packets.put(pos, System.currentTimeMillis());
            this.blocksPlaced++;
         }
      }
   }

   public void attackBlockingCrystals(List<BlockPos> posList) {
      for (BlockPos pos : posList) {
         Entity crystalEntity = mc.world.getOtherEntities(null, new Box(pos)).stream().filter(e -> e instanceof EndCrystalEntity).findFirst().orElse(null);
         if (crystalEntity != null) {
            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(crystalEntity, mc.player.isSneaking()));
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            return;
         }
      }
   }

   public List<BlockPos> getPlacementsFromSurround(List<BlockPos> surround) {
      List<BlockPos> placements = new ArrayList<>();
      boolean helperQueued = false;

      for (BlockPos surroundPos : surround) {
         if (!this.canAttemptPlace(surroundPos)) {
            this.helperPaths.remove(surroundPos);
         } else if (this.hasPlaceFace(surroundPos)) {
            this.helperPaths.remove(surroundPos);
            this.addPlacement(placements, surroundPos);
         } else if (this.support.getValue() && !helperQueued) {
            BlockPos helper = this.getHelperPlacement(surroundPos);
            if (helper != null) {
               this.addPlacement(placements, helper);
               helperQueued = true;
            } else if (this.helperPaths.containsKey(surroundPos)) {
               helperQueued = true;
            }
         }
      }

      return placements;
   }

   private BlockPos getHelperPlacement(BlockPos targetPos) {
      List<BlockPos> path = this.helperPaths.get(targetPos);
      if (path != null) {
         return this.getNextHelperFromPath(path);
      } else {
         path = this.findHelperPath(targetPos);
         if (path == null) {
            return null;
         } else {
            this.helperPaths.put(targetPos, path);
            return this.getNextHelperFromPath(path);
         }
      }
   }

   private List<BlockPos> findHelperPath(BlockPos targetPos) {
      ArrayDeque<BlockPos> queue = new ArrayDeque<>();
      Set<BlockPos> visited = new HashSet<>();
      Map<BlockPos, BlockPos> parent = new HashMap<>();
      queue.add(targetPos);
      visited.add(targetPos);

      while (!queue.isEmpty()) {
         BlockPos current = queue.poll();

         for (Direction direction : Direction.values()) {
            BlockPos next = current.offset(direction);
            if (visited.add(next) && this.canAttemptPlace(next)) {
               parent.put(next, current);
               if (this.hasPlaceFace(next)) {
                  List<BlockPos> path = new ArrayList<>();

                  for (BlockPos pathPos = next; pathPos != null; pathPos = parent.get(pathPos)) {
                     path.add(0, pathPos);
                     if (pathPos.equals(targetPos)) {
                        break;
                     }
                  }

                  return path;
               }

               queue.add(next);
            }
         }
      }

      return null;
   }

   private BlockPos getNextHelperFromPath(List<BlockPos> path) {
      for (int i = path.size() - 1; i >= 0; i--) {
         BlockPos pos = path.get(i);
         if (this.canAttemptPlace(pos) && this.hasPlaceFace(pos)) {
            return pos;
         }
      }

      return null;
   }

   private boolean canAttemptPlace(BlockPos pos) {
      if (this.isPlacementDelayed(pos)) {
         return false;
      } else if (!mc.world.getBlockState(pos).isReplaceable()) {
         return false;
      } else {
         return mc.player.squaredDistanceTo(pos.toCenterPos()) > this.placeRange.getValue() * this.placeRange.getValue()
            ? false
            : mc.world.canPlace(DEFAULT_OBSIDIAN_STATE, pos, ShapeContext.absent());
      }
   }

   private boolean isPlacementDelayed(BlockPos pos) {
      Long placed = this.packets.get(pos);
      return this.shiftDelay.getValue() > 0.0 && placed != null && (double)(System.currentTimeMillis() - placed) < this.shiftDelay.getValue() * 50.0;
   }

   private boolean hasPlaceFace(BlockPos pos) {
      return this.getInteractDirection(pos) != null;
   }

   private void addPlacement(List<BlockPos> placements, BlockPos pos) {
      if (!placements.contains(pos)) {
         placements.add(pos);
      }
   }

   public List<BlockPos> getSurround(PlayerEntity player) {
      List<BlockPos> surroundBlocks = this.getSurroundNoDown(player);
      List<BlockPos> playerBlocks = this.getPlayerBlocks(player);

      for (BlockPos playerPos : playerBlocks) {
         if (!playerPos.equals(player.getBlockPos())) {
            surroundBlocks.add(playerPos.down());
         }
      }

      if (this.mineExtend.getValue()) {
         for (BlockPos surroundPos : new ArrayList<>(surroundBlocks)) {
            if (Alien.BREAK.isMining(surroundPos)) {
               for (Direction direction : Direction.values()) {
                  if (direction != Direction.DOWN) {
                     BlockPos blockerPos = surroundPos.offset(direction);
                     if (!playerBlocks.contains(blockerPos) && !blockerPos.equals(PacketMine.getBreakPos())) {
                        surroundBlocks.add(blockerPos);
                     }
                  }
               }
            }
         }
      }

      if (this.head.getValue()) {
         surroundBlocks.add(mc.player.getBlockPos().up(2));
      }

      return surroundBlocks;
   }

   public List<BlockPos> getSurroundNoDown(PlayerEntity player) {
      return this.getSurroundNoDown(player, 0.0F);
   }

   public List<BlockPos> getSurroundNoDown(PlayerEntity player, float range) {
      List<BlockPos> surroundBlocks = new ArrayList<>();
      List<BlockPos> playerBlocks = this.getPlayerBlocks(player);

      for (BlockPos pos : playerBlocks) {
         if (!(range > 0.0F) || !(mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos()) > (double)(range * range))) {
            for (Direction direction : Direction.values()) {
               if (direction.getAxis().isHorizontal()) {
                  BlockPos pos1 = pos.offset(direction);
                  if (!surroundBlocks.contains(pos1) && !playerBlocks.contains(pos1)) {
                     surroundBlocks.add(pos1);
                  }
               }
            }
         }
      }

      return surroundBlocks;
   }

   public List<BlockPos> getPlayerBlocks(PlayerEntity entity) {
      BlockPos playerPos = this.getRoundedBlockPos(entity.getX(), entity.getY(), entity.getZ());
      List<BlockPos> playerBlocks = new ArrayList<>();
      if (this.extend.getValue()) {
         playerBlocks.addAll(this.getAllInBox(entity.getBoundingBox(), playerPos));
      } else {
         playerBlocks.add(playerPos);
      }

      return playerBlocks;
   }

   public boolean isPlacing() {
      return !this.placements.isEmpty();
   }

   private Surround.BlockSlot getResistantBlockItem() {
      int obsidian = this.getBlockItemSlot(Blocks.OBSIDIAN);
      if (obsidian != -1) {
         return new Surround.BlockSlot(Blocks.OBSIDIAN, obsidian);
      } else {
         int crying = this.getBlockItemSlot(Blocks.CRYING_OBSIDIAN);
         if (crying != -1) {
            return new Surround.BlockSlot(Blocks.CRYING_OBSIDIAN, crying);
         } else {
            int enderChest = this.getBlockItemSlot(Blocks.ENDER_CHEST);
            return enderChest == -1 ? null : new Surround.BlockSlot(Blocks.ENDER_CHEST, enderChest);
         }
      }
   }

   private int getBlockItemSlot(Block block) {
      if (!this.grimV3.getValue() || !BlockUtil.isClickable(block) || this.grimV3.getValue() && block == Blocks.ENDER_CHEST) {
         for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() == block) {
               return i;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   private boolean isBlastResistant(BlockState state) {
      Block block = state.getBlock();
      return block == Blocks.BEDROCK || block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN || block == Blocks.ENDER_CHEST;
   }

   private Direction getInteractDirection(BlockPos pos) {
      Direction direction = Alien.INTERACT.getInteractDirectionInternal(pos, this.strictDirection.getValue(), this.grimV3.getValue());
      return direction == null && this.grimV3.getValue() ? this.getEnderChestInteractDirection(pos) : direction;
   }

   private Direction getEnderChestInteractDirection(BlockPos pos) {
      Set<Direction> validDirections = Alien.INTERACT.getPlaceDirectionsNCP(mc.player.getEyePos(), pos.toCenterPos());

      for (Direction direction : Direction.values()) {
         BlockPos neighbor = pos.offset(direction);
         Direction side = direction.getOpposite();
         BlockState state = mc.world.getBlockState(neighbor);
         if (state.getBlock() == Blocks.ENDER_CHEST
            && (!this.strictDirection.getValue() || validDirections.contains(side))
            && (!this.grimV3.getValue() || BlockUtil.grimStrictDirectionCheck(neighbor, side, mc.world, mc.player))) {
            return side;
         }
      }

      return null;
   }

   private boolean placeNoInteract(BlockPos targetPos, BlockPos neighbor, Direction side, int slot) {
      if (!Alien.INVENTORY.trySetSlot(slot, InventoryManager.SwapPriority.HIGH, "Surround")) {
         return false;
      } else {
         Vec3d hitVec = neighbor.toCenterPos()
            .add(new Vec3d((double)side.getVector().getX(), (double)side.getVector().getY(), (double)side.getVector().getZ()).multiply(0.5));
         BlockHitResult hitResult = new BlockHitResult(hitVec, side, neighbor, false);
         if (this.rotate.getValue()) {
            float[] angles = RotationManager.getRotation(mc.player.getEyePos(), hitResult.getPos());
            Alien.ROTATION.snapAt(angles[0], angles[1]);
         }

         this.suppressContainerOpenTicks = 6;
         mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));

         try {
            this.bypassNoInteract = true;
            Module.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(Hand.OFF_HAND, hitResult, id));
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.OFF_HAND));
         } finally {
            this.bypassNoInteract = false;
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
            Alien.INVENTORY.syncToClient("Surround");
         }

         return true;
      }
   }

   public boolean shouldBypassNoInteract() {
      return this.isOn() && this.bypassNoInteract;
   }

   public void onOpenScreen(OpenScreenEvent event) {
      if (!this.isOff() && this.suppressContainerOpenTicks > 0 && event.screen instanceof GenericContainerScreen) {
         event.cancel();
         this.suppressContainerOpenTicks = 0;
         if (mc.player != null && mc.getNetworkHandler() != null && mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
            mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
         }
      }
   }

   private BlockPos getRoundedBlockPos(double x, double y, double z) {
      return new BlockPos(MathHelper.floor(x), (int)Math.round(y), MathHelper.floor(z));
   }

   private List<BlockPos> getAllInBox(Box box, BlockPos pos) {
      List<BlockPos> intersections = new ArrayList<>();

      for (int x = (int)Math.floor(box.minX); (double)x < Math.ceil(box.maxX); x++) {
         for (int z = (int)Math.floor(box.minZ); (double)z < Math.ceil(box.maxZ); z++) {
            intersections.add(new BlockPos(x, pos.getY(), z));
         }
      }

      return intersections;
   }

   private static record BlockSlot(Block block, int slot) {
   }

   public static enum Timing {
      VANILLA,
      SEQUENTIAL;
   }
}
