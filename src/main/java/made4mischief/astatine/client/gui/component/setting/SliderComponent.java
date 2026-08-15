package made4mischief.astatine.client.gui.component.setting;

import made4mischief.astatine.client.gui.component.GuiRenderContext;
import made4mischief.astatine.client.gui.component.event.MouseButton;
import made4mischief.astatine.client.gui.component.event.MouseClickEvent;
import made4mischief.astatine.client.gui.component.event.MouseMoveEvent;
import made4mischief.astatine.client.gui.component.event.MouseReleaseEvent;
import made4mischief.astatine.client.gui.component.theme.Theme;
import made4mischief.astatine.client.setting.NumberSetting;
import made4mischief.astatine.client.utils.render.animation.Animation;
import made4mischief.astatine.client.utils.render.animation.AnimationType;
import made4mischief.astatine.client.utils.render.core.ColorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class SliderComponent extends SettingComponent {
   private static final float TRACK_HEIGHT = 4.0F;
   private static final float KNOB_SIZE = 12.0F;
   private static final float TRACK_Y_OFFSET = 26.0F;
   private static final float HEIGHT = 34.0F;
   private final NumberSetting setting;
   private final Animation dragAnimation = new Animation(0.0F, 1.0F, 150L, AnimationType.EASE_OUT);
   private boolean dragging;
   private boolean pressed;

   public SliderComponent(NumberSetting setting, float x, float y, float width){
      super(setting, x, y, width);
      this.setting = setting;
      this.setSize(width, 34.0F);
   }

   @Override
   public float getHeight(){
      return 34.0F;
   }

   @Override
   public void render(GuiRenderContext context){
      if (this.isVisible()) {
         Theme theme = context.theme();
         float x = this.getX();
         float y = this.getY();
         float width = this.getWidth();
         context.drawText(this.getLabel().toUpperCase(), x, y + 1.0F, theme.textDim(), true, 0.85F);
         String value = format(this.setting.getValue());
         float textWidth = context.textWidth(value) * 0.8F;
         context.drawText(value, x + width - textWidth, y + 1.0F, theme.accent(), true, 0.8F);
         float var8 = y + 26.0F - 2.0F;
         float normalizedValue = this.getNormalizedValue();
         int accentSecondary = ColorUtil.scaleAlpha(theme.accentSecondary(), 0.25F);
         context.drawBo(x, var8, width, 4.0F, 2.0F, accentSecondary);
         context.drawBo(x, var8, width * normalizedValue, 4.0F, 2.0F, theme.accent());
         boolean height = context.mouseX() >= this.getX()
            && context.mouseX() <= this.getX() + this.getWidth()
            && context.mouseY() >= this.getY()
            && context.mouseY() <= this.getY() + this.getHeight();
         boolean var12 = this.pressed || height;
         if (var12 != this.dragging) {
            this.dragAnimation.setTarget(var12 ? 1.0F : 0.0F);
            this.dragging = var12;
         }

         float get = 1.0F + 0.3F * this.dragAnimation.get();
         float var14 = 12.0F * get;
         float var15 = x + width * normalizedValue - var14 / 2.0F;
         float var16 = y + 26.0F - var14 / 2.0F;
         context.drawBo(var15, var16, var14, var14, var14 / 2.0F, theme.accent());
      }
   }

   private float getNormalizedValue(){
      double min = this.setting.getMax() - this.setting.getMin();
      return min <= 0.0 ? 0.0F : (float)((this.setting.getValue() - this.setting.getMin()) / min);
   }

   private static String format(double value){
      return value == Math.floor(value) && !Double.isInfinite(value) ? String.valueOf((long)value) : String.valueOf(Math.round(value * 100.0) / 100.0);
   }

   @Override
   protected boolean onMouseClicked(MouseClickEvent event){
      if (event.button() == MouseButton.LEFT && this.isWithin(event.x(), event.y())) {
         this.pressed = true;
         this.applyDrag(event.x());
         event.consume(this);
         return true;
      } else {
         return false;
      }
   }

   @Override
   protected boolean onMouseMoved(MouseMoveEvent event){
      if (this.pressed) {
         this.applyDrag(event.x());
         return true;
      } else {
         return false;
      }
   }

   @Override
   protected boolean onMouseReleased(MouseReleaseEvent event){
      if (this.pressed) {
         this.pressed = false;
         return true;
      } else {
         return false;
      }
   }

   private void applyDrag(double mouseX){
      float width = (float)((mouseX - this.getX()) / this.getWidth());
      width = width < 0.0F ? 0.0F : (width > 1.0F ? 1.0F : width);
      double min = this.setting.getMin() + width * (this.setting.getMax() - this.setting.getMin());
      this.setting.setValue(min);
   }
}

