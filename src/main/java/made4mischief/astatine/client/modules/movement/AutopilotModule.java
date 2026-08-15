package made4mischief.astatine.client.modules.movement;

import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.ModeSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import made4mischief.astatine.client.utils.inventory.SilentSlotManager;
import made4mischief.astatine.client.utils.rotation.RotationManager;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.Hand;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;

@Environment(EnvType.CLIENT)
public final class AutopilotModule extends Module {
   private static final Object ROTATION_KEY = new Object();
   private static final int FIREWORK_SLOT = 6;
   private static final int JUMP_INTERVAL = 2;
   private static final int TICK_TIMEOUT = 24;
   private static AutopilotModule instance;
   private final ModeSetting modeSetting = this.addMode("Mode", "Altitude Hold", new String[]{"Altitude Hold", "Standing Fly"});
   private final ModeSetting altitudeSetting = this.addMode("Altitude", "Set", new String[]{"Set", "Current"});
   private final NumberSetting targetYSetting = this.addNumber("Target Y", 120.0, -64.0, 320.0, 1.0);
   private final NumberSetting heightToleranceSetting = this.addNumber("Height Tolerance", 3.5, 1.0, 8.0, 0.5);
   private final NumberSetting climbPitchSetting = this.addNumber("Climb Pitch", -35.0, -60.0, -5.0, 5.0);
   private final NumberSetting descendPitchSetting = this.addNumber("Descend Pitch", 15.0, 0.0, 45.0, 5.0);
   private final NumberSetting cruisePitchSetting = this.addNumber("Cruise Pitch", -5.0, -30.0, 20.0, 5.0);
   private final NumberSetting obstacleRangeSetting = this.addNumber("Obstacle Range", 12.0, 4.0, 24.0, 1.0);
   private final NumberSetting obstacleClearanceSetting = this.addNumber("Obstacle Clearance", 4.0, 2.0, 10.0, 1.0);
   private final NumberSetting avoidHoldSetting = this.addNumber("Avoid Hold", 20.0, 5.0, 60.0, 1.0);
   private final NumberSetting minSpeedSetting = this.addNumber("Min Speed", 1.1, 0.3, 2.5, 0.1);
   private final NumberSetting rocketDelaySetting = this.addNumber("Rocket Delay", 20.0, 5.0, 80.0, 1.0);
   private final BooleanSetting autoTakeoffSetting = this.addBoolean("Auto Takeoff", true);
   private final BooleanSetting autoRocketSetting = this.addBoolean("Auto Rocket", true);
   private final BooleanSetting movementFixSetting = this.addBoolean("Movement Fi", true);
   private final BooleanSetting restoreChestSetting = this.addBoolean("Restore Chest", true);
   private final BooleanSetting disableIfMissingSetting = this.addBoolean("Disable If Missing", true);
   private AutopilotModule.Stage stage = AutopilotModule.Stage.PREPARE;
   private int stageTicks;
   private int prepareTicks;
   private int lastRocketTick = -1073741824;
   private int rocketSlot = -1;
   private int preferredSlot = -1;
   private int invSlot = -1;
   private int hotbarSlot = -1;
   private boolean rocketCooldown;
   private boolean rocketReady;
   private boolean flying;
   private double targetSpeed;
   private double currentSpeed;
   private int ticksAloft;

   public AutopilotModule(){
      super("Autopilot", Category.MOVEMENT, "Giá»¯ Ä‘á»™ cao vÃ  nÃ© váº­t cáº£n khi bay Elytra.", -1, true);
      this.targetYSetting.visibleWhen(() -> this.altitudeSetting.is("Set"));
      this.climbPitchSetting.visibleWhen(() -> this.modeSetting.is("Altitude Hold"));
      this.descendPitchSetting.visibleWhen(() -> this.modeSetting.is("Altitude Hold"));
      this.cruisePitchSetting.visibleWhen(() -> this.modeSetting.is("Altitude Hold"));
      this.minSpeedSetting.visibleWhen(this.autoRocketSetting::getValue);
      this.rocketDelaySetting.visibleWhen(this.autoRocketSetting::getValue);
      instance = this;
   }

