package brokke.cc.config;

import brokke.cc.core.InvBypassMode;
import brokke.cc.core.SwapItem;

public class ModConfig {
   public InvBypassMode invBypassMode = InvBypassMode.a;
   public boolean inventoryStopMoving = false;
   public boolean inventoryUpdateSlot = false;
   public boolean autoSwapEnabled = false;
   public SwapItem firstItem = SwapItem.d;
   public SwapItem secondItem = SwapItem.c;
   public int swapKey = -1;
   public int cerberusKey = -1;
   public boolean autoTotemEnabled = false;
   public float maxHealth = 4.0F;
   public float autoTotemElytraHealth = 4.0F;
   public float autoTotemResetTime = 300.0F;
   public boolean triggerCrystal = true;
   public boolean triggerTnt = true;
   public boolean triggerTrident = false;
   public boolean triggerMinecartTnt = true;
   public float tntDistance = 8.0F;
   public float tridentDistance = 8.0F;
   public boolean hwHelperEnabled = false;
   public int explosiveTrapKey = -1;
   public int trapKey = -1;
   public int stunKey = -1;
   public int explosiveThingKey = -1;
   public int snowballKey = -1;
   public boolean clickPearlEnabled = false;
   public int clickPearlKey = -1;
   public boolean elytraHelperEnabled = false;
   public int elytraSwapKey = -1;
   public int elytraFireworkKey = -1;
   public boolean noPushEnabled = false;
   public boolean noPushWater = true;
   public boolean noPushBlocks = true;
   public boolean noPushEntities = true;
   public boolean noInteractEnabled = false;
   public boolean noEntityTraceEnabled = false;
   public int noEntityTraceKey = -1;
}
