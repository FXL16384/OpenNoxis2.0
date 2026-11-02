package noxisclient.asm.mixins;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.TimeoutException;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.handler.PacketEncoderException;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import noxisclient.Vitality;
import noxisclient.api.events.impl.PacketEvent;
import noxisclient.core.impl.CommandManager;
import noxisclient.mod.modules.impl.client.ClientSetting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientConnection.class})
public class MixinClientConnection {
   @Inject(
      at = {@At("HEAD")},
      method = {"handlePacket"},
      cancellable = true
   )
   private static <T extends PacketListener> void onHandlePacket(Packet<T> packet, PacketListener listener, CallbackInfo ci) {
      PacketEvent.Receive event = new PacketEvent.Receive(packet);
      Vitality.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"send(Lnet/minecraft/network/packet/Packet;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onSendPacketPre(Packet<?> packet, CallbackInfo info) {
      PacketEvent.Send event = new PacketEvent.Send(packet);
      Vitality.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         info.cancel();
      }
   }

   @Inject(
      method = {"send(Lnet/minecraft/network/packet/Packet;)V"},
      at = {@At("RETURN")}
   )
   private void onSendPacketPost(Packet<?> packet, CallbackInfo info) {
      PacketEvent.Sent event = new PacketEvent.Sent(packet);
      Vitality.EVENT_BUS.post(event);
   }

   @Inject(
      method = {"exceptionCaught"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void exceptionCaught(ChannelHandlerContext context, Throwable throwable, CallbackInfo ci) {
      if (ClientSetting.INSTANCE != null
         && !(throwable instanceof TimeoutException)
         && !(throwable instanceof PacketEncoderException)
         && ClientSetting.INSTANCE.caughtException.getValue()) {
         if (ClientSetting.INSTANCE.log.getValue()) {
            CommandManager.sendMessage("§4Caught exception §7" + throwable.getMessage());
         }

         ci.cancel();
      }
   }
}
