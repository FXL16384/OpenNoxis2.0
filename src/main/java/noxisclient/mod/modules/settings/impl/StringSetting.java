package noxisclient.mod.modules.settings.impl;

import java.util.function.BooleanSupplier;
import noxisclient.mod.modules.settings.Setting;

public class StringSetting extends Setting {
   private final String defaultValue;
   private String value;
   private Runnable task = null;
   private boolean injectTask = false;

   public StringSetting(String name, String value) {
      super(name);
      this.value = value;
      this.defaultValue = value;
   }

   public StringSetting(String name, String value, BooleanSupplier visibilityIn) {
      super(name, visibilityIn);
      this.value = value;
      this.defaultValue = value;
   }

   public String getValue() {
      return this.value;
   }

   public void setValue(String text) {
      this.value = text;
      if (this.injectTask) {
         this.task.run();
      }
   }

   public String getDefaultValue() {
      return this.defaultValue;
   }

   public StringSetting injectTask(Runnable task) {
      this.task = task;
      this.injectTask = true;
      return this;
   }
}
