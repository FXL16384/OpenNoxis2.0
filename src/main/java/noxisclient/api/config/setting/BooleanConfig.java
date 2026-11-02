package noxisclient.api.config.setting;

import java.util.function.Supplier;
import noxisclient.api.config.Config;

public class BooleanConfig extends Config<Boolean> {
   public BooleanConfig(String name, String description, Boolean defaultValue) {
      super(name, description, defaultValue);
   }

   public BooleanConfig(String name, String description, Boolean defaultValue, Supplier<Boolean> visibility) {
      super(name, description, defaultValue, visibility);
   }
}
