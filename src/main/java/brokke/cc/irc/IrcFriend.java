package brokke.cc.irc;

public class IrcFriend {
   private final String username;
   private String role;
   private String uid;
   private boolean isOnline;
   private int unreadCount;
   private String lastMessage;

   public IrcFriend(String var1, String var2, String var3, boolean var4) {
      this.username = var1;
      this.role = var2 != null ? var2 : "User";
      this.uid = var3 != null ? var3 : "0";
      this.isOnline = var4;
      this.unreadCount = 0;
      this.lastMessage = "";
   }

   public String getUsername() {
      return this.username;
   }

   public String getRole() {
      return this.role;
   }

   public void setRole(String var1) {
      this.role = var1;
   }

   public String getUid() {
      return this.uid;
   }

   public void setUid(String var1) {
      this.uid = var1;
   }

   public boolean isOnline() {
      return this.isOnline;
   }

   public void setIsOnline(boolean var1) {
      this.isOnline = var1;
   }

   public int getUnreadCount() {
      return this.unreadCount;
   }

   public void setUnreadCount(int var1) {
      this.unreadCount = var1;
   }

   public void f() {
      this.unreadCount++;
   }

   public void g() {
      this.unreadCount = 0;
   }

   public String getLastMessage() {
      return this.lastMessage;
   }

   public void setLastMessage(String var1) {
      this.lastMessage = var1;
   }
}
