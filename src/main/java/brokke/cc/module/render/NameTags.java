package brokke.cc.module.render;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.module.combat.FixHp;
import brokke.cc.module.player.StreamerMode;
import brokke.cc.cosmetic.DonateItem;
import brokke.cc.cosmetic.SkinCache;
import brokke.cc.core.ServerHealth;
import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfManager;
import brokke.cc.manager.FriendManager;
import brokke.cc.manager.ItemClusterManager;
import brokke.cc.manager.NbtDumpManager;
import brokke.cc.manager.NicknameManager;
import brokke.cc.render.RenderUtil;
import brokke.cc.render.TexturePipeline;
import brokke.cc.render.Theme;
import brokke.cc.render.WorldToScreen;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.EventPriority;
import brokke.cc.system.events.impl.HudRenderEvent;
import brokke.cc.system.events.impl.Render2DEvent;
import brokke.cc.ui.ClickGui;
import brokke.cc.util.EnchantmentUtility;
import brokke.cc.util.ItemUtility;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.scoreboard.Team;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;

@NewFunction(name = "NameTags", description = "Показывает ники и здоровье через стены", category = Category.RENDER)
public class NameTags extends Function {
   private static final float TAG_FONT = 6.5F;
   private static final float HEAD_SIZE = 8.5F;
   private static final float ITEM_ICON_SIZE = 8.5F;
   private static final float HAND_FONT = 5.2F;
   private static final float HAND_ICON_SIZE = 7.0F;
   private static final float HAND_PAD_X = 3.5F;
   private static final float HAND_PAD_Y = 1.8F;
   private static final float HAND_ICON_GAP = 3.0F;
   private static final float HAND_LINE_GAP = 1.5F;
   private static final float PAD_X = 4.5F;
   private static final float PAD_Y = 2.5F;
   private static final float ARMOR_CELL_SIZE = 10.5F;
   private static final float ARMOR_CELL_GAP = 1.0F;
   private static final float ARMOR_ITEM_SCALE = 0.58F;
   private static final EquipmentSlot[] ARMOR_EQUIPMENT_SLOTS = new EquipmentSlot[]{
      EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
   };
   private final BooleanSetting targetPlayers = new BooleanSetting("Игроки", true);
   private final BooleanSetting showHead = new BooleanSetting("Показывать голову", true);
   private final BooleanSetting targetMobs = new BooleanSetting("Мобы", true);
   private final BooleanSetting targetItems = new BooleanSetting("Предметы", true);
   private final BooleanSetting groupItems = new BooleanSetting("Группировать", true);
   private final NumberSetting groupDistance = new NumberSetting("Дистанция группы", 3.0, 1.0, 8.0, 0.5);
   private final NumberSetting maxGroupItems = new NumberSetting("Макс. предметов", 10.0, 3.0, 25.0, 1.0);
   private final BooleanSetting tntTimer = new BooleanSetting("ТНТ таймер", true);
   private final BooleanSetting handItems = new BooleanSetting("Руки игрока", false);
   private final NumberSetting size = new NumberSetting("Размер", 1.0, 0.2, 2.0, 0.05);
   private List<Entity> cachedTargets = List.of();
   private long cacheStamp = -1L;
   private long lastItemScan = 0L;
   private static final Map<UUID, NameTags.a> PREFIX_CACHE = new ConcurrentHashMap<>();
   private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("(?i)[§&][0-9a-z]");
   private static final Pattern UNICODE_RANGE_PATTERN = Pattern.compile("[\\uE000-\\uF8FF]");
   private static final Pattern INVALID_CHARS_PATTERN = Pattern.compile("[\\?\\uFFFD]+");
   private static final Pattern TRIM_DECORATIONS_PATTERN = Pattern.compile("^[\\u25C6\\u25C7\\u2605\\u2726\\u2727\\u2022\\*\\-\\.\\[\\]\\(\\)\\s]+");
   private static final Pattern PREFIX_HEX_PATTERN = Pattern.compile("(?i)^[a-f0-9б6]\\s+");
   private static final Pattern SUFFIX_HEX_PATTERN = Pattern.compile("(?i)\\s+[a-f0-9б6]$");
   private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("\\s+");
   private static final Pattern RANK_SYMBOLS_PATTERN = Pattern.compile("[⭐★\\[\\]\\|:•\\*\\-\\.]+");
   private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d+");
   private static final Pattern LEVEL_IV_PATTERN = Pattern.compile("(?i).*(?:^|[^a-zа-я0-9])(iv|4|40%|\\+4)(?:$|[^a-zа-я0-9]).*");
   private static final Pattern LEVEL_III_PATTERN = Pattern.compile("(?i).*(?:^|[^a-zа-я0-9])(iii|3|30%|\\+3)(?:$|[^a-zа-я0-9]).*");
   private static final Pattern LEVEL_II_PATTERN = Pattern.compile("(?i).*(?:^|[^a-zа-я0-9])(ii|2|20%|\\+2)(?:$|[^a-zа-я0-9]).*");
   private static final Pattern LEVEL_I_PATTERN = Pattern.compile("(?i).*(?:^|[^a-zа-я0-9])(i|1|10%|\\+1)(?:$|[^a-zа-я0-9]).*");
   private static final Map<String, String> SPHERE_SKIN_CACHE = new ConcurrentHashMap<>();
   private static final Path SPHERE_CACHE_FILE = FabricLoader.getInstance().getGameDir().resolve("brokke_sphere_cache.json");
   private static boolean SPHERE_CACHE_LOADED = false;
   private static final Pattern TALISMAN_PATTERN = Pattern.compile("(?i)((?:[a-zA-Zа-яА-Я0-9_-]+\\s+)*талисман(?:\\s+[a-zA-Zа-яА-Я0-9_-]+)*)");
   private static final Pattern TEXTURE_HASH_PATTERN = Pattern.compile("textures\\.minecraft\\.net/texture/([a-zA-Z0-9]+)");
   private static final Pattern B64_SKIN_PATTERN = Pattern.compile("(eyJ0ZXh0[A-Za-z0-9+/=]+)");
   private static final Pattern SHARPNESS_PATTERN = Pattern.compile("(?i)(?:острота|sharpness|остр[\\.\\s])\\s*([ivxlcdm\\d]+)");
   private static final Map<ItemStack, String> ITEM_NAME_CACHE = new WeakHashMap<>();

   public static boolean a(Entity var0) {
      NameTags var1 = FunctionManager.a(NameTags.class);
      return var1 != null && var1.isActive() ? var1.b(var0) : false;
   }

   public NameTags() {
      this.addSettings(
         this.targetPlayers,
         this.showHead,
         this.targetMobs,
         this.targetItems,
         this.groupItems,
         this.groupDistance,
         this.maxGroupItems,
         this.tntTimer,
         this.handItems,
         this.size
      );
   }

   private List<Entity> l() {
      if (mc.world == null) {
         return List.of();
      }

      long var1 = mc.world.getTime() / 3L;
      if (var1 != this.cacheStamp) {
         this.cacheStamp = var1;
         ArrayList var3 = new ArrayList();

         for (Entity var5 : mc.world.getEntities()) {
            if (this.b(var5)) {
               var3.add(var5);
            }
         }

         this.cachedTargets = var3;
      }

      return this.cachedTargets;
   }

   private boolean b(Entity var1) {
      if (!var1.isAlive()) {
         return false;
      } else if (var1 instanceof ArmorStandEntity) {
         return false;
      } else if (var1 instanceof PlayerEntity) {
         return this.targetPlayers.isToggled();
      } else if (var1 instanceof ItemEntity) {
         return this.targetItems.isToggled();
      } else if (var1 instanceof TntEntity) {
         return this.tntTimer.isToggled();
      } else {
         return !(var1 instanceof AnimalEntity) && !(var1 instanceof MobEntity) ? false : this.targetMobs.isToggled();
      }
   }

   @EventHandler(priority = EventPriority.HIGH)
   public void onRender2D(Render2DEvent var1) {
      if (mc.player != null && mc.world != null && !mc.options.hudHidden) {
         Matrix4f var2 = RenderUtil.c();
         float var3 = var1.getTickCounter().getTickProgress(true);
         float var4 = RenderUtil.b();
         if (this.targetItems.isToggled() && this.groupItems.isToggled()) {
            ItemClusterManager.a(this.l(), (float)this.groupDistance.getValue(), (int)this.maxGroupItems.getValue(), var3);
            ItemClusterManager.a(var3, (float)this.size.getValue());
         } else {
            ItemClusterManager.a();
         }

         for (Entity var6 : this.l()) {
            if (var6.isAlive()
               && (var6 != mc.player || !mc.options.getPerspective().isFirstPerson())
               && !(var6 instanceof ItemEntity var7 && ItemClusterManager.a(var7))) {
               Vec3d var17 = var6.getLerpedPos(var3);
               float[] var8 = this.a(var17.add(0.0, var6.getHeight() + 0.15, 0.0));
               float[] var9 = this.a(var17);
               if ((var8 != null || var9 != null)
                  && (var9 == null || !(var9[1] < 0.0F))
                  && (var8 == null || !(var8[1] > var4) || var9 == null || !(var9[1] > var4))) {
                  float var10 = var8 != null ? var8[0] : var9[0];
                  float var11 = var8 != null ? var8[1] - 6.0F : 15.0F;
                  if (var11 < 15.0F) {
                     var11 = 15.0F;
                  }

                  float var12 = this.c(var6);
                  if (var6 instanceof PlayerEntity var13) {
                     this.a(var2, var13, var10, var11, var12);
                     if (this.handItems.isToggled() && var9 != null) {
                        this.a(var13, var9[0], var9[1], var12);
                     }
                  } else if (var6 instanceof ItemEntity var14) {
                     this.a(var2, var14, var10, var11, var12);
                  } else if (var6 instanceof TntEntity var15) {
                     this.a(var2, var15, var10, var11, var12, var3);
                  } else if (var6 instanceof LivingEntity var16) {
                     this.a(var2, var16, var10, var11, var12);
                  }
               }
            }
         }
      }
   }

