package brokke.cc.core;

public class PurchaseRecord {
   public long time;
   public String itemId;
   public String label;
   public String seller;
   public long pricePerUnit;
   public long totalPrice;
   public int count;
   public boolean success = true;
   public String failReason;

   public PurchaseRecord() {
   }

   public PurchaseRecord(long var1, String var3, String var4, String var5, long var6, long var8, int var10) {
      this.time = var1;
      this.itemId = var3;
      this.label = var4;
      this.seller = var5;
      this.pricePerUnit = var6;
      this.totalPrice = var8;
      this.count = var10;
   }
}
