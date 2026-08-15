package made4mischief.astatine.client.gui.component.setting;

import made4mischief.astatine.client.gui.component.GuiRenderContext;
import made4mischief.astatine.client.gui.component.event.MouseButton;
import made4mischief.astatine.client.gui.component.event.MouseClickEvent;
import made4mischief.astatine.client.gui.component.theme.Theme;
import made4mischief.astatine.client.setting.ModeSetting;
import made4mischief.astatine.client.utils.render.animation.Animation;
import made4mischief.astatine.client.utils.render.animation.AnimationType;
import made4mischief.astatine.client.utils.render.core.ColorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ModeComponent extends SettingComponent {
   private static final float BUTTON_HEIGHT = 15.0F;
   private static final float PADDING = 8.0F;
   private static final float RADIUS = 7.0F;
   private static final float MIN_WIDTH = 44.0F;
   private final ModeSetting setting;
   private final Animation hoverAnimation = new Animation(0.0F, 1.0F, 180L, AnimationType.EASE_OUT);
   private boolean hovered;

   public ModeComponent(ModeSetting setting, float x, float y, float width){
      super(setting, x, y, width);
      this.setting = setting;
   }

   @Override
   public void render(GuiRenderContext context){
      if (this.isVisible()) {
         Theme theme = context.theme();
         float x = this.getX();
         float y = this.getY();
         float width = this.getWidth();
         float height = this.getHeight();
         String value = this.setting.getValue();
         float textWidth = context.textWidth(value) * 0.8F;
         float max = Math.max(44.0F, textWidth + 16.0F);
         float var10 = x + width - max;
         float var11 = y + (height - 15.0F) / 2.0F;
         boolean mouseY = context.mouseX() >= var10 - 1.0F
            && context.mouseX() <= var10 + max + 1.0F
            && context.mouseY() >= var11 - 1.0F
            && context.mouseY() <= var11 + 15.0F + 1.0F;
         if (mouseY != this.hovered) {
            this.hoverAnimation.setTarget(mouseY ? 1.0F : 0.0F);
            this.hovered = mouseY;
         }

         float get = this.hoverAnimation.get();
         float textHeight = y + (height - context.textHeight() * 0.85F) / 2.0F;
         context.drawText(this.getLabel().toUpperCase(), x, textHeight, theme.textDim(), true, 0.85F);
         int accent2 = ColorUtil.scaleAlpha(theme.accent(), 0.35F);
         int accent = ColorUtil.scaleAlpha(theme.accent(), 0.78F);
         int lerp = ColorUtil.lerp(accent2, accent, get);
         context.drawBo(var10 - 1.0F, var11 - 1.0F, max + 2.0F, 17.0F, 8.0F, lerp);
         int surface = ColorUtil.scaleAlpha(theme.surface(), 0.92F);
         int accentSecondary = ColorUtil.lerp(theme.surfaceElevated(), theme.accentSecondary(), 0.14F);
         int lerp2 = ColorUtil.lerp(surface, accentSecondary, get);
         context.drawBo(var10, var11, max, 15.0F, 7.0F, lerp2);
         int accent3 = ColorUtil.lerp(theme.text(), theme.accent(), get);
         float textHeight2 = y + (height - context.textHeight() * 0.8F) / 2.0F;
         context.drawText(value, var10 + (max - textWidth) / 2.0F, textHeight2, accent3, true, 0.8F);
      }
   }

   @Override
   protected boolean onMouseClicked(MouseClickEvent event){
      if (!this.isWithin(event.x(), event.y())) {
         return false;
      } else if (event.button() == MouseButton.LEFT) {
         this.setting.cycle();
         event.consume(this);
         return true;
      } else if (event.button() == MouseButton.RIGHT) {
         this.cyclePrevious();
         event.consume(this);
         return true;
      } else {
         return false;
      }
   }

   private void cyclePrevious(){
      int size = this.setting.getModes().size();
      if (size > 1) {
         int index = (this.setting.getIndex() - 1 + size) % size;
         this.setting.setValue(this.setting.getModes().get(index));
      }
   }
}

