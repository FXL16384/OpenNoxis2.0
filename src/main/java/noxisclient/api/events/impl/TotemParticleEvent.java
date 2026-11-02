package noxisclient.api.events.impl;

import java.awt.Color;
import noxisclient.api.events.Event;

public class TotemParticleEvent extends Event {
   private static final TotemParticleEvent instance = new TotemParticleEvent();
   public double velocityX;
   public double velocityY;
   public double velocityZ;
   public Color color;

   private TotemParticleEvent() {
   }

   public static TotemParticleEvent get(double velocityX, double velocityY, double velocityZ) {
      instance.velocityX = velocityX;
      instance.velocityY = velocityY;
      instance.velocityZ = velocityZ;
      return instance;
   }
}