   @Override
   protected void onEnable(){
      this.resetToPrepare();
   }

   @Override
   protected void onDisable(){
      RotationManager.clearRotatingState(ROTATION_KEY);
      MinecraftClient client = MinecraftClient.getInstance();
      if (this.restoreChestSetting.getValue()) {
         this.tickRocketUsage(client);
      }

      this.useRocket(client);
      this.resetToPrepare();
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      if (!isInGame(client)) {
         RotationManager.clearRotatingState(ROTATION_KEY);
      } else {
         this.prepareTicks++;
         this.stageTicks++;
         if (this.rocketCooldown || canInteractInventory(client) && this.updateTargetSpeed(client)) {
            switch (this.stage) {
               case PREPARE:
                  this.startPrepare(client);
                  break;
               case JUMP:
                  this.checkElytraEquipped(client);
                  break;
               case START_GLIDE:
                  this.checkElytraBeforeTakeoff(client);
                  break;
               case FLYING:
                  this.checkElytraBeforeFly(client);
            }
         } else {
            this.notifyMessage(client, "Need a usable Elytra" + (this.autoRocketSetting.getValue() ? " and firework." : "."));
         }
      }
   }

   public static void applyTakeoffInput(ClientPlayerEntity player){
      AutopilotModule autopilotModule = instance;
      if (autopilotModule != null && autopilotModule.isEnabled() && autopilotModule.autoTakeoffSetting.getValue() && player != null && MinecraftClient.getInstance().currentScreen == null) {
         boolean onGround = autopilotModule.stage == AutopilotModule.Stage.JUMP && player.isOnGround();
         boolean gliding = autopilotModule.stage == AutopilotModule.Stage.START_GLIDE && !player.isOnGround() && !player.isGliding() && autopilotModule.stageTicks % 2 == 0;
         if (onGround || gliding) {
            PlayerInput playerInput = player.input.playerInput;
            player.input.playerInput = new PlayerInput(
               playerInput.forward(), playerInput.backward(), playerInput.left(), playerInput.right(), true, playerInput.sneak(), playerInput.sprint()
            );
         }
      }
   }

   private void startPrepare(MinecraftClient client){
      if (!this.hasElytraOrChest(client)) {
         this.notifyMessage(client, "Could not equip Elytra.");
      } else if (this.autoRocketSetting.getValue() && !this.isRocketSlotReady(client)) {
         this.notifyMessage(client, "No firework remains.");
      } else {
         if (client.player.isGliding()) {
            this.beginFlight();
         } else if (client.player.isOnGround()) {
            this.setStage(AutopilotModule.Stage.JUMP);
         } else {
            this.setStage(AutopilotModule.Stage.START_GLIDE);
         }
      }
   }

   private void checkElytraEquipped(MinecraftClient client){
      if (!this.hasElytra(client)) {
         this.notifyMessage(client, "Elytra is missing or broken.");
      } else {
         if (client.player.isGliding()) {
            this.beginFlight();
         } else if (!client.player.isOnGround()) {
            this.setStage(AutopilotModule.Stage.START_GLIDE);
         }
      }
   }

   private void checkElytraBeforeTakeoff(MinecraftClient client){
      if (!this.hasElytra(client)) {
         this.notifyMessage(client, "Elytra is missing or broken.");
      } else {
         this.applyPitchControl(client.player, this.getMaxSpeed(), false);
         if (client.player.isGliding()) {
            this.beginFlight();
         } else if (client.player.isOnGround()) {
            this.setStage(AutopilotModule.Stage.JUMP);
         } else {
            if (this.stageTicks % 2 == 0) {
               client.player.networkHandler.sendPacket(new ClientCommandC2SPacket(client.player, Mode.START_FALL_FLYING));
            }

            if (this.stageTicks > 24) {
               this.setStage(AutopilotModule.Stage.PREPARE);
            }
         }
      }
   }

