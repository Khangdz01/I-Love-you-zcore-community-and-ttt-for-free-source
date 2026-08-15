package made4mischief.astatine.client.modules.render;

import made4mischief.astatine.client.gui.component.theme.ThemeManager;
import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.ColorSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;

@Environment(EnvType.CLIENT)
public final class CrystalEspModule extends Module {
   private static CrystalEspModule instance;
   private final BooleanSetting themeColorSetting = this.addBoolean("Theme Color", true);
   private final ColorSetting color = this.addColor("Color", -4879105);

   public CrystalEspModule(){
      super("CrystalESP", Category.RENDER, "Làm nổi bật pha lê End qua tường.", -1, true);
      instance = this;
      this.color.visibleWhen(() -> !this.themeColorSetting.getValue());
   }

   public static boolean shouldOutline(Entity entity){
      return instance != null && instance.isEnabled() && entity instanceof EndCrystalEntity;
   }

   public static int getOutlineColor(Entity entity){
      if (!shouldOutline(entity)) {
         return -1;
      } else {
         int value = instance.themeColorSetting.getValue() ? ThemeManager.active().accent() : instance.color.getValue();
         return value & 16777215;
      }
   }
}
