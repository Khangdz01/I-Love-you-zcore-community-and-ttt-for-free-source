package made4mischief.astatine.client.utils.render.animation;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class AnimationManager {
   private static final float MAX_FRAME_DELTA = 100.0F;
   private static final List<Animation> animations = new ArrayList<>();
   private static long lastTickNanos;

   private AnimationManager(){
   }

   static void register(Animation animation){
      animations.add(animation);
   }

   public static void update(){
      long nanoTime = System.nanoTime();
      if (lastTickNanos == 0L) {
         lastTickNanos = nanoTime;
      } else {
         float var2 = (float)(nanoTime - lastTickNanos) / 1000000.0F;
         lastTickNanos = nanoTime;
         if (var2 > 100.0F) {
            var2 = 100.0F;
         }

         for (int index = 0; index < animations.size(); index++) {
            animations.get(index).tick(var2);
         }
      }
   }

   public static int count(){
      return animations.size();
   }
}

