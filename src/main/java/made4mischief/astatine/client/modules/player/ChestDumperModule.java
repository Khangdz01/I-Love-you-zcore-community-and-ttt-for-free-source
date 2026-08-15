package made4mischief.astatine.client.modules.player;

import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.utils.inventory.InventoryUtil;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;

@Environment(EnvType.CLIENT)
public final class ChestDumperModule extends Module {
   private ScreenHandler screenHandler;

   public ChestDumperModule(){
      super("ChestDumper", Category.PLAYER, "Thả mọi vật phẩm trong rương đang mở.", -1, true);
   }

   @Override
   protected void onEnable(){
      this.screenHandler = null;
   }

   @Override
   protected void onDisable(){
      this.screenHandler = null;
   }

   @EventTarget
   public void onTick(TickEvent event){
      if (event.getClient().player != null && event.getClient().interactionManager != null) {
         ScreenHandler screenHandler = event.getClient().player.currentScreenHandler;
         if (!(screenHandler instanceof GenericContainerScreenHandler)) {
            this.screenHandler = null;
         } else if (screenHandler != this.screenHandler) {
            this.screenHandler = screenHandler;
            InventoryUtil.dumpOpenContainerToGround(event.getClient(), null, true);
         }
      } else {
         this.screenHandler = null;
      }
   }
}
