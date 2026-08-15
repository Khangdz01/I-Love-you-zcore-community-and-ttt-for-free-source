package made4mischief.astatine.client.gui.component;

import made4mischief.astatine.client.gui.component.event.CharTypeEvent;
import made4mischief.astatine.client.gui.component.event.KeyPressEvent;
import made4mischief.astatine.client.gui.component.event.MouseClickEvent;
import made4mischief.astatine.client.gui.component.event.MouseMoveEvent;
import made4mischief.astatine.client.gui.component.event.MouseReleaseEvent;
import made4mischief.astatine.client.gui.component.event.MouseScrollEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface Component {
   float getX();

   float getY();

   void setPosition(float var1, float var2);

   float getWidth();

   float getHeight();

   void setSize(float var1, float var2);

   boolean isWithin(double var1, double var3);

   boolean isVisible();

   void setVisible(boolean var1);

   boolean isEnabled();

   void setEnabled(boolean var1);

   boolean isHovered();

   boolean isFocused();

   void setFocused(boolean var1);

   boolean isFocusable();

   Component getParent();

   void setParent(Component var1);

   void render(GuiRenderContext var1);

   void tick();

   boolean mouseClicked(MouseClickEvent var1);

   boolean mouseReleased(MouseReleaseEvent var1);

   boolean mouseMoved(MouseMoveEvent var1);

   boolean mouseScrolled(MouseScrollEvent var1);

   boolean keyPressed(KeyPressEvent var1);

   boolean charTyped(CharTypeEvent var1);
}
