package made4mischief.astatine.client.setting;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public abstract class Setting {
   private final String name;
   private Setting.Visibility visibility = () -> true;

   protected Setting(String name){
      this.name = name;
   }

   public String getName(){
      return this.name;
   }

   public boolean isVisible(){
      return this.visibility.isVisible();
   }

   public Setting visibleWhen(Setting.Visibility visibility){
      this.visibility = visibility;
      return this;
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface Visibility {
      boolean isVisible();
   }
}

