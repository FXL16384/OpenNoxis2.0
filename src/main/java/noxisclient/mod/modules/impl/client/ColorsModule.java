package noxisclient.mod.modules.impl.client;

import java.awt.Color;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.ColorSetting;

public class ColorsModule extends Module {
   public static ColorsModule INSTANCE;
   public final ColorSetting clientColor = this.add(new ColorSetting("Color", new Color(255, 0, 0)).allowClientColor(false));

   public ColorsModule() {
      super("Colors", Module.Category.Client);
      this.setChinese("颜色");
      INSTANCE = this;
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
