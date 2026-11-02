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

public class EnemyManager extends Manager {
   public final ArrayList<String> enemyList = new ArrayList<>();
   private boolean paused;

   public EnemyManager() {
      this.read();
      this.paused = Socials.INSTANCE == null || !Socials.INSTANCE.isEnemysEnabled();
   }

   public boolean isEnemy(String name) {
      return !this.paused && this.enemyList.contains(name);
   }

   public boolean isEnemy(PlayerEntity entity) {
      return this.isEnemy(entity.getGameProfile().getName());
   }

   public void setPaused(boolean paused) {
      this.paused = paused;
   }

   public boolean isPaused() {
      return this.paused;
   }

   public void remove(String name) {
      this.enemyList.remove(name);
   }

   public void add(String name) {
      if (!this.enemyList.contains(name)) {
         this.enemyList.add(name);
      }
   }

   public void enemy(PlayerEntity entity) {
      this.enemy(entity.getGameProfile().getName());
   }

   public void enemy(String name) {
      if (this.enemyList.contains(name)) {
         this.enemyList.remove(name);
      } else {
         this.enemyList.add(name);
      }
   }

   public void read() {
      try {
         File enemyFile = getFile("enemies.txt");
         if (!enemyFile.exists()) {
            return;
         }

         for (String name : IOUtils.readLines(new FileInputStream(enemyFile), StandardCharsets.UTF_8)) {
            this.add(name);
         }
      } catch (Exception var5) {
         var5.printStackTrace();
      }
   }

   public void save() {
      try {
         File enemyFile = getFile("enemies.txt");
         PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(enemyFile), StandardCharsets.UTF_8));

         for (String name : this.enemyList) {
            writer.println(name);
         }

         writer.close();
      } catch (Exception var5) {
         var5.printStackTrace();
         System.out.println("[Vitality] Failed to save enemies");
      }
   }
}
