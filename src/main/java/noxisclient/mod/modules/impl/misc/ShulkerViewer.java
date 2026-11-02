package noxisclient.mod.modules.impl.misc;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.collection.DefaultedList;
import noxisclient.Alien;
import noxisclient.api.events.impl.UpdateEvent;
import noxisclient.api.utils.math.Timer;
import noxisclient.api.utils.render.Render2DUtil;
import noxisclient.asm.accessors.IContainerComponent;
import noxisclient.core.impl.PlayerManager;
import noxisclient.mod.gui.PeekScreen;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.client.ClickGui;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.ColorSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Unique;

public class ShulkerViewer extends Module {
   private static final ItemStack[] ITEMS = new ItemStack[27];
   public static ShulkerViewer INSTANCE;
   private static int offset;
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   public final BooleanSetting toolTips = this.add(new BooleanSetting("ToolTips", true));
   public final BooleanSetting icon = this.add(new BooleanSetting("Icon", true));
   private final HashMap<UUID, ShulkerViewer.Shulker> map = new HashMap<>();
   private final BooleanSetting peek = this.add(new BooleanSetting("Peek", false).setParent());
   private final SliderSetting renderTime = this.add(new SliderSetting("RenderTime", 10.0, 0.0, 100.0, 0.1, this.peek::isOpen).setSuffix("s"));
   private final SliderSetting xOffset = this.add(new SliderSetting("X", 0, 0, 1500, this.peek::isOpen));
   private final SliderSetting yOffset = this.add(new SliderSetting("Y", 120, 0, 1000, this.peek::isOpen));
   private final SliderSetting space = this.add(new SliderSetting("Space", 78.0, 0.0, 200.0, 1.0, this.peek::isOpen));
   private final ColorSetting innerBackground = this.add(new ColorSetting("Background", new Color(18, 18, 18, 236)));

   public ShulkerViewer() {
      super("ShulkerViewer", Module.Category.Misc);
      this.setChinese("潜影盒查");
      INSTANCE = this;
   }

   public static void renderShulkerToolTip(DrawContext context, int mouseX, int mouseY, ItemStack stack) {
      getItemsInContainerItem(stack, ITEMS);
      draw(context, mouseX, mouseY);
   }

   @Unique
   private static void draw(DrawContext context, int mouseX, int mouseY) {
      if (mc != null && mc.textRenderer != null) {
         RenderSystem.disableDepthTest();
         GL11.glClear(256);
         mouseX += 8;
         mouseY -= 82;
         int cols = 9;
         int rows = 3;
         int slotSize = 18;
         int outerPad = 6;
         int innerPad = 4;
         int titleHeight = 9 + 6;
         int width = cols * slotSize + outerPad * 2;
         int height = titleHeight + rows * slotSize + outerPad + innerPad;
         float radius = 6.0F;
         ClickGui gui = ClickGui.getInstance();
         int backgroundColor = gui != null ? gui.defaultColor.getValue().getRGB() : new Color(30, 30, 30, 236).getRGB();
         int innerColor = INSTANCE != null ? INSTANCE.innerBackground.getValue().getRGB() : new Color(18, 18, 18, 236).getRGB();
         Render2DUtil.drawRoundedRect(context.getMatrices(), (float)mouseX, (float)mouseY, (float)width, (float)height, radius, backgroundColor);
         Render2DUtil.drawRoundedRect(
            context.getMatrices(),
            (float)mouseX + 2.0F,
            (float)(mouseY + titleHeight) - 1.0F,
            (float)width - 4.0F,
            (float)(height - titleHeight) - 1.0F,
            Math.max(0.0F, radius - 2.0F),
            innerColor
         );
         context.drawText(mc.textRenderer, "Shulker", mouseX + outerPad, mouseY + 3, -1, true);
         DiffuseLighting.enableGuiDepthLighting();
         int row = 0;
         int i = 0;

         for (ItemStack itemStack : ITEMS) {
            int itemX = mouseX + outerPad + i * slotSize;
            int itemY = mouseY + titleHeight + innerPad + row * slotSize;
            context.drawItem(itemStack, itemX, itemY);
            context.drawItemInSlot(mc.textRenderer, itemStack, itemX, itemY);
            if (++i >= 9) {
               i = 0;
               row++;
            }
         }

         DiffuseLighting.disableGuiDepthLighting();
         RenderSystem.enableDepthTest();
      }
   }

