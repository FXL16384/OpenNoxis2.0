package noxisclient.asm.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.gui.widget.TextIconButtonWidget;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import noxisclient.Alien;
import noxisclient.core.impl.FontManager;
import noxisclient.mod.modules.impl.client.ClickGui;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PressableWidget.class})
public abstract class MixinPressableWidget {
   @Unique
   private static final int TITLE_BUTTON_BLACK = new Color(0, 0, 0, 128).getRGB();
   @Unique
   private static final float TITLE_BUTTON_EDGE_PAD = 0.75F;
   @Unique
   private static final float TITLE_BUTTON_EDGE_FEATHER = 1.25F;
   @Unique
   private static final int TITLE_BUTTON_CORNER_SEGMENTS = 18;

   @Inject(
      method = {"renderWidget"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void renderNoxisTitleButton(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if ((Object)this instanceof ButtonWidget && !((Object)this instanceof TextIconButtonWidget)) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.currentScreen instanceof TitleScreen) {
            noxisclient.mod.modules.impl.client.TitleScreen titleScreen = noxisclient.mod.modules.impl.client.TitleScreen.INSTANCE;
            if (titleScreen != null && titleScreen.isOn()) {
               renderTitleButton(context, ((ClickableWidget)(Object)this), titleScreen);
               ci.cancel();
            }
         }
      }
   }

   @Unique
   private static void renderTitleButton(DrawContext context, ClickableWidget widget, noxisclient.mod.modules.impl.client.TitleScreen titleScreen) {
      int x = widget.getX();
      int y = widget.getY();
      int width = widget.getWidth();
      int height = widget.getHeight();
      boolean hovered = widget.active && (widget.isHovered() || widget.isFocused());
      ClickGui gui = ClickGui.getInstance();
      int textColor = getTextColor(titleScreen, gui, widget.active, hovered);
      float radius = 6.0F;
      applyTitleButtonBlur((float)x, (float)y, (float)width, (float)height, radius);
      drawTitleRoundedRect(context, (float)x, (float)y, (float)width, (float)height, radius, TITLE_BUTTON_BLACK);
      drawCenteredText(context, widget, textColor);
   }

   @Unique
   private static void applyTitleButtonBlur(float x, float y, float width, float height, float radius) {
      if (Alien.BLUR != null && !(width <= 0.0F) && !(height <= 0.0F)) {
         Alien.BLUR.applyRoundedBlur(12.0F, x, y, width, height, radius);
      }
   }

   @Unique
   private static void drawTitleRoundedRect(DrawContext context, float x, float y, float width, float height, float radius, int color) {
      float pad = 0.75F;
      float left = x - pad;
      float top = y - pad;
      float rectWidth = width + pad * 2.0F;
      float rectHeight = height + pad * 2.0F;
      float r = Math.min(radius + pad, Math.min(rectWidth, rectHeight) * 0.5F);
      if (!(rectWidth <= 0.0F) && !(rectHeight <= 0.0F) && (color >>> 24 & 0xFF) > 0) {
         Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableCull();
         RenderSystem.setShader(GameRenderer::getPositionColorProgram);
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
         buffer.vertex(matrix, left + rectWidth * 0.5F, top + rectHeight * 0.5F, 0.0F).color(color);
         addTitleCorner(buffer, matrix, left + r, top + r, r, Math.PI, Math.PI * 3.0 / 2.0, color);
         addTitleCorner(buffer, matrix, left + rectWidth - r, top + r, r, Math.PI * 3.0 / 2.0, Math.PI * 2, color);
         addTitleCorner(buffer, matrix, left + rectWidth - r, top + rectHeight - r, r, 0.0, Math.PI / 2, color);
         addTitleCorner(buffer, matrix, left + r, top + rectHeight - r, r, Math.PI / 2, Math.PI, color);
         addTitleVertex(buffer, matrix, left, top + r, color);
         BufferRenderer.drawWithGlobalProgram(buffer.end());
         drawTitleRoundedFeather(matrix, left, top, rectWidth, rectHeight, r, 1.25F, color);
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
      }
   }

