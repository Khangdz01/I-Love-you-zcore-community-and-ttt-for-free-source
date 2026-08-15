package made4mischief.astatine.client.gui.component.event;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class CharTypeEvent extends GuiEvent {
   private final char character;
   private final int modifiers;

   public CharTypeEvent(char character, int modifiers){
      this.character = character;
      this.modifiers = modifiers;
   }

   public char character(){
      return this.character;
   }

   public int modifiers(){
      return this.modifiers;
   }
}
