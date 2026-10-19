package cmode.module;

import cmode.module.render.Arrows;
import cmode.module.render.FriendsMarkers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class FunctionManager {
   private static final List<Function> functions = new ArrayList<>();
   private static final Map<Class<? extends Function>, Function> BY_CLASS = new HashMap<>();

   private FunctionManager() {
   }

   public static void a() {
      if (!functions.isEmpty()) {
         return;
      }

      a(new Arrows(), new FriendsMarkers());
   }

   private static void a(Function... var0) {
      functions.addAll(Arrays.asList(var0));

      for (Function var2 : var0) {
         BY_CLASS.put(var2.getClass(), var2);
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
