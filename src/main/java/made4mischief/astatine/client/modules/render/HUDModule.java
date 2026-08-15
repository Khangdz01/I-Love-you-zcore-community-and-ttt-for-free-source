package made4mischief.astatine.client.modules.render;

import made4mischief.astatine.client.hud.HudRenderer;
import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.ColorSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class HUDModule extends Module {
   private final BooleanSetting statsSetting = this.addBoolean("Stats", true);
   private final BooleanSetting inventorySetting = this.addBoolean("Inventory", true);
   private final BooleanSetting playerCharacterSetting = this.addBoolean("Player Character", true);
   private final BooleanSetting moduleListSetting = this.addBoolean("Module List", true);
   private final ColorSetting textColorSetting = this.addColor("Text Color", -1);
   private final ColorSetting customAccent = this.addColor("Custom Accent", -5084161);
   private final ColorSetting customSecondary = this.addColor("Custom Secondary", -13244417);
   private final NumberSetting statsXSetting = this.addNumber("Stats X", 100.0, 0.0, 100.0, 1.0);
   private final NumberSetting statsYSetting = this.addNumber("Stats Y", 100.0, 0.0, 100.0, 1.0);
   private final NumberSetting statsScaleSetting = this.addNumber("Stats Scale", 100.0, 50.0, 200.0, 5.0);
   private final NumberSetting inventoryXSetting = this.addNumber("Inventory X", 63.0, 0.0, 100.0, 1.0);
   private final NumberSetting inventoryYSetting = this.addNumber("Inventory Y", 78.0, 0.0, 100.0, 1.0);
   private final NumberSetting inventoryScaleSetting = this.addNumber("Inventory Scale", 100.0, 50.0, 200.0, 5.0);
   private final NumberSetting inventoryOpacitySetting = this.addNumber("Inventory Opacity", 35.0, 0.0, 100.0, 5.0);
   private final NumberSetting playerXSetting = this.addNumber("Player X", 0.0, 0.0, 100.0, 1.0);
   private final NumberSetting playerYSetting = this.addNumber("Player Y", 43.0, 0.0, 100.0, 1.0);
   private final NumberSetting playerScaleSetting = this.addNumber("Player Scale", 100.0, 50.0, 200.0, 5.0);
   private final NumberSetting moduleListXSetting = this.addNumber("Module List X", 100.0, 0.0, 100.0, 1.0);
   private final NumberSetting moduleListYSetting = this.addNumber("Module List Y", 0.0, 0.0, 100.0, 1.0);
   private final NumberSetting moduleListScaleSetting = this.addNumber("Module List Scale", 100.0, 50.0, 200.0, 5.0);

   public HUDModule(){
      super("HUD", Category.RENDER, "Hiện module, chỉ số, túi đồ và nhân vật.", -1);
      this.statsXSetting.visibleWhen(this.statsSetting::getValue);
      this.statsYSetting.visibleWhen(this.statsSetting::getValue);
      this.statsScaleSetting.visibleWhen(this.statsSetting::getValue);
      this.inventoryXSetting.visibleWhen(this.inventorySetting::getValue);
      this.inventoryYSetting.visibleWhen(this.inventorySetting::getValue);
      this.inventoryScaleSetting.visibleWhen(this.inventorySetting::getValue);
      this.inventoryOpacitySetting.visibleWhen(this.inventorySetting::getValue);
      this.playerXSetting.visibleWhen(this.playerCharacterSetting::getValue);
      this.playerYSetting.visibleWhen(this.playerCharacterSetting::getValue);
      this.playerScaleSetting.visibleWhen(this.playerCharacterSetting::getValue);
      this.moduleListXSetting.visibleWhen(this.moduleListSetting::getValue);
      this.moduleListYSetting.visibleWhen(this.moduleListSetting::getValue);
      this.moduleListScaleSetting.visibleWhen(this.moduleListSetting::getValue);
   }

   @Override
   protected void onEnable(){
      HudRenderer.setEnabled(true);
   }

   @Override
   protected void onDisable(){
      HudRenderer.setEnabled(false);
   }

   public boolean isStatsEnabled(){
      return this.statsSetting.getValue();
   }

   public boolean isInventoryEnabled(){
      return this.inventorySetting.getValue();
   }

   public boolean isPlayerEnabled(){
      return this.playerCharacterSetting.getValue();
   }

   public boolean isModuleListEnabled(){
      return this.moduleListSetting.getValue();
   }

   public int getTextColor(){
      return this.textColorSetting.getValue();
   }

   public int getCustomAccent(){
      return this.customAccent.getValue();
   }

   public int getCustomSecondary(){
      return this.customSecondary.getValue();
   }

   public void setCustomThemeColors(int accent, int secondary){
      this.customAccent.setValue(accent);
      this.customSecondary.setValue(secondary);
   }

   public float getStatsX(){
      return this.statsXSetting.getValueFloat() / 100.0F;
   }

   public float getStatsY(){
      return this.statsYSetting.getValueFloat() / 100.0F;
   }

   public float getStatsScale(){
      return this.statsScaleSetting.getValueFloat() / 100.0F;
   }

   public float getInventoryX(){
      return this.inventoryXSetting.getValueFloat() / 100.0F;
   }

   public float getInventoryY(){
      return this.inventoryYSetting.getValueFloat() / 100.0F;
   }

   public float getInventoryScale(){
      return this.inventoryScaleSetting.getValueFloat() / 100.0F;
   }

   public float getInventoryOpacity(){
      return this.inventoryOpacitySetting.getValueFloat() / 100.0F;
   }

   public float getPlayerX(){
      return this.playerXSetting.getValueFloat() / 100.0F;
   }

   public float getPlayerY(){
      return this.playerYSetting.getValueFloat() / 100.0F;
   }

   public float getPlayerScale(){
      return this.playerScaleSetting.getValueFloat() / 100.0F;
   }

   public float getModuleListX(){
      return this.moduleListXSetting.getValueFloat() / 100.0F;
   }

   public float getModuleListY(){
      return this.moduleListYSetting.getValueFloat() / 100.0F;
   }

   public float getModuleListScale(){
      return this.moduleListScaleSetting.getValueFloat() / 100.0F;
   }

   public void setStatsPosition(float x, float y){
      this.statsXSetting.setValue(x * 100.0F);
      this.statsYSetting.setValue(y * 100.0F);
   }

   public void setInventoryPosition(float x, float y){
      this.inventoryXSetting.setValue(x * 100.0F);
      this.inventoryYSetting.setValue(y * 100.0F);
   }

   public void setPlayerPosition(float x, float y){
      this.playerXSetting.setValue(x * 100.0F);
      this.playerYSetting.setValue(y * 100.0F);
   }

   public void setModuleListPosition(float x, float y){
      this.moduleListXSetting.setValue(x * 100.0F);
      this.moduleListYSetting.setValue(y * 100.0F);
   }
}
