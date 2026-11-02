package noxisclient.core.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import noxisclient.Alien;
import noxisclient.Vitality;
import noxisclient.api.events.impl.DeathEvent;
import noxisclient.api.events.impl.PacketEvent;
import noxisclient.api.events.impl.TotemEvent;
import noxisclient.api.utils.Wrapper;
import noxisclient.mod.modules.Module;

public class PopManager implements Wrapper {
   public final HashMap<String, Integer> popContainer = new HashMap<>();
   private final List<PlayerEntity> deadPlayer = new ArrayList<>();

   public PopManager() {
      this.init();
   }

   public void init() {
      Vitality.EVENT_BUS.subscribe(this);
   }

   public int getPop(String s) {
      return this.popContainer.getOrDefault(s, 0);
   }

   public int getPop(PlayerEntity player) {
      return this.getPop(player.getName().getString());
   }

   public void onUpdate() {
      if (!Module.nullCheck()) {
         for (AbstractClientPlayerEntity player : Vitality.THREAD.getPlayers()) {
            if (player == null || !player.isDead()) {
               this.deadPlayer.remove(player);
            } else if (!this.deadPlayer.contains(player)) {
               Vitality.EVENT_BUS.post(DeathEvent.get(player));
               this.onDeath(player);
               this.deadPlayer.add(player);
            }
         }
      }
   }

   public void onPacketReceive(PacketEvent.Receive event) {
      if (!Module.nullCheck()) {
         Packet<?> packet2 = event.getPacket();
         Entity entity;
         EntityStatusS2CPacket packet;
         if (packet2 instanceof EntityStatusS2CPacket
            && (packet = (EntityStatusS2CPacket)packet2).getStatus() == 35
            && (entity = packet.getEntity(mc.world)) instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity)entity;
            this.onTotemPop(player);
         }
      }
   }

   public void onDeath(PlayerEntity player) {
      this.popContainer.remove(player.getName().getString());
   }

   public void onTotemPop(PlayerEntity player) {
      int l_Count = 1;
      if (this.popContainer.containsKey(player.getName().getString())) {
         l_Count = this.popContainer.get(player.getName().getString());
         this.popContainer.put(player.getName().getString(), ++l_Count);
      } else {
         this.popContainer.put(player.getName().getString(), l_Count);
      }

      Alien.EVENT_BUS.post(TotemEvent.get(player));
   }
}
