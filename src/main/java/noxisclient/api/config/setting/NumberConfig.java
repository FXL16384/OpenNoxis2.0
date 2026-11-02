package noxisclient.api.config.setting;

import java.util.function.Supplier;
import noxisclient.api.config.Config;

public class NumberConfig<T extends Number> extends Config<T> {
   public NumberConfig(String name, String description, T defaultValue) {
      super(name, description, defaultValue);
   }

   public NumberConfig(String name, String description, T minValue, T defaultValue, T maxValue) {
      super(name, description, defaultValue, minValue, maxValue);
   }

   public NumberConfig(String name, String description, T minValue, T defaultValue, T maxValue, Supplier<Boolean> visibility) {
      super(name, description, defaultValue, minValue, maxValue, visibility);
   }

   public double getValueSq() {
      double val = this.getValue().doubleValue();
      return val * val;
   }
}
