package made4mischief.astatine.client.modules.smp;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.registry.tag.ItemTags;

@Environment(EnvType.CLIENT)
public final class AutoShulkerModule extends AbstractFixedSlotShopModule {
   public AutoShulkerModule(){
      super("AutoShulker", "Tự mua hộp Shulker liên tục trong cửa hàng.", stack -> stack.isIn(ItemTags.SHULKER_BOXES), 11, 17, 23);
   }
}
