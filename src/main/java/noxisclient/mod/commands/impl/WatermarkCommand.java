package noxisclient.mod.commands.impl;

import java.util.Arrays;
import java.util.List;
import noxisclient.mod.commands.Command;
import noxisclient.mod.modules.impl.hud.WaterMarkHud;

public class WatermarkCommand extends Command {
   public WatermarkCommand() {
      super("watermark", "[text]");
   }

   @Override
   public void runCommand(String[] parameters) {
      if (parameters.length == 0) {
         this.sendUsage();
      } else {
         StringBuilder text = new StringBuilder();
         boolean first = true;

         for (String s : Arrays.stream(parameters).toList()) {
            if (first) {
               text.append(s);
               first = false;
            } else {
               text.append(" ").append(s);
            }
         }

         if (WaterMarkHud.INSTANCE != null) {
            WaterMarkHud.INSTANCE.setWatermarkText(text.toString());
         }
      }
   }

   @Override
   public String[] getAutocorrect(int count, List<String> seperated) {
      return null;
   }
}
