package brokke.cc.ui;

import brokke.cc.animation.Animation;
import brokke.cc.animation.Easing;
import brokke.cc.config.ConfigManager;
import brokke.cc.config.HudConfig;
import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfManager;
import brokke.cc.module.misc.AutoSbor;
import brokke.cc.render.ColorPipeline;
import brokke.cc.render.GuiBatch;
import brokke.cc.render.RenderUtil;
import brokke.cc.render.Scissor;
import brokke.cc.render.Theme;
import java.awt.Color;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.render.fog.FogRenderer.FogType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent.Builder;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry.Reference;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;

public class AutoSborScreen extends Screen {
   private static final float MAIN_W = 280.0F;
   private static final float MAIN_H = 195.0F;
   private static final float SEARCH_W = 120.0F;
   private static final float SETTINGS_W = 135.0F;
   private static final float CONFIG_PANEL_H = 34.0F;
   private static final float PANEL_GAP = 7.0F;
   private static final int TOP_SLOTS = 8;
   private static final int INVENTORY_ROWS = 3;
   private static final int INVENTORY_COLS = 9;
   private static final float SLOT_SIZE = 24.0F;
   private static final float SLOT_GAP = 4.5F;
   private static final float ITEM_SIZE = 13.0F;
   private static final float SEARCH_SLOT_SIZE = 20.0F;
   private static final float SEARCH_SLOT_GAP = 3.0F;
   private static final float SEARCH_ITEM_SIZE = 12.0F;
   private static final int SEARCH_COLS = 4;
   private static Field GUI_RENDERER_FIELD;
   private static Field FOG_RENDERER_FIELD;
   private static ProfileComponent SPEED_SPHERE_PROFILE;
   private static ProfileComponent CERBERUS_SPHERE_PROFILE;
   private static ProfileComponent MYTHICAL_SPHERE_PROFILE;
   private static ProfileComponent FLASH_SPHERE_PROFILE;
   private static ProfileComponent ARMORTALITY_SPHERE_PROFILE;
   private static ProfileComponent IMMORTALITY_SPHERE_PROFILE;
   public static final ItemStack SPEED_SPHERE_STACK;
   public static final ItemStack DAMAGE_SPHERE_STACK;
   public static final ItemStack CERBERUS_SPHERE_STACK;
   public static final ItemStack ARMOR3_DAMAGE2_SPHERE_STACK;
   public static final ItemStack ARMOR2_DAMAGE3_SPHERE_STACK;
   public static final ItemStack FLASH_SPHERE_STACK;
   public static final ItemStack ARMORTALITY_SPHERE_STACK;
   public static final ItemStack IMMORTALITY_SPHERE_STACK;
   public static final ItemStack TALISMAN_INFINITY_STACK;
   public static final ItemStack TALISMAN_ETERNITY_STACK;
   public static final ItemStack TALISMAN_STINGER_STACK;
   public static final ItemStack ETERNITY_SWORD_STACK;
   public static final ItemStack STINGER_PICKAXE_STACK;
   public static final ItemStack ETERNITY_PICKAXE_STACK;
   public static final ItemStack EXP_50_STACK;
   public static final ItemStack ETERNITY_HELMET_STACK;
   public static final ItemStack ETERNITY_CHESTPLATE_STACK;
   public static final ItemStack ETERNITY_LEGGINGS_STACK;
   public static final ItemStack ETERNITY_BOOTS_STACK;
   public static final ItemStack INFINITY_SWORD_STACK;
   public static final ItemStack INFINITY_HELMET_STACK;
   public static final ItemStack INFINITY_CHESTPLATE_STACK;
   public static final ItemStack INFINITY_LEGGINGS_STACK;
   public static final ItemStack INFINITY_BOOTS_STACK;
   public static final ItemStack CERBERUS_SWORD_STACK;
   public static final ItemStack BACKPACK_LVL1_STACK;
   public static final ItemStack BACKPACK_LVL2_STACK;
   public static final ItemStack BACKPACK_LVL3_STACK;
   public static final ItemStack BACKPACK_LVL4_STACK;
   public static final ItemStack VAMPIRISM_STACK;
   public static final ItemStack POPPED_CHORUS_TRAP_STACK;
   public static final ItemStack WINNER_POTION_STACK;
   public static final ItemStack ENDERMAN_STACK;
   public static final ItemStack GRAVITY_STACK;
   public static final ItemStack JUSTICE_STACK;
   public static final ItemStack EXPLOSIVE_TRAP_STACK;
   public static final ItemStack EXPLOSIVE_THING_STACK;
   public static final ItemStack STUN_STACK;
   public static final ItemStack TURTLE_MASTER_POTION_STACK;
   public static final ItemStack SNOWBALL_STACK;
   public static final ItemStack HEALING_POTION_STACK;
   public static final ItemStack SPEED_POTION_STACK;
   public static final ItemStack STRENGTH_POTION_STACK;
   private static final List<AutoSborScreen.a> CATALOG_ITEMS;
   private final Screen parent;
   private final Animation openAnim = new Animation();
   private final Animation settingsAnim = new Animation();
   private final Animation searchScrollAnim = new Animation();
   private final Animation configScrollAnim = new Animation();
   private boolean closing = false;
   private final float[] closeBtnBox = new float[4];
   private final float[] settingsCloseBtnBox = new float[4];
   private final float[] countInputBox = new float[4];
   private final float[] durInputBox = new float[4];
   private final float[] configInputBox = new float[4];
   private final float[] configSaveBtnBox = new float[4];
   private final StringBuilder searchBuffer = new StringBuilder();
   private boolean searchFocused = false;
   private boolean searchSelectAll = false;
   private float searchScrollTarget = 0.0F;
   private float searchMaxScroll = 0.0F;
   private boolean searchScrollDragging = false;
   private final StringBuilder countBuffer = new StringBuilder();
   private boolean countFocused = false;
   private boolean countSelectAll = false;
   private final StringBuilder durBuffer = new StringBuilder();
   private boolean durFocused = false;
   private boolean durSelectAll = false;
   private final StringBuilder configInputBuffer = new StringBuilder();
   private boolean configInputFocused = false;
   private boolean configInputSelectAll = false;
   private float configScrollTarget = 0.0F;
   private float configMaxScroll = 0.0F;
   private String selectedConfigName = null;
   private int selectedSlotType = -1;
   private int selectedSlotIndex = -1;
   private ItemStack lastSelectedStack = ItemStack.EMPTY;
   private int lastSelectedSlotType = -1;
   private int lastSelectedSlotIndex = -1;
   private boolean sliderDragging = false;
   private String hoveredTooltip = null;

   public AutoSborScreen() {
      this(null);
   }

   public AutoSborScreen(Screen var1) {
      super(Text.of("AutoSbor"));
      this.parent = var1;
      this.openAnim.setEasing(Easing.SPRING_SOFT);
      this.settingsAnim.setEasing(Easing.SPRING_SOFT);
      this.searchScrollAnim.setEasing(Easing.SMOOTH_STEP);
      this.configScrollAnim.setEasing(Easing.SMOOTH_STEP);
      this.h();
      this.g();
   }

   private static ProfileComponent a() {
      if (SPEED_SPHERE_PROFILE == null) {
         SPEED_SPHERE_PROFILE = a(
            "{id:[I;-691918198,261896676,-2097102860,1276043714],name:\"\",properties:[{name:\"textures\",value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGM5MzY1NjQyYzZlZGRjZmVkZjViNWUxNGUyYmM3MTI1N2Q5ZTRhMzM2M2QxMjNjNmYzM2M1NWNhZmJmNmQifX19\"}]}"
         );
      }

      return SPEED_SPHERE_PROFILE;
   }

   private static ProfileComponent b() {
      if (CERBERUS_SPHERE_PROFILE == null) {
         CERBERUS_SPHERE_PROFILE = a(
            "{id:[I;1404353110,-467519837,-1639700424,-1282774674],name:\"\",properties:[{name:\"textures\",value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjA5NWE3ZmQ5MGRhYTFiYmU3MDY5MDg5NzQwZTA1ZDBiZmM2NjI5NmVlM2M0MGVlNzFhNGUwYTY2MTZiMmJiYyJ9fX0=\"}]}"
         );
      }

      return CERBERUS_SPHERE_PROFILE;
   }

   private static ProfileComponent c() {
      if (MYTHICAL_SPHERE_PROFILE == null) {
         MYTHICAL_SPHERE_PROFILE = a(
            "{id:[I;-2028481669,-405130400,-1468526862,-1002383415],name:\"\",properties:[{name:\"textures\",value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmFmZjJlYjQ5OGU1YzZhMDQ0ODRmMGM5Zjc4NWI0NDg0NzlhYjIxM2RmOTVlYzkxMTc2YTMwOGExMmFkZDcwIn19fQ==\"}]}"
         );
      }

      return MYTHICAL_SPHERE_PROFILE;
   }

   private static ProfileComponent d() {
      if (FLASH_SPHERE_PROFILE == null) {
         FLASH_SPHERE_PROFILE = a(
            "{id:[I;-372484930,-74172931,-1856149270,-1219017950],name:\"\",properties:[{name:\"textures\",value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzc0MDBlYTE5ZGJkODRmNzVjMzlhZDY4MjNhYzRlZjc4NmYzOWY0OGZjNmY4NDYwMjM2NmFjMjliODM3NDIyIn19fQ==\"}]}"
         );
      }

      return FLASH_SPHERE_PROFILE;
   }

   private static ProfileComponent e() {
      if (ARMORTALITY_SPHERE_PROFILE == null) {
         ARMORTALITY_SPHERE_PROFILE = a(
            "{id:[I;-6803891,-487902205,-2132082558,-1336441356],name:\"\",properties:[{name:\"textures\",value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWE2MmI5ZGU2YTI2Yjg2ODY5Y2EyMmVhNDBmMWJkZTgwYTA0MzBhNTQ1NDdiZWNjZThmZGE4NzA3Nzc3MjU4ZiJ9fX0=\"}]}"
         );
      }

      return ARMORTALITY_SPHERE_PROFILE;
   }

   private static ProfileComponent f() {
      if (IMMORTALITY_SPHERE_PROFILE == null) {
         IMMORTALITY_SPHERE_PROFILE = a(
            "{id:[I;1380367236,-1919012332,-1216974478,-572027876],name:\"\",properties:[{name:\"textures\",value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODNlZDRjZTIzOTMzZTY2ZTA0ZGYxNjA3MDY0NGY3NTk5ZWViNTUzMDdmN2VhZmU4ZDkyZjQwZmIzNTIwODYzYyJ9fX0=\"}]}"
         );
      }

      return IMMORTALITY_SPHERE_PROFILE;
   }

   private static ProfileComponent a(String var0) {
      try {
         NbtCompound var1 = StringNbtReader.readCompound(var0);
         ProfileComponent var2 = (ProfileComponent)ProfileComponent.CODEC.parse(NbtOps.INSTANCE, var1).result().orElse(null);
         if (var2 != null) {
            a(var2);
            return var2;
         }
      } catch (Throwable var3) {
      }

      return null;
   }

   private static void a(ProfileComponent var0) {
      if (var0 != null) {
         try {
            MinecraftClient var1 = MinecraftClient.getInstance();
            if (var1 != null) {
               if (var1.getSkinProvider() != null && var0.getGameProfile() != null) {
                  var1.getSkinProvider().fetchSkinTextures(var0.getGameProfile());
               }

               if (var1.getPlayerSkinCache() != null) {
                  var1.getPlayerSkinCache().getFuture(var0);
               }
            }
         } catch (Throwable var2) {
         }
      }
   }

   private void g() {
      ProfileComponent var1 = a();
      ProfileComponent var2 = b();
      ProfileComponent var3 = c();
      ProfileComponent var4 = d();
      ProfileComponent var5 = e();
      ProfileComponent var6 = f();
      a(var1);
      a(var2);
      a(var3);
      a(var4);
      a(var5);
      a(var6);
      if (SPEED_SPHERE_STACK != null && var1 != null) {
         SPEED_SPHERE_STACK.set(DataComponentTypes.PROFILE, var1);
      }

      if (DAMAGE_SPHERE_STACK != null && var1 != null) {
         DAMAGE_SPHERE_STACK.set(DataComponentTypes.PROFILE, var1);
      }

      if (CERBERUS_SPHERE_STACK != null && var2 != null) {
         CERBERUS_SPHERE_STACK.set(DataComponentTypes.PROFILE, var2);
      }

      if (ARMOR3_DAMAGE2_SPHERE_STACK != null && var3 != null) {
         ARMOR3_DAMAGE2_SPHERE_STACK.set(DataComponentTypes.PROFILE, var3);
      }

      if (ARMOR2_DAMAGE3_SPHERE_STACK != null && var3 != null) {
         ARMOR2_DAMAGE3_SPHERE_STACK.set(DataComponentTypes.PROFILE, var3);
      }

      if (FLASH_SPHERE_STACK != null && var4 != null) {
         FLASH_SPHERE_STACK.set(DataComponentTypes.PROFILE, var4);
      }

      if (ARMORTALITY_SPHERE_STACK != null && var5 != null) {
         ARMORTALITY_SPHERE_STACK.set(DataComponentTypes.PROFILE, var5);
      }

      if (IMMORTALITY_SPHERE_STACK != null && var6 != null) {
         IMMORTALITY_SPHERE_STACK.set(DataComponentTypes.PROFILE, var6);
      }

      for (AutoSborScreen.a var8 : CATALOG_ITEMS) {
         ItemStack var9 = var8.getStack();
         if (var9 != null && var9.isOf(Items.PLAYER_HEAD)) {
            String var10 = var8.getName().toLowerCase(Locale.ROOT);
            if (var10.contains("цербер")) {
               if (var2 != null) {
                  var9.set(DataComponentTypes.PROFILE, var2);
               }
            } else if (!var10.contains("флеш") && !var10.contains("flash")) {
               if (!var10.contains("арморталити") && !var10.contains("armortality")) {
                  if (!var10.contains("имморталити") && !var10.contains("immortality")) {
                     if (var10.contains("броня")) {
                        if (var3 != null) {
                           var9.set(DataComponentTypes.PROFILE, var3);
                        }
                     } else if ((var10.contains("сфера") || var10.contains("урон") || var10.contains("скорост")) && var1 != null) {
                        var9.set(DataComponentTypes.PROFILE, var1);
                     }
                  } else if (var6 != null) {
                     var9.set(DataComponentTypes.PROFILE, var6);
                  }
               } else if (var5 != null) {
                  var9.set(DataComponentTypes.PROFILE, var5);
               }
            } else if (var4 != null) {
               var9.set(DataComponentTypes.PROFILE, var4);
            }
         }
      }
   }

   private void h() {
      for (int var1 = 0; var1 < AutoSbor.TOP_SLOTS.length; var1++) {
         this.a(AutoSbor.TOP_SLOTS, var1);
      }

      for (int var2 = 0; var2 < AutoSbor.INVENTORY_SLOTS.length; var2++) {
         this.a(AutoSbor.INVENTORY_SLOTS, var2);
      }
   }

   private void a(ItemStack[] var1, int var2) {
      ItemStack var3 = var1[var2];
      if (var3 != null && !var3.isEmpty() && var3.isOf(Items.PLAYER_HEAD)) {
         String var4 = d(var3).toLowerCase(Locale.ROOT);
         int var5 = var3.getCount();
         if (var4.contains("цербер") || var4.contains("cerber")) {
            var1[var2] = CERBERUS_SPHERE_STACK.copy();
            var1[var2].setCount(var5);
         } else if (var4.contains("флеш") || var4.contains("flash")) {
            var1[var2] = FLASH_SPHERE_STACK.copy();
            var1[var2].setCount(var5);
         } else if (var4.contains("арморталити") || var4.contains("armortality") || var4.contains("ᴀʀᴍᴏʀᴛᴀʟɪᴛʏ")) {
            var1[var2] = ARMORTALITY_SPHERE_STACK.copy();
            var1[var2].setCount(var5);
         } else if (!var4.contains("имморталити") && !var4.contains("immortality") && !var4.contains("ɪᴍᴍᴏʀᴛᴀʟɪᴛʏ") && !var4.contains("immortal")) {
            if (!var4.contains("броня 3") && !var4.contains("броня iii") || !var4.contains("урон 2") && !var4.contains("урон ii")) {
               if (!var4.contains("броня 2") && !var4.contains("броня ii") || !var4.contains("урон 3") && !var4.contains("урон iii")) {
                  if (var4.contains("урон") || var4.contains("damage") || var4.contains("сил")) {
                     var1[var2] = DAMAGE_SPHERE_STACK.copy();
                     var1[var2].setCount(var5);
                  } else if (var4.contains("сфера") || var4.contains("скорост") || var4.contains("speed")) {
                     var1[var2] = SPEED_SPHERE_STACK.copy();
                     var1[var2].setCount(var5);
                  }
               } else {
                  var1[var2] = ARMOR2_DAMAGE3_SPHERE_STACK.copy();
                  var1[var2].setCount(var5);
               }
            } else {
               var1[var2] = ARMOR3_DAMAGE2_SPHERE_STACK.copy();
               var1[var2].setCount(var5);
            }
         } else {
            var1[var2] = IMMORTALITY_SPHERE_STACK.copy();
            var1[var2].setCount(var5);
         }
      }
   }

   private static boolean b(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (!var0.isOf(Items.PLAYER_HEAD)) {
         return false;
      }

      String var1 = var0.getName().getString();
      return var1.contains("сфера")
         || var1.contains("Сфера")
         || var1.contains("Speed")
         || var1.contains("Скорост")
         || var1.contains("Урон")
         || var1.contains("урон")
         || var1.contains("Цербер")
         || var1.contains("цербер")
         || var1.contains("Броня")
         || var1.contains("броня")
         || var1.contains("Флеш")
         || var1.contains("флеш")
         || var1.contains("Flash")
         || var1.contains("armortality")
         || var1.contains("Armortality")
         || var1.contains("арморталити")
         || var1.contains("immortality")
         || var1.contains("Immortality")
         || var1.contains("имморталити");
   }

   private static ItemStack i() {
      ItemStack var0 = new ItemStack(Items.PLAYER_HEAD);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Сфера на урон 3"));
      List var1 = List.of(Text.literal("§7• §fСила III"), Text.literal(""));
      var0.set(DataComponentTypes.LORE, new LoreComponent(var1));
      var0.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

      try {
         ProfileComponent var2 = a();
         if (var2 != null) {
            var0.set(DataComponentTypes.PROFILE, var2);
         }

         String var3 = "{SkullOwner:{Id:[I;-691918198,261896676,-2097102860,1276043714],Properties:{textures:[{Value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGM5MzY1NjQyYzZlZGRjZmVkZjViNWUxNGUyYmM3MTI1N2Q5ZTRhMzM2M2QxMjNjNmYzM2M1NWNhZmJmNmQifX19\"}]}},\"VV|Protocol1_20_3To1_20_5\":1b,display:{Lore:['{\"italic\":false,\"extra\":[{\"color\":\"gray\",\"text\":\"• \"},{\"color\":\"#e7e7e7\",\"text\":\"Сила III\"}],\"text\":\"\"}','{\"italic\":false,\"text\":\"\"}'],Name:'{\"italic\":false,\"color\":\"#0082fd\",\"text\":\"Сфера на урон 3\"}'},itemServiceId:{name:\"легендарная_сфера_урон\"}}";
         NbtCompound var4 = StringNbtReader.readCompound(var3);
         var0.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(var4));
      } catch (Throwable var5) {
      }

