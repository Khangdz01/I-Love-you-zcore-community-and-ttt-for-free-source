package made4mischief.astatine.client.utils.render.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum AnimationType {
   LINEAR(Easing::linear),
   EASE_IN(Easing::easeIn),
   EASE_OUT(Easing::easeOut),
   FAST_SETTLE(Easing::fastSettle),
   EASE_IN_OUT(Easing::easeInOut),
   CUBIC(Easing::cubic),
   QUART(Easing::quart),
   QUINT(Easing::quint),
   BACK(Easing::back),
   BOUNCE(Easing::bounce),
   ELASTIC(Easing::elastic);

   private final AnimationType.Curve curve;

   private AnimationType(AnimationType.Curve curve){
      this.curve = curve;
   }

   public float ease(float t){
      return this.curve.apply(t);
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   private interface Curve {
      float apply(float var1);
   }
}
