package made4mischief.astatine.client.modules.player;

import made4mischief.astatine.client.mixin.ClientWorldAccessor;
import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import made4mischief.astatine.client.utils.inventory.InventoryUtil;
import made4mischief.astatine.client.utils.inventory.SilentSlotManager;
import made4mischief.astatine.client.utils.rotation.RotationManager;
import made4mischief.astatine.client.utils.rotation.RotationUtil;
import made4mischief.astatine.client.utils.world.BlockPlacementUtil;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.ClientPlayerEntity;

@Environment(EnvType.CLIENT)
public final class SelfTrapModule extends Module {
   private static final Object ROTATION_STATE = new Object();
   private static final float FALL_TOLERANCE = 1.0F;
   private static final int JUMP_MOTION_TICKS = 10;
   private static final int JUMP_HOLD_TICKS = 4;
   private static final int PACKET_WAIT_TICKS = 6;
   private static final Direction[] TRAP_DIRECTIONS = new Direction[]{
      Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
   };
   private static SelfTrapModule instance;
   private final NumberSetting placeRangeSetting = this.addNumber("Place Range", 4.5, 2.0, 6.0, 0.25);
   private final NumberSetting placeDelaySetting = this.addNumber("Place Delay", 0.0, 0.0, 10.0, 1.0);
   private final NumberSetting topDelaySetting = this.addNumber("Top Delay", 1.0, 0.0, 10.0, 1.0);
   private final BooleanSetting rotateSetting = this.addBoolean("Rotate", true);
   private final BooleanSetting movementFixSetting = this.addBoolean("Movement Fi", true);
   private final BooleanSetting strictDirectionSetting = this.addBoolean("Strict Direction", true);
   private final BooleanSetting jumpForTopSetting = this.addBoolean("Jump For Top", true);
   private final BooleanSetting disableOnMoveSetting = this.addBoolean("Disable On Move", true);
   private final BooleanSetting autoDisableSetting = this.addBoolean("Auto Disable", true);
   private final BooleanSetting swingHandSetting = this.addBoolean("Swing Hand", true);
   private BlockPos anchor;
   private double anchorY;
   private SelfTrapModule.PendingPlacement pendingPlacement;
   private int placeDelayTicks;
   private int motionTicks;
   private int jumpHoldTicks;
   private boolean jumping;
   private BlockPos packetPlaceTarget;
   private int packetPlaceWaitTicks;

   public SelfTrapModule(){
      super("SelfTrap", Category.PLAYER, "Bá»c báº£n thÃ¢n báº±ng tÆ°á»ng vÃ  mÃ¡i háº¯c diá»‡n tháº¡ch.", -1, true);
      this.movementFixSetting.visibleWhen(this.rotateSetting::getValue);
      this.jumpForTopSetting.visibleWhen(this.strictDirectionSetting::getValue);
      instance = this;
   }

   @Override
   protected void onEnable(){
      this.clearAnchor();
      MinecraftClient client = MinecraftClient.getInstance();
      if (isInGame(client)) {
         this.updateAnchor(client.player);
      }
   }

   @Override
   protected void onDisable(){
      RotationManager.clearRotatingState(ROTATION_STATE);
      this.clearAnchor();
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      if (!isInGame(client)) {
         this.clearAnchor();
      } else {
         if (this.anchor == null) {
            this.updateAnchor(client.player);
         }

         if (this.disableOnMoveSetting.getValue() && this.hasAnchorMoved(client.player)) {
            this.disable();
         } else {
            this.tickPlayerMotion(client.player);
            if (this.packetPlaceTarget != null) {
               if (!client.world.getBlockState(this.packetPlaceTarget).isReplaceable()) {
                  this.packetPlaceTarget = null;
                  this.packetPlaceWaitTicks = 0;
               } else {
                  if (this.packetPlaceWaitTicks-- > 0) {
                     return;
                  }

                  this.packetPlaceTarget = null;
               }
            }

            int findHotBarItem = InventoryUtil.findHotBarItem(client, Items.OBSIDIAN);
            if (findHotBarItem == -1) {
               this.clearPendingPlacement();
            } else if (this.placeDelayTicks > 0) {
               this.placeDelayTicks--;
               this.tickAutoFill();
            } else {
               BlockPos pos2 = null;
               if (this.pendingPlacement != null) {
                  BlockPos pos = this.pendingPlacement.placement().target();
                  SelfTrapModule.PlacementResult var6 = this.executePendingPlacement(client, findHotBarItem);
                  if (var6 == SelfTrapModule.PlacementResult.WAITING) {
                     return;
                  }

                  if (var6 == SelfTrapModule.PlacementResult.PLACED) {
                     pos2 = pos;
                     if (this.packetPlaceTarget != null) {
                        return;
                     }
                  }
               }

               SelfTrapModule.PlacementAction var7 = this.findPlacement(client, pos2);
               if (var7 == null) {
                  RotationManager.clearRotatingState(ROTATION_STATE);
                  this.tickPacketPlacement(client);
                  if (this.autoDisableSetting.getValue() && this.isFullyTrapped(client)) {
                     this.disable();
                  }
               } else {
                  this.placeBlock(client, var7);
                  if (!this.rotateSetting.getValue() && this.placeDelayTicks == 0) {
                     this.executePendingPlacement(client, findHotBarItem);
                  }
               }
            }
         }
      }
   }

