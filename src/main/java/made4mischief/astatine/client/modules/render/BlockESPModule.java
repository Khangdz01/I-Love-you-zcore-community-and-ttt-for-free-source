package made4mischief.astatine.client.modules.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import made4mischief.astatine.Astatine;
import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.BlockTargetSetting;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.ColorSetting;
import made4mischief.astatine.client.setting.ModeSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import made4mischief.astatine.client.utils.render.RenderUtil;
import made4mischief.astatine.client.utils.render.ScreenTracerRenderer;
import made4mischief.astatine.client.utils.render.core.ColorUtil;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.render.RenderTickCounter;

@Environment(EnvType.CLIENT)
public final class BlockESPModule extends Module {
   private static final double BOX_INSET = 0.004;
   private static BlockESPModule instance;
   private final BlockTargetSetting blockTargetSetting = this.addSetting(
      new BlockTargetSetting("Block Selector", Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.ANCIENT_DEBRIS)
   );
   private final ModeSetting renderModeSetting = this.addMode("Render Mode", "Both", new String[]{"Both", "Fill", "Outline"});
   private final NumberSetting rangeSetting = this.addNumber("Range", 64.0, 8.0, 128.0, 8.0);
   private final NumberSetting updateDelaySetting = this.addNumber("Update Delay", 10.0, 1.0, 40.0, 1.0);
   private final NumberSetting maxBlocksSetting = this.addNumber("max Blocks", 512.0, 32.0, 2048.0, 32.0);
   private final ColorSetting color = this.addColor("Color", -11147009);
   private final NumberSetting fillAlphaSetting = this.addNumber("Fill Alpha", 55.0, 0.0, 255.0, 5.0);
   private final NumberSetting outlineAlphaSetting = this.addNumber("Outline Alpha", 230.0, 0.0, 255.0, 5.0);
   private final NumberSetting lineWidthSetting = this.addNumber("Line Width", 2.0, 1.0, 5.0, 0.5);
   private final BooleanSetting throughWallsSetting = this.addBoolean("Through Walls", true);
   private final BooleanSetting tracersSetting = this.addBoolean("Tracers", true);
   private final NumberSetting tracerWidthSetting = this.addNumber("Tracer Width", 0.75, 0.5, 2.0, 0.25);
   private final List<BlockESPModule.BlockMarker> blockMarkers = new ArrayList<>();
   private final ScreenTracerRenderer tracerRenderer = new ScreenTracerRenderer();
   private ClientWorld trackedWorld;
   private int updateTickTimer;
   private int selectedBlocksHash;

   public BlockESPModule(){
      super("BlockESP", Category.RENDER, "LÃ m ná»•i báº­t khá»‘i Ä‘Ã£ chá»n trong chunk.", -1, true);
      this.fillAlphaSetting.visibleWhen(() -> !this.renderModeSetting.is("Outline"));
      this.outlineAlphaSetting.visibleWhen(() -> !this.renderModeSetting.is("Fill"));
      this.lineWidthSetting.visibleWhen(() -> !this.renderModeSetting.is("Fill"));
      this.tracerWidthSetting.visibleWhen(this.tracersSetting::getValue);
      instance = this;
      WorldRenderEvents.END_MAIN.register(BlockESPModule::renderBoxes);
      HudElementRegistry.attachElementBefore(VanillaHudElements.CROSSHAIR, Astatine.id("block_esp_tracers"), BlockESPModule::renderCounts);
   }

   @Override
   protected void onEnable(){
      this.clearBlocks();
      this.updateTickTimer = 0;
   }

   @Override
   protected void onDisable(){
      this.clearBlocks();
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      if (client.player != null && client.world != null) {
         int hashCode = this.blockTargetSetting.getSelectedBlocks().hashCode();
         if (this.trackedWorld != client.world || this.selectedBlocksHash != hashCode) {
            this.blockMarkers.clear();
            this.trackedWorld = client.world;
            this.selectedBlocksHash = hashCode;
            this.updateTickTimer = 0;
         }

         if (this.updateTickTimer-- <= 0) {
            this.scanWorld(client.world, client.player);
            this.updateTickTimer = this.updateDelaySetting.getValueInt() - 1;
         }
      } else {
         this.clearBlocks();
         this.updateTickTimer = 0;
      }
   }

   private void scanWorld(ClientWorld world, PlayerEntity player){
      this.blockMarkers.clear();
      Set set = this.blockTargetSetting.getSelectedBlocks();
      if (!set.isEmpty()) {
         int valueInt = this.maxBlocksSetting.getValueInt();
         double value = this.rangeSetting.getValue();
         double var7 = value * value;
         int blockX = player.getBlockX() >> 4;
         int blockZ = player.getBlockZ() >> 4;
         int ceil = (int)Math.ceil(value / 16.0) + 1;

         for (int index = 0; index <= ceil; index++) {
            if (index == 0) {
               if (this.scanChunk(world, player, blockX, blockZ, set, var7, valueInt)) {
                  return;
               }
            } else {
               for (int index2 = -index; index2 <= index; index2++) {
                  if (this.scanChunk(world, player, blockX + index2, blockZ - index, set, var7, valueInt)
                     || this.scanChunk(world, player, blockX + index2, blockZ + index, set, var7, valueInt)) {
                     return;
                  }
               }

               for (int index3 = -index + 1; index3 < index; index3++) {
                  if (this.scanChunk(world, player, blockX - index, blockZ + index3, set, var7, valueInt)
                     || this.scanChunk(world, player, blockX + index, blockZ + index3, set, var7, valueInt)) {
                     return;
                  }
               }
            }
         }
      }
   }

