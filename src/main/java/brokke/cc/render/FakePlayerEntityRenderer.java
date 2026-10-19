package brokke.cc.render;

import brokke.cc.core.FakePlayerEntity;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.Identifier;

public class FakePlayerEntityRenderer extends LivingEntityRenderer<FakePlayerEntity, PlayerEntityRenderState, PlayerEntityModel> {
   private static final Identifier TEXTURE = Identifier.of("minecraft", "textures/entity/steve.png");

   public FakePlayerEntityRenderer(Context var1) {
      super(var1, new PlayerEntityModel(var1.getPart(EntityModelLayers.PLAYER), false), 0.6F);
   }

   public PlayerEntityRenderState createRenderState() {
      return new PlayerEntityRenderState();
   }

   public Identifier getTexture(PlayerEntityRenderState var1) {
      return TEXTURE;
   }
}
