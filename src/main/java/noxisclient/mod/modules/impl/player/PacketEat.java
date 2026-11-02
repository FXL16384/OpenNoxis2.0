package noxisclient.mod.modules.impl.player;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import noxisclient.Alien;
import noxisclient.api.events.impl.PacketEvent;
import noxisclient.api.events.impl.UpdateEvent;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;

public class PacketEat extends Module {
   public static PacketEat INSTANCE;
   private final BooleanSetting deSync = this.add(new BooleanSetting("DeSync", false));
   private final BooleanSetting noRelease = this.add(new BooleanSetting("NoRelease", true));

   public PacketEat() {
      super("PacketEat", Module.Category.Player);
      this.setChinese("发包进食");
      INSTANCE = this;
   }

   public void onUpdate(UpdateEvent event) {
      if (this.deSync.getValue() && mc.player.isUsingItem() && mc.player.getActiveItem().getItem().getComponents().contains(DataComponentTypes.FOOD)) {
         Module.sendSequencedPacket(
            id -> new PlayerInteractItemC2SPacket(mc.player.getActiveHand(), id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch())
         );
      }
   }

   public void onPacket(PacketEvent.Send event) {
      PlayerActionC2SPacket packet;
      Packet<?> packet2;
      if (this.noRelease.getValue()
         && (packet2 = event.getPacket()) instanceof PlayerActionC2SPacket
         && (packet = (PlayerActionC2SPacket)packet2).getAction() == Action.RELEASE_USE_ITEM
         && mc.player.getActiveItem().getItem().getComponents().contains(DataComponentTypes.FOOD)) {
         event.cancel();
      }
   }
}
