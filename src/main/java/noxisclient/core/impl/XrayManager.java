package noxisclient.core.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import net.minecraft.block.Blocks;
import noxisclient.core.Manager;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.render.Xray;
import org.apache.commons.io.IOUtils;

public class XrayManager extends Manager {
   private final ArrayList<String> list = new ArrayList<>();

   public XrayManager() {
      this.read();
   }

   public ArrayList<String> getList() {
      return this.list;
   }

   public boolean inWhitelist(String name) {
      return this.list.contains(name) || this.list.contains(name.replace("block.minecraft.", "").replace("item.minecraft.", ""));
   }

   public void clear() {
      this.list.clear();
   }

   public void remove(String name) {
      if (this.list.remove(name.replace("block.minecraft.", "").replace("item.minecraft.", "")) && !Module.nullCheck() && Xray.INSTANCE.isOn()) {
         mc.worldRenderer.reload();
      }
   }

   public void add(String name) {
      if (!this.list.contains(name = name.replace("block.minecraft.", "").replace("item.minecraft.", ""))) {
         this.list.add(name);
         if (!Module.nullCheck() && Xray.INSTANCE.isOn()) {
            mc.worldRenderer.reload();
         }
      }
   }

   public void read() {
      try {
         File friendFile = getFile("xrays.txt");
         if (!friendFile.exists()) {
            this.add(Blocks.DIAMOND_ORE.getTranslationKey());
            this.add(Blocks.DEEPSLATE_DIAMOND_ORE.getTranslationKey());
            this.add(Blocks.GOLD_ORE.getTranslationKey());
            this.add(Blocks.NETHER_GOLD_ORE.getTranslationKey());
            this.add(Blocks.IRON_ORE.getTranslationKey());
            this.add(Blocks.DEEPSLATE_IRON_ORE.getTranslationKey());
            this.add(Blocks.REDSTONE_ORE.getTranslationKey());
            this.add(Blocks.EMERALD_ORE.getTranslationKey());
            this.add(Blocks.DEEPSLATE_EMERALD_ORE.getTranslationKey());
            this.add(Blocks.DEEPSLATE_REDSTONE_ORE.getTranslationKey());
            this.add(Blocks.COAL_ORE.getTranslationKey());
            this.add(Blocks.DEEPSLATE_COAL_ORE.getTranslationKey());
            this.add(Blocks.ANCIENT_DEBRIS.getTranslationKey());
            this.add(Blocks.NETHER_QUARTZ_ORE.getTranslationKey());
            this.add(Blocks.LAPIS_ORE.getTranslationKey());
            this.add(Blocks.DEEPSLATE_LAPIS_ORE.getTranslationKey());
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
         File friendFile = getFile("xrays.txt");
         PrintWriter printwriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(friendFile), StandardCharsets.UTF_8));

         for (String str : this.list) {
            printwriter.println(str);
         }

         printwriter.close();
      } catch (Exception var5) {
         System.out.println("[Vitality] Failed to save xrays");
      }
   }
}
