package made4mischief.astatine.client.render.nametags;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import made4mischief.astatine.Astatine;
import made4mischief.astatine.client.mixin.ItemLayerRenderStateAccessor;
import made4mischief.astatine.client.mixin.ItemRenderStateAccessor;
import made4mischief.astatine.client.modules.ModuleManager;
import made4mischief.astatine.client.modules.player.FriendModule;
import made4mischief.astatine.client.modules.render.NameTagsModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.util.Formatting;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.text.MutableText;
import net.minecraft.util.math.random.Random;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.client.render.item.ItemRenderState.LayerRenderState;
import net.minecraft.client.render.RenderSetup.OutlineMode;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.util.math.MatrixStack.Entry;

@Environment(EnvType.CLIENT)
public final class NameTagsFeatureRenderer {
   private static final float BASE_SCALE = 0.025F;
   private static final float ITEM_SIZE = 13.0F;
   private static final float ITEM_GAP = 3.0F;
   private static final float ITEM_Y_OFFSET = -24.0F;
   private static final float ITEM_SCALE = 0.42F;
   private static final int OUTLINE_COLOR = 15728880;
   private static final RenderPipeline PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.TRANSFORMS_PROJECTION_FOG_LIGHTING_SNIPPET})
         .withLocation(Astatine.id("pipeline/nametag_item_no_depth"))
         .withVertexShader("core/rendertype_item_entity_translucent_cull")
         .withFragmentShader("core/rendertype_item_entity_translucent_cull")
         .withSampler("Sampler0")
         .withSampler("Sampler2")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withCull(false)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withDepthWrite(false)
         .withVertexFormat(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, DrawMode.QUADS)
         .build()
   );
   private static final RenderLayer ITEM_SHADER = loadFragmentShader("astatine_nametag_items", SpriteAtlasTexture.ITEMS_ATLAS_TEXTURE);
   private static final RenderLayer BLOCK_SHADER = loadFragmentShader("astatine_nametag_blocks", SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
   private static NameTagsModule module;

   private NameTagsFeatureRenderer(){
   }

   public static boolean isEnabled(){
      if (module == null) {
         module = ModuleManager.INSTANCE.getModule(NameTagsModule.class);
      }

      return module != null && module.isEnabled();
   }

   public static Text styleName(PlayerEntityRenderState state, Text vanillaName){
      MinecraftClient client = MinecraftClient.getInstance();
      if (state != null && vanillaName != null && client.world != null) {
         if (client.world.getEntityById(state.id) instanceof PlayerEntity var4) {
            MutableText text = FriendModule.isFriend(var4)
               ? Text.literal(vanillaName.getString()).formatted(Formatting.GREEN)
               : vanillaName.copy();
            if (!isEnabled()) {
               return text;
            } else {
               float health = Math.max(0.0F, var4.getHealth());
               float maxHealth = Math.max(1.0F, var4.getMaxHealth());
               float var8 = health / maxHealth;
               Formatting formatting = var8 > 0.66F ? Formatting.GREEN : (var8 > 0.33F ? Formatting.YELLOW : Formatting.RED);
               return Text.literal(formatHealth(health) + "â¤ ").formatted(formatting).append(text);
            }
         } else {
            return vanillaName;
         }
      } else {
         return vanillaName;
      }
   }

   public static void renderEquipment(PlayerEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState){
      MinecraftClient client = MinecraftClient.getInstance();
      if (isEnabled()
         && client.player != null
         && client.world != null
         && state.id != client.player.getId()
         && !state.invisible
         && state.displayName != null
         && state.nameLabelPos != null
         && !(state.squaredDistanceToCamera > module.getRangeSquared())) {
         if (client.world.getEntityById(state.id) instanceof PlayerEntity var6) {
            List<ItemStack> list = collectEquipmentStacks(var6);
            if (!list.isEmpty()) {
               double sqrt = Math.sqrt(state.squaredDistanceToCamera);
               float var10 = (float)(1.0 + sqrt * 0.04);
               float size = list.size() * 13.0F + (list.size() - 1) * 3.0F;
               float var12 = -size / 2.0F;
               matrices.push();
               translateToLabel(matrices, state.nameLabelPos, cameraState, module.getScale() * var10);

               for (ItemStack stack : list) {
                  renderItem(matrices, queue, client, var6, stack, var12, -24.0F);
                  renderItemLabel(matrices, queue, stack, var12, -10.0F);
                  var12 += 16.0F;
               }

               matrices.pop();
            }
         }
      }
   }

   private static void translateToLabel(MatrixStack matrices, Vec3d labelPosition, CameraRenderState cameraState, float scale){
      matrices.translate(labelPosition.x, labelPosition.y + 0.5, labelPosition.z);
      matrices.multiply(cameraState.orientation);
      float var4 = 0.025F * scale;
      matrices.scale(var4, -var4, var4);
   }

   private static List<ItemStack> collectEquipmentStacks(PlayerEntity player){
      ArrayList var1 = new ArrayList(6);
      addStackIfPresent(var1, player.getMainHandStack());
      addStackIfPresent(var1, player.getEquippedStack(EquipmentSlot.HEAD));
      addStackIfPresent(var1, player.getEquippedStack(EquipmentSlot.CHEST));
      addStackIfPresent(var1, player.getEquippedStack(EquipmentSlot.LEGS));
      addStackIfPresent(var1, player.getEquippedStack(EquipmentSlot.FEET));
      addStackIfPresent(var1, player.getOffHandStack());
      return var1;
   }

   private static void addStackIfPresent(List<ItemStack> stacks, ItemStack stack){
      if (!stack.isEmpty()) {
         stacks.add(stack);
      }
   }

   private static void renderItem(MatrixStack matrices, OrderedRenderCommandQueue queue, MinecraftClient client, PlayerEntity player, ItemStack stack, float x, float y){
      ItemRenderState itemRenderState = new ItemRenderState();
      client.getItemModelManager().updateForLivingEntity(itemRenderState, stack, ItemDisplayContext.GUI, player);
      boolean var8 = isItemStateEmpty(itemRenderState);
      matrices.push();
      matrices.translate(x + 6.5F, y + 6.5F, 0.01F);
      matrices.scale(13.0F, -13.0F, 13.0F);
      if (var8) {
         itemRenderState.render(matrices, queue, 15728880, OverlayTexture.DEFAULT_UV, 0);
      } else {
         drawItemModel(matrices, queue, itemRenderState);
      }

      matrices.pop();
   }

   private static void renderItemLabel(MatrixStack matrices, OrderedRenderCommandQueue queue, ItemStack stack, float x, float y){
      String var5 = getItemCountText(stack);
      if (!var5.isEmpty()) {
         matrices.push();
         matrices.translate(x, y, 0.02F);
         matrices.scale(0.42F, 0.42F, 0.42F);
         queue.submitText(matrices, 0.0F, 0.0F, Text.literal(var5).asOrderedText(), false, TextLayerType.SEE_THROUGH, 15728880, -2496769, 0, 0);
         matrices.pop();
      }
   }

   private static String getItemCountText(ItemStack stack){
      if (stack.isDamageable() && stack.getMaxDamage() > 0) {
         int maxDamage = Math.round(100.0F * (stack.getMaxDamage() - stack.getDamage()) / stack.getMaxDamage());
         return maxDamage + "%";
      } else {
         return "";
      }
   }

   private static String formatHealth(float health){
      int round = Math.round(health * 10.0F);
      return round % 10 == 0 ? Integer.toString(round / 10) : String.format(Locale.ROOT, "%.1f", round / 10.0F);
   }

   private static boolean isItemStateEmpty(ItemRenderState itemState){
      ItemRenderStateAccessor itemRenderStateAccessor = (ItemRenderStateAccessor)itemState;
      LayerRenderState[] var2 = itemRenderStateAccessor.astatine$getLayers();
      int min = Math.min(itemRenderStateAccessor.astatine$getLayerCount(), var2.length);
      if (min == 0) {
         return false;
      } else {
         for (int index = 0; index < min; index++) {
            LayerRenderState layerRenderState = var2[index];
            ItemLayerRenderStateAccessor itemLayerRenderStateAccessor = (ItemLayerRenderStateAccessor)layerRenderState;
            RenderLayer renderLayer = itemLayerRenderStateAccessor.astatine$getRenderLayer();
            if (renderLayer == null) {
               return false;
            }

            boolean blockTranslucentCull = renderLayer == TexturedRenderLayers.getEntityCutout() || renderLayer == TexturedRenderLayers.getBlockTranslucentCull();
            layerRenderState.setRenderLayer(blockTranslucentCull ? BLOCK_SHADER : ITEM_SHADER);
         }

         return true;
      }
   }

   private static void drawItemModel(MatrixStack matrices, OrderedRenderCommandQueue queue, ItemRenderState itemState){
      Sprite sprite = itemState.getParticleSprite(Random.create(0L));
      RenderLayer renderLayer = sprite.getAtlasId().equals(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE) ? BLOCK_SHADER : ITEM_SHADER;
      queue.submitCustom(matrices, renderLayer, (entry, vertices) -> {
         drawQuad(entry, vertices, -0.5F, -0.5F, sprite.getMinU(), sprite.getMaxV());
         drawQuad(entry, vertices, 0.5F, -0.5F, sprite.getMaxU(), sprite.getMaxV());
         drawQuad(entry, vertices, 0.5F, 0.5F, sprite.getMaxU(), sprite.getMinV());
         drawQuad(entry, vertices, -0.5F, 0.5F, sprite.getMinU(), sprite.getMinV());
      });
   }

   private static void drawQuad(Entry entry, VertexConsumer vertices, float x, float y, float u, float v){
      vertices.vertex(entry, x, y, 0.0F)
         .color(-1)
         .texture(u, v)
         .overlay(OverlayTexture.DEFAULT_UV)
         .light(15728880)
         .normal(entry, 0.0F, 0.0F, 1.0F);
   }

   private static RenderLayer loadFragmentShader(String name, Identifier texture){
      return RenderLayer.of(
         name,
         RenderSetup.builder(PIPELINE)
            .texture("Sampler0", texture)
            .useLightmap()
            .useOverlay()
            .translucent()
            .outlineMode(OutlineMode.NONE)
            .build()
      );
   }
}

