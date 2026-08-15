package made4mischief.astatine.client.gui.component.theme;

import made4mischief.astatine.client.utils.render.core.ColorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class AccentTheme implements Theme {
   private final String name;
   private final int accent;
   private final int secondaryColor;
   private final Theme base = ThemeManager.DEFAULT;

   public AccentTheme(String name, int accent, int secondary){
      this.name = name;
      this.accent = accent;
      this.secondaryColor = secondary;
   }

   @Override
   public String name(){
      return this.name;
   }

   @Override
   public int background(){
      return this.base.background();
   }

   @Override
   public int surface(){
      return this.base.surface();
   }

   @Override
   public int surfaceElevated(){
      return this.base.surfaceElevated();
   }

   @Override
   public int border(){
      return ColorUtil.scaleAlpha(this.accent, 0.28F);
   }

   @Override
   public int text(){
      return ColorUtil.lerp(this.accent, -1, 0.62F);
   }

   @Override
   public int textDim(){
      return ColorUtil.withAlpha(this.text(), 191);
   }

   @Override
   public int accent(){
      return this.accent;
   }

   @Override
   public int accentSecondary(){
      return this.secondaryColor;
   }

   @Override
   public int hover(){
      return ColorUtil.scaleAlpha(this.accent, 0.14F);
   }

   @Override
   public int focus(){
      return ColorUtil.scaleAlpha(this.accent, 0.5F);
   }

   @Override
   public float disabledOpacity(){
      return this.base.disabledOpacity();
   }
}

