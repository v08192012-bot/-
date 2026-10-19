package brokke.cc.util;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;

public final class ServerUtility {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   public static boolean hasCT;
   public static int ctTime;
   public static int ftAn = -1;

   public static boolean a() {
      return a("cherry.pizza");
   }

   public static boolean b() {
      return a("spooky");
   }

   public static boolean c() {
      return a("funtime") || a("playft");
   }

   public static boolean d() {
      return a("reallyworld") || a("playrw");
   }

   public static boolean e() {
      return a("funsky");
   }

   public static boolean f() {
      return a("holy") || a("holly") || a("playhw");
   }

   public static boolean g() {
      return a("sunw");
   }

   public static boolean h() {
      return a("saturn");
   }

   public static boolean i() {
      return a("sunmc");
   }

   public static boolean j() {
      return a("mineblaze") || a("dexland");
   }

   public static boolean k() {
      return c() || d() || e();
   }

   public static boolean l() {
      return c() || b() || e();
   }

   public static String m() {
      return mc.player != null && mc.player.networkHandler.getServerInfo() != null ? mc.player.networkHandler.getServerInfo().address : "single";
   }

   public static boolean a(String var0) {
      return m().toLowerCase().contains(var0.toLowerCase());
   }

   public static boolean n() {
      if (mc.player != null && mc.world != null) {
         BlockPos var0 = mc.player.getBlockPos();
         Block var1 = mc.world.getBlockState(var0.down(1)).getBlock();
         Block var2 = mc.world.getBlockState(new BlockPos(var0.getX(), 0, var0.getZ())).getBlock();
         if (c() && mc.world.getDifficulty() == Difficulty.NORMAL) {
            return false;
         } else {
            return !c() && !b()
               ? mc.world.getRegistryKey() == World.OVERWORLD && (var2 == Blocks.BEDROCK || var1 == Blocks.BEDROCK)
               : var1 == Blocks.AIR || var2 == Blocks.AIR;
         }
      } else {
         return false;
      }
   }

   private ServerUtility() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
