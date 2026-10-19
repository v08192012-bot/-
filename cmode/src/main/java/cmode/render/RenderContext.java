package cmode.render;

import org.joml.Matrix4f;
import org.joml.Vector4f;

public final class RenderContext {
   public static final Matrix4f WORLD_PROJECTION = new Matrix4f();
   public static boolean VALID = false;
   public static final Matrix4f VIEW_MATRIX = new Matrix4f();
   public static final Matrix4f PROJECTION_MATRIX = new Matrix4f();
   public static final Matrix4f VIEW_PROJECTION_MATRIX = new Matrix4f();
   public static final float[] SKY_COLOR = new float[]{0.6F, 0.7F, 0.85F};
   public static boolean WORLD_READY = false;

   private RenderContext() {
   }

   public static void a(Matrix4f var0, Matrix4f var1, Vector4f var2) {
      if (var0 != null && var1 != null) {
         VIEW_MATRIX.set(var0);
         PROJECTION_MATRIX.set(var1);
         VIEW_PROJECTION_MATRIX.set(var1).mul(var0);
         if (var2 != null) {
            SKY_COLOR[0] = var2.x;
            SKY_COLOR[1] = var2.y;
            SKY_COLOR[2] = var2.z;
         }

         WORLD_READY = true;
      }
   }
}
