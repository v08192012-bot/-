package brokke.cc.manager;

import brokke.cc.core.FakePlayerEntity;
import brokke.cc.core.FakePlayerRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class FakePlayerManager {
   private static final List<FakePlayerEntity> fakePlayers = new ArrayList<>();

   public static List<FakePlayerEntity> getFakePlayers() {
      return fakePlayers;
   }

   public static void a(Vec3d var0, String var1) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2.world != null && var2.player != null) {
         FakePlayerEntity var3 = new FakePlayerEntity(FakePlayerRegistry.FAKE_PLAYER, var2.world);
         var3.setPosition(var0);
         var3.setCustomName(Text.literal(var1));
         var3.setCustomNameVisible(true);
         var3.setSkinName(var1);
         var3.setHealth(20.0F);
         var3.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
         var3.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
         var3.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
         var3.equipStack(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
         var3.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
         var3.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
         var2.world.addEntity(var3);
         fakePlayers.add(var3);
         var2.player.sendMessage(Text.literal("§aФейковый игрок §f" + var1 + " §aсоздан"), false);
      }
   }

   public static void b() {
      MinecraftClient var0 = MinecraftClient.getInstance();
      if (var0.world != null && var0.player != null) {
         FakePlayerEntity var1 = null;
         double var2 = Double.MAX_VALUE;

         for (FakePlayerEntity var5 : fakePlayers) {
            if (var5 != null && var5.isAlive() && var0.player.squaredDistanceTo(var5) < var2) {
               var2 = var0.player.squaredDistanceTo(var5);
               var1 = var5;
            }
         }

         if (var1 != null) {
            var1.discard();
            fakePlayers.remove(var1);
            var0.player.sendMessage(Text.literal("§cФейковый игрок удалён"), false);
         } else {
            var0.player.sendMessage(Text.literal("§cНет фейковых игроков рядом"), false);
         }
      }
   }

   public static void c() {
      for (FakePlayerEntity var1 : new ArrayList<>(fakePlayers)) {
         if (var1 != null && var1.isAlive()) {
            var1.discard();
         }
      }

      fakePlayers.clear();
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2.player != null) {
         var2.player.sendMessage(Text.literal("§cВсе фейковые игроки удалены"), false);
      }
   }

   public static void d() {
      fakePlayers.removeIf(var0 -> var0 == null || !var0.isAlive());
   }
}
