package made4mischief.astatine.client.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.util.InputUtil.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class ClickGuiManager {
   private static final Logger LOGGER = LoggerFactory.getLogger("astatine/clickgui");
   private static KeyBinding keyBinding;
   private static boolean wasKeyPressed;

   private ClickGuiManager(){
   }

   public static void init(){
      keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.astatine.clickgui", Type.KEYSYM, 344, Category.MISC));
      LOGGER.info("ClickGUI keybind registered on RIGHT SHIFT (rebindable in Options > Controls)");
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> {
         boolean window = client.getWindow() != null && InputUtil.isKeyPressed(client.getWindow(), 344);
         boolean var2 = window && !wasKeyPressed;
         wasKeyPressed = window;
         boolean var3 = false;

         while (keyBinding.wasPressed()) {
            var3 = true;
         }

         if ((var3 || var2) && client.currentScreen == null) {
            LOGGER.info("Opening ClickGUI (bindingPress={}, physicalPress={})", var3, var2);
            client.setScreen(new ClickGuiScreen());
         }
      });
   }
}
