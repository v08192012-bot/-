package brokke.cc.font;

public class MsdfGlyph {
   public final int codepoint;
   public final float advance;
   public final float x;
   public final float y;
   public final float width;
   public final float height;
   public final float u0;
   public final float v0;
   public final float u1;
   public final float v1;
   public final float bearingX;
   public final float bearingY;

   public MsdfGlyph(
      int var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11, float var12
   ) {
      this.codepoint = var1;
      this.advance = var2;
      this.x = var3;
      this.y = var4;
      this.width = var5;
      this.height = var6;
      this.u0 = var7;
      this.v0 = var8;
      this.u1 = var9;
      this.v1 = var10;
      this.bearingX = var11;
      this.bearingY = var12;
   }
}
