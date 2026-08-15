package made4mischief.astatine.client.gui.component.widget;

import java.util.Locale;
import java.util.function.Consumer;
import made4mischief.astatine.client.gui.component.AbstractComponent;
import made4mischief.astatine.client.gui.component.GuiRenderContext;
import made4mischief.astatine.client.gui.component.event.KeyPressEvent;
import made4mischief.astatine.client.gui.component.event.MouseButton;
import made4mischief.astatine.client.gui.component.event.MouseClickEvent;
import made4mischief.astatine.client.gui.component.theme.Theme;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.utils.render.animation.Animation;
import made4mischief.astatine.client.utils.render.animation.AnimationType;
import made4mischief.astatine.client.utils.render.core.ColorUtil;
import made4mischief.astatine.client.utils.render.core.SoundUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class ModuleRow extends AbstractComponent {
   private static final float TOGGLE_WIDTH = 34.0F;
   private static final float TOGGLE_HEIGHT = 18.0F;
   private static final float BADGE_X_INSET = 11.0F;
   private static final float KNOB_SIZE = 11.0F;
   private static final float BADGE_SIZE = 20.0F;
   private static final float NAME_X_OFFSET = 20.0F;
   private static final float NAME_X_GAP = 9.0F;
   private static final float KEYBIND_TEXT_SCALE = 0.78F;
   private static final float TOGGLE_RIGHT_INSET = 4.0F;
   private static final float KNOB_INSET = 2.0F;
   private static final float RADIUS = 11.0F;
   private static final float INDICATOR_WIDTH = 3.0F;
   private static final float BADGE_RADIUS = 7.0F;
   private final Module module;
   private final Animation hoverAnimation = new Animation(0.0F, 1.0F, 200L, AnimationType.EASE_OUT);
   private final Animation enabledAnimation;
   private boolean hovered;
   private boolean capturingKeybind;
   private boolean renderedEnabled;
   private Consumer<ModuleRow> onConfigRequest;

   public ModuleRow(Module module, float x, float y, float width, float height){
      super(x, y, width, height);
      this.module = module;
      float enabled = module.isEnabled() ? 1.0F : 0.0F;
      this.enabledAnimation = new Animation(enabled, enabled, 350L, AnimationType.EASE_OUT);
      this.renderedEnabled = module.isEnabled();
      this.setFocusable(true);
   }

   public Module getModule(){
      return this.module;
   }

   public void setOnConfigRequest(Consumer<ModuleRow> onConfigRequest){
      this.onConfigRequest = onConfigRequest;
   }

   @Override
   public void render(GuiRenderContext context){
      if (this.isVisible()) {
         Theme theme = context.theme();
         if (this.renderedEnabled != this.module.isEnabled()) {
            this.renderedEnabled = this.module.isEnabled();
            this.enabledAnimation.setTarget(this.renderedEnabled ? 1.0F : 0.0F);
         }

         boolean height2 = context.mouseX() >= this.getX()
            && context.mouseX() <= this.getX() + this.getWidth()
            && context.mouseY() >= this.getY()
            && context.mouseY() <= this.getY() + this.getHeight();
         if (height2 != this.hovered) {
            this.hoverAnimation.setTarget(height2 ? 1.0F : 0.0F);
            if (height2) {
               SoundUtil.playHover();
            }

            this.hovered = height2;
         }

         float get2 = this.hoverAnimation.get();
         float get = this.enabledAnimation.get();
         float var6 = get2 * (1.0F - 0.68F * get);
         float x = this.getX();
         float y = this.getY();
         float width = this.getWidth();
         float height = this.getHeight();
         if (get > 0.001F) {
            int accent = ColorUtil.scaleAlpha(theme.accent(), 0.34F * get);
            context.drawBo(x - 1.0F, y - 1.0F, width + 2.0F, height + 2.0F, 12.0F, accent);
         }

         int var15 = 117440511;
         int hover = ColorUtil.lerp(var15, theme.hover(), var6);
         int accent2 = ColorUtil.scaleAlpha(theme.accent(), 0.17F);
         hover = ColorUtil.lerp(hover, accent2, get);
         context.drawBo(x, y, width, height, 11.0F, hover);
         this.drawEnabledIndicator(context, theme, x, y, height, get);
         this.drawKeybindBadge(context, theme, x, y, height, get);
         this.drawModuleName(context, theme, x, y, height, var6, get);
         this.drawToggleSwitch(context, theme, x, y, width, height, get);
      }
   }

   private void drawEnabledIndicator(GuiRenderContext context, Theme theme, float x, float y, float h, float enabled){
      if (!(enabled <= 0.001F)) {
         float var7 = h - 14.0F;
         float var8 = var7 * enabled;
         float var9 = y + (h - var8) / 2.0F;
         context.drawBo(x + 2.0F, var9, 3.0F, var8, 1.5F, ColorUtil.scaleAlpha(theme.accent(), 0.78F * enabled));
      }
   }

   private void drawModuleName(GuiRenderContext context, Theme theme, float x, float y, float h, float hover, float enabled){
      int textDim = theme.textDim();
      int text = theme.text();
      int text2 = ColorUtil.lerp(theme.text(), -1, 0.38F * enabled);
      int lerp = ColorUtil.lerp(textDim, text, hover);
      lerp = ColorUtil.lerp(lerp, text2, enabled);
      float var12 = x + 11.0F + 20.0F + 9.0F;
      float textHeight = y + (h - context.textHeight()) / 2.0F;
      context.drawText(this.module.getName(), var12, textHeight, lerp, false);
   }

   private void drawKeybindBadge(GuiRenderContext context, Theme theme, float x, float y, float h, float enabled){
      float var7 = x + 11.0F;
      float var8 = y + (h - 20.0F) / 2.0F;
      int accent = this.capturingKeybind ? theme.accent() : ColorUtil.scaleAlpha(theme.accent(), 0.22F + enabled * 0.18F);
      int accent2 = this.capturingKeybind ? theme.accent() : 184549375;
      int accent3 = this.capturingKeybind ? theme.background() : ColorUtil.lerp(theme.textDim(), ColorUtil.scaleAlpha(theme.accent(), 0.8F), enabled);
      context.drawBo(var7 - 1.0F, var8 - 1.0F, 22.0F, 22.0F, 7.0F, accent);
      context.drawBo(var7, var8, 20.0F, 20.0F, 6.0F, accent2);
      String keybind = this.capturingKeybind ? "..." : getKeyName(this.module.getKeybind());
      float var13 = 0.78F;
      float textWidth = context.textWidth(keybind) * var13;
      float var15 = 16.0F;
      if (textWidth > var15) {
         var13 *= var15 / textWidth;
      }

      float var16 = 2.0F * var13;
      float var17 = var7 + 10.0F - var16;
      float textHeight = var8 + (20.0F - context.textHeight() * var13) / 2.0F - var16;
      context.drawCenteredText(keybind, var17, textHeight, accent3, false, var13);
   }

   private void drawToggleSwitch(GuiRenderContext context, Theme theme, float x, float y, float w, float h, float enabled){
      float var8 = x + w - 11.0F - 34.0F;
      float var9 = y + (h - 18.0F) / 2.0F;
      int textDim = ColorUtil.scaleAlpha(theme.textDim(), 0.12F);
      int accent = ColorUtil.lerp(theme.accentSecondary(), theme.accent(), 0.48F);
      int lerp = ColorUtil.lerp(textDim, accent, enabled);
      context.drawBo(var8, var9, 34.0F, 18.0F, 9.0F, lerp);
      float var13 = 19.0F;
      float var14 = var8 + 2.0F + var13 * enabled;
      float var15 = var9 + 3.5F;
      int text = ColorUtil.lerp(ColorUtil.scaleAlpha(theme.textDim(), 0.52F), theme.text(), enabled);
      context.drawBo(var14, var15, 11.0F, 11.0F, 5.5F, text);
   }

   @Override
   protected boolean onMouseClicked(MouseClickEvent event){
      if (!this.isWithin(event.x(), event.y())) {
         return false;
      } else if (event.button() == MouseButton.LEFT) {
         SoundUtil.playClick();
         if (this.isHoveringKeybindBadge(event.x(), event.y())) {
            this.capturingKeybind = true;
            event.consume(this);
            return true;
         } else {
            this.module.toggle();
            this.enabledAnimation.setTarget(this.module.isEnabled() ? 1.0F : 0.0F);
            return true;
         }
      } else if (event.button() == MouseButton.RIGHT) {
         SoundUtil.playClick();
         if (this.onConfigRequest != null) {
            this.onConfigRequest.accept(this);
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   protected boolean onKeyPressed(KeyPressEvent event){
      if (this.capturingKeybind) {
         if (event.keyCode() == 256) {
            this.capturingKeybind = false;
            return true;
         } else {
            if (event.keyCode() == 259 || event.keyCode() == 261) {
               this.module.setKeybind(-1);
            } else if (event.keyCode() != -1) {
               this.module.setKeybind(event.keyCode());
            }

            this.capturingKeybind = false;
            return true;
         }
      } else {
         return false;
      }
   }

   private boolean isHoveringKeybindBadge(double pointX, double pointY){
      float x = this.getX() + 11.0F;
      float height = this.getY() + (this.getHeight() - 20.0F) / 2.0F;
      return pointX >= x && pointX <= x + 20.0F && pointY >= height && pointY <= height + 20.0F;
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
               case 32 -> "SPC";
               case 256 -> "ESC";
               case 257, 335 -> "ENT";
               case 258 -> "TAB";
               case 260 -> "INS";
               case 261 -> "DEL";
               case 262 -> "RT";
               case 263 -> "LT";
               case 264 -> "DN";
               case 265 -> "UP";
               case 266 -> "PGU";
               case 267 -> "PGD";
               case 268 -> "HOM";
               case 269 -> "END";
               case 280 -> "CAP";
               case 281 -> "SCR";
               case 282 -> "NUM";
               case 283 -> "PRT";
               case 284 -> "PAU";
               case 340 -> "LSH";
               case 341 -> "LCT";
               case 342 -> "LAT";
               case 344 -> "RSH";
               case 345 -> "RCT";
               case 346 -> "RAT";
               default -> "KEY";
            };
         }
      }
   }
}

