package made4mischief.astatine.client.gui.component.event;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class MouseMoveEvent extends GuiEvent {
   private final double x;
   private final double y;

   public MouseMoveEvent(double x, double y){
      this.x = x;
      this.y = y;
   }

   public double x(){
      return this.x;
   }

   public double y(){
      return this.y;
   }
}

