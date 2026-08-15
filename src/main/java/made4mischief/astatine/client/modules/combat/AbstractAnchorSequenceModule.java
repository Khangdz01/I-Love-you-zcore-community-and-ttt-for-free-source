package made4mischief.astatine.client.modules.combat;

import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.KeybindSetting;
import made4mischief.astatine.client.utils.inventory.InventoryUtil;
import made4mischief.astatine.client.utils.inventory.SilentSlotManager;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Hand;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.block.RespawnAnchorBlock;

@Environment(EnvType.CLIENT)
abstract class AbstractAnchorSequenceModule extends Module {
   private static final int PLACE_COOLDOWN = 10;
   private static final int CHARGE_COOLDOWN = 3;
   private static final Direction[] DIRECTIONS = new Direction[]{
      Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP
   };
   private final KeybindSetting targetModeSetting;
   private final boolean explodeSetting;
   private AbstractAnchorSequenceModule.Stage stage = AbstractAnchorSequenceModule.Stage.PLACE_ANCHOR;
   private BlockPos anchorPos;
   private BlockPos chargeBlockPos;
   private int anchorSlot = -1;
   private int placeTimer;
   private int chargeTimer;

   protected AbstractAnchorSequenceModule(String name, String description, int defaultActionBind, boolean shielded){
      super(name, Category.COMBAT, description, -1, true);
      this.targetModeSetting = this.addKeybind("Action Bind", defaultActionBind);
      this.explodeSetting = shielded;
   }

   @Override
   protected final void onEnable(){
      this.resetStage();
   }

   @Override
   protected final void onDisable(){
      this.resetStage();
   }

   protected final void tickSequence(TickEvent event){
      MinecraftClient client = event.getClient();
      if (!isInGame(client)) {
         this.resetStage();
      } else if (client.currentScreen != null) {
         this.resetStage();
      } else if (!this.getTargetMode(client)) {
         this.resetStage();
      } else {
         switch (this.stage) {
            case PLACE_ANCHOR:
               this.findAnchorSlot(client);
               break;
            case WAIT_ANCHOR:
               this.getAnchorState(client);
               break;
            case PLACE_SHIELD:
               this.tickChargeAnchor(client);
               break;
            case WAIT_SHIELD:
               this.tickExplodeAnchor(client);
               break;
            case WAIT_CHARGED:
               this.getAnchorChargeState(client);
               break;
            case WAIT_REMOVED:
               this.tickExplodeVerify(client);
         }
      }
   }

