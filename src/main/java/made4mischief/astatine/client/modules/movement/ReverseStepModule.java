package made4mischief.astatine.client.modules.movement;

import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.NumberSetting;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

@Environment(EnvType.CLIENT)
public final class ReverseStepModule extends Module {
   private static final double EPSILON = 1.0E-4;
   private final NumberSetting fallSpeedSetting = this.addNumber("Fall Speed", 1.5, 0.25, 5.0, 0.05);
   private final NumberSetting distance = this.addNumber("Distance", 2.25, 0.5, 5.0, 0.25);
   private boolean wasAirborne;

   public ReverseStepModule(){
      super("ReverseStep", Category.MOVEMENT, "KÃ©o ngÆ°á»i chÆ¡i xuá»‘ng Ä‘áº¥t nhanh hÆ¡n.", -1, true);
   }

   @Override
   protected void onEnable(){
      MinecraftClient client = MinecraftClient.getInstance();
      this.wasAirborne = client.player != null && !client.player.isOnGround();
   }

   @Override
   protected void onDisable(){
      this.wasAirborne = false;
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      ClientPlayerEntity player = client.player;
      this.updateSteppingState(client, player);
      if (this.canReverseStep(client, player) && this.isGapBelow(client, player)) {
         Vec3d vec = player.getVelocity();
         player.setVelocity(vec.x, Math.min(vec.y, -this.fallSpeedSetting.getValue()), vec.z);
      }
   }

   private void updateSteppingState(MinecraftClient client, ClientPlayerEntity player){
      if (player == null) {
         this.wasAirborne = false;
      } else {
         if (player.isOnGround()) {
            this.wasAirborne = client.options.jumpKey.isPressed();
         } else if (player.getVelocity().y > 0.001) {
            this.wasAirborne = true;
         }
      }
   }

   private boolean canReverseStep(MinecraftClient client, ClientPlayerEntity player){
      return player != null
         && client.world != null
         && !player.isDead()
         && !player.isOnGround()
         && !this.wasAirborne
         && player.getVelocity().y <= 0.0
         && !player.hasVehicle()
         && !player.getAbilities().flying
         && !player.isGliding()
         && !player.isClimbing()
         && !player.isTouchingWater()
         && !player.isInLava()
         && !AirStuckModule.shouldFreeze(player);
   }

   private boolean isGapBelow(MinecraftClient client, ClientPlayerEntity player){
      Box box3 = player.getBoundingBox();
      double var4 = box3.minY;
      double value = this.distance.getValue();
      Box box2 = new Box(box3.minX, var4 - value, box3.minZ, box3.maxX, var4 - 1.0E-4, box3.maxZ);

      for (VoxelShape voxelShape : client.world.getBlockCollisions(player, box2)) {
         for (Box Box : voxelShape.getBoundingBoxes()) {
            double var13 = var4 - Box.maxY;
            if (var13 >= 0.0 && var13 <= value + 1.0E-4) {
               return true;
            }
         }
      }

      return false;
   }
}

