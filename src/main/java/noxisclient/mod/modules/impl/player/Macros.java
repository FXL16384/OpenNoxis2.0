package noxisclient.mod.modules.impl.player;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import noxisclient.Alien;
import noxisclient.api.events.impl.RotationEvent;
import noxisclient.api.events.impl.TickEvent;
import noxisclient.api.utils.player.EntityUtil;
import noxisclient.api.utils.player.InventoryUtil;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.enums.SwingSide;
import noxisclient.mod.modules.settings.impl.BindSetting;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;

public class Macros extends Module {
   public static Macros INSTANCE;
   private final BindSetting pearlKey = this.add(new BindSetting("PearlKey", -1));
   private final BindSetting fireworkKey = this.add(new BindSetting("FireworkKey", -1));
   private final BindSetting chestSwapKey = this.add(new BindSetting("ChestSwapKey", -1));
   private final BindSetting friendKey = this.add(new BindSetting("FriendKey", -1));
   private final EnumSetting<AutoArmor.ChestPriority> chestPriority = this.add(new EnumSetting<>("ChestPriority", AutoArmor.ChestPriority.NETHERITE));
   private final BooleanSetting chestAutoFirework = this.add(new BooleanSetting("ChestAutoFirework", true));
   private final EnumSetting<SwingSide> swing = this.add(new EnumSetting<>("Swing", SwingSide.All));
   private boolean pearlPressed;
   private boolean fireworkPressed;
   private boolean chestSwapPressed;
   private boolean friendPressed;

   public Macros() {
      super("TweakMarco", Module.Category.Player);
      this.setChinese("宏");
      INSTANCE = this;
      if (AutoPearl.INSTANCE == null) {
         new AutoPearl();
      }
   }

   @Override
   public void onDisable() {
      this.pearlPressed = false;
      this.fireworkPressed = false;
      this.chestSwapPressed = false;
      this.friendPressed = false;
   }

   public SwingSide getSwing() {
      return this.swing.getValue();
   }

   public void onTick(TickEvent event) {
      if (!event.isPost()) {
         if (AutoPearl.INSTANCE != null) {
            AutoPearl.INSTANCE.onUpdate(event);
         }

         if (!Module.nullCheck()) {
            this.handlePearl();
            this.handleFirework();
            this.handleChestSwap();
            this.handleFriend();
         }
      }
   }

   public void onRotate(RotationEvent event) {
      if (AutoPearl.INSTANCE != null) {
         AutoPearl.INSTANCE.onRotate(event);
      }
   }

   private void handlePearl() {
      boolean down = this.pearlKey.isPressed();
      if (down && !this.pearlPressed && mc.currentScreen == null && AutoPearl.INSTANCE != null) {
         AutoPearl.INSTANCE.interactSwing.setValue(this.swing.getValue());
         AutoPearl.INSTANCE.trigger();
      }

      this.pearlPressed = down;
   }

   private void handleFirework() {
      boolean down = this.fireworkKey.isPressed();
      if (down && !this.fireworkPressed && mc.currentScreen == null) {
         this.useFirework();
      }

      this.fireworkPressed = down;
   }

   private void handleChestSwap() {
      boolean down = this.chestSwapKey.isPressed();
      if (down && !this.chestSwapPressed && mc.currentScreen == null && AutoArmor.INSTANCE != null) {
         AutoArmor.INSTANCE.macroChestSwap(this.chestPriority.getValue(), this.chestAutoFirework.getValue(), this.swing.getValue());
      }

      this.chestSwapPressed = down;
   }

   private void handleFriend() {
      boolean down = this.friendKey.isPressed();
      if (down && !this.friendPressed && mc.currentScreen == null) {
         this.toggleTargetFriend();
      }

      this.friendPressed = down;
   }

   private void toggleTargetFriend() {
      if (mc.crosshairTarget instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof PlayerEntity player) {
         Alien.FRIEND.friend(player);
      }
   }

   private void useFirework() {
      if (mc.player.getMainHandStack().getItem() == Items.FIREWORK_ROCKET) {
         sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
         EntityUtil.swingHand(Hand.MAIN_HAND, this.swing.getValue());
      } else if (mc.player.getOffHandStack().getItem() == Items.FIREWORK_ROCKET) {
         sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
         EntityUtil.swingHand(Hand.OFF_HAND, this.swing.getValue());
      } else {
         int firework = InventoryUtil.findItemInventorySlot(Items.FIREWORK_ROCKET);
         if (firework != -1) {
            Alien.INVENTORY.inventorySwap(firework, mc.player.getInventory().selectedSlot);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
            EntityUtil.swingHand(Hand.MAIN_HAND, this.swing.getValue());
            Alien.INVENTORY.inventorySwap(firework, mc.player.getInventory().selectedSlot);
            EntityUtil.syncInventory();
         } else {
            firework = InventoryUtil.findItem(Items.FIREWORK_ROCKET);
            if (firework != -1) {
               Alien.INVENTORY.setSlot(firework);
               sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
               EntityUtil.swingHand(Hand.MAIN_HAND, this.swing.getValue());
               Alien.INVENTORY.syncToClient();
            }
         }
      }
   }
}
