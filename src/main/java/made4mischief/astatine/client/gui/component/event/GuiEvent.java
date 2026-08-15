package made4mischief.astatine.client.gui.component.event;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public abstract class GuiEvent {
   private boolean consumed;
   private Object consumer;

   public final boolean isConsumed(){
      return this.consumed;
   }

   public final void consume(Object consumer){
      this.consumed = true;
      this.consumer = consumer;
   }

   public final Object consumer(){
      return this.consumer;
   }
}
