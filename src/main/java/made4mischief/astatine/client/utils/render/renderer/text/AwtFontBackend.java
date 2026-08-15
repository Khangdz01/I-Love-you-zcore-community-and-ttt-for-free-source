package made4mischief.astatine.client.utils.render.renderer.text;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

@Environment(EnvType.CLIENT)
public final class AwtFontBackend implements FontBackend {
   private static final int ATLAS_PADDING = 2;
   private static final int ATLAS_MARGIN = 2;
   private final Font font;
   private final FontMetrics fontMetrics;
   private final int scaledFontHeight;
   private final int ATLAS_WIDTH = 2048;
   private final int ATLAS_HEIGHT = 2048;
   private NativeImage atlasImage;
   private NativeImageBackedTexture atlasTexture;
   private Identifier atlasIdentifier;
   private boolean atlasInitialized = false;
   private int cursorX = 2;
   private int cursorY = 2;
   private int rowMaxHeight = 0;
   private final Map<Character, AwtFontBackend.Glyph> glyphCache = new HashMap<>();

   public AwtFontBackend(InputStream fontStream, float size){
      Font font;
      try {
         if (fontStream != null) {
            font = Font.createFont(0, fontStream).deriveFont(1, size * 2.0F);
         } else {
            font = new Font("Segoe UI", 1, Math.round(size * 2.0F));
         }
      } catch (Exception e) {
         font = new Font("Segoe UI", 1, Math.round(size * 2.0F));
      }

      this.font = font;
      BufferedImage bufferedImage = new BufferedImage(1, 1, 2);
      Graphics2D graphics2D = bufferedImage.createGraphics();
      graphics2D.setFont(this.font);
      this.fontMetrics = graphics2D.getFontMetrics();
      this.scaledFontHeight = Math.max(1, Math.round(this.fontMetrics.getHeight() / 2.0F));
      graphics2D.dispose();
   }

   private void initAtlas(){
      if (!this.atlasInitialized) {
         this.atlasImage = new NativeImage(2048, 2048, true);
         this.atlasTexture = new AwtFontBackend.SmoothNativeImageBackedTexture(() -> "astatine_font_atlas", this.atlasImage);
         this.atlasIdentifier = Identifier.of("astatine", "dynamic_font_atlas_" + System.nanoTime());
         MinecraftClient.getInstance().getTextureManager().registerTexture(this.atlasIdentifier, this.atlasTexture);
         this.atlasInitialized = true;
      }
   }

   @Override
   public void draw(DrawContext ct, String text, int x, int y, int color, boolean shadow){
      if (text != null && !text.isEmpty()) {
         if (shadow) {
            int var7 = color & 0xFF000000 | (color & 16579836) >> 2;
            this.drawString(ct, text, x + 1, y + 1, var7);
         }

         this.drawString(ct, text, x, y, color);
      }
   }

   private void drawString(DrawContext ct, String text, float x, float y, int color){
      this.initAtlas();
      boolean var6 = false;
      float var7 = x;

      for (int index = 0; index < text.length(); index++) {
         char charAt = text.charAt(index);
         AwtFontBackend.Glyph var10 = this.glyphCache.get(charAt);
         if (var10 == null) {
            var10 = this.getOrRasterizeGlyph(charAt);
            var6 = true;
         }

         if (var10 != null) {
            ct.drawTexture(
               RenderPipelines.GUI_TEXTURED,
               this.atlasIdentifier,
               Math.round(var7),
               Math.round(y),
               var10.u,
               var10.v,
               var10.renderedWidth,
               var10.renderedHeight,
               var10.width,
               var10.height,
               2048,
               2048,
               color
            );
            var7 += var10.advance;
         }
      }

      if (var6) {
         this.atlasTexture.upload();
      }
   }

