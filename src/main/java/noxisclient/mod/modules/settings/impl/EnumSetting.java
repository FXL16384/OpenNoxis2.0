package noxisclient.mod.modules.settings.impl;

import java.util.function.BooleanSupplier;
import noxisclient.mod.modules.settings.EnumConverter;
import noxisclient.mod.modules.settings.Setting;

public class EnumSetting<T extends Enum<T>> extends Setting {
   private final T defaultValue;
   private T value;
   private Runnable task = null;
   private boolean injectTask = false;

   public EnumSetting(String name, T defaultValue) {
      super(name);
      this.value = defaultValue;
      this.defaultValue = defaultValue;
   }

   public EnumSetting(String name, T defaultValue, BooleanSupplier visibilityIn) {
      super(name, visibilityIn);
      this.value = defaultValue;
      this.defaultValue = defaultValue;
   }

   public void loadSetting(String enumString) {
      EnumConverter converter = new EnumConverter();
      if (enumString == null) {
         this.value = this.defaultValue;
      } else {
         Enum<?> value = converter.get(this.defaultValue, enumString);
         if (value == null) {
            for (Enum<?> option : (Enum[])this.defaultValue.getDeclaringClass().getEnumConstants()) {
               if (option.toString().equalsIgnoreCase(enumString)) {
                  value = option;
                  break;
               }
            }
         }

         this.value = (T)(value != null ? value : this.defaultValue);
      }
   }

   public void increaseEnum() {
      this.value = (T)EnumConverter.increaseEnum(this.value);
      if (this.injectTask) {
         this.task.run();
      }
   }

   public void resetValue() {
      this.value = this.defaultValue;
   }

   public T getValue() {
      return this.value;
   }

   public void setValue(T value) {
      this.value = value;
      if (this.injectTask) {
         this.task.run();
      }
   }

   public void setEnumValue(String value) {
      for (Enum e : (Enum[])this.value.getDeclaringClass().getEnumConstants()) {
         if (e.name().equalsIgnoreCase(value) || e.toString().equalsIgnoreCase(value)) {
            this.value = (T)e;
            if (this.injectTask) {
               this.task.run();
            }
         }
      }
   }

   public EnumSetting<T> injectTask(Runnable task) {
      this.task = task;
      this.injectTask = true;
      return this;
   }

   public boolean is(T mode) {
      return this.getValue() == mode;
   }

   public T getDefaultValue() {
      return this.defaultValue;
   }

   public static String getDisplayName(Enum<?> value) {
      if (value == null) {
         return "";
      } else {
         String name = value.toString().replace('_', ' ');
         StringBuilder spaced = new StringBuilder();

         for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i > 0 && c != ' ' && Character.isUpperCase(c)) {
               char prev = name.charAt(i - 1);
               if (prev != ' ' && (Character.isLowerCase(prev) || Character.isDigit(prev))) {
                  spaced.append(' ');
               }
            }

            spaced.append(c);
         }

         StringBuilder result = new StringBuilder();
         boolean wordStart = true;

         for (int i = 0; i < spaced.length(); i++) {
            char c = spaced.charAt(i);
            if (c == ' ') {
               result.append(c);
               wordStart = true;
            } else if (wordStart) {
               result.append(Character.toUpperCase(c));
               wordStart = false;
            } else {
               result.append(Character.toLowerCase(c));
            }
         }

         return result.toString();
      }
   }
}
