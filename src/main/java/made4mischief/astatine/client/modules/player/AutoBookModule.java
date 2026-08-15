package made4mischief.astatine.client.modules.player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.StringSetting;
import made4mischief.astatine.client.utils.inventory.InventoryUtil;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Hand;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.component.type.WritableBookContentComponent;
import net.minecraft.component.DataComponentTypes;

@Environment(EnvType.CLIENT)
public final class AutoBookModule extends Module {
   private static final int MAX_BOOK_LENGTH = 1024;
   private static final int PAGE_COUNT = 100;
   private static final int CHARS_PER_PAGE = 100;
   private static final String BOOK_AUTHOR = "Astatine";
   private final StringSetting contentSetting = this.addString("Content", "Astatine", 256);
   private final BooleanSetting autoSwitchSetting = this.addBoolean("Auto Switch", true);
   private final BooleanSetting randomSetting = this.addBoolean("Random", false);
   private AutoBookModule.Stage stage = AutoBookModule.Stage.FIND_BOOK;
   private BookEditScreen bookScreen;
   private int bookSlot = -1;
   private int pendingMoveSlot = -1;
   private int closeScreenSlot = -1;
   private int pageInde;

   public AutoBookModule(){
      super("AutoBook", Category.PLAYER, "Tá»± viáº¿t vÃ  kÃ½ sÃ¡ch báº±ng ná»™i dung Ä‘Ã£ Ä‘áº·t.");
   }

   @Override
   protected void onEnable(){
      this.resetStage();
   }

   @Override
   protected void onDisable(){
      MinecraftClient client = MinecraftClient.getInstance();
      this.tickSignBook(client);
      this.tickMoveBook(client);
      this.tickCloseScreen(client);
      this.resetStage();
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      if (!this.isInGame(client)) {
         this.resetStage();
      } else {
         switch (this.stage) {
            case FIND_BOOK:
               this.tickAutoWrite(client);
               break;
            case BOOK_OPEN:
               this.signBook(client);
               break;
            case WAIT_FOR_SERVER:
               this.writeBookPages(client);
         }
      }
   }

   private void tickAutoWrite(MinecraftClient client){
      if (client.currentScreen == null && client.player.currentScreenHandler == client.player.playerScreenHandler && !this.contentSetting.getValue().isEmpty()) {
         int findBookSlot = this.findBookSlot(client);
         if (findBookSlot == -1) {
            this.tickCloseScreen(client);
         } else {
            if (this.closeScreenSlot == -1) {
               this.closeScreenSlot = client.player.getInventory().getSelectedSlot();
            }

            this.bookSlot = this.moveBookToSlot(client, findBookSlot);
            if (this.bookSlot == -1) {
               this.tickCloseScreen(client);
            } else {
               this.switchSlot(client, this.bookSlot);
               ItemStack stack = client.player.getInventory().getStack(this.bookSlot);
               if (!stack.isOf(Items.WRITABLE_BOOK)) {
                  this.tickMoveBook(client);
                  this.bookSlot = -1;
               } else {
                  WritableBookContentComponent writableBookContentComponent = (WritableBookContentComponent)stack.getOrDefault(DataComponentTypes.WRITABLE_BOOK_CONTENT, WritableBookContentComponent.DEFAULT);
                  this.bookScreen = new BookEditScreen(client.player, stack, Hand.MAIN_HAND, writableBookContentComponent);
                  client.setScreen(this.bookScreen);
                  this.stage = AutoBookModule.Stage.BOOK_OPEN;
               }
            }
         }
      }
   }

   private void signBook(MinecraftClient client){
      ItemStack stack = client.player.getInventory().getStack(this.bookSlot);
      String value = this.contentSetting.getValue();
      if (stack.isOf(Items.WRITABLE_BOOK) && !value.isEmpty()) {
         List list = this.buildPageList(value, this.randomSetting.getValue());
         client.player.networkHandler.sendPacket(new BookUpdateC2SPacket(this.bookSlot, list, Optional.of("Astatine")));
         this.tickSignBook(client);
         this.pageInde = 0;
         this.stage = AutoBookModule.Stage.WAIT_FOR_SERVER;
      } else {
         this.tickSignBook(client);
         this.tickMoveBook(client);
         this.stage = AutoBookModule.Stage.FIND_BOOK;
         this.bookSlot = -1;
         this.pendingMoveSlot = -1;
      }
   }

