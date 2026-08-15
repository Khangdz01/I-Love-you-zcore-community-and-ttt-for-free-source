package made4mischief.astatine.client.modules.render;

import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.utils.render.animation.Animation;
import made4mischief.astatine.client.utils.render.animation.AnimationType;
import made4mischief.astatine.client.utils.rotation.RotationManager;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.network.ClientPlayerEntity;

@Environment(EnvType.CLIENT)
public final class FreeLookModule extends Module {
   private static final long ANIMATION_DURATION_MS = 480L;
   private static final float CAMERA_DISTANCE_CAP = 0.55F;
   private static FreeLookModule instance;
   private final Animation cameraDistanceAnimation = new Animation(0.0F, 1.0F, 480L, AnimationType.EASE_OUT);
   private Perspective previousPerspective;
   private ClientPlayerEntity cameraEntity;
   private float cameraYaw;
   private float cameraPitch;

   public FreeLookModule(){
      super("FreeLook", Category.RENDER, "Xoay camera góc ba mà không xoay người chơi.");
      instance = this;
   }

   @Override
   protected void onEnable(){
      MinecraftClient client = MinecraftClient.getInstance();
      this.previousPerspective = client.options.getPerspective();
      client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
      this.setCameraEntity(client.player);
      this.restartCameraTimer();
   }

   @Override
   protected void onDisable(){
      MinecraftClient client = MinecraftClient.getInstance();
      if (this.previousPerspective != null) {
         client.options.setPerspective(this.previousPerspective);
      }

      this.previousPerspective = null;
      this.cameraEntity = null;
      this.cameraDistanceAnimation.reset();
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
      if (client.player != this.cameraEntity) {
         this.setCameraEntity(client.player);
         this.restartCameraTimer();
      }
   }

   private void setCameraEntity(ClientPlayerEntity player){
      this.cameraEntity = player;
      if (player != null) {
         this.cameraYaw = player.getYaw();
         this.cameraPitch = player.getPitch();
      }
   }

   private void restartCameraTimer(){
      this.cameraDistanceAnimation.reset();
      this.cameraDistanceAnimation.start();
   }

   public static boolean handleLookDirection(Entity entity, double cursorDeltaX, double cursorDeltaY){
      FreeLookModule freeLookModule = instance;
      if (!isCameraEntity(freeLookModule, entity)) {
         return false;
      } else {
         freeLookModule.cameraYaw = RotationManager.normalizeYaw(freeLookModule.cameraYaw + (float)cursorDeltaX * 0.15F);
         freeLookModule.cameraPitch = RotationManager.clampPitch(freeLookModule.cameraPitch + (float)cursorDeltaY * 0.15F);
         return true;
      }
   }

   public static float cameraYaw(Entity entity, float vanillaYaw){
      FreeLookModule freeLookModule = instance;
      return isCameraEntity(freeLookModule, entity) ? freeLookModule.cameraYaw : vanillaYaw;
   }

   public static float cameraPitch(Entity entity, float vanillaPitch){
      FreeLookModule freeLookModule = instance;
      return isCameraEntity(freeLookModule, entity) ? freeLookModule.cameraPitch : vanillaPitch;
   }

   public static float cameraDistance(float vanillaDistance){
      FreeLookModule freeLookModule = instance;
      MinecraftClient client = MinecraftClient.getInstance();
      if (freeLookModule != null && freeLookModule.isEnabled() && freeLookModule.cameraEntity != null && client.getCameraEntity() == freeLookModule.cameraEntity) {
         float min = Math.min(0.55F, vanillaDistance);
         return min + (vanillaDistance - min) * freeLookModule.cameraDistanceAnimation.get();
      } else {
         return vanillaDistance;
      }
   }

   private static boolean isCameraEntity(FreeLookModule module, Entity entity){
      return module != null && module.isEnabled() && module.cameraEntity != null && entity == module.cameraEntity;
   }
}
