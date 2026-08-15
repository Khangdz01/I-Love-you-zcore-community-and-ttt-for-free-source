package made4mischief.astatine.client.modules.smp;

import java.util.function.Predicate;
import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.NumberSetting;
import made4mischief.astatine.client.setting.StringSetting;
import made4mischief.astatine.client.utils.inventory.InventoryUtil;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
abstract class AbstractFixedSlotShopModule extends Module {
   private static final int NAVIGATE_TIMEOUT_TICKS = 80;
   private static final int MAX_REOPEN_ATTEMPTS = 2;
   private final StringSetting shopCommandSetting = this.addString("Shop Command", "shop", 32);
   private final NumberSetting menuDelaySetting = this.addNumber("Menu Delay", 2.0, 0.0, 10.0, 1.0);
   private final NumberSetting spamIntervalSetting = this.addNumber("Spam Interval", 0.0, 0.0, 10.0, 1.0);
   private final NumberSetting reopenDelaySetting = this.addNumber("Reopen Delay", 2.0, 0.0, 20.0, 1.0);
   private final int[] shopPath;
   private final Predicate<ItemStack> purchasedItemPredicate;
   private AbstractFixedSlotShopModule.Phase phase = AbstractFixedSlotShopModule.Phase.OPEN_SHOP;
   private int pathInde;
   private int waitTicks;
   private int phaseTicks;
   private int reopenAttempts;
   private boolean containerOpen;

   protected AbstractFixedSlotShopModule(String name, String description, Predicate<ItemStack> purchasedItem, int... shopPath){
      super(name, Category.SMP, description, -1, true);
      if (shopPath != null && shopPath.length != 0) {
         this.shopPath = (int[])shopPath.clone();
         this.purchasedItemPredicate = purchasedItem;
      } else {
         throw new IllegalArgumentException("Shop path cannot be empty");
      }
   }

   @Override
   protected void onEnable(){
      this.waitTicks(0);
   }

   @Override
   protected void onDisable(){
      this.waitTicks(0);
   }

