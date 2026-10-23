package shelka.aim.render;

import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.Identifier;
import shelka.aim.core.WarmupBotEntity;

public class WarmupBotRenderer extends LivingEntityRenderer<WarmupBotEntity, PlayerEntityRenderState, PlayerEntityModel> {
   private static final Identifier TEXTURE = Identifier.of("minecraft", "textures/entity/steve.png");

   public WarmupBotRenderer(Context context) {
      super(context, new PlayerEntityModel(context.getPart(EntityModelLayers.PLAYER), false), 0.6F);
   }

   @Override
   public PlayerEntityRenderState createRenderState() {
      return new PlayerEntityRenderState();
   }

   @Override
   public Identifier getTexture(PlayerEntityRenderState state) {
      return TEXTURE;
   }
}
