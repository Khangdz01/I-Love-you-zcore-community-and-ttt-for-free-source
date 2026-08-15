package made4mischief.astatine.client.modules.combat;

import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.modules.player.FriendModule;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import made4mischief.astatine.client.utils.inventory.InventoryUtil;
import made4mischief.astatine.client.utils.rotation.RotationManager;
import made4mischief.astatine.client.utils.rotation.RotationUtil;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;

@Environment(EnvType.CLIENT)
public final class CrystalMacroModule extends Module {
   private static final Direction[] HORIZONTAL_DIRECTIONS = new Direction[]{
      Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
   };
   private static final Direction[] ALL_PLACEMENT_DIRECTIONS = new Direction[]{
      Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP
   };
   private static final int TARGET_TIMEOUT = 20;
   private static final int WAIT_OBSIDIAN_TICKS = 10;
   private static final Object ROTATION_STATE = new Object();
   private static CrystalMacroModule instance;
   private final NumberSetting crystalCPSSetting = this.addNumber("Crystal CPS", 20.0, 1.0, 20.0, 1.0);
   private final BooleanSetting rotateSetting = this.addBoolean("Rotate", true);
   private final BooleanSetting autoObsidianSetting = this.addBoolean("Auto Obsidian", false);
   private BlockPos crystalBasePos;
   private int targetTimeoutTicks;
   private int currentCrystalId = -1;
   private int trackedCrystalId = -1;
   private int attackDelayTicks;
   private double placeProgress;
   private int targetEntityId = -1;
   private CrystalMacroModule.PlacementData placementData;
   private BlockPos placedPos;
   private CrystalMacroModule.AutoObsidianStage autoObsidianStage = CrystalMacroModule.AutoObsidianStage.IDLE;
   private int obsidianWaitTicks;
   private int previousSlot = -1;

   public CrystalMacroModule(){
      super("CrystalMacro", Category.COMBAT, "PhÃ¡ pha lÃª Ä‘Ã£ Ä‘áº·t vÃ  há»— trá»£ Ä‘áº·t háº¯c diá»‡n tháº¡ch.");
      instance = this;
   }

   @Override
   protected void onEnable(){
      this.resetAll();
   }

   @Override
   protected void onDisable(){
      this.restoreSlot(MinecraftClient.getInstance());
      this.resetAll();
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      if (!this.isInGame(client)) {
         this.resetAll();
      } else {
         this.tickAutoObsidian(client);
         boolean startAutoObsidian = this.canStartAutoObsidian(client);
         if (!startAutoObsidian) {
            this.isSurvival(client);
         }
      }
   }

