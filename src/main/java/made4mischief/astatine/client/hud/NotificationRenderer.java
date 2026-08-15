package made4mischief.astatine.client.hud;

import java.util.ArrayList;
import java.util.List;
import made4mischief.astatine.Astatine;
import made4mischief.astatine.client.gui.component.theme.Theme;
import made4mischief.astatine.client.gui.component.theme.ThemeManager;
import made4mischief.astatine.client.utils.render.RenderUtil;
import made4mischief.astatine.client.utils.render.core.ColorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

@Environment(EnvType.CLIENT)
public final class NotificationRenderer {
   private static final float NOTIFICATION_HEIGHT = 16.0F;
   private static final float NOTIFICATION_MIN_WIDTH = 82.0F;
   private static final float NOTIFICATION_MAX_WIDTH = 122.0F;
   private static final float NOTIFICATION_LEFT_BAR_Y = 3.5F;
   private static final float NOTIFICATION_BOTTOM_MARGIN = 22.0F;
   private static final float NOTIFICATION_BOTTOM_GAP = 10.0F;
   private static final float MODULE_TEXT_SCALE = 0.43F;
   private static final float STATE_TEXT_SCALE = 0.29F;
   private static final float NOTIFICATION_GAP = 2.0F;
   private static final long SLIDE_IN_DURATION_MS = 220L;
   private static final long HOLD_DURATION_MS = 1650L;
   private static final long SLIDE_OUT_DURATION_MS = 260L;
   private static final long NOTIFICATION_LIFETIME_MS = 2130L;
   private static final int MAX_NOTIFICATIONS = 4;
   private static final int ENABLED_COLOR = -12388959;
   private static final int DISABLED_COLOR = -41613;
   private static final float ALERT_HEIGHT = 26.0F;
   private static final float ALERT_MIN_WIDTH = 140.0F;
   private static final float ALERT_MAX_WIDTH = 200.0F;
   private static final float ALERT_LEFT_BAR_Y = 4.0F;
   private static final float ALERT_SCREEN_MARGIN = 8.0F;
   private static final float ALERT_RIGHT_MARGIN = 6.0F;
   private static final float ALERT_TITLE_SCALE = 0.45F;
   private static final float ALERT_DESC_SCALE = 0.34F;
   private static final float ALERT_CIRCLE_RADIUS = 3.0F;
   private static final float ALERT_TITLE_Y = 5.0F;
   private static final float ALERT_DESC_Y = 15.0F;
   private static final float ALERT_CIRCLE_X = 9.0F;
   private static final float ALERT_TEXT_X = 17.0F;
   private static final long ALERT_SLIDE_IN_MS = 250L;
   private static final long ALERT_HOLD_MS = 3500L;
   private static final long ALERT_SLIDE_OUT_MS = 350L;
   private static final long ALERT_LIFETIME_MS = 4100L;
   private static final int MAX_ALERTS = 3;
   private static final List<NotificationRenderer.Notification> notifications = new ArrayList<>();
   private static final List<NotificationRenderer.AlertNotification> alerts = new ArrayList<>();
   private static long lastFrameTimeNanos;
   private static boolean registered;

   private NotificationRenderer(){
   }

   public static void init(){
      if (!registered) {
         HudElementRegistry.attachElementAfter(Astatine.id("astatine_hud"), Astatine.id("notifications"), NotificationRenderer::render);
         registered = true;
      }
   }

   public static void showModuleState(String moduleName, boolean enabled){
      if (moduleName != null && !moduleName.isBlank()) {
         while (notifications.size() >= 4) {
            notifications.remove(0);
         }

         notifications.add(new NotificationRenderer.Notification(moduleName, enabled, System.nanoTime()));
      }
   }

   public static void showAlert(String title, String description, NotificationRenderer.NotificationType type){
      if (title != null && !title.isBlank()) {
         while (alerts.size() >= 3) {
            alerts.remove(0);
         }

         alerts.add(new NotificationRenderer.AlertNotification(title, description != null ? description : "", type, System.nanoTime()));
      }
   }

