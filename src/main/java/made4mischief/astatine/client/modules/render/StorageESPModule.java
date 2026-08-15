package made4mischief.astatine.client.modules.render;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import net.minecraft.block.ChestBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.ChestType;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.render.RenderTickCounter;

@Environment(EnvType.CLIENT)
public final class StorageESPModule extends Module {
   private static final int FILL_ALPHA = 255;
   private static final double CHEST_INSET = 0.035;
   private static final double CHEST_BOTTOM_INSET = 0.025;
   private static final double CHEST_TOP_INSET = 0.965;
   private static final double BARREL_INSET = 0.006;
   private static final double SHULKER_INSET = 0.012;
   private static final Block[] STORAGE_BLOCKS = new Block[]{
      Blocks.CHEST,
      Blocks.TRAPPED_CHEST,
      Blocks.COPPER_CHEST,
      Blocks.EXPOSED_COPPER_CHEST,
      Blocks.WEATHERED_COPPER_CHEST,
      Blocks.OXIDIZED_COPPER_CHEST,
      Blocks.WAXED_COPPER_CHEST,
      Blocks.WAXED_EXPOSED_COPPER_CHEST,
      Blocks.WAXED_WEATHERED_COPPER_CHEST,
      Blocks.WAXED_OXIDIZED_COPPER_CHEST,
      Blocks.ENDER_CHEST,
      Blocks.SHULKER_BOX,
      Blocks.WHITE_SHULKER_BOX,
      Blocks.ORANGE_SHULKER_BOX,
      Blocks.MAGENTA_SHULKER_BOX,
      Blocks.LIGHT_BLUE_SHULKER_BOX,
      Blocks.YELLOW_SHULKER_BOX,
      Blocks.LIME_SHULKER_BOX,
      Blocks.PINK_SHULKER_BOX,
      Blocks.GRAY_SHULKER_BOX,
      Blocks.LIGHT_GRAY_SHULKER_BOX,
      Blocks.CYAN_SHULKER_BOX,
      Blocks.PURPLE_SHULKER_BOX,
      Blocks.BLUE_SHULKER_BOX,
      Blocks.BROWN_SHULKER_BOX,
      Blocks.GREEN_SHULKER_BOX,
      Blocks.RED_SHULKER_BOX,
      Blocks.BLACK_SHULKER_BOX,
      Blocks.BARREL,
      Blocks.HOPPER,
      Blocks.FURNACE,
      Blocks.BLAST_FURNACE,
      Blocks.SMOKER,
      Blocks.DISPENSER,
      Blocks.DROPPER,
      Blocks.CRAFTER,
      Blocks.BREWING_STAND,
      Blocks.DECORATED_POT,
      Blocks.CHISELED_BOOKSHELF,
      Blocks.ACACIA_SHELF,
      Blocks.BAMBOO_SHELF,
      Blocks.BIRCH_SHELF,
      Blocks.CHERRY_SHELF,
      Blocks.CRIMSON_SHELF,
      Blocks.DARK_OAK_SHELF,
      Blocks.JUNGLE_SHELF,
      Blocks.MANGROVE_SHELF,
      Blocks.OAK_SHELF,
      Blocks.PALE_OAK_SHELF,
      Blocks.SPRUCE_SHELF,
      Blocks.WARPED_SHELF
   };
   private static StorageESPModule instance;
   private final ModeSetting renderModeSetting = this.addMode("Render Mode", "Fill", new String[]{"Fill", "Both", "Outline"});
   private final NumberSetting rangeSetting = this.addNumber("Range", 256.0, 8.0, 256.0, 8.0);
   private final NumberSetting updateDelaySetting = this.addNumber("Update Delay", 5.0, 1.0, 20.0, 1.0);
   private final NumberSetting outlineAlphaSetting = this.addNumber("Outline Alpha", 230.0, 0.0, 255.0, 5.0);
   private final NumberSetting lineWidthSetting = this.addNumber("Line Width", 2.0, 1.0, 5.0, 0.5);
   private final BooleanSetting throughWallsSetting = this.addBoolean("Through Walls", true);
   private final BooleanSetting tracersSetting = this.addBoolean("Tracers", true);
   private final NumberSetting tracerWidthSetting = this.addNumber("Tracer Width", 0.75, 0.5, 2.0, 0.25);
   private final BlockTargetSetting blockFilterSetting = this.addSetting(new BlockTargetSetting("Storage Selector", Arrays.asList(STORAGE_BLOCKS), STORAGE_BLOCKS));
   private final ColorSetting chestSetting = this.addColor("Chest", -10163);
   private final ColorSetting shulkerSetting = this.addColor("Shulker", -12328849);
   private final ColorSetting enderChestSetting = this.addColor("Ender Chest", -4956929);
   private final ColorSetting barrelSetting = this.addColor("Barrel", -2647467);
   private final ColorSetting hopperSetting = this.addColor("Hopper", -7034440);
   private final ColorSetting furnaceSetting = this.addColor("Furnace", -4011311);
   private final ColorSetting blastFurnaceSetting = this.addColor("Blast Furnace", -9140842);
   private final ColorSetting smokerSetting = this.addColor("Smoker", -4884148);
   private final ColorSetting dispenserSetting = this.addColor("Dispenser", -11163649);
   private final ColorSetting dropperSetting = this.addColor("Dropper", -13184572);
   private final ColorSetting crafterSetting = this.addColor("Crafter", -22208);
   private final ColorSetting brewingStandSetting = this.addColor("Brewing Stand", -37930);
   private final ColorSetting decoratedPotSetting = this.addColor("Decorated Pot", -2525099);
   private final ColorSetting shelfSetting = this.addColor("Shelf", -2509972);
   private final List<StorageESPModule.StorageBo> storageBoxes = new ArrayList<>();
   private final ScreenTracerRenderer tracerRenderer = new ScreenTracerRenderer();
   private ClientWorld trackedWorld;
   private int updateTickTimer;

