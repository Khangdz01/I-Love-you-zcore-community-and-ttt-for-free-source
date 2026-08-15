package made4mischief.astatine.client.utils.render.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Animation {
   private float current;
   private float target;
   private long durationMs;
   private float progress;
   private int direction = 1;
   private boolean running;
   private AnimationType type;

   public Animation(float origin, float target, long durationMs, AnimationType type){
      this.current = origin;
      this.target = target;
      this.durationMs = Math.max(1L, durationMs);
      this.type = type;
      AnimationManager.register(this);
   }

   void tick(float frameDeltaMs){
      if (this.running) {
         float var2 = frameDeltaMs / (float)this.durationMs;
         this.progress = this.progress + var2 * this.direction;
         if (this.progress >= 1.0F) {
            this.progress = 1.0F;
            this.running = false;
         } else if (this.progress <= 0.0F) {
            this.progress = 0.0F;
            this.running = false;
         }
      }
   }

   public void start(){
      this.running = true;
   }

   public void reset(){
      this.progress = 0.0F;
      this.direction = 1;
      this.running = false;
   }

   public void reverse(){
      this.direction = -this.direction;
      this.running = true;
   }

   public boolean isFinished(){
      return this.direction > 0 ? this.progress >= 1.0F : this.progress <= 0.0F;
   }

   public float get(){
      return this.current + (this.target - this.current) * this.type.ease(clamp01(this.progress));
   }

   public float getProgress(){
      return this.progress;
   }

   public void setTarget(float newTarget){
      this.current = this.get();
      this.target = newTarget;
      this.progress = 0.0F;
      this.direction = 1;
      this.running = true;
   }

   public void snapTo(float value){
      this.current = value;
      this.target = value;
      this.progress = 0.0F;
      this.direction = 1;
      this.running = false;
   }

   public void setDuration(long durationMs){
      this.durationMs = Math.max(1L, durationMs);
   }

   public void setType(AnimationType type){
      this.type = type;
   }

   private static float clamp01(float v){
      return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
   }
}

