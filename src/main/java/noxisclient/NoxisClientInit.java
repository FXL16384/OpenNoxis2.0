package noxisclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.util.Identifier;
import noxisclient.license.GateRuntime;
import noxisclient.mod.modules.impl.render.LogoutSpots;
import noxisclient.mod.modules.impl.render.SpearFix;
import noxisclient.mod.modules.impl.render.mace.MaceParticles;

public class NoxisClientInit implements ClientModInitializer {
   @Override
   public void onInitializeClient() {
      GateRuntime.requireActive();
      ModelLoadingPlugin.register(
         pluginContext -> pluginContext.addModels(
               new Identifier[]{
                  Identifier.of("minecraft", "item/wooden_spear"),
                  Identifier.of("minecraft", "item/wooden_spear_in_hand"),
                  Identifier.of("minecraft", "item/stone_spear"),
                  Identifier.of("minecraft", "item/stone_spear_in_hand"),
                  Identifier.of("minecraft", "item/copper_spear"),
                  Identifier.of("minecraft", "item/copper_spear_in_hand"),
                  Identifier.of("minecraft", "item/golden_spear"),
                  Identifier.of("minecraft", "item/golden_spear_in_hand"),
                  Identifier.of("minecraft", "item/iron_spear"),
                  Identifier.of("minecraft", "item/iron_spear_in_hand"),
                  Identifier.of("minecraft", "item/diamond_spear"),
                  Identifier.of("minecraft", "item/diamond_spear_in_hand"),
                  Identifier.of("minecraft", "item/netherite_spear"),
                  Identifier.of("minecraft", "item/netherite_spear_in_hand"),
                  Identifier.of("minecraft", "item/spear_in_hand")
               }
            )
      );
      MaceParticles.register();
      SpearFix.registerSounds();
      LogoutSpots.registerSounds();
   }
}
