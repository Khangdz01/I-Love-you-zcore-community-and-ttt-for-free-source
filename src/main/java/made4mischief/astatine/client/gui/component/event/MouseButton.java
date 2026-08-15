package made4mischief.astatine.client.gui.component.event;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum MouseButton {
   LEFT(0),
   RIGHT(1),
   MIDDLE(2),
   OTHER(-1);

   private final int code;

   private MouseButton(int code){
      this.code = code;
   }

   public int code(){
      return this.code;
   }

   public static MouseButton fromCode(int code){
      switch (code) {
         case 0:
            return LEFT;
         case 1:
            return RIGHT;
         case 2:
            return MIDDLE;
         default:
            return OTHER;
      }
   }
}

