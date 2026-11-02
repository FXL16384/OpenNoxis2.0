package noxisclient.mod.gui.click;

import com.mojang.blaze3d.systems.RenderSystem;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import noxisclient.Alien;
import noxisclient.Vitality;
import noxisclient.api.utils.math.SmoothAnimationUtil;
import noxisclient.mod.modules.HudElement;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.client.ClickGui;
import noxisclient.mod.modules.impl.client.HudEditor;
import org.lwjgl.glfw.GLFW;

public class NewClickGuiScreen extends Screen {
   private static NewClickGuiScreen INSTANCE;
   private Screen returnScreen;
   private final List<Frame> frames = new ArrayList<>();
   private final Map<Module.Category, float[]> framePositions = new HashMap<>();
   private final Map<String, Boolean> moduleExpansionStates = new HashMap<>();
   private final List<HudElement> hudElements = new ArrayList<>();
   private final Map<HudElement, SmoothAnimationUtil> hudAnimX = new HashMap<>();
   private final Map<HudElement, SmoothAnimationUtil> hudAnimY = new HashMap<>();
   private HudElement draggingHud = null;
   private int dragOffsetX = 0;
   private int dragOffsetY = 0;
   private boolean showHudGuides = true;
   public static boolean MOUSE_LEFT_HOLD = false;
   private float lastFrameWidth = -1.0F;
   private float lastFrameSpacing = -1.0F;
   private static final int HUD_SNAP = 2;

   public NewClickGuiScreen() {
      super(Text.literal("ClickGUI"));
      INSTANCE = this;
   }

