package made4mischief.astatine.client.modules.combat;

import java.util.ArrayList;
import java.util.List;
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
import made4mischief.astatine.client.utils.world.BlockPlacementUtil;
import made4mischief.astatine.client.utils.world.HoleScanner;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public final class HoleFillModule extends Module {
   private static final Object ROTATION_STATE_KEY = new Object();
   private static final float ROTATION_TOLERANCE = 1.0F;
   private final NumberSetting targetRangeSetting = this.addNumber("Target Range", 6.0, 1.0, 10.0, 0.5);
   private final NumberSetting holeRangeSetting = this.addNumber("Hole Range", 3.5, 1.0, 8.0, 0.5);
   private final NumberSetting verticalRangeSetting = this.addNumber("Vertical Range", 2.0, 0.0, 5.0, 1.0);
   private final NumberSetting placeRangeSetting = this.addNumber("Place Range", 4.5, 2.0, 6.0, 0.25);
   private final NumberSetting placeDelaySetting = this.addNumber("Place Delay", 0.0, 0.0, 10.0, 1.0);
   private final BooleanSetting doubleHolesSetting = this.addBoolean("Double Holes", true);
   private final BooleanSetting rotateSetting = this.addBoolean("Rotate", true);
   private final BooleanSetting movementFixSetting = this.addBoolean("Movement Fi", true);
   private final BooleanSetting strictDirectionSetting = this.addBoolean("Strict Direction", true);
   private final BooleanSetting pauseUsingItemSetting = this.addBoolean("Pause Using Item", true);
   private final BooleanSetting swingHandSetting = this.addBoolean("Swing Hand", true);
   private final BooleanSetting autoDisableSetting = this.addBoolean("Auto Disable", false);
   private final List<HoleScanner.Hole> holes = new ArrayList<>();
   private PlayerEntity target;
   private HoleFillModule.PendingPlacement pendingPlacement;
   private int placeDelayTicks;

   public HoleFillModule(){
      super("HoleFill", Category.COMBAT, "Láº¥p há»‘ gáº§n má»¥c tiÃªu báº±ng háº¯c diá»‡n tháº¡ch.", -1, true);
      this.movementFixSetting.visibleWhen(this.rotateSetting::getValue);
   }

   @Override
   protected void onEnable(){
      this.clearFilledHoles();
   }

   @Override
   protected void onDisable(){
      RotationManager.clearRotatingState(ROTATION_STATE_KEY);
      this.clearFilledHoles();
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      if (!isInGame(client)) {
         this.clearFilledHoles();
      } else {
         this.tickFill(client);
         if (this.target == null) {
            this.clearPendingPlacement();
            if (this.autoDisableSetting.getValue()) {
               this.disable();
            }
         } else if (this.pauseUsingItemSetting.getValue() && client.player.isUsingItem()) {
            RotationManager.clearRotatingState(ROTATION_STATE_KEY);
         } else {
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
                  HoleFillModule.PlacementResult var6 = this.executePendingPlacement(client, findHotBarItem);
                  if (var6 == HoleFillModule.PlacementResult.WAITING) {
                     return;
                  }

                  if (var6 == HoleFillModule.PlacementResult.PLACED) {
                     pos2 = pos;
                  }
               }

               this.scanHoles(client);
               BlockPlacementUtil.Placement var7 = this.findPlacement(client, pos2);
               if (var7 == null) {
                  RotationManager.clearRotatingState(ROTATION_STATE_KEY);
                  if (this.autoDisableSetting.getValue() && this.holes.isEmpty()) {
                     this.disable();
                  }
               } else {
                  this.rotateToPlacement(client, var7);
                  if (!this.rotateSetting.getValue()) {
                     this.executePendingPlacement(client, findHotBarItem);
                  }
               }
            }
         }
      }
   }

   private void tickFill(MinecraftClient client){
      if (!this.isValidTarget(client, this.target)) {
         PlayerEntity player = this.target;
         this.target = TargetUtil.getClosestTarget(client, this.targetRangeSetting.getValue());
         if (this.target != player) {
            this.clearPendingPlacement();
         }
      }
   }

   private boolean isValidTarget(MinecraftClient client, PlayerEntity player){
      return player != null
         && player != client.player
         && !FriendModule.isFriend(player)
         && player.isAlive()
         && !player.isSpectator()
         && client.world.getEntityById(player.getId()) == player
         && client.player.squaredDistanceTo(player) <= this.targetRangeSetting.getValue() * this.targetRangeSetting.getValue();
   }

   private void scanHoles(MinecraftClient client){
      HoleScanner.scan(
         client.world,
         this.target.getBlockPos(),
         (int)Math.ceil(this.holeRangeSetting.getValue()),
         this.verticalRangeSetting.getValueInt(),
         this.doubleHolesSetting.getValue(),
         this.holes
      );
   }

   private BlockPlacementUtil.Placement findPlacement(MinecraftClient client, BlockPos excluded){
      BlockPlacementUtil.Placement var3 = null;
      double var4 = Double.MAX_VALUE;
      double pos3 = Double.MAX_VALUE;
      double value = this.holeRangeSetting.getValue() * this.holeRangeSetting.getValue();

      for (int index3 = 0; index3 < this.holes.size(); index3++) {
         HoleScanner.Hole var11 = this.holes.get(index3);

         for (int index = 0; index < var11.sizeX(); index++) {
            for (int index2 = 0; index2 < var11.sizeZ(); index2++) {
               BlockPos pos2 = new BlockPos(var11.x() + index, var11.y(), var11.z() + index2);
               if (!pos2.equals(excluded)) {
                  double var15 = getDistanceToBlock(this.target, pos2);
                  if (!(var15 > value)) {
                     BlockPlacementUtil.Placement var17 = BlockPlacementUtil.find(client, pos2, this.placeRangeSetting.getValue(), this.strictDirectionSetting.getValue());
                     if (var17 != null) {
                        double pos = client.player.getEyePos().squaredDistanceTo(var17.hitResult().getPos());
                        if (var15 < var4 || var15 == var4 && pos < pos3) {
                           var3 = var17;
                           var4 = var15;
                           pos3 = pos;
                        }
                     }
                  }
               }
            }
         }
      }

      return var3;
   }

   private void rotateToPlacement(MinecraftClient client, BlockPlacementUtil.Placement placement){
      Vec3d vec = placement.hitResult().getPos();
      float eyePos2 = RotationUtil.getYaw(client.player.getEyePos(), vec);
      float eyePos = RotationUtil.getPitch(client.player.getEyePos(), vec);
      this.pendingPlacement = new HoleFillModule.PendingPlacement(placement, this.target.getId(), eyePos2, eyePos);
      this.tickAutoFill();
   }

   private HoleFillModule.PlacementResult executePendingPlacement(MinecraftClient client, int obsidianSlot){
      HoleFillModule.PendingPlacement var3 = this.pendingPlacement;
      if (var3 == null) {
         return HoleFillModule.PlacementResult.RETRY;
      } else if (this.target != null
         && this.target.getId() == var3.targetId()
         && !(getDistanceToBlock(this.target, var3.placement().target()) > this.holeRangeSetting.getValue() * this.holeRangeSetting.getValue())) {
         BlockPlacementUtil.Placement var4 = BlockPlacementUtil.find(client, var3.placement().target(), this.placeRangeSetting.getValue(), this.strictDirectionSetting.getValue());
         if (!BlockPlacementUtil.sameFace(var3.placement(), var4)) {
            this.clearPendingPlacement();
            return HoleFillModule.PlacementResult.RETRY;
         } else {
            if (this.rotateSetting.getValue()) {
               this.tickAutoFill();
               if (!RotationManager.wasRotationSent(var3.yaw(), var3.pitch(), 1.0F)) {
                  return HoleFillModule.PlacementResult.WAITING;
               }
            }

            ActionResult[] var5 = new ActionResult[]{ActionResult.PASS};
            boolean hitResult = SilentSlotManager.runWithSlot(client, obsidianSlot, () -> {
               var5[0] = client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, var3.placement().hitResult());
               if (this.swingHandSetting.getValue()) {
                  client.player.swingHand(Hand.MAIN_HAND);
               }
            });
            if (!hitResult) {
               return HoleFillModule.PlacementResult.WAITING;
            } else {
               this.pendingPlacement = null;
               this.placeDelayTicks = this.placeDelaySetting.getValueInt();
               RotationManager.clearRotatingState(ROTATION_STATE_KEY);
               return HoleFillModule.PlacementResult.PLACED;
            }
         }
      } else {
         this.clearPendingPlacement();
         return HoleFillModule.PlacementResult.RETRY;
      }
   }

   private void tickAutoFill(){
      if (this.pendingPlacement != null && this.rotateSetting.getValue()) {
         RotationManager.setRotation(ROTATION_STATE_KEY, this.pendingPlacement.yaw(), this.pendingPlacement.pitch(), false, this.movementFixSetting.getValue());
      }
   }

   private void clearPendingPlacement(){
      this.pendingPlacement = null;
      RotationManager.clearRotatingState(ROTATION_STATE_KEY);
   }

   private void clearFilledHoles(){
      this.holes.clear();
      this.target = null;
      this.pendingPlacement = null;
      this.placeDelayTicks = 0;
   }

   private static double getDistanceToBlock(PlayerEntity player, BlockPos block){
      double x = player.getX() - (block.getX() + 0.5);
      double z = player.getZ() - (block.getZ() + 0.5);
      return x * x + z * z;
   }

   private static boolean isInGame(MinecraftClient client){
      return client.player != null
         && client.world != null
         && client.interactionManager != null
         && client.player.networkHandler != null
         && !client.player.isDead();
   }

   @Environment(EnvType.CLIENT)
   private record PendingPlacement(BlockPlacementUtil.Placement placement, int targetId, float yaw, float pitch){
   }

   @Environment(EnvType.CLIENT)
   private static enum PlacementResult {
      WAITING,
      RETRY,
      PLACED;
   }
}

