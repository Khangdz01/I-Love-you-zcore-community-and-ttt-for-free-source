package made4mischief.astatine.client.modules.player;

import made4mischief.astatine.client.mixin.ClientWorldAccessor;
import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.ModeSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import made4mischief.astatine.client.utils.inventory.InventoryUtil;
import made4mischief.astatine.client.utils.rotation.RotationManager;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Hand;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PendingUpdateManager;

@Environment(EnvType.CLIENT)
public final class AutoExpModule extends Module {
   private static final Object ROTATION_STATE = new Object();
   private static final float LOOK_PITCH = 90.0F;
   private static final float PITCH_TOLERANCE = 1.0F;
   private static final EquipmentSlot[] EXP_ARMOR_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   private final ModeSetting modeSetting = this.addMode("Mode", "Packet", new String[]{"Silent", "Packet"});
   private final NumberSetting throwDelaySetting = this.addNumber("Throw Delay", 0.0, 0.0, 20.0, 1.0);
   private final BooleanSetting silentRotateDownSetting = this.addBoolean("Silent Rotate Down", true);
   private final BooleanSetting pauseInGUISetting = this.addBoolean("Pause In GUI", false);
   private int expBottleSlot = -1;
   private int throwDelayTicks;
   private boolean active;
   private float yaw;

   public AutoExpModule(){
      super("AutoExp", Category.PLAYER, "Tự sửa giáp bằng chai kinh nghiệm.");
   }

   @Override
   protected void onEnable(){
      this.stopRotating();
      this.resetBottleSlot();
   }

   @Override
   protected void onDisable(){
      this.tickThrowSequence(MinecraftClient.getInstance());
      this.stopRotating();
      this.resetBottleSlot();
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      if (!this.isInGame(client)) {
         this.stopRotating();
         this.resetBottleSlot();
      } else if (this.pauseInGUISetting.getValue() && client.currentScreen != null) {
         this.stopRotating();
      } else {
         if (this.modeSetting.is("Packet")) {
            this.tickThrowSequence(client);
         }

         if (!this.hasExpArmor(client)) {
            this.tickThrowSequence(client);
            this.stopRotating();
            this.throwDelayTicks = 0;
         } else if (this.throwDelayTicks > 0) {
            this.tickAutoThrow();
            this.throwDelayTicks--;
         } else {
            int findExpBottleSlot = this.findExpBottleSlot(client);
            if (findExpBottleSlot == -1) {
               this.tickThrowSequence(client);
               this.stopRotating();
            } else {
               if (this.silentRotateDownSetting.getValue()) {
                  if (!this.canThrow(client)) {
                     return;
                  }
               } else {
                  this.stopRotating();
               }

               if (this.modeSetting.is("Packet")) {
                  this.throwBottle(client, findExpBottleSlot);
               } else {
                  this.startThrowing(client, findExpBottleSlot);
               }

               this.tickAutoThrow();
               this.throwDelayTicks = this.throwDelaySetting.getValueInt();
            }
         }
      }
   }

   private void startThrowing(MinecraftClient client, int expSlot){
      if (this.expBottleSlot == -1) {
         this.expBottleSlot = client.player.getInventory().getSelectedSlot();
      }

      if (client.player.getInventory().getSelectedSlot() != expSlot) {
         client.player.getInventory().setSelectedSlot(expSlot);
         client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(expSlot));
      }

      this.tickMaintain(client);
   }

   private void throwBottle(MinecraftClient client, int expSlot){
      int selectedSlot = client.player.getInventory().getSelectedSlot();
      client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(expSlot));
      client.player.getInventory().setSelectedSlot(expSlot);

      try {
         this.tickMaintain(client);
      } finally {
         client.player.getInventory().setSelectedSlot(selectedSlot);
         client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(selectedSlot));
      }
   }

   private void tickMaintain(MinecraftClient client){
      boolean value = this.silentRotateDownSetting.getValue() && this.active;
      float yaw = value ? this.yaw : client.player.getYaw();
      float pitch = value ? 90.0F : client.player.getPitch();
      PendingUpdateManager pendingUpdateManager = ((ClientWorldAccessor)client.world).astatine$getPendingUpdateManager().incrementSequence();

      try {
         client.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, pendingUpdateManager.getSequence(), yaw, pitch));
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

   private boolean canThrow(MinecraftClient client){
      if (!this.active) {
         this.yaw = client.player.getYaw();
         this.active = true;
         this.rotateDown();
         return false;
      } else {
         this.rotateDown();
         return RotationManager.wasRotationSent(this.yaw, 90.0F, 1.0F);
      }
   }

   private void rotateDown(){
      RotationManager.setRotation(ROTATION_STATE, this.yaw, 90.0F, true, true);
   }

   private void tickAutoThrow(){
      if (this.silentRotateDownSetting.getValue() && this.active) {
         this.rotateDown();
      } else if (!this.silentRotateDownSetting.getValue()) {
         this.stopRotating();
      }
   }

   private void stopRotating(){
      RotationManager.clearRotatingState(ROTATION_STATE);
      this.active = false;
      this.yaw = 0.0F;
   }

   private boolean hasExpArmor(MinecraftClient client){
      for (EquipmentSlot equipmentSlot : EXP_ARMOR_SLOTS) {
         ItemStack stack = client.player.getEquippedStack(equipmentSlot);
         if (stack.isDamageable() && stack.getDamage() > 0) {
            return true;
         }
      }

      return false;
   }

   private int findExpBottleSlot(MinecraftClient client){
      return InventoryUtil.findHotBarItem(client, Items.EXPERIENCE_BOTTLE);
   }

   private void tickThrowSequence(MinecraftClient client){
      if (this.expBottleSlot != -1) {
         if (client.player != null) {
            client.player.getInventory().setSelectedSlot(this.expBottleSlot);
            client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(this.expBottleSlot));
         }

         this.expBottleSlot = -1;
      }
   }

   private boolean isInGame(MinecraftClient client){
      return client.player != null && client.world != null && client.interactionManager != null && !client.player.isDead();
   }

   private void resetBottleSlot(){
      this.expBottleSlot = -1;
      this.throwDelayTicks = 0;
      this.active = false;
      this.yaw = 0.0F;
   }
}
