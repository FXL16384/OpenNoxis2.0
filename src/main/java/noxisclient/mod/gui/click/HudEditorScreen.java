package noxisclient.mod.gui.click;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import noxisclient.mod.modules.impl.client.HudEditor;

public class HudEditorScreen extends Screen {
   public HudEditorScreen(HudEditor hudEditor, Screen parent) {
      super(Text.literal("HudEditor"));
   }

   public static void open(HudEditor hudEditor, Screen parent) {
      MinecraftClient.getInstance().setScreen(NewClickGuiScreen.getInstance());
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      MinecraftClient.getInstance().setScreen(NewClickGuiScreen.getInstance());
   }

   @Override
   public boolean shouldPause() {
      return false;
   }
}
