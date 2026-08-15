package made4mischief.astatine.client.setting;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ActionSetting extends Setting {
   private final String buttonLabel;
   private final Runnable action;

   public ActionSetting(String name, String buttonLabel, Runnable action){
      super(name);
      this.buttonLabel = buttonLabel;
      this.action = action;
   }

   public String getButtonLabel(){
      return this.buttonLabel;
   }

   public void invoke(){
      if (this.action != null) {
         this.action.run();
      }
   }
}

