package noxisclient.mod.commands.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import noxisclient.Alien;
import noxisclient.core.Manager;
import noxisclient.mod.commands.Command;
import noxisclient.mod.modules.impl.combat.AutoRegear;

public class KitCommand extends Command {
   public KitCommand() {
      super("kit", "[save/list/load/delete] [name]");
   }

   public KitCommand(String name) {
      super(name, "[save/list/load/delete] [name]");
   }

   @Override
   public void runCommand(String[] parameters) {
      if (parameters.length == 0) {
         this.sendUsage();
      } else {
         String var2 = parameters[0].toLowerCase();
         switch (var2) {
            case "list":
               if (parameters.length != 1) {
                  this.sendUsage();
                  return;
               }

               this.listKits();
               break;
            case "save":
               if (parameters.length != 2) {
                  this.sendUsage();
                  return;
               }

               this.saveKit(parameters[1]);
               break;
            case "load":
               if (parameters.length != 2) {
                  this.sendUsage();
                  return;
               }

               if (AutoRegear.INSTANCE == null) {
                  this.sendChatMessage("§cAutoRegear is not initialized.");
                  return;
               }

               AutoRegear.INSTANCE.loadKit(parameters[1]);
               break;
            case "delete":
               if (parameters.length != 2) {
                  this.sendUsage();
                  return;
               }

               this.deleteKit(parameters[1]);
               break;
            default:
               this.sendUsage();
         }
      }
   }

   private void listKits() {
      try {
         File folder = Manager.getFolder();
         File[] files = folder.listFiles();
         if (files == null) {
            return;
         }

         for (File file : files) {
            if (file.getName().endsWith(".kit")) {
               String name = file.getName();
               this.sendChatMessage("Kit: [" + name.substring(0, name.length() - 4) + "]");
            }
         }
      } catch (Exception var8) {
         var8.printStackTrace();
      }
   }

   private void saveKit(String name) {
      if (mc.player != null) {
         try {
            File file = Manager.getFile(name + ".kit");
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));

            for (int i = 0; i < 36; i++) {
               ItemStack stack = mc.player.getInventory().getStack(i);
               writer.println(i + ":" + this.getKitKey(stack));
            }

            writer.close();
            this.sendChatMessage("§fKit [" + name + "] saved");
         } catch (Exception var6) {
            this.sendChatMessage("§fKit [" + name + "] save failed");
            var6.printStackTrace();
         }
      }
   }

   private String getKitKey(ItemStack stack) {
      String itemKey = stack.getItem().getTranslationKey();
      if (stack.getItem() == Items.SPLASH_POTION) {
         PotionContentsComponent potionContents = stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);

         for (StatusEffectInstance effect : potionContents.getEffects()) {
            StatusEffect effectType = effect.getEffectType().value();
            if (effectType == StatusEffects.RESISTANCE.value()) {
               return itemKey + ":turtle";
            }

            if (effectType == StatusEffects.STRENGTH.value()) {
               return itemKey + ":strength";
            }
         }
      }

      return itemKey;
   }

   private void deleteKit(String name) {
      try {
         File file = Manager.getFile(name + ".kit");
         if (file.exists() && !file.delete()) {
            this.sendChatMessage("§fKit [" + name + "] delete failed");
            return;
         }

         this.sendChatMessage("§fKit [" + name + "] deleted");
      } catch (Exception var3) {
         var3.printStackTrace();
      }
   }

   @Override
   public String[] getAutocorrect(int count, List<String> seperated) {
      if (count != 1) {
         return null;
      } else {
         String input = seperated.getLast().toLowerCase();
         ArrayList<String> correct = new ArrayList<>();

         for (String x : List.of("save", "list", "load", "delete")) {
            if (input.equalsIgnoreCase(Alien.getPrefix() + this.getName()) || x.startsWith(input)) {
               correct.add(x);
            }
         }

         return correct.toArray(new String[0]);
      }
   }
}
