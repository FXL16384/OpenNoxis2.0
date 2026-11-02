package noxisclient.mod.modules.impl.player;

import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import noxisclient.Alien;
import noxisclient.api.events.impl.RotationEvent;
import noxisclient.api.events.impl.TickEvent;
import noxisclient.api.utils.player.EntityUtil;
import noxisclient.api.utils.player.InventoryUtil;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.exploit.Blink;
import noxisclient.mod.modules.impl.movement.ElytraFly;
import noxisclient.mod.modules.impl.movement.Velocity;
import noxisclient.mod.modules.settings.enums.SwingSide;
import noxisclient.mod.modules.settings.enums.Timing;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class AutoPearl extends Module {
   public static AutoPearl INSTANCE;
   public static boolean throwing = false;
   public final BooleanSetting inventory = new BooleanSetting("InventorySwap", true);
   public final BooleanSetting OnlyFlying = new BooleanSetting("OnlyFlying", true);
   private final EnumSetting<Timing> timing = new EnumSetting<>("Timing", Timing.All);
   public final EnumSetting<SwingSide> interactSwing = new EnumSetting<>("Swing", SwingSide.All);
   private final BooleanSetting rotation = new BooleanSetting("Rotation", true);
   private final BooleanSetting yawStep = new BooleanSetting("YawStep", true);
   private final BooleanSetting whenElytra = new BooleanSetting("FallFlying", true);
   private final SliderSetting steps = new SliderSetting("Steps", 0.05, 0.0, 1.0, 0.01);
   private final SliderSetting fov = new SliderSetting("Fov", 20.0, 0.0, 360.0, 0.1);
   private final SliderSetting priority = new SliderSetting("Priority", 100, 0, 100);
   private final BooleanSetting sync = new BooleanSetting("Sync", true);
   private boolean active;

   public AutoPearl() {
      super("AutoPearl", Module.Category.Player);
      this.setChinese("扔珍珠");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.trigger();
      if (!this.active) {
         this.disable();
      }
   }

   @Override
   public boolean isOn() {
      return super.isOn() || this.active;
   }

   public void trigger() {
      this.active = false;
      if (!nullCheck()) {
         if (INSTANCE.inventory.getValue()) {
            if (InventoryUtil.findItemInventorySlotFromZero(Items.ENDER_PEARL) == -1) {
               return;
            }
         } else if (InventoryUtil.findItem(Items.ENDER_PEARL) == -1) {
            return;
         }

         if (!this.OnlyFlying.getValue() || !mc.player.isFallFlying()) {
            if (this.shouldYawStep()) {
               this.active = true;
            } else if (!this.inventory.getValue() || EntityUtil.inInventory()) {
               if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
                  this.throwPearl(mc.player.getYaw(), mc.player.getPitch());
               }
            }
         }
      }
   }

   public void onUpdate(TickEvent event) {
      if (this.active || this.state) {
         if (nullCheck()) {
            this.finish();
         } else if ((!this.timing.is(Timing.Pre) || !event.isPost()) && (!this.timing.is(Timing.Post) || !event.isPre())) {
            if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
               if (!this.shouldYawStep()) {
                  this.throwPearl(mc.player.getYaw(), mc.player.getPitch());
                  this.finish();
               } else if (Alien.ROTATION.inFov(mc.player.getYaw(), mc.player.getPitch(), this.fov.getValueFloat())) {
                  if (this.sync.getValue()) {
                     this.throwPearl(mc.player.getYaw(), mc.player.getPitch());
                  } else {
                     throwing = true;
                     if (mc.player.getMainHandStack().getItem() == Items.ENDER_PEARL) {
                        sendSequencedPacket(
                           id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch())
                        );
                        EntityUtil.swingHand(Hand.MAIN_HAND, this.getSwing());
                     } else {
                        int pearl;
                        if (this.inventory.getValue() && (pearl = InventoryUtil.findItemInventorySlotFromZero(Items.ENDER_PEARL)) != -1) {
                           Alien.INVENTORY.inventorySwap(pearl, mc.player.getInventory().selectedSlot);
                           sendSequencedPacket(
                              id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch())
                           );
                           EntityUtil.swingHand(Hand.MAIN_HAND, this.getSwing());
                           Alien.INVENTORY.inventorySwap(pearl, mc.player.getInventory().selectedSlot);
                           EntityUtil.syncInventory();
                        } else {
                           pearl = InventoryUtil.findItem(Items.ENDER_PEARL);
                           if (pearl != -1) {
                              Alien.INVENTORY.setSlot(pearl);
                              sendSequencedPacket(
                                 id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch())
                              );
                              EntityUtil.swingHand(Hand.MAIN_HAND, this.getSwing());
                              Alien.INVENTORY.syncToClient();
                           }
                        }
                     }

                     throwing = false;
                  }

                  this.finish();
               }
            }
         }
      }
   }

   public void onRotate(RotationEvent event) {
      if ((this.active || this.state) && this.shouldYawStep()) {
         event.setRotation(mc.player.getYaw(), mc.player.getPitch(), this.steps.getValueFloat(), this.priority.getValueFloat());
      }
   }

   public SwingSide getSwing() {
      return Macros.INSTANCE != null ? Macros.INSTANCE.getSwing() : this.interactSwing.getValue();
   }

   private void finish() {
      this.active = false;
      if (this.state) {
         this.disable();
      }
   }

   private boolean shouldYawStep() {
      return this.whenElytra.getValue() || !mc.player.isFallFlying() && (!ElytraFly.INSTANCE.isOn() || !ElytraFly.INSTANCE.isFallFlying())
         ? this.yawStep.getValue() && !Velocity.INSTANCE.noRotation()
         : false;
   }

   public void throwPearl(float yaw, float pitch) {
      throwing = true;
      if (mc.player.getMainHandStack().getItem() == Items.ENDER_PEARL) {
         if (this.rotation.getValue()) {
            Alien.ROTATION.snapAt(yaw, pitch);
         }

         sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitch));
         EntityUtil.swingHand(Hand.MAIN_HAND, this.getSwing());
         if (this.rotation.getValue()) {
            Alien.ROTATION.snapBack();
         }
      } else {
         int pearl;
         if (this.inventory.getValue() && (pearl = InventoryUtil.findItemInventorySlotFromZero(Items.ENDER_PEARL)) != -1) {
            Alien.INVENTORY.inventorySwap(pearl, mc.player.getInventory().selectedSlot);
            if (this.rotation.getValue()) {
               Alien.ROTATION.snapAt(yaw, pitch);
            }

            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitch));
            EntityUtil.swingHand(Hand.MAIN_HAND, this.getSwing());
            Alien.INVENTORY.inventorySwap(pearl, mc.player.getInventory().selectedSlot);
            EntityUtil.syncInventory();
            if (this.rotation.getValue()) {
               Alien.ROTATION.snapBack();
            }
         } else {
            pearl = InventoryUtil.findItem(Items.ENDER_PEARL);
            if (pearl != -1) {
               Alien.INVENTORY.setSlot(pearl);
               if (this.rotation.getValue()) {
                  Alien.ROTATION.snapAt(yaw, pitch);
               }

               sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitch));
               EntityUtil.swingHand(Hand.MAIN_HAND, this.getSwing());
               Alien.INVENTORY.syncToClient();
               if (this.rotation.getValue()) {
                  Alien.ROTATION.snapBack();
               }
            }
         }
      }

      throwing = false;
   }
}
