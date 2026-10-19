package brokke.cc.cosmetic;

import java.util.ArrayList;
import java.util.List;

public class GeoBone {
   public GeoBone parent;
   public List<GeoBone> childBones = new ArrayList<>();
   public List<GeoCube> childCubes = new ArrayList<>();
   public String name;
   public boolean isHidden = false;
   public float rotationPointX;
   public float rotationPointY;
   public float rotationPointZ;
   private float rotateX;
   private float rotateY;
   private float rotateZ;
   private float positionX;
   private float positionY;
   private float positionZ;
   private float scaleX = 1.0F;
   private float scaleY = 1.0F;
   private float scaleZ = 1.0F;

   public GeoBone(String var1) {
      this.name = var1;
   }

   public float getRotateX() {
      return this.rotateX;
   }

   public float getRotateY() {
      return this.rotateY;
   }

   public float getRotateZ() {
      return this.rotateZ;
   }

   public void setRotateX(float var1) {
      this.rotateX = var1;
   }

   public void setRotateY(float var1) {
      this.rotateY = var1;
   }

   public void setRotateZ(float var1) {
      this.rotateZ = var1;
   }

   public float getPositionX() {
      return this.positionX;
   }

   public float getPositionY() {
      return this.positionY;
   }

   public float getPositionZ() {
      return this.positionZ;
   }

   public void setPositionX(float var1) {
      this.positionX = var1;
   }

   public void setPositionY(float var1) {
      this.positionY = var1;
   }

   public void setPositionZ(float var1) {
      this.positionZ = var1;
   }

   public float getScaleX() {
      return this.scaleX;
   }

   public float getScaleY() {
      return this.scaleY;
   }

   public float getScaleZ() {
      return this.scaleZ;
   }

   public void setScaleX(float var1) {
      this.scaleX = var1;
   }

   public void setScaleY(float var1) {
      this.scaleY = var1;
   }

   public void setScaleZ(float var1) {
      this.scaleZ = var1;
   }

   public float getRotationPointX() {
      return this.rotationPointX;
   }

   public float getRotationPointY() {
      return this.rotationPointY;
   }

   public float getRotationPointZ() {
      return this.rotationPointZ;
   }

   public void setRotationPointX(float var1) {
      this.rotationPointX = var1;
   }

   public void setRotationPointY(float var1) {
      this.rotationPointY = var1;
   }

   public void setRotationPointZ(float var1) {
      this.rotationPointZ = var1;
   }

   public String getName() {
      return this.name;
   }

   public boolean isHidden() {
      return this.isHidden;
   }

   public void setIsHidden(boolean var1) {
      this.isHidden = var1;
   }
}