   public StorageESPModule(){
      super("StorageESP", Category.RENDER, "LÃ m ná»•i báº­t kho chá»©a Ä‘Ã£ chá»n.", -1, true);
      this.outlineAlphaSetting.visibleWhen(() -> !this.renderModeSetting.is("Fill"));
      this.lineWidthSetting.visibleWhen(() -> !this.renderModeSetting.is("Fill"));
      this.tracerWidthSetting.visibleWhen(this.tracersSetting::getValue);
      this.chestSetting.visibleWhen(() -> this.isTypeVisible(StorageESPModule.StorageType.CHEST));
      this.shulkerSetting.visibleWhen(() -> this.isTypeVisible(StorageESPModule.StorageType.SHULKER));
      this.enderChestSetting.visibleWhen(() -> this.isTypeVisible(StorageESPModule.StorageType.ENDER_CHEST));
      this.barrelSetting.visibleWhen(() -> this.isTypeVisible(StorageESPModule.StorageType.BARREL));
      this.hopperSetting.visibleWhen(() -> this.isTypeVisible(StorageESPModule.StorageType.HOPPER));
      this.furnaceSetting.visibleWhen(() -> this.isTypeVisible(StorageESPModule.StorageType.FURNACE));
      this.blastFurnaceSetting.visibleWhen(() -> this.isTypeVisible(StorageESPModule.StorageType.BLAST_FURNACE));
      this.smokerSetting.visibleWhen(() -> this.isTypeVisible(StorageESPModule.StorageType.SMOKER));
      this.dispenserSetting.visibleWhen(() -> this.isTypeVisible(StorageESPModule.StorageType.DISPENSER));
      this.dropperSetting.visibleWhen(() -> this.isTypeVisible(StorageESPModule.StorageType.DROPPER));
      this.crafterSetting.visibleWhen(() -> this.isTypeVisible(StorageESPModule.StorageType.CRAFTER));
      this.brewingStandSetting.visibleWhen(() -> this.isTypeVisible(StorageESPModule.StorageType.BREWING_STAND));
      this.decoratedPotSetting.visibleWhen(() -> this.isTypeVisible(StorageESPModule.StorageType.DECORATED_POT));
      this.shelfSetting.visibleWhen(() -> this.isTypeVisible(StorageESPModule.StorageType.SHELF));
      instance = this;
      WorldRenderEvents.END_MAIN.register(StorageESPModule::renderBoxes);
      HudElementRegistry.attachElementBefore(VanillaHudElements.CROSSHAIR, Astatine.id("storage_tracers"), StorageESPModule::renderCounts);
   }

