package brokke.cc.module.player;


import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.NewFunction;
import brokke.cc.animation.NotificationManager;
import brokke.cc.animation.NotificationType;
import brokke.cc.cosmetic.DonateItem;
import brokke.cc.manager.NbtDumpManager;
import brokke.cc.setting.MultiSetting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.EventPacket;
import brokke.cc.system.events.impl.EventPopTotem;
import brokke.cc.system.events.impl.PotionEvent;
import brokke.cc.system.events.impl.UpdateEvent;
import brokke.cc.util.ItemUtility;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@NewFunction(name = "Tracker", description = "Отслеживает выбранные использования и уведомляет о них", category = Category.PLAYER)
public class UseTracker extends Function {
   private final MultiSetting track = new MultiSetting("Отслеживать", "Тотемы", "Зелья", "Предметы");
   public static final List<UseTracker.b> trackers = new ArrayList<>();
   private final Set<Integer> usedEntityIds = new HashSet<>();
   private final Map<UUID, Integer> poppedTotems = new HashMap<>();
   private final Map<UUID, Long> lastPopTimes = new HashMap<>();

   public UseTracker() {
      this.track.setOption("Тотема", true);
      this.track.setOption("Зелья", true);
      this.track.setOption("Предмета", true);
      this.addSettings(this.track);
   }

   @Override
   public void onDisable() {
      this.usedEntityIds.clear();
      trackers.clear();
      this.poppedTotems.clear();
      this.lastPopTimes.clear();
      super.onDisable();
   }

