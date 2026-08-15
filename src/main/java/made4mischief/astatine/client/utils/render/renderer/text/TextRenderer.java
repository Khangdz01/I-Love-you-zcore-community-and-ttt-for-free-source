package made4mischief.astatine.client.utils.render.renderer.text;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;

@Environment(EnvType.CLIENT)
public final class TextRenderer {
   private static FontBackend backend = new MinecraftFontBackend();

   private TextRenderer(){
   }

   public static void setBackend(FontBackend newBackend){
      if (newBackend != null) {
         backend = newBackend;
      }
   }

   public static void drawLeft(DrawContext ct, String text, float x, float y, int color, boolean shadow, float scale){
      draw(ct, text, x, y, color, shadow, scale);
   }

   public static void drawCentered(DrawContext ct, String text, float x, float y, int color, boolean shadow, float scale){
      if (!isBlank(text) && !(scale <= 0.0F)) {
         float width = x - backend.getWidth(text) * scale * 0.5F;
         draw(ct, text, width, y, color, shadow, scale);
      }
   }

   public static void drawRight(DrawContext ct, String text, float x, float y, int color, boolean shadow, float scale){
      if (!isBlank(text) && !(scale <= 0.0F)) {
         float width = x - backend.getWidth(text) * scale;
         draw(ct, text, width, y, color, shadow, scale);
      }
   }

   public static float measureWidth(String text, float scale){
      return isBlank(text) ? 0.0F : backend.getWidth(text) * scale;
   }

   public static float measureHeight(float scale){
      return backend.getHeight() * scale;
   }

   private static void draw(DrawContext ct, String text, float x, float y, int color, boolean shadow, float scale){
      if (!isBlank(text) && !(scale <= 0.0F)) {
         if (scale == 1.0F) {
            backend.draw(ct, text, Math.round(x), Math.round(y), color, shadow);
         } else {
            Matrix3x2fStack matrix3x2fStack = ct.getMatrices();
            matrix3x2fStack.pushMatrix();
            matrix3x2fStack.translate(x, y);
            matrix3x2fStack.scale(scale, scale);
            backend.draw(ct, text, 0, 0, color, shadow);
            matrix3x2fStack.popMatrix();
         }
      }
   }

   private static boolean isBlank(String text){
      return text == null || text.isEmpty();
   }
}

