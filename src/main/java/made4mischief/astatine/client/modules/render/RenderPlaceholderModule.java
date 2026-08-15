package made4mischief.astatine.client.modules.render;

import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class RenderPlaceholderModule extends Module {
   public RenderPlaceholderModule(String name){
      super(name, Category.RENDER, "Module hình ảnh dự phòng.", -1, true);
   }
}
