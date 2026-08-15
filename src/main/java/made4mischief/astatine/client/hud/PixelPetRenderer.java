package made4mischief.astatine.client.hud;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.ThreadLocalRandom;
import made4mischief.astatine.Astatine;
import made4mischief.astatine.client.modules.ModuleManager;
import made4mischief.astatine.client.modules.render.PixelPetModule;
import made4mischief.astatine.client.utils.render.RenderUtil;
import made4mischief.astatine.client.utils.render.renderer.text.TextRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.RenderTickCounter;

@Environment(EnvType.CLIENT)
public final class PixelPetRenderer {
   private static final int HOP_THRESHOLD = 3;
   private static final int BODY_HEIGHT = 19;
   private static final int SPEECH_MIN_WIDTH = 20;
   private static final int PET_WIDTH = 57;
   private static final int PET_HEIGHT = 60;
   private static final int EDITOR_PADDING = 4;
   private static final int HEAD_OFFSET = 28;
   private static final int BOTTOM_MARGIN = 4;
   private static final float TEXT_SCALE = 0.65F;
   private static final int AVATAR_WIDTH = 8;
   private static final long MIN_SPEECH_DURATION = 250L;
   private static final long MAX_SPEECH_DURATION = 30000L;
   private static final int BLINK_CHANCE = 30;
   private static final int OUTLINE_COLOR = -14607818;
   private static final int BODY_COLOR = -6985252;
   private static final int HIGHLIGHT_COLOR = -10140514;
   private static final int MOUTH_COLOR = -6224;
   private static final int EYE_COLOR = -11803150;
   private static final int PUPIL_COLOR = -5899521;
   private static final int NOSE_COLOR = -31051;
   private static PixelPetModule module;
   private static boolean initialized;
   private static boolean dragging;
   private static double dragOffsetX;
   private static double dragOffsetY;
   private static Object lastNetworkHandler;
   private static boolean greeted;
   private static final Object speechLock = new Object();
   private static final PriorityQueue<PixelPetRenderer.SpeechRequest> speechQueue = new PriorityQueue<>(
      Comparator.comparingInt(PixelPetRenderer.SpeechRequest::priority).reversed().thenComparingLong(PixelPetRenderer.SpeechRequest::sequence)
   );
   private static PixelPetRenderer.ActiveSpeech activeSpeech;
   private static long lastGreetingTime;

   private PixelPetRenderer(){
   }

