package noxisclient.mod.modules.impl.combat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import noxisclient.Alien;
import noxisclient.api.events.impl.ClientTickEvent;
import noxisclient.api.utils.combat.CombatUtil;
import noxisclient.api.utils.math.Timer;
import noxisclient.api.utils.player.EntityUtil;
import noxisclient.api.utils.player.InventoryUtil;
import noxisclient.api.utils.world.BlockPosX;
import noxisclient.api.utils.world.BlockUtil;
import noxisclient.core.impl.CommandManager;
import noxisclient.core.impl.RotationManager;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.client.AntiCheat;
import noxisclient.mod.modules.impl.exploit.Blink;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class Burrow extends Module {
   public static Burrow INSTANCE;
   private final EnumSetting<Burrow.RotateMode> rotate = this.add(new EnumSetting<>("RotateMode", Burrow.RotateMode.Bypass));
   private final EnumSetting<Burrow.LagBackMode> lagMode = this.add(new EnumSetting<>("LagMode", Burrow.LagBackMode.TrollHack));
   private final EnumSetting<Burrow.LagBackMode> aboveLagMode = this.add(new EnumSetting<>("MoveLagMode", Burrow.LagBackMode.Smart));
   private final List<BlockPos> placePos = new ArrayList<>();
   private final Timer timer = new Timer();
   private final Timer webTimer = new Timer();
   private final BooleanSetting disable = this.add(new BooleanSetting("Disable", true));
   private final BooleanSetting jumpDisable = this.add(new BooleanSetting("JumpDisable", true, () -> !this.disable.getValue()));
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 500, 0, 1000, () -> !this.disable.getValue()));
   private final SliderSetting webTime = this.add(new SliderSetting("WebTime", 0, 0, 500));
   private final BooleanSetting enderChest = this.add(new BooleanSetting("EnderChest", true));
   private final BooleanSetting antiLag = this.add(new BooleanSetting("AntiLag", false));
   private final BooleanSetting single = this.add(new BooleanSetting("SingleBlock", false));
   private final BooleanSetting detectMine = this.add(new BooleanSetting("DetectMining", false));
   private final BooleanSetting headFill = this.add(new BooleanSetting("HeadFill", false));
   private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", false));
   private final BooleanSetting down = this.add(new BooleanSetting("Down", true));
   private final BooleanSetting noSelfPos = this.add(new BooleanSetting("NoSelfPos", false));
   private final BooleanSetting packetPlace = this.add(new BooleanSetting("PacketPlace", true));
   private final BooleanSetting sound = this.add(new BooleanSetting("Sound", true));
   private final SliderSetting blocksPer = this.add(new SliderSetting("BlocksPer", 4.0, 1.0, 4.0, 1.0));
   private final BooleanSetting breakCrystal = this.add(new BooleanSetting("Break", true));
   private final BooleanSetting wait = this.add(new BooleanSetting("Wait", true, this.disable::getValue));
   private final BooleanSetting fakeMove = this.add(new BooleanSetting("FakeMove", true).setParent());
   private final BooleanSetting center = this.add(new BooleanSetting("AllowCenter", true, this.fakeMove::isOpen));
   private final SliderSetting preCorrect = this.add(new SliderSetting("PreCorrect", 0.25, 0.0, 1.0, 0.001, this.fakeMove::isOpen));
   private final SliderSetting moveDis = this.add(new SliderSetting("MoveDis", 0.25, 0.0, 1.0, 0.001, this.fakeMove::isOpen));
   private final SliderSetting moveDis2 = this.add(new SliderSetting("MoveDis2", 0.25, 0.0, 1.0, 0.001, this.fakeMove::isOpen));
   private final SliderSetting moveCorrect2 = this.add(new SliderSetting("Correct", 0.25, 0.0, 1.0, 0.001, this.fakeMove::isOpen));
   private final SliderSetting yOffset = this.add(new SliderSetting("YOffset", 0.01, 0.0, 1.0, 0.001, this.fakeMove::isOpen));
   private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
   private final SliderSetting smartX = this.add(
      new SliderSetting(
         "SmartXZ", 3.0, 0.0, 10.0, 0.1, () -> this.lagMode.getValue() == Burrow.LagBackMode.Smart || this.aboveLagMode.getValue() == Burrow.LagBackMode.Smart
      )
   );
   private final SliderSetting smartUp = this.add(
      new SliderSetting(
         "SmartUp", 3.0, 0.0, 10.0, 0.1, () -> this.lagMode.getValue() == Burrow.LagBackMode.Smart || this.aboveLagMode.getValue() == Burrow.LagBackMode.Smart
      )
   );
   private final SliderSetting smartDown = this.add(
      new SliderSetting(
         "SmartDown",
         3.0,
         0.0,
         10.0,
         0.1,
         () -> this.lagMode.getValue() == Burrow.LagBackMode.Smart || this.aboveLagMode.getValue() == Burrow.LagBackMode.Smart
      )
   );
   private final SliderSetting smartDistance = this.add(
      new SliderSetting(
         "SmartDistance",
         2.0,
         0.0,
         10.0,
         0.1,
         () -> this.lagMode.getValue() == Burrow.LagBackMode.Smart || this.aboveLagMode.getValue() == Burrow.LagBackMode.Smart
      )
   );
   private int progress = 0;
   private Vec3d currentPos;

   public Burrow() {
      super("Burrow", Module.Category.Combat);
      this.setChinese("卡黑曜石");
      INSTANCE = this;
   }

   public void onUpdate(ClientTickEvent event) {
      if (!this.inventory.getValue() || EntityUtil.inInventory()) {
         if (event.isPost()) {
            if (!this.disable.getValue() && this.jumpDisable.getValue() && mc.player.input.jumping) {
               this.disable();
               return;
            }

            if (Alien.PLAYER.isInWeb(mc.player)) {
               this.webTimer.reset();
               return;
            }

            if (this.usingPause.getValue() && mc.player.isUsingItem()) {
               return;
            }

            if (!this.webTimer.passedMs(this.webTime.getValue())) {
               return;
            }

            if (!this.disable.getValue() && !this.timer.passedMs(this.delay.getValue())) {
               return;
            }

            if (!mc.player.isOnGround()) {
               return;
            }

            if (this.antiLag.getValue() && !BlockUtil.canCollide(mc.player, new Box(EntityUtil.getPlayerPos(true).down()))) {
               return;
            }

            if (this.single.getValue() && EntityUtil.isInsideBlock()) {
               if (this.disable.getValue()) {
                  this.disable();
               }

               return;
            }

            if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
               return;
            }

            int oldSlot = mc.player.getInventory().selectedSlot;
            int block = this.getBlock();
            if (block == -1) {
               CommandManager.sendMessageId("§4No block found.", this.hashCode() - 1);
               this.disable();
               return;
            }

            this.progress = 0;
            this.placePos.clear();
            double offset = this.single.getValue() ? 0.0 : AntiCheat.getOffset();
            BlockPosX pos1 = new BlockPosX(mc.player.getX() + offset, mc.player.getY() + 0.5, mc.player.getZ() + offset);
            BlockPosX pos2 = new BlockPosX(mc.player.getX() - offset, mc.player.getY() + 0.5, mc.player.getZ() + offset);
            BlockPosX pos3 = new BlockPosX(mc.player.getX() + offset, mc.player.getY() + 0.5, mc.player.getZ() - offset);
            BlockPosX pos4 = new BlockPosX(mc.player.getX() - offset, mc.player.getY() + 0.5, mc.player.getZ() - offset);
            BlockPosX pos5 = new BlockPosX(mc.player.getX() + offset, mc.player.getY() + 1.5, mc.player.getZ() + offset);
            BlockPosX pos6 = new BlockPosX(mc.player.getX() - offset, mc.player.getY() + 1.5, mc.player.getZ() + offset);
            BlockPosX pos7 = new BlockPosX(mc.player.getX() + offset, mc.player.getY() + 1.5, mc.player.getZ() - offset);
            BlockPosX pos8 = new BlockPosX(mc.player.getX() - offset, mc.player.getY() + 1.5, mc.player.getZ() - offset);
            BlockPosX pos9 = new BlockPosX(mc.player.getX() + offset, mc.player.getY() - 1.0, mc.player.getZ() + offset);
            BlockPosX pos10 = new BlockPosX(mc.player.getX() - offset, mc.player.getY() - 1.0, mc.player.getZ() + offset);
            BlockPosX pos11 = new BlockPosX(mc.player.getX() + offset, mc.player.getY() - 1.0, mc.player.getZ() - offset);
            BlockPosX pos12 = new BlockPosX(mc.player.getX() - offset, mc.player.getY() - 1.0, mc.player.getZ() - offset);
            BlockPos playerPos = EntityUtil.getPlayerPos(true);
            boolean headFill = false;
            if (!this.canPlace(pos1) && !this.canPlace(pos2) && !this.canPlace(pos3) && !this.canPlace(pos4)) {
               boolean cantHeadFill = !this.headFill.getValue() || !this.canPlace(pos5) && !this.canPlace(pos6) && !this.canPlace(pos7) && !this.canPlace(pos8);
               boolean cantDown = !this.down.getValue() || !this.canPlace(pos9) && !this.canPlace(pos10) && !this.canPlace(pos11) && !this.canPlace(pos12);
               if (cantHeadFill) {
                  if (cantDown) {
                     if (!this.wait.getValue() && this.disable.getValue()) {
                        this.disable();
                     }

                     return;
                  }
               } else {
                  headFill = true;
               }
            }

            boolean above = false;
            BlockPos headPos = EntityUtil.getPlayerPos(true).up(2);
            boolean rotate = this.rotate.getValue() == Burrow.RotateMode.Normal;
            CombatUtil.attackCrystal(pos1, rotate, false);
            CombatUtil.attackCrystal(pos2, rotate, false);
            CombatUtil.attackCrystal(pos3, rotate, false);
            CombatUtil.attackCrystal(pos4, rotate, false);
            if (!headFill
               && !mc.player.isCrawling()
               && !this.trapped(headPos)
               && !this.trapped(headPos.add(1, 0, 0))
               && !this.trapped(headPos.add(-1, 0, 0))
               && !this.trapped(headPos.add(0, 0, 1))
               && !this.trapped(headPos.add(0, 0, -1))
               && !this.trapped(headPos.add(1, 0, -1))
               && !this.trapped(headPos.add(-1, 0, -1))
               && !this.trapped(headPos.add(1, 0, 1))
               && !this.trapped(headPos.add(-1, 0, 1))) {
               mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.4199999868869781, mc.player.getZ(), false));
               mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.7531999805212017, mc.player.getZ(), false));
               mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.9999957640154541, mc.player.getZ(), false));
               mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.1661092609382138, mc.player.getZ(), false));
               this.currentPos = new Vec3d(mc.player.getX(), mc.player.getY() + 1.1661092609382138, mc.player.getZ());
            } else {
               above = true;
               if (!this.fakeMove.getValue()) {
                  if (!this.wait.getValue() && this.disable.getValue()) {
                     this.disable();
                  }

                  return;
               }

               if (!this.fakeMove()) {
                  return;
               }
            }

            this.timer.reset();
            this.doSwap(block);
            if (this.rotate.getValue() == Burrow.RotateMode.Bypass) {
               if (above) {
                  float[] angle = RotationManager.getRotation(
                     this.currentPos.add(0.0, (double)mc.player.getEyeHeight(mc.player.getPose()), 0.0), mc.player.getPos()
                  );
                  Alien.ROTATION.snapAt(angle[0], angle[1]);
               } else {
                  Alien.ROTATION.snapAt(Alien.ROTATION.rotationYaw, 90.0F);
               }
            }

            this.placeBlock(playerPos, rotate);
            this.placeBlock(pos1, rotate);
            this.placeBlock(pos2, rotate);
            this.placeBlock(pos3, rotate);
            this.placeBlock(pos4, rotate);
            if (this.down.getValue()) {
               this.placeBlock(pos9, rotate);
               this.placeBlock(pos10, rotate);
               this.placeBlock(pos11, rotate);
               this.placeBlock(pos12, rotate);
            }

            if (this.headFill.getValue() && above) {
               this.placeBlock(pos5, rotate);
               this.placeBlock(pos6, rotate);
               this.placeBlock(pos7, rotate);
               this.placeBlock(pos8, rotate);
            }

            if (this.inventory.getValue()) {
               this.doSwap(block);
               EntityUtil.syncInventory();
            } else {
               this.doSwap(oldSlot);
            }

            switch (above ? (Burrow.LagBackMode)this.aboveLagMode.getValue() : (Burrow.LagBackMode)this.lagMode.getValue()) {
               case Smart:
                  ArrayList<BlockPosX> list = new ArrayList<>();

                  for (double x = mc.player.getPos().getX() - this.smartX.getValue(); x < mc.player.getPos().getX() + this.smartX.getValue(); x++) {
                     for (double z = mc.player.getPos().getZ() - this.smartX.getValue(); z < mc.player.getPos().getZ() + this.smartX.getValue(); z++) {
                        for (double d = mc.player.getPos().getY() - this.smartDown.getValue(); d < mc.player.getPos().getY() + this.smartUp.getValue(); d++) {
                           list.add(new BlockPosX(x, d, z));
                        }
                     }
                  }

                  double getDistance = 0.0;
                  BlockPos bestPos = null;

                  for (BlockPos blockPos : list) {
                     if (this.canMove(blockPos)
                        && !(
                           (double)MathHelper.sqrt((float)mc.player.squaredDistanceTo(blockPos.toCenterPos().add(0.0, -0.5, 0.0)))
                              < this.smartDistance.getValue()
                        )
                        && (bestPos == null || mc.player.squaredDistanceTo(blockPos.toCenterPos()) < getDistance)) {
                        bestPos = blockPos;
                        getDistance = mc.player.squaredDistanceTo(blockPos.toCenterPos());
                     }
                  }

                  if (bestPos != null) {
                     mc.getNetworkHandler()
                        .sendPacket(new PositionAndOnGround((double)bestPos.getX() + 0.5, (double)bestPos.getY(), (double)bestPos.getZ() + 0.5, false));
                  }
                  break;
               case Invalid:
                  for (int i = 0; i < 20; i++) {
                     mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1337.0, mc.player.getZ(), false));
                  }
                  break;
               case TrollHack:
                  mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 2.3400880035762786, mc.player.getZ(), false));
                  break;
               case ToVoid:
                  mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), -70.0, mc.player.getZ(), false));
                  break;
               case ToVoid2:
                  mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), -7.0, mc.player.getZ(), false));
                  break;
               case Normal:
                  mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.9, mc.player.getZ(), false));
                  break;
               case Rotation:
                  mc.getNetworkHandler().sendPacket(new LookAndOnGround(-180.0F, -90.0F, false));
                  mc.getNetworkHandler().sendPacket(new LookAndOnGround(180.0F, 90.0F, false));
                  break;
               case Fly:
                  mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.16610926093821, mc.player.getZ(), false));
                  mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.170005801788139, mc.player.getZ(), false));
                  mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.2426308013947485, mc.player.getZ(), false));
                  mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 2.3400880035762786, mc.player.getZ(), false));
                  mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 2.640088003576279, mc.player.getZ(), false));
                  break;
               case Glide:
                  mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.1001, mc.player.getZ(), false));
                  mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.0605, mc.player.getZ(), false));
                  mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.0802, mc.player.getZ(), false));
                  mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.1127, mc.player.getZ(), false));
            }

            if (this.disable.getValue()) {
               this.disable();
            }
         }
      }
   }

   private boolean fakeMove() {
      double[] offsets = new double[]{1.0, 0.0, -1.0};
      ArrayList<BlockPosX> offList = new ArrayList<>();
      BlockPos playerPos = mc.player.getBlockPos();

      for (double x : offsets) {
         for (double z : offsets) {
            offList.add(new BlockPosX(mc.player.getX() + x, mc.player.getY(), mc.player.getZ() + z));
         }
      }

      for (BlockPos offPos : offList) {
         if (this.checkSelf(offPos) && !BlockUtil.canReplace(offPos) && (!this.headFill.getValue() || !BlockUtil.canReplace(offPos.up()))) {
            this.gotoPos(offPos);
            return true;
         }
      }

      ArrayList<BlockPos> pos = new ArrayList<>();

      for (BlockPos blockPos : offList) {
         if (!playerPos.equals(blockPos) && this.checkSelf(blockPos) && this.canMove(blockPos)) {
            pos.add(blockPos);
         }
      }

      if (!pos.isEmpty()) {
         double dis = 10.0;
         BlockPos target = null;

         for (BlockPos p : pos) {
            double getDistance = mc.player.getPos().distanceTo(p.toCenterPos().add(0.0, -0.5, 0.0));
            if (getDistance < dis || target == null) {
               target = p;
               dis = getDistance;
            }
         }

         this.gotoPos(target);
         return true;
      } else {
         for (BlockPos blockPosx : offList) {
            if (!playerPos.equals(blockPosx) && this.checkSelf(blockPosx)) {
               pos.add(blockPosx);
            }
         }

         if (!pos.isEmpty()) {
            double dis = 10.0;
            BlockPos target = null;

            for (BlockPos px : pos) {
               double getDistance = mc.player.getPos().distanceTo(px.toCenterPos().add(0.0, -0.5, 0.0));
               if (getDistance < dis || target == null) {
                  target = px;
                  dis = getDistance;
               }
            }

            this.gotoPos(target);
            return true;
         } else if (!this.center.getValue()) {
            return false;
         } else {
            offList.clear();
            offList.add(new BlockPosX(mc.player.getX() + 1.0, mc.player.getY(), mc.player.getZ()));
            offList.add(new BlockPosX(mc.player.getX() - 1.0, mc.player.getY(), mc.player.getZ()));
            offList.add(new BlockPosX(mc.player.getX(), mc.player.getY(), mc.player.getZ() - 1.0));
            offList.add(new BlockPosX(mc.player.getX(), mc.player.getY(), mc.player.getZ() + 1.0));

            for (BlockPos blockPosxx : offList) {
               if (this.canMove(blockPosxx)) {
                  this.gotoPos(blockPosxx);
                  return true;
               }
            }

            if (!this.wait.getValue() && this.disable.getValue()) {
               this.disable();
            }

            return false;
         }
      }
   }

   private void placeBlock(BlockPos pos, boolean rotate) {
      if (this.canPlace(pos) && !this.placePos.contains(pos) && this.progress < this.blocksPer.getValueInt()) {
         this.placePos.add(pos);
         if (BlockUtil.allowAirPlace()) {
            this.progress++;
            BlockUtil.placedPos.add(pos);
            if (this.sound.getValue()) {
               mc.world.playSound(mc.player, pos, SoundEvents.BLOCK_STONE_PLACE, SoundCategory.BLOCKS, 1.0F, 0.8F);
            }

            this.clickBlock(pos, Direction.DOWN, rotate, Hand.MAIN_HAND, this.packetPlace.getValue());
         }

         Direction side;
         if ((side = BlockUtil.getPlaceSide(pos)) == null) {
            return;
         }

         this.progress++;
         BlockUtil.placedPos.add(pos);
         if (this.sound.getValue()) {
            mc.world.playSound(mc.player, pos, SoundEvents.BLOCK_STONE_PLACE, SoundCategory.BLOCKS, 1.0F, 0.8F);
         }

         this.clickBlock(pos.offset(side), side.getOpposite(), rotate, Hand.MAIN_HAND, this.packetPlace.getValue());
      }
   }

   public void clickBlock(BlockPos pos, Direction side, boolean rotate, Hand hand, boolean packet) {
      Vec3d directionVec = new Vec3d(
         (double)pos.getX() + 0.5 + (double)side.getVector().getX() * 0.5,
         (double)pos.getY() + 0.5 + (double)side.getVector().getY() * 0.5,
         (double)pos.getZ() + 0.5 + (double)side.getVector().getZ() * 0.5
      );
      if (rotate) {
         float[] angle = RotationManager.getRotation(this.currentPos.add(0.0, (double)mc.player.getEyeHeight(mc.player.getPose()), 0.0), directionVec);
         Alien.ROTATION.snapAt(angle[0], angle[1]);
      }

      EntityUtil.swingHand(hand, AntiCheat.INSTANCE.interactSwing.getValue());
      BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
      if (packet) {
         Module.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(hand, result, id));
      } else {
         mc.interactionManager.interactBlock(mc.player, hand, result);
      }

      if (rotate) {
         Alien.ROTATION.snapBack();
      }
   }

   private void doSwap(int slot) {
      if (this.inventory.getValue()) {
         Alien.INVENTORY.inventorySwap(slot, mc.player.getInventory().selectedSlot);
      } else {
         Alien.INVENTORY.setSlot(slot);
      }
   }

   private void gotoPos(BlockPos offPos) {
      double targetX = (double)offPos.getX() + 0.5;
      double targetZ = (double)offPos.getZ() + 0.5;
      double x = mc.player.getX();
      double z = mc.player.getZ();
      double y = mc.player.getY() + this.yOffset.getValue();
      double xDiff = Math.abs(x - targetX);
      double zDiff = Math.abs(z - targetZ);
      double moveDis = this.preCorrect.getValue();
      if (moveDis > 0.0) {
         if (xDiff >= moveDis) {
            double var18;
            double var19;
            x = x > targetX ? (var18 = x - moveDis) : (var19 = x + moveDis);
            mc.player.networkHandler.sendPacket(new PositionAndOnGround(x, y, z, false));
         }

         if (zDiff >= moveDis) {
            double var26;
            double var27;
            z = z > targetZ ? (var26 = z - moveDis) : (var27 = z + moveDis);
            mc.player.networkHandler.sendPacket(new PositionAndOnGround(x, y, z, false));
         }
      }

      xDiff = Math.abs(x - targetX);
      zDiff = Math.abs(z - targetZ);
      moveDis = this.moveDis.getValue();
      if (moveDis > 0.0) {
         while (xDiff > moveDis) {
            double var20;
            double var21;
            x = x > targetX ? (var20 = x - moveDis) : (var21 = x + moveDis);
            mc.player.networkHandler.sendPacket(new PositionAndOnGround(x, y, z, false));
            xDiff = Math.abs(x - targetX);
         }

         while (zDiff > moveDis) {
            double var28;
            double var29;
            z = z > targetZ ? (var28 = z - moveDis) : (var29 = z + moveDis);
            mc.player.networkHandler.sendPacket(new PositionAndOnGround(x, y, z, false));
            zDiff = Math.abs(z - targetZ);
         }
      }

      if ((moveDis = this.moveDis2.getValue()) > 0.0) {
         while (xDiff > moveDis) {
            double var22;
            double var23;
            x = x > targetX ? (var22 = x - moveDis) : (var23 = x + moveDis);
            mc.player.networkHandler.sendPacket(new PositionAndOnGround(x, y, z, false));
            xDiff = Math.abs(x - targetX);
         }

         while (zDiff > moveDis) {
            double var30;
            double var31;
            z = z > targetZ ? (var30 = z - moveDis) : (var31 = z + moveDis);
            mc.player.networkHandler.sendPacket(new PositionAndOnGround(x, y, z, false));
            zDiff = Math.abs(z - targetZ);
         }
      }

      if ((moveDis = this.moveCorrect2.getValue()) > 0.0) {
         if (xDiff >= moveDis) {
            double var24;
            double var25;
            x = x > targetX ? (var24 = x - moveDis) : (var25 = x + moveDis);
            mc.player.networkHandler.sendPacket(new PositionAndOnGround(x, y, z, false));
         }

         if (zDiff >= moveDis) {
            double var32;
            double var33;
            z = z > targetZ ? (var32 = z - moveDis) : (var33 = z + moveDis);
            mc.player.networkHandler.sendPacket(new PositionAndOnGround(x, y, z, false));
         }
      }

      this.currentPos = new Vec3d(x, y, z);
   }

   private boolean canMove(BlockPos pos) {
      return mc.world.isAir(pos) && mc.world.isAir(pos.up());
   }

   private boolean canPlace(BlockPos pos) {
      if (this.noSelfPos.getValue() && pos.equals(EntityUtil.getPlayerPos(true))) {
         return false;
      } else if (!BlockUtil.allowAirPlace() && BlockUtil.getPlaceSide(pos) == null) {
         return false;
      } else if (!BlockUtil.canReplace(pos)) {
         return false;
      } else {
         return this.detectMine.getValue() && Alien.BREAK.isMining(pos) ? false : !this.hasEntity(pos);
      }
   }

   private boolean hasEntity(BlockPos pos) {
      for (Entity entity : BlockUtil.getEntities(new Box(pos))) {
         if (entity != mc.player
            && entity.isAlive()
            && !(entity instanceof ItemEntity)
            && !(entity instanceof ExperienceOrbEntity)
            && !(entity instanceof ExperienceBottleEntity)
            && !(entity instanceof ArrowEntity)
            && (!(entity instanceof EndCrystalEntity) || !this.breakCrystal.getValue())) {
            return true;
         }
      }

      return false;
   }

   private boolean checkSelf(BlockPos pos) {
      return mc.player.getBoundingBox().intersects(new Box(pos));
   }

   private boolean trapped(BlockPos pos) {
      return (BlockUtil.canCollide(mc.player, new Box(pos)) || BlockUtil.getBlock(pos) == Blocks.COBWEB) && this.checkSelf(pos.down(2));
   }

   private int getBlock() {
      if (this.inventory.getValue()) {
         return InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN) == -1 && this.enderChest.getValue()
            ? InventoryUtil.findBlockInventorySlot(Blocks.ENDER_CHEST)
            : InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN);
      } else {
         return InventoryUtil.findBlock(Blocks.OBSIDIAN) == -1 && this.enderChest.getValue()
            ? InventoryUtil.findBlock(Blocks.ENDER_CHEST)
            : InventoryUtil.findBlock(Blocks.OBSIDIAN);
      }
   }

   private static enum LagBackMode {
      Smart,
      Invalid,
      TrollHack,
      ToVoid,
      ToVoid2,
      Normal,
      Rotation,
      Fly,
      Glide;
   }

   private static enum RotateMode {
      Bypass,
      Normal,
      None;
   }
}
