package made4mischief.astatine.client.modules.render;

import made4mischief.astatine.client.mixin.InputAccessor;
import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import made4mischief.astatine.client.utils.camera.DetachedCameraEntity;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity.RemovalReason;

@Environment(EnvType.CLIENT)
public final class FreeCamModule extends Module {
   private static FreeCamModule instance;
   private final NumberSetting speedSetting = this.addNumber("Speed", 1.0, 0.1, 5.0, 0.1);
   private final BooleanSetting mineBelowSetting = this.addBoolean("Mine Below", true);
   private DetachedCameraEntity cameraEntity;
   private BlockPos breakTargetPos;
   private float lastYaw;
   private float lastPitch;
   private int breakSwingTicks;

   public FreeCamModule(){
      super("FreeCam", Category.RENDER, "TÃ¡ch camera Ä‘á»ƒ bay quan sÃ¡t tá»± do.", -1);
      instance = this;
   }

   @Override
   protected void onEnable(){
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null && client.world != null) {
         this.cameraEntity = new DetachedCameraEntity(client.world, client.player.getGameProfile());
         this.cameraEntity.copyPositionAndRotation(client.player);
         this.cameraEntity.setHeadYaw(client.player.getHeadYaw());
         client.world.addEntity(this.cameraEntity);
         this.lastYaw = this.cameraEntity.getYaw();
         this.lastPitch = this.cameraEntity.getPitch();
         this.breakTargetPos = null;
         this.breakSwingTicks = 0;
         client.setCameraEntity(this.cameraEntity);
         client.worldRenderer.scheduleTerrainUpdate();
      } else {
         this.disable();
      }
   }

   @Override
   protected void onDisable(){
      MinecraftClient client = MinecraftClient.getInstance();
      this.tickInputHandling(client);
      if (client.player != null) {
         client.setCameraEntity(client.player);
      }

      client.worldRenderer.scheduleTerrainUpdate();
      if (this.cameraEntity != null && client.world != null) {
         client.world.removeEntity(this.cameraEntity.getId(), RemovalReason.DISCARDED);
         this.cameraEntity = null;
      }

      this.breakTargetPos = null;
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      if (client.player != null && this.cameraEntity != null) {
         GameOptions gameOptions = client.options;
         double value = this.speedSetting.getValue();
         Vec3d vec3 = Vec3d.fromPolar(0.0F, this.cameraEntity.getYaw());
         Vec3d vec2 = Vec3d.fromPolar(0.0F, this.cameraEntity.getYaw() + 90.0F);
         Vec3d vec = Vec3d.ZERO;
         if (gameOptions.forwardKey.isPressed()) {
            vec = vec.add(vec3);
         }

         if (gameOptions.backKey.isPressed()) {
            vec = vec.subtract(vec3);
         }

         if (gameOptions.rightKey.isPressed()) {
            vec = vec.add(vec2);
         }

         if (gameOptions.leftKey.isPressed()) {
            vec = vec.subtract(vec2);
         }

         if (gameOptions.jumpKey.isPressed()) {
            vec = vec.add(0.0, 1.0, 0.0);
         }

         if (gameOptions.sneakKey.isPressed()) {
            vec = vec.subtract(0.0, 1.0, 0.0);
         }

         if (vec.lengthSquared() > 0.0) {
            vec = vec.normalize().multiply(value);
         }

         this.cameraEntity.setPosition(this.cameraEntity.getEntityPos().add(vec));
         this.cameraEntity.setHeadYaw(this.cameraEntity.getYaw());
         this.tickFreecam(client);
      }
   }

   public static boolean suppressPlayerMovement(ClientPlayerEntity player){
      FreeCamModule freeCamModule = instance;
      MinecraftClient client = MinecraftClient.getInstance();
      if (freeCamModule != null && freeCamModule.isEnabled() && player != null && player == client.player) {
         player.input.playerInput = PlayerInput.DEFAULT;
         ((InputAccessor)player.input).astatine$setMovementVector(Vec2f.ZERO);
         return true;
      } else {
         return false;
      }
   }

   public static boolean shouldSimulateLocalPlayer(ClientPlayerEntity player){
      MinecraftClient client = MinecraftClient.getInstance();
      return isFreeCamActive() && player != null && player == client.player;
   }

   public static boolean handleLookDirection(Entity entity, double cursorDeltaX, double cursorDeltaY){
      FreeCamModule freeCamModule = instance;
      MinecraftClient client = MinecraftClient.getInstance();
      if (freeCamModule != null && freeCamModule.isEnabled() && freeCamModule.cameraEntity != null && entity == client.player && client.getCameraEntity() == freeCamModule.cameraEntity) {
         freeCamModule.cameraEntity.changeLookDirection(cursorDeltaX, cursorDeltaY);
         freeCamModule.cameraEntity.setHeadYaw(freeCamModule.cameraEntity.getYaw());
         return true;
      } else {
         return false;
      }
   }

   public static boolean shouldRenderLocalBody(){
      FreeCamModule freeCamModule = instance;
      MinecraftClient client = MinecraftClient.getInstance();
      return freeCamModule != null && freeCamModule.isEnabled() && freeCamModule.cameraEntity != null && client.player != null && client.getCameraEntity() == freeCamModule.cameraEntity;
   }

   public static boolean isActive(){
      return isFreeCamActive();
   }

   public static boolean handleAttackInput(){
      return isFreeCamActive();
   }

   public static float handPitchSway(float vanillaDegrees){
      FreeCamModule freeCamModule = instance;
      if (isFreeCamActive() && freeCamModule.cameraEntity != null) {
         float pitch = freeCamModule.cameraEntity.getPitch() - freeCamModule.lastPitch;
         freeCamModule.lastPitch += pitch * 0.5F;
         return MathHelper.clamp(pitch * 0.1F, -10.0F, 10.0F);
      } else {
         return vanillaDegrees;
      }
   }

   public static float handYawSway(float vanillaDegrees){
      FreeCamModule freeCamModule = instance;
      if (isFreeCamActive() && freeCamModule.cameraEntity != null) {
         float yaw = MathHelper.wrapDegrees(freeCamModule.cameraEntity.getYaw() - freeCamModule.lastYaw);
         freeCamModule.lastYaw = MathHelper.wrapDegrees(freeCamModule.lastYaw + yaw * 0.5F);
         return MathHelper.clamp(yaw * 0.1F, -10.0F, 10.0F);
      } else {
         return vanillaDegrees;
      }
   }

   private void tickFreecam(MinecraftClient client){
      boolean pressed = this.mineBelowSetting.getValue()
         && client.currentScreen == null
         && client.options.attackKey.isPressed()
         && client.interactionManager != null
         && client.world != null;
      if (!pressed) {
         this.tickInputHandling(client);
      } else {
         BlockPos pos = BlockPos.ofFloored(
            client.player.getX(), client.player.getBoundingBox().minY - 0.01, client.player.getZ()
         );
         BlockState state = client.world.getBlockState(pos);
         if (state.isAir() || state.getHardness(client.world, pos) < 0.0F || !client.player.canInteractWithBlockAt(pos, 1.0)) {
            this.tickInputHandling(client);
         } else if (!pos.equals(this.breakTargetPos)) {
            this.tickInputHandling(client);
            this.breakTargetPos = pos.toImmutable();
            client.interactionManager.attackBlock(this.breakTargetPos, Direction.UP);
            this.releaseSpectatorAttack(client);
            this.breakSwingTicks = 0;
         } else {
            client.interactionManager.updateBlockBreakingProgress(this.breakTargetPos, Direction.UP);
            if (this.breakSwingTicks-- <= 0) {
               this.releaseSpectatorAttack(client);
               this.breakSwingTicks = 4;
            }
         }
      }
   }

   private void releaseSpectatorAttack(MinecraftClient client){
      client.player.swingHand(Hand.MAIN_HAND);
      if (this.cameraEntity != null) {
         this.cameraEntity.swingHand(Hand.MAIN_HAND);
      }
   }

   private void tickInputHandling(MinecraftClient client){
      if (this.breakTargetPos != null && client.interactionManager != null) {
         client.interactionManager.cancelBlockBreaking();
      }

      this.breakTargetPos = null;
      this.breakSwingTicks = 0;
   }

   private static boolean isFreeCamActive(){
      FreeCamModule freeCamModule = instance;
      MinecraftClient client = MinecraftClient.getInstance();
      return freeCamModule != null && freeCamModule.isEnabled() && freeCamModule.cameraEntity != null && client.getCameraEntity() == freeCamModule.cameraEntity;
   }
}

