package made4mischief.astatine.client.modules.render;

import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class PixelPetModule extends Module {
   private final BooleanSetting animationsSetting = this.addBoolean("Animations", true);
   private final NumberSetting animationSpeedSetting = this.addNumber("Animation Speed", 1.0, 0.25, 2.0, 0.05);
   private final NumberSetting positionXSetting = this.addNumber("Position X", 78.0, 0.0, 100.0, 1.0);
   private final NumberSetting positionYSetting = this.addNumber("Position Y", 72.0, 0.0, 100.0, 1.0);

   public PixelPetModule(){
      super("Pet", Category.RENDER, "ThÃº cÆ°ng pixel Asti cÃ³ thá»ƒ kÃ©o tháº£.", -1, true);
   }

   public float getPositionX(){
      return this.positionXSetting.getValueFloat() / 100.0F;
   }

   public float getPositionY(){
      return this.positionYSetting.getValueFloat() / 100.0F;
   }

   public boolean hasAnimations(){
      return this.animationsSetting.getValue();
   }

   public float getAnimationSpeed(){
      return this.animationSpeedSetting.getValueFloat();
   }

   public void setPosition(float x, float y){
      this.positionXSetting.setValue(clamp01(x) * 100.0F);
      this.positionYSetting.setValue(clamp01(y) * 100.0F);
   }

   private static float clamp01(float value){
      return Math.max(0.0F, Math.min(1.0F, value));
   }
}

