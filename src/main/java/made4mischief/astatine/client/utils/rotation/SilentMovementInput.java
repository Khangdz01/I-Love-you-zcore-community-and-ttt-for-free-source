package made4mischief.astatine.client.utils.rotation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public final class SilentMovementInput {
   private static final float[][] DIRECTIONS = new float[][]{
      {0.0F, 1.0F}, {1.0F, 1.0F}, {1.0F, 0.0F}, {1.0F, -1.0F}, {0.0F, -1.0F}, {-1.0F, -1.0F}, {-1.0F, 0.0F}, {-1.0F, 1.0F}
   };

   private SilentMovementInput(){
   }

   public static SilentMovementInput.Result transform(PlayerInput rawInput, Vec2f rawVector, float cameraYaw, float lockedYaw){
      if (rawVector.lengthSquared() <= 1.0E-6F) {
         return new SilentMovementInput.Result(rawInput, rawVector);
      } else {
         double wrapDegrees = Math.toRadians(MathHelper.wrapDegrees(cameraYaw - lockedYaw));
         double cos = Math.cos(wrapDegrees);
         double sin = Math.sin(wrapDegrees);
         float var10 = (float)(-rawVector.y * sin + rawVector.x * cos);
         float var11 = (float)(rawVector.y * cos + rawVector.x * sin);
         Vec2f vec2f = new Vec2f(var10, var11).normalize();
         float var13 = -Float.MAX_VALUE;
         Vec2f vec2f2 = Vec2f.ZERO;
         float var15 = 0.0F;
         float var16 = 0.0F;

         for (float[] var20 : DIRECTIONS) {
            Vec2f vec2f3 = new Vec2f(var20[0], var20[1]).normalize();
            float var22 = vec2f.x * vec2f3.x + vec2f.y * vec2f3.y;
            if (var22 > var13) {
               var13 = var22;
               vec2f2 = vec2f3;
               var15 = var20[0];
               var16 = var20[1];
            }
         }

         boolean var23 = var16 > 0.0F;
         boolean var24 = var16 < 0.0F;
         boolean var25 = var15 > 0.0F;
         boolean var26 = var15 < 0.0F;
         boolean comp_3165 = rawInput.sprint() && var23 && !var24;
         PlayerInput playerInput = new PlayerInput(var23, var24, var25, var26, rawInput.jump(), rawInput.sneak(), comp_3165);
         return new SilentMovementInput.Result(playerInput, vec2f2);
      }
   }

   @Environment(EnvType.CLIENT)
   public record Result(PlayerInput packetInput, Vec2f physicsVector){
   }
}
