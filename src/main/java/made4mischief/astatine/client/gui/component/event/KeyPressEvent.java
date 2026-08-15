package made4mischief.astatine.client.gui.component.event;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class KeyPressEvent extends GuiEvent {
   private final int keyCode;
   private final int scanCode;
   private final int modifiers;
   private final String clipboard;

   public KeyPressEvent(int keyCode, int scanCode, int modifiers){
      this(keyCode, scanCode, modifiers, "");
   }

   public KeyPressEvent(int keyCode, int scanCode, int modifiers, String clipboard){
      this.keyCode = keyCode;
      this.scanCode = scanCode;
      this.modifiers = modifiers;
      this.clipboard = clipboard == null ? "" : clipboard;
   }

   public int keyCode(){
      return this.keyCode;
   }

   public int scanCode(){
      return this.scanCode;
   }

   public int modifiers(){
      return this.modifiers;
   }

   public String clipboard(){
      return this.clipboard;
   }
}
