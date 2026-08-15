package made4mischief.astatine.client.utils.render.renderer;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack.Entry;

@Environment(EnvType.CLIENT)
public final class WorldBoxRenderer {
   private static final RenderLayer SOLID_LAYER = createSolidLayer(false);
   private static final RenderLayer SOLID_LAYER_CRULLED = createSolidLayer(true);
   private static final RenderLayer OUTLINE_LAYER = createOutlineLayer(false);
   private static final RenderLayer OUTLINE_LAYER_CRULLED = createOutlineLayer(true);

   private WorldBoxRenderer(){
   }

   public static void render(
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
      WorldRenderState worldRenderState = context.worldState();
      MatrixStack matrices = context.matrices();
      if ((fill || outline) && !(maxX <= minX) && !(maxY <= minY) && !(maxZ <= minZ) && worldRenderState != null && worldRenderState.cameraRenderState != null && matrices != null) {
         Vec3d vec = worldRenderState.cameraRenderState.pos;
         if (vec != null) {
            float var22 = (float)(maxX - minX);
            float var23 = (float)(maxY - minY);
            float var24 = (float)(maxZ - minZ);
            matrices.push();
            matrices.translate(minX - vec.x, minY - vec.y, minZ - vec.z);
            if (fill && fillColor >>> 24 != 0) {
               RenderLayer renderLayer2 = throughWalls ? SOLID_LAYER_CRULLED : SOLID_LAYER;
               context.commandQueue().submitCustom(matrices, renderLayer2, (entry, vertices) -> drawBoxFilled(entry, vertices, var22, var23, var24, fillColor));
            }

            if (outline && outlineColor >>> 24 != 0) {
               RenderLayer renderLayer = throughWalls ? OUTLINE_LAYER_CRULLED : OUTLINE_LAYER;
               float max = Math.max(1.0F, lineWidth);
               context.commandQueue().submitCustom(matrices, renderLayer, (entry, vertices) -> drawBoxOutline(entry, vertices, var22, var23, var24, outlineColor, max));
            }

            matrices.pop();
         }
      }
   }

   private static RenderLayer createSolidLayer(boolean throughWalls){
      String var1 = throughWalls ? "through" : "depth";
      RenderPipeline renderPipeline = RenderPipeline.builder(new Snippet[]{RenderPipelines.POSITION_COLOR_SNIPPET})
         .withLocation("astatine/world_box_fill_" + var1)
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthTestFunction(throughWalls ? DepthTestFunction.NO_DEPTH_TEST : DepthTestFunction.LEQUAL_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS)
         .build();
      return RenderLayer.of("astatine_world_box_fill_" + var1, RenderSetup.builder(renderPipeline).translucent().expectedBufferSize(8192).build());
   }

   private static RenderLayer createOutlineLayer(boolean throughWalls){
      String var1 = throughWalls ? "through" : "depth";
      RenderPipeline renderPipeline = RenderPipeline.builder(new Snippet[]{RenderPipelines.RENDERTYPE_LINES_SNIPPET})
         .withLocation("astatine/world_box_outline_" + var1)
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthTestFunction(throughWalls ? DepthTestFunction.NO_DEPTH_TEST : DepthTestFunction.LEQUAL_DEPTH_TEST)
         .withDepthWrite(false)
         .withVertexFormat(VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH, DrawMode.LINES)
         .build();
      return RenderLayer.of("astatine_world_box_outline_" + var1, RenderSetup.builder(renderPipeline).translucent().expectedBufferSize(4096).build());
   }

   private static void drawBoxFilled(Entry entry, VertexConsumer vertices, float x, float y, float z, int color){
      drawQuad(entry, vertices, 0.0F, 0.0F, 0.0F, x, 0.0F, 0.0F, x, 0.0F, z, 0.0F, 0.0F, z, color);
      drawQuad(entry, vertices, 0.0F, y, 0.0F, 0.0F, y, z, x, y, z, x, y, 0.0F, color);
      drawQuad(entry, vertices, 0.0F, 0.0F, 0.0F, 0.0F, y, 0.0F, x, y, 0.0F, x, 0.0F, 0.0F, color);
      drawQuad(entry, vertices, 0.0F, 0.0F, z, x, 0.0F, z, x, y, z, 0.0F, y, z, color);
      drawQuad(entry, vertices, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, z, 0.0F, y, z, 0.0F, y, 0.0F, color);
      drawQuad(entry, vertices, x, 0.0F, 0.0F, x, y, 0.0F, x, y, z, x, 0.0F, z, color);
   }

   private static void drawBoxOutline(Entry entry, VertexConsumer vertices, float x, float y, float z, int color, float lineWidth){
      line(entry, vertices, 0.0F, 0.0F, 0.0F, x, 0.0F, 0.0F, color, lineWidth);
      line(entry, vertices, x, 0.0F, 0.0F, x, 0.0F, z, color, lineWidth);
      line(entry, vertices, x, 0.0F, z, 0.0F, 0.0F, z, color, lineWidth);
      line(entry, vertices, 0.0F, 0.0F, z, 0.0F, 0.0F, 0.0F, color, lineWidth);
      line(entry, vertices, 0.0F, y, 0.0F, x, y, 0.0F, color, lineWidth);
      line(entry, vertices, x, y, 0.0F, x, y, z, color, lineWidth);
      line(entry, vertices, x, y, z, 0.0F, y, z, color, lineWidth);
      line(entry, vertices, 0.0F, y, z, 0.0F, y, 0.0F, color, lineWidth);
      line(entry, vertices, 0.0F, 0.0F, 0.0F, 0.0F, y, 0.0F, color, lineWidth);
      line(entry, vertices, x, 0.0F, 0.0F, x, y, 0.0F, color, lineWidth);
      line(entry, vertices, x, 0.0F, z, x, y, z, color, lineWidth);
      line(entry, vertices, 0.0F, 0.0F, z, 0.0F, y, z, color, lineWidth);
   }

   private static void drawQuad(
      Entry entry,
      VertexConsumer vertices,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      float x3,
      float y3,
      float z3,
      float x4,
      float y4,
      float z4,
      int color
   ){
      vertices.vertex(entry, x1, y1, z1).color(color);
      vertices.vertex(entry, x2, y2, z2).color(color);
      vertices.vertex(entry, x3, y3, z3).color(color);
      vertices.vertex(entry, x4, y4, z4).color(color);
   }

   private static void line(Entry entry, VertexConsumer vertices, float x1, float y1, float z1, float x2, float y2, float z2, int color, float lineWidth){
      float var10 = x2 - x1;
      float var11 = y2 - y1;
      float var12 = z2 - z1;
      float var13 = var10 * var10 + var11 * var11 + var12 * var12;
      if (var13 > 1.0E-8F) {
         float sqrt = (float)(1.0 / Math.sqrt(var13));
         var10 *= sqrt;
         var11 *= sqrt;
         var12 *= sqrt;
      } else {
         var10 = 0.0F;
         var11 = 1.0F;
         var12 = 0.0F;
      }

      vertices.vertex(entry, x1, y1, z1).color(color).normal(entry, var10, var11, var12).lineWidth(lineWidth);
      vertices.vertex(entry, x2, y2, z2).color(color).normal(entry, var10, var11, var12).lineWidth(lineWidth);
   }
}

