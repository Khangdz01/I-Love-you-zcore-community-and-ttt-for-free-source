package made4mischief.astatine.client.gui.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import made4mischief.astatine.client.gui.component.event.MouseButton;
import made4mischief.astatine.client.gui.component.event.MouseClickEvent;
import made4mischief.astatine.client.gui.component.event.MouseMoveEvent;
import made4mischief.astatine.client.gui.component.event.MouseReleaseEvent;
import made4mischief.astatine.client.gui.component.event.MouseScrollEvent;
import made4mischief.astatine.client.gui.component.theme.Theme;
import made4mischief.astatine.client.gui.component.widget.ModuleButton;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.utils.render.animation.Animation;
import made4mischief.astatine.client.utils.render.animation.AnimationType;
import made4mischief.astatine.client.utils.render.core.ColorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class CategoryPanel extends AbstractComponent {
   private static final float HEADER_HEIGHT = 22.0F;
   private static final float BUTTON_HEIGHT = 18.0F;
   private static final float CONTENT_GAP = 4.0F;
   private static final float SIDE_INSET = 6.0F;
   private static final float BUTTON_GAP = 4.0F;
   private static final float RADIUS = 6.0F;
   private final String title;
   private final List<ModuleButton> moduleButtons = new ArrayList<>();
   private final List<ModuleButton> unmodifiableButtons = Collections.unmodifiableList(this.moduleButtons);
   private boolean expanded = true;
   private final Animation expandAnimation;
   private final Animation hoverAnimation = new Animation(0.0F, 1.0F, 150L, AnimationType.EASE_OUT);
   private boolean headerHovered;

   public CategoryPanel(String title, float x, float y, float width){
      super(x, y, width, 22.0F);
      this.title = title;
      float var5 = this.expanded ? 1.0F : 0.0F;
      this.expandAnimation = new Animation(var5, var5, 220L, AnimationType.EASE_IN_OUT);
   }

   public void addModule(Module module){
      if (module != null) {
         ModuleButton moduleButton = new ModuleButton(module, this.getX() + 6.0F, this.getY() + 22.0F, this.getWidth() - 12.0F, 18.0F);
         this.moduleButtons.add(moduleButton);
         moduleButton.setParent(this);
         this.refreshLayout();
      }
   }

   public void removeModule(Module module){
      for (int index = 0; index < this.moduleButtons.size(); index++) {
         if (this.moduleButtons.get(index).getModule() == module) {
            ModuleButton moduleButton = this.moduleButtons.remove(index);
            moduleButton.setParent(null);
            this.refreshLayout();
            return;
         }
      }
   }

   public List<ModuleButton> getButtons(){
      return this.unmodifiableButtons;
   }

   public boolean isExpanded(){
      return this.expanded;
   }

   public void toggleExpanded(){
      this.setExpanded(!this.expanded);
   }

   public void setExpanded(boolean expanded){
      if (this.expanded != expanded) {
         this.expanded = expanded;
         this.expandAnimation.setTarget(expanded ? 1.0F : 0.0F);
      }
   }

   public void refreshLayout(){
      this.layoutModules();
   }

   @Override
   public float getHeight(){
      return 22.0F + this.expandAnimation.get() * this.getContentHeight();
   }

   private float getContentHeight(){
      if (this.moduleButtons.isEmpty()) {
         return 0.0F;
      } else {
         float var1 = 4.0F;

         for (int index = 0; index < this.moduleButtons.size(); index++) {
            var1 += this.moduleButtons.get(index).getHeight() + 4.0F;
         }

         return var1;
      }
   }

   private void layoutModules(){
      float x = this.getX() + 6.0F;
      float width = this.getWidth() - 12.0F;
      float y = this.getY() + 22.0F + 4.0F;

      for (int index = 0; index < this.moduleButtons.size(); index++) {
         ModuleButton moduleButton = this.moduleButtons.get(index);
         moduleButton.setPosition(x, y);
         moduleButton.setSize(width, 18.0F);
         y += moduleButton.getHeight() + 4.0F;
      }
   }

   @Override
   public boolean isWithin(double pointX, double pointY){
      return pointX >= this.getX() && pointX <= this.getX() + this.getWidth() && pointY >= this.getY() && pointY <= this.getY() + this.getHeight();
   }

   @Override
   public void render(GuiRenderContext context){
      if (this.isVisible()) {
         Theme theme = context.theme();
         this.layoutModules();
         float x = this.getX();
         float y = this.getY();
         float width = this.getWidth();
         float height = this.getHeight();
         context.drawBo(x - 1.0F, y - 1.0F, width + 2.0F, height + 2.0F, 7.0F, theme.border());
         context.drawBo(x, y, width, height, 6.0F, theme.surface());
         this.updateHeaderHover(context, theme, x, y, width);
         this.renderContent(context, x, y, width, height);
      }
   }

   private void updateHeaderHover(GuiRenderContext context, Theme theme, float x, float y, float w){
      boolean mouseY = context.mouseX() >= x && context.mouseX() <= x + w && context.mouseY() >= y && context.mouseY() <= y + 22.0F;
      if (mouseY != this.headerHovered) {
         this.hoverAnimation.setTarget(mouseY ? 1.0F : 0.0F);
         this.headerHovered = mouseY;
      }

      int get = ColorUtil.lerp(theme.surfaceElevated(), theme.hover(), this.hoverAnimation.get());
      context.drawBo(x, y, w, 22.0F, 6.0F, get);
      float textHeight = y + (22.0F - context.textHeight()) / 2.0F;
      context.drawText(this.title, x + 6.0F, textHeight, theme.text(), true);
      String var9 = this.expanded ? "v" : ">";
      context.drawText(var9, x + w - 6.0F - context.textWidth(var9), textHeight, theme.textDim(), true);
   }

   private void renderContent(GuiRenderContext context, float x, float y, float w, float total){
      float get = this.expandAnimation.get();
      if (!(get <= 0.001F) && !this.moduleButtons.isEmpty()) {
         float var7 = y + 22.0F;
         float var8 = total - 22.0F;
         context.enableScissor(Math.round(x), Math.round(var7), Math.round(x + w), Math.round(var7 + var8));

         for (int index = 0; index < this.moduleButtons.size(); index++) {
            this.moduleButtons.get(index).render(context);
         }

         context.disableScissor();
      }
   }

   @Override
   public void tick(){
      for (int index = 0; index < this.moduleButtons.size(); index++) {
         this.moduleButtons.get(index).tick();
      }
   }

   @Override
   protected boolean onMouseClicked(MouseClickEvent event){
      boolean y = event.x() >= this.getX() && event.x() <= this.getX() + this.getWidth() && event.y() >= this.getY() && event.y() <= this.getY() + 22.0F;
      if (event.button() == MouseButton.RIGHT && y) {
         this.toggleExpanded();
         return true;
      } else {
         if (this.expanded) {
            for (int index = this.moduleButtons.size() - 1; index >= 0; index--) {
               if (this.moduleButtons.get(index).mouseClicked(event)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   @Override
   protected boolean onMouseReleased(MouseReleaseEvent event){
      boolean var2 = false;

      for (int index = this.moduleButtons.size() - 1; index >= 0; index--) {
         if (this.moduleButtons.get(index).mouseReleased(event)) {
            var2 = true;
         }
      }

      return var2;
   }

   @Override
   protected boolean onMouseMoved(MouseMoveEvent event){
      if (this.expanded) {
         for (int index = 0; index < this.moduleButtons.size(); index++) {
            this.moduleButtons.get(index).mouseMoved(event);
         }
      }

      return false;
   }

   protected boolean onMouseScrolled(MouseScrollEvent event){
      if (this.expanded) {
         for (int index = this.moduleButtons.size() - 1; index >= 0; index--) {
            if (this.moduleButtons.get(index).mouseScrolled(event)) {
               return true;
            }
         }
      }

      return false;
   }
}

