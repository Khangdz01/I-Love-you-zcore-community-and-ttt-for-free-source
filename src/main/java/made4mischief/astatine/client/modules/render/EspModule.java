package made4mischief.astatine.client.modules.render;

import made4mischief.astatine.Astatine;
import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.ColorSetting;
import made4mischief.astatine.client.setting.EntityTargetSetting;
import made4mischief.astatine.client.setting.ModeSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import made4mischief.astatine.client.utils.render.RenderUtil;
import made4mischief.astatine.client.utils.render.ScreenTracerRenderer;
import made4mischief.astatine.client.utils.render.core.ColorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

@Environment(EnvType.CLIENT)
public class EspModule extends Module {
   private static EspModule instance;
   private final ModeSetting modeSetting = this.addMode("Mode", "Line", new String[]{"Line", "Full"});
   private final EntityTargetSetting entitySetting = this.addSetting(new EntityTargetSetting("Entity Selector", EntityType.PLAYER));
   private final NumberSetting rangeSetting = this.addNumber("Range", 128.0, 8.0, 256.0, 8.0);
   private final ColorSetting eSPColorSetting = this.addColor("ESP Color", -13244417);
   private final NumberSetting fillAlphaSetting = this.addNumber("Fill Alpha", 55.0, 0.0, 255.0, 5.0);
   private final NumberSetting outlineAlphaSetting = this.addNumber("Outline Alpha", 230.0, 0.0, 255.0, 5.0);
   private final NumberSetting lineWidthSetting = this.addNumber("Line Width", 2.0, 1.0, 5.0, 0.5);
   private final BooleanSetting tracersSetting = this.addBoolean("Tracers", true);
   private final NumberSetting tracerWidthSetting = this.addNumber("Tracer Width", 0.75, 0.5, 2.0, 0.25);
   private final ScreenTracerRenderer screenTracerRenderer = new ScreenTracerRenderer();

   public EspModule(){
      super("ESP", Category.RENDER, "LÃ m ná»•i báº­t ngÆ°á»i chÆ¡i vÃ  sinh váº­t qua tÆ°á»ng.", -1);
      this.fillAlphaSetting.visibleWhen(() -> this.modeSetting.is("Full"));
      this.tracerWidthSetting.visibleWhen(this.tracersSetting::getValue);
      instance = this;
      WorldRenderEvents.END_MAIN.register(EspModule::renderWorld);
      HudElementRegistry.attachElementBefore(VanillaHudElements.CROSSHAIR, Astatine.id("entity_esp_tracers"), EspModule::renderScreen);
   }

   public ModeSetting getMode(){
      return this.modeSetting;
   }

   public ColorSetting getLineColor(){
      return this.eSPColorSetting;
   }

   public boolean isSelected(EntityType<?> entityType){
      return this.entitySetting.isSelected(entityType);
   }

   public boolean shouldRenderEntity(MinecraftClient client, Entity entity){
      return client != null
         && client.player != null
         && entity != null
         && entity != client.player
         && !entity.isRemoved()
         && !entity.isSpectator()
         && this.entitySetting.isSelected(entity.getType())
         && client.player.squaredDistanceTo(entity) <= this.rangeSetting.getValue() * this.rangeSetting.getValue();
   }

   private static void renderWorld(WorldRenderContext context){
      EspModule espModule = instance;
      MinecraftClient client = MinecraftClient.getInstance();
      if (espModule != null && espModule.isEnabled() && client.player != null && client.world != null) {
         boolean is = espModule.modeSetting.is("Full");
         int value = espModule.eSPColorSetting.getValue();

         for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof PlayerEntity) && espModule.shouldRenderEntity(client, entity)) {
               Box Box = entity.getBoundingBox().expand(0.025);
               RenderUtil.drawWorldBo(
                  context,
                  Box.minX,
                  Box.minY,
                  Box.minZ,
                  Box.maxX,
                  Box.maxY,
                  Box.maxZ,
                  ColorUtil.withAlpha(value, espModule.fillAlphaSetting.getValueInt()),
                  ColorUtil.withAlpha(value, espModule.outlineAlphaSetting.getValueInt()),
                  is,
                  true,
                  true,
                  espModule.lineWidthSetting.getValueFloat()
               );
            }
         }
      }
   }

   private static void renderScreen(DrawContext context, RenderTickCounter tickCounter){
      EspModule espModule = instance;
      MinecraftClient client = MinecraftClient.getInstance();
      if (espModule != null
         && espModule.isEnabled()
         && espModule.tracersSetting.getValue()
         && client.player != null
         && client.world != null
         && espModule.screenTracerRenderer.begin(context, client)) {
         for (Entity entity : client.world.getEntities()) {
            if (espModule.shouldRenderEntity(client, entity)) {
               espModule.screenTracerRenderer.draw(context, client, entity.getBoundingBox().getCenter(), espModule.tracerWidthSetting.getValueFloat(), espModule.eSPColorSetting.getValue());
            }
         }
      }
   }
}

