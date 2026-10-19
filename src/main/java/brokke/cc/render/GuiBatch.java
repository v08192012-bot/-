package brokke.cc.render;

import brokke.cc.font.MsdfPipeline;

public final class GuiBatch {
   private static int depth;
   private static GuiBatch.DrawType current;

   private GuiBatch() {
   }

   public static void beginFrame() {
      if (depth != 0) {
         throw new IllegalStateException("GUI batch scope spans multiple frames");
      }

      flush();
      RectPipeline.beginFrame();
      OutlinePipeline.beginFrame();
      TexturePipeline.beginFrame();
      MsdfPipeline.beginFrame();
   }

   public static void begin() {
      depth++;
   }

   public static boolean isActive() {
      return depth > 0;
   }

   public static void select(GuiBatch.DrawType type) {
      if (isActive() && current != type) {
         flush();
         current = type;
      }
   }

   public static void flush() {
      GuiBatch.DrawType pending = current;
      current = null;
      if (pending != null) {
         switch (pending) {
            case RECTANGLE:
               RectPipeline.e();
               break;
            case OUTLINE:
               OutlinePipeline.e();
               break;
            case BLUR:
               BlurPipeline.h();
               break;
            case TEXT:
               MsdfPipeline.flush();
               break;
            case TEXTURE:
               TexturePipeline.flush();
         }
      }
   }

   public static void end() {
      if (depth <= 0) {
         throw new IllegalStateException("GUI batch scope is not open");
      }

      try {
         if (depth == 1) {
            flush();
         }
      } finally {
         depth--;
      }
   }

   public enum DrawType {
      RECTANGLE,
      OUTLINE,
      BLUR,
      TEXT,
      TEXTURE;
   }
}
