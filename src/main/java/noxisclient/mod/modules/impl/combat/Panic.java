package noxisclient.mod.modules.impl.combat;

import java.util.ArrayList;
import java.util.List;
import noxisclient.Vitality;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.StringSetting;

public class Panic extends Module {
   private final BooleanSetting restoreOnDisable;
   private final StringSetting whitelist;
   private final List<Module> disabledModules = new ArrayList<>();

   public Panic() {
      super("Panic", "紧急停止所有功能", Module.Category.Combat);
      this.setChinese("紧急停止所有功能");
      this.restoreOnDisable = this.add(new BooleanSetting("恢复启用", true));
      this.whitelist = this.add(new StringSetting("白名单", "ClickGui,HudEditor,Offhand"));
   }

   @Override
   public void onEnable() {
      this.disabledModules.clear();
      List<Module> allModules = this.getAllModules();
      String[] whitelistNames = this.whitelist.getValue().split(",");
      ArrayList<String> whitelistModules = new ArrayList<>();

      for (String name : whitelistNames) {
         whitelistModules.add(name.trim().toLowerCase());
      }

      for (Module module : allModules) {
         if (module != this && !this.shouldWhitelist(module, whitelistModules) && module.isOn()) {
            this.disabledModules.add(module);
            module.disable();
         }
      }

      this.sendMessage("§c已紧急停止 " + this.disabledModules.size() + " 个功能");
   }

   @Override
   public void onDisable() {
      if (this.restoreOnDisable.getValue() && !this.disabledModules.isEmpty()) {
         int restoredCount = 0;

         for (Module module : this.disabledModules) {
            if (module.isOff()) {
               module.enable();
               restoredCount++;
            }
         }

         this.sendMessage("§a已恢复 " + restoredCount + " 个功能");
      }

      this.disabledModules.clear();
   }

   @Override
   public String getInfo() {
      return this.disabledModules.isEmpty() ? "" : this.disabledModules.size() + "";
   }

   private List<Module> getAllModules() {
      return (List<Module>)(Vitality.MODULE == null ? List.of() : new ArrayList<>(Vitality.MODULE.getModules()));
   }

   private boolean shouldWhitelist(Module module, List<String> whitelistModules) {
      if (module == null) {
         return false;
      } else {
         String moduleName = module.getName().toLowerCase();

         for (String whitelistName : whitelistModules) {
            if (moduleName.equals(whitelistName.toLowerCase())) {
               return true;
            }
         }

         return false;
      }
   }

   public void panicNow() {
      if (!this.isOn()) {
         this.enable();
      }
   }

   public void restoreNow() {
      if (this.isOn()) {
         this.disable();
      }
   }
}
