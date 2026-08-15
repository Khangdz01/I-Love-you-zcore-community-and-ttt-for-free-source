package made4mischief.astatine.client.utils.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.render.Camera;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public final class ScreenTracerRenderer {
   private static final float SCREEN_MARGIN = 2.0F;
   private final Vector3f forward = new Vector3f();
   private final Vector3f right = new Vector3f();
   private final Vector3f up = new Vector3f();
   private Vec3d cameraPos = Vec3d.ZERO;
   private float centerX;
   private float centerY;
   private float screenWidth;
   private float screenHeight;
   private boolean active;

   public boolean begin(DrawContext context, MinecraftClient client){
      this.active = client != null && client.world != null && client.player != null;
      if (!this.active) {
         return false;
      } else {
         Camera camera = client.gameRenderer.getCamera();
         this.cameraPos = camera.getCameraPos();
         Quaternionf quaternion = camera.getRotation();
         this.forward.set(0.0F, 0.0F, -1.0F).rotate(quaternion);
         this.right.set(1.0F, 0.0F, 0.0F).rotate(quaternion);
         this.up.set(0.0F, 1.0F, 0.0F).rotate(quaternion);
         this.screenWidth = context.getScaledWindowWidth();
         this.screenHeight = context.getScaledWindowHeight();
         this.centerX = this.screenWidth * 0.5F;
         this.centerY = this.screenHeight * 0.5F;
         return true;
      }
   }

   public void draw(DrawContext context, MinecraftClient client, Vec3d target, float width, int color){
      if (this.active && target != null) {
         double var6 = target.x - this.cameraPos.x;
         double var8 = target.y - this.cameraPos.y;
         double var10 = target.z - this.cameraPos.z;
         double var12 = var6 * this.forward.x + var8 * this.forward.y + var10 * this.forward.z;
         float clamp;
         float clamp2;
         if (var12 > 0.001) {
            Vec3d vec = client.gameRenderer.project(target);
            if (!isFinitePosition(vec)) {
               return;
            }

            clamp = (float)((vec.x + 1.0) * this.screenWidth * 0.5);
            clamp2 = (float)((1.0 - vec.y) * this.screenHeight * 0.5);
            float computeSmoothFactor = this.computeSmoothFactor(clamp, clamp2);
            clamp = this.centerX + (clamp - this.centerX) * computeSmoothFactor;
            clamp2 = this.centerY + (clamp2 - this.centerY) * computeSmoothFactor;
         } else {
            double var35 = var6 * this.right.x + var8 * this.right.y + var10 * this.right.z;
            double var18 = var6 * this.up.x + var8 * this.up.y + var10 * this.up.z;
            double sqrt = Math.sqrt(var35 * var35 + var18 * var18);
            if (sqrt < 1.0E-4) {
               var35 = 0.0;
               var18 = -1.0;
               sqrt = 1.0;
            }

            double var22 = var35 / sqrt;
            double var24 = var18 / sqrt;
            float var26 = this.screenWidth * 0.5F - 2.0F;
            float var27 = this.screenHeight * 0.5F - 2.0F;
            float abs = Math.abs(var22) > 1.0E-4 ? (float)(var26 / Math.abs(var22)) : Float.MAX_VALUE;
            float abs2 = Math.abs(var24) > 1.0E-4 ? (float)(var27 / Math.abs(var24)) : Float.MAX_VALUE;
            float min = Math.min(abs, abs2);
            clamp = this.centerX + (float)(var22 * min);
            clamp2 = this.centerY - (float)(var24 * min);
         }

         clamp = MathHelper.clamp(clamp, 2.0F, this.screenWidth - 2.0F);
         clamp2 = MathHelper.clamp(clamp2, 2.0F, this.screenHeight - 2.0F);
         RenderUtil.drawLine(context, this.centerX, this.centerY, clamp, clamp2, width, color);
      }
   }

   private float computeSmoothFactor(float targetX, float targetY){
      float var3 = targetX - this.centerX;
      float var4 = targetY - this.centerY;
      float abs = 1.0F;
      if (Math.abs(var3) > 1.0E-4F) {
         float var6 = var3 > 0.0F ? this.screenWidth - 2.0F - this.centerX : this.centerX - 2.0F;
         abs = Math.min(abs, var6 / Math.abs(var3));
      }

      if (Math.abs(var4) > 1.0E-4F) {
         float var7 = var4 > 0.0F ? this.screenHeight - 2.0F - this.centerY : this.centerY - 2.0F;
         abs = Math.min(abs, var7 / Math.abs(var4));
      }

      return MathHelper.clamp(abs, 0.0F, 1.0F);
   }

   private static boolean isFinitePosition(Vec3d position){
      return Double.isFinite(position.x) && Double.isFinite(position.y) && Double.isFinite(position.z);
   }
}
