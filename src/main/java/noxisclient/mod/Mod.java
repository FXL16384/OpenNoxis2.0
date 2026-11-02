package noxisclient.mod;

import noxisclient.api.utils.Wrapper;

public class Mod implements Wrapper {
   private final String name;

   public Mod(String name) {
      this.name = name;
   }

   public String getName() {
      return this.name;
   }
}
