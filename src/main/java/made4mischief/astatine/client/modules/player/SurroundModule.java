package made4mischief.astatine.client.modules.player;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.ModeSetting;
import made4mischief.astatine.client.utils.inventory.InventoryUtil;
import made4mischief.astatine.client.utils.rotation.RotationManager;
import made4mischief.astatine.client.utils.rotation.RotationUtil;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;

@Environment(EnvType.CLIENT)
public class SurroundModule extends Module {
   private static final float ROTATION_TOLERANCE = 0.1F;
   private static final double POSITION_EPSILON = 1.0E-4;
   private static final double MAX_Y_DRIFT = 0.25;
   private static final int CRYSTAL_BREAK_DELAY = 2;
   private static final int INSTANT_SWAP_DELAY = 2;
   private static final Direction[] HORIZONTAL_DIRECTIONS = new Direction[]{
      Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
   };
   private static final Direction[] PLACE_FACES = new Direction[]{
      Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP
   };
   private final ModeSetting modeSetting = this.addMode("Mode", "Packet", new String[]{"Legit", "Packet"});
   private final ModeSetting placeModeSetting = this.addMode("PlaceMode", "Normal", new String[]{"Normal", "Instant"});
   private final BooleanSetting silentPlaceSetting = this.addBoolean("SilentPlace", true);
   private final List<BlockPos> targets = new ArrayList<>();
   private final Set<BlockPos> missingTargets = new LinkedHashSet<>();
   private SurroundModule.PendingAction pendingAction;
   private double lastPlayerYaw;
   private int lastPlayerBlock;
   private int nextTargetInde;
   private int crystalBreakDelayTicks;
   private int instantSwapDelayTicks;
   private int originalSlot = -1;
   private int swapDelayTicks;
   private boolean positionSnapshotted;

   public SurroundModule(){
      super("Surround", Category.PLAYER, "Äáº·t háº¯c diá»‡n tháº¡ch quanh chÃ¢n ngÆ°á»i chÆ¡i.", -1);
   }

   @Override
   protected void onEnable(){
      this.resetState();
      MinecraftClient client = MinecraftClient.getInstance();
      if (this.canEngage(client)) {
         this.snapshotPlayerPosition(client);
      }
   }

   @Override
   protected void onDisable(){
      this.restoreSelectedSlot(MinecraftClient.getInstance());
      RotationManager.clearRotatingState();
      this.resetState();
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      if (!this.canEngage(client)) {
         this.disable();
      } else {
         if (!this.positionSnapshotted) {
            this.snapshotPlayerPosition(client);
         }

         this.tickSwapDelay(client);
         if (this.hasPlayerMoved(client)) {
            this.disable();
         } else if (this.hasMissingTargets(client)) {
            this.cancelPendingAction(client);
         } else {
            int findHotBarItem = InventoryUtil.findHotBarItem(client, Items.OBSIDIAN);
            if (findHotBarItem == -1) {
               this.pendingAction = null;
               RotationManager.clearRotatingState();
            } else {
               if (this.instantSwapDelayTicks > 0) {
                  this.instantSwapDelayTicks--;
               }

               if (this.placeModeSetting.is("Instant")) {
                  this.pendingAction = null;
                  RotationManager.clearRotatingState();
                  if (this.instantSwapDelayTicks == 0) {
                     this.applySilentSwap(client, findHotBarItem);
                  }
               } else {
                  if (this.crystalBreakDelayTicks > 0) {
                     this.crystalBreakDelayTicks--;
                  }

                  if (this.pendingAction != null) {
                     if (!this.isRotationSent()) {
                        this.rotateToAction();
                        return;
                     }

                     this.executePendingAction(client, findHotBarItem);
                  }

                  if (this.pendingAction == null) {
                     this.clearRotationIfIdle(client);
                  }
               }
            }
         }
      }
   }

   private void snapshotPlayerPosition(MinecraftClient client){
      this.lastPlayerYaw = client.player.getY();
      this.lastPlayerBlock = MathHelper.floor(this.lastPlayerYaw + 1.0E-4);
      this.missingTargets.clear();
      this.missingTargets.addAll(this.getPlayerFootprint(client, this.lastPlayerBlock));
      this.targets.clear();
      this.targets.addAll(this.expandFootprint(this.missingTargets));
      this.nextTargetInde = 0;
      this.positionSnapshotted = true;
   }

