package noxisclient.core.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import net.minecraft.entity.player.PlayerEntity;
import noxisclient.core.Manager;
import noxisclient.mod.modules.impl.client.Socials;
import org.apache.commons.io.IOUtils;

public class FriendManager extends Manager {
   public final ArrayList<String> friendList = new ArrayList<>();
   private boolean paused = true;

   public FriendManager() {
      this.read();
      this.paused = Socials.INSTANCE == null || !Socials.INSTANCE.isFriendsEnabled();
   }

   public boolean isFriend(String name) {
      return this.paused ? name.equals("lizard") || name.equals("lizard") : name.equals("lizard") || name.equals("lizard") || this.friendList.contains(name);
   }

   public boolean isFriend(PlayerEntity entity) {
      return this.isFriend(entity.getGameProfile().getName());
   }

   public void setPaused(boolean paused) {
      this.paused = paused;
   }

   public boolean isPaused() {
      return this.paused;
   }

   public void remove(String name) {
      this.friendList.remove(name);
   }

   public void add(String name) {
      if (!this.friendList.contains(name)) {
         this.friendList.add(name);
      }
   }

   public void friend(PlayerEntity entity) {
      this.friend(entity.getGameProfile().getName());
   }

   public void friend(String name) {
      if (this.friendList.contains(name)) {
         this.friendList.remove(name);
      } else {
         this.friendList.add(name);
      }
   }

   public void read() {
      try {
         File friendFile = getFile("friends.txt");
         if (!friendFile.exists()) {
            return;
         }

         for (String s : IOUtils.readLines(new FileInputStream(friendFile), StandardCharsets.UTF_8)) {
            this.add(s);
         }
      } catch (Exception var5) {
         var5.printStackTrace();
      }
   }

   public void save() {
      try {
         File friendFile = getFile("friends.txt");
         PrintWriter printwriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(friendFile), StandardCharsets.UTF_8));

         for (String str : this.friendList) {
            printwriter.println(str);
         }

         printwriter.close();
      } catch (Exception var5) {
         var5.printStackTrace();
         System.out.println("[Vitality] Failed to save friends");
      }
   }
}
