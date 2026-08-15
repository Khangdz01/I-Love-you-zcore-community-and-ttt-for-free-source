package made4mischief.astatine.client.modules.movement;

import made4mischief.astatine.client.mixin.ClientWorldAccessor;
import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.ModeSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import made4mischief.astatine.client.utils.inventory.InventoryUtil;
import made4mischief.astatine.client.utils.inventory.SilentSlotManager;
import made4mischief.astatine.client.utils.rotation.RotationManager;
import made4mischief.astatine.client.utils.rotation.RotationUtil;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Hand;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.MathHelper;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Direction.Axis;

@Environment(EnvType.CLIENT)
public final class TridentMoveModule extends Module {
   private static final Object ROTATION_STATE = new Object();
   private static final float CLUTCH_PITCH = 90.0F;
   private static final float ROTATION_SNAP = 1.0F;
   private static final int TICK_TIMEOUT = 10;
   private static final int CLUTCH_TICKS = 14;
   private static final double BUCKET_RANGE_PAD = 0.35;
   private static TridentMoveModule instance;
   private final ModeSetting swapModeSetting = this.addMode("Swap Mode", "Hotbar", new String[]{"Hotbar", "Offhand"});
   private final NumberSetting riseThresholdSetting = this.addNumber("Rise Threshold", 0.12, 0.03, 0.6, 0.01);
   private final BooleanSetting fallClutchSetting = this.addBoolean("Fall Clutch", true);
   private final NumberSetting minFallDistanceSetting = this.addNumber("Min Fall Distance", 3.0, 1.0, 12.0, 0.25);
   private final NumberSetting clutchRangeSetting = this.addNumber("Clutch Range", 4.25, 2.0, 5.0, 0.05);
   private final BooleanSetting movementFixSetting = this.addBoolean("Movement Fi", true);
   private final BooleanSetting pauseInGUISetting = this.addBoolean("Pause In GUI", true);
   private final Mutable cursorPos = new Mutable();
   private TridentMoveModule.Stage stage = TridentMoveModule.Stage.IDLE;
   private int itemSlot = -1;
   private int tridentSlot = -1;
   private int backupItemSlot = -1;
   private Hand itemHand = Hand.MAIN_HAND;
   private int countdownTicks;
   private float targetYaw;
   private float targetPitch = 90.0F;
   private boolean riptideSelected;
   private boolean restorePending;
   private boolean waitingForRotation;
   private long rotationTick;
   private long lastRotationTick;
   private BlockPos targetPos;

   public TridentMoveModule(){
      super("TridentMove", Category.MOVEMENT, "Tá»± dÃ¹ng nÆ°á»›c Ä‘á»ƒ há»— trá»£ Ä‘inh ba Riptide.", -1, true);
      this.minFallDistanceSetting.visibleWhen(this.fallClutchSetting::getValue);
      this.clutchRangeSetting.visibleWhen(this.fallClutchSetting::getValue);
      instance = this;
   }

   @Override
   protected void onEnable(){
      this.restorePending = false;
      this.backupItemSlot = -1;
      this.resetFullState();
   }

   @Override
   protected void onDisable(){
      this.restoreHand(MinecraftClient.getInstance());
      this.resetFullState();
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      if (!isInGame(client)) {
         this.resetFullState();
      } else {
         ClientPlayerEntity player = client.player;
         boolean mainHandStack = isRiptideTrident(player.getMainHandStack());
         this.rotationTick++;

         try {
            if (!this.pauseInGUISetting.getValue() || client.currentScreen == null) {
               if (this.waitingForRotation) {
                  this.lastRotationTick = this.rotationTick;
                  this.waitingForRotation = false;
               }

               if (!this.swapModeSetting.is("Offhand") && this.restorePending) {
                  this.restoreHand(client);
               }

               if (this.stage == TridentMoveModule.Stage.IDLE) {
                  this.tickFallClutch(client, mainHandStack);
               }

               switch (this.stage) {
                  case IDLE:
                  default:
                     return;
                  case ROTATE_INITIAL_PLACE:
                     this.tickWaterClutch(client);
                     return;
                  case WAIT_INITIAL_PLACE:
                     this.tickRiptideMove(client);
                     return;
                  case ARMED:
                     this.tickRiseMove(client);
                     return;
                  case POST_LAUNCH:
                     this.tickRiptideSequence(client);
                     return;
                  case ROTATE_PICKUP:
                     this.tickAirborne(client);
                     return;
                  case WAIT_PICKUP:
                     this.tickGroundReset(client);
                     return;
                  case AIRBORNE:
                     this.tickIdle(client);
                     return;
                  case ROTATE_CLUTCH:
                     this.tickFallCheck(client);
                     return;
                  case WAIT_CLUTCH:
                     this.tickPlaceWater(client);
                     return;
                  case WAIT_LANDING:
                     this.tickReplaceWater(client);
                     return;
               }
            }

            RotationManager.clearRotatingState(ROTATION_STATE);
            this.waitingForRotation = true;
         } finally {
            this.riptideSelected = mainHandStack;
         }
      }
   }

