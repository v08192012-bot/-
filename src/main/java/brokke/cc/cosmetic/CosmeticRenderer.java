package brokke.cc.cosmetic;

import brokke.cc.core.ModelPosition;
import brokke.cc.render.RenderStack;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;

public class CosmeticRenderer {
   private static CosmeticRenderer instance;
   private final GeckolibCosmeticRenderer geckolibRenderer = GeckolibCosmeticRenderer.a();
   private final RenderStack stack = new RenderStack();

   public static CosmeticRenderer a() {
      if (instance == null) {
         instance = new CosmeticRenderer();
      }

      return instance;
   }

   public void a(
      CosmeticModel var1, AbstractClientPlayerEntity var2, MatrixStack var3, VertexConsumerProvider var4, int var5, PlayerEntityModel var6, float var7
   ) {
      if (var1 != null && var1.getTextureId() != null) {
         this.stack.setStack(var3);
         this.stack.a();
         this.a(var1, var6);
         this.stack.f(180.0F);
         this.stack.a(var1.getX(), var1.getY() + this.c(var1), var1.getZ());
         this.stack.e(var1.getYaw());
         this.stack.d(var1.getPitch());
         this.stack.f(var1.getRoll());
         this.stack.b(var1.getScale(), var1.getScale(), var1.getScale());
         this.geckolibRenderer.a(var1, var3, var4, var5);
         this.stack.b();
      }
   }

   public void a(CosmeticModel var1, AbstractClientPlayerEntity var2, MatrixStack var3, VertexConsumer var4, int var5, PlayerEntityModel var6, float var7) {
      if (var1 != null && var1.getTextureId() != null && var4 != null) {
         this.stack.setStack(var3);
         this.stack.a();
         this.a(var1, var6);
         this.stack.f(180.0F);
         this.stack.a(var1.getX(), var1.getY() + this.c(var1), var1.getZ());
         this.stack.e(var1.getYaw());
         this.stack.d(var1.getPitch());
         this.stack.f(var1.getRoll());
         this.stack.b(var1.getScale(), var1.getScale(), var1.getScale());
         this.geckolibRenderer.a(var1, var3, var4, var5);
         this.stack.b();
      }
   }

   private void a(CosmeticModel var1, PlayerEntityModel var2) {
      if (var2 != null && var1 != null) {
         ModelPosition var3 = this.a(var1);
         switch (var3) {
            case HEAD:
               this.a(var2.head);
               break;
            case BODY:
               this.a(var2.body);
               break;
            case RIGHT_ARM:
               this.a(var2.rightArm);
               break;
            case LEFT_ARM:
               this.a(var2.leftArm);
               break;
            case RIGHT_LEG:
               this.a(var2.rightLeg);
               break;
            case LEFT_LEG:
               this.a(var2.leftLeg);
            case ABOVE_HEAD:
            case FREE:
         }
      }
   }

   private ModelPosition a(CosmeticModel var1) {
      if (var1 == null) {
         return ModelPosition.BODY;
      }

      String var2 = var1.getType();
      if (!"wings".equalsIgnoreCase(var2) && !"bodywear".equalsIgnoreCase(var2)) {
         String var3 = var1.getName();
         if (var3 != null) {
            String var4 = var3.toLowerCase();
            if (var4.contains("wing") || var4.contains("backpack") || var4.contains("bag") || var4.contains("katana") || var4.contains("back")) {
               return ModelPosition.BODY;
            }
         }

         return var1.getPosition();
      } else {
         return ModelPosition.BODY;
      }
   }

   private boolean b(CosmeticModel var1) {
      if (var1 == null) {
         return false;
      }

      String var2 = var1.getType();
      if (!"wings".equalsIgnoreCase(var2) && !"bodywear".equalsIgnoreCase(var2)) {
         String var3 = var1.getName();
         if (var3 == null) {
            return false;
         }

         String var4 = var3.toLowerCase();
         return var4.contains("wing") || var4.contains("backpack") || var4.contains("bag") || var4.contains("katana") || var4.contains("back");
      } else {
         return true;
      }
   }

   private float c(CosmeticModel var1) {
      return this.b(var1) ? -0.35F : 0.0F;
   }

   private void a(ModelPart var1) {
      this.stack.a(var1.originX * 0.0625F, var1.originY * 0.0625F, var1.originZ * 0.0625F);
      this.stack.d(var1.pitch * (180.0F / (float)Math.PI), var1.yaw * (180.0F / (float)Math.PI), var1.roll * (180.0F / (float)Math.PI));
   }
}
