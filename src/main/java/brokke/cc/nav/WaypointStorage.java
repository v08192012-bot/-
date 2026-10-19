package brokke.cc.nav;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class WaypointStorage {
   private static final WaypointStorage INSTANCE = new WaypointStorage();
   private final List<Waypoint> waypoints = new CopyOnWriteArrayList<>();

   public static WaypointStorage getInstance() {
      return INSTANCE;
   }

   public void a(Waypoint var1) {
      if (var1 != null) {
         this.a(var1.getName());
         this.waypoints.add(var1);
      }
   }

   public void b(Waypoint var1) {
      this.a(var1);
   }

   public boolean a(String var1) {
      return var1 == null ? false : this.waypoints.removeIf(var1xx -> var1xx.getName() != null && var1xx.getName().equalsIgnoreCase(var1));
   }

   public void clearWaypoints() {
      this.waypoints.clear();
   }

   public boolean c() {
      return this.waypoints.isEmpty();
   }

   public List<Waypoint> getWaypoints() {
      return this.waypoints;
   }
}
