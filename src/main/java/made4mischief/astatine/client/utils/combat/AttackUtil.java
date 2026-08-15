package made4mischief.astatine.client.utils.combat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Hand;
import net.minecraft.entity.LivingEntity;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public final class AttackUtil {
   private AttackUtil(){
   }

   public static boolean attackTarget(MinecraftClient client, LivingEntity target, double maxRange){
      if (client.player == null || client.interactionManager == null || target == null) {
         return false;
      } else if (!client.player.isDead() && target.isAlive()) {
         if (client.player.distanceTo(target) > maxRange) {
            return false;
         } else {
            client.interactionManager.attackEntity(client.player, target);
            client.player.swingHand(Hand.MAIN_HAND);
            return true;
         }
      } else {
         return false;
      }
   }
}
