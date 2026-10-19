package brokke.cc.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

public class RenderStack {
   private MatrixStack stack;

   public void setStack(MatrixStack var1) {
      this.stack = var1;
   }

   public void a() {
      this.stack.push();
   }

   public void b() {
      this.stack.pop();
   }

   public MatrixStack getStack() {
      return this.stack;
   }

   public void a(float var1, float var2, float var3) {
      this.stack.translate(var1, var2, var3);
   }

   public void b(float var1, float var2, float var3) {
      this.stack.scale(var1, var2, var3);
   }

   public void a(float var1) {
      this.stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(var1));
   }

   public void b(float var1) {
      this.stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var1));
   }

   public void c(float var1) {
      this.stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(var1));
   }

   public void c(float var1, float var2, float var3) {
      if (var3 != 0.0F) {
         this.c(var3);
      }

      if (var2 != 0.0F) {
         this.b(var2);
      }

      if (var1 != 0.0F) {
         this.a(var1);
      }
   }

   public void d(float var1, float var2, float var3) {
      this.c(var1, var2, var3);
   }

   public void d(float var1) {
      this.a(var1);
   }

   public void e(float var1) {
      this.b(var1);
   }

   public void f(float var1) {
      this.c(var1);
   }
}
