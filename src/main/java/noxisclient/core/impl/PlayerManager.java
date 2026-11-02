package noxisclient.core.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.block.Blocks;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import noxisclient.Alien;
import noxisclient.api.events.impl.BlockActivateEvent;
import noxisclient.api.events.impl.GameLeftEvent;
import noxisclient.api.events.impl.OpenScreenEvent;
import noxisclient.api.utils.Wrapper;
import noxisclient.api.utils.world.BlockPosX;
import noxisclient.mod.modules.Module;

public class PlayerManager implements Wrapper {
   private static final long ENDER_CHEST_OPEN_TIMEOUT_MS = 3000L;
   public static Screen screenToOpen;
   public final DefaultedList<ItemStack> ENDERCHEST_ITEM = DefaultedList.ofSize(27, ItemStack.EMPTY);
   public final Map<PlayerEntity, PlayerManager.EntityAttribute> map = new ConcurrentHashMap<>();
   public final CopyOnWriteArrayList<PlayerEntity> inWebPlayers = new CopyOnWriteArrayList<>();
   public boolean known = false;
   private int echestOpenedState;
   private long echestOpenMarkedAt;

   public PlayerManager() {
      Alien.EVENT_BUS.subscribe(this);
   }

   public void markEnderChestOpening() {
      this.echestOpenedState = 1;
      this.echestOpenMarkedAt = System.currentTimeMillis();
   }

   private void resetEnderChestOpening() {
      this.echestOpenedState = 0;
      this.echestOpenMarkedAt = 0L;
   }

   public void onLogout(GameLeftEvent event) {
      this.inWebPlayers.clear();
      this.map.clear();
      this.ENDERCHEST_ITEM.clear();
      this.known = false;
      this.resetEnderChestOpening();
   }

   public void onBlockActivate(BlockActivateEvent event) {
      if (event.blockState.getBlock() instanceof EnderChestBlock && this.echestOpenedState == 0) {
         this.markEnderChestOpening();
      }
   }

   public void onOpenScreenEvent(OpenScreenEvent event) {
      if (this.echestOpenedState == 1) {
         if (System.currentTimeMillis() - this.echestOpenMarkedAt > 3000L) {
            this.resetEnderChestOpening();
         } else if (event.screen instanceof GenericContainerScreen) {
            this.echestOpenedState = 2;
         } else {
            this.resetEnderChestOpening();
         }
      } else if (this.echestOpenedState != 0) {
         if (!(mc.currentScreen instanceof GenericContainerScreen)) {
            this.resetEnderChestOpening();
         } else {
            GenericContainerScreenHandler container = ((GenericContainerScreen)mc.currentScreen).getScreenHandler();
            if (container == null) {
               this.resetEnderChestOpening();
            } else {
               Inventory inv = container.getInventory();

               for (int i = 0; i < 27; i++) {
                  this.ENDERCHEST_ITEM.set(i, inv.getStack(i));
               }

               this.known = true;
               this.resetEnderChestOpening();
            }
         }
      }
   }

   public void onUpdate() {
      if (!Module.nullCheck()) {
         if (screenToOpen != null && mc.currentScreen == null) {
            mc.setScreen(screenToOpen);
            screenToOpen = null;
         }

         this.inWebPlayers.clear();

         for (PlayerEntity playerEntity : Alien.THREAD.getPlayers()) {
            this.map
               .put(
                  playerEntity,
                  new PlayerManager.EntityAttribute(playerEntity.getArmor(), playerEntity.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS))
               );
            this.webUpdate(playerEntity);
         }
      }
   }

   public boolean isInWeb(PlayerEntity player) {
      return this.inWebPlayers.contains(player);
   }

   private void webUpdate(PlayerEntity player) {
      for (float x : new float[]{0.0F, 0.3F, -0.3F}) {
         for (float z : new float[]{0.0F, 0.3F, -0.3F}) {
            for (int y : new int[]{-1, 0, 1, 2}) {
               BlockPos pos = new BlockPosX(player.getX() + (double)x, player.getY(), player.getZ() + (double)z).up(y);
               if (new Box(pos).intersects(player.getBoundingBox()) && mc.world.getBlockState(pos).getBlock() == Blocks.COBWEB) {
                  this.inWebPlayers.add(player);
                  return;
               }
            }
         }
      }
   }

   public static record EntityAttribute(int armor, double toughness) {
   }
}
