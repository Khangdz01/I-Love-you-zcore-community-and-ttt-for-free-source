package made4mischief.astatine.client.modules.combat;

import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class AnchorMacroModule extends AbstractAnchorSequenceModule {
   public AnchorMacroModule(){
      super("AnchorMacro", "Liên tục đặt, nạp và kích nổ neo hồi sinh.", -1, false);
   }

   @EventTarget
   public void onTick(TickEvent event){
      this.tickSequence(event);
   }
}
