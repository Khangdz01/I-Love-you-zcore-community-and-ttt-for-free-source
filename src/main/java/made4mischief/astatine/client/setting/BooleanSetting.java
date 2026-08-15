package made4mischief.astatine.client.setting;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class BooleanSetting extends Setting {
   private boolean value;

   public BooleanSetting(String name, boolean defaultValue){
      super(name);
      this.value = defaultValue;
   }

   public boolean getValue(){
      return this.value;
   }

   public void setValue(boolean value){
      this.value = value;
   }

   public void toggle(){
      this.value = !this.value;
   }
}
