package made4mischief.astatine.client.utils.rotation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public class RotationUtil {
   public static float getYaw(Vec3d from, Vec3d to){
      double var2 = to.x - from.x;
      double var4 = to.y - from.y;
      double var6 = to.z - from.z;
      double atan2 = -Math.toDegrees(Math.atan2(var2, var6));
      return (float)atan2;
   }

   public static float getPitch(Vec3d from, Vec3d to){
      double var2 = to.x - from.x;
      double var4 = to.y - from.y;
      double var6 = to.z - from.z;
      double sqrt = Math.sqrt(var2 * var2 + var6 * var6);
      double atan2 = -Math.toDegrees(Math.atan2(var4, sqrt));
      return (float)atan2;
   }
}
