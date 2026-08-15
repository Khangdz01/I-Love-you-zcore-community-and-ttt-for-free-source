package made4mischief.astatine.client.modules.combat;

import java.util.ArrayList;
import java.util.List;
import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.modules.player.FriendModule;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.EntityTargetSetting;
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
import net.minecraft.util.PlayerInput;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.util.Hand;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;

@Environment(EnvType.CLIENT)
public final class ElytraMaceModule extends Module {
   private static ElytraMaceModule instance;
   private static final Object ROTATION_STATE = new Object();
   private static final int GLIDE_CHECK_TICKS = 6;
   private static final int HIT_STAGE_TICKS = 1;
   private static final int DIVE_TICK_LIMIT = 50;
   private static final int TURBO_ROCKET_TICKS = 160;
   private static final int RESTART_TICKS = 180;
   private static final int PREPARE_TICKS = 14;
   private static final int STAGE_TIMEOUT = 24;
   private static final int ROCKET_RETRY_TICKS = 10;
   private static final int HIT_COOLDOWN_TICKS = 2;
   private static final double AIM_RANGE = 1.5;
   private static final double AIM_OFFSET = 0.5;
   private static final double DESCEND_VELOCITY_THRESHOLD = Math.sqrt(0.5);
   private final NumberSetting targetRangeSetting = this.addNumber("Target Range", 24.0, 4.0, 64.0, 1.0);
   private final EntityTargetSetting targetSetting = this.addSetting(new EntityTargetSetting("Targets", EntityType.PLAYER));
   private final ModeSetting cycleModeSetting = this.addMode("Cycle Mode", "Normal", new String[]{"Normal", "Turbo"});
   private final NumberSetting climbHeightSetting = this.addNumber("Climb Height", 20.0, 8.0, 80.0, 1.0);
   private final NumberSetting armorHeightSetting = this.addNumber("Armor Height", 6.0, 2.0, 12.0, 0.5);
   private final NumberSetting attackRangeSetting = this.addNumber("Attack Range", 3.5, 2.5, 5.0, 0.1);
   private final NumberSetting approachRangeSetting = this.addNumber("Approach Range", 4.5, 2.5, 8.0, 0.25);
   private final NumberSetting minFallDistanceSetting = this.addNumber("Min Fall Distance", 3.0, 1.5, 12.0, 0.5);
   private final NumberSetting rocketRetrySetting = this.addNumber("Rocket Retry", 30.0, 15.0, 60.0, 1.0);
   private final NumberSetting attackChargeSetting = this.addNumber("Attack Charge", 0.9, 0.1, 1.0, 0.05);
   private final NumberSetting hitCooldownSetting = this.addNumber("Hit Cooldown", 0.0, 0.0, 100.0, 1.0);
   private final NumberSetting rotationToleranceSetting = this.addNumber("Rotation Tolerance", 30.0, 1.0, 30.0, 0.5);
   private final BooleanSetting rotateModelSetting = this.addBoolean("Rotate Model", true);
   private final BooleanSetting requireWindBurstSetting = this.addBoolean("Require Wind Burst", true);
   private final BooleanSetting pauseOnEatSetting = this.addBoolean("Pause On Eat", true);
   private final BooleanSetting disableIfMissingSetting = this.addBoolean("Disable If Missing", true);
   private final List<ElytraMaceModule.ManagedHotbarSwap> managedHotbarSwaps = new ArrayList<>();
   private ElytraMaceModule.Stage stage = ElytraMaceModule.Stage.PREPARE;
   private LivingEntity target;
   private int diveTicks;
   private int stageTicks;
   private int lastRocketUseTick;
   private int nextActionTick;
   private int lastHitTick;
   private int maceSlot = -1;
   private int rocketSlot = -1;
   private double diveStartY;
   private double maxDescendVelocity = Double.NaN;
   private boolean paused;
   private boolean diving;
   private boolean turboActive;
   private boolean turboCharging;
   private Item lastChestItem;
   private boolean chestWasEmpty;

   public ElytraMaceModule(){
      super("ElytraMace", Category.COMBAT, "Tá»± bá»• nhÃ o Elytra vÃ  táº¥n cÃ´ng báº±ng chÃ¹y.", -1, true);
      this.hitCooldownSetting.visibleWhen(() -> this.cycleModeSetting.is("Normal"));
      instance = this;
   }

