package noxisclient.mod.commands.impl;

import java.util.List;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import noxisclient.Vitality;
import noxisclient.api.events.impl.PacketEvent;
import noxisclient.mod.commands.Command;

public class PingCommand extends Command {
   private long sendTime;

   public PingCommand() {
      super("ping", "");
   }

   @Override
   public void runCommand(String[] parameters) {
      this.sendTime = System.currentTimeMillis();
      mc.getNetworkHandler().sendChatCommand("chat ");
      Vitality.EVENT_BUS.subscribe(this);
   }

   @Override
   public String[] getAutocorrect(int count, List<String> seperated) {
      return null;
   }

   public void onPacketReceive(PacketEvent.Receive e) {
      Packet<?> packet2 = e.getPacket();
      GameMessageS2CPacket packet;
      if (packet2 instanceof GameMessageS2CPacket
         && (
            (packet = (GameMessageS2CPacket)packet2).content().getString().contains("chat.use")
               || packet.content().getString().contains("命令")
               || packet.content().getString().contains("Bad command")
               || packet.content().getString().contains("No such command")
               || packet.content().getString().contains("<--[HERE]")
               || packet.content().getString().contains("Unknown")
               || packet.content().getString().contains("帮助")
               || packet.content().getString().contains("执行错误")
         )) {
         this.sendChatMessage("ping: " + (System.currentTimeMillis() - this.sendTime) + "ms");
         Vitality.EVENT_BUS.unsubscribe(this);
      }
   }
}
