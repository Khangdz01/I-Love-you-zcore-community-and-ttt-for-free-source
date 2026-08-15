package made4mischief.astatine.client.gui.component.setting;

import made4mischief.astatine.client.gui.component.GuiRenderContext;
import made4mischief.astatine.client.gui.component.event.MouseButton;
import made4mischief.astatine.client.gui.component.event.MouseClickEvent;
import made4mischief.astatine.client.gui.component.theme.Theme;
import made4mischief.astatine.client.setting.ActionSetting;
import made4mischief.astatine.client.utils.render.animation.Animation;
import made4mischief.astatine.client.utils.render.animation.AnimationType;
import made4mischief.astatine.client.utils.render.core.ColorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ActionComponent extends SettingComponent {
   private static final float BUTTON_WIDTH = 58.0F;
   private static final float BUTTON_HEIGHT = 16.0F;
   private final ActionSetting setting;
   private final Animation press = new Animation(0.0F, 0.0F, 180L, AnimationType.EASE_OUT);

   public ActionComponent(ActionSetting setting, float x, float y, float width){
      super(setting, x, y, width);
      this.setting = setting;
   }

   @Override
   public void render(GuiRenderContext context){
      if (this.isVisible()) {
         Theme theme = context.theme();
         float y = this.getY();
         float textHeight = y + (this.getHeight() - context.textHeight() * 0.85F) / 2.0F;
         context.drawText(this.getLabel().toUpperCase(), this.getX(), textHeight, theme.textDim(), true, 0.85F);
         float width = this.getX() + this.getWidth() - 58.0F;
         float height = y + (this.getHeight() - 16.0F) / 2.0F;
         float get = this.press.get();
         int accent = ColorUtil.lerp(ColorUtil.scaleAlpha(theme.accent(), 0.45F), theme.accent(), get);
         int accentSecondary = ColorUtil.lerp(ColorUtil.scaleAlpha(theme.accentSecondary(), 0.22F), ColorUtil.scaleAlpha(theme.accentSecondary(), 0.65F), get);
         context.drawBo(width - 1.0F, height - 1.0F, 60.0F, 18.0F, 6.0F, accent);
         context.drawBo(width, height, 58.0F, 16.0F, 5.0F, accentSecondary);
         float var10 = 0.72F;
         float textHeight2 = height + (16.0F - context.textHeight() * var10) / 2.0F;
         context.drawCenteredText(
            this.setting.getButtonLabel().toUpperCase(), width + 29.0F, textHeight2, ColorUtil.lerp(theme.textDim(), theme.text(), get), true, var10
         );
      }
   }

   @Override
   protected boolean onMouseClicked(MouseClickEvent event){
      if (event.button() == MouseButton.LEFT && this.isInButtonArea(event.x(), event.y())) {
         this.setting.invoke();
         this.press.snapTo(1.0F);
         this.press.setTarget(0.0F);
         event.consume(this);
         return true;
      } else {
         return false;
      }
   }

   private boolean isInButtonArea(double mouseX, double mouseY){
      float width = this.getX() + this.getWidth() - 58.0F;
      float height = this.getY() + (this.getHeight() - 16.0F) / 2.0F;
      return mouseX >= width && mouseX <= width + 58.0F && mouseY >= height && mouseY <= height + 16.0F;
   }
}

