package noxisclient.mod.modules.impl.player;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import noxisclient.Alien;
import noxisclient.api.events.impl.ClickBlockEvent;
import noxisclient.api.events.impl.ClientTickEvent;
import noxisclient.api.events.impl.EntitySpawnEvent;
import noxisclient.api.events.impl.PacketEvent;
import noxisclient.api.utils.Wrapper;
import noxisclient.api.utils.math.Timer;
import noxisclient.api.utils.player.EntityUtil;
import noxisclient.api.utils.player.InventoryUtil;
import noxisclient.api.utils.render.Render3DUtil;
import noxisclient.api.utils.world.BlockUtil;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.enums.SwingSide;
import noxisclient.mod.modules.settings.impl.BindSetting;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.ColorSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class PacketMine extends Module {
   public static PacketMine INSTANCE;
   public static double progress;
   private static final String SWAP_OWNER = "PacketMine";
   private static final BlockPos NEWDISABLER_FAKE_POS = new BlockPos(0, -999, 0);
   private final EnumSetting<PacketMine.Page> page = this.add(new EnumSetting<>("Page", PacketMine.Page.General));
   public final EnumSetting<PacketMine.PauseMode> pause = this.add(
      new EnumSetting<>("Pause", PacketMine.PauseMode.None, () -> this.page.is(PacketMine.Page.General))
   );
   public final SliderSetting range = this.add(new SliderSetting("Range", 5.5, 1.0, 8.0, 0.1, () -> this.page.is(PacketMine.Page.General)));
   public final SliderSetting speed = this.add(new SliderSetting("Speed", 0.8, 0.5, 1.0, 0.01, () -> this.page.is(PacketMine.Page.General)));
   public final BooleanSetting doubleBreak = this.add(new BooleanSetting("DoubleBreak", false, () -> this.page.is(PacketMine.Page.General))).setParent();
   public final SliderSetting firstWait = this.add(
      new SliderSetting("FirstWait", 5, 0, 20, () -> this.page.is(PacketMine.Page.General) && this.doubleBreak.isOpen())
   );
   public final SliderSetting mineInterval = this.add(
      new SliderSetting("MineInterval", 300.0, 0.0, 1000.0, 10.0, () -> this.page.is(PacketMine.Page.General)).setSuffix("ms")
   );
   public final BooleanSetting rebreak = this.add(new BooleanSetting("Rebreak", true, () -> this.page.is(PacketMine.Page.General))).setParent();
   public final SliderSetting rebreakDelay = this.add(new SliderSetting("RebreakDelay", 0, 0, 10, () -> this.page.is(PacketMine.Page.General)));
   public final BooleanSetting bypassGround = this.add(new BooleanSetting("BypassGround", true, () -> this.page.is(PacketMine.Page.Misc)));
   public final BindSetting obsidian = this.add(new BindSetting("Obsidian", -1, () -> this.page.is(PacketMine.Page.Misc)));
   public final BooleanSetting cancelClick = this.add(new BooleanSetting("CancelClick", false, () -> this.page.is(PacketMine.Page.Misc)));
   public final BooleanSetting clickReset = this.add(new BooleanSetting("ClickReset", false, () -> this.page.is(PacketMine.Page.Misc)));
   public final BooleanSetting clickSwing = this.add(new BooleanSetting("ClickSwing", false, () -> this.page.is(PacketMine.Page.Misc)));
   public final EnumSetting<PacketMine.Swap> swapMode = this.add(
      new EnumSetting<>("AutoSwap", PacketMine.Swap.SILENT, () -> this.page.is(PacketMine.Page.Misc))
   );
   public final BooleanSetting autoSwitch = this.add(new BooleanSetting("AutoSwitch", true, () -> this.page.is(PacketMine.Page.Misc)));
   public final EnumSetting<SwingSide> swingSide = this.add(new EnumSetting<>("SwingSide", SwingSide.Server, () -> this.page.is(PacketMine.Page.Misc)));
   public final SliderSetting waitTime = this.add(
      new SliderSetting("WaitTime", 0.0, 0.0, 1.0, 0.01, () -> this.page.is(PacketMine.Page.General) || this.page.is(PacketMine.Page.Misc))
   );
   public final BooleanSetting airEnd = this.add(new BooleanSetting("AirEnd", false, () -> this.page.is(PacketMine.Page.Misc)));
   public final BooleanSetting resetMine = this.add(new BooleanSetting("ResetMine", false, () -> this.page.is(PacketMine.Page.Misc))).setParent();
   public final SliderSetting rmDelay = this.add(new SliderSetting("RMDelay", 0, 0, 20, () -> this.page.is(PacketMine.Page.Misc) && this.resetMine.isOpen()));
   public final SliderSetting stopTimes = this.add(
      new SliderSetting("HStopTimes", 3, 1, 20, () -> this.page.is(PacketMine.Page.Misc) && this.resetMine.isOpen())
   );
   public final BooleanSetting retryAfterStop = this.add(new BooleanSetting("RetryAfterStop", false, () -> this.page.is(PacketMine.Page.Misc)));
   public final SliderSetting retryTicks = this.add(
      new SliderSetting("RetryTicks", 5, 0, 40, () -> this.page.is(PacketMine.Page.Misc) && this.retryAfterStop.getValue())
   );
   public final BooleanSetting noGSpeed = this.add(new BooleanSetting("NOGSpeed", false, () -> this.page.is(PacketMine.Page.Misc)));
   public final BooleanSetting grimV3 = this.add(new BooleanSetting("GrimV3", true, () -> this.page.is(PacketMine.Page.Misc)));
   public final BooleanSetting renderEnable = this.add(new BooleanSetting("RenderEnable", true, () -> this.page.is(PacketMine.Page.Render)));
   public final EnumSetting<PacketMine.MioRenderMode> renderMode = this.add(
      new EnumSetting<>("RenderMode", PacketMine.MioRenderMode.Out, () -> this.page.is(PacketMine.Page.Render))
   );
   public final EnumSetting<PacketMine.MioColorMode> colorMode = this.add(
      new EnumSetting<>("ColorMode", PacketMine.MioColorMode.Progress, () -> this.page.is(PacketMine.Page.Render))
   );
   public final BooleanSetting air = this.add(new BooleanSetting("Air", false, () -> this.page.is(PacketMine.Page.Render)));
   public final BooleanSetting fade = this.add(new BooleanSetting("Fade", true, () -> this.page.is(PacketMine.Page.Render)));
   public final ColorSetting fillColor = this.add(new ColorSetting("Fill", new Color(255, 0, 0, 80), () -> this.page.is(PacketMine.Page.Render)));
   public final ColorSetting outlineColor = this.add(new ColorSetting("Outline", new Color(255, 0, 0), () -> this.page.is(PacketMine.Page.Render)));
   public final ColorSetting secondFillColor = this.add(new ColorSetting("SecondFill", new Color(255, 0, 0, 80), () -> this.page.is(PacketMine.Page.Render)));
   public final ColorSetting secondOutlineColor = this.add(new ColorSetting("SecondOutline", new Color(255, 0, 0), () -> this.page.is(PacketMine.Page.Render)));
   public final SliderSetting lineWidth = this.add(new SliderSetting("LineWidth", 2.0, 0.5, 10.0, 0.1, () -> this.page.is(PacketMine.Page.Render)));
   public double lastMineSpeed = -1.0;
   public PacketMine.BlockDate breakBlockDate;
   public PacketMine.BlockDate firstBreakBlockDate;
   public static PacketMine.BlockDate rebreakBlockDate;
   public static final List<Block> godBlocks = Arrays.asList(
      Blocks.COMMAND_BLOCK,
      Blocks.LAVA,
      Blocks.WATER,
      Blocks.BEDROCK,
      Blocks.BARRIER,
      Blocks.END_PORTAL,
      Blocks.NETHER_PORTAL,
      Blocks.END_PORTAL_FRAME,
      Blocks.AIR
   );
   public static final List<Block> hardBlocks = Arrays.asList(Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.ENDER_CHEST);
   private final HashMap<BlockPos, Timer> donePos = new HashMap<>();
   private final Timer rebreakTimer = new Timer();
   private final Timer breakTimer = new Timer();
   private final Timer mineIntervalTimer = new Timer();
   private boolean doubleBreakAutoSwitchHold;
   private BlockPos doubleBreakAutoSwitchPos;
   private boolean doubleBreakSwapBack;
   private int doubleBreakSwapSlot = -1;
   private boolean mineIntervalArmed;
   private PacketMine.MineData stopRetryData;
   private int stopRetryTicksLeft;
   public boolean shouldPlaceEnderChest = false;
   public boolean shouldMineDown = false;
   private boolean manualOverride;
   private boolean autoMineClick;
   private PacketMine.MiningGoal autoMineGoal = PacketMine.MiningGoal.MINING_ENEMY;
   private BlockPos clickPos = null;

   public PacketMine() {
      super("PacketMine", Module.Category.Player);
      this.setChinese("发包挖掘");
      INSTANCE = this;
   }

   public static BlockPos getBreakPos() {
      return INSTANCE != null && INSTANCE.isOn() && INSTANCE.breakBlockDate != null ? INSTANCE.breakBlockDate.pos : null;
   }

   public static boolean unbreakable(BlockPos pos) {
      return mc.world != null && pos != null && mc.world.getBlockState(pos).getHardness(mc.world, pos) < 0.0F;
   }

   public void mine(BlockPos pos) {
      if (!nullCheck() && !unbreakable(pos) && this.isOn()) {
         Direction direction = BlockUtil.getClickSide(pos);
         this.attackAutoMineBlock(new PacketMine.MineData(pos, direction == null ? Direction.UP : direction, PacketMine.MiningGoal.MINING_ENEMY));
      }
   }

   @Override
   public void onEnable() {
      progress = 0.0;
      this.breakBlockDate = null;
      rebreakBlockDate = null;
      this.firstBreakBlockDate = null;
      this.donePos.clear();
      this.rebreakTimer.reset();
      this.mineIntervalTimer.reset();
      this.mineIntervalArmed = false;
      this.lastMineSpeed = -1.0;
      this.shouldPlaceEnderChest = false;
      this.shouldMineDown = false;
      this.manualOverride = false;
      this.autoMineClick = false;
      this.autoMineGoal = PacketMine.MiningGoal.MINING_ENEMY;
      this.clickPos = null;
      this.doubleBreakAutoSwitchHold = false;
      this.doubleBreakAutoSwitchPos = null;
      this.doubleBreakSwapBack = false;
      this.doubleBreakSwapSlot = -1;
      this.clearStopRetry();
   }

   @Override
   public void onDisable() {
      this.reset();
      progress = 0.0;
      this.breakBlockDate = null;
      rebreakBlockDate = null;
      this.donePos.clear();
      this.rebreakTimer.reset();
      this.lastMineSpeed = -1.0;
      this.shouldPlaceEnderChest = false;
      this.shouldMineDown = false;
      this.manualOverride = false;
      this.autoMineClick = false;
      this.autoMineGoal = PacketMine.MiningGoal.MINING_ENEMY;
      this.clickPos = null;
      this.doubleBreakAutoSwitchHold = false;
      this.doubleBreakAutoSwitchPos = null;
      this.doubleBreakSwapBack = false;
      this.doubleBreakSwapSlot = -1;
      this.clearStopRetry();
   }

   public void onTick(ClientTickEvent event) {
      if (!nullCheck() && !event.isPost()) {
         if (this.manualOverride && !this.hasManualMine()) {
            this.manualOverride = false;
         }

         this.check();
         this.handleStopRetry();
         if (this.doubleBreakAutoSwitchHold && (!this.isNewDisablerDoubleBreak() || !this.autoSwitch.getValue())) {
            this.releaseDoubleBreakAutoSwitch();
         }

         if (!this.isPause(this.pause.getValue())) {
            if (this.firstBreakBlockDate != null) {
               if (!this.firstBreakBlockDate.done) {
                  this.firstBreakBlockDate.freshProgress();
               } else if (mc.world.getBlockState(this.firstBreakBlockDate.pos).getBlock().asItem() != Items.AIR
                  && !this.firstBreakBlockDate.finishTimer.passed((long)this.firstWait.getValueInt() * 100L)) {
                  int toolSlot = this.getToolSlot(mc.world.getBlockState(this.firstBreakBlockDate.pos));
                  if (this.isNewDisablerDoubleBreak()) {
                     this.beginDoubleBreakAutoSwitch(this.firstBreakBlockDate, toolSlot);
                  } else {
                     this.swapTo(toolSlot);
                  }

                  EntityUtil.swingHand(Hand.MAIN_HAND, this.swingSide.getValue());
                  if (!this.grimV3.getValue()) {
                     this.sendPacket(
                        new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, this.firstBreakBlockDate.pos, this.clickSide(this.firstBreakBlockDate.pos))
                     );
                  }

                  if (!this.doubleBreakAutoSwitchHold) {
                     this.swapSync(toolSlot);
                  }
               } else {
                  this.releaseDoubleBreakAutoSwitch();
                  this.firstBreakBlockDate = null;
               }
            }

            if (this.breakBlockDate != null) {
               Block currentBlock = mc.world.getBlockState(this.breakBlockDate.pos).getBlock();
               if (currentBlock == Blocks.AIR || currentBlock == Blocks.WATER || currentBlock == Blocks.LAVA) {
                  if (this.fade.getValue()) {
                     this.donePos.put(this.breakBlockDate.pos, new Timer());
                  }

                  rebreakBlockDate = new PacketMine.BlockDate(this.breakBlockDate.pos, this.breakBlockDate.direction, this.breakBlockDate.goal);
                  this.breakBlockDate = null;
                  this.breakTimer.reset();
                  this.clickPos = null;
                  return;
               }

               if (!this.breakBlockDate.isMining) {
                  if (!this.breakTimer.passed(this.breakBlockDate.getStartDelayMs())) {
                     return;
                  }

                  this.breakBlockDate.isMining = true;
                  EntityUtil.swingHand(Hand.MAIN_HAND, this.swingSide.getValue());
                  this.spoofGroundIfNeeded();
                  if (!this.grimV3.getValue()) {
                     if (this.doubleBreak.getValue() && !mc.player.isInFluid()) {
                        this.sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, this.breakBlockDate.pos, this.breakBlockDate.direction));
                        this.sendPacket(new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, this.breakBlockDate.pos, this.breakBlockDate.direction));
                        this.sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, this.breakBlockDate.pos, this.breakBlockDate.direction));
                     }

                     if (mc.player.isInFluid() || !this.doubleBreak.getValue()) {
                        this.sendPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, this.breakBlockDate.pos, this.breakBlockDate.direction));
                        this.sendPacket(new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, this.breakBlockDate.pos, this.breakBlockDate.direction));
                        this.sendPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, this.breakBlockDate.pos, this.breakBlockDate.direction));
                     }
                  } else {
                     this.sendNewDisablerStart(this.breakBlockDate.pos, this.clickSide(this.breakBlockDate.pos));
                  }

                  this.breakBlockDate.freshProgress();
                  return;
               }

               if (!this.breakBlockDate.done) {
                  this.breakBlockDate.freshProgress();
               } else {
                  this.finishCurrentMine();
               }
            }

            if (rebreakBlockDate != null
               && this.breakBlockDate == null
               && mc.world.getBlockState(rebreakBlockDate.pos).getBlock() != Blocks.AIR
               && mc.world.getBlockState(rebreakBlockDate.pos).getBlock() != Blocks.WATER
               && mc.world.getBlockState(rebreakBlockDate.pos).getBlock() != Blocks.LAVA
               && this.rebreak.getValue()
               && this.rebreakTimer.passedMs((double)this.rebreakDelay.getValueInt() * 200.0)) {
               this.rebreakTimer.reset();
               int slot = this.getToolSlot(mc.world.getBlockState(rebreakBlockDate.pos));
               this.swapTo(slot);
               if (!rebreakBlockDate.subed && this.grimV3.getValue()) {
                  rebreakBlockDate.subed = true;
                  this.sendPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, rebreakBlockDate.pos, this.clickSide(rebreakBlockDate.pos)));
               } else {
                  this.sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, rebreakBlockDate.pos, this.clickSide(rebreakBlockDate.pos)));
                  this.rebreakTimer.reset();
               }

               this.swapSync(slot);
            }
         }
      }
   }

   private void finishCurrentMine() {
      if (this.breakBlockDate != null) {
         PacketMine.MineData finishedData = this.breakBlockDate.toMineData();
         if (this.fade.getValue() && !this.airEnd.getValue()) {
            this.donePos.put(this.breakBlockDate.pos, new Timer());
         }

         int toolSlot = this.getToolSlot(mc.world.getBlockState(this.breakBlockDate.pos));
         this.swapTo(toolSlot);
         EntityUtil.swingHand(Hand.MAIN_HAND, this.swingSide.getValue());
         boolean newDisablerDoubleBreak = this.grimV3.getValue() && this.doubleBreak.getValue();
         if (hardBlocks.contains(mc.world.getBlockState(this.breakBlockDate.pos).getBlock()) && this.grimV3.getValue()) {
            this.spoofGroundIfNeeded();
            this.sendNewDisablerStop(this.breakBlockDate.pos, this.clickSide(this.breakBlockDate.pos));
            if (!newDisablerDoubleBreak) {
               this.sendPacket(new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, this.breakBlockDate.pos, this.clickSide(this.breakBlockDate.pos)));
               this.sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, this.breakBlockDate.pos, this.clickSide(this.breakBlockDate.pos)));
            }
         } else if (this.resetMine.getValue()) {
            if (this.breakBlockDate.stopNums < this.stopTimes.getValueInt()) {
               this.spoofGroundIfNeeded();
               if (this.grimV3.getValue()) {
                  this.sendNewDisablerStop(this.breakBlockDate.pos, this.clickSide(this.breakBlockDate.pos));
               } else {
                  this.sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, this.breakBlockDate.pos, this.clickSide(this.breakBlockDate.pos)));
               }

               this.breakBlockDate.stopNums++;
               this.breakBlockDate.stoped = true;
            }
         } else {
            this.spoofGroundIfNeeded();
            this.breakBlockDate.stoped = true;
            if (this.grimV3.getValue()) {
               this.sendNewDisablerStop(this.breakBlockDate.pos, this.clickSide(this.breakBlockDate.pos));
            } else {
               this.sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, this.breakBlockDate.pos, this.clickSide(this.breakBlockDate.pos)));
            }
         }

         this.swapSync(toolSlot);
         this.armStopRetry(finishedData);
         if (!this.airEnd.getValue()) {
            rebreakBlockDate = new PacketMine.BlockDate(this.breakBlockDate.pos, this.breakBlockDate.direction, this.breakBlockDate.goal);
            this.breakBlockDate = null;
            this.clickPos = null;
         } else if (this.resetMine.getValue() && this.breakBlockDate.finishTimer.passed((long)this.rmDelay.getValueInt() * 100L)) {
            this.sendPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, this.breakBlockDate.pos, this.breakBlockDate.direction));
            this.setBreakBlockDate(this.breakBlockDate.pos, this.clickSide(this.breakBlockDate.pos), this.breakBlockDate.goal);
         }

         this.breakTimer.reset();
      }
   }

   public void onClickBlock(ClickBlockEvent event) {
      if (!nullCheck()) {
         if (mc.player.isCreative()) {
            this.reset();
         } else if (!this.isPause(this.pause.getValue())) {
            if (this.cancelClick.getValue()) {
               event.setCancelled(true);
            }

            BlockPos pos = event.getPos();
            Direction direction = event.getDirection();
            if (this.clickSwing.getValue()) {
               EntityUtil.swingHand(Hand.MAIN_HAND, this.swingSide.getValue());
            }

            if (!this.autoMineClick && this.hasAutoMine()) {
               this.manualOverride = true;
               this.abortGoal(PacketMine.MiningGoal.MINING_ENEMY);
            }

            if (this.firstBreakBlockDate == null || !this.firstBreakBlockDate.pos.equals(pos)) {
               if (this.breakBlockDate == null || !this.breakBlockDate.pos.equals(pos)) {
                  if (mc.world.getBlockState(pos).getBlock() == Blocks.NETHERRACK
                     && direction == Direction.UP
                     && pos.getX() == mc.player.getBlockX()
                     && pos.getZ() == mc.player.getBlockZ()
                     && this.grimV3.getValue()) {
                     int bestSlot = this.getToolSlot(mc.world.getBlockState(pos));
                     if (this.autoSwitch.getValue() && bestSlot != -1) {
                        this.swapTo(bestSlot);
                        this.swapSync(bestSlot);
                     }

                     if (this.breakBlockDate != null && this.breakBlockDate.isMining) {
                        this.sendPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, this.breakBlockDate.pos, this.clickSide(this.breakBlockDate.pos)));
                     }
                  } else if (mc.world.getBlockState(pos).getBlock() != Blocks.NETHERRACK
                     || this.breakBlockDate == null
                     || mc.world.getBlockState(this.breakBlockDate.pos).getBlock() != Blocks.NETHERRACK
                     || !this.grimV3.getValue()) {
                     if (!godBlocks.contains(mc.world.getBlockState(pos).getBlock())
                        && (this.breakBlockDate == null || !this.breakBlockDate.pos.equals(pos))
                        && (rebreakBlockDate == null || !rebreakBlockDate.pos.equals(pos) || this.clickReset.getValue())
                        && !(pos.toCenterPos().distanceTo(mc.player.getPos()) > this.range.getValue())) {
                        if (this.clickPos == null || !this.clickPos.equals(pos)) {
                           this.clickPos = pos;
                        }

                        PacketMine.MiningGoal goal = this.autoMineClick ? this.autoMineGoal : PacketMine.MiningGoal.MANUAL;
                        PacketMine.BlockDate previousMine = this.breakBlockDate;
                        this.setBreakBlockDate(pos, direction, goal, this.getMineIntervalDelayMs());
                        if (this.breakBlockDate != previousMine && this.matchesMine(this.breakBlockDate, pos, goal)) {
                           this.mineIntervalTimer.reset();
                           this.mineIntervalArmed = true;
                           if (this.breakBlockDate.hasMineIntervalDelay()) {
                              event.setCancelled(true);
                           }
                        }

                        if (this.autoMineClick) {
                           this.manualOverride = false;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public void onEntityAdd(EntitySpawnEvent event) {
      if (!nullCheck()) {
         if (this.breakBlockDate != null
            && event.getEntity() instanceof ItemEntity itemEntity
            && this.distanceTo(itemEntity) < this.range.getValue()
            && itemEntity.getBoundingBox().intersects(new Box(this.breakBlockDate.pos))) {
            if (this.fade.getValue()) {
               this.donePos.put(this.breakBlockDate.pos, new Timer());
            }

            rebreakBlockDate = new PacketMine.BlockDate(this.breakBlockDate.pos, this.breakBlockDate.direction, this.breakBlockDate.goal);
            this.breakBlockDate = null;
            this.breakTimer.reset();
            this.clickPos = null;
         }

         if (this.firstBreakBlockDate != null
            && event.getEntity() instanceof ItemEntity itemEntity
            && this.distanceTo(itemEntity) < this.range.getValue()
            && itemEntity.getBoundingBox().intersects(new Box(this.firstBreakBlockDate.pos))) {
            if (this.fade.getValue()) {
               this.donePos.put(this.firstBreakBlockDate.pos, new Timer());
            }

            this.releaseDoubleBreakAutoSwitch();
            this.firstBreakBlockDate = null;
         }
      }
   }

   public void onPacketSend(PacketEvent.Send event) {
      if (!nullCheck()) {
         if (event.getPacket() instanceof PlayerActionC2SPacket packet
            && this.isDestroyAction(packet.getAction())
            && this.isPendingDelayedMine(packet.getPos())) {
            event.cancel();
            return;
         }

         if (event.getPacket() instanceof PlayerActionC2SPacket packet
            && !this.grimV3.getValue()
            && (!this.doubleBreak.getValue() || mc.player.isInFluid())
            && packet.getAction() == Action.START_DESTROY_BLOCK
            && this.breakBlockDate != null
            && this.breakBlockDate.pos.equals(packet.getPos())) {
            this.sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, this.breakBlockDate.pos, this.clickSide(this.breakBlockDate.pos)));
         }
      }
   }

   public void onPacketReceive(PacketEvent.Receive event) {
      if (this.isNewDisablerDoubleBreak() || this.doubleBreakAutoSwitchHold) {
         if (event.getPacket() instanceof BlockUpdateS2CPacket packet) {
            this.handleDoubleBreakBlockUpdate(packet);
         } else if (event.getPacket() instanceof BundleS2CPacket packet) {
            for (Packet<?> bundled : packet.getPackets()) {
               if (bundled instanceof BlockUpdateS2CPacket blockPacket) {
                  this.handleDoubleBreakBlockUpdate(blockPacket);
               }
            }
         }
      }
   }

   private void handleDoubleBreakBlockUpdate(BlockUpdateS2CPacket packet) {
      if (packet.getState().isAir()) {
         if (this.firstBreakBlockDate != null && this.firstBreakBlockDate.pos.equals(packet.getPos())) {
            this.firstBreakBlockDate = null;
            this.releaseDoubleBreakAutoSwitch();
         } else {
            if (this.doubleBreakAutoSwitchPos != null && this.doubleBreakAutoSwitchPos.equals(packet.getPos())) {
               this.releaseDoubleBreakAutoSwitch();
            }
         }
      }
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      if (this.renderEnable.getValue() && !nullCheck()) {
         this.renderDonePos(matrixStack);
         this.renderBlockDate(matrixStack, this.firstBreakBlockDate, this.fillColor.getValue(), this.outlineColor.getValue(), 1.0);
         this.renderBlockDate(matrixStack, this.breakBlockDate, this.secondFillColor.getValue(), this.secondOutlineColor.getValue(), this.speed.getValue());
         if (this.rebreak.getValue() && rebreakBlockDate != null && this.breakBlockDate == null) {
            this.renderMioBox(matrixStack, rebreakBlockDate.pos, 1.0F, this.secondFillColor.getValue(), this.secondOutlineColor.getValue(), true);
         }
      }
   }

   private void renderDonePos(MatrixStack matrixStack) {
      if (!this.donePos.isEmpty() && this.fade.getValue()) {
         Set<BlockPos> blockPos = this.donePos.keySet();

         for (BlockPos pos : new ArrayList<>(blockPos)) {
            Timer timer = this.donePos.get(pos);
            if (timer.passed(500L)) {
               this.donePos.remove(pos);
            } else {
               float factor = timer.getMs() < 150L ? 1.0F : 1.0F - (float)(timer.getMs() - 150L) / 350.0F;
               Color fill = this.withAlpha(this.secondFillColor.getValue(), (int)((float)this.secondFillColor.getValue().getAlpha() * factor));
               Color outline = this.withAlpha(this.secondOutlineColor.getValue(), (int)((float)this.secondOutlineColor.getValue().getAlpha() * factor));
               this.renderMioBox(matrixStack, pos, 1.0F, fill, outline, true);
            }
         }
      }
   }

   private void renderBlockDate(MatrixStack matrixStack, PacketMine.BlockDate data, Color fill, Color outline, double maxProgress) {
      if (data != null) {
         float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
         float denominator = (float)Math.max(0.001, maxProgress);
         float progress = MathHelper.clamp((float)MathHelper.lerp((double)tickDelta, data.prevProgress, data.progress) / denominator, 0.0F, 1.0F);
         this.renderMioBox(matrixStack, data.pos, progress, fill, outline, this.rebreak.getValue());
      }
   }

   private void renderMioBox(MatrixStack matrixStack, BlockPos pos, float progress, Color fill, Color outline, boolean allowAirRender) {
      BlockState state = mc.world.getBlockState(pos);
      VoxelShape shape = state.getOutlineShape(mc.world, pos);
      boolean emptyShape = shape.isEmpty();
      if (emptyShape || !this.canMine(state)) {
         if (!allowAirRender) {
            return;
         }

         if (!this.air.getValue() && !this.fade.getValue()) {
            return;
         }
      }

      Box baseBox = emptyShape ? new Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0) : shape.getBoundingBox();
      Box box = this.applyMioRenderMode(baseBox, progress).offset(pos);
      if (state.isAir()) {
         box = new Box(pos);
      }

      Color[] colors = this.getMioColors(fill, outline, progress);
      Render3DUtil.draw3DBox(matrixStack, box, colors[0], colors[1], true, true, this.lineWidth.getValueFloat());
   }

   private Box applyMioRenderMode(Box box, float progress) {
      return switch ((PacketMine.MioRenderMode)this.renderMode.getValue()) {
         case Plain -> box;
         case In -> this.scaleBox(box, 1.0F - progress);
         case Out -> this.scaleBox(box, progress);
      };
   }

   private Box scaleBox(Box box, float scale) {
      double x = Math.abs(box.getLengthX());
      double y = Math.abs(box.getLengthY());
      double z = Math.abs(box.getLengthZ());
      Vec3d center = box.getCenter();
      return new Box(
         center.subtract((double)scale * x / 2.0, (double)scale * y / 2.0, (double)scale * z / 2.0),
         center.add((double)scale * x / 2.0, (double)scale * y / 2.0, (double)scale * z / 2.0)
      );
   }

   private Color[] getMioColors(Color fill, Color outline, float progress) {
      if (this.colorMode.getValue() == PacketMine.MioColorMode.Custom) {
         return new Color[]{fill, outline};
      } else {
         Color progressColor = this.lerpColor(new Color(255, 0, 0), new Color(0, 255, 0), progress);
         return new Color[]{this.withAlpha(progressColor, fill.getAlpha()), this.withAlpha(progressColor, outline.getAlpha())};
      }
   }

   private Color lerpColor(Color from, Color to, float progress) {
      float clamped = MathHelper.clamp(progress, 0.0F, 1.0F);
      return new Color(
         MathHelper.lerp(clamped, from.getRed(), to.getRed()),
         MathHelper.lerp(clamped, from.getGreen(), to.getGreen()),
         MathHelper.lerp(clamped, from.getBlue(), to.getBlue())
      );
   }

   private Color withAlpha(Color color, int alpha) {
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), MathHelper.clamp(alpha, 0, 255));
   }

   public void check() {
      if (this.firstBreakBlockDate != null && this.distanceTo(this.firstBreakBlockDate.pos) > this.range.getValue()) {
         if (this.firstBreakBlockDate.isMining) {
            this.sendPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, this.firstBreakBlockDate.pos, this.clickSide(this.firstBreakBlockDate.pos)));
         }

         this.releaseDoubleBreakAutoSwitch();
         this.firstBreakBlockDate = null;
      }

      if (this.breakBlockDate != null && this.distanceTo(this.breakBlockDate.pos) > this.range.getValue()) {
         if (this.breakBlockDate.isMining) {
            this.sendPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, this.breakBlockDate.pos, this.clickSide(this.breakBlockDate.pos)));
         }

         this.breakBlockDate = null;
      }

      if (rebreakBlockDate != null && this.distanceTo(rebreakBlockDate.pos) > this.range.getValue()) {
         rebreakBlockDate = null;
      }
   }

   public void setBreakBlockDate(BlockPos pos, Direction direction) {
      this.setBreakBlockDate(pos, direction, PacketMine.MiningGoal.MANUAL);
   }

   public void setBreakBlockDate(BlockPos pos, Direction direction, PacketMine.MiningGoal goal) {
      this.setBreakBlockDate(pos, direction, goal, 0);
   }

   private void setBreakBlockDate(BlockPos pos, Direction direction, PacketMine.MiningGoal goal, int startDelayMs) {
      if (this.breakBlockDate != null) {
         if (this.breakBlockDate.pos.equals(this.clickPos)) {
            if (this.resetMine.getValue()
               && this.breakBlockDate.finishTimer != null
               && this.breakBlockDate.finishTimer.passed((long)this.rmDelay.getValueInt() * 100L)) {
               this.breakBlockDate = new PacketMine.BlockDate(pos, direction, goal);
            }

            return;
         }

         if (this.breakBlockDate.pos.equals(pos)) {
            if (this.breakBlockDate.done) {
               this.sendPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, this.breakBlockDate.pos, this.clickSide(this.breakBlockDate.pos)));
               this.breakBlockDate = new PacketMine.BlockDate(pos, direction, goal);
            }

            return;
         }

         if (this.breakBlockDate.isMining) {
            if (this.doubleBreak.getValue()
               && (mc.world.getBlockState(this.breakBlockDate.pos).getBlock() != Blocks.NETHERRACK || !this.grimV3.getValue())
               && (this.grimV3.getValue() || !mc.player.isInFluid())) {
               if (this.firstBreakBlockDate != null && this.firstBreakBlockDate.pos.equals(pos)) {
                  return;
               }

               if (this.breakBlockDate.stoped) {
                  return;
               }

               this.breakBlockDate.stoped = true;
               if (this.firstBreakBlockDate == null) {
                  this.firstBreakBlockDate = this.breakBlockDate;
               }

               if (!this.grimV3.getValue()) {
                  this.sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, this.breakBlockDate.pos, this.clickSide(this.breakBlockDate.pos)));
               }
            } else if (this.grimV3.getValue()) {
               this.sendPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, this.breakBlockDate.pos, this.clickSide(this.breakBlockDate.pos)));
            }
         } else if (this.doubleBreak.getValue()
            && this.firstBreakBlockDate == null
            && (mc.world.getBlockState(this.breakBlockDate.pos).getBlock() != Blocks.NETHERRACK || !this.grimV3.getValue())
            && !this.breakBlockDate.stoped
            && (this.grimV3.getValue() || !mc.player.isInFluid())) {
            this.breakBlockDate.stoped = true;
            if (!this.grimV3.getValue()) {
               this.sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, this.breakBlockDate.pos, this.clickSide(this.breakBlockDate.pos)));
               this.sendPacket(new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, this.breakBlockDate.pos, this.clickSide(this.breakBlockDate.pos)));
               this.sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, this.breakBlockDate.pos, this.clickSide(this.breakBlockDate.pos)));
            } else {
               this.breakBlockDate.isMining = true;
               this.sendNewDisablerStart(this.breakBlockDate.pos, this.clickSide(this.breakBlockDate.pos));
            }

            this.firstBreakBlockDate = this.breakBlockDate;
         }
      }

      if (rebreakBlockDate != null) {
         if (mc.world.getBlockState(rebreakBlockDate.pos).getBlock().asItem() != Items.AIR) {
            this.sendPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, rebreakBlockDate.pos, this.clickSide(rebreakBlockDate.pos)));
         }

         rebreakBlockDate = null;
      }

      this.breakBlockDate = new PacketMine.BlockDate(pos, direction, goal, startDelayMs);
      progress = 0.0;
      this.breakTimer.reset();
   }

   private int getMineIntervalDelayMs() {
      return !this.mineIntervalArmed ? 0 : (int)Math.max(0L, (long)this.mineInterval.getValueInt() - this.mineIntervalTimer.getMs());
   }

   public void reset() {
      progress = 0.0;
      this.mineIntervalArmed = false;
      this.releaseDoubleBreakAutoSwitch();
      this.clearStopRetry();
      if (this.breakBlockDate != null) {
         if (this.breakBlockDate.isMining) {
            this.sendPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, this.breakBlockDate.pos, this.clickSide(this.breakBlockDate.pos)));
         }

         this.breakBlockDate = null;
      }

      if (rebreakBlockDate != null) {
         this.sendPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, rebreakBlockDate.pos, this.clickSide(rebreakBlockDate.pos)));
         rebreakBlockDate = null;
      }

      if (this.firstBreakBlockDate != null && this.firstBreakBlockDate.isMining) {
         this.sendPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, this.firstBreakBlockDate.pos, this.clickSide(this.firstBreakBlockDate.pos)));
      }

      this.firstBreakBlockDate = null;
      this.manualOverride = false;
      this.autoMineGoal = PacketMine.MiningGoal.MINING_ENEMY;
      this.clickPos = null;
   }

   public boolean canMine(BlockState state) {
      return state != null && !state.isAir() && state.getFluidState().isEmpty();
   }

   public boolean isMining(BlockPos pos) {
      return this.matchesMine(this.breakBlockDate, pos)
         || this.matchesMine(this.firstBreakBlockDate, pos)
         || this.matchesMine(rebreakBlockDate, pos)
         || this.matchesMine(this.stopRetryData, pos);
   }

   public boolean isManualOverride() {
      return this.manualOverride || this.hasManualMine();
   }

   public boolean isHandlingAutoMineClick() {
      return this.autoMineClick;
   }

   public boolean hasAutoMine() {
      return this.hasGoal(this.breakBlockDate, PacketMine.MiningGoal.AUTO_MINE, PacketMine.MiningGoal.MINING_ENEMY, PacketMine.MiningGoal.PREVENT_CRAWL)
         || this.hasGoal(this.firstBreakBlockDate, PacketMine.MiningGoal.AUTO_MINE, PacketMine.MiningGoal.MINING_ENEMY, PacketMine.MiningGoal.PREVENT_CRAWL)
         || this.hasGoal(rebreakBlockDate, PacketMine.MiningGoal.AUTO_MINE, PacketMine.MiningGoal.MINING_ENEMY, PacketMine.MiningGoal.PREVENT_CRAWL);
   }

   public boolean isCurrentMineComplete() {
      if (this.breakBlockDate == null) {
         return true;
      } else {
         BlockState state = mc.world.getBlockState(this.breakBlockDate.pos);
         return this.breakBlockDate.done && (state.isAir() || !this.canMine(state));
      }
   }

   public PacketMine.MineData getCurrentMineData() {
      return this.breakBlockDate == null ? null : this.breakBlockDate.toMineData();
   }

   public PacketMine.MineData getSecondaryMineData() {
      return this.firstBreakBlockDate == null ? null : this.firstBreakBlockDate.toMineData();
   }

   public PacketMine.MineData getRebreakMineData() {
      return rebreakBlockDate == null ? null : rebreakBlockDate.toMineData();
   }

   public boolean hasGoalMine(PacketMine.MiningGoal goal) {
      return this.hasGoal(this.breakBlockDate, goal) || this.hasGoal(this.firstBreakBlockDate, goal) || this.hasGoal(rebreakBlockDate, goal);
   }

   public int getGoalMineCount(PacketMine.MiningGoal goal) {
      int count = 0;
      if (this.hasGoal(this.breakBlockDate, goal)) {
         count++;
      }

      if (this.hasGoal(this.firstBreakBlockDate, goal)) {
         count++;
      }

      if (this.hasGoal(rebreakBlockDate, goal)) {
         count++;
      }

      return count;
   }

   public boolean isPausedForAutoMine() {
      return this.isPause(this.pause.getValue());
   }

   public boolean startAutoMine(PacketMine.MineData data) {
      if (!this.prepareAutoMine(data)) {
         return false;
      } else {
         this.clickPos = data.getPos();
         this.setBreakBlockDate(data.getPos(), data.getDirection(), data.getGoal());
         this.manualOverride = false;
         return true;
      }
   }

   public boolean attackAutoMineBlock(PacketMine.MineData data) {
      if (this.prepareAutoMine(data) && mc.interactionManager != null) {
         this.clickPos = data.getPos();
         this.autoMineClick = true;
         this.autoMineGoal = data.getGoal();

         boolean var3;
         try {
            boolean attacked = mc.interactionManager.attackBlock(data.getPos(), data.getDirection());
            var3 = attacked || this.matchesMine(this.breakBlockDate, data.getPos(), data.getGoal());
         } finally {
            this.autoMineClick = false;
            this.autoMineGoal = PacketMine.MiningGoal.MINING_ENEMY;
         }

         return var3;
      } else {
         return false;
      }
   }

   public boolean canStartAutoMine(PacketMine.MineData data) {
      if (nullCheck() || data == null || this.isManualOverride() || this.isPause(this.pause.getValue())) {
         return false;
      } else if (!this.canMine(data.getState()) || this.isMining(data.getPos())) {
         return false;
      } else if (this.hasHigherPriorityMine(data.getGoal())) {
         return false;
      } else if (this.hasLowerPriorityMine(data.getGoal())) {
         return true;
      } else if (this.breakBlockDate == null) {
         return true;
      } else {
         return this.breakBlockDate.goal == PacketMine.MiningGoal.MANUAL ? false : this.doubleBreak.getValue() && this.firstBreakBlockDate == null;
      }
   }

   private boolean prepareAutoMine(PacketMine.MineData data) {
      if (!this.canStartAutoMine(data)) {
         return false;
      } else {
         this.abortLowerPriorityMines(data.getGoal());
         return true;
      }
   }

   public void abortMine(PacketMine.MineData data) {
      if (data != null) {
         if (this.matchesMine(this.breakBlockDate, data.getPos(), data.getGoal())) {
            this.abortBlockDate(this.breakBlockDate);
            this.breakBlockDate = null;
            this.clickPos = null;
         }

         if (this.matchesMine(this.firstBreakBlockDate, data.getPos(), data.getGoal())) {
            this.abortBlockDate(this.firstBreakBlockDate);
            this.releaseDoubleBreakAutoSwitch();
            this.firstBreakBlockDate = null;
         }

         if (this.matchesMine(rebreakBlockDate, data.getPos(), data.getGoal())) {
            this.abortBlockDate(rebreakBlockDate);
            rebreakBlockDate = null;
         }
      }
   }

   public void abortGoal(PacketMine.MiningGoal goal) {
      if (goal != null) {
         if (this.breakBlockDate != null && this.breakBlockDate.goal == goal) {
            this.abortBlockDate(this.breakBlockDate);
            this.breakBlockDate = null;
            this.clickPos = null;
         }

         if (this.firstBreakBlockDate != null && this.firstBreakBlockDate.goal == goal) {
            this.abortBlockDate(this.firstBreakBlockDate);
            this.releaseDoubleBreakAutoSwitch();
            this.firstBreakBlockDate = null;
         }

         if (rebreakBlockDate != null && rebreakBlockDate.goal == goal) {
            this.abortBlockDate(rebreakBlockDate);
            rebreakBlockDate = null;
         }
      }
   }

   private void abortBlockDate(PacketMine.BlockDate data) {
      if (data != null) {
         this.sendPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, data.pos, this.clickSide(data.pos)));
      }
   }

   private boolean hasGoal(PacketMine.BlockDate data, PacketMine.MiningGoal... goals) {
      if (data == null) {
         return false;
      } else {
         for (PacketMine.MiningGoal goal : goals) {
            if (data.goal == goal) {
               return true;
            }
         }

         return false;
      }
   }

   private boolean hasManualMine() {
      return this.hasGoal(this.breakBlockDate, PacketMine.MiningGoal.MANUAL)
         || this.hasGoal(this.firstBreakBlockDate, PacketMine.MiningGoal.MANUAL)
         || this.hasGoal(rebreakBlockDate, PacketMine.MiningGoal.MANUAL);
   }

   private boolean hasHigherPriorityMine(PacketMine.MiningGoal goal) {
      return this.hasPriorityMine(goal, 1);
   }

   private boolean hasLowerPriorityMine(PacketMine.MiningGoal goal) {
      return this.hasPriorityMine(goal, -1);
   }

   private boolean hasPriorityMine(PacketMine.MiningGoal goal, int direction) {
      int priority = this.getGoalPriority(goal);
      return this.hasPriorityMine(this.breakBlockDate, priority, direction)
         || this.hasPriorityMine(this.firstBreakBlockDate, priority, direction)
         || this.hasPriorityMine(rebreakBlockDate, priority, direction);
   }

   private boolean hasPriorityMine(PacketMine.BlockDate data, int priority, int direction) {
      if (data == null) {
         return false;
      } else {
         int otherPriority = this.getGoalPriority(data.goal);
         return direction > 0 ? otherPriority > priority : otherPriority < priority;
      }
   }

   private void abortLowerPriorityMines(PacketMine.MiningGoal goal) {
      int priority = this.getGoalPriority(goal);
      if (this.breakBlockDate != null && this.getGoalPriority(this.breakBlockDate.goal) < priority) {
         this.abortBlockDate(this.breakBlockDate);
         this.breakBlockDate = null;
         this.clickPos = null;
      }

      if (this.firstBreakBlockDate != null && this.getGoalPriority(this.firstBreakBlockDate.goal) < priority) {
         this.abortBlockDate(this.firstBreakBlockDate);
         this.releaseDoubleBreakAutoSwitch();
         this.firstBreakBlockDate = null;
      }

      if (rebreakBlockDate != null && this.getGoalPriority(rebreakBlockDate.goal) < priority) {
         this.abortBlockDate(rebreakBlockDate);
         rebreakBlockDate = null;
      }
   }

   private int getGoalPriority(PacketMine.MiningGoal goal) {
      return switch (goal) {
         case MANUAL -> 3;
         case AUTO_MINE, MINING_ENEMY -> 1;
         case PREVENT_CRAWL -> 2;
      };
   }

   private boolean matchesMine(PacketMine.BlockDate data, BlockPos pos) {
      return data != null && data.pos.equals(pos);
   }

   private boolean matchesMine(PacketMine.BlockDate data, BlockPos pos, PacketMine.MiningGoal goal) {
      return data != null && data.pos.equals(pos) && data.goal == goal;
   }

   private boolean matchesMine(PacketMine.MineData data, BlockPos pos) {
      return data != null && data.getPos().equals(pos);
   }

   private boolean armStopRetry(PacketMine.MineData data) {
      if (this.retryAfterStop.getValue() && data != null && mc.world != null) {
         this.stopRetryData = data;
         this.stopRetryTicksLeft = this.retryTicks.getValueInt();
         return true;
      } else {
         return false;
      }
   }

   private void handleStopRetry() {
      if (this.stopRetryData != null) {
         if (this.retryAfterStop.getValue() && !this.isPause(this.pause.getValue())) {
            if (this.distanceTo(this.stopRetryData.getPos()) > this.range.getValue()) {
               this.clearStopRetry();
            } else if (this.breakBlockDate == null && this.firstBreakBlockDate == null) {
               if (this.stopRetryTicksLeft > 0) {
                  this.stopRetryTicksLeft--;
               } else {
                  PacketMine.MineData retryData = this.stopRetryData;
                  this.clearStopRetry();
                  if (this.canMine(retryData.getState())) {
                     if (this.matchesMine(rebreakBlockDate, retryData.getPos(), retryData.getGoal())) {
                        rebreakBlockDate = null;
                     }

                     this.clickPos = retryData.getPos();
                     this.setBreakBlockDate(retryData.getPos(), retryData.getDirection(), retryData.getGoal());
                     this.manualOverride = false;
                  }
               }
            }
         } else {
            this.clearStopRetry();
         }
      }
   }

   private void clearStopRetry() {
      this.stopRetryData = null;
      this.stopRetryTicksLeft = 0;
   }

   private void swapTo(int slot) {
      if (this.autoSwitch.getValue() && slot != -1 && mc.player != null) {
         switch ((PacketMine.Swap)this.swapMode.getValue()) {
            case NORMAL:
               InventoryUtil.switchToSlot(slot);
               break;
            case SILENT:
               Alien.INVENTORY.setSlot(slot);
               break;
            case SILENT_ALT:
               Alien.INVENTORY.setSlotAlt(slot);
            case OFF:
         }
      }
   }

   private void swapSync(int slot) {
      if (this.autoSwitch.getValue() && slot != -1) {
         switch ((PacketMine.Swap)this.swapMode.getValue()) {
            case NORMAL:
            case OFF:
            default:
               break;
            case SILENT:
               Alien.INVENTORY.syncToClient();
               break;
            case SILENT_ALT:
               Alien.INVENTORY.setSlotAlt(slot);
         }
      }
   }

   private boolean isNewDisablerDoubleBreak() {
      return this.grimV3.getValue() && this.doubleBreak.getValue();
   }

   private void beginDoubleBreakAutoSwitch(PacketMine.BlockDate data, int slot) {
      if (this.isNewDisablerDoubleBreak() && this.autoSwitch.getValue() && this.swapMode.getValue() != PacketMine.Swap.OFF && data != null && slot != -1) {
         if (!this.doubleBreakAutoSwitchHold || !data.pos.equals(this.doubleBreakAutoSwitchPos)) {
            this.releaseDoubleBreakAutoSwitch();
            this.doubleBreakSwapBack = false;
            this.doubleBreakSwapSlot = -1;
            switch ((PacketMine.Swap)this.swapMode.getValue()) {
               case NORMAL:
                  InventoryUtil.switchToSlot(slot);
                  break;
               case SILENT:
                  if (slot != Alien.INVENTORY.getServerSlot()) {
                     if (!Alien.INVENTORY.trySetSlot(slot, noxisclient.core.impl.InventoryManager.SwapPriority.HIGH, "PacketMine")) {
                        return;
                     }

                     this.doubleBreakSwapBack = true;
                     this.doubleBreakSwapSlot = slot;
                  }
                  break;
               case SILENT_ALT:
                  if (slot != Alien.INVENTORY.getServerSlot()) {
                     Alien.INVENTORY.setSlotAlt(slot);
                     this.doubleBreakSwapBack = true;
                     this.doubleBreakSwapSlot = slot;
                  }
                  break;
               case OFF:
                  return;
            }

            this.doubleBreakAutoSwitchHold = true;
            this.doubleBreakAutoSwitchPos = data.pos;
         }
      }
   }

   private void releaseDoubleBreakAutoSwitch() {
      if (this.doubleBreakAutoSwitchHold) {
         this.doubleBreakAutoSwitchHold = false;
         this.doubleBreakAutoSwitchPos = null;
         if (this.doubleBreakSwapBack) {
            switch ((PacketMine.Swap)this.swapMode.getValue()) {
               case NORMAL:
               case OFF:
               default:
                  break;
               case SILENT:
                  Alien.INVENTORY.syncToClient("PacketMine");
                  break;
               case SILENT_ALT:
                  if (this.doubleBreakSwapSlot != -1) {
                     Alien.INVENTORY.setSlotAlt(this.doubleBreakSwapSlot);
                  }
            }
         }

         this.doubleBreakSwapBack = false;
         this.doubleBreakSwapSlot = -1;
      }
   }

   private boolean hasActiveMine() {
      return this.breakBlockDate != null || this.firstBreakBlockDate != null || rebreakBlockDate != null || this.stopRetryData != null;
   }

   private boolean isPendingDelayedMine(BlockPos pos) {
      return this.breakBlockDate != null && !this.breakBlockDate.isMining && this.breakBlockDate.hasMineIntervalDelay() && this.breakBlockDate.pos.equals(pos);
   }

   private boolean isDestroyAction(Action action) {
      return action == Action.START_DESTROY_BLOCK || action == Action.STOP_DESTROY_BLOCK || action == Action.ABORT_DESTROY_BLOCK;
   }

   public int getToolSlot(BlockState blockState) {
      double bestScore = -1.0;
      int bestSlot = -1;

      for (int i = 0; i < 9; i++) {
         double score = (double)mc.player.getInventory().getStack(i).getMiningSpeedMultiplier(blockState);
         if (score > bestScore) {
            bestScore = score;
            bestSlot = i;
         }
      }

      if (bestSlot < 0) {
         return mc.player.getInventory().selectedSlot;
      } else {
         Item item = mc.player.getInventory().getStack(bestSlot).getItem();
         if (!(item instanceof PickaxeItem) && !(item instanceof AxeItem) && !(item instanceof ShovelItem) && !(item instanceof SwordItem)) {
            bestSlot = -1;
         }

         return bestSlot == -1 ? mc.player.getInventory().selectedSlot : bestSlot;
      }
   }

   public float getWaitTime(BlockPos pos) {
      int toolSlot = this.getToolSlot(mc.world.getBlockState(pos));
      double mineSpeed = (double)this.getBlockBreakingSpeed(mc.world.getBlockState(pos), toolSlot);
      if (toolSlot == -1) {
         return this.waitTime.getValueFloat();
      } else {
         if (this.lastMineSpeed == -1.0) {
            this.lastMineSpeed = mineSpeed;
         } else {
            if (Math.abs(this.lastMineSpeed - mineSpeed) > 1.5) {
               this.lastMineSpeed = mineSpeed;
               return this.waitTime.getValueFloat();
            }

            this.lastMineSpeed = mineSpeed;
         }

         return mc.world.getBlockState(pos).getBlock() == Blocks.NETHERRACK
            ? 0.0F
            : (float)Math.min(Math.max(0.18F, (1.0 - mineSpeed) * 0.3F), (double)this.waitTime.getValueFloat());
      }
   }

   public double getBreakDelta(int slot, BlockState state, BlockPos pos) {
      float hardness = state.getHardness(mc.world, pos);
      if (hardness == -1.0F) {
         return 0.0;
      } else {
         ItemStack stack = mc.player.getInventory().getStack(slot);
         return (double)(this.getBlockBreakingSpeed(state, slot) / hardness / (float)(state.isToolRequired() && !stack.isSuitableFor(state) ? 100 : 30));
      }
   }

   private float getBlockBreakingSpeed(BlockState block, int slot) {
      if (slot == -1) {
         slot = mc.player.getInventory().selectedSlot;
      }

      ItemStack stack = mc.player.getInventory().getStack(slot >= 36 ? slot - 36 : slot);
      float f = stack.getMiningSpeedMultiplier(block);
      if (f > 1.0F) {
         int i = InventoryUtil.getEnchantmentLevel(stack, Enchantments.EFFICIENCY);
         if (i > 0 && !stack.isEmpty()) {
            f += (float)(i * i + 1);
         }
      }

      if (mc.player.hasStatusEffect(StatusEffects.HASTE)) {
         f *= 1.0F + (float)(mc.player.getStatusEffect(StatusEffects.HASTE).getAmplifier() + 1) * 0.2F;
      }

      if (mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
         float g = switch (mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
            case 0 -> 0.3F;
            case 1 -> 0.09F;
            case 2 -> 0.0027F;
            default -> 8.1E-4F;
         };
         f *= g;
      }

      if (!mc.player.isOnGround() && !this.noGSpeed.getValue()) {
         f /= 5.0F;
      }

      return f;
   }

   private Direction clickSide(BlockPos pos) {
      Direction side = BlockUtil.getClickSide(pos);
      return side == null ? Direction.UP : side;
   }

   private void spoofGroundIfNeeded() {
      if (this.bypassGround.getValue()) {
         mc.getNetworkHandler()
            .sendPacket(new Full(mc.player.getX(), mc.player.getY() + 1.0E-9, mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), true));
         mc.player.onLanding();
      }
   }

   private void sendNewDisablerStart(BlockPos pos, Direction direction) {
      sendSequencedPacket(sequence -> new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, pos, direction, sequence));
      sendSequencedPacket(sequence -> new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, NEWDISABLER_FAKE_POS, Direction.UP, sequence));
      this.sendNewDisablerStop(pos, direction);
      this.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
   }

   private void sendNewDisablerStop(BlockPos pos, Direction direction) {
      sendSequencedPacket(sequence -> new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, pos, direction, sequence));
   }

   private void sendPacket(Packet<?> packet) {
      if (mc.getNetworkHandler() != null) {
         mc.getNetworkHandler().sendPacket(packet);
      }
   }

   private double distanceTo(BlockPos pos) {
      return mc.player.getPos().distanceTo(pos.toCenterPos());
   }

   private double distanceTo(ItemEntity entity) {
      return mc.player.getPos().distanceTo(entity.getPos());
   }

   private boolean isPause(PacketMine.PauseMode mode) {
      return switch (mode) {
         case None -> false;
         case Use -> mc.player.isUsingItem();
         case MainHand -> mc.player.isUsingItem() && mc.player.getActiveHand() == Hand.MAIN_HAND;
      };
   }

   public class BlockDate {
      public BlockPos pos;
      public Direction direction;
      public final PacketMine.MiningGoal goal;
      public boolean done = false;
      public double progress;
      public double prevProgress;
      public BlockState blockState;
      public boolean isMining = false;
      public boolean stoped = false;
      public Timer finishTimer = null;
      public boolean subed = false;
      public int stopNums = 0;
      private final float waitTime;
      private final int startDelayMs;

      public BlockDate(BlockPos pos, Direction direction) {
         this(pos, direction, PacketMine.MiningGoal.MANUAL);
      }

      public BlockDate(BlockPos pos, Direction direction, PacketMine.MiningGoal goal) {
         this(pos, direction, goal, 0);
      }

      private BlockDate(BlockPos pos, Direction direction, PacketMine.MiningGoal goal, int startDelayMs) {
         this.pos = pos;
         this.direction = direction;
         this.goal = goal;
         this.done = false;
         this.progress = 0.0;
         this.blockState = Wrapper.mc.world.getBlockState(pos);
         this.waitTime = PacketMine.this.getWaitTime(pos);
         this.startDelayMs = Math.max(0, startDelayMs);
      }

      private long getStartDelayMs() {
         return Math.max((long)(this.waitTime * 1000.0F), (long)this.startDelayMs);
      }

      private boolean hasMineIntervalDelay() {
         return this.startDelayMs > 0;
      }

      public PacketMine.MineData toMineData() {
         return new PacketMine.MineData(this.pos, this.direction, this.goal);
      }

      public void freshProgress() {
         int bestSlot = PacketMine.this.getToolSlot(this.blockState);
         double targetProgress = this.equals(PacketMine.this.firstBreakBlockDate) ? 1.0 : (double)PacketMine.this.speed.getValueFloat();
         if (this.progress <= targetProgress) {
            this.prevProgress = this.progress;
            this.progress = this.progress + PacketMine.this.getBreakDelta(bestSlot, this.blockState, this.pos);
         } else {
            if (this == PacketMine.this.firstBreakBlockDate) {
               if (PacketMine.this.isNewDisablerDoubleBreak()) {
                  PacketMine.this.beginDoubleBreakAutoSwitch(this, bestSlot);
               } else {
                  PacketMine.this.swapTo(bestSlot);
               }
            }

            this.done = true;
            this.finishTimer = new Timer();
            this.progress = 1.0;
         }

         if (this == PacketMine.this.breakBlockDate) {
            PacketMine.progress = this.progress;
         }
      }
   }

   public static class MineData implements Comparable<PacketMine.MineData> {
      private final BlockPos pos;
      private final Direction direction;
      private final PacketMine.MiningGoal goal;

      public MineData(BlockPos pos, Direction direction) {
         this(pos, direction, PacketMine.MiningGoal.MINING_ENEMY);
      }

      public MineData(BlockPos pos, Direction direction, PacketMine.MiningGoal goal) {
         this.pos = pos;
         this.direction = direction;
         this.goal = goal;
      }

      public int compareTo(PacketMine.MineData o) {
         return Double.compare(this.getPriority(), o.getPriority());
      }

      private double getPriority() {
         return 10.0;
      }

      @Override
      public boolean equals(Object obj) {
         if (obj instanceof PacketMine.MineData data && data.getPos().equals(this.pos)) {
            return true;
         }

         return false;
      }

      @Override
      public int hashCode() {
         return this.pos.hashCode();
      }

      public BlockPos getPos() {
         return this.pos;
      }

      public Direction getDirection() {
         return this.direction;
      }

      public PacketMine.MiningGoal getGoal() {
         return this.goal;
      }

      public BlockState getState() {
         return Wrapper.mc.world.getBlockState(this.pos);
      }
   }

   public static enum MiningGoal {
      MANUAL,
      AUTO_MINE,
      MINING_ENEMY,
      PREVENT_CRAWL;
   }

   public static enum MioColorMode {
      Progress,
      Custom;
   }

   public static enum MioRenderMode {
      Plain,
      In,
      Out;
   }

   public static enum Page {
      General,
      Misc,
      Render;
   }

   public static enum PauseMode {
      None,
      Use,
      MainHand;
   }

   public static enum Swap {
      NORMAL,
      SILENT,
      SILENT_ALT,
      OFF;
   }
}
