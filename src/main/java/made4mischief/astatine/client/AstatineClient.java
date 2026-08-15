package made4mischief.astatine.client;

import java.io.InputStream;
import made4mischief.astatine.client.config.ClientConfigManager;
import made4mischief.astatine.client.hud.HudRenderer;
import made4mischief.astatine.client.hud.NotificationRenderer;
import made4mischief.astatine.client.hud.PixelPetRenderer;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.modules.ModuleManager;
import made4mischief.astatine.client.screen.ClickGuiManager;
import made4mischief.astatine.client.utils.render.renderer.text.AwtFontBackend;
import made4mischief.astatine.client.utils.render.renderer.text.MinecraftFontBackend;
import made4mischief.astatine.client.utils.render.renderer.text.TextRenderer;
import made4mischief.astatine.loader.api.AuthenticatedLoaderRuntime;
import made4mischief.astatine.loader.api.LoaderRuntime;
import made4mischief.astatine.loader.api.event.EventManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.ClientStopping;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.StartTick;

@Environment(EnvType.CLIENT)
public class AstatineClient {
   public void start(LoaderRuntime loaderRuntime){
      if (loaderRuntime == null) {
         throw new SecurityException("Authenticated LoaderRuntime is required.");
      } else if (loaderRuntime.getClass() == AuthenticatedLoaderRuntime.class
         && loaderRuntime.getClass().getClassLoader() == LoaderRuntime.class.getClassLoader()) {
         loaderRuntime.requireActive();
         EventManager.INSTANCE.installRuntime(loaderRuntime);
         ClientConfigManager.initialize(loaderRuntime);
         this.initialize();
      } else {
         throw new SecurityException("LoaderRuntime implementation or classloader is invalid.");
      }
   }

   private void initialize(){
      try {
         InputStream inputStream = AstatineClient.class.getResourceAsStream("/assets/astatine/font/inter.ttf");
         if (inputStream != null) {
            TextRenderer.setBackend(new AwtFontBackend(inputStream, 16.0F));
         } else {
            TextRenderer.setBackend(new MinecraftFontBackend());
         }
      } catch (Exception e) {
         TextRenderer.setBackend(new MinecraftFontBackend());
      }

      ModuleManager.INSTANCE.init();
      initFeature("ESP", "made4mischief.astatine.client.render.esp.PlayerEspFeatureRenderer");
      initFeature("Skeleton", "made4mischief.astatine.client.render.skeleton.SkeletonFeatureRenderer");
      initFeature("KillAura", "made4mischief.astatine.client.hud.TargetHudRenderer");
      HudRenderer.init();
      PixelPetRenderer.init();
      NotificationRenderer.init();
      ClientConfigManager.load();
      ClickGuiManager.init();
      ClientTickEvents.START_CLIENT_TICK.register((StartTick)client -> ModuleManager.INSTANCE.invokeStaticHook("CrystalAura", "onClientTickStart", client));
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> {
         HudRenderer.onClientTick();
         ClientConfigManager.onClientTick(client);
      });
      ClientLifecycleEvents.CLIENT_STOPPING.register((ClientStopping)client -> ClientConfigManager.saveAndFlush());
   }

   private static void initFeature(String moduleName, String initializerClassName){
      Module module = ModuleManager.INSTANCE.getModule(moduleName);
      if (module != null) {
         try {
            Class clazz = Class.forName(initializerClassName, true, AstatineClient.class.getClassLoader());
            clazz.getMethod("init").invoke(null);
         } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Khong the init feature " + moduleName, e);
         }
      }
   }
}
