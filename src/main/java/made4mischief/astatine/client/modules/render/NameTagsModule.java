package made4mischief.astatine.client.modules.render;

import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class NameTagsModule extends Module {
   private final NumberSetting rangeSetting = this.addNumber("Range", 50.0, 16.0, 50.0, 4.0);
   private final NumberSetting scale = this.addNumber("Scale", 1.0, 0.65, 2.0, 0.05);

   public NameTagsModule(){
      super("NameTags", Category.RENDER, "Hiện giáp và vật phẩm trên tên người chơi.", -1);
   }

   public double getRangeSquared(){
      double value = this.rangeSetting.getValue();
      return value * value;
   }

   public float getScale(){
      return this.scale.getValueFloat();
   }
}
