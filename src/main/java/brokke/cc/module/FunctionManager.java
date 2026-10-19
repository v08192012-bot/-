package brokke.cc.module;


import brokke.cc.module.combat.AimAssist;
import brokke.cc.module.combat.AimBot;
import brokke.cc.module.movement.AirStuck;
import brokke.cc.module.render.Ambience;
import brokke.cc.module.render.Atmosphere;
import brokke.cc.module.combat.AntiBot;
import brokke.cc.module.render.ArmorDurability;
import brokke.cc.module.player.AuctionHelper;
import brokke.cc.module.combat.Aura;
import brokke.cc.module.misc.AutoAccept;
import brokke.cc.module.combat.AutoArmor;
import brokke.cc.module.misc.AutoBuy;
import brokke.cc.module.combat.AutoCrystal;
import brokke.cc.module.player.AutoEat;
import brokke.cc.module.combat.AutoExplosion;
import brokke.cc.module.combat.AutoGapple;
import brokke.cc.module.combat.AutoMace;
import brokke.cc.module.combat.AutoPotion;
import brokke.cc.module.player.AutoRespawn;
import brokke.cc.module.misc.AutoSbor;
import brokke.cc.module.movement.AutoSprint;
import brokke.cc.module.combat.AutoSwap;
import brokke.cc.module.player.AutoTool;
import brokke.cc.module.combat.AutoTotem;
import brokke.cc.module.misc.AutoZamok;
import brokke.cc.module.movement.Baritone;
import brokke.cc.module.render.Beautifully;
import brokke.cc.module.render.BlockESP;
import brokke.cc.module.movement.BoatHighJump;
import brokke.cc.module.movement.BoatLongJump;
import brokke.cc.module.render.CameraTweaks;
import brokke.cc.module.misc.ChatHelper;
import brokke.cc.module.misc.ChestStealer;
import brokke.cc.module.player.ClickAction;
import brokke.cc.module.render.CustomFog;
import brokke.cc.module.movement.ElytraFly;
import brokke.cc.module.movement.ElytraTarget;
import brokke.cc.module.player.ElytraUtils;
import brokke.cc.module.render.EntityBox;
import brokke.cc.module.render.ExstraOptimizer;
import brokke.cc.module.combat.FixHp;
import brokke.cc.module.misc.FrameSync;
import brokke.cc.module.movement.FreeCam;
import brokke.cc.module.render.FreeLook;
import brokke.cc.module.render.FullBright;
import brokke.cc.module.movement.GrimGlide;
import brokke.cc.module.player.GuiMove;
import brokke.cc.module.render.HandsShader;
import brokke.cc.module.render.InterfaceModule;
import brokke.cc.module.player.InventoryButtons;
import brokke.cc.module.combat.ItemHelper;
import brokke.cc.module.render.ItemPhysic;
import brokke.cc.module.player.ItemScroller;
import brokke.cc.module.render.JumpCircle;
import brokke.cc.module.render.KillEffect;
import brokke.cc.module.render.NameTags;
import brokke.cc.module.player.NoDelay;
import brokke.cc.module.combat.NoFriendDamage;
import brokke.cc.module.player.NoInteract;
import brokke.cc.module.movement.NoPush;
import brokke.cc.module.render.NoRender;
import brokke.cc.module.player.NoRotate;
import brokke.cc.module.movement.NoSlow;
import brokke.cc.module.movement.NoWeb;
import brokke.cc.module.player.OpenWalls;
import brokke.cc.module.misc.Optimizer;
import brokke.cc.module.render.Particles;
import brokke.cc.module.misc.Party;
import brokke.cc.module.render.Pointers;
import brokke.cc.module.misc.PotionThrower;
import brokke.cc.module.render.Prediction;
import brokke.cc.module.misc.PvPSell;
import brokke.cc.module.render.SeeInvisibles;
import brokke.cc.module.misc.ServerHelper;
import brokke.cc.module.render.ShaderESP;
import brokke.cc.module.movement.ShulkerJump;
import brokke.cc.module.player.SoundReducer;
import brokke.cc.module.misc.Sounds;
import brokke.cc.module.movement.Speed;
import brokke.cc.module.movement.Spider;
import brokke.cc.module.player.StreamerMode;
import brokke.cc.module.player.Structures;
import brokke.cc.module.render.SwingAnimation;
import brokke.cc.module.render.TargetESP;
import brokke.cc.module.combat.TargetPearl;
import brokke.cc.module.combat.TriggerBot;
import brokke.cc.module.player.UseTracker;
import brokke.cc.module.combat.Velocity;
import brokke.cc.module.render.ViewModel;
import brokke.cc.module.render.WetWorld;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FunctionManager {
   private static final List<Function> functions = new ArrayList<>();
   private static final Map<Class<? extends Function>, Function> BY_CLASS = new HashMap<>();

   public static void a() {
      a(
         new Aura(),
         new TriggerBot(),
         new NoFriendDamage(),
         new AutoTotem(),
         new AutoSwap(),
         new Velocity(),
         new ShaderESP(),
         new AimBot(),
         new AimAssist(),
         new TargetPearl(),
         new AutoMace(),
         new AutoCrystal(),
         new FixHp()
      );
      a(
         new NoSlow(),
         new NoPush(),
         new NoWeb(),
         new AutoSprint(),
         new ShulkerJump(),
         new ElytraFly(),
         new BoatHighJump(),
         new AuctionHelper(),
         new ItemScroller(),
         new BoatLongJump(),
         new Spider(),
         new Speed(),
         new Baritone()
      );
      a(new AirStuck());
      a(new InterfaceModule());
      a(new FullBright());
      a(new TargetESP());
       a(new NameTags());
       a(new ExstraOptimizer());
       a(new EntityBox());
      a(new NoRender());
      a(new CustomFog());
      a(new WetWorld());
      a(new SeeInvisibles());
      a(new Pointers());
      a(new Beautifully());
      a(new BlockESP());
      a(JumpCircle.INSTANCE);
      a(
         new Particles(),
         new Ambience(),
         new Atmosphere(),
         new Prediction(),
         new SwingAnimation(),
         new ViewModel(),
         new KillEffect(),
         new HandsShader(),
         new ItemPhysic(),
         new ArmorDurability(),
         new CameraTweaks(),
         new FreeLook()
      );
      a(
         new NoDelay(),
         new ClickAction(),
         new NoRotate(),
         new NoInteract(),
         new GuiMove(),
         new FreeCam(),
         new ElytraUtils(),
         new StreamerMode(),
         new AutoGapple(),
         new UseTracker(),
         new AutoTool(),
         new InventoryButtons(),
         new AutoRespawn(),
         new AutoEat()
      );
      a(
         new ServerHelper(),
         new AutoAccept(),
         new Sounds(),
         new PvPSell(),
         new ChestStealer(),
         new Party(),
         new Optimizer(),
         new FrameSync(),
         new AutoBuy(),
         new AutoSbor()
      );
   }

   private static void a(Function... var0) {
      functions.addAll(Arrays.asList(var0));

      for (Function var4 : var0) {
         BY_CLASS.put((Class<? extends Function>)var4.getClass(), var4);
      }
   }

   public static <T extends Function> T a(Class<T> var0) {
      return (T)BY_CLASS.get(var0);
   }

   public static List<Function> a(Category var0) {
      return functions.stream().filter(var1 -> var1.getCategory() == var0).collect(Collectors.toList());
   }

   public static List<Function> getFunctions() {
      return functions;
   }
}