   @Override
   protected void onEnable(){
      this.clearBoxes();
      this.updateTickTimer = 0;
   }

   @Override
   protected void onDisable(){
      this.clearBoxes();
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      if (client.player != null && client.world != null) {
         if (this.trackedWorld != client.world) {
            this.clearBoxes();
            this.trackedWorld = client.world;
            this.updateTickTimer = 0;
         }

         if (this.updateTickTimer-- <= 0) {
            this.scanWorld(client.world, client.player);
            this.updateTickTimer = this.updateDelaySetting.getValueInt() - 1;
         }
      } else {
         this.clearBoxes();
         this.updateTickTimer = 0;
      }
   }

   private void scanWorld(ClientWorld world, PlayerEntity player){
      this.storageBoxes.clear();
      double value2 = this.rangeSetting.getValue() * this.rangeSetting.getValue();
      int blockX = player.getBlockX() >> 4;
      int blockZ = player.getBlockZ() >> 4;
      int value = (int)Math.ceil(this.rangeSetting.getValue() / 16.0) + 1;

      for (int index2 = blockX - value; index2 <= blockX + value; index2++) {
         for (int index = blockZ - value; index <= blockZ + value; index++) {
            WorldChunk worldChunk = world.getChunkManager().getWorldChunk(index2, index, false);
            if (worldChunk != null) {
               for (BlockEntity blockEntity : worldChunk.getBlockEntities().values()) {
                  StorageESPModule.StorageBo var13 = this.createStorageBo(world, blockEntity);
                  if (var13 != null && var13.getDistanceToPlayer(player) <= value2) {
                     this.storageBoxes.add(var13);
                  }
               }
            }
         }
      }
   }

   private StorageESPModule.StorageBo createStorageBo(ClientWorld world, BlockEntity blockEntity){
      Block block = blockEntity.getCachedState().getBlock();
      if (!this.blockFilterSetting.isSelected(block)) {
         return null;
      } else {
         StorageESPModule.StorageType var4 = getStorageType(block);
         if (var4 == null) {
            return null;
         } else if (var4 == StorageESPModule.StorageType.CHEST && blockEntity instanceof ChestBlockEntity) {
            return this.createStorageBoxForBlock(world, blockEntity, block);
         } else if (var4 == StorageESPModule.StorageType.SHULKER) {
            return boxForShulker(blockEntity.getPos(), block);
         } else {
            return var4 == StorageESPModule.StorageType.ENDER_CHEST
               ? boxForChest(blockEntity.getPos(), block, var4)
               : boxForBarrel(blockEntity.getPos(), block, var4);
         }
      }
   }

   private StorageESPModule.StorageBo createStorageBoxForBlock(ClientWorld world, BlockEntity blockEntity, Block block){
      BlockPos pos2 = blockEntity.getPos();
      BlockState state = blockEntity.getCachedState();
      if (state.getBlock() instanceof ChestBlock && state.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE) {
         BlockPos pos = ChestBlock.getPosInFrontOf(pos2, state);
         BlockEntity blockEntity2 = world.getBlockEntity(pos);
         boolean block2 = blockEntity2 instanceof ChestBlockEntity
            && blockEntity2.getCachedState().getBlock() instanceof ChestBlock
            && this.blockFilterSetting.isSelected(blockEntity2.getCachedState().getBlock());
         if (!block2) {
            return boxForChest(pos2, block, StorageESPModule.StorageType.CHEST);
         } else if (pos2.asLong() > pos.asLong()) {
            return null;
         } else {
            int x = Math.min(pos2.getX(), pos.getX());
            int z2 = Math.min(pos2.getZ(), pos.getZ());
            int x2 = Math.max(pos2.getX(), pos.getX()) + 1;
            int z = Math.max(pos2.getZ(), pos.getZ()) + 1;
            return new StorageESPModule.StorageBo(
               x + 0.035,
               pos2.getY() + 0.025,
               z2 + 0.035,
               x2 - 0.035,
               pos2.getY() + 0.965,
               z - 0.035,
               block,
               StorageESPModule.StorageType.CHEST
            );
         }
      } else {
         return boxForChest(pos2, block, StorageESPModule.StorageType.CHEST);
      }
   }

