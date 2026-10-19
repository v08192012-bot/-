package brokke.cc.core;

public class HandlerProcessor {
   private static HandlerProcessor instance;
   private final InventoryHandler inventoryHandler;
   private final UseableHandler useableHandler;
   private final StopHandler stopHandler = new StopHandler();
   private final InteractHandler interactHandler;

   public HandlerProcessor() {
      this.inventoryHandler = new InventoryHandler();
      this.useableHandler = new UseableHandler();
      this.interactHandler = new InteractHandler();
   }

   public static synchronized HandlerProcessor a() {
      if (instance == null) {
         instance = new HandlerProcessor();
      }

      return instance;
   }

   public static void b() {
      a();
   }

   public InventoryHandler getInventoryHandler() {
      return this.inventoryHandler;
   }

   public UseableHandler getUseableHandler() {
      return this.useableHandler;
   }

   public StopHandler getStopHandler() {
      return this.stopHandler;
   }

   public InteractHandler getInteractHandler() {
      return this.interactHandler;
   }
}