   private Set<BlockPos> getPlayerFootprint(MinecraftClient client, int blockY){
      Box Box = client.player.getBoundingBox();
      int floor4 = MathHelper.floor(Box.minX + 1.0E-4);
      int floor3 = MathHelper.floor(Box.maxX - 1.0E-4);
      int floor2 = MathHelper.floor(Box.minZ + 1.0E-4);
      int floor = MathHelper.floor(Box.maxZ - 1.0E-4);
      LinkedHashSet linkedHashSet = new LinkedHashSet();

      for (int index = floor4; index <= floor3; index++) {
         for (int index2 = floor2; index2 <= floor; index2++) {
            linkedHashSet.add(new BlockPos(index, blockY, index2));
         }
      }

      return linkedHashSet;
   }

   private List<BlockPos> expandFootprint(Set<BlockPos> footprint){
      LinkedHashSet linkedHashSet = new LinkedHashSet();

      for (BlockPos pos2 : footprint) {
         for (Direction direction : HORIZONTAL_DIRECTIONS) {
            BlockPos pos = pos2.offset(direction);
            if (!footprint.contains(pos)) {
               linkedHashSet.add(pos.toImmutable());
            }
         }
      }

      return new ArrayList<>(linkedHashSet);
   }

   private boolean hasPlayerMoved(MinecraftClient client){
      if (Math.abs(client.player.getY() - this.lastPlayerYaw) > 0.25) {
         return true;
      } else if (!client.player.isOnGround() && client.player.getVelocity().y > 0.08) {
         return true;
      } else {
         BlockPos pos = new BlockPos(
            MathHelper.floor(client.player.getX()), this.lastPlayerBlock, MathHelper.floor(client.player.getZ())
         );
         return !this.missingTargets.contains(pos);
      }
   }

   private void clearRotationIfIdle(MinecraftClient client){
      if (this.targets.isEmpty()) {
         RotationManager.clearRotatingState();
      } else {
         for (int index = 0; index < this.targets.size(); index++) {
            int size = (this.nextTargetInde + index) % this.targets.size();
            BlockPos pos = this.targets.get(size);
            if (this.isTargetFilled(client, pos)) {
               List list = this.getEntitiesAtTarget(client, pos);
               EndCrystalEntity crystal = this.getClosestCrystal(client, list);
               if (crystal != null) {
                  if (this.crystalBreakDelayTicks == 0) {
                     this.nextTargetInde = size;
                     this.queueCrystalBreak(client, pos, crystal);
                     return;
                  }
               } else if (list.isEmpty()) {
                  SurroundModule.PlacementData var7 = this.findPlacementData(client, pos);
                  if (var7 != null && this.isHitResultInRange(client, var7.hitResult())) {
                     this.nextTargetInde = size;
                     this.queueBlockPlace(client, pos, var7);
                     return;
                  }
               }
            }
         }

         RotationManager.clearRotatingState();
      }
   }

   private void queueCrystalBreak(MinecraftClient client, BlockPos target, EndCrystalEntity crystal){
      Vec3d vec = crystal.getBoundingBox().getCenter();
      this.pendingAction = this.createPendingAction(client, SurroundModule.PendingActionType.BREAK_CRYSTAL, target, crystal, null, vec);
      this.rotateToAction();
   }

   private void queueBlockPlace(MinecraftClient client, BlockPos target, SurroundModule.PlacementData placement){
      this.pendingAction = this.createPendingAction(client, SurroundModule.PendingActionType.PLACE_BLOCK, target, null, placement, placement.hitResult().getPos());
      this.rotateToAction();
   }

   private SurroundModule.PendingAction createPendingAction(
      MinecraftClient client,
      SurroundModule.PendingActionType type,
      BlockPos target,
      EndCrystalEntity crystal,
      SurroundModule.PlacementData placement,
      Vec3d aimPosition
   ){
      Vec3d vec = client.player.getEyePos();
      float yaw = RotationUtil.getYaw(vec, aimPosition);
      float pitch = RotationUtil.getPitch(vec, aimPosition);
      return new SurroundModule.PendingAction(type, target, crystal, placement, yaw, pitch);
   }

   private boolean isRotationSent(){
      return RotationManager.wasRotationSent(this.pendingAction.yaw(), this.pendingAction.pitch(), 0.1F);
   }

   private void rotateToAction(){
      RotationManager.setRotation(this.pendingAction.yaw(), this.pendingAction.pitch(), true);
   }

   private void executePendingAction(MinecraftClient client, int obsidianSlot){
      if (this.pendingAction.type() == SurroundModule.PendingActionType.BREAK_CRYSTAL) {
         this.breakCrystal(client);
      } else {
         this.placeBlock(client, obsidianSlot);
      }
   }

   private void breakCrystal(MinecraftClient client){
      EndCrystalEntity crystal = this.pendingAction.crystal();
      BlockPos pos = this.pendingAction.target();
      if (crystal != null && !crystal.isRemoved() && crystal.isAlive() && crystal.getBoundingBox().intersects(new Box(pos))) {
         client.interactionManager.attackEntity(client.player, crystal);
         client.player.swingHand(Hand.MAIN_HAND);
         this.crystalBreakDelayTicks = 2;
         this.pendingAction = null;
      } else {
         this.pendingAction = null;
      }
   }

