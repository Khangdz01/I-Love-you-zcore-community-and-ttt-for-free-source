package made4mischief.astatine.client.utils.world;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public class BlockUtil {
   public static boolean isAir(MinecraftClient client, BlockPos pos){
      BlockState state = client.world.getBlockState(pos);
      return state.isAir();
   }
}