   public static void recordCrystalPlacement(PlayerEntity player, Hand hand, BlockHitResult hitResult){
      CrystalMacroModule crystalMacroModule = instance;
      if (crystalMacroModule != null && crystalMacroModule.isEnabled() && player.getStackInHand(hand).isOf(Items.END_CRYSTAL)) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.world != null) {
            BlockPos pos = hitResult.getBlockPos();
            if (isObsidianLike(client, pos)) {
               crystalMacroModule.crystalBasePos = pos.toImmutable();
               crystalMacroModule.targetTimeoutTicks = 20;
               crystalMacroModule.trackedCrystalId = -1;
               crystalMacroModule.attackDelayTicks = 0;
            }
         }
      }
   }

   public static void recordSwordAttack(PlayerEntity player, Entity target){
      CrystalMacroModule crystalMacroModule = instance;
      if (crystalMacroModule != null && crystalMacroModule.isEnabled() && crystalMacroModule.autoObsidianSetting.getValue()) {
         if (target instanceof PlayerEntity var3 && var3.isAlive() && !FriendModule.isFriend(var3)) {
            if (player.getMainHandStack().isIn(ItemTags.SWORDS)) {
               crystalMacroModule.targetEntityId = target.getId();
               crystalMacroModule.placementData = null;
               crystalMacroModule.placedPos = null;
               crystalMacroModule.autoObsidianStage = CrystalMacroModule.AutoObsidianStage.PREPARE_OBSIDIAN;
               crystalMacroModule.obsidianWaitTicks = 0;
               crystalMacroModule.previousSlot = player.getInventory().getSelectedSlot();
               crystalMacroModule.placeProgress = 0.0;
            }
         }
      }
   }

   private void tickAutoObsidian(MinecraftClient client){
      if (this.crystalBasePos != null) {
         if (--this.targetTimeoutTicks < 0) {
            this.clearTarget();
         } else {
            EndCrystalEntity crystal = this.findCrystalAt(client, this.crystalBasePos);
            if (crystal != null) {
               if (!this.isEntityInReach(client, crystal)) {
                  this.currentCrystalId = -1;
                  RotationManager.clearRotatingState(ROTATION_STATE);
               } else if (this.rotateSetting.getValue()) {
                  Vec3d vec = client.player.getEyePos();
                  Vec3d vec2 = crystal.getBoundingBox().getCenter();
                  float yaw = RotationUtil.getYaw(vec, vec2);
                  float pitch = RotationUtil.getPitch(vec, vec2);
                  RotationManager.setRotation(ROTATION_STATE, yaw, pitch, true, true);
                  if (this.currentCrystalId != crystal.getId()) {
                     this.currentCrystalId = crystal.getId();
                  } else if (RotationManager.wasRotationSent(yaw, pitch, 1.0F)) {
                     client.interactionManager.attackEntity(client.player, crystal);
                     client.player.swingHand(Hand.MAIN_HAND);
                     this.clearTarget();
                  }
               } else {
                  RotationManager.clearRotatingState(ROTATION_STATE);
                  if (this.attackDelayTicks > 0) {
                     this.attackDelayTicks--;
                  } else {
                     this.isCrystalAt(client, crystal);
                     this.trackedCrystalId = crystal.getId();
                     this.attackDelayTicks = 1;
                  }
               }
            } else {
               if (this.trackedCrystalId != -1) {
                  Entity entity = client.world.getEntityById(this.trackedCrystalId);
                  if (!(entity instanceof EndCrystalEntity) || entity.isRemoved() || !entity.isAlive()) {
                     this.clearTarget();
                  }
               }

               this.currentCrystalId = -1;
               RotationManager.clearRotatingState(ROTATION_STATE);
            }
         }
      }
   }

   private void isCrystalAt(MinecraftClient client, EndCrystalEntity crystal){
      Vec3d vec2 = crystal.getBoundingBox().getCenter();
      Vec3d vec = client.player.getEyePos();
      float yaw = RotationUtil.getYaw(vec, vec2);
      float pitch = RotationUtil.getPitch(vec, vec2);
      client.player.networkHandler.sendPacket(new LookAndOnGround(yaw, pitch, client.player.isOnGround(), client.player.horizontalCollision));
      RotationManager.markRotationSent(yaw, pitch);
      client.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, client.player.isSneaking()));
      client.player.swingHand(Hand.MAIN_HAND);
   }

   private void isSurvival(MinecraftClient client){
      if (client.currentScreen == null && client.options.useKey.isPressed()) {
         Hand hand = this.isHoldingCrystal(client);
         if (hand == null) {
            this.placeProgress = 0.0;
         } else {
            BlockHitResult hitResult = this.getCrosshairHit(client);
            if (hitResult != null) {
               this.placeProgress = this.placeProgress + this.crystalCPSSetting.getValue() / 20.0;
               if (!(this.placeProgress < 1.0)) {
                  this.placeProgress--;
                  client.interactionManager.interactBlock(client.player, hand, hitResult);
                  client.player.swingHand(hand);
               }
            }
         }
      } else {
         this.placeProgress = 0.0;
      }
   }

   private Hand isHoldingCrystal(MinecraftClient client){
      if (client.player.getMainHandStack().isOf(Items.END_CRYSTAL)) {
         return Hand.MAIN_HAND;
      } else {
         return client.player.getOffHandStack().isOf(Items.END_CRYSTAL) ? Hand.OFF_HAND : null;
      }
   }

   private BlockHitResult getCrosshairHit(MinecraftClient client){
      if (client.crosshairTarget instanceof BlockHitResult var2) {
         BlockPos pos = var2.getBlockPos();
         if (!isObsidianLike(client, pos)) {
            return null;
         } else {
            BlockHitResult hitResult = placementPoint(pos);
            return this.isHitInReach(client, hitResult) ? hitResult : null;
         }
      } else {
         return null;
      }
   }

   private boolean canStartAutoObsidian(MinecraftClient client){
      if (this.autoObsidianStage == CrystalMacroModule.AutoObsidianStage.IDLE) {
         return false;
      } else if (!this.autoObsidianSetting.getValue()) {
         this.stopAutoObsidian(client);
         return true;
      } else if (this.autoObsidianStage == CrystalMacroModule.AutoObsidianStage.PREPARE_OBSIDIAN) {
         if (client.world.getEntityById(this.targetEntityId) instanceof PlayerEntity var10
            && var10.isAlive()
            && !FriendModule.isFriend(var10)
            && InventoryUtil.findHotBarItem(client, Items.OBSIDIAN) != -1
            && InventoryUtil.findHotBarItem(client, Items.END_CRYSTAL) != -1) {
            this.placementData = this.findPlacementForTarget(client, var10);
            if (this.placementData == null) {
               this.stopAutoObsidian(client);
               return true;
            } else {
               this.rotateToPlacement(client, this.placementData);
               this.autoObsidianStage = CrystalMacroModule.AutoObsidianStage.PLACE_OBSIDIAN;
               return true;
            }
         } else {
            this.stopAutoObsidian(client);
            return true;
         }
      } else if (this.autoObsidianStage == CrystalMacroModule.AutoObsidianStage.PLACE_OBSIDIAN) {
         if (client.world.getEntityById(this.targetEntityId) instanceof PlayerEntity var9
            && var9.isAlive()
            && !FriendModule.isFriend(var9)
            && this.placementData != null
            && this.isPlacementValid(client, var9, this.placementData)) {
            this.placementData = this.findPlacementAt(client, this.placementData.target());
            if (this.placementData == null) {
               this.stopAutoObsidian(client);
               return true;
            } else {
               int findHotBarItem2 = InventoryUtil.findHotBarItem(client, Items.OBSIDIAN);
               if (findHotBarItem2 == -1) {
                  this.stopAutoObsidian(client);
                  return true;
               } else {
                  this.rotateToPlacement(client, this.placementData);
                  this.switchSlot(client, findHotBarItem2);
                  client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, this.placementData.hitResult());
                  client.player.swingHand(Hand.MAIN_HAND);
                  this.placedPos = this.placementData.target();
                  this.autoObsidianStage = CrystalMacroModule.AutoObsidianStage.WAIT_FOR_OBSIDIAN;
                  this.obsidianWaitTicks = 10;
                  return true;
               }
            }
         } else {
            this.stopAutoObsidian(client);
            return true;
         }
      } else if (this.autoObsidianStage == CrystalMacroModule.AutoObsidianStage.WAIT_FOR_OBSIDIAN) {
         if (this.placedPos == null) {
            this.stopAutoObsidian(client);
            return true;
         } else if (!client.world.getBlockState(this.placedPos).isOf(Blocks.OBSIDIAN)) {
            if (--this.obsidianWaitTicks <= 0) {
               this.stopAutoObsidian(client);
            }

            return true;
         } else {
            BlockHitResult hitResult = placementPoint(this.placedPos);
            if (!this.isPlacementPathClear(client, this.placedPos, hitResult)) {
               this.stopAutoObsidian(client);
               return true;
            } else {
               int findHotBarItem = InventoryUtil.findHotBarItem(client, Items.END_CRYSTAL);
               if (findHotBarItem == -1) {
                  this.stopAutoObsidian(client);
                  return true;
               } else {
                  this.rotate(client, hitResult.getPos());
                  this.switchSlot(client, findHotBarItem);
                  this.autoObsidianStage = CrystalMacroModule.AutoObsidianStage.PLACE_CRYSTAL;
                  return true;
               }
            }
         }
      } else if (this.autoObsidianStage == CrystalMacroModule.AutoObsidianStage.PLACE_CRYSTAL) {
         BlockHitResult hitResult2 = this.placedPos == null ? null : placementPoint(this.placedPos);
         if (hitResult2 != null && this.isPlacementPathClear(client, this.placedPos, hitResult2)) {
            int findHotBarItem3 = InventoryUtil.findHotBarItem(client, Items.END_CRYSTAL);
            if (findHotBarItem3 == -1) {
               this.stopAutoObsidian(client);
               return true;
            } else {
               this.rotate(client, hitResult2.getPos());
               this.switchSlot(client, findHotBarItem3);
               client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitResult2);
               client.player.swingHand(Hand.MAIN_HAND);
               this.crystalBasePos = this.placedPos.toImmutable();
               this.targetTimeoutTicks = 20;
               this.resetRotationState();
               return true;
            }
         } else {
            this.stopAutoObsidian(client);
            return true;
         }
      } else {
         return true;
      }
   }

   private CrystalMacroModule.PlacementData findPlacementForTarget(MinecraftClient client, LivingEntity target){
      BlockPos pos2 = target.getBlockPos();
      CrystalMacroModule.PlacementData var4 = null;
      double pos4 = Double.MAX_VALUE;

      for (Direction direction : HORIZONTAL_DIRECTIONS) {
         BlockPos pos3 = pos2.offset(direction);
         CrystalMacroModule.PlacementData var12 = this.findPlacementAt(client, pos3);
         if (var12 != null) {
            double pos = client.player.getEyePos().squaredDistanceTo(var12.hitResult().getPos());
            if (pos < pos4) {
               var4 = var12;
               pos4 = pos;
            }
         }
      }

      return var4;
   }

   private CrystalMacroModule.PlacementData findPlacementAt(MinecraftClient client, BlockPos target){
      if (!client.world.getBlockState(target).isReplaceable()) {
         return null;
      } else if (client.world.getBlockState(target.up()).isReplaceable()
         && client.world.getBlockState(target.up(2)).isReplaceable()
         && !this.isBlockFree(client, target)
         && !this.isBlockFree(client, target.up())) {
         for (Direction direction2 : ALL_PLACEMENT_DIRECTIONS) {
            BlockPos pos = target.offset(direction2);
            if (!client.world.getBlockState(pos).isReplaceable()) {
               Direction direction = direction2.getOpposite();
               Vec3d vec = Vec3d.ofCenter(pos).add(direction.getOffsetX() * 0.5, direction.getOffsetY() * 0.5, direction.getOffsetZ() * 0.5);
               BlockHitResult hitResult = new BlockHitResult(vec, direction, pos, false);
               if (this.isHitInReach(client, hitResult)) {
                  return new CrystalMacroModule.PlacementData(target.toImmutable(), hitResult);
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private boolean isPlacementValid(MinecraftClient client, LivingEntity target, CrystalMacroModule.PlacementData placement){
      BlockPos pos2 = target.getBlockPos();
      BlockPos pos = placement.target();
      int z = Math.abs(pos.getX() - pos2.getX()) + Math.abs(pos.getZ() - pos2.getZ());
      return pos.getY() == pos2.getY() && z == 1 && this.findPlacementAt(client, pos) != null;
   }

   private void rotateToPlacement(MinecraftClient client, CrystalMacroModule.PlacementData placement){
      this.rotate(client, placement.hitResult().getPos());
   }

   private void rotate(MinecraftClient client, Vec3d point){
      Vec3d vec = client.player.getEyePos();
      client.player.setYaw(RotationUtil.getYaw(vec, point));
      client.player.setPitch(RotationUtil.getPitch(vec, point));
   }

   private void switchSlot(MinecraftClient client, int slot){
      if (slot >= 0 && slot <= 8 && client.player.getInventory().getSelectedSlot() != slot) {
         client.player.getInventory().setSelectedSlot(slot);
         client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
      }
   }

   private void restoreSlot(MinecraftClient client){
      if (this.previousSlot != -1 && client.player != null) {
         this.switchSlot(client, this.previousSlot);
         this.previousSlot = -1;
      }
   }

   private static BlockHitResult placementPoint(BlockPos base){
      Vec3d vec = new Vec3d(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5);
      return new BlockHitResult(vec, Direction.UP, base, false);
   }

   private boolean isPlacementPathClear(MinecraftClient client, BlockPos base, BlockHitResult hitResult){
      return isObsidianLike(client, base)
         && client.world.getBlockState(base.up()).isReplaceable()
         && client.world.getBlockState(base.up(2)).isReplaceable()
         && !this.isBlockFree(client, base.up())
         && this.isHitInReach(client, hitResult);
   }

   private EndCrystalEntity findCrystalAt(MinecraftClient client, BlockPos base){
      Box Box = new Box(base.up()).expand(0.75, 1.0, 0.75);
      EndCrystalEntity crystal2 = null;
      double squaredDistanceTo2 = Double.MAX_VALUE;

      for (EndCrystalEntity crystal : client.world.getEntitiesByClass(EndCrystalEntity.class, Box, entity -> entity.isAlive() && !entity.isRemoved())) {
         if (crystal.getBlockPos().down().equals(base)) {
            double squaredDistanceTo = client.player.squaredDistanceTo(crystal);
            if (squaredDistanceTo < squaredDistanceTo2) {
               crystal2 = crystal;
               squaredDistanceTo2 = squaredDistanceTo;
            }
         }
      }

      return crystal2;
   }

   private boolean isBlockFree(MinecraftClient client, BlockPos pos){
      return !client.world
         .getOtherEntities(null, new Box(pos), entity -> !entity.isRemoved() && !entity.isSpectator() && entity.canHit())
         .isEmpty();
   }

   private boolean isHitInReach(MinecraftClient client, BlockHitResult hitResult){
      double blockInteractionRange = client.player.getBlockInteractionRange();
      return client.player.getEyePos().squaredDistanceTo(hitResult.getPos()) <= blockInteractionRange * blockInteractionRange;
   }

   private boolean isEntityInReach(MinecraftClient client, Entity entity){
      double entityInteractionRange = client.player.getEntityInteractionRange();
      return client.player.getEyePos().squaredDistanceTo(entity.getBoundingBox().getCenter()) <= entityInteractionRange * entityInteractionRange;
   }

   private static boolean isObsidianLike(MinecraftClient client, BlockPos pos){
      return client.world.getBlockState(pos).isOf(Blocks.OBSIDIAN) || client.world.getBlockState(pos).isOf(Blocks.BEDROCK);
   }

   private boolean isInGame(MinecraftClient client){
      return client.player != null && client.world != null && client.interactionManager != null && !client.player.isDead();
   }

   private void clearTarget(){
      this.crystalBasePos = null;
      this.targetTimeoutTicks = 0;
      this.currentCrystalId = -1;
      this.trackedCrystalId = -1;
      this.attackDelayTicks = 0;
      RotationManager.clearRotatingState(ROTATION_STATE);
   }

   private void resetPlacementState(){
      this.targetEntityId = -1;
      this.placementData = null;
      this.placedPos = null;
      this.autoObsidianStage = CrystalMacroModule.AutoObsidianStage.IDLE;
      this.obsidianWaitTicks = 0;
   }

   private void stopAutoObsidian(MinecraftClient client){
      this.resetPlacementState();
      this.restoreSlot(client);
   }

   private void resetRotationState(){
      this.resetPlacementState();
      this.previousSlot = -1;
   }

   private void clearPending(){
      this.clearTarget();
      this.resetPlacementState();
      this.placeProgress = 0.0;
   }

   private void resetAll(){
      this.clearPending();
      this.previousSlot = -1;
   }

   @Environment(EnvType.CLIENT)
   private static enum AutoObsidianStage {
      IDLE,
      PREPARE_OBSIDIAN,
      PLACE_OBSIDIAN,
      WAIT_FOR_OBSIDIAN,
      PLACE_CRYSTAL;
   }

   @Environment(EnvType.CLIENT)
   private record PlacementData(BlockPos target, BlockHitResult hitResult){
   }
}

