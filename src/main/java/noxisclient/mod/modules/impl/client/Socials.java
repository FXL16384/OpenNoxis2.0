package noxisclient.mod.modules.impl.client;

import java.awt.Color;
import noxisclient.Vitality;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.ColorSetting;

public class Socials extends Module {
   public static Socials INSTANCE;
   public static final String ADD_FRIEND_MESSAGE = "I just add your friend on Noxis!";
   public final BooleanSetting friends = this.add(new BooleanSetting("Friends", true).setParentWhenEnabled().injectTask(this::syncFriendState));
   public final ColorSetting friendsColor = this.add(new ColorSetting("FriendsColor", new Color(90, 200, 255), this.friends::isOpen));
   public final BooleanSetting addFriend = this.add(new BooleanSetting("AddFriend", true, this.friends::isOpen));
   public final BooleanSetting enemys = this.add(new BooleanSetting("Enemys", true).setParentWhenEnabled().injectTask(this::syncEnemyState));
   public final ColorSetting enemysColor = this.add(new ColorSetting("EnemysColor", new Color(255, 85, 85), this.enemys::isOpen));

   public Socials() {
      super("Socials", Module.Category.Client);
      this.setChinese("社交");
      INSTANCE = this;
   }

   public boolean isFriendsEnabled() {
      return this.friends.getValue();
   }

   public boolean isEnemysEnabled() {
      return this.enemys.getValue();
   }

   public void notifyFriendAdded(String name, boolean wasFriend) {
      if (!wasFriend && this.isFriendsEnabled() && this.addFriend.getValue() && mc.player != null && mc.player.networkHandler != null) {
         mc.player.networkHandler.sendChatCommand("msg " + name + " I just add your friend on Noxis!");
      }
   }

   private void syncFriendState() {
      if (Vitality.FRIEND != null) {
         Vitality.FRIEND.setPaused(!this.isFriendsEnabled());
      }
   }

   private void syncEnemyState() {
      if (Vitality.ENEMY != null) {
         Vitality.ENEMY.setPaused(!this.isEnemysEnabled());
      }
   }

   public void syncState() {
      this.syncFriendState();
      this.syncEnemyState();
   }

   @Override
   public void enable() {
      this.state = true;
   }

   @Override
   public void disable() {
      this.state = true;
   }

   @Override
   public boolean isOn() {
      return true;
   }
}