   private AwtFontBackend.Glyph getOrRasterizeGlyph(char c){
      this.initAtlas();
      int charWidth = this.fontMetrics.charWidth(c);
      if (charWidth <= 0) {
         charWidth = this.fontMetrics.charWidth(' ');
      }

      int height = this.fontMetrics.getHeight();
      int round3 = Math.max(1, Math.round(charWidth / 2.0F));
      byte var5 = 4;
      int var6 = charWidth + var5 * 2;
      int var7 = height + var5 * 2;
      int round2 = Math.max(1, Math.round(var6 / 2.0F));
      int round = Math.max(1, Math.round(var7 / 2.0F));
      if (this.cursorX + var6 > 2048) {
         this.cursorX = 2;
         this.cursorY = this.cursorY + this.rowMaxHeight + 2;
         this.rowMaxHeight = 0;
      }

      if (this.cursorY + var7 > 2048) {
         this.resetLayout();
         return this.getOrRasterizeGlyph(c);
      } else {
         BufferedImage bufferedImage = new BufferedImage(var6, var7, 2);
         Graphics2D graphics2D = bufferedImage.createGraphics();
         graphics2D.setFont(this.font);
         graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
         graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         graphics2D.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
         float var12 = var5;
         float ascent = var5 + this.fontMetrics.getAscent();
         graphics2D.setColor(Color.WHITE);
         graphics2D.drawString(String.valueOf(c), var12, ascent);
         graphics2D.dispose();

         for (int index = 0; index < var7; index++) {
            for (int index2 = 0; index2 < var6; index2++) {
               int rGB = bufferedImage.getRGB(index2, index);
               int var17 = rGB >> 24 & 0xFF;
               int var18 = rGB >> 16 & 0xFF;
               int var19 = rGB >> 8 & 0xFF;
               int var20 = rGB & 0xFF;
               int var21 = var17 << 24 | var20 << 16 | var19 << 8 | var18;
               this.atlasImage.setColor(this.cursorX + index2, this.cursorY + index, var21);
            }
         }

         AwtFontBackend.Glyph var22 = new AwtFontBackend.Glyph(this.cursorX, this.cursorY, var6, var7, round2, round, round3);
         this.glyphCache.put(c, var22);
         this.cursorX += var6 + 2;
         if (var7 > this.rowMaxHeight) {
            this.rowMaxHeight = var7;
         }

         return var22;
      }
   }

   private void resetLayout(){
      this.glyphCache.clear();
      this.cursorX = 2;
      this.cursorY = 2;
      this.rowMaxHeight = 0;
      if (this.atlasInitialized) {
         this.atlasImage.fillRect(0, 0, 2048, 2048, 0);
      }
   }

   @Override
   public int getWidth(String text){
      if (text != null && !text.isEmpty()) {
         this.initAtlas();
         int var2 = 0;
         boolean var3 = false;

         for (int index = 0; index < text.length(); index++) {
            char charAt = text.charAt(index);
            AwtFontBackend.Glyph var6 = this.glyphCache.get(charAt);
            if (var6 == null) {
               var6 = this.getOrRasterizeGlyph(charAt);
               var3 = true;
            }

            if (var6 != null) {
               var2 += var6.advance;
            }
         }

         if (var3) {
            this.atlasTexture.upload();
         }

         return var2;
      } else {
         return 0;
      }
   }

   @Override
   public int getHeight(){
      return this.scaledFontHeight;
   }

   @Environment(EnvType.CLIENT)
   public static final class Glyph {
      public final int u;
      public final int v;
      public final int width;
      public final int height;
      public final int renderedWidth;
      public final int renderedHeight;
      public final int advance;

      public Glyph(int u, int v, int width, int height, int renderedWidth, int renderedHeight, int advance){
         this.u = u;
         this.v = v;
         this.width = width;
         this.height = height;
         this.renderedWidth = renderedWidth;
         this.renderedHeight = renderedHeight;
         this.advance = advance;
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class SmoothNativeImageBackedTexture extends NativeImageBackedTexture {
      private SmoothNativeImageBackedTexture(Supplier<String> label, NativeImage image){
         super(label, image);
         this.sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
      }
   }
}

