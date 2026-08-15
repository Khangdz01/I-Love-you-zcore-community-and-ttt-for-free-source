package made4mischief.astatine.client.modules.player;

import java.util.function.Predicate;
import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.ItemTargetSetting;
import made4mischief.astatine.client.setting.ModeSetting;
import made4mischief.astatine.client.utils.inventory.InventoryUtil;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;

@Environment(EnvType.CLIENT)
public final class InventoryDumpModule extends Module {
   private final ModeSetting dumpModeSetting = this.addMode("Dump Mode", "All", new String[]{"All", "Selected"});
   private final ItemTargetSetting itemSetting = this.addSetting(new ItemTargetSetting("Items"));
   private boolean dumping;

   public InventoryDumpModule(){
      super("InventoryDump", Category.PLAYER, "Thả nhanh toàn bộ hoặc vật phẩm đã chọn.", -1, true);
      this.itemSetting.visibleWhen(() -> this.dumpModeSetting.is("Selected"));
   }

   @Override
   protected void onEnable(){
      this.dumping = true;
   }

   @EventTarget
   public void onTick(TickEvent event){
      if (this.dumping) {
         if (event.getClient().player != null && event.getClient().world != null && event.getClient().interactionManager != null) {
            if (this.dumpModeSetting.is("All")) {
               InventoryUtil.dumpPlayerInventory(event.getClient());
            } else {
               InventoryUtil.dumpPlayerInventory(event.getClient(), (Predicate<ItemStack>)(stack -> this.itemSetting.isSelected(stack.getItem())));
            }

            this.dumping = false;
            this.disable();
         } else {
            this.dumping = false;
            this.disable();
         }
      }
   }
}
