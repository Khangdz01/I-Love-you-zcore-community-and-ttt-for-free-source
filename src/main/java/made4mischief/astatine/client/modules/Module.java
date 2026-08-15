package made4mischief.astatine.client.modules;

import java.util.ArrayList;
import java.util.List;
import made4mischief.astatine.client.hud.NotificationRenderer;
import made4mischief.astatine.client.setting.ActionSetting;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.ColorSetting;
import made4mischief.astatine.client.setting.KeybindSetting;
import made4mischief.astatine.client.setting.ModeSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import made4mischief.astatine.client.setting.Setting;
import made4mischief.astatine.client.setting.StringSetting;
import made4mischief.astatine.loader.api.event.EventManager;
import made4mischief.astatine.loader.api.event.Listenable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public abstract class Module implements Listenable {
   protected static final MinecraftClient mc = MinecraftClient.getInstance();
   private final String name;
   private final Category category;
   private final String description;
   private boolean enabled;
   private int keybind;
   private final boolean isNew;
   private final List<Setting> settings = new ArrayList<>();

   public Module(String name, Category category){
      this(name, category, "", -1);
   }

   public Module(String name, Category category, String description){
      this(name, category, description, -1);
   }

   public Module(String name, Category category, String description, int keybind){
      this(name, category, description, keybind, false);
   }

   public Module(String name, Category category, String description, int keybind, boolean newModule){
      this.name = name;
      this.category = category;
      this.description = description;
      this.keybind = keybind;
      this.isNew = newModule;
   }

   public void enable(){
      if (!this.enabled) {
         this.enabled = true;
         EventManager.INSTANCE.register(this);
         this.onEnable();
         NotificationRenderer.showModuleState(this.name, true);
      }
   }

   public void disable(){
      if (this.enabled) {
         this.enabled = false;
         this.onDisable();
         EventManager.INSTANCE.unregister(this);
         NotificationRenderer.showModuleState(this.name, false);
      }
   }

   public void toggle(){
      if (this.enabled) {
         this.disable();
      } else {
         this.enable();
      }
   }

   public void setEnabled(boolean enabled){
      if (enabled) {
         this.enable();
      } else {
         this.disable();
      }
   }

   protected void onEnable(){
   }

   protected void onDisable(){
   }

   protected <T extends Setting> T addSetting(T setting){
      this.settings.add(setting);
      return (T)setting;
   }

   protected BooleanSetting addBoolean(String name, boolean defaultValue){
      return this.addSetting(new BooleanSetting(name, defaultValue));
   }

   protected ActionSetting addAction(String name, String buttonLabel, Runnable action){
      return this.addSetting(new ActionSetting(name, buttonLabel, action));
   }

   protected NumberSetting addNumber(String name, double def, double min, double max, double step){
      return this.addSetting(new NumberSetting(name, def, min, max, step));
   }

   protected ColorSetting addColor(String name, int defaultColor){
      return this.addSetting(new ColorSetting(name, defaultColor));
   }

   protected ModeSetting addMode(String name, String def, String... modes){
      return this.addSetting(new ModeSetting(name, def, modes));
   }

   protected StringSetting addString(String name, String defaultValue, int maxLength){
      return this.addSetting(new StringSetting(name, defaultValue, maxLength));
   }

   protected KeybindSetting addKeybind(String name, int defaultValue){
      return this.addSetting(new KeybindSetting(name, defaultValue));
   }

   public List<Setting> getSettings(){
      return this.settings;
   }

   public boolean canReceiveEvents(){
      return this.enabled;
   }

   public String getName(){
      return this.name;
   }

   public String getHudName(){
      return this.name;
   }

   public Category getCategory(){
      return this.category;
   }

   public String getDescription(){
      return this.description;
   }

   public boolean isEnabled(){
      return this.enabled;
   }

   public int getKeybind(){
      return this.keybind;
   }

   public void setKeybind(int keybind){
      this.keybind = keybind;
   }

   public boolean isNewModule(){
      return this.isNew;
   }

   public boolean isToggleableInScreen(){
      return false;
   }
}

