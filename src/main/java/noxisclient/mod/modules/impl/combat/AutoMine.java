package noxisclient.mod.modules.impl.combat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import noxisclient.api.events.impl.UpdateEvent;
import noxisclient.api.utils.combat.CombatUtil;
import noxisclient.api.utils.world.BlockUtil;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.exploit.Blink;
import noxisclient.mod.modules.impl.player.PacketMine;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;
import noxisclient.mod.modules.settings.impl.StringSetting;

public class AutoMine extends Module {
   public static AutoMine INSTANCE;
   public static final List<Block> hard = Arrays.asList(
      Blocks.OBSIDIAN, Blocks.ENDER_CHEST, Blocks.NETHERITE_BLOCK, Blocks.CRYING_OBSIDIAN, Blocks.RESPAWN_ANCHOR, Blocks.ANCIENT_DEBRIS, Blocks.ANVIL
   );
   private final EnumSetting<AutoMine.Selection> selection = this.add(new EnumSetting<>("Selection", AutoMine.Selection.ALL));
   private final StringSetting whitelist = this.add(
      new StringSetting("Whitelist", "minecraft:obsidian,minecraft:ender_chest", () -> this.selection.is(AutoMine.Selection.WHITELIST))
         .injectTask(this::refreshBlockFilters)
   );
   private final StringSetting blacklist = this.add(
      new StringSetting("Blacklist", "minecraft:shulker_box", () -> this.selection.is(AutoMine.Selection.BLACKLIST)).injectTask(this::refreshBlockFilters)
   );
   private final BooleanSetting avoidSelf = this.add(new BooleanSetting("AvoidSelf", false));
   private final BooleanSetting strictDirection = this.add(new BooleanSetting("StrictDirection", false));
   public final SliderSetting targetRange = this.add(new SliderSetting("TargetRange", 5.0, 1.0, 10.0, 0.1).setSuffix("m"));
   public final SliderSetting range = this.add(new SliderSetting("Range", 4.0, 0.1, 6.0, 0.1).setSuffix("m"));
   private final BooleanSetting antiCrawl = this.add(new BooleanSetting("AntiCrawl", false));
   private final BooleanSetting targetBody = this.add(new BooleanSetting("TargetBody", false));
   private final BooleanSetting targetHead = this.add(new BooleanSetting("TargetHead", false));
   private final BooleanSetting cevPause = this.add(new BooleanSetting("CevPause", true));
   private final Set<Block> whitelistBlocks = new HashSet<>();
   private final Set<Block> blacklistBlocks = new HashSet<>();
   private UUID trackedTargetId;
   private BlockPos trackedTargetPos;
   private PlayerEntity playerTarget;
   private BlockPos pendingSecondPos;
   private UUID pendingSecondTargetId;
   private long pendingSecondSinceMs;

   public AutoMine() {
      super("AutoMine", "Automatically selects enemy blocks for PacketMine", Module.Category.Combat);
      this.setChinese("自动挖掘");
      INSTANCE = this;
      this.refreshBlockFilters();
   }

