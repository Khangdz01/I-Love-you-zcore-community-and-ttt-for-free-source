package made4mischief.astatine.client.utils.render;

import made4mischief.astatine.client.utils.render.renderer.ItemStackRenderer;
import made4mischief.astatine.client.utils.render.renderer.LineRenderer;
import made4mischief.astatine.client.utils.render.renderer.PlayerFaceRenderer;
import made4mischief.astatine.client.utils.render.renderer.RoundedRectRenderer;
import made4mischief.astatine.client.utils.render.renderer.WorldBoxRenderer;
import made4mischief.astatine.client.utils.render.renderer.WorldRingRenderer;
import made4mischief.astatine.client.utils.render.renderer.text.TextRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.SkinTextures;

@Environment(EnvType.CLIENT)
public final class RenderUtil {
   private RenderUtil(){
   }

   public static void drawBo(DrawContext ct, float x, float y, float width, float height, float radius, int color){
      RoundedRectRenderer.render(ct, x, y, width, height, radius, color);
   }

   public static void drawBo(DrawContext ct, int x, int y, int width, int height, int radius, int color){
      RoundedRectRenderer.render(ct, x, y, width, height, radius, color);
   }

   public static void drawCircle(DrawContext ct, float centerX, float centerY, float radius, int color){
      RoundedRectRenderer.render(ct, centerX - radius, centerY - radius, radius * 2.0F, radius * 2.0F, radius, color);
   }

   public static void drawLine(DrawContext ct, float x1, float y1, float x2, float y2, float thickness, int color){
      LineRenderer.render(ct, x1, y1, x2, y2, thickness, color);
   }

   public static void drawWorldRing(
      WorldRenderContext context, double worldX, double worldY, double worldZ, float radius, float thickness, float glow, int rgb, float opacity
   ){
      WorldRingRenderer.render(context, worldX, worldY, worldZ, radius, thickness, glow, rgb, opacity);
   }

   public static void drawWorldBo(
      WorldRenderContext context,
      double minX,
      double minY,
      double minZ,
      double maxX,
      double maxY,
      double maxZ,
      int fillColor,
      int outlineColor,
      boolean fill,
      boolean outline,
      boolean throughWalls,
      float lineWidth
   ){
      WorldBoxRenderer.render(context, minX, minY, minZ, maxX, maxY, maxZ, fillColor, outlineColor, fill, outline, throughWalls, lineWidth);
   }

   public static void drawPlayerFace(DrawContext ct, SkinTextures skin, int x, int y, int size, int color){
      PlayerFaceRenderer.render(ct, skin, x, y, size, color);
   }

   public static void drawItemStack(DrawContext ct, LivingEntity entity, ItemStack stack, int x, int y, int seed){
      ItemStackRenderer.render(ct, entity, stack, x, y, seed);
   }

   public static void drawText(DrawContext ct, String text, float x, float y, int color, boolean shadow, float scale){
      TextRenderer.drawLeft(ct, text, x, y, color, shadow, scale);
   }

   public static void drawText(DrawContext ct, String text, float x, float y, int color, boolean shadow){
      TextRenderer.drawLeft(ct, text, x, y, color, shadow, 1.0F);
   }

   public static void drawCenteredText(DrawContext ct, String text, float x, float y, int color, boolean shadow, float scale){
      TextRenderer.drawCentered(ct, text, x, y, color, shadow, scale);
   }

   public static void drawCenteredText(DrawContext ct, String text, float x, float y, int color, boolean shadow){
      TextRenderer.drawCentered(ct, text, x, y, color, shadow, 1.0F);
   }

   public static void drawRightAlignedText(DrawContext ct, String text, float x, float y, int color, boolean shadow, float scale){
      TextRenderer.drawRight(ct, text, x, y, color, shadow, scale);
   }

   public static void drawRightAlignedText(DrawContext ct, String text, float x, float y, int color, boolean shadow){
      TextRenderer.drawRight(ct, text, x, y, color, shadow, 1.0F);
   }

   public static float getTextWidth(String text, float scale){
      return TextRenderer.measureWidth(text, scale);
   }

   public static float getTextWidth(String text){
      return TextRenderer.measureWidth(text, 1.0F);
   }

   public static float getTextHeight(float scale){
      return TextRenderer.measureHeight(scale);
   }

   public static float getTextHeight(){
      return TextRenderer.measureHeight(1.0F);
   }
}

