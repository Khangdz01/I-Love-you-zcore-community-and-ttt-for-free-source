package made4mischief.astatine.client.utils.render.renderer;

import made4mischief.astatine.client.utils.render.core.ColorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;

@Environment(EnvType.CLIENT)
public final class LineRenderer {
   private LineRenderer(){
   }

   public static void render(DrawContext ct, float x1, float y1, float x2, float y2, float thickness, int color){
      float var7 = x2 - x1;
      float var8 = y2 - y1;
      float sqrt = (float)Math.sqrt(var7 * var7 + var8 * var8);
      if (!(sqrt <= 0.0F) && !(thickness <= 0.0F) && ColorUtil.alpha(color) != 0) {
         Matrix3x2fStack matrix3x2fStack = ct.getMatrices();
         matrix3x2fStack.pushMatrix();
         matrix3x2fStack.translate(x1, y1);
         matrix3x2fStack.rotate((float)Math.atan2(var8, var7));
         matrix3x2fStack.translate(0.0F, -thickness * 0.5F);
         matrix3x2fStack.scale(sqrt, thickness);
         ct.fill(0, 0, 1, 1, color);
         matrix3x2fStack.popMatrix();
      }
   }
}

