package made4mischief.astatine.client.setting;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class StringSetting extends Setting {
   private final int maxLength;
   private String value;

   public StringSetting(String name, String defaultValue, int maxLength){
      super(name);
      if (maxLength < 1) {
         throw new IllegalArgumentException("maxLength must be positive");
      } else {
         this.maxLength = maxLength;
         this.setValue(defaultValue);
      }
   }

   public String getValue(){
      return this.value;
   }

   public void setValue(String value){
      String var2 = value == null ? "" : value;
      this.value = var2.length() <= this.maxLength ? var2 : var2.substring(0, this.maxLength);
   }

   public int getMaxLength(){
      return this.maxLength;
   }
}