   private void writeBookPages(MinecraftClient client){
      ItemStack stack = client.player.getInventory().getStack(this.bookSlot);
      if (!stack.isOf(Items.WRITABLE_BOOK)) {
         if (this.pendingMoveSlot == -1 || client.player.currentScreenHandler == client.player.playerScreenHandler) {
            this.tickMoveBook(client);
            this.bookSlot = -1;
            this.pageInde = 0;
            if (this.autoSwitchSetting.getValue()) {
               this.stage = AutoBookModule.Stage.FIND_BOOK;
            } else {
               this.disable();
            }
         }
      } else {
         if (++this.pageInde >= 100) {
            this.disable();
         }
      }
   }

   private int findBookSlot(MinecraftClient client){
      int findHotBarItem = InventoryUtil.findHotBarItem(client, Items.WRITABLE_BOOK);
      return findHotBarItem != -1 ? findHotBarItem : InventoryUtil.findInventoryItem(client, Items.WRITABLE_BOOK);
   }

   private int moveBookToSlot(MinecraftClient client, int inventorySlot){
      this.pendingMoveSlot = -1;
      if (inventorySlot >= 0 && inventorySlot < 9) {
         return inventorySlot;
      } else if (inventorySlot >= 9 && inventorySlot < 36) {
         int findEmptyHotbarSlot = this.findEmptyHotbarSlot(client);
         if (findEmptyHotbarSlot == -1) {
            findEmptyHotbarSlot = client.player.getInventory().getSelectedSlot();
         }

         client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId, inventorySlot, findEmptyHotbarSlot, SlotActionType.SWAP, client.player);
         if (!client.player.getInventory().getStack(findEmptyHotbarSlot).isOf(Items.WRITABLE_BOOK)) {
            return -1;
         } else {
            this.pendingMoveSlot = inventorySlot;
            return findEmptyHotbarSlot;
         }
      } else {
         return -1;
      }
   }

   private void tickMoveBook(MinecraftClient client){
      if (this.pendingMoveSlot != -1
         && this.bookSlot != -1
         && client.player != null
         && client.interactionManager != null
         && client.player.currentScreenHandler == client.player.playerScreenHandler) {
         client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId, this.pendingMoveSlot, this.bookSlot, SlotActionType.SWAP, client.player);
         this.pendingMoveSlot = -1;
      }
   }

   private int findEmptyHotbarSlot(MinecraftClient client){
      for (int index = 0; index < 9; index++) {
         if (client.player.getInventory().getStack(index).isEmpty()) {
            return index;
         }
      }

      return -1;
   }

   private void switchSlot(MinecraftClient client, int slot){
      if (client.player.getInventory().getSelectedSlot() != slot) {
         client.player.getInventory().setSelectedSlot(slot);
         client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
      }
   }

   private List<String> buildPageList(String source, boolean randomized){
      ArrayList var3 = new ArrayList(100);

      for (int index = 0; index < 100; index++) {
         var3.add(randomized ? this.buildPageTextRandomized(source) : this.buildPageText(source));
      }

      return var3;
   }

   private String buildPageText(String source){
      StringBuilder builder = new StringBuilder(1024);

      while (builder.length() < 1024) {
         int length = 1024 - builder.length();
         builder.append(source, 0, Math.min(source.length(), length));
      }

      return builder.toString();
   }

   private String buildPageTextRandomized(String source){
      StringBuilder builder = new StringBuilder(1024);
      ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();

      for (int index = 0; index < 1024; index++) {
         builder.append(source.charAt(threadLocalRandom.nextInt(source.length())));
      }

      return builder.toString();
   }

   private void tickSignBook(MinecraftClient client){
      if (client.currentScreen == this.bookScreen) {
         client.setScreen(null);
      }

      this.bookScreen = null;
   }

   private void tickCloseScreen(MinecraftClient client){
      if (this.closeScreenSlot != -1 && client.player != null) {
         this.switchSlot(client, this.closeScreenSlot);
         this.closeScreenSlot = -1;
      } else {
         this.closeScreenSlot = -1;
      }
   }

   private boolean isInGame(MinecraftClient client){
      return client.player != null && client.world != null && client.interactionManager != null && !client.player.isDead();
   }

   private void resetStage(){
      this.stage = AutoBookModule.Stage.FIND_BOOK;
      this.bookScreen = null;
      this.bookSlot = -1;
      this.pendingMoveSlot = -1;
      this.closeScreenSlot = -1;
      this.pageInde = 0;
   }

   @Environment(EnvType.CLIENT)
   private static enum Stage {
      FIND_BOOK,
      BOOK_OPEN,
      WAIT_FOR_SERVER;
   }
}

