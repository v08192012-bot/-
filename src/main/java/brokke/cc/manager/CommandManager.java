package brokke.cc.manager;

import brokke.cc.config.ConfigManager;
import brokke.cc.core.FakePlayerTickHandler;
import brokke.cc.core.NeuroRecorder;
import brokke.cc.module.combat.Aura;
import brokke.cc.neuro.NeuroModels;
import brokke.cc.neuro.NeuroTrainer;
import brokke.cc.core.RCTCommand;
import brokke.cc.module.Function;
import brokke.cc.module.FunctionManager;
import brokke.cc.module.player.StreamerMode;
import brokke.cc.nav.GotoTask;
import brokke.cc.nav.MineBlockTask;
import brokke.cc.nav.NavRecorder;
import brokke.cc.nav.NavigationManager;
import brokke.cc.nav.Waypoint;
import brokke.cc.nav.WaypointStorage;
import brokke.cc.render.Theme;
import brokke.cc.ui.ClickGui;
import brokke.cc.util.AnarchyUtil;
import brokke.cc.util.KeyHelper;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class CommandManager {
   public static final String PREFIX = ".";
   private static final Map<String, CommandManager.b> COMMANDS = new LinkedHashMap<>();

   public static CommandManager.a a(String var0) {
      return a(var0, var0 != null ? var0.length() : 0);
   }

   public static CommandManager.a a(String var0, int var1) {
      if (var0 != null && var0.startsWith(".")) {
         int var2 = Math.max(0, Math.min(var1, var0.length()));
         int var3 = var2;

         while (var3 > 0 && var0.charAt(var3 - 1) != ' ') {
            var3--;
         }

         String var4 = var0.substring(var3, var2);
         String var5 = var0.substring(0, var3).trim();
         if (var5.isEmpty()) {
            String var17 = var4.toLowerCase(Locale.ROOT);
            ArrayList var18 = new ArrayList();

            for (String var20 : COMMANDS.keySet()) {
               String var21 = "." + var20;
               if (var21.toLowerCase(Locale.ROOT).startsWith(var17)) {
                  String var22 = var4 + var21.substring(Math.min(var4.length(), var21.length()));
                  var18.add(var22);
               }
            }

            return new CommandManager.a(var3, var18);
         } else {
            if (!var5.startsWith(".")) {
               return new CommandManager.a(var3, List.of());
            }

            String var6 = var5.substring(".".length()).trim();
            if (var6.isEmpty()) {
               return new CommandManager.a(var3, List.of());
            }

            String[] var7 = var6.split("\\s+");
            String var8 = var7[0].toLowerCase(Locale.ROOT);
            CommandManager.b var9 = COMMANDS.get(var8);
            if (var9 == null) {
               return new CommandManager.a(var3, List.of());
            }

            int var10 = var7.length;
            List var11 = List.of();
            if (var8.equals("friend")) {
               if (var10 == 1) {
                  var11 = List.of("add", "remove", "list", "clear");
               } else if (var10 == 2) {
                  if (var7[1].equalsIgnoreCase("add")) {
                     var11 = a();
                  } else if (var7[1].equalsIgnoreCase("remove") || var7[1].equalsIgnoreCase("del") || var7[1].equalsIgnoreCase("delete")) {
                     var11 = new ArrayList<>(FriendManager.b());
                  }
               }
            } else if (var8.equals("bind")) {
               if (var10 == 1) {
                  ArrayList var12 = new ArrayList<>(List.of("list", "remove", "clear"));
                  var12.addAll(FunctionManager.getFunctions().stream().map(var0x -> var0x.getName().toLowerCase(Locale.ROOT)).toList());
                  var11 = var12;
               } else if (var10 == 2) {
                  if (!var7[1].equalsIgnoreCase("remove") && !var7[1].equalsIgnoreCase("del") && !var7[1].equalsIgnoreCase("delete")) {
                     var11 = List.of("none", "rshift", "lshift", "rctrl", "lctrl", "alt", "space", "enter", "tab", "c", "v", "b", "x", "z", "f", "g", "r");
                  } else {
                     var11 = FunctionManager.getFunctions()
                        .stream()
                        .filter(var0x -> var0x.getKeybind() != 0 && var0x.getKeybind() != -1)
                        .map(var0x -> var0x.getName().toLowerCase(Locale.ROOT))
                        .toList();
                  }
               }
            } else if (!var8.equals("config") && !var8.equals("cfg")) {
               if (var8.equals("fakeplayer")) {
                  if (var10 == 1) {
                     var11 = List.of("spawn", "remove", "clear", "move");
                  } else if (var10 == 2) {
                     if (var7[1].equalsIgnoreCase("move") || var7[1].equalsIgnoreCase("spar")) {
                        var11 = List.of("on", "off");
                     } else if (var7[1].equalsIgnoreCase("spawn")) {
                        var11 = List.of("Target", "Bot");
                     }
                  }
               } else if (var8.equals("gps")) {
                  if (var10 == 1) {
                     var11 = List.of("zamok", "remove", "list", "clear");
                  } else if (var10 == 2 && (var7[1].equalsIgnoreCase("remove") || var7[1].equalsIgnoreCase("del") || var7[1].equalsIgnoreCase("delete"))) {
                     var11 = WaypointStorage.getInstance().getWaypoints().stream().map(Waypoint::getName).toList();
                  }
               } else if (var8.equals("rct")) {
                  if (var10 == 1) {
                     int var23 = AnarchyUtil.a();
                     int var13 = var23 > 0 ? var23 : RCTCommand.getLastAnarchyNumber();
                     var11 = var13 > 0 ? (StreamerMode.k() ? List.of("hub", "stop") : List.of(String.valueOf(var13), "hub", "stop")) : List.of("hub", "stop");
                  }
               } else if (var8.equals("party")) {
                  if (var10 == 1) {
                     var11 = List.of("create", "join", "leave", "disband", "list");
                  }
               } else if (var8.equals("goto")) {
                  if (var10 == 1) {
                     var11 = List.of("stop");
                  }
               } else if (var8.equals("mine")) {
                  if (var10 == 1) {
                     var11 = List.of(
                        "stop", "diamond_ore", "deepslate_diamond_ore", "iron_ore", "deepslate_iron_ore", "gold_ore", "ancient_debris", "oak_log", "stone"
                     );
                  }
                } else if (var8.equals("nav") || var8.equals("baritone")) {
                   if (var10 == 1) {
                      var11 = List.of("status", "record", "stop", "list", "goto", "mine");
                   } else if (var10 != 2 || !var7[1].equalsIgnoreCase("record") && !var7[1].equalsIgnoreCase("rec")) {
                      if (var10 == 2 && var7[1].equalsIgnoreCase("mine")) {
                         var11 = List.of("stop", "diamond_ore", "iron_ore", "ancient_debris", "oak_log");
                      }
                   } else {
                      var11 = NavRecorder.d();
                   }
                } else if (var8.equals("gui")) {
                   if (var10 == 1) {
                      var11 = List.of("cs", "panels");
                   }
                } else if (var8.equals("neuro")) {
                   if (var10 == 1) {
                      var11 = List.of("record", "stop", "status");
                   } else if (var10 == 2 && (var7[1].equalsIgnoreCase("record") || var7[1].equalsIgnoreCase("rec"))) {
                      var11 = List.of("20", "30", "60", "nobot");
                   }
                }
            } else if (var10 == 1) {
               var11 = List.of("save", "load", "reset", "dir", "list", "delete");
            } else if (var10 == 2
               && (
                  var7[1].equalsIgnoreCase("load") || var7[1].equalsIgnoreCase("delete") || var7[1].equalsIgnoreCase("del") || var7[1].equalsIgnoreCase("save")
               )) {
               var11 = ConfigManager.j();
            }

            String var24 = var4.toLowerCase(Locale.ROOT);
            ArrayList var25 = new ArrayList();

            for (String var15 : (Iterable<String>)(Iterable<?>)(var11)) {
               if (var15 != null && !var15.startsWith("<") && var15.toLowerCase(Locale.ROOT).startsWith(var24)) {
                  String var16 = var4 + var15.substring(Math.min(var4.length(), var15.length()));
                  var25.add(var16);
               }
            }

            return new CommandManager.a(var3, var25);
         }
      } else {
         return new CommandManager.a(0, List.of());
      }
   }

   private static List<String> a() {
      MinecraftClient var0 = MinecraftClient.getInstance();
      if (var0 == null) {
         return List.of();
      }

      ArrayList var1 = new ArrayList();
      String var2 = var0.player != null ? var0.player.getName().getString() : null;
      if (var0.getNetworkHandler() != null && var0.getNetworkHandler().getPlayerList() != null) {
         for (PlayerListEntry var4 : var0.getNetworkHandler().getPlayerList()) {
            if (var4 != null && var4.getProfile() != null) {
               String var5 = var4.getProfile().name();
               if (var5 != null && !var5.isEmpty() && !var5.equalsIgnoreCase(var2) && !var1.contains(var5)) {
                  var1.add(var5);
               }
            }
         }
      }

      if (var1.isEmpty() && var0.world != null) {
         for (AbstractClientPlayerEntity var7 : var0.world.getPlayers()) {
            if (var7 != null && var7 != var0.player) {
               String var8 = var7.getName().getString();
               if (var8 != null && !var8.isEmpty() && !var1.contains(var8)) {
                  var1.add(var8);
               }
            }
         }
      }

      return var1;
   }

   public static List<String> b(String var0) {
      CommandManager.a var1 = a(var0);
      if (var1.getCandidates().isEmpty()) {
         return List.of();
      }

      if (var1.getStart() == 0) {
         return var1.getCandidates();
      }

      String var2 = var0.substring(0, var1.getStart());
      ArrayList var3 = new ArrayList();

      for (String var5 : var1.getCandidates()) {
         var3.add(var2 + var5);
      }

      return var3;
   }

   public static boolean c(String var0) {
      if (var0 != null && var0.startsWith(".")) {
         String[] var1 = var0.substring(".".length()).trim().split("\\s+");
         if (var1.length != 0 && !var1[0].isEmpty()) {
            try {
               switch (var1[0].toLowerCase(Locale.ROOT)) {
                  case "help":
                     d("§b§lShelka §7» §fСписок доступных команд:");
                     d(" §b.help §7— §fсписок всех команд");
                     d(" §b.bind §3<модуль> <клавиша> §7— §fпривязать клавишу к модулю");
                     d(" §b.bind §3list §7— §fсписок всех активных биндов");
                     d(" §b.bind §3remove <модуль> §7— §fудалить бинд с модуля");
                     d(" §b.friend §3add | remove | list | clear §7— §fуправление друзьями");
                     d(" §b.cfg §3save | load | list | dir | delete | reset §7— §fуправление конфигами");
                     d(" §b.fakeplayer §3spawn | remove | clear | move §7— §fтренировочный бот");
                     d(" §b.gps §3<x y z> | zamok | list | remove | clear §7— §fнавигатор к точкам");
                     d(" §b.rct §3<номер> | hub | stop §7— §fпереподключение к анархии");
                     d(" §b.party §3create | join | leave | disband | list §7— §fуправление пати");
                     d(" §b.goto §3<x y z> | <x z> | stop §7— §fнавигация к координатам");
                     d(" §b.mine §3<x y z> | <блок> | stop §7— §fвскапывание блока");
                      d(" §b.nav §3status | record <имя> | stop | list §7— §fуправление навигацией и рекордером");
                      d(" §b.neuro §3bot | record <минуты> [nobot] | stop | train <эпох> | model | status §7— §fнейро-датасет и обучение GRU (режим ауры «Нейро»)");
                      break;
                  case "friend":
                     a(var1);
                     break;
                  case "bind":
                     b(var1);
                     break;
                  case "config":
                  case "cfg":
                     c(var1);
                     break;
                  case "fakeplayer":
                     d(var1);
                     break;
                  case "gps":
                     e(var1);
                     break;
                  case "rct":
                     f(var1);
                     break;
                  case "party":
                     g(var1);
                     break;
                  case "nbtdump":
                  case "nbt":
                     c();
                     break;
                  case "goto":
                     h(var1);
                     break;
                  case "irc":
                     ClickGui.m();
                     break;
                  case "gui":
                     n(var1);
                     break;
                  case "mine":
                     i(var1);
                     break;
                   case "nav":
                   case "baritone":
                      j(var1);
                      break;
                   case "neuro":
                      k(var1);
                      break;
                  default:
                     d("§cНеизвестная команда. §f.help§c — список команд");
               }
            } catch (Exception var4) {
               d("§cОшибка: §f" + var4.getMessage());
            }

            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   /** .gui cs | .gui panels — переключение вида клик-гуи. */
   private static void n(String[] var0) {
      if (var0.length < 2) {
         d("§7Клик-гуи сейчас: §f" + Theme.getInstance().getGuiMode().getLabel() + "§7. Используй §f.gui cs§7 или §f.gui panels");
         return;
      }

      String var1 = var0[1].toLowerCase(Locale.ROOT);
      if (var1.equals("cs")) {
         Theme.getInstance().a(Theme.b.CS);
         d("§7Клик-гуи: §fCS§7 (RightShift)");
      } else if (var1.equals("panels") || var1.equals("panel") || var1.equals("classic")) {
         Theme.getInstance().a(Theme.b.PANELS);
         d("§7Клик-гуи: §fПанели§7 (RightShift)");
      } else {
         d("§cИспользование: §f.gui cs §7| §f.gui panels");
      }
   }

   private static void a(String[] var0) {
      if (var0.length < 2) {
         d("§cИспользование: §f.friend add|remove|list|clear [ник]");
      } else {
         switch (var0[1].toLowerCase(Locale.ROOT)) {
            case "add":
               if (var0.length < 3) {
                  d("§cИспользование: §f.friend add <ник>");
                  return;
               }

               FriendManager.b(var0[2]);
               d("§aДруг добавлен: §f" + var0[2]);
               break;
            case "remove":
            case "del":
            case "delete":
               if (var0.length < 3) {
                  d("§cИспользование: §f.friend remove <ник>");
                  return;
               }

               FriendManager.c(var0[2]);
               d("§aДруг удалён: §f" + var0[2]);
               break;
            case "clear":
               FriendManager.c();
               d("§aСписок друзей очищен");
               break;
            case "list":
               if (FriendManager.b().isEmpty()) {
                  d("§7Список друзей пуст");
               } else {
                  d("§bДрузья: §f" + String.join(", ", FriendManager.b()));
               }
               break;
            default:
               d("§cИспользование: §f.friend add|remove|list|clear [ник]");
         }
      }
   }

   private static void b(String[] var0) {
      if (var0.length < 2) {
         d("§cИспользование: §f.bind <модуль> <клавиша> §7| §f.bind list §7| §f.bind remove <модуль>");
      } else {
         String var1 = var0[1].toLowerCase(Locale.ROOT);
         if (var1.equals("list")) {
            List var8 = FunctionManager.getFunctions().stream().filter(var0x -> var0x.getKeybind() != 0 && var0x.getKeybind() != -1).toList();
            if (var8.isEmpty()) {
               d("§7[§bBind§7] §fСписок активных биндов пуст");
            } else {
               d("§7[§bBind§7] §fСписок биндов (§b" + var8.size() + "§f):");

               for (Function var12 : (Iterable<Function>)(Iterable<?>)(var8)) {
                  String var5 = var12.isActive() ? " §a(вкл)" : " §7(выкл)";
                  d("  §7• §f" + var12.getName() + " §7→ §b" + KeyHelper.a(var12.getKeybind()) + var5);
               }
            }
         } else if (!var1.equals("clear")) {
            if (!var1.equals("remove") && !var1.equals("del") && !var1.equals("delete")) {
               Function var7 = FunctionManager.getFunctions().stream().filter(var1x -> var1x.getName().equalsIgnoreCase(var0[1])).findFirst().orElse(null);
               if (var7 == null) {
                  d("§cМодуль не найден: §f" + var0[1]);
               } else if (var0.length < 3) {
                  int var10 = var7.getKeybind();
                  String var4 = KeyHelper.a(var10);
                  d("§7[§bBind§7] §fМодуль §b" + var7.getName() + " §fзабинжен на §b" + var4);
                  d("§7Использование: §f.bind " + var7.getName().toLowerCase(Locale.ROOT) + " <клавиша|none>");
               } else if (!var0[2].equalsIgnoreCase("none")
                  && !var0[2].equalsIgnoreCase("0")
                  && !var0[2].equalsIgnoreCase("clear")
                  && !var0[2].equalsIgnoreCase("remove")) {
                  int var9 = e(var0[2]);
                  if (var9 == 0) {
                     d("§cНеизвестная клавиша: §f" + var0[2]);
                  } else {
                     var7.setKeybind(var9);
                     d("§7[§bBind§7] §fМодуль §b" + var7.getName() + " §7забинжен на §b" + KeyHelper.a(var9));
                  }
               } else {
                  var7.setKeybind(0);
                  d("§7[§bBind§7] §fБинд снят с модуля §b" + var7.getName());
               }
            } else if (var0.length < 3) {
               d("§cИспользование: §f.bind remove <модуль>");
            } else {
               Function var6 = FunctionManager.getFunctions().stream().filter(var1x -> var1x.getName().equalsIgnoreCase(var0[2])).findFirst().orElse(null);
               if (var6 == null) {
                  d("§cМодуль не найден: §f" + var0[2]);
               } else {
                  var6.setKeybind(0);
                  d("§7[§bBind§7] §fБинд снят с модуля §b" + var6.getName());
               }
            }
         } else {
            for (Function var3 : FunctionManager.getFunctions()) {
               var3.setKeybind(0);
            }

            d("§7[§bBind§7] §fВсе бинды успешно очищены");
         }
      }
   }

   private static void c(String[] var0) {
      if (var0.length < 2) {
         b();
      } else {
         String var1 = var0[1].toLowerCase(Locale.ROOT);
         String var2 = var0.length >= 3 ? var0[2] : null;
         switch (var1) {
            case "save":
            case "create":
            case "add":
               if (var2 == null || var2.trim().isEmpty()) {
                  d("§cИспользование: §f.cfg save <название>");
                  return;
               }

               boolean var8 = ConfigManager.a(var2);
               if (var8) {
                  d("§aКонфиг §f" + var2 + " §aуспешно сохранён");
               } else {
                  d("§cНе удалось сохранить конфиг §f" + var2);
               }
               break;
            case "load":
            case "use":
               if (var2 == null || var2.trim().isEmpty()) {
                  d("§cИспользование: §f.cfg load <название>");
                  return;
               }

               boolean var7 = ConfigManager.b(var2);
               if (var7) {
                  d("§aКонфиг §f" + var2 + " §aуспешно загружен");
               } else {
                  d("§cКонфиг §f" + var2 + " §cне найден");
               }
               break;
            case "reset":
            case "clear":
               boolean var6 = ConfigManager.i();
               if (var6) {
                  d("§aВсе настройки и модули успешно сброшены к начальным");
               } else {
                  d("§cОшибка при сбросе настроек");
               }
               break;
            case "delete":
            case "remove":
            case "del":
               if (var2 == null || var2.trim().isEmpty()) {
                  d("§cУкажите имя конфига: §f.cfg delete <имя>");
                  return;
               }

               boolean var5 = ConfigManager.c(var2);
               if (var5) {
                  d("§aКонфиг §f" + var2 + " §aудалён");
               } else {
                  d("§cНе удалось удалить конфиг §f" + var2);
               }
               break;
            case "list":
               b();
               break;
            case "dir":
            case "folder":
            case "open":
               ConfigManager.openConfigFolder();
               d("§aПапка с конфигами открыта");
               break;
            default:
               d("§cИспользование: §f.cfg save|load|reset|dir|list|delete [имя]");
         }
      }
   }

   private static void b() {
      List var0 = ConfigManager.j();
      if (var0.isEmpty()) {
         d("§7Список конфигов пуст");
      } else {
         d("§bСписок конфигов (" + var0.size() + "):");

         for (int var1 = 0; var1 < var0.size(); var1++) {
            String var2 = (String)var0.get(var1);
            String var3 = "";
            if (var2.equalsIgnoreCase(ConfigManager.getCurrentConfig())) {
               var3 = " §a(активен)";
            } else if (var2.equalsIgnoreCase(ConfigManager.getLastLoadedConfig())) {
               var3 = " §e(загружен)";
            }

            d("§7[" + (var1 + 1) + "] §f" + var2 + var3);
         }
      }
   }

   private static int e(String var0) {
      if (var0 != null && !var0.isEmpty() && !var0.equalsIgnoreCase("none") && !var0.equals("0")) {
         String var1 = var0.trim().toUpperCase(Locale.ROOT);

         return switch (var1) {
            case "RSHIFT", "R.SHIFT", "RIGHT_SHIFT", "RIGHTSHIFT" -> {
               short var30 = 344;
               short var32 = var30;
               yield var32;
            }
            case "LSHIFT", "L.SHIFT", "LEFT_SHIFT", "LEFTSHIFT", "SHIFT" -> {
               short var29 = 340;
               short var31 = var29;
               yield var31;
            }
            case "RCTRL", "R.CTRL", "RIGHT_CTRL", "RIGHT_CONTROL", "RIGHTCONTROL" -> {
               short var28 = 345;
               short var30 = var28;
               yield var30;
            }
            case "LCTRL", "L.CTRL", "LEFT_CTRL", "LEFT_CONTROL", "LEFTCONTROL", "CTRL", "CONTROL" -> {
               short var27 = 341;
               short var29 = var27;
               yield var29;
            }
            case "RALT", "R.ALT", "RIGHT_ALT", "RIGHTALT" -> {
               short var26 = 346;
               short var28 = var26;
               yield var28;
            }
            case "LALT", "L.ALT", "LEFT_ALT", "LEFTALT", "ALT" -> {
               short var25 = 342;
               short var27 = var25;
               yield var27;
            }
            case "CAPS", "CAPSLOCK", "CAPS_LOCK" -> {
               short var24 = 280;
               short var26 = var24;
               yield var26;
            }
            case "ESC", "ESCAPE" -> {
               short var23 = 256;
               short var25 = var23;
               yield var25;
            }
            case "ENTER", "RETURN", "RET" -> {
               short var22 = 257;
               short var24 = var22;
               yield var24;
            }
            case "SPACE", "SPC" -> {
               byte var21 = 32;
               byte var23 = var21;
               yield var23;
            }
            case "TAB" -> {
               short var20 = 258;
               short var22 = var20;
               yield var22;
            }
            case "BACKSPACE", "BACK" -> {
               short var19 = 259;
               short var21 = var19;
               yield var21;
            }
            case "DEL", "DELETE" -> {
               short var18 = 261;
               short var20 = var18;
               yield var20;
            }
            case "INS", "INSERT" -> {
               short var17 = 260;
               short var19 = var17;
               yield var19;
            }
            case "PGUP", "PAGE_UP", "PAGEUP" -> {
               short var16 = 266;
               short var18 = var16;
               yield var18;
            }
            case "PGDN", "PGDOWN", "PAGE_DOWN", "PAGEDOWN" -> {
               short var15 = 267;
               short var17 = var15;
               yield var17;
            }
            case "HOME" -> {
               short var14 = 268;
               short var16 = var14;
               yield var16;
            }
            case "END" -> {
               short var13 = 269;
               short var15 = var13;
               yield var15;
            }
            case "UP" -> {
               short var12 = 265;
               short var14 = var12;
               yield var14;
            }
            case "DOWN" -> {
               short var11 = 264;
               short var13 = var11;
               yield var13;
            }
            case "LEFT" -> {
               short var10 = 263;
               short var12 = var10;
               yield var12;
            }
            case "RIGHT" -> {
               short var9 = 262;
               short var11 = var9;
               yield var11;
            }
            default -> {
               label252: {
                  if (var1.length() == 1) {
                     char var5 = var1.charAt(0);
                     if (var5 >= 'A' && var5 <= 'Z' || var5 >= '0' && var5 <= '9') {
                        char var8 = var5;
                        char var10 = var8;
                        yield var10;
                     }
                  }

                  int var7;
                  try {
                     Field var31 = GLFW.class.getField("GLFW_KEY_" + var1);
                     var7 = var31.getInt(null);
                  } catch (Exception var6) {
                     yield 0;
                  }

                  int var9 = var7;
                  yield var9;
               }
            }
         };
      } else {
         return 0;
      }
   }

   private static void d(String[] var0) {
      if (var0.length < 2) {
         d("§c.fakeplayer spawn|remove|clear|move [ник]");
      } else {
         switch (var0[1].toLowerCase(Locale.ROOT)) {
            case "spawn":
               String var6 = var0.length >= 3 ? var0[2] : "Target";
               MinecraftClient var4 = MinecraftClient.getInstance();
               if (var4.player == null) {
                  return;
               }

               Vec3d var5 = var4.player.getEntityPos().add(var4.player.getRotationVector().multiply(3.0));
               FakePlayerManager.a(var5, var6);
               break;
            case "remove":
               FakePlayerManager.b();
               break;
            case "clear":
               FakePlayerManager.c();
               break;
            case "move":
            case "spar":
               boolean var3 = var0.length < 3 ? !FakePlayerTickHandler.isMoving() : var0[2].equalsIgnoreCase("on") || var0[2].equalsIgnoreCase("вкл");
               FakePlayerTickHandler.a(var3);
               if (var3) {
                  d("§aБот двигается: стрейфы, смена дистанции, прыжки");
               } else {
                  d("§7Бот остановлен");
               }
               break;
            default:
               d("§c.fakeplayer spawn|remove|clear|move [ник]");
         }
      }
   }

   private static void e(String[] var0) {
      if (var0.length < 2) {
         d("§cНеверный формат. Используй: §f.gps <имя> <x> <y> <z> §7(напр. §f.gps База 100 64 -200§7)");
         d("§7Также: §f.gps zamok§7, §f.gps remove [имя]§7, §f.gps list§7, §f.gps clear");
      } else {
         switch (var0[1].toLowerCase(Locale.ROOT)) {
            case "zamok":
               WaypointStorage.getInstance().b(new Waypoint(0.0, 100.0, 0.0, "Замок"));
               d("§aМетка замка поставлена: §f0, 100, 0");
               break;
            case "remove":
            case "del":
            case "delete":
               if (var0.length >= 3) {
                  StringBuilder var13 = new StringBuilder();

                  for (int var17 = 2; var17 < var0.length; var17++) {
                     if (var17 > 2) {
                        var13.append(' ');
                     }

                     var13.append(var0[var17]);
                  }

                  String var18 = var13.toString().trim();
                  if (WaypointStorage.getInstance().a(var18)) {
                     d("§aМетка «" + var18 + "» удалена!");
                  } else {
                     d("§cМетка «" + var18 + "» не найдена");
                  }
               } else if (!WaypointStorage.getInstance().c()) {
                  WaypointStorage.getInstance().clearWaypoints();
                  d("§aВсе метки удалены!");
               } else {
                  d("§7Меток не было");
               }
               break;
            case "clear":
            case "off":
            case "reset":
               if (!WaypointStorage.getInstance().c()) {
                  WaypointStorage.getInstance().clearWaypoints();
                  d("§aВсе метки удалены!");
               } else {
                  d("§7Меток не было");
               }
               break;
            case "list":
               List var12 = WaypointStorage.getInstance().getWaypoints();
               if (var12.isEmpty()) {
                  d("§7Список меток пуст");
                  return;
               }

               d("§bАктивные метки (" + var12.size() + "):");
               MinecraftClient var16 = MinecraftClient.getInstance();

               for (Waypoint var24 : (Iterable<Waypoint>)(Iterable<?>)(var12)) {
                  double var26 = var16.player != null ? Math.sqrt(var16.player.squaredDistanceTo(var24.getX(), var24.getY(), var24.getZ())) : 0.0;
                  d(
                     "§f• §b"
                        + var24.getName()
                        + "§7: §f"
                        + (int)var24.getX()
                        + " "
                        + (int)var24.getY()
                        + " "
                        + (int)var24.getZ()
                        + " §7("
                        + Math.round(var26)
                        + "м)"
                  );
               }
               break;
            default:
               int var3 = var0.length;
               if (var3 >= 5) {
                  try {
                     int var4 = Integer.parseInt(var0[var3 - 3]);
                     int var5 = Integer.parseInt(var0[var3 - 2]);
                     int var6 = Integer.parseInt(var0[var3 - 1]);
                     StringBuilder var7 = new StringBuilder();

                     for (int var8 = 1; var8 < var3 - 3; var8++) {
                        if (var8 > 1) {
                           var7.append(' ');
                        }

                        var7.append(var0[var8]);
                     }

                     String var27 = var7.toString().trim();
                     WaypointStorage.getInstance().b(new Waypoint(var4, var5, var6, var27));
                     d("§aМетка «" + var27 + "» поставлена: §f" + var4 + ", " + var5 + ", " + var6);
                     return;
                  } catch (NumberFormatException var13) {
                  }
               } else if (var3 == 4) {
                  try {
                     int var14 = Integer.parseInt(var0[1]);
                     int var19 = Integer.parseInt(var0[2]);
                     int var22 = Integer.parseInt(var0[3]);
                     WaypointStorage.getInstance().b(new Waypoint(var14, var19, var22, "GPS"));
                     d("§aМетка поставлена: §f" + var14 + ", " + var19 + ", " + var22);
                     return;
                  } catch (NumberFormatException var121) {
                  }
               } else if (var3 == 3) {
                  try {
                     int var15 = Integer.parseInt(var0[1]);
                     int var20 = Integer.parseInt(var0[2]);
                     MinecraftClient var23 = MinecraftClient.getInstance();
                     int var25 = var23.player != null ? (int)var23.player.getY() : 64;
                     WaypointStorage.getInstance().b(new Waypoint(var15, var25, var20, "GPS"));
                     d("§aМетка поставлена: §f" + var15 + ", " + var25 + ", " + var20);
                     return;
                  } catch (NumberFormatException var11) {
                  }
               }

               d("§cНеверный формат. Используй: §f.gps <имя> <x> <y> <z> §7(напр. §f.gps База 100 64 -200§7)");
         }
      }
   }

   private static void f(String[] var0) {
      if (var0.length == 1) {
         RCTCommand.b();
      } else if (var0[1].equalsIgnoreCase("hub")) {
         RCTCommand.c();
      } else if (!var0[1].equalsIgnoreCase("stop") && !var0[1].equalsIgnoreCase("cancel")) {
         StringBuilder var1 = new StringBuilder();

         for (int var2 = 1; var2 < var0.length; var2++) {
            var1.append(var0[var2]).append(" ");
         }

         String var6 = var1.toString().trim();
         int var3 = AnarchyUtil.a(var6);
         if (var3 != -1) {
            RCTCommand.a(var3);
         } else {
            try {
               int var4 = Integer.parseInt(var6.replaceAll("[^0-9]", ""));
               RCTCommand.a(var4);
            } catch (Exception var5) {
               d("§cИспользование: §f.rct [1-36|hub|stop]");
            }
         }
      } else if (RCTCommand.isRunning()) {
         RCTCommand.d();
         d("§aПодключение отменено");
      } else {
         d("§7Подключение не выполняется");
      }
   }

   private static void g(String[] var0) {
      if (var0.length < 2) {
         d("§b§lParty §7» §fКоманды пати:");
         d(" §b.party create §7— §fсоздать команду");
         d(" §b.party join <код> §7— §fвойти в команду по коду");
         d(" §b.party leave §7— §fвыйти из команды");
         d(" §b.party disband §7— §fраспустить команду (для владельца)");
         d(" §b.party list §7— §fсписок участников команды");
      } else {
         switch (var0[1].toLowerCase(Locale.ROOT)) {
            case "create":
               PartyManager.getInstance().n();
               break;
            case "join":
               if (var0.length < 3) {
                  d("§cИспользование: §f.party join <код>");
                  return;
               }

               PartyManager.getInstance().d(var0[2]);
               break;
            case "leave":
               PartyManager.getInstance().o();
               break;
            case "disband":
            case "delete":
               PartyManager.getInstance().p();
               break;
            case "list":
            case "info":
            case "members":
               PartyManager.getInstance().q();
               break;
            default:
               d("§cНеизвестная подкоманда. §f.party §cдля помощи");
         }
      }
   }

   private static void c() {
      MinecraftClient var0 = MinecraftClient.getInstance();
      if (var0.player != null) {
         ItemStack var1 = var0.player.getMainHandStack();
         if (var1 == null || var1.isEmpty()) {
            var1 = var0.player.getOffHandStack();
         }

         if (var1 != null && !var1.isEmpty()) {
            StringBuilder var2 = new StringBuilder();
            var2.append("=== ITEM DUMP ===\n");
            var2.append("Item: ").append(Registries.ITEM.getId(var1.getItem())).append("\n");
            var2.append("Count: ").append(var1.getCount()).append("\n");
            Text var3 = var1.get(DataComponentTypes.CUSTOM_NAME);
            if (var3 != null) {
               var2.append("CustomName: ").append(var3.getString()).append("\n");
            }

            LoreComponent var4 = var1.get(DataComponentTypes.LORE);
            if (var4 != null) {
               var2.append("Lore:\n");

               for (Text var6 : var4.lines()) {
                  var2.append("  - ").append(var6.getString()).append("\n");
               }
            }

            ProfileComponent var11 = var1.get(DataComponentTypes.PROFILE);
            if (var11 != null) {
               var2.append("Profile: ").append(var11.toString()).append("\n");

               try {
                  GameProfile var12 = var11.getGameProfile();
                  if (var12 != null) {
                     var2.append("GameProfile ID: ").append(var12.id()).append("\n");
                     var2.append("GameProfile Name: ").append(var12.name()).append("\n");
                     PropertyMap var7 = var12.properties();
                     if (var7 != null) {
                        for (Property var9 : var7.get("textures")) {
                           var2.append("Texture Value: ").append(var9.value()).append("\n");
                        }
                     }
                  }
               } catch (Throwable var10) {
               }
            }

            NbtComponent var13 = var1.get(DataComponentTypes.CUSTOM_DATA);
            if (var13 != null) {
               var2.append("CustomData: ").append(var13.copyNbt().toString()).append("\n");
            }

            ComponentMap var14 = var1.getComponents();
            if (var14 != null) {
               var2.append("All Components:\n").append(var14.toString()).append("\n");
            }

            String var15 = var2.toString();
            var0.keyboard.setClipboard(var15);
            d("§b§lShelka §7» §aNBT и компоненты предмета §f" + var1.getName().getString() + " §aскопированы в буфер обмена!");
         } else {
            d("§cВозьмите предмет в руку!");
         }
      }
   }

   private static void h(String[] var0) {
      if (var0.length < 2) {
         d("§cИспользование: §f.goto <x> <y> <z> §7| §f.goto <x> <z> §7| §f.goto stop");
      } else if (!var0[1].equalsIgnoreCase("stop") && !var0[1].equalsIgnoreCase("cancel")) {
         if (var0.length >= 4) {
            try {
               int var1 = Integer.parseInt(var0[1]);
               int var2 = Integer.parseInt(var0[2]);
               int var3 = Integer.parseInt(var0[3]);
               NavigationManager.getInstance().a(new GotoTask(new BlockPos(var1, var2, var3)));
               return;
            } catch (NumberFormatException var5) {
            }
         } else if (var0.length == 3) {
            try {
               int var6 = Integer.parseInt(var0[1]);
               int var7 = Integer.parseInt(var0[2]);
               NavigationManager.getInstance().a(new GotoTask(var6, var7));
               return;
            } catch (NumberFormatException var4) {
            }
         }

         d("§cНеверные координаты. Пример: §f.goto 100 64 -200 §7или §f.goto 100 -200");
      } else {
         NavigationManager.getInstance().c();
      }
   }

   private static void i(String[] var0) {
      if (var0.length < 2) {
         d("§cИспользование: §f.mine <x> <y> <z> §7| §f.mine <блок> §7| §f.mine stop");
      } else if (!var0[1].equalsIgnoreCase("stop") && !var0[1].equalsIgnoreCase("cancel")) {
         if (var0.length >= 4) {
            try {
               int var17 = Integer.parseInt(var0[1]);
               int var18 = Integer.parseInt(var0[2]);
               int var19 = Integer.parseInt(var0[3]);
               NavigationManager.getInstance().a(new MineBlockTask(new BlockPos(var17, var18, var19)));
               return;
            } catch (NumberFormatException var16) {
            }
         }

         MinecraftClient var1 = MinecraftClient.getInstance();
         if (var1.player != null && var1.world != null) {
            String var2 = var0[1].toLowerCase(Locale.ROOT);
            BlockPos var3 = var1.player.getBlockPos();
            BlockPos var4 = null;
            double var5 = Double.MAX_VALUE;
            byte var7 = 32;

            for (int var8 = -var7; var8 <= var7; var8++) {
               for (int var9 = -16; var9 <= 16; var9++) {
                  for (int var10 = -var7; var10 <= var7; var10++) {
                     BlockPos var11 = var3.add(var8, var9, var10);
                     BlockState var12 = var1.world.getBlockState(var11);
                     if (!var12.isAir()) {
                        String var13 = Registries.BLOCK.getId(var12.getBlock()).getPath().toLowerCase(Locale.ROOT);
                        if (var13.contains(var2)) {
                           double var14 = var3.getSquaredDistance(var11);
                           if (var14 < var5) {
                              var5 = var14;
                              var4 = var11;
                           }
                        }
                     }
                  }
               }
            }

            if (var4 != null) {
               d("§aНайден блок §f" + var2 + " §aна: §f" + var4.getX() + " " + var4.getY() + " " + var4.getZ());
               NavigationManager.getInstance().a(new MineBlockTask(var4));
            } else {
               d("§cБлок §f" + var2 + " §cне найден поблизости (радиус " + var7 + ")");
            }
         }
      } else {
         NavigationManager.getInstance().c();
      }
   }

   private static void j(String[] var0) {
      if (var0.length < 2) {
         d();
      } else {
         String var1 = var0[1].toLowerCase(Locale.ROOT);
         switch (var1) {
            case "status":
               d();
               break;
            case "stop":
               boolean var10 = false;
               if (NavigationManager.getInstance().getRecorder().isRecording()) {
                  String var12 = NavigationManager.getInstance().getRecorder().b();
                  d("§a" + var12);
                  var10 = true;
               }

               if (NavigationManager.getInstance().d()) {
                  NavigationManager.getInstance().c();
                  var10 = true;
               }

               if (!var10) {
                  d("§7Нет активных задач навигации или записи.");
               }
               break;
            case "record":
            case "rec":
               String var9 = var0.length >= 3 ? var0[2] : "walk";
               String var11 = NavigationManager.getInstance().getRecorder().a(var9);
               if (var11 == null) {
                  d("§aЗапись навигации начата: §f" + var9 + ".csv\n§7Ходите, прыгайте и поворачивайтесь в своём стиле (~10 мин). Для остановки: §f.nav stop");
               } else {
                  d("§c" + var11);
               }
               break;
            case "list":
               List var8 = NavRecorder.d();
               if (var8.isEmpty()) {
                  d("§7Нет записанных датасетов навигации.");
               } else {
                  d("§bДатасеты навигации (" + var8.size() + "):");

                  for (String var6 : (Iterable<String>)(Iterable<?>)(var8)) {
                     d(" §7• §f" + var6);
                  }
               }
               break;
            case "goto":
               String[] var7 = new String[var0.length - 1];
               var7[0] = "goto";
               System.arraycopy(var0, 2, var7, 1, var0.length - 2);
               h(var7);
               break;
            case "mine":
               String[] var4 = new String[var0.length - 1];
               var4[0] = "mine";
               System.arraycopy(var0, 2, var4, 1, var0.length - 2);
               i(var4);
               break;
            default:
               d("§cИспользование: §f.nav status | record [имя] | stop | list | goto | mine");
         }
      }
   }

   private static void k(String[] var0) {
      if (var0.length < 2) {
         d("§cИспользование: §f.neuro bot | record [минуты] [nobot] | stop | train [эпох] | model | status");
      } else {
         switch (var0[1].toLowerCase(Locale.ROOT)) {
            case "status":
               d(NeuroRecorder.INSTANCE.status());
               d("§7Модель: " + NeuroModels.describe());
               d(NeuroTrainer.status());
               d(String.format(
                  Locale.ROOT,
                  "§7Повороты (пакеты): макс §f%.1f° §7yaw, §f%.1f° §7pitch, крупных шагов: §f%d",
                  Aura.dbgMaxYawStep,
                  Aura.dbgMaxPitchStep,
                  Aura.dbgBigSteps
               ));
               d(String.format(
                  Locale.ROOT,
                  "§7Удары: §f%d §7из них без урона: §f%d §7(иммунитет/щит/дистанция) — §f.neuro reset",
                  Aura.dbgAttacks(),
                  Aura.dbgNoDamage()
               ));
               break;
            case "reset":
               Aura.dbgReset();
               d("§7Счётчики поворотов сброшены");
               break;
            case "stop":
               d(NeuroRecorder.INSTANCE.stop("вручную"));
               break;
            case "bot": {
               MinecraftClient var8 = MinecraftClient.getInstance();
               if (var8.player == null || var8.world == null) {
                  d("§cНужно быть в мире");
                  break;
               }

               boolean var9 = FakePlayerManager.getFakePlayers().stream().anyMatch(var1x -> var1x != null && var1x.isAlive());
               if (var9) {
                  d("§7Бот уже есть — он стоит целью для записи. Убрать: §f.fakeplayer clear");
                  break;
               }

               String var10 = var0.length > 2 ? String.join(" ", java.util.Arrays.copyOfRange(var0, 2, var0.length)) : "Target";
               FakePlayerManager.a(var8.player.getEntityPos().add(var8.player.getRotationVector().multiply(3.0)), var10);
               FakePlayerTickHandler.a(true);
               d("§aБот заспавнен и двигается §7(стрейфы, дистанция, прыжки — так в датасет попадут скорость/ускорение/прыжок цели).");
               d("§7Нужен §fплоский мир §7(суперплоский) и выживание; бей бота руками — §fаура должна быть выключена§7. Дальше: §f.neuro record 20");
               break;
            }
            case "train": {
               int var11 = 120;
               if (var0.length > 2) {
                  try {
                     var11 = Integer.parseInt(var0[2]);
                  } catch (NumberFormatException var12) {
                  }
               }

               String var13 = NeuroTrainer.start(var11);
               if (var13 != null) {
                  d("§c" + var13);
               } else {
                  d("§aОбучение запущено в фоне: §f" + var11 + " эпох §7(GRU + MDN NLL). Прогресс: §f.neuro status");
               }
               break;
            }
            case "model": {
               NeuroModels.reload();
               d("§7Модель: " + NeuroModels.describe() + "§7, версия §f" + NeuroModels.revision());
               break;
            }
            case "record":
            case "rec": {
               int var1 = 20;
               boolean var2 = false;

               for (int var3 = 2; var3 < var0.length; var3++) {
                  if (var0[var3].equalsIgnoreCase("nobot")) {
                     var2 = true;
                  } else {
                     try {
                        var1 = Integer.parseInt(var0[var3]);
                     } catch (NumberFormatException var7) {
                     }
                  }
               }

               var1 = Math.max(1, Math.min(var1, 240));
               String var6 = NeuroRecorder.INSTANCE.start(var1);
               if (var6 != null) {
                  d("§c" + var6);
                  break;
               }

               if (!var2) {
                  MinecraftClient var4 = MinecraftClient.getInstance();
                  if (var4.player != null) {
                     boolean var5 = FakePlayerManager.getFakePlayers().stream().anyMatch(var2x -> var2x != null && var2x.isAlive());
                     if (!var5) {
                        FakePlayerManager.a(var4.player.getEntityPos().add(var4.player.getRotationVector().multiply(3.0)), "Target");
                     }
                  }
               }

               d("§aЗапись датасета SpaceTime начата: §f" + var1 + " мин"
                  + (var2 ? " §7(без бота)" : " §7(цель — бот/любая сущность в 6 блоках)")
                  + "\n§7Бей как обычно; аура на паузе, файл: §f" + NeuroRecorder.dir()
                  + "\n§7Остановка: §f.neuro stop§7, статус: §f.neuro status");
               break;
            }
            default:
               d("§cИспользование: §f.neuro bot | record [минуты] [nobot] | stop | train [эпох] | model | status");
         }
      }
   }

   private static void d() {
      NavigationManager var0 = NavigationManager.getInstance();
      if (var0.d()) {
         d("§bНавигация: §aактивна §7(Задача: §f" + var0.getCurrentTask().getName() + "§7, статус: §f" + var0.getCurrentTask().f() + "§7)");
      } else {
         d("§bНавигация: §7не активна");
      }

      NavRecorder var1 = var0.getRecorder();
      if (var1.isRecording()) {
         d(
            "§bРекордер: §aидёт запись «"
               + var1.getCurrentName()
               + "»§7, тиков: §f"
               + var1.getSessionRecordedTicks()
               + " §7("
               + NavRecorder.a(var1.getSessionRecordedTicks())
               + ")"
         );
      } else {
         d("§bРекордер: §7остановлен. Записей в папке: §f" + NavRecorder.d().size());
      }
   }

   public static void d(String var0) {
      MinecraftClient var1 = MinecraftClient.getInstance();
      var1.execute(() -> {
         if (var1.player != null) {
            var1.player.sendMessage(Text.literal(var0), false);
         }
      });
   }

   static {
      COMMANDS.put("help", new CommandManager.b("список команд", List.of()));
      COMMANDS.put("bind", new CommandManager.b("управление биндами клавиш", List.of("<модуль>", "list", "remove", "clear")));
      COMMANDS.put("friend", new CommandManager.b("управление друзьями", List.of("add", "remove", "list", "clear")));
      COMMANDS.put("cfg", new CommandManager.b("управление конфигами", List.of("save", "load", "list", "dir", "delete", "reset")));
      COMMANDS.put("config", new CommandManager.b("управление конфигами", List.of("save", "load", "list", "dir", "delete", "reset")));
      COMMANDS.put("fakeplayer", new CommandManager.b("тренировочный бот", List.of("spawn", "remove", "clear", "move")));
      COMMANDS.put("gps", new CommandManager.b("навигатор к координатам", List.of("<имя> <x> <y> <z>", "zamok", "list", "remove", "clear")));
      COMMANDS.put("rct", new CommandManager.b("переподключение к анархии", List.of("<номер>", "hub", "stop")));
      COMMANDS.put("party", new CommandManager.b("управление пати и метками", List.of("create", "join <код>", "leave", "disband", "list")));
      COMMANDS.put("nbtdump", new CommandManager.b("дамп NBT/компонентов предмета в руке в буфер обмена", List.of()));
      COMMANDS.put("nbt", new CommandManager.b("дамп NBT/компонентов предмета в руке в буфер обмена", List.of()));
      COMMANDS.put("goto", new CommandManager.b("навигация к координатам", List.of("<x> <y> <z>", "<x> <z>", "stop")));
      COMMANDS.put("mine", new CommandManager.b("вскапывание блока по координатам или имени", List.of("<x> <y> <z>", "<блок>", "stop")));
      COMMANDS.put("nav", new CommandManager.b("управление навигацией и рекордером", List.of("status", "record", "stop", "list", "goto", "mine")));
       COMMANDS.put("baritone", new CommandManager.b("управление навигацией Baritone", List.of("status", "record", "stop", "list", "goto", "mine")));
       COMMANDS.put("neuro", new CommandManager.b("нейросеть: бот, запись датасета, обучение GRU, модель", List.of("bot", "record", "stop", "train", "model", "status", "reset")));
      COMMANDS.put("irc", new CommandManager.b("открыть меню IRC и друзей", List.of()));
      COMMANDS.put("gui", new CommandManager.b("режим клик-гуи: cs | panels", List.of("cs", "panels")));
   }

   public record a(int start, List<String> candidates) {
      public int getStart() {
         return this.start;
      }

      public List<String> getCandidates() {
         return this.candidates;
      }
   }

   record b(String description, List<String> sub) {
      public String getDescription() {
         return this.description;
      }

      public List<String> getSub() {
         return this.sub;
      }
   }
}
