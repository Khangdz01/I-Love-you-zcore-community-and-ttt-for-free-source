package made4mischief.astatine.client.modules.smp;

import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.BlockTargetSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Hand;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.client.network.ClientPlayerEntity;

@Environment(EnvType.CLIENT)
public final class EasyPlaceModule extends Module {
   private static EasyPlaceModule instance;
   private final BlockTargetSetting blockSetting = this.addSetting(
      new BlockTargetSetting(
         "Blocks",
         Blocks.CHEST,
         Blocks.TRAPPED_CHEST,
         Blocks.ENDER_CHEST,
         Blocks.BARREL,
         Blocks.HOPPER,
         Blocks.DISPENSER,
         Blocks.DROPPER,
         Blocks.SHULKER_BOX,
         Blocks.WHITE_SHULKER_BOX,
         Blocks.ORANGE_SHULKER_BOX,
         Blocks.MAGENTA_SHULKER_BOX,
         Blocks.LIGHT_BLUE_SHULKER_BOX,
         Blocks.YELLOW_SHULKER_BOX,
         Blocks.LIME_SHULKER_BOX,
         Blocks.PINK_SHULKER_BOX,
         Blocks.GRAY_SHULKER_BOX,
         Blocks.LIGHT_GRAY_SHULKER_BOX,
         Blocks.CYAN_SHULKER_BOX,
         Blocks.PURPLE_SHULKER_BOX,
         Blocks.BLUE_SHULKER_BOX,
         Blocks.BROWN_SHULKER_BOX,
         Blocks.GREEN_SHULKER_BOX,
         Blocks.RED_SHULKER_BOX,
         Blocks.BLACK_SHULKER_BOX,
         Blocks.FURNACE,
         Blocks.BLAST_FURNACE,
         Blocks.SMOKER,
         Blocks.CRAFTING_TABLE,
         Blocks.CRAFTER,
         Blocks.BREWING_STAND,
         Blocks.BEACON,
         Blocks.ENCHANTING_TABLE,
         Blocks.ANVIL,
         Blocks.CHIPPED_ANVIL,
         Blocks.DAMAGED_ANVIL,
         Blocks.GRINDSTONE,
         Blocks.SMITHING_TABLE,
         Blocks.STONECUTTER,
         Blocks.LOOM,
         Blocks.CARTOGRAPHY_TABLE
      )
   );

   public EasyPlaceModule(){
      super("EasyPlace", Category.SMP, "Đặt khối lên kho chứa mà không cần giữ Shift.", -1);
      instance = this;
   }

   public static boolean shouldAssist(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult){
      EasyPlaceModule easyPlaceModule = instance;
      if (easyPlaceModule != null && easyPlaceModule.isEnabled() && player != null && player.input != null && !player.input.playerInput.sneak()) {
         ItemStack stack = player.getStackInHand(hand);
         boolean item2 = stack.getItem() instanceof BlockItem;
         boolean item = hand == Hand.MAIN_HAND && player.getOffHandStack().getItem() instanceof BlockItem;
         if (!item2 && !item) {
            return false;
         } else {
            BlockState state = player.getEntityWorld().getBlockState(hitResult.getBlockPos());
            return easyPlaceModule.blockSetting.isSelected(state.getBlock());
         }
      } else {
         return false;
      }
   }
}
