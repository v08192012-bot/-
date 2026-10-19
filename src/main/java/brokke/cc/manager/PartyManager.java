package brokke.cc.manager;

import brokke.cc.core.User;
import brokke.cc.nav.PartyMarker;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

public class PartyManager {
   private static final PartyManager INSTANCE = new PartyManager();
   static final MinecraftClient mc = MinecraftClient.getInstance();
   private final Map<String, PartyMarker> markers = new ConcurrentHashMap<>();
   private String serverAddress = r();
   String currentPartyCode = null;
   private String currentPartyOwner = null;
   private final List<String> currentPartyMembers = new CopyOnWriteArrayList<>();
   private String lastKnownUsername = null;

   public static PartyManager getInstance() {
      return INSTANCE;
   }

   private static String r() {
      return "внешняя сеть отключена";
   }

   public void a(String var1) {
      if (var1 != null && !var1.isBlank()) {
         this.serverAddress = var1.trim();
      }
   }

   public String getServerAddress() {
      return this.serverAddress;
   }

   public String getCurrentPartyCode() {
      return this.currentPartyCode;
   }

   public String getCurrentPartyOwner() {
      return this.currentPartyOwner;
   }

   public List<String> getCurrentPartyMembers() {
      return this.currentPartyMembers;
   }

   public boolean f() {
      return this.currentPartyCode != null && !this.currentPartyCode.isBlank();
   }

   public boolean g() {
      return false;
   }

   public String h() {
      String var1 = User.getInstance().getUsername();
      if (var1 != null && !var1.isBlank()) {
         return var1;
      }

      if (mc != null) {
         if (mc.player != null && mc.player.getName() != null && !mc.player.getName().getString().isBlank()) {
            return mc.player.getName().getString();
         }

         if (mc.getSession() != null && mc.getSession().getUsername() != null && !mc.getSession().getUsername().isBlank()) {
            return mc.getSession().getUsername();
         }
      }

      return "User";
   }

   public void i() {
      String var1 = this.h();
      if (this.lastKnownUsername == null) {
         this.lastKnownUsername = var1;
      } else if (!this.lastKnownUsername.equalsIgnoreCase(var1)) {
         this.b(var1);
      }
   }

   public void b(String var1) {
      CommandManager.d("§7эта функция удалена из локальной сборки: внешние соединения отключены");
   }

   public void a(PartyMarker var1) {
      if (var1 == null) {
         this.markers.remove("local");
         this.e("local");
      } else {
         this.markers.put("local", var1);
         this.c(var1);
      }
   }

   public PartyMarker j() {
      return this.markers.get("local");
   }

   public void k() {
      this.markers.remove("local");
      this.e("local");
   }

   public void b(PartyMarker var1) {
      if (var1 != null && var1.getId() != null) {
         this.markers.put(var1.getId(), var1);
      }
   }

   public void c(String var1) {
      if (var1 != null) {
         this.markers.remove(var1);
      }
   }

   public void clearMarkers() {
      this.markers.clear();
   }

   public Collection<PartyMarker> m() {
      return this.markers.values();
   }

   public void n() {
      this.a(() -> {
         JsonObject var1 = new JsonObject();
         var1.addProperty("type", "create");
         var1.addProperty("username", this.h());
         this.a(var1);
      });
   }

   public void d(String var1) {
      if (var1 != null && !var1.isBlank()) {
         this.a(() -> {
            JsonObject var2 = new JsonObject();
            var2.addProperty("type", "join");
            var2.addProperty("code", var1.toUpperCase().trim());
            var2.addProperty("username", this.h());
            this.a(var2);
         });
      } else {
         CommandManager.d("§cУкажите код команды: §f.party join <код>");
      }
   }

   public void o() {
      if (!this.f()) {
         CommandManager.d("§cВы не состоите ни в какой команде!");
      } else if (this.g()) {
         CommandManager.d("§cВладелец не может покинуть команду! Используйте §f.party disband");
      } else {
         this.a(() -> {
            JsonObject var1 = new JsonObject();
            var1.addProperty("type", "leave");
            this.a(var1);
         });
      }
   }

   public void p() {
      if (!this.f()) {
         CommandManager.d("§cВы не состоите ни в какой команде!");
      } else if (!this.g()) {
         CommandManager.d("§cТолько владелец может распустить команду!");
      } else {
         this.a(() -> {
            JsonObject var1 = new JsonObject();
            var1.addProperty("type", "disband");
            this.a(var1);
         });
      }
   }

   public void q() {
      if (!this.f()) {
         CommandManager.d("§cВы не состоите ни в какой команде!");
      } else {
         this.a(() -> {
            JsonObject var1 = new JsonObject();
            var1.addProperty("type", "list");
            this.a(var1);
         });
      }
   }

   public void c(PartyMarker var1) {
      if (this.f() && var1 != null) {
         this.a(() -> {
            JsonObject var2 = new JsonObject();
            var2.addProperty("type", "marker_set");
            JsonObject var3 = new JsonObject();
            var3.addProperty("id", this.h());
            var3.addProperty("sender", this.h());
            var3.addProperty("isPlayer", var1.isPlayerTarget());
            if (var1.getTargetPlayerUuid() != null) {
               var3.addProperty("targetUuid", var1.getTargetPlayerUuid().toString());
            }

            var3.addProperty("label", var1.getLabel());
            Vec3d var4 = var1.getStaticPos();
            if (var4 != null) {
               var3.addProperty("x", var4.x);
               var3.addProperty("y", var4.y);
               var3.addProperty("z", var4.z);
            }

            var3.addProperty("createdAt", var1.getCreatedAt());
            var2.add("marker", var3);
            this.a(var2);
         });
      }
   }

