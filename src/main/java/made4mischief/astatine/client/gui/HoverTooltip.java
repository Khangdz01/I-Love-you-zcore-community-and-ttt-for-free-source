package made4mischief.astatine.client.gui;

import java.util.ArrayList;
import java.util.List;
import made4mischief.astatine.client.gui.component.theme.Theme;
import made4mischief.astatine.client.utils.render.RenderUtil;
import made4mischief.astatine.client.utils.render.core.ColorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;

@Environment(EnvType.CLIENT)
public final class HoverTooltip {
   private static final float TEXT_SCALE = 0.38F;
   private static final float TEXT_X_INSET = 5.0F;
   private static final float TEXT_START_Y = 4.0F;
   private static final float LINE_GAP = 1.0F;
   private static final float CURSOR_Y_OFFSET = 7.0F;
   private static final float MAX_WIDTH = 240.0F;
   private static final float OPACITY_LERP_SPEED = 18.0F;
   private static final float POSITION_LERP_SPEED = 28.0F;
   private final List<String> lines = new ArrayList<>();
   private String text = "";
   private float opacity;
   private float animatedX;
   private float animatedY;
   private long lastFrameNanos;

   public void render(DrawContext context, String text, double mouseX, double mouseY, int screenWidth, int screenHeight, Theme theme){
      float deltaSeconds = this.getDeltaSeconds();
      boolean blank = text != null && !text.isBlank();
      boolean var12 = this.opacity < 0.01F;
      if (blank && !text.equals(this.text)) {
         this.text = text;
         this.wrapText(text, Math.min(240.0F, screenWidth - 36.0F));
      }

      float var13 = blank ? 1.0F : 0.0F;
      this.opacity = lerpExponential(this.opacity, var13, 18.0F, deltaSeconds);
      if (!(this.opacity < 0.01F) && !this.lines.isEmpty()) {
         float maxLineWidth = this.getMaxLineWidth();
         float textHeight = RenderUtil.getTextHeight(0.38F);
         float size = this.lines.size() * textHeight + Math.max(0, this.lines.size() - 1) * 1.0F;
         float var17 = maxLineWidth + 10.0F;
         float var18 = size + 8.0F;
         float var19 = clamp((float)mouseX - var17 / 2.0F, 6.0F, screenWidth - var17 - 6.0F);
         float var20 = clamp((float)mouseY - var18 - 7.0F, 6.0F, screenHeight - var18 - 6.0F);
         if (var12 && blank) {
            this.animatedX = var19;
            this.animatedY = var20;
         } else {
            this.animatedX = lerpExponential(this.animatedX, var19, 28.0F, deltaSeconds);
            this.animatedY = lerpExponential(this.animatedY, var20, 28.0F, deltaSeconds);
         }

         float var21 = 1.0F - (1.0F - this.opacity) * (1.0F - this.opacity);
         float var22 = 0.92F + var21 * 0.08F;
         float var23 = this.animatedX + var17 / 2.0F;
         float var24 = this.animatedY + var18 / 2.0F;
         context.getMatrices().pushMatrix();
         context.getMatrices().translate(var23, var24 + (1.0F - var21) * 3.0F);
         context.getMatrices().scale(var22, var22);
         context.getMatrices().translate(-var17 / 2.0F, -var18 / 2.0F);
         int accent = ColorUtil.scaleAlpha(theme.accent(), 0.65F * var21);
         int surfaceElevated = ColorUtil.scaleAlpha(theme.surfaceElevated(), 0.97F * var21);
         int text2 = ColorUtil.scaleAlpha(theme.text(), var21);
         RenderUtil.drawBo(context, -1.0F, -1.0F, var17 + 2.0F, var18 + 2.0F, 7.0F, accent);
         RenderUtil.drawBo(context, 0.0F, 0.0F, var17, var18, 6.0F, surfaceElevated);
         float var28 = 4.0F;

         for (String var30 : this.lines) {
            RenderUtil.drawText(context, var30, 5.0F, var28, text2, true, 0.38F);
            var28 += textHeight + 1.0F;
         }

         context.getMatrices().popMatrix();
      }
   }

   private void wrapText(String text, float maximumWidth){
      this.lines.clear();

      for (String var6 : text.split("\\n", -1)) {
         this.wrapParagraph(var6, maximumWidth);
      }
   }

   private void wrapParagraph(String paragraph, float maximumWidth){
      if (paragraph.isBlank()) {
         this.lines.add("");
      } else {
         StringBuilder builder = new StringBuilder();

         for (String var7 : paragraph.trim().split("\\s+")) {
            String empty = builder.isEmpty() ? var7 : builder + " " + var7;
            if (!builder.isEmpty() && RenderUtil.getTextWidth(empty, 0.38F) > maximumWidth) {
               this.lines.add(builder.toString());
               builder.setLength(0);
            }

            if (!builder.isEmpty()) {
               builder.append(' ');
            }

            builder.append(var7);
         }

         if (!builder.isEmpty()) {
            this.lines.add(builder.toString());
         }
      }
   }

   private float getMaxLineWidth(){
      float textWidth = 0.0F;

      for (String var3 : this.lines) {
         textWidth = Math.max(textWidth, RenderUtil.getTextWidth(var3, 0.38F));
      }

      return textWidth;
   }

   private float getDeltaSeconds(){
      long nanoTime = System.nanoTime();
      if (this.lastFrameNanos == 0L) {
         this.lastFrameNanos = nanoTime;
         return 0.016666668F;
      } else {
         float var3 = (float)(nanoTime - this.lastFrameNanos) / 1.0E9F;
         this.lastFrameNanos = nanoTime;
         return Math.min(var3, 0.05F);
      }
   }

   private static float lerpExponential(float current, float target, float speed, float deltaSeconds){
      float exp = 1.0F - (float)Math.exp(-speed * deltaSeconds);
      return current + (target - current) * exp;
   }

   private static float clamp(float value, float minimum, float maximum){
      return maximum < minimum ? minimum : Math.max(minimum, Math.min(maximum, value));
   }
}

