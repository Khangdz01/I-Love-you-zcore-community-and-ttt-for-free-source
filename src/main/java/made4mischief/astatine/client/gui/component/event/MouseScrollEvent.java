package made4mischief.astatine.client.gui.component.event;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class MouseScrollEvent extends GuiEvent {
   private final double x;
   private final double y;
   private final double mouseX;
   private final double mouseY;

   public MouseScrollEvent(double x, double y, double horizontal, double vertical){
      this.x = x;
      this.y = y;
      this.mouseX = horizontal;
      this.mouseY = vertical;
   }

   public double x(){
      return this.x;
   }

   public double y(){
      return this.y;
   }

   public double horizontal(){
      return this.mouseX;
   }

   public double vertical(){
      return this.mouseY;
   }
}

