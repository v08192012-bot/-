package brokke.cc.core;

import brokke.cc.render.FakePlayerEntityRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.EntityType.Builder;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class FakePlayerRegistry {
   public static EntityType<FakePlayerEntity> FAKE_PLAYER;

   public static void a() {
      Identifier var0 = Identifier.of("brokke", "fake_player");
      RegistryKey var1 = RegistryKey.of(RegistryKeys.ENTITY_TYPE, var0);
      FAKE_PLAYER = Registry.register(
         Registries.ENTITY_TYPE,
         var1,
         Builder.create(FakePlayerEntity::new, SpawnGroup.MISC).dimensions(0.6F, 1.8F).maxTrackingRange(64).trackingTickInterval(1).build(var1)
      );
      FabricDefaultAttributeRegistry.register(FAKE_PLAYER, FakePlayerEntity.a().build());
      EntityRendererRegistry.register(FAKE_PLAYER, FakePlayerEntityRenderer::new);
   }
}
