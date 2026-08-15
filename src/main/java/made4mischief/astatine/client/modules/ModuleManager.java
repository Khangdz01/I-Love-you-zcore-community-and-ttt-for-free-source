package made4mischief.astatine.client.modules;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import made4mischief.astatine.client.screen.ClickGuiScreen;
import made4mischief.astatine.loader.api.event.EventManager;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.KeyEvent;
import made4mischief.astatine.loader.api.event.Listenable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

@Environment(EnvType.CLIENT)
public class ModuleManager implements Listenable {
   public static final ModuleManager INSTANCE = new ModuleManager();
   private final List<Module> modules = new ArrayList<>();
   private final Map<String, Method> hookMethodCache = new HashMap<>();
   private final Set<String> failedHookKeys = new HashSet<>();

   private ModuleManager(){
   }

   public void init(){
      if (!this.modules.isEmpty()) {
         throw new IllegalStateException("ModuleManager da duoc init.");
      } else {
         List<ModuleManager.CatalogEntry> list = this.loadCatalog();
         HashSet<String> var2 = new HashSet<>();
         HashSet<String> var3 = new HashSet<>();

         for (ModuleManager.CatalogEntry catalogEntry : list) {
            this.validateEntry(catalogEntry.className(), var2);
            Module module = this.instantiateModule(catalogEntry.className());
            if (!module.getName().equals(catalogEntry.moduleName()) || !var3.add(module.getName())) {
               throw new SecurityException("Module name khong khop/trung lap: " + module.getName());
            }

            this.register(module);
         }

         EventManager.INSTANCE.register(this);
      }
   }

   private Module instantiateModule(String className){
      try {
         ClassLoader classLoader = ModuleManager.class.getClassLoader();
         Class clazz = Class.forName(className, true, classLoader);
         if (!Module.class.isAssignableFrom(clazz)) {
            throw new SecurityException(className + " khong ke thua Module.");
         } else {
            Constructor constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return (Module)constructor.newInstance();
         }
      } catch (SecurityException e2) {
         throw e2;
      } catch (ReflectiveOperationException e) {
         throw new IllegalStateException("Khong the khoi tao full-payload module " + className, e);
      }
   }

   private List<ModuleManager.CatalogEntry> loadCatalog(){
      String var1 = "/assets/astatine/bundle-modules.json";

      try {
         ArrayList var13;
         try (InputStream inputStream = ModuleManager.class.getResourceAsStream(var1)) {
            if (inputStream == null) {
               throw new IllegalStateException("Khong tim thay " + var1);
            }

            JsonObject jsonObject2 = JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).getAsJsonObject();
            ArrayList var4 = new ArrayList();

            for (JsonElement jsonElement : jsonObject2.getAsJsonArray("modules")) {
               JsonObject jsonObject = jsonElement.getAsJsonObject();
               String asString2 = jsonObject.get("config_name").getAsString();
               String asString = jsonObject.get("class_name").getAsString();
               var4.add(new ModuleManager.CatalogEntry(asString2, asString));
            }

            var13 = var4;
         }

         return var13;
      } catch (Exception e) {
         throw new IllegalStateException("Khong the doc default module entrypoint catalog.", e);
      }
   }

   private void validateEntry(String className, Set<String> seenClassNames){
      if (className == null || !className.startsWith("made4mischief.astatine.client.modules.") || !seenClassNames.add(className)) {
         throw new SecurityException("Module entrypoint khong hop le/trung lap.");
      }
   }

   public void register(Module module){
      this.modules.add(module);
   }

   public List<Module> getModules(){
      return this.modules;
   }

   public List<Module> getModules(Category category){
      ArrayList var2 = new ArrayList();

      for (Module module : this.modules) {
         if (module.getCategory() == category) {
            var2.add(module);
         }
      }

      return var2;
   }

   public Module getModule(String name){
      for (Module module : this.modules) {
         if (module.getName().equalsIgnoreCase(name)) {
            return module;
         }
      }

      return null;
   }

   public void invokeStaticHook(String moduleName, String methodName, Object argument){
      Module module = this.getModule(moduleName);
      if (module != null) {
         String name = module.getClass().getName() + "#" + methodName;
         if (!this.failedHookKeys.contains(name)) {
            try {
               Method method = this.hookMethodCache.get(name);
               if (method == null) {
                  method = this.method_179(module.getClass(), methodName, argument);
                  this.hookMethodCache.put(name, method);
               }

               method.invoke(null, argument);
            } catch (ReflectiveOperationException e) {
               if (this.failedHookKeys.add(name)) {
                  System.err.println("[Astatine/Module] Static hook loi " + name + ": " + e.getMessage());
               }
            }
         }
      }
   }

   private Method method_179(Class<?> moduleClass, String methodName, Object argument) throws NoSuchMethodException{
      for (Method method : moduleClass.getMethods()) {
         if (method.getName().equals(methodName) && Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 1) {
            Class clazz = method.getParameterTypes()[0];
            if (argument == null || clazz.isAssignableFrom(argument.getClass())) {
               return method;
            }
         }
      }

      throw new NoSuchMethodException(moduleClass.getName() + "#" + methodName);
   }

   public <T extends Module> T getModule(Class<T> clazz){
      for (Module module : this.modules) {
         if (clazz.isInstance(module)) {
            return (T)module;
         }
      }

      return null;
   }

   @EventTarget
   public void onKey(KeyEvent event){
      if (event.getKey() != -1) {
         Screen screen = MinecraftClient.getInstance().currentScreen;
         boolean var3 = screen != null;
         boolean var4 = screen instanceof ClickGuiScreen;

         for (Module module : this.modules) {
            if (module.getKeybind() == event.getKey() && (!var3 || module.isEnabled() && !var4)) {
               module.toggle();
            }
         }
      }
   }

   public boolean canReceiveEvents(){
      return true;
   }

   @Environment(EnvType.CLIENT)
   private record CatalogEntry(String moduleName, String className){
   }
}
