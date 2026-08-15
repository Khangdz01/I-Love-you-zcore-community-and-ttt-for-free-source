package made4mischief.astatine.client.gui.component.setting;

import java.awt.Color;
import made4mischief.astatine.client.gui.component.GuiRenderContext;
import made4mischief.astatine.client.gui.component.event.MouseButton;
import made4mischief.astatine.client.gui.component.event.MouseClickEvent;
import made4mischief.astatine.client.gui.component.event.MouseMoveEvent;
import made4mischief.astatine.client.gui.component.event.MouseReleaseEvent;
import made4mischief.astatine.client.gui.component.theme.Theme;
import made4mischief.astatine.client.setting.ColorSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ColorPickerComponent extends SettingComponent {
   private static final float GRID_Y_OFFSET = 23.0F;
   private static final float GRID_HEIGHT = 52.0F;
   private static final float HUE_STRIP_WIDTH = 12.0F;
   private static final float GRID_GAP = 6.0F;
   private static final float HEIGHT = 77.0F;
   private static final int GRID_COLUMNS = 18;
   private static final int GRID_ROWS = 9;
   private static final int HUE_STRIP_CELLS = 12;
   private final ColorSetting setting;
   private ColorPickerComponent.DragTarget dragTarget = ColorPickerComponent.DragTarget.NONE;

   public ColorPickerComponent(ColorSetting setting, float x, float y, float width){
      super(setting, x, y, width);
      this.setting = setting;
      this.setSize(width, 77.0F);
   }

   @Override
   public float getHeight(){
      return 77.0F;
   }

   @Override
   public void render(GuiRenderContext context){
      if (this.isVisible()) {
         Theme theme = context.theme();
         float x = this.getX();
         float y = this.getY();
         float width = this.getWidth();
         float var6 = width - 12.0F - 6.0F;
         context.drawText(this.getLabel().toUpperCase(), x, y + 1.0F, theme.textDim(), true, 0.85F);
         String hexRgb = this.setting.getHexRgb();
         float textWidth = context.textWidth(hexRgb) * 0.8F;
         float var9 = 12.0F;
         float var10 = x + width - var9;
         context.drawBo(var10 - 1.0F, y + 1.0F, var9 + 2.0F, var9 + 2.0F, 4.0F, theme.border());
         context.drawBo(var10, y + 2.0F, var9, var9, 3.0F, this.setting.getValue());
         context.drawText(hexRgb, var10 - 5.0F - textWidth, y + 1.0F, theme.accent(), true, 0.8F);
         this.drawSaturationGrid(context, x, y + 23.0F, var6);
         this.drawHueStrip(context, x + var6 + 6.0F, y + 23.0F);
         this.drawSelectors(context, theme, x, y + 23.0F, var6);
      }
   }

   private void drawSaturationGrid(GuiRenderContext context, float x, float y, float width){
      float var5 = width / 18.0F;
      float var6 = 5.7777777F;

      for (int index2 = 0; index2 < 9; index2++) {
         float var8 = 1.0F - (index2 + 0.5F) / 9.0F;

         for (int index = 0; index < 18; index++) {
            float var10 = (index + 0.5F) / 18.0F;
            int hue = 0xFF000000 | Color.HSBtoRGB(this.setting.getHue(), var10, var8) & 16777215;
            context.drawBo(x + index * var5, y + index2 * var6, var5 + 0.5F, var6 + 0.5F, 0.0F, hue);
         }
      }
   }

   private void drawHueStrip(GuiRenderContext context, float x, float y){
      float var4 = 4.3333335F;

      for (int index = 0; index < 12; index++) {
         float var6 = (index + 0.5F) / 12.0F;
         int hSBtoRGB = 0xFF000000 | Color.HSBtoRGB(var6, 1.0F, 1.0F) & 16777215;
         context.drawBo(x, y + index * var4, 12.0F, var4 + 0.5F, 0.0F, hSBtoRGB);
      }
   }

   private void drawSelectors(GuiRenderContext context, Theme theme, float x, float y, float squareWidth){
      float var6 = 5.0F;
      float saturation = x + this.setting.getSaturation() * squareWidth;
      float brightness = y + (1.0F - this.setting.getBrightness()) * 52.0F;
      context.drawBo(saturation - var6 / 2.0F - 1.0F, brightness - var6 / 2.0F - 1.0F, var6 + 2.0F, var6 + 2.0F, var6 / 2.0F + 1.0F, -16777216);
      context.drawBo(saturation - var6 / 2.0F, brightness - var6 / 2.0F, var6, var6, var6 / 2.0F, -1);
      float hue = y + this.setting.getHue() * 52.0F;
      float var10 = x + squareWidth + 6.0F;
      context.drawBo(var10 - 2.0F, hue - 2.0F, 16.0F, 4.0F, 2.0F, theme.text());
   }

   @Override
   protected boolean onMouseClicked(MouseClickEvent event){
      if (event.button() == MouseButton.LEFT && this.isWithin(event.x(), event.y())) {
         this.dragTarget = this.getDragTarget(event.x(), event.y());
         if (this.dragTarget == ColorPickerComponent.DragTarget.NONE) {
            return false;
         } else {
            this.applyDrag(event.x(), event.y());
            event.consume(this);
            return true;
         }
      } else {
         return false;
      }
   }

   @Override
   protected boolean onMouseMoved(MouseMoveEvent event){
      if (this.dragTarget == ColorPickerComponent.DragTarget.NONE) {
         return false;
      } else {
         this.applyDrag(event.x(), event.y());
         return true;
      }
   }

   @Override
   protected boolean onMouseReleased(MouseReleaseEvent event){
      if (this.dragTarget == ColorPickerComponent.DragTarget.NONE) {
         return false;
      } else {
         this.dragTarget = ColorPickerComponent.DragTarget.NONE;
         return true;
      }
   }

   private ColorPickerComponent.DragTarget getDragTarget(double mouseX, double mouseY){
      float x = this.getX();
      float y = this.getY() + 23.0F;
      float width = this.getWidth() - 12.0F - 6.0F;
      if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 52.0F) {
         return ColorPickerComponent.DragTarget.SATURATION_BRIGHTNESS;
      } else {
         float var8 = x + width + 6.0F;
         return mouseX >= var8 && mouseX <= var8 + 12.0F && mouseY >= y && mouseY <= y + 52.0F
            ? ColorPickerComponent.DragTarget.HUE
            : ColorPickerComponent.DragTarget.NONE;
      }
   }

   private void applyDrag(double mouseX, double mouseY){
      float x = this.getX();
      float y = this.getY() + 23.0F;
      float width = this.getWidth() - 12.0F - 6.0F;
      if (this.dragTarget == ColorPickerComponent.DragTarget.SATURATION_BRIGHTNESS) {
         float var8 = clamp01((float)((mouseX - x) / width));
         float var9 = 1.0F - clamp01((float)((mouseY - y) / 52.0));
         this.setting.setHsb(this.setting.getHue(), var8, var9);
      } else if (this.dragTarget == ColorPickerComponent.DragTarget.HUE) {
         float var10 = clamp01((float)((mouseY - y) / 52.0));
         this.setting.setHsb(var10, this.setting.getSaturation(), this.setting.getBrightness());
      }
   }

   private static float clamp01(float value){
      return Math.max(0.0F, Math.min(1.0F, value));
   }

   @Environment(EnvType.CLIENT)
   private static enum DragTarget {
      NONE,
      SATURATION_BRIGHTNESS,
      HUE;
   }
}

