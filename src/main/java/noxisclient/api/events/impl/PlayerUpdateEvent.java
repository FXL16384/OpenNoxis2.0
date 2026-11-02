package noxisclient.api.events.impl;

import noxisclient.api.events.Event;

public class PlayerUpdateEvent extends Event {
   private float yaw;
   private float pitch;

   public float getYaw() {
      return this.yaw;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
   }
}
