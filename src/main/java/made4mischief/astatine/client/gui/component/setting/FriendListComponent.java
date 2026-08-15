package made4mischief.astatine.client.gui.component.setting;

import java.util.List;
import made4mischief.astatine.client.gui.component.GuiRenderContext;
import made4mischief.astatine.client.gui.component.theme.Theme;
import made4mischief.astatine.client.setting.FriendListSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class FriendListComponent extends SettingComponent {
   private static final float ROW_HEIGHT = 12.0F;
   private static final int FRIEND_COLOR = -11141291;
   private final FriendListSetting setting;

   public FriendListComponent(FriendListSetting setting, float x, float y, float width){
      super(setting, x, y, width);
      this.setting = setting;
   }

   @Override
   public float getHeight(){
      return 18.0F + Math.max(1, this.setting.getFriends().size()) * 12.0F;
   }

   @Override
   public void render(GuiRenderContext context){
      if (this.isVisible()) {
         Theme theme = context.theme();
         context.drawText(this.getLabel().toUpperCase(), this.getX(), this.getY(), theme.textDim(), true, 0.85F);
         List<String> list = this.setting.getFriends();
         float y = this.getY() + 18.0F;
         if (list.isEmpty()) {
            context.drawText("NO FRIENDS", this.getX() + 4.0F, y, theme.textDim(), false, 0.8F);
         } else {
            for (String var6 : list) {
               context.drawText("- " + var6, this.getX() + 4.0F, y, -11141291, true, 0.8F);
               y += 12.0F;
            }
         }
      }
   }
}

