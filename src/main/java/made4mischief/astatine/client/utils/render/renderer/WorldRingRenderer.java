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
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack.Entry;

@Environment(EnvType.CLIENT)
public final class WorldRingRenderer {
   private static final int SEGMENT_COUNT = 128;
   private static final float[] COS_TABLE = new float[129];
   private static final float[] SIN = new float[129];
   private static final RenderPipeline PIPELINE = RenderPipeline.builder(new Snippet[]{RenderPipelines.POSITION_COLOR_SNIPPET})
      .withLocation("astatine/world_ring")
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS)
      .build();
   private static final RenderLayer LAYER = RenderLayer.of(
      "astatine_world_ring", RenderSetup.builder(PIPELINE).translucent().expectedBufferSize(8192).build()
   );

   private WorldRingRenderer(){
   }

   public static void render(
      WorldRenderContext context, double worldX, double worldY, double worldZ, float radius, float thickness, float glow, int rgb, float opacity
   ){
      if (!(radius <= 0.0F) && !(thickness <= 0.0F) && !(opacity <= 0.0F) && context.worldState().cameraRenderState != null) {
         Vec3d vec = context.worldState().cameraRenderState.pos;
         if (vec != null) {
            float min = Math.min(1.0F, opacity);
            MatrixStack matrices = context.matrices();
            matrices.push();
            matrices.translate(worldX - vec.x, worldY - vec.y, worldZ - vec.z);
            context.commandQueue().submitCustom(matrices, LAYER, (entry, vertices) -> {
               if (glow > 0.001F) {
                  drawRingFade(entry, vertices, radius, thickness + glow, withAlpha(rgb, Math.round(58.0F * min)));
               }

               drawRingFade(entry, vertices, radius, thickness, withAlpha(rgb, Math.round(185.0F * min)));
               drawRingSolid(entry, vertices, radius, Math.max(0.012F, thickness * 0.22F), withAlpha(rgb, Math.round(245.0F * min)));
            });
            matrices.pop();
         }
      }
   }

   private static void drawRingFade(Entry entry, VertexConsumer vertices, float radius, float halfWidth, int centerColor){
      int var5 = centerColor & 16777215;
      float max = Math.max(0.0F, radius - halfWidth);
      float var7 = radius + halfWidth;
      drawRingSegment(entry, vertices, max, radius, var5, centerColor);
      drawRingSegment(entry, vertices, radius, var7, centerColor, var5);
   }

   private static void drawRingSolid(Entry entry, VertexConsumer vertices, float radius, float halfWidth, int color){
      drawRingSegment(entry, vertices, Math.max(0.0F, radius - halfWidth), radius + halfWidth, color, color);
   }

   private static void drawRingSegment(Entry entry, VertexConsumer vertices, float innerRadius, float outerRadius, int innerColor, int outerColor){
      for (int index = 0; index < 128; index++) {
         vertices.vertex(entry, COS_TABLE[index] * innerRadius, 0.0F, SIN[index] * innerRadius).color(innerColor);
         vertices.vertex(entry, COS_TABLE[index + 1] * innerRadius, 0.0F, SIN[index + 1] * innerRadius).color(innerColor);
         vertices.vertex(entry, COS_TABLE[index + 1] * outerRadius, 0.0F, SIN[index + 1] * outerRadius).color(outerColor);
         vertices.vertex(entry, COS_TABLE[index] * outerRadius, 0.0F, SIN[index] * outerRadius).color(outerColor);
      }
   }

   private static int withAlpha(int rgb, int alpha){
      return Math.max(0, Math.min(255, alpha)) << 24 | rgb & 16777215;
   }

   static {
      for (int var0 = 0; var0 <= 128; var0++) {
         double var1 = (Math.PI * 2) * var0 / 128.0;
         COS_TABLE[var0] = (float)Math.cos(var1);
         SIN[var0] = (float)Math.sin(var1);
      }
   }
}

