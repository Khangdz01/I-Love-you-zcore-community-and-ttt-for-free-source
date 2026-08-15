package made4mischief.astatine.client.utils.render.core;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ColorUtil {
   private ColorUtil(){
   }

   public static int alpha(int color){
      return color >>> 24;
   }

   public static int red(int color){
      return color >> 16 & 0xFF;
   }

   public static int green(int color){
      return color >> 8 & 0xFF;
   }

   public static int blue(int color){
      return color & 0xFF;
   }

   public static int pack(int a, int r, int g, int b){
      return clampByte(a) << 24 | clampByte(r) << 16 | clampByte(g) << 8 | clampByte(b);
   }

   public static int withAlpha(int color, int a){
      return clampByte(a) << 24 | color & 16777215;
   }

   public static int scaleAlpha(int color, float coverage){
      float var2 = coverage < 0.0F ? 0.0F : (coverage > 1.0F ? 1.0F : coverage);
      int round = Math.round(alpha(color) * var2);
      return withAlpha(color, round);
   }

   public static int lerp(int from, int to, float t){
      float var3 = t < 0.0F ? 0.0F : (t > 1.0F ? 1.0F : t);
      int round4 = Math.round(alpha(from) + (alpha(to) - alpha(from)) * var3);
      int round3 = Math.round(red(from) + (red(to) - red(from)) * var3);
      int round2 = Math.round(green(from) + (green(to) - green(from)) * var3);
      int round = Math.round(blue(from) + (blue(to) - blue(from)) * var3);
      return pack(round4, round3, round2, round);
   }

   private static int clampByte(int v){
      return v < 0 ? 0 : (v > 255 ? 255 : v);
   }
}
