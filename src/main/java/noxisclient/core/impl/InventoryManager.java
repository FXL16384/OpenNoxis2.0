package noxisclient.core.impl;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;
import noxisclient.Vitality;
import noxisclient.api.events.impl.ClientTickEvent;
import noxisclient.api.events.impl.DeathEvent;
import noxisclient.api.events.impl.GameLeftEvent;
import noxisclient.api.events.impl.PacketEvent;
import noxisclient.api.utils.Wrapper;
import noxisclient.api.utils.math.Timer;
import noxisclient.asm.accessors.IBundlePacket;
import noxisclient.mod.modules.impl.client.AntiCheat;

public class InventoryManager implements Wrapper {
   private static final String LEGACY_OWNER = "Legacy";
   private static final long SILENT_SWAP_TIMEOUT_MS = 1000L;
   private final List<InventoryManager.PreSwapData> swapData = new CopyOnWriteArrayList<>();
   private int slot = -1;
   private boolean forcingSlot;
   private String silentSwapOwner;
   private InventoryManager.SwapPriority silentSwapPriority = InventoryManager.SwapPriority.LOW;
   private long silentSwapTime;
   private String preferredSilentSwapOwner;
   private int preferredSilentSwapSlot = -1;
   private InventoryManager.SwapPriority preferredSilentSwapPriority = InventoryManager.SwapPriority.LOW;
   private long preferredSilentSwapTime;
   private int alienClientReturnSlot = -1;

   public InventoryManager() {
      Vitality.EVENT_BUS.subscribe(this);
   }

   public void onPacketOutbound(PacketEvent.Send event) {
      if (!event.isCancelled()) {
         if (event.getPacket() instanceof UpdateSelectedSlotC2SPacket packet) {
            int packetSlot = packet.getSelectedSlot();
            if (!PlayerInventory.isValidHotbarIndex(packetSlot)) {
               event.cancel();
               return;
            }

            if (!this.forcingSlot && this.slot == packetSlot) {
               event.cancel();
               return;
            }

            this.slot = packetSlot;
         }
      }
   }

   public void onPacketInbound(PacketEvent.Receive event) {
      if (mc.player != null && !event.isCancelled()) {
         Packet<?> packet = event.getPacket();
         if (packet instanceof UpdateSelectedSlotS2CPacket slotPacket) {
            this.slot = slotPacket.getSlot();
         } else {
            if (packet instanceof BundleS2CPacket bundle) {
               List<Packet<? super ClientPlayPacketListener>> packets = new ArrayList<>();

               for (Packet<? super ClientPlayPacketListener> bundledPacket : bundle.getPackets()) {
                  if (!this.shouldCancelSlotUpdate(bundledPacket)) {
                     packets.add(bundledPacket);
                  }
               }

               ((IBundlePacket)bundle).setPackets(packets);
            } else if (this.shouldCancelSlotUpdate(packet)) {
               event.cancel();
            }
         }
      }
   }

   public void onTick(ClientTickEvent event) {
      this.swapData.removeIf(InventoryManager.PreSwapData::isPassedClearTime);
      if (this.silentSwapOwner != null && System.currentTimeMillis() - this.silentSwapTime > 1000L) {
         if (this.alienClientReturnSlot != -1 && this.isAlienSilentSwapEnabled() && !this.isPreferredSilentSwapActive()) {
            this.syncAlienClientSlot();
            return;
         }

         this.silentSwapOwner = null;
         this.silentSwapPriority = InventoryManager.SwapPriority.LOW;
      }

      if (this.preferredSilentSwapOwner != null && System.currentTimeMillis() - this.preferredSilentSwapTime > 1000L) {
         this.releasePreferredSilentSwap(this.preferredSilentSwapOwner);
      }
   }

   public void onDeath(DeathEvent event) {
      if (event.getPlayer() == mc.player) {
         this.forceSyncToClient();
      }
   }

   public void onGameLeft(GameLeftEvent event) {
      this.slot = -1;
      this.swapData.clear();
      this.silentSwapOwner = null;
      this.silentSwapPriority = InventoryManager.SwapPriority.LOW;
      this.preferredSilentSwapOwner = null;
      this.preferredSilentSwapSlot = -1;
      this.preferredSilentSwapPriority = InventoryManager.SwapPriority.LOW;
      this.alienClientReturnSlot = -1;
   }

   public void setSlot(int barSlot) {
      this.trySetSlot(barSlot, InventoryManager.SwapPriority.NORMAL, "Legacy");
   }

