package made4mischief.astatine.client.gui.component.setting;

import made4mischief.astatine.client.gui.component.GuiRenderContext;
import made4mischief.astatine.client.gui.component.event.MouseButton;
import made4mischief.astatine.client.gui.component.event.MouseClickEvent;
import made4mischief.astatine.client.gui.component.theme.Theme;
import made4mischief.astatine.client.screen.ItemTargetSelectorScreen;
import made4mischief.astatine.client.setting.ItemTargetSetting;
import made4mischief.astatine.client.utils.render.core.ColorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

@Environment(EnvType.CLIENT)
public final class ItemTargetComponent extends SettingComponent {
   private static final float BUTTON_HEIGHT = 15.0F;
   private static final float BUTTON_WIDTH = 70.0F;
   private final ItemTargetSetting setting;

   public ItemTargetComponent(ItemTargetSetting setting, float x, float y, float width){
      super(setting, x, y, width);
      this.setting = setting;
   }

   @Override
   public void render(GuiRenderContext context){
      if (this.isVisible()) {
         Theme theme = context.theme();
         float width = this.getX() + this.getWidth() - 70.0F;
         float height = this.getY() + (this.getHeight() - 15.0F) / 2.0F;
         boolean mouseY = this.isInButtonArea(context.mouseX(), context.mouseY());
         context.drawText(
            this.getLabel().toUpperCase(), this.getX(), this.getY() + (this.getHeight() - context.textHeight() * 0.85F) / 2.0F, theme.textDim(), true, 0.85F
         );
         int accent = ColorUtil.scaleAlpha(theme.accent(), mouseY ? 0.78F : 0.35F);
         int surface = mouseY ? ColorUtil.lerp(theme.surfaceElevated(), theme.accentSecondary(), 0.14F) : ColorUtil.scaleAlpha(theme.surface(), 0.92F);
         context.drawBo(width - 1.0F, height - 1.0F, 72.0F, 17.0F, 8.0F, accent);
         context.drawBo(width, height, 70.0F, 15.0F, 7.0F, surface);
         String selectedCount = this.setting.getSelectedCount() + " selected";
         float textWidth = context.textWidth(selectedCount) * 0.8F;
         context.drawText(
            selectedCount,
            width + (70.0F - textWidth) / 2.0F,
            this.getY() + (this.getHeight() - context.textHeight() * 0.8F) / 2.0F,
            mouseY ? theme.accent() : theme.text(),
            true,
            0.8F
         );
      }
   }

   @Override
   protected boolean onMouseClicked(MouseClickEvent event){
      if (event.button() == MouseButton.LEFT && this.isInButtonArea(event.x(), event.y())) {
         MinecraftClient client = MinecraftClient.getInstance();
         Screen screen = client.currentScreen;
         client.setScreen(new ItemTargetSelectorScreen(screen, this.setting));
         event.consume(this);
         return true;
      } else {
         return false;
      }
   }

   private boolean isInButtonArea(double mouseX, double mouseY){
      float width = this.getX() + this.getWidth() - 70.0F;
      float height = this.getY() + (this.getHeight() - 15.0F) / 2.0F;
      return mouseX >= width && mouseX <= width + 70.0F && mouseY >= height && mouseY <= height + 15.0F;
   }
}

