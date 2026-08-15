package made4mischief.astatine.client.modules.smp;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import made4mischief.astatine.client.hud.NotificationRenderer;
import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.ColorSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import made4mischief.astatine.client.utils.render.RenderUtil;
import made4mischief.astatine.client.utils.render.core.ColorUtil;
import made4mischief.astatine.client.utils.render.core.SoundUtil;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.block.PillarBlock;
import net.minecraft.text.Text;
import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.AbstractPlantStemBlock;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.world.Heightmap;

@Environment(EnvType.CLIENT)
public final class ChunkFinderModule extends Module {
   private static final double BOX_Y = 70.0;
   private static final double BOX_HEIGHT = 0.035;
   private static final int MUSHROOM_SCAN_RADIUS = 4;
   private static final int MUSHROOM_SCAN_Y_RANGE = 1;
   private static final int AREA_SOLID_RADIUS = 7;
   private static final int AREA_SCAN_PADDING = 4;
   private static final int MIN_COBBLESTONE_COUNT = 20;
   private static final int MIN_AREA_EXTENT = 5;
   private static final int MIN_MOSSY_COUNT = 2;
   private static final ChunkFinderModule.DetectionResult EMPTY_DETECTION = new ChunkFinderModule.DetectionResult(0, 0, false);
   private static final ChunkFinderModule.MushroomDetections EMPTY_MUSHROOM_DETECTIONS = new ChunkFinderModule.MushroomDetections(EMPTY_DETECTION, EMPTY_DETECTION);
   private static ChunkFinderModule instance;
   private final BooleanSetting kelpSetting = this.addBoolean("Kelp", true);
   private final NumberSetting kelpThresholdSetting = this.addNumber("Kelp Threshold", 5.0, 1.0, 20.0, 1.0);
   private final BooleanSetting glowBerrySetting = this.addBoolean("Glow Berry", true);
   private final NumberSetting glowBerryThresholdSetting = this.addNumber("Glow Berry Threshold", 20.0, 1.0, 20.0, 1.0);
   private final BooleanSetting strictGlowBerrySetting = this.addBoolean("Strict Glow Berry", true);
   private final BooleanSetting mushroomDetectorSetting = this.addBoolean("Mushroom Detector", true);
   private final NumberSetting mushroomThresholdSetting = this.addNumber("Mushroom Threshold", 5.0, 5.0, 7.0, 1.0);
   private final BooleanSetting honeyNestSetting = this.addBoolean("Honey Nest", true);
   private final BooleanSetting buriedTunnelSetting = this.addBoolean("Buried Tunnel", true);
   private final NumberSetting buriedTunnelThresholdSetting = this.addNumber("Buried Tunnel Threshold", 10.0, 2.0, 20.0, 1.0);
   private final BooleanSetting rotatedDeepslateSetting = this.addBoolean("Rotated Deepslate", true);
   private final NumberSetting chunksPerTickSetting = this.addNumber("Chunks Per Tick", 4.0, 1.0, 16.0, 1.0);
   private final NumberSetting clearDistanceSetting = this.addNumber("Clear Distance", 500.0, 64.0, 1000.0, 25.0);
   private final BooleanSetting notifications = this.addBoolean("Notifications", true);
   private final BooleanSetting renderChunksSetting = this.addBoolean("Render Chunks", true);
   private final ColorSetting renderColorSetting = this.addColor("Render Color", -4776932);
   private final NumberSetting fillAlphaSetting = this.addNumber("Fill Alpha", 120.0, 0.0, 255.0, 5.0);
   private final ArrayDeque<ChunkPos> pendingChunks = new ArrayDeque<>();
   private final Map<Long, ChunkFinderModule.FlaggedChunk> flaggedChunks = new LinkedHashMap<>();
   private ClientWorld trackedWorld;
   private int scanCenterChunkX = Integer.MIN_VALUE;
   private int scanCenterChunkZ = Integer.MIN_VALUE;

   public ChunkFinderModule(){
      super("ChunkFinder", Category.SMP, "LMAOO.", -1);
      this.kelpThresholdSetting.visibleWhen(this.kelpSetting::getValue);
      this.glowBerryThresholdSetting.visibleWhen(this.glowBerrySetting::getValue);
      this.strictGlowBerrySetting.visibleWhen(this.glowBerrySetting::getValue);
      this.mushroomThresholdSetting.visibleWhen(this.mushroomDetectorSetting::getValue);
      this.buriedTunnelThresholdSetting.visibleWhen(this.buriedTunnelSetting::getValue);
      this.renderColorSetting.visibleWhen(this.renderChunksSetting::getValue);
      this.fillAlphaSetting.visibleWhen(this.renderChunksSetting::getValue);
      instance = this;
      WorldRenderEvents.BEFORE_DEBUG_RENDER.register(ChunkFinderModule::renderWireframes);
   }

