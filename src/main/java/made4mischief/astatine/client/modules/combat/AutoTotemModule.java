package made4mischief.astatine.client.modules.combat;

import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.ModeSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import made4mischief.astatine.client.utils.inventory.InventoryUtil;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.Items;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;

@Environment(EnvType.CLIENT)
public final class AutoTotemModule extends Module {
   private static AutoTotemModule instance;
   private final ModeSetting modeSetting = this.addMode("Mode", "Packet", new String[]{"Packet", "Legit"});
   private final NumberSetting healthThresholdSetting = this.addNumber("Health Threshold", 20.0, 0.0, 36.0, 1.0);
   private final BooleanSetting forceTotemSetting = this.addBoolean("Force Totem", false);
   private final NumberSetting packetDelaySetting = this.addNumber("Packet Delay", 2.0, 1.0, 5.0, 1.0);
   private int swapStage;
   private int stageTicks;
   private int totemSlot = -1;
   private int retryDelay;
   private boolean inventoryOpened;
   private String activeMode;

   public AutoTotemModule(){
      super("AutoTotem", Category.COMBAT, "Tự trang bị vật tổ vào tay phụ.", -1, true);
      instance = this;
      this.packetDelaySetting.visibleWhen(() -> this.modeSetting.is("Packet"));
   }

   @Override
   protected void onEnable(){
      this.finishTotemSwap(false);
   }

   @Override
   protected void onDisable(){
      this.finishTotemSwap(true);
   }

   @EventTarget
   public void onTick(TickEvent event){
      if (mc.player != null && mc.world != null) {
         if (this.swapStage != 0) {
            this.tickTotemState("Legit".equalsIgnoreCase(this.activeMode));
         } else if (this.retryDelay > 0) {
            this.retryDelay--;
         } else {
            double absorptionAmount = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            if (this.forceTotemSetting.getValue() || !(absorptionAmount > this.healthThresholdSetting.getValue())) {
               if (this.isTotemMissing()) {
                  this.tickTotemState(this.modeSetting.is("Legit"));
               }
            }
         }
      }
   }

   private boolean isTotemMissing(){
      return !this.hasTotem();
   }

   private boolean hasTotem(){
      return mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
   }

   private void tickTotemState(boolean renderInventory){
      switch (this.swapStage) {
         case 0:
            if (!this.isInventoryClosed()) {
               return;
            }

            int findInventoryItem = InventoryUtil.findInventoryItem(mc, Items.TOTEM_OF_UNDYING);
            if (findInventoryItem < 0) {
               this.retryDelay = 20;
               return;
            }

            this.totemSlot = findInventoryItem < 9 ? 36 + findInventoryItem : findInventoryItem;
            this.activeMode = this.modeSetting.getValue();
            if (renderInventory) {
               mc.setScreen(new InventoryScreen(mc.player));
               this.inventoryOpened = true;
            }

            this.swapStage = 1;
            this.stageTicks = 0;
            break;
         case 1:
            if (this.tickDelay() && this.isInventoryClosed()) {
               if (!mc.player.playerScreenHandler.getSlot(this.totemSlot).getStack().isOf(Items.TOTEM_OF_UNDYING)) {
                  this.finishTotemSwap(true);
                  this.retryDelay = 20;
                  return;
               }

               this.clickSlot(this.totemSlot);
               this.swapStage = 2;
               this.stageTicks = 0;
            }
            break;
         case 2:
            if (this.tickDelay() && this.isInventoryClosed()) {
               this.clickSlot(45);
               this.swapStage = 3;
               this.stageTicks = 0;
            }
            break;
         case 3:
            if (this.tickDelay() && this.isInventoryClosed()) {
               this.clickSlot(this.totemSlot);
               this.swapStage = 4;
               this.stageTicks = 0;
            }
            break;
         case 4:
            if (this.tickDelay()) {
               this.finishTotemSwap(true);
            }
      }
   }

   private boolean tickDelay(){
      this.stageTicks++;
      int valueInt = "Packet".equalsIgnoreCase(this.activeMode) ? this.packetDelaySetting.getValueInt() : 1;
      return this.stageTicks >= valueInt;
   }

   private boolean isInventoryClosed(){
      return mc.player != null && mc.interactionManager != null && mc.player.currentScreenHandler == mc.player.playerScreenHandler;
   }

   private void clickSlot(int screenSlot){
      mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, screenSlot, 0, SlotActionType.PICKUP, mc.player);
   }

   private void finishTotemSwap(boolean closeOpenedInventory){
      if (this.swapStage > 1 && this.swapStage < 4 && this.isInventoryClosed() && !mc.player.playerScreenHandler.getCursorStack().isEmpty() && this.totemSlot >= 0) {
         this.clickSlot(this.totemSlot);
      }

      if (closeOpenedInventory && this.inventoryOpened && mc.player != null && mc.currentScreen instanceof InventoryScreen) {
         mc.player.closeHandledScreen();
      }

      this.swapStage = 0;
      this.stageTicks = 0;
      this.totemSlot = -1;
      this.inventoryOpened = false;
      this.activeMode = null;
   }

   public static boolean isPacketSequenceActive(ClientPlayerEntity player){
      AutoTotemModule autoTotemModule = instance;
      return autoTotemModule != null && autoTotemModule.isEnabled() && autoTotemModule.swapStage != 0 && "Packet".equalsIgnoreCase(autoTotemModule.activeMode) && player != null && player == mc.player;
   }
}