   public boolean trySetSlot(int barSlot, InventoryManager.SwapPriority priority, String owner) {
      if (this.isAlienSilentSwapEnabled() && !this.isPreferredSilentSwapActive()) {
         return this.setAlienClientSlot(barSlot, priority, owner);
      } else if (mc.player == null || !PlayerInventory.isValidHotbarIndex(barSlot) || this.slot == barSlot) {
         return this.tryAcquireSilentSwap(owner, priority);
      } else if (!this.tryAcquireSilentSwap(owner, priority)) {
         return false;
      } else {
         this.setTrackedServerSlot(barSlot);
         return true;
      }
   }

   public boolean holdPreferredSilentSwap(int barSlot, InventoryManager.SwapPriority priority, String owner) {
      if (mc.player != null && PlayerInventory.isValidHotbarIndex(barSlot)) {
         String safeOwner = this.getSafeOwner(owner);
         this.preferredSilentSwapOwner = safeOwner;
         this.preferredSilentSwapSlot = barSlot;
         this.preferredSilentSwapPriority = priority;
         this.preferredSilentSwapTime = System.currentTimeMillis();
         return this.trySetSlot(barSlot, priority, safeOwner);
      } else {
         return false;
      }
   }

   public void releasePreferredSilentSwap(String owner) {
      String safeOwner = this.getSafeOwner(owner);
      if (this.preferredSilentSwapOwner != null) {
         if (this.preferredSilentSwapOwner.equals(safeOwner)) {
            this.preferredSilentSwapOwner = null;
            this.preferredSilentSwapSlot = -1;
            this.preferredSilentSwapPriority = InventoryManager.SwapPriority.LOW;
            if (this.silentSwapOwner == null || this.silentSwapOwner.equals(safeOwner)) {
               this.forceSyncToClient();
            }
         }
      }
   }

   private void setTrackedServerSlot(int barSlot) {
      ItemStack[] hotbarCopy = new ItemStack[9];

      for (int i = 0; i < hotbarCopy.length; i++) {
         hotbarCopy[i] = mc.player.getInventory().getStack(i).copy();
      }

      int starting = this.slot;
      this.setSlotForced(barSlot);
      this.swapData.add(new InventoryManager.PreSwapData(hotbarCopy, starting, barSlot));
   }

   private boolean setAlienClientSlot(int barSlot, InventoryManager.SwapPriority priority, String owner) {
      if (mc.player == null || !PlayerInventory.isValidHotbarIndex(barSlot)) {
         return false;
      } else if (!this.tryAcquireSilentSwap(owner, priority)) {
         return false;
      } else {
         if (this.alienClientReturnSlot == -1) {
            this.alienClientReturnSlot = mc.player.getInventory().selectedSlot;
         }

         this.switchAlienClientSlot(barSlot);
         return true;
      }
   }

   public boolean tryAcquireSilentSwap(String owner, InventoryManager.SwapPriority priority) {
      if (mc.player == null) {
         return false;
      } else {
         String safeOwner = this.getSafeOwner(owner);
         if (this.isPreferredSilentSwapActive() && !this.preferredSilentSwapOwner.equals(safeOwner) && priority == InventoryManager.SwapPriority.LOW) {
            return false;
         } else if (this.silentSwapOwner != null
            && !this.silentSwapOwner.equals(safeOwner)
            && !this.silentSwapOwner.equals(this.preferredSilentSwapOwner)
            && (this.preferredSilentSwapOwner == null || !this.preferredSilentSwapOwner.equals(safeOwner))
            && priority.getLevel() < this.silentSwapPriority.getLevel()) {
            return false;
         } else {
            this.silentSwapOwner = safeOwner;
            this.silentSwapPriority = priority;
            this.silentSwapTime = System.currentTimeMillis();
            return true;
         }
      }
   }

