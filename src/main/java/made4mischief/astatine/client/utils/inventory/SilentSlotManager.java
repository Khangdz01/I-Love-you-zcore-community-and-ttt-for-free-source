package made4mischief.astatine.client.utils.inventory;

import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;

@Environment(EnvType.CLIENT)
public final class SilentSlotManager {
   private static ClientPlayNetworkHandler networkHandler;
   private static int currentSlot = -1;
   private static boolean swapping;

   private SilentSlotManager(){
   }

   public static boolean runWithSlot(MinecraftClient client, int slot, Runnable action){
      Objects.requireNonNull(action, "action");
      if (isValidSlot(slot) && isClientReady(client) && !swapping) {
         int selectedSlot = client.player.getInventory().getSelectedSlot();
         syncServerSlot(client);
         int var4 = currentSlot;
         swapping = true;

         try {
            selectServerSlot(client, slot);
            if (selectedSlot != slot) {
               client.player.getInventory().setSelectedSlot(slot);
            }

            action.run();
         } finally {
            try {
               if (selectedSlot != slot && client.player != null) {
                  client.player.getInventory().setSelectedSlot(selectedSlot);
               }

               selectServerSlot(client, var4);
            } finally {
               swapping = false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static void selectServerSlot(MinecraftClient client, int slot){
      if (isValidSlot(slot) && isClientReady(client)) {
         syncServerSlot(client);
         if (currentSlot != slot) {
            client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
         }
      }
   }

   public static int getServerSlot(MinecraftClient client){
      if (!isClientReady(client)) {
         return -1;
      } else {
         syncServerSlot(client);
         return currentSlot;
      }
   }

   public static void observeOutgoingPacket(ClientPlayNetworkHandler handler, Packet<?> packet){
      if (packet instanceof UpdateSelectedSlotC2SPacket var2) {
         if (networkHandler != handler) {
            networkHandler = handler;
         }

         currentSlot = var2.getSelectedSlot();
      }
   }

   private static void syncServerSlot(MinecraftClient client){
      if (networkHandler != client.player.networkHandler) {
         networkHandler = client.player.networkHandler;
         currentSlot = client.player.getInventory().getSelectedSlot();
         swapping = false;
      }
   }

   private static boolean isClientReady(MinecraftClient client){
      return client != null && client.player != null && client.world != null && client.player.networkHandler != null;
   }

   private static boolean isValidSlot(int slot){
      return slot >= 0 && slot < 9;
   }
}
