package made4mischief.astatine.client.modules.movement;

import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.BooleanSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.network.ClientPlayerEntity;

@Environment(EnvType.CLIENT)
public final class VelocityModule extends Module {
   private static VelocityModule instance;
   private final BooleanSetting blockPushSetting = this.addBoolean("Block Push", true);
   private final BooleanSetting playerPushSetting = this.addBoolean("Player Push", true);
   private final BooleanSetting explosion = this.addBoolean("Explosion", true);

   public VelocityModule(){
      super("Velocity", Category.MOVEMENT, "Điều chỉnh lực đẩy từ khối, thực thể và vụ nổ.", -1);
      instance = this;
   }

   public static boolean shouldPreventBlockPush(){
      VelocityModule velocityModule = instance;
      return velocityModule != null && velocityModule.isEnabled() && velocityModule.blockPushSetting.getValue();
   }

   public static boolean prepareForPearlPhase(){
      VelocityModule velocityModule = instance;
      if (velocityModule != null && velocityModule.blockPushSetting.getValue()) {
         if (!velocityModule.isEnabled()) {
            velocityModule.enable();
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean shouldModifyPlayerPush(){
      VelocityModule velocityModule = instance;
      return velocityModule != null && velocityModule.isEnabled() && velocityModule.playerPushSetting.getValue();
   }

   public static boolean shouldModifyExplosion(){
      VelocityModule velocityModule = instance;
      return velocityModule != null && velocityModule.isEnabled() && velocityModule.explosion.getValue();
   }

   public static void removeAddedVelocity(ClientPlayerEntity player, Vec3d before){
      player.setVelocity(before);
   }
}
