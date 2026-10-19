package brokke.cc.nav;

import java.util.List;

public interface NavTask {
   void a();

   void b();

   boolean isFinished();

   void d();

   String getName();

   String f();

   NavGoal getGoal();

   List<PathNode> getPath();
}