   private void placeBlock(MinecraftClient client, int obsidianSlot){
      BlockPos pos = this.pendingAction.target();
      if (!this.isTargetFilled(client, pos)) {
         this.markTargetFilled(pos);
      } else {
         List list = this.getEntitiesAtTarget(client, pos);
         if (!list.isEmpty()) {
            this.pendingAction = null;
         } else {
            SurroundModule.PlacementData var5 = this.findPlacementData(client, pos);
            if (var5 == null || !this.isHitResultInRange(client, var5.hitResult())) {
               this.markTargetFilled(pos);
            } else if (!var5.sameFaceAs(this.pendingAction.placement())) {
               this.queueBlockPlace(client, pos, var5);
            } else {
               ActionResult actionResult = this.selectInteractionMethod(client, var5.hitResult(), obsidianSlot);
               client.player.swingHand(Hand.MAIN_HAND);
               this.markTargetFilled(pos);
            }
         }
      }
   }

   private void markTargetFilled(BlockPos target){
      int indexOf = this.targets.indexOf(target);
      if (indexOf >= 0 && !this.targets.isEmpty()) {
         this.nextTargetInde = (indexOf + 1) % this.targets.size();
      }

      this.pendingAction = null;
   }

   private ActionResult selectInteractionMethod(MinecraftClient client, BlockHitResult hitResult, int obsidianSlot){
      return this.modeSetting.is("Legit") ? this.interactLegit(client, hitResult, obsidianSlot) : this.interactPacket(client, hitResult, obsidianSlot);
   }

