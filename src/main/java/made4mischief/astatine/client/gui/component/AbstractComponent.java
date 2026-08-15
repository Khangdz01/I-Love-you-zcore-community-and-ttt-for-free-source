package made4mischief.astatine.client.gui.component;

import made4mischief.astatine.client.gui.component.event.CharTypeEvent;
import made4mischief.astatine.client.gui.component.event.KeyPressEvent;
import made4mischief.astatine.client.gui.component.event.MouseClickEvent;
import made4mischief.astatine.client.gui.component.event.MouseMoveEvent;
import made4mischief.astatine.client.gui.component.event.MouseReleaseEvent;
import made4mischief.astatine.client.gui.component.event.MouseScrollEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public abstract class AbstractComponent implements Component {
   private float x;
   private float y;
   private float width;
   private float height;
   private boolean visible = true;
   private boolean enabled = true;
   private boolean hovered;
   private boolean focused;
   private boolean focusable;
   private Component parent;

   protected AbstractComponent(){
   }

   protected AbstractComponent(float x, float y, float width, float height){
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
   }

   @Override
   public float getX(){
      return this.x;
   }

   @Override
   public float getY(){
      return this.y;
   }

   @Override
   public void setPosition(float x, float y){
      this.x = x;
      this.y = y;
   }

   @Override
   public float getWidth(){
      return this.width;
   }

   @Override
   public float getHeight(){
      return this.height;
   }

   @Override
   public void setSize(float width, float height){
      this.width = width;
      this.height = height;
   }

   @Override
   public boolean isWithin(double pointX, double pointY){
      return pointX >= this.x && pointX <= this.x + this.width && pointY >= this.y && pointY <= this.y + this.height;
   }

   @Override
   public boolean isVisible(){
      return this.visible;
   }

   @Override
   public void setVisible(boolean visible){
      this.visible = visible;
      if (!visible) {
         this.hovered = false;
         this.focused = false;
      }
   }

   @Override
   public boolean isEnabled(){
      return this.enabled;
   }

   @Override
   public void setEnabled(boolean enabled){
      this.enabled = enabled;
      if (!enabled) {
         this.hovered = false;
         this.focused = false;
      }
   }

   @Override
   public boolean isHovered(){
      return this.hovered;
   }

   protected void setHovered(boolean hovered){
      this.hovered = hovered;
   }

   @Override
   public boolean isFocused(){
      return this.focused;
   }

   @Override
   public void setFocused(boolean focused){
      this.focused = focused;
   }

   @Override
   public boolean isFocusable(){
      return this.focusable;
   }

   public void setFocusable(boolean focusable){
      this.focusable = focusable;
   }

   @Override
   public Component getParent(){
      return this.parent;
   }

   @Override
   public void setParent(Component parent){
      this.parent = parent;
   }

   @Override
   public void tick(){
   }

   @Override
   public final boolean mouseClicked(MouseClickEvent event){
      return !this.interactive() ? false : this.onMouseClicked(event);
   }

   @Override
   public final boolean mouseReleased(MouseReleaseEvent event){
      return !this.interactive() ? false : this.onMouseReleased(event);
   }

   @Override
   public final boolean mouseMoved(MouseMoveEvent event){
      if (!this.interactive()) {
         return false;
      } else {
         this.hovered = this.isWithin(event.x(), event.y());
         return this.onMouseMoved(event);
      }
   }

   public final boolean mouseScrolled(MouseScrollEvent event){
      return !this.interactive() ? false : this.onMouseScrolled(event);
   }

   @Override
   public final boolean keyPressed(KeyPressEvent event){
      return !this.interactive() ? false : this.onKeyPressed(event);
   }

   @Override
   public final boolean charTyped(CharTypeEvent event){
      return !this.interactive() ? false : this.onCharTyped(event);
   }

   protected final boolean interactive(){
      return this.visible && this.enabled;
   }

   protected boolean onMouseClicked(MouseClickEvent event){
      return false;
   }

   protected boolean onMouseReleased(MouseReleaseEvent event){
      return false;
   }

   protected boolean onMouseMoved(MouseMoveEvent event){
      return false;
   }

   protected boolean onMouseScrolled(MouseScrollEvent event){
      return false;
   }

   protected boolean onKeyPressed(KeyPressEvent event){
      return false;
   }

   protected boolean onCharTyped(CharTypeEvent event){
      return false;
   }
}

