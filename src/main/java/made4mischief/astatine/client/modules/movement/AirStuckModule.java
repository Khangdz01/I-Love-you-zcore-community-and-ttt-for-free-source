package made4mischief.astatine.client.modules.movement;

import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.PacketEvent;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

@Environment(EnvType.CLIENT)
public final class AirStuckModule extends Module {
   private static AirStuckModule instance;
   private ClientPlayerEntity stuckPlayer;
   private Vec3d anchor;

   public AirStuckModule(){
      super("AirStuck", Category.MOVEMENT, "Đứng yên trên không nhưng vẫn xoay và tương tác.", -1, true);
      instance = this;
   }

   @Override
   protected void onEnable(){
      this.captureAnchor(MinecraftClient.getInstance().player);
   }

   @Override
   protected void onDisable(){
      this.releasePlayer(true);
   }

   @EventTarget
   public void onPacket(PacketEvent event){
      if (event.isSend() && this.stuckPlayer != null) {
         if (event.getPacket() instanceof PlayerInputC2SPacket) {
            event.cancel();
         } else {
            if (event.getPacket() instanceof PlayerMoveC2SPacket var2 && !var2.changesLook()) {
               event.cancel();
            }
         }
      }
   }

   @EventTarget
   public void onTick(TickEvent event){
      ClientPlayerEntity player = event.getClient().player;
      if (player != null && event.getClient().world != null && !player.isDead()) {
         if (this.stuckPlayer != player || this.anchor == null) {
            this.releasePlayer(false);
            this.captureAnchor(player);
         }

         player.setNoGravity(true);
         player.setVelocity(Vec3d.ZERO);
         player.setPosition(this.anchor);
      } else {
         this.releasePlayer(false);
      }
   }

   private void captureAnchor(ClientPlayerEntity player){
      if (player != null) {
         this.stuckPlayer = player;
         this.anchor = new Vec3d(player.getX(), player.getY(), player.getZ());
         player.setNoGravity(true);
         player.setVelocity(Vec3d.ZERO);
      }
   }

   private void releasePlayer(boolean startFalling){
      if (this.stuckPlayer != null) {
         this.stuckPlayer.setNoGravity(false);
         if (startFalling && !this.stuckPlayer.isOnGround()) {
            this.stuckPlayer.setVelocity(0.0, -0.08, 0.0);
         }
      }

      this.stuckPlayer = null;
      this.anchor = null;
   }

   public static boolean shouldFreeze(ClientPlayerEntity player){
      return instance != null && instance.isEnabled() && instance.stuckPlayer == player && instance.anchor != null;
   }
}
