package made4mischief.astatine.client.utils.render.core;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvent;

@Environment(EnvType.CLIENT)
public class SoundUtil {
   private static final SoundEvent SCROLL_SOUND = SoundEvent.of(Identifier.of("astatine", "scroll"));
   private static final SoundEvent CLICK_SOUND = SoundEvent.of(Identifier.of("astatine", "click"));
   private static final SoundEvent NOTIFICATION_SOUND = SoundEvent.of(Identifier.of("astatine", "notification"));

   public static void playHover(){
      play(SCROLL_SOUND, 1.0F, 1.0F);
   }

   public static void playClick(){
      play(CLICK_SOUND, 1.0F, 1.0F);
   }

   public static void playNotification(){
      play(NOTIFICATION_SOUND, 1.0F, 1.0F);
   }

   private static void play(SoundEvent event, float volume, float pitch){
      MinecraftClient client = MinecraftClient.getInstance();
      if (client != null && client.getSoundManager() != null) {
         client.getSoundManager().play(PositionedSoundInstance.ui(event, pitch, volume));
      }
   }
}
