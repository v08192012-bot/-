package brokke.cc.nav;

import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.EventMoveInput;
import brokke.cc.system.events.impl.UpdateEvent;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class NavigationManager {
   private static final NavigationManager INSTANCE = new NavigationManager();
   private static boolean initialized;
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private final NavMovementController movementController = new NavMovementController();
   private final NavRotationController rotationController = new NavRotationController();
   private final NavRecorder recorder = new NavRecorder();
   private final PathRenderer pathRenderer = new PathRenderer();
   private NavTask currentTask;

   public static NavigationManager getInstance() {
      return INSTANCE;
   }

   public static void b() {
      if (!initialized) {
         EventBus.register(INSTANCE);
         EventBus.register(INSTANCE.pathRenderer);
         initialized = true;
      }
   }

   public void a(NavTask var1) {
      if (this.currentTask != null) {
         this.currentTask.d();
      }

      this.currentTask = var1;
      if (var1 != null) {
         var1.a();
         this.b("§aНачата задача: §f" + var1.getName());
      }
   }

   public void c() {
      if (this.currentTask != null) {
         this.currentTask.d();
         this.b("§7Задача остановлена: §f" + this.currentTask.getName());
         this.currentTask = null;
      }

      this.movementController.a();
      this.rotationController.b();
   }

   public void a(String var1) {
      if (this.currentTask != null) {
         this.b("§c" + var1 + ": §f" + this.currentTask.getName());
         this.currentTask.d();
         this.currentTask = null;
      }

      this.movementController.a();
      this.rotationController.b();
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (this.currentTask != null) {
         this.currentTask.b();
         this.movementController.a(this.rotationController);
         if (this.currentTask != null && this.currentTask.isFinished()) {
            this.b("§aЗадача завершена: §f" + this.currentTask.getName());
            this.currentTask.d();
            this.currentTask = null;
            this.movementController.a();
            this.rotationController.b();
         }
      }

      this.recorder.c();
   }

   @EventHandler
   public void onMoveInput(EventMoveInput var1) {
      if (this.currentTask != null) {
         this.movementController.a(var1);
      }
   }

   public void b(String var1) {
      if (this.mc.player != null) {
         this.mc.player.sendMessage(Text.literal("§b§lBaritone §7» " + var1), false);
      }
   }

   public boolean d() {
      return this.currentTask != null && !this.currentTask.isFinished();
   }

   public NavTask getCurrentTask() {
      return this.currentTask;
   }

   public NavMovementController getMovementController() {
      return this.movementController;
   }

   public NavRotationController getRotationController() {
      return this.rotationController;
   }

   public NavRecorder getRecorder() {
      return this.recorder;
   }

   public PathRenderer getPathRenderer() {
      return this.pathRenderer;
   }

   public List<PathNode> j() {
      return this.currentTask != null ? this.currentTask.getPath() : Collections.emptyList();
   }
}
