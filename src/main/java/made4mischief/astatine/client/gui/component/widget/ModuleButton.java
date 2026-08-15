package made4mischief.astatine.client.gui.component.widget;

import made4mischief.astatine.client.gui.component.AbstractComponent;
import made4mischief.astatine.client.gui.component.GuiRenderContext;
import made4mischief.astatine.client.gui.component.event.MouseButton;
import made4mischief.astatine.client.gui.component.event.MouseClickEvent;
import made4mischief.astatine.client.gui.component.theme.Theme;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.utils.render.animation.Animation;
import made4mischief.astatine.client.utils.render.animation.AnimationType;
import made4mischief.astatine.client.utils.render.core.ColorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ModuleButton extends AbstractComponent {
   private final Module module;
   private float headerHeight;
   private float RESERVED_HEIGHT = 44.0F;
   private boolean expanded;
   private final Animation hoverAnimation = new Animation(0.0F, 1.0F, 150L, AnimationType.EASE_OUT);
   private final Animation enabledAnimation;
   private final Animation expandAnimation = new Animation(0.0F, 1.0F, 200L, AnimationType.EASE_IN_OUT);
   private boolean hovered;

   public ModuleButton(Module module, float x, float y, float width, float headerHeight){
      super(x, y, width, headerHeight);
      this.module = module;
      this.headerHeight = headerHeight;
      float enabled = module.isEnabled() ? 1.0F : 0.0F;
      this.enabledAnimation = new Animation(enabled, enabled, 200L, AnimationType.EASE_OUT);
   }

   public Module getModule(){
      return this.module;
   }

   public boolean isExpanded(){
      return this.expanded;
   }

   public void setExpanded(boolean expanded){
      if (this.expanded != expanded) {
         this.expanded = expanded;
         this.expandAnimation.setTarget(expanded ? 1.0F : 0.0F);
      }
   }

   public void toggleExpanded(){
      this.setExpanded(!this.expanded);
   }

   public void setReservedHeight(float reservedHeight){
      this.RESERVED_HEIGHT = Math.max(0.0F, reservedHeight);
   }

   @Override
   public void setSize(float width, float height){
      super.setSize(width, height);
      this.headerHeight = height;
   }

   @Override
   public float getHeight(){
      return this.headerHeight + this.expandAnimation.get() * this.RESERVED_HEIGHT;
   }

   @Override
   public boolean isWithin(double pointX, double pointY){
      return pointX >= this.getX() && pointX <= this.getX() + this.getWidth() && pointY >= this.getY() && pointY <= this.getY() + this.headerHeight;
   }

   @Override
   public void render(GuiRenderContext context){
      if (this.isVisible()) {
         Theme theme = context.theme();
         boolean hovered = this.isHovered();
         if (hovered != this.hovered) {
            this.hoverAnimation.setTarget(hovered ? 1.0F : 0.0F);
            this.hovered = hovered;
         }

         float get2 = this.hoverAnimation.get();
         float get = this.enabledAnimation.get();
         this.drawExpandedContent(context, theme);
         this.drawBackground(context, theme, get2, get);
      }
   }

   private void drawBackground(GuiRenderContext context, Theme theme, float hover, float enabled){
      float x = this.getX();
      float y = this.getY();
      float width = this.getWidth();
      int surfaceElevated = ColorUtil.lerp(theme.surface(), theme.surfaceElevated(), hover);
      surfaceElevated = ColorUtil.lerp(surfaceElevated, theme.accent(), enabled * 0.18F);
      context.drawBo(x, y, width, this.headerHeight, 6.0F, surfaceElevated);
      int accent = ColorUtil.lerp(theme.text(), theme.accent(), enabled);
      float textHeight = y + (this.headerHeight - context.textHeight()) / 2.0F;
      context.drawText(this.module.getName(), x + 8.0F, textHeight, accent, true);
      int accent2 = ColorUtil.lerp(theme.border(), theme.accent(), enabled);
      float var12 = 8.0F;
      float var13 = 16.0F;
      context.drawBo(x + width - var13 - 8.0F, y + (this.headerHeight - var12) / 2.0F, var13, var12, var12 / 2.0F, accent2);
   }

   private void drawExpandedContent(GuiRenderContext context, Theme theme){
      float get = this.expandAnimation.get();
      if (!(get <= 0.001F)) {
         float x = this.getX();
         float y = this.getY() + this.headerHeight;
         float var6 = get * this.RESERVED_HEIGHT;
         int round2 = Math.round(y);
         int round = Math.round(y + var6);
         context.enableScissor(Math.round(x), round2, Math.round(x + this.getWidth()), round);
         context.drawBo(x, y, this.getWidth(), this.RESERVED_HEIGHT, 4.0F, theme.surface());
         float textHeight = y + (this.RESERVED_HEIGHT - context.textHeight()) / 2.0F;
         context.drawText("No settings yet", x + 10.0F, textHeight, theme.textDim(), false, 0.9F);
         context.disableScissor();
      }
   }

   @Override
   protected boolean onMouseClicked(MouseClickEvent event){
      if (!this.isWithin(event.x(), event.y())) {
         return false;
      } else if (event.button() == MouseButton.LEFT) {
         this.module.toggle();
         this.enabledAnimation.setTarget(this.module.isEnabled() ? 1.0F : 0.0F);
         return true;
      } else if (event.button() == MouseButton.RIGHT) {
         this.toggleExpanded();
         return true;
      } else {
         return false;
      }
   }
}