   private static StorageESPModule.StorageBo boxForChest(BlockPos position, Block block, StorageESPModule.StorageType type){
      return new StorageESPModule.StorageBo(
         position.getX() + 0.035,
         position.getY() + 0.025,
         position.getZ() + 0.035,
         position.getX() + 1.0 - 0.035,
         position.getY() + 0.965,
         position.getZ() + 1.0 - 0.035,
         block,
         type
      );
   }

   private static StorageESPModule.StorageBo boxForBarrel(BlockPos position, Block block, StorageESPModule.StorageType type){
      return new StorageESPModule.StorageBo(
         position.getX() - 0.006,
         position.getY() - 0.006,
         position.getZ() - 0.006,
         position.getX() + 1.0 + 0.006,
         position.getY() + 1.0 + 0.006,
         position.getZ() + 1.0 + 0.006,
         block,
         type
      );
   }

   private static StorageESPModule.StorageBo boxForShulker(BlockPos position, Block block){
      return new StorageESPModule.StorageBo(
         position.getX() - 0.012,
         position.getY() - 0.012,
         position.getZ() - 0.012,
         position.getX() + 1.0 + 0.012,
         position.getY() + 1.0 + 0.012,
         position.getZ() + 1.0 + 0.012,
         block,
         StorageESPModule.StorageType.SHULKER
      );
   }

   private static void renderBoxes(WorldRenderContext context){
      StorageESPModule storageESPModule = instance;
      if (storageESPModule != null && storageESPModule.isEnabled() && !storageESPModule.storageBoxes.isEmpty()) {
         boolean is2 = !storageESPModule.renderModeSetting.is("Outline");
         boolean is = !storageESPModule.renderModeSetting.is("Fill");
         int valueInt = storageESPModule.outlineAlphaSetting.getValueInt();

         for (int index = 0; index < storageESPModule.storageBoxes.size(); index++) {
            StorageESPModule.StorageBo var6 = storageESPModule.storageBoxes.get(index);
            if (storageESPModule.blockFilterSetting.isSelected(var6.block)) {
               int typeColor = storageESPModule.getTypeColor(var6.type);
               RenderUtil.drawWorldBo(
                  context,
                  var6.minX,
                  var6.minY,
                  var6.minZ,
                  var6.maxX,
                  var6.maxY,
                  var6.maxZ,
                  ColorUtil.withAlpha(typeColor, 255),
                  ColorUtil.withAlpha(typeColor, valueInt),
                  is2,
                  is,
                  storageESPModule.throughWallsSetting.getValue(),
                  storageESPModule.lineWidthSetting.getValueFloat()
               );
            }
         }
      }
   }

   private static void renderCounts(DrawContext context, RenderTickCounter tickCounter){
      StorageESPModule storageESPModule = instance;
      MinecraftClient client = MinecraftClient.getInstance();
      if (storageESPModule != null && storageESPModule.isEnabled() && storageESPModule.tracersSetting.getValue() && !storageESPModule.storageBoxes.isEmpty() && client.player != null && client.world != null) {
         if (storageESPModule.tracerRenderer.begin(context, client)) {
            for (StorageESPModule.StorageBo storageBo : storageESPModule.storageBoxes) {
               if (storageESPModule.blockFilterSetting.isSelected(storageBo.block)) {
                  storageESPModule.tracerRenderer.draw(context, client, storageBo.center, storageESPModule.tracerWidthSetting.getValueFloat(), storageESPModule.getTypeColor(storageBo.type));
               }
            }
         }
      }
   }

   private boolean isTypeVisible(StorageESPModule.StorageType targetType){
      for (Block block : this.blockFilterSetting.getSelectedBlocks()) {
         if (getStorageType(block) == targetType) {
            return true;
         }
      }

      return false;
   }

   private int getTypeColor(StorageESPModule.StorageType type){
      return switch (type) {
         case CHEST -> this.chestSetting.getValue();
         case SHULKER -> this.shulkerSetting.getValue();
         case ENDER_CHEST -> this.enderChestSetting.getValue();
         case BARREL -> this.barrelSetting.getValue();
         case HOPPER -> this.hopperSetting.getValue();
         case FURNACE -> this.furnaceSetting.getValue();
         case BLAST_FURNACE -> this.blastFurnaceSetting.getValue();
         case SMOKER -> this.smokerSetting.getValue();
         case DISPENSER -> this.dispenserSetting.getValue();
         case DROPPER -> this.dropperSetting.getValue();
         case CRAFTER -> this.crafterSetting.getValue();
         case BREWING_STAND -> this.brewingStandSetting.getValue();
         case DECORATED_POT -> this.decoratedPotSetting.getValue();
         case SHELF -> this.shelfSetting.getValue();
      };
   }

