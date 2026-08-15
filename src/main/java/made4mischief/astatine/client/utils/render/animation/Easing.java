package made4mischief.astatine.client.utils.render.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Easing {
   private Easing(){
   }

   public static float linear(float t){
      return t;
   }

   public static float easeIn(float t){
      return t * t;
   }

   public static float easeOut(float t){
      return t * (2.0F - t);
   }

   public static float fastSettle(float t){
      if (t <= 0.0F) {
         return 0.0F;
      } else if (t >= 1.0F) {
         return 1.0F;
      } else {
         double var1 = 6.5;
         return (float)((1.0 - Math.exp(-6.5 * t)) / (1.0 - Math.exp(-6.5)));
      }
   }

   public static float easeInOut(float t){
      return t < 0.5F ? 2.0F * t * t : 1.0F - quadCurve(-2.0F * t + 2.0F) / 2.0F;
   }

   public static float cubic(float t){
      return t < 0.5F ? 4.0F * t * t * t : 1.0F - cubicCurve(-2.0F * t + 2.0F) / 2.0F;
   }

   public static float quart(float t){
      return t < 0.5F ? 8.0F * quarticCurve(t) : 1.0F - quarticCurve(-2.0F * t + 2.0F) / 2.0F;
   }

   public static float quint(float t){
      return t < 0.5F ? 16.0F * quinticCurve(t) : 1.0F - quinticCurve(-2.0F * t + 2.0F) / 2.0F;
   }

   public static float back(float t){
      float var1 = 1.70158F;
      float var2 = 2.5949094F;
      return t < 0.5F
         ? quadCurve(2.0F * t) * (7.189819F * t - 2.5949094F) / 2.0F
         : (quadCurve(2.0F * t - 2.0F) * (3.5949094F * (2.0F * t - 2.0F) + 2.5949094F) + 2.0F) / 2.0F;
   }

   public static float bounce(float t){
      return bounceCurve(t);
   }

   public static float elastic(float t){
      if (t <= 0.0F) {
         return 0.0F;
      } else if (t >= 1.0F) {
         return 1.0F;
      } else {
         float var1 = (float) (Math.PI * 2.0 / 3.0);
         return (float)(Math.pow(2.0, -10.0 * t) * Math.sin((10.0 * t - 0.75) * (float) (Math.PI * 2.0 / 3.0)) + 1.0);
      }
   }

   private static float bounceCurve(float t){
      float var1 = 7.5625F;
      float var2 = 2.75F;
      if (t < 0.36363637F) {
         return 7.5625F * t * t;
      } else if (t < 0.72727275F) {
         t -= 0.54545456F;
         return 7.5625F * t * t + 0.75F;
      } else if (t < 0.90909094F) {
         t -= 0.8181818F;
         return 7.5625F * t * t + 0.9375F;
      } else {
         t -= 0.95454544F;
         return 7.5625F * t * t + 0.984375F;
      }
   }

   private static float quadCurve(float v){
      return v * v;
   }

   private static float cubicCurve(float v){
      return v * v * v;
   }

   private static float quarticCurve(float v){
      return v * v * v * v;
   }

   private static float quinticCurve(float v){
      return v * v * v * v * v;
   }
}