   @Override
   protected void onEnable(){
      RotationManager.clearRotatingState(ROTATION_STATE);
      this.resetToPrepare();
      if (mc.player != null) {
         ItemStack stack = mc.player.getEquippedStack(EquipmentSlot.CHEST);
         this.chestWasEmpty = stack.isEmpty();
         this.lastChestItem = stack.isEmpty() ? null : stack.getItem();
      }
   }

   @Override
   protected void onDisable(){
      RotationManager.clearRotatingState(ROTATION_STATE);
      if (isInGame(mc)) {
         this.restoreChestSlot(mc);
         this.cleanupHotbarSwaps(mc);
      }

      this.resetToPrepare();
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      if (!isInGame(client)) {
         this.target = null;
         RotationManager.clearRotatingState(ROTATION_STATE);
      } else if (this.pauseOnEatSetting.getValue() && isPlayerUsingItem(client.player)) {
         RotationManager.clearRotatingState(ROTATION_STATE);
      } else {
         this.diveTicks++;
         this.stageTicks++;
         this.isTargetValid(client);
         if (this.target == null) {
            this.resetComboState();
         } else {
            switch (this.stage) {
               case PREPARE:
                  this.handleTargetState(client);
                  break;
               case JUMP:
                  this.checkElytraAvailable(client);
                  break;
               case START_GLIDE:
                  this.tickGlideAim(client);
                  break;
               case CLIMB:
                  this.tickPrepareGlide(client);
                  break;
               case DIVE:
                  this.tickRestartGlide(client);
                  break;
               case ARM_ATTACK:
                  this.tickDiveAim(client);
                  break;
               case WAIT_BOUNCE:
                  this.tickHitAim(client);
                  break;
               case RESTART_GLIDE:
                  this.tickRestart(client);
            }
         }
      }
   }

   private void handleTargetState(MinecraftClient client){
      if (this.target != null) {
         if (!canInteractInventory(client)) {
            RotationManager.clearRotatingState(ROTATION_STATE);
         } else if (!this.canStartCombo(client)) {
            this.notifyMessage(client, "Need Elytra, firework, mace and chest armor" + (this.requireWindBurstSetting.getValue() ? " (mace needs Wind Burst)." : "."));
         } else if (!this.tryEquipElytra(client)) {
            this.notifyMessage(client, "Could not equip Elytra.");
         } else {
            if (client.player.isGliding()) {
               this.diving = true;
               this.setStage(ElytraMaceModule.Stage.CLIMB);
            } else if (client.player.isOnGround()) {
               this.setStage(ElytraMaceModule.Stage.JUMP);
            } else {
               this.setStage(ElytraMaceModule.Stage.START_GLIDE);
            }
         }
      }
   }

   private void checkElytraAvailable(MinecraftClient client){
      if (this.target != null) {
         if (!isElytraEquipped(client) && !this.tryEquipElytra(client)) {
            this.notifyMessage(client, "Elytra is no longer available.");
         } else {
            this.getCurrentAim(client);
            if (client.player.isGliding()) {
               this.diving = true;
               this.setStage(ElytraMaceModule.Stage.CLIMB);
            } else {
               if (client.player.isOnGround()) {
                  this.paused = true;
               } else {
                  this.setStage(ElytraMaceModule.Stage.START_GLIDE);
               }
            }
         }
      }
   }

   private void tickGlideAim(MinecraftClient client){
      if (this.target != null) {
         this.getCurrentAim(client);
         if (client.player.isGliding()) {
            this.diving = true;
            this.setStage(ElytraMaceModule.Stage.CLIMB);
         } else if (client.player.isOnGround()) {
            if (this.stageTicks > 2) {
               this.paused = false;
               this.setStage(ElytraMaceModule.Stage.JUMP);
            }
         } else if (!isElytraEquipped(client) && !this.tryEquipElytra(client)) {
            this.notifyMessage(client, "Elytra is no longer available.");
         } else {
            if (this.stageTicks > 50) {
               this.tryStartGlide(client);
            }
         }
      }
   }

