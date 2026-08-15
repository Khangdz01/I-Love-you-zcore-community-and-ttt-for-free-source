package made4mischief.astatine.client.gui.component.theme;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ThemeManager {
   public static final Theme DEFAULT = new DefaultTheme();
   private static final CustomAccentTheme customTheme = new CustomAccentTheme("Custom", -5084161, -13244417);
   private static final Theme[] themes = new Theme[]{
      DEFAULT,
      new AccentTheme("Red", -45747, -4907214),
      new AccentTheme("Purple", -5084161, -10079301),
      new AccentTheme("Green", -12714064, -16737946),
      new AccentTheme("Gold", -12452, -4687853),
      customTheme
   };
   private static Theme active = DEFAULT;

   private ThemeManager(){
   }

   public static Theme active(){
      return active;
   }

   public static void set(Theme theme){
      active = theme == null ? DEFAULT : theme;
   }

   public static Theme[] available(){
      return (Theme[])themes.clone();
   }

   public static CustomAccentTheme custom(){
      return customTheme;
   }

   public static void setCustomColors(int accent, int secondary){
      customTheme.setColors(accent, secondary);
   }

   public static boolean setByName(String name){
      if (name == null) {
         return false;
      } else {
         for (Theme theme : themes) {
            if (theme.name().equalsIgnoreCase(name)) {
               set(theme);
               return true;
            }
         }

         return false;
      }
   }
}
