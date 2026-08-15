package made4mischief.astatine.client.render.skeleton;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import made4mischief.astatine.Astatine;
import made4mischief.astatine.client.modules.ModuleManager;
import made4mischief.astatine.client.modules.render.SkeletonModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.RenderCommandQueue;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.util.math.MatrixStack.Entry;

@Environment(EnvType.CLIENT)
public final class SkeletonFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
   private static final float LINE_SCALE = 0.0625F;
   private static final RenderPipeline PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.RENDERTYPE_LINES_SNIPPET})
         .withLocation(Astatine.id("pipeline/player_skeleton"))
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withDepthWrite(false)
         .withVertexFormat(VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH, DrawMode.LINES)
         .build()
   );
   private static final RenderLayer LAYER = RenderLayer.of(
      "astatine_player_skeleton", RenderSetup.builder(PIPELINE).translucent().expectedBufferSize(512).build()
   );
   private static SkeletonModule module;
   private static boolean initialized;

   private SkeletonFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context){
      super(context);
   }

   public static void init(){
      if (!initialized) {
         LivingEntityFeatureRendererRegistrationCallback.EVENT
            .register((LivingEntityFeatureRendererRegistrationCallback)(entityType, renderer, helper, context) -> {
               if (renderer instanceof PlayerEntityRenderer var4) {
                  helper.register(new SkeletonFeatureRenderer(var4));
               }
            });
         initialized = true;
      }
   }

   public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, PlayerEntityRenderState state, float limbAngle, float limbDistance){
      MinecraftClient client = MinecraftClient.getInstance();
      if (shouldRender(client, state)) {
         PlayerEntityModel playerEntityModel = (PlayerEntityModel)this.getContextModel();
         playerEntityModel.setAngles(state);
         int color = module.getColor();
         float lineWidth = module.getLineWidth();
         RenderCommandQueue renderCommandQueue = queue.getBatchingQueue(1);
         matrices.push();
         playerEntityModel.getRootPart().applyTransform(matrices);
         matrices.push();
         playerEntityModel.body.applyTransform(matrices);
         drawLine(renderCommandQueue, matrices, 0.0F, 0.0F, 0.0F, 0.0F, 0.75F, 0.0F, color, lineWidth);
         drawLine(renderCommandQueue, matrices, -0.3125F, 0.125F, 0.0F, 0.3125F, 0.125F, 0.0F, color, lineWidth);
         drawLine(renderCommandQueue, matrices, -0.11875F, 0.75F, 0.0F, 0.11875F, 0.75F, 0.0F, color, lineWidth);
         matrices.pop();
         drawBone(renderCommandQueue, matrices, playerEntityModel.rightArm, 0.0F, 0.0F, 0.0F, 0.0F, 0.625F, 0.0F, color, lineWidth);
         drawBone(renderCommandQueue, matrices, playerEntityModel.leftArm, 0.0F, 0.0F, 0.0F, 0.0F, 0.625F, 0.0F, color, lineWidth);
         drawBone(renderCommandQueue, matrices, playerEntityModel.rightLeg, 0.0F, 0.0F, 0.0F, 0.0F, 0.75F, 0.0F, color, lineWidth);
         drawBone(renderCommandQueue, matrices, playerEntityModel.leftLeg, 0.0F, 0.0F, 0.0F, 0.0F, 0.75F, 0.0F, color, lineWidth);
         matrices.pop();
      }
   }

   private static boolean shouldRender(MinecraftClient client, PlayerEntityRenderState state){
      if (module == null) {
         module = ModuleManager.INSTANCE.getModule(SkeletonModule.class);
      }

      return module != null
         && module.isEnabled()
         && client.player != null
         && (module.rendersSelf() || state.id != client.player.getId())
         && !state.spectator
         && !state.invisible;
   }

   private static void drawBone(
      RenderCommandQueue queue, MatrixStack matrices, ModelPart part, float x1, float y1, float z1, float x2, float y2, float z2, int color, float width
   ){
      if (part.visible && !part.hidden) {
         matrices.push();
         part.applyTransform(matrices);
         drawLine(queue, matrices, x1, y1, z1, x2, y2, z2, color, width);
         matrices.pop();
      }
   }

   private static void drawLine(RenderCommandQueue queue, MatrixStack matrices, float x1, float y1, float z1, float x2, float y2, float z2, int color, float width){
      queue.submitCustom(matrices, LAYER, (entry, vertices) -> line(entry, vertices, x1, y1, z1, x2, y2, z2, color, width));
   }

   private static void line(Entry entry, VertexConsumer vertices, float x1, float y1, float z1, float x2, float y2, float z2, int color, float width){
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

      vertices.vertex(entry, x1, y1, z1).color(color).normal(entry, var10, var11, var12).lineWidth(width);
      vertices.vertex(entry, x2, y2, z2).color(color).normal(entry, var10, var11, var12).lineWidth(width);
   }
}