   private boolean scanChunk(ClientWorld world, PlayerEntity player, int chunkX, int chunkZ, Set<Block> selected, double rangeSquared, int limit){
      WorldChunk worldChunk = world.getChunkManager().getWorldChunk(chunkX, chunkZ, false);
      if (worldChunk == null) {
         return false;
      } else {
         ChunkSection[] var10 = worldChunk.getSectionArray();

         for (int index4 = 0; index4 < var10.length; index4++) {
            ChunkSection chunkSection = var10[index4];
            if (chunkSection != null && !chunkSection.isEmpty() && chunkSection.hasAny(state -> selected.contains(state.getBlock()))) {
               int var13 = chunkX << 4;
               int bottomY = world.getBottomY() + (index4 << 4);
               int var15 = chunkZ << 4;

               for (int index = 0; index < 16; index++) {
                  for (int index2 = 0; index2 < 16; index2++) {
                     for (int index3 = 0; index3 < 16; index3++) {
                        BlockState state = chunkSection.getBlockState(index3, index, index2);
                        if (selected.contains(state.getBlock())) {
                           BlockPos pos = new BlockPos(var13 + index3, bottomY + index, var15 + index2);
                           double x = pos.getX() + 0.5 - player.getX();
                           double y = pos.getY() + 0.5 - player.getY();
                           double z = pos.getZ() + 0.5 - player.getZ();
                           if (!(x * x + y * y + z * z > rangeSquared)) {
                              this.blockMarkers.add(new BlockESPModule.BlockMarker(pos.toImmutable(), state.getBlock()));
                              if (this.blockMarkers.size() >= limit) {
                                 return true;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         return false;
      }
   }

   private static void renderBoxes(WorldRenderContext context){
      BlockESPModule blockESPModule = instance;
      if (blockESPModule != null && blockESPModule.isEnabled() && !blockESPModule.blockMarkers.isEmpty()) {
         boolean is2 = !blockESPModule.renderModeSetting.is("Outline");
         boolean is = !blockESPModule.renderModeSetting.is("Fill");
         int value = blockESPModule.color.getValue();

         for (BlockESPModule.BlockMarker blockMarker : blockESPModule.blockMarkers) {
            if (blockESPModule.blockTargetSetting.isSelected(blockMarker.block())) {
               BlockPos pos = blockMarker.position();
               RenderUtil.drawWorldBo(
                  context,
                  pos.getX() - 0.004,
                  pos.getY() - 0.004,
                  pos.getZ() - 0.004,
                  pos.getX() + 1.0 + 0.004,
                  pos.getY() + 1.0 + 0.004,
                  pos.getZ() + 1.0 + 0.004,
                  ColorUtil.withAlpha(value, blockESPModule.fillAlphaSetting.getValueInt()),
                  ColorUtil.withAlpha(value, blockESPModule.outlineAlphaSetting.getValueInt()),
                  is2,
                  is,
                  blockESPModule.throughWallsSetting.getValue(),
                  blockESPModule.lineWidthSetting.getValueFloat()
               );
            }
         }
      }
   }

   private static void renderCounts(DrawContext context, RenderTickCounter tickCounter){
      BlockESPModule blockESPModule = instance;
      MinecraftClient client = MinecraftClient.getInstance();
      if (blockESPModule != null
         && blockESPModule.isEnabled()
         && blockESPModule.tracersSetting.getValue()
         && !blockESPModule.blockMarkers.isEmpty()
         && client.player != null
         && client.world != null
         && blockESPModule.tracerRenderer.begin(context, client)) {
         int value = blockESPModule.color.getValue();
         float valueFloat = blockESPModule.tracerWidthSetting.getValueFloat();

         for (BlockESPModule.BlockMarker blockMarker : blockESPModule.blockMarkers) {
            if (blockESPModule.blockTargetSetting.isSelected(blockMarker.block())) {
               blockESPModule.tracerRenderer.draw(context, client, Vec3d.ofCenter(blockMarker.position()), valueFloat, value);
            }
         }
      }
   }

   private void clearBlocks(){
      this.blockMarkers.clear();
      this.trackedWorld = null;
      this.selectedBlocksHash = 0;
   }

   @Environment(EnvType.CLIENT)
   private record BlockMarker(BlockPos position, Block block){
   }
}