   private void tickPrepareGlide(MinecraftClient client){
      if (this.target == null) {
         this.tryStartGlide(client);
      } else if (!client.player.isGliding()) {
         this.setStage(ElytraMaceModule.Stage.START_GLIDE);
      } else {
         double value = this.target.getY() + this.climbHeightSetting.getValue();
         double entityPos = horizontalDistance(client.player.getEntityPos(), this.target.getEntityPos());
         if (client.player.getY() >= value && entityPos <= 1.5) {
            if (this.diveTicks >= this.nextActionTick) {
               this.startDive();
            } else {
               this.aimHorizontal(client);
               this.diving = false;
            }
         } else {
            ElytraMaceModule.AimRotation var6 = this.getCurrentAim(client);
            boolean valueInt = this.diving || this.diveTicks - this.lastRocketUseTick >= this.rocketRetrySetting.getValueInt();
            if (valueInt && RotationManager.wasRotationSent(var6.yaw(), var6.pitch(), this.rotationToleranceSetting.getValueFloat())) {
               if (!this.useFireworkRocket(client)) {
                  this.notifyMessage(client, "No usable firework remains.");
                  return;
               }

               this.diving = false;
            }

            if (this.stageTicks > 160) {
               if (this.diveTicks >= this.nextActionTick
                  && client.player.getY() > this.target.getY() + this.armorHeightSetting.getValue() + 2.0
                  && entityPos <= Math.max(1.5, this.approachRangeSetting.getValue())) {
                  this.startDive();
               } else {
                  this.tryStartGlide(client);
               }
            }
         }
      }
   }

   private void tickRestartGlide(MinecraftClient client){
      if (this.target == null) {
         this.tryStartGlide(client);
      } else if (!client.player.isGliding()) {
         this.tryStartGlide(client);
      } else {
         ElytraMaceModule.AimRotation var2 = this.aimAtTarget(client);
         if (this.diving && RotationManager.wasRotationSent(var2.yaw(), var2.pitch(), this.rotationToleranceSetting.getValueFloat())) {
            if (!this.useFireworkRocket(client)) {
               this.notifyMessage(client, "No usable firework remains for the dive.");
               return;
            }

            this.diving = false;
         }

         double y = client.player.getY() - this.target.getY();
         double entityPos = horizontalDistance(client.player.getEntityPos(), this.target.getEntityPos());
         boolean value = client.player.getVelocity().y < -0.08
            && y <= this.armorHeightSetting.getValue()
            && y > -1.5
            && entityPos <= this.approachRangeSetting.getValue()
            && client.player.fallDistance >= this.minFallDistanceSetting.getValue();
         if (value) {
            if (!this.isChestElytraEquipped(client)) {
               this.notifyMessage(client, "Chest armor is no longer available.");
            } else {
               this.setStage(ElytraMaceModule.Stage.ARM_ATTACK);
            }
         } else {
            if (client.player.isOnGround() || client.player.getY() < this.target.getY() - 3.0 || this.stageTicks > 180) {
               this.tryStartGlide(client);
            }
         }
      }
   }

   private void tickDiveAim(MinecraftClient client){
      if (this.target == null) {
         this.tryStartGlide(client);
      } else {
         ElytraMaceModule.AimRotation var2 = this.aimAtTarget(client);
         if (this.stageTicks >= 1
            && this.diveTicks >= this.nextActionTick
            && client.player.fallDistance >= this.minFallDistanceSetting.getValue()
            && client.player.getAttackCooldownProgress(0.0F) >= this.attackChargeSetting.getValueFloat()
            && this.isInAttackRange(client, this.target)
            && RotationManager.wasRotationSent(var2.yaw(), var2.pitch(), this.rotationToleranceSetting.getValueFloat())) {
            if (!this.prepareMaceSlot(client)) {
               this.notifyMessage(client, "Mace is no longer available.");
            } else {
               boolean attackEntity = SilentSlotManager.runWithSlot(client, this.maceSlot, () -> {
                  client.interactionManager.attackEntity(client.player, this.target);
                  client.player.swingHand(Hand.MAIN_HAND);
               });
               if (attackEntity) {
                  this.diveStartY = client.player.getY();
                  this.nextActionTick = this.diveTicks + this.getHitCooldown();
                  this.turboActive = false;
                  this.lastHitTick = -1073741824;
                  this.maxDescendVelocity = Double.NaN;
                  this.setStage(ElytraMaceModule.Stage.WAIT_BOUNCE);
               }
            }
         } else {
            if (client.player.isOnGround() || client.player.getY() < this.target.getY() - 2.0 || this.stageTicks > 14) {
               this.tryStartGlide(client);
            }
         }
      }
   }