   private static void render(DrawContext context, RenderTickCounter tickCounter){
      if (MinecraftClient.getInstance().currentScreen == null) {
         renderOverlay(context);
      }
   }

   public static void renderOverlay(DrawContext context){
      long nanoTime = System.nanoTime();
      float var3 = getNotificationLifetime(nanoTime);
      sweepExpiredNotifications(nanoTime);
      renderNotifications(context, nanoTime);
      if (!notifications.isEmpty()) {
         float scaledWindowWidth = context.getScaledWindowWidth() / 2.0F;
         float scaledWindowHeight = context.getScaledWindowHeight() - 22.0F - 10.0F - 16.0F;
         int index2 = 0;

         for (int index = notifications.size() - 1; index >= 0; index--) {
            NotificationRenderer.Notification var8 = notifications.get(index);
            float var9 = scaledWindowHeight - index2 * 19.5F;
            var8.animateToY(var9, var3);
            renderNotification(context, var8, scaledWindowWidth, nanoTime);
            index2++;
         }
      }
   }

   private static void renderNotifications(DrawContext context, long currentTimeNanos){
      if (!alerts.isEmpty()) {
         int scaledWindowWidth = context.getScaledWindowWidth();
         int scaledWindowHeight = context.getScaledWindowHeight();
         float var5 = 8.0F;

         for (int index = alerts.size() - 1; index >= 0; index--) {
            NotificationRenderer.AlertNotification var7 = alerts.get(index);
            long ageMs = var7.getAgeMs(currentTimeNanos);
            if (ageMs < 4100L) {
               float var12 = 1.0F;
               float var10;
               float var11;
               if (ageMs < 250L) {
                  float var13 = (float)ageMs / 250.0F;
                  float var14 = easeOut(var13);
                  var10 = var14;
                  var11 = (1.0F - var14) * 200.0F;
                  var12 = 0.94F + 0.06F * var14;
               } else if (ageMs < 3750L) {
                  var10 = 1.0F;
                  var11 = 0.0F;
               } else {
                  float var24 = (float)(ageMs - 250L - 3500L);
                  float var26 = var24 / 350.0F;
                  float var15 = easeIn(ease(var26));
                  var10 = 1.0F - var15;
                  var11 = -5.0F * var15;
               }

               if (!(var10 <= 0.001F)) {
                  String var25 = var7.description;
                  float textWidth = RenderUtil.getTextWidth(var7.title, 0.45F);
                  float textWidth2 = var25.isEmpty() ? 0.0F : RenderUtil.getTextWidth(var25, 0.34F);
                  float max = 17.0F + Math.max(textWidth, textWidth2) + 6.0F;
                  float var17 = clamp(max, 140.0F, 200.0F);
                  float var18 = scaledWindowWidth - var17 - 6.0F + var11;
                  float var19 = var5 + index * 30.0F;
                  context.getMatrices().pushMatrix();
                  context.getMatrices().translate(var18, var19);
                  context.getMatrices().scale(var12, var12);
                  int scaleAlpha = ColorUtil.scaleAlpha(var7.type.color, 0.42F * var10);
                  int surface = ColorUtil.scaleAlpha(ColorUtil.withAlpha(ThemeManager.active().surface(), 232), var10);
                  int text = ColorUtil.scaleAlpha(ThemeManager.active().text(), var10);
                  int textDim = ColorUtil.scaleAlpha(ThemeManager.active().textDim(), var10);
                  RenderUtil.drawBo(context, -0.5F, -0.5F, var17 + 1.0F, 27.0F, 5.0F, scaleAlpha);
                  RenderUtil.drawBo(context, 0.0F, 0.0F, var17, 26.0F, 4.5F, surface);
                  RenderUtil.drawBo(context, 0.0F, 4.0F, 1.5F, 18.0F, 0.75F, ColorUtil.scaleAlpha(var7.type.color, var10));
                  RenderUtil.drawCircle(context, 9.0F, 13.0F, 3.0F, ColorUtil.scaleAlpha(var7.type.color, var10));
                  RenderUtil.drawText(context, var7.title, 17.0F, 5.0F, text, true, 0.45F);
                  if (!var25.isEmpty()) {
                     RenderUtil.drawText(context, var25, 17.0F, 15.0F, textDim, false, 0.34F);
                  }

                  context.getMatrices().popMatrix();
               }
            }
         }
      }
   }

