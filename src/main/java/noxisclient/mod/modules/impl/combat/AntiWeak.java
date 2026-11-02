package noxisclient.mod.modules.impl.combat;

import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.InteractType;
import noxisclient.Alien;
import noxisclient.api.events.impl.PacketEvent;
import noxisclient.api.utils.math.Timer;
import noxisclient.api.utils.player.EntityUtil;
import noxisclient.api.utils.player.InventoryUtil;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class AntiWeak extends Module {
   private final EnumSetting<AntiWeak.SwapMode> swapMode = this.add(new EnumSetting<>("SwapMode", AntiWeak.SwapMode.Inventory));
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 100, 0, 500).setSuffix("ms"));
   private final BooleanSetting onlyCrystal = this.add(new BooleanSetting("OnlyCrystal", true));
   private final Timer delayTimer = new Timer();
   boolean ignore = false;
   private PlayerInteractEntityC2SPacket lastPacket = null;

   public AntiWeak() {
      super("AntiWeak", Module.Category.Combat);
      this.setChinese("反虚弱");
   }

   @Override
   public String getInfo() {
      return this.swapMode.getValue().name();
   }

   public void onPacketSend(PacketEvent.Send event) {
      if (!nullCheck()) {
         if (!event.isCancelled()) {
            if (!this.ignore) {
               if (mc.player.getStatusEffect(StatusEffects.WEAKNESS) != null) {
                  if (!(mc.player.getMainHandStack().getItem() instanceof SwordItem)) {
                     if (this.delayTimer.passedMs(this.delay.getValue())) {
                        Packet<?> packet2 = event.getPacket();
                        PlayerInteractEntityC2SPacket packet;
                        if (packet2 instanceof PlayerInteractEntityC2SPacket
                           && Criticals.getInteractType(packet = (PlayerInteractEntityC2SPacket)packet2) == InteractType.ATTACK) {
                           if (this.onlyCrystal.getValue() && !(Criticals.getEntity(packet) instanceof EndCrystalEntity)) {
                              return;
                           }

                           this.lastPacket = packet;
                           this.delayTimer.reset();
                           this.ignore = true;
                           this.doAnti();
                           this.ignore = false;
                           event.cancel();
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void doAnti() {
      if (this.lastPacket != null) {
         int strong = this.swapMode.getValue() != AntiWeak.SwapMode.Inventory
            ? InventoryUtil.findClass(SwordItem.class)
            : InventoryUtil.findClassInventorySlot(SwordItem.class);
         if (strong != -1) {
            int old = mc.player.getInventory().selectedSlot;
            if (this.swapMode.getValue() == AntiWeak.SwapMode.Silent) {
               Alien.INVENTORY.setSlot(strong);
            } else if (this.swapMode.getValue() != AntiWeak.SwapMode.Inventory) {
               InventoryUtil.switchToSlot(strong);
            } else {
               Alien.INVENTORY.inventorySwap(strong, mc.player.getInventory().selectedSlot);
            }

            mc.getNetworkHandler().sendPacket(this.lastPacket);
            if (this.swapMode.getValue() != AntiWeak.SwapMode.Inventory) {
               if (this.swapMode.getValue() == AntiWeak.SwapMode.Silent) {
                  Alien.INVENTORY.syncToClient();
               } else if (this.swapMode.getValue() != AntiWeak.SwapMode.Normal) {
                  InventoryUtil.switchToSlot(old);
               }
            } else {
               Alien.INVENTORY.inventorySwap(strong, mc.player.getInventory().selectedSlot);
               EntityUtil.syncInventory();
            }
         }
      }
   }

   public static enum SwapMode {
      Normal,
      Silent,
      Inventory;
   }
}
