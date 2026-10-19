package brokke.cc.nav;

import brokke.cc.util.GCDUtil2;
import java.io.BufferedWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public final class NavRecorder {
   private static final int FLUSH_INTERVAL = 256;
   public static final int REQUIRED_TICKS = 12000;
   private static final double TICKS_PER_MINUTE = 1200.0;
   private static final String CSV_HEADER = "t,x,y,z,vx,vy,vz,yaw,pitch,dyaw,dpitch,gcd,tyaw,tpitch,ryaw,rpitch,dist,hdist,ground,water,sprint,sneak,jump,fwd,strafe,mining\n";
   private static final int COLUMN_COUNT = "t,x,y,z,vx,vy,vz,yaw,pitch,dyaw,dpitch,gcd,tyaw,tpitch,ryaw,rpitch,dist,hdist,ground,water,sprint,sneak,jump,fwd,strafe,mining\n"
      .split(",").length;
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private BufferedWriter writer;
   private Path currentFile;
   private String currentName = "walk";
   private boolean recording;
   private int sessionRecordedTicks;
   private int flushCounter;
   private boolean hasPrevRot;
   private float prevYaw;
   private float prevPitch;

   public static Path a() {
      MinecraftClient var0 = MinecraftClient.getInstance();
      Path var1 = var0 != null && var0.runDirectory != null ? var0.runDirectory.toPath() : Path.of("run");
      return var1.resolve("brokke").resolve("nav").resolve("data");
   }

   public String a(String var1) {
      if (this.recording) {
         return "Запись навигации уже идёт: " + this.currentName;
      }

      try {
         Path var2 = a();
         Files.createDirectories(var2);
         Path var3 = var2.resolve(var1 + ".csv");
         boolean var4 = !Files.isRegularFile(var3) || Files.size(var3) == 0L;
         if (!var4) {
            a(var3);
         }

         this.writer = Files.newBufferedWriter(var3, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
         if (var4) {
            this.writer
               .write("t,x,y,z,vx,vy,vz,yaw,pitch,dyaw,dpitch,gcd,tyaw,tpitch,ryaw,rpitch,dist,hdist,ground,water,sprint,sneak,jump,fwd,strafe,mining\n");
         }

         this.currentFile = var3;
      } catch (Exception var5) {
         return "Не удалось открыть файл " + var1 + ".csv";
      }

      this.currentName = var1;
      this.flushCounter = 0;
      this.sessionRecordedTicks = 0;
      this.hasPrevRot = false;
      this.recording = true;
      return null;
   }

   public String b() {
      if (!this.recording) {
         return "Запись навигации не идёт.";
      }

      this.recording = false;
      this.h();
      int var1 = this.currentFile == null ? this.sessionRecordedTicks : b(this.currentFile);
      String var2 = b(var1);
      return this.currentName
         + ".csv: +"
         + a(this.sessionRecordedTicks)
         + " за сессию, всего "
         + a(var1)
         + " ("
         + c(var1)
         + ")"
         + (var2 == null ? " [Готово для модели!]" : ". До 10 мин не хватает: " + var2);
   }

   public void c() {
      if (this.recording && this.mc.player != null && this.mc.world != null && this.writer != null) {
         float var1 = this.mc.player.getYaw();
         float var2 = this.mc.player.getPitch();
         float var3 = this.hasPrevRot ? MathHelper.wrapDegrees(var1 - this.prevYaw) : 0.0F;
         float var4 = this.hasPrevRot ? var2 - this.prevPitch : 0.0F;
         this.prevYaw = var1;
         this.prevPitch = var2;
         this.hasPrevRot = true;
         Vec3d var5 = this.mc.player.getEntityPos();
         Vec3d var6 = this.mc.player.getVelocity();
         float var7 = var1;
         float var8 = var2;
         double var9 = 0.0;
         double var11 = 0.0;
         PathNode var13 = NavigationManager.getInstance().getMovementController().i();
         if (var13 != null) {
            BlockPos var14 = var13.getPos();
            Vec3d var15 = new Vec3d(var14.getX() + 0.5, var14.getY() + 0.5, var14.getZ() + 0.5);
            double var16 = var15.x - this.mc.player.getX();
            double var18 = var15.y - this.mc.player.getEyeY();
            double var20 = var15.z - this.mc.player.getZ();
            var11 = Math.hypot(var16, var20);
            var9 = Math.sqrt(var16 * var16 + var18 * var18 + var20 * var20);
            var7 = (float)Math.toDegrees(Math.atan2(var20, var16)) - 90.0F;
            var8 = (float)(-Math.toDegrees(Math.atan2(var18, Math.max(0.001, var11))));
         }

         float var27 = MathHelper.wrapDegrees(var7 - var1);
         float var28 = var8 - var2;
         float var29 = GCDUtil2.a();
         float var17 = 0.0F;
         float var30 = 0.0F;
         boolean var19 = false;
         boolean var31 = false;
         if (this.mc.player.input != null) {
            Vec2f var21 = this.mc.player.input.getMovementInput();
            if (var21 != null) {
               var17 = var21.y;
               var30 = var21.x;
            }

            if (this.mc.player.input.playerInput != null) {
               var19 = this.mc.player.input.playerInput.jump();
               var31 = this.mc.player.input.playerInput.sneak();
            }
         }

         boolean var32 = this.mc.player.isSprinting();
         boolean var22 = this.mc.player.isOnGround();
         boolean var23 = this.mc.player.isTouchingWater();
         boolean var24 = this.mc.options.attackKey.isPressed();

         try {
            this.writer
               .write(
                  String.format(
                     Locale.ROOT,
                     "%d,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.2f,%.2f,%.2f,%.2f,%.6f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%d,%d,%d,%d,%d,%.1f,%.1f,%d\n",
                     this.sessionRecordedTicks,
                     var5.x,
                     var5.y,
                     var5.z,
                     var6.x,
                     var6.y,
                     var6.z,
                     var1,
                     var2,
                     var3,
                     var4,
                     var29,
                     var7,
                     var8,
                     var27,
                     var28,
                     var9,
                     var11,
                     var22 ? 1 : 0,
                     var23 ? 1 : 0,
                     var32 ? 1 : 0,
                     var31 ? 1 : 0,
                     var19 ? 1 : 0,
                     var17,
                     var30,
                     var24 ? 1 : 0
                  )
               );
            this.sessionRecordedTicks++;
            if (++this.flushCounter >= 256) {
               this.flushCounter = 0;
               this.writer.flush();
            }
         } catch (Exception var26) {
            this.h();
         }
      }
   }

   private static void a(Path var0) {
      try (FileChannel var1 = FileChannel.open(var0, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
         long var2 = var1.size();
         ByteBuffer var4 = ByteBuffer.allocate((int)Math.min(var2, 8192L));
         var1.read(var4, var2 - var4.capacity());
         byte[] var5 = var4.array();

         for (int var6 = var5.length - 1; var6 >= 0; var6--) {
            if (var5[var6] == 10) {
               long var7 = var2 - var5.length + var6 + 1L;
               if (var7 < var2) {
                  var1.truncate(var7);
               }

               return;
            }
         }
      } catch (Exception var11) {
      }
   }

   private void h() {
      try {
         if (this.writer != null) {
            this.writer.flush();
            this.writer.close();
         }
      } catch (Exception var2) {
      }

      this.writer = null;
   }

   public static String a(int var0) {
      return String.format(Locale.ROOT, "%.1f мин", var0 / 1200.0);
   }

   public static String b(int var0) {
      return var0 >= 12000 ? null : a(12000 - var0);
   }

   public static String c(int var0) {
      if (var0 < 3600) {
         return "мало";
      } else if (var0 < 7200) {
         return "маловато";
      } else if (var0 < 12000) {
         return "почти готово";
      } else {
         return var0 < 24000 ? "нормально" : "отлично";
      }
   }

   public static List<String> d() {
      ArrayList var0 = new ArrayList();

      try (Stream<Path> var1 = Files.list(a())) {
         var1.filter(var0x -> var0x.getFileName().toString().endsWith(".csv")).sorted().forEach(var1x -> {
            int var2 = b(var1x);
            var0.add(var1x.getFileName().toString().replaceFirst("\\.csv$", "") + " · " + a(var2) + " · " + c(var2));
         });
      } catch (Exception var6) {
      }

      return var0;
   }

   private static int b(Path var0) {
      int var1 = 0;

      try (Stream var2 = Files.lines(var0)) {
         Iterator var3 = var2.iterator();

         while (var3.hasNext()) {
            String var4 = (String)var3.next();
            if (!var4.isBlank() && !var4.startsWith("t,")) {
               var1++;
            }
         }
      } catch (Exception var7) {
      }

      return var1;
   }

   public boolean isRecording() {
      return this.recording;
   }

   public String getCurrentName() {
      return this.currentName;
   }

   public int getSessionRecordedTicks() {
      return this.sessionRecordedTicks;
   }
}
