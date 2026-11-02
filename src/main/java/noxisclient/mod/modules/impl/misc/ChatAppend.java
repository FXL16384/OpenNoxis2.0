package noxisclient.mod.modules.impl.misc;

import java.util.Random;
import noxisclient.api.events.impl.SendMessageEvent;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.StringSetting;

public class ChatAppend extends Module {
   public static ChatAppend INSTANCE;
   private final EnumSetting<ChatAppend.Mode> mode = this.add(new EnumSetting<>("Mode", ChatAppend.Mode.Custom));
   private final StringSetting message = this.add(new StringSetting("Text", "Vitality", () -> this.mode.getValue() == ChatAppend.Mode.Custom));
   private static final String CHAR_POOL = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
   private final Random random = new Random();

   public ChatAppend() {
      super("ChatAppend", Module.Category.Misc);
      this.setChinese("消息后缀");
      INSTANCE = this;
   }

   private String generateRandomSuffix() {
      StringBuilder sb = new StringBuilder("‹");

      for (int i = 0; i < 3; i++) {
         sb.append(
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
               .charAt(this.random.nextInt("0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".length()))
         );
      }

      sb.append("›");
      return sb.toString();
   }

   public void onSendMessage(SendMessageEvent event) {
      if (!nullCheck() && !event.isCancelled() && !AutoReconnect.inQueueServer) {
         Object message = event.message;
         if (!((String)message).startsWith("/")
            && !((String)message).startsWith("!")
            && !((String)message).startsWith("$")
            && !((String)message).startsWith("#")) {
            String suffix = this.mode.getValue() == ChatAppend.Mode.Random ? this.generateRandomSuffix() : this.message.getValue();
            if (!((String)message).endsWith(suffix)) {
               event.message = (String)message + " " + suffix;
            }
         }
      }
   }

   public static enum Mode {
      Custom,
      Random;
   }
}