   private static void renderNotification(DrawContext context, NotificationRenderer.Notification notification, float screenCenterX, long currentTimeNanoseconds){
      long elapsedMs = notification.elapsedMs(currentTimeNanoseconds);
      NotificationRenderer.AnimationFrame var7 = getAnimationFrame(elapsedMs);
      Theme theme = ThemeManager.active();
      String var9 = notification.enabled ? "ENABLED" : "DISABLED";
      int var10 = notification.enabled ? -12388959 : -41613;
      float textWidth2 = RenderUtil.getTextWidth(var9, 0.29F);
      float var12 = 122.0F - textWidth2 - 27.5F;
      String var13 = truncateText(notification.moduleName, var12, 0.43F);
      float textWidth = 13.5F + RenderUtil.getTextWidth(var13, 0.43F) + 10.0F + textWidth2 + 6.0F;
      float var15 = clamp(textWidth, 82.0F, 122.0F);
      float var16 = notification.animatedY + var7.verticalOffset;
      float var17 = 0.86F;
      float var18 = 0.58F;
      float textHeight = (16.0F - RenderUtil.getTextHeight(0.43F) - 0.43F) / 2.0F - var17;
      float textHeight2 = (16.0F - RenderUtil.getTextHeight(0.29F)) / 2.0F - var18;
      context.getMatrices().pushMatrix();
      context.getMatrices().translate(screenCenterX, var16 + 8.0F);
      context.getMatrices().scale(var7.scale, var7.scale);
      context.getMatrices().translate(-var15 / 2.0F, -8.0F);
      int scaleAlpha2 = ColorUtil.scaleAlpha(var10, 0.42F * var7.opacity);
      int surface = ColorUtil.scaleAlpha(ColorUtil.withAlpha(theme.surface(), 232), var7.opacity);
      int text = ColorUtil.scaleAlpha(theme.text(), var7.opacity);
      int scaleAlpha = ColorUtil.scaleAlpha(var10, var7.opacity);
      RenderUtil.drawBo(context, -0.5F, -0.5F, var15 + 1.0F, 17.0F, 5.0F, scaleAlpha2);
      RenderUtil.drawBo(context, 0.0F, 0.0F, var15, 16.0F, 4.5F, surface);
      RenderUtil.drawBo(context, 0.0F, 3.5F, 1.5F, 9.0F, 0.75F, scaleAlpha);
      RenderUtil.drawCircle(context, 8.0F, 8.0F, 1.75F, scaleAlpha);
      RenderUtil.drawText(context, var13, 13.5F - var17, textHeight, text, true, 0.43F);
      RenderUtil.drawRightAlignedText(context, var9, var15 - 6.0F - var18, textHeight2, scaleAlpha, false, 0.29F);
      context.getMatrices().popMatrix();
   }

   private static NotificationRenderer.AnimationFrame getAnimationFrame(long ageMilliseconds){
      if (ageMilliseconds < 220L) {
         float var6 = (float)ageMilliseconds / 220.0F;
         float var3 = easeOut(var6);
         return new NotificationRenderer.AnimationFrame(var3, 7.0F * (1.0F - var3), 0.94F + 0.06F * var3);
      } else {
         long var2 = 1870L;
         if (ageMilliseconds < var2) {
            return new NotificationRenderer.AnimationFrame(1.0F, 0.0F, 1.0F);
         } else {
            float var4 = (float)(ageMilliseconds - var2) / 260.0F;
            float var5 = easeIn(ease(var4));
            return new NotificationRenderer.AnimationFrame(1.0F - var5, -5.0F * var5, 1.0F - 0.04F * var5);
         }
      }
   }

