package made4mischief.astatine.client.utils.camera;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.network.OtherClientPlayerEntity;

@Environment(EnvType.CLIENT)
public final class DetachedCameraEntity extends OtherClientPlayerEntity {
   public DetachedCameraEntity(ClientWorld world, GameProfile profile){
      super(world, profile);
      this.noClip = true;
      this.setNoGravity(true);
   }

   public boolean method_5810(){
      return false;
   }

   public boolean method_30949(Entity other){
      return false;
   }

   public boolean method_30948(Entity other){
      return false;
   }

   public boolean method_5863(){
      return false;
   }

   public void method_5697(Entity other){
   }

   protected void method_6087(Entity other){
   }
}
