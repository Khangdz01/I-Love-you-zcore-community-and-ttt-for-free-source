package made4mischief.astatine.client.gui.component.widget;

import java.util.function.Consumer;
import made4mischief.astatine.client.gui.component.AbstractComponent;
import made4mischief.astatine.client.gui.component.GuiRenderContext;
import made4mischief.astatine.client.gui.component.event.CharTypeEvent;
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
public class SearchBar extends AbstractComponent {
   private static final int KEY_BACKSPACE = 259;
   private static final int KEY_ESCAPE = 256;
   private static final float RADIUS = 14.0F;
   private static final float TEXT_X_INSET = 18.0F;
   private final StringBuilder queryBuilder = new StringBuilder();
   private final String placeholder;
   private Consumer<String> onChange;
   private final Animation focusAnimation = new Animation(0.0F, 1.0F, 200L, AnimationType.EASE_OUT);
   private boolean focused;
   private final Animation cursorBlinkAnimation = new Animation(0.0F, 1.0F, 1060L, AnimationType.LINEAR);

   public SearchBar(String placeholder, float x, float y, float width, float height){
      super(x, y, width, height);
      this.placeholder = placeholder;
      this.setFocusable(true);
      this.cursorBlinkAnimation.start();
   }

   public String getQuery(){
      return this.queryBuilder.toString();
   }

   public void clear(){
      if (this.queryBuilder.length() != 0) {
         this.queryBuilder.setLength(0);
         this.applyFilter();
      }
   }

   public void setOnChange(Consumer<String> onChange){
      this.onChange = onChange;
   }

   @Override
   public void render(GuiRenderContext context){
      if (this.isVisible()) {
         Theme theme = context.theme();
         if (this.cursorBlinkAnimation.isFinished()) {
            this.cursorBlinkAnimation.reset();
            this.cursorBlinkAnimation.start();
         }

         boolean focused = this.isFocused();
         if (focused != this.focused) {
            this.focusAnimation.setTarget(focused ? 1.0F : 0.0F);
            this.focused = focused;
         }

         float get = this.focusAnimation.get();
         float x = this.getX();
         float y = this.getY();
         float width = this.getWidth();
         float height = this.getHeight();
         int accent = ColorUtil.lerp(ColorUtil.scaleAlpha(theme.accent(), 0.55F), theme.accent(), get);
         context.drawBo(x - 1.5F, y - 1.5F, width + 3.0F, height + 3.0F, 15.5F, accent);
         int surfaceElevated = ColorUtil.lerp(theme.surfaceElevated(), -234873264, get);
         context.drawBo(x, y, width, height, 14.0F, surfaceElevated);
         float textHeight = y + (height - context.textHeight()) / 2.0F;
         if (this.queryBuilder.length() == 0 && !focused) {
            context.drawText(this.placeholder, x + 18.0F, textHeight, -1281305857, false);
         } else {
            String toString = this.queryBuilder.toString();
            context.drawText(toString, x + 18.0F, textHeight, theme.accent(), false);
            if (focused && this.cursorBlinkAnimation.get() < 0.5F) {
               float textWidth = x + 18.0F + context.textWidth(toString) + 1.0F;
               context.drawBo(textWidth, textHeight, 1.0F, context.textHeight(), 0.0F, theme.accent());
            }
         }
      }
   }

   @Override
   protected boolean onMouseClicked(MouseClickEvent event){
      if (event.button() == MouseButton.LEFT && this.isWithin(event.x(), event.y())) {
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
         this.queryBuilder.append(character);
         this.applyFilter();
         return true;
      } else {
         return false;
      }
   }

   @Override
   protected boolean onKeyPressed(KeyPressEvent event){
      if (event.keyCode() == 259) {
         if (this.queryBuilder.length() > 0) {
            this.queryBuilder.deleteCharAt(this.queryBuilder.length() - 1);
            this.applyFilter();
         }

         return true;
      } else {
         return event.keyCode() == 256 ? false : false;
      }
   }

   private void applyFilter(){
      if (this.onChange != null) {
         this.onChange.accept(this.queryBuilder.toString().toLowerCase().trim());
      }
   }
}