   private void checkElytraBeforeFly(MinecraftClient client){
      if (!this.hasElytra(client)) {
         this.notifyMessage(client, "Elytra is missing or broken.");
      } else if (!client.player.isGliding()) {
         RotationManager.clearRotatingState(ROTATION_KEY);
         this.setStage(client.player.isOnGround() ? AutopilotModule.Stage.JUMP : AutopilotModule.Stage.START_GLIDE);
      } else {
         this.adjustSpeed(client);
         double maxSpeed = this.getMaxSpeed();
         double y = maxSpeed - client.player.getY();
         boolean value = this.autoRocketSetting.getValue()
            && this.prepareTicks - this.lastRocketTick >= this.rocketDelaySetting.getValueInt()
            && (
               this.flying
                  || y > this.heightToleranceSetting.getValue()
                  || this.modeSetting.is("Altitude Hold") && horizontalSpeed(client.player.getVelocity()) < this.minSpeedSetting.getValue()
            );
         this.applyPitchControl(client.player, maxSpeed, value);
         if (value) {
            if (!this.canUseRockets(client)) {
               this.notifyMessage(client, "No usable firework remains.");
               return;
            }

            this.lastRocketTick = this.prepareTicks;
            this.flying = false;
         }
      }
   }

   private void beginFlight(){
      this.flying = true;
      this.setStage(AutopilotModule.Stage.FLYING);
   }

   private void applyPitchControl(ClientPlayerEntity player, double effectiveTarget, boolean rocketClimb){
      float valueFloat;
      if (this.modeSetting.is("Standing Fly")) {
         valueFloat = -89.0F;
      } else {
         double y = effectiveTarget - player.getY();
         double value = this.heightToleranceSetting.getValue();
         if (rocketClimb || y > value) {
            valueFloat = this.climbPitchSetting.getValueFloat();
         } else if (y < -value) {
            valueFloat = this.descendPitchSetting.getValueFloat();
         } else if (player.getVelocity().y < -0.12) {
            valueFloat = Math.max(-20.0F, this.climbPitchSetting.getValueFloat() * 0.45F);
         } else if (player.getVelocity().y > 0.18) {
            valueFloat = Math.min(10.0F, this.descendPitchSetting.getValueFloat() * 0.4F);
         } else {
            valueFloat = this.cruisePitchSetting.getValueFloat();
         }
      }

      RotationManager.setRotation(ROTATION_KEY, player.getYaw(), valueFloat, false, this.movementFixSetting.getValue());
   }

   private void adjustSpeed(MinecraftClient client){
      double forwardSpeed = this.getForwardSpeed(client);
      if (forwardSpeed > this.targetSpeed) {
         this.currentSpeed = Math.max(this.currentSpeed, forwardSpeed);
         this.ticksAloft = this.avoidHoldSetting.getValueInt();
      } else {
         if (this.ticksAloft > 0) {
            this.ticksAloft--;
         } else {
            this.currentSpeed = this.targetSpeed;
         }
      }
   }

   private double getForwardSpeed(MinecraftClient client){
      float yaw = client.player.getYaw() * (float) (Math.PI / 180.0);
      double sin = -Math.sin(yaw);
      double cos = Math.cos(yaw);
      double cos2 = cos;
      double var9 = -sin;
      int y = MathHelper.floor(client.player.getY());
      int max = Integer.MIN_VALUE;

      for (int index2 = 2; index2 <= this.obstacleRangeSetting.getValueInt(); index2++) {
         for (int index3 = -1; index3 <= 1; index3++) {
            int x = MathHelper.floor(client.player.getX() + sin * index2 + cos2 * index3 * 0.65);
            int z = MathHelper.floor(client.player.getZ() + cos * index2 + var9 * index3 * 0.65);
            boolean var17 = isBlockSolid(client, new BlockPos(x, y, z)) || isBlockSolid(client, new BlockPos(x, y + 1, z));
            if (var17) {
               int var18 = y + 40;

               for (int index = y; index <= var18; index++) {
                  if (isBlockSolid(client, new BlockPos(x, index, z))) {
                     max = Math.max(max, index);
                  }
               }
            }
         }
      }

      return max == Integer.MIN_VALUE ? this.targetSpeed : max + 1.0 + this.obstacleClearanceSetting.getValue();
   }

