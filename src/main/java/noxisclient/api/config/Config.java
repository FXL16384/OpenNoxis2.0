package noxisclient.api.config;

import java.util.function.Supplier;

public class Config<T> {
   private final String name;
   private final String description;
   protected T value;
   private final T defaultValue;
   private final T minValue;
   private final T maxValue;
   private final Supplier<Boolean> visibility;

   public Config(String name, String description, T defaultValue) {
      this(name, description, defaultValue, null, null, null);
   }

   public Config(String name, String description, T defaultValue, Supplier<Boolean> visibility) {
      this(name, description, defaultValue, null, null, visibility);
   }

   public Config(String name, String description, T defaultValue, T minValue, T maxValue) {
      this(name, description, defaultValue, minValue, maxValue, null);
   }

   public Config(String name, String description, T defaultValue, T minValue, T maxValue, Supplier<Boolean> visibility) {
      this.name = name;
      this.description = description;
      this.value = defaultValue;
      this.defaultValue = defaultValue;
      this.minValue = minValue;
      this.maxValue = maxValue;
      this.visibility = visibility;
   }

   public String getName() {
      return this.name;
   }

   public String getDescription() {
      return this.description;
   }

   public T getValue() {
      return this.value;
   }

   public void setValue(T value) {
      this.value = value;
   }

   public T getDefaultValue() {
      return this.defaultValue;
   }

   public T getMinValue() {
      return this.minValue;
   }

   public T getMaxValue() {
      return this.maxValue;
   }

   public boolean isVisible() {
      return this.visibility == null || this.visibility.get();
   }

   public float getValueFloat() {
      return this.value instanceof Number ? ((Number)this.value).floatValue() : 0.0F;
   }
}
