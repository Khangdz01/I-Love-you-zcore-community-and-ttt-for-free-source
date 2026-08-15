package made4mischief.astatine.client.modules.render;

import made4mischief.astatine.client.gui.component.theme.ThemeManager;
import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.ColorSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class SkeletonModule extends Module {
   private final BooleanSetting themeColorSetting = this.addBoolean("Theme Color", true);
   private final ColorSetting color = this.addColor("Color", -13244417);
   private final NumberSetting lineWidthSetting = this.addNumber("Line Width", 2.0, 1.0, 5.0, 0.25);
   private final BooleanSetting self = this.addBoolean("Self", true);

   public SkeletonModule(){
      super("Skeleton", Category.RENDER, "Hiện bộ xương chuyển động trong người chơi.", -1, true);
      this.color.visibleWhen(() -> !this.themeColorSetting.getValue());
   }

   public int getColor(){
      int value = this.themeColorSetting.getValue() ? ThemeManager.active().accent() : this.color.getValue();
      return 0xFF000000 | value & 16777215;
   }

   public float getLineWidth(){
      return this.lineWidthSetting.getValueFloat();
   }

   public boolean rendersSelf(){
      return this.self.getValue();
   }
}
