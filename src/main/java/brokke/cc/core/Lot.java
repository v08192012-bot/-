package brokke.cc.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Lot {
   public final int slot;
   public final String itemId;
   public final String name;
   public final String lore;
   public final String seller;
   public final int count;
   public final long unitPrice;
   public final long totalPrice;
   public final int durability;
   public final boolean barrier;
   public final boolean hasMarkers;
   public final Map<String, String> pbv;
   public final Set<String> customEnch;
   public final Map<String, String> customData;
   public Set<String> potionEffects = Collections.emptySet();
   public int sharpness = 0;

   public Lot(
      int var1,
      String var2,
      String var3,
      String var4,
      String var5,
      int var6,
      long var7,
      long var9,
      int var11,
      boolean var12,
      boolean var13,
      Map<String, String> var14,
      Set<String> var15,
      Map<String, String> var16
   ) {
      this.slot = var1;
      this.itemId = var2;
      this.name = var3;
      this.lore = var4 == null ? "" : var4;
      this.seller = var5 == null ? "" : var5;
      this.count = var6;
      this.unitPrice = var7;
      this.totalPrice = var9;
      this.durability = var11;
      this.barrier = var12;
      this.hasMarkers = var13;
      this.pbv = var14 == null ? Collections.emptyMap() : var14;
      this.customEnch = var15 == null ? Collections.emptySet() : var15;
      this.customData = var16 == null ? Collections.emptyMap() : var16;
   }

   public static Lot a(int var0) {
      return new Lot(var0, "minecraft:barrier", null, "", "", 0, 0L, 0L, 100, true, false, new HashMap<>(), new HashSet<>(), new HashMap<>());
   }
}
