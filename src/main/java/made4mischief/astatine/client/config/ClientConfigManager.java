package made4mischief.astatine.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import made4mischief.astatine.client.gui.component.theme.ThemeManager;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.modules.ModuleManager;
import made4mischief.astatine.client.modules.render.HUDModule;
import made4mischief.astatine.client.setting.ActionSetting;
import made4mischief.astatine.client.setting.BlockTargetSetting;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.ColorSetting;
import made4mischief.astatine.client.setting.EntityTargetSetting;
import made4mischief.astatine.client.setting.ItemTargetSetting;
import made4mischief.astatine.client.setting.KeybindSetting;
import made4mischief.astatine.client.setting.ModeSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import made4mischief.astatine.client.setting.Setting;
import made4mischief.astatine.client.setting.StringSetting;
import made4mischief.astatine.loader.api.LoaderRuntime;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.block.Block;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class ClientConfigManager {
   private static final Logger field_1 = LoggerFactory.getLogger("Astatine/Config");
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final int CONFIG_VERSION = 2;
   private static final int SAVE_INTERVAL_TICKS = 20;
   private static final Map<String, Boolean> pendingEnabledStates = new HashMap<>();
   private static boolean loaded;
   private static boolean worldInitialized = true;
   private static int tickCounter;
   private static String lastSerializedConfig = "";
   private static String savedUsername = "";
   private static JsonObject modulesJson = new JsonObject();
   private static String initialConfigJson;
   private static LoaderRuntime runtime;

   private ClientConfigManager(){
   }

   public static synchronized void initialize(LoaderRuntime runtimeInstance){
      if (loaded || runtime != null) {
         throw new IllegalStateException("LoaderRuntime cannot be replaced after payload startup.");
      } else if (runtimeInstance == null) {
         throw new SecurityException("Authenticated LoaderRuntime is required.");
      } else {
         runtimeInstance.requireActive();
         String claimInitialConfiguration = runtimeInstance.claimInitialConfiguration();
         if (claimInitialConfiguration != null && !claimInitialConfiguration.isBlank()) {
            runtime = runtimeInstance;
            initialConfigJson = claimInitialConfiguration;
         } else {
            throw new SecurityException("LoaderRuntime returned empty config.");
         }
      }
   }

   private static LoaderRuntime getRuntime(){
      LoaderRuntime loaderRuntime = runtime;
      if (loaderRuntime == null) {
         throw new SecurityException("Authenticated LoaderRuntime is required.");
      } else {
         loaderRuntime.requireActive();
         return loaderRuntime;
      }
   }

   public static void load(){
      getRuntime();
      if (!loaded) {
         loaded = true;

         try {
            if (initialConfigJson == null) {
               throw new SecurityException("Authenticated loader configuration is required.");
            } else {
               String var0 = initialConfigJson;
               initialConfigJson = null;
               JsonElement jsonElement = JsonParser.parseString(var0);
               if (!jsonElement.isJsonObject()) {
                  throw new IllegalStateException("Config root must be a JSON object");
               } else {
                  JsonObject jsonObject = jsonElement.getAsJsonObject();
                  loadGlobalConfig(jsonObject);
                  loadModulesConfig(jsonObject);
                  loadHudConfig(jsonObject);
                  lastSerializedConfig = buildRootObject();
                  field_1.info("Loaded authenticated Astatine config in RAM");
               }
            }
         } catch (Exception e) {
            throw new SecurityException("Khong the apply authenticated server config.", e);
         }
      }
   }

   public static void onClientTick(MinecraftClient client){
      getRuntime();
      if (loaded) {
         if (!worldInitialized && client.player != null && client.world != null) {
            markInitialized();
         }

         if (++tickCounter >= 20) {
            tickCounter = 0;
            saveConfig();
         }
      }
   }

   public static void save(){
      if (loaded) {
         String var0 = buildRootObject();
         if (loadConfigFromString(var0)) {
            lastSerializedConfig = var0;
         }
      }
   }

   public static void saveAndFlush(){
      save();
      boolean flushConfiguration = getRuntime().flushConfiguration(10000L);
      if (!flushConfiguration) {
         field_1.warn("Final in-memory config sync did not finish");
      }
   }

   public static void applyConfigurationToHotLoadedModule(Module module){
      if (loaded && module != null) {
         JsonObject jsonObject2 = getJsonObject(modulesJson, module.getName());
         if (jsonObject2 == null) {
            throw new SecurityException("Server config thieu hot-loaded module " + module.getName());
         } else {
            if (getJsonElement(jsonObject2, "keybind") != null) {
               try {
                  module.setKeybind(jsonObject2.get("keybind").getAsInt());
               } catch (RuntimeException e) {
                  logInvalidConfig(module, "keybind", e);
               }
            }

            JsonObject jsonObject = getJsonObject(jsonObject2, "settings");
            if (jsonObject != null) {
               for (Setting setting : module.getSettings()) {
                  JsonElement jsonElement2 = jsonObject.get(setting.getName());
                  if ((jsonElement2 == null || jsonElement2.isJsonNull()) && "Velocity".equals(module.getName())) {
                     jsonElement2 = readSettingByName(jsonObject, setting.getName());
                  }

                  if (jsonElement2 != null && !jsonElement2.isJsonNull()) {
                     try {
                        applySettingValue(setting, jsonElement2);
                     } catch (RuntimeException e2) {
                        logInvalidConfig(module, setting.getName(), e2);
                     }
                  }
               }
            }

            JsonElement jsonElement = getJsonElement(jsonObject2, "enabled");
            if (jsonElement != null && jsonElement.getAsBoolean()) {
               module.setEnabled(true);
               lastSerializedConfig = buildRootObject();
            } else {
               throw new SecurityException("Server chua authorize enabled=true cho " + module.getName());
            }
         }
      } else {
         throw new IllegalStateException("Config chua san sang cho hot-loaded module.");
      }
   }

   private static void loadModulesConfig(JsonObject root){
      JsonObject jsonObject3 = getJsonObject(root, "modules");
      if (jsonObject3 == null) {
         modulesJson = new JsonObject();
      } else {
         modulesJson = jsonObject3.deepCopy();
         pendingEnabledStates.clear();

         for (Module module : ModuleManager.INSTANCE.getModules()) {
            JsonObject jsonObject2 = getJsonObject(jsonObject3, module.getName());
            if (jsonObject2 != null) {
               if (getJsonElement(jsonObject2, "enabled") != null) {
                  try {
                     pendingEnabledStates.put(module.getName(), jsonObject2.get("enabled").getAsBoolean());
                  } catch (RuntimeException e2) {
                     logInvalidConfig(module, "enabled", e2);
                  }
               }

               if (getJsonElement(jsonObject2, "keybind") != null) {
                  try {
                     module.setKeybind(jsonObject2.get("keybind").getAsInt());
                  } catch (RuntimeException e3) {
                     logInvalidConfig(module, "keybind", e3);
                  }
               }

               JsonObject jsonObject = getJsonObject(jsonObject2, "settings");
               if (jsonObject != null) {
                  for (Setting setting : module.getSettings()) {
                     JsonElement jsonElement = jsonObject.get(setting.getName());
                     if ((jsonElement == null || jsonElement.isJsonNull()) && "Velocity".equals(module.getName())) {
                        jsonElement = readSettingByName(jsonObject, setting.getName());
                     }

                     if (jsonElement != null && !jsonElement.isJsonNull()) {
                        try {
                           applySettingValue(setting, jsonElement);
                        } catch (RuntimeException e) {
                           logInvalidConfig(module, setting.getName(), e);
                        }
                     }
                  }
               }
            }
         }

         worldInitialized = pendingEnabledStates.isEmpty();
      }
   }

   private static void loadHudConfig(JsonObject root){
      HUDModule hUDModule = ModuleManager.INSTANCE.getModule(HUDModule.class);
      int accent = hUDModule != null ? hUDModule.getCustomAccent() : ThemeManager.custom().accent();
      int accentSecondary = hUDModule != null ? hUDModule.getCustomSecondary() : ThemeManager.custom().accentSecondary();
      JsonObject jsonObject = getJsonObject(root, "theme");
      if (jsonObject != null) {
         JsonElement jsonElement3 = getJsonElement(jsonObject, "customAccent");
         JsonElement jsonElement2 = getJsonElement(jsonObject, "customSecondary");

         try {
            if (jsonElement3 != null) {
               accent = parseIntSetting(jsonElement3);
            }

            if (jsonElement2 != null) {
               accentSecondary = parseIntSetting(jsonElement2);
            }
         } catch (RuntimeException e2) {
            field_1.warn("Ignored invalid custom theme colors", e2);
         }

         JsonElement jsonElement = getJsonElement(jsonObject, "active");
         if (jsonElement != null) {
            try {
               String asString = jsonElement.getAsString();
               if (!ThemeManager.setByName(asString)) {
                  field_1.warn("Unknown saved theme '{}'; using Aqua", asString);
                  ThemeManager.set(ThemeManager.DEFAULT);
               }
            } catch (RuntimeException e) {
               field_1.warn("Ignored invalid active theme", e);
               ThemeManager.set(ThemeManager.DEFAULT);
            }
         }
      }

      if (hUDModule != null) {
         hUDModule.setCustomThemeColors(accent, accentSecondary);
      }

      ThemeManager.setCustomColors(accent, accentSecondary);
   }

   private static void markInitialized(){
      worldInitialized = true;

      for (Module module : ModuleManager.INSTANCE.getModules()) {
         Boolean booleanValue = pendingEnabledStates.get(module.getName());
         if (booleanValue != null && booleanValue != module.isEnabled()) {
            module.setEnabled(booleanValue);
         }
      }

      pendingEnabledStates.clear();
      lastSerializedConfig = buildRootObject();
   }

   private static void applySettingValue(Setting setting, JsonElement value){
      if (setting instanceof BooleanSetting var2) {
         var2.setValue(value.getAsBoolean());
      } else if (setting instanceof NumberSetting var3) {
         var3.setValue(value.getAsDouble());
      } else if (setting instanceof ModeSetting var4) {
         var4.setValue(value.getAsString());
      } else if (setting instanceof ColorSetting var5) {
         var5.setValue(parseIntSetting(value));
      } else if (setting instanceof StringSetting var6) {
         var6.setValue(value.getAsString());
      } else if (setting instanceof KeybindSetting var7) {
         var7.setValue(value.getAsInt());
      } else if (setting instanceof EntityTargetSetting var8) {
         var8.setSelectedTypes(parseEntityTargets(value));
      } else if (setting instanceof ItemTargetSetting var9) {
         var9.setSelectedItems(parseItemTargets(value));
      } else if (setting instanceof BlockTargetSetting var10) {
         var10.setSelectedBlocks(parseBlockTargets(value));
      }
   }

   private static String buildRootObject(){
      JsonObject jsonObject5 = new JsonObject();
      if (!savedUsername.isBlank()) {
         jsonObject5.addProperty("username", savedUsername);
      }

      jsonObject5.addProperty("version", 2);
      HUDModule hUDModule = ModuleManager.INSTANCE.getModule(HUDModule.class);
      int accent = hUDModule != null ? hUDModule.getCustomAccent() : ThemeManager.custom().accent();
      int accentSecondary = hUDModule != null ? hUDModule.getCustomSecondary() : ThemeManager.custom().accentSecondary();
      ThemeManager.setCustomColors(accent, accentSecondary);
      JsonObject jsonObject3 = new JsonObject();
      jsonObject3.addProperty("active", ThemeManager.active().name());
      jsonObject3.addProperty("customAccent", colorToHe(accent));
      jsonObject3.addProperty("customSecondary", colorToHe(accentSecondary));
      jsonObject5.add("theme", jsonObject3);
      JsonObject jsonObject2 = modulesJson.deepCopy();

      for (Module module : ModuleManager.INSTANCE.getModules()) {
         JsonObject jsonObject = new JsonObject();
         boolean enabled = worldInitialized ? module.isEnabled() : pendingEnabledStates.getOrDefault(module.getName(), module.isEnabled());
         jsonObject.addProperty("enabled", enabled);
         jsonObject.addProperty("keybind", module.getKeybind());
         JsonObject jsonObject4 = new JsonObject();

         for (Setting setting : module.getSettings()) {
            if (!(setting instanceof ActionSetting)) {
               JsonElement jsonElement = serializeSetting(setting);
               if (jsonElement != null) {
                  jsonObject4.add(setting.getName(), jsonElement);
               }
            }
         }

         jsonObject.add("settings", jsonObject4);
         jsonObject2.add(module.getName(), jsonObject);
      }

      modulesJson = jsonObject2.deepCopy();
      jsonObject5.add("modules", jsonObject2);
      return GSON.toJson(jsonObject5);
   }

   private static JsonElement serializeSetting(Setting setting){
      if (setting instanceof BooleanSetting var10) {
         return GSON.toJsonTree(var10.getValue());
      } else if (setting instanceof NumberSetting var9) {
         return GSON.toJsonTree(var9.getValue());
      } else if (setting instanceof ModeSetting var8) {
         return GSON.toJsonTree(var8.getValue());
      } else if (setting instanceof ColorSetting var7) {
         return GSON.toJsonTree(colorToHe(var7.getValue()));
      } else if (setting instanceof StringSetting var6) {
         return GSON.toJsonTree(var6.getValue());
      } else if (setting instanceof KeybindSetting var5) {
         return GSON.toJsonTree(var5.getValue());
      } else if (setting instanceof EntityTargetSetting var4) {
         JsonArray jsonArray = new JsonArray();
         var4.getSelectedTypes().stream().map(Registries.ENTITY_TYPE::getId).map(Identifier::toString).sorted().forEach(jsonArray::add);
         return jsonArray;
      } else if (setting instanceof ItemTargetSetting var3) {
         JsonArray jsonArray2 = new JsonArray();
         var3.getSelectedItems().stream().map(Registries.ITEM::getId).map(Identifier::toString).sorted().forEach(jsonArray2::add);
         return jsonArray2;
      } else if (setting instanceof BlockTargetSetting var1) {
         JsonArray jsonArray3 = new JsonArray();
         var1.getSelectedBlocks().stream().map(Registries.BLOCK::getId).map(Identifier::toString).sorted().forEach(jsonArray3::add);
         return jsonArray3;
      } else {
         return null;
      }
   }

   private static List<EntityType<?>> parseEntityTargets(JsonElement value){
      ArrayList var1 = new ArrayList();

      for (JsonElement jsonElement : value.getAsJsonArray()) {
         Identifier id = Identifier.tryParse(jsonElement.getAsString());
         if (id != null && Registries.ENTITY_TYPE.containsId(id)) {
            var1.add((EntityType)Registries.ENTITY_TYPE.get(id));
         }
      }

      return var1;
   }

   private static List<Item> parseItemTargets(JsonElement value){
      ArrayList var1 = new ArrayList();

      for (JsonElement jsonElement : value.getAsJsonArray()) {
         Identifier id = Identifier.tryParse(jsonElement.getAsString());
         if (id != null && Registries.ITEM.containsId(id)) {
            var1.add((Item)Registries.ITEM.get(id));
         }
      }

      return var1;
   }

   private static List<Block> parseBlockTargets(JsonElement value){
      ArrayList var1 = new ArrayList();

      for (JsonElement jsonElement : value.getAsJsonArray()) {
         Identifier id = Identifier.tryParse(jsonElement.getAsString());
         if (id != null && Registries.BLOCK.containsId(id)) {
            var1.add((Block)Registries.BLOCK.get(id));
         }
      }

      return var1;
   }

   private static int parseIntSetting(JsonElement value){
      if (value.getAsJsonPrimitive().isNumber()) {
         return value.getAsInt();
      } else {
         String trim = value.getAsString().trim();
         if (trim.startsWith("#")) {
            trim = trim.substring(1);
         }

         if (trim.length() == 6) {
            return 0xFF000000 | Integer.parseUnsignedInt(trim, 16);
         } else if (trim.length() == 8) {
            return (int)Long.parseLong(trim, 16);
         } else {
            throw new IllegalArgumentException("Invalid color value");
         }
      }
   }

   private static String colorToHe(int color){
      return String.format("#%06X", color & 16777215);
   }

   private static void saveConfig(){
      String var0 = buildRootObject();
      if (!var0.equals(lastSerializedConfig) && loadConfigFromString(var0)) {
         lastSerializedConfig = var0;
      }
   }

   private static boolean loadConfigFromString(String json){
      try {
         return getRuntime().configurationChanged(json);
      } catch (RuntimeException e) {
         field_1.error("Unable to queue in-memory config update", e);
         return false;
      }
   }

   private static JsonObject getJsonObject(JsonObject parent, String name){
      JsonElement jsonElement = parent.get(name);
      return jsonElement != null && jsonElement.isJsonObject() ? jsonElement.getAsJsonObject() : null;
   }

   private static void loadGlobalConfig(JsonObject root){
      JsonElement jsonElement = getJsonElement(root, "username");
      if (jsonElement != null && jsonElement.getAsJsonPrimitive().isString()) {
         String trim = jsonElement.getAsString().trim();
         savedUsername = trim.length() <= 150 ? trim : "";
      } else {
         savedUsername = "";
      }
   }

   private static JsonElement getJsonElement(JsonObject parent, String name){
      JsonElement jsonElement = parent.get(name);
      return jsonElement != null && jsonElement.isJsonPrimitive() ? jsonElement : null;
   }

   private static JsonElement readSettingByName(JsonObject settings, String settingName){
      return switch (settingName) {
         case "Player Push" -> settings.get("Knockback");
         case "Explosion" -> settings.get("Explosions");
         default -> null;
      };
   }

   private static void logInvalidConfig(Module module, String setting, RuntimeException exception){
      field_1.warn("Ignored invalid config value {}.{}", new Object[]{module.getName(), setting, exception});
   }
}

