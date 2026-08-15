package made4mischief.astatine.client.utils.inventory;

import java.util.Collection;
import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public class InventoryUtil {
   public static int findHotBarItem(MinecraftClient client, Item item){
      if (client.player == null) {
         return -1;
      } else {
         for (int index = 0; index < 9; index++) {
            ItemStack stack = client.player.getInventory().getStack(index);
            if (stack.isOf(item)) {
               return index;
            }
         }

         return -1;
      }
   }

   public static int findInventoryItem(MinecraftClient client, Item item){
      if (client.player == null) {
         return -1;
      } else {
         for (int index = 0; index < 36; index++) {
            ItemStack stack = client.player.getInventory().getStack(index);
            if (stack.isOf(item)) {
               return index;
            }
         }

         return -1;
      }
   }

   public static boolean hasMatchingItem(MinecraftClient client, Predicate<ItemStack> itemFilter){
      if (client.player == null) {
         return false;
      } else {
         PlayerInventory playerInventory = client.player.getInventory();

         for (int index = 0; index < 36; index++) {
            ItemStack stack = playerInventory.getStack(index);
            if (!stack.isEmpty() && (itemFilter == null || itemFilter.test(stack))) {
               return true;
            }
         }

         return false;
      }
   }

   public static int dumpPlayerInventory(MinecraftClient client){
      return dumpPlayerInventory(client, (Predicate<ItemStack>)null);
   }

   public static int dumpPlayerInventory(MinecraftClient client, Item item){
      return item == null ? dumpPlayerInventory(client) : dumpPlayerInventory(client, (Predicate<ItemStack>)(stack -> stack.isOf(item)));
   }

   public static int dumpPlayerInventory(MinecraftClient client, Collection<Item> items){
      return items != null && !items.isEmpty()
         ? dumpPlayerInventory(client, (Predicate<ItemStack>)(stack -> items.contains(stack.getItem())))
         : dumpPlayerInventory(client);
   }

   public static int dumpPlayerInventory(MinecraftClient client, Predicate<ItemStack> itemFilter){
      if (client.player != null && client.interactionManager != null) {
         ScreenHandler screenHandler = client.player.currentScreenHandler;
         PlayerInventory playerInventory = client.player.getInventory();
         int index = 0;

         for (Slot slot : screenHandler.slots) {
            if (slot.inventory == playerInventory && slot.getIndex() >= 0 && slot.getIndex() < 36) {
               ItemStack stack = slot.getStack();
               if (!stack.isEmpty() && (itemFilter == null || itemFilter.test(stack))) {
                  client.interactionManager.clickSlot(screenHandler.syncId, slot.id, 1, SlotActionType.THROW, client.player);
                  index++;
               }
            }
         }

         return index;
      } else {
         return 0;
      }
   }

   public static int dumpOpenContainerToGround(MinecraftClient client, Predicate<ItemStack> itemFilter, boolean autoClose){
      if (client.player != null && client.interactionManager != null) {
         ScreenHandler screenHandler = client.player.currentScreenHandler;
         if (screenHandler != null && screenHandler != client.player.playerScreenHandler) {
            PlayerInventory playerInventory = client.player.getInventory();
            int index = 0;

            for (Slot slot : screenHandler.slots) {
               if (slot.inventory != playerInventory) {
                  ItemStack stack = slot.getStack();
                  if (!stack.isEmpty() && (itemFilter == null || itemFilter.test(stack))) {
                     client.interactionManager.clickSlot(screenHandler.syncId, slot.id, 1, SlotActionType.THROW, client.player);
                     index++;
                  }
               }
            }

            if (autoClose) {
               client.player.closeHandledScreen();
            }

            return index;
         } else {
            return 0;
         }
      } else {
         return 0;
      }
   }

   public static int dumpItemsToOpenGUI(MinecraftClient client, Predicate<ItemStack> itemFilter, boolean autoClose){
      if (client.player != null && client.interactionManager != null) {
         ScreenHandler screenHandler = client.player.currentScreenHandler;
         if (screenHandler != null && screenHandler != client.player.playerScreenHandler) {
            int index = 0;
            PlayerInventory playerInventory = client.player.getInventory();

            for (Slot slot : screenHandler.slots) {
               if (slot.inventory == playerInventory) {
                  ItemStack stack = slot.getStack();
                  if (!stack.isEmpty() && (itemFilter == null || itemFilter.test(stack))) {
                     client.interactionManager.clickSlot(screenHandler.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, client.player);
                     index++;
                  }
               }
            }

            if (autoClose && index > 0) {
               client.player.closeHandledScreen();
            }

            return index;
         } else {
            return 0;
         }
      } else {
         return 0;
      }
   }
}