   private void tickFallClutch(MinecraftClient client, boolean riptideSelected){
      if (this.shouldFallClutch(client)) {
         this.setStageAndLook(TridentMoveModule.Stage.ROTATE_CLUTCH, client.player.getYaw());
      } else if (riptideSelected && !this.riptideSelected) {
         if (!this.swapModeSetting.is("Offhand") || this.selectWaterBucket(client)) {
            if (client.player.isTouchingWater()) {
               this.tridentSlot = client.player.getInventory().getSelectedSlot();
               this.targetPos = null;
               this.stage = TridentMoveModule.Stage.ARMED;
            } else if (this.selectWaterBucket(client)) {
               this.tridentSlot = client.player.getInventory().getSelectedSlot();
               this.targetPos = client.player.getBlockPos().toImmutable();
               this.setStageAndLook(TridentMoveModule.Stage.ROTATE_INITIAL_PLACE, client.player.getYaw());
            }
         }
      }
   }

   private void tickWaterClutch(MinecraftClient client){
      if (this.isTridentSelected(client) && this.isItemInHand(client, Items.WATER_BUCKET)) {
         this.rotateToTarget();
         if (!this.hasRotationSettled()) {
            if (--this.countdownTicks <= 0) {
               this.resetToIdle();
            }
         } else {
            if (this.isHandReady(client)) {
               this.clearRotation();
               this.countdownTicks = 14;
               this.stage = TridentMoveModule.Stage.WAIT_INITIAL_PLACE;
            } else if (--this.countdownTicks <= 0) {
               this.resetToIdle();
            }
         }
      } else {
         this.resetToIdle();
      }
   }

   private void tickRiptideMove(MinecraftClient client){
      if (this.isItemInHand(client, Items.BUCKET)) {
         BlockPos pos = this.findValidBlockPos(client, this.targetPos);
         if (pos != null) {
            this.targetPos = pos;
         }

         this.stage = TridentMoveModule.Stage.ARMED;
         this.countdownTicks = 0;
      } else {
         if (--this.countdownTicks <= 0) {
            this.resetToIdle();
         }
      }
   }

   private void tickRiseMove(MinecraftClient client){
      ClientPlayerEntity player = client.player;
      if (player.getVelocity().y > this.riseThresholdSetting.getValue() && (player.isUsingRiptide() || !player.isOnGround())) {
         if (this.targetPos == null || !this.selectBucket(client, false)) {
            this.stage = TridentMoveModule.Stage.AIRBORNE;
            return;
         }

         this.updateLookTargetE(player);
         this.countdownTicks = 14;
         if (this.isInBucketRange(client) && this.useItem(client)) {
            this.stage = TridentMoveModule.Stage.WAIT_PICKUP;
         } else {
            this.stage = TridentMoveModule.Stage.ROTATE_PICKUP;
         }
      }
   }

