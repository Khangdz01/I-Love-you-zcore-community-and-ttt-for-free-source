package made4mischief.astatine.client.modules.smp;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.ActionSetting;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.ModeSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import made4mischief.astatine.client.utils.camera.DetachedCameraEntity;
import made4mischief.astatine.client.utils.render.RenderUtil;
import made4mischief.astatine.client.utils.render.animation.Animation;
import made4mischief.astatine.client.utils.render.animation.AnimationType;
import made4mischief.astatine.client.utils.rotation.RotationUtil;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.util.math.Direction.Axis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class BaseDiggerModule extends Module {
   private static final Logger LOGGER = LoggerFactory.getLogger("astatine/base-digger");
   private static final long ANIMATION_DURATION = 650L;
   private static final int BOX_COLOR = 808900863;
   private static final int BOX_COLOR_ACTIVE = -531635713;
   private static final int THREE_BY_THREE_SIZE = 3;
   private static final int RESTOCK_INTERVAL = 200;
   private static final int PHASE_IDLE = 0;
   private static final int PHASE_ACTIVE = 1;
   private static BaseDiggerModule instance;
   private final ModeSetting pickaxeSetting = this.addMode("Pickaxe", "1x1", new String[]{"1x1", "3x3"});
   private final NumberSetting sizeX = this.addNumber("Size X", 9.0, 1.0, 300.0, 1.0);
   private final NumberSetting sizeYSetting = this.addNumber("Size Y", 6.0, 1.0, 64.0, 1.0);
   private final NumberSetting sizeZ = this.addNumber("Size Z", 9.0, 1.0, 300.0, 1.0);
   private final ActionSetting resetPositionSetting = this.addAction("Reset Position", "Reset", this::refreshAnchor);
   private final BooleanSetting autoOrbitSetting = this.addBoolean("Auto Orbit", true);
   private final NumberSetting orbitSpeedSetting = this.addNumber("Orbit Speed", 22.0, 2.0, 90.0, 1.0);
   private final NumberSetting cameraDistanceSetting = this.addNumber("Camera Distance", 14.0, 4.0, 64.0, 0.5);
   private final BooleanSetting renderBoxSetting = this.addBoolean("Render Box", true);
   private final Animation boxAnimation = new Animation(0.0F, 1.0F, 650L, AnimationType.EASE_OUT);
   private BlockPos anchor;
   private ClientWorld worldRef;
   private boolean initialized;
   private DetachedCameraEntity cameraEntity;
   private ClientWorld lastWorld;
   private Entity lastPlayer;
   private Perspective targetPath;
   private boolean orbitActive;
   private boolean digging;
   private double orbitAngle;
   private long lastOrbitNanos;
   private Boolean prevAllowBreak;
   private Boolean prevAutoTool;
   private Boolean prevAllowParkour;
   private BlockPos targetPos;
   private Direction inwardDirection;
   private BaseDiggerModule.ExcavationBounds bounds;
   private List<BaseDiggerModule.ExcavationLayer> layers = List.of();
   private List<BlockPos> digQueue = List.of();
   private List<BaseDiggerModule.TunnelStep> tunnelSteps = List.of();
   private BlockPos stagingPos;
   private List<BlockPos> layerQueue = List.of();
   private BaseDiggerModule.ThreeByThreePhase threeByThreePhase = BaseDiggerModule.ThreeByThreePhase.IDLE;
   private long lastActionNanos;
   private int digQueueIndex;
   private int tunnelIndex;
   private int layerIndex;
   private int layerQueueIndex;
   private BlockPos currentDigPos;
   private int retryCount;
   private int restockCheckTick;

   public BaseDiggerModule(){
      super("BaseDigger", Category.SMP, "Xem truoc vung roi dung Baritone de dao.");
      instance = this;
      this.orbitSpeedSetting.visibleWhen(this.autoOrbitSetting::getValue);
      WorldRenderEvents.END_MAIN.register(BaseDiggerModule::renderWireframes);
   }

   @Override
   protected void onEnable(){
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null && client.world != null) {
         if (this.anchor == null || this.worldRef != client.world) {
            this.setAnchor(client);
         }

         this.initialized = true;
         this.tickExcavation();
      } else {
         this.disable();
      }
   }

   @Override
   protected void onDisable(){
      this.stopAndReset();
   }

   public static void beginEditorPreview(){
      BaseDiggerModule baseDiggerModule = instance;
      MinecraftClient client = MinecraftClient.getInstance();
      if (baseDiggerModule != null && client.player != null && client.world != null) {
         if (baseDiggerModule.anchor == null || baseDiggerModule.worldRef != client.world) {
            baseDiggerModule.setAnchor(client);
         }

         baseDiggerModule.initialized = true;
         baseDiggerModule.digging = false;
         if (baseDiggerModule.cameraEntity != null && baseDiggerModule.lastWorld == client.world) {
            client.options.setPerspective(Perspective.FIRST_PERSON);
            client.setCameraEntity(baseDiggerModule.cameraEntity);
         } else {
            baseDiggerModule.tickWorldSync();
            baseDiggerModule.lastPlayer = client.getCameraEntity();
            baseDiggerModule.targetPath = client.options.getPerspective();
            baseDiggerModule.lastWorld = client.world;
            baseDiggerModule.cameraEntity = new DetachedCameraEntity(client.world, client.player.getGameProfile());
            baseDiggerModule.cameraEntity.copyPositionAndRotation(client.player);
            baseDiggerModule.cameraEntity.setHeadYaw(client.player.getHeadYaw());
            client.world.addEntity(baseDiggerModule.cameraEntity);
            client.options.setPerspective(Perspective.FIRST_PERSON);
            client.setCameraEntity(baseDiggerModule.cameraEntity);
            baseDiggerModule.boxAnimation.snapTo(0.0F);
         }

         baseDiggerModule.orbitAngle = client.player.getYaw() + 135.0;
         baseDiggerModule.lastOrbitNanos = System.nanoTime();
         baseDiggerModule.orbitActive = true;
         baseDiggerModule.boxAnimation.setTarget(1.0F);
      }
   }

   public static void endEditorPreview(){
      BaseDiggerModule baseDiggerModule = instance;
      if (baseDiggerModule != null && baseDiggerModule.orbitActive) {
         baseDiggerModule.digging = true;
         baseDiggerModule.boxAnimation.setTarget(0.0F);
         if (baseDiggerModule.isEnabled()) {
            baseDiggerModule.tickExcavation();
         }
      }
   }

   public static void updateEditorCamera(){
      BaseDiggerModule baseDiggerModule = instance;
      if (baseDiggerModule != null && baseDiggerModule.orbitActive) {
         baseDiggerModule.tickPathing();
      }
   }

   public static void forceCloseEditorCamera(){
      BaseDiggerModule baseDiggerModule = instance;
      if (baseDiggerModule != null) {
         baseDiggerModule.tickWorldSync();
      }
   }

   public static boolean shouldRenderLocalBody(){
      BaseDiggerModule baseDiggerModule = instance;
      MinecraftClient client = MinecraftClient.getInstance();
      return baseDiggerModule != null && baseDiggerModule.orbitActive && baseDiggerModule.cameraEntity != null && client.player != null && client.getCameraEntity() == baseDiggerModule.cameraEntity;
   }

   private void setAnchor(MinecraftClient client){
      this.anchor = client.player.getBlockPos().toImmutable();
      this.worldRef = client.world;
   }

   private void refreshAnchor(){
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null && client.world != null) {
         this.setAnchor(client);
         this.initialized = true;
         if (this.isEnabled()) {
            this.tickExcavation();
         }
      }
   }

   private void tickExcavation(){
      MinecraftClient client = MinecraftClient.getInstance();
      BaseDiggerModule.ExcavationBounds var2 = this.getBounds();
      if (client.player != null && client.world != null && var2 != null && this.worldRef == client.world) {
         if ("3x3".equals(this.pickaxeSetting.getValue())) {
            this.stopAndReset();
            this.startExcavation(var2, this.collectBlocksToDig(client.world, var2));
         } else {
            try {
               this.stopAndReset();
               BaritoneHelper.cancelEverything();
               this.prevAllowBreak = BaritoneHelper.getSettingBool("allowBreak");
               BaritoneHelper.setSettingBool("allowBreak", true);
               BaritoneHelper.clearArea(var2.min(), var2.max());
               LOGGER.info(
                  "Started BaseDigger {} mode with 1x1 clear-area behavior from {} to {}", new Object[]{this.pickaxeSetting.getValue(), var2.min(), var2.max()}
               );
            } catch (Exception e) {
               LOGGER.error("Unable to start Baritone BaseDigger process", e);
               if (this.isEnabled()) {
                  this.disable();
               }
            }
         }
      }
   }

   private void startExcavation(BaseDiggerModule.ExcavationBounds bounds, List<BaseDiggerModule.ExcavationBlock> blocksToDig){
      this.bounds = bounds;
      this.layers = this.buildLayers(bounds, blocksToDig);
      this.layerIndex = 0;
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null) {
         this.anchor = this.findCenterStaging(client.player.getBlockPos(), bounds);
         this.inwardDirection = this.getInwardDirection(this.anchor, bounds);
         this.digQueue = this.getLayerBlockPositions(this.anchor, this.inwardDirection, bounds);
         this.tunnelSteps = List.of();
         this.digQueueIndex = 0;
         this.tunnelIndex = 0;
         this.currentDigPos = null;
         this.retryCount = 0;
         this.threeByThreePhase = BaseDiggerModule.ThreeByThreePhase.MOVING_TO_STAGING;
         if (!this.isPickaxeEffective(client)) {
            client.player.sendMessage(Text.literal("Hotbar slot 2 phai chua cup thuong."), false);
            LOGGER.warn("Hotbar slot 2 does not contain a normal pickaxe");
            this.disable();
         } else {
            try {
               this.prevAutoTool = BaritoneHelper.getSettingBool("autoTool");
               this.prevAllowParkour = BaritoneHelper.getSettingBool("allowInventory");
               BaritoneHelper.setSettingBool("autoTool", false);
               BaritoneHelper.setSettingBool("allowInventory", false);
               BaritoneHelper.setGoalAndPath(this.anchor);
               LOGGER.info("Moving to BaseDigger 3x3 staging position {} outside the top Box edge", this.anchor);
            } catch (Exception e) {
               LOGGER.error("Unable to move to the BaseDigger 3x3 staging position", e);
               if (this.isEnabled()) {
                  this.disable();
               }
            }
         }
      }
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      if ("3x3".equals(this.pickaxeSetting.getValue())
         && this.threeByThreePhase != BaseDiggerModule.ThreeByThreePhase.IDLE
         && client.player != null
         && client.world != null
         && client.interactionManager != null) {
         switch (this.threeByThreePhase) {
            case IDLE:
            case COMPLETE:
            default:
               break;
            case MOVING_TO_STAGING:
               this.checkHotbarRestock(client);
               break;
            case THINKING:
               this.digNextBlock(client);
               break;
            case MINING_FIRST_TUNNEL:
               this.findNextAirBlock(client);
               break;
            case MOVING_INTO_FIRST_VOLUME:
               this.checkAdjacentTarget(client);
               break;
            case MINING_FIRST_LAYER:
               this.progressTunnels(client);
               break;
            case CLEANING_FIRST_LAYER:
               this.checkToolRestock(client);
               break;
            case MOVING_TO_DESCENT_CORNER:
               this.tickStaging(client);
               break;
            case MINING_DOWNWARD_SHAFT:
               this.digLayerQueue(client);
         }
      }
   }

   private void checkHotbarRestock(MinecraftClient client){
      if (this.anchor != null) {
         if (!this.isPickaxeEffective(client)) {
            client.player.sendMessage(Text.literal("Hotbar slot 2 khong con chua cup thuong."), false);
            LOGGER.warn("The normal pickaxe is no longer in hotbar slot 2");
            this.disable();
         } else if (client.player.getBlockPos().equals(this.anchor)) {
            BaritoneHelper.cancelEverything();
            BlockPos pos = this.digQueue.isEmpty() ? this.anchor.offset(this.inwardDirection) : this.digQueue.getFirst();
            this.mineBlock(client, pos);
            client.player.sendMessage(Text.literal("Thinking..."), false);
            long nextLong = ThreadLocalRandom.current().nextLong(1L, 2001L);
            this.lastActionNanos = System.nanoTime() + nextLong * 1000000L;
            this.threeByThreePhase = BaseDiggerModule.ThreeByThreePhase.THINKING;
            LOGGER.info("BaseDigger 3x3 thinking for {} ms", nextLong);
         }
      }
   }

   private void digNextBlock(MinecraftClient client){
      if (!this.digQueue.isEmpty()) {
         this.mineBlock(client, this.digQueue.getFirst());
      }

      if (System.nanoTime() >= this.lastActionNanos) {
         if (!this.isLayerComplete()) {
            this.stopDigging(client);
         } else if (this.digQueue.isEmpty()) {
            this.stopDigging(client);
         } else {
            this.selectToolSlot(client);
            this.threeByThreePhase = BaseDiggerModule.ThreeByThreePhase.MINING_FIRST_TUNNEL;
         }
      }
   }

   private boolean isLayerComplete(){
      if (this.bounds != null && !this.layers.isEmpty()) {
         BaseDiggerModule.ExcavationLayer var1 = this.layers.get(this.layerIndex);
         int x = this.bounds.max().getX() - this.bounds.min().getX() + 1;
         int z = this.bounds.max().getZ() - this.bounds.min().getZ() + 1;
         return var1.topY() - var1.bottomY() + 1 == 3 && x >= 3 && z >= 3;
      } else {
         return false;
      }
   }

   private void findNextAirBlock(MinecraftClient client){
      while (this.currentDigPos == null && this.digQueueIndex < this.digQueue.size()) {
         BlockPos pos2 = this.digQueue.get(this.digQueueIndex);
         if (client.world.getBlockState(pos2).isAir()) {
            this.digQueueIndex++;
         } else {
            this.currentDigPos = pos2;
            this.retryCount = 0;
         }
      }

      if (this.currentDigPos == null) {
         client.interactionManager.cancelBlockBreaking();
         if (this.digQueue.isEmpty()) {
            this.stopDigging(client);
         } else {
            int size = Math.min(2, this.digQueue.size());
            BlockPos pos = this.anchor.offset(this.inwardDirection, size);
            this.tunnelSteps = this.getTunnelSteps(pos);
            this.tunnelIndex = 0;
            this.threeByThreePhase = BaseDiggerModule.ThreeByThreePhase.MOVING_INTO_FIRST_VOLUME;
            LOGGER.info("BaseDigger finished the first three-block 3x3 tunnel");
         }
      } else {
         this.mineBlock(client, this.currentDigPos);
         if (client.world.getBlockState(this.currentDigPos).isAir()) {
            client.interactionManager.cancelBlockBreaking();
            this.digQueueIndex++;
            this.currentDigPos = null;
            this.retryCount = 0;
         } else {
            if (this.retryCount == 0) {
               client.interactionManager.attackBlock(this.currentDigPos, this.inwardDirection.getOpposite());
            } else {
               client.interactionManager.updateBlockBreakingProgress(this.currentDigPos, this.inwardDirection.getOpposite());
            }

            client.player.swingHand(Hand.MAIN_HAND);
            this.retryCount++;
            if (this.retryCount >= 200) {
               LOGGER.info("3x3 pickaxe could not finish {}; switching to normal-pickaxe cleanup", this.currentDigPos);
               this.stopDigging(client);
            }
         }
      }
   }

   private void checkAdjacentTarget(MinecraftClient client){
      int size = Math.min(2, this.digQueue.size());
      BlockPos pos = this.anchor.offset(this.inwardDirection, size);
      if (this.isAtPosition(client, pos)) {
         this.tunnelIndex = 0;
         this.threeByThreePhase = BaseDiggerModule.ThreeByThreePhase.MINING_FIRST_LAYER;
         LOGGER.info("Entered the center of the first 3x3x3 excavation volume at {}", pos);
      }
   }

   private void progressTunnels(MinecraftClient client){
      if (this.tunnelIndex >= this.tunnelSteps.size()) {
         this.stopDigging(client);
      } else {
         BaseDiggerModule.TunnelStep var2 = this.tunnelSteps.get(this.tunnelIndex);
         BlockPos pos = var2.feetPosition().up();
         if (client.player.getBlockPos().equals(var2.feetPosition())) {
            this.cancelBaritone();
            client.interactionManager.cancelBlockBreaking();
            this.currentDigPos = null;
            this.retryCount = 0;
            this.tunnelIndex++;
         } else {
            this.mineBlock(client, pos);
            if (var2.mineBeforeMove() && !client.world.getBlockState(pos).isAir()) {
               this.cancelBaritone();
               this.selectToolSlot(client);
               if (!pos.equals(this.currentDigPos)) {
                  client.interactionManager.cancelBlockBreaking();
                  this.currentDigPos = pos;
                  this.retryCount = 0;
               }

               if (this.retryCount == 0) {
                  client.interactionManager.attackBlock(pos, var2.direction().getOpposite());
               } else {
                  client.interactionManager.updateBlockBreakingProgress(pos, var2.direction().getOpposite());
               }

               client.player.swingHand(Hand.MAIN_HAND);
               this.retryCount++;
               if (this.retryCount >= 200) {
                  LOGGER.info("3x3 pickaxe left an unfinished center at {}; starting cleanup", pos);
                  this.stopDigging(client);
               }
            } else {
               if (this.currentDigPos != null) {
                  client.interactionManager.cancelBlockBreaking();
                  this.currentDigPos = null;
                  this.retryCount = 0;
               }

               this.isAtPosition(client, var2.feetPosition());
            }
         }
      }
   }

   private boolean isAtPosition(MinecraftClient client, BlockPos feetPosition){
      if (client.player.getBlockPos().equals(feetPosition)) {
         this.cancelBaritone();
         return true;
      } else if (!this.isPickaxeEffective(client)) {
         client.player.sendMessage(Text.literal("Hotbar slot 2 phai chua cup thuong de Baritone di chuyen."), false);
         this.disable();
         return false;
      } else {
         if (!feetPosition.equals(this.targetPos)) {
            try {
               BaritoneHelper.cancelEverything();
               BaritoneHelper.setGoalAndPath(feetPosition);
               this.targetPos = feetPosition.toImmutable();
            } catch (Exception e) {
               LOGGER.error("Unable to path to excavation waypoint {}", feetPosition, e);
               this.disable();
            }
         }

         return false;
      }
   }

   private void cancelBaritone(){
      if (this.targetPos != null) {
         try {
            BaritoneHelper.cancelEverything();
         } catch (Exception e) {
         }

         this.targetPos = null;
      }
   }

   private void stopDigging(MinecraftClient client){
      this.cancelBaritone();
      client.interactionManager.cancelBlockBreaking();
      this.currentDigPos = null;
      this.retryCount = 0;
      if (this.bounds == null || this.layers.isEmpty()) {
         this.disable();
      } else if (!this.isPickaxeEffective(client)) {
         client.player.sendMessage(Text.literal("Hotbar slot 2 phai chua cup thuong de don block du."), false);
         this.disable();
      } else {
         BaseDiggerModule.ExcavationLayer var2 = this.layers.get(this.layerIndex);
         BlockPos pos2 = new BlockPos(this.bounds.min().getX(), var2.bottomY(), this.bounds.min().getZ());
         BlockPos pos = new BlockPos(this.bounds.max().getX(), var2.topY(), this.bounds.max().getZ());

         try {
            BaritoneHelper.cancelEverything();
            if (this.prevAllowBreak == null) {
               this.prevAllowBreak = BaritoneHelper.getSettingBool("allowBreak");
            }

            BaritoneHelper.setSettingBool("allowBreak", true);
            BaritoneHelper.clearArea(pos2, pos);
            this.restockCheckTick = 0;
            this.threeByThreePhase = BaseDiggerModule.ThreeByThreePhase.CLEANING_FIRST_LAYER;
            LOGGER.info("Cleaning remaining first-layer blocks from {} to {} with the normal pickaxe", pos2, pos);
         } catch (Exception e) {
            LOGGER.error("Unable to start first-layer cleanup", e);
            this.disable();
         }
      }
   }

   private void checkToolRestock(MinecraftClient client){
      this.restockCheckTick++;
      if (!this.isPickaxeEffective(client)) {
         client.player.sendMessage(Text.literal("Cup thuong khong con o hotbar slot 2."), false);
         this.disable();
      } else {
         boolean active;
         try {
            active = BaritoneHelper.isBuilderActive();
         } catch (Exception e) {
            LOGGER.error("Unable to inspect first-layer cleanup", e);
            this.disable();
            return;
         }

         if (this.restockCheckTick > 1 && !active) {
            this.restoreAllowBreak();
            client.player.sendMessage(Text.literal("BaseDigger: da clear tang " + (this.layerIndex + 1) + "."), false);
            LOGGER.info("BaseDigger completed excavation layer {}", this.layerIndex + 1);
            if (this.layerIndex + 1 < this.layers.size()) {
               this.layerIndex++;
               this.resetDigState(client);
            } else {
               this.restoreAutoTool();
               this.threeByThreePhase = BaseDiggerModule.ThreeByThreePhase.COMPLETE;
               client.player.sendMessage(Text.literal("BaseDigger: da clear toan bo Box."), false);
            }
         }
      }
   }

   private void resetDigState(MinecraftClient client){
      this.currentDigPos = null;
      this.retryCount = 0;
      this.tunnelSteps = List.of();
      this.tunnelIndex = 0;
      this.layerQueue = List.of();
      this.layerQueueIndex = 0;
      if (!this.isLayerComplete()) {
         this.stopDigging(client);
      } else {
         BaseDiggerModule.ExcavationLayer var2 = this.layers.get(this.layerIndex - 1);
         this.stagingPos = this.findStagingPosition(client.player.getBlockPos(), var2.bottomY());
         this.inwardDirection = this.findInwardDirection(this.stagingPos);
         BaseDiggerModule.ExcavationLayer var3 = this.layers.get(this.layerIndex);
         ArrayList var4 = new ArrayList(3);

         for (int index = var3.topY(); index >= var3.bottomY(); index--) {
            var4.add(new BlockPos(this.stagingPos.getX(), index, this.stagingPos.getZ()));
         }

         this.layerQueue = List.copyOf(var4);
         this.threeByThreePhase = BaseDiggerModule.ThreeByThreePhase.MOVING_TO_DESCENT_CORNER;
         LOGGER.info("Preparing layer {} from corner {} with downward targets {}", new Object[]{this.layerIndex + 1, this.stagingPos, this.layerQueue});
      }
   }

   private BlockPos findStagingPosition(BlockPos playerPosition, int standingY){
      int x2 = this.bounds.min().getX() + 1;
      int x = this.bounds.max().getX() - 1;
      int z2 = this.bounds.min().getZ() + 1;
      int z = this.bounds.max().getZ() - 1;
      List list = List.of(
         new BlockPos(x2, standingY, z2),
         new BlockPos(x, standingY, z2),
         new BlockPos(x, standingY, z),
         new BlockPos(x2, standingY, z)
      );
      BlockPos pos2 = (BlockPos)list.getFirst();
      double squaredDistance = pos2.getSquaredDistance(playerPosition);

      for (int index = 1; index < list.size(); index++) {
         BlockPos pos = (BlockPos)list.get(index);
         double squaredDistance2 = pos.getSquaredDistance(playerPosition);
         if (squaredDistance2 < squaredDistance) {
            pos2 = pos;
            squaredDistance = squaredDistance2;
         }
      }

      return pos2;
   }

   private Direction findInwardDirection(BlockPos corner){
      int x2 = this.bounds.min().getX() + 1;
      int x = this.bounds.max().getX() - 1;
      int z2 = this.bounds.min().getZ() + 1;
      int z = this.bounds.max().getZ() - 1;
      if (corner.getX() == x2 && corner.getZ() == z2) {
         return Direction.EAST;
      } else if (corner.getX() == x && corner.getZ() == z2) {
         return Direction.SOUTH;
      } else {
         return corner.getX() == x && corner.getZ() == z ? Direction.WEST : Direction.NORTH;
      }
   }

   private void tickStaging(MinecraftClient client){
      if (this.isAtPosition(client, this.stagingPos)) {
         this.layerQueueIndex = 0;
         this.currentDigPos = null;
         this.retryCount = 0;
         this.threeByThreePhase = BaseDiggerModule.ThreeByThreePhase.MINING_DOWNWARD_SHAFT;
      }
   }

   private void digLayerQueue(MinecraftClient client){
      if (this.layerQueueIndex >= this.layerQueue.size()) {
         BaseDiggerModule.ExcavationLayer var4 = this.layers.get(this.layerIndex);
         BlockPos pos = new BlockPos(this.stagingPos.getX(), var4.bottomY(), this.stagingPos.getZ());
         this.tunnelSteps = this.getTunnelSteps(pos);
         this.tunnelIndex = 0;
         this.currentDigPos = null;
         this.retryCount = 0;
         this.threeByThreePhase = BaseDiggerModule.ThreeByThreePhase.MINING_FIRST_LAYER;
         LOGGER.info("Entered layer {} through the 3x3 downward shaft", this.layerIndex + 1);
      } else {
         BlockPos pos2 = this.layerQueue.get(this.layerQueueIndex);
         if (client.player.getBlockPos().equals(pos2)) {
            this.cancelBaritone();
            client.interactionManager.cancelBlockBreaking();
            this.layerQueueIndex++;
            this.currentDigPos = null;
            this.retryCount = 0;
         } else if (client.world.getBlockState(pos2).isAir()) {
            if (this.currentDigPos != null) {
               client.interactionManager.cancelBlockBreaking();
               this.currentDigPos = null;
               this.retryCount = 0;
            }

            this.isAtPosition(client, pos2);
         } else {
            this.cancelBaritone();
            this.selectToolSlot(client);
            this.mineBlock(client, pos2);
            if (!pos2.equals(this.currentDigPos)) {
               client.interactionManager.cancelBlockBreaking();
               this.currentDigPos = pos2;
               this.retryCount = 0;
            }

            if (this.retryCount == 0) {
               client.interactionManager.attackBlock(pos2, Direction.UP);
            } else {
               client.interactionManager.updateBlockBreakingProgress(pos2, Direction.UP);
            }

            client.player.swingHand(Hand.MAIN_HAND);
            this.retryCount++;
            if (this.retryCount >= 200) {
               LOGGER.info("Downward 3x3 mining stalled at {}; starting normal cleanup", pos2);
               this.stopDigging(client);
            }
         }
      }
   }

   private BlockPos findCenterStaging(BlockPos playerPosition, BaseDiggerModule.ExcavationBounds bounds){
      BlockPos pos4 = bounds.min();
      BlockPos pos3 = bounds.max();
      int y = Math.max(pos4.getY(), pos3.getY() - 2);
      int x3 = pos3.getX() - pos4.getX() + 1;
      int z = pos3.getZ() - pos4.getZ() + 1;
      int x2 = pos4.getX() + (x3 >= 3 ? 1 : 0);
      int x = pos3.getX() - (x3 >= 3 ? 1 : 0);
      int z2 = pos4.getZ() + (z >= 3 ? 1 : 0);
      int z3 = pos3.getZ() - (z >= 3 ? 1 : 0);
      List list = List.of(
         new BlockPos(pos4.getX() - 1, y, z2),
         new BlockPos(pos3.getX() + 1, y, z3),
         new BlockPos(x, y, pos4.getZ() - 1),
         new BlockPos(x2, y, pos3.getZ() + 1)
      );
      BlockPos pos2 = (BlockPos)list.getFirst();
      double squaredDistance2 = pos2.getSquaredDistance(playerPosition);

      for (int index = 1; index < list.size(); index++) {
         BlockPos pos = (BlockPos)list.get(index);
         double squaredDistance = pos.getSquaredDistance(playerPosition);
         if (squaredDistance < squaredDistance2) {
            pos2 = pos;
            squaredDistance2 = squaredDistance;
         }
      }

      return pos2;
   }

   private Direction getInwardDirection(BlockPos stagingPosition, BaseDiggerModule.ExcavationBounds bounds){
      if (stagingPosition.getX() < bounds.min().getX()) {
         return Direction.EAST;
      } else if (stagingPosition.getX() > bounds.max().getX()) {
         return Direction.WEST;
      } else {
         return stagingPosition.getZ() < bounds.min().getZ() ? Direction.SOUTH : Direction.NORTH;
      }
   }

   private List<BlockPos> getLayerBlockPositions(BlockPos stagingPosition, Direction inwardDirection, BaseDiggerModule.ExcavationBounds bounds){
      int y = Math.min(bounds.max().getY(), stagingPosition.getY() + 1);
      BlockPos pos2 = new BlockPos(stagingPosition.getX(), y, stagingPosition.getZ()).offset(inwardDirection);
      ArrayList var6 = new ArrayList(3);

      for (int index = 0; index < 3; index++) {
         BlockPos pos = pos2.offset(inwardDirection, index);
         if (this.isWithinBounds(pos, bounds)) {
            var6.add(pos);
         }
      }

      return List.copyOf(var6);
   }

   private List<BaseDiggerModule.TunnelStep> getTunnelSteps(BlockPos volumeCenter){
      if (this.bounds != null && !this.layers.isEmpty()) {
         BaseDiggerModule.ExcavationLayer var2 = this.layers.get(this.layerIndex);
         int x = this.bounds.max().getX() - this.bounds.min().getX() + 1;
         int z = this.bounds.max().getZ() - this.bounds.min().getZ() + 1;
         if (var2.topY() - var2.bottomY() + 1 >= 3 && x >= 3 && z >= 3) {
            Direction direction = this.getOppositeDirection(this.inwardDirection);
            int layerStartCoord = this.getLayerStartCoord(this.inwardDirection, this.bounds);
            ArrayList var7 = new ArrayList();
            BlockPos pos = volumeCenter;
            boolean var9 = true;

            while (true) {
               Direction direction2 = var9 ? direction : direction.getOpposite();
               int layerEndCoord = this.getLayerEndCoord(direction2, this.bounds);

               while (this.getCoordAlongAxis(pos, direction2) != layerEndCoord) {
                  pos = pos.offset(direction2);
                  var7.add(new BaseDiggerModule.TunnelStep(pos, direction2, true));
               }

               pos = pos.offset(direction2.getOpposite());
               var7.add(new BaseDiggerModule.TunnelStep(pos, direction2.getOpposite(), false));
               int coordAlongAxis = Math.abs(this.getCoordAlongAxis(pos, this.inwardDirection) - layerStartCoord);
               if (coordAlongAxis == 0) {
                  return List.copyOf(var7);
               }

               int min = Math.min(3, coordAlongAxis);

               for (int index = 0; index < min + 1; index++) {
                  pos = pos.offset(this.inwardDirection);
                  var7.add(new BaseDiggerModule.TunnelStep(pos, this.inwardDirection, true));
               }

               pos = pos.offset(this.inwardDirection.getOpposite());
               var7.add(new BaseDiggerModule.TunnelStep(pos, this.inwardDirection.getOpposite(), false));
               var9 = !var9;
            }
         } else {
            return List.of();
         }
      } else {
         return List.of();
      }
   }

   private Direction getOppositeDirection(Direction direction){
      return switch (direction) {
         case NORTH -> Direction.EAST;
         case EAST -> Direction.SOUTH;
         case SOUTH -> Direction.WEST;
         case WEST -> Direction.NORTH;
         default -> throw new IllegalArgumentException("Expected a horizontal direction, got " + direction);
      };
   }

   private int getLayerStartCoord(Direction direction, BaseDiggerModule.ExcavationBounds bounds){
      return switch (direction) {
         case NORTH -> bounds.min().getZ() + 1;
         case EAST -> bounds.max().getX() - 1;
         case SOUTH -> bounds.max().getZ() - 1;
         case WEST -> bounds.min().getX() + 1;
         default -> throw new IllegalArgumentException("Expected a horizontal direction, got " + direction);
      };
   }

   private int getLayerEndCoord(Direction direction, BaseDiggerModule.ExcavationBounds bounds){
      return switch (direction) {
         case NORTH -> bounds.min().getZ();
         case EAST -> bounds.max().getX();
         case SOUTH -> bounds.max().getZ();
         case WEST -> bounds.min().getX();
         default -> throw new IllegalArgumentException("Expected a horizontal direction, got " + direction);
      };
   }

   private int getCoordAlongAxis(BlockPos position, Direction direction){
      return direction.getAxis() == Axis.X ? position.getX() : position.getZ();
   }

   private boolean isWithinBounds(BlockPos position, BaseDiggerModule.ExcavationBounds bounds){
      return position.getX() >= bounds.min().getX()
         && position.getX() <= bounds.max().getX()
         && position.getY() >= bounds.min().getY()
         && position.getY() <= bounds.max().getY()
         && position.getZ() >= bounds.min().getZ()
         && position.getZ() <= bounds.max().getZ();
   }

   private void mineBlock(MinecraftClient client, BlockPos position){
      Vec3d vec2 = client.player.getEyePos();
      Vec3d vec = Vec3d.ofCenter(position);
      float yaw = RotationUtil.getYaw(vec2, vec);
      float pitch = RotationUtil.getPitch(vec2, vec);
      client.player.setYaw(yaw);
      client.player.setHeadYaw(yaw);
      client.player.setBodyYaw(yaw);
      client.player.setPitch(pitch);
   }

   private void selectToolSlot(MinecraftClient client){
      this.selectSlot(client, 0);
   }

   private boolean isPickaxeEffective(MinecraftClient client){
      ItemStack stack = client.player.getInventory().getStack(1);
      if (!stack.isEmpty() && stack.isIn(ItemTags.PICKAXES) && !this.hasRequiredEnchant(stack)) {
         this.selectSlot(client, 1);
         return true;
      } else {
         return false;
      }
   }

   private void selectSlot(MinecraftClient client, int slot){
      if (client.player.getInventory().getSelectedSlot() != slot) {
         client.player.getInventory().setSelectedSlot(slot);
         this.sendSlotPacket(client, slot);
      }
   }

   private boolean hasRequiredEnchant(ItemStack stack){
      if (!stack.isEmpty() && stack.isOf(Items.NETHERITE_PICKAXE)) {
         LoreComponent loreComponent = (LoreComponent)stack.get(DataComponentTypes.LORE);
         if (loreComponent == null) {
            return false;
         } else {
            for (Text text : loreComponent.lines()) {
               String toLowerCase = Normalizer.normalize(text.getString(), Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
               if (toLowerCase.contains("dao 3x3 khoi moi lan")) {
                  return true;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   private void sendSlotPacket(MinecraftClient client, int slot){
      if (client.player.networkHandler != null) {
         client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
      }
   }

   private void restoreAutoTool(){
      if (this.prevAutoTool != null) {
         BaritoneHelper.setSettingBool("autoTool", this.prevAutoTool);
         this.prevAutoTool = null;
      }

      if (this.prevAllowParkour != null) {
         BaritoneHelper.setSettingBool("allowInventory", this.prevAllowParkour);
         this.prevAllowParkour = null;
      }
   }

   private void restoreAllowBreak(){
      if (this.prevAllowBreak != null) {
         BaritoneHelper.setSettingBool("allowBreak", this.prevAllowBreak);
         this.prevAllowBreak = null;
      }
   }

   private List<BaseDiggerModule.ExcavationLayer> buildLayers(BaseDiggerModule.ExcavationBounds bounds, List<BaseDiggerModule.ExcavationBlock> blocksToDig){
      int y3 = bounds.max().getY();
      int y2 = bounds.min().getY();
      int var5 = y3 - y2 + 1;
      int var6 = (var5 + 2) / 3;
      ArrayList var7 = new ArrayList(var6);

      for (int index2 = 0; index2 < var6; index2++) {
         var7.add(new ArrayList());
      }

      for (BaseDiggerModule.ExcavationBlock excavationBlock : blocksToDig) {
         int y = (y3 - excavationBlock.position().getY()) / 3;
         ((List)var7.get(y)).add(excavationBlock);
      }

      ArrayList var15 = new ArrayList(var6);

      for (int index = 0; index < var6; index++) {
         int var17 = y3 - index * 3;
         int max = Math.max(y2, var17 - 2);
         int value = var17 - max + 1;

         byte var13 = switch (value) {
            case 1 -> 30;
            case 2 -> 70;
            case 3 -> 100;
            default -> throw new IllegalStateException("Unexpected excavation layer height: " + value);
         };
         var15.add(
            new BaseDiggerModule.ExcavationLayer(
               index, var17, max, var13, List.copyOf((Collection<? extends BaseDiggerModule.ExcavationBlock>)var7.get(index))
            )
         );
      }

      return List.copyOf(var15);
   }

   private List<BaseDiggerModule.ExcavationBlock> collectBlocksToDig(ClientWorld world, BaseDiggerModule.ExcavationBounds bounds){
      ArrayList var3 = new ArrayList();

      for (BlockPos pos : BlockPos.iterate(bounds.min(), bounds.max())) {
         BlockState state = world.getBlockState(pos);
         if (!state.isAir()) {
            var3.add(new BaseDiggerModule.ExcavationBlock(pos.toImmutable(), state));
         }
      }

      return List.copyOf(var3);
   }

   private void stopAndReset(){
      MinecraftClient client = MinecraftClient.getInstance();
      this.cancelBaritone();
      this.restoreAutoTool();
      this.restoreAllowBreak();
      this.anchor = null;
      this.targetPos = null;
      this.inwardDirection = null;
      this.bounds = null;
      this.layers = List.of();
      this.digQueue = List.of();
      this.tunnelSteps = List.of();
      this.stagingPos = null;
      this.layerQueue = List.of();
      this.threeByThreePhase = BaseDiggerModule.ThreeByThreePhase.IDLE;
      this.lastActionNanos = 0L;
      this.digQueueIndex = 0;
      this.tunnelIndex = 0;
      this.layerIndex = 0;
      this.layerQueueIndex = 0;
      this.currentDigPos = null;
      this.retryCount = 0;
      this.restockCheckTick = 0;
      if (client.interactionManager != null) {
         client.interactionManager.cancelBlockBreaking();
      }

      try {
         BaritoneHelper.cancelEverything();
      } catch (Exception e) {
      }
   }

   private BaseDiggerModule.ExcavationBounds getBounds(){
      if (this.anchor == null) {
         return null;
      } else {
         int valueInt3 = this.sizeX.getValueInt();
         int valueInt2 = this.sizeYSetting.getValueInt();
         int valueInt = this.sizeZ.getValueInt();
         int x = this.anchor.getX() - valueInt3 / 2;
         int y = this.anchor.getY();
         int z = this.anchor.getZ() - valueInt / 2;
         BlockPos pos2 = new BlockPos(x, y, z);
         BlockPos pos = new BlockPos(x + valueInt3 - 1, y + valueInt2 - 1, z + valueInt - 1);
         return new BaseDiggerModule.ExcavationBounds(pos2, pos);
      }
   }

   private void tickPathing(){
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null && client.world != null && this.cameraEntity != null && this.lastWorld == client.world) {
         long nanoTime = System.nanoTime();
         double max = Math.min(0.1, Math.max(0.0, (nanoTime - this.lastOrbitNanos) / 1.0E9));
         this.lastOrbitNanos = nanoTime;
         if (this.autoOrbitSetting.getValue() && !this.digging) {
            this.orbitAngle = MathHelper.wrapDegrees(this.orbitAngle + max * this.orbitSpeedSetting.getValue());
         }

         BaseDiggerModule.ExcavationBounds var6 = this.getBounds();
         if (var6 == null) {
            this.tickWorldSync();
         } else {
            Vec3d vec5 = var6.center();
            double toRadians = Math.toRadians(this.orbitAngle);
            double value2 = this.cameraDistanceSetting.getValue();
            double value = Math.max(3.0, Math.min(16.0, this.sizeYSetting.getValue() * 0.45 + 2.5));
            Vec3d vec4 = new Vec3d(vec5.x + Math.cos(toRadians) * value2, vec5.y + value, vec5.z + Math.sin(toRadians) * value2);
            float get = MathHelper.clamp(this.boxAnimation.get(), 0.0F, 1.0F);
            Vec3d vec = client.player.getEntityPos();
            Vec3d vec2 = vec.lerp(vec4, get);
            Vec3d vec3 = vec5.subtract(vec2);
            double sqrt = Math.sqrt(vec3.x * vec3.x + vec3.z * vec3.z);
            float atan2 = (float)Math.toDegrees(Math.atan2(vec3.z, vec3.x)) - 90.0F;
            float atan22 = (float)(-Math.toDegrees(Math.atan2(vec3.y, sqrt)));
            float yaw = client.player.getYaw() + MathHelper.wrapDegrees(atan2 - client.player.getYaw()) * get;
            float pitch = MathHelper.lerp(get, client.player.getPitch(), atan22);
            this.cameraEntity.setPosition(vec2);
            this.cameraEntity.setYaw(yaw);
            this.cameraEntity.setPitch(MathHelper.clamp(pitch, -90.0F, 90.0F));
            this.cameraEntity.setHeadYaw(yaw);
            if (client.getCameraEntity() != this.cameraEntity) {
               client.setCameraEntity(this.cameraEntity);
            }

            if (this.digging && get <= 0.001F) {
               this.tickWorldSync();
            }
         }
      } else {
         this.tickWorldSync();
      }
   }

   private void tickWorldSync(){
      MinecraftClient client = MinecraftClient.getInstance();
      if (this.cameraEntity != null && client.getCameraEntity() == this.cameraEntity) {
         Entity removed = this.lastPlayer;
         if (removed == null || removed.isRemoved()) {
            removed = client.player;
         }

         if (removed != null) {
            client.setCameraEntity(removed);
         }
      }

      if (this.targetPath != null) {
         client.options.setPerspective(this.targetPath);
      }

      if (this.cameraEntity != null && this.lastWorld != null) {
         this.lastWorld.removeEntity(this.cameraEntity.getId(), RemovalReason.DISCARDED);
      }

      this.cameraEntity = null;
      this.lastWorld = null;
      this.lastPlayer = null;
      this.targetPath = null;
      this.orbitActive = false;
      this.digging = false;
      this.lastOrbitNanos = 0L;
      this.boxAnimation.snapTo(0.0F);
   }

   private static void renderWireframes(WorldRenderContext context){
      BaseDiggerModule baseDiggerModule = instance;
      MinecraftClient client = MinecraftClient.getInstance();
      if (baseDiggerModule != null && baseDiggerModule.renderBoxSetting.getValue() && (baseDiggerModule.initialized || baseDiggerModule.isEnabled()) && baseDiggerModule.worldRef == client.world) {
         BaseDiggerModule.ExcavationBounds var3 = baseDiggerModule.getBounds();
         if (var3 != null) {
            BlockPos pos2 = var3.min();
            BlockPos pos = var3.max();
            RenderUtil.drawWorldBo(
               context,
               pos2.getX(),
               pos2.getY(),
               pos2.getZ(),
               pos.getX() + 1.0,
               pos.getY() + 1.0,
               pos.getZ() + 1.0,
               808900863,
               -531635713,
               true,
               true,
               true,
               2.0F
            );
         }
      }
   }

   private static final class BaritoneHelper {
      static void cancelEverything() {
         try {
            Object provider = getProvider();
            if (provider == null) return;
            Object baritone = provider.getClass().getMethod("getPrimaryBaritone").invoke(provider);
            Object behavior = baritone.getClass().getMethod("getPathingBehavior").invoke(baritone);
            behavior.getClass().getMethod("cancelEverything").invoke(behavior);
         } catch (Throwable ignored) {}
      }

      static void setGoalAndPath(BlockPos pos) {
         try {
            Object provider = getProvider();
            if (provider == null) return;
            Object baritone = provider.getClass().getMethod("getPrimaryBaritone").invoke(provider);
            Object customGoal = baritone.getClass().getMethod("getCustomGoalProcess").invoke(baritone);
            Class<?> goalBlockClass = Class.forName("baritone.api.pathing.goals.GoalBlock");
            Constructor<?> ctor = goalBlockClass.getConstructor(BlockPos.class);
            Object goal = ctor.newInstance(pos);
            Method m = customGoal.getClass().getMethod("setGoalAndPath", Class.forName("baritone.api.pathing.goals.Goal"));
            m.invoke(customGoal, goal);
         } catch (Throwable ignored) {}
      }

      static void clearArea(BlockPos min, BlockPos max) {
         try {
            Object provider = getProvider();
            if (provider == null) return;
            Object baritone = provider.getClass().getMethod("getPrimaryBaritone").invoke(provider);
            Object builder = baritone.getClass().getMethod("getBuilderProcess").invoke(baritone);
            Method m = builder.getClass().getMethod("clearArea", BlockPos.class, BlockPos.class);
            m.invoke(builder, min, max);
         } catch (Throwable ignored) {}
      }

      static boolean isBuilderActive() {
         try {
            Object provider = getProvider();
            if (provider == null) return false;
            Object baritone = provider.getClass().getMethod("getPrimaryBaritone").invoke(provider);
            Object builder = baritone.getClass().getMethod("getBuilderProcess").invoke(baritone);
            return (boolean) builder.getClass().getMethod("isActive").invoke(builder);
         } catch (Throwable ignored) {
            return false;
         }
      }

      static Boolean getSettingBool(String name) {
         try {
            Class<?> apiClass = Class.forName("baritone.api.BaritoneAPI");
            Object settings = apiClass.getMethod("getSettings").invoke(null);
            Field f = settings.getClass().getField(name);
            Object settingObj = f.get(settings);
            Field valField = settingObj.getClass().getField("value");
            return (Boolean) valField.get(settingObj);
         } catch (Throwable ignored) {
            return null;
         }
      }

      static void setSettingBool(String name, Boolean val) {
         try {
            Class<?> apiClass = Class.forName("baritone.api.BaritoneAPI");
            Object settings = apiClass.getMethod("getSettings").invoke(null);
            Field f = settings.getClass().getField(name);
            Object settingObj = f.get(settings);
            Field valField = settingObj.getClass().getField("value");
            valField.set(settingObj, val);
         } catch (Throwable ignored) {}
      }

      private static Object getProvider() {
         try {
            Class<?> apiClass = Class.forName("baritone.api.BaritoneAPI");
            return apiClass.getMethod("getProvider").invoke(null);
         } catch (Throwable ignored) {
            return null;
         }
      }
   }

   @Environment(EnvType.CLIENT)
   private record ExcavationBlock(BlockPos position, BlockState state){
   }

   @Environment(EnvType.CLIENT)
   private record ExcavationBounds(BlockPos min, BlockPos max){
      private Vec3d center(){
         return new Vec3d(
            (this.min.getX() + this.max.getX() + 1.0) * 0.5,
            (this.min.getY() + this.max.getY() + 1.0) * 0.5,
            (this.min.getZ() + this.max.getZ() + 1.0) * 0.5
         );
      }
   }

   @Environment(EnvType.CLIENT)
   private record ExcavationLayer(int index, int topY, int bottomY, int heightPercent, List<BaseDiggerModule.ExcavationBlock> blocksToDig){
   }

   @Environment(EnvType.CLIENT)
   private static enum ThreeByThreePhase {
      IDLE,
      MOVING_TO_STAGING,
      THINKING,
      MINING_FIRST_TUNNEL,
      MOVING_INTO_FIRST_VOLUME,
      MINING_FIRST_LAYER,
      CLEANING_FIRST_LAYER,
      MOVING_TO_DESCENT_CORNER,
      MINING_DOWNWARD_SHAFT,
      COMPLETE;
   }

   @Environment(EnvType.CLIENT)
   private record TunnelStep(BlockPos feetPosition, Direction direction, boolean mineBeforeMove){
   }
}
