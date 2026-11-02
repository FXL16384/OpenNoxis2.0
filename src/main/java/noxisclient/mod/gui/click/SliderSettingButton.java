package noxisclient.mod.gui.click;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import noxisclient.api.utils.math.Timer;
import noxisclient.api.utils.render.Render2DUtil;
import noxisclient.core.impl.FontManager;
import noxisclient.mod.gui.items.Item;
import noxisclient.mod.modules.impl.client.ClickGui;
import noxisclient.mod.modules.impl.client.Fonts;
import noxisclient.mod.modules.settings.impl.SliderSetting;
import org.apache.commons.lang3.ArrayUtils;

public class SliderSettingButton extends SettingButton {
   private final SliderSetting sliderSetting;
   private char[] buffer;
   private boolean typing;
   private boolean idling;
   private boolean selected;
   private boolean dragging;
   private final Timer idleTimer = new Timer();

   public SliderSettingButton(SliderSetting setting, ModuleButton parent, float x, float y) {
      super(setting, parent, x, y, 99.0F, 9.0F + 4.0F);
      this.sliderSetting = setting;
      this.buffer = setting.getValueString().toCharArray();
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      ModuleButton.beginShaderBatch();
      this.renderBackground(context, mouseX, mouseY, delta);
      ModuleButton.flushShaderBatch(context);
      this.renderForeground(context, mouseX, mouseY, delta);
   }