   private static float getNotificationLifetime(long currentTimeNanoseconds){
      if (lastFrameTimeNanos == 0L) {
         lastFrameTimeNanos = currentTimeNanoseconds;
         return 0.0F;
      } else {
         float var2 = (float)(currentTimeNanoseconds - lastFrameTimeNanos) / 1.0E9F;
         lastFrameTimeNanos = currentTimeNanoseconds;
         return Math.min(var2, 0.1F);
      }
   }

   private static void sweepExpiredNotifications(long currentTimeNanoseconds){
      for (int index2 = notifications.size() - 1; index2 >= 0; index2--) {
         NotificationRenderer.Notification var3 = notifications.get(index2);
         if (var3.elapsedMs(currentTimeNanoseconds) >= 2130L) {
            notifications.remove(index2);
         }
      }

      for (int index = alerts.size() - 1; index >= 0; index--) {
         if (alerts.get(index).getAgeMs(currentTimeNanoseconds) >= 4100L) {
            alerts.remove(index);
         }
      }
   }

   private static String truncateText(String text, float maximumWidth, float scale){
      if (RenderUtil.getTextWidth(text, scale) <= maximumWidth) {
         return text;
      } else {
         String var3 = "...";

         for (int index = text.length() - 1; index > 0; index--) {
            String substring = text.substring(0, index) + var3;
            if (RenderUtil.getTextWidth(substring, scale) <= maximumWidth) {
               return substring;
            }
         }

         return var3;
      }
   }

   private static float easeOut(float progress){
      float var1 = 1.0F - ease(progress);
      return 1.0F - var1 * var1 * var1;
   }

   private static float easeIn(float progress){
      float var1 = ease(progress);
      return var1 * var1 * var1;
   }

   private static float ease(float value){
      return Math.max(0.0F, Math.min(1.0F, value));
   }

   private static float clamp(float value, float minimum, float maximum){
      return Math.max(minimum, Math.min(maximum, value));
   }

   @Environment(EnvType.CLIENT)
   private record AlertNotification(String title, String description, NotificationRenderer.NotificationType type, long creationTimeNanos){
      private long getAgeMs(long now){
         return (now - this.creationTimeNanos) / 1000000L;
      }
   }

   @Environment(EnvType.CLIENT)
   private record AnimationFrame(float opacity, float verticalOffset, float scale){
      public float getY(){
         return this.verticalOffset;
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Notification {
      private final String moduleName;
      private final boolean enabled;
      private final long creationTimeNanos;
      private float animatedY = Float.NaN;

      private Notification(String moduleName, boolean enabled, long creationTimeNanoseconds){
         this.moduleName = moduleName;
         this.enabled = enabled;
         this.creationTimeNanos = creationTimeNanoseconds;
      }

      private long elapsedMs(long currentTimeNanoseconds){
         return (currentTimeNanoseconds - this.creationTimeNanos) / 1000000L;
      }

      private void animateToY(float targetY, float frameDeltaSeconds){
         if (Float.isNaN(this.animatedY)) {
            this.animatedY = targetY;
         } else {
            float exp = 1.0F - (float)Math.exp(-18.0F * frameDeltaSeconds);
            this.animatedY = this.animatedY + (targetY - this.animatedY) * exp;
         }
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum NotificationType {
      INFO(-12409355),
      WARNING(-22746),
      STASH(-12388959),
      ALERT(-1092784);

      public final int color;

      private NotificationType(int color){
         this.color = color;
      }
   }
}

