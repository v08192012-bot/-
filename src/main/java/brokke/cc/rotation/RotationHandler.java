package brokke.cc.rotation;

import net.minecraft.client.MinecraftClient;

public class RotationHandler {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final RotationHandler INSTANCE = new RotationHandler();
   private Rotation2 currentRotation = Rotation2.ZERO;
   private Rotation2 prevRotation = Rotation2.ZERO;
   private Rotation2 renderRotation = Rotation2.ZERO;
   private boolean idling = true;

   public static RotationHandler getInstance() {
      return INSTANCE;
   }

   public boolean isIdling() {
      return this.idling;
   }

   public void a(Rotation2 var1, MoveCorrection var2, float var3, float var4, float var5, RotationPriority var6) {
      this.currentRotation = var1;
      this.idling = false;
      if (mc.player != null) {
         mc.player.setYaw(var1.getYaw());
         mc.player.setPitch(var1.getPitch());
      }
   }

   public void a(Rotation2 var1, MoveCorrection var2, float var3, float var4, float var5) {
      this.a(var1, var2, var3, var4, var5, RotationPriority.NORMAL);
   }

   public void a(Rotation2 var1, RotationPriority var2) {
      this.a(var1, MoveCorrection.DIRECT, 180.0F, 180.0F, 180.0F, var2);
   }

   public void a(Rotation2 var1) {
      this.a(var1, MoveCorrection.DIRECT, 180.0F, 180.0F, 180.0F, RotationPriority.NORMAL);
   }

   public Rotation2 getCurrentRotation() {
      return this.currentRotation;
   }

   public Rotation2 d() {
      return mc.player == null ? Rotation2.ZERO : new Rotation2(mc.player.getYaw(), mc.player.getPitch());
   }

   public Rotation2 getRenderRotation() {
      return this.renderRotation;
   }

   public void setIdling(boolean var1) {
      this.idling = var1;
   }
}
