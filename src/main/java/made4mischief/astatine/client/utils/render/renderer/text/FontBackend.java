package made4mischief.astatine.client.utils.render.renderer.text;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;

@Environment(EnvType.CLIENT)
public interface FontBackend {
   void draw(DrawContext var1, String var2, int var3, int var4, int var5, boolean var6);

   int getWidth(String var1);

   int getHeight();
}
