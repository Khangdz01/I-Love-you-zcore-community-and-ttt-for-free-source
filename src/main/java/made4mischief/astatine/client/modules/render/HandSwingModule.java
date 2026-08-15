package made4mischief.astatine.client.modules.render;

import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class HandSwingModule extends Module {
   private final NumberSetting speedSetting = this.addNumber("Speed", 1.0, 0.05, 5.0, 0.05);
   private static HandSwingModule instance;

   public HandSwingModule(){
      super("HandSwing", Category.RENDER, "Chỉnh tốc độ vung tay.");
      instance = this;
   }

   public static float getSpeed(){
      return instance != null && instance.isEnabled() ? (float)instance.speedSetting.getValue() : 1.0F;
   }
}