   public static void applyJumpInput(ClientPlayerEntity player){
      SelfTrapModule selfTrapModule = instance;
      if (selfTrapModule != null && selfTrapModule.isEnabled() && selfTrapModule.jumping && player != null && MinecraftClient.getInstance().currentScreen == null) {
         PlayerInput playerInput = player.input.playerInput;
         player.input.playerInput = new PlayerInput(
            playerInput.forward(), playerInput.backward(), playerInput.left(), playerInput.right(), true, playerInput.sneak(), playerInput.sprint()
         );
      }
   }

   private SelfTrapModule.PlacementAction findPlacement(MinecraftClient client, BlockPos excluded){
      for (Direction direction : TRAP_DIRECTIONS) {
         for (int index = 0; index <= 1; index++) {
            BlockPos pos = this.anchor.offset(direction).up(index);
            if (client.world.getBlockState(pos).isReplaceable() && !pos.equals(excluded)) {
               BlockPlacementUtil.Placement var9 = BlockPlacementUtil.find(client, pos, this.placeRangeSetting.getValue(), this.strictDirectionSetting.getValue());
               return var9 == null ? null : new SelfTrapModule.PlacementAction(var9, false);
            }
         }
      }

      BlockPos pos2 = this.anchor.up(2);
      if (!client.world.getBlockState(pos2).isReplaceable()) {
         return null;
      } else {
         BlockPlacementUtil.Placement var11 = this.findBlockPlacement(client, pos2, excluded);
         if (var11 != null) {
            return new SelfTrapModule.PlacementAction(var11, false);
         } else {
            BlockPlacementUtil.Placement var12 = this.findCapPlacement(client, pos2, excluded);
            if (var12 != null) {
               return new SelfTrapModule.PlacementAction(var12, true);
            } else {
               BlockPlacementUtil.Placement var13 = this.findTopPlacement(client, excluded);
               return var13 == null ? null : new SelfTrapModule.PlacementAction(var13, true);
            }
         }
      }
   }

   private BlockPlacementUtil.Placement findBlockPlacement(MinecraftClient client, BlockPos block, BlockPos excluded){
      return block.equals(excluded) ? null : BlockPlacementUtil.find(client, block, this.placeRangeSetting.getValue(), this.strictDirectionSetting.getValue());
   }

   private BlockPlacementUtil.Placement findCapPlacement(MinecraftClient client, BlockPos cap, BlockPos excluded){
      BlockPlacementUtil.Placement var4 = null;
      double pos3 = Double.MAX_VALUE;

      for (Direction direction : TRAP_DIRECTIONS) {
         BlockPos pos2 = cap.offset(direction);
         if (!client.world.getBlockState(pos2).isReplaceable()) {
            BlockPlacementUtil.Placement var12 = this.findSupportPlacement(client, cap, excluded, pos2, direction.getOpposite());
            if (var12 != null) {
               double pos = client.player.getEyePos().squaredDistanceTo(var12.hitResult().getPos());
               if (pos < pos3) {
                  var4 = var12;
                  pos3 = pos;
               }
            }
         }
      }

      return var4;
   }

