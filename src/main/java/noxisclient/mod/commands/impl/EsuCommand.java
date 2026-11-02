package noxisclient.mod.commands.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import net.minecraft.text.Text;
import noxisclient.Alien;
import noxisclient.mod.commands.Command;
import noxisclient.mod.modules.Module;

public class EsuCommand extends Command {
   public EsuCommand() {
      super("esu", "");
   }

   @Override
   public void runCommand(String[] parameters) {
      if (parameters.length == 1) {
         Alien.THREAD.execute(() -> {
            try {
               URL url = new URL("https://api.xywlapi.cc/qqapi?qq=" + parameters[0]);
               BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

               String inputLine;
               while ((inputLine = in.readLine()) != null) {
                  if (Module.nullCheck()) {
                     return;
                  }

                  mc.player.sendMessage(Text.of(inputLine));
               }
            } catch (Exception var4) {
               var4.printStackTrace();
            }
         });
      }
   }

   @Override
   public String[] getAutocorrect(int count, List<String> seperated) {
      return null;
   }
}
