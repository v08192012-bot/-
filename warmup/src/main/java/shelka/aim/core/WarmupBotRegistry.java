package shelka.aim.core;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EntityType.Builder;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import shelka.aim.render.WarmupBotRenderer;

public final class WarmupBotRegistry {
   public static final String MOD_ID = "shelka_aim";
   public static EntityType<WarmupBotEntity> BOT;

   private WarmupBotRegistry() {
   }

   public static void register() {
      if (BOT != null) {
         return;
      }

      Identifier id = Identifier.of(MOD_ID, "warmup_bot");
      RegistryKey<EntityType<?>> key = RegistryKey.of(RegistryKeys.ENTITY_TYPE, id);
      BOT = Registry.register(
         Registries.ENTITY_TYPE,
         key,
         Builder.create(WarmupBotEntity::new, SpawnGroup.MISC)
            .dimensions(0.6F, 1.8F)
            .maxTrackingRange(64)
            .trackingTickInterval(1)
            .build(key)
      );
      FabricDefaultAttributeRegistry.register(BOT, WarmupBotEntity.createBotAttributes().build());
      EntityRendererRegistry.register(BOT, WarmupBotRenderer::new);
   }
}
