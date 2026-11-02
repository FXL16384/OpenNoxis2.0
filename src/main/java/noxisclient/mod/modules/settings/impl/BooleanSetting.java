package noxisclient.mod.modules.settings.impl;

import java.util.function.BooleanSupplier;
import noxisclient.mod.modules.settings.Setting;

public class BooleanSetting extends Setting {
   private final boolean defaultValue;
   private boolean value;
   private boolean hasParent = false;
   private boolean expandWhenEnabledOnly = false;
   private boolean open = false;
   private Runnable task = null;
   private boolean injectTask = false;

   public BooleanSetting(String name, boolean defaultValue) {
      super(name);
      this.defaultValue = defaultValue;
      this.value = defaultValue;
   }

   public BooleanSetting(String name, boolean defaultValue, BooleanSupplier visibilityIn) {
      super(name, visibilityIn);
      this.defaultValue = defaultValue;
      this.value = defaultValue;
   }

   public boolean getValue() {
      return this.value;
   }

   public void setValue(boolean value) {
      this.value = value;
      if (this.injectTask) {
         this.task.run();
      }
   }

   public void setValueWithoutTask(boolean value) {
      this.value = value;
   }

   public boolean isOpen() {
      return this.hasParent ? this.open : true;
   }

   public void setOpen(boolean open) {
      this.open = open;
   }

   public boolean hasParent() {
      return this.hasParent;
   }

   public boolean canExpand() {
      return this.hasParent && (!this.expandWhenEnabledOnly || this.value);
   }

   public BooleanSetting setParent() {
      this.hasParent = true;
      return this;
   }

   public BooleanSetting setParentWhenEnabled() {
      this.expandWhenEnabledOnly = true;
      return this.setParent();
   }

   public BooleanSetting injectTask(Runnable task) {
      this.task = task;
      this.injectTask = true;
      return this;
   }

   public boolean getDefaultValue() {
      return this.defaultValue;
   }
}
