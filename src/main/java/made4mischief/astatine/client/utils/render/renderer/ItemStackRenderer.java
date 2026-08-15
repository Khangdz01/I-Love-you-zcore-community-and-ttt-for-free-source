package made4mischief.astatine.client.utils.render.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

@Environment(EnvType.CLIENT)
public final class ItemStackRenderer {
   private ItemStackRenderer(){
   }

   public static void render(DrawContext context, LivingEntity entity, ItemStack stack, int x, int y, int seed){
      if (stack != null && !stack.isEmpty()) {
         context.drawItem(entity, stack, x, y, seed);
         context.drawStackOverlay(MinecraftClient.getInstance().textRenderer, stack, x, y);
      }
   }
}