   @Override
   public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
      ClickGui gui = ClickGui.getInstance();
      boolean thunder = gui.isThunderStyle();
      this.height = thunder ? 18.0F : 9.0F + 4.0F;
      int hoverAlpha = this.getHoverAlpha((double)mouseX, (double)mouseY);
      double min = this.sliderSetting.getMin();
      double max = this.sliderSetting.getMax();
      this.updateDragValue(mouseX, min, max);
      if (thunder) {
         if (!this.typing) {
            this.buffer = this.sliderSetting.getValueString().toCharArray();
         }

         if (this.shouldUseShaderBackground()) {
            this.queueSliderShaderBackground(context, min, max);
         }
      } else {
         if (!this.typing) {
            this.buffer = this.sliderSetting.getValueString().toCharArray();
            if (this.shouldUseShaderBackground()) {
               this.queueSliderShaderBackground(context, min, max);
            } else {
               float fill = this.getFillProgress(min, max);
               Color baseColor = gui.color.getValue();
               int alpha = Math.min(255, 100 + hoverAlpha);
               Color fillColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alpha);
               this.fill(context, this.x, this.y, fill * this.width * 1.0F, this.height * 1.0F, fillColor.getRGB());
            }
         } else if (this.shouldUseShaderBackground()) {
            this.queueSliderShaderBackground(context, min, max);
         }
      }
   }

   @Override
   public void renderForeground(DrawContext context, int mouseX, int mouseY, float delta) {
      ClickGui gui = ClickGui.getInstance();
      boolean thunder = gui.isThunderStyle();
      this.height = thunder ? 18.0F : 9.0F + 4.0F;
      double min = this.sliderSetting.getMin();
      double max = this.sliderSetting.getMax();
      if (thunder) {
         float fill = this.getFillProgress(min, max);
         String valueText = this.typing ? new String(this.buffer) + this.getInsertionPoint() : this.sliderSetting.getValueString();
         int labelText = gui.getThunderTextColor(false);
         int valueTextColor = gui.getThunderTextColor(true);
         this.drawString(context, this.setting.getName(), this.x + 6.0F, this.y + 4.0F, labelText);
         float valueWidth = Fonts.useClickGuiFont() ? FontManager.ui.getWidth(valueText) : (float)mc.textRenderer.getWidth(valueText);
         this.drawString(context, valueText, this.x + this.width * 1.0F - 6.0F - valueWidth, this.y + 5.0F, valueTextColor);
         float trackX = this.x + 6.0F;
         float trackY = this.y + this.height * 1.0F - 6.0F;
         float trackW = this.width * 1.0F - 12.0F;
         context.fill((int)trackX, (int)trackY, (int)(trackX + trackW), (int)(trackY + 2.0F), 687865855);
         Render2DUtil.horizontalGradient(
            context.getMatrices(), trackX, trackY, trackX + trackW * fill, trackY + 2.0F, gui.getThunderColor(180), gui.getThunderColor(0)
         );
         context.fill(
            (int)(this.x + 6.0F + (this.width * 1.0F - 14.0F) * fill),
            (int)(this.y + this.height * 1.0F - 7.5F),
            (int)(this.x + 8.0F + (this.width * 1.0F - 14.0F) * fill),
            (int)(this.y + this.height * 1.0F - 2.5F),
            -1973791
         );
      } else {
         int labelText = this.typing ? gui.getEnableTextColor() : gui.getDefaultTextColor();
         int valueText = gui.getEnableTextColor();
         String text = new String(this.buffer);
         if (this.typing && this.selected) {
            float textWidth;
            if (Fonts.useClickGuiFont()) {
               textWidth = FontManager.ui.getWidth(text);
            } else {
               textWidth = (float)mc.textRenderer.getWidth(text);
            }

            this.rect(context, this.x + 2.0F, this.y + 4.0F, textWidth * 1.0F, 9.0F * 1.0F, new Color(0, 120, 215, 160).getRGB());
         }

         this.drawString(
            context, this.typing ? text + this.getInsertionPoint() : this.setting.getName(), this.x + 2.0F, this.y + 4.0F, this.typing ? valueText : labelText
         );
         float textLeng = (float)mc.textRenderer.getWidth(this.setting.getName()) * 1.0F;
         if (!this.typing) {
            this.drawString(context, " " + this.sliderSetting.getValueString(), this.x + 2.0F + textLeng, this.y + 4.0F, valueText);
         }
      }
   }

   private void updateDragValue(int mouseX, double min, double max) {
      if (this.dragging && NewClickGuiScreen.MOUSE_LEFT_HOLD) {
         float fillv = ((float)mouseX - this.x) / (this.width * 1.0F);
         double val = min + (double)fillv * (max - min);
         double bval = MathHelper.clamp(val, min, max);
         this.sliderSetting.setValue(bval);
         float lower = (float)MathHelper.ceil(this.x + 1.0F);
         float upper = (float)MathHelper.floor(this.x + this.width * 1.0F - 1.0F);
         if ((float)mouseX < lower) {
            this.sliderSetting.setValue(min);
         } else if ((float)mouseX >= upper) {
            this.sliderSetting.setValue(max);
         }
      }
   }

   private void queueSliderShaderBackground(DrawContext context, double min, double max) {
      float fill = this.getFillProgress(min, max);
      float fullWidth = this.width * 1.0F;
      float filledWidth = fullWidth * fill;
      if (filledWidth > 0.0F) {
         this.queueShaderBackground(context, true, this.x, this.y, filledWidth, this.height * 1.0F);
      }

      if (filledWidth < fullWidth) {
         this.queueShaderBackground(context, false, this.x + filledWidth, this.y, fullWidth - filledWidth, this.height * 1.0F);
      }
   }

   private float getFillProgress(double min, double max) {
      double range = max - min;
      return range != 0.0 && !Double.isNaN(range) && !Double.isInfinite(range)
         ? MathHelper.clamp((float)((this.sliderSetting.getValue() - min) / range), 0.0F, 1.0F)
         : 0.0F;
   }

   @Override
   public void mouseClicked(double mouseX, double mouseY, int button) {
      if (this.visible) {
         if (this.isWithin(mouseX, mouseY)) {
            if (button == 0 && !this.typing) {
               this.dragging = true;
            } else if (button == 1) {
               if (this.typing) {
                  String number = new String(this.buffer);

                  try {
                     this.sliderSetting.setValue(Double.parseDouble(number));
                  } catch (NumberFormatException var8) {
                  }

                  this.typing = false;
                  this.selected = false;
               } else {
                  this.typing = true;
                  this.selected = false;
               }

               Item.sound();
            }
         } else {
            this.typing = false;
         }
      }
   }

   @Override
   public void mouseReleased(double mouseX, double mouseY, int button) {
      if (button == 0) {
         this.dragging = false;
      }
   }

   @Override
   public void keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.typing) {
         boolean control = (modifiers & 2) != 0;
         if (control) {
            if (keyCode == 86) {
               String clipboard = mc.keyboard.getClipboard();
               if (this.selected) {
                  this.buffer = new char[0];

                  for (char c : clipboard.toCharArray()) {
                     if (this.isSliderDigit(c)) {
                        this.buffer = ArrayUtils.add(this.buffer, c);
                     }
                  }

                  this.selected = false;
               } else {
                  for (char cx : clipboard.toCharArray()) {
                     if (this.isSliderDigit(cx)) {
                        this.buffer = ArrayUtils.add(this.buffer, cx);
                     }
                  }
               }

               return;
            }

            if (keyCode == 67) {
               mc.keyboard.setClipboard(new String(this.buffer));
               return;
            }

            if (keyCode == 65) {
               this.selected = true;
               return;
            }
         }

         switch (keyCode) {
            case 256:
               this.buffer = this.sliderSetting.getValueString().toCharArray();
               this.typing = false;
               this.selected = false;
               break;
            case 257:
               String number = new String(this.buffer);

               try {
                  this.sliderSetting.setValue(Double.parseDouble(number));
               } catch (NumberFormatException var10) {
               }

               this.buffer = this.sliderSetting.getValueString().toCharArray();
               this.typing = false;
               this.selected = false;
            case 258:
            default:
               break;
            case 259:
               if (this.selected) {
                  this.buffer = new char[0];
                  this.selected = false;
               } else if (this.buffer.length != 0) {
                  this.buffer = ArrayUtils.remove(this.buffer, this.buffer.length - 1);
               }
         }
      }
   }

   private boolean isSliderDigit(char c) {
      return Character.isDigit(c) || c == '.';
   }

   @Override
   public void charTyped(char chr, int modifiers) {
      if (this.typing && this.isSliderDigit(chr)) {
         if (this.selected) {
            this.buffer = new char[]{chr};
            this.selected = false;
         } else {
            this.buffer = ArrayUtils.add(this.buffer, chr);
         }
      }
   }

   public String getInsertionPoint() {
      if (this.idleTimer.passed(250L)) {
         this.idling = !this.idling;
         this.idleTimer.reset();
      }

      return this.idling && this.typing ? "_" : "";
   }

   public void endTyping() {
      this.typing = false;
   }

   public boolean isTyping() {
      return this.typing;
   }
}
