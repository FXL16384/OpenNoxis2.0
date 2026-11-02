package noxisclient.mod.modules.impl.client;

import java.io.PrintWriter;
import noxisclient.mod.modules.Module;

public class IRC extends Module {
   public static PrintWriter printWriter;
   private static final String SERVER_HOST = "127.0.0.1";
   private static final int SERVER_PORT = 25565;
   public static volatile boolean connect;

   public IRC() {
      super("IRC", Module.Category.Client);
   }
}
