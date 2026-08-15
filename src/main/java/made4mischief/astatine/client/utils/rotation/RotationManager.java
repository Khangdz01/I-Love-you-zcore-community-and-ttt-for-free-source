package made4mischief.astatine.client.utils.rotation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public final class RotationManager {
   public static float yaw;
   public static float pitch;
   public static boolean isRotating;
   private static boolean rotateModel;
   private static boolean silentMovement;
   private static Object owner;
   private static float sentYaw;
   private static float sentPitch;
   private static boolean rotationSent;

   private RotationManager(){
   }

   public static boolean isRotating(){
      return isRotating;
   }

   public static float getYaw(){
      return yaw;
   }

   public static float getPitch(){
      return pitch;
   }

   public static void setRotation(float inputYaw, float inputPitch){
      setRotation(inputYaw, inputPitch, false, false);
   }

   public static void setRotation(float inputYaw, float inputPitch, boolean rotateModel){
      setRotation(inputYaw, inputPitch, rotateModel, false);
   }

   public static void setRotation(float inputYaw, float inputPitch, boolean rotateModel, boolean silentMovement){
      applyRotation(null, inputYaw, inputPitch, rotateModel, silentMovement);
   }

   public static void setRotation(Object owner, float inputYaw, float inputPitch, boolean rotateModel, boolean silentMovement){
      applyRotation(owner, inputYaw, inputPitch, rotateModel, silentMovement);
   }

   private static void applyRotation(Object owner, float inputYaw, float inputPitch, boolean rotateModel, boolean silentMovement){
      if (Float.isFinite(inputYaw) && Float.isFinite(inputPitch)) {
         yaw = normalizeYaw(inputYaw);
         pitch = clampPitch(inputPitch);
         rotateModel = rotateModel;
         silentMovement = silentMovement;
         owner = owner;
         isRotating = true;
      } else {
         clearRotatingState(owner);
      }
   }

   public static boolean shouldRotateClientModel(){
      return isRotating && rotateModel;
   }

   public static boolean shouldTransformMovement(){
      return isRotating && silentMovement;
   }

   public static void markRotationSent(float sentYaw, float sentPitch){
      sentYaw = normalizeYaw(sentYaw);
      sentPitch = clampPitch(sentPitch);
      rotationSent = true;
   }

   public static boolean wasRotationSent(float expectedYaw, float expectedPitch, float tolerance){
      if (!rotationSent) {
         return false;
      } else {
         float wrapDegrees = Math.abs(MathHelper.wrapDegrees(sentYaw - expectedYaw));
         float abs = Math.abs(sentPitch - expectedPitch);
         return wrapDegrees <= tolerance && abs <= tolerance;
      }
   }

   public static float normalizeYaw(float yaw){
      return MathHelper.wrapDegrees(yaw);
   }

   public static float clampPitch(float pitch){
      return MathHelper.clamp(pitch, -90.0F, 90.0F);
   }

   public static void clearRotatingState(){
      isRotating = false;
      rotateModel = false;
      silentMovement = false;
      owner = null;
   }

   public static void clearRotatingState(Object owner){
      if (owner == owner) {
         clearRotatingState();
      }
   }
}

