package noxisclient.api.config.setting;

import java.awt.Color;
import java.util.function.Supplier;
import noxisclient.api.config.Config;

public class ColorConfig extends Config<Color> {
   private final boolean allowAlpha;
   private boolean global;

   public ColorConfig(String name, String desc, Color value) {
      this(name, desc, value, true, true);
   }

   public ColorConfig(String name, String desc, Color value, boolean allowAlpha, boolean global) {
      super(name, desc, value);
      this.allowAlpha = allowAlpha;
      this.global = global;
   }

   public ColorConfig(String name, String desc, Color value, boolean allowAlpha, boolean global, Supplier<Boolean> visible) {
      super(name, desc, value, visible);
      this.allowAlpha = allowAlpha;
      this.global = global;
   }

   public ColorConfig(String name, String desc, Color value, boolean allowAlpha, Supplier<Boolean> visible) {
      this(name, desc, value, allowAlpha, true, visible);
   }

   public ColorConfig(String name, String desc, Color value, Supplier<Boolean> visible) {
      this(name, desc, value, true, visible);
   }

   public Color getValue() {
      return new Color(this.value.getRed(), this.value.getGreen(), this.value.getBlue(), this.allowAlpha ? this.value.getAlpha() : 255);
   }

   public Color getValue(int alpha) {
      return new Color(this.value.getRed(), this.value.getGreen(), this.value.getBlue(), alpha);
   }

   public int getRgb() {
      return this.getValue().getRGB();
   }

   public int getRed() {
      return this.value.getRed();
   }

   public int getGreen() {
      return this.value.getGreen();
   }

   public int getBlue() {
      return this.value.getBlue();
   }

   public boolean allowAlpha() {
      return this.allowAlpha;
   }

   public int getAlpha() {
      return this.allowAlpha ? this.value.getAlpha() : 255;
   }

   public boolean isGlobal() {
      return this.global;
   }

   public void setGlobal(boolean global) {
      this.global = global;
   }
}
