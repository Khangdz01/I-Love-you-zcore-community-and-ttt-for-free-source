package made4mischief.astatine.client.utils.render.renderer;

import made4mischief.astatine.client.utils.render.core.ColorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;

@Environment(EnvType.CLIENT)
public final class RoundedRectRenderer {
   private RoundedRectRenderer(){
   }

   public static void render(DrawContext ct, float x, float y, float width, float height, float radius, int color){
      if (!(width <= 0.0F) && !(height <= 0.0F) && ColorUtil.alpha(color) != 0) {
         int round7 = Math.round(x);
         int round6 = Math.round(y);
         int round5 = Math.round(x + width);
         int round9 = Math.round(y + height);
         int var11 = round5 - round7;
         int var12 = round9 - round6;
         float min = Math.max(0.0F, Math.min(radius, Math.min(width, height) * 0.5F));
         if (!(min < 1.0F) && var11 >= 4 && var12 >= 4) {
            int max = Math.max(1, var11 / 2 - 1);
            int round8 = Math.min(max, Math.max(1, Math.round(min * 0.42F)));
            int round = Math.min(max, Math.max(1, Math.round(min * 0.14F)));
            int round2 = Math.max(1, Math.round(min * 0.22F));
            int round3 = Math.max(1, Math.round(min * 0.24F));
            if (2 * (round2 + round3) >= var12) {
               int round4 = Math.max(1, Math.min(var12 / 3, Math.round(min * 0.35F)));
               ct.fill(round7 + round8, round6, round5 - round8, round6 + round4, color);
               ct.fill(round7, round6 + round4, round5, round9 - round4, color);
               ct.fill(round7 + round8, round9 - round4, round5 - round8, round9, color);
            } else {
               int var19 = round6 + round2;
               int var20 = var19 + round3;
               int var21 = round9 - round2;
               int var22 = var21 - round3;
               ct.fill(round7 + round8, round6, round5 - round8, var19, color);
               ct.fill(round7 + round, var19, round5 - round, var20, color);
               ct.fill(round7, var20, round5, var22, color);
               ct.fill(round7 + round, var22, round5 - round, var21, color);
               ct.fill(round7 + round8, var21, round5 - round8, round9, color);
            }
         } else {
            ct.fill(round7, round6, round5, round9, color);
         }
      }
   }
}