   @EventHandler(priority = EventPriority.HIGH)
   public void onHudRender(HudRenderEvent var1) {
      if (mc.player != null && mc.world != null && !mc.options.hudHidden) {
         float var2 = mc.getRenderTickCounter().getTickProgress(true);
         DrawContext var3 = var1.getContext();
         float var4 = RenderUtil.b();
         if (this.targetItems.isToggled() && this.groupItems.isToggled()) {
            ItemClusterManager.a(var3, var2, (float)this.size.getValue());
         }

         for (Entity var6 : this.l()) {
            if (var6.isAlive() && !(var6 instanceof ItemEntity var7 && ItemClusterManager.a(var7))) {
               if (var6 instanceof PlayerEntity var18) {
                  if (var6 != mc.player || !mc.options.getPerspective().isFirstPerson()) {
                     Vec3d var19 = var6.getLerpedPos(var2);
                     float[] var20 = this.a(var19.add(0.0, var6.getHeight() + 0.15, 0.0));
                     float[] var21 = this.a(var19);
                     if ((var20 != null || var21 != null)
                        && (var21 == null || !(var21[1] < 0.0F))
                        && (var20 == null || !(var20[1] > var4) || var21 == null || !(var21[1] > var4))) {
                        float var22 = var20 != null ? var20[0] : var21[0];
                        float var23 = var20 != null ? var20[1] - 6.0F : 15.0F;
                        if (var23 < 15.0F) {
                           var23 = 15.0F;
                        }

                        float var24 = this.c(var6);
                        float var15 = 11.5F;
                        float var16 = var15 * var24;
                        float var17 = var23 - var16 - 2.0F - 10.5F * var24;
                        if (var17 < 2.0F) {
                           var17 = var23 + var16 + 2.0F;
                        }

                        this.a(var3, var18, var22, var17, var24);
                        if (this.handItems.isToggled() && var21 != null) {
                           this.b(var3, var18, var21[0], var21[1], var24);
                        }
                     }
                  }
               } else if (var6 instanceof ItemEntity var8) {
                  Vec3d var9 = var6.getLerpedPos(var2);
                  float[] var10 = this.a(var9.add(0.0, var6.getHeight() + 0.15, 0.0));
                  float[] var11 = this.a(var9);
                  if ((var10 != null || var11 != null)
                     && (var11 == null || !(var11[1] < 0.0F))
                     && (var10 == null || !(var10[1] > var4) || var11 == null || !(var11[1] > var4))) {
                     float var12 = var10 != null ? var10[0] : var11[0];
                     float var13 = var10 != null ? var10[1] - 6.0F : 15.0F;
                     if (var13 < 15.0F) {
                        var13 = 15.0F;
                     }

                     float var14 = this.c(var6);
                     this.a(var3, var8, var12, var13, var14);
                  }
               }
            }
         }
      }
   }

   private void a(DrawContext var1, ItemEntity var2, float var3, float var4, float var5) {
      ItemStack var6 = var2.getStack();
      if (!var6.isEmpty()) {
         String var7 = h(var6);
         if (var7.isEmpty()) {
            String var8 = NbtDumpManager.a(var6);
            var7 = StreamerMode.a(var8 != null ? var8 : var6.getName().getString());
         }

         int var27 = var6.getCount();
         String var9 = var27 > 1 ? String.valueOf(var27) : "";
         float var10 = Msdf.a(MsdfManager.BOLD, var7, 6.5F);
         float var11 = var9.isEmpty() ? 0.0F : Msdf.a(MsdfManager.BOLD, var9, 6.5F) + 3.0F;
         float var12 = Msdf.a(MsdfManager.BOLD, "|", 6.5F);
         float var13 = 13.0F + var11 + 3.0F + var12 + 3.0F + var10 + 4.5F;
         float var14 = 11.5F;
         float var15 = var3 - var13 / 2.0F;
         float var16 = var4 - var14;
         float var17 = var15 + 4.5F;
         float var18 = var16 + (var14 - 8.5F) / 2.0F;
         float var19 = var3 + (var17 + 4.25F - var3) * var5;
         float var20 = var4 + (var18 + 4.25F - var4) * var5;
         float var21 = 8.5F * var5;
         if (!ClickGui.a(var19 - var21 / 2.0F, var20 - var21 / 2.0F, var21, var21)) {
            Matrix3x2fStack var22 = var1.getMatrices();
            float var23 = 2.0F / mc.getWindow().getScaleFactor();
            float var24 = 0.53125F * var5 * var23;
            var22.pushMatrix();
            var22.translate(var19 * var23, var20 * var23);
            var22.scale(var24, var24);
            var22.translate(-8.0F, -8.0F);

            try {
               var1.drawItem(var6, 0, 0);
            } catch (Exception var26) {
            }

            var22.popMatrix();
         }
      }
   }

   private void a(DrawContext var1, PlayerEntity var2, float var3, float var4, float var5) {
      ItemStack[] var6 = new ItemStack[6];
      int var7 = 0;
      ItemStack var8 = var2.getOffHandStack();
      if (!var8.isEmpty()) {
         var6[var7++] = var8;
      }

      for (EquipmentSlot var12 : ARMOR_EQUIPMENT_SLOTS) {
         ItemStack var13 = var2.getEquippedStack(var12);
         if (!var13.isEmpty()) {
            var6[var7++] = var13;
         }
      }

      ItemStack var23 = var2.getMainHandStack();
      if (!var23.isEmpty()) {
         var6[var7++] = var23;
      }

      if (var7 != 0) {
         float var24 = 10.5F * var5;
         float var25 = 1.0F * var5;
         float var26 = var7 * var24 + (var7 - 1) * var25;
         float var27 = var3 - var26 / 2.0F;
         Matrix3x2fStack var14 = var1.getMatrices();
         float var15 = 2.0F / mc.getWindow().getScaleFactor();
         float var16 = 0.38062498F * var5 * var15;
         String var17 = StreamerMode.a(var2.getName().getString());
         boolean var18 = FriendManager.a(var17);

         for (int var19 = 0; var19 < var7; var19++) {
            int var20 = var7 - 1 - var19;
            ItemStack var21 = var6[var20];
            float var22 = var27 + var19 * (var24 + var25);
            if (!ClickGui.a(var22, var4, var24, var24)) {
               if (var18) {
                  this.b(RenderUtil.c(), var22, var4, var24, var24, 2.5F * var5, 2.5F * var5, 2.5F * var5, 2.5F * var5);
               } else {
                  this.a(RenderUtil.c(), var22, var4, var24, var24, 2.5F * var5, 2.5F * var5, 2.5F * var5, 2.5F * var5);
               }

               var14.pushMatrix();
               var14.translate((var22 + var24 / 2.0F) * var15, (var4 + var24 / 2.0F) * var15);
               var14.scale(var16, var16);
               var14.translate(-8.0F, -8.0F);
               var1.drawItem(var21, 0, 0);
               var14.popMatrix();
            }
         }
      }
   }

   private NameTags.b a(PlayerEntity var1) {
      if (var1 == null) {
         return null;
      }

      UUID var2 = var1.getUuid();
      long var3 = System.currentTimeMillis();
      NameTags.a var5 = PREFIX_CACHE.get(var2);
      if (var5 != null && var3 - var5.timestamp < 1200L) {
         return var5.info;
      }

      NameTags.b var6 = this.b(var1);
      PREFIX_CACHE.put(var2, new NameTags.a(var6, var3));
      return var6;
   }

