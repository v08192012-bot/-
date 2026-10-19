package brokke.cc.cosmetic;

import net.minecraft.client.util.math.MatrixStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class GeckoRenderHelper {
   private static final Vector3f POSITIVE_X = new Vector3f(1.0F, 0.0F, 0.0F);
   private static final Vector3f POSITIVE_Y = new Vector3f(0.0F, 1.0F, 0.0F);
   private static final Vector3f POSITIVE_Z = new Vector3f(0.0F, 0.0F, 1.0F);

   public static void a(GeoBone var0, MatrixStack var1) {
      var1.translate(-var0.getPositionX() / 16.0F, var0.getPositionY() / 16.0F, var0.getPositionZ() / 16.0F);
   }

   public static void b(GeoBone var0, MatrixStack var1) {
      var1.translate(var0.getRotationPointX() / 16.0F, var0.getRotationPointY() / 16.0F, var0.getRotationPointZ() / 16.0F);
   }

   public static void c(GeoBone var0, MatrixStack var1) {
      var1.translate(-var0.getRotationPointX() / 16.0F, -var0.getRotationPointY() / 16.0F, -var0.getRotationPointZ() / 16.0F);
   }

   public static void d(GeoBone var0, MatrixStack var1) {
      if (var0.getRotateZ() != 0.0F) {
         var1.multiply(a(POSITIVE_Z, var0.getRotateZ()));
      }

      if (var0.getRotateY() != 0.0F) {
         var1.multiply(a(POSITIVE_Y, var0.getRotateY()));
      }

      if (var0.getRotateX() != 0.0F) {
         var1.multiply(a(POSITIVE_X, var0.getRotateX()));
      }
   }

   public static void e(GeoBone var0, MatrixStack var1) {
      var1.scale(var0.getScaleX(), var0.getScaleY(), var0.getScaleZ());
   }

   public static void a(GeoCube var0, MatrixStack var1) {
      var1.translate(var0.pivot.getX() / 16.0F, var0.pivot.getY() / 16.0F, var0.pivot.getZ() / 16.0F);
   }

   public static void b(GeoCube var0, MatrixStack var1) {
      var1.translate(-var0.pivot.getX() / 16.0F, -var0.pivot.getY() / 16.0F, -var0.pivot.getZ() / 16.0F);
   }

   public static void c(GeoCube var0, MatrixStack var1) {
      if (var0.rotation.getZ() != 0.0F) {
         var1.multiply(a(POSITIVE_Z, var0.rotation.getZ()));
      }

      if (var0.rotation.getY() != 0.0F) {
         var1.multiply(a(POSITIVE_Y, var0.rotation.getY()));
      }

      if (var0.rotation.getX() != 0.0F) {
         var1.multiply(a(POSITIVE_X, var0.rotation.getX()));
      }
   }

   public static Quaternionf a(Vector3f var0, float var1) {
      float var2 = (float)Math.sin(var1 / 2.0F);
      float var3 = var0.x() * var2;
      float var4 = var0.y() * var2;
      float var5 = var0.z() * var2;
      float var6 = (float)Math.cos(var1 / 2.0F);
      return new Quaternionf(var3, var4, var5, var6);
   }
}