   private BlockPlacementUtil.Placement findTopPlacement(MinecraftClient client, BlockPos excluded){
      BlockPlacementUtil.Placement var3 = null;
      double pos4 = Double.MAX_VALUE;

      for (Direction direction : TRAP_DIRECTIONS) {
         BlockPos pos2 = this.anchor.offset(direction).up(2);
         BlockPos pos3 = pos2.down();
         BlockPlacementUtil.Placement var12 = this.findSupportPlacement(client, pos2, excluded, pos3, Direction.UP);
         if (var12 != null) {
            double pos = client.player.getEyePos().squaredDistanceTo(var12.hitResult().getPos());
            if (pos < pos4) {
               var3 = var12;
               pos4 = pos;
            }
         }
      }

      return var3;
   }

   private BlockPlacementUtil.Placement findSupportPlacement(MinecraftClient client, BlockPos target, BlockPos excluded, BlockPos support, Direction clickedFace){
      if (!target.equals(excluded) && BlockPlacementUtil.canPlaceAt(client, target) && !client.world.getBlockState(support).isReplaceable()) {
         Vec3d vec = Vec3d.ofCenter(support)
            .add(clickedFace.getOffsetX() * 0.5, clickedFace.getOffsetY() * 0.5, clickedFace.getOffsetZ() * 0.5);
         double blockInteractionRange = Math.min(this.placeRangeSetting.getValue(), client.player.getBlockInteractionRange());
         return client.player.getEyePos().squaredDistanceTo(vec) > blockInteractionRange * blockInteractionRange
            ? null
            : new BlockPlacementUtil.Placement(target.toImmutable(), new BlockHitResult(vec, clickedFace, support, false));
      } else {
         return null;
      }
   }

   private void placeBlock(MinecraftClient client, SelfTrapModule.PlacementAction action){
      BlockPlacementUtil.Placement var3 = action.placement();
      Vec3d vec = var3.hitResult().getPos();
      float eyePos2 = RotationUtil.getYaw(client.player.getEyePos(), vec);
      float eyePos = RotationUtil.getPitch(client.player.getEyePos(), vec);
      this.pendingPlacement = new SelfTrapModule.PendingPlacement(var3, eyePos2, eyePos, action.directPacket());
      this.tickAutoFill();
      if (var3.target().getY() >= this.anchor.getY() + 2) {
         this.placeDelayTicks = Math.max(this.placeDelayTicks, this.topDelaySetting.getValueInt());
      }
   }

