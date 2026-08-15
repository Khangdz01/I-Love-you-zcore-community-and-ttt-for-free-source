package made4mischief.astatine.client.gui.component.theme;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class DefaultTheme implements Theme {
   @Override
   public String name(){
      return "Aqua";
   }

   @Override
   public int background(){
      return -16446438;
   }

   @Override
   public int surface(){
      return -536868322;
   }

   @Override
   public int surfaceElevated(){
      return -352315071;
   }

   @Override
   public int border(){
      return 1191218431;
   }

   @Override
   public int text(){
      return -6499073;
   }

   @Override
   public int textDim(){
      return -1079325185;
   }

   @Override
   public int accent(){
      return -16722689;
   }

   @Override
   public int accentSecondary(){
      return -16760628;
   }

   @Override
   public int hover(){
      return 604000456;
   }

   @Override
   public int focus(){
      return -2147429121;
   }

   @Override
   public float disabledOpacity(){
      return 0.4F;
   }
}
