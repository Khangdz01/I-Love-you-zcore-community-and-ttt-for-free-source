package made4mischief.astatine.client.modules.movement;

import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.PlayerInput;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

@Environment(EnvType.CLIENT)
public final class SprintModule extends Module {
   private boolean wasSprinting;

   public SprintModule(){
      super("Sprint", Category.MOVEMENT, "Tự chạy nước rút khi đi tới.", -1, true);
   }

   @Override
   protected void onDisable(){
      ClientPlayerEntity player = MinecraftClient.getInstance().player;
      if (player != null && this.wasSprinting) {
         player.setSprinting(false);
      }

      this.wasSprinting = false;
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      ClientPlayerEntity player = client.player;
      if (player != null && client.world != null && !player.isDead()) {
         boolean var4 = canSprint(player);
         if (var4) {
            if (!player.isSprinting()) {
               player.setSprinting(true);
               this.wasSprinting = true;
            }
         } else if (this.wasSprinting) {
            player.setSprinting(false);
            this.wasSprinting = false;
         }
      } else {
         this.wasSprinting = false;
      }
   }

   private static boolean canSprint(ClientPlayerEntity player){
      PlayerInput playerInput = player.input.playerInput;
      boolean comp_3160 = playerInput.forward() && !playerInput.backward();
      boolean abilities = player.getHungerManager().getFoodLevel() > 6 || player.getAbilities().allowFlying;
      boolean ignoreItemSlowdown = !player.isUsingItem() || NoSlowModule.shouldIgnoreItemSlowdown(player);
      return comp_3160
         && !playerInput.sneak()
         && !player.horizontalCollision
         && !player.hasVehicle()
         && !player.isGliding()
         && !player.hasStatusEffect(StatusEffects.BLINDNESS)
         && abilities
         && ignoreItemSlowdown;
   }
}
