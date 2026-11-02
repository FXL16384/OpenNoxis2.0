package noxisclient.mod.modules.impl.combat;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FacingBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import noxisclient.Alien;
import noxisclient.api.events.impl.UpdateEvent;
import noxisclient.api.utils.combat.CombatUtil;
import noxisclient.api.utils.math.MathUtil;
import noxisclient.api.utils.math.Timer;
import noxisclient.api.utils.player.EntityUtil;
import noxisclient.api.utils.player.InventoryUtil;
import noxisclient.api.utils.world.BlockPosX;
import noxisclient.api.utils.world.BlockUtil;
import noxisclient.core.impl.RotationManager;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.client.ClientSetting;
import noxisclient.mod.modules.impl.exploit.Blink;
import noxisclient.mod.modules.impl.player.PacketMine;
import noxisclient.mod.modules.settings.impl.BindSetting;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class AutoPush extends Module {
   public static AutoPush INSTANCE;
   private final BooleanSetting torch = this.add(new BooleanSetting("Torch", false));
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotation", true));
   private final BooleanSetting yawDeceive = this.add(new BooleanSetting("YawDeceive", true));
   private final BooleanSetting pistonPacket = this.add(new BooleanSetting("PistonPacket", false));
   private final BooleanSetting powerPacket = this.add(new BooleanSetting("PowerPacket", true));
   private final BooleanSetting noEating = this.add(new BooleanSetting("EatingPause", true));
   private final BooleanSetting mine = this.add(new BooleanSetting("Mine", true));
   private final BooleanSetting allowWeb = this.add(new BooleanSetting("AllowWeb", true));
   private final SliderSetting updateDelay = this.add(new SliderSetting("Delay", 100, 0, 1000));
   private final BooleanSetting selfGround = this.add(new BooleanSetting("SelfGround", true));
   private final BooleanSetting onlyGround = this.add(new BooleanSetting("OnlyGround", false));
   private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true));
   private final SliderSetting range = this.add(new SliderSetting("Range", 5.0, 0.0, 6.0));
   private final SliderSetting placeRange = this.add(new SliderSetting("PlaceRange", 5.0, 0.0, 6.0));
   private final SliderSetting surroundCheck = this.add(new SliderSetting("SurroundCheck", 2, 0, 4));
   private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
   private final BindSetting selfPush = this.add(new BindSetting("SelfPush", -1));
   private boolean wasSelfPushPressed = false;
   private final Timer timer = new Timer();

   public AutoPush() {
      super("AutoPush", Module.Category.Combat);
      this.setChinese("活塞推人");
      INSTANCE = this;
   }

   public static void pistonFacing(Direction i) {
      if (i == Direction.EAST) {
         Alien.ROTATION.snapAt(-90.0F, 5.0F);
      } else if (i == Direction.WEST) {
         Alien.ROTATION.snapAt(90.0F, 5.0F);
      } else if (i == Direction.NORTH) {
         Alien.ROTATION.snapAt(180.0F, 5.0F);
      } else if (i == Direction.SOUTH) {
         Alien.ROTATION.snapAt(0.0F, 5.0F);
      }
   }

   @Override
   public void onEnable() {
      AutoCrystal.INSTANCE.lastBreakTimer.reset();
   }

   boolean isTargetHere(BlockPos pos, Entity target) {
      return new Box(pos).intersects(target.getBoundingBox());
   }

   public void onUpdate(UpdateEvent event) {
      boolean selfPressed = this.selfPush.isPressed();
      if (selfPressed && !this.wasSelfPushPressed) {
         this.doSelfPush();
         this.wasSelfPushPressed = true;
      } else {
         this.wasSelfPushPressed = selfPressed;
         if (!selfPressed) {
            if (this.timer.passedMs(this.updateDelay.getValue())) {
               if (!this.selfGround.getValue() || mc.player.isOnGround()) {
                  if (this.findBlock(this.getBlockType()) != -1 && this.findClass(PistonBlock.class) != -1) {
                     if (!this.noEating.getValue() || !mc.player.isUsingItem()) {
                        if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
                           for (PlayerEntity player : CombatUtil.getEnemies(this.range.getValue())) {
                              if (this.canPush(player)) {
                                 float[] offset;
                                 for (float x : offset = new float[]{-0.25F, 0.0F, 0.25F}) {
                                    for (float z : offset) {
                                       BlockPosX playerPos = new BlockPosX(player.getX() + (double)x, player.getY() + 0.5, player.getZ() + (double)z);

                                       for (Direction i : Direction.values()) {
                                          BlockPos pos;
                                          if (i != Direction.UP
                                             && i != Direction.DOWN
                                             && this.isTargetHere(pos = playerPos.offset(i), player)
                                             && mc.world.canCollide(player, new Box(pos))) {
                                             if (this.tryPush(playerPos.offset(i.getOpposite()), i)) {
                                                this.timer.reset();
                                                return;
                                             }

                                             if (this.tryPush(playerPos.offset(i.getOpposite()).up(), i)) {
                                                this.timer.reset();
                                                return;
                                             }
                                          }
                                       }
                                    }
                                 }

                                 if (!mc.world.canCollide(player, new Box(new BlockPosX(player.getX(), player.getY() + 2.5, player.getZ())))) {
                                    for (Direction ix : Direction.values()) {
                                       if (ix != Direction.UP && ix != Direction.DOWN) {
                                          BlockPos pos2 = EntityUtil.getEntityPos(player).offset(ix);
                                          Box box = player.getBoundingBox()
                                             .offset(new Vec3d((double)ix.getOffsetX(), (double)ix.getOffsetY(), (double)ix.getOffsetZ()));
                                          if (this.getBlock(pos2.up()) != Blocks.PISTON_HEAD
                                             && !mc.world.canCollide(player, box.offset(0.0, 1.0, 0.0))
                                             && !this.isTargetHere(pos2, player)) {
                                             if (this.tryPush(EntityUtil.getEntityPos(player).offset(ix.getOpposite()).up(), ix)) {
                                                this.timer.reset();
                                                return;
                                             }

                                             if (this.tryPush(EntityUtil.getEntityPos(player).offset(ix.getOpposite()), ix)) {
                                                this.timer.reset();
                                                return;
                                             }
                                          }
                                       }
                                    }
                                 }

                                 for (float x : offset) {
                                    for (float z : offset) {
                                       BlockPosX playerPos = new BlockPosX(player.getX() + (double)x, player.getY() + 0.5, player.getZ() + (double)z);

                                       for (Direction ixx : Direction.values()) {
                                          if (ixx != Direction.UP && ixx != Direction.DOWN && this.isTargetHere(playerPos.offset(ixx), player)) {
                                             if (this.tryPush(playerPos.offset(ixx.getOpposite()).up(), ixx)) {
                                                this.timer.reset();
                                                return;
                                             }

                                             if (this.tryPush(playerPos.offset(ixx.getOpposite()), ixx)) {
                                                this.timer.reset();
                                                return;
                                             }
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  } else {
                     if (this.autoDisable.getValue()) {
                        this.disable();
                     }
                  }
               }
            }
         }
      }
   }

   private int findPiston() {
      if (this.inventory.getValue()) {
         for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem && ((BlockItem)stack.getItem()).getBlock() instanceof PistonBlock) {
               return i < 9 ? i + 36 : i;
            }
         }
      } else {
         for (int ix = 0; ix < 9; ix++) {
            ItemStack stack = mc.player.getInventory().getStack(ix);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem && ((BlockItem)stack.getItem()).getBlock() instanceof PistonBlock) {
               return ix;
            }
         }
      }

      return -1;
   }

   private int findRedstone() {
      if (this.inventory.getValue()) {
         for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem && ((BlockItem)stack.getItem()).getBlock() == Blocks.REDSTONE_BLOCK) {
               return i < 9 ? i + 36 : i;
            }
         }
      } else {
         for (int ix = 0; ix < 9; ix++) {
            ItemStack stack = mc.player.getInventory().getStack(ix);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem && ((BlockItem)stack.getItem()).getBlock() == Blocks.REDSTONE_BLOCK) {
               return ix;
            }
         }
      }

      return -1;
   }

   private void doSelfPush() {
      PlayerEntity player = mc.player;
      BlockPos playerPos = EntityUtil.getEntityPos(player);
      int pistonSlot = this.findPiston();
      int powerSlot = this.findRedstone();
      if (pistonSlot != -1 && powerSlot != -1) {
         Direction facing = MathUtil.getFacingOrder(mc.gameRenderer.getCamera().getYaw(), 0.0F);
         BlockPos pistonPos = playerPos.offset(facing.getOpposite());
         if (BlockUtil.canPlace(pistonPos)) {
            Direction side = BlockUtil.getPlaceSide(pistonPos);
            if (side != null) {
               if (this.rotate.getValue()) {
                  Alien.ROTATION.lookAt(pistonPos.offset(side), side.getOpposite());
               }

               if (this.yawDeceive.getValue()) {
                  pistonFacing(facing.getOpposite());
               }

               int old = mc.player.getInventory().selectedSlot;
               this.doSwap(pistonSlot);
               BlockUtil.placeBlock(pistonPos, false, this.pistonPacket.getValue());
               if (this.inventory.getValue()) {
                  this.doSwap(pistonSlot);
                  EntityUtil.syncInventory();
               } else {
                  this.doSwap(old);
               }

               boolean powered = false;
               Direction bestPowerDir = BlockUtil.getBestNeighboring(pistonPos, facing);
               if (bestPowerDir != null && !pistonPos.offset(bestPowerDir).equals(playerPos) && BlockUtil.canPlace(pistonPos.offset(bestPowerDir))) {
                  powered = this.placePowerBlock(pistonPos.offset(bestPowerDir), powerSlot, true);
               }

               if (!powered) {
                  for (Direction powerDir : Direction.values()) {
                     BlockPos powerPos = pistonPos.offset(powerDir);
                     if (!powerPos.equals(playerPos)
                        && BlockUtil.canPlace(powerPos)
                        && BlockUtil.getPlaceSide(powerPos) != null
                        && this.placePowerBlock(powerPos, powerSlot, true)) {
                        powered = true;
                        break;
                     }
                  }
               }

               if (this.rotate.getValue() && this.yawDeceive.getValue()) {
                  Alien.ROTATION.lookAt(pistonPos.offset(side), side.getOpposite());
               }

               if (this.rotate.getValue()) {
                  Alien.ROTATION.snapBack();
               }

               if (powered) {
                  this.timer.reset();
               }
            }
         }
      }
   }

   private boolean tryPush(BlockPos piston, Direction direction) {
      if (!mc.world.isAir(piston.offset(direction))) {
         return false;
      } else {
         if (this.isTrueFacing(piston, direction) && this.facingCheck(piston) && BlockUtil.clientCanPlace(piston, false)) {
            boolean canPower = false;
            if (BlockUtil.getPlaceSide(piston, this.placeRange.getValue()) != null) {
               CombatUtil.modifyPos = piston;
               CombatUtil.modifyBlockState = Blocks.PISTON.getDefaultState();

               for (Direction direction2 : Direction.values()) {
                  if (this.getBlock(piston.offset(direction2)) == this.getBlockType()) {
                     canPower = true;
                     break;
                  }
               }

               for (Direction direction3 : Direction.values()) {
                  if (canPower) {
                     break;
                  }

                  if (BlockUtil.canPlace(piston.offset(direction3), this.placeRange.getValue())) {
                     canPower = true;
                  }
               }

               CombatUtil.modifyPos = null;
               if (canPower) {
                  int pistonSlot = this.findClass(PistonBlock.class);
                  Direction side = BlockUtil.getPlaceSide(piston);
                  if (side != null) {
                     if (this.rotate.getValue()) {
                        Alien.ROTATION.lookAt(piston.offset(side), side.getOpposite());
                     }

                     if (this.yawDeceive.getValue()) {
                        pistonFacing(direction.getOpposite());
                     }

                     int old = mc.player.getInventory().selectedSlot;
                     this.doSwap(pistonSlot);
                     BlockUtil.placeBlock(piston, false, this.pistonPacket.getValue());
                     if (this.inventory.getValue()) {
                        this.doSwap(pistonSlot);
                        EntityUtil.syncInventory();
                     } else {
                        this.doSwap(old);
                     }

                     if (this.rotate.getValue() && this.yawDeceive.getValue()) {
                        Alien.ROTATION.lookAt(piston.offset(side), side.getOpposite());
                     }

                     if (this.rotate.getValue()) {
                        Alien.ROTATION.snapBack();
                     }

                     for (Direction i : Direction.values()) {
                        if (this.getBlock(piston.offset(i)) == this.getBlockType()) {
                           if (this.mine.getValue()) {
                              PacketMine.INSTANCE.mine(piston.offset(i));
                           }

                           if (this.autoDisable.getValue()) {
                              this.disable();
                           }

                           return true;
                        }
                     }

                     for (Direction ix : Direction.values()) {
                        if ((ix != Direction.UP || !this.torch.getValue()) && BlockUtil.canPlace(piston.offset(ix), this.placeRange.getValue())) {
                           int powerSlot = this.findBlock(this.getBlockType());
                           if (this.placePowerBlock(piston.offset(ix), powerSlot, this.rotate.getValue())) {
                              if (this.mine.getValue()) {
                                 PacketMine.INSTANCE.mine(piston.offset(ix));
                              }

                              return true;
                           }
                        }
                     }

                     return true;
                  }
               }
            } else {
               Direction powerFacing = null;

               for (Direction ixx : Direction.values()) {
                  if (ixx != Direction.UP || !this.torch.getValue()) {
                     if (powerFacing != null) {
                        break;
                     }

                     CombatUtil.modifyPos = piston.offset(ixx);
                     CombatUtil.modifyBlockState = this.getBlockType().getDefaultState();
                     if (BlockUtil.getPlaceSide(piston) != null) {
                        powerFacing = ixx;
                     }

                     CombatUtil.modifyPos = null;
                     if (powerFacing != null && !BlockUtil.canPlace(piston.offset(powerFacing))) {
                        powerFacing = null;
                     }
                  }
               }

               if (powerFacing != null) {
                  int powerSlot = this.findBlock(this.getBlockType());
                  if (!this.placePowerBlock(piston.offset(powerFacing), powerSlot, this.rotate.getValue())) {
                     CombatUtil.modifyPos = null;
                     return false;
                  }

                  CombatUtil.modifyPos = piston.offset(powerFacing);
                  CombatUtil.modifyBlockState = this.getBlockType().getDefaultState();
                  int n = this.findClass(PistonBlock.class);
                  Direction side2 = BlockUtil.getPlaceSide(piston);
                  if (side2 != null) {
                     if (this.rotate.getValue()) {
                        Alien.ROTATION.lookAt(piston.offset(side2), side2.getOpposite());
                     }

                     if (this.yawDeceive.getValue()) {
                        pistonFacing(direction.getOpposite());
                     }

                     int oldx = mc.player.getInventory().selectedSlot;
                     this.doSwap(n);
                     BlockUtil.placeBlock(piston, false, this.pistonPacket.getValue());
                     if (this.inventory.getValue()) {
                        this.doSwap(n);
                        EntityUtil.syncInventory();
                     } else {
                        this.doSwap(oldx);
                     }

                     if (this.rotate.getValue() && this.yawDeceive.getValue()) {
                        Alien.ROTATION.lookAt(piston.offset(side2), side2.getOpposite());
                     }

                     if (this.rotate.getValue()) {
                        Alien.ROTATION.snapBack();
                     }
                  }

                  CombatUtil.modifyPos = null;
                  return true;
               }
            }
         }

         if (mc.world.getBlockState(piston).getBlock() instanceof PistonBlock && this.getBlockState(piston).get(FacingBlock.FACING) == direction) {
            for (Direction direction4 : Direction.values()) {
               if (this.getBlock(piston.offset(direction4)) == this.getBlockType()) {
                  if (this.autoDisable.getValue()) {
                     this.disable();
                     return true;
                  }

                  return false;
               }
            }

            for (Direction direction5 : Direction.values()) {
               if ((direction5 != Direction.UP || !this.torch.getValue()) && BlockUtil.canPlace(piston.offset(direction5), this.placeRange.getValue())) {
                  int powerSlotx = this.findBlock(this.getBlockType());
                  if (this.placePowerBlock(piston.offset(direction5), powerSlotx, this.rotate.getValue())) {
                     return true;
                  }
               }
            }
         }

         return false;
      }
   }

   private boolean facingCheck(BlockPos pos) {
      if (!ClientSetting.INSTANCE.lowVersion.getValue()) {
         return true;
      } else {
         Direction direction = MathUtil.getDirectionFromEntityLiving(pos, mc.player);
         return direction != Direction.UP && direction != Direction.DOWN;
      }
   }

   private boolean isTrueFacing(BlockPos pos, Direction facing) {
      if (this.yawDeceive.getValue()) {
         return true;
      } else {
         Direction side = BlockUtil.getPlaceSide(pos);
         if (side == null) {
            return false;
         } else {
            Vec3d directionVec = new Vec3d(
               (double)pos.getX() + 0.5 + (double)side.getVector().getX() * 0.5,
               (double)pos.getY() + 0.5 + (double)side.getVector().getY() * 0.5,
               (double)pos.getZ() + 0.5 + (double)side.getVector().getZ() * 0.5
            );
            float[] rotation = RotationManager.getRotation(directionVec);
            return MathUtil.getFacingOrder(rotation[0], rotation[1]).getOpposite() == facing;
         }
      }
   }

   private void doSwap(int slot) {
      if (this.inventory.getValue()) {
         Alien.INVENTORY.inventorySwap(slot, mc.player.getInventory().selectedSlot);
      } else {
         Alien.INVENTORY.setSlot(slot);
      }
   }

   private boolean placePowerBlock(BlockPos pos, int powerSlot, boolean rotate) {
      int oldSlot = mc.player.getInventory().selectedSlot;
      this.doSwap(powerSlot);
      BlockUtil.placeBlock(pos, rotate, this.powerPacket.getValue());
      if (this.inventory.getValue()) {
         this.doSwap(powerSlot);
         EntityUtil.syncInventory();
      } else {
         this.doSwap(oldSlot);
      }

      return this.powerPacket.getValue() || this.getBlock(pos) == this.getBlockType();
   }

   public int findBlock(Block blockIn) {
      return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(blockIn) : InventoryUtil.findBlock(blockIn);
   }

   public int findClass(Class<?> clazz) {
      return this.inventory.getValue() ? InventoryUtil.findClassInventorySlot(clazz) : InventoryUtil.findClass(clazz);
   }

   private Boolean canPush(PlayerEntity player) {
      if (this.onlyGround.getValue() && !player.isOnGround()) {
         return false;
      } else if (!this.allowWeb.getValue() && Alien.PLAYER.isInWeb(player)) {
         return false;
      } else {
         float[] offset = new float[]{-0.25F, 0.0F, 0.25F};
         int progress = 0;
         if (mc.world.canCollide(player, new Box(new BlockPosX(player.getX() + 1.0, player.getY() + 0.5, player.getZ())))) {
            progress++;
         }

         if (mc.world.canCollide(player, new Box(new BlockPosX(player.getX() - 1.0, player.getY() + 0.5, player.getZ())))) {
            progress++;
         }

         if (mc.world.canCollide(player, new Box(new BlockPosX(player.getX(), player.getY() + 0.5, player.getZ() + 1.0)))) {
            progress++;
         }

         if (mc.world.canCollide(player, new Box(new BlockPosX(player.getX(), player.getY() + 0.5, player.getZ() - 1.0)))) {
            progress++;
         }

         for (float x : offset) {
            for (float z : offset) {
               BlockPosX playerPos = new BlockPosX(player.getX() + (double)x, player.getY() + 0.5, player.getZ() + (double)z);

               for (Direction i : Direction.values()) {
                  BlockPos pos;
                  if (i != Direction.UP && i != Direction.DOWN && this.isTargetHere(pos = playerPos.offset(i), player)) {
                     if (mc.world.canCollide(player, new Box(pos))) {
                        return true;
                     }

                     if ((double)progress > this.surroundCheck.getValue() - 1.0) {
                        return true;
                     }
                  }
               }
            }
         }

         if (!mc.world.canCollide(player, new Box(new BlockPosX(player.getX(), player.getY() + 2.5, player.getZ())))) {
            for (Direction ix : Direction.values()) {
               if (ix != Direction.UP && ix != Direction.DOWN) {
                  BlockPos pos = EntityUtil.getEntityPos(player).offset(ix);
                  Box box = player.getBoundingBox().offset(new Vec3d((double)ix.getOffsetX(), (double)ix.getOffsetY(), (double)ix.getOffsetZ()));
                  if (this.getBlock(pos.up()) != Blocks.PISTON_HEAD
                     && !mc.world.canCollide(player, box.offset(0.0, 1.0, 0.0))
                     && !this.isTargetHere(pos, player)
                     && mc.world.canCollide(player, new Box(new BlockPosX(player.getX(), player.getY() + 0.5, player.getZ())))) {
                     return true;
                  }
               }
            }
         }

         return (double)progress > this.surroundCheck.getValue() - 1.0 || Alien.HOLE.isHard(new BlockPosX(player.getX(), player.getY() + 0.5, player.getZ()));
      }
   }

   private Block getBlock(BlockPos pos) {
      return mc.world.getBlockState(pos).getBlock();
   }

   private Block getBlockType() {
      return this.torch.getValue() ? Blocks.REDSTONE_TORCH : Blocks.REDSTONE_BLOCK;
   }

   private BlockState getBlockState(BlockPos pos) {
      return mc.world.getBlockState(pos);
   }
}
