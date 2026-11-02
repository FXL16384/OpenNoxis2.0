package noxisclient.mod.modules.impl.misc;

import net.minecraft.client.network.PlayerListEntry;
import noxisclient.Alien;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.client.Socials;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class ExtraTab extends Module {
   public static ExtraTab INSTANCE;
   public final EnumSetting<ExtraTab.Mode> mode = this.add(new EnumSetting<>("Mode", ExtraTab.Mode.Any));
   public final BooleanSetting self = this.add(new BooleanSetting("Self", true));
   public final SliderSetting size = this.add(new SliderSetting("Size", 200.0, 80.0, 1000.0, 1.0));
   public final SliderSetting columns = this.add(new SliderSetting("Columns", 20.0, 1.0, 100.0, 1.0));

   public ExtraTab() {
      super("ExtraTab", Module.Category.Misc);
      this.setChinese("列表无上限");
      INSTANCE = this;
   }

   public boolean shouldInclude(String name) {
      boolean friend = Socials.INSTANCE != null && Socials.INSTANCE.isFriendsEnabled() && Alien.FRIEND != null && Alien.FRIEND.friendList.contains(name);
      boolean enemy = Socials.INSTANCE != null && Socials.INSTANCE.isEnemysEnabled() && Alien.ENEMY != null && Alien.ENEMY.enemyList.contains(name);

      return switch ((ExtraTab.Mode)this.mode.getValue()) {
         case Enemy -> enemy;
         case Friend -> friend;
         case All -> friend || enemy;
         case Any -> true;
      };
   }

   public boolean shouldInclude(PlayerListEntry entry) {
      return mc.player != null && entry.getProfile().getId().equals(mc.player.getUuid())
         ? this.self.getValue()
         : this.shouldInclude(entry.getProfile().getName());
   }

   public static enum Mode {
      Enemy,
      Friend,
      All,
      Any;
   }
}