   @EventTarget
   public final void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      if (client.player != null && client.world != null && client.interactionManager != null) {
         boolean openContainer = this.hasOpenContainer(client);
         if (this.containerOpen && !openContainer) {
            if (++this.reopenAttempts > 2) {
               this.waitTicks(this.reopenDelaySetting.getValueInt());
            }
         } else {
            if (openContainer) {
               this.reopenAttempts = 0;
            }

            if (this.waitTicks > 0) {
               this.waitTicks--;
            } else {
               this.phaseTicks++;
               switch (this.phase) {
                  case OPEN_SHOP:
                     this.tickOpenShop(client);
                     break;
                  case NAVIGATE:
                     this.tickWaitForShop(client);
                     break;
                  case SPAM_PURCHASE:
                     this.tickPurchaseItem(client);
                     break;
                  case DROP_PURCHASED:
                     this.tickHandleResponse(client);
                     break;
                  case WAIT_FOR_SPACE:
                     this.tickRetryPurchase(client);
               }
            }
         }
      }
   }

   private void tickOpenShop(MinecraftClient client){
      if (!this.hasOpenContainer(client)) {
         String trim = this.shopCommandSetting.getValue().trim();

         while (trim.startsWith("/")) {
            trim = trim.substring(1);
         }

         if (!trim.isEmpty() && client.player.networkHandler != null) {
            client.player.networkHandler.sendChatCommand(trim);
            this.containerOpen = false;
            this.pathInde = 0;
            this.advancePhase(AbstractFixedSlotShopModule.Phase.NAVIGATE, this.menuDelaySetting.getValueInt());
         }
      }
   }

   private void tickWaitForShop(MinecraftClient client){
      if (!this.hasOpenContainer(client)) {
         this.tickTimeoutCheck(client);
      } else {
         this.containerOpen = true;
         int var2 = this.shopPath[this.pathInde];
         if (!this.isSlotPurchasable(client, var2)) {
            this.tickTimeoutCheck(client);
         } else if (this.clickShopSlot(client, var2)) {
            this.pathInde++;
            if (this.pathInde >= this.shopPath.length) {
               this.advancePhase(AbstractFixedSlotShopModule.Phase.SPAM_PURCHASE, this.menuDelaySetting.getValueInt());
            } else {
               this.advancePhase(AbstractFixedSlotShopModule.Phase.NAVIGATE, this.menuDelaySetting.getValueInt());
            }
         }
      }
   }

   private void tickPurchaseItem(MinecraftClient client){
      if (this.hasOpenContainer(client)) {
         if (this.isTargetItemInHotbar(client)) {
            this.advancePhase(AbstractFixedSlotShopModule.Phase.DROP_PURCHASED, 0);
         } else {
            this.clickShopSlot(client, this.shopPath[this.shopPath.length - 1]);
            this.phaseTicks = 0;
            this.waitTicks = this.spamIntervalSetting.getValueInt();
         }
      }
   }

   private void tickHandleResponse(MinecraftClient client){
      if (this.hasOpenContainer(client)) {
         InventoryUtil.dumpPlayerInventory(client, this.purchasedItemPredicate);
         this.advancePhase(AbstractFixedSlotShopModule.Phase.WAIT_FOR_SPACE, 1);
      }
   }

   private void tickRetryPurchase(MinecraftClient client){
      if (this.hasOpenContainer(client)) {
         if (!this.isTargetItemInHotbar(client)) {
            this.advancePhase(AbstractFixedSlotShopModule.Phase.SPAM_PURCHASE, this.spamIntervalSetting.getValueInt());
         } else {
            if (this.phaseTicks >= 2) {
               this.advancePhase(AbstractFixedSlotShopModule.Phase.DROP_PURCHASED, 0);
            }
         }
      }
   }

   private boolean clickShopSlot(MinecraftClient client, int slotId){
      ScreenHandler screenHandler = client.player.currentScreenHandler;
      if (screenHandler != null && screenHandler != client.player.playerScreenHandler && slotId >= 0 && slotId < screenHandler.slots.size()) {
         client.interactionManager.clickSlot(screenHandler.syncId, slotId, 0, SlotActionType.PICKUP, client.player);
         return true;
      } else {
         return false;
      }
   }

   private boolean isSlotPurchasable(MinecraftClient client, int slotId){
      ScreenHandler screenHandler = client.player.currentScreenHandler;
      return screenHandler != null
         && screenHandler != client.player.playerScreenHandler
         && slotId >= 0
         && slotId < screenHandler.slots.size()
         && !((Slot)screenHandler.slots.get(slotId)).getStack().isEmpty();
   }

   private boolean isTargetItemInHotbar(MinecraftClient client){
      PlayerInventory playerInventory = client.player.getInventory();

      for (int index = 0; index < 36; index++) {
         ItemStack stack = playerInventory.getStack(index);
         if (stack.isEmpty()) {
            return false;
         }

         if (this.purchasedItemPredicate.test(stack) && stack.getCount() < stack.getMaxCount()) {
            return false;
         }
      }

      return true;
   }

   private boolean hasOpenContainer(MinecraftClient client){
      return client.player.currentScreenHandler != null && client.player.currentScreenHandler != client.player.playerScreenHandler;
   }

   private void tickTimeoutCheck(MinecraftClient client){
      if (this.phaseTicks >= 80) {
         if (this.hasOpenContainer(client)) {
            client.player.closeHandledScreen();
         }

         this.waitTicks(this.reopenDelaySetting.getValueInt());
      }
   }

   private void waitTicks(int delayTicks){
      this.containerOpen = false;
      this.reopenAttempts = 0;
      this.pathInde = 0;
      this.advancePhase(AbstractFixedSlotShopModule.Phase.OPEN_SHOP, delayTicks);
   }

   private void advancePhase(AbstractFixedSlotShopModule.Phase nextPhase, int delayTicks){
      this.phase = nextPhase;
      this.waitTicks = Math.max(0, delayTicks);
      this.phaseTicks = 0;
   }

   @Environment(EnvType.CLIENT)
   private static enum Phase {
      OPEN_SHOP,
      NAVIGATE,
      SPAM_PURCHASE,
      DROP_PURCHASED,
      WAIT_FOR_SPACE;
   }
}

