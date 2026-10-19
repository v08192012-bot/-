package brokke.cc.core;

import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public enum AutoBuyEntry {
   POPPER_POTION(
      "[★] Хлопушка",
      Items.SPLASH_POTION,
      new PotionProcessor()
         .a(new PotionCondition(StatusEffects.SLOWNESS, 10, 600))
         .a(new PotionCondition(StatusEffects.SPEED, 5, 300))
         .a(new PotionCondition(StatusEffects.BLINDNESS, 10, 100))
         .a(new PotionCondition(StatusEffects.GLOWING, 1, 3600)),
      16711680
   ),
   HOLY_WATER(
      "[★] Святая вода",
      Items.SPLASH_POTION,
      new PotionProcessor()
         .a(new PotionCondition(StatusEffects.REGENERATION, 2, 900))
         .a(new PotionCondition(StatusEffects.INVISIBILITY, 2, 12000))
         .a(new PotionCondition(StatusEffects.INSTANT_HEALTH, 2, 0)),
      16777215
   ),
   RAGE_POTION(
      "[★] Зелье Гнева",
      Items.SPLASH_POTION,
      new PotionProcessor().a(new PotionCondition(StatusEffects.STRENGTH, 5, 600)).a(new PotionCondition(StatusEffects.SLOWNESS, 4, 600)),
      10040115
   ),
   PALLADIN_POTION(
      "[★] Зелье Палладина",
      Items.SPLASH_POTION,
      new PotionProcessor()
         .a(new PotionCondition(StatusEffects.RESISTANCE, 1, 12000))
         .a(new PotionCondition(StatusEffects.FIRE_RESISTANCE, 1, 12000))
         .a(new PotionCondition(StatusEffects.HEALTH_BOOST, 3, 1200))
         .a(new PotionCondition(StatusEffects.INVISIBILITY, 1, 18000)),
      65535
   ),
   ASSASSIN_POTION(
      "[★] Зелье Ассасина",
      Items.SPLASH_POTION,
      new PotionProcessor()
         .a(new PotionCondition(StatusEffects.STRENGTH, 4, 1200))
         .a(new PotionCondition(StatusEffects.SPEED, 3, 6000))
         .a(new PotionCondition(StatusEffects.HASTE, 1, 1200))
         .a(new PotionCondition(StatusEffects.INSTANT_DAMAGE, 2, 0)),
      3355443
   ),
   RADIATION_POTION(
      "[★] Зелье Радиации",
      Items.SPLASH_POTION,
      new PotionProcessor()
         .a(new PotionCondition(StatusEffects.POISON, 2, 300))
         .a(new PotionCondition(StatusEffects.WITHER, 2, 300))
         .a(new PotionCondition(StatusEffects.SLOWNESS, 3, 300))
         .a(new PotionCondition(StatusEffects.HUNGER, 5, 300))
         .a(new PotionCondition(StatusEffects.GLOWING, 1, 300)),
      3329330
   ),
   SLEEPING_PILL(
      "[★] Снотворное",
      Items.SPLASH_POTION,
      new PotionProcessor()
         .a(new PotionCondition(StatusEffects.WEAKNESS, 2, 1800))
         .a(new PotionCondition(StatusEffects.MINING_FATIGUE, 2, 600))
         .a(new PotionCondition(StatusEffects.WITHER, 3, 1800))
         .a(new PotionCondition(StatusEffects.BLINDNESS, 1, 600)),
      4737096
   ),
   TALISMAN_CRUSHER(
      "[★] Талисман Крушителя",
      Items.TOTEM_OF_UNDYING,
      new AttributeProcessor()
         .a(new AttributeCondition(EntityAttributes.MAX_HEALTH, 4.0, Operation.ADD_VALUE))
         .a(new AttributeCondition(EntityAttributes.ATTACK_DAMAGE, 3.0, Operation.ADD_VALUE))
         .a(new AttributeCondition(EntityAttributes.ARMOR_TOUGHNESS, 2.0, Operation.ADD_VALUE))
         .a(new AttributeCondition(EntityAttributes.ARMOR, 2.0, Operation.ADD_VALUE))
   ),
   TALISMAN_DISCORD(
      "[★] Талисман Раздора",
      Items.TOTEM_OF_UNDYING,
      new AttributeProcessor()
         .a(new AttributeCondition(EntityAttributes.ATTACK_DAMAGE, 4.0, Operation.ADD_VALUE))
         .a(new AttributeCondition(EntityAttributes.MAX_HEALTH, 2.0, Operation.ADD_VALUE))
         .a(new AttributeCondition(EntityAttributes.MOVEMENT_SPEED, 0.10000001817743467, Operation.ADD_MULTIPLIED_BASE))
         .a(new AttributeCondition(EntityAttributes.ATTACK_SPEED, 0.10000001817743467, Operation.ADD_MULTIPLIED_BASE))
         .a(new AttributeCondition(EntityAttributes.ARMOR, -3.0, Operation.ADD_VALUE))
   ),
   TALISMAN_TYRANT(
      "[★] Талисман Тирана",
      Items.TOTEM_OF_UNDYING,
      new AttributeProcessor()
         .a(new AttributeCondition(EntityAttributes.ATTACK_DAMAGE, 2.0, Operation.ADD_VALUE))
         .a(new AttributeCondition(EntityAttributes.ARMOR, 2.0, Operation.ADD_VALUE))
         .a(new AttributeCondition(EntityAttributes.MAX_HEALTH, -4.0, Operation.ADD_VALUE))
   ),
   TALISMAN_RAGE(
      "[★] Талисман Ярости",
      Items.TOTEM_OF_UNDYING,
      new AttributeProcessor()
         .a(new AttributeCondition(EntityAttributes.ATTACK_DAMAGE, 5.0, Operation.ADD_VALUE))
         .a(new AttributeCondition(EntityAttributes.MAX_HEALTH, -4.0, Operation.ADD_VALUE))
   ),
   TALISMAN_WHIRLWIND(
      "[★] Талисман Вихря",
      Items.TOTEM_OF_UNDYING,
      new AttributeProcessor()
         .a(new AttributeCondition(EntityAttributes.MAX_HEALTH, 2.0, Operation.ADD_VALUE))
         .a(new AttributeCondition(EntityAttributes.MOVEMENT_SPEED, 0.1500000347540806, Operation.ADD_MULTIPLIED_BASE))
         .a(new AttributeCondition(EntityAttributes.ATTACK_SPEED, 0.1500000347540806, Operation.ADD_MULTIPLIED_BASE))
   ),
   TALISMAN_GLOOM(
      "[★] Талисман Мрака",
      Items.TOTEM_OF_UNDYING,
      new AttributeProcessor()
         .a(new AttributeCondition(EntityAttributes.ARMOR, 1.5, Operation.ADD_VALUE))
         .a(new AttributeCondition(EntityAttributes.MAX_HEALTH, 1.5, Operation.ADD_VALUE))
   ),
   TALISMAN_DEMON(
      "[★] Талисман Демона",
      Items.TOTEM_OF_UNDYING,
      new AttributeProcessor()
         .a(new AttributeCondition(EntityAttributes.ATTACK_DAMAGE, 2.5, Operation.ADD_VALUE))
         .a(new AttributeCondition(EntityAttributes.ATTACK_SPEED, 0.10000001817743467, Operation.ADD_MULTIPLIED_BASE))
   ),
   TALISMAN_PUNISHER(
      "[★] Талисман Карателя xxx",
      Items.TOTEM_OF_UNDYING,
      new AttributeProcessor()
         .a(new AttributeCondition(EntityAttributes.ATTACK_DAMAGE, 7.0, Operation.ADD_VALUE))
         .a(new AttributeCondition(EntityAttributes.MAX_HEALTH, -4.0, Operation.ADD_VALUE))
         .a(new AttributeCondition(EntityAttributes.MOVEMENT_SPEED, 0.10000001817743467, Operation.ADD_MULTIPLIED_BASE))
   ),
   SPHERE_CHAOS(
      "[★] Сфера Хаоса",
      Items.PLAYER_HEAD,
      new AttributeProcessor()
         .a(new AttributeCondition(EntityAttributes.MAX_HEALTH, -4.0, Operation.ADD_VALUE))
         .a(new AttributeCondition(EntityAttributes.ARMOR, 1.5, Operation.ADD_VALUE))
         .a(new AttributeCondition(EntityAttributes.ATTACK_DAMAGE, 2.5, Operation.ADD_VALUE))
         .a(new AttributeCondition(EntityAttributes.MOVEMENT_SPEED, 0.07000004566537438, Operation.ADD_MULTIPLIED_BASE))
         .a(new AttributeCondition(EntityAttributes.ATTACK_SPEED, 0.13000000024761527, Operation.ADD_MULTIPLIED_BASE))
         .a(new AttributeCondition(EntityAttributes.GRAVITY, 0.09000001839414021, Operation.ADD_MULTIPLIED_BASE)),
      "ewogICJ0aW1lc3RhbXAiIDogMTc1MDI3ODY0MTkwMCwKICAicHJvZmlsZUlkIiA6ICIxNzRjZmRiNGEzY2I0M2I1YmZjZGU0MjRjM2JiMmM2ZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJtYXJhZWwxOCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9lN2E3YWU3Y2RjZjYxNmU4YjdhNDIyMWE2MjFiMjQzNTc1M2M2MGVkNmEyNThlYTA2MGRhZTMwMDJmZmU5ZTI4IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0="
   ),
   SPHERE_SATYR(
      "[★] Сфера Сатира",
      Items.PLAYER_HEAD,
      new AttributeProcessor()
         .a(new AttributeCondition(EntityAttributes.ATTACK_DAMAGE, 2.0, Operation.ADD_VALUE))
         .a(new AttributeCondition(EntityAttributes.JUMP_STRENGTH, -0.10000000768432185, Operation.ADD_MULTIPLIED_BASE))
         .a(new AttributeCondition(EntityAttributes.ATTACK_SPEED, 0.1500000347540806, Operation.ADD_MULTIPLIED_BASE)),
      "ewogICJ0aW1lc3RhbXAiIDogMTc1MDI3ODYwODUyOCwKICAicHJvZmlsZUlkIiA6ICJkMTQ4NjFiM2UwZmM0Njk5OTFlMTcyNTllMzdiZjZhZCIsCiAgInByb2ZpbGVOYW1lIiA6ICJyYXhpdG9jbCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83NzFhOWE0OThiNGZhNWVjNDkzNjJmOWJjODhlZGE0ZjUyYjA0ZGU0OWQ3NWFhM2NhMzMyYTFmZWExYWEwZTU3IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0="
   ),
   SPHERE_BEAST(
      "[★] Сфера Бестии",
      Items.PLAYER_HEAD,
      new AttributeProcessor()
         .a(new AttributeCondition(EntityAttributes.ARMOR, 1.0, Operation.ADD_VALUE))
         .a(new AttributeCondition(EntityAttributes.MAX_HEALTH, 4.0, Operation.ADD_VALUE))
         .a(new AttributeCondition(EntityAttributes.MOVEMENT_SPEED, 0.10000001817743467, Operation.ADD_MULTIPLIED_BASE))
         .a(new AttributeCondition(EntityAttributes.ATTACK_SPEED, 0.10000001817743467, Operation.ADD_MULTIPLIED_BASE)),
      "ewogICJ0aW1lc3RhbXAiIDogMTc1MDM0MzgzNDkzMCwKICAicHJvZmlsZUlkIiA6ICI1MzUzNWIxN2M0ZDY0NWQ0YWUwY2U2ZjM4Zjk0NTFjYSIsCiAgInByb2ZpbGVOYW1lIiA6ICJVYml2aXMiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTQxMWFjMTczODFiOWZjZTliYWIzYzcyYWZkYjdmMTk4NTcwZGFmNDczMmJkODExZDMxYzIyN2Q4MGZhMzliMSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9"
   ),
   SPHERE_ARES(
      "[★] Сфера Ареса",
      Items.PLAYER_HEAD,
      new AttributeProcessor()
         .a(new AttributeCondition(EntityAttributes.ATTACK_DAMAGE, 6.0, Operation.ADD_VALUE))
         .a(new AttributeCondition(EntityAttributes.ARMOR, -2.0, Operation.ADD_VALUE))
         .a(new AttributeCondition(EntityAttributes.MAX_HEALTH, -2.0, Operation.ADD_VALUE)),
      "ewogICJ0aW1lc3RhbXAiIDogMTc1MDM0Mzc3NDI1NSwKICAicHJvZmlsZUlkIiA6ICJhYWMxYjA2OWNkMjE0NWE2ODNlNzQxNzE4MDcxMGU4MiIsCiAgInByb2ZpbGVOYW1lIiA6ICJqdXNhbXUiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzE2YWRjNmJhZmNiNTdmZDcwN2RlZTdkZDZhNzM2ZmUxMjY3MTFkNTNhMWZkNmNlNzg5ZGE0MWIzYmUxM2YyYSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9"
   ),
   SPHERE_HYDRA(
      "[★] Сфера Гидры",
      Items.PLAYER_HEAD,
      new AttributeProcessor()
         .a(new AttributeCondition(EntityAttributes.MAX_HEALTH, 4.0, Operation.ADD_VALUE))
         .a(new AttributeCondition(EntityAttributes.ARMOR, 2.0, Operation.ADD_VALUE))
         .a(new AttributeCondition(EntityAttributes.SUBMERGED_MINING_SPEED, 0.5, Operation.ADD_MULTIPLIED_BASE))
         .a(new AttributeCondition(EntityAttributes.OXYGEN_BONUS, 0.5, Operation.ADD_MULTIPLIED_BASE)),
      "ewogICJ0aW1lc3RhbXAiIDogMTc1MDI3ODUzMjE4MywKICAicHJvZmlsZUlkIiA6ICI1OGZmZWI5NTMxNGQ0ODcwYTQwYjVjYjQyZDRlYTU5OCIsCiAgInByb2ZpbGVOYW1lIiA6ICJTa2luREJuZXQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2UzYzExOGQ2OTZkOTEwZTU0ZGUwMmNhNGQ4MDc1NDNmOWIxOGMwMDhjOTgzOGQyZmY2OTM3NzYyMmZiMWQzMiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9"
   ),
   SPHERE_ICARUS(
      "[★] Сфера Икара",
      Items.PLAYER_HEAD,
      new AttributeProcessor()
         .a(new AttributeCondition(EntityAttributes.ATTACK_DAMAGE, 2.0, Operation.ADD_VALUE))
         .a(new AttributeCondition(EntityAttributes.MAX_HEALTH, 2.0, Operation.ADD_VALUE)),
      "ewogICJ0aW1lc3RhbXAiIDogMTc1MDI3ODU4MjQ5MSwKICAicHJvZmlsZUlkIiA6ICJhZWNkODIxZTQyYzE0ZDJlOThmNTA1OTg1MWI5OWMzNyIsCiAgInByb2ZpbGVOYW1lIiA6ICJSb2RyaVgyMDc1IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2M2ODAzZTZkNTY2N2EyZDYxMDYyOGJjM2IzMmY4NjNjZGE0OTVjNDY1NjE2ZGU2NTVjYjMyOTkzM2I2MWFmNzciLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ=="
   ),
   SPHERE_ERIS(
      "[★] Сфера Эрида",
      Items.PLAYER_HEAD,
      new AttributeProcessor()
         .a(new AttributeCondition(EntityAttributes.LUCK, 1.0, Operation.ADD_VALUE))
         .a(new AttributeCondition(EntityAttributes.MAX_HEALTH, 2.0, Operation.ADD_VALUE))
         .a(new AttributeCondition(EntityAttributes.BLOCK_INTERACTION_RANGE, 1.0, Operation.ADD_VALUE)),
      "ewogICJ0aW1lc3RhbXAiIDogMTc1MDM0Mzg2MTE4NywKICAicHJvZmlsZUlkIiA6ICJlZGUyYzdhMGFjNjM0MTNiYjA5ZDNmMGJlZTllYzhlYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJ0aGVEZXZKYWRlIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzZlNGUyZjEwNDdmM2VjNmU5ZTQ1OTE4NDczOWUzM2I3YzFmYzYzYWQ4MjAyYmRhYjlmMDI0NTA4YWRkMjNlNWIiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ=="
   ),
   SPHERE_TITAN(
      "[★] Сфера Титана",
      Items.PLAYER_HEAD,
      new AttributeProcessor()
         .a(new AttributeCondition(EntityAttributes.ARMOR, 2.5, Operation.ADD_VALUE))
         .a(new AttributeCondition(EntityAttributes.ARMOR_TOUGHNESS, 2.5, Operation.ADD_VALUE))
         .a(new AttributeCondition(EntityAttributes.MOVEMENT_SPEED, -0.15000007945819008, Operation.ADD_MULTIPLIED_BASE)),
      "ewogICJ0aW1lc3RhbXAiIDogMTc1MDM1NDQ1NTE5MiwKICAicHJvZmlsZUlkIiA6ICJkOTcwYzEzZTM4YWI0NzlhOTY1OGM1ZDQ1MjZkMTM0YiIsCiAgInByb2ZpbGVOYW1lIiA6ICJDcmltcHlMYWNlODUxMjciLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODFlOTY5ODQ1OGI3ODQxYzk2YWU0ZjI0ZWM4NGFlMDE3MjQxMDA2NDFjNTY0ZTJhN2IxODVmNDA2ZThlZDIzIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0="
   ),
   TRAP("[★] Трапка", Items.NETHERITE_SCRAP, new DescriptionProcessor().a("Каст: Нерушимая клетка")),
   CLEAR_DUST("[★] Явная пыль", Items.SUGAR, new DescriptionProcessor().a("Каст: Световая вспышка")),
   SNOWBALL("[★] Снежок заморозка", Items.SNOWBALL, new DescriptionProcessor().a("Каст: Ледяная сфера")),
   GOD_AURA("[★] Божья аура", Items.PHANTOM_MEMBRANE, new DescriptionProcessor().a("Каст: Божественная аура")),
   DISORIENTATION("[★] Дезориентация", Items.ENDER_EYE, new DescriptionProcessor().a("Каст: Звуковая волна")),
   STRATUM("[★] Пласт", Items.DRIED_KELP, new DescriptionProcessor().a("Каст: Нерушимая стена")),
   GOLDEN_APPLE("Золотое яблоко", Items.GOLDEN_APPLE),
   ENCHANTED_GOLDEN_APPLE("Чарка", Items.ENCHANTED_GOLDEN_APPLE),
   TOTEM("Тотем бессмертия", Items.TOTEM_OF_UNDYING),
   SHIELD("Щит", Items.SHIELD),
   ENDER_PEARL("Эндер-жемчуг", Items.ENDER_PEARL),
   CHORUS_FRUIT("Хорус", Items.CHORUS_FRUIT),
   WIND_CHARGE("Заряд ветра", Items.WIND_CHARGE);

   private final String displayName;
   private final Object profile;
   private final Item item;
   private final AttributeProcessor attributeProcessor;
   private final EnchantmentProcessor enchantmentProcessor;
   private final DescriptionProcessor descriptionProcessor;
   private final PotionProcessor potionProcessor;

   AutoBuyEntry(String nullxx, Item nullxxx) {
      this(nullxx, nullxxx, null, null, null, null, null);
   }

   AutoBuyEntry(String nullxx, Item nullxxx, PotionProcessor nullxxxx, Object nullxxxxx) {
      this(nullxx, nullxxx, null, null, null, nullxxxx, nullxxxxx);
   }

   AutoBuyEntry(String nullxx, Item nullxxx, AttributeProcessor nullxxxx) {
      this(nullxx, nullxxx, nullxxxx, null, null, null, null);
   }

   AutoBuyEntry(String nullxx, Item nullxxx, AttributeProcessor nullxxxx, Object nullxxxxx) {
      this(nullxx, nullxxx, nullxxxx, null, null, null, nullxxxxx);
   }

   AutoBuyEntry(String nullxx, Item nullxxx, DescriptionProcessor nullxxxx) {
      this(nullxx, nullxxx, null, null, nullxxxx, null, null);
   }

   AutoBuyEntry(
      String nullxx,
      Item nullxxx,
      AttributeProcessor nullxxxx,
      EnchantmentProcessor nullxxxxx,
      DescriptionProcessor nullxxxxxx,
      PotionProcessor nullxxxxxxx,
      Object nullxxxxxxxx
   ) {
      this.displayName = nullxx;
      this.item = nullxxx;
      this.attributeProcessor = nullxxxx;
      this.enchantmentProcessor = nullxxxxx;
      this.descriptionProcessor = nullxxxxxx;
      this.potionProcessor = nullxxxxxxx;
      this.profile = nullxxxxxxxx;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public Item getItem() {
      return this.item;
   }

   public boolean a(ItemStack var1) {
      return var1 != null
         && !var1.isEmpty()
         && var1.isOf(this.item)
         && (this.attributeProcessor == null || this.attributeProcessor.a(var1))
         && (this.enchantmentProcessor == null || this.enchantmentProcessor.a(var1))
         && (this.descriptionProcessor == null || this.descriptionProcessor.a(var1))
         && (this.potionProcessor == null || this.potionProcessor.a(var1));
   }
}
