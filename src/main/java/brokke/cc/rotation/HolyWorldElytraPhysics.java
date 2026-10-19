package brokke.cc.rotation;

public final class HolyWorldElytraPhysics {
   public static final double GRAVITY = 0.08;
   public static final double JUMP_MOTION = 0.42;
   public static final double AIR_HORIZONTAL_DRAG = 0.91;
   public static final double AIR_VERTICAL_DRAG = 0.98;

   private HolyWorldElytraPhysics() {
   }

   public static boolean a(HolyWorldElytraPhysics.a var0, double var1, double var3, double var5, double var7, int var9) {
      boolean var10 = var0.onGround || !var0.isGliding;
      boolean var11 = var10 && !var0.hasJumped;
      if (var11 && var0.onGround) {
         var0.motionY = Math.max(0.42, var0.motionY);
         var0.motionX += 0.2;
      }

      if (var11 && !var0.onGround && !var0.isGliding) {
         var0.isGliding = true;
      }

      if (var0.isGliding) {
         a(var0, var1);
      } else if (var0.onGround) {
         var0.motionX = (var0.motionX + 0.13) * 0.546;
         var0.motionY = (var0.motionY - 0.08) * 0.98;
      } else {
         var0.motionX = (var0.motionX + 0.026) * 0.91;
         var0.motionY = (var0.motionY - 0.08) * 0.98;
      }

      if (var0.fallCooldown >= 0 && --var0.fallCooldown < 0) {
         var0.isGliding = false;
      }

      double var12 = var0.posY + var0.motionY;
      boolean var14 = var7 > var3 + 0.6 && var7 > Math.max(var0.posY, var12);
      if (var14) {
         var0.motionX = 0.0;
      } else {
         var0.posX = var0.posX + var0.motionX;
      }

      double var15 = var14 ? var3 : var5;
      if (var12 <= var15) {
         var0.posY = var15;
         var0.motionY = 0.0;
         var0.onGround = true;
         if (var0.fallCooldown < 0 && var0.isGliding) {
            var0.fallCooldown = var9;
         }
      } else {
         var0.posY = var12;
         var0.onGround = false;
      }

      var0.hasJumped = var11;
      return var14;
   }

   private static void a(HolyWorldElytraPhysics.a var0, double var1) {
      double var3 = Math.toRadians(var1);
      double var5 = Math.cos(var3);
      double var7 = var5 * var5;
      double var9 = Math.abs(var0.motionX);
      var0.motionY += 0.08 * (-1.0 + var7 * 0.75);
      if (var0.motionY < 0.0 && var5 > 0.0) {
         double var11 = var0.motionY * -0.1 * var7;
         var0.motionX += var11;
         var0.motionY += var11;
      }

      if (var3 < 0.0 && var5 > 0.0) {
         double var13 = var9 * -Math.sin(var3) * 0.04;
         var0.motionX -= var13;
         var0.motionY += var13 * 3.2;
      }

      if (var5 > 0.0) {
         var0.motionX = var0.motionX + (var9 - var0.motionX) * 0.1;
      }

      var0.motionX *= 0.99;
      var0.motionY *= 0.98;
   }

   public static final class a {
      public double posX;
      public double posY;
      public double motionX;
      public double motionY;
      public boolean isGliding;
      public boolean onGround;
      public boolean hasJumped;
      public int fallCooldown;

      public void a(HolyWorldElytraPhysics.a var1) {
         this.posX = var1.posX;
         this.posY = var1.posY;
         this.motionX = var1.motionX;
         this.motionY = var1.motionY;
         this.isGliding = var1.isGliding;
         this.onGround = var1.onGround;
         this.hasJumped = var1.hasJumped;
         this.fallCooldown = var1.fallCooldown;
      }
   }
}
