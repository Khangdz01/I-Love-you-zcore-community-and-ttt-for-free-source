package made4mischief.astatine.client.modules.player;

import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.ModeSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class NoFallModule extends Module {
   private final ModeSetting modeSetting = this.addMode("Mode", "Packet", new String[]{"Packet", "NoGround", "Vanilla"});

   public NoFallModule(){
      super("NoFall", Category.PLAYER, "Ngăn sát thương khi rơi.", -1);
   }

   public ModeSetting getMode(){
      return this.modeSetting;
   }
}
