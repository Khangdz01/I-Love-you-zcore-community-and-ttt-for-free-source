package made4mischief.astatine.client.render.popchams;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.HashMap;
import java.util.Map;
import made4mischief.astatine.Astatine;
import made4mischief.astatine.client.mixin.LivingEntityRendererInvoker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityPose;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderSetup.OutlineMode;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.client.model.ModelPart.Cuboid;

@Environment(EnvType.CLIENT)
public final class PopChamsRenderer {
   private static final int OUTLINE_COLOR = 15728880;
   private static final RenderPipeline PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.ENTITY_SNIPPET})
         .withLocation(Astatine.id("pipeline/pop_chams_full"))
         .withShaderDefine("ALPHA_CUTOUT", 0.02F)
         .withShaderDefine("PER_FACE_LIGHTING")
         .withSampler("Sampler1")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withCull(false)
         .withDepthWrite(false)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .build()
   );
   private static final Map<Identifier, RenderLayer> RENDER_LAYERS = new HashMap<>();
   private static final RenderPipeline LINES_PIPELINE = RenderPipeline.builder(new Snippet[]{RenderPipelines.RENDERTYPE_LINES_SNIPPET})
      .withLocation(Astatine.id("pipeline/pop_chams_line"))
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withVertexFormat(VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH, DrawMode.LINES)
      .build();
   private static final RenderLayer LINES_LAYER = RenderLayer.of(
      "astatine_pop_chams_line", RenderSetup.builder(LINES_PIPELINE).translucent().expectedBufferSize(2048).build()
   );

   private PopChamsRenderer(){
   }

   public static void render(
      WorldRenderContext context,
      PlayerEntityRenderState state,
      double worldX,
      double worldY,
      double worldZ,
      int rgb,
      float fillOpacity,
      float lineOpacity,
      boolean line,
      float lineWidth
   ){
      if ((!(fillOpacity <= 0.001F) || line && !(lineOpacity <= 0.001F)) && context.worldState().cameraRenderState != null) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.getEntityRenderDispatcher().getRenderer(state) instanceof PlayerEntityRenderer var15) {
            Vec3d vec = context.worldState().cameraRenderState.pos;
            if (vec != null) {
               Identifier id = var15.getTexture(state);
               PlayerEntityModel playerEntityModel = (PlayerEntityModel)var15.getModel();
               Vec3d vec2 = var15.getPositionOffset(state);
               MatrixStack matrices = context.matrices();
               matrices.push();
               matrices.translate(
                  worldX - vec.x + vec2.x, worldY - vec.y + vec2.y, worldZ - vec.z + vec2.z
               );
               renderEntityOutline(var15, state, matrices);
               if (fillOpacity > 0.001F) {
                  int round2 = Math.max(0, Math.min(255, Math.round(fillOpacity * 255.0F)));
                  int var22 = round2 << 24 | rgb & 16777215;
                  context.commandQueue().submitModel(playerEntityModel, state, matrices, getOrCreateOutlineLayer(id), 15728880, OverlayTexture.DEFAULT_UV, var22, null, 0, null);
               }

               if (line && lineOpacity > 0.001F) {
                  int round = Math.max(0, Math.min(255, Math.round(lineOpacity * 255.0F)));
                  int var24 = round << 24 | rgb & 16777215;
                  renderModelOutline(context, playerEntityModel, state, matrices, var24, lineWidth);
               }

               matrices.pop();
            }
         }
      }
   }

   private static void renderModelOutline(WorldRenderContext context, PlayerEntityModel model, PlayerEntityRenderState state, MatrixStack matrices, int color, float lineWidth){
      model.setAngles(state);
      matrices.push();
      model.getRootPart().applyTransform(matrices);
      renderCuboidOutline(context, matrices, model.head, color, lineWidth);
      renderCuboidOutline(context, matrices, model.body, color, lineWidth);
      renderCuboidOutline(context, matrices, model.rightArm, color, lineWidth);
      renderCuboidOutline(context, matrices, model.leftArm, color, lineWidth);
      renderCuboidOutline(context, matrices, model.rightLeg, color, lineWidth);
      renderCuboidOutline(context, matrices, model.leftLeg, color, lineWidth);
      matrices.pop();
   }

   private static void renderCuboidOutline(WorldRenderContext context, MatrixStack matrices, ModelPart part, int color, float lineWidth){
      if (part.visible && !part.hidden && !part.isEmpty()) {
         part.forEachCuboid(
            matrices,
            (entry, path, index, cuboid) -> context.commandQueue()
               .submitCustom(matrices, LINES_LAYER, (queuedEntry, vertices) -> renderCuboidWireframe(queuedEntry, vertices, cuboid, color, lineWidth))
         );
      }
   }

   private static void renderCuboidWireframe(Entry entry, VertexConsumer vertices, Cuboid cuboid, int color, float width){
      float var5 = cuboid.minX / 16.0F;
      float var6 = cuboid.minY / 16.0F;
      float var7 = cuboid.minZ / 16.0F;
      float var8 = cuboid.maxX / 16.0F;
      float var9 = cuboid.maxY / 16.0F;
      float var10 = cuboid.maxZ / 16.0F;
      line(entry, vertices, var5, var6, var7, var8, var6, var7, color, width);
      line(entry, vertices, var8, var6, var7, var8, var9, var7, color, width);
      line(entry, vertices, var8, var9, var7, var5, var9, var7, color, width);
      line(entry, vertices, var5, var9, var7, var5, var6, var7, color, width);
      line(entry, vertices, var5, var6, var10, var8, var6, var10, color, width);
      line(entry, vertices, var8, var6, var10, var8, var9, var10, color, width);
      line(entry, vertices, var8, var9, var10, var5, var9, var10, color, width);
      line(entry, vertices, var5, var9, var10, var5, var6, var10, color, width);
      line(entry, vertices, var5, var6, var7, var5, var6, var10, color, width);
      line(entry, vertices, var8, var6, var7, var8, var6, var10, color, width);
      line(entry, vertices, var8, var9, var7, var8, var9, var10, color, width);
      line(entry, vertices, var5, var9, var7, var5, var9, var10, color, width);
   }

   private static void line(Entry entry, VertexConsumer vertices, float x1, float y1, float z1, float x2, float y2, float z2, int color, float width){
      vertices.vertex(entry, x1, y1, z1).color(color).normal(entry, 0.0F, 1.0F, 0.0F).lineWidth(width);
      vertices.vertex(entry, x2, y2, z2).color(color).normal(entry, 0.0F, 1.0F, 0.0F).lineWidth(width);
   }

   private static void renderEntityOutline(PlayerEntityRenderer<?> renderer, PlayerEntityRenderState state, MatrixStack matrices){
      if (state.isInPose(EntityPose.SLEEPING)) {
         Direction direction = state.sleepingDirection;
         if (direction != null) {
            float var4 = state.standingEyeHeight - 0.1F;
            matrices.translate(-direction.getOffsetX() * var4, 0.0F, -direction.getOffsetZ() * var4);
         }
      }

      float var5 = state.baseScale;
      matrices.scale(var5, var5, var5);
      LivingEntityRendererInvoker livingEntityRendererInvoker = (LivingEntityRendererInvoker)renderer;
      livingEntityRendererInvoker.astatine$setupTransforms(state, matrices, state.bodyYaw, var5);
      matrices.scale(-1.0F, -1.0F, 1.0F);
      livingEntityRendererInvoker.astatine$scale(state, matrices);
      matrices.translate(0.0F, -1.501F, 0.0F);
   }

   private static RenderLayer getOrCreateOutlineLayer(Identifier texture){
      return RENDER_LAYERS.computeIfAbsent(
         texture,
         id -> RenderLayer.of(
            "astatine_pop_chams_full",
            RenderSetup.builder(PIPELINE)
               .texture("Sampler0", id)
               .useLightmap()
               .useOverlay()
               .translucent()
               .outlineMode(OutlineMode.NONE)
               .build()
         )
      );
   }
}

