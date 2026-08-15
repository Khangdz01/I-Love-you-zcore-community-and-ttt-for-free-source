package made4mischief.astatine.client.gui.component.setting;

import made4mischief.astatine.client.gui.component.GuiRenderContext;
import made4mischief.astatine.client.gui.component.event.MouseButton;
import made4mischief.astatine.client.gui.component.event.MouseClickEvent;
import made4mischief.astatine.client.gui.component.theme.Theme;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.utils.render.animation.Animation;
import made4mischief.astatine.client.utils.render.animation.AnimationType;
import made4mischief.astatine.client.utils.render.core.ColorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class BooleanComponent extends SettingComponent {
   private static final float SWITCH_WIDTH = 30.0F;
   private static final float SWITCH_HEIGHT = 16.0F;
   private static final float KNOB_SIZE = 10.0F;
   private final BooleanSetting setting;
   private final Animation switchAnimation;

   public BooleanComponent(BooleanSetting setting, float x, float y, float width){
      super(setting, x, y, width);
      this.setting = setting;
      float value = setting.getValue() ? 1.0F : 0.0F;
      this.switchAnimation = new Animation(value, value, 300L, AnimationType.EASE_OUT);
   }

   @Override
   public void render(GuiRenderContext context){
      if (this.isVisible()) {
         Theme theme = context.theme();
         float x = this.getX();
         float y = this.getY();
         float width = this.getWidth();
         float height = this.getHeight();
         float get = this.switchAnimation.get();
         float textHeight = y + (height - context.textHeight() * 0.85F) / 2.0F;
         context.drawText(this.getLabel().toUpperCase(), x, textHeight, theme.textDim(), true, 0.85F);
         float var9 = x + width - 30.0F;
         float var10 = y + (height - 16.0F) / 2.0F;
         int accentSecondary = ColorUtil.lerp(ColorUtil.scaleAlpha(theme.border(), 1.0F), theme.accentSecondary(), get);
         context.drawBo(var9, var10, 30.0F, 16.0F, 8.0F, accentSecondary);
         float var12 = 16.0F;
         float var13 = var9 + 2.0F + var12 * get;
         float var14 = var10 + 3.0F;
         int text = ColorUtil.lerp(theme.textDim(), theme.text(), get);
         context.drawBo(var13, var14, 10.0F, 10.0F, 5.0F, text);
      }
   }

   @Override
   protected boolean onMouseClicked(MouseClickEvent event){
      if (event.button() == MouseButton.LEFT && this.isWithin(event.x(), event.y())) {
         this.setting.toggle();
         this.switchAnimation.setTarget(this.setting.getValue() ? 1.0F : 0.0F);
         event.consume(this);
         return true;
      } else {
         return false;
      }
   }
}

