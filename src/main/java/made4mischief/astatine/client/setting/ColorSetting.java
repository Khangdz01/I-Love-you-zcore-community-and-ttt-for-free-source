package made4mischief.astatine.client.setting;

import java.awt.Color;
import made4mischief.astatine.client.utils.render.core.ColorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ColorSetting extends Setting {
   private int value;
   private float hue;
   private float saturation;
   private float brightness;
   private String hexRgb;

   public ColorSetting(String name, int defaultColor){
      super(name);
      this.setValue(defaultColor);
   }

   public int getValue(){
      return this.value;
   }

   public void setValue(int color){
      this.value = ColorUtil.withAlpha(color, 255);
      float[] var2 = Color.RGBtoHSB(ColorUtil.red(this.value), ColorUtil.green(this.value), ColorUtil.blue(this.value), null);
      this.hue = var2[0];
      this.saturation = var2[1];
      this.brightness = var2[2];
      this.updateHexString();
   }

   public void setHsb(float hue, float saturation, float brightness){
      this.hue = positiveModulo(hue);
      this.saturation = clamp01(saturation);
      this.brightness = clamp01(brightness);
      this.value = 0xFF000000 | Color.HSBtoRGB(this.hue, this.saturation, this.brightness) & 16777215;
      this.updateHexString();
   }

   public float getHue(){
      return this.hue;
   }

   public float getSaturation(){
      return this.saturation;
   }

   public float getBrightness(){
      return this.brightness;
   }

   public String getHexRgb(){
      return this.hexRgb;
   }

   private void updateHexString(){
      this.hexRgb = String.format("#%06X", this.value & 16777215);
   }

   private static float clamp01(float value){
      return Math.max(0.0F, Math.min(1.0F, value));
   }

   private static float positiveModulo(float value){
      float var1 = value % 1.0F;
      return var1 < 0.0F ? var1 + 1.0F : var1;
   }
}

