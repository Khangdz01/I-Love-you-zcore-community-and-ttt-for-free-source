package made4mischief.astatine.client.modules.render;

import java.util.ArrayList;
import java.util.List;
import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.ColorSetting;
import made4mischief.astatine.client.setting.ModeSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import made4mischief.astatine.client.utils.render.RenderUtil;
import made4mischief.astatine.client.utils.render.core.ColorUtil;
import made4mischief.astatine.client.utils.world.HoleScanner;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;

@Environment(EnvType.CLIENT)
public final class HoleESPModule extends Module {
   private static final double BOX_INSET = 0.035;
   private static final double BOX_BOTTOM_INSET = 0.012;
   private static final double FLAT_HEIGHT = 0.025;
   private static HoleESPModule instance;
   private final ModeSetting renderModeSetting = this.addMode("Render Mode", "Both", new String[]{"Both", "Fill", "Outline", "Flat"});
   private final NumberSetting rangeSetting = this.addNumber("Range", 12.0, 3.0, 24.0, 1.0);
   private final NumberSetting verticalRangeSetting = this.addNumber("Vertical Range", 5.0, 1.0, 10.0, 1.0);
   private final NumberSetting updateDelaySetting = this.addNumber("Update Delay", 4.0, 1.0, 20.0, 1.0);
   private final NumberSetting height = this.addNumber("Height", 1.0, 0.1, 2.0, 0.05);
   private final NumberSetting fillAlphaSetting = this.addNumber("Fill Alpha", 50.0, 0.0, 255.0, 5.0);
   private final NumberSetting outlineAlphaSetting = this.addNumber("Outline Alpha", 220.0, 0.0, 255.0, 5.0);
   private final NumberSetting lineWidthSetting = this.addNumber("Line Width", 2.0, 1.0, 5.0, 0.5);
   private final BooleanSetting throughWallsSetting = this.addBoolean("Through Walls", true);
   private final BooleanSetting doubleHolesSetting = this.addBoolean("Double Holes", true);
   private final BooleanSetting bedrockHolesSetting = this.addBoolean("Bedrock Holes", true);
   private final ColorSetting bedrockColorSetting = this.addColor("Bedrock Color", -11673978);
   private final BooleanSetting obsidianHolesSetting = this.addBoolean("Obsidian Holes", true);
   private final ColorSetting obsidianColorSetting = this.addColor("Obsidian Color", -1946254);
   private final BooleanSetting mixedHolesSetting = this.addBoolean("Mixed Holes", true);
   private final ColorSetting mixedColorSetting = this.addColor("Mixed Color", -18355);
   private final List<HoleScanner.Hole> holes = new ArrayList<>();
   private ClientWorld trackedWorld;
   private int updateTickTimer;

   public HoleESPModule(){
      super("HoleESP", Category.RENDER, "LÃ m ná»•i báº­t cÃ¡c há»‘ PvP an toÃ n.", -1);
      this.height.visibleWhen(() -> !this.renderModeSetting.is("Flat"));
      this.fillAlphaSetting.visibleWhen(() -> !this.renderModeSetting.is("Outline"));
      this.outlineAlphaSetting.visibleWhen(() -> !this.renderModeSetting.is("Fill"));
      this.lineWidthSetting.visibleWhen(() -> !this.renderModeSetting.is("Fill"));
      this.bedrockColorSetting.visibleWhen(this.bedrockHolesSetting::getValue);
      this.obsidianColorSetting.visibleWhen(this.obsidianHolesSetting::getValue);
      this.mixedColorSetting.visibleWhen(this.mixedHolesSetting::getValue);
      instance = this;
      WorldRenderEvents.BEFORE_DEBUG_RENDER.register(HoleESPModule::renderWorld);
   }

   @Override
   protected void onEnable(){
      this.holes.clear();
      this.trackedWorld = null;
      this.updateTickTimer = 0;
   }

   @Override
   protected void onDisable(){
      this.holes.clear();
      this.trackedWorld = null;
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      if (client.player != null && client.world != null) {
         if (this.trackedWorld != client.world) {
            this.holes.clear();
            this.trackedWorld = client.world;
            this.updateTickTimer = 0;
         }

         if (this.updateTickTimer-- <= 0) {
            HoleScanner.scan(
               client.world,
               client.player.getBlockPos(),
               this.rangeSetting.getValueInt(),
               this.verticalRangeSetting.getValueInt(),
               this.doubleHolesSetting.getValue(),
               this.holes
            );
            this.updateTickTimer = this.updateDelaySetting.getValueInt() - 1;
         }
      } else {
         this.holes.clear();
         this.trackedWorld = null;
         this.updateTickTimer = 0;
      }
   }

   private static void renderWorld(WorldRenderContext context){
      HoleESPModule holeESPModule = instance;
      if (holeESPModule != null && holeESPModule.isEnabled() && !holeESPModule.holes.isEmpty()) {
         boolean is3 = !holeESPModule.renderModeSetting.is("Outline");
         boolean is2 = !holeESPModule.renderModeSetting.is("Fill");
         boolean is = holeESPModule.renderModeSetting.is("Flat");
         double value = is ? 0.025 : holeESPModule.height.getValue();
         int valueInt2 = holeESPModule.fillAlphaSetting.getValueInt();
         int valueInt = holeESPModule.outlineAlphaSetting.getValueInt();

         for (int index = 0; index < holeESPModule.holes.size(); index++) {
            HoleScanner.Hole var10 = holeESPModule.holes.get(index);
            int holeColor = holeESPModule.getHoleColor(var10);
            if (holeColor != 0) {
               double x = var10.x() + 0.035;
               double y = var10.y() + 0.012;
               double z = var10.z() + 0.035;
               double sizeX = var10.x() + var10.sizeX() - 0.035;
               double var20 = y + value;
               double sizeZ = var10.z() + var10.sizeZ() - 0.035;
               RenderUtil.drawWorldBo(
                  context,
                  x,
                  y,
                  z,
                  sizeX,
                  var20,
                  sizeZ,
                  ColorUtil.withAlpha(holeColor, valueInt2),
                  ColorUtil.withAlpha(holeColor, valueInt),
                  is3,
                  is2,
                  holeESPModule.throughWallsSetting.getValue(),
                  holeESPModule.lineWidthSetting.getValueFloat()
               );
            }
         }
      }
   }

   private int getHoleColor(HoleScanner.Hole hole){
      return switch (hole.type()) {
         case BEDROCK -> this.bedrockHolesSetting.getValue() ? this.bedrockColorSetting.getValue() : 0;
         case OBSIDIAN -> this.obsidianHolesSetting.getValue() ? this.obsidianColorSetting.getValue() : 0;
         case MIXED -> this.mixedHolesSetting.getValue() ? this.mixedColorSetting.getValue() : 0;
      };
   }
}

