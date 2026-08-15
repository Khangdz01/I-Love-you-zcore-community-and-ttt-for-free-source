package made4mischief.astatine.client.gui.component.event;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class MouseReleaseEvent extends GuiEvent {
   private final double x;
   private final double y;
   private final MouseButton button;

   public MouseReleaseEvent(double x, double y, MouseButton button){
      this.x = x;
      this.y = y;
      this.button = button;
   }

   public double x(){
      return this.x;
   }

   public double y(){
      return this.y;
   }

   public MouseButton button(){
      return this.button;
   }
}