   private static boolean isBlockSolid(MinecraftClient client, BlockPos pos){
      return !client.world.getBlockState(pos).getCollisionShape(client.world, pos).isEmpty();
   }

   private double getMaxSpeed(){
      return Math.max(this.targetSpeed, this.currentSpeed);
   }

   private boolean updateTargetSpeed(MinecraftClient client){
      this.targetSpeed = this.altitudeSetting.is("Current") ? client.player.getY() : this.targetYSetting.getValue();
      this.currentSpeed = this.targetSpeed;
      this.ticksAloft = 0;
      this.rocketReady = client.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
      if (!this.rocketReady) {
         this.rocketSlot = findElytraSlot(client);
         if (this.rocketSlot == -1) {
            return false;
         }
      }

      if (this.autoRocketSetting.getValue() && !this.findRocketSlot(client)) {
         return false;
      } else {
         this.rocketCooldown = true;
         return true;
      }
   }

   private boolean hasElytraOrChest(MinecraftClient client){
      if (isUsableElytra(client.player.getEquippedStack(EquipmentSlot.CHEST))) {
         return true;
      } else {
         if (this.rocketSlot < 0 || !isUsableElytra(client.player.getInventory().getStack(this.rocketSlot))) {
            this.rocketSlot = findElytraSlot(client);
         }

         return this.rocketSlot >= 0 && this.swapRocketToHotbar(client, this.rocketSlot) && isUsableElytra(client.player.getEquippedStack(EquipmentSlot.CHEST));
      }
   }

   private boolean hasElytra(MinecraftClient client){
      return isUsableElytra(client.player.getEquippedStack(EquipmentSlot.CHEST));
   }

