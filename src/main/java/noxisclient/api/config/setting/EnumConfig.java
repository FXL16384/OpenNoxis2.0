package noxisclient.api.config.setting;

import java.util.function.Supplier;
import noxisclient.api.config.Config;

public class EnumConfig<T extends Enum<T>> extends Config<T> {
   private final T[] values;

   public EnumConfig(String name, String description, T defaultValue, T[] values) {
      super(name, description, defaultValue);
      this.values = values;
   }

   public EnumConfig(String name, String description, T defaultValue, T[] values, Supplier<Boolean> visibility) {
      super(name, description, defaultValue, visibility);
      this.values = values;
   }

   public T[] getValues() {
      return this.values;
   }
}
