package brokke.cc.irc;

import java.text.SimpleDateFormat;
import java.util.Date;

public class IrcMessage {
   private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");
   private final String id;
   private final String senderName;
   private final String senderRole;
   private final String senderUid;
   private final String target;
   private final String text;
   private final IrcMessage.a cords;
   private final long timestamp;
   private final boolean isSelf;

   public IrcMessage(String var1, String var2, String var3, String var4, String var5, String var6, IrcMessage.a var7, long var8, boolean var10) {
      this.id = var1;
      this.senderName = var2;
      this.senderRole = var3 != null ? var3 : "User";
      this.senderUid = var4 != null ? var4 : "0";
      this.target = var5;
      this.text = var6 != null ? var6 : "";
      this.cords = var7;
      this.timestamp = var8;
      this.isSelf = var10;
   }

   public String getId() {
      return this.id;
   }

   public String getSenderName() {
      return this.senderName;
   }

   public String getSenderRole() {
      return this.senderRole;
   }

   public String getSenderUid() {
      return this.senderUid;
   }

   public String getTarget() {
      return this.target;
   }

   public String getText() {
      return this.text;
   }

   public IrcMessage.a getCords() {
      return this.cords;
   }

   public boolean h() {
      return this.cords != null;
   }

   public long getTimestamp() {
      return this.timestamp;
   }

   public boolean isSelf() {
      return this.isSelf;
   }

   public String k() {
      return TIME_FORMAT.format(new Date(this.timestamp));
   }

   public record a(int x, int y, int z, String server, String dimension) {
      public String a() {
         StringBuilder var1 = new StringBuilder();
         var1.append("XYZ: ").append(this.x).append(", ").append(this.y).append(", ").append(this.z);
         if (this.server != null && !this.server.isEmpty()) {
            var1.append(" • ").append(this.server);
         }

         if (this.dimension != null && !this.dimension.isEmpty()) {
            var1.append(" (").append(this.dimension).append(")");
         }

         return var1.toString();
      }

      public int getX() {
         return this.x;
      }

      public int getY() {
         return this.y;
      }

      public int getZ() {
         return this.z;
      }

      public String getServer() {
         return this.server;
      }

      public String getDimension() {
         return this.dimension;
      }
   }
}
