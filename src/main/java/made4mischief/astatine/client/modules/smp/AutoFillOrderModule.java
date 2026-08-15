package made4mischief.astatine.client.modules.smp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import made4mischief.astatine.client.mixin.InputAccessor;
import made4mischief.astatine.client.mixin.SignEditScreenAccessor;
import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.ItemTargetSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import net.minecraft.network.packet.c2s.play.RenameItemC2SPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.registry.Registries;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.DataComponentTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class AutoFillOrderModule extends Module {
   private static AutoFillOrderModule instance;
   private static final Logger LOGGER = LoggerFactory.getLogger("astatine/auto-fill-order");
   private static final int PAGE_ROWS = 4;
   private static final int GUI_ITEM_START_SLOT = 44;
   private static final int GUI_ITEM_END_SLOT = 45;
   private static final int GUI_ORDER_SLOT = 53;
   private static final int ORDER_SLOT = 50;
   private static final int PHASE_TIMEOUT = 100;
   private static final int INVENTORY_SLOTS = 64;
   private static final int CLICK_DELAY = 3;
   private static final int RETRY_DELAY = 3;
   private static final int SCAN_BATCH = 8;
   private static final int CLOSE_DELAY = 12;
   private static final int SCROLL_BATCH = 3;
   private static final int MAX_CANDIDATES = 40;
   private static final int CONFIRM_DELAY = 20;
   private static final double SPAWNER_SEARCH_RANGE = 16.0;
   private static final double FILL_THRESHOLD = 0.85;
   private static final float[][] PAGE_OFFSETS = new float[][]{
      {0.0F, 1.0F}, {0.0F, -1.0F}, {1.0F, 0.0F}, {-1.0F, 0.0F}, {1.0F, 1.0F}, {-1.0F, 1.0F}, {1.0F, -1.0F}, {-1.0F, -1.0F}
   };
   private static final Pattern ORDER_COUNT_PATTERN = Pattern.compile("\\((\\d+)\\s*/\\s*(\\d+)\\)");
   private static final Pattern PRICE_PATTERN = Pattern.compile("\\$\\s*([0-9]+(?:[.,][0-9]+)?)\\s*([KMB]?)", 2);
   private static final Pattern STOCK_PATTERN = Pattern.compile("([0-9][0-9,]*)\\s*/\\s*([0-9][0-9,]*)(?![0-9.,])");
   private final ItemTargetSetting itemSetting = this.addSetting(new ItemTargetSetting("Item", 1, Items.BONE));
   private final BooleanSetting requireSpawnerSetting = this.addBoolean("Require Spawner", true);
   private final BooleanSetting replayModeSetting = this.addBoolean("Replay Mode", true);
   private final NumberSetting replayDelayTicksSetting = this.addNumber("Replay Delay (ticks)", 10.0, 0.0, 200.0, 1.0);
   private AutoFillOrderModule.Phase phase = AutoFillOrderModule.Phase.IDLE;
   private Item item;
   private int phaseTicks;
   private int pageTicks;
   private int retryTicks;
   private boolean paused;
   private boolean orderConfirmed;
   private AutoFillOrderModule.OrderCandidate currentOrder;
   private final List<AutoFillOrderModule.OrderCandidate> orderCandidates = new ArrayList<>();
   private int pageInde;
   private int orderInde;
   private int scanStartSlot;
   private int scanEndSlot;
   private int bestPageInde;
   private int confirmedCount;
   private int totalNeeded;
   private ItemEntity targetSpawner;
   private int orderCycles;
   private int remainingToFill;
   private int deliveredCount;
   private double totalSpent;
   private String lastChatMessage = "";
   private String expectedChatMessage = "";
   private String orderName = "";
   private int guiSyncId;
   private int clickSlot;
   private int clickButton;
   private long lastClickNanos;
   private int retryDelay;
   private int inventorySlot;
   private int guiSlot;
   private int attemptedClickCount;
   private int failedClickCount;
   private final Set<Integer> attemptedSlots = new LinkedHashSet<>();

   public AutoFillOrderModule(){
      super("AutoFillOrder", Category.SMP, "Tá»± láº¥y lá»“ng quÃ¡i vÃ  kiá»ƒm tra Ä‘Æ¡n hÃ ng.", -1, true);
      instance = this;
      this.replayDelayTicksSetting.visibleWhen(this.replayModeSetting::getValue);
   }

   @Override
   protected void onEnable(){
      this.item = this.itemSetting.getSelectedItems().stream().findFirst().orElse(null);
      this.retryTicks = 0;
      this.paused = false;
      this.orderConfirmed = false;
      this.targetSpawner = null;
      this.orderCycles = 0;
      this.resetFillStats();
      this.clearCurrentOrder();
      if (this.item == null) {
         this.stopWithMessage("Select exactly one item before enabling the module.");
      } else {
         this.setPhase(this.requireSpawnerSetting.getValue() ? AutoFillOrderModule.Phase.FIND_SPAWNER : AutoFillOrderModule.Phase.COLLECT_DROPS, 0);
      }
   }

   @Override
   protected void onDisable(){
      this.phase = AutoFillOrderModule.Phase.IDLE;
      this.phaseTicks = 0;
      this.pageTicks = 0;
      this.item = null;
      this.retryTicks = 0;
      this.paused = false;
      this.orderConfirmed = false;
      this.targetSpawner = null;
      this.orderCycles = 0;
      this.resetFillStats();
      this.clearCurrentOrder();
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      if (client.player != null && client.world != null && client.interactionManager != null && this.item != null) {
         if (this.pageTicks > 0) {
            this.pageTicks--;
         } else {
            this.phaseTicks++;
            switch (this.phase) {
               case IDLE:
               case DONE:
               default:
                  break;
               case FIND_SPAWNER:
                  this.closeScreen(client);
                  break;
               case WAIT_SPAWNER_GUI:
                  this.resetRetryCounter(client);
                  break;
               case DUMP_SPAWNER_PAGE:
                  this.tickFindSpawner(client);
                  break;
               case COLLECT_DROPS:
                  this.tickLocateSpawner(client);
                  break;
               case SEND_ORDER:
                  this.tickCloseOrderGui(client);
                  break;
               case WAIT_ORDER_GUI:
                  this.tickSearchOrder(client);
                  break;
               case CLICK_ORDER_SEARCH:
                  this.tickVerifySlotReady(client);
                  break;
               case WAIT_ITEM_INPUT:
                  this.tickOpenOrderGui(client);
                  break;
               case SUBMIT_ANVIL:
                  this.tickWaitResult(client);
                  break;
               case WAIT_RESULT_GUI:
                  this.tickLogResult(client);
                  break;
               case LOG_RESULT:
                  this.tickNavigateResults(client);
                  break;
               case SCAN_ORDER_PAGE:
                  this.tickScanPages(client);
                  break;
               case NAVIGATE_BEST_PAGE:
                  this.tickReturnToBestPage(client);
                  break;
               case SELECT_BEST_ORDER:
                  this.tickSelectBestOrder(client);
                  break;
               case WAIT_FILL_GUI:
                  this.tickFillOrderGui(client);
                  break;
               case FILL_ORDER_GUI:
                  this.tickWaitConfirm(client);
                  break;
               case VERIFY_FILL_TRANSFER:
                  this.fillFromInventory(client);
                  break;
               case CLOSE_FILLED_GUI:
                  this.tickConfirmOrderFlow(client);
                  break;
               case WAIT_CONFIRM_GUI:
                  this.tickCheckOrderMessage(client);
                  break;
               case FINISH_CONFIRM:
                  this.tickConfirmOrder(client);
            }
         }
      }
   }

   private void closeScreen(MinecraftClient client){
      if (this.isShopGuiOpen(client)) {
         client.player.closeHandledScreen();
         this.pageTicks = 2;
      } else if (client.currentScreen == null) {
         BlockPos pos = this.findNearbySpawnerPos(client);
         if (pos != null) {
            Vec3d vec = Vec3d.ofCenter(pos).add(0.0, 0.5, 0.0);
            BlockHitResult hitResult = new BlockHitResult(vec, Direction.UP, pos, false);
            client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitResult);
            client.player.swingHand(Hand.MAIN_HAND);
            this.setPhase(AutoFillOrderModule.Phase.WAIT_SPAWNER_GUI, 2);
         }
      }
   }

   private void resetRetryCounter(MinecraftClient client){
      if (this.isShopGuiOpen(client)) {
         this.retryTicks = 0;
         this.paused = false;
         this.setPhase(AutoFillOrderModule.Phase.DUMP_SPAWNER_PAGE, 2);
      } else {
         if (this.phaseTicks >= 100) {
            this.setPhase(AutoFillOrderModule.Phase.FIND_SPAWNER, 10);
         }
      }
   }

   private void tickFindSpawner(MinecraftClient client){
      if (!this.isShopGuiOpen(client)) {
         this.setPhase(AutoFillOrderModule.Phase.FIND_SPAWNER, 10);
      } else {
         Slot slot = this.findItemInGui(client, this.item, 53);
         if (slot != null) {
            this.clickGuiSlot(client, slot.id, 1, SlotActionType.THROW);
            this.paused = true;
            this.pageTicks = 1;
         } else if (this.paused && this.retryTicks < 64 && this.isGuiSlotValid(client, 53)) {
            this.clickGuiSlot(client, 53, 0, SlotActionType.PICKUP);
            this.retryTicks++;
            this.paused = false;
            this.setPhase(AutoFillOrderModule.Phase.DUMP_SPAWNER_PAGE, 3);
         } else {
            client.player.closeHandledScreen();
            this.targetSpawner = null;
            this.setPhase(AutoFillOrderModule.Phase.COLLECT_DROPS, 3);
         }
      }
   }

   private void tickLocateSpawner(MinecraftClient client){
      if (client.currentScreen == null && !this.isShopGuiOpen(client)) {
         this.targetSpawner = this.findSpawner(client);
         if (this.targetSpawner != null) {
            if (!this.hasEnoughItems(client)) {
               this.targetSpawner = null;
               if (this.countInventoryItems(client) > 0) {
                  this.clearCurrentOrder();
                  this.setPhase(AutoFillOrderModule.Phase.SEND_ORDER, 0);
               } else {
                  this.stopWithMessage("Target items remain on the ground, but the inventory has no free space.");
               }
            }
         } else if (this.phaseTicks >= 20) {
            if (this.countInventoryItems(client) > 0) {
               this.clearCurrentOrder();
               this.setPhase(AutoFillOrderModule.Phase.SEND_ORDER, 0);
            } else {
               this.completeOrder(client);
            }
         }
      } else {
         this.targetSpawner = null;
         if (this.isShopGuiOpen(client)) {
            if (this.countInventoryItems(client) > 0) {
               client.player.closeHandledScreen();
               this.clearCurrentOrder();
               this.setPhase(AutoFillOrderModule.Phase.SEND_ORDER, 3);
               return;
            }

            if (this.findSpawner(client) == null) {
               client.player.closeHandledScreen();
               this.phaseTicks = 0;
               this.pageTicks = 2;
            }
         }
      }
   }

   private void tickCloseOrderGui(MinecraftClient client){
      if (this.isShopGuiOpen(client)) {
         client.player.closeHandledScreen();
         this.pageTicks = 2;
      } else if (client.currentScreen == null && client.player.networkHandler != null) {
         client.player.networkHandler.sendChatCommand("order");
         this.setPhase(AutoFillOrderModule.Phase.WAIT_ORDER_GUI, 2);
      }
   }

   private void tickSearchOrder(MinecraftClient client){
      if (this.isShopGuiOpen(client)) {
         this.setPhase(AutoFillOrderModule.Phase.CLICK_ORDER_SEARCH, 2);
      } else {
         if (this.phaseTicks >= 100) {
            this.stopWithMessage("/order did not open a container GUI.");
         }
      }
   }

   private void tickVerifySlotReady(MinecraftClient client){
      if (!this.isShopGuiOpen(client)) {
         if (this.phaseTicks >= 100) {
            this.stopWithMessage("/order GUI closed before slot 50 was ready.");
         }
      } else if (!this.isGuiSlotValid(client, 50)) {
         if (this.phaseTicks >= 100) {
            this.stopWithMessage("Slot 50 in /order GUI is missing or empty.");
         }
      } else {
         this.clickGuiSlot(client, 50, 0, SlotActionType.PICKUP);
         this.orderConfirmed = false;
         this.setPhase(AutoFillOrderModule.Phase.WAIT_ITEM_INPUT, 2);
      }
   }

   private void tickOpenOrderGui(MinecraftClient client){
      String itemName = this.getItemName();
      if (client.player.currentScreenHandler instanceof AnvilScreenHandler) {
         if (!this.orderConfirmed && client.player.networkHandler != null) {
            client.player.networkHandler.sendPacket(new RenameItemC2SPacket(itemName));
            this.orderConfirmed = true;
         }

         this.setPhase(AutoFillOrderModule.Phase.SUBMIT_ANVIL, 2);
      } else if (client.currentScreen instanceof AbstractSignEditScreen var6) {
         String[] var7 = ((SignEditScreenAccessor)var6).astatine$getMessages();

         for (int index = 0; index < var7.length; index++) {
            var7[index] = index == 0 ? itemName : "";
         }

         this.orderConfirmed = true;
         client.setScreen(null);
         this.setPhase(AutoFillOrderModule.Phase.WAIT_RESULT_GUI, 3);
      } else if (client.currentScreen instanceof ChatScreen) {
         this.sendChatQuery(client, itemName);
      } else if (!this.isShopGuiOpen(client) && client.currentScreen == null && this.phaseTicks >= 5) {
         this.sendChatQuery(client, itemName);
      } else {
         if (this.phaseTicks >= 100) {
            String name = client.currentScreen == null ? "none" : client.currentScreen.getClass().getName();
            this.stopWithMessage("Unsupported item-input screen: " + name);
         }
      }
   }

   private void sendChatQuery(MinecraftClient client, String query){
      if (client.player.networkHandler != null) {
         client.player.networkHandler.sendChatMessage(query);
         this.orderConfirmed = true;
         if (client.currentScreen instanceof ChatScreen) {
            client.setScreen(null);
         }

         this.setPhase(AutoFillOrderModule.Phase.WAIT_RESULT_GUI, 3);
      }
   }

   private void tickWaitResult(MinecraftClient client){
      if (!(client.player.currentScreenHandler instanceof AnvilScreenHandler)) {
         this.setPhase(AutoFillOrderModule.Phase.WAIT_RESULT_GUI, 2);
      } else if (this.isGuiSlotValid(client, 2)) {
         this.clickGuiSlot(client, 2, 0, SlotActionType.PICKUP);
         this.setPhase(AutoFillOrderModule.Phase.WAIT_RESULT_GUI, 3);
      } else {
         if (this.phaseTicks >= 100) {
            this.stopWithMessage("Anvil input accepted no output for '" + this.getItemName() + "'.");
         }
      }
   }

   private void tickLogResult(MinecraftClient client){
      if (this.isShopGuiOpen(client) && !(client.player.currentScreenHandler instanceof AnvilScreenHandler)) {
         this.setPhase(AutoFillOrderModule.Phase.LOG_RESULT, 3);
      } else {
         if (this.phaseTicks >= 100) {
            this.stopWithMessage("No result GUI opened after entering '" + this.getItemName() + "'.");
         }
      }
   }

   private void tickNavigateResults(MinecraftClient client){
      if (this.isShopGuiOpen(client) && !(client.player.currentScreenHandler instanceof AnvilScreenHandler)) {
         AutoFillOrderModule.PageInfo var2 = this.getCurrentPageInfo(client);
         if (var2 == null) {
            if (this.phaseTicks >= 100) {
               this.stopWithMessage("The search result GUI has no '(page/total)' title.");
            }
         } else if (var2.current != 1) {
            if (this.isGuiSlotValid(client, 45)) {
               this.clickGuiSlot(client, 45, 0, SlotActionType.PICKUP);
               this.setPhase(AutoFillOrderModule.Phase.SCAN_ORDER_PAGE, 8);
            } else if (this.phaseTicks >= 100) {
               this.reopenAndRescan(client, "Could not return to order page 1.");
            }
         } else {
            this.clearCurrentOrder();
            client.player
               .sendMessage(Text.literal("Â§8[Â§bAutoFillOrderÂ§8] Â§fScanning page 1 for the best Â§b" + this.getItemName() + " Â§fprice..."), false);
            this.setPhase(AutoFillOrderModule.Phase.SCAN_ORDER_PAGE, 0);
         }
      } else {
         if (this.phaseTicks >= 100) {
            this.stopWithMessage("Result GUI closed before its items could be logged.");
         }
      }
   }

   private void tickScanPages(MinecraftClient client){
      if (!this.isShopGuiOpen(client)) {
         if (this.phaseTicks >= 100) {
            this.stopWithMessage("Order GUI closed while pages were being scanned.");
         }
      } else {
         AutoFillOrderModule.PageInfo var2 = this.getCurrentPageInfo(client);
         if (var2 == null) {
            if (this.phaseTicks >= 100) {
               this.stopWithMessage("Could not read the current order page.");
            }
         } else if (var2.current == this.pageInde) {
            if (this.phaseTicks >= 100) {
               this.stopWithMessage("Order page did not advance from page " + var2.current + ".");
            }
         } else {
            ScreenHandler screenHandler = client.player.currentScreenHandler;
            int size = Math.min(44, screenHandler.slots.size() - 1);

            for (int index = 0; index <= size; index++) {
               AutoFillOrderModule.OrderCandidate var6 = this.createOrderCandidate(((Slot)screenHandler.slots.get(index)).getStack(), var2.current, index);
               if (var6 != null) {
                  this.orderCandidates.add(var6);
               }
            }

            this.pageInde = var2.current;
            this.orderInde++;
            int countInventoryItems = this.countInventoryItems(client);
            this.orderCandidates.removeIf(candidate -> candidate.remaining < countInventoryItems);
            this.orderCandidates.sort(Comparator.comparingDouble(AutoFillOrderModule.OrderCandidate::price).reversed());
            this.currentOrder = this.orderCandidates.isEmpty() ? null : this.orderCandidates.get(0);
            if (this.currentOrder == null) {
               LOGGER.info("No page-1 {} order can accept all {} inventory items; waiting before retry.", this.getItemName(), countInventoryItems);
               client.player
                  .sendMessage(
                     Text.literal("Â§8[Â§bAutoFillOrderÂ§8] Â§eNo page-1 order can accept all " + countInventoryItems + " " + this.getItemName() + "; waiting to retry."),
                     false
                  );
               client.player.closeHandledScreen();
               this.clearCurrentOrder();
               this.setPhase(AutoFillOrderModule.Phase.SEND_ORDER, this.replayModeSetting.getValue() ? this.replayDelayTicksSetting.getValueInt() : 10);
            } else {
               LOGGER.info(
                  "Best {} order after {} pages: page={}, slot={}, price={}, name='{}'",
                  new Object[]{
                     this.getItemName(), this.orderInde, this.currentOrder.page, this.currentOrder.slotId, this.currentOrder.price, this.currentOrder.displayName
                  }
               );
               client.player
                  .sendMessage(
                     Text.literal(
                        "Â§8[Â§bAutoFillOrderÂ§8] Â§aBest price: Â§f$"
                           + formatPrice(this.currentOrder.price)
                           + " Â§7(page "
                           + this.currentOrder.page
                           + ", "
                           + this.currentOrder.displayName
                           + ")"
                     ),
                     false
                  );
               this.scanEndSlot = -1;
               this.setPhase(AutoFillOrderModule.Phase.NAVIGATE_BEST_PAGE, 3);
            }
         }
      }
   }

   private void tickReturnToBestPage(MinecraftClient client){
      if (!this.isShopGuiOpen(client)) {
         if (this.phaseTicks >= 100) {
            this.stopWithMessage("Order GUI closed while returning to the best page.");
         }
      } else {
         AutoFillOrderModule.PageInfo var2 = this.getCurrentPageInfo(client);
         if (var2 == null) {
            if (this.phaseTicks >= 100) {
               this.stopWithMessage("Lost order page number while returning to the best order.");
            }
         } else {
            if (this.scanEndSlot > 0) {
               if (var2.current != this.scanEndSlot) {
                  if (var2.current == this.bestPageInde) {
                     if (this.phaseTicks >= 12) {
                        if (this.confirmedCount >= 3) {
                           this.reopenAndRescan(client, "Navigation remained at page " + var2.current + " after " + this.confirmedCount + " retries.");
                           return;
                        }

                        int var3 = var2.current > this.currentOrder.page ? 45 : 53;
                        if (!this.isGuiSlotValid(client, var3)) {
                           this.reopenAndRescan(client, "Navigation button was unavailable at page " + var2.current + ".");
                           return;
                        }

                        this.confirmedCount++;
                        LOGGER.info("Retrying order navigation from page {} to {} ({}/{})", new Object[]{var2.current, this.scanEndSlot, this.confirmedCount, 3});
                        this.clickGuiSlot(client, var3, 0, SlotActionType.PICKUP);
                        this.setPhase(AutoFillOrderModule.Phase.NAVIGATE_BEST_PAGE, 8);
                        return;
                     }

                     return;
                  }

                  this.scanEndSlot = -1;
                  this.bestPageInde = -1;
                  this.confirmedCount = 0;
               } else {
                  this.scanEndSlot = -1;
                  this.bestPageInde = -1;
                  this.confirmedCount = 0;
               }
            }

            if (var2.current == this.currentOrder.page) {
               this.setPhase(AutoFillOrderModule.Phase.SELECT_BEST_ORDER, 2);
            } else {
               byte var4;
               if (var2.current > this.currentOrder.page) {
                  var4 = 45;
                  this.scanEndSlot = var2.current - 1;
               } else {
                  var4 = 53;
                  this.scanEndSlot = var2.current + 1;
               }

               if (!this.isGuiSlotValid(client, var4)) {
                  if (this.phaseTicks >= 12) {
                     this.reopenAndRescan(client, "Navigation slot " + var4 + " is unavailable on order page " + var2.current + ".");
                  }
               } else {
                  this.bestPageInde = var2.current;
                  this.confirmedCount = 0;
                  this.clickGuiSlot(client, var4, 0, SlotActionType.PICKUP);
                  this.setPhase(AutoFillOrderModule.Phase.NAVIGATE_BEST_PAGE, 8);
               }
            }
         }
      }
   }

   private void reopenAndRescan(MinecraftClient client, String reason){
      LOGGER.info("{} Reopening /order and rescanning.", reason);
      client.player.sendMessage(Text.literal("Â§8[Â§bAutoFillOrderÂ§8] Â§e" + reason + " Rescanning orders..."), false);
      if (this.isShopGuiOpen(client)) {
         client.player.closeHandledScreen();
      }

      this.clearCurrentOrder();
      this.setPhase(AutoFillOrderModule.Phase.SEND_ORDER, 4);
   }

   private void tickSelectBestOrder(MinecraftClient client){
      if (!this.isShopGuiOpen(client)) {
         if (this.phaseTicks >= 100) {
            this.stopWithMessage("Order GUI closed before selecting the best order.");
         }
      } else {
         AutoFillOrderModule.PageInfo var2 = this.getCurrentPageInfo(client);
         if (var2 != null && var2.current == this.currentOrder.page) {
            ScreenHandler screenHandler = client.player.currentScreenHandler;
            int index2 = -1;
            AutoFillOrderModule.OrderCandidate var5 = null;
            int countInventoryItems = this.countInventoryItems(client);
            int size = Math.min(44, screenHandler.slots.size() - 1);

            for (int index = 0; index <= size; index++) {
               AutoFillOrderModule.OrderCandidate var9 = this.createOrderCandidate(((Slot)screenHandler.slots.get(index)).getStack(), var2.current, index);
               if (var9 != null
                  && var9.displayName.equals(this.currentOrder.displayName)
                  && Math.abs(var9.price - this.currentOrder.price) < 1.0E-6
                  && var9.remaining >= countInventoryItems) {
                  index2 = index;
                  var5 = var9;
                  break;
               }
            }

            if (index2 < 0) {
               this.removeCurrentOrder(client, "The previous best order was filled or changed.");
            } else {
               this.currentOrder = var5;
               this.lastClickNanos = this.currentOrder.remaining;
               this.scanStartSlot = screenHandler.syncId;
               this.attemptedSlots.clear();
               this.clickGuiSlot(client, index2, 0, SlotActionType.PICKUP);
               this.setPhase(AutoFillOrderModule.Phase.WAIT_FILL_GUI, 3);
            }
         } else {
            this.setPhase(AutoFillOrderModule.Phase.NAVIGATE_BEST_PAGE, 2);
         }
      }
   }

   private void removeCurrentOrder(MinecraftClient client, String reason){
      this.orderCandidates.remove(this.currentOrder);
      int countInventoryItems = this.countInventoryItems(client);
      this.orderCandidates.removeIf(candidate -> candidate.remaining < countInventoryItems);
      if (this.orderCandidates.isEmpty()) {
         LOGGER.info("{} No cached unfilled {} orders remain; rescanning /order.", reason, this.getItemName());
         client.player.sendMessage(Text.literal("Â§8[Â§bAutoFillOrderÂ§8] Â§e" + reason + " Rescanning orders..."), false);
         if (this.isShopGuiOpen(client)) {
            client.player.closeHandledScreen();
         }

         this.clearCurrentOrder();
         this.setPhase(AutoFillOrderModule.Phase.SEND_ORDER, 3);
      } else {
         this.currentOrder = this.orderCandidates.get(0);
         this.lastClickNanos = this.currentOrder.remaining;
         this.scanEndSlot = -1;
         LOGGER.info(
            "{} Moving to next {} order: page={}, price={}, remaining={}, name='{}'",
            new Object[]{reason, this.getItemName(), this.currentOrder.page, this.currentOrder.price, this.currentOrder.remaining, this.currentOrder.displayName}
         );
         client.player
            .sendMessage(Text.literal("Â§8[Â§bAutoFillOrderÂ§8] Â§e" + reason + " Trying next price: Â§f$" + formatPrice(this.currentOrder.price)), false);
         this.setPhase(AutoFillOrderModule.Phase.NAVIGATE_BEST_PAGE, 2);
      }
   }

   private void tickFillOrderGui(MinecraftClient client){
      if (this.isShopGuiOpen(client)) {
         ScreenHandler screenHandler = client.player.currentScreenHandler;
         AutoFillOrderModule.PageInfo var3 = this.getCurrentPageInfo(client);
         if (screenHandler.syncId != this.scanStartSlot || var3 == null) {
            this.totalNeeded = this.countInventoryItems(client);
            this.rememberGuiSyncId(client);
            this.setPhase(AutoFillOrderModule.Phase.FILL_ORDER_GUI, 2);
            return;
         }
      }

      if (this.phaseTicks >= 100) {
         this.stopWithMessage("Clicking the best order did not open its delivery GUI.");
      }
   }

   private void tickWaitConfirm(MinecraftClient client){
      if (!this.isShopGuiOpen(client)) {
         if (this.phaseTicks >= 100) {
            this.stopWithMessage("Delivery GUI closed before inventory transfer finished.");
         }
      } else {
         String var2 = getChatLastMessage(client);
         if (!this.expectedChatMessage.isEmpty() && !this.expectedChatMessage.equals(var2)) {
            this.updateRemainingCount(client);
            this.setPhase(AutoFillOrderModule.Phase.WAIT_CONFIRM_GUI, 0);
         } else {
            ScreenHandler screenHandler = client.player.currentScreenHandler;
            if (screenHandler.syncId != this.guiSyncId) {
               this.rememberGuiSyncId(client);
            }

            int countInventoryItems = this.countInventoryItems(client);
            if (countInventoryItems > this.lastClickNanos) {
               this.reopenAndRescan(client, "This order only needs " + this.lastClickNanos + " more, but the inventory now has " + countInventoryItems + ".");
            } else {
               PlayerInventory playerInventory = client.player.getInventory();

               for (Slot slot : screenHandler.slots) {
                  if (slot.inventory == playerInventory && slot.getStack().isOf(this.item)) {
                     this.retryDelay = this.countInventoryItems(client);
                     this.inventorySlot = slot.getIndex();
                     this.guiSlot = slot.getStack().getCount();
                     this.attemptedClickCount = screenHandler.syncId;
                     this.attemptedSlots.add(slot.id);
                     this.clickGuiSlot(client, slot.id, 0, SlotActionType.QUICK_MOVE);
                     this.setPhase(AutoFillOrderModule.Phase.VERIFY_FILL_TRANSFER, 3);
                     return;
                  }
               }

               this.setPhase(AutoFillOrderModule.Phase.CLOSE_FILLED_GUI, 3);
            }
         }
      }
   }

   private void fillFromInventory(MinecraftClient client){
      int countInventoryItems = this.countInventoryItems(client);
      ItemStack stack = this.inventorySlot >= 0 && this.inventorySlot < 36 ? client.player.getInventory().getStack(this.inventorySlot) : ItemStack.EMPTY;
      boolean count2 = !stack.isOf(this.item) || stack.getCount() < this.guiSlot;
      boolean var5 = countInventoryItems < this.retryDelay;
      if (!count2 && !var5) {
         this.failedClickCount++;
         LOGGER.info(
            "Delivery transfer made no progress ({}/{}): sync {} -> {}, items={}",
            new Object[]{this.failedClickCount, 3, this.attemptedClickCount, this.isShopGuiOpen(client) ? client.player.currentScreenHandler.syncId : -1, countInventoryItems}
         );
         if (this.failedClickCount >= 3) {
            this.abortOrder(client, "Order rejected 3 delivery attempts.");
         } else if (this.isShopGuiOpen(client) && this.expectedChatMessage.equals(getChatLastMessage(client))) {
            ScreenHandler screenHandler2 = client.player.currentScreenHandler;
            if (screenHandler2.syncId != this.guiSyncId) {
               this.rememberGuiSyncId(client);
            }

            this.setPhase(AutoFillOrderModule.Phase.FILL_ORDER_GUI, 2);
         } else {
            this.abortOrder(client, "Delivery GUI closed without accepting the item.");
         }
      } else {
         int count = !stack.isOf(this.item) ? this.guiSlot : Math.max(0, this.guiSlot - stack.getCount());
         int max = Math.max(count, Math.max(0, this.retryDelay - countInventoryItems));
         this.lastClickNanos = Math.max(0L, this.lastClickNanos - max);
         this.failedClickCount = 0;
         this.updateRemainingCount(client);
         if (!this.isShopGuiOpen(client)) {
            this.setPhase(AutoFillOrderModule.Phase.WAIT_CONFIRM_GUI, 1);
         } else {
            String var8 = getChatLastMessage(client);
            if (!this.expectedChatMessage.equals(var8)) {
               this.setPhase(AutoFillOrderModule.Phase.WAIT_CONFIRM_GUI, 0);
            } else {
               ScreenHandler screenHandler = client.player.currentScreenHandler;
               if (screenHandler.syncId != this.guiSyncId) {
                  this.rememberGuiSyncId(client);
               }

               this.setPhase(AutoFillOrderModule.Phase.FILL_ORDER_GUI, 1);
            }
         }
      }
   }

   private void tickConfirmOrderFlow(MinecraftClient client){
      this.updateRemainingCount(client);
      if (this.isShopGuiOpen(client)) {
         this.tickCloseAfterFill(client);
      }

      this.setPhase(AutoFillOrderModule.Phase.WAIT_CONFIRM_GUI, 2);
   }

   private void tickCheckOrderMessage(MinecraftClient client){
      if (this.isShopGuiOpen(client)) {
         String var2 = getChatLastMessage(client);
         if (!this.expectedChatMessage.isEmpty() && this.expectedChatMessage.equals(var2)) {
            ScreenHandler screenHandler = client.player.currentScreenHandler;
            if (this.countInventoryItems(client) > 0) {
               if (screenHandler.syncId != this.guiSyncId) {
                  this.rememberGuiSyncId(client);
               }

               this.setPhase(AutoFillOrderModule.Phase.FILL_ORDER_GUI, 1);
               return;
            }

            if (this.findSpawner(client) != null) {
               return;
            }

            this.tickCloseAfterFill(client);
            this.phaseTicks = 0;
            this.pageTicks = 2;
            return;
         }

         if (this.isGuiSlotValid(client, 16)) {
            this.orderName = var2;
            this.clickButton = 0;
            this.clickGuiSlot(client, 16, 0, SlotActionType.PICKUP);
            this.setPhase(AutoFillOrderModule.Phase.FINISH_CONFIRM, 4);
            return;
         }
      }

      if (!this.isShopGuiOpen(client) && this.clickButton >= 2 && this.phaseTicks >= 40) {
         this.confirmFilledOrder(client);
      } else {
         if (this.phaseTicks >= 100) {
            this.abortOrder(client, "Confirmation GUI did not open; restarting the order loop.");
         }
      }
   }

   private void abortOrder(MinecraftClient client, String reason){
      if (this.currentOrder != null) {
         this.updateRemainingCount(client);
      }

      if (this.isShopGuiOpen(client)) {
         client.player.closeHandledScreen();
      }

      LOGGER.info("{} Remaining {} in inventory: {}. Restarting.", new Object[]{reason, this.getItemName(), this.countInventoryItems(client)});
      client.player.sendMessage(Text.literal("Â§8[Â§bAutoFillOrderÂ§8] Â§e" + reason + " Closing GUIs and retrying."), false);
      this.targetSpawner = null;
      this.resetFillStats();
      this.clearCurrentOrder();
      if (this.countInventoryItems(client) > 0) {
         this.setPhase(AutoFillOrderModule.Phase.SEND_ORDER, 4);
      } else {
         this.setPhase(AutoFillOrderModule.Phase.COLLECT_DROPS, this.replayModeSetting.getValue() ? this.replayDelayTicksSetting.getValueInt() : 3);
      }
   }

   private void tickConfirmOrder(MinecraftClient client){
      if (this.isShopGuiOpen(client)) {
         String var2 = getChatLastMessage(client);
         if (!this.expectedChatMessage.isEmpty() && this.expectedChatMessage.equals(var2)) {
            ScreenHandler screenHandler = client.player.currentScreenHandler;
            if (this.countInventoryItems(client) > 0) {
               if (screenHandler.syncId != this.guiSyncId) {
                  this.rememberGuiSyncId(client);
               }

               this.setPhase(AutoFillOrderModule.Phase.FILL_ORDER_GUI, 1);
            } else if (this.findSpawner(client) == null) {
               this.tickCloseAfterFill(client);
               if (this.clickButton >= 2) {
                  this.confirmFilledOrder(client);
               } else {
                  this.phaseTicks = 0;
                  this.pageTicks = 2;
               }
            }
         } else if (this.orderName.isEmpty() || !this.orderName.equals(var2) || this.phaseTicks >= 40) {
            this.tickCloseAfterFill(client);
            if (this.clickButton >= 2) {
               this.confirmFilledOrder(client);
            } else {
               this.phaseTicks = 0;
               this.pageTicks = 2;
            }
         }
      } else if (this.clickButton >= 2 || this.phaseTicks >= 40) {
         this.confirmFilledOrder(client);
      }
   }

   private void confirmFilledOrder(MinecraftClient client){
      LOGGER.info(
         "Confirmed filled {} order: price={}, buyer='{}', attemptedSlots={}, deliveredItems={}, remainingItems={}, closedGuis={}",
         new Object[]{this.getItemName(), this.totalSpent, this.lastChatMessage, this.attemptedSlots.size(), this.remainingToFill, this.deliveredCount, this.clickButton}
      );
      client.player
         .sendMessage(
            Text.literal(
               "Â§8[Â§bAutoFillOrderÂ§8] Â§aFilled best order at Â§f$"
                  + formatPrice(this.totalSpent)
                  + " Â§7("
                  + this.remainingToFill
                  + " delivered, "
                  + this.deliveredCount
                  + " remaining, confirmed)"
            ),
            false
         );
      this.orderCycles++;
      this.targetSpawner = null;
      this.resetFillStats();
      if (this.countInventoryItems(client) > 0) {
         this.clearCurrentOrder();
         this.setPhase(AutoFillOrderModule.Phase.SEND_ORDER, 3);
      } else if (this.findSpawner(client) != null) {
         this.clearCurrentOrder();
         this.setPhase(AutoFillOrderModule.Phase.COLLECT_DROPS, 2);
      } else if (this.replayModeSetting.getValue()) {
         this.clearCurrentOrder();
         this.setPhase(
            this.requireSpawnerSetting.getValue() ? AutoFillOrderModule.Phase.FIND_SPAWNER : AutoFillOrderModule.Phase.COLLECT_DROPS, this.replayDelayTicksSetting.getValueInt()
         );
      } else {
         this.phase = AutoFillOrderModule.Phase.DONE;
         this.disable();
      }
   }

   private void tickCloseAfterFill(MinecraftClient client){
      if (this.isShopGuiOpen(client)) {
         client.player.closeHandledScreen();
         this.clickButton++;
         LOGGER.info("Closed delivery GUI {}/2 for the current order.", this.clickButton);
      }
   }

   private AutoFillOrderModule.OrderCandidate createOrderCandidate(ItemStack stack, int page, int slotId){
      if (!stack.isEmpty() && stack.isOf(this.item)) {
         LoreComponent loreComponent = (LoreComponent)stack.get(DataComponentTypes.LORE);
         if (loreComponent == null) {
            return null;
         } else {
            double replace3 = Double.NaN;
            long max = Long.MAX_VALUE;

            for (Text text : loreComponent.lines()) {
               String string = text.getString();
               Matcher matcher = PRICE_PATTERN.matcher(string);
               if (Double.isNaN(replace3) && matcher.find()) {
                  try {
                     replace3 = Double.parseDouble(matcher.group(1).replace(',', '.'));
                     String toUpperCase = matcher.group(2).toUpperCase();

                     replace3 *= switch (toUpperCase) {
                        case "K" -> 1000.0;
                        case "M" -> 1000000.0;
                        case "B" -> 1.0E9;
                        default -> 1.0;
                     };
                  } catch (NumberFormatException e2) {
                     return null;
                  }
               }

               if (!string.contains("$")) {
                  Matcher matcher2 = STOCK_PATTERN.matcher(string);
                  if (matcher2.find()) {
                     try {
                        long replace2 = Long.parseLong(matcher2.group(1).replace(",", ""));
                        long replace = Long.parseLong(matcher2.group(2).replace(",", ""));
                        max = Math.max(0L, replace - replace2);
                     } catch (NumberFormatException e) {
                        return null;
                     }
                  }
               }
            }

            return !Double.isNaN(replace3) && max > 0L ? new AutoFillOrderModule.OrderCandidate(page, slotId, replace3, stack.getName().getString(), max) : null;
         }
      } else {
         return null;
      }
   }

   private void rememberGuiSyncId(MinecraftClient client){
      ScreenHandler screenHandler = client.player.currentScreenHandler;
      this.guiSyncId = screenHandler.syncId;
      this.clickSlot++;
      if (this.expectedChatMessage.isEmpty()) {
         this.expectedChatMessage = getChatLastMessage(client);
      }

      this.attemptedSlots.clear();
   }

   private void updateRemainingCount(MinecraftClient client){
      int countInventoryItems = this.countInventoryItems(client);
      this.remainingToFill = Math.max(0, this.totalNeeded - countInventoryItems);
      this.deliveredCount = countInventoryItems;
      this.totalSpent = this.currentOrder.price;
      this.lastChatMessage = this.currentOrder.displayName;
   }

   private static String getChatLastMessage(MinecraftClient client){
      return client.currentScreen == null ? "" : client.currentScreen.getTitle().getString();
   }

   private AutoFillOrderModule.PageInfo getCurrentPageInfo(MinecraftClient client){
      if (client.currentScreen == null) {
         return null;
      } else {
         Matcher matcher = ORDER_COUNT_PATTERN.matcher(client.currentScreen.getTitle().getString());
         if (!matcher.find()) {
            return null;
         } else {
            try {
               int group2 = Integer.parseInt(matcher.group(1));
               int group = Integer.parseInt(matcher.group(2));
               return group2 >= 1 && group >= group2 ? new AutoFillOrderModule.PageInfo(group2, group) : null;
            } catch (NumberFormatException e) {
               return null;
            }
         }
      }
   }

   private int countInventoryItems(MinecraftClient client){
      int var2 = 0;
      PlayerInventory playerInventory = client.player.getInventory();

      for (int index = 0; index < 36; index++) {
         ItemStack stack = playerInventory.getStack(index);
         if (stack.isOf(this.item)) {
            var2 += stack.getCount();
         }
      }

      return var2;
   }

   private static String formatPrice(double price){
      return Math.rint(price) == price ? Long.toString((long)price) : String.format(Locale.ROOT, "%.2f", price).replaceAll("0+$", "").replaceAll("\\.$", "");
   }

   private void clearCurrentOrder(){
      this.currentOrder = null;
      this.orderCandidates.clear();
      this.pageInde = 0;
      this.orderInde = 0;
      this.scanStartSlot = -1;
      this.scanEndSlot = -1;
      this.bestPageInde = -1;
      this.confirmedCount = 0;
      this.totalNeeded = 0;
      this.expectedChatMessage = "";
      this.orderName = "";
      this.guiSyncId = -1;
      this.clickSlot = 0;
      this.clickButton = 0;
      this.lastClickNanos = 0L;
      this.retryDelay = 0;
      this.inventorySlot = -1;
      this.guiSlot = 0;
      this.attemptedClickCount = -1;
      this.failedClickCount = 0;
      this.attemptedSlots.clear();
   }

   private void resetFillStats(){
      this.remainingToFill = 0;
      this.deliveredCount = 0;
      this.totalSpent = 0.0;
      this.lastChatMessage = "";
   }

   private ItemEntity findSpawner(MinecraftClient client){
      ItemEntity itemEntity = null;
      double entityPos2 = 256.0;
      Vec3d vec = client.player.getEntityPos();

      for (Entity entity : client.world.getEntities()) {
         if (entity instanceof ItemEntity var8 && !var8.isRemoved() && var8.getStack().isOf(this.item)) {
            double entityPos = vec.squaredDistanceTo(var8.getEntityPos());
            if (entityPos < entityPos2) {
               entityPos2 = entityPos;
               itemEntity = var8;
            }
         }
      }

      return itemEntity;
   }

   private boolean hasEnoughItems(MinecraftClient client){
      PlayerInventory playerInventory = client.player.getInventory();

      for (int index = 0; index < 36; index++) {
         ItemStack stack = playerInventory.getStack(index);
         if (stack.isEmpty() || stack.isOf(this.item) && stack.getCount() < stack.getMaxCount()) {
            return true;
         }
      }

      return false;
   }

   private void completeOrder(MinecraftClient client){
      LOGGER.info("AutoFillOrder completed: no {} remains after {} order cycles.", this.getItemName(), this.orderCycles);
      if (this.replayModeSetting.getValue()) {
         client.player
            .sendMessage(Text.literal("Â§8[Â§bAutoFillOrderÂ§8] Â§aNo " + this.getItemName() + " remains; waiting for the next replay."), false);
         this.clearCurrentOrder();
         this.setPhase(
            this.requireSpawnerSetting.getValue() ? AutoFillOrderModule.Phase.FIND_SPAWNER : AutoFillOrderModule.Phase.COLLECT_DROPS, this.replayDelayTicksSetting.getValueInt()
         );
      } else {
         client.player
            .sendMessage(
               Text.literal("Â§8[Â§bAutoFillOrderÂ§8] Â§aFinished: no " + this.getItemName() + " remains in inventory or on the ground."), false
            );
         this.phase = AutoFillOrderModule.Phase.DONE;
         this.disable();
      }
   }

   public static void applyPickupInput(ClientPlayerEntity player){
      AutoFillOrderModule autoFillOrderModule = instance;
      MinecraftClient client = MinecraftClient.getInstance();
      if (autoFillOrderModule != null
         && autoFillOrderModule.isEnabled()
         && autoFillOrderModule.phase == AutoFillOrderModule.Phase.COLLECT_DROPS
         && autoFillOrderModule.targetSpawner != null
         && !autoFillOrderModule.targetSpawner.isRemoved()
         && player != null
         && player == client.player
         && client.currentScreen == null
         && !player.hasVehicle()
         && !player.isGliding()) {
         double x = autoFillOrderModule.targetSpawner.getX() - player.getX();
         double z = autoFillOrderModule.targetSpawner.getZ() - player.getZ();
         double var7 = x * x + z * z;
         PlayerInput playerInput = player.input.playerInput;
         if (var7 <= 0.7224999999999999) {
            applyMovementInput(player, playerInput, 0.0F, 0.0F, false);
         } else {
            double sqrt2 = 1.0 / Math.sqrt(var7);
            double var12 = x * sqrt2;
            double var14 = z * sqrt2;
            double yaw = Math.toRadians(player.getYaw());
            float sin = (float)(var12 * Math.cos(yaw) + var14 * Math.sin(yaw));
            float cos = (float)(-var12 * Math.sin(yaw) + var14 * Math.cos(yaw));
            float var20 = 0.0F;
            float var21 = 0.0F;
            float var22 = -Float.MAX_VALUE;

            for (float[] var26 : PAGE_OFFSETS) {
               float sqrt = (float)Math.sqrt(var26[0] * var26[0] + var26[1] * var26[1]);
               float var28 = var26[0] / sqrt;
               float var29 = var26[1] / sqrt;
               float var30 = sin * var28 + cos * var29;
               if (var30 > var22) {
                  var22 = var30;
                  var20 = var26[0];
                  var21 = var26[1];
               }
            }

            applyMovementInput(player, playerInput, var20, var21, var21 > 0.0F);
         }
      }
   }

   private static void applyMovementInput(ClientPlayerEntity player, PlayerInput current, float strafe, float forward, boolean sprint){
      boolean var5 = forward > 0.0F;
      boolean var6 = forward < 0.0F;
      boolean var7 = strafe > 0.0F;
      boolean var8 = strafe < 0.0F;
      player.input.playerInput = new PlayerInput(var5, var6, var7, var8, false, current.sneak(), sprint && var5 && !var6);
      ((InputAccessor)player.input)
         .astatine$setMovementVector(strafe == 0.0F && forward == 0.0F ? Vec2f.ZERO : new Vec2f(strafe, forward).normalize());
   }

   private BlockPos findNearbySpawnerPos(MinecraftClient client){
      BlockPos pos3 = client.player.getBlockPos();
      BlockPos pos2 = null;
      double ofCenter2 = 16.0;

      for (int index3 = -4; index3 <= 4; index3++) {
         for (int index2 = -4; index2 <= 4; index2++) {
            for (int index = -4; index <= 4; index++) {
               BlockPos pos = pos3.add(index3, index2, index);
               if (client.world.getBlockState(pos).isOf(Blocks.SPAWNER)) {
                  double ofCenter = client.player.getEntityPos().squaredDistanceTo(Vec3d.ofCenter(pos));
                  if (ofCenter <= ofCenter2) {
                     ofCenter2 = ofCenter;
                     pos2 = pos;
                  }
               }
            }
         }
      }

      return pos2;
   }

   private Slot findItemInGui(MinecraftClient client, Item item, int excludedSlotId){
      ScreenHandler screenHandler = client.player.currentScreenHandler;
      PlayerInventory playerInventory = client.player.getInventory();

      for (Slot slot : screenHandler.slots) {
         if (slot.id != excludedSlotId && slot.inventory != playerInventory && slot.getStack().isOf(item)) {
            return slot;
         }
      }

      return null;
   }

   private boolean isShopGuiOpen(MinecraftClient client){
      return client.player.currentScreenHandler != null && client.player.currentScreenHandler != client.player.playerScreenHandler;
   }

   private boolean isGuiSlotValid(MinecraftClient client, int slotId){
      ScreenHandler screenHandler = client.player.currentScreenHandler;
      return screenHandler != null
         && screenHandler != client.player.playerScreenHandler
         && slotId >= 0
         && slotId < screenHandler.slots.size()
         && !((Slot)screenHandler.slots.get(slotId)).getStack().isEmpty();
   }

   private void clickGuiSlot(MinecraftClient client, int slotId, int button, SlotActionType actionType){
      ScreenHandler screenHandler = client.player.currentScreenHandler;
      if (screenHandler != null && screenHandler != client.player.playerScreenHandler && slotId >= 0 && slotId < screenHandler.slots.size()) {
         client.interactionManager.clickSlot(screenHandler.syncId, slotId, button, actionType, client.player);
      }
   }

   private String getItemName(){
      return Registries.ITEM.getId(this.item).getPath();
   }

   private void setPhase(AutoFillOrderModule.Phase nextPhase, int delayTicks){
      this.phase = nextPhase;
      this.phaseTicks = 0;
      this.pageTicks = Math.max(0, delayTicks);
   }

   private void stopWithMessage(String message){
      LOGGER.warn("AutoFillOrder stopped: {}", message);
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null) {
         client.player.sendMessage(Text.literal("Â§8[Â§bAutoFillOrderÂ§8] Â§c" + message), false);
      }

      this.phase = AutoFillOrderModule.Phase.DONE;
      this.disable();
   }

   @Environment(EnvType.CLIENT)
   private record OrderCandidate(int page, int slotId, double price, String displayName, long remaining){
   }

   @Environment(EnvType.CLIENT)
   private record PageInfo(int current, int total){
   }

   @Environment(EnvType.CLIENT)
   private static enum Phase {
      IDLE,
      FIND_SPAWNER,
      WAIT_SPAWNER_GUI,
      DUMP_SPAWNER_PAGE,
      COLLECT_DROPS,
      SEND_ORDER,
      WAIT_ORDER_GUI,
      CLICK_ORDER_SEARCH,
      WAIT_ITEM_INPUT,
      SUBMIT_ANVIL,
      WAIT_RESULT_GUI,
      LOG_RESULT,
      SCAN_ORDER_PAGE,
      NAVIGATE_BEST_PAGE,
      SELECT_BEST_ORDER,
      WAIT_FILL_GUI,
      FILL_ORDER_GUI,
      VERIFY_FILL_TRANSFER,
      CLOSE_FILLED_GUI,
      WAIT_CONFIRM_GUI,
      FINISH_CONFIRM,
      DONE;
   }
}

