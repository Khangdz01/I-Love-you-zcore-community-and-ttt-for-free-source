package made4mischief.astatine.client.utils.render.renderer.text;

import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;

@Environment(EnvType.CLIENT)
public final class MinecraftFontBackend implements FontBackend {
   private final Map<String, OrderedText> textCache = new HashMap<>();
   private TextRenderer textRenderer;

   private TextRenderer font(){
      if (this.textRenderer == null) {
         this.textRenderer = MinecraftClient.getInstance().textRenderer;
      }
      return this.textRenderer;
   }

   @Override
   public void draw(DrawContext ct, String text, int x, int y, int color, boolean shadow){
      ct.drawText(this.font(), this.getOrderedText(text), x, y, color, shadow);
   }

   @Override
   public int getWidth(String text){
      return this.font().getWidth(this.getOrderedText(text));
   }

   @Override
   public int getHeight(){
      return 12;
   }

   private OrderedText getOrderedText(String text){
      return this.textCache.computeIfAbsent(text, value -> Text.literal(value).asOrderedText());
   }
}
