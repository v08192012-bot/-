package brokke.cc.hud;

import brokke.cc.animation.Animation;
import brokke.cc.animation.Easing;
import brokke.cc.config.ConfigManager;
import brokke.cc.config.HudConfig;
import brokke.cc.cosmetic.SkinCache;
import brokke.cc.core.ServerHealth;
import brokke.cc.font.Msdf;
import brokke.cc.font.MsdfManager;
import brokke.cc.module.combat.AimAssist;
import brokke.cc.module.combat.Aura;
import brokke.cc.module.combat.FixHp;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.player.StreamerMode;
import brokke.cc.module.combat.TriggerBot;
import brokke.cc.render.GlowPipeline;
import brokke.cc.render.RenderUtil;
import brokke.cc.render.TexturePipeline;
import brokke.cc.render.Theme;
import brokke.cc.system.events.EventBus;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.AttackEntityEvent;
import brokke.cc.system.events.impl.EventPopTotem;
import brokke.cc.system.events.impl.HudRenderEvent;
import brokke.cc.system.events.impl.Render2DEvent;
import brokke.cc.ui.ClickGui;
import brokke.cc.util.ItemUtility;
import brokke.cc.util.ServerUtility;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public class TargetHud {
   private static final TargetHud INSTANCE = new TargetHud();
   private static final String ID = "target";
   private static boolean enabled = false;
   static boolean showOnHover = false;
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private static final float CARD_H = 27.0F;
   private static final float HEAD_SIZE = 21.0F;
   private static final float PAD_X = 3.0F;
   private static final float ITEM_SIZE = 8.0F;
   private static final float ITEM_GAP = 1.5F;
   private final Animation alphaAnim = new Animation();
   private final Animation hpAnim = new Animation(1.0F);
   private final Animation hpTrailAnim = new Animation(1.0F);
   private final Animation absorptionAnim = new Animation(0.0F);
   private final Animation popAnim = new Animation(0.0F);
   private LivingEntity lastTarget;
   private float lastHpValue = -1.0F;
   private float x = -1.0F;
   private float y = -1.0F;
   private boolean dragging;
   private boolean wasPressed;
   private float dragDX;
   private float dragDY;
   private boolean initPos;
   private float cardW = 95.0F;
   private final List<TargetHud.a> particles = new ArrayList<>();
   private final List<TargetHud.a> particlePool = new ArrayList<>();
   private float prevCircleProgress = -1.0F;
   private long lastParticleTime = 0L;
   private static final Map<UUID, TargetHud.d> ARMOR_CACHE = new ConcurrentHashMap<>();
   private final List<TargetHud.e> targetItems = new ArrayList<>();
   private final List<TargetHud.e> targetItemPool = new ArrayList<>();
   private static String lastRawName;
   private static float lastMaxW;
   private static float lastFontSize;
   private static String cachedTrimmed;

   private TargetHud.a d() {
      return !this.particlePool.isEmpty() ? this.particlePool.remove(this.particlePool.size() - 1) : new TargetHud.a();
   }

   private void a(TargetHud.a var1) {
      if (this.particlePool.size() < 35) {
         this.particlePool.add(var1);
      }
   }

   private void e() {
      for (TargetHud.a var2 : this.particles) {
         this.a(var2);
      }

      this.particles.clear();
      this.prevCircleProgress = -1.0F;
      this.lastParticleTime = 0L;
   }

   private static int a(EquipmentSlot var0) {
      if (var0 == null) {
         return -1;
      }

      return switch (var0) {
         case FEET -> 0;
         case LEGS -> 1;
         case CHEST -> 2;
         case HEAD -> 3;
         default -> -1;
      };
   }

   public static boolean a(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         int var1 = var0.getDamage();
         if (var0.isOf(Items.NETHERITE_HELMET) && var1 == 203) {
            return true;
         }

         if (var0.isOf(Items.NETHERITE_CHESTPLATE) && var1 == 183) {
            return true;
         }

         if (var0.isOf(Items.NETHERITE_LEGGINGS) && var1 == 96) {
            return true;
         }

         if (var0.isOf(Items.NETHERITE_BOOTS) && var1 == 123) {
            return true;
         }

         if (!var0.isOf(Items.DIAMOND_HELMET) || var1 != 181 && var1 != 182) {
            if (var0.isOf(Items.DIAMOND_CHESTPLATE) && var1 == 264) {
               return true;
            } else {
               return !var0.isOf(Items.DIAMOND_LEGGINGS) || var1 != 247 && var1 != 248 ? var0.isOf(Items.DIAMOND_BOOTS) && (var1 == 214 || var1 == 215) : true;
            }
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   private static EquipmentSlot a(int var0) {
      return switch (var0) {
         case 0 -> EquipmentSlot.FEET;
         case 1 -> EquipmentSlot.LEGS;
         case 2 -> EquipmentSlot.CHEST;
         case 3 -> EquipmentSlot.HEAD;
         default -> null;
      };
   }

   private ItemUtility.a a(LivingEntity var1, EquipmentSlot var2, ItemStack var3) {
      if (var3 != null && !var3.isEmpty()) {
         int var4;
         if ((var4 = !a(var3) && !ServerUtility.f() ? 0 : 1) == 0 && ItemUtility.u(var3)) {
            ItemUtility.a var5 = ItemUtility.v(var3);
            if (var5 != null) {
               return var5;
            }
         }

         int var13;
         if ((var13 = a(var2)) != -1 && var1 != null) {
            UUID var14 = var1.getUuid();
            TargetHud.d var15 = ARMOR_CACHE.computeIfAbsent(var14, var0 -> new TargetHud.d());
            TargetHud.b var8 = var15.slots[var13];
            int var9;
            if ((var9 = ItemUtility.s(var3)) <= 0) {
               var9 = var3.getMaxDamage();
            }

            if (var9 <= 0) {
               var9 = ItemUtility.t(var3);
            }

            Item var10 = var3.getItem();
            if (var8.item != var10 || var8.max <= 0) {
               var8.item = var10;
               var8.max = var9;
               var8.cur = var9;
               var8.tracked = false;
            }

            if (var1.hurtTime > 0 && var1.hurtTime != var15.lastHurt) {
               var15.lastHurt = var1.hurtTime;
               long var11;
               if ((var11 = System.currentTimeMillis()) - var15.lastHurtTs > 300L) {
                  var15.lastHurtTs = var11;
                  a(var1, 2);
               }
            }

            if (var1 instanceof PlayerEntity && var1 != this.mc.player) {
               return new ItemUtility.a(Math.max(1, var8.cur), var8.max);
            }

            if (var4 != 0) {
               return new ItemUtility.a(Math.max(1, var8.cur), var8.max);
            }

            ItemUtility.a var16 = ItemUtility.v(var3);
            return var16 != null ? var16 : new ItemUtility.a(Math.max(1, var8.cur), var8.max);
         } else {
            ItemUtility.a var6 = ItemUtility.v(var3);
            if (var6 != null) {
               return var6;
            }

            int var7;
            return (var7 = ItemUtility.s(var3)) > 0 ? new ItemUtility.a(var7, var7) : null;
         }
      } else {
         return null;
      }
   }

   private static void a(LivingEntity var0, int var1) {
      if (var0 != null) {
         TargetHud.d var2 = ARMOR_CACHE.computeIfAbsent(var0.getUuid(), var0x -> new TargetHud.d());

         for (int var3 = 0; var3 < 4; var3++) {
            TargetHud.b var4 = var2.slots[var3];
            if (var4.item == null) {
               EquipmentSlot var5 = a(var3);
               if (var5 != null) {
                  ItemStack var6 = var0.getEquippedStack(var5);
                  if (!var6.isEmpty()) {
                     var4.item = var6.getItem();
                     var4.max = ItemUtility.s(var6);
                     if (var4.max <= 0) {
                        var4.max = var6.getMaxDamage();
                     }

                     if (var4.max <= 0) {
                        var4.max = ItemUtility.t(var6);
                     }

                     var4.cur = var4.max;
                  }
               }
            }

            if (var4.item != null) {
               var4.cur = Math.max(1, var4.cur - var1);
               var4.tracked = true;
            }
         }
      }
   }

   @EventHandler
   public void onAttackEntity(AttackEntityEvent var1) {
      if (var1.getTarget() instanceof LivingEntity var2 && this.mc.player != null) {
         ItemStack var10 = this.mc.player.getMainHandStack();
         int var4 = 2;
         if (!var10.isEmpty()) {
            if (var10.isIn(ItemTags.AXES)) {
               var4 = 4;
            } else if (var10.isIn(ItemTags.SWORDS)) {
               var4 = 3;
            } else if (var10.isOf(Items.MACE)) {
               var4 = 5;
            }
         }

         if (this.mc.player.fallDistance > 0.0 && !this.mc.player.isOnGround()) {
            var4++;
         }

         TargetHud.d var5 = ARMOR_CACHE.computeIfAbsent(var2.getUuid(), var0 -> new TargetHud.d());
         var5.lastHurtTs = System.currentTimeMillis();

         for (int var6 = 0; var6 < 4; var6++) {
            TargetHud.b var7 = var5.slots[var6];
            if (var7.item == null) {
               EquipmentSlot var8 = a(var6);
               if (var8 != null) {
                  ItemStack var9 = var2.getEquippedStack(var8);
                  if (!var9.isEmpty()) {
                     var7.item = var9.getItem();
                     var7.max = ItemUtility.s(var9);
                     if (var7.max <= 0) {
                        var7.max = var9.getMaxDamage();
                     }

                     if (var7.max <= 0) {
                        var7.max = ItemUtility.t(var9);
                     }

                     var7.cur = var7.max;
                  }
               }
            }

            if (var7.item != null) {
               int var11 = var4;
               if (var6 == 3 && var10.isIn(ItemTags.AXES)) {
                  var11 += 2;
               }

               var7.cur = Math.max(1, var7.cur - var11);
               var7.tracked = true;
            }
         }
      }
   }

   @EventHandler
   public void onPopTotem(EventPopTotem var1) {
      PlayerEntity var2 = var1.getPlayer();
      if (var2 != null) {
         a(var2, 12);
      }
   }

   private void a(ItemStack var1, float var2, EquipmentSlot var3) {
      int var4;
      if ((var4 = this.targetItems.size()) < this.targetItemPool.size()) {
         TargetHud.e var5 = this.targetItemPool.get(var4);
         var5.a(var1, var2, var3);
         this.targetItems.add(var5);
      } else {
         TargetHud.e var6 = new TargetHud.e(var1, var2, var3);
         this.targetItemPool.add(var6);
         this.targetItems.add(var6);
      }
   }

   public static float[] a() {
      if (INSTANCE.initPos && !(INSTANCE.x < 0.0F) && !(INSTANCE.y < 0.0F)) {
         return new float[]{INSTANCE.x, INSTANCE.y};
      }

      float var0 = RenderUtil.a();
      float var1 = RenderUtil.b();
      return new float[]{(var0 - 95.0F) * 0.5F, var1 * 0.5F + 35.0F};
   }

   public static void a(float var0, float var1) {
      if (var0 >= 0.0F && var1 >= 0.0F) {
         INSTANCE.x = var0;
         INSTANCE.y = var1;
         INSTANCE.initPos = true;
      }
   }

   public static boolean isEnabled() {
      return enabled;
   }

   public static void setEnabled(boolean var0) {
      enabled = var0;
   }

   public static void a(JsonObject var0) {
      var0.addProperty("enabled", enabled);
      var0.addProperty("showOnHover", showOnHover);
   }

   public static void b(JsonObject var0) {
      if (var0.has("enabled")) {
         enabled = var0.get("enabled").getAsBoolean();
      }

      if (var0.has("showOnHover")) {
         showOnHover = var0.get("showOnHover").getAsBoolean();
      }
   }

   public static void c() {
      EventBus.register(INSTANCE);
      ArrayList var0 = new ArrayList();
      var0.add(new HudConfig.b() {
         @Override
         public String a() {
            return "Показывать при наводке";
         }

         @Override
         public boolean b() {
            return TargetHud.showOnHover;
         }

         @Override
         public void a(boolean var1) {
            TargetHud.showOnHover = var1;
            ConfigManager.onSettingChanged();
         }
      });
      HudConfig.a("target", var0);
   }

   private LivingEntity a(MinecraftClient var1) {
      if (var1.currentScreen instanceof ChatScreen) {
         return var1.player;
      } else {
         Aura var2 = FunctionManager.a(Aura.class);
         if (var2 != null && var2.isActive() && var2.getTarget() != null && var2.getTarget().isAlive()) {
            return var2.getTarget();
         } else {
            TriggerBot var3 = FunctionManager.a(TriggerBot.class);
            if (var3 != null && var3.isActive() && var3.k() != null && var3.k().isAlive()) {
               return var3.k();
            } else {
               AimAssist var4 = FunctionManager.a(AimAssist.class);
               if (var4 != null && var4.isActive() && var4.k() != null && var4.k().isAlive()) {
                  return var4.k();
               } else {
                  return showOnHover && var1.targetedEntity instanceof LivingEntity var5 && var5.isAlive() && var5 != var1.player ? var5 : null;
               }
            }
         }
      }
   }

   private void a(MinecraftClient var1, float var2, float var3) {
      if (!(var1.currentScreen instanceof ChatScreen)) {
         this.dragging = false;
         this.wasPressed = false;
      } else {
         double var4 = var1.getWindow().getScaleFactor();
         float var6 = (float)(var1.mouse.getX() / var4) * RenderUtil.e();
         float var7 = (float)(var1.mouse.getY() / var4) * RenderUtil.e();
         int var8;
         if ((var8 = GLFW.glfwGetMouseButton(var1.getWindow().getHandle(), 0) == 1 ? 1 : 0) != 0
            && !this.wasPressed
            && !HudConfig.b(var6, var7)
            && var6 >= this.x - 2.0F
            && var6 <= this.x + var2 + 2.0F
            && var7 >= this.y - 2.0F
            && var7 <= this.y + var3 + 2.0F) {
            this.dragging = true;
            this.dragDX = var6 - this.x;
            this.dragDY = var7 - this.y;
         }

         if (var8 == 0) {
            if (this.dragging) {
               HudConfig.b();
            }

            this.dragging = false;
         }

         this.wasPressed = var8 != 0;
         if (this.dragging) {
            float[] var9 = HudConfig.b("target", var6 - this.dragDX, var7 - this.dragDY, var2, var3);
            this.x = var9[0];
            this.y = var9[1];
         }
      }
   }

   @EventHandler
   public void onRender2D(Render2DEvent var1) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (enabled && this.mc.player != null && !this.mc.options.hudHidden) {
         this.alphaAnim.setEasing(Easing.SPRING_SOFT);
         this.hpAnim.setEasing(Easing.SPRING_SOFT);
         this.hpTrailAnim.setEasing(Easing.GENTLE);
         this.absorptionAnim.setEasing(Easing.SPRING_SOFT);
         this.popAnim.setEasing(Easing.SPRING_SOFT);
         LivingEntity var3 = this.a(var2);
         this.alphaAnim.animate(var3 != null && var3.isAlive() ? 1.0F : 0.0F, 0.0F, 5.5F);
         float var4;
         if ((var4 = Math.clamp(this.alphaAnim.getValue(), 0.0F, 1.0F)) <= 0.02F) {
            this.lastTarget = null;
            this.targetItems.clear();
            this.lastHpValue = -1.0F;
            this.e();
         } else {
            LivingEntity var5 = var3 != null && var3.isAlive() ? var3 : this.lastTarget;
            if (var5 == null) {
               this.targetItems.clear();
               this.e();
            } else {
               if (this.lastTarget != var5) {
                  this.lastHpValue = -1.0F;
                  this.e();
               }

               this.lastTarget = var5;
               int var6 = Math.round(255.0F * var4);
                float var7 = Math.max(1.0F, var5.getMaxHealth());
                float var8 = Math.max(0.0F, var5.getHealth());
                boolean varUnknown = false;
                if (var5 instanceof PlayerEntity var300) {
                   float var301 = FixHp.scoreHealth(var300);
                   if (var301 < 0.0F) {
                      var301 = ServerHealth.a(var300);
                   }
                   if (var301 >= 0.0F) {
                      var8 = var301;
                   }
                   varUnknown = FixHp.isUnknown(var300);
                }
                float var9 = Math.max(0.0F, var5.getAbsorptionAmount());
                float var10 = Math.clamp((var8 + var9) / var7, 0.0F, 1.0F);
                float var11 = Math.clamp(var9 / var7, 0.0F, 1.0F);
                if (varUnknown) {
                   var8 = 10.0F;
                   var9 = 0.0F;
                   var10 = 0.0F;
                   var11 = 0.0F;
                   this.hpAnim.reset(0.0F);
                   this.hpTrailAnim.reset(0.0F);
                   this.absorptionAnim.reset(0.0F);
                   this.prevCircleProgress = 0.0F;
                }
               if (this.lastHpValue < 0.0F) {
                  this.hpAnim.reset(var10);
                  this.hpTrailAnim.reset(var10);
                  this.absorptionAnim.reset(var11);
                  this.lastHpValue = var8 + var9;
                  this.prevCircleProgress = var10;
                  this.e();
               }

               this.hpAnim.animate(var10, 0.0F, 12.0F);
               this.hpTrailAnim.animate(var10, 0.0F, 4.0F);
               this.absorptionAnim.animate(var11, 0.0F, 12.0F);
               String var12 = StreamerMode.a(var5.getName().getString());
                 String var14 = varUnknown ? "?" : String.valueOf((int)var8);
               float var15 = 21.0F;
               float var16 = 2.0F;
               float var17 = 3.0F;
               float var18 = 4.0F;
               float var19 = this.x + 3.0F + 21.0F + var18;
               if (var3 != null && var3.isAlive()) {
                  this.targetItems.clear();
                  float var20 = var19;
                  ItemStack var21 = var5.getMainHandStack();
                  if (!var21.isEmpty()) {
                     this.a(var21, var20, null);
                     var20 += 9.5F;
                  }

                  ItemStack var22 = var5.getOffHandStack();
                  if (!var22.isEmpty()) {
                     this.a(var22, var20, null);
                     var20 += 9.5F;
                  }

                  for (EquipmentSlot var26 : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                     ItemStack var27 = var5.getEquippedStack(var26);
                     if (!var27.isEmpty()) {
                        this.a(var27, var20, var26);
                        var20 += 9.5F;
                     }
                  }
               }

               float var71 = Msdf.a(MsdfManager.BOLD, var12, 6.5F);
               float var72 = Math.max(
                  this.targetItems.isEmpty() ? 0.0F : this.targetItems.size() * 8.0F + Math.max(0, this.targetItems.size() - 1) * 1.5F, var71
               );
               this.cardW = Math.max(80.0F, 24.0F + var18 + var72 + 5.0F + var15 + var17);
               float var73 = HudConfig.a("target");
               float var74 = this.cardW * var73;
               float var75 = 27.0F * var73;
               this.popAnim.animate(var4, 0.0F, 5.5F);
               float var76 = Math.clamp(this.popAnim.getValue(), 0.0F, 1.0F);
               float var77 = (1.0F - var4) * 4.0F;
               float var28 = (0.94F + 0.06F * var76) * var73;
               if (!this.initPos || this.x < 0.0F || this.y < 0.0F) {
                  float var29 = RenderUtil.a();
                  float var30 = RenderUtil.b();
                  this.x = (var29 - var74) * 0.5F;
                  this.y = var30 * 0.5F + 35.0F;
                  this.initPos = true;
               }

               this.a(var2, var74, var75);
               HudConfig.c("target", this.x, this.y, var74, var75);
               float var78 = this.y + var77;
               float var79 = this.x + this.cardW - var17 - var15;
               float var31 = var78 + (27.0F - var15) / 2.0F;
               float var32 = Math.max(10.0F, var79 - var19 - 4.0F);
               String var33 = a(var12, var32, 6.5F);
               Matrix4f var34 = RenderUtil.d();
               var34.translate(this.x, var78, 0.0F);
               var34.scale(var28, var28, 1.0F);
               var34.translate(-this.x, -var78, 0.0F);
               HudConfig.a(var34, "target", this.x, var78, this.cardW, 27.0F, 5.0F, var6 << 24);
               float var35 = this.x + 3.0F;
               float var36 = var78 + 3.0F;
               int var10000 = Math.round(140.0F * var4) << 24 | 1966080 | 7680 | 40;
               var10000 = Math.round(25.0F * var4) << 24 | 16777215;
               float var39 = var5.hurtTime > 0 ? Math.clamp(var5.hurtTime / 10.0F, 0.0F, 1.0F) : 0.0F;
               if (var5 instanceof PlayerEntity var40) {
                  this.a(var34, var2, var40, var35, var36, 21.0F, var6, var39);
               } else {
                  this.a(var34, var2, var5, var35, var36, 21.0F, var6, var39);
               }

               float var80 = this.targetItems.isEmpty() ? var78 + 10.0F : var78 + 4.5F;
               Msdf.a(var34, MsdfManager.BOLD, var33, var19, var80, 6.5F, a(Color.WHITE, var6));
               float var41 = Math.clamp(this.hpAnim.getValue(), 0.0F, 1.0F);
               float var42 = Math.clamp(this.absorptionAnim.getValue(), 0.0F, 1.0F);
               Color var43 = Theme.getInstance().getAccent();
               Color var44 = new Color(55, 58, 72, 120);
               RenderUtil.a(var34, var79, var31, var15, var16, var41, var43, var44, var4);
               if (var42 > 0.001F) {
                  RenderUtil.a(var34, var79, var31, var15, var16, var42, new Color(255, 205, 35), new Color(0, 0, 0, 0), var4);
               }

               long var45 = System.nanoTime();
               float var47 = this.lastParticleTime > 0L ? (float)((var45 - this.lastParticleTime) / 1.0E9) : 0.016F;
               this.lastParticleTime = var45;
               if (var47 > 0.05F) {
                  var47 = 0.05F;
               }

               if (var47 < 0.001F) {
                  var47 = 0.001F;
               }

               float var48 = var47 * 60.0F;
               float var49 = var79 + var15 * 0.5F;
               float var50 = var31 + var15 * 0.5F;
               float var51 = (var15 - var16) * 0.5F;
               if (this.prevCircleProgress < 0.0F) {
                  this.prevCircleProgress = var41;
               } else if (!(this.prevCircleProgress > var41)) {
                  this.prevCircleProgress = var41;
               } else {
                  float var52;
                  if ((var52 = this.prevCircleProgress - var41) > 3.0E-4F) {
                     int var53 = Math.clamp((int)(var52 * 140.0F) + 1, 1, 3);
                     Color var54 = var42 > 0.001F && var41 <= var42 ? new Color(255, 215, 60) : var43;

                     for (int var55 = 0; var55 < var53 && this.particles.size() < 28; var55++) {
                        float var56 = var41 + (float)Math.random() * Math.min(var52, 0.025F);
                        float var57;
                        float var58 = (float)Math.cos(var57 = (float) (-Math.PI / 2) + var56 * (float) (Math.PI * 2));
                        float var59 = (float)Math.sin(var57);
                        float var60 = var51 + ((float)Math.random() - 0.5F) * 0.6F;
                        float var61 = 0.22F + (float)Math.random() * 0.22F;
                        float var62 = 0.2F + (float)Math.random() * 0.2F;
                        float var63 = ((float)Math.random() - 0.5F) * 0.12F;
                        float var64 = ((float)Math.random() - 0.5F) * 0.12F;
                        TargetHud.a var65 = this.d();
                        var65.relX = var58 * var60;
                        var65.relY = var59 * var60;
                        var65.vx = -var59 * var61 + var58 * var62 + var63;
                        var65.vy = var58 * var61 + var59 * var62 + var64;
                        var65.life = 0.0F;
                        var65.maxLife = 0.22F + (float)Math.random() * 0.14F;
                        var65.coreSize = 0.7F + (float)Math.random() * 0.4F;
                        var65.glowSize = 1.8F + (float)Math.random() * 0.8F;
                        var65.color = var54;
                        this.particles.add(var65);
                     }
                  }

                  this.prevCircleProgress = var41;
               }

               for (int var81 = this.particles.size() - 1; var81 >= 0; var81--) {
                  TargetHud.a var85 = this.particles.get(var81);
                  var85.life += var47;
                  if (var85.life >= var85.maxLife) {
                     this.particles.remove(var81);
                     this.a(var85);
                  } else {
                     var85.relX = var85.relX + var85.vx * var48;
                     var85.relY = var85.relY + var85.vy * var48;
                     var85.vx = var85.vx * (float)Math.pow(0.88, var48);
                     var85.vy = var85.vy * (float)Math.pow(0.88, var48);
                  }
               }

               if (!this.particles.isEmpty()) {
                  for (TargetHud.a var86 : this.particles) {
                     float var89 = var86.life / var86.maxLife;
                     float var93;
                     if (!((var93 = (1.0F - var89) * (1.0F - var89) * var4) <= 0.01F)) {
                        float var95 = var49 + var86.relX;
                        float var97 = var50 + var86.relY;
                        float var99 = var86.glowSize * (1.0F - var89 * 0.3F);
                        int var101 = var86.color.getRed();
                        int var103 = var86.color.getGreen();
                        int var105 = var86.color.getBlue();
                        int var108 = Math.clamp((int)(var93 * 170.0F), 0, 255) << 24 | var101 << 16 | var103 << 8 | var105;
                        GlowPipeline.a(var34, var95, var97, var99, var108, 0.0F);
                     }
                  }

                  RenderUtil.f();

                  for (TargetHud.a var87 : this.particles) {
                     float var90 = var87.life / var87.maxLife;
                     float var94;
                     if (!((var94 = (1.0F - var90) * (1.0F - var90) * var4) <= 0.01F)) {
                        float var96 = var49 + var87.relX;
                        float var98 = var50 + var87.relY;
                        float var100;
                        float var102 = (var100 = var87.coreSize * (1.0F - var90 * 0.5F)) * 2.0F;
                        int var104 = var87.color.getRed();
                        int var106 = var87.color.getGreen();
                        int var107 = var87.color.getBlue();
                        int var109 = Math.clamp((int)(var94 * 70.0F), 0, 255) << 24 | var104 << 16 | var106 << 8 | var107;
                        RenderUtil.a(var34, var96 - var102 * 0.5F, var98 - var102 * 0.5F, var102, var102, var102 * 0.5F, var109);
                        int var66 = Math.min(255, var104 + (int)((255 - var104) * 0.75F));
                        int var67 = Math.min(255, var106 + (int)((255 - var106) * 0.75F));
                        int var68 = Math.min(255, var107 + (int)((255 - var107) * 0.75F));
                        int var70 = Math.clamp((int)(var94 * 255.0F), 0, 255) << 24 | var66 << 16 | var67 << 8 | var68;
                        RenderUtil.a(var34, var96 - var100 * 0.5F, var98 - var100 * 0.5F, var100, var100, var100 * 0.5F, var70);
                     }
                  }

                  RenderUtil.g();
               }

                float var84 = var8 < 10.0F ? 8.5F : 6.25F;
                float var88 = Msdf.a(MsdfManager.BOLD, var14, var84);
                float var91 = var79 + (var15 - var88) / 2.0F;
                float var92 = var31 + var15 / 2.0F - 0.593F * var84;
                Msdf.a(var34, MsdfManager.BOLD, var14, var91, var92, var84, a(varUnknown ? new Color(170, 170, 170) : b(var8), var6));
               HudConfig.c("target");
            }
         }
      }
   }

   @EventHandler
   public void onHudRender(HudRenderEvent var1) {
      float var2;
      if (enabled
         && this.mc.player != null
         && !this.mc.options.hudHidden
         && !((var2 = Math.clamp(this.alphaAnim.getValue(), 0.0F, 1.0F)) <= 0.02F)
         && this.lastTarget != null) {
         DrawContext var3 = var1.getContext();
         float var4 = HudConfig.a("target");
         float var5 = Math.clamp(this.popAnim.getValue(), 0.0F, 1.0F);
         float var6 = (0.94F + 0.06F * var5) * var4;
         float var7 = (1.0F - var2) * 4.0F;
         float var8 = this.y + var7;
         if (this.lastTarget != null && !(this.lastTarget instanceof PlayerEntity) && a(this.lastTarget.getType()) == null) {
            ItemStack var9 = a(this.lastTarget);
            if (!var9.isEmpty()) {
               float var10 = 13.0F;
               float var11 = this.x + 3.0F;
               float var12 = var8 + 3.0F;
               float var13 = var11 + (21.0F - var10) / 2.0F;
               float var14 = var12 + (21.0F - var10) / 2.0F;
               if (!ClickGui.a(var13, var14, var10, var10)) {
                  this.a(var3, var9, var13, var14, var10, var6, this.x, var8);
               }
            }
         }

         if (!this.targetItems.isEmpty()) {
            float var15 = var8 + 13.5F;

            for (TargetHud.e var17 : this.targetItems) {
               float var18 = var17.relX;
               if (!ClickGui.a(var17.relX, var15, 8.0F, 8.0F)) {
                  this.a(var3, var17.stack, var18, var15, 8.0F, var6, this.x, var8);
               }
            }
         }
      }
   }

   private static String a(String var0, float var1, float var2) {
      if (var0 != null && !var0.isEmpty() && !(var1 <= 0.0F)) {
         if (var0.equals(lastRawName) && Math.abs(var1 - lastMaxW) < 0.5F && var2 == lastFontSize) {
            return cachedTrimmed;
         }

         lastRawName = var0;
         lastMaxW = var1;
         lastFontSize = var2;
         if (Msdf.a(MsdfManager.BOLD, var0, var2) <= var1) {
            cachedTrimmed = var0;
            return var0;
         }

         String var3 = "...";
         float var4;
         if ((var4 = Msdf.a(MsdfManager.BOLD, var3, var2)) >= var1) {
            cachedTrimmed = var3;
            return var3;
         }

         float var5 = var1 - var4;
         int var6 = var0.length();

         while (var6 > 0 && Msdf.a(MsdfManager.BOLD, var0.substring(0, var6), var2) > var5) {
            var6--;
         }

         cachedTrimmed = var0.substring(0, var6) + var3;
         return cachedTrimmed;
      } else {
         return "";
      }
   }

   private static TargetHud.c a(EntityType<?> var0) {
      if (var0 == EntityType.WITHER_SKELETON) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/skeleton/wither_skeleton.png"), 0.125F, 0.25F, 0.25F, 0.5F);
      } else if (var0 == EntityType.SKELETON) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/skeleton/skeleton.png"), 0.125F, 0.25F, 0.25F, 0.5F);
      } else if (var0 == EntityType.STRAY) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/skeleton/stray.png"), 0.125F, 0.25F, 0.25F, 0.5F);
      } else if (var0 == EntityType.BOGGED) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/skeleton/bogged.png"), 0.125F, 0.25F, 0.25F, 0.5F);
      } else if (var0 == EntityType.ZOMBIE || var0 == EntityType.GIANT) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/zombie/zombie.png"), 0.125F, 0.125F, 0.25F, 0.25F);
      } else if (var0 == EntityType.HUSK) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/zombie/husk.png"), 0.125F, 0.125F, 0.25F, 0.25F);
      } else if (var0 == EntityType.DROWNED) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/zombie/drowned.png"), 0.125F, 0.125F, 0.25F, 0.25F);
      } else if (var0 == EntityType.ZOMBIE_VILLAGER) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/zombie_villager/zombie_villager.png"), 0.125F, 0.125F, 0.25F, 0.25F);
      } else if (var0 == EntityType.CREEPER) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/creeper/creeper.png"), 0.125F, 0.25F, 0.25F, 0.5F);
      } else if (var0 == EntityType.SPIDER) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/spider/spider.png"), 0.5F, 0.125F, 0.625F, 0.375F);
      } else if (var0 == EntityType.CAVE_SPIDER) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/spider/cave_spider.png"), 0.5F, 0.125F, 0.625F, 0.375F);
      } else if (var0 == EntityType.ENDERMAN) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/enderman/enderman.png"), 0.125F, 0.25F, 0.25F, 0.5F);
      } else if (var0 == EntityType.PIG) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/pig/pig.png"), 0.125F, 0.25F, 0.25F, 0.5F);
      } else if (var0 == EntityType.COW) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/cow/cow.png"), 0.125F, 0.25F, 0.25F, 0.5F);
      } else if (var0 == EntityType.MOOSHROOM) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/cow/red_mooshroom.png"), 0.125F, 0.25F, 0.25F, 0.5F);
      } else if (var0 == EntityType.SHEEP) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/sheep/sheep.png"), 0.125F, 0.25F, 0.25F, 0.5F);
      } else if (var0 == EntityType.CHICKEN) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/chicken.png"), 0.0F, 0.28125F, 0.09375F, 0.46875F);
      } else if (var0 == EntityType.BLAZE) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/blaze.png"), 0.125F, 0.25F, 0.25F, 0.5F);
      } else if (var0 == EntityType.SLIME) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/slime/slime.png"), 0.125F, 0.25F, 0.25F, 0.5F);
      } else if (var0 == EntityType.MAGMA_CUBE) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/slime/magmacube.png"), 0.125F, 0.25F, 0.25F, 0.5F);
      } else if (var0 == EntityType.PIGLIN) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/piglin/piglin.png"), 0.125F, 0.125F, 0.25F, 0.25F);
      } else if (var0 == EntityType.PIGLIN_BRUTE) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/piglin/piglin_brute.png"), 0.125F, 0.125F, 0.25F, 0.25F);
      } else if (var0 == EntityType.ZOMBIFIED_PIGLIN) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/piglin/zombified_piglin.png"), 0.125F, 0.125F, 0.25F, 0.25F);
      } else if (var0 == EntityType.VILLAGER) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/villager/villager.png"), 0.125F, 0.125F, 0.25F, 0.25F);
      } else if (var0 == EntityType.WANDERING_TRADER) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/wandering_trader.png"), 0.125F, 0.125F, 0.25F, 0.25F);
      } else if (var0 == EntityType.IRON_GOLEM) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/iron_golem/iron_golem.png"), 0.0625F, 0.078125F, 0.125F, 0.15625F);
      } else if (var0 == EntityType.SNOW_GOLEM) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/snow_golem.png"), 0.125F, 0.25F, 0.25F, 0.5F);
      } else if (var0 == EntityType.WITCH) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/witch.png"), 0.125F, 0.0625F, 0.25F, 0.125F);
      } else if (var0 == EntityType.EVOKER) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/illager/evoker.png"), 0.125F, 0.125F, 0.25F, 0.25F);
      } else if (var0 == EntityType.VINDICATOR) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/illager/vindicator.png"), 0.125F, 0.125F, 0.25F, 0.25F);
      } else if (var0 == EntityType.PILLAGER) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/illager/pillager.png"), 0.125F, 0.125F, 0.25F, 0.25F);
      } else if (var0 == EntityType.ILLUSIONER) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/illager/illusioner.png"), 0.125F, 0.125F, 0.25F, 0.25F);
      } else if (var0 == EntityType.WITHER) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/wither/wither.png"), 0.125F, 0.125F, 0.25F, 0.25F);
      } else if (var0 == EntityType.GHAST) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/ghast/ghast.png"), 0.125F, 0.25F, 0.375F, 0.75F);
      } else if (var0 == EntityType.WARDEN) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/warden/warden.png"), 0.125F, 0.125F, 0.25F, 0.25F);
      } else if (var0 == EntityType.BREEZE) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/breeze/breeze.png"), 0.125F, 0.125F, 0.25F, 0.25F);
      } else if (var0 == EntityType.GUARDIAN) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/guardian.png"), 0.125F, 0.125F, 0.25F, 0.25F);
      } else if (var0 == EntityType.ELDER_GUARDIAN) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/guardian_elder.png"), 0.125F, 0.125F, 0.25F, 0.25F);
      } else if (var0 == EntityType.WOLF) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/wolf/wolf.png"), 0.125F, 0.25F, 0.21875F, 0.4375F);
      } else if (var0 == EntityType.CAT || var0 == EntityType.OCELOT) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/cat/all_black.png"), 0.125F, 0.25F, 0.203125F, 0.40625F);
      } else if (var0 == EntityType.FOX) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/fox/fox.png"), 0.125F, 0.25F, 0.25F, 0.4375F);
      } else if (var0 == EntityType.BAT) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/bat.png"), 0.25F, 0.25F, 0.4375F, 0.4375F);
      } else if (var0 == EntityType.SHULKER) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/shulker/shulker.png"), 0.125F, 0.125F, 0.25F, 0.25F);
      } else if (var0 == EntityType.SQUID) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/squid/squid.png"), 0.125F, 0.25F, 0.3125F, 0.625F);
      } else if (var0 == EntityType.GLOW_SQUID) {
         return new TargetHud.c(Identifier.of("minecraft", "textures/entity/squid/glow_squid.png"), 0.125F, 0.25F, 0.3125F, 0.625F);
      } else {
         return var0 == EntityType.ENDER_DRAGON
            ? new TargetHud.c(Identifier.of("minecraft", "textures/entity/enderdragon/dragon.png"), 0.6875F, 0.171875F, 0.75F, 0.234375F)
            : null;
      }
   }

   public static ItemStack a(LivingEntity var0) {
      if (var0 == null) {
         return ItemStack.EMPTY;
      }

      EntityType var1 = var0.getType();
      if (var1 == EntityType.ZOMBIE) {
         return new ItemStack(Items.ZOMBIE_HEAD);
      }

      if (var1 == EntityType.SKELETON) {
         return new ItemStack(Items.SKELETON_SKULL);
      }

      if (var1 == EntityType.WITHER_SKELETON) {
         return new ItemStack(Items.WITHER_SKELETON_SKULL);
      }

      if (var1 == EntityType.CREEPER) {
         return new ItemStack(Items.CREEPER_HEAD);
      }

      if (var1 == EntityType.PIGLIN) {
         return new ItemStack(Items.PIGLIN_HEAD);
      }

      if (var1 == EntityType.ENDER_DRAGON) {
         return new ItemStack(Items.DRAGON_HEAD);
      }

      if (var1 == EntityType.IRON_GOLEM) {
         return new ItemStack(Items.IRON_BLOCK);
      }

      if (var1 == EntityType.SNOW_GOLEM) {
         return new ItemStack(Items.CARVED_PUMPKIN);
      }

      SpawnEggItem var2 = SpawnEggItem.forEntity(var1);
      return var2 != null ? new ItemStack(var2) : ItemStack.EMPTY;
   }

   private void a(DrawContext var1, ItemStack var2, float var3, float var4, float var5, float var6, float var7, float var8) {
      float var9 = 2.0F / this.mc.getWindow().getScaleFactor();
      Matrix3x2fStack var10 = var1.getMatrices();
      var10.pushMatrix();
      var10.translate(var7 * var9, var8 * var9);
      var10.scale(var6, var6);
      var10.translate((var3 - var7 + var5 * 0.5F) * var9, (var4 - var8 + var5 * 0.5F) * var9);
      float var11 = var5 / 16.0F * var9;
      var10.scale(var11, var11);
      var10.translate(-8.0F, -8.0F);
      var1.drawItem(var2, 0, 0);
      var10.popMatrix();
   }

   private void a(Matrix4f var1, MinecraftClient var2, PlayerEntity var3, float var4, float var5, float var6, int var7, float var8) {
      if (var3.isInvisible()) {
         String var50 = "?";
         float var51 = var6 * 0.75F;
         float var52 = Msdf.a(MsdfManager.BOLD, var50, var51);
         Msdf.b();
         Msdf.a(var1, MsdfManager.BOLD, var50, var4 + (var6 - var52) / 2.0F, var5 + var6 / 2.0F - 0.593F * var51, var51, a(Color.WHITE, var7));
         Msdf.c();
      } else {
         Identifier var9 = SkinCache.a(var2, var3);
         if (var9 != null) {
            try {
               GpuTextureView var10 = var2.getTextureManager().getTexture(var9).getGlTextureView();
               short var11 = 255;
               int var12 = (int)(255.0F * (1.0F - 0.75F * var8));
               int var13 = (int)(255.0F * (1.0F - 0.75F * var8));
               int var14 = var7 << 24 | var11 << 16 | var12 << 8 | var13;
               TexturePipeline.b(var1, var4 + 0.5F, var5 + 0.5F, var6 - 1.0F, var6 - 1.0F, var10, var14, 3.5F, 0.0F, 0.125F, 0.125F, 0.25F, 0.25F);
               TexturePipeline.b(var1, var4 + 0.5F, var5 + 0.5F, var6 - 1.0F, var6 - 1.0F, var10, var14, 3.5F, 0.0F, 0.625F, 0.125F, 0.75F, 0.25F);
            } catch (Exception var15) {
            }
         }
      }
   }

   private void a(Matrix4f var1, MinecraftClient var2, LivingEntity var3, float var4, float var5, float var6, int var7, float var8) {
      TargetHud.c var9 = a(var3.getType());
      if (var9 != null) {
         try {
            AbstractTexture var10 = var2.getTextureManager().getTexture(var9.getId());
            if (var10 != null) {
               GpuTextureView var11 = var10.getGlTextureView();
               short var12 = 255;
               int var13 = (int)(255.0F * (1.0F - 0.75F * var8));
               int var14 = (int)(255.0F * (1.0F - 0.75F * var8));
               int var15 = var7 << 24 | var12 << 16 | var13 << 8 | var14;
               TexturePipeline.b(
                  var1, var4 + 0.5F, var5 + 0.5F, var6 - 1.0F, var6 - 1.0F, var11, var15, 3.5F, 0.0F, var9.getU0(), var9.getV0(), var9.getU1(), var9.getV1()
               );
            }
         } catch (Exception var16) {
         }
      }
   }

   private static int a(Color var0, int var1) {
      return Math.clamp(var1, 0, 255) << 24 | var0.getRed() << 16 | var0.getGreen() << 8 | var0.getBlue();
   }

   private static Color b(float var0) {
      if (var0 <= 5.0F) {
         return new Color(255, 85, 85);
      } else if (var0 <= 10.0F) {
         return new Color(255, 170, 0);
      } else if (var0 <= 15.0F) {
         return new Color(255, 255, 85);
      } else {
         return var0 <= 20.0F ? new Color(85, 255, 85) : new Color(0, 170, 0);
      }
   }

   private static Color a(int var0, int var1) {
      if (var1 <= 0) {
         return Color.WHITE;
      } else {
         float var2;
         if ((var2 = Math.clamp((float)var0 / var1, 0.0F, 1.0F)) >= 0.5F) {
            float var7 = (var2 - 0.5F) * 2.0F;
            int var8 = (int)(255.0F * (1.0F - var7) + 78.0F * var7);
            int var9 = (int)(200.0F * (1.0F - var7) + 205.0F * var7);
            int var10 = (int)(60.0F * (1.0F - var7) + 120.0F * var7);
            return new Color(var8, var9, var10);
         } else {
            float var3 = var2 * 2.0F;
            int var4 = (int)(240.0F * (1.0F - var3) + 255.0F * var3);
            int var5 = (int)(65.0F * (1.0F - var3) + 200.0F * var3);
            int var6 = (int)(65.0F * (1.0F - var3) + 60.0F * var3);
            return new Color(var4, var5, var6);
         }
      }
   }

   static class a {
      float relX;
      float relY;
      float vx;
      float vy;
      float life;
      float maxLife;
      float coreSize;
      float glowSize;
      Color color;
   }

   static class b {
      Item item;
      int cur;
      int max;
      boolean tracked;
   }

   record c(Identifier id, float u0, float v0, float u1, float v1) {
      public Identifier getId() {
         return this.id;
      }

      public float getU0() {
         return this.u0;
      }

      public float getV0() {
         return this.v0;
      }

      public float getU1() {
         return this.u1;
      }

      public float getV1() {
         return this.v1;
      }
   }

   static class d {
      final TargetHud.b[] slots = new TargetHud.b[]{new TargetHud.b(), new TargetHud.b(), new TargetHud.b(), new TargetHud.b()};
      int lastHurt;
      long lastHurtTs;
   }

   static class e {
      ItemStack stack;
      float relX;
      EquipmentSlot slot;

      e() {
      }

      e(ItemStack var1, float var2, EquipmentSlot var3) {
         this.stack = var1;
         this.relX = var2;
         this.slot = var3;
      }

      void a(ItemStack var1, float var2, EquipmentSlot var3) {
         this.stack = var1;
         this.relX = var2;
         this.slot = var3;
      }
   }
}
