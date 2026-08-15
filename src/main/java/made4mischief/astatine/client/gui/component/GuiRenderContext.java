package made4mischief.astatine.client.gui.component;

import made4mischief.astatine.client.gui.component.theme.Theme;
import made4mischief.astatine.client.gui.component.theme.ThemeManager;
import made4mischief.astatine.client.utils.render.RenderUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;

@Environment(EnvType.CLIENT)
public final class GuiRenderContext {
   private DrawContext drawContext;
   private double mouseX;
   private double mouseY;
   private float frameDelta;

   public GuiRenderContext(DrawContext ct, double mouseX, double mouseY, float frameDelta){
      this.update(ct, mouseX, mouseY, frameDelta);
   }

   public void update(DrawContext ct, double mouseX, double mouseY, float frameDelta){
      this.drawContext = ct;
      this.mouseX = mouseX;
      this.mouseY = mouseY;
      this.frameDelta = frameDelta;
   }

   public double mouseX(){
      return this.mouseX;
   }

   public double mouseY(){
      return this.mouseY;
   }

   public float frameDelta(){
      return this.frameDelta;
   }

   public Theme theme(){
      return ThemeManager.active();
   }

   public void drawBo(float x, float y, float width, float height, float radius, int color){
      RenderUtil.drawBo(this.drawContext, x, y, width, height, radius, color);
   }

   public void drawText(String text, float x, float y, int color, boolean shadow){
      RenderUtil.drawText(this.drawContext, text, x, y, color, shadow);
   }

   public void drawText(String text, float x, float y, int color, boolean shadow, float scale){
      RenderUtil.drawText(this.drawContext, text, x, y, color, shadow, scale);
   }

   public void drawCenteredText(String text, float x, float y, int color, boolean shadow){
      RenderUtil.drawCenteredText(this.drawContext, text, x, y, color, shadow);
   }

   public void drawCenteredText(String text, float x, float y, int color, boolean shadow, float scale){
      RenderUtil.drawCenteredText(this.drawContext, text, x, y, color, shadow, scale);
   }

   public float textWidth(String text){
      return RenderUtil.getTextWidth(text);
   }

   public float textHeight(){
      return RenderUtil.getTextHeight();
   }

   public void pushMatrix(){
      this.drawContext.getMatrices().pushMatrix();
   }

   public void popMatrix(){
      this.drawContext.getMatrices().popMatrix();
   }

   public void translate(float x, float y){
      this.drawContext.getMatrices().translate(x, y);
   }

   public void scale(float x, float y){
      this.drawContext.getMatrices().scale(x, y);
   }

   public void enableScissor(int x, int y, int right, int bottom){
      this.drawContext.enableScissor(x, y, right, bottom);
   }

   public void disableScissor(){
      this.drawContext.disableScissor();
   }
}

