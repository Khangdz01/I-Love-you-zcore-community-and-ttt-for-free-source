package made4mischief.astatine.client.gui.component.setting;

import made4mischief.astatine.client.gui.component.GuiRenderContext;
import made4mischief.astatine.client.gui.component.event.CharTypeEvent;
import made4mischief.astatine.client.gui.component.event.KeyPressEvent;
import made4mischief.astatine.client.gui.component.event.MouseButton;
import made4mischief.astatine.client.gui.component.event.MouseClickEvent;
import made4mischief.astatine.client.gui.component.theme.Theme;
import made4mischief.astatine.client.setting.StringSetting;
import made4mischief.astatine.client.utils.render.animation.Animation;
import made4mischief.astatine.client.utils.render.animation.AnimationType;
import made4mischief.astatine.client.utils.render.core.ColorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class StringComponent extends SettingComponent {
   private static final int KEY_A = 65;
   private static final int KEY_V = 86;
   private static final int KEY_BACKSPACE = 259;
   private static final int MODIFIER_CONTROL = 2;
   private static final int MODIFIER_SUPER = 8;
   private static final float INPUT_WIDTH = 96.0F;
   private static final float INPUT_HEIGHT = 16.0F;
   private static final float INPUT_RADIUS = 5.0F;
   private static final float TEXT_X_INSET = 5.0F;
   private final StringSetting setting;
   private final Animation focusAnimation = new Animation(0.0F, 1.0F, 180L, AnimationType.EASE_OUT);
   private final Animation cursorBlinkAnimation = new Animation(0.0F, 1.0F, 1060L, AnimationType.LINEAR);
   private boolean focused;
   private boolean selectAll;

   public StringComponent(StringSetting setting, float x, float y, float width){
      super(setting, x, y, width);
      this.setting = setting;
      this.setFocusable(true);
      this.cursorBlinkAnimation.start();
   }

   @Override
   public void render(GuiRenderContext context){
      if (this.isVisible()) {
         if (this.cursorBlinkAnimation.isFinished()) {
            this.cursorBlinkAnimation.reset();
            this.cursorBlinkAnimation.start();
         }

         if (this.isFocused() != this.focused) {
            this.focusAnimation.setTarget(this.isFocused() ? 1.0F : 0.0F);
            this.focused = this.isFocused();
            this.cursorBlinkAnimation.reset();
            this.cursorBlinkAnimation.start();
         }

         Theme theme = context.theme();
         float textHeight2 = this.getY() + (this.getHeight() - context.textHeight() * 0.85F) / 2.0F;
         context.drawText(this.getLabel().toUpperCase(), this.getX(), textHeight2, theme.textDim(), true, 0.85F);
         float width = this.getX() + this.getWidth() - 96.0F;
         float height = this.getY() + (this.getHeight() - 16.0F) / 2.0F;
         float get = this.focusAnimation.get();
         int accent = ColorUtil.lerp(ColorUtil.scaleAlpha(theme.accent(), 0.35F), ColorUtil.scaleAlpha(theme.accent(), 0.85F), get);
         int surfaceElevated = ColorUtil.lerp(theme.surface(), theme.surfaceElevated(), get);
         context.drawBo(width - 1.0F, height - 1.0F, 98.0F, 18.0F, 6.0F, accent);
         context.drawBo(width, height, 96.0F, 16.0F, 5.0F, surfaceElevated);
         String focused = this.truncateToFit(context, this.setting.getValue(), 86.0F - (this.isFocused() ? 2.0F : 0.0F));
         float textHeight = this.getY() + (this.getHeight() - context.textHeight() * 0.8F) / 2.0F;
         if (this.isFocused() && this.selectAll && !focused.isEmpty()) {
            context.drawBo(
               width + 5.0F - 1.0F,
               textHeight - 1.0F,
               context.textWidth(focused) * 0.8F + 2.0F,
               context.textHeight() * 0.8F + 2.0F,
               1.0F,
               ColorUtil.scaleAlpha(theme.accent(), 0.35F)
            );
         }

         context.drawText(focused, width + 5.0F, textHeight, theme.text(), false, 0.8F);
         if (this.isFocused() && this.cursorBlinkAnimation.get() < 0.5F) {
            float textWidth = width + 5.0F + context.textWidth(focused) * 0.8F + 1.0F;
            context.drawBo(textWidth, textHeight, 1.0F, context.textHeight() * 0.8F, 0.0F, theme.accent());
         }
      }
   }

   private String truncateToFit(GuiRenderContext context, String value, float availableWidth){
      int index = 0;

      while (index < value.length() && context.textWidth(value.substring(index)) * 0.8F > availableWidth) {
         index++;
      }

      return value.substring(index);
   }

   @Override
   protected boolean onMouseClicked(MouseClickEvent event){
      if (event.button() == MouseButton.LEFT && this.isInButtonArea(event.x(), event.y())) {
         this.selectAll = false;
         event.consume(this);
         return true;
      } else {
         return false;
      }
   }

   @Override
   protected boolean onCharTyped(CharTypeEvent event){
      char character = event.character();
      if (character >= ' ' && character != 127) {
         this.setting.setValue(this.selectAll ? String.valueOf(character) : this.setting.getValue() + character);
         this.selectAll = false;
         this.restartCursorBlink();
         event.consume(this);
         return true;
      } else {
         return false;
      }
   }

   @Override
   protected boolean onKeyPressed(KeyPressEvent event){
      boolean modifiers = (event.modifiers() & 10) != 0;
      if (modifiers && event.keyCode() == 65) {
         this.selectAll = true;
         event.consume(this);
         return true;
      } else if (modifiers && event.keyCode() == 86) {
         String clipboard = this.sanitizeClipboard(event.clipboard());
         this.setting.setValue(this.selectAll ? clipboard : this.setting.getValue() + clipboard);
         this.selectAll = false;
         this.restartCursorBlink();
         event.consume(this);
         return true;
      } else if (event.keyCode() == 259) {
         String value = this.setting.getValue();
         if (this.selectAll) {
            this.setting.setValue("");
         } else if (!value.isEmpty()) {
            this.setting.setValue(value.substring(0, value.length() - 1));
         }

         this.selectAll = false;
         this.restartCursorBlink();
         event.consume(this);
         return true;
      } else {
         return false;
      }
   }

   private String sanitizeClipboard(String clipboard){
      StringBuilder builder = new StringBuilder(clipboard.length());

      for (int index = 0; index < clipboard.length(); index++) {
         char charAt = clipboard.charAt(index);
         if (charAt >= ' ' && charAt != 127) {
            builder.append(charAt);
         }
      }

      return builder.toString();
   }

   private void restartCursorBlink(){
      this.cursorBlinkAnimation.reset();
      this.cursorBlinkAnimation.start();
   }

   private boolean isInButtonArea(double mouseX, double mouseY){
      float width = this.getX() + this.getWidth() - 96.0F;
      float height = this.getY() + (this.getHeight() - 16.0F) / 2.0F;
      return mouseX >= width && mouseX <= width + 96.0F && mouseY >= height && mouseY <= height + 16.0F;
   }
}

