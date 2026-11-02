package noxisclient.core.impl;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import noxisclient.Alien;
import noxisclient.Vitality;
import noxisclient.api.events.impl.ClientTickEvent;
import noxisclient.api.events.impl.UpdateEvent;
import noxisclient.api.utils.Wrapper;
import noxisclient.api.utils.render.JelloUtil;
import noxisclient.api.utils.world.BlockUtil;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.client.ClientSetting;
import noxisclient.mod.modules.impl.combat.AutoAnchor;
import noxisclient.mod.modules.impl.combat.AutoCrystal;
import noxisclient.mod.modules.impl.render.HoleESP;
import noxisclient.mod.modules.impl.render.PlaceRender;

public class ThreadManager implements Wrapper {
   public static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
   public static ThreadManager.ClientService clientService;
   public volatile Iterable<Entity> threadSafeEntityList = Collections.emptyList();
   public volatile List<AbstractClientPlayerEntity> threadSafePlayersList = Collections.emptyList();
   public volatile boolean tickRunning = false;

   public ThreadManager() {
      this.init();
   }

   public void init() {
      Vitality.EVENT_BUS.subscribe(this);
      clientService = new ThreadManager.ClientService();
      clientService.setName("AlienClientService");
      clientService.setDaemon(true);
      clientService.start();
   }

   public Iterable<Entity> getEntities() {
      return this.threadSafeEntityList;
   }

   public List<AbstractClientPlayerEntity> getPlayers() {
      return this.threadSafePlayersList;
   }

   public void execute(Runnable runnable) {
      EXECUTOR.execute(runnable);
   }

   public void onEvent(ClientTickEvent event) {
      Vitality.POP.onUpdate();
      Vitality.SERVER.onUpdate();
      if (event.isPre()) {
         JelloUtil.updateJello();
         this.tickRunning = true;
         BlockUtil.placedPos.forEach(pos -> PlaceRender.INSTANCE.create(pos));
         BlockUtil.placedPos.clear();
         Alien.PLAYER.onUpdate();
         if (!Module.nullCheck()) {
            Alien.EVENT_BUS.post(UpdateEvent.INSTANCE);
         }
      } else {
         this.tickRunning = false;
         if (mc.world == null || mc.player == null) {
            return;
         }

         this.threadSafeEntityList = Lists.newArrayList(mc.world.getEntities());
         this.threadSafePlayersList = Lists.newArrayList(mc.world.getPlayers());
      }

      if (!clientService.isAlive() || clientService.isInterrupted()) {
         clientService = new ThreadManager.ClientService();
         clientService.setName("AlienService");
         clientService.setDaemon(true);
         clientService.start();
      }
   }

   public class ClientService extends Thread {
      @Override
      public void run() {
         while (true) {
            try {
               if (ThreadManager.this.tickRunning) {
                  Thread.onSpinWait();
               } else {
                  AutoCrystal.INSTANCE.onThread();
                  HoleESP.INSTANCE.onThread();
                  AutoAnchor.INSTANCE.onThread();
               }
            } catch (Exception var2) {
               var2.printStackTrace();
               if (ClientSetting.INSTANCE.debug.getValue()) {
                  CommandManager.sendMessage("§4An error has occurred [Thread] Message: [" + var2.getMessage() + "]");
               }
            }
         }
      }
   }
}