   private SelfTrapModule.PlacementResult executePendingPlacement(MinecraftClient client, int obsidianSlot){
      SelfTrapModule.PendingPlacement var3 = this.pendingPlacement;
      if (var3 == null) {
         return SelfTrapModule.PlacementResult.RETRY;
      } else if (!this.isPlacementValid(client, var3)) {
         this.clearPendingPlacement();
         return SelfTrapModule.PlacementResult.RETRY;
      } else {
         if (this.rotateSetting.getValue()) {
            this.tickAutoFill();
            if (!RotationManager.wasRotationSent(var3.yaw(), var3.pitch(), 1.0F)) {
               return SelfTrapModule.PlacementResult.WAITING;
            }
         }

         ActionResult[] var4 = new ActionResult[]{ActionResult.PASS};
         boolean hitResult = SilentSlotManager.runWithSlot(client, obsidianSlot, () -> {
            if (var3.directPacket()) {
               this.interactBlockPacket(client, var3.placement().hitResult());
               var4[0] = ActionResult.SUCCESS;
            } else {
               var4[0] = client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, var3.placement().hitResult());
            }

            if (this.swingHandSetting.getValue()) {
               client.player.swingHand(Hand.MAIN_HAND);
            }
         });
         if (!hitResult) {
            return SelfTrapModule.PlacementResult.WAITING;
         } else {
            if (var3.directPacket()) {
               this.packetPlaceTarget = var3.placement().target();
               this.packetPlaceWaitTicks = 6;
            }

            this.pendingPlacement = null;
            this.placeDelayTicks = this.placeDelaySetting.getValueInt();
            RotationManager.clearRotatingState(ROTATION_STATE);
            return SelfTrapModule.PlacementResult.PLACED;
         }
      }
   }

   private boolean isPlacementValid(MinecraftClient client, SelfTrapModule.PendingPlacement action){
      if (!action.directPacket()) {
         BlockPlacementUtil.Placement var6 = BlockPlacementUtil.find(client, action.placement().target(), this.placeRangeSetting.getValue(), this.strictDirectionSetting.getValue());
         return BlockPlacementUtil.sameFace(action.placement(), var6);
      } else {
         BlockHitResult hitResult = action.placement().hitResult();
         if (BlockPlacementUtil.canPlaceAt(client, action.placement().target()) && !client.world.getBlockState(hitResult.getBlockPos()).isReplaceable()) {
            double blockInteractionRange = Math.min(this.placeRangeSetting.getValue(), client.player.getBlockInteractionRange());
            return client.player.getEyePos().squaredDistanceTo(hitResult.getPos()) <= blockInteractionRange * blockInteractionRange;
         } else {
            return false;
         }
      }
   }

   private void interactBlockPacket(MinecraftClient client, BlockHitResult hitResult){
      PendingUpdateManager pendingUpdateManager = ((ClientWorldAccessor)client.world).astatine$getPendingUpdateManager().incrementSequence();

      try {
         client.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, pendingUpdateManager.getSequence()));
      } catch (Throwable e) {
         if (pendingUpdateManager != null) {
            try {
               pendingUpdateManager.close();
            } catch (Throwable e2) {
               e.addSuppressed(e2);
            }
         }

         throw e;
      }

      if (pendingUpdateManager != null) {
         pendingUpdateManager.close();
      }
   }

   private void tickAutoFill(){
      if (this.pendingPlacement != null && this.rotateSetting.getValue()) {
         RotationManager.setRotation(ROTATION_STATE, this.pendingPlacement.yaw(), this.pendingPlacement.pitch(), false, this.movementFixSetting.getValue());
      }
   }

   private void tickPacketPlacement(MinecraftClient client){
      if (this.strictDirectionSetting.getValue()
         && this.jumpForTopSetting.getValue()
         && this.motionTicks <= 0
         && client.player.isOnGround()
         && this.isTrapped(client)
         && client.world.getBlockState(this.anchor.up(2)).isReplaceable()) {
         this.jumping = true;
         this.jumpHoldTicks = 4;
         this.motionTicks = 10;
      }
   }

   private void tickPlayerMotion(ClientPlayerEntity player){
      if (this.motionTicks > 0) {
         this.motionTicks--;
      }

      if (this.jumping) {
         if (player.isOnGround() && this.jumpHoldTicks > 0) {
            this.jumpHoldTicks--;
         } else {
            this.jumping = false;
            this.jumpHoldTicks = 0;
         }
      }
   }

   private boolean isTrapped(MinecraftClient client){
      for (Direction direction : TRAP_DIRECTIONS) {
         for (int index = 0; index <= 1; index++) {
            if (client.world.getBlockState(this.anchor.offset(direction).up(index)).isReplaceable()) {
               return false;
            }
         }
      }

      return true;
   }

   private boolean isFullyTrapped(MinecraftClient client){
      return this.isTrapped(client) && !client.world.getBlockState(this.anchor.up(2)).isReplaceable();
   }

   private void updateAnchor(ClientPlayerEntity player){
      this.anchor = player.getBlockPos().toImmutable();
      this.anchorY = player.getY();
   }

   private boolean hasAnchorMoved(ClientPlayerEntity player){
      return MathHelper.floor(player.getX()) != this.anchor.getX()
         || MathHelper.floor(player.getZ()) != this.anchor.getZ()
         || player.getY() < this.anchorY - 0.25
         || player.getY() > this.anchorY + 1.45;
   }

   private void clearPendingPlacement(){
      this.pendingPlacement = null;
      RotationManager.clearRotatingState(ROTATION_STATE);
   }

   private void clearAnchor(){
      this.anchor = null;
      this.anchorY = 0.0;
      this.pendingPlacement = null;
      this.placeDelayTicks = 0;
      this.motionTicks = 0;
      this.jumpHoldTicks = 0;
      this.jumping = false;
      this.packetPlaceTarget = null;
      this.packetPlaceWaitTicks = 0;
   }

   private static boolean isInGame(MinecraftClient client){
      return client.player != null
         && client.world != null
         && client.interactionManager != null
         && client.player.networkHandler != null
         && !client.player.isDead();
   }

   @Environment(EnvType.CLIENT)
   private record PendingPlacement(BlockPlacementUtil.Placement placement, float yaw, float pitch, boolean directPacket){
   }

   @Environment(EnvType.CLIENT)
   private record PlacementAction(BlockPlacementUtil.Placement placement, boolean directPacket){
   }

   @Environment(EnvType.CLIENT)
   private static enum PlacementResult {
      WAITING,
      RETRY,
      PLACED;
   }
}

