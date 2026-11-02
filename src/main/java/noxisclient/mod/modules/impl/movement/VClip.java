package noxisclient.mod.modules.impl.movement;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import noxisclient.api.events.impl.UpdateEvent;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.EnumSetting;

public class VClip extends Module {
   private final EnumSetting<VClip.Mode> mode = this.add(new EnumSetting<>("Mode", VClip.Mode.Jump));

   public VClip() {
      super("VClip", Module.Category.Movement);
      this.setChinese("纵向穿墙");
   }

   public void onUpdate(UpdateEvent event) {
      this.disable();
      switch ((VClip.Mode)this.mode.getValue()) {
         case Glitch:
            double posX = mc.player.getX();
            double posY = (double)Math.round(mc.player.getY());
            double posZ = mc.player.getZ();
            boolean onGround = mc.player.isOnGround();
            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(posX, posY, posZ, onGround));
            double halfY = 0.005;
            double var11;
            mc.player.setPosition(posX, var11 = posY - halfY, posZ);
            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(posX, var11, posZ, onGround));
            mc.player.setPosition(posX, posY = var11 - halfY * 300.0, posZ);
            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(posX, posY, posZ, onGround));
            break;
         case Teleport:
            mc.player.setPosition(mc.player.getX(), mc.player.getY() + 3.0, mc.player.getZ());
            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true));
            break;
         case Jump:
            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.4199999868869781, mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.7531999805212017, mc.player.getZ(), false));
            mc.player.setPosition(mc.player.getX(), mc.player.getY() + 1.0, mc.player.getZ());
            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true));
      }
   }

   public static enum Mode {
      Glitch,
      Teleport,
      Jump;
   }
}
