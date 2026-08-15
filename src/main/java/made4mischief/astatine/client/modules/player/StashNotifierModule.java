package made4mischief.astatine.client.modules.player;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;
import made4mischief.astatine.client.hud.NotificationRenderer;
import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.BlockTargetSetting;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import made4mischief.astatine.client.utils.render.core.SoundUtil;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;

@Environment(EnvType.CLIENT)
public final class StashNotifierModule extends Module {
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
   private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.US);
   private final NumberSetting thresholdSetting = this.addNumber("Threshold", 20.0, 1.0, 100.0, 1.0);
   private final NumberSetting rangeSetting = this.addNumber("Range", 128.0, 8.0, 128.0, 8.0);
   private final NumberSetting scanIntervalSetting = this.addNumber("Scan Interval", 20.0, 5.0, 200.0, 5.0);
   private final BooleanSetting chatAlertSetting = this.addBoolean("Chat Alert", true);
   private final BooleanSetting playSoundSetting = this.addBoolean("Play Sound", true);
   private final BlockTargetSetting storageSelectorSetting = this.addSetting(new BlockTargetSetting("Storage Selector", Arrays.asList(STORAGE_BLOCKS), STORAGE_BLOCKS));
   private int scanTicks;
   private ClientWorld world;
   private boolean notified;

   public StashNotifierModule(){
      super("StashNotifier", Category.PLAYER, "BÃ¡o khi cÃ³ nhiá»u kho chá»©a gáº§n báº¡n.");
   }

   @Override
   protected void onEnable(){
      this.scanTicks = 0;
      this.world = null;
      this.notified = false;
   }

   @Override
   protected void onDisable(){
      this.world = null;
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      if (client.player != null && client.world != null) {
         if (this.world != client.world) {
            this.world = client.world;
            this.scanTicks = 0;
            this.notified = false;
         }

         if (this.scanTicks-- <= 0) {
            int countStorageBlocks = this.countStorageBlocks(client.world, client.player);
            if (countStorageBlocks >= this.thresholdSetting.getValueInt()) {
               if (!this.notified) {
                  String format = NUMBER_FORMAT.format((long)countStorageBlocks);
                  NotificationRenderer.showAlert("Stash Alert", format + " storage detected", NotificationRenderer.NotificationType.STASH);
                  if (this.playSoundSetting.getValue()) {
                     SoundUtil.playNotification();
                  }

                  if (this.chatAlertSetting.getValue()) {
                     String blockZ = "Â§8[Â§aStashNotifierÂ§8] Â§fDetected Â§e"
                        + format
                        + " storage blocks Â§fnearby at Â§aX: "
                        + client.player.getBlockX()
                        + ", Y: "
                        + client.player.getBlockY()
                        + ", Z: "
                        + client.player.getBlockZ();
                     client.player.sendMessage(Text.literal(blockZ), false);
                  }

                  this.notified = true;
               }
            } else {
               this.notified = false;
            }

            this.scanTicks = this.scanIntervalSetting.getValueInt() - 1;
         }
      } else {
         this.world = null;
      }
   }

   private int countStorageBlocks(ClientWorld world, PlayerEntity player){
      int index3 = 0;
      double value2 = this.rangeSetting.getValue() * this.rangeSetting.getValue();
      int blockX = player.getBlockX() >> 4;
      int blockZ = player.getBlockZ() >> 4;
      int value = (int)Math.ceil(this.rangeSetting.getValue() / 16.0) + 1;

      for (int index = blockX - value; index <= blockX + value; index++) {
         for (int index2 = blockZ - value; index2 <= blockZ + value; index2++) {
            WorldChunk worldChunk = world.getChunkManager().getWorldChunk(index, index2, false);
            if (worldChunk != null) {
               for (BlockEntity blockEntity : worldChunk.getBlockEntities().values()) {
                  BlockPos pos = blockEntity.getPos();
                  double x = player.getX() - pos.getX();
                  double z = player.getZ() - pos.getZ();
                  if (!(x * x + z * z > value2) && this.storageSelectorSetting.isSelected(blockEntity.getCachedState().getBlock())) {
                     index3++;
                  }
               }
            }
         }
      }

      return index3;
   }
}