   @Unique
   private static void drawTitleRoundedFeather(Matrix4f matrix, float x, float y, float width, float height, float radius, float feather, int color) {
      if (!(feather <= 0.0F) && (color >>> 24 & 0xFF) > 0) {
         int transparent = color & 16777215;
         float outerX = x - feather;
         float outerY = y - feather;
         float outerWidth = width + feather * 2.0F;
         float outerHeight = height + feather * 2.0F;
         float outerRadius = Math.min(radius + feather, Math.min(outerWidth, outerHeight) * 0.5F);
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
         addTitleFeatherCorner(
            buffer,
            matrix,
            x + radius,
            y + radius,
            radius,
            outerX + outerRadius,
            outerY + outerRadius,
            outerRadius,
            Math.PI,
            Math.PI * 3.0 / 2.0,
            color,
            transparent
         );
         addTitleFeatherCorner(
            buffer,
            matrix,
            x + width - radius,
            y + radius,
            radius,
            outerX + outerWidth - outerRadius,
            outerY + outerRadius,
            outerRadius,
            Math.PI * 3.0 / 2.0,
            Math.PI * 2,
            color,
            transparent
         );
         addTitleFeatherCorner(
            buffer,
            matrix,
            x + width - radius,
            y + height - radius,
            radius,
            outerX + outerWidth - outerRadius,
            outerY + outerHeight - outerRadius,
            outerRadius,
            0.0,
            Math.PI / 2,
            color,
            transparent
         );
         addTitleFeatherCorner(
            buffer,
            matrix,
            x + radius,
            y + height - radius,
            radius,
            outerX + outerRadius,
            outerY + outerHeight - outerRadius,
            outerRadius,
            Math.PI / 2,
            Math.PI,
            color,
            transparent
         );
         addTitleFeatherPair(buffer, matrix, x, y + radius, outerX, outerY + outerRadius, color, transparent);
         BufferRenderer.drawWithGlobalProgram(buffer.end());
      }
   }

   @Unique
   private static void addTitleFeatherCorner(
      BufferBuilder buffer,
      Matrix4f matrix,
      float innerCx,
      float innerCy,
      float innerRadius,
      float outerCx,
      float outerCy,
      float outerRadius,
      double startAngle,
      double endAngle,
      int innerColor,
      int outerColor
   ) {
      for (int i = 0; i <= 18; i++) {
         double progress = (double)i / 18.0;
         double angle = startAngle + (endAngle - startAngle) * progress;
         float cos = (float)Math.cos(angle);
         float sin = (float)Math.sin(angle);
         addTitleFeatherPair(
            buffer,
            matrix,
            innerCx + cos * innerRadius,
            innerCy + sin * innerRadius,
            outerCx + cos * outerRadius,
            outerCy + sin * outerRadius,
            innerColor,
            outerColor
         );
      }
   }

   @Unique
   private static void addTitleFeatherPair(
      BufferBuilder buffer, Matrix4f matrix, float innerX, float innerY, float outerX, float outerY, int innerColor, int outerColor
   ) {
      buffer.vertex(matrix, outerX, outerY, 0.0F).color(outerColor);
      buffer.vertex(matrix, innerX, innerY, 0.0F).color(innerColor);
   }

   @Unique
   private static void addTitleCorner(BufferBuilder buffer, Matrix4f matrix, float cx, float cy, float radius, double startAngle, double endAngle, int color) {
      for (int i = 0; i <= 18; i++) {
         double progress = (double)i / 18.0;
         double angle = startAngle + (endAngle - startAngle) * progress;
         addTitleVertex(buffer, matrix, cx + (float)Math.cos(angle) * radius, cy + (float)Math.sin(angle) * radius, color);
      }
   }

   @Unique
   private static void addTitleVertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, int color) {
      buffer.vertex(matrix, x, y, 0.0F).color(color);
   }

   @Unique
   private static void drawCenteredText(DrawContext context, ClickableWidget widget, int color) {
      String text = widget.getMessage().getString();
      if (!text.isEmpty()) {
         float textWidth = FontManager.INSTANCE.width(text);
         float textHeight = FontManager.INSTANCE.height();
         if (!(textWidth <= 0.0F) && !(textHeight <= 0.0F)) {
            float maxWidth = Math.max(1.0F, (float)widget.getWidth() - 12.0F);
            float maxHeight = Math.max(1.0F, (float)widget.getHeight() - 6.0F);
            float scale = Math.min(1.0F, Math.min(maxWidth / textWidth, maxHeight / textHeight));
            float drawWidth = textWidth * scale;
            float drawHeight = textHeight * scale;
            float textX = (float)widget.getX() + ((float)widget.getWidth() - drawWidth) / 2.0F;
            float textY = (float)widget.getY() + ((float)widget.getHeight() - drawHeight) / 2.0F + 0.5F;
            context.getMatrices().push();
            context.getMatrices().translate(textX, textY, 0.0F);
            context.getMatrices().scale(scale, scale, 1.0F);
            FontManager.INSTANCE.drawString(context, text, 0.0F, 0.0F, color, true);
            context.getMatrices().pop();
         }
      }
   }

   @Unique
   private static int getTextColor(noxisclient.mod.modules.impl.client.TitleScreen titleScreen, ClickGui gui, boolean active, boolean hovered) {
      if (!active) {
         return new Color(145, 145, 145, 190).getRGB();
      } else if (titleScreen != null && titleScreen.getShaderMode() == noxisclient.mod.modules.impl.client.TitleScreen.MenuShader.TitleImage) {
         return hovered ? new Color(255, 248, 243).getRGB() : new Color(247, 226, 218).getRGB();
      } else if (gui == null) {
         return hovered ? Color.WHITE.getRGB() : new Color(220, 220, 220).getRGB();
      } else {
         return hovered ? gui.enableTextColor.getValue().getRGB() : gui.defaultTextColor.getValue().getRGB();
      }
   }
}
