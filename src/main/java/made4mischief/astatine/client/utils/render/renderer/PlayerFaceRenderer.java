package made4mischief.astatine.client.utils.render.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.entity.player.SkinTextures;

@Environment(EnvType.CLIENT)
public final class PlayerFaceRenderer {
   private PlayerFaceRenderer(){
   }

   public static void render(DrawContext context, SkinTextures skin, int x, int y, int size, int color){
      if (skin != null && size > 0) {
         PlayerSkinDrawer.draw(context, skin, x, y, size, color);
      }
   }
}
