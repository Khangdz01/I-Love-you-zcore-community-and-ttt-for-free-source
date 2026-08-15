package made4mischief.astatine.client.gui.component;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class Panel extends Container {
   private float radius = 8.0F;
   private boolean drawBorder = true;

   public Panel(){
   }

   public Panel(float x, float y, float width, float height){
      super(x, y, width, height);
   }

   public void setRadius(float radius){
      this.radius = radius;
   }

   public void setDrawBorder(boolean drawBorder){
      this.drawBorder = drawBorder;
   }

   @Override
   protected void renderSelf(GuiRenderContext context){
      if (this.drawBorder) {
         context.drawBo(
            this.getX() - 1.0F, this.getY() - 1.0F, this.getWidth() + 2.0F, this.getHeight() + 2.0F, this.radius + 1.0F, context.theme().border()
         );
      }

      context.drawBo(this.getX(), this.getY(), this.getWidth(), this.getHeight(), this.radius, context.theme().surface());
   }
}

