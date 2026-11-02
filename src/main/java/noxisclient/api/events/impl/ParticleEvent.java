package noxisclient.api.events.impl;

import net.minecraft.client.particle.Particle;
import noxisclient.api.events.Event;

public class ParticleEvent extends Event {
   private static final ParticleEvent instance = new ParticleEvent();
   public Particle particle;

   public static ParticleEvent get(Particle particle) {
      instance.particle = particle;
      instance.setCancelled(false);
      return instance;
   }
}
