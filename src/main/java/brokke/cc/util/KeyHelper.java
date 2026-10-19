package brokke.cc.util;

import org.lwjgl.glfw.GLFW;

public class KeyHelper {
   public static String a(int var0) {
      if (var0 == -1 || var0 == 0) {
         return "None";
      }

      if (var0 < 0) {
         return "Mouse" + (-var0 - 100);
      }

      if (var0 >= 65 && var0 <= 90) {
         return String.valueOf((char)var0);
      }

      if (var0 >= 48 && var0 <= 57) {
         return String.valueOf((char)var0);
      }

      return switch (var0) {
         case 32 -> "Space";
         default -> {
            String var1 = GLFW.glfwGetKeyName(var0, 0);
            yield var1 == null ? "Key " + var0 : var1.toUpperCase();
         }
         case 39 -> "'";
         case 44 -> ",";
         case 45 -> "-";
         case 46 -> ".";
         case 47 -> "/";
         case 59 -> ";";
         case 61 -> "=";
         case 91 -> "[";
         case 92 -> "\\";
         case 93 -> "]";
         case 96 -> "`";
         case 256 -> "Esc";
         case 257 -> "Enter";
         case 258 -> "Tab";
         case 259 -> "Backspace";
         case 260 -> "Ins";
         case 261 -> "Del";
         case 262 -> "Right";
         case 263 -> "Left";
         case 264 -> "Down";
         case 265 -> "Up";
         case 266 -> "PgUp";
         case 267 -> "PgDn";
         case 268 -> "Home";
         case 269 -> "End";
         case 280 -> "Caps";
         case 281 -> "ScrLk";
         case 282 -> "NumLk";
         case 283 -> "Print";
         case 284 -> "Pause";
         case 290 -> "F1";
         case 291 -> "F2";
         case 292 -> "F3";
         case 293 -> "F4";
         case 294 -> "F5";
         case 295 -> "F6";
         case 296 -> "F7";
         case 297 -> "F8";
         case 298 -> "F9";
         case 299 -> "F10";
         case 300 -> "F11";
         case 301 -> "F12";
         case 320 -> "Num 0";
         case 321 -> "Num 1";
         case 322 -> "Num 2";
         case 323 -> "Num 3";
         case 324 -> "Num 4";
         case 325 -> "Num 5";
         case 326 -> "Num 6";
         case 327 -> "Num 7";
         case 328 -> "Num 8";
         case 329 -> "Num 9";
         case 330 -> "Num .";
         case 331 -> "Num /";
         case 332 -> "Num *";
         case 333 -> "Num -";
         case 334 -> "Num +";
         case 335 -> "NumEnt";
         case 336 -> "Num =";
         case 340 -> "L.Shift";
         case 341 -> "L.Ctrl";
         case 342 -> "L.Alt";
         case 344 -> "R.Shift";
         case 345 -> "R.Ctrl";
         case 346 -> "R.Alt";
         case 348 -> "Menu";
      };
   }
}