   private void tickHitAim(MinecraftClient client){
      if (this.target != null) {
         this.aimAtTarget(client);
      }

      if (this.cycleModeSetting.is("Turbo") && this.turboActive && this.isDescendingSlowly(client)) {
         this.startTurboDive(client);
      } else {
         boolean y = this.turboActive
            && this.diveTicks - this.lastHitTick >= this.getCycleOffset()
            && (client.player.getVelocity().y > 0.08 || client.player.getY() > this.diveStartY + 0.2);
         if (y) {
            if (!this.tryEquipElytra(client)) {
               this.notifyMessage(client, "Could not re-equip Elytra after the mace hit.");
            } else {
               this.turboCharging = true;
               this.setStage(ElytraMaceModule.Stage.RESTART_GLIDE);
            }
         } else {
            if (this.stageTicks > 24 || !this.turboActive && client.player.isOnGround() && this.stageTicks > 10) {
               this.tryStartGlide(client);
            }
         }
      }
   }

   private boolean isDescendingSlowly(MinecraftClient client){
      double velocity = client.player.getVelocity().y;
      return Double.isFinite(this.maxDescendVelocity) && !(this.maxDescendVelocity <= 0.0) ? velocity <= this.maxDescendVelocity * DESCEND_VELOCITY_THRESHOLD : velocity <= 0.0;
   }

   private void startTurboDive(MinecraftClient client){
      if (!this.tryEquipElytra(client)) {
         this.notifyMessage(client, "Could not equip Elytra for the turbo dive.");
      } else {
         client.player.startGliding();
         client.player.networkHandler.sendPacket(new ClientCommandC2SPacket(client.player, Mode.START_FALL_FLYING));
         this.turboCharging = true;
         this.setStage(ElytraMaceModule.Stage.RESTART_GLIDE);
         this.tickRestart(client);
      }
   }

   private void tickRestart(MinecraftClient client){
      if (this.target == null) {
         this.tryStartGlide(client);
      } else if (!isElytraEquipped(client) && !this.tryEquipElytra(client)) {
         this.notifyMessage(client, "Elytra is no longer available.");
      } else {
         if (this.turboCharging && this.diveTicks >= this.nextActionTick) {
            this.aimAtTarget(client);
         } else if (this.turboCharging) {
            this.aimHorizontal(client);
         } else {
            this.getCurrentAim(client);
         }

         if (client.player.isGliding()) {
            if (this.turboCharging) {
               if (this.diveTicks >= this.nextActionTick) {
                  this.turboCharging = false;
                  this.startDive();
                  if (this.cycleModeSetting.is("Turbo")) {
                     this.tickRestartGlide(client);
                  }
               }
            } else {
               this.diving = true;
               this.setStage(ElytraMaceModule.Stage.CLIMB);
            }
         } else if (client.player.isOnGround()) {
            this.tryStartGlide(client);
         } else {
            if (this.stageTicks > 50) {
               this.tryStartGlide(client);
            }
         }
      }
   }

   private boolean canStartCombo(MinecraftClient client){
      if (!hasElytra(client) || !hasItemInInventory(client, Items.FIREWORK_ROCKET) || !hasItemInInventory(client, Items.MACE) || !hasChestEquippable(client)) {
         return false;
      } else {
         return this.requireWindBurstSetting.getValue() && !this.hasWindBurstMace(client) ? false : this.prepareMaceSlot(client) && this.prepareRocketSlot(client);
      }
   }

   private boolean prepareMaceSlot(MinecraftClient client){
      this.maceSlot = this.findMaceSlotInRange(client, 0, 9);
      if (this.maceSlot == -1) {
         int findMaceSlotInRange = this.findMaceSlotInRange(client, 9, 36);
         this.maceSlot = this.swapItemToSlot(client, findMaceSlotInRange, this.rocketSlot);
      }

      return this.maceSlot >= 0 && this.isMaceUsable(client.player.getInventory().getStack(this.maceSlot));
   }

