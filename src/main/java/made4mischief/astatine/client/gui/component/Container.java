package made4mischief.astatine.client.gui.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import made4mischief.astatine.client.gui.component.event.CharTypeEvent;
import made4mischief.astatine.client.gui.component.event.KeyPressEvent;
import made4mischief.astatine.client.gui.component.event.MouseClickEvent;
import made4mischief.astatine.client.gui.component.event.MouseMoveEvent;
import made4mischief.astatine.client.gui.component.event.MouseReleaseEvent;
import made4mischief.astatine.client.gui.component.event.MouseScrollEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class Container extends AbstractComponent {
   private final List<Component> children = new ArrayList<>();

   public Container(){
   }

   public Container(float x, float y, float width, float height){
      super(x, y, width, height);
   }

   public Component add(Component child){
      if (child != null && !this.children.contains(child)) {
         this.children.add(child);
         child.setParent(this);
         return child;
      } else {
         return child;
      }
   }

   public void remove(Component child){
      if (this.children.remove(child)) {
         child.setParent(null);
      }
   }

   public void clear(){
      for (Component component : this.children) {
         component.setParent(null);
      }

      this.children.clear();
   }

   public List<Component> getChildren(){
      return Collections.unmodifiableList(this.children);
   }

   @Override
   public final void render(GuiRenderContext context){
      if (this.isVisible()) {
         this.renderSelf(context);

         for (int index = 0; index < this.children.size(); index++) {
            Component component = this.children.get(index);
            if (component.isVisible()) {
               component.render(context);
            }
         }
      }
   }

   protected void renderSelf(GuiRenderContext context){
   }

   @Override
   public void tick(){
      for (int index = 0; index < this.children.size(); index++) {
         this.children.get(index).tick();
      }
   }

   @Override
   protected boolean onMouseClicked(MouseClickEvent event){
      for (int index = this.children.size() - 1; index >= 0; index--) {
         Component component = this.children.get(index);
         if (component.mouseClicked(event)) {
            return true;
         }
      }

      return false;
   }

   @Override
   protected boolean onMouseReleased(MouseReleaseEvent event){
      boolean var2 = false;

      for (int index = this.children.size() - 1; index >= 0; index--) {
         if (this.children.get(index).mouseReleased(event)) {
            var2 = true;
         }
      }

      return var2;
   }

   @Override
   protected boolean onMouseMoved(MouseMoveEvent event){
      for (int index = 0; index < this.children.size(); index++) {
         this.children.get(index).mouseMoved(event);
      }

      return false;
   }

   protected boolean onMouseScrolled(MouseScrollEvent event){
      for (int index = this.children.size() - 1; index >= 0; index--) {
         Component component = this.children.get(index);
         if (component.mouseScrolled(event)) {
            return true;
         }
      }

      return false;
   }

   @Override
   protected boolean onKeyPressed(KeyPressEvent event){
      for (int index = this.children.size() - 1; index >= 0; index--) {
         Component component = this.children.get(index);
         if (component.keyPressed(event)) {
            return true;
         }
      }

      return false;
   }

   @Override
   protected boolean onCharTyped(CharTypeEvent event){
      for (int index = this.children.size() - 1; index >= 0; index--) {
         Component component = this.children.get(index);
         if (component.charTyped(event)) {
            return true;
         }
      }

      return false;
   }
}