   public static void onTridentReleased(ClientPlayerEntity player, ItemStack trident, int remainingUseTicks){
      TridentMoveModule tridentMoveModule = instance;
      int maxUseTime = trident.getMaxUseTime(player) - remainingUseTicks;
      if (tridentMoveModule != null && tridentMoveModule.isEnabled() && tridentMoveModule.stage == TridentMoveModule.Stage.ARMED && player != null && isRiptideTrident(trident) && maxUseTime >= 10) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (isInGame(client) && client.player == player && tridentMoveModule.targetPos != null) {
            if (tridentMoveModule.selectBucket(client, false)) {
               tridentMoveModule.updateLookTarget(player);
               if (tridentMoveModule.useItem(client)) {
                  tridentMoveModule.countdownTicks = 14;
                  tridentMoveModule.stage = TridentMoveModule.Stage.WAIT_PICKUP;
                  tridentMoveModule.clearRotation();
                  return;
               }
            }

            tridentMoveModule.countdownTicks = 10;
            tridentMoveModule.stage = TridentMoveModule.Stage.POST_LAUNCH;
            tridentMoveModule.clearRotation();
         }
      }
   }

   private void tickAirborne(MinecraftClient client){
      if (this.targetPos == null) {
         this.clearRotation();
         this.stage = TridentMoveModule.Stage.AIRBORNE;
      } else if (this.itemHand != Hand.OFF_HAND && !this.isItemInHand(client, Items.BUCKET)) {
         this.clearRotation();
         this.stage = TridentMoveModule.Stage.AIRBORNE;
      } else if (!this.isInBucketRange(client)) {
         this.clearRotation();
         this.stage = TridentMoveModule.Stage.AIRBORNE;
      } else {
         this.rotateToTarget();
         if (!this.hasRotationSettled()) {
            if (--this.countdownTicks <= 0) {
               this.clearRotation();
               this.stage = TridentMoveModule.Stage.AIRBORNE;
            }
         } else {
            if (this.useItem(client)) {
               this.clearRotation();
               this.countdownTicks = 14;
               this.stage = TridentMoveModule.Stage.WAIT_PICKUP;
            } else if (--this.countdownTicks <= 0) {
               this.clearRotation();
               this.stage = TridentMoveModule.Stage.AIRBORNE;
            }
         }
      }
   }

   private void tickGroundReset(MinecraftClient client){
      if (this.isItemInHand(client, Items.WATER_BUCKET)) {
         this.clearRotation();
         this.targetPos = null;
         this.countdownTicks = 0;
         this.stage = TridentMoveModule.Stage.AIRBORNE;
      } else {
         if (this.targetPos == null || !this.isInBucketRange(client)) {
            this.rotateToTarget();
         } else if (this.itemHand != Hand.OFF_HAND && !this.isItemInHand(client, Items.BUCKET)) {
            this.rotateToTarget();
         } else {
            this.updateLookTargetE(client.player);
            if ((this.countdownTicks & 1) == 0) {
               this.useItem(client);
            }

            this.clearRotation();
         }

         if (--this.countdownTicks <= 0) {
            this.clearRotation();
            this.targetPos = null;
            this.stage = TridentMoveModule.Stage.AIRBORNE;
         }
      }
   }

   private void tickRiptideSequence(MinecraftClient client){
      ClientPlayerEntity player = client.player;
      if (player.isOnGround() || player.getVelocity().y <= 0.08) {
         if (--this.countdownTicks <= 0) {
            this.targetPos = null;
            this.clearRotation();
            this.stage = TridentMoveModule.Stage.AIRBORNE;
         }
      } else if (this.targetPos == null) {
         this.clearRotation();
         this.stage = TridentMoveModule.Stage.AIRBORNE;
      } else if (!this.selectBucket(client, false)) {
         this.targetPos = null;
         this.clearRotation();
         this.stage = TridentMoveModule.Stage.AIRBORNE;
      } else if (!this.isInBucketRange(client)) {
         this.clearRotation();
         this.stage = TridentMoveModule.Stage.AIRBORNE;
      } else {
         this.updateLookTarget(player);
         this.useItem(client);
         this.clearRotation();
         this.countdownTicks = 14;
         this.stage = TridentMoveModule.Stage.WAIT_PICKUP;
      }
   }

   private void updateLookTarget(ClientPlayerEntity player){
      Vec3d vec = Vec3d.ofCenter(this.targetPos);
      this.targetYaw = RotationUtil.getYaw(player.getEyePos(), vec);
      this.targetPitch = RotationUtil.getPitch(player.getEyePos(), vec);
   }

   private void tickIdle(MinecraftClient client){
      if (client.player.isOnGround()) {
         this.resetToIdle();
      } else {
         if (this.shouldFallClutch(client)) {
            this.setStageAndLook(TridentMoveModule.Stage.ROTATE_CLUTCH, client.player.getYaw());
         }
      }
   }

   private void tickFallCheck(MinecraftClient client){
      ClientPlayerEntity player = client.player;
      if (this.fallClutchSetting.getValue() && this.isItemInHand(client, Items.WATER_BUCKET)) {
         TridentMoveModule.GroundTarget var3 = this.findGroundTarget(client, this.clutchRangeSetting.getValue() + 7.0);
         if (var3 == null) {
            if (!player.isOnGround() && --this.countdownTicks > 0) {
               this.rotateToTarget();
            } else {
               this.resetToIdle();
            }
         } else {
            this.rotateToTarget();
            double y = player.getEyeY() - player.getY();
            double blockInteractionRange = Math.max(0.0, player.getBlockInteractionRange() - y - 0.15);
            double value = Math.min(this.clutchRangeSetting.getValue(), blockInteractionRange);
            if (this.hasRotationSettled() && !(var3.distance() > value)) {
               if (this.isHandReady(client)) {
                  this.targetPos = var3.waterPosition();
                  this.clearRotation();
                  this.countdownTicks = 14;
                  this.stage = TridentMoveModule.Stage.WAIT_CLUTCH;
               } else if (--this.countdownTicks <= 0) {
                  this.resetToIdle();
               }
            } else {
               if (player.isOnGround() || --this.countdownTicks <= 0) {
                  this.resetToIdle();
               }
            }
         }
      } else {
         this.resetToIdle();
      }
   }

   private void tickPlaceWater(MinecraftClient client){
      if (this.isItemInHand(client, Items.BUCKET)) {
         BlockPos pos = this.findValidBlockPos(client, this.targetPos);
         if (pos != null) {
            this.targetPos = pos;
         }

         this.countdownTicks = 14;
         this.stage = TridentMoveModule.Stage.WAIT_LANDING;
      } else {
         if (--this.countdownTicks <= 0) {
            this.resetToIdle();
         }
      }
   }

   private void tickReplaceWater(MinecraftClient client){
      ClientPlayerEntity player = client.player;
      if (this.isItemInHand(client, Items.BUCKET) && this.targetPos != null) {
         BlockPos pos = this.findValidBlockPos(client, this.targetPos);
         if (pos != null) {
            this.targetPos = pos;
         }

         boolean onGround = player.isTouchingWater() || player.isOnGround() || player.fallDistance <= 0.1F;
         if (!onGround) {
            if (--this.countdownTicks <= 0) {
               this.resetToIdle();
            }
         } else {
            this.updateLookTargetE(player);
            this.countdownTicks = 14;
            if (this.isInBucketRange(client) && this.useItem(client)) {
               this.clearRotation();
               this.stage = TridentMoveModule.Stage.WAIT_PICKUP;
            } else {
               this.stage = TridentMoveModule.Stage.ROTATE_PICKUP;
            }
         }
      } else {
         this.resetToIdle();
      }
   }

   private boolean shouldFallClutch(MinecraftClient client){
      ClientPlayerEntity player = client.player;
      if (!this.fallClutchSetting.getValue() || player.isOnGround() || player.getVelocity().y >= -0.08 || player.fallDistance < this.minFallDistanceSetting.getValue()) {
         return false;
      } else if (!this.selectWaterBucket(client)) {
         return false;
      } else {
         TridentMoveModule.GroundTarget var3 = this.findGroundTarget(client, this.clutchRangeSetting.getValue() + 7.0);
         return var3 != null;
      }
   }

   private TridentMoveModule.GroundTarget findGroundTarget(MinecraftClient client, double maxDistance){
      ClientPlayerEntity player = client.player;
      int x = MathHelper.floor(player.getX());
      int z = MathHelper.floor(player.getZ());
      int y3 = MathHelper.floor(player.getY()) - 1;
      int y2 = MathHelper.floor(player.getY() - maxDistance) - 1;

      for (int index = y3; index >= y2; index--) {
         this.cursorPos.set(x, index, z);
         VoxelShape voxelShape = client.world.getBlockState(this.cursorPos).getCollisionShape(client.world, this.cursorPos);
         if (!voxelShape.isEmpty()) {
            double max = index + voxelShape.getMax(Axis.Y);
            double y = player.getY() - max;
            if (y >= 0.0 && y <= maxDistance) {
               return new TridentMoveModule.GroundTarget(y, new BlockPos(x, index + 1, z));
            }
         }
      }

      return null;
   }

   private BlockPos findValidBlockPos(MinecraftClient client, BlockPos origin){
      if (origin == null) {
         return null;
      } else {
         BlockPos pos = null;
         double squaredDistance2 = Double.MAX_VALUE;

         for (int index3 = origin.getY() - 1; index3 <= origin.getY() + 1; index3++) {
            for (int index2 = origin.getX() - 1; index2 <= origin.getX() + 1; index2++) {
               for (int index = origin.getZ() - 1; index <= origin.getZ() + 1; index++) {
                  this.cursorPos.set(index2, index3, index);
                  FluidState fluidState = client.world.getFluidState(this.cursorPos);
                  if (fluidState.isIn(FluidTags.WATER) && fluidState.isStill()) {
                     double squaredDistance = this.cursorPos.getSquaredDistance(origin);
                     if (squaredDistance < squaredDistance2) {
                        pos = this.cursorPos.toImmutable();
                        squaredDistance2 = squaredDistance;
                     }
                  }
               }
            }
         }

         return pos;
      }
   }

   private void updateLookTargetE(ClientPlayerEntity player){
      Vec3d vec = Vec3d.ofCenter(this.targetPos);
      this.targetYaw = RotationUtil.getYaw(player.getEyePos(), vec);
      this.targetPitch = RotationUtil.getPitch(player.getEyePos(), vec);
      this.lastRotationTick = this.rotationTick;
      this.rotateToTarget();
   }

   private boolean isInBucketRange(MinecraftClient client){
      double blockInteractionRange = client.player.getBlockInteractionRange() + 0.35;
      return client.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(this.targetPos)) <= blockInteractionRange * blockInteractionRange;
   }

   private void setStageAndLook(TridentMoveModule.Stage nextStage, float yaw){
      this.targetYaw = yaw;
      this.targetPitch = 90.0F;
      this.countdownTicks = 14;
      this.stage = nextStage;
      this.lastRotationTick = this.rotationTick;
      this.rotateToTarget();
   }

   private boolean hasRotationSettled(){
      return this.rotationTick > this.lastRotationTick && RotationManager.wasRotationSent(this.targetYaw, this.targetPitch, 1.0F);
   }

   private void rotateToTarget(){
      RotationManager.setRotation(ROTATION_STATE, this.targetYaw, this.targetPitch, true, this.movementFixSetting.getValue());
   }

   private void clearRotation(){
      RotationManager.clearRotatingState(ROTATION_STATE);
   }

   private boolean selectWaterBucket(MinecraftClient client){
      return this.prepareItemHand(client, Items.WATER_BUCKET, false);
   }

   private boolean selectBucket(MinecraftClient client, boolean selectMainHand){
      return this.prepareItemHand(client, Items.BUCKET, selectMainHand);
   }

   private boolean prepareItemHand(MinecraftClient client, Item item, boolean selectMainHand){
      if (this.swapModeSetting.is("Offhand")) {
         this.itemHand = Hand.OFF_HAND;
         this.itemSlot = -1;
         return this.isItemAvailable(client, item);
      } else {
         this.itemHand = Hand.MAIN_HAND;
         this.itemSlot = InventoryUtil.findHotBarItem(client, item);
         if (this.itemSlot == -1) {
            return false;
         } else {
            if (selectMainHand) {
               this.selectSlot(client, this.itemSlot);
            }

            return true;
         }
      }
   }

   private boolean isItemAvailable(MinecraftClient client, Item item){
      if (client.player.getOffHandStack().isOf(item)) {
         return true;
      } else if (!this.restorePending && client.interactionManager != null && client.player.currentScreenHandler == client.player.playerScreenHandler) {
         int findHotBarItem = InventoryUtil.findHotBarItem(client, item);
         if (findHotBarItem == -1) {
            return false;
         } else {
            client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId, 36 + findHotBarItem, 40, SlotActionType.SWAP, client.player);
            if (!client.player.getOffHandStack().isOf(item)) {
               return false;
            } else {
               this.restorePending = true;
               this.backupItemSlot = findHotBarItem;
               return true;
            }
         }
      } else {
         return false;
      }
   }

   private void restoreHand(MinecraftClient client){
      if (this.restorePending) {
         if (client.player != null
            && client.interactionManager != null
            && this.backupItemSlot >= 0
            && this.backupItemSlot < 9
            && client.player.currentScreenHandler == client.player.playerScreenHandler) {
            client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId, 36 + this.backupItemSlot, 40, SlotActionType.SWAP, client.player);
            this.restorePending = false;
            this.backupItemSlot = -1;
         }
      }
   }

   private boolean isItemInHand(MinecraftClient client, Item item){
      return this.itemHand == Hand.OFF_HAND ? client.player.getOffHandStack().isOf(item) : isItemInSlot(client, this.itemSlot, item);
   }

   private boolean isHandReady(MinecraftClient client){
      if (this.itemHand == Hand.OFF_HAND) {
         if (client.player.getOffHandStack().isEmpty()) {
            return false;
         } else {
            this.useItemPacket(client, Hand.OFF_HAND);
            return true;
         }
      } else {
         return SilentSlotManager.runWithSlot(client, this.itemSlot, () -> this.useItemPacket(client, Hand.MAIN_HAND));
      }
   }

   private boolean useItem(MinecraftClient client){
      if (this.itemHand == Hand.OFF_HAND) {
         this.useItemPacket(client, Hand.OFF_HAND);
         return true;
      } else {
         return !this.isItemInHand(client, Items.BUCKET)
            ? false
            : SilentSlotManager.runWithSlot(client, this.itemSlot, () -> this.useItemPacket(client, Hand.MAIN_HAND));
      }
   }

   private void useItemPacket(MinecraftClient client, Hand hand){
      PendingUpdateManager pendingUpdateManager = ((ClientWorldAccessor)client.world).astatine$getPendingUpdateManager().incrementSequence();

      try {
         client.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(hand, pendingUpdateManager.getSequence(), this.targetYaw, this.targetPitch));
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

   private void selectMainSlot(MinecraftClient client){
      if (this.itemHand == Hand.MAIN_HAND) {
         this.selectSlot(client, this.itemSlot);
      }
   }

   private void selectSlot(MinecraftClient client, int slot){
      if (slot >= 0 && slot < 9) {
         SilentSlotManager.selectServerSlot(client, slot);
         client.player.getInventory().setSelectedSlot(slot);
      }
   }

   private boolean isTridentSelected(MinecraftClient client){
      return client.player.getInventory().getSelectedSlot() == this.tridentSlot && isRiptideTrident(client.player.getMainHandStack());
   }

   private static boolean isRiptideTrident(ItemStack stack){
      if (!stack.isOf(Items.TRIDENT)) {
         return false;
      } else {
         ItemEnchantmentsComponent itemEnchantmentsComponent = EnchantmentHelper.getEnchantments(stack);

         for (RegistryEntry registryEntry : itemEnchantmentsComponent.getEnchantments()) {
            if (registryEntry.matchesKey(Enchantments.RIPTIDE)) {
               return true;
            }
         }

         return false;
      }
   }

   private static boolean isItemInSlot(MinecraftClient client, int slot, Item item){
      return slot >= 0 && slot < 9 && client.player.getInventory().getStack(slot).isOf(item);
   }

   private static boolean isInGame(MinecraftClient client){
      return client.player != null
         && client.world != null
         && client.player.networkHandler != null
         && !client.player.isDead()
         && !client.player.hasVehicle()
         && !client.player.getAbilities().flying;
   }

   private void resetToIdle(){
      this.clearRotation();
      this.stage = TridentMoveModule.Stage.IDLE;
      this.itemSlot = -1;
      this.tridentSlot = -1;
      this.itemHand = Hand.MAIN_HAND;
      this.countdownTicks = 0;
      this.targetYaw = 0.0F;
      this.targetPitch = 90.0F;
      this.lastRotationTick = this.rotationTick;
      this.waitingForRotation = false;
      this.targetPos = null;
   }

   private void resetFullState(){
      this.resetToIdle();
      this.riptideSelected = false;
      this.rotationTick = 0L;
      this.lastRotationTick = 0L;
   }

   @Environment(EnvType.CLIENT)
   private record GroundTarget(double distance, BlockPos waterPosition){
   }

   @Environment(EnvType.CLIENT)
   private static enum Stage {
      IDLE,
      ROTATE_INITIAL_PLACE,
      WAIT_INITIAL_PLACE,
      ARMED,
      POST_LAUNCH,
      ROTATE_PICKUP,
      WAIT_PICKUP,
      AIRBORNE,
      ROTATE_CLUTCH,
      WAIT_CLUTCH,
      WAIT_LANDING;
   }
}