      return var0;
   }

   private static ItemStack j() {
      ItemStack var0 = new ItemStack(Items.PLAYER_HEAD);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Сфера Цербера"));
      List var1 = List.of(Text.literal("§cПроклятие утраты"), Text.literal("§7• §fСпешка I"), Text.literal("§7• §fУрон V"), Text.literal(""));
      var0.set(DataComponentTypes.LORE, new LoreComponent(var1));
      var0.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

      try {
         ProfileComponent var2 = b();
         if (var2 != null) {
            var0.set(DataComponentTypes.PROFILE, var2);
         }

         String var3 = "{SkullOwner:{Id:[I;1404353110,-467519837,-1639700424,-1282774674],Properties:{textures:[{Value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjA5NWE3ZmQ5MGRhYTFiYmU3MDY5MDg5NzQwZTA1ZDBiZmM2NjI5NmVlM2M0MGVlNzFhNGUwYTY2MTZiMmJiYyJ9fX0=\"}]}},SkullOwnerOrig:[I;-1,-2036088060,-1,-2036088060],\"VV|Protocol1_20_3To1_20_5\":1b,display:{Lore:['{\"italic\":false,\"color\":\"red\",\"text\":\"Проклятие утраты\"}','{\"italic\":false,\"extra\":[{\"color\":\"gray\",\"text\":\"• \"},{\"color\":\"#e7e7e7\",\"text\":\"Спешка I\"}],\"text\":\"\"}','{\"italic\":false,\"extra\":[{\"color\":\"gray\",\"text\":\"• \"},{\"color\":\"#e7e7e7\",\"text\":\"Урон V\"}],\"text\":\"\"}','{\"italic\":false,\"text\":\"\"}'],Name:'{\"italic\":false,\"extra\":[{\"bold\":true,\"color\":\"#8d0000\",\"text\":\"С\"},{\"bold\":true,\"color\":\"#960000\",\"text\":\"ф\"},{\"bold\":true,\"color\":\"#9e0000\",\"text\":\"е\"},{\"bold\":true,\"color\":\"#a70000\",\"text\":\"р\"},{\"bold\":true,\"color\":\"#b00000\",\"text\":\"а \"},{\"bold\":true,\"color\":\"#b90000\",\"text\":\"Ц\"},{\"bold\":true,\"color\":\"#b90000\",\"text\":\"е\"},{\"bold\":true,\"color\":\"#b00000\",\"text\":\"р\"},{\"bold\":true,\"color\":\"#a70000\",\"text\":\"б\"},{\"bold\":true,\"color\":\"#9e0000\",\"text\":\"е\"},{\"bold\":true,\"color\":\"#960000\",\"text\":\"р\"},{\"bold\":true,\"color\":\"#8d0000\",\"text\":\"а\"}],\"text\":\"\"}'},itemServiceId:{name:\"сфера_цербера\"},sphereEffect:{effects:'[{\"lvl\":1,\"nbtName\":\"hms-rush\"},{\"lvl\":5,\"nbtName\":\"hms-damage\"}]',isLoss:1b,isMascot:0b,lore:\"[]\",name:\"Cerber\",rank:\"CERBER\"}}";
         NbtCompound var4 = StringNbtReader.readCompound(var3);
         var0.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(var4));
      } catch (Throwable var5) {
      }

      return var0;
   }

   private static ItemStack k() {
      ItemStack var0 = new ItemStack(Items.PLAYER_HEAD);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Сфера на Броня 3 Урон 2"));
      List var1 = List.of(Text.literal("§7• §fБроня III"), Text.literal("§7• §fУрон II"), Text.literal(""));
      var0.set(DataComponentTypes.LORE, new LoreComponent(var1));
      var0.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

      try {
         ProfileComponent var2 = c();
         if (var2 != null) {
            var0.set(DataComponentTypes.PROFILE, var2);
         }

         String var3 = "{SkullOwner:{Id:[I;-2028481669,-405130400,-1468526862,-1002383415],Properties:{textures:[{Value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmFmZjJlYjQ5OGU1YzZhMDQ0ODRmMGM5Zjc4NWI0NDg0NzlhYjIxM2RmOTVlYzkxMTc2YTMwOGExMmFkZDcwIn19fQ==\"}]}},SkullOwnerOrig:[I;-1,-1670196947,-1,-1670196947],\"VV|Protocol1_20_3To1_20_5\":1b,display:{Lore:['{\"italic\":false,\"extra\":[{\"color\":\"gray\",\"text\":\"• \"},{\"color\":\"#e7e7e7\",\"text\":\"Броня III\"}],\"text\":\"\"}','{\"italic\":false,\"extra\":[{\"color\":\"gray\",\"text\":\"• \"},{\"color\":\"#e7e7e7\",\"text\":\"Урон II\"}],\"text\":\"\"}','{\"italic\":false,\"text\":\"\"}'],Name:'{\"italic\":false,\"color\":\"#ff0000\",\"text\":\"Сфера на Броня 3 Урон 2\"}'},itemServiceId:{name:\"мифическая_сфера2\"},sphereEffect:{effects:'[{\"lvl\":3,\"nbtName\":\"hms-armor\"},{\"lvl\":2,\"nbtName\":\"hms-damage\"}]',isLoss:0b,isMascot:0b,lore:\"[]\",name:\"Mythical2\",rank:\"MYTHICAL\"}}";
         NbtCompound var4 = StringNbtReader.readCompound(var3);
         var0.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(var4));
      } catch (Throwable var5) {
      }

      return var0;
   }

   private static ItemStack l() {
      ItemStack var0 = new ItemStack(Items.PLAYER_HEAD);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Сфера на Броня 2 Урон 3"));
      List var1 = List.of(Text.literal("§7• §fБроня II"), Text.literal("§7• §fУрон III"), Text.literal(""));
      var0.set(DataComponentTypes.LORE, new LoreComponent(var1));
      var0.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

      try {
         ProfileComponent var2 = c();
         if (var2 != null) {
            var0.set(DataComponentTypes.PROFILE, var2);
         }

         String var3 = "{SkullOwner:{Id:[I;-2028481669,-405130400,-1468526862,-1002383415],Properties:{textures:[{Value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmFmZjJlYjQ5OGU1YzZhMDQ0ODRmMGM5Zjc4NWI0NDg0NzlhYjIxM2RmOTVlYzkxMTc2YTMwOGExMmFkZDcwIn19fQ==\"}]}},SkullOwnerOrig:[I;-1,-1670196947,-1,-1670196947],\"VV|Protocol1_20_3To1_20_5\":1b,display:{Lore:['{\"italic\":false,\"extra\":[{\"color\":\"gray\",\"text\":\"• \"},{\"color\":\"#e7e7e7\",\"text\":\"Броня II\"}],\"text\":\"\"}','{\"italic\":false,\"extra\":[{\"color\":\"gray\",\"text\":\"• \"},{\"color\":\"#e7e7e7\",\"text\":\"Урон III\"}],\"text\":\"\"}','{\"italic\":false,\"text\":\"\"}'],Name:'{\"italic\":false,\"color\":\"#ff0000\",\"text\":\"Сфера на Броня 2 Урон 3\"}'},itemServiceId:{name:\"мифическая_сфера3\"},sphereEffect:{effects:'[{\"lvl\":2,\"nbtName\":\"hms-armor\"},{\"lvl\":3,\"nbtName\":\"hms-damage\"}]',isLoss:0b,isMascot:0b,lore:\"[]\",name:\"Mythical3\",rank:\"MYTHICAL\"}}";
         NbtCompound var4 = StringNbtReader.readCompound(var3);
         var0.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(var4));
      } catch (Throwable var5) {
      }

      return var0;
   }

   private static ItemStack m() {
      ItemStack var0 = new ItemStack(Items.PLAYER_HEAD);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Сфера Флеша"));
      List var1 = List.of(Text.literal("§cПроклятие утраты"), Text.literal("§7• §fБроня I"), Text.literal("§7• §fСкорость III"), Text.literal(""));
      var0.set(DataComponentTypes.LORE, new LoreComponent(var1));
      var0.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

      try {
         ProfileComponent var2 = d();
         if (var2 != null) {
            var0.set(DataComponentTypes.PROFILE, var2);
         }

         String var3 = "{AttributeModifiers:[{Amount:1.0d,AttributeName:\"minecraft:generic.armor\",Name:\" generic.armor\",Operation:0,Slot:\"offhand\",UUID:[I;445760916,-488289763,-1860756847,-980098331]}],HideFlags:2,SkullOwner:{Id:[I;-372484930,-74172931,-1856149270,-1219017950],Properties:{textures:[{Value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzc0MDBlYTE5ZGJkODRmNzVjMzlhZDY4MjNhYzRlZjc4NmYzOWY0OGZjNmY4NDYwMjM2NmFjMjliODM3NDIyIn19fQ==\"}]}},SkullOwnerOrig:[I;0,1411878768,0,1411878768],\"VV|Protocol1_20_3To1_20_5\":1b,display:{Lore:['{\"italic\":false,\"color\":\"red\",\"text\":\"Проклятие утраты\"}','{\"italic\":false,\"extra\":[{\"color\":\"gray\",\"text\":\"• \"},{\"color\":\"#e7e7e7\",\"text\":\"Броня I\"}],\"text\":\"\"}','{\"italic\":false,\"extra\":[{\"color\":\"gray\",\"text\":\"• \"},{\"color\":\"#e7e7e7\",\"text\":\"Скорость III\"}],\"text\":\"\"}','{\"italic\":false,\"text\":\"\"}'],Name:'{\"italic\":false,\"extra\":[{\"bold\":true,\"color\":\"#90bbc4\",\"text\":\"С\"},{\"bold\":true,\"color\":\"#9ac7d1\",\"text\":\"ф\"},{\"bold\":true,\"color\":\"#a3d4de\",\"text\":\"е\"},{\"bold\":true,\"color\":\"#ade0eb\",\"text\":\"р\"},{\"bold\":true,\"color\":\"#b6edf8\",\"text\":\"а \"},{\"bold\":true,\"color\":\"#b9edf8\",\"text\":\"Ф\"},{\"bold\":true,\"color\":\"#b4e2eb\",\"text\":\"л\"},{\"bold\":true,\"color\":\"#afd7de\",\"text\":\"е\"},{\"bold\":true,\"color\":\"#aaccd1\",\"text\":\"ш\"},{\"bold\":true,\"color\":\"#a5c1c4\",\"text\":\"а\"}],\"text\":\"\"}'},itemServiceId:{name:\"сфера_флеша\"},sphereEffect:{effects:'[{\"lvl\":1,\"nbtName\":\"hms-armor\"},{\"lvl\":3,\"nbtName\":\"hms-speed\"}]',isLoss:1b,isMascot:0b,lore:\"[]\",mascot:{enabled:0b,title:\"§x§9§0§b§b§c§4§lС§x§9§a§c§7§d§1§lф§x§a§3§d§4§d§e§lе§x§a§d§e§0§e§b§lр§x§b§6§e§d§f§8§lа §x§b§9§e§d§f§8§lФ§x§b§4§e§2§e§b§lл§x§a§f§d§7§d§e§lе§x§a§a§c§c§d§1§lш§x§a§5§c§1§c§4§lа\"},name:\"Flash\",rank:\"FLASH\",title:\"§x§9§0§b§b§c§4§lС§x§9§a§c§7§d§1§lф§x§a§3§d§4§d§e§lе§x§a§d§e§0§e§b§lр§x§b§6§e§d§f§8§lа §x§b§9§e§d§f§8§lФ§x§b§4§e§2§e§b§lл§x§a§f§d§7§d§e§lе§x§a§a§c§c§d§1§lш§x§a§5§c§1§c§4§lа\"}}";
         NbtCompound var4 = StringNbtReader.readCompound(var3);
         var0.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(var4));
      } catch (Throwable var5) {
      }

      return var0;
   }

   private static ItemStack n() {
      ItemStack var0 = new ItemStack(Items.PLAYER_HEAD);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Сфера Арморталити"));
      List var1 = List.of(Text.literal("§7• §fУрон II"), Text.literal("§7• §fМакс. здоровье II"), Text.literal("§7• §fБроня II"), Text.literal(""));
      var0.set(DataComponentTypes.LORE, new LoreComponent(var1));
      var0.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

      try {
         ProfileComponent var2 = e();
         if (var2 != null) {
            var0.set(DataComponentTypes.PROFILE, var2);
         }

         String var3 = "{AttributeModifiers:[{Amount:2.0d,AttributeName:\"minecraft:generic.armor\",Name:\" generic.armor\",Operation:0,Slot:\"offhand\",UUID:[I;445760916,-488289763,-1860756847,-980098331]}],HideFlags:2,SkullOwner:{Id:[I;-6803891,-487902205,-2132082558,-1336441356],Properties:{textures:[{Value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWE2MmI5ZGU2YTI2Yjg2ODY5Y2EyMmVhNDBmMWJkZTgwYTA0MzBhNTQ1NDdiZWNjZThmZGE4NzA3Nzc3MjU4ZiJ9fX0=\"}]}},SkullOwnerOrig:[I;-1,-1383772854,-1,-1383772854],\"VV|Protocol1_20_3To1_20_5\":1b,display:{Lore:['{\"italic\":false,\"extra\":[{\"color\":\"gray\",\"text\":\"• \"},{\"color\":\"#e7e7e7\",\"text\":\"Урон II\"}],\"text\":\"\"}','{\"italic\":false,\"extra\":[{\"color\":\"gray\",\"text\":\"• \"},{\"color\":\"#e7e7e7\",\"text\":\"Макс. здоровье II\"}],\"text\":\"\"}','{\"italic\":false,\"extra\":[{\"color\":\"gray\",\"text\":\"• \"},{\"color\":\"#e7e7e7\",\"text\":\"Броня II\"}],\"text\":\"\"}','{\"italic\":false,\"text\":\"\"}'],Name:'{\"italic\":false,\"extra\":[{\"bold\":true,\"color\":\"#283451\",\"text\":\"С\"},{\"bold\":true,\"color\":\"#2b3856\",\"text\":\"ф\"},{\"bold\":true,\"color\":\"#2f3c5b\",\"text\":\"е\"},{\"bold\":true,\"color\":\"#324060\",\"text\":\"р\"},{\"bold\":true,\"color\":\"#354365\",\"text\":\"а \"},{\"bold\":true,\"color\":\"#39476a\",\"text\":\"ᴀ\"},{\"bold\":true,\"color\":\"#3c4b6f\",\"text\":\"ʀ\"},{\"bold\":true,\"color\":\"#3f4f74\",\"text\":\"ᴍ\"},{\"bold\":true,\"color\":\"#3f4f74\",\"text\":\"ᴏ\"},{\"bold\":true,\"color\":\"#3c4b6f\",\"text\":\"ʀ\"},{\"bold\":true,\"color\":\"#39476a\",\"text\":\"ᴛ\"},{\"bold\":true,\"color\":\"#354365\",\"text\":\"ᴀ\"},{\"bold\":true,\"color\":\"#324060\",\"text\":\"ʟ\"},{\"bold\":true,\"color\":\"#2f3c5b\",\"text\":\"ɪ\"},{\"bold\":true,\"color\":\"#2b3856\",\"text\":\"ᴛ\"},{\"bold\":true,\"color\":\"#283451\",\"text\":\"ʏ\"}],\"text\":\"\"}'},itemServiceId:{name:\"сфера_арморталити\"},sphereEffect:{effects:'[{\"lvl\":2,\"nbtName\":\"hms-damage\"},{\"lvl\":2,\"nbtName\":\"hms-health\"},{\"lvl\":2,\"nbtName\":\"hms-armor\"}]',isLoss:0b,isMascot:0b,lore:\"[]\",mascot:{enabled:0b,title:\"%rank%\"},name:\"Armortality\",rank:\"ARMORTALITY\",title:\"%rank%\"}}";
         NbtCompound var4 = StringNbtReader.readCompound(var3);
         var0.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(var4));
      } catch (Throwable var5) {
      }

      return var0;
   }

   private static ItemStack o() {
      ItemStack var0 = new ItemStack(Items.PLAYER_HEAD);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Сфера Имморталити"));
      List var1 = List.of(Text.literal("§7• §fСкорость II"), Text.literal("§7• §fУрон III"), Text.literal(""));
      var0.set(DataComponentTypes.LORE, new LoreComponent(var1));
      var0.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

      try {
         ProfileComponent var2 = f();
         if (var2 != null) {
            var0.set(DataComponentTypes.PROFILE, var2);
         }

         String var3 = "{SkullOwner:{Id:[I;1380367236,-1919012332,-1216974478,-572027876],Properties:{textures:[{Value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODNlZDRjZTIzOTMzZTY2ZTA0ZGYxNjA3MDY0NGY3NTk5ZWViNTUzMDdmN2VhZmU4ZDkyZjQwZmIzNTIwODYzYyJ9fX0=\"}]}},SkullOwnerOrig:[I;0,349453861,0,349453861],\"VV|Protocol1_20_3To1_20_5\":1b,display:{Lore:['{\"italic\":false,\"extra\":[{\"color\":\"gray\",\"text\":\"• \"},{\"color\":\"#e7e7e7\",\"text\":\"Скорость II\"}],\"text\":\"\"}','{\"italic\":false,\"extra\":[{\"color\":\"gray\",\"text\":\"• \"},{\"color\":\"#e7e7e7\",\"text\":\"Урон III\"}],\"text\":\"\"}','{\"italic\":false,\"text\":\"\"}'],Name:'{\"italic\":false,\"extra\":[{\"color\":\"#4900fb\",\"text\":\"С\"},{\"color\":\"#6100ef\",\"text\":\"ф\"},{\"color\":\"#7800e2\",\"text\":\"е\"},{\"color\":\"#9000d6\",\"text\":\"р\"},{\"color\":\"#a800c9\",\"text\":\"а \"},{\"color\":\"#c000bd\",\"text\":\"ɪ\"},{\"color\":\"#d700b1\",\"text\":\"ᴍ\"},{\"color\":\"#ef00a4\",\"text\":\"ᴍ\"},{\"color\":\"#ef00a4\",\"text\":\"ᴏ\"},{\"color\":\"#d700b1\",\"text\":\"ʀ\"},{\"color\":\"#c000bd\",\"text\":\"ᴛ\"},{\"color\":\"#a800c9\",\"text\":\"ᴀ\"},{\"color\":\"#9000d6\",\"text\":\"ʟ\"},{\"color\":\"#7800e2\",\"text\":\"ɪ\"},{\"color\":\"#6100ef\",\"text\":\"ᴛ\"},{\"color\":\"#4900fb\",\"text\":\"ʏ\"}],\"text\":\"\"}'},itemServiceId:{name:\"сфера_имморталити\"},sphereEffect:{effects:'[{\"lvl\":2,\"nbtName\":\"hms-speed\"},{\"lvl\":3,\"nbtName\":\"hms-damage\"}]',isLoss:0b,isMascot:0b,lore:\"[]\",mascot:{enabled:0b,title:\"%rank%\"},name:\"Immortal\",rank:\"IMMORTAL\",title:\"%rank%\"}}";
         NbtCompound var4 = StringNbtReader.readCompound(var3);
         var0.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(var4));
      } catch (Throwable var5) {
      }

      return var0;
   }

   private static ItemStack p() {
      ItemStack var0 = new ItemStack(Items.ENDER_PEARL);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Эндермен"));
      var0.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
      return var0;
   }

   private static ItemStack q() {
      ItemStack var0 = new ItemStack(Items.FEATHER);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Гравитация"));
      var0.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
      return var0;
   }

   private static ItemStack r() {
      ItemStack var0 = new ItemStack(Items.POTION);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Справедливость"));

      try {
         var0.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Potions.WATER));
      } catch (Throwable var2) {
      }

      return var0;
   }

   private static ItemStack s() {
      ItemStack var0 = new ItemStack(Items.SNOWBALL);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Снежок"));
      var0.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
      return var0;
   }

   private static ItemStack t() {
      ItemStack var0 = new ItemStack(Items.PLAYER_HEAD);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Сфера на скорость 3"));
      List var1 = List.of(Text.literal("§7• §fСкорость III"), Text.literal(""));
      var0.set(DataComponentTypes.LORE, new LoreComponent(var1));
      var0.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

      try {
         ProfileComponent var2 = a();
         if (var2 != null) {
            var0.set(DataComponentTypes.PROFILE, var2);
         }

         String var3 = "{SkullOwner:{Id:[I;-691918198,261896676,-2097102860,1276043714],Properties:{textures:[{Value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGM5MzY1NjQyYzZlZGRjZmVkZjViNWUxNGUyYmM3MTI1N2Q5ZTRhMzM2M2QxMjNjNmYzM2M1NWNhZmJmNmQifX19\"}]}},SkullOwnerOrig:[I;-1,-1891592620,-1,-1891592620],\"VV|Protocol1_20_3To1_20_5\":1b,display:{Lore:['{\"italic\":false,\"extra\":[{\"color\":\"gray\",\"text\":\"• \"},{\"color\":\"#e7e7e7\",\"text\":\"Скорость III\"}],\"text\":\"\"}','{\"italic\":false,\"text\":\"\"}'],Name:'{\"italic\":false,\"color\":\"#0082fd\",\"text\":\"Сфера на скорость 3\"}'},itemServiceId:{name:\"легендарная_сфера_скорость\"},sphereEffect:{effects:'[{\"lvl\":3,\"nbtName\":\"hms-speed\"}]',isLoss:0b,isMascot:0b,lore:\"[]\",mascot:{enabled:1b,title:\"%rank%\"},name:\"Speed3\",rank:\"LEGENDARY\",title:\"%rank%\"}}";
         NbtCompound var4 = StringNbtReader.readCompound(var3);
         var0.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(var4));
      } catch (Throwable var5) {
      }

      return var0;
   }

   private static ItemStack u() {
      ItemStack var0 = new ItemStack(Items.EXPERIENCE_BOTTLE);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Опыт 50"));
      var0.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
      return var0;
   }

   private static ItemStack v() {
      ItemStack var0 = new ItemStack(Items.NETHERITE_SWORD);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Меч ETERNITY"));
      List var1 = List.of(
         Text.literal("§7• Острота VII"),
         Text.literal("§7• Заговор огня III"),
         Text.literal("§7• Прочность III"),
         Text.literal("§7• Починка I"),
         Text.literal("§7• Отдача II"),
         Text.literal("§7• Разящий клинок III"),
         Text.literal("")
      );
      var0.set(DataComponentTypes.LORE, new LoreComponent(var1));

      try {
         MinecraftClient var2 = MinecraftClient.getInstance();
         if (var2 != null && var2.world != null) {
            Registry var3 = var2.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
            Builder var4 = new Builder(ItemEnchantmentsComponent.DEFAULT);
            Reference var5 = var3.getOrThrow(Enchantments.SHARPNESS);
            Reference var6 = var3.getOrThrow(Enchantments.FIRE_ASPECT);
            Reference var7 = var3.getOrThrow(Enchantments.UNBREAKING);
            Reference var8 = var3.getOrThrow(Enchantments.MENDING);
            Reference var9 = var3.getOrThrow(Enchantments.KNOCKBACK);
            Reference var10 = var3.getOrThrow(Enchantments.SWEEPING_EDGE);
            var4.set(var5, 7);
            var4.set(var6, 3);
            var4.set(var7, 3);
            var4.set(var8, 1);
            var4.set(var9, 2);
            var4.set(var10, 3);
            var0.set(DataComponentTypes.ENCHANTMENTS, var4.build());
         }
      } catch (Throwable var11) {
      }

      var0.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
      return var0;
   }

   private static ItemStack w() {
      ItemStack var0 = new ItemStack(Items.NETHERITE_HELMET);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Шлем ETERNITY"));
      List var1 = List.of(
         Text.literal("§7• Подводное дыхание III"),
         Text.literal("§7• Защита от снарядов V"),
         Text.literal("§7• Прочность V"),
         Text.literal("§7• Подводник"),
         Text.literal("§7• Защита V"),
         Text.literal("§7• Взрывоустойчивость V"),
         Text.literal("§7• Огнеупорность V"),
         Text.literal("§7• Непробиваемый I"),
         Text.literal("")
      );
      var0.set(DataComponentTypes.LORE, new LoreComponent(var1));

      try {
         MinecraftClient var2 = MinecraftClient.getInstance();
         if (var2 != null && var2.world != null) {
            Registry var3 = var2.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
            Builder var4 = new Builder(ItemEnchantmentsComponent.DEFAULT);
            var4.set(var3.getOrThrow(Enchantments.RESPIRATION), 3);
            var4.set(var3.getOrThrow(Enchantments.PROJECTILE_PROTECTION), 5);
            var4.set(var3.getOrThrow(Enchantments.UNBREAKING), 5);
            var4.set(var3.getOrThrow(Enchantments.AQUA_AFFINITY), 1);
            var4.set(var3.getOrThrow(Enchantments.PROTECTION), 5);
            var4.set(var3.getOrThrow(Enchantments.BLAST_PROTECTION), 5);
            var4.set(var3.getOrThrow(Enchantments.FIRE_PROTECTION), 5);
            var0.set(DataComponentTypes.ENCHANTMENTS, var4.build());
         }
      } catch (Throwable var5) {
      }

      var0.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
      return var0;
   }

   private static ItemStack x() {
      ItemStack var0 = new ItemStack(Items.NETHERITE_CHESTPLATE);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Нагрудник ETERNITY"));
      List var1 = List.of(
         Text.literal("§7• Защита V"),
         Text.literal("§7• Прочность V"),
         Text.literal("§7• Защита от снарядов V"),
         Text.literal("§7• Взрывоустойчивость V"),
         Text.literal("§7• Огнеупорность V"),
         Text.literal("§7• Шипы III"),
         Text.literal("§7• Непробиваемый I"),
         Text.literal("")
      );
      var0.set(DataComponentTypes.LORE, new LoreComponent(var1));

      try {
         MinecraftClient var2 = MinecraftClient.getInstance();
         if (var2 != null && var2.world != null) {
            Registry var3 = var2.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
            Builder var4 = new Builder(ItemEnchantmentsComponent.DEFAULT);
            var4.set(var3.getOrThrow(Enchantments.PROTECTION), 5);
            var4.set(var3.getOrThrow(Enchantments.UNBREAKING), 5);
            var4.set(var3.getOrThrow(Enchantments.PROJECTILE_PROTECTION), 5);
            var4.set(var3.getOrThrow(Enchantments.BLAST_PROTECTION), 5);
            var4.set(var3.getOrThrow(Enchantments.FIRE_PROTECTION), 5);
            var4.set(var3.getOrThrow(Enchantments.THORNS), 3);
            var0.set(DataComponentTypes.ENCHANTMENTS, var4.build());
         }
      } catch (Throwable var5) {
      }

      var0.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
      return var0;
   }

   private static ItemStack y() {
      ItemStack var0 = new ItemStack(Items.NETHERITE_LEGGINGS);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Поножи ETERNITY"));
      List var1 = List.of(
         Text.literal("§7• Защита V"),
         Text.literal("§7• Прочность V"),
         Text.literal("§7• Защита от снарядов V"),
         Text.literal("§7• Взрывоустойчивость V"),
         Text.literal("§7• Огнеупорность V"),
         Text.literal("§7• Проворство III"),
         Text.literal("§7• Скорость души III"),
         Text.literal("§7• Непробиваемый I"),
         Text.literal("")
      );
      var0.set(DataComponentTypes.LORE, new LoreComponent(var1));

      try {
         MinecraftClient var2 = MinecraftClient.getInstance();
         if (var2 != null && var2.world != null) {
            Registry var3 = var2.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
            Builder var4 = new Builder(ItemEnchantmentsComponent.DEFAULT);
            var4.set(var3.getOrThrow(Enchantments.PROTECTION), 5);
            var4.set(var3.getOrThrow(Enchantments.UNBREAKING), 5);
            var4.set(var3.getOrThrow(Enchantments.PROJECTILE_PROTECTION), 5);
            var4.set(var3.getOrThrow(Enchantments.BLAST_PROTECTION), 5);
            var4.set(var3.getOrThrow(Enchantments.FIRE_PROTECTION), 5);
            var4.set(var3.getOrThrow(Enchantments.SOUL_SPEED), 3);
            var0.set(DataComponentTypes.ENCHANTMENTS, var4.build());
         }
      } catch (Throwable var5) {
      }

      var0.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
      return var0;
   }

   private static ItemStack z() {
      ItemStack var0 = new ItemStack(Items.NETHERITE_BOOTS);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Ботинки ETERNITY"));
      List var1 = List.of(
         Text.literal("§7• Защита V"),
         Text.literal("§7• Прочность V"),
         Text.literal("§7• Защита от снарядов V"),
         Text.literal("§7• Взрывоустойчивость V"),
         Text.literal("§7• Огнеупорность V"),
         Text.literal("§7• Невесомость IV"),
         Text.literal("§7• Глубинный шагоход III"),
         Text.literal("§7• Скорость души III"),
         Text.literal("§7• Непробиваемый I"),
         Text.literal("")
      );
      var0.set(DataComponentTypes.LORE, new LoreComponent(var1));

      try {
         MinecraftClient var2 = MinecraftClient.getInstance();
         if (var2 != null && var2.world != null) {
            Registry var3 = var2.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
            Builder var4 = new Builder(ItemEnchantmentsComponent.DEFAULT);
            var4.set(var3.getOrThrow(Enchantments.PROTECTION), 5);
            var4.set(var3.getOrThrow(Enchantments.UNBREAKING), 5);
            var4.set(var3.getOrThrow(Enchantments.PROJECTILE_PROTECTION), 5);
            var4.set(var3.getOrThrow(Enchantments.BLAST_PROTECTION), 5);
            var4.set(var3.getOrThrow(Enchantments.FIRE_PROTECTION), 5);
            var4.set(var3.getOrThrow(Enchantments.FEATHER_FALLING), 4);
            var4.set(var3.getOrThrow(Enchantments.DEPTH_STRIDER), 3);
            var4.set(var3.getOrThrow(Enchantments.SOUL_SPEED), 3);
            var0.set(DataComponentTypes.ENCHANTMENTS, var4.build());
         }
      } catch (Throwable var5) {
      }

      var0.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
      return var0;
   }

   private static ItemStack A() {
      ItemStack var0 = new ItemStack(Items.POTION);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Зелье исцеления"));
      List var1 = List.of(Text.literal("§9Исцеление II"), Text.literal(""));
      var0.set(DataComponentTypes.LORE, new LoreComponent(var1));

      try {
         var0.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Potions.STRONG_HEALING));
      } catch (Throwable var3) {
      }

      return var0;
   }

   private static ItemStack B() {
      ItemStack var0 = new ItemStack(Items.NETHERITE_PICKAXE);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Кирка STINGER"));
      List var1 = List.of(
         Text.literal("§7• Эффективность VII"),
         Text.literal("§7• Прочность V"),
         Text.literal("§7• Шелковое касание I"),
         Text.literal("§7• Починка I"),
         Text.literal("")
      );
      var0.set(DataComponentTypes.LORE, new LoreComponent(var1));
      var0.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
      return var0;
   }

   private static ItemStack C() {
      ItemStack var0 = new ItemStack(Items.NETHERITE_PICKAXE);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Кирка ETERNITY"));
      List var1 = List.of(
         Text.literal("§7• Эффективность X"), Text.literal("§7• Удача IV"), Text.literal("§7• Прочность V"), Text.literal("§7• Починка I"), Text.literal("")
      );
      var0.set(DataComponentTypes.LORE, new LoreComponent(var1));
      var0.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
      return var0;
   }

   private static ItemStack D() {
      ItemStack var0 = new ItemStack(Items.PRISMARINE_SHARD);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Взрывная трапка"));
      var0.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
      return var0;
   }

   private static ItemStack E() {
      ItemStack var0 = new ItemStack(Items.FIRE_CHARGE);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Взрывная штучка"));
      var0.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
      return var0;
   }

   private static ItemStack F() {
      ItemStack var0 = new ItemStack(Items.NETHER_STAR);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Стан"));
      var0.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
      return var0;
   }

   private static ItemStack G() {
      ItemStack var0 = new ItemStack(Items.POTION);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Зелье черепашьей мощи"));
      List var1 = List.of(Text.literal("§9Замедление IV (00:40)"), Text.literal("§9Сопротивление III (00:40)"), Text.literal(""));
      var0.set(DataComponentTypes.LORE, new LoreComponent(var1));

      try {
         var0.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Potions.LONG_TURTLE_MASTER));
      } catch (Throwable var3) {
      }

      return var0;
   }

   private static ItemStack H() {
      ItemStack var0 = new ItemStack(Items.POTION);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Улучшенное зелье скорости"));
      List var1 = List.of(Text.literal("§9Скорость II (01:30)"), Text.literal(""));
      var0.set(DataComponentTypes.LORE, new LoreComponent(var1));

      try {
         var0.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Potions.STRONG_SWIFTNESS));
      } catch (Throwable var3) {
      }

      return var0;
   }

   private static ItemStack I() {
      ItemStack var0 = new ItemStack(Items.POTION);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Улучшенное зелье силы"));

      try {
         var0.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Potions.STRONG_STRENGTH));
      } catch (Throwable var2) {
      }

      return var0;
   }

   private static ItemStack J() {
      ItemStack var0 = new ItemStack(Items.NETHERITE_SWORD);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Меч INFINITY"));
      var0.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
      return var0;
   }

   private static ItemStack K() {
      ItemStack var0 = new ItemStack(Items.NETHERITE_HELMET);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Шлем INFINITY"));
      var0.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
      return var0;
   }

   private static ItemStack L() {
      ItemStack var0 = new ItemStack(Items.NETHERITE_CHESTPLATE);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Нагрудник INFINITY"));
      var0.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
      return var0;
   }

   private static ItemStack M() {
      ItemStack var0 = new ItemStack(Items.NETHERITE_LEGGINGS);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Поножи INFINITY"));
      var0.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
      return var0;
   }

   private static ItemStack N() {
      ItemStack var0 = new ItemStack(Items.NETHERITE_BOOTS);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Ботинки INFINITY"));
      var0.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
      return var0;
   }

   private static ItemStack O() {
      ItemStack var0 = new ItemStack(Items.NETHERITE_SWORD);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Меч Цербера"));
      var0.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
      return var0;
   }

   private static ItemStack P() {
      ItemStack var0 = new ItemStack(Items.PINK_SHULKER_BOX);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Рюкзак 1 уровень"));
      return var0;
   }

   private static ItemStack Q() {
      ItemStack var0 = new ItemStack(Items.LIGHT_BLUE_SHULKER_BOX);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Рюкзак 2 уровень"));
      return var0;
   }

   private static ItemStack R() {
      ItemStack var0 = new ItemStack(Items.RED_SHULKER_BOX);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Рюкзак 3 уровень"));
      return var0;
   }

   private static ItemStack S() {
      ItemStack var0 = new ItemStack(Items.MAGENTA_SHULKER_BOX);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Рюкзак 4 уровень"));
      return var0;
   }

   private static ItemStack T() {
      ItemStack var0 = new ItemStack(Items.WITHER_SKELETON_SKULL);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Вампиризм"));
      var0.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
      return var0;
   }

   private static ItemStack U() {
      ItemStack var0 = new ItemStack(Items.POPPED_CHORUS_FRUIT);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Трапка"));
      var0.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
      return var0;
   }

   private static ItemStack V() {
      ItemStack var0 = new ItemStack(Items.POTION);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Зелье победителя"));
      var0.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
      return var0;
   }

   private static ItemStack W() {
      ItemStack var0 = new ItemStack(Items.TOTEM_OF_UNDYING);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Талисман Infinity"));
      var0.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
      return var0;
   }

   private static ItemStack X() {
      ItemStack var0 = new ItemStack(Items.TOTEM_OF_UNDYING);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Талисман Eternity"));
      var0.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
      return var0;
   }

   private static ItemStack Y() {
      ItemStack var0 = new ItemStack(Items.TOTEM_OF_UNDYING);
      var0.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Талисман Stinger"));
      var0.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
      return var0;
   }

   public static int a(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return 64;
      }

      if (var0.isOf(Items.EXPERIENCE_BOTTLE) || var0.isOf(Items.ENDER_PEARL)) {
         return 128;
      }

      if (var0.isOf(Items.ELYTRA)) {
         return 1;
      }

      if (!var0.isOf(Items.PINK_SHULKER_BOX)
         && !var0.isOf(Items.LIGHT_BLUE_SHULKER_BOX)
         && !var0.isOf(Items.RED_SHULKER_BOX)
         && !var0.isOf(Items.MAGENTA_SHULKER_BOX)) {
         if (!var0.isOf(Items.NETHERITE_SWORD)
            && !var0.isOf(Items.NETHERITE_PICKAXE)
            && !var0.isOf(Items.NETHERITE_AXE)
            && !var0.isOf(Items.NETHERITE_HELMET)
            && !var0.isOf(Items.NETHERITE_CHESTPLATE)
            && !var0.isOf(Items.NETHERITE_LEGGINGS)
            && !var0.isOf(Items.NETHERITE_BOOTS)
            && !var0.isOf(Items.DIAMOND_SWORD)
            && !var0.isOf(Items.DIAMOND_PICKAXE)
            && !var0.isOf(Items.DIAMOND_AXE)
            && !var0.isOf(Items.DIAMOND_HELMET)
            && !var0.isOf(Items.DIAMOND_CHESTPLATE)
            && !var0.isOf(Items.DIAMOND_LEGGINGS)
            && !var0.isOf(Items.DIAMOND_BOOTS)) {
            for (AutoSborScreen.a var2 : CATALOG_ITEMS) {
               if (a(var2.getStack(), var0)) {
                  return var2.getMaxStack();
               }
            }

            return 64;
         } else {
            return 1;
         }
      } else {
         return 1;
      }
   }

   private static int[] a(int var0) {
      if (var0 >= 128) {
         return new int[]{1, 32, 64, 128};
      } else if (var0 >= 64) {
         return new int[]{1, 16, 32, 64};
      } else {
         return var0 >= 16 ? new int[]{1, 4, 8, 16} : new int[]{1};
      }
   }

   private static void Z() {
      try {
         if (GUI_RENDERER_FIELD != null && FOG_RENDERER_FIELD != null) {
            MinecraftClient var0 = MinecraftClient.getInstance();
            if (var0 != null && var0.gameRenderer != null) {
               GuiRenderer var1 = (GuiRenderer)GUI_RENDERER_FIELD.get(var0.gameRenderer);
               FogRenderer var2 = (FogRenderer)FOG_RENDERER_FIELD.get(var0.gameRenderer);
               if (var1 != null && var2 != null) {
                  var1.render(var2.getFogBuffer(FogType.NONE));
                  var1.incrementFrame();
               }
            }
         }
      } catch (Throwable var3) {
      }
   }

   @Override
   public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      MsdfManager.a();
      GuiBatch.begin();

      try {
         this.renderContent(context, mouseX, mouseY, deltaTicks);
      } finally {
         GuiBatch.end();
      }
   }

   private void renderContent(DrawContext var1, int var2, int var3, float var4) {
      this.hoveredTooltip = null;
      if (this.closing) {
         this.openAnim.animate(0.0F, var4, 4.8F);
         if (this.openAnim.getValue() <= 0.005F) {
            this.ac();
            return;
         }
      } else {
         this.openAnim.animate(1.0F, var4, 4.2F);
      }

      float var5 = Math.clamp(this.openAnim.getValue(), 0.0F, 1.0F);
      if (!(var5 <= 0.005F)) {
         if (this.parent != null) {
            if (this.parent instanceof ClickGui) {
               ClickGui.externalSlideProg = var5;
            }

            this.parent.render(var1, -1, -1, var4);
         } else if (MinecraftClient.getInstance().world == null) {
            var1.fill(0, 0, this.width, this.height, -16118766);
         } else {
            int var6 = (int)(48.0F * Math.clamp(var5, 0.0F, 1.0F));
            if (var6 > 0) {
               var1.fill(0, 0, this.width, this.height, var6 << 24);
            }
         }

         float var30 = RenderUtil.e();
         float var7 = var2 * var30;
         float var8 = var3 * var30;
         float var9 = RenderUtil.a();
         float var10 = RenderUtil.b();
         boolean var11 = this.selectedSlotType != -1 && this.aa() != null && !this.aa().isEmpty();
         if (var11) {
            this.lastSelectedStack = this.aa().copy();
            this.lastSelectedSlotType = this.selectedSlotType;
            this.lastSelectedSlotIndex = this.selectedSlotIndex;
         }

         this.settingsAnim.animate(var11 ? 1.0F : 0.0F, var4, 4.5F);
         float var12 = Math.clamp(this.settingsAnim.getValue(), 0.0F, 1.0F);
         float var13 = 407.0F + (var12 > 0.01F ? 142.0F * var12 : 0.0F);
         float var14 = 236.0F;
         float var15 = (var9 - var13) / 2.0F;
         float var16 = var9 - var15 + 50.0F;
         float var17 = var15 + (1.0F - var5) * var16;
         float var18 = var17 + 120.0F + 7.0F;
         float var19 = (var10 - var14) / 2.0F;
         float var20 = var17;
         float var21 = var19;
         float var22 = var18 + 280.0F + 7.0F;
         float var23 = var19;
         float var24 = var19 + 195.0F + 7.0F;
         this.b(var1, var20, var21, var7, var8, var5);
         this.a(var1, var18, var19, var7, var8, var5);
         this.d(var1, var18, var24, var7, var8, var5);
         if (var12 > 0.01F) {
            this.c(var1, var22, var23, var7, var8, var5 * var12);
         }

         Z();
         if (this.hoveredTooltip != null && !this.hoveredTooltip.isEmpty() && var5 > 0.85F && !this.closing) {
            float var25 = Msdf.a(MsdfManager.BOLD, this.hoveredTooltip, 5.5F);
            float var26 = var25 + 12.0F;
            float var27 = 15.0F;
            float var28 = var7 + 10.0F;
            float var29 = var8 - 16.0F;
            if (var28 + var26 > var9 - 4.0F) {
               var28 = var7 - var26 - 4.0F;
            }

            if (var29 < 4.0F) {
               var29 = var8 + 10.0F;
            }

            HudConfig.a("autosbor_tooltip", var28, var29, var26, var27, 4.0F, new Color(15, 17, 22, (int)(245.0F * var5)));
            RenderUtil.a(var28, var29, var26, var27, 4.0F, 0.5F, new Color[]{new Color(255, 255, 255, (int)(35.0F * var5))});
            RenderUtil.a(var1, MsdfManager.BOLD, var28 + 6.0F, var29 + 4.5F, this.hoveredTooltip, 5.5F, new Color(245, 245, 250, (int)(255.0F * var5)));
         }
      }
   }

   private void a(DrawContext var1, float var2, float var3, float var4, float var5, float var6) {
      Color var7 = Theme.getInstance().getAccent();
      Color var8 = new Color(var7.getRed(), var7.getGreen(), var7.getBlue(), (int)(255.0F * var6));
      HudConfig.a("autosbor_modal", var2, var3, 280.0F, 195.0F, 8.0F, new Color(15, 17, 22, (int)(245.0F * var6)));
      RenderUtil.a(var2, var3, 280.0F, 195.0F, 8.0F, 0.5F, new Color[]{new Color(255, 255, 255, (int)(35.0F * var6))});
      RenderUtil.a(var1, MsdfManager.BOLD, var2 + 10.0F, var3 + 7.5F, "Авто-Сбор", 7.5F, new Color(245, 245, 250, (int)(255.0F * var6)));
      float var9 = var2 + 280.0F - 12.0F;
      float var10 = var3 + 11.5F;
      this.closeBtnBox[0] = var9 - 6.0F;
      this.closeBtnBox[1] = var10 - 6.0F;
      this.closeBtnBox[2] = 12.0F;
      this.closeBtnBox[3] = 12.0F;
      boolean var11 = var4 >= this.closeBtnBox[0]
         && var4 <= this.closeBtnBox[0] + this.closeBtnBox[2]
         && var5 >= this.closeBtnBox[1]
         && var5 <= this.closeBtnBox[1] + this.closeBtnBox[3];
      BufferBuilder var12 = ColorPipeline.a();
      if (var12 != null) {
         Color var13 = var11 ? var8 : new Color(170, 175, 185);
         int var14 = (int)(255.0F * var6) << 24 | var13.getRGB() & 16777215;
         ColorPipeline.a(var12, var9 - 3.0F, var10 - 3.0F, var9 + 3.0F, var10 + 3.0F, 1.3F, var14);
         ColorPipeline.a(var12, var9 + 3.0F, var10 - 3.0F, var9 - 3.0F, var10 + 3.0F, 1.3F, var14);
         ColorPipeline.a(var12, false);
      }

      float var26 = 223.5F;
      float var27 = var2 + (280.0F - var26) / 2.0F;
      float var15 = var3 + 24.0F;

      for (int var16 = 0; var16 < 8; var16++) {
         float var17 = var27 + var16 * 28.5F;
         float var18 = var15;
         boolean var19 = this.selectedSlotType == 0 && this.selectedSlotIndex == var16;
         boolean var20 = var4 >= var17 && var4 <= var17 + 24.0F && var5 >= var18 && var5 <= var18 + 24.0F;
         this.a(var1, var17, var18, AutoSbor.TOP_SLOTS[var16], var20, var19, var6);
      }

      float var28 = 252.0F;
      float var29 = var2 + (280.0F - var28) / 2.0F;
      float var30 = var3 + 60.0F;

      for (int var31 = 0; var31 < 3; var31++) {
         for (int var35 = 0; var35 < 9; var35++) {
            int var21 = var31 * 9 + var35;
            float var22 = var29 + var35 * 28.5F;
            float var23 = var30 + var31 * 28.5F;
            boolean var24 = this.selectedSlotType == 1 && this.selectedSlotIndex == var21;
            boolean var25 = var4 >= var22 && var4 <= var22 + 24.0F && var5 >= var23 && var5 <= var23 + 24.0F;
            this.a(var1, var22, var23, AutoSbor.INVENTORY_SLOTS[var21], var25, var24, var6);
         }
      }

      Z();

      for (int var32 = 0; var32 < 8; var32++) {
         float var36 = var27 + var32 * 28.5F;
         float var39 = var15;
         boolean var42 = var4 >= var36 && var4 <= var36 + 24.0F && var5 >= var39 && var5 <= var39 + 24.0F;
         this.a(var1, var36, var39, AutoSbor.TOP_SLOTS[var32], var42, var6, 0, var32);
      }

      for (int var33 = 0; var33 < 3; var33++) {
         for (int var37 = 0; var37 < 9; var37++) {
            int var40 = var33 * 9 + var37;
            float var43 = var29 + var37 * 28.5F;
            float var45 = var30 + var33 * 28.5F;
            boolean var46 = var4 >= var43 && var4 <= var43 + 24.0F && var5 >= var45 && var5 <= var45 + 24.0F;
            this.a(var1, var43, var45, AutoSbor.INVENTORY_SLOTS[var40], var46, var6, 1, var40);
         }
      }

      String var34 = "Правая кнопка мыши - настройки предмета";
      String var38 = "Средняя кнопка мыши - удалить предмет";
      float var41 = Msdf.a(MsdfManager.MONTSERRAT, var34, 4.8F);
      float var44 = Msdf.a(MsdfManager.MONTSERRAT, var38, 4.8F);
      RenderUtil.a(
         var1, MsdfManager.MONTSERRAT, var2 + (280.0F - var41) / 2.0F, var3 + 195.0F - 24.0F, var34, 4.8F, new Color(155, 160, 170, (int)(180.0F * var6))
      );
      RenderUtil.a(
         var1, MsdfManager.MONTSERRAT, var2 + (280.0F - var44) / 2.0F, var3 + 195.0F - 14.0F, var38, 4.8F, new Color(155, 160, 170, (int)(180.0F * var6))
      );
   }

   private void a(DrawContext var1, float var2, float var3, ItemStack var4, boolean var5, boolean var6, float var7) {
      Color var8 = Theme.getInstance().getAccent();
      Color var9 = new Color(var8.getRed(), var8.getGreen(), var8.getBlue(), (int)(255.0F * var7));
      Color var10;
      if (var6) {
         var10 = new Color(var8.getRed(), var8.getGreen(), var8.getBlue(), (int)(45.0F * var7));
      } else if (var5) {
         var10 = new Color(255, 255, 255, (int)(25.0F * var7));
      } else {
         var10 = new Color(255, 255, 255, (int)(10.0F * var7));
      }

      Color var11;
      if (var6) {
         var11 = var9;
      } else if (var5) {
         var11 = var9;
      } else {
         var11 = new Color(255, 255, 255, (int)(18.0F * var7));
      }

      RenderUtil.a(var2, var3, 24.0F, 24.0F, 4.0F, new Color[]{var10});
      RenderUtil.a(var2, var3, 24.0F, 24.0F, 4.0F, 0.5F, new Color[]{var11});
      if (var4 != null && !var4.isEmpty()) {
         this.a(var1, var4, var2, var3, 24.0F, 13.0F);
      }
   }

   private void a(DrawContext var1, float var2, float var3, ItemStack var4, boolean var5, float var6, int var7, int var8) {
      if (var4 != null && !var4.isEmpty()) {
         Color var9 = Theme.getInstance().getAccent();
         Color var10 = new Color(var9.getRed(), var9.getGreen(), var9.getBlue(), (int)(255.0F * var6));
         if (AutoSbor.e(var4)) {
            int var11 = AutoSbor.a(var7, var8);
            if (var11 > 0) {
               String var12 = var11 + "%";
               float var13 = Msdf.a(MsdfManager.BOLD, var12, 5.0F);
               RenderUtil.a(var1, MsdfManager.BOLD, var2 + 24.0F - var13 - 2.5F, var3 + 24.0F - 9.5F, var12, 5.0F, var10);
            }
         } else if (var4.getCount() > 1) {
            String var14 = String.valueOf(var4.getCount());
            float var15 = Msdf.a(MsdfManager.BOLD, var14, 5.5F);
            RenderUtil.a(var1, MsdfManager.BOLD, var2 + 24.0F - var15 - 2.5F, var3 + 24.0F - 9.5F, var14, 5.5F, new Color(255, 255, 255, (int)(245.0F * var6)));
         }

         if (var5) {
            this.hoveredTooltip = d(var4);
         }
      }
   }

   private void b(DrawContext var1, float var2, float var3, float var4, float var5, float var6) {
      Color var7 = Theme.getInstance().getAccent();
      Color var8 = new Color(var7.getRed(), var7.getGreen(), var7.getBlue(), (int)(255.0F * var6));
      HudConfig.a("autosbor_modal", var2, var3, 120.0F, 195.0F, 8.0F, new Color(15, 17, 22, (int)(245.0F * var6)));
      RenderUtil.a(var2, var3, 120.0F, 195.0F, 8.0F, 0.5F, new Color[]{new Color(255, 255, 255, (int)(35.0F * var6))});
      RenderUtil.a(var1, MsdfManager.BOLD, var2 + 8.0F, var3 + 7.5F, "Поиск", 7.5F, new Color(245, 245, 250, (int)(255.0F * var6)));
      float var9 = var2 + 8.0F;
      float var10 = var3 + 22.0F;
      float var11 = 104.0F;
      float var12 = 15.0F;
      Color var13 = this.searchFocused ? new Color(255, 255, 255, (int)(22.0F * var6)) : new Color(255, 255, 255, (int)(12.0F * var6));
      Color var14 = this.searchFocused ? var8 : new Color(255, 255, 255, (int)(24.0F * var6));
      RenderUtil.a(var9, var10, var11, var12, 3.5F, new Color[]{var13});
      RenderUtil.a(var9, var10, var11, var12, 3.5F, 0.5F, new Color[]{var14});
      String var15 = this.searchBuffer.toString();
      float var16 = var9 + 5.0F;
      float var17 = var10 + 4.5F;
      if (var15.isEmpty() && !this.searchFocused) {
         RenderUtil.a(var1, MsdfManager.BOLD, var16, var17, "Поиск...", 5.0F, new Color(130, 135, 145, (int)(180.0F * var6)));
      } else {
         float var18 = Msdf.a(MsdfManager.BOLD, var15, 5.0F);
         if (this.searchSelectAll && !var15.isEmpty()) {
            RenderUtil.a(
               var16 - 1.0F,
               var17 - 1.0F,
               var18 + 2.0F,
               8.0F,
               1.5F,
               new Color[]{new Color(var7.getRed(), var7.getGreen(), var7.getBlue(), (int)(120.0F * var6))}
            );
         }

         RenderUtil.a(var1, MsdfManager.BOLD, var16, var17, var15, 5.0F, new Color(245, 245, 250, (int)(255.0F * var6)));
         if (this.searchFocused && System.currentTimeMillis() / 500L % 2L == 0L && !this.searchSelectAll) {
            float var19 = var16 + var18 + 1.0F;
            RenderUtil.a(var19, var17, 0.8F, 6.5F, 0.4F, new Color[]{var8});
         }
      }

      List var40 = this.b(var15);
      float var41 = var10 + var12 + 7.0F;
      float var20 = 195.0F - (var41 - var3) - 8.0F;
      float var21 = var2 + 8.0F;
      int var22 = (int)Math.ceil(var40.size() / 4.0);
      float var23 = var22 * 20.0F + Math.max(0, var22 - 1) * 3.0F;
      this.searchMaxScroll = Math.max(0.0F, var23 - var20);
      this.searchScrollAnim.animate(this.searchScrollTarget, 0.05F, 12.0F);
      float var24 = this.searchScrollAnim.getValue();
      MinecraftClient var25 = MinecraftClient.getInstance();
      float var26 = var25.getWindow().getScaleFactor();
      float var27 = 2.0F / var26;
      int var28 = (int)(var2 * var27);
      int var29 = (int)(var41 * var27);
      int var30 = (int)((var2 + 120.0F) * var27);
      int var31 = (int)((var41 + var20) * var27);
      var1.enableScissor(var28, var29, var30, var31);
      Scissor.b(var2, var41, 120.0, var20);

      for (int var32 = 0; var32 < var40.size(); var32++) {
         int var33 = var32 / 4;
         int var34 = var32 % 4;
         float var35 = var21 + var34 * 23.0F;
         float var36 = var41 + var33 * 23.0F + var24;
         if (!(var36 + 20.0F < var41 - 4.0F) && !(var36 > var41 + var20 + 4.0F)) {
            boolean var37 = var4 >= var35 && var4 <= var35 + 20.0F && var5 >= var36 && var5 <= var36 + 20.0F && var5 >= var41 && var5 <= var41 + var20;
            Color var38 = var37 ? new Color(255, 255, 255, (int)(26.0F * var6)) : new Color(255, 255, 255, (int)(10.0F * var6));
            Color var39 = var37 ? var8 : new Color(255, 255, 255, (int)(24.0F * var6));
            RenderUtil.a(var35, var36, 20.0F, 20.0F, 3.5F, new Color[]{var38});
            RenderUtil.a(var35, var36, 20.0F, 20.0F, 3.5F, 0.5F, new Color[]{var39});
            this.a(var1, ((AutoSborScreen.a)var40.get(var32)).getStack(), var35, var36, 20.0F, 12.0F);
            if (var37) {
               this.hoveredTooltip = d(((AutoSborScreen.a)var40.get(var32)).getStack());
            }
         }
      }

      Z();
      Scissor.c();
      var1.disableScissor();
      if (this.searchMaxScroll > 0.5F) {
         float var42 = 3.5F;
         float var43 = var2 + 120.0F - 6.5F;
         float var44 = Math.max(14.0F, var20 * (var20 / (var20 + this.searchMaxScroll)));
         float var45 = var41 + -var24 / this.searchMaxScroll * (var20 - var44);
         RenderUtil.a(var43, var41, var42, var20, 1.75F, new Color[]{new Color(255, 255, 255, (int)(16.0F * var6))});
         RenderUtil.a(var43, var45, var42, var44, 1.75F, new Color[]{var8});
      }
   }

   private void c(DrawContext var1, float var2, float var3, float var4, float var5, float var6) {
      Color var7 = Theme.getInstance().getAccent();
      Color var8 = new Color(var7.getRed(), var7.getGreen(), var7.getBlue(), (int)(255.0F * var6));
      HudConfig.a("autosbor_modal", var2, var3, 135.0F, 195.0F, 8.0F, new Color(15, 17, 22, (int)(245.0F * var6)));
      RenderUtil.a(var2, var3, 135.0F, 195.0F, 8.0F, 0.5F, new Color[]{new Color(255, 255, 255, (int)(35.0F * var6))});
      RenderUtil.a(var1, MsdfManager.BOLD, var2 + 8.0F, var3 + 7.5F, "Настройки", 7.5F, new Color(245, 245, 250, (int)(255.0F * var6)));
      float var9 = var2 + 135.0F - 12.0F;
      float var10 = var3 + 11.5F;
      this.settingsCloseBtnBox[0] = var9 - 6.0F;
      this.settingsCloseBtnBox[1] = var10 - 6.0F;
      this.settingsCloseBtnBox[2] = 12.0F;
      this.settingsCloseBtnBox[3] = 12.0F;
      boolean var11 = var4 >= this.settingsCloseBtnBox[0]
         && var4 <= this.settingsCloseBtnBox[0] + this.settingsCloseBtnBox[2]
         && var5 >= this.settingsCloseBtnBox[1]
         && var5 <= this.settingsCloseBtnBox[1] + this.settingsCloseBtnBox[3];
      BufferBuilder var12 = ColorPipeline.a();
      if (var12 != null) {
         Color var13 = var11 ? var8 : new Color(170, 175, 185);
         int var14 = (int)(255.0F * var6) << 24 | var13.getRGB() & 16777215;
         ColorPipeline.a(var12, var9 - 3.0F, var10 - 3.0F, var9 + 3.0F, var10 + 3.0F, 1.3F, var14);
         ColorPipeline.a(var12, var9 + 3.0F, var10 - 3.0F, var9 - 3.0F, var10 + 3.0F, 1.3F, var14);
         ColorPipeline.a(var12, false);
      }

      ItemStack var90 = this.aa();
      int var91 = this.selectedSlotType;
      int var15 = this.selectedSlotIndex;
      if (var90 != null && !var90.isEmpty()) {
         this.lastSelectedStack = var90;
         this.lastSelectedSlotType = this.selectedSlotType;
         this.lastSelectedSlotIndex = this.selectedSlotIndex;
      } else {
         var90 = this.lastSelectedStack;
         var91 = this.lastSelectedSlotType;
         var15 = this.lastSelectedSlotIndex;
      }

      if (var90 != null && !var90.isEmpty() && var91 != -1 && var15 != -1) {
         boolean var16 = AutoSbor.n(var90);
         boolean var17 = AutoSbor.m(var90);
         boolean var18 = AutoSbor.V(var90);
         boolean var19 = AutoSbor.W(var90);
         boolean var20 = AutoSbor.X(var90);
         boolean var21 = AutoSbor.Y(var90);
         boolean var22 = var18 || var19 || var20 || var21;
         float var23 = var22 ? 20.0F : (!var16 && !var17 ? 34.0F : 26.0F);
         float var24 = var2 + (135.0F - var23) / 2.0F;
         float var25 = var3 + (var22 ? 16.0F : (!var16 && !var17 ? 28.0F : 22.0F));
         RenderUtil.a(var24, var25, var23, var23, 5.0F, new Color[]{new Color(255, 255, 255, (int)(14.0F * var6))});
         RenderUtil.a(var24, var25, var23, var23, 5.0F, 0.5F, new Color[]{new Color(255, 255, 255, (int)(24.0F * var6))});
         this.a(var1, var90, var24, var25, var23, var22 ? 12.0F : (!var16 && !var17 ? 18.0F : 14.0F));
         Z();
         boolean var26 = var4 >= var24 && var4 <= var24 + var23 && var5 >= var25 && var5 <= var25 + var23;
         if (var26) {
            this.hoveredTooltip = d(var90);
         }

         String var27 = d(var90);
         float var28 = Msdf.a(MsdfManager.BOLD, var27, 5.5F);
         float var29 = var2 + (135.0F - var28) / 2.0F;
         float var30 = var25 + var23 + (var22 ? 3.0F : (!var16 && !var17 ? 9.0F : 5.0F));
         RenderUtil.a(var1, MsdfManager.BOLD, var29, var30, var27, 5.5F, new Color(235, 240, 250, (int)(240.0F * var6)));
         boolean var31 = AutoSbor.e(var90);
         if (var31) {
            int var32 = AutoSbor.a(var91, var15);
            float var33 = var30 + (var22 ? 9.0F : (!var16 && !var17 ? 16.0F : 12.0F));
            String var34 = "Прочность:";
            RenderUtil.a(var1, MsdfManager.BOLD, var2 + 8.0F, var33 + 2.5F, var34, 5.5F, new Color(245, 245, 250, (int)(245.0F * var6)));
            float var35 = Msdf.a(MsdfManager.BOLD, var34, 5.5F);
            float var36 = var2 + 8.0F + var35 + 4.0F;
            float var37 = 30.0F;
            float var38 = var22 ? 11.0F : 13.0F;
            float var39 = var33;
            this.durInputBox[0] = var36;
            this.durInputBox[1] = var39;
            this.durInputBox[2] = var37;
            this.durInputBox[3] = var38;
            Color var40 = this.durFocused ? new Color(255, 255, 255, (int)(26.0F * var6)) : new Color(255, 255, 255, (int)(12.0F * var6));
            Color var41 = this.durFocused ? var8 : new Color(255, 255, 255, (int)(24.0F * var6));
            RenderUtil.a(var36, var39, var37, var38, 3.0F, new Color[]{var40});
            RenderUtil.a(var36, var39, var37, var38, 3.0F, 0.5F, new Color[]{var41});
            String var42 = this.durFocused ? this.durBuffer.toString() : String.valueOf(var32);
            float var43 = Msdf.a(MsdfManager.BOLD, var42, 5.0F);
            float var44 = var36 + (var37 - var43) / 2.0F;
            float var45 = var39 + (var38 - Msdf.a(MsdfManager.BOLD, 5.0F)) / 2.0F - 0.5F;
            if (this.durFocused && this.durSelectAll && !var42.isEmpty()) {
               RenderUtil.a(
                  var44 - 1.0F,
                  var45 - 1.0F,
                  var43 + 2.0F,
                  8.0F,
                  1.5F,
                  new Color[]{new Color(var7.getRed(), var7.getGreen(), var7.getBlue(), (int)(120.0F * var6))}
               );
            }

            RenderUtil.a(var1, MsdfManager.BOLD, var44, var45, var42, 5.0F, new Color(245, 245, 250, (int)(255.0F * var6)));
            if (this.durFocused && System.currentTimeMillis() / 500L % 2L == 0L && !this.durSelectAll) {
               float var46 = var44 + var43 + 1.0F;
               RenderUtil.a(var46, var45, 0.8F, 6.5F, 0.4F, new Color[]{var8});
            }

            float var106 = var36 + var37 + 4.0F;
            RenderUtil.a(var1, MsdfManager.BOLD, var106, var33 + 2.5F, "%", 5.5F, new Color(170, 175, 185, (int)(220.0F * var6)));
            float var47 = var2 + 9.0F;
            float var48 = var33 + (var22 ? 13.0F : (!var16 && !var17 ? 17.0F : 15.0F));
            float var49 = 117.0F;
            float var50 = 3.0F;
            RenderUtil.a(var47, var48, var49, var50, 1.5F, new Color[]{new Color(255, 255, 255, (int)(20.0F * var6))});
            float var51 = var32 / 100.0F;
            float var52 = var51 * var49;
            if (var52 > 0.5F) {
               RenderUtil.a(var47, var48, var52, var50, 1.5F, new Color[]{var8});
            }

            float var53 = var47 + var51 * var49;
            float var54 = var48 + var50 / 2.0F;
            float var55 = 3.5F;
            RenderUtil.a(var53 - var55, var54 - var55, var55 * 2.0F, var55 * 2.0F, var55, new Color[]{new Color(255, 255, 255, (int)(255.0F * var6))});
            float var56 = var48 + (var22 ? 8.0F : (!var16 && !var17 ? 15.0F : 10.0F));
            int[] var57 = new int[]{0, 50, 80, 100};
            float var58 = 3.0F;
            float var59 = (var49 - (var57.length - 1) * var58) / var57.length;
            float var60 = var22 ? 9.0F : (!var16 && !var17 ? 12.0F : 10.0F);

            for (int var61 = 0; var61 < var57.length; var61++) {
               float var62 = var47 + var61 * (var59 + var58);
               boolean var63 = var32 == var57[var61];
               boolean var64 = var4 >= var62 && var4 <= var62 + var59 && var5 >= var56 && var5 <= var56 + var60;
               Color var65 = var63 ? var8 : (var64 ? new Color(255, 255, 255, (int)(28.0F * var6)) : new Color(255, 255, 255, (int)(12.0F * var6)));
               Color var66 = var63 ? var8 : (var64 ? var8 : new Color(255, 255, 255, (int)(22.0F * var6)));
               RenderUtil.a(var62, var56, var59, var60, 2.5F, new Color[]{var65});
               RenderUtil.a(var62, var56, var59, var60, 2.5F, 0.5F, new Color[]{var66});
               String var67 = var57[var61] + "%";
               float var68 = Msdf.a(MsdfManager.BOLD, var67, 4.5F);
               RenderUtil.a(
                  var1,
                  MsdfManager.BOLD,
                  var62 + (var59 - var68) / 2.0F,
                  var56 + (var60 - Msdf.a(MsdfManager.BOLD, 4.5F)) / 2.0F - 0.5F,
                  var67,
                  4.5F,
                  new Color(245, 245, 250, (int)(255.0F * var6))
               );
            }

            if (var16) {
               int var123 = AutoSbor.b(var91, var15);
               float var127 = var56 + var60 + 8.0F;
               String var131 = "Острота: " + (var123 == 0 ? "Любая" : String.valueOf(var123));
               RenderUtil.a(var1, MsdfManager.BOLD, var2 + 8.0F, var127, var131, 5.5F, new Color(245, 245, 250, (int)(255.0F * var6)));
               float var135 = var127 + 10.0F;
               float var139 = 2.0F;
               float var143 = (var49 - 8.0F * var139) / 9.0F;
               float var147 = 10.0F;

               for (int var151 = 1; var151 <= 9; var151++) {
                  float var69 = var47 + (var151 - 1) * (var143 + var139);
                  boolean var70 = var123 == var151;
                  boolean var71 = var4 >= var69 && var4 <= var69 + var143 && var5 >= var135 && var5 <= var135 + var147;
                  Color var72 = var70 ? var8 : (var71 ? new Color(255, 255, 255, (int)(28.0F * var6)) : new Color(255, 255, 255, (int)(12.0F * var6)));
                  Color var73 = var70 ? var8 : (var71 ? var8 : new Color(255, 255, 255, (int)(22.0F * var6)));
                  RenderUtil.a(var69, var135, var143, var147, 2.5F, new Color[]{var72});
                  RenderUtil.a(var69, var135, var143, var147, 2.5F, 0.5F, new Color[]{var73});
                  String var74 = String.valueOf(var151);
                  float var75 = Msdf.a(MsdfManager.BOLD, var74, 4.5F);
                  RenderUtil.a(
                     var1,
                     MsdfManager.BOLD,
                     var69 + (var143 - var75) / 2.0F,
                     var135 + (var147 - Msdf.a(MsdfManager.BOLD, 4.5F)) / 2.0F - 0.5F,
                     var74,
                     4.5F,
                     new Color(245, 245, 250, (int)(255.0F * var6))
                  );
               }

               boolean var152 = AutoSbor.c(var91, var15);
               float var157 = var135 + var147 + 7.0F;
               float var162 = 13.0F;
               boolean var166 = var4 >= var47 && var4 <= var47 + var49 && var5 >= var157 && var5 <= var157 + var162;
               Color var170 = var152
                  ? new Color(var7.getRed(), var7.getGreen(), var7.getBlue(), (int)(60.0F * var6))
                  : (var166 ? new Color(255, 255, 255, (int)(20.0F * var6)) : new Color(255, 255, 255, (int)(10.0F * var6)));
               Color var174 = var152 ? var8 : (var166 ? var8 : new Color(255, 255, 255, (int)(22.0F * var6)));
               RenderUtil.a(var47, var157, var49, var162, 3.0F, new Color[]{var170});
               RenderUtil.a(var47, var157, var49, var162, 3.0F, 0.5F, new Color[]{var174});
               String var178 = "Критический II: " + (var152 ? "ВКЛ" : "ВЫКЛ");
               float var182 = Msdf.a(MsdfManager.BOLD, var178, 5.0F);
               RenderUtil.a(
                  var1,
                  MsdfManager.BOLD,
                  var47 + (var49 - var182) / 2.0F,
                  var157 + (var162 - Msdf.a(MsdfManager.BOLD, 5.0F)) / 2.0F - 0.5F,
                  var178,
                  5.0F,
                  var152 ? var8 : new Color(200, 205, 215, (int)(240.0F * var6))
               );
            } else if (var17) {
               int var124 = AutoSbor.d(var91, var15);
               float var128 = var56 + var60 + 10.0F;
               String var132 = "Эффективность: " + (var124 == 0 ? "Любая" : String.valueOf(var124));
               RenderUtil.a(var1, MsdfManager.BOLD, var2 + 8.0F, var128, var132, 5.5F, new Color(245, 245, 250, (int)(245.0F * var6)));
               float var136 = var128 + 11.0F;
               float var140 = 2.0F;
               float var144 = (var49 - 9.0F * var140) / 10.0F;
               float var148 = 11.0F;

               for (int var153 = 1; var153 <= 10; var153++) {
                  float var158 = var47 + (var153 - 1) * (var144 + var140);
                  boolean var163 = var124 == var153;
                  boolean var167 = var4 >= var158 && var4 <= var158 + var144 && var5 >= var136 && var5 <= var136 + var148;
                  Color var171 = var163 ? var8 : (var167 ? new Color(255, 255, 255, (int)(28.0F * var6)) : new Color(255, 255, 255, (int)(12.0F * var6)));
                  Color var175 = var163 ? var8 : (var167 ? var8 : new Color(255, 255, 255, (int)(22.0F * var6)));
                  RenderUtil.a(var158, var136, var144, var148, 2.5F, new Color[]{var171});
                  RenderUtil.a(var158, var136, var144, var148, 2.5F, 0.5F, new Color[]{var175});
                  String var179 = String.valueOf(var153);
                  float var183 = Msdf.a(MsdfManager.BOLD, var179, 4.5F);
                  RenderUtil.a(
                     var1,
                     MsdfManager.BOLD,
                     var158 + (var144 - var183) / 2.0F,
                     var136 + (var148 - Msdf.a(MsdfManager.BOLD, 4.5F)) / 2.0F - 0.5F,
                     var179,
                     4.5F,
                     new Color(245, 245, 250, (int)(255.0F * var6))
                  );
               }
            } else if (var22) {
               int var125 = AutoSbor.e(var91, var15);
               float var129 = var56 + var60 + (!var18 && !var21 ? 7.0F : 5.0F);
               String var133 = "Защита: " + (var125 == 0 ? "Любая" : String.valueOf(var125));
               RenderUtil.a(var1, MsdfManager.BOLD, var2 + 8.0F, var129, var133, 5.0F, new Color(245, 245, 250, (int)(255.0F * var6)));
               float var137 = var129 + 8.0F;
               float var141 = 2.0F;
               float var145 = (var49 - 4.0F * var141) / 5.0F;
               float var149 = 9.0F;

               for (int var154 = 1; var154 <= 5; var154++) {
                  float var159 = var47 + (var154 - 1) * (var145 + var141);
                  boolean var164 = var125 == var154;
                  boolean var168 = var4 >= var159 && var4 <= var159 + var145 && var5 >= var137 && var5 <= var137 + var149;
                  Color var172 = var164 ? var8 : (var168 ? new Color(255, 255, 255, (int)(28.0F * var6)) : new Color(255, 255, 255, (int)(12.0F * var6)));
                  Color var176 = var164 ? var8 : (var168 ? var8 : new Color(255, 255, 255, (int)(22.0F * var6)));
                  RenderUtil.a(var159, var137, var145, var149, 2.5F, new Color[]{var172});
                  RenderUtil.a(var159, var137, var145, var149, 2.5F, 0.5F, new Color[]{var176});
                  String var180 = String.valueOf(var154);
                  float var184 = Msdf.a(MsdfManager.BOLD, var180, 4.5F);
                  RenderUtil.a(
                     var1,
                     MsdfManager.BOLD,
                     var159 + (var145 - var184) / 2.0F,
                     var137 + (var149 - Msdf.a(MsdfManager.BOLD, 4.5F)) / 2.0F - 0.5F,
                     var180,
                     4.5F,
                     new Color(245, 245, 250, (int)(255.0F * var6))
                  );
               }

               int var155 = AutoSbor.f(var91, var15);
               float var160 = var137 + var149 + (!var18 && !var21 ? 6.0F : 4.0F);
               String var165 = "Прочность: " + (var155 == 0 ? "Любая" : String.valueOf(var155));
               RenderUtil.a(var1, MsdfManager.BOLD, var2 + 8.0F, var160, var165, 5.0F, new Color(245, 245, 250, (int)(245.0F * var6)));
               float var169 = var160 + 8.0F;
               float var173 = 2.0F;
               float var177 = (var49 - 4.0F * var173) / 5.0F;
               float var181 = 9.0F;

               for (int var185 = 1; var185 <= 5; var185++) {
                  float var76 = var47 + (var185 - 1) * (var177 + var173);
                  boolean var77 = var155 == var185;
                  boolean var78 = var4 >= var76 && var4 <= var76 + var177 && var5 >= var169 && var5 <= var169 + var181;
                  Color var79 = var77 ? var8 : (var78 ? new Color(255, 255, 255, (int)(28.0F * var6)) : new Color(255, 255, 255, (int)(12.0F * var6)));
                  Color var80 = var77 ? var8 : (var78 ? var8 : new Color(255, 255, 255, (int)(22.0F * var6)));
                  RenderUtil.a(var76, var169, var177, var181, 2.5F, new Color[]{var79});
                  RenderUtil.a(var76, var169, var177, var181, 2.5F, 0.5F, new Color[]{var80});
                  String var81 = String.valueOf(var185);
                  float var82 = Msdf.a(MsdfManager.BOLD, var81, 4.5F);
                  RenderUtil.a(
                     var1,
                     MsdfManager.BOLD,
                     var76 + (var177 - var82) / 2.0F,
                     var169 + (var181 - Msdf.a(MsdfManager.BOLD, 4.5F)) / 2.0F - 0.5F,
                     var81,
                     4.5F,
                     new Color(245, 245, 250, (int)(255.0F * var6))
                  );
               }

               if (var18) {
                  int var186 = AutoSbor.g(var91, var15);
                  float var188 = var169 + var181 + 4.0F;
                  String var190 = "Подводное Дыхание: " + (var186 == 0 ? "Любое" : String.valueOf(var186));
                  RenderUtil.a(var1, MsdfManager.BOLD, var2 + 8.0F, var188, var190, 5.0F, new Color(245, 245, 250, (int)(245.0F * var6)));
                  float var192 = var188 + 8.0F;
                  float var194 = 2.0F;
                  float var196 = (var49 - 2.0F * var194) / 3.0F;
                  float var198 = 9.0F;

                  for (int var200 = 1; var200 <= 3; var200++) {
                     float var83 = var47 + (var200 - 1) * (var196 + var194);
                     boolean var84 = var186 == var200;
                     boolean var85 = var4 >= var83 && var4 <= var83 + var196 && var5 >= var192 && var5 <= var192 + var198;
                     Color var86 = var84 ? var8 : (var85 ? new Color(255, 255, 255, (int)(28.0F * var6)) : new Color(255, 255, 255, (int)(12.0F * var6)));
                     Color var87 = var84 ? var8 : (var85 ? var8 : new Color(255, 255, 255, (int)(22.0F * var6)));
                     RenderUtil.a(var83, var192, var196, var198, 2.5F, new Color[]{var86});
                     RenderUtil.a(var83, var192, var196, var198, 2.5F, 0.5F, new Color[]{var87});
                     String var88 = String.valueOf(var200);
                     float var89 = Msdf.a(MsdfManager.BOLD, var88, 4.5F);
                     RenderUtil.a(
                        var1,
                        MsdfManager.BOLD,
                        var83 + (var196 - var89) / 2.0F,
                        var192 + (var198 - Msdf.a(MsdfManager.BOLD, 4.5F)) / 2.0F - 0.5F,
                        var88,
                        4.5F,
                        new Color(245, 245, 250, (int)(255.0F * var6))
                     );
                  }
               } else if (var21) {
                  int var187 = AutoSbor.h(var91, var15);
                  float var189 = var169 + var181 + 4.0F;
                  String var191 = "Подводная ходьба: " + (var187 == 0 ? "Любой" : String.valueOf(var187));
                  RenderUtil.a(var1, MsdfManager.BOLD, var2 + 8.0F, var189, var191, 5.0F, new Color(245, 245, 250, (int)(245.0F * var6)));
                  float var193 = var189 + 8.0F;
                  float var195 = 2.0F;
                  float var197 = (var49 - 2.0F * var195) / 3.0F;
                  float var199 = 9.0F;

                  for (int var201 = 1; var201 <= 3; var201++) {
                     float var202 = var47 + (var201 - 1) * (var197 + var195);
                     boolean var203 = var187 == var201;
                     boolean var204 = var4 >= var202 && var4 <= var202 + var197 && var5 >= var193 && var5 <= var193 + var199;
                     Color var205 = var203 ? var8 : (var204 ? new Color(255, 255, 255, (int)(28.0F * var6)) : new Color(255, 255, 255, (int)(12.0F * var6)));
                     Color var206 = var203 ? var8 : (var204 ? var8 : new Color(255, 255, 255, (int)(22.0F * var6)));
                     RenderUtil.a(var202, var193, var197, var199, 2.5F, new Color[]{var205});
                     RenderUtil.a(var202, var193, var197, var199, 2.5F, 0.5F, new Color[]{var206});
                     String var207 = String.valueOf(var201);
                     float var208 = Msdf.a(MsdfManager.BOLD, var207, 4.5F);
                     RenderUtil.a(
                        var1,
                        MsdfManager.BOLD,
                        var202 + (var197 - var208) / 2.0F,
                        var193 + (var199 - Msdf.a(MsdfManager.BOLD, 4.5F)) / 2.0F - 0.5F,
                        var207,
                        4.5F,
                        new Color(245, 245, 250, (int)(255.0F * var6))
                     );
                  }
               }
            }
         } else {
            int var92 = a(var90);
            int var93 = Math.clamp(var90.getCount(), 1, var92);
            float var94 = var30 + 16.0F;
            String var95 = "Кол-во:";
            RenderUtil.a(var1, MsdfManager.BOLD, var2 + 8.0F, var94 + 2.5F, var95, 6.0F, new Color(245, 245, 250, (int)(245.0F * var6)));
            float var96 = Msdf.a(MsdfManager.BOLD, var95, 6.0F);
            float var97 = var2 + 8.0F + var96 + 4.0F;
            float var98 = 38.0F;
            float var99 = 13.0F;
            float var100 = var94;
            this.countInputBox[0] = var97;
            this.countInputBox[1] = var100;
            this.countInputBox[2] = var98;
            this.countInputBox[3] = var99;
            Color var101 = this.countFocused ? new Color(255, 255, 255, (int)(26.0F * var6)) : new Color(255, 255, 255, (int)(12.0F * var6));
            Color var102 = this.countFocused ? var8 : new Color(255, 255, 255, (int)(24.0F * var6));
            RenderUtil.a(var97, var100, var98, var99, 3.0F, new Color[]{var101});
            RenderUtil.a(var97, var100, var98, var99, 3.0F, 0.5F, new Color[]{var102});
            String var103 = this.countFocused ? this.countBuffer.toString() : String.valueOf(var93);
            float var104 = Msdf.a(MsdfManager.BOLD, var103, 5.5F);
            float var105 = var97 + (var98 - var104) / 2.0F;
            float var107 = var100 + (var99 - Msdf.a(MsdfManager.BOLD, 5.5F)) / 2.0F - 0.5F;
            if (this.countFocused && this.countSelectAll && !var103.isEmpty()) {
               RenderUtil.a(
                  var105 - 1.0F,
                  var107 - 1.0F,
                  var104 + 2.0F,
                  8.0F,
                  1.5F,
                  new Color[]{new Color(var7.getRed(), var7.getGreen(), var7.getBlue(), (int)(120.0F * var6))}
               );
            }

            RenderUtil.a(var1, MsdfManager.BOLD, var105, var107, var103, 5.5F, new Color(245, 245, 250, (int)(255.0F * var6)));
            if (this.countFocused && System.currentTimeMillis() / 500L % 2L == 0L && !this.countSelectAll) {
               float var108 = var105 + var104 + 1.0F;
               RenderUtil.a(var108, var107, 0.8F, 6.5F, 0.4F, new Color[]{var8});
            }

            float var109 = var97 + var98 + 4.0F;
            RenderUtil.a(var1, MsdfManager.BOLD, var109, var94 + 2.5F, "шт.", 6.0F, new Color(170, 175, 185, (int)(220.0F * var6)));
            float var110 = var2 + 9.0F;
            float var111 = var94 + 17.0F;
            float var112 = 117.0F;
            float var113 = 3.0F;
            RenderUtil.a(var110, var111, var112, var113, 1.5F, new Color[]{new Color(255, 255, 255, (int)(20.0F * var6))});
            float var114 = var92 > 1 ? (float)(var93 - 1) / (var92 - 1) : 0.0F;
            float var115 = var114 * var112;
            if (var115 > 0.5F) {
               RenderUtil.a(var110, var111, var115, var113, 1.5F, new Color[]{var8});
            }

            float var116 = var110 + var114 * var112;
            float var117 = var111 + var113 / 2.0F;
            float var118 = 4.0F;
            RenderUtil.a(var116 - var118, var117 - var118, var118 * 2.0F, var118 * 2.0F, var118, new Color[]{new Color(255, 255, 255, (int)(255.0F * var6))});
            float var119 = var111 + 15.0F;
            int[] var120 = a(var92);
            float var121 = 3.0F;
            float var122 = (var112 - (var120.length - 1) * var121) / var120.length;
            float var126 = 12.0F;

            for (int var130 = 0; var130 < var120.length; var130++) {
               float var134 = var110 + var130 * (var122 + var121);
               boolean var138 = var93 == var120[var130];
               boolean var142 = var4 >= var134 && var4 <= var134 + var122 && var5 >= var119 && var5 <= var119 + var126;
               Color var146 = var138 ? var8 : (var142 ? new Color(255, 255, 255, (int)(28.0F * var6)) : new Color(255, 255, 255, (int)(12.0F * var6)));
               Color var150 = var138 ? var8 : (var142 ? var8 : new Color(255, 255, 255, (int)(22.0F * var6)));
               RenderUtil.a(var134, var119, var122, var126, 2.5F, new Color[]{var146});
               RenderUtil.a(var134, var119, var122, var126, 2.5F, 0.5F, new Color[]{var150});
               String var156 = String.valueOf(var120[var130]);
               float var161 = Msdf.a(MsdfManager.BOLD, var156, 4.75F);
               RenderUtil.a(
                  var1,
                  MsdfManager.BOLD,
                  var134 + (var122 - var161) / 2.0F,
                  var119 + (var126 - Msdf.a(MsdfManager.BOLD, 4.75F)) / 2.0F - 0.5F,
                  var156,
                  4.75F,
                  new Color(245, 245, 250, (int)(255.0F * var6))
               );
            }
         }
      }
   }

   private void d(DrawContext var1, float var2, float var3, float var4, float var5, float var6) {
      Color var7 = Theme.getInstance().getAccent();
      Color var8 = new Color(var7.getRed(), var7.getGreen(), var7.getBlue(), (int)(255.0F * var6));
      HudConfig.a("autosbor_configs", var2, var3, 280.0F, 34.0F, 6.0F, new Color(15, 17, 22, (int)(245.0F * var6)));
      RenderUtil.a(var2, var3, 280.0F, 34.0F, 6.0F, 0.5F, new Color[]{new Color(255, 255, 255, (int)(35.0F * var6))});
      float var9 = var2 + 7.0F;
      float var10 = var3 + 7.0F;
      float var11 = 76.0F;
      float var12 = 20.0F;
      this.configInputBox[0] = var9;
      this.configInputBox[1] = var10;
      this.configInputBox[2] = var11;
      this.configInputBox[3] = var12;
      Color var13 = this.configInputFocused ? new Color(255, 255, 255, (int)(22.0F * var6)) : new Color(255, 255, 255, (int)(12.0F * var6));
      Color var14 = this.configInputFocused ? var8 : new Color(255, 255, 255, (int)(24.0F * var6));
      RenderUtil.a(var9, var10, var11, var12, 3.5F, new Color[]{var13});
      RenderUtil.a(var9, var10, var11, var12, 3.5F, 0.5F, new Color[]{var14});
      String var15 = this.configInputBuffer.toString();
      float var16 = var9 + 5.0F;
      float var17 = var10 + 6.5F;
      if (var15.isEmpty() && !this.configInputFocused) {
         RenderUtil.a(var1, MsdfManager.BOLD, var16, var17, "Имя конфига...", 4.8F, new Color(130, 135, 145, (int)(180.0F * var6)));
      } else {
         float var18 = Msdf.a(MsdfManager.BOLD, var15, 4.8F);
         if (this.configInputSelectAll && !var15.isEmpty()) {
            RenderUtil.a(
               var16 - 1.0F,
               var17 - 1.0F,
               var18 + 2.0F,
               8.0F,
               1.5F,
               new Color[]{new Color(var7.getRed(), var7.getGreen(), var7.getBlue(), (int)(120.0F * var6))}
            );
         }

         RenderUtil.a(var1, MsdfManager.BOLD, var16, var17, var15, 4.8F, new Color(245, 245, 250, (int)(255.0F * var6)));
         if (this.configInputFocused && System.currentTimeMillis() / 500L % 2L == 0L && !this.configInputSelectAll) {
            float var19 = var16 + var18 + 1.0F;
            RenderUtil.a(var19, var17, 0.8F, 6.5F, 0.4F, new Color[]{var8});
         }
      }

      float var61 = var2 + 86.0F;
      float var62 = var3 + 7.0F;
      float var20 = 44.0F;
      float var21 = 20.0F;
      this.configSaveBtnBox[0] = var61;
      this.configSaveBtnBox[1] = var62;
      this.configSaveBtnBox[2] = var20;
      this.configSaveBtnBox[3] = var21;
      boolean var22 = var4 >= var61 && var4 <= var61 + var20 && var5 >= var62 && var5 <= var62 + var21;
      Color var23 = var22 ? var8 : new Color(var7.getRed(), var7.getGreen(), var7.getBlue(), (int)(180.0F * var6));
      RenderUtil.a(var61, var62, var20, var21, 3.5F, new Color[]{var23});
      RenderUtil.a(var61, var62, var20, var21, 3.5F, 0.5F, new Color[]{var8});
      String var24 = "Сохранить";
      float var25 = Msdf.a(MsdfManager.BOLD, var24, 4.6F);
      RenderUtil.a(var1, MsdfManager.BOLD, var61 + (var20 - var25) / 2.0F, var62 + 6.5F, var24, 4.6F, new Color(255, 255, 255, (int)(255.0F * var6)));
      float var26 = var2 + 135.0F;
      RenderUtil.a(var26, var3 + 6.0F, 0.5F, 22.0F, 0.25F, new Color[]{new Color(255, 255, 255, (int)(25.0F * var6))});
      float var27 = var2 + 140.0F;
      float var28 = var3 + 5.0F;
      float var29 = 135.0F;
      float var30 = 24.0F;
      List var31 = AutoSbor.r();
      float var32 = 0.0F;
      float var33 = 18.0F;
      float var34 = 4.0F;
      ArrayList var35 = new ArrayList();

      for (String var37 : (Iterable<String>)(Iterable<?>)(var31)) {
         float var38 = Msdf.a(MsdfManager.BOLD, var37, 4.8F) + 20.0F;
         var35.add(var38);
         var32 += var38 + var34;
      }

      if (var32 > 0.0F) {
         var32 -= var34;
      }

      this.configMaxScroll = Math.max(0.0F, var32 - var29);
      this.configScrollAnim.animate(this.configScrollTarget, 0.05F, 12.0F);
      float var63 = this.configScrollAnim.getValue();
      MinecraftClient var64 = MinecraftClient.getInstance();
      float var65 = var64.getWindow().getScaleFactor();
      float var39 = 2.0F / var65;
      int var40 = (int)(var27 * var39);
      int var41 = (int)(var28 * var39);
      int var42 = (int)((var27 + var29) * var39);
      int var43 = (int)((var28 + var30) * var39);
      var1.enableScissor(var40, var41, var42, var43);
      Scissor.b(var27, var28, var29, var30);
      if (var31.isEmpty()) {
         RenderUtil.a(
            var1, MsdfManager.MONTSERRAT, var27 + 8.0F, var28 + 7.5F, "Нет сохраненных конфигов", 4.6F, new Color(130, 135, 145, (int)(180.0F * var6))
         );
      } else {
         float var44 = var27 + var63;
         float var45 = var28 + (var30 - var33) / 2.0F;

         for (int var46 = 0; var46 < var31.size(); var46++) {
            String var47 = (String)var31.get(var46);
            float var48 = (Float)var35.get(var46);
            if (var44 + var48 >= var27 && var44 <= var27 + var29) {
               boolean var49 = var47.equals(this.selectedConfigName);
               boolean var50 = var4 >= var44 && var4 <= var44 + var48 && var5 >= var45 && var5 <= var45 + var33 && var4 >= var27 && var4 <= var27 + var29;
               float var51 = 12.0F;
               float var52 = var44 + var48 - var51 - 2.0F;
               boolean var53 = var4 >= var52 && var4 <= var52 + var51 && var5 >= var45 && var5 <= var45 + var33 && var4 >= var27 && var4 <= var27 + var29;
               Color var54 = var49
                  ? new Color(var7.getRed(), var7.getGreen(), var7.getBlue(), (int)(55.0F * var6))
                  : (var50 ? new Color(255, 255, 255, (int)(22.0F * var6)) : new Color(255, 255, 255, (int)(12.0F * var6)));
               Color var55 = var49 ? var8 : (var50 ? var8 : new Color(255, 255, 255, (int)(25.0F * var6)));
               RenderUtil.a(var44, var45, var48, var33, 3.0F, new Color[]{var54});
               RenderUtil.a(var44, var45, var48, var33, 3.0F, 0.5F, new Color[]{var55});
               RenderUtil.a(var1, MsdfManager.BOLD, var44 + 5.0F, var45 + 5.5F, var47, 4.8F, var49 ? var8 : new Color(245, 245, 250, (int)(245.0F * var6)));
               BufferBuilder var56 = ColorPipeline.a();
               if (var56 != null) {
                  float var57 = var52 + var51 / 2.0F;
                  float var58 = var45 + var33 / 2.0F;
                  Color var59 = var53 ? new Color(255, 80, 80) : new Color(160, 165, 175);
                  int var60 = (int)(255.0F * var6) << 24 | var59.getRGB() & 16777215;
                  ColorPipeline.a(var56, var57 - 2.5F, var58 - 2.5F, var57 + 2.5F, var58 + 2.5F, 1.2F, var60);
                  ColorPipeline.a(var56, var57 + 2.5F, var58 - 2.5F, var57 - 2.5F, var58 + 2.5F, 1.2F, var60);
                  ColorPipeline.a(var56, false);
               }
            }

            var44 += var48 + var34;
         }
      }

      Z();
      Scissor.c();
      var1.disableScissor();
   }

   private List<AutoSborScreen.a> b(String var1) {
      String var2 = var1.trim().toLowerCase();
      ArrayList var3 = new ArrayList();

      for (AutoSborScreen.a var5 : CATALOG_ITEMS) {
         if (!this.c(var5.getStack()) && (var2.isEmpty() || var5.getName().toLowerCase().contains(var2))) {
            var3.add(var5);
         }
      }

      return var3;
   }

   private boolean c(ItemStack var1) {
      for (ItemStack var5 : AutoSbor.TOP_SLOTS) {
         if (var5 != null && !var5.isEmpty() && a(var5, var1)) {
            return true;
         }
      }

      for (ItemStack var9 : AutoSbor.INVENTORY_SLOTS) {
         if (var9 != null && !var9.isEmpty() && a(var9, var1)) {
            return true;
         }
      }

      return false;
   }

   private static boolean a(ItemStack var0, ItemStack var1) {
      if (var0 != null && var1 != null) {
         if (var0.getItem() != var1.getItem()) {
            return false;
         }

         String var2 = d(var0);
         String var3 = d(var1);
         return var2.equalsIgnoreCase(var3);
      } else {
         return false;
      }
   }

   private static String d(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return "";
      } else if (var0.isOf(Items.ENCHANTED_GOLDEN_APPLE)) {
         Text var6 = var0.get(DataComponentTypes.CUSTOM_NAME);
         return var6 != null ? c(var6.getString()) : "Зачарованное яблоко";
      } else if (var0.isOf(Items.GOLDEN_APPLE)) {
         Text var5 = var0.get(DataComponentTypes.CUSTOM_NAME);
         return var5 != null ? c(var5.getString()) : "Золотое яблоко";
      } else {
         Text var1 = var0.get(DataComponentTypes.CUSTOM_NAME);
         String var2 = var1 != null ? var1.getString() : var0.getName().getString();
         String var3 = c(var2);
         String var4 = var3.toLowerCase(Locale.ROOT);
         if (!var4.contains("armortality") && !var4.contains("арморталити") && !var4.contains("ᴀʀᴍᴏʀᴛᴀʟɪᴛʏ")) {
            return !var4.contains("immortality") && !var4.contains("имморталити") && !var4.contains("ɪᴍᴍᴏʀᴛᴀʟɪᴛʏ") && !var4.contains("immortal")
               ? var3
               : "Сфера Имморталити";
         } else {
            return "Сфера Арморталити";
         }
      }
   }

   private static String c(String var0) {
      return var0 == null ? "" : var0.replaceAll("(?i)§[0-9a-fk-or#x]", "").replaceAll("(?i)&[0-9a-fk-or#x]", "").replaceAll("§", "").trim();
   }

   private ItemStack aa() {
      if (this.selectedSlotType == 0) {
         if (this.selectedSlotIndex >= 0 && this.selectedSlotIndex < AutoSbor.TOP_SLOTS.length) {
            return AutoSbor.TOP_SLOTS[this.selectedSlotIndex];
         }
      } else if (this.selectedSlotType == 1 && this.selectedSlotIndex >= 0 && this.selectedSlotIndex < AutoSbor.INVENTORY_SLOTS.length) {
         return AutoSbor.INVENTORY_SLOTS[this.selectedSlotIndex];
      }

      return null;
   }

   @Override
   public boolean mouseClicked(Click click, boolean doubled) {
      if (!this.closing && !(this.openAnim.getValue() < 0.85F)) {
         float var3 = RenderUtil.e();
         float var4 = (float)(click.x() * var3);
         float var5 = (float)(click.y() * var3);
         int var6 = click.button();
         float var7 = RenderUtil.a();
         float var8 = RenderUtil.b();
         float var9 = Math.clamp(this.settingsAnim.getValue(), 0.0F, 1.0F);
         float var10 = 407.0F + (var9 > 0.01F ? 142.0F * var9 : 0.0F);
         float var11 = 236.0F;
         float var12 = (var7 - var10) / 2.0F;
         float var13 = var12 + 120.0F + 7.0F;
         float var14 = (var8 - var11) / 2.0F;
         float var15 = var12;
         float var16 = var14;
         float var17 = var13 + 280.0F + 7.0F;
         float var18 = var14;
         float var19 = var13;
         float var20 = var14 + 195.0F + 7.0F;
         if (var6 == 0
            && var4 >= this.closeBtnBox[0]
            && var4 <= this.closeBtnBox[0] + this.closeBtnBox[2]
            && var5 >= this.closeBtnBox[1]
            && var5 <= this.closeBtnBox[1] + this.closeBtnBox[3]) {
            this.ab();
            return true;
         }

         if (var9 > 0.01F
            && var6 == 0
            && var4 >= this.settingsCloseBtnBox[0]
            && var4 <= this.settingsCloseBtnBox[0] + this.settingsCloseBtnBox[2]
            && var5 >= this.settingsCloseBtnBox[1]
            && var5 <= this.settingsCloseBtnBox[1] + this.settingsCloseBtnBox[3]) {
            this.selectedSlotType = -1;
            this.selectedSlotIndex = -1;
            return true;
         }

         if (var4 >= var15 && var4 <= var15 + 120.0F && var5 >= var16 && var5 <= var16 + 195.0F) {
            this.configInputFocused = false;
            this.configInputSelectAll = false;
            float var66 = var15 + 8.0F;
            float var69 = var16 + 22.0F;
            float var72 = 104.0F;
            float var76 = 15.0F;
            if (var6 == 0 && var4 >= var66 && var4 <= var66 + var72 && var5 >= var69 && var5 <= var69 + var76) {
               this.searchFocused = true;
               this.searchSelectAll = false;
               return true;
            }

            this.searchFocused = false;
            this.searchSelectAll = false;
            float var80 = var69 + var76 + 7.0F;
            float var84 = 195.0F - (var80 - var16) - 8.0F;
            float var87 = var15 + 8.0F;
            if (this.searchMaxScroll > 0.5F) {
               float var90 = 7.0F;
               float var94 = var15 + 120.0F - 8.5F;
               if (var4 >= var94 && var4 <= var94 + var90 && var5 >= var80 && var5 <= var80 + var84 && var6 == 0) {
                  this.searchScrollDragging = true;
                  this.a(var5, var80, var84);
                  return true;
               }
            }

            if (var5 >= var80 && var5 <= var80 + var84) {
               List var91 = this.b(this.searchBuffer.toString());
               float var95 = this.searchScrollAnim.getValue();

               for (int var98 = 0; var98 < var91.size(); var98++) {
                  int var101 = var98 / 4;
                  int var103 = var98 % 4;
                  float var105 = var87 + var103 * 23.0F;
                  float var107 = var80 + var101 * 23.0F + var95;
                  if (!(var107 + 20.0F < var80 - 4.0F)
                     && !(var107 > var80 + var84 + 4.0F)
                     && var4 >= var105
                     && var4 <= var105 + 20.0F
                     && var5 >= var107
                     && var5 <= var107 + 20.0F
                     && var6 == 0) {
                     AutoSborScreen.a var110 = (AutoSborScreen.a)var91.get(var98);
                     if (this.e(var110.getStack().copy())) {
                        return true;
                     }
                  }
               }
            }

            return true;
         } else if (var4 >= var13 && var4 <= var13 + 280.0F && var5 >= var14 && var5 <= var14 + 195.0F) {
            this.configInputFocused = false;
            this.configInputSelectAll = false;
            this.searchFocused = false;
            this.searchSelectAll = false;
            float var65 = 223.5F;
            float var68 = var13 + (280.0F - var65) / 2.0F;
            float var71 = var14 + 24.0F;

            for (int var74 = 0; var74 < 8; var74++) {
               float var78 = var68 + var74 * 28.5F;
               float var82 = var71;
               if (var4 >= var78 && var4 <= var78 + 24.0F && var5 >= var82 && var5 <= var82 + 24.0F) {
                  this.a(0, var74, var6);
                  return true;
               }
            }

            float var75 = 252.0F;
            float var79 = var13 + (280.0F - var75) / 2.0F;
            float var83 = var14 + 60.0F;

            for (int var86 = 0; var86 < 3; var86++) {
               for (int var89 = 0; var89 < 9; var89++) {
                  int var93 = var86 * 9 + var89;
                  float var97 = var79 + var89 * 28.5F;
                  float var100 = var83 + var86 * 28.5F;
                  if (var4 >= var97 && var4 <= var97 + 24.0F && var5 >= var100 && var5 <= var100 + 24.0F) {
                     this.a(1, var93, var6);
                     return true;
                  }
               }
            }

            return true;
         } else if (var9 > 0.01F && var4 >= var17 && var4 <= var17 + 135.0F && var5 >= var18 && var5 <= var18 + 195.0F) {
            this.configInputFocused = false;
            this.configInputSelectAll = false;
            this.searchFocused = false;
            this.searchSelectAll = false;
            ItemStack var64 = this.aa();
            int var67 = this.selectedSlotType;
            int var70 = this.selectedSlotIndex;
            if (var64 == null || var64.isEmpty()) {
               var64 = this.lastSelectedStack;
               var67 = this.lastSelectedSlotType;
               var70 = this.lastSelectedSlotIndex;
            }

            if (var64 != null && !var64.isEmpty() && var67 != -1 && var70 != -1) {
               boolean var73 = AutoSbor.n(var64);
               boolean var77 = AutoSbor.m(var64);
               boolean var81 = AutoSbor.V(var64);
               boolean var85 = AutoSbor.W(var64);
               boolean var88 = AutoSbor.X(var64);
               boolean var92 = AutoSbor.Y(var64);
               boolean var96 = var81 || var85 || var88 || var92;
               float var99 = var96 ? 20.0F : (!var73 && !var77 ? 34.0F : 26.0F);
               float var102 = var18 + (var96 ? 16.0F : (!var73 && !var77 ? 28.0F : 22.0F));
               float var104 = var102 + var99 + (var96 ? 3.0F : (!var73 && !var77 ? 9.0F : 5.0F));
               boolean var106 = AutoSbor.e(var64);
               if (var106) {
                  float var108 = var104 + (var96 ? 9.0F : (!var73 && !var77 ? 16.0F : 12.0F));
                  if (var6 == 0
                     && var4 >= this.durInputBox[0]
                     && var4 <= this.durInputBox[0] + this.durInputBox[2]
                     && var5 >= this.durInputBox[1]
                     && var5 <= this.durInputBox[1] + this.durInputBox[3]) {
                     this.durFocused = true;
                     this.durBuffer.setLength(0);
                     this.durBuffer.append(AutoSbor.a(var67, var70));
                     this.durSelectAll = true;
                     this.countFocused = false;
                     this.countSelectAll = false;
                     return true;
                  }

                  this.durFocused = false;
                  this.durSelectAll = false;
                  float var36 = var17 + 9.0F;
                  float var37 = var108 + (var96 ? 13.0F : (!var73 && !var77 ? 17.0F : 15.0F));
                  float var38 = 117.0F;
                  float var39 = 3.0F;
                  if (var6 == 0 && var4 >= var36 - 5.0F && var4 <= var36 + var38 + 5.0F && var5 >= var37 - 6.0F && var5 <= var37 + var39 + 6.0F) {
                     this.sliderDragging = true;
                     this.b(var4, var36, var38);
                     return true;
                  }

                  float var40 = var37 + (var96 ? 8.0F : (!var73 && !var77 ? 15.0F : 10.0F));
                  int[] var41 = new int[]{0, 50, 80, 100};
                  float var42 = 3.0F;
                  float var43 = (var38 - (var41.length - 1) * var42) / var41.length;
                  float var44 = var96 ? 9.0F : (!var73 && !var77 ? 12.0F : 10.0F);

                  for (int var45 = 0; var45 < var41.length; var45++) {
                     float var46 = var36 + var45 * (var43 + var42);
                     if (var6 == 0 && var4 >= var46 && var4 <= var46 + var43 && var5 >= var40 && var5 <= var40 + var44) {
                        AutoSbor.a(var67, var70, var41[var45]);
                        ConfigManager.onSettingChanged();
                        return true;
                     }
                  }

                  if (var73) {
                     float var120 = var40 + var44 + 8.0F;
                     float var124 = var120 + 10.0F;
                     float var47 = 2.0F;
                     float var48 = (var38 - 8.0F * var47) / 9.0F;
                     float var49 = 10.0F;

                     for (int var50 = 1; var50 <= 9; var50++) {
                        float var51 = var36 + (var50 - 1) * (var48 + var47);
                        if (var6 == 0 && var4 >= var51 && var4 <= var51 + var48 && var5 >= var124 && var5 <= var124 + var49) {
                           int var52 = AutoSbor.b(var67, var70);
                           AutoSbor.b(var67, var70, var52 == var50 ? 0 : var50);
                           ConfigManager.onSettingChanged();
                           return true;
                        }
                     }

                     float var135 = var124 + var49 + 7.0F;
                     float var139 = 13.0F;
                     if (var6 == 0 && var4 >= var36 && var4 <= var36 + var38 && var5 >= var135 && var5 <= var135 + var139) {
                        boolean var143 = AutoSbor.c(var67, var70);
                        AutoSbor.a(var67, var70, !var143);
                        ConfigManager.onSettingChanged();
                        return true;
                     }
                  } else if (var77) {
                     float var121 = var40 + var44 + 10.0F;
                     float var125 = var121 + 11.0F;
                     float var128 = 2.0F;
                     float var131 = (var38 - 9.0F * var128) / 10.0F;
                     float var133 = 11.0F;

                     for (int var136 = 1; var136 <= 10; var136++) {
                        float var140 = var36 + (var136 - 1) * (var131 + var128);
                        if (var6 == 0 && var4 >= var140 && var4 <= var140 + var131 && var5 >= var125 && var5 <= var125 + var133) {
                           int var144 = AutoSbor.d(var67, var70);
                           AutoSbor.c(var67, var70, var144 == var136 ? 0 : var136);
                           ConfigManager.onSettingChanged();
                           return true;
                        }
                     }
                  } else if (var96) {
                     float var122 = var40 + var44 + (!var81 && !var92 ? 7.0F : 5.0F);
                     float var126 = var122 + 8.0F;
                     float var129 = 2.0F;
                     float var132 = (var38 - 4.0F * var129) / 5.0F;
                     float var134 = 9.0F;

                     for (int var137 = 1; var137 <= 5; var137++) {
                        float var141 = var36 + (var137 - 1) * (var132 + var129);
                        if (var6 == 0 && var4 >= var141 && var4 <= var141 + var132 && var5 >= var126 && var5 <= var126 + var134) {
                           int var145 = AutoSbor.e(var67, var70);
                           AutoSbor.d(var67, var70, var145 == var137 ? 0 : var137);
                           ConfigManager.onSettingChanged();
                           return true;
                        }
                     }

                     float var138 = var126 + var134 + (!var81 && !var92 ? 6.0F : 4.0F);
                     float var142 = var138 + 8.0F;
                     float var146 = 2.0F;
                     float var53 = (var38 - 4.0F * var146) / 5.0F;
                     float var54 = 9.0F;

                     for (int var55 = 1; var55 <= 5; var55++) {
                        float var56 = var36 + (var55 - 1) * (var53 + var146);
                        if (var6 == 0 && var4 >= var56 && var4 <= var56 + var53 && var5 >= var142 && var5 <= var142 + var54) {
                           int var57 = AutoSbor.f(var67, var70);
                           AutoSbor.e(var67, var70, var57 == var55 ? 0 : var55);
                           ConfigManager.onSettingChanged();
                           return true;
                        }
                     }

                     if (var81) {
                        float var147 = var142 + var54 + 4.0F;
                        float var149 = var147 + 8.0F;
                        float var151 = 2.0F;
                        float var58 = (var38 - 2.0F * var151) / 3.0F;
                        float var59 = 9.0F;

                        for (int var60 = 1; var60 <= 3; var60++) {
                           float var61 = var36 + (var60 - 1) * (var58 + var151);
                           if (var6 == 0 && var4 >= var61 && var4 <= var61 + var58 && var5 >= var149 && var5 <= var149 + var59) {
                              int var62 = AutoSbor.g(var67, var70);
                              AutoSbor.f(var67, var70, var62 == var60 ? 0 : var60);
                              ConfigManager.onSettingChanged();
                              return true;
                           }
                        }
                     } else if (var92) {
                        float var148 = var142 + var54 + 4.0F;
                        float var150 = var148 + 8.0F;
                        float var152 = 2.0F;
                        float var153 = (var38 - 2.0F * var152) / 3.0F;
                        float var154 = 9.0F;

                        for (int var155 = 1; var155 <= 3; var155++) {
                           float var156 = var36 + (var155 - 1) * (var153 + var152);
                           if (var6 == 0 && var4 >= var156 && var4 <= var156 + var153 && var5 >= var150 && var5 <= var150 + var154) {
                              int var157 = AutoSbor.h(var67, var70);
                              AutoSbor.g(var67, var70, var157 == var155 ? 0 : var155);
                              ConfigManager.onSettingChanged();
                              return true;
                           }
                        }
                     }
                  }
               } else {
                  int var109 = a(var64);
                  float var111 = var104 + 16.0F;
                  if (var6 == 0
                     && var4 >= this.countInputBox[0]
                     && var4 <= this.countInputBox[0] + this.countInputBox[2]
                     && var5 >= this.countInputBox[1]
                     && var5 <= this.countInputBox[1] + this.countInputBox[3]) {
                     this.countFocused = true;
                     this.countBuffer.setLength(0);
                     this.countBuffer.append(var64.getCount());
                     this.countSelectAll = true;
                     this.durFocused = false;
                     this.durSelectAll = false;
                     return true;
                  }

                  this.countFocused = false;
                  this.countSelectAll = false;
                  float var112 = var17 + 9.0F;
                  float var113 = var111 + 17.0F;
                  float var114 = 117.0F;
                  float var115 = 3.0F;
                  if (var6 == 0 && var4 >= var112 - 5.0F && var4 <= var112 + var114 + 5.0F && var5 >= var113 - 6.0F && var5 <= var113 + var115 + 6.0F) {
                     this.sliderDragging = true;
                     this.a(var4, var112, var114, var64);
                     return true;
                  }

                  float var116 = var113 + 15.0F;
                  int[] var117 = a(var109);
                  float var118 = 3.0F;
                  float var119 = (var114 - (var117.length - 1) * var118) / var117.length;
                  float var123 = 12.0F;

                  for (int var127 = 0; var127 < var117.length; var127++) {
                     float var130 = var112 + var127 * (var119 + var118);
                     if (var6 == 0 && var4 >= var130 && var4 <= var130 + var119 && var5 >= var116 && var5 <= var116 + var123) {
                        var64.setCount(var117[var127]);
                        ConfigManager.onSettingChanged();
                        return true;
                     }
                  }
               }
            }

            return true;
         } else if (var4 >= var19 && var4 <= var19 + 280.0F && var5 >= var20 && var5 <= var20 + 34.0F) {
            this.searchFocused = false;
            this.searchSelectAll = false;
            this.durFocused = false;
            this.durSelectAll = false;
            this.countFocused = false;
            this.countSelectAll = false;
            if (var6 == 0
               && var4 >= this.configInputBox[0]
               && var4 <= this.configInputBox[0] + this.configInputBox[2]
               && var5 >= this.configInputBox[1]
               && var5 <= this.configInputBox[1] + this.configInputBox[3]) {
               this.configInputFocused = true;
               this.configInputSelectAll = false;
               return true;
            }

            this.configInputFocused = false;
            this.configInputSelectAll = false;
            if (var6 == 0
               && var4 >= this.configSaveBtnBox[0]
               && var4 <= this.configSaveBtnBox[0] + this.configSaveBtnBox[2]
               && var5 >= this.configSaveBtnBox[1]
               && var5 <= this.configSaveBtnBox[1] + this.configSaveBtnBox[3]) {
               String var63 = this.configInputBuffer.toString().trim();
               if (!var63.isEmpty()) {
                  AutoSbor.c(var63);
                  this.selectedConfigName = var63;
                  this.configInputBuffer.setLength(0);
               }

               return true;
            } else {
               float var21 = var19 + 140.0F;
               float var22 = var20 + 5.0F;
               float var23 = 135.0F;
               float var24 = 24.0F;
               if (var4 >= var21 && var4 <= var21 + var23 && var5 >= var22 && var5 <= var22 + var24) {
                  List var25 = AutoSbor.r();
                  float var26 = this.configScrollAnim.getValue();
                  float var27 = 18.0F;
                  float var28 = 4.0F;
                  float var29 = var21 + var26;
                  float var30 = var22 + (var24 - var27) / 2.0F;

                  for (String var32 : (Iterable<String>)(Iterable<?>)(var25)) {
                     float var33 = Msdf.a(MsdfManager.BOLD, var32, 4.8F) + 20.0F;
                     if (var4 >= var29 && var4 <= var29 + var33 && var5 >= var30 && var5 <= var30 + var27) {
                        float var34 = 12.0F;
                        float var35 = var29 + var33 - var34 - 2.0F;
                        if (var6 == 0 && var4 >= var35 && var4 <= var35 + var34) {
                           AutoSbor.e(var32);
                           if (var32.equals(this.selectedConfigName)) {
                              this.selectedConfigName = null;
                           }

                           return true;
                        }

                        if (var6 == 0) {
                           AutoSbor.d(var32);
                           this.selectedConfigName = var32;
                           this.h();
                           return true;
                        }
                     }

                     var29 += var33 + var28;
                  }
               }

               return true;
            }
         } else {
            this.searchFocused = false;
            this.searchSelectAll = false;
            this.countFocused = false;
            this.countSelectAll = false;
            this.durFocused = false;
            this.durSelectAll = false;
            this.configInputFocused = false;
            this.configInputSelectAll = false;
            return true;
         }
      } else {
         return true;
      }
   }

   private void a(float var1, float var2, float var3) {
      float var4 = Math.clamp((var1 - var2) / var3, 0.0F, 1.0F);
      this.searchScrollTarget = -var4 * this.searchMaxScroll;
   }

   private boolean e(ItemStack var1) {
      for (int var2 = 0; var2 < AutoSbor.TOP_SLOTS.length; var2++) {
         if (AutoSbor.TOP_SLOTS[var2] == null || AutoSbor.TOP_SLOTS[var2].isEmpty()) {
            AutoSbor.TOP_SLOTS[var2] = var1;
            this.selectedSlotType = 0;
            this.selectedSlotIndex = var2;
            ConfigManager.onSettingChanged();
            return true;
         }
      }

      for (int var3 = 0; var3 < AutoSbor.INVENTORY_SLOTS.length; var3++) {
         if (AutoSbor.INVENTORY_SLOTS[var3] == null || AutoSbor.INVENTORY_SLOTS[var3].isEmpty()) {
            AutoSbor.INVENTORY_SLOTS[var3] = var1;
            this.selectedSlotType = 1;
            this.selectedSlotIndex = var3;
            ConfigManager.onSettingChanged();
            return true;
         }
      }

      return false;
   }

   private void a(int var1, int var2, int var3) {
      ItemStack var4 = var1 == 0 ? AutoSbor.TOP_SLOTS[var2] : AutoSbor.INVENTORY_SLOTS[var2];
      if (var3 == 2) {
         if (var4 != null && !var4.isEmpty()) {
            if (var1 == 0) {
               AutoSbor.TOP_SLOTS[var2] = ItemStack.EMPTY;
               AutoSbor.a(0, var2, 0);
            } else {
               AutoSbor.INVENTORY_SLOTS[var2] = ItemStack.EMPTY;
               AutoSbor.a(1, var2, 0);
            }

            if (this.selectedSlotType == var1 && this.selectedSlotIndex == var2) {
               this.selectedSlotType = -1;
               this.selectedSlotIndex = -1;
            }

            ConfigManager.onSettingChanged();
         }
      } else if (var3 == 0 || var3 == 1) {
         if (var4 != null && !var4.isEmpty()) {
            this.selectedSlotType = var1;
            this.selectedSlotIndex = var2;
         } else {
            this.selectedSlotType = -1;
            this.selectedSlotIndex = -1;
         }
      }
   }

   private void a(float var1, float var2, float var3, ItemStack var4) {
      int var5 = a(var4);
      if (var5 <= 1) {
         var4.setCount(1);
      } else {
         float var6 = Math.clamp((var1 - var2) / var3, 0.0F, 1.0F);
         int var7 = 1 + Math.round(var6 * (var5 - 1));
         var4.setCount(Math.clamp(var7, 1, var5));
      }
   }

   private void b(float var1, float var2, float var3) {
      float var4 = Math.clamp((var1 - var2) / var3, 0.0F, 1.0F);
      int var5 = Math.round(var4 * 100.0F);
      AutoSbor.a(this.selectedSlotType, this.selectedSlotIndex, var5);
   }

   @Override
   public boolean mouseReleased(Click click) {
      if (this.sliderDragging) {
         ConfigManager.onSettingChanged();
      }

      this.sliderDragging = false;
      this.searchScrollDragging = false;
      return super.mouseReleased(click);
   }

   @Override
   public boolean mouseDragged(Click click, double offsetX, double offsetY) {
      if (!this.closing && !(this.openAnim.getValue() < 0.85F)) {
         float var6 = RenderUtil.e();
         float var7 = (float)(click.x() * var6);
         float var8 = (float)(click.y() * var6);
         if (this.sliderDragging) {
            ItemStack var9 = this.aa();
            if (var9 != null && !var9.isEmpty()) {
               float var19 = RenderUtil.a();
               float var20 = Math.clamp(this.settingsAnim.getValue(), 0.0F, 1.0F);
               float var21 = 407.0F + (var20 > 0.01F ? 142.0F * var20 : 0.0F);
               float var22 = (var19 - var21) / 2.0F;
               float var23 = var22 + 120.0F + 7.0F;
               float var24 = var23 + 280.0F + 7.0F;
               float var25 = var24 + 9.0F;
               float var26 = 117.0F;
               if (AutoSbor.e(var9)) {
                  this.b(var7, var25, var26);
               } else {
                  this.a(var7, var25, var26, var9);
               }

               return true;
            }
         }

         if (this.searchScrollDragging) {
            float var18 = RenderUtil.a();
            float var10 = RenderUtil.b();
            float var11 = Math.clamp(this.settingsAnim.getValue(), 0.0F, 1.0F);
            float var12 = 407.0F + (var11 > 0.01F ? 142.0F * var11 : 0.0F);
            float var13 = 236.0F;
            float var14 = (var18 - var12) / 2.0F;
            float var15 = (var10 - var13) / 2.0F;
            float var16 = var15 + 22.0F + 15.0F + 7.0F;
            float var17 = 195.0F - (var16 - var15) - 8.0F;
            this.a(var8, var16, var17);
            return true;
         } else {
            return super.mouseDragged(click, offsetX, offsetY);
         }
      } else {
         return true;
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (!this.closing && !(this.openAnim.getValue() < 0.85F)) {
         float var9 = RenderUtil.e();
         float var10 = (float)(mouseX * var9);
         float var11 = (float)(mouseY * var9);
         float var12 = RenderUtil.a();
         float var13 = RenderUtil.b();
         float var14 = Math.clamp(this.settingsAnim.getValue(), 0.0F, 1.0F);
         float var15 = 407.0F + (var14 > 0.01F ? 142.0F * var14 : 0.0F);
         float var16 = 236.0F;
         float var17 = (var12 - var15) / 2.0F;
         float var18 = var17 + 120.0F + 7.0F;
         float var19 = (var13 - var16) / 2.0F;
         float var20 = var17;
         float var21 = (var13 - var16) / 2.0F;
         float var22 = var18 + 280.0F + 7.0F;
         float var23 = var21;
         float var24 = var18;
         float var25 = var19 + 195.0F + 7.0F;
         if (var10 >= var24 + 140.0F && var10 <= var24 + 280.0F - 5.0F && var11 >= var25 && var11 <= var25 + 34.0F) {
            this.configScrollTarget += (float)(verticalAmount * 24.0);
            this.configScrollTarget = Math.clamp(this.configScrollTarget, -this.configMaxScroll, 0.0F);
            return true;
         }

         if (var10 >= var20 && var10 <= var20 + 120.0F && var11 >= var21 && var11 <= var21 + 195.0F) {
            this.searchScrollTarget += (float)(verticalAmount * 18.0);
            this.searchScrollTarget = Math.clamp(this.searchScrollTarget, -this.searchMaxScroll, 0.0F);
            return true;
         }

         if (var14 > 0.01F && var10 >= var22 && var10 <= var22 + 135.0F && var11 >= var23 && var11 <= var23 + 195.0F) {
            ItemStack var26 = this.aa();
            if (var26 != null && !var26.isEmpty()) {
               if (AutoSbor.e(var26)) {
                  int var29 = AutoSbor.a(this.selectedSlotType, this.selectedSlotIndex);
                  if (verticalAmount > 0.0) {
                     AutoSbor.a(this.selectedSlotType, this.selectedSlotIndex, Math.min(var29 + 5, 100));
                  } else if (verticalAmount < 0.0) {
                     AutoSbor.a(this.selectedSlotType, this.selectedSlotIndex, Math.max(var29 - 5, 0));
                  }

                  ConfigManager.onSettingChanged();
                  return true;
               }

               int var27 = a(var26);
               int var28 = var26.getCount();
               if (verticalAmount > 0.0) {
                  var26.setCount(Math.min(var28 + 1, var27));
               } else if (verticalAmount < 0.0) {
                  var26.setCount(Math.max(var28 - 1, 1));
               }

               ConfigManager.onSettingChanged();
               return true;
            }
         }

         return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
      } else {
         return true;
      }
   }

   @Override
   public boolean keyPressed(KeyInput input) {
      if (this.closing) {
         return true;
      }

      if (this.openAnim.getValue() < 0.85F && input.key() != 256) {
         return true;
      }

      int var2 = input.key();
      if (this.configInputFocused) {
         if (var2 == 256) {
            this.configInputFocused = false;
            this.configInputSelectAll = false;
            return true;
         }

         if (var2 == 65 && (input.modifiers() & 2) != 0) {
            this.configInputSelectAll = true;
            return true;
         }

         if (var2 == 259) {
            if (this.configInputSelectAll) {
               this.configInputBuffer.setLength(0);
               this.configInputSelectAll = false;
               return true;
            }

            if (!this.configInputBuffer.isEmpty()) {
               this.configInputBuffer.deleteCharAt(this.configInputBuffer.length() - 1);
            }

            return true;
         } else {
            if (var2 != 257 && var2 != 335) {
               return true;
            }

            String var9 = this.configInputBuffer.toString().trim();
            if (!var9.isEmpty()) {
               AutoSbor.c(var9);
               this.selectedConfigName = var9;
               this.configInputBuffer.setLength(0);
            }

            this.configInputFocused = false;
            this.configInputSelectAll = false;
            return true;
         }
      } else if (this.durFocused) {
         if (var2 != 256 && var2 != 257 && var2 != 335) {
            if (var2 == 65 && (input.modifiers() & 2) != 0) {
               this.durSelectAll = true;
               return true;
            }

            if (var2 != 259) {
               return true;
            }

            if (this.durSelectAll) {
               this.durBuffer.setLength(0);
               this.durSelectAll = false;
               AutoSbor.a(this.selectedSlotType, this.selectedSlotIndex, 0);
               ConfigManager.onSettingChanged();
               return true;
            }

            if (!this.durBuffer.isEmpty()) {
               this.durBuffer.deleteCharAt(this.durBuffer.length() - 1);
               if (this.durBuffer.isEmpty()) {
                  AutoSbor.a(this.selectedSlotType, this.selectedSlotIndex, 0);
               } else {
                  try {
                     int var8 = Integer.parseInt(this.durBuffer.toString());
                     AutoSbor.a(this.selectedSlotType, this.selectedSlotIndex, Math.clamp(var8, 0, 100));
                  } catch (Exception var6) {
                  }
               }

               ConfigManager.onSettingChanged();
            }

            return true;
         } else {
            this.durFocused = false;
            this.durSelectAll = false;
            return true;
         }
      } else if (this.countFocused) {
         ItemStack var3 = this.aa();
         int var4 = var3 != null ? a(var3) : 64;
         if (var2 != 256 && var2 != 257 && var2 != 335) {
            if (var2 == 65 && (input.modifiers() & 2) != 0) {
               this.countSelectAll = true;
               return true;
            }

            if (var2 != 259) {
               return true;
            }

            if (this.countSelectAll) {
               this.countBuffer.setLength(0);
               this.countSelectAll = false;
               if (var3 != null) {
                  var3.setCount(1);
               }

               ConfigManager.onSettingChanged();
               return true;
            } else {
               if (!this.countBuffer.isEmpty()) {
                  this.countBuffer.deleteCharAt(this.countBuffer.length() - 1);
                  if (var3 != null) {
                     if (this.countBuffer.isEmpty()) {
                        var3.setCount(1);
                     } else {
                        try {
                           int var5 = Integer.parseInt(this.countBuffer.toString());
                           var3.setCount(Math.clamp(var5, 1, var4));
                        } catch (Exception var7) {
                        }
                     }
                  }

                  ConfigManager.onSettingChanged();
               }

               return true;
            }
         } else {
            this.countFocused = false;
            this.countSelectAll = false;
            if (this.countBuffer.isEmpty() && var3 != null) {
               var3.setCount(1);
               ConfigManager.onSettingChanged();
            }

            return true;
         }
      } else if (this.searchFocused) {
         if (var2 == 256) {
            this.searchFocused = false;
            this.searchSelectAll = false;
            return true;
         }

         if (var2 == 65 && (input.modifiers() & 2) != 0) {
            this.searchSelectAll = true;
            return true;
         }

         if (var2 == 259) {
            if (this.searchSelectAll) {
               this.searchBuffer.setLength(0);
               this.searchSelectAll = false;
               this.searchScrollTarget = 0.0F;
               return true;
            }

            if (!this.searchBuffer.isEmpty()) {
               this.searchBuffer.deleteCharAt(this.searchBuffer.length() - 1);
               this.searchScrollTarget = 0.0F;
            }

            return true;
         } else {
            if (var2 != 257 && var2 != 335) {
               return true;
            }

            this.searchFocused = false;
            this.searchSelectAll = false;
            return true;
         }
      } else if (var2 == 256) {
         this.ab();
         return true;
      } else {
         return super.keyPressed(input);
      }
   }

   @Override
   public boolean charTyped(CharInput input) {
      if (this.closing || this.openAnim.getValue() < 0.85F) {
         return true;
      }

      if (this.configInputFocused && input.isValidChar()) {
         if (this.configInputSelectAll) {
            this.configInputBuffer.setLength(0);
            this.configInputSelectAll = false;
         }

         String var10 = input.asString();
         if (var10 != null && !var10.isEmpty() && this.configInputBuffer.length() < 24) {
            this.configInputBuffer.append(var10);
         }

         return true;
      } else {
         if (this.durFocused && input.isValidChar()) {
            String var2 = input.asString();
            if (var2 != null && !var2.isEmpty()) {
               char var3 = var2.charAt(0);
               if (var3 >= '0' && var3 <= '9') {
                  if (this.durSelectAll) {
                     this.durBuffer.setLength(0);
                     this.durSelectAll = false;
                  }

                  if (this.durBuffer.length() < 3) {
                     this.durBuffer.append(var3);

                     try {
                        int var12 = Integer.parseInt(this.durBuffer.toString());
                        AutoSbor.a(this.selectedSlotType, this.selectedSlotIndex, Math.clamp(var12, 0, 100));
                     } catch (Exception var7) {
                     }

                     ConfigManager.onSettingChanged();
                  }

                  return true;
               }
            }
         }

         if (this.countFocused && input.isValidChar()) {
            String var9 = input.asString();
            if (var9 != null && !var9.isEmpty()) {
               char var11 = var9.charAt(0);
               if (var11 >= '0' && var11 <= '9') {
                  if (this.countSelectAll) {
                     this.countBuffer.setLength(0);
                     this.countSelectAll = false;
                  }

                  if (this.countBuffer.length() < 4) {
                     this.countBuffer.append(var11);
                     ItemStack var4 = this.aa();
                     if (var4 != null) {
                        try {
                           int var5 = Integer.parseInt(this.countBuffer.toString());
                           int var6 = a(var4);
                           var4.setCount(Math.clamp(var5, 1, var6));
                        } catch (Exception var8) {
                        }
                     }

                     ConfigManager.onSettingChanged();
                  }

                  return true;
               }
            }
         }

         if (this.searchFocused && input.isValidChar()) {
            if (this.searchSelectAll) {
               this.searchBuffer.setLength(0);
               this.searchSelectAll = false;
            }

            this.searchBuffer.append(input.asString());
            this.searchScrollTarget = 0.0F;
            return true;
         } else {
            return super.charTyped(input);
         }
      }
   }

   @Override
   public void close() {
      this.ab();
   }

   @Override
   public void removed() {
      ClickGui.externalSlideProg = 0.0F;
      ConfigManager.onSettingChanged();
      super.removed();
   }

   private void ab() {
      if (!this.closing) {
         this.closing = true;
         this.searchFocused = false;
         this.durFocused = false;
         this.countFocused = false;
         this.configInputFocused = false;
      }
   }

   private void ac() {
      this.closing = false;
      ClickGui.externalSlideProg = 0.0F;
      ConfigManager.onSettingChanged();
      MinecraftClient var1 = MinecraftClient.getInstance();
      if (this.parent != null) {
         var1.setScreen(this.parent);
      } else {
         var1.setScreen(null);
      }
   }

   private void a(DrawContext var1, ItemStack var2, float var3, float var4, float var5, float var6) {
      if (var2 != null && !var2.isEmpty() && !(this.openAnim.getValue() < 0.05F)) {
         MinecraftClient var7 = MinecraftClient.getInstance();
         float var8 = 2.0F / var7.getWindow().getScaleFactor();
         float var9 = var3 + var5 / 2.0F;
         float var10 = var4 + var5 / 2.0F;
         Matrix3x2fStack var11 = var1.getMatrices();
         var11.pushMatrix();
         var11.translate(var9 * var8, var10 * var8);
         float var12 = var6 / 16.0F * var8;
         var11.scale(var12, var12);
         var11.translate(-8.0F, -8.0F);
         var1.drawItem(var2, 0, 0);
         var11.popMatrix();
      }
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   static {
      try {
         for (Field var3 : GameRenderer.class.getDeclaredFields()) {
            if (GuiRenderer.class.isAssignableFrom(var3.getType())) {
               GUI_RENDERER_FIELD = var3;
               GUI_RENDERER_FIELD.setAccessible(true);
            } else if (FogRenderer.class.isAssignableFrom(var3.getType())) {
               FOG_RENDERER_FIELD = var3;
               FOG_RENDERER_FIELD.setAccessible(true);
            }
         }
      } catch (Throwable var4) {
      }

      SPEED_SPHERE_STACK = t();
      DAMAGE_SPHERE_STACK = i();
      CERBERUS_SPHERE_STACK = j();
      ARMOR3_DAMAGE2_SPHERE_STACK = k();
      ARMOR2_DAMAGE3_SPHERE_STACK = l();
      FLASH_SPHERE_STACK = m();
      ARMORTALITY_SPHERE_STACK = n();
      IMMORTALITY_SPHERE_STACK = o();
      TALISMAN_INFINITY_STACK = W();
      TALISMAN_ETERNITY_STACK = X();
      TALISMAN_STINGER_STACK = Y();
      ETERNITY_SWORD_STACK = v();
      STINGER_PICKAXE_STACK = B();
      ETERNITY_PICKAXE_STACK = C();
      EXP_50_STACK = u();
      ETERNITY_HELMET_STACK = w();
      ETERNITY_CHESTPLATE_STACK = x();
      ETERNITY_LEGGINGS_STACK = y();
      ETERNITY_BOOTS_STACK = z();
      INFINITY_SWORD_STACK = J();
      INFINITY_HELMET_STACK = K();
      INFINITY_CHESTPLATE_STACK = L();
      INFINITY_LEGGINGS_STACK = M();
      INFINITY_BOOTS_STACK = N();
      CERBERUS_SWORD_STACK = O();
      BACKPACK_LVL1_STACK = P();
      BACKPACK_LVL2_STACK = Q();
      BACKPACK_LVL3_STACK = R();
      BACKPACK_LVL4_STACK = S();
      VAMPIRISM_STACK = T();
      POPPED_CHORUS_TRAP_STACK = U();
      WINNER_POTION_STACK = V();
      ENDERMAN_STACK = p();
      GRAVITY_STACK = q();
      JUSTICE_STACK = r();
      EXPLOSIVE_TRAP_STACK = D();
      EXPLOSIVE_THING_STACK = E();
      STUN_STACK = F();
      TURTLE_MASTER_POTION_STACK = G();
      SNOWBALL_STACK = s();
      HEALING_POTION_STACK = A();
      SPEED_POTION_STACK = H();
      STRENGTH_POTION_STACK = I();
      CATALOG_ITEMS = new ArrayList<>();
      CATALOG_ITEMS.add(new AutoSborScreen.a(INFINITY_HELMET_STACK.copy(), "Шлем INFINITY", 1));
      CATALOG_ITEMS.add(new AutoSborScreen.a(INFINITY_CHESTPLATE_STACK.copy(), "Нагрудник INFINITY", 1));
      CATALOG_ITEMS.add(new AutoSborScreen.a(INFINITY_LEGGINGS_STACK.copy(), "Поножи INFINITY", 1));
      CATALOG_ITEMS.add(new AutoSborScreen.a(INFINITY_BOOTS_STACK.copy(), "Ботинки INFINITY", 1));
      CATALOG_ITEMS.add(new AutoSborScreen.a(ETERNITY_HELMET_STACK.copy(), "Шлем ETERNITY", 1));
      CATALOG_ITEMS.add(new AutoSborScreen.a(ETERNITY_CHESTPLATE_STACK.copy(), "Нагрудник ETERNITY", 1));
      CATALOG_ITEMS.add(new AutoSborScreen.a(ETERNITY_LEGGINGS_STACK.copy(), "Поножи ETERNITY", 1));
      CATALOG_ITEMS.add(new AutoSborScreen.a(ETERNITY_BOOTS_STACK.copy(), "Ботинки ETERNITY", 1));
      CATALOG_ITEMS.add(new AutoSborScreen.a(new ItemStack(Items.NETHERITE_HELMET), "Незеритовый шлем", 1));
      CATALOG_ITEMS.add(new AutoSborScreen.a(new ItemStack(Items.NETHERITE_CHESTPLATE), "Незеритовый нагрудник", 1));
      CATALOG_ITEMS.add(new AutoSborScreen.a(new ItemStack(Items.NETHERITE_LEGGINGS), "Незеритовые поножи", 1));
      CATALOG_ITEMS.add(new AutoSborScreen.a(new ItemStack(Items.NETHERITE_BOOTS), "Незеритовые ботинки", 1));
      CATALOG_ITEMS.add(new AutoSborScreen.a(new ItemStack(Items.ELYTRA), "Элитры", 1));
      CATALOG_ITEMS.add(new AutoSborScreen.a(INFINITY_SWORD_STACK.copy(), "Меч INFINITY", 1));
      CATALOG_ITEMS.add(new AutoSborScreen.a(ETERNITY_SWORD_STACK.copy(), "Меч ETERNITY", 1));
      CATALOG_ITEMS.add(new AutoSborScreen.a(CERBERUS_SWORD_STACK.copy(), "Меч Цербера", 1));
      CATALOG_ITEMS.add(new AutoSborScreen.a(new ItemStack(Items.NETHERITE_SWORD), "Незеритовый меч", 1));
      CATALOG_ITEMS.add(new AutoSborScreen.a(STINGER_PICKAXE_STACK.copy(), "Кирка STINGER", 1));
      CATALOG_ITEMS.add(new AutoSborScreen.a(ETERNITY_PICKAXE_STACK.copy(), "Кирка ETERNITY", 1));
      CATALOG_ITEMS.add(new AutoSborScreen.a(new ItemStack(Items.NETHERITE_PICKAXE), "Незеритовая кирка", 1));
      CATALOG_ITEMS.add(new AutoSborScreen.a(SPEED_SPHERE_STACK.copy(), "Сфера на скорость 3", 64));
      CATALOG_ITEMS.add(new AutoSborScreen.a(DAMAGE_SPHERE_STACK.copy(), "Сфера на урон 3", 64));
      CATALOG_ITEMS.add(new AutoSborScreen.a(CERBERUS_SPHERE_STACK.copy(), "Сфера Цербера", 64));
      CATALOG_ITEMS.add(new AutoSborScreen.a(FLASH_SPHERE_STACK.copy(), "Сфера Флеша", 64));
      CATALOG_ITEMS.add(new AutoSborScreen.a(ARMORTALITY_SPHERE_STACK.copy(), "Сфера Арморталити", 64));
      CATALOG_ITEMS.add(new AutoSborScreen.a(IMMORTALITY_SPHERE_STACK.copy(), "Сфера Имморталити", 64));
      CATALOG_ITEMS.add(new AutoSborScreen.a(ARMOR3_DAMAGE2_SPHERE_STACK.copy(), "Сфера на Броня 3 Урон 2", 64));
      CATALOG_ITEMS.add(new AutoSborScreen.a(ARMOR2_DAMAGE3_SPHERE_STACK.copy(), "Сфера на Броня 2 Урон 3", 64));
      CATALOG_ITEMS.add(new AutoSborScreen.a(BACKPACK_LVL1_STACK.copy(), "Рюкзак 1 уровень", 1));
      CATALOG_ITEMS.add(new AutoSborScreen.a(BACKPACK_LVL2_STACK.copy(), "Рюкзак 2 уровень", 1));
      CATALOG_ITEMS.add(new AutoSborScreen.a(BACKPACK_LVL3_STACK.copy(), "Рюкзак 3 уровень", 1));
      CATALOG_ITEMS.add(new AutoSborScreen.a(BACKPACK_LVL4_STACK.copy(), "Рюкзак 4 уровень", 1));
      CATALOG_ITEMS.add(new AutoSborScreen.a(ENDERMAN_STACK.copy(), "Эндермен", 128));
      CATALOG_ITEMS.add(new AutoSborScreen.a(GRAVITY_STACK.copy(), "Гравитация", 64));
      CATALOG_ITEMS.add(new AutoSborScreen.a(JUSTICE_STACK.copy(), "Справедливость", 64));
      CATALOG_ITEMS.add(new AutoSborScreen.a(VAMPIRISM_STACK.copy(), "Вампиризм", 64));
      CATALOG_ITEMS.add(new AutoSborScreen.a(EXPLOSIVE_TRAP_STACK.copy(), "Взрывная трапка", 64));
      CATALOG_ITEMS.add(new AutoSborScreen.a(STUN_STACK.copy(), "Стан", 64));
      CATALOG_ITEMS.add(new AutoSborScreen.a(POPPED_CHORUS_TRAP_STACK.copy(), "Трапка", 64));
      CATALOG_ITEMS.add(new AutoSborScreen.a(EXPLOSIVE_THING_STACK.copy(), "Взрывная штучка", 64));
      CATALOG_ITEMS.add(new AutoSborScreen.a(SNOWBALL_STACK.copy(), "Снежок", 64));
      CATALOG_ITEMS.add(new AutoSborScreen.a(EXP_50_STACK.copy(), "Опыт 50", 128));
      CATALOG_ITEMS.add(new AutoSborScreen.a(WINNER_POTION_STACK.copy(), "Зелье победителя", 64));
      CATALOG_ITEMS.add(new AutoSborScreen.a(TURTLE_MASTER_POTION_STACK.copy(), "Зелье черепашьей мощи", 64));
      CATALOG_ITEMS.add(new AutoSborScreen.a(new ItemStack(Items.TOTEM_OF_UNDYING), "Тотем бессмертия", 64));
      CATALOG_ITEMS.add(new AutoSborScreen.a(TALISMAN_INFINITY_STACK.copy(), "Талисман Infinity", 64));
      CATALOG_ITEMS.add(new AutoSborScreen.a(TALISMAN_ETERNITY_STACK.copy(), "Талисман Eternity", 64));
      CATALOG_ITEMS.add(new AutoSborScreen.a(TALISMAN_STINGER_STACK.copy(), "Талисман Stinger", 64));
      CATALOG_ITEMS.add(new AutoSborScreen.a(new ItemStack(Items.GOLDEN_APPLE), "Золотое яблоко", 64));
      CATALOG_ITEMS.add(new AutoSborScreen.a(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE), "Зачарованное яблоко", 64));
      CATALOG_ITEMS.add(new AutoSborScreen.a(new ItemStack(Items.GOLDEN_CARROT), "Золотая морковь", 64));
      CATALOG_ITEMS.add(new AutoSborScreen.a(new ItemStack(Items.FIREWORK_ROCKET), "Фейерверк", 64));
      CATALOG_ITEMS.add(new AutoSborScreen.a(new ItemStack(Items.ENDER_PEARL), "Эндер-жемчуг", 128));
      CATALOG_ITEMS.add(new AutoSborScreen.a(new ItemStack(Items.CHORUS_FRUIT), "Плод хоруса", 64));
      CATALOG_ITEMS.add(new AutoSborScreen.a(HEALING_POTION_STACK.copy(), "Зелье исцеления", 64));
      CATALOG_ITEMS.add(new AutoSborScreen.a(SPEED_POTION_STACK.copy(), "Улучшенное зелье скорости", 64));
      CATALOG_ITEMS.add(new AutoSborScreen.a(STRENGTH_POTION_STACK.copy(), "Улучшенное зелье силы", 64));
   }

   record a(ItemStack stack, String name, int maxStack) {
      public ItemStack getStack() {
         return this.stack;
      }

      public String getName() {
         return this.name;
      }

      public int getMaxStack() {
         return this.maxStack;
      }
   }
}
