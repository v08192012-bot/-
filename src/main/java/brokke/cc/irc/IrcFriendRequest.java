package brokke.cc.irc;

public class IrcFriendRequest {
   private final String username;
   private final String role;
   private final String uid;
   private final long timestamp;

   public IrcFriendRequest(String var1, String var2, String var3, long var4) {
      this.username = var1;
      this.role = var2 != null ? var2 : "User";
      this.uid = var3 != null ? var3 : "0";
      this.timestamp = var4;
   }

   public String getUsername() {
      return this.username;
   }

   public String getRole() {
      return this.role;
   }

   public String getUid() {
      return this.uid;
   }

   public long getTimestamp() {
      return this.timestamp;
   }
}