   private boolean prepareRocketSlot(MinecraftClient client){
      this.rocketSlot = InventoryUtil.findHotBarItem(client, Items.FIREWORK_ROCKET);
      if (this.rocketSlot == -1) {
         this.rocketSlot = this.findItemSlotAvoiding(client, Items.FIREWORK_ROCKET, this.maceSlot);
      }

      return this.rocketSlot >= 0 && client.player.getInventory().getStack(this.rocketSlot).isOf(Items.FIREWORK_ROCKET);
   }

   private int findItemSlotAvoiding(MinecraftClient client, Item item, int avoidSlot){
      int var4 = findItemSlot(client, item);
      return var4 < 9 ? var4 : this.swapItemToSlot(client, var4, avoidSlot);
   }

   private int swapItemToSlot(MinecraftClient client, int source, int avoidSlot){
      if (!canInteractInventory(client)) {
         return -1;
      } else {
         int findEmptyHotbarSlotAvoiding = this.findEmptyHotbarSlotAvoiding(client, avoidSlot);
         if (source != -1 && findEmptyHotbarSlotAvoiding != -1) {
            Item item = client.player.getInventory().getStack(source).getItem();
            client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId, toContainerSlot(source), findEmptyHotbarSlotAvoiding, SlotActionType.SWAP, client.player);
            if (!client.player.getInventory().getStack(findEmptyHotbarSlotAvoiding).isOf(item)) {
               return -1;
            } else {
               this.managedHotbarSwaps.add(new ElytraMaceModule.ManagedHotbarSwap(source, findEmptyHotbarSlotAvoiding));
               return findEmptyHotbarSlotAvoiding;
            }
         } else {
            return -1;
         }
      }
   }

   private int findEmptyHotbarSlotAvoiding(MinecraftClient client, int avoidSlot){
      int selectedSlot = client.player.getInventory().getSelectedSlot();

      for (int index2 = 0; index2 < 9; index2++) {
         if (index2 != avoidSlot && index2 != selectedSlot && client.player.getInventory().getStack(index2).isEmpty()) {
            return index2;
         }
      }

      for (int index = 0; index < 9; index++) {
         if (index != avoidSlot && index != selectedSlot) {
            return index;
         }
      }

      return selectedSlot == avoidSlot ? -1 : selectedSlot;
   }

   private boolean useFireworkRocket(MinecraftClient client){
      if (!this.prepareRocketSlot(client)) {
         return false;
      } else {
         boolean interactItem = SilentSlotManager.runWithSlot(client, this.rocketSlot, () -> client.interactionManager.interactItem(client.player, Hand.MAIN_HAND));
         if (interactItem) {
            this.lastRocketUseTick = this.diveTicks;
         }

         return interactItem;
      }
   }

   private boolean tryEquipElytra(MinecraftClient client){
      if (isElytraEquipped(client)) {
         return true;
      } else {
         int var2 = findItemSlot(client, Items.ELYTRA);
         return var2 >= 0 && this.swapInventoryToHotbar(client, var2) && isElytraEquipped(client);
      }
   }

   private boolean isChestElytraEquipped(MinecraftClient client){
      ItemStack stack = client.player.getEquippedStack(EquipmentSlot.CHEST);
      if (isChestEquippable(stack)) {
         return true;
      } else {
         int var3 = findChestItemSlot(client, this.lastChestItem);
         if (var3 == -1) {
            var3 = findChestItemSlot(client, null);
         }

         return var3 >= 0 && this.swapInventoryToHotbar(client, var3) && isChestEquippable(client.player.getEquippedStack(EquipmentSlot.CHEST));
      }
   }

   private boolean swapInventoryToHotbar(MinecraftClient client, int inventorySlot){
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

   private void restoreChestSlot(MinecraftClient client){
      if (canInteractInventory(client)) {
         ItemStack stack = client.player.getEquippedStack(EquipmentSlot.CHEST);
         if (this.chestWasEmpty) {
            if (!stack.isEmpty()) {
               int var4 = findEmptyInventorySlot(client);
               if (var4 >= 0) {
                  this.swapInventoryToHotbar(client, var4);
               }
            }
         } else if (this.lastChestItem != null && !stack.isOf(this.lastChestItem)) {
            int var3 = findItemSlot(client, this.lastChestItem);
            if (var3 >= 0) {
               this.swapInventoryToHotbar(client, var3);
            }
         }
      }
   }

   private void cleanupHotbarSwaps(MinecraftClient client){
      if (canInteractInventory(client)) {
         for (int index = this.managedHotbarSwaps.size() - 1; index >= 0; index--) {
            ElytraMaceModule.ManagedHotbarSwap var3 = this.managedHotbarSwaps.get(index);
            client.interactionManager
               .clickSlot(
                  client.player.playerScreenHandler.syncId, toContainerSlot(var3.inventorySlot()), var3.hotbarSlot(), SlotActionType.SWAP, client.player
               );
         }

         this.managedHotbarSwaps.clear();
      }
   }

   private void tryStartGlide(MinecraftClient client){
      if (isInGame(client)) {
         this.turboCharging = false;
         if (client.player.isOnGround()) {
            if (!this.tryEquipElytra(client)) {
               this.notifyMessage(client, "Could not recover Elytra.");
               return;
            }

            this.paused = false;
            this.setStage(ElytraMaceModule.Stage.JUMP);
         } else {
            if (!this.tryEquipElytra(client)) {
               this.notifyMessage(client, "Could not recover Elytra.");
               return;
            }

            this.setStage(ElytraMaceModule.Stage.RESTART_GLIDE);
         }
      }
   }

   private void isTargetValid(MinecraftClient client){
      if (this.target == null
         || client.world.getEntityById(this.target.getId()) != this.target
         || !this.target.isAlive()
         || this.target.isSpectator()
         || FriendModule.isFriend(this.target)
         || !this.targetSetting.isSelected(this.target.getType())) {
         this.target = null;
         double value = this.targetRangeSetting.getValue();
         double squaredDistanceTo2 = value * value;

         for (LivingEntity entity : client.world
            .getEntitiesByClass(
               LivingEntity.class,
               client.player.getBoundingBox().expand(value),
               entity -> entity != client.player
                  && entity.isAlive()
                  && !entity.isSpectator()
                  && !FriendModule.isFriend(entity)
                  && this.targetSetting.isSelected(entity.getType())
            )) {
            double squaredDistanceTo = client.player.squaredDistanceTo(entity);
            if (squaredDistanceTo <= squaredDistanceTo2) {
               this.target = entity;
               squaredDistanceTo2 = squaredDistanceTo;
            }
         }
      }
   }

   private ElytraMaceModule.AimRotation getCurrentAim(MinecraftClient client){
      if (this.target == null) {
         return new ElytraMaceModule.AimRotation(client.player.getYaw(), client.player.getPitch());
      } else {
         Vec3d vec = new Vec3d(this.target.getX(), this.target.getY() + this.climbHeightSetting.getValue(), this.target.getZ());
         float eyePos2 = RotationUtil.getYaw(client.player.getEyePos(), vec);
         float eyePos = RotationUtil.getPitch(client.player.getEyePos(), vec);
         return this.rotateAndStore(eyePos2, eyePos);
      }
   }

   private ElytraMaceModule.AimRotation aimHorizontal(MinecraftClient client){
      float entityPos = RotationUtil.getYaw(client.player.getEyePos(), this.target.getEntityPos());
      return this.rotateAndStore(entityPos, 0.0F);
   }

   private ElytraMaceModule.AimRotation aimAtTarget(MinecraftClient client){
      Vec3d vec = this.target.getEntityPos().add(0.0, Math.max(0.2, this.target.getHeight() * 0.55), 0.0);
      float eyePos2 = RotationUtil.getYaw(client.player.getEyePos(), vec);
      float eyePos = RotationUtil.getPitch(client.player.getEyePos(), vec);
      return this.rotateAndStore(eyePos2, eyePos);
   }

   private ElytraMaceModule.AimRotation rotateAndStore(float yaw, float pitch){
      RotationManager.setRotation(ROTATION_STATE, yaw, pitch, this.rotateModelSetting.getValue(), true);
      return new ElytraMaceModule.AimRotation(yaw, pitch);
   }

   private void startDive(){
      this.diving = true;
      this.setStage(ElytraMaceModule.Stage.DIVE);
   }

   private int getCycleOffset(){
      return this.cycleModeSetting.is("Turbo") ? 0 : 2;
   }

   private int getHitCooldown(){
      return this.cycleModeSetting.is("Turbo") ? 0 : this.hitCooldownSetting.getValueInt();
   }

   private void resetComboState(){
      this.paused = false;
      this.diving = false;
      this.turboCharging = false;
      this.turboActive = false;
      this.lastHitTick = -1073741824;
      this.maxDescendVelocity = Double.NaN;
      this.setStage(ElytraMaceModule.Stage.PREPARE);
      RotationManager.clearRotatingState(ROTATION_STATE);
   }

   public static void applyTakeoffInput(ClientPlayerEntity player){
      ElytraMaceModule elytraMaceModule = instance;
      if (elytraMaceModule != null && elytraMaceModule.isEnabled() && elytraMaceModule.target != null && player != null && MinecraftClient.getInstance().currentScreen == null) {
         boolean onGround = elytraMaceModule.stage == ElytraMaceModule.Stage.JUMP && player.isOnGround();
         boolean velocity = (elytraMaceModule.stage == ElytraMaceModule.Stage.START_GLIDE || elytraMaceModule.stage == ElytraMaceModule.Stage.RESTART_GLIDE)
            && !player.isOnGround()
            && !player.isGliding()
            && (elytraMaceModule.shouldRestartTurbo() || player.getVelocity().y <= 0.1)
            && (elytraMaceModule.stageTicks + 1) % 1 == 0;
         if (onGround || velocity) {
            PlayerInput playerInput = player.input.playerInput;
            player.input.playerInput = new PlayerInput(
               playerInput.forward(), playerInput.backward(), playerInput.left(), playerInput.right(), true, playerInput.sneak(), playerInput.sprint()
            );
         }
      }
   }

   private boolean shouldRestartTurbo(){
      return this.cycleModeSetting.is("Turbo") && this.turboCharging && this.stage == ElytraMaceModule.Stage.RESTART_GLIDE;
   }

   public static boolean acceptExpectedWindBurst(EntityVelocityUpdateS2CPacket packet){
      ElytraMaceModule elytraMaceModule = instance;
      MinecraftClient client = MinecraftClient.getInstance();
      if (elytraMaceModule != null
         && elytraMaceModule.isEnabled()
         && elytraMaceModule.stage == ElytraMaceModule.Stage.WAIT_BOUNCE
         && elytraMaceModule.stageTicks <= 24
         && client.player != null
         && packet.getEntityId() == client.player.getId()
         && !(packet.getVelocity().y <= 0.08)) {
         elytraMaceModule.turboActive = true;
         elytraMaceModule.lastHitTick = elytraMaceModule.diveTicks;
         elytraMaceModule.maxDescendVelocity = Math.max(0.0, packet.getVelocity().y);
         return true;
      } else {
         return false;
      }
   }

   private boolean isInAttackRange(MinecraftClient client, LivingEntity entityTarget){
      double value = this.attackRangeSetting.getValue();
      return entityTarget.getBoundingBox().squaredMagnitude(client.player.getEyePos()) <= value * value;
   }

   private boolean hasWindBurstMace(MinecraftClient client){
      for (int index = 0; index < 36; index++) {
         ItemStack stack = client.player.getInventory().getStack(index);
         if (stack.isOf(Items.MACE) && hasWindBurstEnchantment(stack)) {
            return true;
         }
      }

      return false;
   }

   private int findMaceSlotInRange(MinecraftClient client, int start, int end){
      for (int index = start; index < end; index++) {
         if (this.isMaceUsable(client.player.getInventory().getStack(index))) {
            return index;
         }
      }

      return -1;
   }

   private boolean isMaceUsable(ItemStack stack){
      return stack.isOf(Items.MACE) && (!this.requireWindBurstSetting.getValue() || hasWindBurstEnchantment(stack));
   }

   private static boolean hasWindBurstEnchantment(ItemStack stack){
      ItemEnchantmentsComponent itemEnchantmentsComponent = EnchantmentHelper.getEnchantments(stack);

      for (RegistryEntry registryEntry : itemEnchantmentsComponent.getEnchantments()) {
         if (registryEntry.matchesKey(Enchantments.WIND_BURST) && itemEnchantmentsComponent.getLevel(registryEntry) > 0) {
            return true;
         }
      }

      return false;
   }

   private static boolean hasItemInInventory(MinecraftClient client, Item item){
      return findItemSlot(client, item) >= 0;
   }

   private static boolean hasElytra(MinecraftClient client){
      return client.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA) || hasItemInInventory(client, Items.ELYTRA);
   }

   private static boolean hasChestEquippable(MinecraftClient client){
      return isChestEquippable(client.player.getEquippedStack(EquipmentSlot.CHEST)) ? true : findChestItemSlot(client, null) >= 0;
   }

   private static boolean isElytraEquipped(MinecraftClient client){
      return client.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
   }

   private static boolean isChestEquippable(ItemStack stack){
      if (!stack.isEmpty() && !stack.isOf(Items.ELYTRA)) {
         EquippableComponent equippableComponent = (EquippableComponent)stack.get(DataComponentTypes.EQUIPPABLE);
         return equippableComponent != null && equippableComponent.slot() == EquipmentSlot.CHEST;
      } else {
         return false;
      }
   }

   private static int findChestItemSlot(MinecraftClient client, Item preferredItem){
      for (int index = 0; index < 36; index++) {
         ItemStack stack = client.player.getInventory().getStack(index);
         if (isChestEquippable(stack) && (preferredItem == null || stack.isOf(preferredItem))) {
            return index;
         }
      }

      return -1;
   }

   private static int findItemSlot(MinecraftClient client, Item item){
      for (int index = 0; index < 36; index++) {
         if (client.player.getInventory().getStack(index).isOf(item)) {
            return index;
         }
      }

      return -1;
   }

   private static int findEmptyInventorySlot(MinecraftClient client){
      for (int index = 9; index < 36; index++) {
         if (client.player.getInventory().getStack(index).isEmpty()) {
            return index;
         }
      }

      return -1;
   }

   private static int toContainerSlot(int inventorySlot){
      return inventorySlot < 9 ? 36 + inventorySlot : inventorySlot;
   }

   private void notifyMessage(MinecraftClient client, String message){
      client.player.sendMessage(Text.literal("Â§8[Â§dElytraMaceÂ§8] Â§c" + message), false);
      if (this.disableIfMissingSetting.getValue()) {
         this.disable();
      } else {
         this.setStage(ElytraMaceModule.Stage.PREPARE);
      }
   }

   private void setStage(ElytraMaceModule.Stage next){
      this.stage = next;
      this.stageTicks = 0;
   }

   private void resetToPrepare(){
      this.stage = ElytraMaceModule.Stage.PREPARE;
      this.target = null;
      this.diveTicks = 0;
      this.stageTicks = 0;
      this.lastRocketUseTick = -1073741824;
      this.nextActionTick = 0;
      this.lastHitTick = -1073741824;
      this.maceSlot = -1;
      this.rocketSlot = -1;
      this.diveStartY = 0.0;
      this.maxDescendVelocity = Double.NaN;
      this.paused = false;
      this.diving = false;
      this.turboActive = false;
      this.turboCharging = false;
      this.lastChestItem = null;
      this.chestWasEmpty = true;
      this.managedHotbarSwaps.clear();
   }

   private static double horizontalDistance(Vec3d first, Vec3d second){
      double var2 = first.x - second.x;
      double var4 = first.z - second.z;
      return Math.sqrt(var2 * var2 + var4 * var4);
   }

   private static boolean isPlayerUsingItem(PlayerEntity player){
      if (!player.isUsingItem()) {
         return false;
      } else {
         UseAction useAction = player.getActiveItem().getUseAction();
         return useAction == UseAction.EAT || useAction == UseAction.DRINK;
      }
   }

   private static boolean canInteractInventory(MinecraftClient client){
      return isInGame(client)
         && client.interactionManager != null
         && client.player.currentScreenHandler == client.player.playerScreenHandler
         && client.player.playerScreenHandler.getCursorStack().isEmpty();
   }

   private static boolean isInGame(MinecraftClient client){
      return client != null
         && client.player != null
         && client.world != null
         && client.player.networkHandler != null
         && !client.player.isDead();
   }

   @Environment(EnvType.CLIENT)
   private record AimRotation(float yaw, float pitch){
   }

   @Environment(EnvType.CLIENT)
   private record ManagedHotbarSwap(int inventorySlot, int hotbarSlot){
   }

   @Environment(EnvType.CLIENT)
   private static enum Stage {
      PREPARE,
      JUMP,
      START_GLIDE,
      CLIMB,
      DIVE,
      ARM_ATTACK,
      WAIT_BOUNCE,
      RESTART_GLIDE;
   }
}

