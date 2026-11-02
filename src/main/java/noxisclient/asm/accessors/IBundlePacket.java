package noxisclient.asm.accessors;

import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.BundlePacket;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({BundlePacket.class})
public interface IBundlePacket<T extends PacketListener> {
   @Mutable
   @Accessor("packets")
   void setPackets(Iterable<Packet<? super T>> var1);
}
