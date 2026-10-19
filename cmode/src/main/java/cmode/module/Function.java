package cmode.module;

import cmode.config.ConfigManager;
import cmode.setting.Setting;
import cmode.system.events.EventBus;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.MinecraftClient;

public abstract class Function {
   protected static final MinecraftClient mc = MinecraftClient.getInstance();
   private final String name;
   private final String desc;
   private final Category category;
   private int keybind;
   private boolean enabled;
   private final List<Setting> settings = new ArrayList<>();

   public Function() {
      if (this.getClass().isAnnotationPresent(NewFunction.class)) {
         NewFunction var1 = this.getClass().getAnnotation(NewFunction.class);
         this.name = var1.name();
         this.desc = var1.description();
         this.category = var1.category();
         this.enabled = false;
      } else {
         throw new RuntimeException("Error " + this.getClass().getSimpleName());
      }
   }

   public void addSettings(Setting... var1) {
      this.settings.addAll(Arrays.asList(var1));
   }

   public void setEnabled(boolean var1) {
      if (this.enabled != var1) {
         this.enabled = var1;
         if (this.enabled) {
            EventBus.register(this);

            try {
               this.onEnable();
            } catch (Exception var4) {
               var4.printStackTrace();
            }
         } else {
            EventBus.unregister(this);

            try {
               this.onDisable();
            } catch (Exception var3) {
               var3.printStackTrace();
            }
         }

         ConfigManager.onModuleChanged();
      }
   }

   public void toggle() {
      this.setEnabled(!this.enabled);
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public void onEnable() {
   }

   public void onDisable() {
   }

   public String getName() {
      return this.name;
   }

   public String getDescription() {
      return this.desc;
   }

   public Category getCategory() {
      return this.category;
   }

   public int getKeybind() {
      return this.keybind;
   }

   public void setKeybind(int var1) {
      this.keybind = var1;
      ConfigManager.onModuleChanged();
   }

   public boolean isActive() {
      return this.enabled;
   }

   public List<Setting> getSettings() {
      return this.settings;
   }
}
