package made4mischief.astatine.client.modules.combat;

import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.ModeSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import made4mischief.astatine.client.utils.combat.TargetUtil;
import made4mischief.astatine.client.utils.rotation.RotationManager;
import made4mischief.astatine.client.utils.rotation.RotationUtil;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public class AimModule extends Module {
   private final NumberSetting rangeSetting = this.addNumber("Range", 5.0, 1.0, 8.0, 0.5);
   private final ModeSetting aimModeSetting = this.addMode("AimMode", "Body", new String[]{"Head", "Body"});

   public AimModule(){
      super("AimAssist", Category.COMBAT, "Tự ngắm vào mục tiêu gần nhất.", -1);
   }

   @Override
   protected void onDisable(){
      RotationManager.clearRotatingState();
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      if (client.player != null && !client.player.isDead()) {
         PlayerEntity player = TargetUtil.getClosestTarget(client, this.rangeSetting.getValueInt());
         if (player == null) {
            RotationManager.clearRotatingState();
         } else {
            Vec3d vec2 = client.player.getEyePos();
            Vec3d vec;
            if (this.aimModeSetting.is("Body")) {
               vec = player.getEyePos().add(0.0, -player.getHeight() * 0.25, 0.0);
            } else {
               vec = player.getEyePos();
            }

            float yaw = RotationUtil.getYaw(vec2, vec);
            float pitch = RotationUtil.getPitch(vec2, vec);
            client.player.setYaw(yaw);
            client.player.setPitch(pitch);
         }
      } else {
         RotationManager.clearRotatingState();
      }
   }
}
