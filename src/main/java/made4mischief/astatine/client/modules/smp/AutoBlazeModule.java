package made4mischief.astatine.client.modules.smp;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Items;

@Environment(EnvType.CLIENT)
public final class AutoBlazeModule extends AbstractFixedSlotShopModule {
   public AutoBlazeModule(){
      super("AutoBlaze", "Tự mua que quỷ lửa liên tục trong cửa hàng.", stack -> stack.isOf(Items.BLAZE_ROD), 12, 9, 17, 23);
   }
}
