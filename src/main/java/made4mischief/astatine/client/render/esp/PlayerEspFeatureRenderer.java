package made4mischief.astatine.client.render.esp;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import java.util.HashMap;
import java.util.Map;
import made4mischief.astatine.Astatine;
import made4mischief.astatine.client.modules.ModuleManager;
import made4mischief.astatine.client.modules.render.EspModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.RenderCommandQueue;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.entity.Entity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.RenderSetup.OutlineMode;

@Environment(EnvType.CLIENT)
public final class PlayerEspFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
   private static final int OUTLINE_COLOR = 15728880;
   private static final RenderPipeline PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.ENTITY_SNIPPET})
         .withLocation(Astatine.id("pipeline/player_esp_full"))
         .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         .withShaderDefine("PER_FACE_LIGHTING")
         .withSampler("Sampler1")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withCull(false)
         .withDepthWrite(false)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .build()
   );
   private static final Map<Identifier, RenderLayer> RENDER_LAYERS = new HashMap<>();
   private static EspModule module;
   private static boolean initialized;

   private PlayerEspFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context){
      super(context);
   }

   public static void init(){
      if (!initialized) {
         LivingEntityFeatureRendererRegistrationCallback.EVENT
            .register((LivingEntityFeatureRendererRegistrationCallback)(entityType, renderer, helper, context) -> {
               if (renderer instanceof PlayerEntityRenderer var4) {
                  helper.register(new PlayerEspFeatureRenderer(var4));
               }
            });
         initialized = true;
      }
   }

   public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, PlayerEntityRenderState state, float limbAngle, float limbDistance){
      MinecraftClient client = MinecraftClient.getInstance();
      if (this.shouldRender(client, state)) {
         Identifier id = state.skinTextures.body().texturePath();
         RenderCommandQueue renderCommandQueue = queue.getBatchingQueue(1);
         if (module.getMode().is("Line")) {
            this.renderLineMode(renderCommandQueue, matrices, state, id);
         } else {
            this.renderFullMode(renderCommandQueue, matrices, state, id);
         }
      }
   }

   private boolean shouldRender(MinecraftClient client, PlayerEntityRenderState state){
      if (module == null) {
         module = ModuleManager.INSTANCE.getModule(EspModule.class);
      }

      Entity entity = client.world == null ? null : client.world.getEntityById(state.id);
      return module != null && module.isEnabled() && module.shouldRenderEntity(client, entity) && !state.spectator && !state.invisible;
   }

   private void renderLineMode(RenderCommandQueue queue, MatrixStack matrices, PlayerEntityRenderState state, Identifier skinTexture){
      queue.submitModel(
         this.getContextModel(),
         state,
         matrices,
         RenderLayers.outlineNoCull(skinTexture),
         15728880,
         OverlayTexture.DEFAULT_UV,
         -1,
         null,
         module.getLineColor().getValue(),
         null
      );
   }

   private void renderFullMode(RenderCommandQueue queue, MatrixStack matrices, PlayerEntityRenderState state, Identifier skinTexture){
      queue.submitModel(this.getContextModel(), state, matrices, getOrCreatePipeline(skinTexture), 15728880, OverlayTexture.DEFAULT_UV, -1, null, 0, null);
   }

   private static RenderLayer getOrCreatePipeline(Identifier skinTexture){
      return RENDER_LAYERS.computeIfAbsent(
         skinTexture,
         texture -> RenderLayer.of(
            "astatine_player_esp_full",
            RenderSetup.builder(PIPELINE)
               .texture("Sampler0", texture)
               .useLightmap()
               .useOverlay()
               .translucent()
               .outlineMode(OutlineMode.NONE)
               .build()
         )
      );
   }
}
