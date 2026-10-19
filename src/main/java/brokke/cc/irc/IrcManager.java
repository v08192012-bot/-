package brokke.cc.irc;

import brokke.cc.manager.CommandManager;
import brokke.cc.manager.FriendManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class IrcManager {
   private static final IrcManager INSTANCE = new IrcManager();
   private String serverUrl;
   private IrcManager.a connectionStatus;
   private int onlineCount;
   private final List<IrcMessage> globalMessages = new CopyOnWriteArrayList<>();
   private final Map<String, List<IrcMessage>> directMessages;
   private final List<IrcFriend> friends = new CopyOnWriteArrayList<>();
   private final List<IrcFriendRequest> incomingRequests = new CopyOnWriteArrayList<>();
   private final List<IrcFriendRequest> outgoingRequests = new CopyOnWriteArrayList<>();
   private String activeChat;
   private int unreadGlobal;
   private final Map<String, Integer> unreadDirect;

   public static IrcManager getInstance() {
      return INSTANCE;
   }

   private IrcManager() {
      this.directMessages = new ConcurrentHashMap<>();
      this.unreadDirect = new ConcurrentHashMap<>();
      this.connectionStatus = IrcManager.a.DISCONNECTED;
      this.serverUrl = "внешняя сеть отключена";
      this.activeChat = "global";
   }

   public void c() {
   }

   public String getServerUrl() {
      return this.serverUrl;
   }

   public void a(String var1) {
      CommandManager.d("§7эта функция удалена из локальной сборки: внешние соединения отключены");
   }

   public IrcManager.a getConnectionStatus() {
      return this.connectionStatus;
   }

   public void a(IrcManager.a var1) {
      this.connectionStatus = var1;
      if (var1 == IrcManager.a.DISCONNECTED) {
         this.onlineCount = 0;
      } else if (var1 == IrcManager.a.CONNECTED && this.onlineCount <= 0) {
         this.onlineCount = 1;
      }
   }

   public int getOnlineCount() {
      return this.onlineCount;
   }

   public void setOnlineCount(int var1) {
      this.onlineCount = var1;
   }

   public String getActiveChat() {
      return this.activeChat;
   }

   public void b(String var1) {
      this.activeChat = var1;
      if ("global".equalsIgnoreCase(var1)) {
         this.unreadGlobal = 0;
      } else {
         this.unreadDirect.put(var1.toLowerCase(), 0);

         for (IrcFriend var3 : this.friends) {
            if (var3.getUsername().equalsIgnoreCase(var1)) {
               var3.g();
            }
         }
      }
   }

   public List<IrcMessage> getGlobalMessages() {
      return this.globalMessages;
   }

   public List<IrcMessage> c(String var1) {
      return this.directMessages.computeIfAbsent(var1.toLowerCase(), var0 -> new CopyOnWriteArrayList<>());
   }

   public List<IrcFriend> getFriends() {
      return this.friends;
   }

   public List<IrcFriendRequest> getIncomingRequests() {
      return this.incomingRequests;
   }

   public List<IrcFriendRequest> getOutgoingRequests() {
      return this.outgoingRequests;
   }

   public void clearGlobalMessages() {
      this.globalMessages.clear();
   }

   public void d(String var1) {
      this.c(var1).clear();
   }

   public void a(IrcMessage var1, boolean var2) {
      this.globalMessages.add(var1);
      if (this.globalMessages.size() > 500) {
         this.globalMessages.removeFirst();
      }

      if (var2 && !"global".equalsIgnoreCase(this.activeChat)) {
         this.unreadGlobal++;
      }
   }

   public void a(String var1, IrcMessage var2, boolean var3) {
      String var4 = var1.toLowerCase();
      List var5 = this.c(var4);
      var5.add(var2);
      if (var5.size() > 300) {
         var5.removeFirst();
      }

      for (IrcFriend var7 : this.friends) {
         if (var7.getUsername().equalsIgnoreCase(var1)) {
            String var8 = var2.h() ? "Координаты" : var2.getText();
            var7.setLastMessage(var8);
            if (var3 && !var1.equalsIgnoreCase(this.activeChat)) {
               var7.f();
               this.unreadDirect.put(var4, var7.getUnreadCount());
            }
            break;
         }
      }
   }

   public void a(JsonArray var1, JsonArray var2, JsonArray var3) {
      this.friends.clear();
      if (var1 != null) {
         for (JsonElement var5 : var1) {
            JsonObject var6 = var5.getAsJsonObject();
            String var7 = var6.get("username").getAsString();
            String var8 = var6.has("role") ? var6.get("role").getAsString() : "User";
            String var9 = var6.has("uid") ? var6.get("uid").getAsString() : "0";
            boolean var10 = var6.has("isOnline") && var6.get("isOnline").getAsBoolean();
            IrcFriend var11 = new IrcFriend(var7, var8, var9, var10);
            int var12 = this.unreadDirect.getOrDefault(var7.toLowerCase(), 0);
            var11.setUnreadCount(var12);
            List var13 = this.c(var7);
            if (!var13.isEmpty()) {
               IrcMessage var14 = (IrcMessage)var13.getLast();
               var11.setLastMessage(var14.h() ? "Координаты" : var14.getText());
            }

            this.friends.add(var11);
            FriendManager.b(var7);
         }
      }

      this.incomingRequests.clear();
      if (var2 != null) {
         for (JsonElement var17 : var2) {
            JsonObject var19 = var17.getAsJsonObject();
            String var21 = var19.get("username").getAsString();
            String var23 = var19.has("role") ? var19.get("role").getAsString() : "User";
            String var25 = var19.has("uid") ? var19.get("uid").getAsString() : "0";
            long var27 = var19.has("timestamp") ? var19.get("timestamp").getAsLong() : System.currentTimeMillis();
            this.incomingRequests.add(new IrcFriendRequest(var21, var23, var25, var27));
         }
      }

      this.outgoingRequests.clear();
      if (var3 != null) {
         for (JsonElement var18 : var3) {
            JsonObject var20 = var18.getAsJsonObject();
            String var22 = var20.get("username").getAsString();
            String var24 = var20.has("role") ? var20.get("role").getAsString() : "User";
            String var26 = var20.has("uid") ? var20.get("uid").getAsString() : "0";
            long var28 = var20.has("timestamp") ? var20.get("timestamp").getAsLong() : System.currentTimeMillis();
            this.outgoingRequests.add(new IrcFriendRequest(var22, var24, var26, var28));
         }
      }
   }

   public void a(JsonArray var1) {
      this.a(var1, null, null);
   }

   public void a(String var1, boolean var2) {
      for (IrcFriend var4 : this.friends) {
         if (var4.getUsername().equalsIgnoreCase(var1)) {
            var4.setIsOnline(var2);
            break;
         }
      }
   }

   public void e(String var1) {
      CommandManager.d("§7эта функция удалена из локальной сборки: внешние соединения отключены");
   }

   public void m() {
      CommandManager.d("§7эта функция удалена из локальной сборки: внешние соединения отключены");
   }

   public void f(String var1) {
      CommandManager.d("§7эта функция удалена из локальной сборки: внешние соединения отключены");
   }

   public void g(String var1) {
      this.f(var1);
   }

   public void h(String var1) {
      CommandManager.d("§7эта функция удалена из локальной сборки: внешние соединения отключены");
   }

   public void i(String var1) {
      CommandManager.d("§7эта функция удалена из локальной сборки: внешние соединения отключены");
   }

   public void j(String var1) {
      CommandManager.d("§7эта функция удалена из локальной сборки: внешние соединения отключены");
   }

   public int getUnreadGlobal() {
      return this.unreadGlobal;
   }

   public int o() {
      int var1 = this.unreadGlobal;

      for (IrcFriend var3 : this.friends) {
         var1 += var3.getUnreadCount();
      }

      return var1;
   }

   public void p() {
      CommandManager.d("§7эта функция удалена из локальной сборки: внешние соединения отключены");
   }

   public enum a {
      DISCONNECTED,
      CONNECTING,
      CONNECTED;
   }
}