   public static boolean hasItems(ItemStack itemStack) {
      Object containerObj = itemStack.get(DataComponentTypes.CONTAINER);
      if (containerObj != null) {
         try {
            IContainerComponent container = (IContainerComponent)containerObj;
            if (!container.getStacks().isEmpty()) {
               return true;
            }
         } catch (ClassCastException var3) {
         }
      }

      NbtCompound compoundTag = itemStack.getOrDefault(DataComponentTypes.BLOCK_ENTITY_DATA, NbtComponent.DEFAULT).getNbt();
      return compoundTag != null && compoundTag.contains("Items", 9);
   }

   public static void getItemsInContainerItem(ItemStack itemStack, ItemStack[] items) {
      if (itemStack.getItem() == Items.ENDER_CHEST) {
         for (int i = 0; i < Alien.PLAYER.ENDERCHEST_ITEM.size(); i++) {
            items[i] = Alien.PLAYER.ENDERCHEST_ITEM.get(i);
         }
      } else {
         Arrays.fill(items, ItemStack.EMPTY);
         ComponentMap components = itemStack.getComponents();
         if (components.contains(DataComponentTypes.CONTAINER)) {
            Object containerObj = components.get(DataComponentTypes.CONTAINER);

            try {
               IContainerComponent container = (IContainerComponent)containerObj;
               DefaultedList<ItemStack> stacks = container.getStacks();

               for (int i = 0; i < stacks.size(); i++) {
                  if (i >= 0 && i < items.length) {
                     items[i] = stacks.get(i);
                  }
               }
            } catch (ClassCastException var7) {
            }
         } else if (components.contains(DataComponentTypes.BLOCK_ENTITY_DATA)) {
            NbtComponent nbt2 = components.get(DataComponentTypes.BLOCK_ENTITY_DATA);
            if (nbt2.contains("Items")) {
               NbtList nbt3 = (NbtList)nbt2.getNbt().get("Items");

               for (int ix = 0; ix < nbt3.size(); ix++) {
                  int slot = nbt3.getCompound(ix).getByte("Slot");
                  if (slot >= 0 && slot < items.length && mc != null && mc.player != null) {
                     items[slot] = ItemStack.fromNbtOrEmpty(mc.player.getRegistryManager(), nbt3.getCompound(ix));
                  }
               }
            }
         }
      }
   }

   public static boolean openContainer(ItemStack itemStack, ItemStack[] contents, boolean pause) {
      if (!hasItems(itemStack) && itemStack.getItem() != Items.ENDER_CHEST) {
         return false;
      } else {
         getItemsInContainerItem(itemStack, contents);
         if (pause) {
            PlayerManager.screenToOpen = new PeekScreen(itemStack, contents);
         } else if (mc != null) {
            mc.setScreen(new PeekScreen(itemStack, contents));
         }

         return true;
      }
   }

   public void onUpdate(UpdateEvent event) {
      if (this.peek.getValue()) {
         for (AbstractClientPlayerEntity player : Alien.THREAD.getPlayers()) {
            ItemStack stack = player.getMainHandStack();
            if (!(stack.getItem() instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof ShulkerBoxBlock)) {
               stack = player.getOffHandStack();
            }

            if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
               this.map.put(player.getGameProfile().getId(), new ShulkerViewer.Shulker(stack, player.getGameProfile().getName()));
            }
         }
      }
   }

   @Override
   public void onRender2D(DrawContext drawContext, float tickDelta) {
      if (this.peek.getValue()) {
         offset = 0;
         this.map.values().removeIf(shulker -> shulker.draw(drawContext));
      }
   }

   class Shulker {
      final ItemStack itemStack;
      final String name;
      private final Timer timer;

      public Shulker(ItemStack itemStack, String name) {
         this.itemStack = itemStack;
         this.timer = new Timer();
         this.name = name;
      }

      public boolean draw(DrawContext context) {
         if (!this.timer.passedS(ShulkerViewer.this.renderTime.getValue()) && ShulkerViewer.mc != null && ShulkerViewer.mc.textRenderer != null) {
            ShulkerViewer.renderShulkerToolTip(
               context, ShulkerViewer.this.xOffset.getValueInt() - 8, ShulkerViewer.this.yOffset.getValueInt() + ShulkerViewer.offset, this.itemStack
            );
            context.drawText(
               ShulkerViewer.mc.textRenderer,
               this.name,
               ShulkerViewer.this.xOffset.getValueInt(),
               ShulkerViewer.this.yOffset.getValueInt() + ShulkerViewer.offset - 9 - 82,
               -1,
               true
            );
            ShulkerViewer.offset = ShulkerViewer.offset + ShulkerViewer.this.space.getValueInt();
            return false;
         } else {
            return true;
         }
      }
   }
}