   public static void init(){
      if (!initialized) {
         module = ModuleManager.INSTANCE.getModule(PixelPetModule.class);
         HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR, Astatine.id("pixel_pet"), PixelPetRenderer::render);
         initialized = true;
      }
   }

   public static void speak(String message, long durationMillis){
      speak(message, durationMillis, PetExpression.NORMAL, 0);
   }

   public static void speak(String message, long durationMillis, PetExpression expression){
      speak(message, durationMillis, expression, 0);
   }

   public static void speak(String message, long durationMillis, PetExpression expression, int priority){
      if (message != null && !message.isBlank()) {
         String strip = message.strip();
         long min = Math.max(250L, Math.min(30000L, durationMillis));
         PetExpression petExpression = expression == null ? PetExpression.NORMAL : expression;
         synchronized (speechLock) {
            long nanoTime = System.nanoTime();
            advanceSpeech(nanoTime);
            PixelPetRenderer.SpeechRequest var9 = new PixelPetRenderer.SpeechRequest(strip, min * 1000000L, petExpression, priority, lastGreetingTime++);
            if (activeSpeech == null) {
               activeSpeech = var9.activate(nanoTime);
            } else if (priority > activeSpeech.request().priority()) {
               activeSpeech = var9.activate(nanoTime);
            } else {
               if (speechQueue.size() < 8) {
                  speechQueue.offer(var9);
               }
            }
         }
      }
   }

   public static void clearSpeech(){
      synchronized (speechLock) {
         activeSpeech = null;
         speechQueue.clear();
      }
   }

   public static boolean isSpeaking(){
      return getActiveSpeech() != null;
   }

   private static void render(DrawContext context, RenderTickCounter tickCounter){
      MinecraftClient client = MinecraftClient.getInstance();
      sweepExpiredSpeeches();
      handlePlayerConnection(client);
      if (module != null && module.isEnabled() && client.player != null && client.world != null) {
         PixelPetRenderer.Bounds var3 = computeBounds(context.getScaledWindowWidth(), context.getScaledWindowHeight());
         drawPet(context, var3.x(), var3.y(), false);
      }
   }

   public static void renderEditorPreview(DrawContext context){
      sweepExpiredSpeeches();
      if (module != null) {
         PixelPetRenderer.Bounds var1 = computeBounds(context.getScaledWindowWidth(), context.getScaledWindowHeight());
         drawPet(context, var1.x(), var1.y(), true);
      }
   }

   public static boolean beginEditorDrag(double mouseX, double mouseY, int screenWidth, int screenHeight){
      sweepExpiredSpeeches();
      if (module == null) {
         return false;
      } else {
         PixelPetRenderer.Bounds var6 = computeBounds(screenWidth, screenHeight);
         if (!var6.contains(mouseX, mouseY)) {
            return false;
         } else {
            dragging = true;
            dragOffsetX = mouseX - var6.x();
            dragOffsetY = mouseY - var6.y();
            return true;
         }
      }
   }

   public static void updateEditorDrag(double mouseX, double mouseY, int screenWidth, int screenHeight){
      if (dragging && module != null) {
         float max2 = Math.max(0.0F, screenWidth - 57 - 8.0F);
         float max = Math.max(0.0F, (float)(screenHeight - 60 - 28 - 4));
         float var8 = (float)mouseX - (float)dragOffsetX;
         float var9 = (float)mouseY - (float)dragOffsetY;
         float var10 = max2 <= 0.0F ? 0.0F : (var8 - 4.0F) / max2;
         float var11 = max <= 0.0F ? 0.0F : (var9 - 28.0F) / max;
         module.setPosition(var10, var11);
      }
   }

   public static void endEditorDrag(){
      dragging = false;
   }

   public static boolean isEditorDragging(){
      return dragging;
   }

   private static void drawPet(DrawContext context, float x, float y, boolean editor){
      int round2 = Math.round(x);
      int round = Math.round(y);
      PixelPetRenderer.ActiveSpeech var6 = getActiveSpeech();
      PetExpression petExpression = var6 == null ? PetExpression.NORMAL : var6.request().expression();
      PixelPetRenderer.AnimationFrame var8 = getAnimationFrame(petExpression);
      int hop = round - var8.hop();
      int breathe = hop - var8.breathe();
      drawShadow(context, round2, round, var8.hop());
      drawTail(context, round2, hop, var8.tail());
      drawBody(context, round2, hop);
      drawHead(context, round2, breathe, var8);
      drawSpeechBubble(context, round2, breathe, context.getScaledWindowWidth(), var6);
      if (editor) {
         drawEditorOutline(context, round2, round);
      }
   }

   private static void drawShadow(DrawContext context, int baseX, int baseY, int hop){
      if (hop >= 3) {
         rect(context, baseX, baseY, 7, 18, 6, 1, 974590500);
         rect(context, baseX, baseY, 8, 19, 4, 1, 538382884);
      } else if (hop > 0) {
         rect(context, baseX, baseY, 5, 18, 10, 1, 1108808228);
         rect(context, baseX, baseY, 7, 19, 6, 1, 605491748);
      } else {
         rect(context, baseX, baseY, 4, 18, 12, 1, 1243025956);
         rect(context, baseX, baseY, 6, 19, 8, 1, 672600612);
      }
   }

   private static void drawTail(DrawContext context, int baseX, int baseY, int frame){
      rect(context, baseX, baseY, 14, 12, 3, 2, -14607818);
      rect(context, baseX, baseY, 15, 12, 2, 1, -10140514);
      if (frame == 0) {
         rect(context, baseX, baseY, 16, 11, 3, 3, -14607818);
         rect(context, baseX, baseY, 17, 12, 1, 1, -11803150);
      } else if (frame == 1) {
         rect(context, baseX, baseY, 16, 9, 3, 5, -14607818);
         rect(context, baseX, baseY, 17, 10, 1, 3, -11803150);
      } else {
         rect(context, baseX, baseY, 15, 8, 3, 5, -14607818);
         rect(context, baseX, baseY, 16, 9, 1, 3, -11803150);
      }
   }

   private static void drawBody(DrawContext context, int baseX, int baseY){
      rect(context, baseX, baseY, 5, 10, 10, 8, -14607818);
      rect(context, baseX, baseY, 6, 10, 8, 7, -6985252);
      rect(context, baseX, baseY, 7, 11, 6, 5, -10140514);
      rect(context, baseX, baseY, 4, 16, 5, 3, -14607818);
      rect(context, baseX, baseY, 11, 16, 5, 3, -14607818);
      rect(context, baseX, baseY, 5, 17, 3, 1, -6224);
      rect(context, baseX, baseY, 12, 17, 3, 1, -6224);
   }

   private static void drawHead(DrawContext context, int baseX, int baseY, PixelPetRenderer.AnimationFrame frame){
      drawFace(context, baseX, baseY, frame, 0);
   }

   private static void drawFace(DrawContext context, int baseX, int baseY, PixelPetRenderer.AnimationFrame frame, int lookDirection){
      rect(context, baseX, baseY, 2, 2, 5, 5, -14607818);
      rect(context, baseX, baseY, 3, 3, 3, 3, -6985252);
      rect(context, baseX, baseY - frame.earTwitch(), 13, 2, 5, 5, -14607818);
      rect(context, baseX, baseY - frame.earTwitch(), 14, 3, 3, 3, -6985252);
      rect(context, baseX, baseY, 4, 4, 12, 9, -14607818);
      rect(context, baseX, baseY, 5, 4, 10, 8, -6985252);
      rect(context, baseX, baseY, 6, 5, 8, 6, -6224);
      rect(context, baseX, baseY, 5, 7, 10, 3, -6224);
      if (frame.expression() == PetExpression.HAPPY) {
         drawPixel(context, baseX, baseY, 7, 8, -11803150);
         drawPixel(context, baseX, baseY, 8, 7, -5899521);
         drawPixel(context, baseX, baseY, 11, 7, -5899521);
         drawPixel(context, baseX, baseY, 12, 8, -11803150);
      } else if (frame.expression() == PetExpression.ALERT) {
         rect(context, baseX, baseY, 7, 7, 2, 2, -5899521);
         rect(context, baseX, baseY, 11, 7, 2, 2, -5899521);
         drawPixel(context, baseX, baseY, 8, 8, -11803150);
         drawPixel(context, baseX, baseY, 12, 8, -11803150);
      } else if (frame.expression() == PetExpression.WORRIED) {
         drawPixel(context, baseX, baseY, 7, 7, -5899521);
         drawPixel(context, baseX, baseY, 8, 8, -11803150);
         drawPixel(context, baseX, baseY, 11, 8, -11803150);
         drawPixel(context, baseX, baseY, 12, 7, -5899521);
      } else if (frame.blink()) {
         rect(context, baseX, baseY, 7, 8, 2, 1, -10140514);
         rect(context, baseX, baseY, 11, 8, 2, 1, -10140514);
      } else {
         rect(context, baseX, baseY, 7, 7, 2, 2, -11803150);
         rect(context, baseX, baseY, 11, 7, 2, 2, -11803150);
         if (lookDirection == 0) {
            drawPixel(context, baseX, baseY, 7, 7, -5899521);
            drawPixel(context, baseX, baseY, 11, 7, -5899521);
         } else {
            int var5 = lookDirection < 0 ? 0 : 1;
            drawPixel(context, baseX, baseY, 7 + var5, 8, -14607818);
            drawPixel(context, baseX, baseY, 11 + var5, 8, -14607818);
         }
      }

      drawPixel(context, baseX, baseY, 10, 9, -31051);
      if (frame.expression() == PetExpression.HAPPY) {
         drawPixel(context, baseX, baseY, 9, 9, -14607818);
         drawPixel(context, baseX, baseY, 11, 9, -14607818);
         drawPixel(context, baseX, baseY, 10, 10, -31051);
      } else if (frame.expression() == PetExpression.ALERT) {
         drawPixel(context, baseX, baseY, 10, 10, -14607818);
      } else if (frame.expression() == PetExpression.WORRIED) {
         drawPixel(context, baseX, baseY, 9, 10, -14607818);
         drawPixel(context, baseX, baseY, 10, 9, -14607818);
         drawPixel(context, baseX, baseY, 11, 10, -14607818);
      } else {
         drawPixel(context, baseX, baseY, 9, 10, -14607818);
         drawPixel(context, baseX, baseY, 11, 10, -14607818);
      }

      rect(context, baseX, baseY, 7, 11, 6, 2, -14607818);
      rect(context, baseX, baseY, 8, 11, 4, 1, -11803150);
      drawPixel(context, baseX, baseY, 10, 1, -14607818);
      rect(context, baseX, baseY, 9, 0, 3, 2, -14607818);
      drawPixel(context, baseX, baseY, 10, 0, frame.crystalPulse() ? -5899521 : -11803150);
      drawPixel(context, baseX, baseY, 9, 1, -11803150);
      drawPixel(context, baseX, baseY, 11, 1, -6985252);
      if (frame.expression() != PetExpression.NORMAL && frame.crystalPulse()) {
         drawPixel(context, baseX, baseY, 7, 0, -5899521);
         drawPixel(context, baseX, baseY, 13, 1, -11803150);
      }
   }

   private static void drawSpeechBubble(DrawContext context, int baseX, int headY, int screenWidth, PixelPetRenderer.ActiveSpeech speech){
      if (speech != null) {
         String max2 = truncateMessage(speech.request().message(), Math.max(20, screenWidth - 8 - 10));
         int measureWidth = Math.round(TextRenderer.measureWidth(max2, 0.65F));
         int measureHeight = Math.round(TextRenderer.measureHeight(0.65F));
         int var8 = measureWidth + 10;
         int var9 = measureHeight + 8;
         int var10 = baseX + 28;
         int min2 = var10 - var8 / 2;
         min2 = Math.max(4, Math.min(min2, screenWidth - 4 - var8));
         int max = Math.max(2, headY - var9 - 5);
         int var13 = min2 + var8;
         int var14 = max + var9;
         context.fill(min2, max, var13, var14, -14607818);
         context.fill(min2 + 1, max + 1, var13 - 1, var14 - 1, -298506160);
         int expression = speech.request().expression() == PetExpression.ALERT ? -31051 : -6985252;
         context.fill(min2 + 2, max + 2, var13 - 2, max + 3, expression);
         int min = Math.max(min2 + 5, Math.min(var10, var13 - 5));
         context.fill(min - 3, var14, min + 4, var14 + 2, -14607818);
         context.fill(min - 1, var14 + 2, min + 2, var14 + 4, -14607818);
         context.fill(min - 2, var14, min + 3, var14 + 1, -10140514);
         RenderUtil.drawText(context, max2, min2 + 5, max + 4, -6224, false, 0.65F);
      }
   }

   private static String truncateMessage(String message, int maxWidth){
      if (TextRenderer.measureWidth(message, 0.65F) <= maxWidth) {
         return message;
      } else {
         String var2 = "...";
         int index = message.length();

         while (index > 0) {
            String stripTrailing = message.substring(0, index).stripTrailing() + var2;
            if (TextRenderer.measureWidth(stripTrailing, 0.65F) <= maxWidth) {
               return stripTrailing;
            }

            index--;
            if (index > 0 && Character.isLowSurrogate(message.charAt(index))) {
               index--;
            }
         }

         return var2;
      }
   }

   private static void drawEditorOutline(DrawContext context, int baseX, int baseY){
      int var3 = dragging ? -649337358 : 2056612316;
      int var4 = baseX - 2;
      int var5 = baseY - 2;
      int var6 = baseX + 57 + 2;
      int var7 = baseY + 60 + 2;
      context.fill(var4, var5, var6, var5 + 1, var3);
      context.fill(var4, var7 - 1, var6, var7, var3);
      context.fill(var4, var5 + 1, var4 + 1, var7 - 1, var3);
      context.fill(var6 - 1, var5 + 1, var6, var7 - 1, var3);
   }

   private static PixelPetRenderer.AnimationFrame getAnimationFrame(PetExpression expression){
      if (module != null && module.hasAnimations()) {
         long animationSpeed = (long)(System.nanoTime() / 1000000.0 * module.getAnimationSpeed());
         long var3 = animationSpeed % 9000L;
         int sin = 0;
         if (var3 >= 7000L && var3 < 7700L) {
            double var6 = (var3 - 7000L) / 700.0;
            sin = (int)Math.round(Math.sin(var6 * Math.PI) * 4.0);
         }

         long var16 = animationSpeed % 4300L;
         boolean var8 = var16 >= 3500L && var16 < 3640L || var16 >= 3820L && var16 < 3920L;
         long var9 = animationSpeed % 5700L;
         int var11 = (var9 < 4780L || var9 >= 4900L) && (var9 < 5020L || var9 >= 5140L) ? 0 : 1;
         int var12 = (int)(animationSpeed / 260L % 4L);
         int var13 = var12 == 3 ? 1 : var12;
         int var14 = animationSpeed / 720L % 2L == 0L ? 0 : 1;
         boolean var15 = animationSpeed / 360L % 2L == 0L;
         return new PixelPetRenderer.AnimationFrame(var14, sin, var13, var8, var11, var15, expression);
      } else {
         return PixelPetRenderer.AnimationFrame.staticFrame(expression);
      }
   }

   private static void handlePlayerConnection(MinecraftClient client){
      ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
      if (networkHandler != null && client.player != null) {
         if (networkHandler != lastNetworkHandler) {
            lastNetworkHandler = networkHandler;
            greeted = false;
            clearSpeech();
         }

         if (!greeted && module != null && module.isEnabled()) {
            greeted = true;
            if (ThreadLocalRandom.current().nextInt(100) < 30) {
               speak("ChÃ o má»«ng trá»Ÿ láº¡i, " + client.player.getName().getString() + "!", 5000L, PetExpression.HAPPY, -10);
            }
         }
      } else {
         lastNetworkHandler = null;
         greeted = false;
         clearSpeech();
      }
   }

   private static PixelPetRenderer.ActiveSpeech getActiveSpeech(){
      synchronized (speechLock) {
         advanceSpeech(System.nanoTime());
         return activeSpeech;
      }
   }

   private static void advanceSpeech(long now){
      if (activeSpeech == null || now >= activeSpeech.untilNanos()) {
         PixelPetRenderer.SpeechRequest var2 = speechQueue.poll();
         activeSpeech = var2 == null ? null : var2.activate(now);
      }
   }

   private static void drawPixel(DrawContext context, int baseX, int baseY, int x, int y, int color){
      rect(context, baseX, baseY, x, y, 1, 1, color);
   }

   private static void rect(DrawContext context, int baseX, int baseY, int x, int y, int width, int height, int color){
      int var8 = baseX + x * 3;
      int var9 = baseY + y * 3;
      context.fill(var8, var9, var8 + width * 3, var9 + height * 3, color);
   }

   private static PixelPetRenderer.Bounds computeBounds(int screenWidth, int screenHeight){
      float max2 = Math.max(0.0F, screenWidth - 57 - 8.0F);
      float max = Math.max(0.0F, (float)(screenHeight - 60 - 28 - 4));
      float positionX = 4.0F + module.getPositionX() * max2;
      float positionY = 28.0F + module.getPositionY() * max;
      return new PixelPetRenderer.Bounds(positionX, positionY, 57.0F, 60.0F);
   }

   private static void sweepExpiredSpeeches(){
      if (module == null) {
         module = ModuleManager.INSTANCE.getModule(PixelPetModule.class);
      }
   }

   @Environment(EnvType.CLIENT)
   private record ActiveSpeech(PixelPetRenderer.SpeechRequest request, long untilNanos){
   }

   @Environment(EnvType.CLIENT)
   private record AnimationFrame(int breathe, int hop, int tail, boolean blink, int earTwitch, boolean crystalPulse, PetExpression expression){
      private static PixelPetRenderer.AnimationFrame staticFrame(PetExpression expression){
         return new PixelPetRenderer.AnimationFrame(0, 0, 1, false, 0, true, expression);
      }
   }

   @Environment(EnvType.CLIENT)
   private record Bounds(float x, float y, float width, float height){
      private boolean contains(double pointX, double pointY){
         return pointX >= this.x && pointX <= this.x + this.width && pointY >= this.y && pointY <= this.y + this.height;
      }
   }

   @Environment(EnvType.CLIENT)
   private record SpeechRequest(String message, long durationNanos, PetExpression expression, int priority, long sequence){
      private PixelPetRenderer.ActiveSpeech activate(long now){
         return new PixelPetRenderer.ActiveSpeech(this, now + this.durationNanos);
      }
   }
}

