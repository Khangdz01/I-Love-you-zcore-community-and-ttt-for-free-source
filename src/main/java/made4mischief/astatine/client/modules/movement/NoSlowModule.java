package made4mischief.astatine.client.modules.movement;

import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;

@Environment(EnvType.CLIENT)
public final class NoSlowModule extends Module {
   private static NoSlowModule instance;

   public NoSlowModule(){
      super("NoSlow", Category.MOVEMENT, "Không bị chậm khi dùng vật phẩm.", -1);
      instance = this;
   }

   public static boolean shouldIgnoreItemSlowdown(ClientPlayerEntity player){
      return instance != null && instance.isEnabled() && player != null && player.isUsingItem();
   }
}
