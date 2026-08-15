package made4mischief.astatine.client.setting;

import java.util.Arrays;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ModeSetting extends Setting {
   private final List<String> modes;
   private int index;

   public ModeSetting(String name, String defaultMode, String... modes){
      super(name);
      this.modes = Arrays.asList(modes);
      int indexOf = this.modes.indexOf(defaultMode);
      this.index = indexOf >= 0 ? indexOf : 0;
   }

   public String getValue(){
      return this.modes.get(this.index);
   }

   public boolean is(String mode){
      return this.getValue().equalsIgnoreCase(mode);
   }

   public void setValue(String mode){
      int indexOf = this.modes.indexOf(mode);
      if (indexOf >= 0) {
         this.index = indexOf;
      }
   }

   public void cycle(){
      this.index = (this.index + 1) % this.modes.size();
   }

   public List<String> getModes(){
      return this.modes;
   }

   public int getIndex(){
      return this.index;
   }
}

