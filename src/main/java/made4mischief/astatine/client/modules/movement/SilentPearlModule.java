package made4mischief.astatine.client.modules.movement;

import made4mischief.astatine.client.mixin.ClientWorldAccessor;
import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.utils.inventory.InventoryUtil;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Hand;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PendingUpdateManager;

@Environment(EnvType.CLIENT)
public final class SilentPearlModule extends Module {
   private boolean waitingForSwap;

   public SilentPearlModule(){
      super("SilentPearl", Category.MOVEMENT, "Âm thầm ném ngọc Ender theo hướng nhìn.", -1, true);
   }

   @Override
   protected void onEnable(){
      this.waitingForSwap = true;
   }

   @Override
   protected void onDisable(){
      this.waitingForSwap = false;
   }

   public boolean isUpcomingInventoryInteraction(){
      return true;
   }

   @EventTarget
   public void onTick(TickEvent event){
      if (this.waitingForSwap) {
         MinecraftClient client = event.getClient();
         if (client.player != null && client.world != null && client.player.networkHandler != null && !client.player.isDead()) {
            this.waitingForSwap = false;
            if (client.player.getOffHandStack().isOf(Items.ENDER_PEARL)) {
               this.useEnderPearl(client, Hand.OFF_HAND);
               this.disable();
            } else {
               int findHotBarItem = InventoryUtil.findHotBarItem(client, Items.ENDER_PEARL);
               if (findHotBarItem == -1) {
                  this.disable();
               } else {
                  int selectedSlot = client.player.getInventory().getSelectedSlot();
                  boolean var5 = findHotBarItem != selectedSlot;
                  if (var5) {
                     client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(findHotBarItem));
                  }

                  try {
                     this.useEnderPearl(client, Hand.MAIN_HAND);
                  } finally {
                     if (var5) {
                        client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(selectedSlot));
                     }

                     this.disable();
                  }
               }
            }
         } else {
            this.disable();
         }
      }
   }

   private void useEnderPearl(MinecraftClient client, Hand hand){
      PendingUpdateManager pendingUpdateManager = ((ClientWorldAccessor)client.world).astatine$getPendingUpdateManager().incrementSequence();

      try {
         client.player
            .networkHandler
            .sendPacket(new PlayerInteractItemC2SPacket(hand, pendingUpdateManager.getSequence(), client.player.getYaw(), client.player.getPitch()));
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
}