   private void findAnchorSlot(MinecraftClient client){
      int findHotBarItem = InventoryUtil.findHotBarItem(client, Items.RESPAWN_ANCHOR);
      if (findHotBarItem != -1) {
         if (InventoryUtil.findHotBarItem(client, Items.GLOWSTONE) != -1) {
            if (client.crosshairTarget instanceof BlockHitResult var3) {
               if (isHitInReach(client, var3)) {
                  ItemStack stack = client.player.getInventory().getStack(findHotBarItem);
                  ItemPlacementContext itemPlacementContext = new ItemPlacementContext(client.player, Hand.MAIN_HAND, stack, var3);
                  if (itemPlacementContext.canPlace()) {
                     BlockPos pos2 = itemPlacementContext.getBlockPos().toImmutable();
                     BlockPos pos = this.explodeSetting ? pos2.offset(getDirectionToAnchor(client, pos2)).toImmutable() : null;
                     int of = this.explodeSetting
                           && !client.player.getAbilities().creativeMode
                           && !client.world.getBlockState(pos).isOf(Blocks.GLOWSTONE)
                        ? 2
                        : 1;
                     if (countItemInInventory(client, Items.GLOWSTONE) >= of) {
                        if (this.runWithSlot(client, findHotBarItem, () -> {
                           client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, var3);
                           client.player.swingHand(Hand.MAIN_HAND);
                        })) {
                           this.anchorSlot = findHotBarItem;
                           this.anchorPos = pos2;
                           this.chargeBlockPos = pos;
                           this.placeTimer = 10;
                           this.stage = AbstractAnchorSequenceModule.Stage.WAIT_ANCHOR;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void getAnchorState(MinecraftClient client){
      BlockState state = client.world.getBlockState(this.anchorPos);
      if (state.isOf(Blocks.RESPAWN_ANCHOR)) {
         this.tickPlaceAnchor(client, state);
      } else {
         if (--this.placeTimer <= 0) {
            this.resetStage();
         }
      }
   }

   private void tickPlaceAnchor(MinecraftClient client, BlockState anchorState){
      if (this.explodeSetting && !this.isAnchorCharged(client)) {
         this.stage = AbstractAnchorSequenceModule.Stage.PLACE_SHIELD;
      } else {
         if ((Integer)anchorState.get(RespawnAnchorBlock.CHARGES) > 0) {
            this.tickSequence(client);
         } else {
            this.tickVerifyCharge(client);
         }
      }
   }

   private void tickChargeAnchor(MinecraftClient client){
      if (!client.world.getBlockState(this.anchorPos).isOf(Blocks.RESPAWN_ANCHOR)) {
         this.resetStage();
      } else if (this.isAnchorCharged(client)) {
         this.tickPlaceAnchor(client, client.world.getBlockState(this.anchorPos));
      } else {
         int findHotBarItem = InventoryUtil.findHotBarItem(client, Items.GLOWSTONE);
         if (findHotBarItem != -1) {
            AbstractAnchorSequenceModule.Placement var3 = this.findAnchorPlacement(client, this.chargeBlockPos, findHotBarItem);
            if (var3 != null) {
               if (this.runWithSlot(client, findHotBarItem, () -> {
                  client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, var3.hitResult());
                  client.player.swingHand(Hand.MAIN_HAND);
               })) {
                  this.placeTimer = 10;
                  this.stage = AbstractAnchorSequenceModule.Stage.WAIT_SHIELD;
               }
            }
         }
      }
   }

   private void tickExplodeAnchor(MinecraftClient client){
      if (!client.world.getBlockState(this.anchorPos).isOf(Blocks.RESPAWN_ANCHOR)) {
         this.resetStage();
      } else if (this.isAnchorCharged(client)) {
         this.tickPlaceAnchor(client, client.world.getBlockState(this.anchorPos));
      } else {
         if (--this.placeTimer <= 0) {
            this.stage = AbstractAnchorSequenceModule.Stage.PLACE_SHIELD;
         }
      }
   }

   private void tickVerifyCharge(MinecraftClient client){
      if (this.explodeSetting && !this.isAnchorCharged(client)) {
         this.stage = AbstractAnchorSequenceModule.Stage.PLACE_SHIELD;
      } else {
         int findHotBarItem = InventoryUtil.findHotBarItem(client, Items.GLOWSTONE);
         if (findHotBarItem == -1) {
            if (this.explodeSetting) {
               this.stage = AbstractAnchorSequenceModule.Stage.PLACE_SHIELD;
            } else {
               this.resetStage();
            }
         } else {
            BlockHitResult hitResult = raycastToBlock(client, this.anchorPos);
            if (isHitInReach(client, hitResult) && this.runWithSlot(client, findHotBarItem, () -> {
               client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitResult);
               client.player.swingHand(Hand.MAIN_HAND);
            })) {
               this.placeTimer = 10;
               this.stage = AbstractAnchorSequenceModule.Stage.WAIT_CHARGED;
            }
         }
      }
   }

   private void getAnchorChargeState(MinecraftClient client){
      BlockState state = client.world.getBlockState(this.anchorPos);
      if (!state.isOf(Blocks.RESPAWN_ANCHOR)) {
         this.resetStage();
      } else if ((Integer)state.get(RespawnAnchorBlock.CHARGES) <= 0) {
         if (--this.placeTimer <= 0) {
            this.resetStage();
         }
      } else {
         if (this.explodeSetting && !this.isAnchorCharged(client)) {
            this.stage = AbstractAnchorSequenceModule.Stage.PLACE_SHIELD;
         } else {
            this.tickSequence(client);
         }
      }
   }

   private void tickSequence(MinecraftClient client){
      if (this.explodeSetting && !this.isAnchorCharged(client)) {
         this.stage = AbstractAnchorSequenceModule.Stage.PLACE_SHIELD;
      } else {
         BlockHitResult hitResult = raycastToBlock(client, this.anchorPos);
         int anchorInHotbar = this.isAnchorInHotbar(client);
         if (anchorInHotbar != -1 && isHitInReach(client, hitResult) && this.runWithSlot(client, anchorInHotbar, () -> {
            client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitResult);
            client.player.swingHand(Hand.MAIN_HAND);
         })) {
            this.placeTimer = 10;
            this.chargeTimer = 3;
            this.stage = AbstractAnchorSequenceModule.Stage.WAIT_REMOVED;
         }
      }
   }

   private void tickExplodeVerify(MinecraftClient client){
      BlockState state = client.world.getBlockState(this.anchorPos);
      if (!state.isOf(Blocks.RESPAWN_ANCHOR)) {
         this.resetStage();
      } else if (--this.placeTimer <= 0) {
         this.resetStage();
      } else {
         if (--this.chargeTimer <= 0 && (Integer)state.get(RespawnAnchorBlock.CHARGES) > 0) {
            if (!this.explodeSetting || this.isAnchorCharged(client)) {
               BlockHitResult hitResult = raycastToBlock(client, this.anchorPos);
               int anchorInHotbar = this.isAnchorInHotbar(client);
               if (anchorInHotbar != -1 && isHitInReach(client, hitResult)) {
                  this.runWithSlot(client, anchorInHotbar, () -> {
                     client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitResult);
                     client.player.swingHand(Hand.MAIN_HAND);
                  });
               }
            }

            this.chargeTimer = 3;
         }
      }
   }

   private AbstractAnchorSequenceModule.Placement findAnchorPlacement(MinecraftClient client, BlockPos target, int itemSlot){
      if (target != null && client.world.getBlockState(target).isReplaceable()) {
         ItemStack stack = client.player.getInventory().getStack(itemSlot);

         for (Direction direction : DIRECTIONS) {
            BlockPos pos = target.offset(direction);
            if (!pos.equals(this.anchorPos) && !client.world.getBlockState(pos).isReplaceable()) {
               Direction direction2 = direction.getOpposite();
               Vec3d vec2 = Vec3d.ofCenter(pos).add(direction2.getOffsetX() * 0.5, direction2.getOffsetY() * 0.5, direction2.getOffsetZ() * 0.5);
               BlockHitResult hitResult = new BlockHitResult(vec2, direction2, pos, false);
               if (isHitInReach(client, hitResult)) {
                  ItemPlacementContext itemPlacementContext2 = new ItemPlacementContext(client.player, Hand.MAIN_HAND, stack, hitResult);
                  if (itemPlacementContext2.canPlace() && itemPlacementContext2.getBlockPos().equals(target)) {
                     return new AbstractAnchorSequenceModule.Placement(hitResult);
                  }
               }
            }
         }

         Vec3d vec = Vec3d.ofCenter(target);
         BlockHitResult hitResult2 = new BlockHitResult(vec, Direction.UP, target, false);
         if (isHitInReach(client, hitResult2)) {
            ItemPlacementContext itemPlacementContext = new ItemPlacementContext(client.player, Hand.MAIN_HAND, stack, hitResult2);
            if (itemPlacementContext.canPlace() && itemPlacementContext.getBlockPos().equals(target)) {
               return new AbstractAnchorSequenceModule.Placement(hitResult2);
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private boolean isAnchorCharged(MinecraftClient client){
      return this.chargeBlockPos != null && client.world.getBlockState(this.chargeBlockPos).isOf(Blocks.GLOWSTONE);
   }

   private int isAnchorInHotbar(MinecraftClient client){
      if (this.anchorSlot >= 0 && this.anchorSlot < 9 && !client.player.getInventory().getStack(this.anchorSlot).isOf(Items.GLOWSTONE)) {
         return this.anchorSlot;
      } else {
         for (int index = 0; index < 9; index++) {
            if (!client.player.getInventory().getStack(index).isOf(Items.GLOWSTONE)) {
               return index;
            }
         }

         return -1;
      }
   }

   private boolean runWithSlot(MinecraftClient client, int slot, Runnable action){
      return SilentSlotManager.runWithSlot(client, slot, action);
   }

   private boolean getTargetMode(MinecraftClient client){
      int value = this.targetModeSetting.getValue();
      return value != -1 && InputUtil.isKeyPressed(client.getWindow(), value);
   }

   private static Direction getDirectionToAnchor(MinecraftClient client, BlockPos anchor){
      double x = client.player.getX() - (anchor.getX() + 0.5);
      double z = client.player.getZ() - (anchor.getZ() + 0.5);
      if (Math.abs(x) > Math.abs(z)) {
         return x >= 0.0 ? Direction.EAST : Direction.WEST;
      } else {
         return z >= 0.0 ? Direction.SOUTH : Direction.NORTH;
      }
   }

   private static int countItemInInventory(MinecraftClient client, Item item){
      int var2 = 0;

      for (int index = 0; index < 9; index++) {
         ItemStack stack = client.player.getInventory().getStack(index);
         if (stack.isOf(item)) {
            var2 += stack.getCount();
         }
      }

      return var2;
   }

   private static BlockHitResult raycastToBlock(MinecraftClient client, BlockPos pos){
      Vec3d vec3 = Vec3d.ofCenter(pos);
      Vec3d vec2 = client.player.getEyePos();
      Direction direction = Direction.getFacing(vec2.x - vec3.x, vec2.y - vec3.y, vec2.z - vec3.z);
      Vec3d vec = vec3.add(direction.getOffsetX() * 0.5, direction.getOffsetY() * 0.5, direction.getOffsetZ() * 0.5);
      return new BlockHitResult(vec, direction, pos, false);
   }

   private static boolean isHitInReach(MinecraftClient client, BlockHitResult hitResult){
      double blockInteractionRange = client.player.getBlockInteractionRange();
      return client.player.getEyePos().squaredDistanceTo(hitResult.getPos()) <= blockInteractionRange * blockInteractionRange;
   }

   private static boolean isInGame(MinecraftClient client){
      return client != null && client.player != null && client.world != null && client.interactionManager != null && !client.player.isDead();
   }

   private void resetStage(){
      this.stage = AbstractAnchorSequenceModule.Stage.PLACE_ANCHOR;
      this.anchorPos = null;
      this.chargeBlockPos = null;
      this.anchorSlot = -1;
      this.placeTimer = 0;
      this.chargeTimer = 0;
   }

   @Environment(EnvType.CLIENT)
   private record Placement(BlockHitResult hitResult){
   }

   @Environment(EnvType.CLIENT)
   private static enum Stage {
      PLACE_ANCHOR,
      WAIT_ANCHOR,
      PLACE_SHIELD,
      WAIT_SHIELD,
      WAIT_CHARGED,
      WAIT_REMOVED;
   }
}

