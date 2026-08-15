package made4mischief.astatine.client.modules.render;

import java.util.ArrayList;
import java.util.List;
import made4mischief.astatine.client.gui.component.theme.ThemeManager;
import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.ColorSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import made4mischief.astatine.client.utils.render.RenderUtil;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.network.ClientPlayerEntity;

@Environment(EnvType.CLIENT)
public final class JumpCirclesModule extends Module {
   private static JumpCirclesModule instance;
   private static final double RING_Y_OFFSET = 0.001;
   private final BooleanSetting themeColorSetting = this.addBoolean("Theme Color", true);
   private final ColorSetting color = this.addColor("Color", ThemeManager.DEFAULT.accent());
   private final NumberSetting radiusSetting = this.addNumber("Radius", 1.35, 0.4, 3.0, 0.05);
   private final NumberSetting durationSetting = this.addNumber("Duration", 650.0, 300.0, 1400.0, 25.0);
   private final NumberSetting thicknessSetting = this.addNumber("Thickness", 0.1, 0.03, 0.3, 0.01);
   private final NumberSetting glowSetting = this.addNumber("Glow", 0.16, 0.0, 0.45, 0.01);
   private final List<JumpCirclesModule.JumpRing> rings = new ArrayList<>();
   private boolean wasOnGround;
   private double lastGroundY = Double.NaN;

   public JumpCirclesModule(){
      super("JumpCircles", Category.RENDER, "Táº¡o vÃ²ng sÃ¡ng lan rá»™ng khi nháº£y.", -1);
      this.color.visibleWhen(() -> !this.themeColorSetting.getValue());
      instance = this;
      WorldRenderEvents.BEFORE_DEBUG_RENDER.register(JumpCirclesModule::renderWorld);
   }

   @Override
   protected void onEnable(){
      this.rings.clear();
      this.wasOnGround = mc.player != null && mc.player.isOnGround();
      this.lastGroundY = this.wasOnGround ? mc.player.getY() : Double.NaN;
   }

   @Override
   protected void onDisable(){
      this.rings.clear();
      this.lastGroundY = Double.NaN;
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      ClientPlayerEntity player = client.player;
      if (player != null && client.world != null) {
         boolean onGround = player.isOnGround();
         if (this.wasOnGround && !onGround && player.getVelocity().y > 0.02) {
            double boundingBo = Double.isFinite(this.lastGroundY) ? this.lastGroundY : player.getBoundingBox().minY;
            this.rings.add(new JumpCirclesModule.JumpRing(player.getX(), boundingBo + 0.001, player.getZ()));
         }

         if (onGround) {
            this.lastGroundY = player.getY();
         }

         this.wasOnGround = onGround;
         long nanoTime = System.nanoTime();
         long valueInt = this.durationSetting.getValueInt() * 1000000L;

         for (int index = this.rings.size() - 1; index >= 0; index--) {
            if (nanoTime - this.rings.get(index).createdAtNanos >= valueInt) {
               this.rings.remove(index);
            }
         }
      } else {
         this.rings.clear();
         this.wasOnGround = false;
         this.lastGroundY = Double.NaN;
      }
   }

   private static void renderWorld(WorldRenderContext context){
      JumpCirclesModule jumpCirclesModule = instance;
      if (jumpCirclesModule != null && jumpCirclesModule.isEnabled() && !jumpCirclesModule.rings.isEmpty()) {
         long nanoTime = System.nanoTime();
         float valueInt = jumpCirclesModule.durationSetting.getValueInt() * 1000000.0F;
         int circleColor = jumpCirclesModule.getCircleColor();

         for (int index = 0; index < jumpCirclesModule.rings.size(); index++) {
            JumpCirclesModule.JumpRing var7 = jumpCirclesModule.rings.get(index);
            float clamp = MathHelper.clamp((float)(nanoTime - var7.createdAtNanos) / valueInt, 0.0F, 1.0F);
            float var9 = 1.0F - clamp;
            float var10 = 1.0F - var9 * var9 * var9;
            float valueFloat = jumpCirclesModule.radiusSetting.getValueFloat() * (0.06F + 0.94F * var10);
            float var12 = smoothstep(0.0F, 0.1F, clamp);
            float var13 = 1.0F - smoothstep(0.52F, 1.0F, clamp);
            float var14 = var12 * var13;
            if (!(var14 <= 0.001F)) {
               RenderUtil.drawWorldRing(context, var7.x, var7.y, var7.z, valueFloat, jumpCirclesModule.thicknessSetting.getValueFloat(), jumpCirclesModule.glowSetting.getValueFloat(), circleColor, var14);
            }
         }
      }
   }

   private static float smoothstep(float edge0, float edge1, float value){
      float clamp = MathHelper.clamp((value - edge0) / (edge1 - edge0), 0.0F, 1.0F);
      return clamp * clamp * (3.0F - 2.0F * clamp);
   }

   private int getCircleColor(){
      int value = this.themeColorSetting.getValue() ? ThemeManager.active().accent() : this.color.getValue();
      return value & 16777215;
   }

   @Environment(EnvType.CLIENT)
   private static final class JumpRing {
      private final double x;
      private final double y;
      private final double z;
      private final long createdAtNanos;

      private JumpRing(double x, double y, double z){
         this.x = x;
         this.y = y;
         this.z = z;
         this.createdAtNanos = System.nanoTime();
      }
   }
}

