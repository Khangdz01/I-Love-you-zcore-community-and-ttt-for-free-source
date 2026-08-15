package made4mischief.astatine.client.modules.movement;

import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.ModeSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import made4mischief.astatine.client.utils.rotation.RotationManager;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public final class RotationModule extends Module {
   private static final Object ROTATION_KEY = new Object();
   private final ModeSetting modeSetting = this.addMode("Mode", "Server", new String[]{"Server", "Client"});
   private final ModeSetting serverModelSetting = this.addMode("Server Model", "Full", new String[]{"Head Only", "Full"});
   private final NumberSetting yaw = this.addNumber("Yaw", 0.0, -180.0, 180.0, 1.0);
   private final NumberSetting pitch = this.addNumber("Pitch", 0.0, -90.0, 90.0, 1.0);
   private final NumberSetting rotationSpeedSetting = this.addNumber("Rotation Speed", 10.0, -60.0, 60.0, 1.0);
   private final BooleanSetting movementFixSetting = this.addBoolean("Movement Fi", true);
   private long rotationTicks;

   public RotationModule(){
      super("Rotation", Category.MOVEMENT, "Xoay gÃ³c nhÃ¬n tháº­t hoáº·c giáº£ láº­p gÃ³c nhÃ¬n mÃ¡y chá»§.", -1, true);
      this.serverModelSetting.visibleWhen(() -> this.modeSetting.is("Server"));
      this.movementFixSetting.visibleWhen(() -> this.modeSetting.is("Server"));
   }

   @Override
   protected void onEnable(){
      this.rotationTicks = 0L;
   }

   @Override
   protected void onDisable(){
      RotationManager.clearRotatingState(ROTATION_KEY);
      this.rotationTicks = 0L;
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      if (client.player != null && client.world != null && !client.player.isDead()) {
         float value = MathHelper.wrapDegrees((float)(this.yaw.getValue() + this.rotationTicks * this.rotationSpeedSetting.getValue()));
         float valueFloat = this.pitch.getValueFloat();
         this.rotationTicks++;
         if (this.modeSetting.is("Server")) {
            RotationManager.setRotation(ROTATION_KEY, value, valueFloat, this.serverModelSetting.is("Full"), this.movementFixSetting.getValue());
         } else {
            RotationManager.clearRotatingState(ROTATION_KEY);
            client.player.setYaw(value);
            client.player.setHeadYaw(value);
            client.player.setBodyYaw(value);
            client.player.setPitch(valueFloat);
         }
      } else {
         RotationManager.clearRotatingState(ROTATION_KEY);
         this.rotationTicks = 0L;
      }
   }
}