   @Override
   protected void onEnable(){
      this.clearDetections();
   }

   @Override
   protected void onDisable(){
      this.clearDetections();
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      if (client.player != null && client.world != null && client.world.getRegistryKey().equals(World.OVERWORLD)) {
         if (this.trackedWorld != client.world) {
            this.clearDetections();
            this.trackedWorld = client.world;
         }

         this.scanSurroundingChunks(client);
         this.checkAutoDisable();
         int blockX = client.player.getBlockX() >> 4;
         int blockZ = client.player.getBlockZ() >> 4;
         if (this.pendingChunks.isEmpty() || blockX != this.scanCenterChunkX || blockZ != this.scanCenterChunkZ) {
            this.clearScan(client, blockX, blockZ);
         }

         int valueInt = this.chunksPerTickSetting.getValueInt();

         for (int index = 0; index < valueInt && !this.pendingChunks.isEmpty(); index++) {
            ChunkPos chunkPos = this.pendingChunks.removeFirst();
            WorldChunk worldChunk = client.world.getChunkManager().getWorldChunk(chunkPos.x, chunkPos.z, false);
            if (worldChunk != null && this.isChunkInRange(client, chunkPos.x, chunkPos.z)) {
               this.detectInChunk(worldChunk, client.world);
            }
         }
      } else {
         this.clearDetections();
      }
   }

   private void clearScan(MinecraftClient client, int centerX, int centerZ){
      this.pendingChunks.clear();
      this.scanCenterChunkX = centerX;
      this.scanCenterChunkZ = centerZ;
      int clampedViewDistance = Math.max(2, client.options.getClampedViewDistance() + 2);
      this.scanChunk(client, centerX, centerZ);

      for (int index2 = 1; index2 <= clampedViewDistance; index2++) {
         int var6 = centerX - index2;
         int var7 = centerX + index2;
         int var8 = centerZ - index2;
         int var9 = centerZ + index2;

         for (int index3 = var6; index3 <= var7; index3++) {
            this.scanChunk(client, index3, var8);
            this.scanChunk(client, index3, var9);
         }

         for (int index = var8 + 1; index < var9; index++) {
            this.scanChunk(client, var6, index);
            this.scanChunk(client, var7, index);
         }
      }
   }

   private void scanChunk(MinecraftClient client, int chunkX, int chunkZ){
      if (this.isChunkInRange(client, chunkX, chunkZ)) {
         if (client.world.getChunkManager().getWorldChunk(chunkX, chunkZ, false) != null) {
            this.pendingChunks.addLast(new ChunkPos(chunkX, chunkZ));
         }
      }
   }

   private boolean isChunkInRange(MinecraftClient client, int chunkX, int chunkZ){
      double var4 = chunkX * 16.0 + 8.0;
      double var6 = chunkZ * 16.0 + 8.0;
      double value = this.clearDistanceSetting.getValue() * this.clearDistanceSetting.getValue();
      double x = client.player.getX() - var4;
      double z = client.player.getZ() - var6;
      return x * x + z * z <= value;
   }

   private void scanSurroundingChunks(MinecraftClient client){
      double x = client.player.getX();
      double z = client.player.getZ();
      double value = this.clearDistanceSetting.getValue() * this.clearDistanceSetting.getValue();
      this.flaggedChunks.entrySet().removeIf(entry -> {
         ChunkFinderModule.FlaggedChunk var7 = entry.getValue();
         double var8 = x - (var7.chunkX * 16.0 + 8.0);
         double var10 = z - (var7.chunkZ * 16.0 + 8.0);
         return var8 * var8 + var10 * var10 > value;
      });
   }

   private void detectInChunk(WorldChunk chunk, ClientWorld world){
      if (hasAnyDetection(chunk)) {
         this.flaggedChunks.remove(chunk.getPos().toLong());
      } else {
         ChunkFinderModule.DetectionResult var3 = this.kelpSetting.getValue() ? this.detectChests(chunk, world) : EMPTY_DETECTION;
         ChunkFinderModule.DetectionResult var4 = this.glowBerrySetting.getValue() ? this.detectBarrels(chunk, world) : EMPTY_DETECTION;
         ChunkFinderModule.MushroomDetections var5 = this.mushroomDetectorSetting.getValue() ? this.detectMushrooms(chunk, world) : EMPTY_MUSHROOM_DETECTIONS;
         ChunkFinderModule.DetectionResult var6 = this.honeyNestSetting.getValue() ? this.detectDoorways(chunk) : EMPTY_DETECTION;
         ChunkFinderModule.DetectionResult var7 = this.buriedTunnelSetting.getValue() ? this.detectLavaPockets(chunk, world) : EMPTY_DETECTION;
         ChunkFinderModule.DetectionResult var8 = this.rotatedDeepslateSetting.getValue() ? this.detectPortals(chunk) : EMPTY_DETECTION;
         this.reportDetection(chunk.getPos(), var3, var4, var5.red, var5.brown, var6, var7, var8);
      }
   }

