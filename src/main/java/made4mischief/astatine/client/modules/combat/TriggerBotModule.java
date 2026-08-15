package made4mischief.astatine.client.modules.combat;

import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.modules.player.FriendModule;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.ModeSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Hand;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.EntityHitResult;

@Environment(EnvType.CLIENT)
public final class TriggerBotModule extends Module {
   private final NumberSetting delaySetting = this.addNumber("Delay", 10.0, 0.0, 20.0, 1.0);
   private final NumberSetting rangeSetting = this.addNumber("Range", 4.5, 1.0, 6.0, 0.5);
   private final ModeSetting targetSetting = this.addMode("Target", "All Living", new String[]{"All Living", "Players Only"});
   private final BooleanSetting requireAttackKeySetting = this.addBoolean("Require Attack Key", false);
   private int delayTicks;

   public TriggerBotModule(){
      super("TriggerBot", Category.COMBAT, "Tự đánh thực thể nằm dưới tâm ngắm.", -1, true);
   }

   @Override
   protected void onEnable(){
      this.delayTicks = 0;
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      if (client.player != null && client.world != null && client.interactionManager != null) {
         if (this.delayTicks > 0) {
            this.delayTicks--;
         } else if (!this.requireAttackKeySetting.getValue() || client.options.attackKey.isPressed()) {
            if (client.crosshairTarget instanceof EntityHitResult var3) {
               if (var3.getEntity() instanceof LivingEntity var5) {
                  if (var5.isAlive() && var5 != client.player) {
                     if (!FriendModule.isFriend(var5)) {
                        if (!this.targetSetting.is("Players Only") || var5 instanceof PlayerEntity) {
                           if (!(client.player.distanceTo(var5) > this.rangeSetting.getValue())) {
                              client.interactionManager.attackEntity(client.player, var5);
                              client.player.swingHand(Hand.MAIN_HAND);
                              this.delayTicks = this.delaySetting.getValueInt();
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
