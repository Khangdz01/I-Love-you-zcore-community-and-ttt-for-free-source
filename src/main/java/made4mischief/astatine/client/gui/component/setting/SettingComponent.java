package made4mischief.astatine.client.gui.component.setting;

import made4mischief.astatine.client.gui.component.AbstractComponent;
import made4mischief.astatine.client.setting.Setting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public abstract class SettingComponent extends AbstractComponent {
   protected static final float ROW_HEIGHT = 18.0F;
   protected static final float LABEL_SCALE = 0.85F;
   protected static final float VALUE_SCALE = 0.8F;
   private final Setting setting;

   protected SettingComponent(Setting setting, float x, float y, float width){
      super(x, y, width, 18.0F);
      this.setting = setting;
   }

   public Setting getSetting(){
      return this.setting;
   }

   public String getLabel(){
      return this.setting.getName();
   }
}

