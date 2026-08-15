package made4mischief.astatine.client.gui.component.widget;

import made4mischief.astatine.client.gui.component.AbstractComponent;
import made4mischief.astatine.client.gui.component.GuiRenderContext;
import made4mischief.astatine.client.gui.component.event.KeyPressEvent;
import made4mischief.astatine.client.gui.component.event.MouseButton;
import made4mischief.astatine.client.gui.component.event.MouseClickEvent;
import made4mischief.astatine.client.gui.component.theme.Theme;
import made4mischief.astatine.client.utils.render.animation.Animation;
import made4mischief.astatine.client.utils.render.animation.AnimationType;
import made4mischief.astatine.client.utils.render.core.ColorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class Button extends AbstractComponent {
   private static final int ENTER_KEY = 257;
   private static final int SPACE_KEY = 32;
   private String label;
   private Runnable onClick;
   private final Animation hoverAnimation = new Animation(0.0F, 1.0F, 160L, AnimationType.EASE_OUT);
   private boolean hovered;

   public Button(String label, float x, float y, float width, float height, Runnable onClick){
      super(x, y, width, height);
      this.label = label;
      this.onClick = onClick;
      this.setFocusable(true);
   }

   public void setLabel(String label){
      this.label = label;
   }

   public String getLabel(){
      return this.label;
   }

   public void setOnClick(Runnable onClick){
      this.onClick = onClick;
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

         float get = this.hoverAnimation.get();
         int hover = ColorUtil.lerp(theme.surfaceElevated(), theme.hover(), get);
         if (!this.isEnabled()) {
            hover = ColorUtil.scaleAlpha(theme.surfaceElevated(), theme.disabledOpacity());
         }

         context.drawBo(this.getX(), this.getY(), this.getWidth(), this.getHeight(), 6.0F, hover);
         if (this.isFocused()) {
            context.drawBo(this.getX() - 1.0F, this.getY() - 1.0F, this.getWidth() + 2.0F, this.getHeight() + 2.0F, 7.0F, theme.focus());
            context.drawBo(this.getX(), this.getY(), this.getWidth(), this.getHeight(), 6.0F, hover);
         }

         int disabledOpacity = this.isEnabled() ? ColorUtil.lerp(theme.text(), theme.accent(), get) : ColorUtil.scaleAlpha(theme.textDim(), theme.disabledOpacity());
         context.drawCenteredText(
            this.label, this.getX() + this.getWidth() / 2.0F, this.getY() + (this.getHeight() - context.textHeight()) / 2.0F, disabledOpacity, true
         );
      }
   }

   @Override
   protected boolean onMouseClicked(MouseClickEvent event){
      if (event.button() == MouseButton.LEFT && this.isWithin(event.x(), event.y())) {
         this.activate();
         return true;
      } else {
         return false;
      }
   }

   @Override
   protected boolean onKeyPressed(KeyPressEvent event){
      if (event.keyCode() != 257 && event.keyCode() != 32) {
         return false;
      } else {
         this.activate();
         return true;
      }
   }

   private void activate(){
      if (this.onClick != null) {
         this.onClick.run();
      }
   }
}

