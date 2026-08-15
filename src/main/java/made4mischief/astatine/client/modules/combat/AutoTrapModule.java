package made4mischief.astatine.client.modules.combat;

import java.util.Iterator;
import made4mischief.astatine.client.mixin.ClientWorldAccessor;
import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.modules.player.FriendModule;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import made4mischief.astatine.client.utils.combat.TargetUtil;
import made4mischief.astatine.client.utils.inventory.InventoryUtil;
import made4mischief.astatine.client.utils.inventory.SilentSlotManager;
import made4mischief.astatine.client.utils.rotation.RotationManager;
import made4mischief.astatine.client.utils.rotation.RotationUtil;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@Environment(EnvType.CLIENT)
public final class AutoTrapModule extends Module {
   private static final Object ROTATION_STATE = new Object();
   private static final float ROTATION_SNAP_TOLERANCE = 1.0F;
   private static final int PACKET_WAIT_TICKS = 6;
   private static final int[] CAP_OFFSET = new int[]{0, 2, 0};
   private static final Direction[] TRAP_DIRECTIONS = new Direction[]{
      Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
   };
   private static final Direction[] sizeSetting = new Direction[]{
      Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP
   };
   private final NumberSetting topLayerSetting = this.addNumber("Target Range", 5.0, 1.0, 7.0, 0.5);
   private final NumberSetting placeRangeSetting = this.addNumber("Place Range", 4.5, 2.0, 6.0, 0.25);
   private final NumberSetting rotateSetting = this.addNumber("Place Delay", 0.0, 0.0, 10.0, 1.0);
   private final NumberSetting topDelaySetting = this.addNumber("Top Delay", 1.0, 0.0, 10.0, 1.0);
   private final BooleanSetting rotationDelaySetting = this.addBoolean("Rotate", true);
   private final BooleanSetting movementFixSetting = this.addBoolean("Movement Fi", true);
   private final BooleanSetting strictDirectionSetting = this.addBoolean("Strict Direction", true);
   private final BooleanSetting swingHandSetting = this.addBoolean("Swing Hand", true);
   private final BooleanSetting autoDisableSetting = this.addBoolean("Auto Disable", false);
   private PlayerEntity target;
   private AutoTrapModule.PendingPlacement pendingPlacement;
   private BlockPos anchor;
   private Direction anchorDirection;
   private int placeDelayTicks;
   private BlockPos packetPlaceTarget;
   private int packetPlaceWaitTicks;

   public AutoTrapModule(){
      super("AutoTrap", Category.COMBAT, "Nhá»‘t ngÆ°á»i chÆ¡i gáº§n nháº¥t báº±ng háº¯c diá»‡n tháº¡ch.", -1, true);
      this.movementFixSetting.visibleWhen(this.rotationDelaySetting::getValue);
   }

   @Override
   protected void onEnable(){
      this.resetTarget();
   }

