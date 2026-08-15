package made4mischief.astatine.client.hud;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import made4mischief.astatine.Astatine;
import made4mischief.astatine.client.gui.component.theme.Theme;
import made4mischief.astatine.client.gui.component.theme.ThemeManager;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.modules.ModuleManager;
import made4mischief.astatine.client.modules.render.HUDModule;
import made4mischief.astatine.client.utils.render.RenderUtil;
import made4mischief.astatine.client.utils.render.animation.Animation;
import made4mischief.astatine.client.utils.render.animation.AnimationType;
import made4mischief.astatine.client.utils.render.core.ColorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EntityPose;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public final class HudRenderer {
   private static final String WATERMARK_TEXT = "ASTATINE";
   private static final float WATERMARK_X = 4.0F;
   private static final float WATERMARK_Y = 4.0F;
   private static final float HUD_PADDING = 4.0F;
   private static final float WATERMARK_SCALE = 0.55F;
   private static final float STAT_LINE_SCALE = 0.58F;
   private static final float PLAYER_MODEL_GAP = 18.0F;
   private static final float MIN_ALPHA = 1.0F;
   private static final float PANEL_RADIUS = 4.0F;
   private static final float STAT_LINE_OFFSET = 12.0F;
   private static final float WIDGET_SIZE = 10.0F;
   private static final float MODULE_LIST_SCALE = 0.5F;
   private static final float PLAYER_WIDGET_WIDTH = 70.0F;
   private static final float PLAYER_WIDGET_HEIGHT = 96.0F;
   private static final int MAX_MODULE_NAME_LENGTH = 40;
   private static final Animation editorAnimation = new Animation(0.0F, 0.0F, 220L, AnimationType.EASE_OUT);
   private static HUDModule module;
   private static boolean initialized;
   private static long lastTickNanos;
   private static float tps = 20.0F;
   private static Object cachedWorld;
   private static Object cachedNetworkHandler;
   private static Object cachedPlayer;
   private static int tickCounter;
   private static HudRenderer.Widget selectedWidget = HudRenderer.Widget.NONE;
   private static double editorDragStartX;
   private static double editorDragStartY;
   private static float selectedWidgetX;
   private static float selectedWidgetY;

   private HudRenderer(){
   }

   public static void init(){
      if (!initialized) {
         module = ModuleManager.INSTANCE.getModule(HUDModule.class);
         setEnabled(module != null && module.isEnabled());
         HudElementRegistry.attachElementAfter(VanillaHudElements.SUBTITLES, Astatine.id("astatine_hud"), HudRenderer::render);
         initialized = true;
      }
   }

   public static void setEnabled(boolean enabled){
      editorAnimation.setTarget(enabled ? 1.0F : 0.0F);
   }

   public static void onClientTick(){
      MinecraftClient client = MinecraftClient.getInstance();
      ClientWorld world = client.world;
      ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
      ClientPlayerEntity player = client.player;
      if (world != cachedWorld || networkHandler != cachedNetworkHandler || player != cachedPlayer) {
         cachedWorld = world;
         cachedNetworkHandler = networkHandler;
         cachedPlayer = player;
         tickCounter = 0;
         lastTickNanos = 0L;
         tps = 20.0F;
      }

      if (isInWorld(client)) {
         tickCounter = Math.min(40, tickCounter + 1);
      } else {
         tickCounter = 0;
      }

      long nanoTime = System.nanoTime();
      if (lastTickNanos != 0L) {
         float var6 = (float)(nanoTime - lastTickNanos) / 1.0E9F;
         if (var6 > 0.001F && var6 < 1.0F) {
            float min = Math.min(20.0F, 1.0F / var6);
            tps = tps + (min - tps) * 0.12F;
         }
      }

      lastTickNanos = nanoTime;
   }

   private static void render(DrawContext context, RenderTickCounter tickCounter){
      MinecraftClient client = MinecraftClient.getInstance();
      if (module == null) {
         module = ModuleManager.INSTANCE.getModule(HUDModule.class);
      }

      if (module != null && isInGame(client)) {
         float get = editorAnimation.get();
         if (module.isEnabled() && !(get <= 0.001F)) {
            renderWidgets(context, client, get, false);
         }
      }
   }

   public static void renderEditorPreview(DrawContext context){
      MinecraftClient client = MinecraftClient.getInstance();
      if (module == null) {
         module = ModuleManager.INSTANCE.getModule(HUDModule.class);
      }

      if (module != null && isInGame(client)) {
         renderWidgets(context, client, 1.0F, true);
      }
   }

   private static boolean isPlayerWidgetVisible(MinecraftClient client){
      return tickCounter >= 40 && isInWorld(client);
   }

   private static boolean isInGame(MinecraftClient client){
      return client.player != null && client.world != null && client.interactionManager != null;
   }

   private static boolean isInWorld(MinecraftClient client){
      return client.player != null
            && client.world != null
            && client.interactionManager != null
            && client.getNetworkHandler() != null
            && client.player.networkHandler == client.getNetworkHandler()
            && !client.player.isRemoved()
            && client.world.getEntityById(client.player.getId()) == client.player
         ? client.getNetworkHandler().getPlayerListEntry(client.player.getUuid()) != null
         : false;
   }

   private static void renderWidgets(DrawContext context, MinecraftClient client, float alpha, boolean editor){
      Theme theme = ThemeManager.active();
      int scaledWindowWidth = context.getScaledWindowWidth();
      int scaledWindowHeight = context.getScaledWindowHeight();
      ClientPlayerEntity player = client.player;
      renderWatermark(context, alpha);
      if (module.isStatsEnabled()) {
         renderStatsWidget(context, theme, alpha, client, scaledWindowWidth, scaledWindowHeight, editor);
      }

      if (module.isInventoryEnabled()) {
         renderInventoryWidget(context, theme, alpha, player, scaledWindowWidth, scaledWindowHeight, editor);
      }

      if (module.isPlayerEnabled() && isPlayerWidgetVisible(client)) {
         renderPlayerWidget(context, theme, alpha, player, scaledWindowWidth, scaledWindowHeight, editor);
      }

      if (module.isModuleListEnabled()) {
         renderModuleListWidget(context, theme, alpha, client, scaledWindowWidth, scaledWindowHeight, editor);
      }
   }

   private static void renderWatermark(DrawContext context, float alpha){
      drawText(context, "ASTATINE", 4.0F, 4.0F, alpha, 0.55F);
      float textWidth = 4.0F + RenderUtil.getTextWidth("ASTATINE", 0.55F);
      drawText(context, "+", textWidth, 4.0F, alpha, 0.55F);
   }

   private static void renderStatsWidget(DrawContext context, Theme theme, float alpha, MinecraftClient client, int screenWidth, int screenHeight, boolean editor){
      float var7 = getStatsWidgetWidth(client);
      float var8 = 48.0F;
      float statsScale = module.getStatsScale();
      HudRenderer.WidgetBounds var10 = computeWidgetBounds(HudRenderer.Widget.STATS, screenWidth, screenHeight, var7 * statsScale, var8 * statsScale);
      ClientPlayerEntity player = client.player;
      double velocity = Math.sqrt(
            player.getVelocity().x * player.getVelocity().x + player.getVelocity().z * player.getVelocity().z
         )
         * 20.0;
      String var14 = "TPS " + formatFloat(tps);
      String currentFps = "FPS " + client.getCurrentFps();
      String var16 = "SPEED " + formatFloat(velocity) + " b/s";
      String z = "XYZ " + player.getBlockPos().getX() + " " + player.getBlockPos().getY() + " " + player.getBlockPos().getZ();
      boolean var18 = var10.x + var10.width * 0.5F >= screenWidth * 0.5F;
      context.getMatrices().pushMatrix();
      context.getMatrices().translate(var10.x, var10.y);
      context.getMatrices().scale(statsScale, statsScale);
      renderStatLine(context, var14, 0.0F, var7, alpha, var18);
      renderStatLine(context, currentFps, 12.0F, var7, alpha, var18);
      renderStatLine(context, var16, 24.0F, var7, alpha, var18);
      renderStatLine(context, z, 36.0F, var7, alpha, var18);
      context.getMatrices().popMatrix();
   }

   private static void renderStatLine(DrawContext context, String text, float y, float naturalWidth, float alpha, boolean alignRight){
      if (alignRight) {
         drawTextRightAligned(context, text, naturalWidth, y, alpha, 0.58F);
      } else {
         drawText(context, text, 0.0F, y, alpha, 0.58F);
      }
   }

   private static void renderInventoryWidget(DrawContext context, Theme theme, float alpha, PlayerEntity player, int screenWidth, int screenHeight, boolean editor){
      float var7 = getInventoryWidgetHeight();
      float var8 = getInventoryWidgetWidth();
      float inventoryScale = module.getInventoryScale();
      HudRenderer.WidgetBounds var10 = computeWidgetBounds(HudRenderer.Widget.INVENTORY, screenWidth, screenHeight, var7 * inventoryScale, var8 * inventoryScale);
      float inventoryOpacity = alpha * module.getInventoryOpacity();
      int accent = ColorUtil.scaleAlpha(theme.accent(), (editor ? 0.72F : 0.34F) * inventoryOpacity);
      int surface = ColorUtil.scaleAlpha(theme.surface(), inventoryOpacity);
      context.getMatrices().pushMatrix();
      context.getMatrices().translate(var10.x, var10.y);
      context.getMatrices().scale(inventoryScale, inventoryScale);
      RenderUtil.drawBo(context, -1.0F, -1.0F, var7 + 2.0F, var8 + 2.0F, 5.0F, accent);
      RenderUtil.drawBo(context, 0.0F, 0.0F, var7, var8, 4.0F, surface);
      context.getMatrices().pushMatrix();
      context.getMatrices().translate(4.0F, 4.0F);
      context.getMatrices().scale(1.0F, 1.0F);

      for (int index = 9; index < 36; index++) {
         ItemStack stack = player.getInventory().getStack(index);
         int var16 = index - 9;
         int var17 = var16 % 9;
         int var18 = var16 / 9;
         int round = Math.round(var17 * 18.0F + 1.0F);
         int round2 = Math.round(var18 * 18.0F + 1.0F);
         RenderUtil.drawBo(context, (float)round, (float)round2, 16.0F, 16.0F, 3.0F, ColorUtil.scaleAlpha(theme.border(), 0.22F * inventoryOpacity));
         if (!stack.isEmpty()) {
            RenderUtil.drawItemStack(context, player, stack, round, round2, index);
         }
      }

      context.getMatrices().popMatrix();
      context.getMatrices().popMatrix();
   }

   private static void renderPlayerWidget(DrawContext context, Theme theme, float alpha, AbstractClientPlayerEntity player, int screenWidth, int screenHeight, boolean editor){
      float playerScale = module.getPlayerScale();
      HudRenderer.WidgetBounds var8 = computeWidgetBounds(HudRenderer.Widget.PLAYER, screenWidth, screenHeight, 70.0F * playerScale, 96.0F * playerScale);
      int round2 = Math.round(var8.x);
      int round4 = Math.round(var8.y);
      int round5 = Math.round(var8.x + var8.width);
      int round = Math.round(var8.y + var8.height);
      float var13 = (round2 + round5) / 2.0F;
      float var14 = (round4 + round) / 2.0F;
      int round3 = Math.max(1, Math.round(40.0F * playerScale));
      renderPlayerModel(context, player, round2, round4, round5, round, round3, var13 - 18.0F * playerScale, var14 - 7.0F * playerScale);
   }

   private static void renderPlayerModel(DrawContext context, AbstractClientPlayerEntity player, int x1, int y1, int x2, int y2, int modelSize, float mouseX, float mouseY){
      MinecraftClient client = MinecraftClient.getInstance();
      PlayerEntityRenderer playerEntityRenderer = client.getEntityRenderDispatcher().getPlayerRenderer(player);
      PlayerEntityRenderState playerEntityRenderState = playerEntityRenderer.createRenderState();
      playerEntityRenderer.updateRenderState(player, playerEntityRenderState, 1.0F);
      playerEntityRenderState.light = 15728880;
      playerEntityRenderState.shadowPieces.clear();
      playerEntityRenderState.outlineColor = 0;
      float var12 = (x1 + x2) / 2.0F;
      float var13 = (y1 + y2) / 2.0F;
      float atan = (float)Math.atan((var12 - mouseX) / 40.0F);
      float atan2 = (float)Math.atan((var13 - mouseY) / 40.0F);
      Quaternionf quaternion = new Quaternionf().rotateZ((float) Math.PI);
      Quaternionf quaternion2 = new Quaternionf().rotateX(atan2 * 20.0F * (float) (Math.PI / 180.0));
      quaternion.mul(quaternion2);
      playerEntityRenderState.bodyYaw = 180.0F + atan * 20.0F;
      playerEntityRenderState.relativeHeadYaw = atan * 20.0F;
      playerEntityRenderState.pitch = playerEntityRenderState.pose == EntityPose.GLIDING ? 0.0F : -atan2 * 20.0F;
      if (playerEntityRenderState.baseScale != 0.0F) {
         playerEntityRenderState.width = playerEntityRenderState.width / playerEntityRenderState.baseScale;
         playerEntityRenderState.height = playerEntityRenderState.height / playerEntityRenderState.baseScale;
      }

      playerEntityRenderState.baseScale = 1.0F;
      Vector3f vec = new Vector3f(0.0F, playerEntityRenderState.height / 2.0F + 0.0625F, 0.0F);
      context.addEntity(playerEntityRenderState, modelSize, vec, quaternion, quaternion2, x1, y1, x2, y2);
   }

   private static void renderModuleListWidget(DrawContext context, Theme theme, float alpha, MinecraftClient client, int screenWidth, int screenHeight, boolean editor){
      float moduleListScale = module.getModuleListScale();
      float var8 = getModuleListWidth();
      List list = ModuleManager.INSTANCE
         .getModules()
         .stream()
         .filter(Module::isEnabled)
         .sorted(
            Comparator.<Module>comparingInt(enabled -> enabled.getHudName().length())
               .reversed()
               .thenComparing(Module::getHudName, String.CASE_INSENSITIVE_ORDER)
         )
         .collect(Collectors.toList());
      float size = 10.0F * list.size();
      HudRenderer.WidgetBounds var11 = computeWidgetBounds(HudRenderer.Widget.MODULE_LIST, screenWidth, screenHeight, var8 * moduleListScale, size * moduleListScale);
      boolean var12 = var11.x + var11.width * 0.5F >= screenWidth * 0.5F;
      context.getMatrices().pushMatrix();
      context.getMatrices().translate(var11.x, var11.y);
      context.getMatrices().scale(moduleListScale, moduleListScale);

      for (int index = 0; index < list.size(); index++) {
         String hudName = ((Module)list.get(index)).getHudName();
         float var15 = index * 10.0F;
         if (var12) {
            drawTextRightAligned(context, hudName, var8, var15, alpha, 0.5F);
         } else {
            drawText(context, hudName, 0.0F, var15, alpha, 0.5F);
         }
      }

      context.getMatrices().popMatrix();
   }

   private static void drawText(DrawContext context, String text, float x, float y, float alpha, float scale){
      int textColor = ColorUtil.scaleAlpha(module.getTextColor(), alpha);
      RenderUtil.drawText(context, text, x, y, textColor, true, scale);
   }

   private static void drawTextRightAligned(DrawContext context, String text, float rightX, float y, float alpha, float scale){
      float textWidth = rightX - RenderUtil.getTextWidth(text, scale);
      drawText(context, text, textWidth, y, alpha, scale);
   }

   private static float getModuleListHeight(){
      long count = ModuleManager.INSTANCE.getModules().stream().filter(Module::isEnabled).count();
      return 10.0F * Math.max(1, (int)count);
   }

   private static float getModuleListWidth(){
      float hudName2 = 0.0F;

      for (Module module : ModuleManager.INSTANCE.getModules()) {
         if (module.isEnabled()) {
            float hudName = RenderUtil.getTextWidth(module.getHudName(), 0.5F);
            if (hudName > hudName2) {
               hudName2 = hudName;
            }
         }
      }

      return Math.max(hudName2, 10.0F);
   }

   private static float getInventoryWidgetHeight(){
      return 170.0F;
   }

   private static float getInventoryWidgetWidth(){
      return 62.0F;
   }

   private static float getStatsWidgetWidth(MinecraftClient client){
      ClientPlayerEntity player = client.player;
      double velocity = Math.sqrt(player.getVelocity().x * player.getVelocity().x + player.getVelocity().z * player.getVelocity().z)
         * 20.0;
      float textWidth = RenderUtil.getTextWidth("TPS " + formatFloat(tps), 0.58F);
      textWidth = Math.max(textWidth, RenderUtil.getTextWidth("FPS " + client.getCurrentFps(), 0.58F));
      textWidth = Math.max(textWidth, RenderUtil.getTextWidth("SPEED " + formatFloat(velocity) + " b/s", 0.58F));
      String z = "XYZ " + player.getBlockPos().getX() + " " + player.getBlockPos().getY() + " " + player.getBlockPos().getZ();
      return Math.max(textWidth, RenderUtil.getTextWidth(z, 0.58F));
   }

   private static HudRenderer.WidgetBounds computeWidgetBounds(HudRenderer.Widget widget, int screenWidth, int screenHeight, float width, float height){
      float statsX;
      float statsY;
      switch (widget) {
         case STATS:
            statsX = module.getStatsX();
            statsY = module.getStatsY();
            break;
         case INVENTORY:
            statsX = module.getInventoryX();
            statsY = module.getInventoryY();
            break;
         case PLAYER:
            statsX = module.getPlayerX();
            statsY = module.getPlayerY();
            break;
         case MODULE_LIST:
            statsX = module.getModuleListX();
            statsY = module.getModuleListY();
            break;
         default:
            statsX = 0.0F;
            statsY = 0.0F;
      }

      float max2 = Math.max(0.0F, screenWidth - width - 8.0F);
      float max = Math.max(0.0F, screenHeight - height - 8.0F);
      float var9 = 4.0F + clamp(statsX, 0.0F, 1.0F) * max2;
      float var10 = 4.0F + clamp(statsY, 0.0F, 1.0F) * max;
      return new HudRenderer.WidgetBounds(var9, var10, width, height);
   }

   public static boolean beginEditorDrag(double mouseX, double mouseY, int screenWidth, int screenHeight){
      HudRenderer.Widget var6 = getWidgetAt(mouseX, mouseY, screenWidth, screenHeight);
      if (var6 != HudRenderer.Widget.NONE && module != null) {
         selectedWidget = var6;
         editorDragStartX = mouseX;
         editorDragStartY = mouseY;
         selectedWidgetX = getWidgetWidth(var6);
         selectedWidgetY = getWidgetHeight(var6);
         return true;
      } else {
         return false;
      }
   }

   public static void updateEditorDrag(double mouseX, double mouseY, int screenWidth, int screenHeight){
      if (selectedWidget != HudRenderer.Widget.NONE && module != null) {
         float var6 = clamp(selectedWidgetX + (float)((mouseX - editorDragStartX) / screenWidth), 0.0F, 1.0F);
         float var7 = clamp(selectedWidgetY + (float)((mouseY - editorDragStartY) / screenHeight), 0.0F, 1.0F);
         switch (selectedWidget) {
            case STATS:
               module.setStatsPosition(var6, var7);
               break;
            case INVENTORY:
               module.setInventoryPosition(var6, var7);
               break;
            case PLAYER:
               module.setPlayerPosition(var6, var7);
               break;
            case MODULE_LIST:
               module.setModuleListPosition(var6, var7);
         }
      }
   }

   public static void endEditorDrag(){
      selectedWidget = HudRenderer.Widget.NONE;
   }

   public static boolean isEditorDragging(){
      return selectedWidget != HudRenderer.Widget.NONE;
   }

   private static HudRenderer.Widget getWidgetAt(double mouseX, double mouseY, int screenWidth, int screenHeight){
      if (module == null) {
         return HudRenderer.Widget.NONE;
      } else {
         MinecraftClient client = MinecraftClient.getInstance();
         HudRenderer.WidgetBounds var7 = computeWidgetBounds(
            HudRenderer.Widget.STATS, screenWidth, screenHeight, getStatsWidgetWidth(client) * module.getStatsScale(), 48.0F * module.getStatsScale()
         );
         HudRenderer.WidgetBounds var8 = computeWidgetBounds(
            HudRenderer.Widget.INVENTORY, screenWidth, screenHeight, getInventoryWidgetHeight() * module.getInventoryScale(), getInventoryWidgetWidth() * module.getInventoryScale()
         );
         HudRenderer.WidgetBounds var9 = computeWidgetBounds(
            HudRenderer.Widget.PLAYER, screenWidth, screenHeight, 70.0F * module.getPlayerScale(), 96.0F * module.getPlayerScale()
         );
         HudRenderer.WidgetBounds var10 = computeWidgetBounds(
            HudRenderer.Widget.MODULE_LIST, screenWidth, screenHeight, getModuleListWidth() * module.getModuleListScale(), getModuleListHeight() * module.getModuleListScale()
         );
         if (module.isStatsEnabled() && var7.contains(mouseX, mouseY)) {
            return HudRenderer.Widget.STATS;
         } else if (module.isInventoryEnabled() && var8.contains(mouseX, mouseY)) {
            return HudRenderer.Widget.INVENTORY;
         } else if (module.isPlayerEnabled() && var9.contains(mouseX, mouseY)) {
            return HudRenderer.Widget.PLAYER;
         } else {
            return module.isModuleListEnabled() && var10.contains(mouseX, mouseY) ? HudRenderer.Widget.MODULE_LIST : HudRenderer.Widget.NONE;
         }
      }
   }

   private static float getWidgetWidth(HudRenderer.Widget widget){
      return switch (widget) {
         case STATS -> module.getStatsX();
         case INVENTORY -> module.getInventoryX();
         case PLAYER -> module.getPlayerX();
         case MODULE_LIST -> module.getModuleListX();
         default -> 0.0F;
      };
   }

   private static float getWidgetHeight(HudRenderer.Widget widget){
      return switch (widget) {
         case STATS -> module.getStatsY();
         case INVENTORY -> module.getInventoryY();
         case PLAYER -> module.getPlayerY();
         case MODULE_LIST -> module.getModuleListY();
         default -> 0.0F;
      };
   }

   private static String formatFloat(double value){
      int max = (int)Math.round(Math.max(0.0, value) * 10.0);
      return max / 10 + "." + max % 10;
   }

   private static float clamp(float value, float min, float max){
      return Math.max(min, Math.min(max, value));
   }

   @Environment(EnvType.CLIENT)
   private static enum Widget {
      NONE,
      STATS,
      INVENTORY,
      PLAYER,
      MODULE_LIST;
   }

   @Environment(EnvType.CLIENT)
   private record WidgetBounds(float x, float y, float width, float height){
      private boolean contains(double pointX, double pointY){
         return pointX >= this.x && pointX <= this.x + this.width && pointY >= this.y && pointY <= this.y + this.height;
      }
   }
}