   public static NewClickGuiScreen getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new NewClickGuiScreen();
      }

      return INSTANCE;
   }

   public NewClickGuiScreen openFrom(Screen screen) {
      this.returnScreen = screen instanceof NewClickGuiScreen ? null : screen;
      return this;
   }

   private void saveFramePositions() {
      for (Frame frame : this.frames) {
         this.framePositions.put(frame.getCategory(), new float[]{frame.getX(), frame.getY()});
      }
   }

   private void saveModuleExpansionStates() {
      for (Frame frame : this.frames) {
         for (ModuleButton moduleButton : frame.getModuleButtons()) {
            this.moduleExpansionStates.put(moduleButton.getModule().getName(), moduleButton.isOpen());
         }
      }
   }

   private void restoreModuleExpansionStates() {
      for (Frame frame : this.frames) {
         for (ModuleButton moduleButton : frame.getModuleButtons()) {
            boolean expanded = this.moduleExpansionStates.getOrDefault(moduleButton.getModule().getName(), false);
            moduleButton.restoreExpansion(expanded);
         }
      }
   }

   private void setupFrames() {
      if (!this.frames.isEmpty()) {
         this.saveFramePositions();
         this.saveModuleExpansionStates();
      }

      this.frames.clear();
      this.hudElements.clear();
      this.hudAnimX.clear();
      this.hudAnimY.clear();
      if (this.client != null && this.client.getWindow() != null) {
         ClickGui gui = ClickGui.getInstance();
         float frameWidth = gui == null ? 100.0F : gui.width.getValueFloat();
         float frameSpacing = gui == null ? 2.0F : gui.titleGap.getValueFloat();
         List<Module.Category> defaultOrder = this.getDefaultFrameOrder();
         int categoryCount = defaultOrder.size();
         float totalWidth = (float)categoryCount * (frameWidth + frameSpacing) - frameSpacing;
         int screenWidth = this.client.getWindow().getScaledWidth();
         float startX = ((float)screenWidth - totalWidth) / 2.0F - 50.0F;
         float x = startX;

         for (Module.Category category : defaultOrder) {
            float[] savedPos = this.framePositions.get(category);
            if (savedPos != null) {
               this.frames.add(new Frame(category, savedPos[0], savedPos[1]));
            } else {
               this.frames.add(new Frame(category, x, 20.0F));
            }

            x += frameWidth + frameSpacing;
         }

         this.restoreModuleExpansionStates();
         this.setupHudElements();
         this.lastFrameWidth = frameWidth;
         this.lastFrameSpacing = frameSpacing;
      }
   }

   private List<Module.Category> getDefaultFrameOrder() {
      List<Module.Category> categories = new ArrayList<>();

      for (Module.Category category : Module.Category.values()) {
         categories.add(category);
      }

      return categories;
   }

   private void setupHudElements() {
      if (Alien.MODULE != null) {
         for (Module module : Alien.MODULE.getModules()) {
            if (module.getCategory() == Module.Category.HUD && module instanceof HudElement hudElement) {
               this.hudElements.add(hudElement);
               this.hudAnimX.put(hudElement, new SmoothAnimationUtil((double)hudElement.getElementX(), 0.28, 0.2));
               this.hudAnimY.put(hudElement, new SmoothAnimationUtil((double)hudElement.getElementY(), 0.28, 0.2));
            }
         }
      }
   }

   private boolean isHudEditorMode() {
      return HudEditor.INSTANCE != null && HudEditor.INSTANCE.isOn();
   }

   private boolean isFrameVisible(Frame frame) {
      boolean hudFrame = frame.getCategory() == Module.Category.HUD;
      return this.isHudEditorMode() ? hudFrame : !hudFrame;
   }

   private List<Frame> getVisibleFramesSnapshot() {
      List<Frame> visibleFrames = new ArrayList<>();

      for (Frame frame : this.frames) {
         if (this.isFrameVisible(frame)) {
            visibleFrames.add(frame);
         }
      }

      return visibleFrames;
   }

   private SmoothAnimationUtil getAnimX(HudElement element) {
      return this.hudAnimX.computeIfAbsent(element, key -> new SmoothAnimationUtil((double)element.getElementX(), 0.28, 0.2));
   }

   private SmoothAnimationUtil getAnimY(HudElement element) {
      return this.hudAnimY.computeIfAbsent(element, key -> new SmoothAnimationUtil((double)element.getElementY(), 0.28, 0.2));
   }

   private float getRenderHudX(HudElement element) {
      SmoothAnimationUtil anim = this.getAnimX(element);
      anim.setTarget((double)element.getElementX());
      return (float)anim.update();
   }

   private float getRenderHudY(HudElement element) {
      SmoothAnimationUtil anim = this.getAnimY(element);
      anim.setTarget((double)element.getElementY());
      return (float)anim.update();
   }

   private void relayoutFrames(float frameWidth, float frameSpacing) {
      if (this.client != null && this.client.getWindow() != null && !this.frames.isEmpty()) {
         int categoryCount = this.frames.size();
         float totalWidth = (float)categoryCount * (frameWidth + frameSpacing) - frameSpacing;
         int screenWidth = this.client.getWindow().getScaledWidth();
         float startX = ((float)screenWidth - totalWidth) / 2.0F - 50.0F;
         float x = startX;

         for (Frame frame : this.frames) {
            frame.setX(x);
            x += frameWidth + frameSpacing;
         }
      }
   }

   private float getRenderScale() {
      ClickGui gui = ClickGui.getInstance();
      if (gui == null) {
         return 1.0F;
      } else {
         float fadeScale = 0.92F + 0.08F * (float)gui.alphaValue;
         return Math.max(0.05F, fadeScale * gui.size.getValueFloat());
      }
   }

   private float getSlideY() {
      ClickGui gui = ClickGui.getInstance();
      return gui == null ? 0.0F : (1.0F - (float)gui.alphaValue) * 20.0F;
   }

   private double toLocalMouseX(double mouseX) {
      return mouseX / (double)this.getRenderScale();
   }

   private double toLocalMouseY(double mouseY) {
      return (mouseY - (double)this.getSlideY()) / (double)this.getRenderScale();
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      if (this.client != null && this.client.world == null && this.returnScreen != null) {
         this.returnScreen.render(context, mouseX, mouseY, delta);
      }

      if (ClickGui.getInstance() != null) {
         float frameWidth = ClickGui.getInstance().width.getValueFloat();
         float frameSpacing = ClickGui.getInstance().titleGap.getValueFloat();
         if (Math.abs(frameWidth - this.lastFrameWidth) > 0.01F || Math.abs(frameSpacing - this.lastFrameSpacing) > 0.01F) {
            this.relayoutFrames(frameWidth, frameSpacing);
            this.lastFrameWidth = frameWidth;
            this.lastFrameSpacing = frameSpacing;
         }

         float keyCodec = (float)ClickGui.getInstance().alphaValue;
         float scale = this.getRenderScale();
         float slideY = this.getSlideY();
         int localMouseX = (int)this.toLocalMouseX((double)mouseX);
         int localMouseY = (int)this.toLocalMouseY((double)mouseY);
         ClickGui.getInstance().renderVanillaBlur(delta);
         if (this.isHudEditorMode()) {
            this.renderHudOverlay(context, mouseX, mouseY, delta);
         }

         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, keyCodec);
         context.getMatrices().push();
         context.getMatrices().translate(0.0F, slideY, 0.0F);
         context.getMatrices().scale(scale, scale, 1.0F);
         Frame.beginTitleShaderBatch();

         for (Frame frame : this.getVisibleFramesSnapshot()) {
            frame.render(context, localMouseX, localMouseY, delta);
         }

         Frame.flushTitleShaderBatch(context);

         for (Frame frame : this.getVisibleFramesSnapshot()) {
            frame.renderTitleForeground(context);
         }

         context.getMatrices().pop();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   private void renderHudOverlay(DrawContext context, int mouseX, int mouseY, float delta) {
      if (this.showHudGuides && this.draggingHud != null) {
         context.fill(this.width / 2, 0, this.width / 2 + 1, this.height, 369098751);
         context.fill(0, this.height / 2, this.width, this.height / 2 + 1, 369098751);
      }

      for (HudElement element : this.hudElements) {
         this.renderHudElement(context, element, mouseX, mouseY, delta);
      }

      if (this.showHudGuides && this.draggingHud != null) {
         this.renderHudAlignGuides(context);
      }

      context.fill(0, this.height - 13, this.width, this.height, -1879048192);
      String alignInfo = this.draggingHud != null ? (this.draggingHud.getAlignMode() == HudElement.AlignMode.RIGHT ? " [Right]" : " [Left]") : "";
      String info = this.draggingHud != null
         ? String.format("HudEditor (%d, %d)%s", this.draggingHud.getElementX(), this.draggingHud.getElementY(), alignInfo)
         : "HudEditor | Left drag | Right toggle | G guides | R center";
      context.drawCenteredTextWithShadow(this.textRenderer, info, this.width / 2, this.height - 10, 16777215);
   }

   private void renderHudElement(DrawContext context, HudElement element, int mouseX, int mouseY, float delta) {
      int actualX = element.getElementX();
      int actualY = element.getElementY();
      int width = element.getElementWidth();
      int height = element.getElementHeight();
      if (width > 0 && height > 0) {
         float renderX = this.getRenderHudX(element);
         float renderY = this.getRenderHudY(element);
         if (element.isElementVisible()) {
            try {
               context.getMatrices().push();
               context.getMatrices().translate(renderX - (float)actualX, renderY - (float)actualY, 0.0F);
               element.renderElement(context, delta);
               context.getMatrices().pop();
            } catch (Exception var18) {
            }
         }

         if (this.draggingHud == element) {
            int rx = Math.round(renderX);
            int ry = Math.round(renderY);
            this.drawDashedRect(context, rx, ry, width, height, -1);
            String label = element.getElementName();
            int labelWidth = this.textRenderer.getWidth(label);
            int labelX = rx + width + 4;
            int labelY = ry - 2;
            if (labelX + labelWidth + 6 > this.width) {
               labelX = rx - labelWidth - 6;
            }

            if (labelY < 2) {
               labelY = ry + height + 2;
            }

            context.fill(labelX - 2, labelY - 1, labelX + labelWidth + 2, labelY + 9, -1342177280);
            context.drawTextWithShadow(this.textRenderer, label, labelX, labelY, -1);
         }
      }
   }

   private void drawDashedRect(DrawContext context, int x, int y, int width, int height, int color) {
      int dash = 4;
      int gap = 2;

      for (int i = 0; i < width; i += dash + gap) {
         int end = Math.min(i + dash, width);
         context.fill(x + i, y, x + end, y + 1, color);
         context.fill(x + i, y + height - 1, x + end, y + height, color);
      }

      for (int i = 0; i < height; i += dash + gap) {
         int end = Math.min(i + dash, height);
         context.fill(x, y + i, x + 1, y + end, color);
         context.fill(x + width - 1, y + i, x + width, y + end, color);
      }
   }

   private void renderHudAlignGuides(DrawContext context) {
      int dx = this.draggingHud.getElementX();
      int dy = this.draggingHud.getElementY();
      int dw = this.draggingHud.getElementWidth();
      int dh = this.draggingHud.getElementHeight();
      if (Math.abs(dx + dw / 2 - this.width / 2) <= 2) {
         context.fill(this.width / 2, 0, this.width / 2 + 1, this.height, -2147418113);
      }

      if (Math.abs(dy + dh / 2 - this.height / 2) <= 2) {
         context.fill(0, this.height / 2, this.width, this.height / 2 + 1, -2147418113);
      }

      for (HudElement other : this.hudElements) {
         if (other != this.draggingHud && other.getElementWidth() > 0) {
            int ox = other.getElementX();
            int oy = other.getElementY();
            int ow = other.getElementWidth();
            int oh = other.getElementHeight();
            int minY = Math.min(dy, oy);
            int maxY = Math.max(dy + dh, oy + oh);
            int minX = Math.min(dx, ox);
            int maxX = Math.max(dx + dw, ox + ow);
            if (Math.abs(dx - ox) <= 2) {
               context.fill(ox, minY, ox + 1, maxY, -2147418113);
            }

            if (Math.abs(dx + dw - ox - ow) <= 2) {
               context.fill(ox + ow - 1, minY, ox + ow, maxY, -2130737152);
            }

            if (Math.abs(dy - oy) <= 2) {
               context.fill(minX, oy, maxX, oy + 1, -2147418113);
            }

            if (Math.abs(dy + dh - oy - oh) <= 2) {
               context.fill(minX, oy + oh - 1, maxX, oy + oh, -2147418113);
            }

            if (Math.abs(dx - ox - ow) <= 2) {
               context.fill(ox + ow, minY, ox + ow + 1, maxY, -2147418368);
            }

            if (Math.abs(dx + dw - ox) <= 2) {
               context.fill(ox - 1, minY, ox, maxY, -2147418368);
            }

            if (Math.abs(dy - oy - oh) <= 2) {
               context.fill(minX, oy + oh, maxX, oy + oh + 1, -2147418368);
            }

            if (Math.abs(dy + dh - oy) <= 2) {
               context.fill(minX, oy - 1, maxX, oy, -2147418368);
            }
         }
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         MOUSE_LEFT_HOLD = true;
      }

      if (this.isHudEditorMode()) {
         HudElement clicked = this.getHoveredHud((int)mouseX, (int)mouseY);
         if (button == 0 && clicked != null && clicked.isDraggable()) {
            this.draggingHud = clicked;
            this.dragOffsetX = (int)mouseX - clicked.getElementX();
            this.dragOffsetY = (int)mouseY - clicked.getElementY();
            return true;
         }

         if (button == 1 && clicked instanceof Module module) {
            module.toggle();
            return true;
         }
      }

      double localMouseX = this.toLocalMouseX(mouseX);
      double localMouseY = this.toLocalMouseY(mouseY);

      for (Frame frame : this.getVisibleFramesSnapshot()) {
         frame.mouseClicked(localMouseX, localMouseY, button);
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (button == 0) {
         MOUSE_LEFT_HOLD = false;
      }

      if (this.isHudEditorMode() && button == 0 && this.draggingHud != null) {
         this.persistHudElement(this.draggingHud);
         this.draggingHud = null;
         return true;
      } else {
         double localMouseX = this.toLocalMouseX(mouseX);
         double localMouseY = this.toLocalMouseY(mouseY);

         for (Frame frame : this.getVisibleFramesSnapshot()) {
            frame.mouseReleased(localMouseX, localMouseY, button);
         }

         return super.mouseReleased(mouseX, mouseY, button);
      }
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (this.isHudEditorMode() && button == 0 && this.draggingHud != null) {
         int nextX = (int)mouseX - this.dragOffsetX;
         int nextY = (int)mouseY - this.dragOffsetY;
         int dragWidth = this.draggingHud.getElementWidth();
         int dragHeight = this.draggingHud.getElementHeight();
         if (Math.abs(nextX) < 2) {
            nextX = 0;
         }

         if (Math.abs(nextY) < 2) {
            nextY = 0;
         }

         if (Math.abs(nextX + dragWidth - this.width) < 2) {
            nextX = this.width - dragWidth;
         }

         if (Math.abs(nextY + dragHeight - this.height) < 2) {
            nextY = this.height - dragHeight;
         }

         int centerX = (this.width - dragWidth) / 2;
         int centerY = (this.height - dragHeight) / 2;
         if (Math.abs(nextX - centerX) < 2) {
            nextX = centerX;
         }

         if (Math.abs(nextY - centerY) < 2) {
            nextY = centerY;
         }

         boolean snappedX = false;
         boolean snappedY = false;

         for (HudElement other : this.hudElements) {
            if (other != this.draggingHud && other.getElementWidth() > 0) {
               int ox = other.getElementX();
               int oy = other.getElementY();
               int ow = other.getElementWidth();
               int oh = other.getElementHeight();
               boolean verticallyAdjacent = Math.abs(nextY - oy - oh) < 2 || Math.abs(nextY + dragHeight - oy) < 2;
               if (verticallyAdjacent && !snappedX) {
                  int rightSnapRange = 6;
                  if (Math.abs(nextX + dragWidth - (ox + ow)) < rightSnapRange) {
                     nextX = ox + ow - dragWidth;
                     snappedX = true;
                  }

                  if (!snappedX && Math.abs(nextX - ox) < rightSnapRange) {
                     nextX = ox;
                     snappedX = true;
                  }
               }

               if (!snappedY && Math.abs(nextY - oy - oh) < 2) {
                  nextY = oy + oh;
                  snappedY = true;
               }

               if (!snappedY && Math.abs(nextY + dragHeight - oy) < 2) {
                  nextY = oy - dragHeight;
                  snappedY = true;
               }
            }
         }

         for (HudElement otherx : this.hudElements) {
            if (otherx != this.draggingHud && otherx.getElementWidth() > 0) {
               int oxx = otherx.getElementX();
               int oyx = otherx.getElementY();
               int owx = otherx.getElementWidth();
               int ohx = otherx.getElementHeight();
               if (!snappedX) {
                  if (Math.abs(nextX - oxx) < 2) {
                     nextX = oxx;
                     snappedX = true;
                  }

                  if (!snappedX && Math.abs(nextX + dragWidth - oxx - owx) < 2) {
                     nextX = oxx + owx - dragWidth;
                     snappedX = true;
                  }

                  if (!snappedX && Math.abs(nextX - oxx - owx) < 2) {
                     nextX = oxx + owx;
                     snappedX = true;
                  }

                  if (!snappedX && Math.abs(nextX + dragWidth - oxx) < 2) {
                     nextX = oxx - dragWidth;
                     snappedX = true;
                  }
               }

               if (!snappedY) {
                  if (Math.abs(nextY - oyx) < 2) {
                     nextY = oyx;
                     snappedY = true;
                  }

                  if (!snappedY && Math.abs(nextY + dragHeight - oyx - ohx) < 2) {
                     nextY = oyx + ohx - dragHeight;
                     snappedY = true;
                  }

                  if (!snappedY && Math.abs(nextY - oyx - ohx) < 2) {
                     nextY = oyx + ohx;
                     snappedY = true;
                  }

                  if (!snappedY && Math.abs(nextY + dragHeight - oyx) < 2) {
                     nextY = oyx - dragHeight;
                     snappedY = true;
                  }
               }
            }
         }

         if (this.draggingHud.getAlignMode() == HudElement.AlignMode.RIGHT) {
            this.forceSetLeftAlignX(this.draggingHud, nextX);
         } else {
            this.draggingHud.setElementX(nextX);
         }

         this.draggingHud.setElementY(nextY);
         return true;
      } else {
         return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
      }
   }

   private void updateAlignMode(HudElement hud) {
      int hudX = hud.getElementX();
      int hudWidth = hud.getElementWidth();
      int rightEdge = hudX + hudWidth;
      boolean hasRightAlignPartner = false;

      for (HudElement other : this.hudElements) {
         if (other != hud && other.getElementWidth() > 0) {
            int otherRightEdge = other.getElementX() + other.getElementWidth();
            if (Math.abs(rightEdge - otherRightEdge) <= 1) {
               hasRightAlignPartner = true;
               break;
            }
         }
      }

      if (!hasRightAlignPartner && hudX + hudWidth / 2 <= this.width / 2) {
         hud.setAlignMode(HudElement.AlignMode.LEFT);
      } else {
         hud.setAlignMode(HudElement.AlignMode.RIGHT);
      }
   }

   private void forceSetLeftAlignX(HudElement hud, int leftX) {
      try {
         Field hudXField = hud.getClass().getDeclaredField("hudX");
         hudXField.setAccessible(true);
         if (hud.getAlignMode() == HudElement.AlignMode.RIGHT) {
            hudXField.setInt(hud, leftX + hud.getElementWidth());
         } else {
            hudXField.setInt(hud, leftX);
         }
      } catch (Exception var4) {
         hud.setElementX(leftX);
      }
   }

   private void moveHudLeft(HudElement hud, int delta) {
      try {
         Field hudXField = hud.getClass().getDeclaredField("hudX");
         hudXField.setAccessible(true);
         int current = hudXField.getInt(hud);
         hudXField.setInt(hud, current - delta);
      } catch (Exception var5) {
         hud.setElementX(hud.getElementX() - delta);
      }
   }

   private HudElement getHoveredHud(int mouseX, int mouseY) {
      HudElement result = null;
      int minArea = Integer.MAX_VALUE;

      for (HudElement element : this.hudElements) {
         if (this.isOverHud(element, mouseX, mouseY)) {
            int area = element.getElementWidth() * element.getElementHeight();
            if (area > 0 && area < minArea) {
               minArea = area;
               result = element;
            }
         }
      }

      return result;
   }

   private boolean isOverHud(HudElement element, int mouseX, int mouseY) {
      int margin = 4;
      int x = element.getElementX();
      int y = element.getElementY();
      int width = element.getElementWidth();
      int height = element.getElementHeight();
      return width > 0 && height > 0 && mouseX >= x - margin && mouseX <= x + width + margin && mouseY >= y - margin && mouseY <= y + height + margin;
   }

   private void clampHud(HudElement element) {
      int clampedX = Math.max(0, Math.min(element.getElementX(), this.width - element.getElementWidth()));
      int clampedY = Math.max(0, Math.min(element.getElementY(), this.height - element.getElementHeight()));
      this.forceSetLeftAlignX(element, clampedX);
      element.setElementY(clampedY);
   }

   private void persistHudElement(HudElement element) {
      if (element != null) {
         this.clampHud(element);
         this.updateAlignMode(element);
         Vitality.save();
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (this.client != null && this.client.getWindow() != null) {
         long handle = this.client.getWindow().getHandle();
         boolean shiftPressed = GLFW.glfwGetKey(handle, 340) == 1;
         float scrollDelta = (float)verticalAmount * 15.0F / this.getRenderScale();
         if (shiftPressed) {
            for (Frame frame : this.getVisibleFramesSnapshot()) {
               frame.setX(frame.getX() + scrollDelta);
            }
         } else {
            for (Frame frame : this.getVisibleFramesSnapshot()) {
               frame.setY(frame.getY() + scrollDelta);
            }
         }

         return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
      } else {
         return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
      }
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      List<Frame> visibleFrames = this.getVisibleFramesSnapshot();
      boolean searchListening = visibleFrames.stream().anyMatch(Frame::isSearchListening);
      if (searchListening) {
         for (Frame frame : visibleFrames) {
            frame.keyPressed(keyCode, scanCode, modifiers);
         }

         return true;
      } else {
         boolean shouldCloseOnEsc = true;

         for (Frame frame : visibleFrames) {
            if (frame.isBindListening()) {
               shouldCloseOnEsc = false;
               break;
            }
         }

         for (Frame framex : visibleFrames) {
            framex.keyPressed(keyCode, scanCode, modifiers);
         }

         if (this.isHudEditorMode() && this.draggingHud != null && this.draggingHud.isDraggable()) {
            int step = (modifiers & 1) != 0 ? 10 : 1;
            switch (keyCode) {
               case 71:
                  this.showHudGuides = !this.showHudGuides;
                  return true;
               case 82:
                  this.forceSetLeftAlignX(this.draggingHud, (this.width - this.draggingHud.getElementWidth()) / 2);
                  this.draggingHud.setElementY((this.height - this.draggingHud.getElementHeight()) / 2);
                  this.persistHudElement(this.draggingHud);
                  return true;
               case 262:
                  this.moveHudLeft(this.draggingHud, -step);
                  this.persistHudElement(this.draggingHud);
                  return true;
               case 263:
                  this.moveHudLeft(this.draggingHud, step);
                  this.persistHudElement(this.draggingHud);
                  return true;
               case 264:
                  this.draggingHud.setElementY(this.draggingHud.getElementY() + step);
                  this.persistHudElement(this.draggingHud);
                  return true;
               case 265:
                  this.draggingHud.setElementY(this.draggingHud.getElementY() - step);
                  this.persistHudElement(this.draggingHud);
                  return true;
            }
         }

         return keyCode == 256 && !shouldCloseOnEsc ? true : super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      List<Frame> visibleFrames = this.getVisibleFramesSnapshot();
      if (visibleFrames.stream().anyMatch(Frame::isSearchListening)) {
         for (Frame frame : visibleFrames) {
            frame.charTyped(chr, modifiers);
         }

         return true;
      } else {
         for (Frame frame : visibleFrames) {
            frame.charTyped(chr, modifiers);
         }

         return super.charTyped(chr, modifiers);
      }
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   public boolean shouldCloseOnEsc() {
      List<Frame> visibleFrames = this.getVisibleFramesSnapshot();
      if (visibleFrames.stream().anyMatch(Frame::isSearchListening)) {
         return false;
      } else {
         for (Frame frame : visibleFrames) {
            if (frame.isBindListening()) {
               return false;
            }
         }

         return true;
      }
   }

   @Override
   public void close() {
      this.saveFramePositions();
      this.saveModuleExpansionStates();
      if (this.draggingHud != null) {
         this.persistHudElement(this.draggingHud);
      } else {
         Vitality.save();
      }

      this.draggingHud = null;
      this.frames.forEach(Frame::clearSearch);
      Screen target = this.returnScreen;
      this.returnScreen = null;
      if (this.client != null) {
         this.client.setScreen(target);
      } else {
         super.close();
      }
   }

   @Override
   public void resize(MinecraftClient client, int width, int height) {
      super.resize(client, width, height);
      this.setupFrames();
   }

   @Override
   protected void init() {
      super.init();
      this.setupFrames();
   }

   public List<Frame> getFrames() {
      return this.frames;
   }
}
