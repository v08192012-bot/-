package cmode.core;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;

public final class LegacyRenderCompat {
   private LegacyRenderCompat() {
   }

   public static void a(
      EntityRenderManager var0, Entity var1, float var2, double var3, double var5, double var7, MatrixStack var9, VertexConsumerProvider var10
   ) {
      EntityRenderState var11 = var0.getAndUpdateRenderState(var1, var2);
      var11.shadowRadius = 0.0F;
      a(
         var10,
         var9xx -> var0.render(var11, MinecraftClient.getInstance().gameRenderer.getEntityRenderStates().cameraRenderState, var3, var5, var7, var9, var9xx)
      );
   }

   public static void a(VertexConsumerProvider var0, Consumer<OrderedRenderCommandQueue> var1) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      OrderedRenderCommandQueueImpl var3 = new OrderedRenderCommandQueueImpl();
      LegacyRenderCompat.a var4 = var0 instanceof Immediate var5 ? null : new LegacyRenderCompat.a(var0);
      Immediate var10 = var4 == null ? (Immediate)var0 : var4;
      RenderDispatcher var6 = new RenderDispatcher(
         var3, var2.getBlockRenderManager(), var10, var2.getAtlasManager(), new OutlineVertexConsumerProvider(), var10, var2.textRenderer
      );

      try {
         var1.accept(var3);
         var6.render();
         var6.endLayeredCustoms();
      } finally {
         var6.close();
         if (var4 != null) {
            var4.a();
         }
      }
   }

   static final class a extends Immediate {
      private final VertexConsumerProvider delegate;

      a(VertexConsumerProvider var1) {
         super(new BufferAllocator(256), new Object2ObjectLinkedOpenHashMap());
         this.delegate = var1;
      }

      @Override
      public VertexConsumer getBuffer(RenderLayer layer) {
         return this.delegate.getBuffer(layer);
      }

      @Override
      public void drawCurrentLayer() {
      }

      @Override
      public void draw() {
      }

      @Override
      public void draw(RenderLayer layer) {
      }

      void a() {
         this.allocator.close();
      }
   }
}
