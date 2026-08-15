package made4mischief.astatine.client.modules.smp;

import java.util.Comparator;
import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.hit.BlockHitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class SpawnerProtectModule extends Module {
   private static final Logger LOGGER = LoggerFactory.getLogger("astatine/spawner-protect");
   private static final int SEARCH_RADIUS = 5;
   private static final int MINE_TIMEOUT_TICKS = 240;
   private static final int SCAN_GRACE_TICKS = 12;
   private static final int RANGE_SAFE_TICKS = 60;
   private static final int OPEN_GUI_TIMEOUT = 50;
   private static final int MAX_TRANSFER_ATTEMPTS = 3;
   private SpawnerProtectModule.Phase phase = SpawnerProtectModule.Phase.ARMED;
   private BlockPos mineTarget;
   private BlockPos enderChestPos;
   private int stateTicks;
   private int mineTicks;
   private int lastHotbarCount;
   private int transferFailures;
   private boolean transferPending;
   private String detectedPlayerName = "";
   private Object disconnectHandler;

   public SpawnerProtectModule(){
      super("SpawnerProtect", Category.SMP, "LÃ©n Ä‘Ã o lá»“ng quÃ¡i vÃ  thoÃ¡t khi phÃ¡t hiá»‡n ngÆ°á»i chÆ¡i.", -1, true);
   }

   @Override
   protected void onEnable(){
      this.armProtection();
   }

   @Override
   protected void onDisable(){
      MinecraftClient client = MinecraftClient.getInstance();
      this.tickPingCheck(client);
      if (client.interactionManager != null) {
         client.interactionManager.cancelBlockBreaking();
      }

      this.armProtection();
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      if (this.phase == SpawnerProtectModule.Phase.DISCONNECTING) {
         if (!canAct(client) || client.player.networkHandler == this.disconnectHandler) {
            return;
         }

         this.armProtection();
      }

      if (canAct(client)) {
         this.stateTicks++;
         switch (this.phase) {
            case ARMED:
               this.tickAttack(client);
               break;
            case MINING:
               this.tickAutoLog(client);
               break;
            case WAIT_PICKUP:
               this.tickScanSpawner(client);
               break;
            case OPEN_ENDER_CHEST:
               this.tickVerifyTarget(client);
               break;
            case WAIT_ENDER_CHEST:
               this.tickInCombat(client);
               break;
            case DEPOSIT:
               this.tickOutOfCombat(client);
            case DISCONNECTING:
         }
      }
   }

   private void tickAttack(MinecraftClient client){
      if (this.findSpawnerBlock(client, false) != null) {
         PlayerEntity player = this.findTargetPlayer(client);
         if (player != null) {
            this.detectedPlayerName = player.getName().getString();
            client.player
               .sendMessage(Text.literal("Â§8[Â§cSpawnerProtectÂ§8] Â§eDetected Â§f" + this.detectedPlayerName + "Â§e; securing nearby spawners."), false);
            LOGGER.warn("Detected player '{}' while {} spawners are nearby.", this.detectedPlayerName, this.getLookingAtY(client));
            if (!this.isLookingAtBlock(client)) {
               this.disconnectWithReason(client, "No pickaxe was available while " + this.detectedPlayerName + " was detected.");
            } else {
               this.holdForwardKey(client);
               this.setPhase(SpawnerProtectModule.Phase.MINING);
            }
         }
      }
   }

   private void tickAutoLog(MinecraftClient client){
      this.holdForwardKey(client);
      if (!this.isLookingAtBlock(client)) {
         this.disconnectWithReason(client, "Pickaxe was lost during emergency mining.");
      } else {
         if (this.mineTarget != null && !client.world.getBlockState(this.mineTarget).isOf(Blocks.SPAWNER)) {
            client.interactionManager.cancelBlockBreaking();
            this.mineTarget = null;
            this.mineTicks = 0;
         }

         if (this.mineTarget == null) {
            BlockPos pos = this.findSpawnerBlock(client, true);
            if (pos == null) {
               if (this.findSpawnerBlock(client, false) != null) {
                  this.disconnectWithReason(client, "A nearby spawner was outside interaction reach.");
               } else {
                  client.interactionManager.cancelBlockBreaking();
                  this.tickPingCheck(client);
                  this.setPhase(SpawnerProtectModule.Phase.WAIT_PICKUP);
               }
            } else {
               this.mineTarget = pos;
               this.mineTicks = 0;
               client.interactionManager.attackBlock(this.mineTarget, Direction.UP);
               client.player.swingHand(Hand.MAIN_HAND);
            }
         } else {
            this.mineTicks++;
            client.interactionManager.updateBlockBreakingProgress(this.mineTarget, Direction.UP);
            client.player.swingHand(Hand.MAIN_HAND);
            if (this.mineTicks >= 240) {
               this.disconnectWithReason(client, "Mining stalled at " + this.mineTarget.toShortString() + ".");
            }
         }
      }
   }

   private void tickScanSpawner(MinecraftClient client){
      if (this.enderChestPos == null) {
         this.enderChestPos = this.getSpawnerTarget(client);
         if (this.enderChestPos == null) {
            this.disconnectWithReason(client, "No Ender Chest was found within 5 blocks.");
            return;
         }
      }

      if (this.stateTicks >= 12 && (this.stateTicks >= 60 || !this.isRangeSafe(client))) {
         this.setPhase(SpawnerProtectModule.Phase.OPEN_ENDER_CHEST);
      }
   }

   private void tickVerifyTarget(MinecraftClient client){
      if (this.enderChestPos == null
         || !client.world.getBlockState(this.enderChestPos).isOf(Blocks.ENDER_CHEST)
         || !client.player.canInteractWithBlockAt(this.enderChestPos, 1.0)) {
         this.disconnectWithReason(client, "The nearby Ender Chest could not be reached.");
      } else if (client.currentScreen == null && client.player.currentScreenHandler == client.player.playerScreenHandler) {
         Vec3d vec = Vec3d.ofCenter(this.enderChestPos);
         BlockHitResult hitResult = new BlockHitResult(vec, Direction.UP, this.enderChestPos, false);
         client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitResult);
         client.player.swingHand(Hand.MAIN_HAND);
         this.setPhase(SpawnerProtectModule.Phase.WAIT_ENDER_CHEST);
      } else {
         client.player.closeHandledScreen();
      }
   }

   private void tickInCombat(MinecraftClient client){
      if (isInCombat(client)) {
         this.transferPending = false;
         this.transferFailures = 0;
         this.setPhase(SpawnerProtectModule.Phase.DEPOSIT);
      } else {
         if (this.stateTicks >= 50) {
            this.disconnectWithReason(client, "The Ender Chest GUI did not open.");
         }
      }
   }

   private void tickOutOfCombat(MinecraftClient client){
      if (!isInCombat(client)) {
         this.disconnectWithReason(client, "The Ender Chest GUI closed before deposit.");
      } else {
         int hotbarSlot = this.getHotbarSlot(client);
         if (this.transferPending) {
            if (this.stateTicks < 2) {
               return;
            }

            if (hotbarSlot < this.lastHotbarCount) {
               this.transferFailures = 0;
            } else {
               this.transferFailures++;
            }

            this.transferPending = false;
            if (this.transferFailures >= 3) {
               this.disconnectWithReason(client, "The Ender Chest rejected 3 spawner transfers.");
               return;
            }
         }

         if (hotbarSlot <= 0) {
            client.player.closeHandledScreen();
            this.disconnectWithReason(client, "Nearby spawners were secured.");
         } else {
            PlayerInventory playerInventory = client.player.getInventory();
            ScreenHandler screenHandler = client.player.currentScreenHandler;

            for (Slot slot : screenHandler.slots) {
               if (slot.inventory == playerInventory && isStackSpawnerBlock(slot.getStack())) {
                  this.lastHotbarCount = hotbarSlot;
                  this.transferPending = true;
                  client.interactionManager.clickSlot(screenHandler.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, client.player);
                  this.stateTicks = 0;
                  return;
               }
            }

            this.disconnectWithReason(client, "No transferable spawner stack was found.");
         }
      }
   }

   private PlayerEntity findTargetPlayer(MinecraftClient client){
      for (PlayerEntity player : client.world.getPlayers()) {
         if (player != client.player && !player.getUuid().equals(client.player.getUuid())) {
            return player;
         }
      }

      return null;
   }

   private BlockPos findSpawnerBlock(MinecraftClient client, boolean requireReach){
      BlockPos pos3 = client.player.getBlockPos();
      BlockPos pos2 = null;
      double squaredDistance2 = Double.MAX_VALUE;

      for (BlockPos pos : BlockPos.iterate(pos3.add(-5, -5, -5), pos3.add(5, 5, 5))) {
         double squaredDistance = pos.getSquaredDistance(pos3);
         if (!(squaredDistance > 25.0)
            && client.world.getBlockState(pos).isOf(Blocks.SPAWNER)
            && (!requireReach || client.player.canInteractWithBlockAt(pos, 1.0))
            && squaredDistance < squaredDistance2) {
            squaredDistance2 = squaredDistance;
            pos2 = pos.toImmutable();
         }
      }

      return pos2;
   }

   private int getLookingAtY(MinecraftClient client){
      BlockPos pos2 = client.player.getBlockPos();
      int index = 0;

      for (BlockPos pos : BlockPos.iterate(pos2.add(-5, -5, -5), pos2.add(5, 5, 5))) {
         if (pos.getSquaredDistance(pos2) <= 25.0 && client.world.getBlockState(pos).isOf(Blocks.SPAWNER)) {
            index++;
         }
      }

      return index;
   }

   private BlockPos getSpawnerTarget(MinecraftClient client){
      BlockPos playerPos = client.player.getBlockPos();
      return BlockPos.stream(playerPos.add(-5, -5, -5), playerPos.add(5, 5, 5))
         .filter(p -> p.getSquaredDistance(playerPos) <= 25.0)
         .filter(p -> client.world.getBlockState(p).isOf(Blocks.ENDER_CHEST))
         .min(Comparator.comparingDouble(p -> p.getSquaredDistance(playerPos)))
         .<BlockPos>map(BlockPos::toImmutable)
         .orElse(null);
   }

   private boolean isLookingAtBlock(MinecraftClient client){
      PlayerInventory playerInventory = client.player.getInventory();
      int selectedSlot = playerInventory.getSelectedSlot();
      if (playerInventory.getStack(selectedSlot).isIn(ItemTags.PICKAXES)) {
         return true;
      } else {
         for (int index2 = 0; index2 < 9; index2++) {
            if (playerInventory.getStack(index2).isIn(ItemTags.PICKAXES)) {
               playerInventory.setSelectedSlot(index2);
               this.selectItemSlot(client, index2);
               return true;
            }
         }

         int index3 = -1;

         for (int index = 9; index < 36; index++) {
            if (playerInventory.getStack(index).isIn(ItemTags.PICKAXES)) {
               index3 = index;
               break;
            }
         }

         if (index3 >= 0 && client.interactionManager != null) {
            PlayerScreenHandler playerScreenHandler = client.player.playerScreenHandler;

            for (Slot slot : playerScreenHandler.slots) {
               if (slot.inventory == playerInventory && slot.getIndex() == index3) {
                  client.interactionManager.clickSlot(playerScreenHandler.syncId, slot.id, selectedSlot, SlotActionType.SWAP, client.player);
                  playerInventory.setSelectedSlot(selectedSlot);
                  this.selectItemSlot(client, selectedSlot);
                  return playerInventory.getStack(selectedSlot).isIn(ItemTags.PICKAXES);
               }
            }

            return false;
         } else {
            return false;
         }
      }
   }

   private void selectItemSlot(MinecraftClient client, int slot){
      if (client.player.networkHandler != null) {
         client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
      }
   }

   private int getHotbarSlot(MinecraftClient client){
      int var2 = 0;
      PlayerInventory playerInventory = client.player.getInventory();

      for (int index = 0; index < 36; index++) {
         ItemStack stack = playerInventory.getStack(index);
         if (isStackSpawnerBlock(stack)) {
            var2 += stack.getCount();
         }
      }

      return var2;
   }

   private boolean isRangeSafe(MinecraftClient client){
      double var2 = 25.0;
      Vec3d vec = client.player.getEntityPos();

      for (Entity entity : client.world.getEntities()) {
         if (entity instanceof ItemEntity var7 && !var7.isRemoved() && isStackSpawnerBlock(var7.getStack()) && var7.getEntityPos().squaredDistanceTo(vec) <= var2) {
            return true;
         }
      }

      return false;
   }

   private static boolean isStackSpawnerBlock(ItemStack stack){
      return !stack.isEmpty() && stack.isOf(Blocks.SPAWNER.asItem());
   }

   private void holdForwardKey(MinecraftClient client){
      client.options.sneakKey.setPressed(true);
   }

   private void tickPingCheck(MinecraftClient client){
      if (client != null) {
         client.options.sneakKey.setPressed(false);
      }
   }

   private void disconnectWithReason(MinecraftClient client, String reason){
      if (this.phase != SpawnerProtectModule.Phase.DISCONNECTING) {
         this.phase = SpawnerProtectModule.Phase.DISCONNECTING;
         this.tickPingCheck(client);
         if (client.interactionManager != null) {
            client.interactionManager.cancelBlockBreaking();
         }

         if (client.player != null) {
            client.player.sendMessage(Text.literal("Â§8[Â§cSpawnerProtectÂ§8] Â§c" + reason + " Disconnecting."), false);
            if (isInCombat(client)) {
               client.player.closeHandledScreen();
            }
         }

         LOGGER.warn("Disconnecting after detecting '{}': {}", this.detectedPlayerName, reason);
         this.disconnectHandler = client.player == null ? null : client.player.networkHandler;
         client.disconnect(Text.literal("SpawnerProtect: " + reason));
      }
   }

   private void setPhase(SpawnerProtectModule.Phase next){
      this.phase = next;
      this.stateTicks = 0;
   }

   private void armProtection(){
      this.phase = SpawnerProtectModule.Phase.ARMED;
      this.mineTarget = null;
      this.enderChestPos = null;
      this.stateTicks = 0;
      this.mineTicks = 0;
      this.lastHotbarCount = 0;
      this.transferFailures = 0;
      this.transferPending = false;
      this.detectedPlayerName = "";
      this.disconnectHandler = null;
   }

   private static boolean isInCombat(MinecraftClient client){
      return client.player != null && client.player.currentScreenHandler != client.player.playerScreenHandler;
   }

   private static boolean canAct(MinecraftClient client){
      return client.player != null && client.world != null && client.interactionManager != null && client.player.networkHandler != null;
   }

   @Environment(EnvType.CLIENT)
   private static enum Phase {
      ARMED,
      MINING,
      WAIT_PICKUP,
      OPEN_ENDER_CHEST,
      WAIT_ENDER_CHEST,
      DEPOSIT,
      DISCONNECTING;
   }
}

