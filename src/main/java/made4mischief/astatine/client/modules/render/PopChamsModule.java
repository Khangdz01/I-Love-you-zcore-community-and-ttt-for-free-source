package made4mischief.astatine.client.modules.render;

import java.util.ArrayList;
import java.util.List;
import made4mischief.astatine.client.gui.component.theme.ThemeManager;
import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.render.popchams.PopChamsRenderer;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.ColorSetting;
import made4mischief.astatine.client.setting.ModeSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.PacketEvent;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.world.ClientWorld;

@Environment(EnvType.CLIENT)
public final class PopChamsModule extends Module {
   private static PopChamsModule instance;
   private final ModeSetting modeSetting = this.addMode("Mode", "Full", new String[]{"Full", "Line"});
   private final BooleanSetting themeColorSetting = this.addBoolean("Theme Color", true);
   private final ColorSetting color = this.addColor("Color", -4879105);
   private final NumberSetting durationSetting = this.addNumber("Duration", 1100.0, 300.0, 2500.0, 50.0);
   private final NumberSetting riseHeightSetting = this.addNumber("Rise Height", 1.35, 0.0, 3.0, 0.05);
   private final NumberSetting fullOpacitySetting = this.addNumber("Full Opacity", 0.62, 0.1, 1.0, 0.05);
   private final NumberSetting lineFillOpacitySetting = this.addNumber("Line Fill Opacity", 0.22, 0.0, 1.0, 0.05);
   private final NumberSetting lineWidthSetting = this.addNumber("Line Width", 2.0, 1.0, 5.0, 0.25);
   private final BooleanSetting selfPopsSetting = this.addBoolean("Self Pops", false);
   private final List<PopChamsModule.PopGhost> ghosts = new ArrayList<>();
   private ClientWorld world;

   public PopChamsModule(){
      super("PopChams", Category.RENDER, "Hiá»‡n bÃ³ng má» khi ngÆ°á»i chÆ¡i ná»• váº­t tá»•.", -1, true);
      instance = this;
      this.color.visibleWhen(() -> !this.themeColorSetting.getValue());
      this.fullOpacitySetting.visibleWhen(() -> this.modeSetting.is("Full"));
      this.lineFillOpacitySetting.visibleWhen(() -> this.modeSetting.is("Line"));
      this.lineWidthSetting.visibleWhen(() -> this.modeSetting.is("Line"));
      WorldRenderEvents.BEFORE_DEBUG_RENDER.register(PopChamsModule::renderWorld);
   }

   @Override
   protected void onEnable(){
      this.ghosts.clear();
      this.world = MinecraftClient.getInstance().world;
   }

   @Override
   protected void onDisable(){
      this.ghosts.clear();
      this.world = null;
   }

   @EventTarget
   public void onPacket(PacketEvent event){
      if (event.isReceive() && event.getPacket() instanceof EntityStatusS2CPacket var2 && var2.getStatus() == 35) {
         MinecraftClient client = MinecraftClient.getInstance();
         client.execute(() -> this.onEntityStatusPacket(var2));
      }
   }

   @EventTarget
   public void onTick(TickEvent event){
      ClientWorld world = event.getClient().world;
      if (world != this.world) {
         this.ghosts.clear();
         this.world = world;
      }

      this.pruneExpired(System.nanoTime());
   }

   private void onEntityStatusPacket(EntityStatusS2CPacket packet){
      if (this.isEnabled()) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.world != null && client.player != null) {
            if (packet.getEntity(client.world) instanceof PlayerEntity var4) {
               this.capturePop(var4);
            }
         }
      }
   }

   public static void captureLocalPop(PlayerEntity player){
      PopChamsModule popChamsModule = instance;
      if (popChamsModule != null && popChamsModule.isEnabled()) {
         popChamsModule.capturePop(player);
      }
   }

   private void capturePop(PlayerEntity player){
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.world != null && client.player != null && !player.isRemoved() && (this.selfPopsSetting.getValue() || player != client.player)) {
         if (client.getEntityRenderDispatcher().getAndUpdateRenderState(player, 1.0F) instanceof PlayerEntityRenderState var4) {
            var4.onFire = false;
            var4.displayName = null;
            var4.nameLabelPos = null;
            var4.shadowPieces.clear();
            var4.shadowRadius = 0.0F;
            var4.outlineColor = 0;
            this.ghosts.add(new PopChamsModule.PopGhost(var4, var4.x, var4.y, var4.z, System.nanoTime()));
         }
      }
   }

   private static void renderWorld(WorldRenderContext context){
      PopChamsModule popChamsModule = instance;
      if (popChamsModule != null && popChamsModule.isEnabled() && !popChamsModule.ghosts.isEmpty()) {
         long nanoTime = System.nanoTime();
         float valueInt = popChamsModule.durationSetting.getValueInt() * 1000000.0F;
         int chamColor = popChamsModule.getChamColor();
         boolean is = popChamsModule.modeSetting.is("Line");

         for (int index = 0; index < popChamsModule.ghosts.size(); index++) {
            PopChamsModule.PopGhost var8 = popChamsModule.ghosts.get(index);
            float clamp = MathHelper.clamp((float)(nanoTime - var8.createdAtNanos) / valueInt, 0.0F, 1.0F);
            if (!(clamp >= 1.0F)) {
               float var10 = 1.0F - (1.0F - clamp) * (1.0F - clamp);
               float var11 = 1.0F - smoothstep(0.08F, 1.0F, clamp);
               float valueFloat = var11 * (is ? popChamsModule.lineFillOpacitySetting.getValueFloat() : popChamsModule.fullOpacitySetting.getValueFloat());
               float var13 = is ? var11 : 0.0F;
               PopChamsRenderer.render(
                  context,
                  var8.state,
                  var8.x,
                  var8.y + popChamsModule.riseHeightSetting.getValueFloat() * var10,
                  var8.z,
                  chamColor,
                  valueFloat,
                  var13,
                  is,
                  popChamsModule.lineWidthSetting.getValueFloat()
               );
            }
         }
      }
   }

   private void pruneExpired(long now){
      long valueInt = this.durationSetting.getValueInt() * 1000000L;

      for (int index = this.ghosts.size() - 1; index >= 0; index--) {
         if (now - this.ghosts.get(index).createdAtNanos >= valueInt) {
            this.ghosts.remove(index);
         }
      }
   }

   private int getChamColor(){
      int value = this.themeColorSetting.getValue() ? ThemeManager.active().accent() : this.color.getValue();
      return value & 16777215;
   }

   private static float smoothstep(float edge0, float edge1, float value){
      float clamp = MathHelper.clamp((value - edge0) / (edge1 - edge0), 0.0F, 1.0F);
      return clamp * clamp * (3.0F - 2.0F * clamp);
   }

   @Environment(EnvType.CLIENT)
   private record PopGhost(PlayerEntityRenderState state, double x, double y, double z, long createdAtNanos){
   }
}

