package brokke.cc.manager;

import brokke.cc.core.NickGenerator;
import brokke.cc.mixins.interfaces.IMinecraftClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;

public class AccountManager {
   private static final List<AccountManager.a> accounts = new ArrayList<>();
   private static final Set<String> usedNicknames = ConcurrentHashMap.newKeySet();
   private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("brokke");
   private static final Path FILE = CONFIG_DIR.resolve("accounts.json");
   private static final Path OLD_FILE = FabricLoader.getInstance().getGameDir().resolve("brokke_accounts.json");
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static String activeUsername = null;

   public static void a() {
      e();
   }

   public static List<AccountManager.a> getAccounts() {
      return accounts;
   }

   public static boolean a(String var0) {
      if (var0 != null && !var0.trim().isEmpty()) {
         String var1 = var0.trim().toLowerCase();
         if (usedNicknames.contains(var1)) {
            return true;
         }

         for (AccountManager.a var3 : accounts) {
            if (var3 != null && var3.getUsername() != null && var3.getUsername().equalsIgnoreCase(var0.trim())) {
               return true;
            }
         }

         MinecraftClient var4 = MinecraftClient.getInstance();
         return var4 != null
            && var4.getSession() != null
            && var4.getSession().getUsername() != null
            && var4.getSession().getUsername().equalsIgnoreCase(var0.trim());
      } else {
         return true;
      }
   }

   public static void b(String var0) {
      if (var0 != null && !var0.trim().isEmpty()) {
         usedNicknames.add(var0.trim().toLowerCase());
         f();
      }
   }

   public static String c() {
      return NickGenerator.a();
   }

   public static void c(String var0) {
      if (var0 != null && !var0.trim().isEmpty()) {
         String var1 = var0.trim();
         usedNicknames.add(var1.toLowerCase());

         for (AccountManager.a var3 : accounts) {
            if (var3.getUsername().equalsIgnoreCase(var1)) {
               d(var3.getUsername());
               return;
            }
         }

         accounts.add(0, new AccountManager.a(var1, false));
         d(var1);
         f();
      }
   }

   public static void a(AccountManager.a var0) {
      if (accounts.remove(var0)) {
         if (activeUsername != null && activeUsername.equalsIgnoreCase(var0.getUsername())) {
            activeUsername = accounts.isEmpty() ? null : accounts.get(0).getUsername();
            if (activeUsername != null) {
               d(activeUsername);
            }
         }

         f();
      }
   }

   public static void b(AccountManager.a var0) {
      var0.setFavorite(!var0.isFavorite());
      f();
   }

   public static String getActiveUsername() {
      return activeUsername;
   }

   public static void d(String var0) {
      if (var0 != null && !var0.trim().isEmpty()) {
         String var1 = var0.trim();
         activeUsername = var1;
         f();
         MinecraftClient var2 = MinecraftClient.getInstance();
         if (var2 != null) {
            try {
               UUID var3 = UUID.nameUUIDFromBytes(("OfflinePlayer:" + var1).getBytes(StandardCharsets.UTF_8));
               Session var4 = var2.getSession();
               Object var5 = null;
               if (var4 != null) {
                  for (Method var9 : Session.class.getDeclaredMethods()) {
                     if (var9.getParameterCount() == 0 && var9.getReturnType().isEnum()) {
                        var9.setAccessible(true);

                        try {
                           var5 = var9.invoke(var4);
                           if (var5 != null) {
                              break;
                           }
                        } catch (Exception var19) {
                        }
                     }
                  }

                  if (var5 == null) {
                     for (Field var31 : Session.class.getDeclaredFields()) {
                        if (var31.getType().isEnum()) {
                           var31.setAccessible(true);

                           try {
                              var5 = var31.get(var4);
                              if (var5 != null) {
                                 break;
                              }
                           } catch (Exception var181) {
                           }
                        }
                     }
                  }
               }

               if (var5 == null) {
                  for (Class var32 : Session.class.getDeclaredClasses()) {
                     if (var32.isEnum() && var32.getEnumConstants().length > 0) {
                        var5 = var32.getEnumConstants()[0];
                        break;
                     }
                  }
               }

               Constructor[] var24 = Session.class.getDeclaredConstructors();
               int var27 = var24.length;
               byte var30 = 0;
               if (var30 < var27) {
                  Constructor var33 = var24[var30];
                  var33.setAccessible(true);
                  Class[] var10 = var33.getParameterTypes();
                  Object[] var11 = new Object[var10.length];

                  for (int var12 = 0; var12 < var10.length; var12++) {
                     Class var13 = var10[var12];
                     if (var13 == String.class) {
                        var11[var12] = var12 == 0 ? var1 : "";
                     } else if (var13 == UUID.class) {
                        var11[var12] = var3;
                     } else if (var13 == Optional.class) {
                        var11[var12] = Optional.empty();
                     } else if (var13.isEnum()) {
                        var11[var12] = var5 != null && var13.isInstance(var5)
                           ? var5
                           : (var13.getEnumConstants().length > 0 ? var13.getEnumConstants()[0] : null);
                     }
                  }

                  Session var34 = (Session)var33.newInstance(var11);

                  try {
                     ((IMinecraftClient)var2).setSession(var34);
                  } catch (Throwable var18) {
                     for (Field var17 : MinecraftClient.class.getDeclaredFields()) {
                        if (var17.getType() == Session.class) {
                           var17.setAccessible(true);
                           var17.set(var2, var34);
                           break;
                        }
                     }
                  }
               }
            } catch (Exception var21) {
            }

            NicknameManager.a(var1, var1);
            PartyManager.getInstance().b(var1);
         }
      }
   }