   private static StorageESPModule.StorageType getStorageType(Block block){
      if (block instanceof ChestBlock) {
         return StorageESPModule.StorageType.CHEST;
      } else if (block instanceof ShulkerBoxBlock) {
         return StorageESPModule.StorageType.SHULKER;
      } else if (block == Blocks.ENDER_CHEST) {
         return StorageESPModule.StorageType.ENDER_CHEST;
      } else if (block == Blocks.BARREL) {
         return StorageESPModule.StorageType.BARREL;
      } else if (block == Blocks.HOPPER) {
         return StorageESPModule.StorageType.HOPPER;
      } else if (block == Blocks.FURNACE) {
         return StorageESPModule.StorageType.FURNACE;
      } else if (block == Blocks.BLAST_FURNACE) {
         return StorageESPModule.StorageType.BLAST_FURNACE;
      } else if (block == Blocks.SMOKER) {
         return StorageESPModule.StorageType.SMOKER;
      } else if (block == Blocks.DISPENSER) {
         return StorageESPModule.StorageType.DISPENSER;
      } else if (block == Blocks.DROPPER) {
         return StorageESPModule.StorageType.DROPPER;
      } else if (block == Blocks.CRAFTER) {
         return StorageESPModule.StorageType.CRAFTER;
      } else if (block == Blocks.BREWING_STAND) {
         return StorageESPModule.StorageType.BREWING_STAND;
      } else if (block == Blocks.DECORATED_POT) {
         return StorageESPModule.StorageType.DECORATED_POT;
      } else {
         return block != Blocks.CHISELED_BOOKSHELF
               && block != Blocks.ACACIA_SHELF
               && block != Blocks.BAMBOO_SHELF
               && block != Blocks.BIRCH_SHELF
               && block != Blocks.CHERRY_SHELF
               && block != Blocks.CRIMSON_SHELF
               && block != Blocks.DARK_OAK_SHELF
               && block != Blocks.JUNGLE_SHELF
               && block != Blocks.MANGROVE_SHELF
               && block != Blocks.OAK_SHELF
               && block != Blocks.PALE_OAK_SHELF
               && block != Blocks.SPRUCE_SHELF
               && block != Blocks.WARPED_SHELF
            ? null
            : StorageESPModule.StorageType.SHELF;
      }
   }

   private void clearBoxes(){
      this.storageBoxes.clear();
      this.trackedWorld = null;
   }

   @Environment(EnvType.CLIENT)
   private static final class StorageBo {
      private final double minX;
      private final double minY;
      private final double minZ;
      private final double maxX;
      private final double maxY;
      private final double maxZ;
      private final Block block;
      private final StorageESPModule.StorageType type;
      private final Vec3d center;

      private StorageBo(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Block block, StorageESPModule.StorageType type){
         this.minX = minX;
         this.minY = minY;
         this.minZ = minZ;
         this.maxX = maxX;
         this.maxY = maxY;
         this.maxZ = maxZ;
         this.block = block;
         this.type = type;
         this.center = new Vec3d((minX + maxX) * 0.5, (minY + maxY) * 0.5, (minZ + maxZ) * 0.5);
      }

      private double getDistanceToPlayer(PlayerEntity player){
         double x = player.getX() - this.center.x;
         double y = player.getY() - this.center.y;
         double z = player.getZ() - this.center.z;
         return x * x + y * y + z * z;
      }
   }

   @Environment(EnvType.CLIENT)
   private static enum StorageType {
      CHEST,
      SHULKER,
      ENDER_CHEST,
      BARREL,
      HOPPER,
      FURNACE,
      BLAST_FURNACE,
      SMOKER,
      DISPENSER,
      DROPPER,
      CRAFTER,
      BREWING_STAND,
      DECORATED_POT,
      SHELF;
   }
}