   @Override
   protected void onDisable(){
      RotationManager.clearRotatingState(ROTATION_STATE);
      this.resetTarget();
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      if (!isInGame(client)) {
         RotationManager.clearRotatingState(ROTATION_STATE);
         this.resetTarget();
      } else {
         this.tickTrap(client);
         if (this.target == null) {
            RotationManager.clearRotatingState(ROTATION_STATE);
            this.pendingPlacement = null;
            this.packetPlaceTarget = null;
            this.packetPlaceWaitTicks = 0;
         } else {
            this.tickPlacement(client);
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
               RotationManager.clearRotatingState(ROTATION_STATE);
               this.pendingPlacement = null;
            } else if (this.placeDelayTicks > 0) {
               this.placeDelayTicks--;
               if (this.pendingPlacement != null && this.rotationDelaySetting.getValue()) {
                  this.rotate(this.pendingPlacement.yaw(), this.pendingPlacement.pitch());
               }
            } else {
               BlockPos pos3 = null;
               if (this.pendingPlacement != null) {
                  BlockPos pos2 = this.pendingPlacement.block();
                  AutoTrapModule.PlacementResult var6 = this.executePendingPlacement(client, findHotBarItem);
                  if (var6 == AutoTrapModule.PlacementResult.WAITING) {
                     return;
                  }

                  if (var6 == AutoTrapModule.PlacementResult.PLACED) {
                     pos3 = pos2;
                     if (this.packetPlaceTarget != null) {
                        return;
                     }
                  }
               }

               BlockPos pos = this.target.getBlockPos();
               AutoTrapModule.Placement var8 = this.findPlacement(client, pos, pos3);
               if (var8 == null) {
                  RotationManager.clearRotatingState(ROTATION_STATE);
                  if (this.autoDisableSetting.getValue() && this.canAnchor(client, pos)) {
                     this.disable();
                  }
               } else {
                  this.rotateToPlacement(client, pos, var8);
                  if (!this.rotationDelaySetting.getValue() && this.placeDelayTicks == 0) {
                     this.executePendingPlacement(client, findHotBarItem);
                  }
               }
            }
         }
      }
   }

   private void tickTrap(MinecraftClient client){
      if (!this.isValidTarget(client, this.target)) {
         this.target = TargetUtil.getClosestTarget(client, this.topLayerSetting.getValue());
         this.pendingPlacement = null;
         this.anchor = null;
         this.anchorDirection = null;
      }
   }

   private void tickPlacement(MinecraftClient client){
      BlockPos pos = this.target.getBlockPos();
      if (!pos.equals(this.anchor)) {
         this.anchor = pos.toImmutable();
         this.anchorDirection = this.getDirectionToAnchor(client, pos);
         this.clearPendingPlacement();
         this.placeDelayTicks = 0;
         this.packetPlaceTarget = null;
         this.packetPlaceWaitTicks = 0;
      }
   }

   private Direction getDirectionToAnchor(MinecraftClient client, BlockPos anchor){
      double x = client.player.getX() - (anchor.getX() + 0.5);
      double z = client.player.getZ() - (anchor.getZ() + 0.5);
      if (Math.abs(x) >= Math.abs(z)) {
         return x >= 0.0 ? Direction.EAST : Direction.WEST;
      } else {
         return z >= 0.0 ? Direction.SOUTH : Direction.NORTH;
      }
   }

   private boolean isValidTarget(MinecraftClient client, PlayerEntity player){
      return player != null
         && player != client.player
         && !FriendModule.isFriend(player)
         && player.isAlive()
         && !player.isSpectator()
         && client.world.getEntityById(player.getId()) == player
         && client.player.squaredDistanceTo(player) <= this.topLayerSetting.getValue() * this.topLayerSetting.getValue();
   }

   private AutoTrapModule.Placement findPlacement(MinecraftClient client, BlockPos anchor, BlockPos excludedBlock){
      for (Direction direction : TRAP_DIRECTIONS) {
         if (direction != this.anchorDirection) {
            AutoTrapModule.Placement var8 = this.findPlacementInDirection(client, anchor, direction, excludedBlock);
            if (var8 != null) {
               return var8;
            }

            if (!this.hasSpaceInDirection(client, anchor, direction, excludedBlock)) {
               return null;
            }
         }
      }

      AutoTrapModule.Placement var9 = this.findPlacementInDirection(client, anchor, this.anchorDirection, excludedBlock);
      if (var9 != null) {
         return var9;
      } else if (!this.hasSpaceInDirection(client, anchor, this.anchorDirection, excludedBlock)) {
         return null;
      } else {
         BlockPos pos = anchor.add(CAP_OFFSET[0], CAP_OFFSET[1], CAP_OFFSET[2]);
         if (!client.world.getBlockState(pos).isReplaceable()) {
            return null;
         } else {
            AutoTrapModule.Placement var11 = this.findCapPlacement(client, pos, excludedBlock);
            if (var11 != null) {
               return var11;
            } else {
               AutoTrapModule.Placement var12 = this.findTopPlacement(client, pos, excludedBlock);
               return var12 != null ? var12 : this.findRingPlacement(client, anchor, excludedBlock);
            }
         }
      }
   }

   private AutoTrapModule.Placement findPlacementInDirection(MinecraftClient client, BlockPos anchor, Direction direction, BlockPos excludedBlock){
      for (int index = 0; index <= 1; index++) {
         BlockPos pos = anchor.offset(direction).up(index);
         if (client.world.getBlockState(pos).isReplaceable() && !pos.equals(excludedBlock)) {
            return this.findTopPlacement(client, pos, excludedBlock);
         }
      }

      return null;
   }

   private boolean hasSpaceInDirection(MinecraftClient client, BlockPos anchor, Direction direction, BlockPos excludedBlock){
      for (int index = 0; index <= 1; index++) {
         BlockPos pos = anchor.offset(direction).up(index);
         if (client.world.getBlockState(pos).isReplaceable() && !pos.equals(excludedBlock)) {
            return false;
         }
      }

      return true;
   }

   private AutoTrapModule.Placement findTopPlacement(MinecraftClient client, BlockPos block, BlockPos excludedBlock){
      if (!block.equals(excludedBlock) && this.isBlockPlaceable(client, block)) {
         BlockHitResult hitResult = this.findHitResult(client, block);
         return hitResult != null && this.isInReach(client, hitResult.getPos()) ? new AutoTrapModule.Placement(block.toImmutable(), hitResult, false) : null;
      } else {
         return null;
      }
   }

   private AutoTrapModule.Placement findCapPlacement(MinecraftClient client, BlockPos cap, BlockPos excludedBlock){
      AutoTrapModule.Placement var4 = null;
      double pos3 = Double.MAX_VALUE;

      for (Direction direction : TRAP_DIRECTIONS) {
         BlockPos pos2 = cap.offset(direction);
         if (!client.world.getBlockState(pos2).isReplaceable()) {
            AutoTrapModule.Placement var12 = this.findSupportPlacement(client, cap, excludedBlock, pos2, direction.getOpposite());
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

   private AutoTrapModule.Placement findRingPlacement(MinecraftClient client, BlockPos anchor, BlockPos excludedBlock){
      AutoTrapModule.Placement var4 = null;
      double pos3 = Double.MAX_VALUE;

      for (Direction direction : TRAP_DIRECTIONS) {
         BlockPos pos2 = anchor.offset(direction).up(2);
         AutoTrapModule.Placement var12 = this.findSupportPlacement(client, pos2, excludedBlock, pos2.down(), Direction.UP);
         if (var12 != null) {
            double pos = client.player.getEyePos().squaredDistanceTo(var12.hitResult().getPos());
            if (pos < pos3) {
               var4 = var12;
               pos3 = pos;
            }
         }
      }

      return var4;
   }

   private AutoTrapModule.Placement findSupportPlacement(MinecraftClient client, BlockPos target, BlockPos excludedBlock, BlockPos support, Direction clickedFace){
      if (!target.equals(excludedBlock) && this.isBlockPlaceable(client, target) && !client.world.getBlockState(support).isReplaceable()) {
         Vec3d vec = Vec3d.ofCenter(support)
            .add(clickedFace.getOffsetX() * 0.5, clickedFace.getOffsetY() * 0.5, clickedFace.getOffsetZ() * 0.5);
         return !this.isInReach(client, vec)
            ? null
            : new AutoTrapModule.Placement(target.toImmutable(), new BlockHitResult(vec, clickedFace, support, false), true);
      } else {
         return null;
      }
   }

   private boolean canAnchor(MinecraftClient client, BlockPos anchor){
      if (!this.isAnchorSurrounded(client, anchor)) {
         return false;
      } else {
         BlockPos pos = anchor.add(CAP_OFFSET[0], CAP_OFFSET[1], CAP_OFFSET[2]);
         return !client.world.getBlockState(pos).isReplaceable();
      }
   }

   private boolean isAnchorSurrounded(MinecraftClient client, BlockPos anchor){
      for (Direction direction : TRAP_DIRECTIONS) {
         for (int index = 0; index <= 1; index++) {
            BlockPos pos = anchor.offset(direction).up(index);
            if (client.world.getBlockState(pos).isReplaceable()) {
               return false;
            }
         }
      }

      return true;
   }

   private boolean isBlockPlaceable(MinecraftClient client, BlockPos pos){
      if (!client.world.getBlockState(pos).isReplaceable()) {
         return false;
      } else {
         Iterator iterator = client.world
            .getOtherEntities(null, new Box(pos), entity -> !entity.isRemoved() && !entity.isSpectator() && entity.canHit())
            .iterator();
         if (iterator.hasNext()) {
            Entity entity = (Entity)iterator.next();
            return false;
         } else {
            return true;
         }
      }
   }

   private BlockHitResult findHitResult(MinecraftClient client, BlockPos targetPos){
      BlockHitResult hitResult2 = null;
      double squaredDistanceTo2 = Double.MAX_VALUE;

      for (Direction direction : sizeSetting) {
         BlockPos pos = targetPos.offset(direction);
         if (!client.world.getBlockState(pos).isReplaceable()) {
            Direction direction2 = direction.getOpposite();
            Vec3d vec = Vec3d.ofCenter(pos).add(direction2.getOffsetX() * 0.5, direction2.getOffsetY() * 0.5, direction2.getOffsetZ() * 0.5);
            BlockHitResult hitResult = new BlockHitResult(vec, direction2, pos, false);
            if (!this.strictDirectionSetting.getValue() || this.isHitResultValid(client, hitResult)) {
               double squaredDistanceTo = client.player.getEyePos().squaredDistanceTo(vec);
               if (squaredDistanceTo < squaredDistanceTo2) {
                  hitResult2 = hitResult;
                  squaredDistanceTo2 = squaredDistanceTo;
               }
            }
         }
      }

      return hitResult2;
   }

   private boolean isHitResultValid(MinecraftClient client, BlockHitResult hitResult){
      Vec3d vec2 = Vec3d.of(hitResult.getSide().getVector());
      Vec3d vec = hitResult.getPos().subtract(vec2.multiply(0.01));
      BlockHitResult hitResult2 = client.world
         .raycast(new RaycastContext(client.player.getEyePos(), vec, ShapeType.OUTLINE, FluidHandling.NONE, client.player));
      return hitResult2.getBlockPos().equals(hitResult.getBlockPos()) && hitResult2.getSide() == hitResult.getSide();
   }

   private boolean isInReach(MinecraftClient client, Vec3d hitPosition){
      double blockInteractionRange = Math.min(this.placeRangeSetting.getValue(), client.player.getBlockInteractionRange());
      return client.player.getEyePos().squaredDistanceTo(hitPosition) <= blockInteractionRange * blockInteractionRange;
   }

   private void rotateToPlacement(MinecraftClient client, BlockPos anchor, AutoTrapModule.Placement placement){
      float pos2 = RotationUtil.getYaw(client.player.getEyePos(), placement.hitResult().getPos());
      float pos = RotationUtil.getPitch(client.player.getEyePos(), placement.hitResult().getPos());
      this.pendingPlacement = new AutoTrapModule.PendingPlacement(
         anchor.toImmutable(), placement.block(), placement.hitResult(), pos2, pos, placement.directPacket()
      );
      if (this.rotationDelaySetting.getValue()) {
         this.rotate(pos2, pos);
      }

      if (placement.block().getY() >= anchor.getY() + 2) {
         this.placeDelayTicks = Math.max(this.placeDelayTicks, this.topDelaySetting.getValueInt());
      }
   }

   private AutoTrapModule.PlacementResult executePendingPlacement(MinecraftClient client, int obsidianSlot){
      AutoTrapModule.PendingPlacement var3 = this.pendingPlacement;
      if (var3 == null) {
         return AutoTrapModule.PlacementResult.RETRY;
      } else if (this.target == null || !this.target.getBlockPos().equals(var3.anchor())) {
         this.clearPendingPlacement();
         return AutoTrapModule.PlacementResult.RETRY;
      } else if (!this.isBlockPlaceable(client, var3.block()) || !this.isInReach(client, var3.hitResult().getPos())) {
         this.clearPendingPlacement();
         return AutoTrapModule.PlacementResult.RETRY;
      } else if (!this.isPlacementValid(client, var3)) {
         this.clearPendingPlacement();
         return AutoTrapModule.PlacementResult.RETRY;
      } else {
         if (this.rotationDelaySetting.getValue()) {
            this.rotate(var3.yaw(), var3.pitch());
            if (!RotationManager.wasRotationSent(var3.yaw(), var3.pitch(), 1.0F)) {
               return AutoTrapModule.PlacementResult.WAITING;
            }
         } else {
            RotationManager.clearRotatingState(ROTATION_STATE);
         }

         ActionResult[] var4 = new ActionResult[]{ActionResult.PASS};
         boolean hitResult = SilentSlotManager.runWithSlot(client, obsidianSlot, () -> {
            if (var3.directPacket()) {
               this.interactBlockPacket(client, var3.hitResult());
               var4[0] = ActionResult.SUCCESS;
            } else {
               var4[0] = client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, var3.hitResult());
            }

            if (this.swingHandSetting.getValue()) {
               client.player.swingHand(Hand.MAIN_HAND);
            }
         });
         if (!hitResult) {
            return AutoTrapModule.PlacementResult.WAITING;
         } else {
            if (var3.directPacket()) {
               this.packetPlaceTarget = var3.block();
               this.packetPlaceWaitTicks = 6;
            }

            this.pendingPlacement = null;
            this.placeDelayTicks = this.rotateSetting.getValueInt();
            RotationManager.clearRotatingState(ROTATION_STATE);
            return AutoTrapModule.PlacementResult.PLACED;
         }
      }
   }

   private boolean isPlacementValid(MinecraftClient client, AutoTrapModule.PendingPlacement pending){
      if (!pending.directPacket()) {
         BlockHitResult hitResult = this.findHitResult(client, pending.block());
         return isSameHitResult(pending.hitResult(), hitResult);
      } else {
         return this.isBlockPlaceable(client, pending.block())
               && this.isInReach(client, pending.hitResult().getPos())
               && !client.world.getBlockState(pending.hitResult().getBlockPos()).isReplaceable()
            ? pending.hitResult().getBlockPos().offset(pending.hitResult().getSide()).equals(pending.block())
            : false;
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

   private void rotate(float yaw, float pitch){
      RotationManager.setRotation(ROTATION_STATE, yaw, pitch, false, this.movementFixSetting.getValue());
   }

   private void clearPendingPlacement(){
      this.pendingPlacement = null;
      RotationManager.clearRotatingState(ROTATION_STATE);
   }

   private static boolean isSameHitResult(BlockHitResult first, BlockHitResult second){
      return first != null && second != null && first.getBlockPos().equals(second.getBlockPos()) && first.getSide() == second.getSide();
   }

   private static boolean isInGame(MinecraftClient client){
      return client.player != null
         && client.world != null
         && client.interactionManager != null
         && client.player.networkHandler != null
         && !client.player.isDead();
   }

   private void resetTarget(){
      this.target = null;
      this.pendingPlacement = null;
      this.anchor = null;
      this.anchorDirection = null;
      this.placeDelayTicks = 0;
      this.packetPlaceTarget = null;
      this.packetPlaceWaitTicks = 0;
   }

   @Environment(EnvType.CLIENT)
   private record PendingPlacement(BlockPos anchor, BlockPos block, BlockHitResult hitResult, float yaw, float pitch, boolean directPacket){
   }

   @Environment(EnvType.CLIENT)
   private record Placement(BlockPos block, BlockHitResult hitResult, boolean directPacket){
   }

   @Environment(EnvType.CLIENT)
   private static enum PlacementResult {
      WAITING,
      RETRY,
      PLACED;
   }
}

