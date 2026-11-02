package noxisclient.mod.modules.impl.combat;

import noxisclient.Vitality;
import noxisclient.mod.modules.Module;

public class AntiTK extends Module {
   public static AntiTK INSTANCE;

   public AntiTK() {
      super("AntiTK", "Anti Team Kill - Temporarily disable friend list when enabled", Module.Category.Combat);
      this.setChinese("防友");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      Vitality.FRIEND.setPaused(true);
      this.sendMessage("§cFriend list temporarily disabled");
   }

   @Override
   public void onDisable() {
      Vitality.FRIEND.setPaused(false);
      this.sendMessage("§aFriend list restored");
   }

   @Override
   public String getInfo() {
      return Vitality.FRIEND.isPaused() ? "Active" : "";
   }
}