   public void e(String var1) {
      if (this.f()) {
         this.a(() -> {
            JsonObject var1x = new JsonObject();
            var1x.addProperty("type", "marker_remove");
            var1x.addProperty("id", this.h());
            this.a(var1x);
         });
      }
   }

   private void a(Runnable var1) {
      CommandManager.d("§7эта функция удалена из локальной сборки: внешние соединения отключены");
   }

   void a(JsonObject var1) {
      CommandManager.d("§7эта функция удалена из локальной сборки: внешние соединения отключены");
   }

   void f(String var1) {
      try {
         JsonObject var2 = JsonParser.parseString(var1).getAsJsonObject();
         String var3 = var2.has("type") ? var2.get("type").getAsString() : "";
         switch (var3) {
            case "party_created":
               this.currentPartyCode = var2.get("code").getAsString();
               this.currentPartyOwner = var2.get("owner").getAsString();
               this.currentPartyMembers.clear();
               this.currentPartyMembers.add(this.currentPartyOwner);
               CommandManager.d("§b§lParty §7» §fКоманда создана! Код: §e" + this.currentPartyCode);
               this.s();
               break;
            case "party_joined":
               String var18 = var2.get("code").getAsString();
               boolean var19 = this.f() && var18.equalsIgnoreCase(this.currentPartyCode);
               this.currentPartyCode = var18;
               this.currentPartyOwner = var2.get("owner").getAsString();
               this.currentPartyMembers.clear();
               if (var2.has("members")) {
                  for (JsonElement var22 : var2.getAsJsonArray("members")) {
                     this.currentPartyMembers.add(var22.getAsString());
                  }
               }

               if (var2.has("markers")) {
                  for (JsonElement var23 : var2.getAsJsonArray("markers")) {
                     this.b(var23.getAsJsonObject());
                  }
               }

               if (!var19) {
                  CommandManager.d("§b§lParty §7» §fВы успешно вошли в команду §e" + this.currentPartyCode);
               }

               this.s();
               break;
            case "member_joined":
               String var17 = var2.get("username").getAsString();
               if (!this.currentPartyMembers.contains(var17)) {
                  this.currentPartyMembers.add(var17);
               }

               CommandManager.d("§b§lParty §7» §fИгрок §b" + var17 + "§f вошёл в команду");
               break;
            case "member_left":
               String var16 = var2.get("username").getAsString();
               this.currentPartyMembers.remove(var16);
               this.markers.remove(var16);
               CommandManager.d("§b§lParty §7» §fИгрок §b" + var16 + "§f покинул команду");
               break;
            case "party_left":
               CommandManager.d("§b§lParty §7» §fВы вышли из команды");
               this.t();
               break;
            case "party_disbanded":
               String var15 = var2.has("message") ? var2.get("message").getAsString() : "Команда распущена";
               CommandManager.d("§b§lParty §7» §c" + var15);
               this.t();
               break;
            case "marker_update":
               if (var2.has("marker")) {
                  this.b(var2.getAsJsonObject("marker"));
               }
               break;
            case "marker_removed":
               String var14 = var2.get("id").getAsString();
               this.markers.remove(var14);
               break;
            case "party_info":
               String var13 = var2.get("code").getAsString();
               String var7 = var2.get("owner").getAsString();
               JsonArray var8 = var2.getAsJsonArray("members");
               CopyOnWriteArrayList var9 = new CopyOnWriteArrayList();

               for (JsonElement var11 : var8) {
                  var9.add(var11.getAsString());
               }

               CommandManager.d("§b§lParty §7» §fКоманда: §e" + var13 + " §7| Владелец: §a" + var7 + " §7| Участники: §b" + String.join(", ", var9));
               break;
            case "error":
               String var6 = var2.get("message").getAsString();
               CommandManager.d("§c[Party] " + var6);
         }
      } catch (Exception var12) {
         var12.printStackTrace();
      }
   }

   private void s() {
      PartyMarker var1 = this.j();
      if (var1 != null) {
         this.c(var1);
      }
   }

   private void b(JsonObject var1) {
      String var2 = var1.get("id").getAsString();
      String var3 = var1.has("sender") ? var1.get("sender").getAsString() : var2;
      if (!var3.equalsIgnoreCase(this.h())) {
         boolean var4 = var1.has("isPlayer") && var1.get("isPlayer").getAsBoolean();
         UUID var5 = null;
         if (var1.has("targetUuid") && !var1.get("targetUuid").isJsonNull()) {
            try {
               var5 = UUID.fromString(var1.get("targetUuid").getAsString());
            } catch (Exception var16) {
            }
         }

         String var6 = var1.has("label") ? var1.get("label").getAsString() : var3;
         double var7 = var1.has("x") ? var1.get("x").getAsDouble() : 0.0;
         double var9 = var1.has("y") ? var1.get("y").getAsDouble() : 0.0;
         double var11 = var1.has("z") ? var1.get("z").getAsDouble() : 0.0;
         long var13 = var1.has("createdAt") ? var1.get("createdAt").getAsLong() : System.currentTimeMillis();
         PartyMarker var15 = new PartyMarker(var2, var3, var4, var5, var6, new Vec3d(var7, var9, var11), var13);
         this.markers.put(var2, var15);
      }
   }

   private void t() {
      this.currentPartyCode = null;
      this.currentPartyOwner = null;
      this.currentPartyMembers.clear();
      this.markers.entrySet().removeIf(var0 -> !var0.getKey().equals("local"));
   }
}
