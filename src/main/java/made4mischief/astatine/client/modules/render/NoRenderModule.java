package made4mischief.astatine.client.modules.render;

import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public final class NoRenderModule extends Module {
   private static NoRenderModule instance;
   private final BooleanSetting insideBlockSetting = this.addBoolean("Inside Block", true);
   private final BooleanSetting fireOverlaySetting = this.addBoolean("Fire Overlay", true);
   private final BooleanSetting worldFireSetting = this.addBoolean("World Fire", true);
   private final BooleanSetting underwaterOverlaySetting = this.addBoolean("Underwater Overlay", false);
   private final BooleanSetting portalOverlaySetting = this.addBoolean("Portal Overlay", false);
   private final BooleanSetting nauseaSetting = this.addBoolean("Nausea", true);
   private final BooleanSetting damageEffectSetting = this.addBoolean("Damage Effect", true);
   private final BooleanSetting movementBobbingSetting = this.addBoolean("Movement Bobbing", true);
   private final BooleanSetting handSwaySetting = this.addBoolean("Hand Sway", true);
   private final BooleanSetting explosionParticlesSetting = this.addBoolean("Explosion Particles", true);
   private final BooleanSetting potionParticlesSetting = this.addBoolean("Potion Particles", true);
   private final BooleanSetting statusEffectHUDSetting = this.addBoolean("Status Effect HUD", false);
   private final BooleanSetting totemAnimationSetting = this.addBoolean("Totem Animation", false);
   private final BooleanSetting armorSetting = this.addBoolean("Armor", false);
   private final BooleanSetting weatherSetting = this.addBoolean("Weather", true);
   private boolean fireEnabled;

   public NoRenderModule(){
      super("NoRender", Category.RENDER, "Ẩn lớp phủ, hạt và hiệu ứng đã chọn.");
      instance = this;
   }

   @Override
   protected void onEnable(){
      this.fireEnabled = this.worldFireSetting.getValue();
      clearPortalOverlay();
   }

   @Override
   protected void onDisable(){
      clearPortalOverlay();
   }

   @EventTarget
   public void onTick(TickEvent event){
      boolean value = this.worldFireSetting.getValue();
      if (value != this.fireEnabled) {
         this.fireEnabled = value;
         clearPortalOverlay();
      }
   }

   public static boolean shouldHideInsideBlock(){
      NoRenderModule noRenderModule = instance;
      return noRenderModule != null && noRenderModule.isEnabled() && noRenderModule.insideBlockSetting.getValue();
   }

   public static boolean shouldHideFireOverlay(){
      NoRenderModule noRenderModule = instance;
      return noRenderModule != null && noRenderModule.isEnabled() && noRenderModule.fireOverlaySetting.getValue();
   }

   public static boolean shouldHideWorldFire(){
      NoRenderModule noRenderModule = instance;
      return noRenderModule != null && noRenderModule.isEnabled() && noRenderModule.worldFireSetting.getValue();
   }

   public static boolean shouldHideUnderwaterOverlay(){
      NoRenderModule noRenderModule = instance;
      return noRenderModule != null && noRenderModule.isEnabled() && noRenderModule.underwaterOverlaySetting.getValue();
   }

   public static boolean shouldHidePortalOverlay(){
      NoRenderModule noRenderModule = instance;
      return noRenderModule != null && noRenderModule.isEnabled() && noRenderModule.portalOverlaySetting.getValue();
   }

   public static boolean shouldHideNausea(){
      NoRenderModule noRenderModule = instance;
      return noRenderModule != null && noRenderModule.isEnabled() && noRenderModule.nauseaSetting.getValue();
   }

   public static boolean shouldHideDamageEffect(){
      NoRenderModule noRenderModule = instance;
      return noRenderModule != null && noRenderModule.isEnabled() && noRenderModule.damageEffectSetting.getValue();
   }

   public static boolean shouldHideMovementBobbing(){
      NoRenderModule noRenderModule = instance;
      return noRenderModule != null && noRenderModule.isEnabled() && noRenderModule.movementBobbingSetting.getValue();
   }

   public static boolean shouldHideHandSway(){
      NoRenderModule noRenderModule = instance;
      return noRenderModule != null && noRenderModule.isEnabled() && noRenderModule.handSwaySetting.getValue();
   }

   public static boolean shouldHideExplosionParticles(){
      NoRenderModule noRenderModule = instance;
      return noRenderModule != null && noRenderModule.isEnabled() && noRenderModule.explosionParticlesSetting.getValue();
   }

   public static boolean shouldHidePotionParticles(){
      NoRenderModule noRenderModule = instance;
      return noRenderModule != null && noRenderModule.isEnabled() && noRenderModule.potionParticlesSetting.getValue();
   }

   public static boolean shouldHideStatusEffectHud(){
      NoRenderModule noRenderModule = instance;
      return noRenderModule != null && noRenderModule.isEnabled() && noRenderModule.statusEffectHUDSetting.getValue();
   }

   public static boolean shouldHideTotemAnimation(){
      NoRenderModule noRenderModule = instance;
      return noRenderModule != null && noRenderModule.isEnabled() && noRenderModule.totemAnimationSetting.getValue();
   }

   public static boolean shouldHideArmor(){
      NoRenderModule noRenderModule = instance;
      return noRenderModule != null && noRenderModule.isEnabled() && noRenderModule.armorSetting.getValue();
   }

   public static boolean shouldHideWeather(){
      NoRenderModule noRenderModule = instance;
      return noRenderModule != null && noRenderModule.isEnabled() && noRenderModule.weatherSetting.getValue();
   }

   private static void clearPortalOverlay(){
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.world != null && client.worldRenderer != null) {
         client.worldRenderer.scheduleTerrainUpdate();
      }
   }
}
