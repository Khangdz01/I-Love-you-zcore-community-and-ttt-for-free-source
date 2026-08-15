package made4mischief.astatine.client.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.util.InputUtil.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class RenderTestManager {
   private static final Logger LOGGER = LoggerFactory.getLogger("astatine/render-test");
   private static KeyBinding keyBinding;

   private RenderTestManager(){
   }

   public static void init(){
      keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.astatine.render_test", Type.KEYSYM, 297, Category.MISC));
      LOGGER.info("Render test keybind registered on F8 (rebindable in Options > Controls)");
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> {
         while (keyBinding.wasPressed()) {
            LOGGER.info("Render test key pressed; currentScreen={}", client.currentScreen == null ? "null" : client.currentScreen.getClass().getSimpleName());
            if (client.currentScreen == null) {
               client.setScreen(new RenderTestScreen());
            }
         }
      });
   }
}
