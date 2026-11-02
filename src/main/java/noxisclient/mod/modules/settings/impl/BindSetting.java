package noxisclient.mod.modules.settings.impl;

import java.util.function.BooleanSupplier;
import net.minecraft.client.util.InputUtil.Type;
import noxisclient.api.utils.Wrapper;
import noxisclient.mod.modules.settings.Setting;
import org.lwjgl.glfw.GLFW;

public class BindSetting extends Setting {
   private final int defaultValue;
   public boolean holding = false;
   private int value;
   private boolean pressed = false;
   private boolean holdEnable = false;

   public BindSetting(String name, int value) {
      super(name);
      this.defaultValue = value;
      this.value = value;
   }

   public BindSetting(String name, int value, BooleanSupplier visibilityIn) {
      super(name, visibilityIn);
      this.defaultValue = value;
      this.value = value;
   }

   public int getValue() {
      return this.value;
   }

   public void setValue(int value) {
      this.value = value;
   }

   public String getKeyString() {
      if (this.value == -1) {
         return "None";
      } else if (this.value < -1) {
         return "Mouse" + (Math.abs(this.value) - 1);
      } else if (this.value == 0) {
         return "None";
      } else {
         String keyName = GLFW.glfwGetKeyName(this.value, GLFW.glfwGetKeyScancode(this.value));
         if (keyName == null) {
            keyName = Type.KEYSYM.createFromCode(this.value).getLocalizedText().getString();
         }

         return keyName.toUpperCase();
      }
   }

   public boolean isPressed() {
      if (this.value == -1 || this.value == 0) {
         return false;
      } else {
         return this.value < -1
            ? GLFW.glfwGetMouseButton(Wrapper.mc.getWindow().getHandle(), -(this.value + 1)) == 1
            : GLFW.glfwGetKey(Wrapper.mc.getWindow().getHandle(), this.value) == 1;
      }
   }

   public void setPressed(boolean pressed) {
      this.pressed = pressed;
   }

   public boolean isHoldEnable() {
      return this.holdEnable;
   }

   public void setHoldEnable(boolean holdEnable) {
      this.holdEnable = holdEnable;
   }

   public int getDefaultValue() {
      return this.defaultValue;
   }
}