   public void onUpdate(UpdateEvent event) {
      if (!Module.nullCheck()) {
         PacketMine packetMine = PacketMine.INSTANCE;
         if (packetMine == null || packetMine.isOff() || packetMine.isPausedForAutoMine()) {
            this.releaseAllMines();
         } else if (CevBreaker.INSTANCE != null && CevBreaker.INSTANCE.isOn() && this.cevPause.getValue()) {
            this.releaseAllMines();
         } else if (Blink.INSTANCE == null || !Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
            PlayerEntity currentTarget = CombatUtil.getClosestEnemy(this.targetRange.getValue());
            this.playerTarget = currentTarget;
            if (AntiCrawl.INSTANCE != null && AntiCrawl.INSTANCE.work) {
               this.releaseEnemyMine();
            } else if (this.handleAntiCrawl(packetMine, currentTarget)) {
               this.clearPendingSecond();
            } else if (packetMine.isManualOverride()) {
               packetMine.abortGoal(PacketMine.MiningGoal.AUTO_MINE);
               this.clearPendingSecond();
            } else if (packetMine.hasGoalMine(PacketMine.MiningGoal.PREVENT_CRAWL)) {
               this.clearPendingSecond();
            } else {
               PacketMine.MineData rebreakData = packetMine.getRebreakMineData();
               if (rebreakData == null || !packetMine.rebreak.getValue() || !packetMine.canMine(rebreakData.getState())) {
                  if (currentTarget == null) {
                     this.releaseEnemyMine();
                  } else {
                     BlockPos targetPos = getRoundedBlockPos(currentTarget);
                     boolean targetChanged = this.trackedTargetId != null
                        && (!this.trackedTargetId.equals(currentTarget.getUuid()) || !targetPos.equals(this.trackedTargetPos));
                     if (targetChanged) {
                        packetMine.abortGoal(PacketMine.MiningGoal.AUTO_MINE);
                        this.clearPendingSecond();
                     }

                     this.trackedTargetId = currentTarget.getUuid();
                     this.trackedTargetPos = targetPos;
                     if (!this.handlePendingSecond(packetMine, currentTarget)) {
                        boolean bedrockPhased = this.isBedrockPhased(currentTarget, targetPos) && !currentTarget.isCrawling();
                        List<BlockPos> phaseBlocks = this.getPhaseBlocks(currentTarget, targetPos, bedrockPhased);
                        if (!phaseBlocks.isEmpty()) {
                           this.startCandidates(packetMine, phaseBlocks);
                        } else {
                           List<BlockPos> miningBlocks = this.getMiningBlocks(currentTarget, targetPos, bedrockPhased);
                           List<BlockPos> candidates = this.getValidMiningBlocks(miningBlocks, bedrockPhased);
                           this.startCandidates(packetMine, candidates);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public void onDisable() {
      this.releaseAllMines();
      super.onDisable();
   }

   @Override
   public String getInfo() {
      return this.playerTarget == null ? null : this.playerTarget.getName().getString();
   }

   private boolean handleAntiCrawl(PacketMine packetMine, PlayerEntity target) {
      boolean shouldPreventCrawl = this.antiCrawl.getValue() && mc.player.isCrawling();
      if (!shouldPreventCrawl) {
         if (packetMine.hasGoalMine(PacketMine.MiningGoal.PREVENT_CRAWL)) {
            packetMine.abortGoal(PacketMine.MiningGoal.PREVENT_CRAWL);
         }

         return false;
      } else {
         BlockPos antiCrawlPos = this.getAntiCrawlPos(target);
         if (antiCrawlPos == null) {
            return false;
         } else if (packetMine.isMining(antiCrawlPos)) {
            return true;
         } else {
            Direction direction = this.getMiningDirection(antiCrawlPos);
            if (direction == null) {
               return false;
            } else {
               packetMine.attackAutoMineBlock(new PacketMine.MineData(antiCrawlPos, direction, PacketMine.MiningGoal.PREVENT_CRAWL));
               return packetMine.hasGoalMine(PacketMine.MiningGoal.PREVENT_CRAWL);
            }
         }
      }
   }

   private void startCandidates(PacketMine packetMine, List<BlockPos> candidates) {
      if (!candidates.isEmpty() && this.pendingSecondPos == null) {
         int maxMines = packetMine.doubleBreak.getValue() ? 2 : 1;
         int activeMines = packetMine.getGoalMineCount(PacketMine.MiningGoal.AUTO_MINE);
         if (activeMines < maxMines) {
            if (activeMines == 1 && packetMine.doubleBreak.getValue()) {
               this.scheduleSecondCandidate(packetMine, candidates, null);
            } else {
               for (BlockPos pos : candidates) {
                  if (!packetMine.isMining(pos)) {
                     Direction direction = this.getMiningDirection(pos);
                     if (direction != null) {
                        boolean started = packetMine.attackAutoMineBlock(new PacketMine.MineData(pos, direction, PacketMine.MiningGoal.AUTO_MINE));
                        if (started || packetMine.isMining(pos)) {
                           if (packetMine.doubleBreak.getValue()) {
                              this.scheduleSecondCandidate(packetMine, candidates, pos);
                           }

                           return;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private boolean handlePendingSecond(PacketMine packetMine, PlayerEntity target) {
      if (this.pendingSecondPos == null) {
         return false;
      } else if (!packetMine.doubleBreak.getValue() || this.pendingSecondTargetId == null || !this.pendingSecondTargetId.equals(target.getUuid())) {
         this.clearPendingSecond();
         return false;
      } else if (packetMine.getGoalMineCount(PacketMine.MiningGoal.AUTO_MINE) >= 2) {
         this.clearPendingSecond();
         return false;
      } else {
         long elapsedMs = System.currentTimeMillis() - this.pendingSecondSinceMs;
         if (elapsedMs < (long)packetMine.mineInterval.getValueInt()) {
            return true;
         } else {
            BlockPos pos = this.pendingSecondPos;
            this.clearPendingSecond();
            if (!this.isValidAutoMineBlock(pos)) {
               return false;
            } else {
               Direction direction = this.getMiningDirection(pos);
               if (direction == null) {
                  return false;
               } else {
                  boolean started = packetMine.attackAutoMineBlock(new PacketMine.MineData(pos, direction, PacketMine.MiningGoal.AUTO_MINE));
                  return started || packetMine.isMining(pos);
               }
            }
         }
      }
   }

   private void scheduleSecondCandidate(PacketMine packetMine, List<BlockPos> candidates, BlockPos firstPos) {
      if (packetMine.doubleBreak.getValue() && this.playerTarget != null) {
         for (BlockPos candidate : candidates) {
            if (!candidate.equals(firstPos) && !packetMine.isMining(candidate)) {
               this.pendingSecondPos = candidate;
               this.pendingSecondTargetId = this.playerTarget.getUuid();
               this.pendingSecondSinceMs = System.currentTimeMillis();
               return;
            }
         }
      }
   }

   private void clearPendingSecond() {
      this.pendingSecondPos = null;
      this.pendingSecondTargetId = null;
      this.pendingSecondSinceMs = 0L;
   }

   private List<BlockPos> getPhaseBlocks(PlayerEntity player, BlockPos playerPos, boolean bedrockPhased) {
      BlockPos phaseLevel = bedrockPhased && this.targetBody.getValue() ? playerPos.up() : playerPos;
      LinkedHashSet<BlockPos> phaseBlocks = new LinkedHashSet<>(getAllInBox(player.getBoundingBox(), phaseLevel));
      if (bedrockPhased && this.targetHead.getValue()) {
         phaseBlocks.add(playerPos.up(2));
      }

      phaseBlocks.removeIf(pos -> !this.isValidAutoMineBlock(pos));
      return new ArrayList<>(phaseBlocks);
   }

   private List<BlockPos> getMiningBlocks(PlayerEntity player, BlockPos playerPos, boolean bedrockPhased) {
      List<BlockPos> surroundingBlocks = this.getSurroundNoDown(player);
      List<BlockPos> miningBlocks = new ArrayList<>();
      if (bedrockPhased) {
         if (this.targetBody.getValue()) {
            for (BlockPos pos : surroundingBlocks) {
               miningBlocks.add(pos.up());
            }
         }

         if (this.canMine(playerPos.down())) {
            miningBlocks.add(playerPos.down());
         }
      } else {
         miningBlocks.addAll(surroundingBlocks);
      }

      if (this.avoidSelf.getValue()) {
         miningBlocks.removeIf(this::intersectsPlayer);
      }

      return miningBlocks;
   }

   private List<BlockPos> getValidMiningBlocks(List<BlockPos> miningBlocks, boolean bedrockPhased) {
      List<BlockPos> candidates = new ArrayList<>();

      for (BlockPos pos : miningBlocks) {
         if (this.isValidAutoMineBlock(pos)) {
            Block blockBelow = BlockUtil.getBlock(pos.down());
            if (bedrockPhased || blockBelow == Blocks.OBSIDIAN || blockBelow == Blocks.BEDROCK) {
               candidates.add(pos);
            }
         }
      }

      candidates.sort(Comparator.comparingDouble(this::getCrystalPriority).thenComparingDouble(this::getDistanceSq));
      return candidates;
   }

   private double getCrystalPriority(BlockPos pos) {
      if (AutoCrystal.INSTANCE == null) {
         return 0.0;
      } else {
         double placeRange = AutoCrystal.INSTANCE.placeRange.getValue();
         return this.getDistanceSq(pos.down()) <= placeRange * placeRange ? 10.0 : 0.0;
      }
   }

   private boolean isValidAutoMineBlock(BlockPos pos) {
      if (!this.canMine(pos) || PacketMine.INSTANCE.isMining(pos) || !this.isInRange(pos)) {
         return false;
      } else if (!this.isAutoMineBlock(pos)) {
         return false;
      } else {
         return this.avoidSelf.getValue() && this.intersectsPlayer(pos) ? false : !this.strictDirection.getValue() || BlockUtil.getClickSideStrict(pos) != null;
      }
   }

   private boolean isAutoMineBlock(BlockPos pos) {
      if (PacketMine.unbreakable(pos)) {
         return false;
      } else {
         Block block = BlockUtil.getBlock(pos);

         return switch ((AutoMine.Selection)this.selection.getValue()) {
            case WHITELIST -> this.whitelistBlocks.contains(block);
            case BLACKLIST -> !this.blacklistBlocks.contains(block);
            case ALL -> true;
         };
      }
   }

   private boolean canMine(BlockPos pos) {
      BlockState state = mc.world.getBlockState(pos);
      return !state.isAir() && state.getFluidState().isEmpty();
   }

   private boolean isInRange(BlockPos pos) {
      return this.getDistanceSq(pos) <= this.range.getValue() * this.range.getValue();
   }

   private double getDistanceSq(BlockPos pos) {
      return mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos());
   }

   private Direction getMiningDirection(BlockPos pos) {
      Direction direction = BlockUtil.getClickSideStrict(pos);
      if (direction != null) {
         return direction;
      } else {
         return this.strictDirection.getValue() ? null : Direction.UP;
      }
   }

   private BlockPos getAntiCrawlPos(PlayerEntity target) {
      if (!mc.player.isOnGround()) {
         return null;
      } else {
         BlockPos playerPos = getRoundedBlockPos(mc.player);
         boolean targetBelow = target != null && getRoundedBlockPos(target).getY() < playerPos.getY();
         BlockPos candidate = targetBelow ? playerPos.down() : playerPos.up();
         return this.isValidAutoMineBlock(candidate) ? candidate : null;
      }
   }

   private boolean isBedrockPhased(PlayerEntity player, BlockPos playerPos) {
      for (BlockPos pos : getAllInBox(player.getBoundingBox(), playerPos)) {
         if (PacketMine.unbreakable(pos)) {
            return true;
         }
      }

      return false;
   }

   private boolean intersectsPlayer(BlockPos pos) {
      List<BlockPos> playerBlocks = getAllInBox(mc.player.getBoundingBox(), getRoundedBlockPos(mc.player));
      if (playerBlocks.contains(pos)) {
         return true;
      } else {
         for (BlockPos playerBlock : playerBlocks) {
            for (Direction direction : Direction.values()) {
               if (direction.getAxis().isHorizontal() && playerBlock.offset(direction).equals(pos)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   private List<BlockPos> getSurroundNoDown(PlayerEntity player) {
      List<BlockPos> playerBlocks = getAllInBox(player.getBoundingBox(), getRoundedBlockPos(player));
      LinkedHashSet<BlockPos> surroundingBlocks = new LinkedHashSet<>();

      for (BlockPos playerBlock : playerBlocks) {
         for (Direction direction : Direction.values()) {
            if (direction.getAxis().isHorizontal()) {
               BlockPos surroundPos = playerBlock.offset(direction);
               if (!playerBlocks.contains(surroundPos) && this.isInRange(surroundPos)) {
                  surroundingBlocks.add(surroundPos);
               }
            }
         }
      }

      return new ArrayList<>(surroundingBlocks);
   }

   private static BlockPos getRoundedBlockPos(PlayerEntity player) {
      return new BlockPos(MathHelper.floor(player.getX()), (int)Math.round(player.getY()), MathHelper.floor(player.getZ()));
   }

   private static List<BlockPos> getAllInBox(Box box, BlockPos level) {
      List<BlockPos> intersections = new ArrayList<>();

      for (int x = (int)Math.floor(box.minX); (double)x < Math.ceil(box.maxX); x++) {
         for (int z = (int)Math.floor(box.minZ); (double)z < Math.ceil(box.maxZ); z++) {
            intersections.add(new BlockPos(x, level.getY(), z));
         }
      }

      return intersections;
   }

   private void refreshBlockFilters() {
      this.whitelistBlocks.clear();
      this.blacklistBlocks.clear();
      this.parseBlockList(this.whitelist.getValue(), this.whitelistBlocks);
      this.parseBlockList(this.blacklist.getValue(), this.blacklistBlocks);
   }

   private void parseBlockList(String value, Set<Block> destination) {
      if (value != null && !value.isBlank()) {
         for (String token : value.split("[,;\\s]+")) {
            if (!token.isBlank()) {
               try {
                  Identifier id = Identifier.of(token.contains(":") ? token : "minecraft:" + token);
                  Block block = Registries.BLOCK.get(id);
                  if (id.equals(Registries.BLOCK.getId(block))) {
                     destination.add(block);
                  }
               } catch (RuntimeException var9) {
               }
            }
         }
      }
   }

   private void releaseEnemyMine() {
      if (PacketMine.INSTANCE != null) {
         PacketMine.INSTANCE.abortGoal(PacketMine.MiningGoal.AUTO_MINE);
      }

      this.clearPendingSecond();
      this.trackedTargetId = null;
      this.trackedTargetPos = null;
      this.playerTarget = null;
   }

   private void releaseAllMines() {
      if (PacketMine.INSTANCE != null) {
         PacketMine.INSTANCE.abortGoal(PacketMine.MiningGoal.AUTO_MINE);
         PacketMine.INSTANCE.abortGoal(PacketMine.MiningGoal.PREVENT_CRAWL);
      }

      this.clearPendingSecond();
      this.trackedTargetId = null;
      this.trackedTargetPos = null;
      this.playerTarget = null;
   }

   public static enum Selection {
      WHITELIST,
      BLACKLIST,
      ALL;
   }
}