   public static void e() {
      Path var0 = FILE;
      if (!Files.exists(var0) && Files.exists(OLD_FILE)) {
         var0 = OLD_FILE;
      }

      try {
         if (Files.exists(var0)) {
            String var1 = Files.readString(var0, StandardCharsets.UTF_8).trim();
            if (var1.startsWith("{")) {
               AccountManager.b var2 = (AccountManager.b)GSON.fromJson(var1, AccountManager.b.class);
               if (var2 != null) {
                  accounts.clear();
                  if (var2.accounts != null) {
                     accounts.addAll(var2.accounts);
                  }

                  if (var2.usedNicknames != null) {
                     for (String var4 : var2.usedNicknames) {
                        if (var4 != null && !var4.trim().isEmpty()) {
                           usedNicknames.add(var4.trim().toLowerCase());
                        }
                     }
                  }

                  if (var2.selectedUsername != null && !var2.selectedUsername.trim().isEmpty()) {
                     activeUsername = var2.selectedUsername.trim();
                  }
               }
            } else if (var1.startsWith("[")) {
               Type var8 = (new TypeToken<List<AccountManager.a>>() {}).getType();
               List var10 = (List)GSON.fromJson(var1, var8);
               if (var10 != null) {
                  accounts.clear();
                  accounts.addAll(var10);
               }
            }
         }
      } catch (Exception var5) {
      }

      if (accounts.isEmpty()) {
         MinecraftClient var6 = MinecraftClient.getInstance();
         if (var6 != null && var6.getSession() != null) {
            accounts.add(new AccountManager.a(var6.getSession().getUsername(), false));
         }
      }

      for (AccountManager.a var9 : accounts) {
         if (var9 != null && var9.getUsername() != null && !var9.getUsername().trim().isEmpty()) {
            usedNicknames.add(var9.getUsername().trim().toLowerCase());
         }
      }

      if (activeUsername != null && !activeUsername.trim().isEmpty()) {
         usedNicknames.add(activeUsername.trim().toLowerCase());
      }

      if (activeUsername != null && !activeUsername.isEmpty()) {
         d(activeUsername);
      } else if (!accounts.isEmpty()) {
         activeUsername = accounts.get(0).getUsername();
         d(activeUsername);
      }
   }

   public static void f() {
      try {
         if (FILE.getParent() != null) {
            Files.createDirectories(FILE.getParent());
         }

         try (BufferedWriter var0 = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
            AccountManager.b var1 = new AccountManager.b();
            var1.selectedUsername = activeUsername;
            var1.accounts = accounts;
            var1.usedNicknames = new ArrayList<>(usedNicknames);
            GSON.toJson(var1, var0);
         }
      } catch (Exception var5) {
      }
   }

   static {
      Runtime.getRuntime().addShutdownHook(new Thread(AccountManager::f, "Brokke-Account-Save-Hook"));
   }

   public static class a {
      private String username;
      private boolean favorite;

      public a(String var1, boolean var2) {
         this.username = var1;
         this.favorite = var2;
      }

      public String getUsername() {
         return this.username;
      }

      public void setUsername(String var1) {
         this.username = var1;
      }

      public boolean isFavorite() {
         return this.favorite;
      }

      public void setFavorite(boolean var1) {
         this.favorite = var1;
      }
   }

   static class b {
      String selectedUsername;
      List<AccountManager.a> accounts;
      List<String> usedNicknames;
   }
}
