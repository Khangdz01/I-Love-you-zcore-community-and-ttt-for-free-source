package made4mischief.astatine.client.modules.smp;

import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.ItemTargetSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import made4mischief.astatine.client.utils.inventory.InventoryUtil;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public final class AutoSellModule extends Module {
   private final ItemTargetSetting itemSetting = this.addSetting(
      new ItemTargetSetting(
         "Items to Sell",
         Items.COBBLESTONE,
         Items.WHEAT,
         Items.CARROT,
         Items.POTATO,
         Items.SUGAR_CANE,
         Items.KELP,
         Items.BAMBOO,
         Items.ROTTEN_FLESH,
         Items.BONE,
         Items.GUNPOWDER,
         Items.STRING,
         Items.SPIDER_EYE
      )
   );
   private final NumberSetting delayMsSetting = this.addNumber("Delay (ms)", 500.0, 100.0, 3000.0, 50.0);
   private final BooleanSetting autoCloseGUISetting = this.addBoolean("Auto Close GUI", true);
   private final BooleanSetting chatNotifySetting = this.addBoolean("Chat Notify", true);
   private AutoSellModule.State state = AutoSellModule.State.IDLE;
   private long lastDumpTime = 0L;
   private long lastCheckTime = 0L;

   public AutoSellModule(){
      super("AutoSell", Category.SMP, "Tự bán liên tục các vật phẩm đã chọn.", -1);
   }

   @Override
   protected void onEnable(){
      this.state = AutoSellModule.State.IDLE;
      this.lastDumpTime = 0L;
   }

   @Override
   protected void onDisable(){
      this.state = AutoSellModule.State.IDLE;
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      if (client.player != null && client.world != null) {
         long currentTimeMillis = System.currentTimeMillis();
         boolean var5 = client.player.currentScreenHandler != null && client.player.currentScreenHandler != client.player.playerScreenHandler;
         if (var5) {
            int value = InventoryUtil.dumpItemsToOpenGUI(client, stack -> this.itemSetting.isSelected(stack.getItem()), this.autoCloseGUISetting.getValue());
            if (value > 0) {
               if (this.chatNotifySetting.getValue()) {
                  client.player.sendMessage(Text.literal("§8[§bAutoSell§8] §aSuccessfully dumped " + value + " item stacks into sell GUI!"), false);
               }

               this.lastDumpTime = currentTimeMillis;
               this.state = AutoSellModule.State.COOLDOWN;
            }
         } else {
            switch (this.state) {
               case IDLE:
               case COOLDOWN:
                  long value2 = (long)this.delayMsSetting.getValue();
                  if (currentTimeMillis - this.lastDumpTime < value2) {
                     return;
                  }

                  boolean item = InventoryUtil.hasMatchingItem(client, stack -> this.itemSetting.isSelected(stack.getItem()));
                  if (item && client.player.networkHandler != null) {
                     client.player.networkHandler.sendChatCommand("sell");
                     this.state = AutoSellModule.State.WAITING_FOR_GUI;
                     this.lastCheckTime = currentTimeMillis;
                  }
                  break;
               case WAITING_FOR_GUI:
                  if (currentTimeMillis - this.lastCheckTime > 3000L) {
                     this.state = AutoSellModule.State.COOLDOWN;
                     this.lastDumpTime = currentTimeMillis;
                  }
            }
         }
      }
   }

   @Environment(EnvType.CLIENT)
   private static enum State {
      IDLE,
      WAITING_FOR_GUI,
      COOLDOWN;
   }
}