   @EventHandler
   public void onUpdate(UpdateEvent var1) {
      if (mc.world != null && mc.player != null) {
         boolean var2 = this.track.isOptionEnabled("Предмета");
         boolean var3 = this.track.isOptionEnabled("Зелья");
         if (var2 || var3) {
            for (Entity var5 : mc.world.getEntities()) {
               if (var5 instanceof PlayerEntity var6 && var6 != mc.player) {
                  ItemStack var7 = var6.getActiveItem();
                  boolean var8 = var7.getItem() instanceof PotionItem;
                  int var9 = var7.get(DataComponentTypes.FOOD) == null && var7.getItem() != Items.MILK_BUCKET ? 0 : 1;
                  int var10 = var8 && var3 || var9 != 0 && var2 ? 1 : 0;
                  if (var6.isUsingItem() && var10 != 0) {
                     if (var6.getItemUseTimeLeft() <= 1) {
                        if (this.usedEntityIds.add(var6.getId())) {
                           String var11 = b(var7);
                           String var12 = var8 ? "§a" : "§c";
                           if (var7.isOf(Items.MILK_BUCKET)) {
                              trackers.removeIf(var1x -> var1x.getEntityId() == var6.getId());
                           }

                           this.c("§7[" + this.getName() + "] §f" + var6.getName().getString() + " использовал \"" + var12 + var11 + "§7\"");
                           NotificationManager.addNotification("Использование", var6.getName().getString() + " использовал " + var11, NotificationType.INFO, 1500L);
                        }
                     } else {
                        this.usedEntityIds.remove(var6.getId());
                     }
                  } else {
                     this.usedEntityIds.remove(var6.getId());
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onPotion(PotionEvent var1) {
      if (this.track.isOptionEnabled("Зелья") && var1.getType() == PotionEvent.Type.PARTICLES && mc.world != null) {
         for (UseTracker.a var5 : UseTracker.a.values()) {
            int[] var6 = var5.getThrowColors();
            int var7 = var6.length;

            for (int var8 = 0; var8 < var7; var8++) {
               int var9;
               if (((var9 = var6[var8]) & 16777215) == (var1.getData() & 16777215)) {
                  BlockPos var10 = var1.getPos();
                  Vec3d var11 = var10.toCenterPos();
                  Box var12 = new Box(var10.getX() - 4, var10.getY() - 4, var10.getZ() - 4, var10.getX() + 5, var10.getY() + 5, var10.getZ() + 5);

                  for (PlayerEntity var14 : mc.world.getEntitiesByClass(PlayerEntity.class, var12, LivingEntity::isAlive)) {
                     Box var15 = var14.getBoundingBox();
                     double var16;
                     if ((
                           var16 = 1.0
                              - Math.sqrt(
                                    Math.pow(var11.x - MathHelper.clamp(var11.x, var15.minX, var15.maxX), 2.0)
                                       + Math.pow(var11.y - MathHelper.clamp(var11.y, var15.minY, var15.maxY), 2.0)
                                       + Math.pow(var11.z - MathHelper.clamp(var11.z, var15.minZ, var15.maxZ), 2.0)
                                 )
                                 / 4.0
                        )
                        > 0.0) {
                        ArrayList var18 = new ArrayList();
                        int var19 = (int)(var16 * 100.0);
                        if (var14 != mc.player) {
                           this.a(
                              Text.literal("§7[" + this.getName() + "] §f" + var14.getName().getString() + " получил эффекты от \"")
                                 .append(var5.e())
                                 .append(Text.literal("§7\""))
                           );
                           this.c("§7[" + this.getName() + "] §7- Успешность: §a" + var19 + "%");
                        } else {
                           this.a(
                              Text.literal("§7[" + this.getName() + "] §fВы получили эффекты от \"")
                                 .append(var5.e())
                                 .append(Text.literal("§7\" (" + var19 + "%)"))
                           );
                        }

                        NotificationManager.addNotification(
                           "Зелье", var14.getName().getString() + " получил " + var5.getDisplayName() + " (" + var19 + "%)", NotificationType.INFO, 2000L
                        );

                        for (Entry var21 : var5.getEffects()) {
                           int var22 = Math.max(0, MathHelper.floor(((int[])var21.getValue())[0] * var16 + 0.5));
                           int var23 = ((int[])var21.getValue())[1];
                           if (var22 > 20) {
                              int var24 = var22 / 20;
                              if (var14 != mc.player) {
                                 this.c(
                                    "§7["
                                       + this.getName()
                                       + "] §7- §c"
                                       + ((StatusEffect)((RegistryEntry<?>)var21.getKey()).value()).getName().getString()
                                       + " "
                                       + b(var23)
                                       + " §7("
                                       + var24 / 60
                                       + ":"
                                       + String.format(Locale.ROOT, "%02d", var24 % 60)
                                       + ")"
                                 );
                              }

                              if (((RegistryEntry<?>)var21.getKey()).equals(StatusEffects.BLINDNESS)
                                 || ((RegistryEntry<?>)var21.getKey()).equals(StatusEffects.STRENGTH)
                                 || ((RegistryEntry<?>)var21.getKey()).equals(StatusEffects.SLOWNESS)
                                 || ((RegistryEntry<?>)var21.getKey()).equals(StatusEffects.WITHER)
                                 || ((RegistryEntry<?>)var21.getKey()).equals(StatusEffects.POISON)
                                 || ((RegistryEntry<?>)var21.getKey()).equals(StatusEffects.WEAKNESS)
                                 || ((RegistryEntry<?>)var21.getKey()).equals(StatusEffects.REGENERATION)
                                 || ((RegistryEntry<?>)var21.getKey()).equals(StatusEffects.HEALTH_BOOST)
                                 || ((RegistryEntry<?>)var21.getKey()).equals(StatusEffects.RESISTANCE)) {
                                 var18.add(new StatusEffectInstance((RegistryEntry<StatusEffect>)var21.getKey(), var22, var23));
                              }
                           }
                        }

                        if (!var18.isEmpty()) {
                           trackers.add(new UseTracker.b(List.copyOf(var18), var14.getId(), var14.age));
                        }
                     }
                  }

                  return;
               }
            }
         }
      }
   }

   @EventHandler
   public void onPacket(EventPacket var1) {
      if (mc.world != null) {
         if (var1.getPacket() instanceof EntityAttributesS2CPacket var2) {
            for (net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket.Entry var4 : var2.getEntries()) {
               if (var4.attribute().getKey().toString().contains("minecraft:movement_speed")) {
                  for (EntityAttributeModifier var6 : var4.modifiers()) {
                     if (mc.world.getEntityById(var2.getEntityId()) instanceof PlayerEntity
                        && var6.id().toString().equals("minecraft:effect.speed")
                        && var6.value() <= 0.40000001199465773
                        && var6.operation() == Operation.ADD_MULTIPLIED_TOTAL) {
                        trackers.removeIf(var1x -> var1x.getEntityId() == var2.getEntityId());
                     }
                  }
               }
            }
         }

         if (var1.getPacket() instanceof EntityStatusS2CPacket var7 && var7.getEntity(mc.world) instanceof PlayerEntity var11 && var7.getStatus() == 3) {
            this.poppedTotems.remove(var11.getUuid());
            this.lastPopTimes.remove(var11.getUuid());
         }
      }
   }

   @EventHandler
   public void onPopTotem(EventPopTotem var1) {
      PlayerEntity var2 = var1.getPlayer();
      long var3;
      if (var2 != null && mc.world != null && (var3 = System.currentTimeMillis()) - this.lastPopTimes.getOrDefault(var2.getUuid(), 0L) >= 100L) {
         this.lastPopTimes.put(var2.getUuid(), var3);
         trackers.removeIf(var1x -> var1x.getEntityId() == var2.getId());
         if (this.track.isOptionEnabled("Тотема") || this.track.isOptionEnabled("Тотем") || this.track.isOptionEnabled("Тотемы")) {
            ItemStack var5 = var2.getMainHandStack().getItem() == Items.TOTEM_OF_UNDYING
               ? var2.getMainHandStack()
               : (var2.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING ? var2.getOffHandStack() : null);
            if (var5 == null) {
               var5 = new ItemStack(Items.TOTEM_OF_UNDYING);
            }

            String var6 = a(var5);
            String var8 = var6.startsWith("Талисман") || var5.hasGlint() || var5.hasEnchantments() ? "§a●§7" : "§c●§7";
            if (var2 == mc.player) {
               int var9;
               String var10 = (var9 = this.poppedTotems.merge(mc.player.getUuid(), 1, Integer::sum)) > 1 ? "§6" + var9 + "x " : "";
               this.c("§7[" + this.getName() + "] §fВы использовали " + var10 + "тотем §7(\"" + var6 + "§7\", зачарован: " + var8 + ")");
               NotificationManager.addNotification("Тотем", "Вы использовали " + var10 + "тотем (" + var6 + ")", NotificationType.WARNING, 2000L);
            } else {
               int var11;
               String var12 = (var11 = this.poppedTotems.merge(var2.getUuid(), 1, Integer::sum)) > 1 ? "§6" + var11 + "x " : "";
               this.c(
                  "§7["
                     + this.getName()
                     + "] §fВы дали "
                     + var12
                     + "тотем §e"
                     + var2.getName().getString()
                     + " §7(\""
                     + var6
                     + "§7\", зачарован: "
                     + var8
                     + ")"
               );
               NotificationManager.addNotification("Тотем", "Вы дали " + var12 + "тотем " + var2.getName().getString() + " (" + var6 + ")", NotificationType.WARNING, 2000L);
            }
         }
      }
   }

   public static String a(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         String var1 = NbtDumpManager.a(var0);
         if (var1 != null && !var1.isEmpty()) {
            return var1;
         }

         String var2 = ItemUtility.p(var0);
         return var2 != null && !var2.isEmpty() ? var2 : ItemUtility.b(var0.getName().getString());
      } else {
         return "Тотем";
      }
   }

   public static String b(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         String var1 = NbtDumpManager.a(var0);
         if (var1 != null && !var1.isEmpty()) {
            return var1;
         }

         DonateItem var2 = ItemUtility.e(var0);
         if (var2 != null) {
            String var3 = b(var2.getName());
            if (var3 != null && !var3.isEmpty()) {
               return var3;
            }
         }

         Text var14 = var0.get(DataComponentTypes.CUSTOM_NAME);
         if (var14 == null) {
            var14 = var0.get(DataComponentTypes.ITEM_NAME);
         }

         String var4 = var14 != null ? b(var14.getString()) : "";
         if (!var4.isEmpty() && !a(var4)) {
            return var4;
         }

         String var5 = b(var0.getName().getString());
         if (!var5.isEmpty() && !a(var5)) {
            return var5;
         }

         if (var0.getItem() instanceof PotionItem) {
            LoreComponent var6 = var0.get(DataComponentTypes.LORE);
            if (var6 != null) {
               for (Text var8 : var6.lines()) {
                  String var9 = b(var8.getString());
                  if (var9.toLowerCase().contains("зелье")
                     || var9.toLowerCase().contains("вода")
                     || var9.toLowerCase().contains("снотворное")
                     || var9.toLowerCase().contains("хлопушка")) {
                     return var9;
                  }
               }
            }

            PotionContentsComponent var15 = var0.get(DataComponentTypes.POTION_CONTENTS);
            if (var15 != null) {
               ArrayList var16 = new ArrayList();
               var15.getEffects().forEach(var16::add);
               String var17 = a(var16);
               if (var17 != null) {
                  return var17;
               }

               ArrayList var10 = new ArrayList();

               for (StatusEffectInstance var12 : (Iterable<StatusEffectInstance>)(Iterable<?>)(var16)) {
                  String var13 = var12.getEffectType().value().getName().getString();
                  if (var12.getAmplifier() > 0) {
                     var13 = var13 + " " + b(var12.getAmplifier());
                  }

                  var10.add(var13);
               }

               if (!var10.isEmpty()) {
                  return "Зелье (" + String.join(", ", var10) + ")";
               }
            }
         }

         return !var5.isEmpty() ? var5 : var0.getItem().getName().getString();
      } else {
         return "";
      }
   }

   private static boolean a(String var0) {
      if (var0 != null && !var0.isBlank()) {
         String var1 = var0.toLowerCase().trim();
         return var1.equals("зелье")
            || var1.equals("potion")
            || var1.equals("несоздаваемое зелье")
            || var1.equals("uncraftable potion")
            || var1.equals("взрывное зелье")
            || var1.equals("splash potion")
            || var1.equals("оседающее зелье")
            || var1.equals("lingering potion");
      } else {
         return true;
      }
   }

   private static String a(List<StatusEffectInstance> var0) {
      if (var0 != null && !var0.isEmpty()) {
         for (UseTracker.a var4 : UseTracker.a.values()) {
            byte var5 = 1;

            for (Entry var7 : var4.getEffects()) {
               byte var8 = 0;

               for (StatusEffectInstance var10 : var0) {
                  if (var10.getEffectType().equals(var7.getKey())) {
                     var8 = 1;
                     break;
                  }
               }

               if (var8 == 0) {
                  var5 = 0;
                  break;
               }
            }

            if (var5 != 0) {
               return var4.getDisplayName();
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static String b(String var0) {
      if (var0 == null) {
         return "";
      }

      String var1 = var0.replaceAll("(?i)§[0-9a-fk-or]", "").replaceAll("(?i)&[0-9a-fk-or]", "");
      var1 = var1.replaceAll("[\\uE000-\\uF8FF]", "");
      var1 = var1.replaceAll("[\\?\\uFFFD]+", "");
      return var1.trim();
   }

   private static String b(int var0) {
      String[] var1 = new String[]{"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
      return var0 >= 0 && var0 < var1.length ? var1[var0] : String.valueOf(var0 + 1);
   }

   private void c(String var1) {
      if (mc.player != null) {
         mc.player.sendMessage(Text.literal(var1), false);
      }
   }

   private void a(MutableText var1) {
      if (mc.player != null) {
         mc.player.sendMessage(var1, false);
      }
   }

   public MultiSetting getTrack() {
      return this.track;
   }

   public Set<Integer> getUsedEntityIds() {
      return this.usedEntityIds;
   }

   public Map<UUID, Integer> getPoppedTotems() {
      return this.poppedTotems;
   }

   public Map<UUID, Long> getLastPopTimes() {
      return this.lastPopTimes;
   }

   public enum a {
      POPPER_POTION(
         List.of(
            Map.entry(StatusEffects.SLOWNESS, new int[]{200, 9}),
            Map.entry(StatusEffects.SPEED, new int[]{300, 4}),
            Map.entry(StatusEffects.BLINDNESS, new int[]{100, 9}),
            Map.entry(StatusEffects.GLOWING, new int[]{3600, 0})
         ),
         "[★] Хлопушка",
         new int[]{16738740},
         new int[]{16711765, 16727869, 16743972, 16760076, 14410269, 9628759, 4846994, 65484}
      ),
      HOLY_WATER(
         List.of(
            Map.entry(StatusEffects.REGENERATION, new int[]{900, 1}),
            Map.entry(StatusEffects.INVISIBILITY, new int[]{12000, 1}),
            Map.entry(StatusEffects.INSTANT_HEALTH, new int[]{0, 2})
         ),
         "[★] Святая вода",
         new int[]{16777215},
         new int[]{16777163, 16777148, 16777132, 16777117, 16777102, 16777087, 16776815, 16776800, 16776785, 16776769, 16776754}
      ),
      RAGE_POTION(
         List.of(Map.entry(StatusEffects.STRENGTH, new int[]{600, 4}), Map.entry(StatusEffects.SLOWNESS, new int[]{600, 3})),
         "[★] Зелье Гнева",
         new int[]{10040115},
         new int[]{9109504, 10620416, 12131328, 13707520, 15218432, 16729344, 16732928, 16736512, 16740352, 16743936, 16747520}
      ),
      PALLADIN_POTION(
         List.of(
            Map.entry(StatusEffects.RESISTANCE, new int[]{12000, 0}),
            Map.entry(StatusEffects.FIRE_RESISTANCE, new int[]{12000, 0}),
            Map.entry(StatusEffects.HEALTH_BOOST, new int[]{1200, 2}),
            Map.entry(StatusEffects.INVISIBILITY, new int[]{18000, 2})
         ),
         "[★] Зелье Палладина",
         new int[]{65535},
         new int[]{
            13762395,
            14090092,
            14417789,
            14745486,
            15007648,
            15335345,
            15663042,
            15990739,
            15663042,
            15335345,
            15007648,
            14745486,
            14417789,
            14090092,
            13762395
         }
      ),
      ASSASSIN_POTION(
         List.of(
            Map.entry(StatusEffects.STRENGTH, new int[]{1200, 3}),
            Map.entry(StatusEffects.SPEED, new int[]{6000, 2}),
            Map.entry(StatusEffects.HASTE, new int[]{1200, 0}),
            Map.entry(StatusEffects.INSTANT_DAMAGE, new int[]{0, 1})
         ),
         "[★] Зелье Ассасина",
         new int[]{3355443},
         new int[]{4277061, 4603456, 4929850, 5256245, 5516848, 5843242, 6169637, 6496032, 6822427, 7148821, 7409424, 7735819, 8062213, 8388608}
      ),
      RADIATION_POTION(
         List.of(
            Map.entry(StatusEffects.POISON, new int[]{1200, 1}),
            Map.entry(StatusEffects.WITHER, new int[]{1200, 1}),
            Map.entry(StatusEffects.SLOWNESS, new int[]{1800, 2}),
            Map.entry(StatusEffects.HUNGER, new int[]{1200, 4}),
            Map.entry(StatusEffects.GLOWING, new int[]{2400, 0})
         ),
         "[★] Зелье Радиации",
         new int[]{3329330},
         new int[]{16774970, 16250192, 15659878, 15135100, 14545043, 14020265, 13429951, 12905919, 12382378, 11858836, 11269759, 10746217, 10222676, 9699134}
      ),
      SLEEPING_PILL(
         List.of(
            Map.entry(StatusEffects.WEAKNESS, new int[]{1800, 1}),
            Map.entry(StatusEffects.MINING_FATIGUE, new int[]{200, 1}),
            Map.entry(StatusEffects.WITHER, new int[]{1800, 2}),
            Map.entry(StatusEffects.BLINDNESS, new int[]{200, 0})
         ),
         "[★] Снотворное",
         new int[]{255, 4737096},
         new int[]{4132250, 3219615, 2306725, 1394090, 481455, 812728, 2322884, 3833041, 5408733, 6918889}
      );

      private final List<Entry<RegistryEntry<StatusEffect>, int[]>> effects;
      private final String displayName;
      private final int[] throwColors;
      private final int[] nameColors;

      a(List<Entry<RegistryEntry<StatusEffect>, int[]>> nullxx, String nullxxx, int[] nullxxxx, int[] nullxxxxx) {
         this.effects = nullxx;
         this.displayName = nullxxx;
         this.throwColors = nullxxxx;
         this.nameColors = nullxxxxx;
      }

      public List<Entry<RegistryEntry<StatusEffect>, int[]>> getEffects() {
         return this.effects;
      }

      public String getDisplayName() {
         return this.displayName;
      }

      public int[] getThrowColors() {
         return this.throwColors;
      }

      public int[] getNameColors() {
         return this.nameColors;
      }

      public MutableText e() {
         int var1 = this.displayName.indexOf(32) + 1;
         MutableText var2 = Text.literal("");

         for (int var3 = 0; var3 < this.displayName.length(); var3++) {
            int var4 = var3 < var1 ? this.nameColors[0] : this.nameColors[Math.min(var3 - var1, this.nameColors.length - 1)];
            var2.append(Text.literal(String.valueOf(this.displayName.charAt(var3))).setStyle(Style.EMPTY.withColor(var4).withBold(true)));
         }

         return var2;
      }
   }

   public record b(List<StatusEffectInstance> effects, int entityId, int age) {
      public List<StatusEffectInstance> getEffects() {
         return this.effects;
      }

      public int getEntityId() {
         return this.entityId;
      }

      public int getAge() {
         return this.age;
      }
   }
}
