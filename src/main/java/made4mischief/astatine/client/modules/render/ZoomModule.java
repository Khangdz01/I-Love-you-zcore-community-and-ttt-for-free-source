package made4mischief.astatine.client.modules.render;

import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.utils.render.animation.Animation;
import made4mischief.astatine.client.utils.render.animation.AnimationType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public final class ZoomModule extends Module {
   private static final float DEFAULT_ZOOM_FOV = 30.0F;
   private static final float MIN_ZOOM_FOV = 5.0F;
   private static final float MAX_ZOOM_FOV = 70.0F;
   private static final float SCROLL_STEP = 5.0F;
   private static final long ANIMATION_DURATION_MS = 420L;
   private static final long MIN_ANIMATION_DURATION_MS = 120L;
   private static ZoomModule instance;
   private final Animation fovTransitionAnimation = new Animation(0.0F, 1.0F, 420L, AnimationType.FAST_SETTLE);
   private final Animation zoomFovAnimation = new Animation(30.0F, 30.0F, 140L, AnimationType.EASE_OUT);
   private float targetZoomFov = 30.0F;

   public ZoomModule(){
      super("Zoom", Category.RENDER, "PhÃ³ng to mÆ°á»£t; cuá»™n chuá»™t Ä‘á»ƒ chá»‰nh má»©c zoom.");
      instance = this;
   }

   @Override
   protected void onEnable(){
      if (this.fovTransitionAnimation.get() <= 0.001F) {
         this.targetZoomFov = 30.0F;
         this.zoomFovAnimation.snapTo(30.0F);
      }

      this.animateFov(1.0F);
   }

   @Override
   protected void onDisable(){
      this.targetZoomFov = this.zoomFovAnimation.get();
      this.zoomFovAnimation.snapTo(this.targetZoomFov);
      this.animateFov(0.0F);
   }

   public static float applyFov(float vanillaFov){
      ZoomModule zoomModule = instance;
      MinecraftClient client = MinecraftClient.getInstance();
      if (zoomModule != null
         && client.player != null
         && client.world != null
         && client.currentScreen == null
         && (zoomModule.isEnabled() || !(zoomModule.fovTransitionAnimation.get() <= 0.001F))) {
         float get2 = Math.min(vanillaFov, zoomModule.zoomFovAnimation.get());
         float get = zoomModule.fovTransitionAnimation.get();
         return vanillaFov + (get2 - vanillaFov) * get;
      } else {
         return vanillaFov;
      }
   }

   public static boolean onMouseScrolled(double verticalScroll){
      ZoomModule zoomModule = instance;
      MinecraftClient client = MinecraftClient.getInstance();
      if (zoomModule != null && zoomModule.isEnabled() && verticalScroll != 0.0 && client.player != null && client.world != null && client.currentScreen == null) {
         zoomModule.targetZoomFov = clamp(zoomModule.targetZoomFov - (float)verticalScroll * 5.0F, 5.0F, 70.0F);
         zoomModule.zoomFovAnimation.setTarget(zoomModule.targetZoomFov);
         return true;
      } else {
         return false;
      }
   }

   private void animateFov(float target){
      float get = this.fovTransitionAnimation.get();
      float abs = Math.abs(target - get);
      if (abs <= 0.001F) {
         this.fovTransitionAnimation.snapTo(target);
      } else {
         long round = Math.max(120L, (long)Math.round(420.0F * (0.35F + 0.65F * abs)));
         this.fovTransitionAnimation.setDuration(round);
         this.fovTransitionAnimation.setTarget(target);
      }
   }

   private static float clamp(float value, float min, float max){
      return Math.max(min, Math.min(max, value));
   }
}