   private void tickRocketUsage(MinecraftClient client){
      if (this.rocketCooldown
         && !this.rocketReady
         && canInteractInventory(client)
         && client.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)
         && this.rocketSlot >= 0) {
         this.swapRocketToHotbar(client, this.rocketSlot);
      }
   }

   private boolean canUseRockets(MinecraftClient client){
      return !this.isRocketSlotReady(client)
         ? false
         : SilentSlotManager.runWithSlot(client, this.preferredSlot, () -> client.interactionManager.interactItem(client.player, Hand.MAIN_HAND));
   }

   private boolean isRocketSlotReady(MinecraftClient client){
      return this.preferredSlot >= 0 && this.preferredSlot < 9 && client.player.getInventory().getStack(this.preferredSlot).isOf(Items.FIREWORK_ROCKET)
         ? true
         : this.findRocketSlot(client);
   }

   private boolean findRocketSlot(MinecraftClient client){
      int var2 = findHotbarItem(client, Items.FIREWORK_ROCKET);
      if (var2 >= 0) {
         this.preferredSlot = var2;
         return true;
      } else {
         this.useRocket(client);
         int var3 = findInventoryItem(client, Items.FIREWORK_ROCKET);
         int selectedSlot = this.getSelectedSlot(client);
         if (var3 >= 9 && selectedSlot != -1) {
            client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId, toContainerSlot(var3), selectedSlot, SlotActionType.SWAP, client.player);
            if (!client.player.getInventory().getStack(selectedSlot).isOf(Items.FIREWORK_ROCKET)) {
               return false;
            } else {
               this.invSlot = var3;
               this.hotbarSlot = selectedSlot;
               this.preferredSlot = selectedSlot;
               return true;
            }
         } else {
            return false;
         }
      }
   }

   private void useRocket(MinecraftClient client){
      if (this.invSlot >= 0 && this.hotbarSlot >= 0 && canInteractInventory(client)) {
         client.interactionManager
            .clickSlot(client.player.playerScreenHandler.syncId, toContainerSlot(this.invSlot), this.hotbarSlot, SlotActionType.SWAP, client.player);
         this.invSlot = -1;
         this.hotbarSlot = -1;
         this.preferredSlot = -1;
      }
   }

   private boolean swapRocketToHotbar(MinecraftClient client, int inventorySlot){
      if (canInteractInventory(client) && inventorySlot >= 0 && inventorySlot < 36) {
         int var3 = client.player.playerScreenHandler.syncId;
         int var4 = toContainerSlot(inventorySlot);
         client.interactionManager.clickSlot(var3, var4, 0, SlotActionType.PICKUP, client.player);
         client.interactionManager.clickSlot(var3, 6, 0, SlotActionType.PICKUP, client.player);
         if (!client.player.playerScreenHandler.getCursorStack().isEmpty()) {
            client.interactionManager.clickSlot(var3, var4, 0, SlotActionType.PICKUP, client.player);
         }

         return client.player.playerScreenHandler.getCursorStack().isEmpty();
      } else {
         return false;
      }
   }

   private int getSelectedSlot(MinecraftClient client){
      int selectedSlot = client.player.getInventory().getSelectedSlot();

      for (int index2 = 0; index2 < 9; index2++) {
         if (index2 != selectedSlot && index2 != this.rocketSlot && client.player.getInventory().getStack(index2).isEmpty()) {
            return index2;
         }
      }

      for (int index = 0; index < 9; index++) {
         if (index != selectedSlot && index != this.rocketSlot) {
            return index;
         }
      }

      return selectedSlot == this.rocketSlot ? -1 : selectedSlot;
   }

   private static int findElytraSlot(MinecraftClient client){
      for (int index = 0; index < 36; index++) {
         if (isUsableElytra(client.player.getInventory().getStack(index))) {
            return index;
         }
      }

      return -1;
   }

   private static boolean isUsableElytra(ItemStack stack){
      return stack.isOf(Items.ELYTRA) && (!stack.isDamageable() || stack.getDamage() < stack.getMaxDamage() - 1);
   }

   private static int findHotbarItem(MinecraftClient client, Item item){
      for (int index = 0; index < 9; index++) {
         if (client.player.getInventory().getStack(index).isOf(item)) {
            return index;
         }
      }

      return -1;
   }

   private static int findInventoryItem(MinecraftClient client, Item item){
      for (int index = 0; index < 36; index++) {
         if (client.player.getInventory().getStack(index).isOf(item)) {
            return index;
         }
      }

      return -1;
   }

   private static int toContainerSlot(int inventorySlot){
      return inventorySlot < 9 ? 36 + inventorySlot : inventorySlot;
   }

   private static double horizontalSpeed(Vec3d velocity){
      return Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
   }

   private static boolean canInteractInventory(MinecraftClient client){
      return client != null
         && client.player != null
         && client.interactionManager != null
         && client.player.currentScreenHandler == client.player.playerScreenHandler
         && client.player.playerScreenHandler.getCursorStack().isEmpty();
   }

   private static boolean isInGame(MinecraftClient client){
      return client != null
         && client.player != null
         && client.world != null
         && client.interactionManager != null
         && client.player.networkHandler != null
         && !client.player.isDead()
         && !client.player.hasVehicle()
         && !client.player.getAbilities().flying;
   }

   private void notifyMessage(MinecraftClient client, String message){
      client.player.sendMessage(Text.literal("Â§8[Â§dAutopilotÂ§8] Â§c" + message), false);
      if (this.disableIfMissingSetting.getValue()) {
         this.disable();
      } else {
         RotationManager.clearRotatingState(ROTATION_KEY);
         this.setStage(AutopilotModule.Stage.PREPARE);
      }
   }

   private void setStage(AutopilotModule.Stage next){
      this.stage = next;
      this.stageTicks = 0;
   }

   private void resetToPrepare(){
      this.stage = AutopilotModule.Stage.PREPARE;
      this.stageTicks = 0;
      this.prepareTicks = 0;
      this.lastRocketTick = -1073741824;
      this.rocketSlot = -1;
      this.preferredSlot = -1;
      this.invSlot = -1;
      this.hotbarSlot = -1;
      this.rocketCooldown = false;
      this.rocketReady = false;
      this.flying = false;
      this.targetSpeed = 0.0;
      this.currentSpeed = 0.0;
      this.ticksAloft = 0;
   }

   @Environment(EnvType.CLIENT)
   private static enum Stage {
      PREPARE,
      JUMP,
      START_GLIDE,
      FLYING;
   }
}

