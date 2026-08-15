package made4mischief.astatine.client.modules.render;

import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.ModeSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.world.LightType;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public final class FullbrightModule extends Module {
   private static final int NIGHT_VISION_DURATION = 420;
   private static FullbrightModule instance;
   private final ModeSetting modeSetting = this.addMode("Mode", "Gamma", new String[]{"Gamma", "Potion", "Luminance"});
   private final ModeSetting lightTypeSetting = this.addMode("Light Type", "Block", new String[]{"Block", "Sky"});
   private final NumberSetting minimumLightSetting = this.addNumber("Minimum Light", 8.0, 0.0, 15.0, 1.0);
   private String lastMode;
   private String lastLightType;
   private int lastMinimumLight;
   private boolean gammaApplied;

   public FullbrightModule(){
      super("Fullbright", Category.RENDER, "Làm sáng thế giới bằng nhiều chế độ.", -1);
      instance = this;
      this.lightTypeSetting.visibleWhen(() -> this.modeSetting.is("Luminance"));
      this.minimumLightSetting.visibleWhen(() -> this.modeSetting.is("Luminance"));
      this.storeLastGamma();
   }

   @Override
   protected void onEnable(){
      this.storeLastGamma();
      if (this.modeSetting.is("Luminance")) {
         this.updateLighting();
      } else if (this.modeSetting.is("Potion")) {
         this.applyGamma(mc);
      }
   }

   @Override
   protected void onDisable(){
      if (this.gammaApplied || this.modeSetting.is("Potion") || "Potion".equals(this.lastMode)) {
         this.resetGamma(mc);
      }

      if (this.modeSetting.is("Luminance") || "Luminance".equals(this.lastMode)) {
         this.updateLighting();
      }

      this.gammaApplied = false;
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      this.applyGammaMode(client);
      if (this.modeSetting.is("Potion")) {
         this.applyGamma(client);
      }
   }

   public static boolean shouldUseGamma(){
      FullbrightModule fullbrightModule = instance;
      return fullbrightModule != null && fullbrightModule.isEnabled() && fullbrightModule.modeSetting.is("Gamma");
   }

   public static int getMinimumLuminance(LightType type){
      FullbrightModule fullbrightModule = instance;
      if (fullbrightModule != null && fullbrightModule.isEnabled() && fullbrightModule.modeSetting.is("Luminance")) {
         boolean is = type == LightType.BLOCK ? fullbrightModule.lightTypeSetting.is("Block") : fullbrightModule.lightTypeSetting.is("Sky");
         return is ? fullbrightModule.minimumLightSetting.getValueInt() : 0;
      } else {
         return 0;
      }
   }

   private void applyGammaMode(MinecraftClient client){
      String value2 = this.modeSetting.getValue();
      String value = this.lightTypeSetting.getValue();
      int valueInt = this.minimumLightSetting.getValueInt();
      if (!value2.equals(this.lastMode)) {
         if ("Potion".equals(this.lastMode)) {
            this.resetGamma(client);
         }

         if ("Luminance".equals(this.lastMode) || this.modeSetting.is("Luminance")) {
            this.updateLighting();
         }
      } else if (this.modeSetting.is("Luminance") && (!value.equals(this.lastLightType) || valueInt != this.lastMinimumLight)) {
         this.updateLighting();
      }

      this.lastMode = value2;
      this.lastLightType = value;
      this.lastMinimumLight = valueInt;
   }

   private void applyGamma(MinecraftClient client){
      if (client.player != null) {
         StatusEffectInstance statusEffectInstance = client.player.getStatusEffect(StatusEffects.NIGHT_VISION);
         if (statusEffectInstance == null || statusEffectInstance.getDuration() < 420) {
            client.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 420, 0, false, false, false));
         }

         this.gammaApplied = true;
      }
   }

   private void resetGamma(MinecraftClient client){
      if (client.player != null) {
         client.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
      }

      this.gammaApplied = false;
   }

   private void updateLighting(){
      if (mc.world != null && mc.worldRenderer != null) {
         mc.worldRenderer.reload();
      }
   }

   private void storeLastGamma(){
      this.lastMode = this.modeSetting.getValue();
      this.lastLightType = this.lightTypeSetting.getValue();
      this.lastMinimumLight = this.minimumLightSetting.getValueInt();
   }
}