   public void setSlotAlt(int barSlot) {
      if (mc.player != null && mc.interactionManager != null && PlayerInventory.isValidHotbarIndex(barSlot)) {
         mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, barSlot + 36, this.slot, SlotActionType.SWAP, mc.player);
      }
   }

   public void setClientSlot(int barSlot) {
      if (mc.player != null && PlayerInventory.isValidHotbarIndex(barSlot) && mc.player.getInventory().selectedSlot != barSlot) {
         mc.player.getInventory().selectedSlot = barSlot;
         this.setSlotForced(barSlot);
      }
   }

   public void setSlotForced(int barSlot) {
      if (mc.getNetworkHandler() != null && PlayerInventory.isValidHotbarIndex(barSlot)) {
         this.forcingSlot = true;

         try {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(barSlot));
         } finally {
            this.forcingSlot = false;
         }
      }
   }

   public void syncToClient() {
      this.syncToClient("Legacy");
   }

   public boolean syncToClient(String owner) {
      String safeOwner = this.getSafeOwner(owner);
      if (this.silentSwapOwner != null && !this.silentSwapOwner.equals(safeOwner)) {
         return false;
      } else if (this.isAlienSilentSwapEnabled() && !this.isPreferredSilentSwapActive()) {
         this.syncAlienClientSlot();
         return true;
      } else if (this.isPreferredSilentSwapActive() && !this.preferredSilentSwapOwner.equals(safeOwner)) {
         this.restorePreferredSilentSwap();
         return true;
      } else {
         this.forceSyncToClient();
         return true;
      }
   }

   public void forceSyncToClient() {
      if (this.isAlienSilentSwapEnabled() && !this.isPreferredSilentSwapActive() && this.alienClientReturnSlot != -1) {
         this.syncAlienClientSlot();
      } else if (mc.player != null && this.isDesynced()) {
         this.setSlotForced(mc.player.getInventory().selectedSlot);

         for (InventoryManager.PreSwapData data : this.swapData) {
            data.beginClear();
         }

         this.silentSwapOwner = null;
         this.silentSwapPriority = InventoryManager.SwapPriority.LOW;
         this.clearPreferredSilentSwap();
         this.alienClientReturnSlot = -1;
      } else {
         this.silentSwapOwner = null;
         this.silentSwapPriority = InventoryManager.SwapPriority.LOW;
         this.clearPreferredSilentSwap();
         this.alienClientReturnSlot = -1;
      }
   }

   private void syncAlienClientSlot() {
      if (mc.player != null && PlayerInventory.isValidHotbarIndex(this.alienClientReturnSlot)) {
         this.switchAlienClientSlot(this.alienClientReturnSlot);
      }

      this.alienClientReturnSlot = -1;
      this.silentSwapOwner = null;
      this.silentSwapPriority = InventoryManager.SwapPriority.LOW;
   }

   private void switchAlienClientSlot(int barSlot) {
      mc.player.getInventory().selectedSlot = barSlot;
      this.setSlotForced(barSlot);
   }

   private void restorePreferredSilentSwap() {
      if (mc.player != null && PlayerInventory.isValidHotbarIndex(this.preferredSilentSwapSlot)) {
         if (this.slot != this.preferredSilentSwapSlot) {
            this.setTrackedServerSlot(this.preferredSilentSwapSlot);
         }

         this.silentSwapOwner = this.preferredSilentSwapOwner;
         this.silentSwapPriority = this.preferredSilentSwapPriority;
         this.silentSwapTime = System.currentTimeMillis();
         this.preferredSilentSwapTime = this.silentSwapTime;
      } else {
         this.silentSwapOwner = null;
         this.silentSwapPriority = InventoryManager.SwapPriority.LOW;
      }
   }

   public boolean ownsSilentSwap(String owner) {
      String safeOwner = this.getSafeOwner(owner);
      return this.silentSwapOwner == null || this.silentSwapOwner.equals(safeOwner);
   }

   public boolean isSilentSwapLockEnabled() {
      return this.isPreferredSilentSwapActive() || this.silentSwapOwner != null;
   }

   public boolean isShorelineSilentSwapEnabled() {
      return this.getSilentSwapMode() == AntiCheat.SilentSwapMode.Shoreline;
   }

   public boolean isDefaultSilentSwapEnabled() {
      return false;
   }

   public boolean isMioSilentSwapEnabled() {
      return false;
   }

   public boolean isAlienSilentSwapEnabled() {
      return this.getSilentSwapMode() == AntiCheat.SilentSwapMode.Alien;
   }

   public AntiCheat.SilentSwapMode getSilentSwapMode() {
      return AntiCheat.INSTANCE == null ? AntiCheat.SilentSwapMode.Shoreline : AntiCheat.INSTANCE.silentSwapMode.getValue();
   }

   public String getSilentSwapOwner() {
      return this.silentSwapOwner;
   }

   public InventoryManager.SwapPriority getSilentSwapPriority() {
      return this.silentSwapPriority;
   }

   private boolean isPreferredSilentSwapActive() {
      return this.preferredSilentSwapOwner != null && PlayerInventory.isValidHotbarIndex(this.preferredSilentSwapSlot);
   }

   private String getSafeOwner(String owner) {
      return owner != null && !owner.isBlank() ? owner : "Legacy";
   }

   private void clearPreferredSilentSwap() {
      this.preferredSilentSwapOwner = null;
      this.preferredSilentSwapSlot = -1;
      this.preferredSilentSwapPriority = InventoryManager.SwapPriority.LOW;
   }

   public boolean isDesynced() {
      return mc.player != null && mc.player.getInventory().selectedSlot != this.slot;
   }

   public void closeScreen() {
      if (mc.player != null && mc.getNetworkHandler() != null) {
         mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
      }
   }

   public void inventorySwap(int slot, int selectedSlot) {
      if (mc.player != null) {
         if (slot - 36 != selectedSlot) {
            this.click(slot, selectedSlot, SlotActionType.SWAP);
         }
      }
   }

   public int pickupSlot(int slot) {
      return this.click(slot, 0, SlotActionType.PICKUP);
   }

   public void quickMove(int slot) {
      this.click(slot, 0, SlotActionType.QUICK_MOVE);
   }

   public void throwSlot(int slot) {
      this.click(slot, 0, SlotActionType.THROW);
   }

   public int click(int slot, int button, SlotActionType type) {
      if (mc.player != null && slot >= 0) {
         ScreenHandler screenHandler = mc.player.currentScreenHandler;
         DefaultedList<Slot> defaultedList = screenHandler.slots;
         int size = defaultedList.size();
         List<ItemStack> stacks = new ArrayList<>(size);

         for (Slot screenSlot : defaultedList) {
            stacks.add(screenSlot.getStack().copy());
         }

         screenHandler.onSlotClick(slot, button, type, mc.player);
         Int2ObjectOpenHashMap<ItemStack> changedStacks = new Int2ObjectOpenHashMap<>();

         for (int i = 0; i < size; i++) {
            ItemStack oldStack = stacks.get(i);
            ItemStack newStack = defaultedList.get(i).getStack();
            if (!ItemStack.areEqual(oldStack, newStack)) {
               changedStacks.put(i, newStack.copy());
            }
         }

         mc.player
            .networkHandler
            .sendPacket(
               new ClickSlotC2SPacket(
                  screenHandler.syncId, screenHandler.getRevision(), slot, button, type, screenHandler.getCursorStack().copy(), changedStacks
               )
            );
         return screenHandler.getRevision();
      } else {
         return -1;
      }
   }

   public int getServerSlot() {
      return this.slot;
   }

   public int getClientSlot() {
      return mc.player == null ? -1 : mc.player.getInventory().selectedSlot;
   }

   public ItemStack getServerItem() {
      return mc.player != null && PlayerInventory.isValidHotbarIndex(this.slot) ? mc.player.getInventory().getStack(this.slot) : ItemStack.EMPTY;
   }

   private boolean shouldCancelSlotUpdate(Packet<?> packet) {
      if (!(packet instanceof ScreenHandlerSlotUpdateS2CPacket slotPacket)) {
         return false;
      } else {
         int hotbarSlot = slotPacket.getSlot() - 36;
         if (PlayerInventory.isValidHotbarIndex(hotbarSlot) && !slotPacket.getStack().isEmpty()) {
            for (InventoryManager.PreSwapData data : this.swapData) {
               if (data.getSlot() == hotbarSlot || data.getStarting() == hotbarSlot) {
                  ItemStack preStack = data.getPreHolding(hotbarSlot);
                  if (!this.isEqual(preStack, slotPacket.getStack())) {
                     return true;
                  }
               }
            }

            return false;
         } else {
            return false;
         }
      }
   }

   private boolean isEqual(ItemStack stack1, ItemStack stack2) {
      return stack1.getItem().equals(stack2.getItem()) && stack1.getName().equals(stack2.getName());
   }

   public static class PreSwapData {
      private final ItemStack[] preHotbar;
      private final int starting;
      private final int swapTo;
      private Timer clearTime;

      public PreSwapData(ItemStack[] preHotbar, int starting, int swapTo) {
         this.preHotbar = preHotbar;
         this.starting = starting;
         this.swapTo = swapTo;
      }

      public void beginClear() {
         this.clearTime = new Timer();
         this.clearTime.reset();
      }

      public boolean isPassedClearTime() {
         return this.clearTime != null && this.clearTime.passed(300L);
      }

      public ItemStack getPreHolding(int slot) {
         return this.preHotbar[slot];
      }

      public int getStarting() {
         return this.starting;
      }

      public int getSlot() {
         return this.swapTo;
      }
   }

   public static enum SwapPriority {
      LOW(0),
      NORMAL(50),
      HIGH(100),
      CRITICAL(200);

      private final int level;

      private SwapPriority(int level) {
         this.level = level;
      }

      public int getLevel() {
         return this.level;
      }
   }
}
