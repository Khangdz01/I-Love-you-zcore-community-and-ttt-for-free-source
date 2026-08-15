package made4mischief.astatine.client.modules.combat;

import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.ModeSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;

@Environment(EnvType.CLIENT)
public final class CriticalsModule extends Module {
   private static CriticalsModule instance;
   private final ModeSetting modeSetting = this.addMode("Mode", "Packet", new String[]{"Packet", "Jump"});

   public CriticalsModule(){
      super("Criticals", Category.COMBAT, "Biến mỗi đòn đánh thành chí mạng.", -1, true);
      instance = this;
   }

   public static void beforeAttack(PlayerEntity player){
      if (instance != null && instance.isEnabled()) {
         if (player instanceof ClientPlayerEntity var1) {
            if (canCrit(player)) {
               if (instance.modeSetting.is("Packet")) {
                  double x = player.getX();
                  double y = player.getY();
                  double z = player.getZ();
                  var1.networkHandler.sendPacket(new PositionAndOnGround(x, y + 0.0625, z, false, false));
                  var1.networkHandler.sendPacket(new PositionAndOnGround(x, y, z, false, false));
               } else {
                  player.jump();
               }
            }
         }
      }
   }

   private static boolean canCrit(PlayerEntity player){
      return player.isOnGround()
         && !player.isClimbing()
         && !player.isTouchingWater()
         && !player.hasVehicle()
         && !player.hasStatusEffect(StatusEffects.BLINDNESS)
         && !player.isSprinting();
   }
}