   private NameTags.b b(PlayerEntity var1) {
      Team var2 = var1.getScoreboardTeam();
      if (var2 == null && mc.world != null) {
         var2 = mc.world.getScoreboard().getScoreHolderTeam(var1.getNameForScoreboard());
      }

      if (var2 == null && mc.getNetworkHandler() != null) {
         PlayerListEntry var3 = mc.getNetworkHandler().getPlayerListEntry(var1.getUuid());
         if (var3 != null) {
            var2 = var3.getScoreboardTeam();
         }
      }

      String var10 = null;
      Integer var4 = null;
      if (var2 != null) {
         Text var5 = var2.getPrefix();
         if (var5 != null && !var5.getString().isBlank()) {
            var10 = var5.getString();
            if (var5.getStyle() != null && var5.getStyle().getColor() != null) {
               var4 = var5.getStyle().getColor().getRgb();
            }
         }

         if (var4 == null && var2.getColor() != null && var2.getColor().getColorValue() != null) {
            var4 = var2.getColor().getColorValue();
         }
      }

      if ((var10 == null || var10.isBlank()) && mc.getNetworkHandler() != null) {
         PlayerListEntry var11 = mc.getNetworkHandler().getPlayerListEntry(var1.getUuid());
         if (var11 != null && var11.getDisplayName() != null) {
            Text var6 = var11.getDisplayName();
            String var7 = var6.getString();
            String var8 = var1.getName().getString();
            int var9 = var7.indexOf(var8);
            if (var9 > 0) {
               var10 = var7.substring(0, var9);
               if (var6.getStyle() != null && var6.getStyle().getColor() != null) {
                  var4 = var6.getStyle().getColor().getRgb();
               }
            }
         }
      }

      if (var10 != null && !var10.isBlank()) {
         if (var4 == null) {
            var4 = a(var10);
         }

         String var12 = b(var10);
         if (!var12.isEmpty() && !var12.equalsIgnoreCase("player") && !var12.equalsIgnoreCase("игрок")) {
            if (var4 == null || var4 == 16777215) {
               var4 = c(var12);
            }

            int var13 = 0xFF000000 | var4 & 16777215;
            return new NameTags.b(var12, var13);
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static Integer a(String var0) {
      if (var0 == null) {
         return null;
      }

      for (int var1 = 0; var1 < var0.length() - 1; var1++) {
         if (var0.charAt(var1) == 167 || var0.charAt(var1) == '&') {
            char var2 = Character.toLowerCase(var0.charAt(var1 + 1));
            switch (var2) {
               case '0':
                  return 0;
               case '1':
                  return 170;
               case '2':
                  return 43520;
               case '3':
                  return 43690;
               case '4':
                  return 11141120;
               case '5':
                  return 11141290;
               case '6':
                  return 16755200;
               case '7':
                  return 11184810;
               case '8':
                  return 5592405;
               case '9':
                  return 5592575;
               case ':':
               case ';':
               case '<':
               case '=':
               case '>':
               case '?':
               case '@':
               case 'A':
               case 'B':
               case 'C':
               case 'D':
               case 'E':
               case 'F':
               case 'G':
               case 'H':
               case 'I':
               case 'J':
               case 'K':
               case 'L':
               case 'M':
               case 'N':
               case 'O':
               case 'P':
               case 'Q':
               case 'R':
               case 'S':
               case 'T':
               case 'U':
               case 'V':
               case 'W':
               case 'X':
               case 'Y':
               case 'Z':
               case '[':
               case '\\':
               case ']':
               case '^':
               case '_':
               case '`':
               default:
                  break;
               case 'a':
                  return 5635925;
               case 'b':
                  return 5636095;
               case 'c':
                  return 16733525;
               case 'd':
                  return 16733695;
               case 'e':
                  return 16777045;
               case 'f':
                  return 16777215;
            }
         }
      }

      return null;
   }

   private static String b(String var0) {
      if (var0 == null) {
         return "";
      }

      var0 = k(var0);
      var0 = COLOR_CODE_PATTERN.matcher(var0).replaceAll("");
      var0 = UNICODE_RANGE_PATTERN.matcher(var0).replaceAll("");
      var0 = RANK_SYMBOLS_PATTERN.matcher(var0).replaceAll(" ");
      var0 = MULTI_SPACE_PATTERN.matcher(var0.trim()).replaceAll(" ");
      return var0.toUpperCase();
   }

   private static int c(String var0) {
      if (var0 == null) {
         return -1;
      } else {
         String var1 = var0.toUpperCase();
         if (var1.contains("СТАЖ") || var1.contains("STAZ") || var1.contains("HELP") || var1.contains("ХЕЛП")) {
            return -16718337;
         } else if (var1.contains("ETERNITY") || var1.contains("ЭТЕРНИТИ")) {
            return -43521;
         } else if (var1.contains("STINGER") || var1.contains("СТИНГЕР")) {
            return -37312;
         } else if (var1.contains("DRAGON") || var1.contains("ДРАГОН")) {
            return -4560696;
         } else if (var1.contains("KRAKEN") || var1.contains("КРАКЕН")) {
            return -16718218;
         } else if (var1.contains("WITHER") || var1.contains("ВИЗЕР")) {
            return 37120;
         } else if (var1.contains("GHAST") || var1.contains("ГАСТ")) {
            return -16718337;
         } else if (var1.contains("MUSTANG") || var1.contains("МУСТАНГ")) {
            return 54784;
         } else if (var1.contains("GRIEF") || var1.contains("ГРИФ") || var1.contains("GRIF")) {
            return -59580;
         } else if (var1.contains("MAGMA") || var1.contains("МАГМА")) {
            return -35072;
         } else if (var1.contains("TITAN") || var1.contains("ТИТАН")) {
            return -13395457;
         } else if (var1.contains("HERO") || var1.contains("ГЕРОЙ")) {
            return -11141291;
         } else if (var1.contains("VIP") || var1.contains("ВИП")) {
            return -11141291;
         } else if (var1.contains("PREMIUM") || var1.contains("ПРЕМИУМ")) {
            return -16718337;
         } else if (var1.contains("DELUXE") || var1.contains("ДЕЛЮКС")) {
            return -43521;
         } else if (!var1.contains("SPONSOR") && !var1.contains("СПОНСОР")) {
            return !var1.contains("ADMIN") && !var1.contains("АДМИН") && !var1.contains("MODER") && !var1.contains("МОДЕР") ? -1 : -52429;
         } else {
            return 54784;
         }
      }
   }

   private void a(Matrix4f var1, PlayerEntity var2, float var3, float var4, float var5) {
      String var6 = StreamerMode.a(var2.getName().getString());
      boolean var7 = FriendManager.a(var6);
      String var8 = NicknameManager.a(var6);
      if (var8 == null) {
         var8 = var6;
      }

      float var33 = FixHp.scoreHealth(var2);
      if (var33 < 0.0F) {
         var33 = ServerHealth.a(var2);
      }
      float var10 = var33 >= 0.0F ? var33 : var2.getHealth();
      boolean varUnknown = FixHp.isUnknown(var2);
      if (varUnknown) {
         var10 = 10.0F;
      }
      int var11 = (int)var10;
      String var12 = varUnknown ? "?" : String.valueOf(var11);
      float var32 = var10 < 10.0F ? 8.5F : 6.5F;
      String var13 = var7 ? " [F]" : "";
      NameTags.b var14 = this.a(var2);
      String var15 = var14 != null ? var14.text : null;
      float var16 = var15 != null ? Msdf.a(MsdfManager.BOLD, var15, 6.5F) + 3.5F : 0.0F;
      float var17 = Msdf.a(MsdfManager.BOLD, var8, 6.5F);
      float var18 = Msdf.a(MsdfManager.BOLD, var12, var32);
      float var19 = Msdf.a(MsdfManager.ICONS, "7", 6.5F);
      float var20 = var7 ? Msdf.a(MsdfManager.BOLD, var13, 6.5F) : 0.0F;
      boolean var21 = this.showHead.isToggled();
      float var22 = var21 ? 12.0F : 0.0F;
      float var23 = 4.5F + var22 + var16 + var17 + var20 + 3.5F + var18 + 2.0F + var19 + 4.5F;
      float var24 = 11.5F;
      float var25 = var3 - var23 / 2.0F;
      float var26 = var4 - var24;
      Matrix4f var27 = this.a(var3, var4, var5);
      if (var7) {
         this.b(var27, var25, var26, var23, var24);
      } else {
         this.a(var27, var25, var26, var23, var24);
      }

      if (var21) {
         this.b(var27, var2, var25 + 4.5F, var26 + (var24 - 8.5F) / 2.0F, 8.5F);
      }

      float var28 = var26 + (var24 - Msdf.a(MsdfManager.BOLD, 6.5F)) / 2.0F;
      float var29 = var25 + 4.5F + var22;
      Msdf.b();
      if (var15 != null) {
         Msdf.a(var27, MsdfManager.BOLD, var15, var29, var28, 6.5F, var14.color);
         var29 += var16;
      }

      Msdf.a(var27, MsdfManager.BOLD, var8, var29, var28, 6.5F, var7 ? -1114126 : -1);
      float var31 = var29 + var17;
      if (var7) {
         Msdf.a(var27, MsdfManager.BOLD, var13, var31, var28, 6.5F, -12714101);
         var31 += var20;
      }

      var29 = var31 + 3.5F;
      int var30 = varUnknown ? 0xFFAAAAAA : b(var10);
      Msdf.a(var27, MsdfManager.BOLD, var12, var29, var28, var32, var30);
      var29 += var18 + 2.0F;
      Msdf.a(var27, MsdfManager.ICONS, "7", var29, var28 + 1.7F, 6.5F, var30);
      Msdf.c();
   }

   private void a(Matrix4f var1, LivingEntity var2, float var3, float var4, float var5) {
      String var6 = StreamerMode.a(var2.getDisplayName().getString());
      float var7 = var2.getMaxHealth();
      float var8 = var2.getHealth() + var2.getAbsorptionAmount();
      int var9 = (int)Math.ceil(var8);
      String var10 = "[ " + var9 + " ]";
      float var11 = Msdf.a(MsdfManager.BOLD, var6, 6.5F);
      float var12 = Msdf.a(MsdfManager.BOLD, var10, 6.5F);
      float var13 = Msdf.a(MsdfManager.BOLD, "|", 6.5F);
      float var14 = 4.5F + var11 + 3.5F + var13 + 3.5F + var12 + 4.5F;
      float var15 = 11.5F;
      float var16 = var3 - var14 / 2.0F;
      float var17 = var4 - var15;
      Matrix4f var18 = this.a(var3, var4, var5);
      this.a(var18, var16, var17, var14, var15);
      float var19 = var17 + (var15 - Msdf.a(MsdfManager.BOLD, 6.5F)) / 2.0F;
      float var20 = var16 + 4.5F;
      Msdf.b();
      Msdf.a(var18, MsdfManager.BOLD, var6, var20, var19, 6.5F, -1);
      var20 += var11 + 3.5F;
      Msdf.a(var18, MsdfManager.BOLD, "|", var20, var19, 6.5F, -1603374206);
      var20 += var13 + 3.5F;
      int var21 = a(var8, var7);
      Msdf.a(var18, MsdfManager.BOLD, var10, var20, var19, 6.5F, var21);
      Msdf.c();
   }

   private void a(Matrix4f var1, ItemEntity var2, float var3, float var4, float var5) {
      ItemStack var6 = var2.getStack();
      String var7 = h(var6);
      if (var7.isEmpty()) {
         String var8 = NbtDumpManager.a(var6);
         var7 = StreamerMode.a(var8 != null ? var8 : var6.getName().getString());
      }

      int var20 = var6.getCount();
      String var9 = var20 > 1 ? String.valueOf(var20) : "";
      float var10 = Msdf.a(MsdfManager.BOLD, var7, 6.5F);
      float var11 = var9.isEmpty() ? 0.0F : Msdf.a(MsdfManager.BOLD, var9, 6.5F) + 3.0F;
      float var12 = Msdf.a(MsdfManager.BOLD, "|", 6.5F);
      float var13 = 13.0F + var11 + 3.0F + var12 + 3.0F + var10 + 4.5F;
      float var14 = 11.5F;
      float var15 = var3 - var13 / 2.0F;
      float var16 = var4 - var14;
      Matrix4f var17 = this.a(var3, var4, var5);
      this.a(var17, var15, var16, var13, var14);
      float var18 = var16 + (var14 - Msdf.a(MsdfManager.BOLD, 6.5F)) / 2.0F;
      float var19 = var15 + 4.5F + 8.5F + 3.0F;
      Msdf.b();
      if (!var9.isEmpty()) {
         Msdf.a(var17, MsdfManager.BOLD, var9, var19, var18, 6.5F, -3617316);
         var19 += var11;
      }

      Msdf.a(var17, MsdfManager.BOLD, "|", var19, var18, 6.5F, -1603374206);
      var19 += var12 + 3.0F;
      Msdf.a(var17, MsdfManager.BOLD, var7, var19, var18, 6.5F, -1);
      Msdf.c();
   }

   private void a(Matrix4f var1, TntEntity var2, float var3, float var4, float var5, float var6) {
      String var7 = StreamerMode.a(var2.getDisplayName().getString());
      float var8 = Math.max(0.0F, (var2.getFuse() - var6) / 20.0F);
      String var9 = String.format(Locale.US, "%.1f", var8) + "с";
      float var10 = Msdf.a(MsdfManager.BOLD, var7, 6.5F);
      float var11 = Msdf.a(MsdfManager.BOLD, var9, 6.5F);
      float var12 = Msdf.a(MsdfManager.BOLD, "|", 6.5F);
      float var13 = 4.5F + var10 + 3.5F + var12 + 3.5F + var11 + 4.5F;
      float var14 = 11.5F;
      float var15 = var3 - var13 / 2.0F;
      float var16 = var4 - var14;
      Matrix4f var17 = this.a(var3, var4, var5);
      this.a(var17, var15, var16, var13, var14);
      float var18 = var16 + (var14 - Msdf.a(MsdfManager.BOLD, 6.5F)) / 2.0F;
      float var19 = var15 + 4.5F;
      int var20 = var8 < 1.0F ? -48060 : (var8 < 2.5F ? 43520 : -11141291);
      Msdf.b();
      Msdf.a(var17, MsdfManager.BOLD, var7, var19, var18, 6.5F, -1);
      var19 += var10 + 3.5F;
      Msdf.a(var17, MsdfManager.BOLD, "|", var19, var18, 6.5F, -1603374206);
      var19 += var12 + 3.5F;
      Msdf.a(var17, MsdfManager.BOLD, var9, var19, var18, 6.5F, var20);
      Msdf.c();
   }

   private Matrix4f a(float var1, float var2, float var3) {
      Matrix4f var4 = RenderUtil.d();
      var4.translate(var1, var2, 0.0F);
      var4.scale(var3, var3, 1.0F);
      var4.translate(-var1, -var2, 0.0F);
      return var4;
   }

   private void a(Matrix4f var1, float var2, float var3, float var4, float var5) {
      this.a(var1, var2, var3, var4, var5, 4.5F);
   }

   private void a(Matrix4f var1, float var2, float var3, float var4, float var5, float var6) {
      this.a(var1, var2, var3, var4, var5, var6, var6, var6, var6);
   }

   private void a(Matrix4f var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9) {
      if (Theme.getInstance().getGuiStyle() == Theme.a.BLUR) {
         RenderUtil.a(var1, var2, var3, var4, var5, var6, 5.0F);
      }

      RenderUtil.a(var1, var2, var3, var4, var5, var6, var7, var8, var9, false, -2113929216);
      RenderUtil.a(var1, var2, var3, var4, var5, var6, 0.75F, 436207615);
   }

   private void b(Matrix4f var1, float var2, float var3, float var4, float var5) {
      this.b(var1, var2, var3, var4, var5, 4.5F);
   }

   private void b(Matrix4f var1, float var2, float var3, float var4, float var5, float var6) {
      this.b(var1, var2, var3, var4, var5, var6, var6, var6, var6);
   }

   private void b(Matrix4f var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9) {
      if (Theme.getInstance().getGuiStyle() == Theme.a.BLUR) {
         RenderUtil.a(var1, var2, var3, var4, var5, var6, 5.0F);
      }

      RenderUtil.a(var1, var2, var3, var4, var5, var6, var7, var8, var9, false, -1358291952);
      RenderUtil.a(var1, var2, var3, var4, var5, var6, 1.1F, -432210046);
   }

   public NumberSetting getSize() {
      return this.size;
   }

   private float c(Entity var1) {
      return (float)this.size.getValue();
   }

   private static int a(float var0, float var1) {
      if (var1 <= 0.0F) {
         return -13710223;
      } else {
         float var2 = Math.clamp(var0 / var1, 0.0F, 1.0F);
         if (var2 >= 0.5F) {
            float var7 = (var2 - 0.5F) * 2.0F;
            int var8 = (int)(255.0F * (1.0F - var7) + 46.0F * var7);
            int var9 = (int)(165.0F * (1.0F - var7) + 204.0F * var7);
            int var6 = (int)(0.0F * (1.0F - var7) + 113.0F * var7);
            return 0xFF000000 | var8 << 16 | var9 << 8 | var6;
         } else {
            float var3 = var2 * 2.0F;
            int var4 = (int)(200.0F * (1.0F - var3) + 255.0F * var3);
            int var5 = (int)(30.0F * (1.0F - var3) + 165.0F * var3);
            return 0xFF000000 | var4 << 16 | var5 << 8;
         }
      }
   }

   private static int b(float var0) {
      if (var0 <= 5.0F) {
         return 0xFFFF5555;
      } else if (var0 <= 10.0F) {
         return 0xFFFFAA00;
      } else if (var0 <= 15.0F) {
         return 0xFFFFFF55;
      } else {
         return var0 <= 20.0F ? 0xFF55FF55 : 0xFF00AA00;
      }
   }

   private void b(Matrix4f var1, PlayerEntity var2, float var3, float var4, float var5) {
      RenderUtil.a(var1, var3, var4, var5, var5, 2.0F, 2.0F, 2.0F, 2.0F, false, 2013265920);
      Identifier var6 = SkinCache.a(mc, var2);
      if (var6 != null) {
         try {
            GpuTextureView var7 = mc.getTextureManager().getTexture(var6).getGlTextureView();
            TexturePipeline.b(var1, var3, var4, var5, var5, var7, -1, 2.0F, 0.0F, 0.125F, 0.125F, 0.25F, 0.25F);
         } catch (Exception var8) {
         }
      }
   }

   private float[] a(Vec3d var1) {
      return WorldToScreen.a(var1);
   }

   private static int a(Color var0, int var1) {
      int var2 = (int)Math.clamp(var1, 0.0F, 255.0F);
      return var2 << 24 | var0.getRed() << 16 | var0.getGreen() << 8 | var0.getBlue();
   }

   private void a(PlayerEntity var1, float var2, float var3, float var4) {
      ItemStack var5 = var1.getMainHandStack();
      ItemStack var6 = var1.getOffHandStack();
      if (!var5.isEmpty() || !var6.isEmpty()) {
         ArrayList var7 = new ArrayList();
         if (!var5.isEmpty()) {
            var7.add(a(var5, var1));
         }

         if (!var6.isEmpty()) {
            var7.add(a(var6, var1));
         }

         if (!var7.isEmpty()) {
            float var8 = 7.0F;
            float var9 = 3.0F;
            float var10 = 0.0F;

            for (String var12 : (Iterable<String>)(Iterable<?>)(var7)) {
               float var13 = var8 + var9 + Msdf.a(MsdfManager.BOLD, var12, 5.2F);
               if (var13 > var10) {
                  var10 = var13;
               }
            }

            float var28 = Math.max(7.0F, Msdf.a(MsdfManager.BOLD, 5.2F));
            float var29 = var7.size() * var28 + (var7.size() - 1) * 1.5F;
            float var30 = 7.0F + var10;
            float var14 = 3.6F + var29;
            float var15 = var2 - var30 / 2.0F;
            float var16 = var3 + 12.0F;
            Matrix4f var17 = this.a(var2, var16, var4);
            String var18 = StreamerMode.a(var1.getName().getString());
            boolean var19 = FriendManager.a(var18);
            if (var19) {
               this.b(var17, var15, var16, var30, var14);
            } else {
               this.a(var17, var15, var16, var30, var14);
            }

            Msdf.b();

            for (int var20 = 0; var20 < var7.size(); var20++) {
               String var21 = (String)var7.get(var20);
               float var22 = Msdf.a(MsdfManager.BOLD, var21, 5.2F);
               float var23 = var8 + var9 + var22;
               float var24 = var15 + (var30 - var23) / 2.0F;
               float var25 = var16 + 1.8F + var20 * (var28 + 1.5F);
               float var26 = var24 + var8 + var9;
               float var27 = var25 + (var28 - Msdf.a(MsdfManager.BOLD, 5.2F)) / 2.0F;
               Msdf.a(var17, MsdfManager.BOLD, var21, var26, var27, 5.2F, -1);
            }

            Msdf.c();
         }
      }
   }

   private void b(DrawContext var1, PlayerEntity var2, float var3, float var4, float var5) {
      ItemStack var6 = var2.getMainHandStack();
      ItemStack var7 = var2.getOffHandStack();
      if (!var6.isEmpty() || !var7.isEmpty()) {
         ArrayList var8 = new ArrayList();
         ArrayList var9 = new ArrayList();
         if (!var6.isEmpty()) {
            var8.add(var6);
            var9.add(a(var6, var2));
         }

         if (!var7.isEmpty()) {
            var8.add(var7);
            var9.add(a(var7, var2));
         }

         if (!var8.isEmpty()) {
            float var10 = 7.0F;
            float var11 = 3.0F;
            float var12 = 0.0F;

            for (String var14 : (Iterable<String>)(Iterable<?>)(var9)) {
               float var15 = Msdf.a(MsdfManager.BOLD, var14, 5.2F);
               if (var10 + var11 + var15 > var12) {
                  var12 = var10 + var11 + var15;
               }
            }

            float var34 = Math.max(7.0F, Msdf.a(MsdfManager.BOLD, 5.2F));
            float var35 = 7.0F + var12;
            float var36 = var3 - var35 / 2.0F;
            float var16 = var4 + 12.0F;
            Matrix3x2fStack var17 = var1.getMatrices();
            float var18 = 2.0F / mc.getWindow().getScaleFactor();
            float var19 = 0.4375F * var5 * var18;

            for (int var20 = 0; var20 < var8.size(); var20++) {
               ItemStack var21 = (ItemStack)var8.get(var20);
               String var22 = (String)var9.get(var20);
               float var23 = Msdf.a(MsdfManager.BOLD, var22, 5.2F);
               float var24 = var10 + var11 + var23;
               float var25 = var36 + (var35 - var24) / 2.0F;
               float var26 = var16 + 1.8F + var20 * (var34 + 1.5F);
               float var27 = var25;
               float var28 = var26 + (var34 - 7.0F) / 2.0F;
               float var29 = var3 + (var27 + 3.5F - var3) * var5;
               float var30 = var16 + (var28 + 3.5F - var16) * var5;
               float var31 = 7.0F * var5;
               if (!ClickGui.a(var29 - var31 / 2.0F, var30 - var31 / 2.0F, var31, var31)) {
                  var17.pushMatrix();
                  var17.translate(var29 * var18, var30 * var18);
                  var17.scale(var19, var19);
                  var17.translate(-8.0F, -8.0F);

                  try {
                     var1.drawItem(var21, 0, 0);
                  } catch (Exception var33) {
                  }

                  var17.popMatrix();
               }
            }
         }
      }
   }

   private void m() {
      if (mc.player != null && mc.world != null) {
         try {
            int var1 = mc.player.getInventory().size();

            for (int var2 = 0; var2 < var1; var2++) {
               ItemStack var3 = mc.player.getInventory().getStack(var2);
               if (var3 != null && !var3.isEmpty()) {
                  h(var3);
               }
            }

            if (mc.player.currentScreenHandler != null) {
               for (Slot var10 : mc.player.currentScreenHandler.slots) {
                  ItemStack var4 = var10.getStack();
                  if (var4 != null && !var4.isEmpty()) {
                     h(var4);
                  }
               }
            }

            for (Entity var11 : mc.world.getEntities()) {
               if (var11 instanceof ItemEntity var13) {
                  ItemStack var5 = var13.getStack();
                  if (var5 != null && !var5.isEmpty()) {
                     h(var5);
                  }
               }
            }

            for (PlayerEntity var12 : mc.world.getPlayers()) {
               if (var12 != mc.player) {
                  ItemStack var14 = var12.getMainHandStack();
                  if (var14 != null && !var14.isEmpty()) {
                     h(var14);
                  }

                  ItemStack var15 = var12.getOffHandStack();
                  if (var15 != null && !var15.isEmpty()) {
                     h(var15);
                  }
               }
            }
         } catch (Exception var6) {
         }
      }
   }

   private static synchronized void n() {
      if (!SPHERE_CACHE_LOADED) {
         SPHERE_CACHE_LOADED = true;

         try {
            if (Files.exists(SPHERE_CACHE_FILE)) {
               String var0 = Files.readString(SPHERE_CACHE_FILE, StandardCharsets.UTF_8);
               JsonObject var1 = JsonParser.parseString(var0).getAsJsonObject();

               for (String var3 : var1.keySet()) {
                  String var4 = var1.get(var3).getAsString();
                  if (var4 != null && !var4.toLowerCase().contains("талисман") && !var4.equalsIgnoreCase("Голова игрока")) {
                     SPHERE_SKIN_CACHE.put(var3, var4);
                  }
               }
            }
         } catch (Exception var5) {
         }
      }
   }

   private static synchronized void o() {
      try {
         JsonObject var0 = new JsonObject();

         for (Entry var2 : SPHERE_SKIN_CACHE.entrySet()) {
            var0.addProperty((String)var2.getKey(), (String)var2.getValue());
         }

         Files.writeString(SPHERE_CACHE_FILE, var0.toString(), StandardCharsets.UTF_8);
      } catch (Exception var3) {
      }
   }

   private static void a(String var0, String var1) {
      if (var0 != null
         && var1 != null
         && !var1.equalsIgnoreCase("Сфера")
         && !var1.equalsIgnoreCase("Голова игрока")
         && !var1.toLowerCase().contains("талисман")) {
         String var2 = SPHERE_SKIN_CACHE.put(var0, var1);
         if (!var1.equals(var2)) {
            o();
         }
      }
   }

   private static String d(String var0) {
      if (var0 != null && !var0.isBlank()) {
         String var1 = k(var0);
         var1 = COLOR_CODE_PATTERN.matcher(var1).replaceAll("");
         var1 = UNICODE_RANGE_PATTERN.matcher(var1).replaceAll("");
         Matcher var2 = TALISMAN_PATTERN.matcher(var1);
         if (var2.find()) {
            String var3 = var2.group(1).trim();
            String var6 = INVALID_CHARS_PATTERN.matcher(var3).replaceAll("").trim();
            if (!var6.isEmpty()) {
               return var6;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static String a(ItemStack var0) {
      if (var0 != null && var0.isOf(Items.PLAYER_HEAD)) {
         ProfileComponent var1 = var0.get(DataComponentTypes.PROFILE);
         if (var1 != null) {
            String var2 = var1.toString();
            Matcher var3 = TEXTURE_HASH_PATTERN.matcher(var2);
            if (var3.find()) {
               return var3.group(1);
            }

            Matcher var4 = B64_SKIN_PATTERN.matcher(var2);
            if (var4.find()) {
               try {
                  String var19 = new String(Base64.getDecoder().decode(var4.group(1)), StandardCharsets.UTF_8);
                  Matcher var21 = TEXTURE_HASH_PATTERN.matcher(var19);
                  if (var21.find()) {
                     return var21.group(1);
                  }

                  return var4.group(1);
               } catch (Exception var11) {
                  return var4.group(1);
               }
            }
         }

         String var15 = "";

         try {
            var15 = var0.getComponents().toString();
         } catch (Exception var14) {
         }

         Matcher var16 = TEXTURE_HASH_PATTERN.matcher(var15);
         if (var16.find()) {
            return var16.group(1);
         }

         Matcher var17 = B64_SKIN_PATTERN.matcher(var15);
         if (var17.find()) {
            try {
               String var18 = new String(Base64.getDecoder().decode(var17.group(1)), StandardCharsets.UTF_8);
               Matcher var20 = TEXTURE_HASH_PATTERN.matcher(var18);
               return var20.find() ? var20.group(1) : var17.group(1);
            } catch (Exception var12) {
               return var17.group(1);
            }
         } else {
            NbtCompound var5 = ItemUtility.b(var0);
            if (var5 != null) {
               String var6 = var5.toString();
               Matcher var7 = TEXTURE_HASH_PATTERN.matcher(var6);
               if (var7.find()) {
                  return var7.group(1);
               }

               Matcher var8 = B64_SKIN_PATTERN.matcher(var6);
               if (var8.find()) {
                  try {
                     String var9 = new String(Base64.getDecoder().decode(var8.group(1)), StandardCharsets.UTF_8);
                     Matcher var10 = TEXTURE_HASH_PATTERN.matcher(var9);
                     if (var10.find()) {
                        return var10.group(1);
                     }

                     return var8.group(1);
                  } catch (Exception var13) {
                     return var8.group(1);
                  }
               }
            }

            return null;
         }
      } else {
         return null;
      }
   }

   private static String a(List<String> var0) {
      if (var0 != null && !var0.isEmpty()) {
         boolean var1 = false;
         boolean var2 = false;
         boolean var3 = false;
         boolean var4 = false;

         for (String var6 : var0) {
            String var7 = var6.toLowerCase();
            if (var7.contains("макс. здоровье") || var7.contains("макс.здоровье") || var7.contains("здоровь") || var7.contains("health")) {
               var1 = true;
            }

            if (var7.contains("броня") || var7.contains("защит") || var7.contains("armor")) {
               var2 = true;
            }

            if (var7.contains("урон") || var7.contains("damage")) {
               var3 = true;
            }

            if (var7.contains("скорост") || var7.contains("speed")) {
               var4 = true;
            }
         }

         if (var1 && var2 && var3 && var4) {
            return "Талисман INFINITY";
         }

         if (var3 && var2 && var4 && !var1) {
            for (String var9 : var0) {
               if (var9.toLowerCase().contains("скорость i") && !var9.toLowerCase().contains("скорость ii")) {
                  return "Талисман STINGER";
               }
            }

            return "Талисман ETERNITY";
         } else if (var1 && var2) {
            return "Талисман Титана";
         } else if (var4 && var3) {
            return "Талисман STINGER";
         } else {
            return var4 && var2 ? "Талисман Сатиры" : null;
         }
      } else {
         return null;
      }
   }

   private static String b(int var0) {
      return switch (var0) {
         case 1 -> "I";
         case 2 -> "II";
         case 3 -> "III";
         case 4 -> "IV";
         default -> String.valueOf(var0);
      };
   }

   private static int e(String var0) {
      if (var0 == null) {
         return 0;
      } else {
         String var1 = var0.toLowerCase();
         if (LEVEL_IV_PATTERN.matcher(var1).matches()) {
            return 4;
         } else if (LEVEL_III_PATTERN.matcher(var1).matches()) {
            return 3;
         } else if (LEVEL_II_PATTERN.matcher(var1).matches()) {
            return 2;
         } else {
            return LEVEL_I_PATTERN.matcher(var1).matches() ? 1 : 1;
         }
      }
   }

   private static int b(String var0, String var1) {
      if (var0 == null) {
         return 0;
      }

      int var2 = var0.indexOf(var1);
      if (var2 == -1) {
         return 0;
      }

      int var3 = var0.indexOf("\"lvl\":", var2);
      if (var3 == -1) {
         var3 = var0.indexOf("lvl:", var2);
      }

      if (var3 != -1 && var3 - var2 < 60) {
         int var4 = var3 + (var0.charAt(var3) == '"' ? 6 : 4);

         while (var4 < var0.length() && !Character.isDigit(var0.charAt(var4))) {
            var4++;
         }

         int var5 = var4;

         while (var5 < var0.length() && Character.isDigit(var0.charAt(var5))) {
            var5++;
         }

         if (var5 > var4) {
            try {
               return Integer.parseInt(var0.substring(var4, var5));
            } catch (Exception var7) {
            }
         }
      }

      return 0;
   }

   private static int c(String var0, String var1) {
      if (var0 == null) {
         return 0;
      }

      int var2 = var0.indexOf(var1);
      if (var2 != -1) {
         int var3 = var0.indexOf("amount=", var2);
         if (var3 != -1) {
            int var4 = var3 + 7;
            int var5 = var4;

            while (var5 < var0.length() && (Character.isDigit(var0.charAt(var5)) || var0.charAt(var5) == '.')) {
               var5++;
            }

            if (var5 > var4) {
               try {
                  double var6 = Double.parseDouble(var0.substring(var4, var5));
                  if (var1.contains("speed")) {
                     if (var6 >= 0.25) {
                        return 3;
                     }

                     if (var6 >= 0.15) {
                        return 2;
                     }

                     if (var6 >= 0.05) {
                        return 1;
                     }
                  } else {
                     if (var6 >= 3.5) {
                        return 4;
                     }

                     if (var6 >= 2.5) {
                        return 3;
                     }

                     if (var6 >= 1.5) {
                        return 2;
                     }

                     if (var6 >= 0.5) {
                        return 1;
                     }
                  }
               } catch (Exception var8) {
               }
            }
         }
      }

      return 0;
   }

   private static String b(ItemStack var0) {
      if (var0 != null && !var0.isEmpty() && var0.isOf(Items.PLAYER_HEAD)) {
         String var1 = a(var0);
         Text var2 = var0.get(DataComponentTypes.CUSTOM_NAME);
         if (var2 == null) {
            var2 = var0.get(DataComponentTypes.ITEM_NAME);
         }

         String var3 = var2 != null ? var2.getString() : "";
         if (var3.isEmpty()) {
            String var4 = var0.getName().getString();
            String var5 = var0.getItem().getName().getString();
            if (!var4.equalsIgnoreCase(var5)) {
               var3 = var4;
            }
         }

         String var18 = l(var3);
         LoreComponent var19 = var0.get(DataComponentTypes.LORE);
         ArrayList var6 = new ArrayList();
         if (var19 != null) {
            for (Text var8 : var19.lines()) {
               String var9 = l(var8.getString());
               if (!var9.isEmpty()) {
                  var6.add(var9);
               }
            }
         }

         int var20 = 0;
         int var21 = 0;
         int var22 = 0;
         boolean var10 = false;
         ArrayList var11 = new ArrayList();
         if (ItemUtility.l(var0) || var18.toLowerCase().contains("цербер") || var18.toLowerCase().contains("cerber")) {
            var10 = true;
            var20 = 4;
         }

         for (String var13 : (Iterable<String>)(Iterable<?>)(var6)) {
            String var14 = var13.toLowerCase();
            if (var14.contains("цербер") || var14.contains("cerber")) {
               var10 = true;
               var20 = 4;
               if (!var11.contains("damage")) {
                  var11.add("damage");
               }
            } else if (var14.contains("урон") || var14.contains("damage") || var14.contains("сила") || var14.contains("силы") || var14.contains("strength")) {
               int var33 = e(var14);
               if (var33 >= 4) {
                  var10 = true;
                  var20 = 4;
               } else if (var33 > 0) {
                  var20 = var33;
               }

               if (!var11.contains("damage")) {
                  var11.add("damage");
               }
            } else if (var14.contains("броня")
               || var14.contains("брони")
               || var14.contains("защит")
               || var14.contains("armor")
               || var14.contains("protection")
               || var14.contains("сопротивлен")) {
               int var32 = e(var14);
               if (var32 > 0) {
                  var21 = var32;
               }

               if (!var11.contains("armor")) {
                  var11.add("armor");
               }
            } else if (var14.contains("скорост") || var14.contains("speed")) {
               int var15 = e(var14);
               if (var15 > 0) {
                  var22 = var15;
               }

               if (!var11.contains("speed")) {
                  var11.add("speed");
               }
            }
         }

         NbtCompound var23 = ItemUtility.b(var0);
         if (var23 != null) {
            String var24 = var23.toString().toLowerCase();
            if (!var10 && (var24.contains("cerber") || var24.contains("цербер"))) {
               var10 = true;
               var20 = 4;
               if (!var11.contains("damage")) {
                  var11.add("damage");
               }
            }

            if (var20 == 0) {
               int var27 = b(var24, "hms-damage");
               if (var27 == 0) {
                  var27 = b(var24, "damage");
               }

               if (var27 == 0) {
                  var27 = b(var24, "sdamage");
               }

               if (var27 >= 4) {
                  var10 = true;
                  var20 = 4;
               } else if (var27 > 0) {
                  var20 = var27;
               }

               if (var20 > 0 && !var11.contains("damage")) {
                  var11.add("damage");
               }
            }

            if (var21 == 0) {
               int var28 = b(var24, "hms-armor");
               if (var28 == 0) {
                  var28 = b(var24, "hms-protection");
               }

               if (var28 == 0) {
                  var28 = b(var24, "armor");
               }

               if (var28 == 0) {
                  var28 = b(var24, "sarmor");
               }

               if (var28 > 0) {
                  var21 = var28;
               }

               if (var21 > 0 && !var11.contains("armor")) {
                  var11.add("armor");
               }
            }

            if (var22 == 0) {
               int var29 = b(var24, "hms-speed");
               if (var29 == 0) {
                  var29 = b(var24, "speed");
               }

               if (var29 == 0) {
                  var29 = b(var24, "sspeed");
               }

               if (var29 > 0) {
                  var22 = var29;
               }

               if (var22 > 0 && !var11.contains("speed")) {
                  var11.add("speed");
               }
            }
         }

         if (var22 == 0) {
            int var25 = ItemUtility.m(var0);
            if (var25 > 0) {
               var22 = var25;
               if (!var11.contains("speed")) {
                  var11.add("speed");
               }
            }
         }

         AttributeModifiersComponent var26 = var0.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
         if (var26 != null) {
            String var30 = var26.toString().toLowerCase();
            if (var20 == 0 && (var30.contains("attack_damage") || var30.contains("damage"))) {
               int var34 = c(var30, "attack_damage");
               if (var34 >= 4) {
                  var10 = true;
                  var20 = 4;
               } else if (var34 > 0) {
                  var20 = var34;
               }

               if (var20 > 0 && !var11.contains("damage")) {
                  var11.add("damage");
               }
            }

            if (var21 == 0 && var30.contains("armor")) {
               int var35 = c(var30, "armor");
               if (var35 > 0) {
                  var21 = var35;
               }

               if (var21 > 0 && !var11.contains("armor")) {
                  var11.add("armor");
               }
            }

            if (var22 == 0 && (var30.contains("movement_speed") || var30.contains("speed"))) {
               int var36 = c(var30, "movement_speed");
               if (var36 > 0) {
                  var22 = var36;
               }

               if (var22 > 0 && !var11.contains("speed")) {
                  var11.add("speed");
               }
            }
         }

         if (var21 > 0 && !var11.contains("armor")) {
            var11.add("armor");
         }

         if (var20 > 0 && !var11.contains("damage")) {
            var11.add("damage");
         }

         if (var22 > 0 && !var11.contains("speed")) {
            var11.add("speed");
         }

         String var31 = null;
         if (!var11.isEmpty()) {
            if (var11.size() == 1 && var10) {
               var31 = "Сфера Цербера";
            } else {
               ArrayList var37 = new ArrayList();

               for (String var17 : (Iterable<String>)(Iterable<?>)(var11)) {
                  if ("armor".equals(var17) && var21 > 0) {
                     var37.add("Броня " + b(var21));
                  } else if ("damage".equals(var17) && var20 > 0) {
                     if (!var10 && var20 < 4) {
                        var37.add("Урон " + b(var20));
                     } else {
                        var37.add("Цербер");
                     }
                  } else if ("speed".equals(var17) && var22 > 0) {
                     var37.add("Скорость " + b(var22));
                  }
               }

               if (!var37.isEmpty()) {
                  var31 = "Сфера на " + String.join(" ", var37);
               }
            }
         }

         if (var31 != null) {
            if (var1 != null) {
               a(var1, var31);
            }

            return var31;
         } else {
            return var1 != null && SPHERE_SKIN_CACHE.containsKey(var1) ? SPHERE_SKIN_CACHE.get(var1) : "Сфера";
         }
      } else {
         return "Сфера";
      }
   }

   private static int c(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         CustomModelDataComponent var1 = var0.get(DataComponentTypes.CUSTOM_MODEL_DATA);
         if (var1 != null) {
            if (!var1.floats().isEmpty()) {
               return var1.floats().getFirst().intValue();
            }

            if (!var1.strings().isEmpty()) {
               String var2 = var1.strings().getFirst();

               try {
                  return Integer.parseInt(var2.replaceAll("[^0-9]", ""));
               } catch (Exception var4) {
               }
            }

            if (!var1.colors().isEmpty()) {
               return var1.colors().getFirst();
            }
         }

         NbtCompound var5 = ItemUtility.b(var0);
         if (var5 != null) {
            if (var5.contains("CustomModelData")) {
               return var5.getInt("CustomModelData").orElse(0);
            }

            if (var5.contains("custom_model_data")) {
               return var5.getInt("custom_model_data").orElse(0);
            }

            if (var5.contains("cmd")) {
               return var5.getInt("cmd").orElse(0);
            }
         }

         return 0;
      } else {
         return 0;
      }
   }

   private static int f(String var0) {
      if (var0 == null) {
         return 0;
      }

      Matcher var1 = DIGIT_PATTERN.matcher(var0);
      if (var1.find()) {
         try {
            return Integer.parseInt(var1.group());
         } catch (Exception var3) {
         }
      }

      return 0;
   }

   private static String c(int var0) {
      return switch (var0) {
         case 1 -> "Талисман Мрака";
         case 2 -> "Талисман Вихря";
         case 3 -> "Талисман Демона";
         case 4 -> "Талисман Раздора";
         case 5 -> "Талисман Ярости";
         case 6 -> "Талисман Крушителя";
         case 7 -> "Талисман Карателя";
         case 8 -> "Талисман Тирана";
         case 9 -> "Талисман Гринча";
         case 10 -> "Талисман Ареса";
         case 11 -> "Талисман Хаоса";
         case 12 -> "Талисман Титана";
         case 13 -> "Талисман Гидры";
         case 14 -> "Талисман Бестии";
         case 15 -> "Талисман Икара";
         case 16 -> "Талисман Сатиры";
         case 17 -> "Талисман Эриды";
         case 18 -> "Талисман Мороза";
         case 19 -> "Талисман Гармонии";
         case 20 -> "Талисман Дедала";
         case 21 -> "Талисман Ехидна";
         case 22 -> "Талисман Грани";
         case 23 -> "Талисман Тритона";
         case 24 -> "Талисман Феникс";
         case 25 -> "Талисман INFINITY";
         case 26 -> "Талисман ETERNITY";
         case 27 -> "Талисман STINGER";
         default -> null;
      };
   }

   private static String g(String var0) {
      if (var0 != null && !var0.isBlank()) {
         String var1 = var0.toLowerCase();
         if (var1.contains("карател") || var1.contains("karatel")) {
            return "Талисман Карателя";
         } else if (var1.contains("крушител") || var1.contains("krush")) {
            return "Талисман Крушителя";
         } else if (var1.contains("ярост") || var1.contains("yarost") || var1.contains("rage")) {
            return "Талисман Ярости";
         } else if (var1.contains("раздор") || var1.contains("razdor")) {
            return "Талисман Раздора";
         } else if (var1.contains("демон") || var1.contains("demon")) {
            return "Талисман Демона";
         } else if (var1.contains("вихр") || var1.contains("vihr") || var1.contains("vortex")) {
            return "Талисман Вихря";
         } else if (var1.contains("мрак") || var1.contains("mrak")) {
            return "Талисман Мрака";
         } else if (var1.contains("тиран") || var1.contains("tiran")) {
            return "Талисман Тирана";
         } else if (var1.contains("гринч") || var1.contains("grinch")) {
            return "Талисман Гринча";
         } else if (var1.contains("арес") || var1.contains("ares")) {
            return "Талисман Ареса";
         } else if (var1.contains("хаос") || var1.contains("haos") || var1.contains("chaos")) {
            return "Талисман Хаоса";
         } else if (var1.contains("титан") || var1.contains("titan")) {
            return "Талисман Титана";
         } else if (var1.contains("гидр") || var1.contains("gidr") || var1.contains("hydra")) {
            return "Талисман Гидры";
         } else if (var1.contains("бести") || var1.contains("besti")) {
            return "Талисман Бестии";
         } else if (var1.contains("икар") || var1.contains("ikar")) {
            return "Талисман Икара";
         } else if (var1.contains("сатир") || var1.contains("satir")) {
            return "Талисман Сатиры";
         } else if (var1.contains("эрид") || var1.contains("erid")) {
            return "Талисман Эриды";
         } else if (var1.contains("мороз") || var1.contains("moroz")) {
            return "Талисман Мороза";
         } else if (var1.contains("гармон") || var1.contains("garmon")) {
            return "Талисман Гармонии";
         } else if (var1.contains("дедал") || var1.contains("dedal")) {
            return "Талисман Дедала";
         } else if (var1.contains("ехидн") || var1.contains("ehidn")) {
            return "Талисман Ехидна";
         } else if (var1.contains("грани") || var1.contains("grani")) {
            return "Талисман Грани";
         } else if (var1.contains("тритон") || var1.contains("triton")) {
            return "Талисман Тритона";
         } else if (var1.contains("феникс") || var1.contains("fenix") || var1.contains("phoenix")) {
            return "Талисман Феникс";
         } else if (var1.contains("infinity")) {
            return "Талисман INFINITY";
         } else if (var1.contains("eternity")) {
            return "Талисман ETERNITY";
         } else if (var1.contains("stinger")) {
            return "Талисман STINGER";
         } else if (var1.contains("мифическ") || var1.contains("mythical")) {
            return "Мифический талисман";
         } else if (!var1.contains("легендарн") && !var1.contains("legendary")) {
            return !var1.contains("эпическ") && !var1.contains("epic") ? null : "Эпический талисман";
         } else {
            return "Легендарный талисман";
         }
      } else {
         return null;
      }
   }

   private static boolean d(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (var0.hasEnchantments()) {
         return true;
      }

      ItemEnchantmentsComponent var1 = var0.get(DataComponentTypes.ENCHANTMENTS);
      if (var1 != null && !var1.isEmpty()) {
         return true;
      }

      ItemEnchantmentsComponent var2 = var0.get(DataComponentTypes.STORED_ENCHANTMENTS);
      if (var2 != null && !var2.isEmpty()) {
         return true;
      }

      Boolean var3 = var0.get(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);
      if (Boolean.TRUE.equals(var3)) {
         return true;
      }

      if (var0.hasGlint()) {
         return true;
      }

      NbtCompound var4 = ItemUtility.b(var0);
      return var4 != null && (var4.contains("Enchantments") || var4.contains("ench") || var4.contains("StoredEnchantments") || var4.contains("enchantments"));
   }

   private static String e(ItemStack var0) {
      String var1 = ItemUtility.p(var0);
      return var1 != null && !var1.isEmpty() ? var1 : "Талисман";
   }

   private static boolean f(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      }

      if (var0.isIn(ItemTags.SWORDS)) {
         return true;
      }

      String var1 = Registries.ITEM.getId(var0.getItem()).getPath();
      return var1.endsWith("_sword") || var1.equals("sword");
   }

   private static int h(String var0) {
      if (var0 != null && !var0.isBlank()) {
         String var3 = var0.trim().toUpperCase();
         if (var3.matches("\\d+")) {
            try {
               return Integer.parseInt(var3);
            } catch (Exception var3x) {
            }
         }

         return i(var3);
      } else {
         return 0;
      }
   }

   private static int i(String var0) {
      short var1 = 0;
      short var2 = 0;

      for (int var3 = var0.length() - 1; var3 >= 0; var3--) {
         short var4 = switch (var0.charAt(var3)) {
            case 'C' -> 100;
            case 'D' -> 500;
            default -> 0;
            case 'I' -> 1;
            case 'L' -> 50;
            case 'M' -> 1000;
            case 'V' -> 5;
            case 'X' -> 10;
         };
         if (var4 == 0) {
            return 0;
         }

         if (var4 < var2) {
            var1 -= var4;
         } else {
            var1 += var4;
            var2 = var4;
         }
      }

      return var1;
   }

   private static int j(String var0) {
      if (var0 != null && !var0.isBlank()) {
         String var1 = l(var0);
         Matcher var2 = SHARPNESS_PATTERN.matcher(var1);
         return var2.find() ? h(var2.group(1)) : 0;
      } else {
         return 0;
      }
   }

   private static int g(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         int var1 = EnchantmentUtility.a(var0, Enchantments.SHARPNESS);
         if (var1 > 0) {
            return var1;
         }

         ItemEnchantmentsComponent var2 = var0.get(DataComponentTypes.ENCHANTMENTS);
         if (var2 != null) {
            for (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry var4 : var2.getEnchantmentEntries()) {
               if (((RegistryEntry<net.minecraft.enchantment.Enchantment>)var4.getKey()).matchesKey(Enchantments.SHARPNESS)) {
                  int var5 = var4.getIntValue();
                  if (var5 > 0) {
                     return var5;
                  }
               }
            }
         }

         ItemEnchantmentsComponent var8 = var0.get(DataComponentTypes.STORED_ENCHANTMENTS);
         if (var8 != null) {
            for (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry var11 : var8.getEnchantmentEntries()) {
               if (((RegistryEntry<net.minecraft.enchantment.Enchantment>)var11.getKey()).matchesKey(Enchantments.SHARPNESS)) {
                  int var6 = var11.getIntValue();
                  if (var6 > 0) {
                     return var6;
                  }
               }
            }
         }

         LoreComponent var10 = var0.get(DataComponentTypes.LORE);
         if (var10 != null) {
            for (Text var14 : var10.lines()) {
               int var7 = j(var14.getString());
               if (var7 > 0) {
                  return var7;
               }
            }
         }

         Text var13 = var0.get(DataComponentTypes.CUSTOM_NAME);
         if (var13 != null) {
            int var15 = j(var13.getString());
            if (var15 > 0) {
               return var15;
            }
         }

         NbtCompound var16 = ItemUtility.b(var0);
         if (var16 != null) {
            int var17 = b(var16.toString().toLowerCase(), "sharpness");
            if (var17 > 0) {
               return var17;
            }

            var17 = b(var16.toString().toLowerCase(), "острота");
            if (var17 > 0) {
               return var17;
            }
         }

         return 0;
      } else {
         return 0;
      }
   }

   private static String h(ItemStack var0) {
      return a(var0, null);
   }

   private static String a(ItemStack var0, PlayerEntity var1) {
      if (var0 != null && !var0.isEmpty()) {
         synchronized (ITEM_NAME_CACHE) {
            String var3 = ITEM_NAME_CACHE.get(var0);
            if (var3 != null) {
               return var3;
            }
         }

         String var8 = b(var0, var1);
         synchronized (ITEM_NAME_CACHE) {
            ITEM_NAME_CACHE.put(var0, var8);
            return var8;
         }
      } else {
         return "";
      }
   }

   private static String b(ItemStack var0, PlayerEntity var1) {
      String var2 = NbtDumpManager.a(var0);
      if (var2 != null && !var2.isEmpty()) {
         return StreamerMode.a(var2);
      }

      if (var0.isOf(Items.PLAYER_HEAD)) {
         return StreamerMode.a(b(var0));
      }

      if (var0.isOf(Items.TOTEM_OF_UNDYING)) {
         return StreamerMode.a(e(var0));
      }

      String var3 = null;
      DonateItem var4 = ItemUtility.e(var0);
      if (var4 != null) {
         String var5 = l(var4.getName());
         if (var5 != null && !var5.isEmpty()) {
            var3 = var5;
         }
      }

      if (var3 == null) {
         Text var9 = var0.get(DataComponentTypes.CUSTOM_NAME);
         if (var9 == null) {
            var9 = var0.get(DataComponentTypes.ITEM_NAME);
         }

         String var6 = var9 != null ? var9.getString() : "";
         if (var6.isEmpty()) {
            String var7 = var0.getName().getString();
            String var8 = var0.getItem().getName().getString();
            if (!var7.equalsIgnoreCase(var8)) {
               var6 = var7;
            }
         }

         String var12 = l(var6);
         if (!var12.isEmpty()) {
            String var13 = var0.getItem().getName().getString();
            if (!var12.equalsIgnoreCase(var13)) {
               var3 = var12;
            }
         }
      }

      if (var3 == null) {
         var3 = var0.getItem().getName().getString();
      }

      if (f(var0)) {
         int var10 = g(var0);
         if (var10 > 0) {
            String var11 = var3.toLowerCase();
            if (!var11.contains("остр") && !var11.contains("sharpness")) {
               var3 = var3 + " (Остр " + var10 + ")";
            }
         }
      }

      return StreamerMode.a(var3);
   }

   private static String k(String var0) {
      if (var0 == null) {
         return "";
      }

      StringBuilder var1 = new StringBuilder();

      for (int var2 = 0; var2 < var0.length(); var2++) {
         int var3 = var0.codePointAt(var2);
         if (Character.isSupplementaryCodePoint(var3)) {
            var2++;
            if (var3 >= 119808 && var3 <= 120831) {
               if (var3 >= 119808 && var3 <= 119833) {
                  var1.append((char)(65 + (var3 - 119808)));
               } else if (var3 >= 119834 && var3 <= 119859) {
                  var1.append((char)(97 + (var3 - 119834)));
               } else if (var3 >= 119860 && var3 <= 119885) {
                  var1.append((char)(65 + (var3 - 119860)));
               } else if (var3 >= 119886 && var3 <= 119911) {
                  var1.append((char)(97 + (var3 - 119886)));
               } else if (var3 >= 119912 && var3 <= 119937) {
                  var1.append((char)(65 + (var3 - 119912)));
               } else if (var3 >= 119938 && var3 <= 119963) {
                  var1.append((char)(97 + (var3 - 119938)));
               } else if (var3 >= 119964 && var3 <= 119989) {
                  var1.append((char)(65 + (var3 - 119964)));
               } else if (var3 >= 119990 && var3 <= 120015) {
                  var1.append((char)(97 + (var3 - 119990)));
               } else if (var3 >= 120016 && var3 <= 120041) {
                  var1.append((char)(65 + (var3 - 120016)));
               } else if (var3 >= 120042 && var3 <= 120067) {
                  var1.append((char)(97 + (var3 - 120042)));
               } else if (var3 >= 120068 && var3 <= 120093) {
                  var1.append((char)(65 + (var3 - 120068)));
               } else if (var3 >= 120094 && var3 <= 120119) {
                  var1.append((char)(97 + (var3 - 120094)));
               } else if (var3 >= 120120 && var3 <= 120145) {
                  var1.append((char)(65 + (var3 - 120120)));
               } else if (var3 >= 120146 && var3 <= 120171) {
                  var1.append((char)(97 + (var3 - 120146)));
               } else if (var3 >= 120172 && var3 <= 120197) {
                  var1.append((char)(65 + (var3 - 120172)));
               } else if (var3 >= 120198 && var3 <= 120223) {
                  var1.append((char)(97 + (var3 - 120198)));
               } else if (var3 >= 120224 && var3 <= 120249) {
                  var1.append((char)(65 + (var3 - 120224)));
               } else if (var3 >= 120250 && var3 <= 120275) {
                  var1.append((char)(97 + (var3 - 120250)));
               } else if (var3 >= 120276 && var3 <= 120301) {
                  var1.append((char)(65 + (var3 - 120276)));
               } else if (var3 >= 120302 && var3 <= 120327) {
                  var1.append((char)(97 + (var3 - 120302)));
               } else if (var3 >= 120328 && var3 <= 120353) {
                  var1.append((char)(65 + (var3 - 120328)));
               } else if (var3 >= 120354 && var3 <= 120379) {
                  var1.append((char)(97 + (var3 - 120354)));
               } else if (var3 >= 120380 && var3 <= 120405) {
                  var1.append((char)(65 + (var3 - 120380)));
               } else if (var3 >= 120406 && var3 <= 120431) {
                  var1.append((char)(97 + (var3 - 120406)));
               } else if (var3 >= 120432 && var3 <= 120457) {
                  var1.append((char)(65 + (var3 - 120432)));
               } else if (var3 >= 120458 && var3 <= 120483) {
                  var1.append((char)(97 + (var3 - 120458)));
               } else if (var3 >= 120782 && var3 <= 120831) {
                  var1.append((char)(48 + (var3 - 120782) % 10));
               }
            }
         } else {
            char var4 = (char)var3;
            switch (var4) {
               case '¦':
               case 'ı':
               case 'ǀ':
               case 'ɪ':
               case 'І':
               case 'Ї':
               case 'і':
               case 'ї':
               case 'ᶦ':
               case 'ℐ':
               case 'ℑ':
               case 'ℒ':
                  var1.append('I');
                  break;
               case 'Ø':
               case 'ø':
               case 'ᴏ':
               case 'ᵒ':
                  var1.append('O');
                  break;
               case 'Ŀ':
               case 'ŀ':
               case 'Ł':
               case 'ł':
               case 'ʟ':
               case 'ˡ':
                  var1.append('L');
                  break;
               case 'Ş':
               case 'ş':
               case 'ˢ':
               case 'Ѕ':
               case 'ѕ':
               case 'ꜱ':
                  var1.append('S');
                  break;
               case 'Ɓ':
               case 'Ƃ':
               case 'ʙ':
               case 'ᵇ':
                  var1.append('B');
                  break;
               case 'Ɖ':
               case 'Ɗ':
               case 'ԁ':
               case 'ᴅ':
               case 'ᵈ':
                  var1.append('D');
                  break;
               case 'Ǝ':
               case 'ǝ':
               case 'ᴇ':
               case 'ᵉ':
               case 'ℇ':
               case 'ℰ':
                  var1.append('E');
                  break;
               case 'Ƒ':
               case 'ƒ':
               case 'Ϝ':
               case 'ϝ':
               case 'Ғ':
               case 'ғ':
               case 'ᶠ':
               case 'ℱ':
               case 'Ⅎ':
               case 'ꜰ':
               case 'Ꞙ':
               case 'ꞙ':
               case 'ꟻ':
                  var1.append('F');
                  break;
               case 'Ɠ':
               case 'ɢ':
               case 'Ԍ':
               case 'ԍ':
               case 'ᵍ':
                  var1.append('G');
                  break;
               case 'ǫ':
               case 'ϙ':
               case 'ℚ':
                  var1.append('Q');
                  break;
               case 'ɐ':
               case 'ᴀ':
               case 'ᵃ':
               case 'Ɐ':
                  var1.append('A');
                  break;
               case 'ɴ':
               case 'ᵑ':
               case 'ℕ':
                  var1.append('N');
                  break;
               case 'ʀ':
               case 'ʁ':
               case 'ʳ':
               case 'ℛ':
               case 'ℜ':
               case 'ℝ':
                  var1.append('R');
                  break;
               case 'ʏ':
               case 'ʸ':
                  var1.append('Y');
                  break;
               case 'ʜ':
               case 'Һ':
               case 'һ':
               case 'ℋ':
               case 'ℌ':
               case 'ℍ':
                  var1.append('H');
                  break;
               case 'ʲ':
               case 'Ј':
               case 'ј':
               case 'ᴊ':
                  var1.append('J');
                  break;
               case 'ʷ':
               case 'ᴡ':
                  var1.append('W');
                  break;
               case 'ˣ':
                  var1.append('X');
                  break;
               case 'Қ':
               case 'қ':
               case 'ᴋ':
               case 'ᵏ':
                  var1.append('K');
                  break;
               case 'ᴄ':
               case 'ᶜ':
               case 'ℂ':
               case 'ℭ':
                  var1.append('C');
                  break;
               case 'ᴍ':
               case 'ᵐ':
               case 'ℳ':
                  var1.append('M');
                  break;
               case 'ᴘ':
               case 'ᵖ':
               case 'ℙ':
                  var1.append('P');
                  break;
               case 'ᴙ':
                  var1.append('Я');
                  break;
               case 'ᴛ':
               case 'ᵗ':
                  var1.append('T');
                  break;
               case 'ᴜ':
               case 'ᵘ':
                  var1.append('U');
                  break;
               case 'ᴠ':
               case 'ᵛ':
                  var1.append('V');
                  break;
               case 'ᴢ':
               case 'ℤ':
               case 'ℨ':
                  var1.append('Z');
                  break;
               case 'ᴦ':
                  var1.append('Г');
                  break;
               case 'ᴧ':
               case 'ᴫ':
                  var1.append('Л');
                  break;
               case 'ᴨ':
                  var1.append('П');
                  break;
               case 'ᴩ':
                  var1.append('Р');
                  break;
               default:
                  if (var4 >= 'Ａ' && var4 <= 'Ｚ') {
                     var1.append((char)(65 + (var4 - 'Ａ')));
                  } else if (var4 >= 'ａ' && var4 <= 'ｚ') {
                     var1.append((char)(97 + (var4 - 'ａ')));
                  } else if (var4 >= '０' && var4 <= '９') {
                     var1.append((char)(48 + (var4 - '０')));
                  } else {
                     var1.append(var4);
                  }
            }
         }
      }

      return var1.toString();
   }

   private static String l(String var0) {
      if (var0 == null) {
         return "";
      }

      String var1 = k(var0);
      var1 = COLOR_CODE_PATTERN.matcher(var1).replaceAll("");
      var1 = UNICODE_RANGE_PATTERN.matcher(var1).replaceAll("");
      var1 = INVALID_CHARS_PATTERN.matcher(var1).replaceAll(" ");
      var1 = TRIM_DECORATIONS_PATTERN.matcher(var1).replaceAll("");
      var1 = PREFIX_HEX_PATTERN.matcher(var1).replaceAll("");
      var1 = SUFFIX_HEX_PATTERN.matcher(var1).replaceAll("");
      return MULTI_SPACE_PATTERN.matcher(var1.trim()).replaceAll(" ");
   }

   static {
      n();
   }

   record a(NameTags.b info, long timestamp) {
      public NameTags.b getInfo() {
         return this.info;
      }

      public long getTimestamp() {
         return this.timestamp;
      }
   }

   record b(String text, int color) {
      public String getText() {
         return this.text;
      }

      public int getColor() {
         return this.color;
      }
   }
}