   private ActionResult interactLegit(MinecraftClient client, BlockHitResult hitResult, int obsidianSlot){
      if (this.originalSlot == -1) {
         this.originalSlot = client.player.getInventory().getSelectedSlot();
      }

      client.player.getInventory().setSelectedSlot(obsidianSlot);
      this.swapDelayTicks = 1;
      return client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitResult);
   }

   private void tickSwapDelay(MinecraftClient client){
      if (this.originalSlot != -1 && this.swapDelayTicks > 0) {
         this.swapDelayTicks--;
         if (this.swapDelayTicks == 0) {
            this.restoreSelectedSlot(client);
         }
      }
   }

   private void restoreSelectedSlot(MinecraftClient client){
      if (this.originalSlot != -1) {
         if (client.player != null) {
            client.player.getInventory().setSelectedSlot(this.originalSlot);
         }

         this.originalSlot = -1;
         this.swapDelayTicks = 0;
      }
   }

   private ActionResult interactPacket(MinecraftClient client, BlockHitResult hitResult, int obsidianSlot){
      if (!this.silentPlaceSetting.getValue()) {
         return this.interactLegit(client, hitResult, obsidianSlot);
      } else {
         int selectedSlot = client.player.getInventory().getSelectedSlot();
         client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(obsidianSlot));
         client.player.getInventory().setSelectedSlot(obsidianSlot);

         ActionResult actionResult;
         try {
            actionResult = client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitResult);
         } finally {
            client.player.getInventory().setSelectedSlot(selectedSlot);
            client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(selectedSlot));
         }

         return actionResult;
      }
   }

   private void applySilentSwap(MinecraftClient client, int obsidianSlot){
      boolean value = this.modeSetting.is("Packet") && this.silentPlaceSetting.getValue();
      int selectedSlot = client.player.getInventory().getSelectedSlot();
      int index = 0;
      if (value) {
         client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(obsidianSlot));
         client.player.getInventory().setSelectedSlot(obsidianSlot);
      } else {
         if (this.originalSlot == -1) {
            this.originalSlot = selectedSlot;
         }

         client.player.getInventory().setSelectedSlot(obsidianSlot);
         this.swapDelayTicks = 1;
      }

      try {
         for (BlockPos pos : this.targets) {
            if (this.isTargetFilled(client, pos)) {
               List<Entity> list = this.getEntitiesAtTarget(client, pos);
               if (!this.hasOnlyCrystals(list)) {
                  for (Entity entity : list) {
                     if (entity instanceof EndCrystalEntity var11) {
                        this.rotateToPosition(client, var11.getBoundingBox().getCenter());
                        client.interactionManager.attackEntity(client.player, var11);
                        client.player.swingHand(Hand.MAIN_HAND);
                     }
                  }

                  SurroundModule.PlacementData var15 = this.findPlacementData(client, pos);
                  if (var15 != null && this.isHitResultInRange(client, var15.hitResult())) {
                     this.rotateToPosition(client, var15.hitResult().getPos());
                     ActionResult actionResult = client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, var15.hitResult());
                     client.player.swingHand(Hand.MAIN_HAND);
                     index++;
                  }
               }
            }
         }
      } finally {
         if (value) {
            client.player.getInventory().setSelectedSlot(selectedSlot);
            client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(selectedSlot));
         }
      }

      if (index > 0) {
         this.instantSwapDelayTicks = 2;
      }
   }

   private boolean hasOnlyCrystals(List<Entity> entities){
      for (Entity entity : entities) {
         if (!(entity instanceof EndCrystalEntity)) {
            return true;
         }
      }

      return false;
   }

   private void rotateToPosition(MinecraftClient client, Vec3d aimPosition){
      float eyePos2 = RotationUtil.getYaw(client.player.getEyePos(), aimPosition);
      float eyePos = RotationUtil.getPitch(client.player.getEyePos(), aimPosition);
      client.player.networkHandler.sendPacket(new LookAndOnGround(eyePos2, eyePos, client.player.isOnGround(), client.player.horizontalCollision));
      RotationManager.markRotationSent(eyePos2, eyePos);
   }

   private boolean isTargetFilled(MinecraftClient client, BlockPos target){
      return client.world.getBlockState(target).isReplaceable();
   }

   private boolean hasMissingTargets(MinecraftClient client){
      for (BlockPos pos : this.targets) {
         if (this.isTargetFilled(client, pos)) {
            return false;
         }
      }

      return true;
   }

   private void cancelPendingAction(MinecraftClient client){
      this.pendingAction = null;
      this.restoreSelectedSlot(client);
      RotationManager.clearRotatingState();
   }

   private List<Entity> getEntitiesAtTarget(MinecraftClient client, BlockPos target){
      return client.world
         .getOtherEntities(client.player, new Box(target), entity -> !entity.isRemoved() && !entity.isSpectator() && entity.canHit());
   }

   private EndCrystalEntity getClosestCrystal(MinecraftClient client, List<Entity> entities){
      EndCrystalEntity crystal = null;
      double squaredDistanceTo2 = Double.MAX_VALUE;

      for (Entity entity : entities) {
         if (entity instanceof EndCrystalEntity var8) {
            double squaredDistanceTo = client.player.squaredDistanceTo(var8);
            if (squaredDistanceTo < squaredDistanceTo2) {
               crystal = var8;
               squaredDistanceTo2 = squaredDistanceTo;
            }
         }
      }

      return crystal;
   }

   private SurroundModule.PlacementData findPlacementData(MinecraftClient client, BlockPos target){
      for (Direction direction2 : PLACE_FACES) {
         BlockPos pos = target.offset(direction2);
         if (!client.world.getBlockState(pos).isReplaceable()) {
            Direction direction = direction2.getOpposite();
            Vec3d vec = Vec3d.ofCenter(pos).add(direction.getOffsetX() * 0.5, direction.getOffsetY() * 0.5, direction.getOffsetZ() * 0.5);
            BlockHitResult hitResult = new BlockHitResult(vec, direction, pos, false);
            return new SurroundModule.PlacementData(hitResult);
         }
      }

      return null;
   }

   private boolean isHitResultInRange(MinecraftClient client, BlockHitResult hitResult){
      double blockInteractionRange = client.player.getBlockInteractionRange();
      return client.player.getEyePos().squaredDistanceTo(hitResult.getPos()) <= blockInteractionRange * blockInteractionRange;
   }

   private boolean canEngage(MinecraftClient client){
      return client.player != null && client.world != null && client.interactionManager != null && !client.player.isDead();
   }

   private void resetState(){
      this.pendingAction = null;
      this.targets.clear();
      this.missingTargets.clear();
      this.nextTargetInde = 0;
      this.crystalBreakDelayTicks = 0;
      this.instantSwapDelayTicks = 0;
      this.originalSlot = -1;
      this.swapDelayTicks = 0;
      this.positionSnapshotted = false;
   }

   @Environment(EnvType.CLIENT)
   private record PendingAction(
      SurroundModule.PendingActionType type, BlockPos target, EndCrystalEntity crystal, SurroundModule.PlacementData placement, float yaw, float pitch
   ){
   }

   @Environment(EnvType.CLIENT)
   private static enum PendingActionType {
      BREAK_CRYSTAL,
      PLACE_BLOCK;
   }

   @Environment(EnvType.CLIENT)
   private record PlacementData(BlockHitResult hitResult){
      private boolean sameFaceAs(SurroundModule.PlacementData other){
         return other != null
            && this.hitResult.getBlockPos().equals(other.hitResult.getBlockPos())
            && this.hitResult.getSide() == other.hitResult.getSide();
      }
   }
}

