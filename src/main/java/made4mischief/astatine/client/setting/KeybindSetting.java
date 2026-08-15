package made4mischief.astatine.client.setting;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class KeybindSetting extends Setting {
   private int keyCode;

   public KeybindSetting(String name, int defaultValue){
      super(name);
      this.keyCode = defaultValue;
   }

   public int getValue(){
      return this.keyCode;
   }

   public void setValue(int value){
      this.keyCode = value;
   }
}
