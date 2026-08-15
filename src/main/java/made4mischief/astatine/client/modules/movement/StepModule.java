package made4mischief.astatine.client.modules.movement;

import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;

@Environment(EnvType.CLIENT)
public final class StepModule extends Module {
   private static final Identifier STEP_HEIGHT_ID = Identifier.of("astatine", "step_height");
   private final NumberSetting height = this.addNumber("Height", 1.0, 0.6, 2.5, 0.1);
   private final BooleanSetting onlyGroundSetting = this.addBoolean("Only Ground", true);
   private final BooleanSetting pauseSneakSetting = this.addBoolean("Pause Sneak", true);
   private ClientPlayerEntity steppedPlayer;

   public StepModule(){
      super("Step", Category.MOVEMENT, "Tá»± bÆ°á»›c qua cÃ¡c khá»‘i cao.", -1, true);
   }

   @Override
   protected void onDisable(){
      this.removeStep();
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      ClientPlayerEntity player = client.player;
      if (player != null && client.world != null && !player.isDead()) {
         if (this.steppedPlayer != null && this.steppedPlayer != player) {
            this.removeStep();
         }

         boolean pressed = player.hasVehicle()
            || AirStuckModule.shouldFreeze(player)
            || this.onlyGroundSetting.getValue() && !player.isOnGround()
            || this.pauseSneakSetting.getValue() && client.options.sneakKey.isPressed();
         if (pressed) {
            this.removeStep();
         } else {
            this.applyStep(player);
         }
      } else {
         this.removeStep();
      }
   }

   private void applyStep(ClientPlayerEntity player){
      EntityAttributeInstance entityAttributeInstance = player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
      if (entityAttributeInstance == null) {
         this.removeStep();
      } else {
         entityAttributeInstance.removeModifier(STEP_HEIGHT_ID);
         double value = Math.max(0.0, this.height.getValue() - entityAttributeInstance.getValue());
         if (value > 1.0E-6) {
            entityAttributeInstance.addTemporaryModifier(new EntityAttributeModifier(STEP_HEIGHT_ID, value, Operation.ADD_VALUE));
         }

         this.steppedPlayer = player;
      }
   }

   private void removeStep(){
      if (this.steppedPlayer != null) {
         EntityAttributeInstance entityAttributeInstance = this.steppedPlayer.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
         if (entityAttributeInstance != null) {
            entityAttributeInstance.removeModifier(STEP_HEIGHT_ID);
         }
      }

      this.steppedPlayer = null;
   }
}

