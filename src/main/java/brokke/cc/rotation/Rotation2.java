package brokke.cc.rotation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Rotation2 {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   public static final Rotation2 ZERO = new Rotation2(0.0F, 0.0F);
   private float yaw;
   private float pitch;

   public Rotation2() {
      this(0.0F, 0.0F);
   }

   public Rotation2(float var1, float var2) {
      this.yaw = var1;
      this.pitch = var2;
   }

   public Rotation2(Entity var1) {
      this(var1.getYaw(), var1.getPitch());
   }

   public float a(Rotation2 var1) {
      float var2 = MathHelper.wrapDegrees(var1.getYaw() - this.yaw);
      float var3 = var1.getPitch() - this.pitch;
      return (float)Math.hypot(Math.abs(var2), Math.abs(var3));
   }

   public double b(Rotation2 var1) {
      double var2 = MathHelper.wrapDegrees(var1.getYaw() - this.yaw);
      double var4 = MathHelper.wrapDegrees(var1.getPitch() - this.pitch);
      return Math.hypot(var2, var4);
   }

   public Rotation2 c(Rotation2 var1) {
      float var2 = MathHelper.wrapDegrees(this.yaw - var1.yaw);
      float var3 = MathHelper.wrapDegrees(this.pitch - var1.pitch);
      return new Rotation2(var2, var3);
   }

   public float d(Rotation2 var1) {
      float var2 = MathHelper.wrapDegrees(this.yaw - var1.yaw);
      float var3 = MathHelper.wrapDegrees(this.pitch - var1.pitch);
      return Math.abs(var2) + Math.abs(var3);
   }

   public static Vector2f a() {
      return new Vector2f(b(), c());
   }

   public static float b() {
      return mc.gameRenderer != null && mc.gameRenderer.getCamera() != null
         ? MathHelper.wrapDegrees(mc.gameRenderer.getCamera().getYaw() + (mc.gameRenderer.getCamera().isThirdPerson() ? 180 : 0))
         : 0.0F;
   }

   public static float c() {
      return mc.gameRenderer != null && mc.gameRenderer.getCamera() != null
         ? (mc.gameRenderer.getCamera().isThirdPerson() ? -1 : 1) * mc.gameRenderer.getCamera().getPitch()
         : 0.0F;
   }

   public static Rotation2 a(PlayerEntity var0, Entity var1) {
      Vec3d var2 = var0.getCameraPosVec(0.0F);
      Vec3d var3 = new Vec3d(var1.getX(), var1.getY() + var1.getHeight() * 0.5, var1.getZ());
      double var4 = var3.x - var2.x;
      double var6 = var3.y - var2.y;
      double var8 = var3.z - var2.z;
      double var10 = Math.sqrt(var4 * var4 + var8 * var8);
      float var12 = (float)Math.toDegrees(Math.atan2(var8, var4)) - 90.0F;
      float var13 = (float)(-Math.toDegrees(Math.atan2(var6, var10)));
      return new Rotation2(var12, var13);
   }

   public final Vec3d d() {
      float var1 = this.pitch * (float) (Math.PI / 180.0);
      float var2 = -this.yaw * (float) (Math.PI / 180.0);
      float var3 = MathHelper.cos(var2);
      float var4 = MathHelper.sin(var2);
      float var5 = MathHelper.cos(var1);
      float var6 = MathHelper.sin(var1);
      return new Vec3d(var4 * var5, -var6, var3 * var5);
   }

   public Vec3d e() {
      return mc.player == null ? Vec3d.ZERO : mc.player.getRotationVector(this.pitch, this.yaw);
   }

   public void a(float var1, float var2) {
      this.yaw += var1;
      this.pitch += var2;
   }

   public float getYaw() {
      return this.yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public void setYaw(float var1) {
      this.yaw = var1;
   }

   public void setPitch(float var1) {
      this.pitch = var1;
   }
}
