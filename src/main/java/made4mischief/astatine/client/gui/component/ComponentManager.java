package made4mischief.astatine.client.gui.component;

import made4mischief.astatine.client.gui.component.event.CharTypeEvent;
import made4mischief.astatine.client.gui.component.event.KeyPressEvent;
import made4mischief.astatine.client.gui.component.event.MouseButton;
import made4mischief.astatine.client.gui.component.event.MouseClickEvent;
import made4mischief.astatine.client.gui.component.event.MouseMoveEvent;
import made4mischief.astatine.client.gui.component.event.MouseReleaseEvent;
import made4mischief.astatine.client.gui.component.event.MouseScrollEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

@Environment(EnvType.CLIENT)
public final class ComponentManager {
   private static final int KEY_V = 86;
   private static final int MODIFIER_CONTROL = 2;
   private static final int MODIFIER_SUPER = 8;
   private final Container root = new Container();
   private Component focused;
   private GuiRenderContext renderContext;

   public Container root(){
      return this.root;
   }

   public void resize(float width, float height){
      this.root.setPosition(0.0F, 0.0F);
      this.root.setSize(width, height);
   }

   public Component focused(){
      return this.focused;
   }

   public void focus(Component component){
      Component component2 = component != null && component.isFocusable() ? component : null;
      if (component2 != this.focused) {
         if (this.focused != null) {
            this.focused.setFocused(false);
         }

         this.focused = component2;
         if (this.focused != null) {
            this.focused.setFocused(true);
         }
      }
   }

   public void render(DrawContext ct, double mouseX, double mouseY, float frameDelta){
      if (this.renderContext == null) {
         this.renderContext = new GuiRenderContext(ct, mouseX, mouseY, frameDelta);
      } else {
         this.renderContext.update(ct, mouseX, mouseY, frameDelta);
      }

      this.root.render(this.renderContext);
   }

   public void tick(){
      this.root.tick();
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button){
      MouseClickEvent mouseClickEvent = new MouseClickEvent(mouseX, mouseY, MouseButton.fromCode(button));
      boolean mouseClicked = this.root.mouseClicked(mouseClickEvent);
      Object consumer = mouseClickEvent.consumer();
      if (consumer instanceof Component) {
         this.focus((Component)consumer);
      } else if (!mouseClicked) {
         this.focus(null);
      }

      return mouseClicked;
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button){
      MouseReleaseEvent mouseReleaseEvent = new MouseReleaseEvent(mouseX, mouseY, MouseButton.fromCode(button));
      return this.root.mouseReleased(mouseReleaseEvent);
   }

   public void mouseMoved(double mouseX, double mouseY){
      this.root.mouseMoved(new MouseMoveEvent(mouseX, mouseY));
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical){
      return this.root.mouseScrolled(new MouseScrollEvent(mouseX, mouseY, horizontal, vertical));
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers){
      if (this.focused == null) {
         return false;
      } else {
         String clipboard = "";
         if (keyCode == 86 && (modifiers & 10) != 0) {
            clipboard = MinecraftClient.getInstance().keyboard.getClipboard();
         }

         return this.focused.keyPressed(new KeyPressEvent(keyCode, scanCode, modifiers, clipboard));
      }
   }

   public boolean charTyped(char character, int modifiers){
      return this.focused == null ? false : this.focused.charTyped(new CharTypeEvent(character, modifiers));
   }
}

