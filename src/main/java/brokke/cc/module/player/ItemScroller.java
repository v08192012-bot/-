/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  brokke.cc.module.Category
 *  brokke.cc.module.Function
 *  brokke.cc.module.NewFunction
 *  brokke.cc.setting.BooleanSetting
 *  brokke.cc.setting.NumberSetting
 *  brokke.cc.setting.Setting
 *  brokke.cc.system.events.EventHandler
 *  brokke.cc.system.events.impl.EventClickSlot
 *  net.minecraft.client.util.InputUtil
 *  net.minecraft.client.util.Window
 *  net.minecraft.entity.player.PlayerEntity
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.Items
 *  net.minecraft.screen.slot.Slot
 *  net.minecraft.screen.slot.SlotActionType
 */
package brokke.cc.module.player;


import brokke.cc.module.FunctionManager;
import brokke.cc.module.Category;
import brokke.cc.module.Function;
import brokke.cc.module.NewFunction;
import brokke.cc.setting.BooleanSetting;
import brokke.cc.setting.NumberSetting;
import brokke.cc.setting.Setting;
import brokke.cc.system.events.EventHandler;
import brokke.cc.system.events.impl.EventClickSlot;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

@NewFunction(name="Item Scroller", description="\u0423\u0431\u0438\u0440\u0430\u0435\u0442 \u0437\u0430\u0434\u0435\u0440\u0436\u043a\u0443 \u043d\u0430 \u043f\u0435\u0440\u0435\u0442\u0430\u0441\u043a\u0438\u0432\u0430\u043d\u0438\u0435 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u043e\u0432 \u0438 \u0434\u043e\u0431\u0430\u0432\u043b\u044f\u0435\u0442 \u043c\u0430\u0441\u0441\u043e\u0432\u043e\u0435 \u0432\u044b\u0431\u0440\u0430\u0441\u044b\u0432\u0430\u043d\u0438\u0435", category=Category.PLAYER)
public class ItemScroller
extends Function {
    public static final NumberSetting delay = new NumberSetting("\u0417\u0430\u0434\u0435\u0440\u0436\u043a\u0430", 80.0, 30.0, 200.0, 1.0);
    public static final BooleanSetting throwAllEnabled = new BooleanSetting("ThrowAll (Shift+Ctrl+Q)", true);
    public static final BooleanSetting scrollEnabled = new BooleanSetting("Shift+Drag \u0441\u043a\u0440\u043e\u043b\u043b", true);
    private boolean pauseListening = false;

    public ItemScroller() {
        this.addSettings(new Setting[]{delay, throwAllEnabled, scrollEnabled});
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @EventHandler
    public void onClick(EventClickSlot var1) {
        if (ItemScroller.mc.player == null || ItemScroller.mc.interactionManager == null) {
            this.pauseListening = false;
            return;
        }
        if (throwAllEnabled.getValue() && this.l() && this.m() && var1.getActionType() == SlotActionType.THROW && !this.pauseListening && ItemScroller.mc.currentScreen != null && ItemScroller.mc.player.currentScreenHandler != null) {
            if (var1.getSlotId() < 0 || var1.getSlotId() >= ItemScroller.mc.player.currentScreenHandler.slots.size()) {
                return;
            }
            ItemStack var2 = ((Slot)ItemScroller.mc.player.currentScreenHandler.slots.get(var1.getSlotId())).getStack();
            if (var2 == null || var2.isEmpty() || var2.getItem() == Items.AIR) {
                return;
            }
            Item var3 = var2.getItem();
            this.pauseListening = true;
            try {
                for (int var4 = 0; var4 < ItemScroller.mc.player.currentScreenHandler.slots.size(); ++var4) {
                    if (((Slot)ItemScroller.mc.player.currentScreenHandler.slots.get(var4)).getStack().getItem() != var3 || var4 == var1.getSlotId()) continue;
                    ItemScroller.mc.interactionManager.clickSlot(ItemScroller.mc.player.currentScreenHandler.syncId, var4, 1, SlotActionType.THROW, (PlayerEntity)ItemScroller.mc.player);
                }
            }
            finally {
                this.pauseListening = false;
            }
        }
    }

    private boolean l() {
        return this.b(340) || this.b(344);
    }

    private boolean m() {
        return this.b(341) || this.b(345);
    }

    public boolean b(int var1) {
        return var1 < 10 ? false : mc != null && mc.getWindow() != null && InputUtil.isKeyPressed((Window)mc.getWindow(), (int)var1);
    }

    public static long k() {
        double var0 = delay.getValue();
        return (long)ThreadLocalRandom.current().nextDouble(Math.max(0.0, var0 - 3.0), var0 + 1.0);
    }
}
