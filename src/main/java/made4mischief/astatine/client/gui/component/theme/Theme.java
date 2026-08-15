package made4mischief.astatine.client.gui.component.theme;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface Theme {
   String name();

   int background();

   int surface();

   int surfaceElevated();

   int border();

   int text();

   int textDim();

   int accent();

   int accentSecondary();

   int hover();

   int focus();

   float disabledOpacity();
}