   private void checkAutoDisable(){
      if (!this.kelpSetting.getValue()
         || !this.glowBerrySetting.getValue()
         || !this.mushroomDetectorSetting.getValue()
         || !this.honeyNestSetting.getValue()
         || !this.buriedTunnelSetting.getValue()
         || !this.rotatedDeepslateSetting.getValue()) {
         this.flaggedChunks
            .replaceAll(
               (key, flagged) -> new ChunkFinderModule.FlaggedChunk(
                  flagged.chunkX,
                  flagged.chunkZ,
                  this.kelpSetting.getValue() ? flagged.kelp : EMPTY_DETECTION,
                  this.glowBerrySetting.getValue() ? flagged.glowBerry : EMPTY_DETECTION,
                  this.mushroomDetectorSetting.getValue() ? flagged.redMushroom : EMPTY_DETECTION,
                  this.mushroomDetectorSetting.getValue() ? flagged.brownMushroom : EMPTY_DETECTION,
                  this.honeyNestSetting.getValue() ? flagged.fullHoney : EMPTY_DETECTION,
                  this.buriedTunnelSetting.getValue() ? flagged.buriedTunnel : EMPTY_DETECTION,
                  this.rotatedDeepslateSetting.getValue() ? flagged.rotatedDeepslate : EMPTY_DETECTION
               )
            );
         this.flaggedChunks
            .entrySet()
            .removeIf(
               entry -> {
                  ChunkFinderModule.FlaggedChunk var1 = entry.getValue();
                  return !var1.kelp.detected
                     && !var1.glowBerry.detected
                     && !var1.redMushroom.detected
                     && !var1.brownMushroom.detected
                     && !var1.fullHoney.detected
                     && !var1.buriedTunnel.detected
                     && !var1.rotatedDeepslate.detected;
               }
            );
      }
   }

   private static boolean hasAnyDetection(WorldChunk chunk){
      boolean var1 = false;
      boolean var2 = false;

      for (ChunkSection chunkSection : chunk.getSectionArray()) {
         if (!chunkSection.isEmpty()) {
            if (chunkSection.hasAny(state -> state.isOf(Blocks.TRIAL_SPAWNER) || state.isOf(Blocks.VAULT))) {
               return true;
            }

            if (!var1 && chunkSection.hasAny(ChunkFinderModule::isChestLikeBlock)) {
               var1 = true;
            }

            if (!var2 && chunkSection.hasAny(ChunkFinderModule::isShulkerLikeBlock)) {
               var2 = true;
            }
         }
      }

      return var1 && var2;
   }

   private static boolean isChestLikeBlock(BlockState state){
      String path = Registries.BLOCK.getId(state.getBlock()).getPath();
      return path.equals("tuff") || path.startsWith("tuff_") || path.startsWith("polished_tuff") || path.startsWith("chiseled_tuff");
   }

   private static boolean isShulkerLikeBlock(BlockState state){
      String path = Registries.BLOCK.getId(state.getBlock()).getPath();
      return path.contains("copper")
         && (path.startsWith("waxed_") || path.startsWith("oxidized_") || path.startsWith("weathered_") || path.equals("copper_block"));
   }

   private ChunkFinderModule.DetectionResult detectChests(WorldChunk chunk, ClientWorld world){
      ChunkPos chunkPos = chunk.getPos();
      Mutable mutable = new Mutable();
      int index5 = 0;
      int max = 0;

      for (int index4 = 0; index4 < 16; index4++) {
         int startX = chunkPos.getStartX() + index4;

         for (int index = 0; index < 16; index++) {
            int startZ = chunkPos.getStartZ() + index;
            int sampleHeightmap = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, startX, startZ);
            mutable.set(startX, sampleHeightmap, startZ);
            BlockState state = chunk.getBlockState(mutable);
            if (state.isOf(Blocks.KELP)
               && (Integer)state.get(AbstractPlantStemBlock.AGE) >= 25
               && world.getBiome(mutable).isIn(BiomeTags.IS_OCEAN)) {
               mutable.setY(sampleHeightmap + 1);
               if (!chunk.getFluidState(mutable).isIn(FluidTags.WATER)) {
                  int index2 = 0;

                  for (int index3 = sampleHeightmap; index3 >= world.getBottomY(); index3--) {
                     mutable.set(startX, index3, startZ);
                     BlockState state2 = chunk.getBlockState(mutable);
                     if (!state2.isOf(Blocks.KELP) && !state2.isOf(Blocks.KELP_PLANT)) {
                        break;
                     }

                     index2++;
                  }

                  if (index2 > 0) {
                     index5++;
                     max = Math.max(max, index2);
                  }
               }
            }
         }
      }

