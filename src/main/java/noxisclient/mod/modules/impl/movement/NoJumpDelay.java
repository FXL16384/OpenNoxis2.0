package noxisclient.mod.modules.impl.movement;

import noxisclient.api.events.impl.UpdateEvent;
import noxisclient.asm.accessors.ILivingEntity;
import noxisclient.mod.modules.Module;

public class NoJumpDelay extends Module {
   public static NoJumpDelay INSTANCE;

   public NoJumpDelay() {
      super("NoJumpDelay", Module.Category.Movement);
      this.setChinese("无跳跃冷却");
      INSTANCE = this;
   }

   public void onUpdate(UpdateEvent event) {
      ((ILivingEntity)mc.player).setLastJumpCooldown(0);
   }
}
