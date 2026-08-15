package made4mischief.astatine.client.gui.component.setting;

import java.util.Locale;
import made4mischief.astatine.client.gui.component.GuiRenderContext;
import made4mischief.astatine.client.gui.component.event.KeyPressEvent;
import made4mischief.astatine.client.gui.component.event.MouseButton;
import made4mischief.astatine.client.gui.component.event.MouseClickEvent;
import made4mischief.astatine.client.gui.component.theme.Theme;
import made4mischief.astatine.client.setting.KeybindSetting;
import made4mischief.astatine.client.utils.render.core.ColorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class KeybindComponent extends SettingComponent {
   private static final float BUTTON_WIDTH = 42.0F;
   private static final float BUTTON_HEIGHT = 16.0F;
   private static final float TEXT_SCALE = 0.78F;
   private final KeybindSetting setting;
   private boolean recording;

   public KeybindComponent(KeybindSetting setting, float x, float y, float width){
      super(setting, x, y, width);
      this.setting = setting;
      this.setFocusable(true);
   }

   @Override
   public void render(GuiRenderContext context){
      if (this.isVisible()) {
         Theme theme = context.theme();
         float y = this.getY();
         float textHeight2 = y + (this.getHeight() - context.textHeight() * 0.85F) / 2.0F;
         context.drawText(this.getLabel().toUpperCase(), this.getX(), textHeight2, theme.textDim(), true, 0.85F);
         float width = this.getX() + this.getWidth() - 42.0F;
         float height = y + (this.getHeight() - 16.0F) / 2.0F;
         int accent = this.recording ? theme.accent() : ColorUtil.scaleAlpha(theme.accent(), 0.35F);
         int accentSecondary = this.recording ? ColorUtil.scaleAlpha(theme.accentSecondary(), 0.42F) : 184549375;
         int textDim = this.recording ? theme.text() : theme.textDim();
         context.drawBo(width - 1.0F, height - 1.0F, 44.0F, 18.0F, 6.0F, accent);
         context.drawBo(width, height, 42.0F, 16.0F, 5.0F, accentSecondary);
         String value = this.recording ? "..." : getKeyName(this.setting.getValue());
         float var11 = 0.78F;
         float var12 = 36.0F;
         float textWidth = context.textWidth(value) * var11;
         if (textWidth > var12) {
            var11 *= var12 / textWidth;
         }

         float textHeight = height + (16.0F - context.textHeight() * var11) / 2.0F;
         context.drawCenteredText(value, width + 21.0F, textHeight, textDim, false, var11);
      }
   }

   @Override
   protected boolean onMouseClicked(MouseClickEvent event){
      if (event.button() == MouseButton.LEFT && this.isInKeybindArea(event.x(), event.y())) {
         this.recording = true;
         event.consume(this);
         return true;
      } else {
         return false;
      }
   }

   @Override
   protected boolean onKeyPressed(KeyPressEvent event){
      if (!this.recording) {
         return false;
      } else if (event.keyCode() == 256) {
         this.recording = false;
         return true;
      } else {
         if (event.keyCode() == 259 || event.keyCode() == 261) {
            this.setting.setValue(-1);
         } else if (event.keyCode() != -1) {
            this.setting.setValue(event.keyCode());
         }

         this.recording = false;
         event.consume(this);
         return true;
      }
   }

   @Override
   public void setFocused(boolean focused){
      super.setFocused(focused);
      if (!focused) {
         this.recording = false;
      }
   }

   private boolean isInKeybindArea(double x, double y){
      float width = this.getX() + this.getWidth() - 42.0F;
      float height = this.getY() + (this.getHeight() - 16.0F) / 2.0F;
      return x >= width && x <= width + 42.0F && y >= height && y <= height + 16.0F;
   }

   private static String getKeyName(int key){
      if (key == -1) {
         return "-";
      } else {
         String glfwGetKeyName = GLFW.glfwGetKeyName(key, 0);
         if (glfwGetKeyName != null && !glfwGetKeyName.isBlank()) {
            return glfwGetKeyName.toUpperCase(Locale.ROOT);
         } else if (key >= 290 && key <= 314) {
            return "F" + (key - 290 + 1);
         } else {
            return switch (key) {
               case 32 -> "SPACE";
               case 256 -> "ESC";
               case 257, 335 -> "ENTER";
               case 258 -> "TAB";
               case 260 -> "INS";
               case 261 -> "DEL";
               case 262 -> "RIGHT";
               case 263 -> "LEFT";
               case 264 -> "DOWN";
               case 265 -> "UP";
               case 266 -> "PGUP";
               case 267 -> "PGDN";
               case 268 -> "HOME";
               case 269 -> "END";
               case 340 -> "LSHIFT";
               case 341 -> "LCTRL";
               case 342 -> "LALT";
               case 344 -> "RSHIFT";
               case 345 -> "RCTRL";
               case 346 -> "RALT";
               default -> "KEY";
            };
         }
      }
   }
}