      return new ChunkFinderModule.DetectionResult(index5, max, index5 >= this.kelpThresholdSetting.getValueInt());
   }

   private ChunkFinderModule.DetectionResult detectBarrels(WorldChunk chunk, ClientWorld world){
      int index7 = 0;
      int max = 0;
      Mutable mutable = new Mutable();
      ChunkSection[] var6 = chunk.getSectionArray();

      for (int index4 = 0; index4 < var6.length; index4++) {
         ChunkSection chunkSection = var6[index4];
         if (!chunkSection.isEmpty() && chunkSection.hasAny(state -> state.isOf(Blocks.CAVE_VINES))) {
            int bottomY = world.getBottomY() + index4 * 16;

            for (int index5 = 0; index5 < 16; index5++) {
               for (int index6 = 0; index6 < 16; index6++) {
                  for (int index3 = 0; index3 < 16; index3++) {
                     BlockState state = chunkSection.getBlockState(index6, index5, index3);
                     if (state.isOf(Blocks.CAVE_VINES) && (Integer)state.get(AbstractPlantStemBlock.AGE) >= 25) {
                        int startX = chunk.getPos().getStartX() + index6;
                        int var15 = bottomY + index5;
                        int startZ = chunk.getPos().getStartZ() + index3;
                        int index = 1;
                        int index22 = var15 + 1;

                        for (int index2 = var15 + 1; index2 <= world.getTopYInclusive(); index2++) {
                           mutable.set(startX, index2, startZ);
                           BlockState state2 = chunk.getBlockState(mutable);
                           if (!state2.isOf(Blocks.CAVE_VINES) && !state2.isOf(Blocks.CAVE_VINES_PLANT)) {
                              index22 = index2;
                              break;
                           }

                           index++;
                           index22 = index2 + 1;
                        }

                        mutable.set(startX, var15, startZ);
                        boolean matchesKey = world.getBiome(mutable).matchesKey(BiomeKeys.LUSH_CAVES);
                        mutable.set(startX, index22, startZ);
                        BlockState state3 = world.getBlockState(mutable);
                        if (!this.strictGlowBerrySetting.getValue() || !matchesKey || isFullOpaqueBlock(state3)) {
                           index7++;
                           max = Math.max(max, index);
                        }
                     }
                  }
               }
            }
         }
      }

      return new ChunkFinderModule.DetectionResult(index7, max, index7 >= this.glowBerryThresholdSetting.getValueInt());
   }

   private static boolean isFullOpaqueBlock(BlockState state){
      return state.isIn(BlockTags.PLANKS)
         || state.isIn(BlockTags.SLABS)
         || state.isIn(BlockTags.STAIRS)
         || state.isIn(BlockTags.WALLS)
         || state.isIn(BlockTags.FENCES)
         || state.isOf(Blocks.COBBLESTONE)
         || state.isOf(Blocks.STONE_BRICKS)
         || state.isOf(Blocks.BRICKS)
         || state.isOf(Blocks.GLASS)
         || state.isOf(Blocks.GLASS_PANE)
         || state.isOf(Blocks.IRON_BLOCK);
   }

   private ChunkFinderModule.MushroomDetections detectMushrooms(WorldChunk chunk, ClientWorld world){
      ArrayList var3 = new ArrayList();
      ArrayList var4 = new ArrayList();
      ChunkSection[] var5 = chunk.getSectionArray();

      for (int index2 = 0; index2 < var5.length; index2++) {
         ChunkSection chunkSection = var5[index2];
         if (!chunkSection.isEmpty() && chunkSection.hasAny(state -> state.isOf(Blocks.RED_MUSHROOM) || state.isOf(Blocks.BROWN_MUSHROOM))) {
            int bottomY = world.getBottomY() + index2 * 16;

            for (int index = 0; index < 16; index++) {
               for (int index3 = 0; index3 < 16; index3++) {
                  for (int index4 = 0; index4 < 16; index4++) {
                     BlockState state = chunkSection.getBlockState(index3, index, index4);
                     if (state.isOf(Blocks.RED_MUSHROOM) || state.isOf(Blocks.BROWN_MUSHROOM)) {
                        BlockPos pos = new BlockPos(chunk.getPos().getStartX() + index3, bottomY + index, chunk.getPos().getStartZ() + index4);
                        if (!hasBlockAbove(world, pos)) {
                           if (state.isOf(Blocks.RED_MUSHROOM)) {
                              var3.add(pos);
                           } else {
                              var4.add(pos);
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      ChunkFinderModule.DetectionResult var14 = this.detectMushroomGrowth(world, var3, Blocks.RED_MUSHROOM);
      ChunkFinderModule.DetectionResult var15 = this.detectMushroomGrowth(world, var4, Blocks.BROWN_MUSHROOM);
      return new ChunkFinderModule.MushroomDetections(var14, var15);
   }

   private ChunkFinderModule.DetectionResult detectMushroomGrowth(ClientWorld world, List<BlockPos> origins, Block mushroom){
      int max = 0;
      Mutable mutable = new Mutable();

      for (BlockPos pos : origins) {
         int index2 = 0;

         for (int index = pos.getX() - 4; index <= pos.getX() + 4; index++) {
            for (int index3 = pos.getY() - 1; index3 <= pos.getY() + 1; index3++) {
               for (int index4 = pos.getZ() - 4; index4 <= pos.getZ() + 4; index4++) {
                  mutable.set(index, index3, index4);
                  if (world.getBlockState(mutable).isOf(mushroom) && !hasBlockAbove(world, mutable)) {
                     if (++index2 >= this.mushroomThresholdSetting.getValueInt()) {
                        return new ChunkFinderModule.DetectionResult(index2, 0, true);
                     }
                  }
               }
            }
         }

         max = Math.max(max, index2);
      }

      return new ChunkFinderModule.DetectionResult(max, 0, false);
   }

   private static boolean hasBlockAbove(ClientWorld world, BlockPos pos){
      RegistryEntry registryEntry = world.getBiome(pos);
      return registryEntry.isIn(BiomeTags.IS_FOREST)
         || registryEntry.isIn(BiomeTags.IS_TAIGA)
         || registryEntry.matchesKey(BiomeKeys.SWAMP)
         || registryEntry.matchesKey(BiomeKeys.MANGROVE_SWAMP)
         || registryEntry.matchesKey(BiomeKeys.MUSHROOM_FIELDS);
   }

   private ChunkFinderModule.DetectionResult detectDoorways(WorldChunk chunk){
      for (ChunkSection chunkSection : chunk.getSectionArray()) {
         if (!chunkSection.isEmpty() && chunkSection.hasAny(ChunkFinderModule::isDoorBlock)) {
            return new ChunkFinderModule.DetectionResult(1, 0, true);
         }
      }

      return EMPTY_DETECTION;
   }

   private static boolean isDoorBlock(BlockState state){
      return (state.isOf(Blocks.BEE_NEST) || state.isOf(Blocks.BEEHIVE))
         && (Integer)state.get(BeehiveBlock.HONEY_LEVEL) >= 5;
   }

   private ChunkFinderModule.DetectionResult detectLavaPockets(WorldChunk chunk, ClientWorld world){
      ChunkPos chunkPos = chunk.getPos();
      Mutable mutable = new Mutable();
      int valueInt = this.buriedTunnelThresholdSetting.getValueInt();
      int bottomY = Math.max(0, world.getBottomY());

      for (int index4 = 0; index4 < 16; index4++) {
         int startX = chunkPos.getStartX() + index4;

         for (int index = 0; index < 16; index++) {
            int startZ = chunkPos.getStartZ() + index;
            int sampleHeightmap = chunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, startX, startZ);
            int topYInclusive = findTopSolidY(chunk, mutable, startX, Math.min(sampleHeightmap, world.getTopYInclusive()), startZ, bottomY);
            if (topYInclusive != Integer.MIN_VALUE) {
               int index2 = topYInclusive;

               int index3;
               for (index3 = 0; index2 >= bottomY; index2--) {
                  mutable.set(startX, index2, startZ);
                  if (!isAir(chunk.getBlockState(mutable))) {
                     break;
                  }

                  index3++;
               }

               if (index3 != 0 && index2 >= bottomY) {
                  int index5 = 0;

                  int index22;
                  for (index22 = index2; index2 >= bottomY; index2--) {
                     mutable.set(startX, index2, startZ);
                     if (!isNormalBlock(chunk.getBlockState(mutable))) {
                        break;
                     }

                     index5++;
                  }

                  int var17 = index2 + 1;
                  if (index5 >= valueInt
                     && isSolidColumn(world, startX, index22 + 1, startZ)
                     && isFilledArea(world, startX, index22, var17, startZ)
                     && !isAreaSolid(world, startX, index22, var17, startZ)) {
                     return new ChunkFinderModule.DetectionResult(index3, index5, true);
                  }
               }
            }
         }
      }

      return EMPTY_DETECTION;
   }

   private static boolean isSolidColumn(ClientWorld world, int x, int y, int z){
      Mutable mutable = new Mutable();

      for (Direction direction : Direction.values()) {
         mutable.set(x + direction.getOffsetX(), y + direction.getOffsetY(), z + direction.getOffsetZ());
         if (world.getBlockState(mutable).isAir()) {
            return true;
         }
      }

      return false;
   }

   private static boolean isFilledArea(ClientWorld world, int centerX, int fillTopY, int fillBottomY, int centerZ){
      int var5 = fillTopY - fillBottomY + 1;
      int min = Math.min(5, var5);
      int index4 = 0;
      Mutable mutable = new Mutable();

      for (int index2 = 0; index2 < min; index2++) {
         int round = min == 1 ? 0 : Math.round(index2 * (var5 - 1.0F) / (min - 1));
         int var11 = fillTopY - round;
         int index = 0;
         int index3 = 0;

         for (Direction direction : Direction.Type.HORIZONTAL) {
            mutable.set(centerX + direction.getOffsetX(), var11, centerZ + direction.getOffsetZ());
            BlockState state = world.getBlockState(mutable);
            if (isVisibleFullBlock(state)) {
               index++;
            } else if (isNormalBlock(state)) {
               index3++;
            }
         }

         if (index >= 3 && index3 <= 1) {
            index4++;
         }
      }

      return index4 * 2 >= min;
   }

   private static boolean isVisibleFullBlock(BlockState state){
      return !state.isAir() && state.getFluidState().isEmpty() && !state.isIn(BlockTags.REPLACEABLE) && isOpaqueBlock(state);
   }

   private static boolean isAreaSolid(ClientWorld world, int centerX, int fillTopY, int fillBottomY, int centerZ){
      Mutable mutable = new Mutable();
      int bottomY = Math.max(world.getBottomY(), fillBottomY - 4);
      int topYInclusive = Math.min(world.getTopYInclusive(), fillTopY + 4);

      for (int index3 = bottomY; index3 <= topYInclusive; index3++) {
         int index2 = 0;
         int index5 = 0;
         int min2 = Integer.MAX_VALUE;
         int max = Integer.MIN_VALUE;
         int min = Integer.MAX_VALUE;
         int max2 = Integer.MIN_VALUE;

         for (int index4 = centerX - 7; index4 <= centerX + 7; index4++) {
            for (int index = centerZ - 7; index <= centerZ + 7; index++) {
               mutable.set(index4, index3, index);
               BlockState state = world.getBlockState(mutable);
               if (state.isOf(Blocks.SPAWNER)) {
                  return true;
               }

               if (state.isOf(Blocks.COBBLESTONE) || state.isOf(Blocks.MOSSY_COBBLESTONE)) {
                  index2++;
                  if (state.isOf(Blocks.MOSSY_COBBLESTONE)) {
                     index5++;
                  }

                  min2 = Math.min(min2, index4);
                  max = Math.max(max, index4);
                  min = Math.min(min, index);
                  max2 = Math.max(max2, index);
               }
            }
         }

         int var18 = index2 == 0 ? 0 : max - min2 + 1;
         int var19 = index2 == 0 ? 0 : max2 - min + 1;
         if (index2 >= 20 && index5 >= 2 && var18 >= 5 && var19 >= 5) {
            return true;
         }
      }

      return false;
   }

   private static int findTopSolidY(WorldChunk chunk, Mutable mutable, int x, int topY, int z, int minimumVisibleY){
      for (int index = topY; index >= minimumVisibleY; index--) {
         mutable.set(x, index, z);
         BlockState state = chunk.getBlockState(mutable);
         if (!state.isAir() && state.getFluidState().isEmpty() && !state.isIn(BlockTags.REPLACEABLE)) {
            return isAir(state) ? index : Integer.MIN_VALUE;
         }
      }

      return Integer.MIN_VALUE;
   }

   private static boolean isAir(BlockState state){
      return state.isIn(BlockTags.DIRT)
         || state.isIn(BlockTags.SAND)
         || state.isOf(Blocks.GRAVEL)
         || state.isOf(Blocks.STONE);
   }

   private ChunkFinderModule.DetectionResult detectPortals(WorldChunk chunk){
      for (ChunkSection chunkSection : chunk.getSectionArray()) {
         if (!chunkSection.isEmpty() && chunkSection.hasAny(ChunkFinderModule::isPortalBlock)) {
            return new ChunkFinderModule.DetectionResult(1, 0, true);
         }
      }

      return EMPTY_DETECTION;
   }

   private static boolean isPortalBlock(BlockState state){
      return state.isOf(Blocks.DEEPSLATE)
         && state.contains(PillarBlock.AXIS)
         && state.get(PillarBlock.AXIS) != Axis.Y;
   }

   private static boolean isNormalBlock(BlockState state){
      return !isOpaqueBlock(state) && !isAnomalousBlock(state);
   }

   private static boolean isOpaqueBlock(BlockState state){
      if (!state.isAir()
         && state.getFluidState().isEmpty()
         && !state.isIn(BlockTags.REPLACEABLE)
         && !state.isIn(BlockTags.BASE_STONE_OVERWORLD)
         && !state.isIn(BlockTags.DIRT)
         && !state.isIn(BlockTags.SAND)
         && !state.isIn(BlockTags.TERRACOTTA)
         && !state.isIn(BlockTags.ICE)
         && !state.isIn(BlockTags.SNOW)
         && !state.isIn(BlockTags.STONE_ORE_REPLACEABLES)
         && !state.isIn(BlockTags.DEEPSLATE_ORE_REPLACEABLES)
         && !state.isIn(BlockTags.COAL_ORES)
         && !state.isIn(BlockTags.IRON_ORES)
         && !state.isIn(BlockTags.COPPER_ORES)
         && !state.isIn(BlockTags.GOLD_ORES)
         && !state.isIn(BlockTags.REDSTONE_ORES)
         && !state.isIn(BlockTags.EMERALD_ORES)
         && !state.isIn(BlockTags.LAPIS_ORES)
         && !state.isIn(BlockTags.DIAMOND_ORES)) {
         Block block = state.getBlock();
         return block == Blocks.BEDROCK
            || block == Blocks.GRAVEL
            || block == Blocks.CLAY
            || block == Blocks.SANDSTONE
            || block == Blocks.RED_SANDSTONE
            || block == Blocks.CALCITE
            || block == Blocks.DRIPSTONE_BLOCK
            || block == Blocks.POINTED_DRIPSTONE
            || block == Blocks.MAGMA_BLOCK
            || block == Blocks.MOSS_BLOCK
            || block == Blocks.MOSS_CARPET
            || block == Blocks.MUD
            || block == Blocks.MANGROVE_ROOTS
            || block == Blocks.MUDDY_MANGROVE_ROOTS
            || block == Blocks.BONE_BLOCK
            || block == Blocks.AMETHYST_BLOCK
            || block == Blocks.BUDDING_AMETHYST
            || block == Blocks.AMETHYST_CLUSTER
            || block == Blocks.LARGE_AMETHYST_BUD
            || block == Blocks.MEDIUM_AMETHYST_BUD
            || block == Blocks.SMALL_AMETHYST_BUD
            || block == Blocks.SCULK
            || block == Blocks.SCULK_VEIN
            || block == Blocks.SCULK_CATALYST
            || block == Blocks.SCULK_SENSOR
            || block == Blocks.SCULK_SHRIEKER
            || block == Blocks.GLOW_LICHEN
            || block == Blocks.CAVE_VINES
            || block == Blocks.CAVE_VINES_PLANT;
      } else {
         return true;
      }
   }

   private static boolean isAnomalousBlock(BlockState state){
      if (!state.isOf(Blocks.TRIAL_SPAWNER) && !state.isOf(Blocks.VAULT) && !isChestLikeBlock(state) && !isShulkerLikeBlock(state)) {
         String path = Registries.BLOCK.getId(state.getBlock()).getPath();
         return path.equals("decorated_pot")
            || path.equals("red_glazed_terracotta")
            || path.equals("black_stained_glass")
            || path.equals("light_gray_stained_glass")
            || path.equals("white_stained_glass");
      } else {
         return true;
      }
   }

   private void reportDetection(
      ChunkPos pos,
      ChunkFinderModule.DetectionResult kelp,
      ChunkFinderModule.DetectionResult glowBerry,
      ChunkFinderModule.DetectionResult redMushroom,
      ChunkFinderModule.DetectionResult brownMushroom,
      ChunkFinderModule.DetectionResult fullHoney,
      ChunkFinderModule.DetectionResult buriedTunnel,
      ChunkFinderModule.DetectionResult rotatedDeepslate
   ){
      long toLong = pos.toLong();
      ChunkFinderModule.FlaggedChunk var11 = this.flaggedChunks.get(toLong);
      if (!kelp.detected
         && !glowBerry.detected
         && !redMushroom.detected
         && !brownMushroom.detected
         && !fullHoney.detected
         && !buriedTunnel.detected
         && !rotatedDeepslate.detected) {
         this.flaggedChunks.remove(toLong);
      } else {
         this.flaggedChunks
            .put(
               toLong,
               new ChunkFinderModule.FlaggedChunk(
                  pos.x, pos.z, kelp, glowBerry, redMushroom, brownMushroom, fullHoney, buriedTunnel, rotatedDeepslate
               )
            );
      }

      if (kelp.detected && (var11 == null || !var11.kelp.detected)) {
         this.logDetection(pos, "Kelp", kelp.count + " max-age surface columns");
      }

      if (glowBerry.detected && (var11 == null || !var11.glowBerry.detected)) {
         this.logDetection(pos, "Glow Berry", glowBerry.count + " max-age vines");
      }

      if (redMushroom.detected && (var11 == null || !var11.redMushroom.detected)) {
         this.logDetection(pos, "Red Mushroom", redMushroom.count + " mushrooms within a 9x9x3 area");
      }

      if (brownMushroom.detected && (var11 == null || !var11.brownMushroom.detected)) {
         this.logDetection(pos, "Brown Mushroom", brownMushroom.count + " mushrooms within a 9x9x3 area");
      }

      if (fullHoney.detected && (var11 == null || !var11.fullHoney.detected)) {
         this.logDetection(pos, "Full Honey Nest", "bee nest or beehive at honey level 5");
      }

      if (buriedTunnel.detected && (var11 == null || !var11.buriedTunnel.detected)) {
         this.logDetection(pos, "Buried Tunnel", buriedTunnel.longest + "+ suspicious fill blocks below " + buriedTunnel.count + " natural cover blocks");
      }

      if (rotatedDeepslate.detected && (var11 == null || !var11.rotatedDeepslate.detected)) {
         this.logDetection(pos, "Rotated Deepslate", "horizontal-axis deepslate placed on a wall");
      }
   }

   private void logDetection(ChunkPos pos, String detector, String details){
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null) {
         client.player
            .sendMessage(
               Text.literal(
                  "Â§8[Â§cChunkFinderÂ§8] Â§f" + detector + " activity in chunk Â§c" + pos.x + ", " + pos.z + " Â§7(" + details + ")"
               ),
               false
            );
      }

      if (this.notifications.getValue()) {
         NotificationRenderer.showAlert(
            "ChunkFinder: " + detector,
            "Flagged chunk " + pos.x + ", " + pos.z + " (" + details + ")",
            NotificationRenderer.NotificationType.WARNING
         );
         SoundUtil.playNotification();
      }
   }

   private void clearDetections(){
      this.pendingChunks.clear();
      this.flaggedChunks.clear();
      this.trackedWorld = null;
      this.scanCenterChunkX = Integer.MIN_VALUE;
      this.scanCenterChunkZ = Integer.MIN_VALUE;
   }

   private static void renderWireframes(WorldRenderContext context){
      ChunkFinderModule chunkFinderModule = instance;
      if (chunkFinderModule != null && chunkFinderModule.isEnabled() && chunkFinderModule.renderChunksSetting.getValue() && !chunkFinderModule.flaggedChunks.isEmpty()) {
         int value = chunkFinderModule.renderColorSetting.getValue();
         int valueInt = ColorUtil.withAlpha(value, chunkFinderModule.fillAlphaSetting.getValueInt());

         for (ChunkFinderModule.FlaggedChunk flaggedChunk : chunkFinderModule.flaggedChunks.values()) {
            double var6 = flaggedChunk.chunkX * 16.0;
            double var8 = flaggedChunk.chunkZ * 16.0;
            RenderUtil.drawWorldBo(context, var6, 70.0, var8, var6 + 16.0, 70.035, var8 + 16.0, valueInt, 0, true, false, true, 1.0F);
         }
      }
   }

   @Environment(EnvType.CLIENT)
   private record DetectionResult(int count, int longest, boolean detected){
   }

   @Environment(EnvType.CLIENT)
   private record FlaggedChunk(
      int chunkX,
      int chunkZ,
      ChunkFinderModule.DetectionResult kelp,
      ChunkFinderModule.DetectionResult glowBerry,
      ChunkFinderModule.DetectionResult redMushroom,
      ChunkFinderModule.DetectionResult brownMushroom,
      ChunkFinderModule.DetectionResult fullHoney,
      ChunkFinderModule.DetectionResult buriedTunnel,
      ChunkFinderModule.DetectionResult rotatedDeepslate
   ){
      public ChunkFinderModule.DetectionResult getLastDetection(){
         return this.fullHoney;
      }
   }

   @Environment(EnvType.CLIENT)
   private record MushroomDetections(ChunkFinderModule.DetectionResult red, ChunkFinderModule.DetectionResult brown){
   }
}

