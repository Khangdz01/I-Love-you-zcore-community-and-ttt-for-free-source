package made4mischief.astatine.client.hud;

import java.util.UUID;
import made4mischief.astatine.Astatine;
import made4mischief.astatine.client.gui.component.theme.Theme;
import made4mischief.astatine.client.gui.component.theme.ThemeManager;
import made4mischief.astatine.client.modules.ModuleManager;
import made4mischief.astatine.client.modules.combat.KillAuraModule;
import made4mischief.astatine.client.utils.render.RenderUtil;
import made4mischief.astatine.client.utils.render.animation.Animation;
import made4mischief.astatine.client.utils.render.animation.AnimationManager;
import made4mischief.astatine.client.utils.render.animation.AnimationType;
import made4mischief.astatine.client.utils.render.core.ColorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.client.render.RenderTickCounter;

@Environment(EnvType.CLIENT)
public final class TargetHudRenderer {
   private static final float TARGET_HUD_WIDTH = 244.0F;
   private static final float TARGET_HUD_HEIGHT = 102.0F;
   private static final float RADIUS = 13.0F;
   private static final int TARGET_NAME_MAX_LENGTH = 42;
   private static final float TARGET_INFO_SCALE = 0.88F;
   private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   private static final Animation targetInAnimation = new Animation(1.0F, 1.0F, 280L, AnimationType.EASE_OUT);
   private static final Animation targetOutAnimation = new Animation(0.0F, 1.0F, 105L, AnimationType.EASE_OUT);
   private static final Animation pulseAnimation = new Animation(0.0F, 1.0F, 210L, AnimationType.EASE_OUT);
   private static KillAuraModule module;
   private static PlayerEntity currentTarget;
   private static PlayerEntity lastTarget;
   private static UUID lastTargetId;
   private static float lastTargetHealth = -1.0F;
   private static int targetTicks;
   private static boolean targetVisible;
   private static boolean initialized;

   private TargetHudRenderer(){
   }

   public static void init(){
      if (!initialized) {
         module = ModuleManager.INSTANCE.getModule(KillAuraModule.class);
         HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR, Astatine.id("target_hud"), TargetHudRenderer::render);
         initialized = true;
      }
   }

   public static void syncTarget(LivingEntity target){
      currentTarget = target instanceof PlayerEntity var1 ? var1 : null;
   }

   private static void render(DrawContext context, RenderTickCounter tickCounter){
      AnimationManager.update();
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null && client.world != null) {
         PlayerEntity player = hasValidTarget(client) ? currentTarget : null;
         setTarget(player);
         if (player != null) {
            lastTarget = player;
            clearTarget(player);
         }

         if (lastTarget != null) {
            updateTargetAnimation();
            float get = ease(pulseAnimation.get());
            if (!targetVisible && get <= 0.001F && pulseAnimation.isFinished()) {
               clearTargetState();
            } else {
               renderTargetHud(context, client, lastTarget, get);
            }
         }
      } else {
         resetAnimation();
      }
   }

   public static void renderPreview(DrawContext context){
      MinecraftClient client = MinecraftClient.getInstance();
      if (module == null) {
         module = ModuleManager.INSTANCE.getModule(KillAuraModule.class);
      }

      if (module != null && client.player != null) {
         ClientPlayerEntity player = client.player;
         float maxHealth = Math.max(1.0F, player.getMaxHealth());
         float health = ease(Math.max(0.0F, player.getHealth()) / maxHealth);
         float targetHudPositionX = context.getScaledWindowWidth() * module.getTargetHudPositionX();
         float targetHudPositionY = context.getScaledWindowHeight() * module.getTargetHudPositionY();
         renderHudFrame(context, client, player, targetHudPositionX, targetHudPositionY, module.getTargetHudScale(), health, 0.0F);
      }
   }

   private static void setTarget(PlayerEntity target){
      boolean var1 = target != null;
      if (var1 != targetVisible) {
         targetVisible = var1;
         pulseAnimation.setType(var1 ? AnimationType.EASE_OUT : AnimationType.EASE_IN);
         pulseAnimation.setTarget(var1 ? 1.0F : 0.0F);
      }
   }

   private static void clearTarget(PlayerEntity target){
      float maxHealth = Math.max(1.0F, target.getMaxHealth());
      float health = Math.max(0.0F, target.getHealth());
      float var3 = ease(health / maxHealth);
      if (!target.getUuid().equals(lastTargetId)) {
         lastTargetId = target.getUuid();
         lastTargetHealth = health;
         targetTicks = target.hurtTime;
         targetInAnimation.snapTo(var3);
         targetOutAnimation.snapTo(0.0F);
         targetVisible = false;
      } else {
         boolean var4 = health < lastTargetHealth - 0.001F;
         boolean var5 = target.hurtTime > targetTicks;
         if (Math.abs(health - lastTargetHealth) > 0.001F) {
            targetInAnimation.setTarget(var3);
            lastTargetHealth = health;
         }

         if (var4 || var5) {
            resetState();
         }

         targetTicks = target.hurtTime;
      }
   }

   private static void resetState(){
      targetOutAnimation.snapTo(0.0F);
      targetOutAnimation.setTarget(1.0F);
      targetVisible = false;
   }

   private static void updateTargetAnimation(){
      if (!targetVisible && targetOutAnimation.getProgress() >= 1.0F && targetOutAnimation.isFinished()) {
         targetOutAnimation.reverse();
         targetVisible = true;
      } else if (targetVisible && targetOutAnimation.isFinished()) {
         targetVisible = false;
      }
   }

   private static void renderTargetHud(DrawContext context, MinecraftClient client, PlayerEntity target, float visibility){
      float targetHudScale = module.getTargetHudScale() * (0.88F + 0.120000005F * visibility);
      float targetHudPositionX = context.getScaledWindowWidth() * module.getTargetHudPositionX();
      float targetHudPositionY = context.getScaledWindowHeight() * module.getTargetHudPositionY();
      renderHudFrame(context, client, target, targetHudPositionX, targetHudPositionY, targetHudScale, ease(targetInAnimation.get()), targetOutAnimation.get());
   }

   private static boolean hasValidTarget(MinecraftClient client){
      return module != null
         && module.isEnabled()
         && module.isTargetHudEnabled()
         && currentTarget != null
         && currentTarget.isAlive()
         && client.world.getEntityById(currentTarget.getId()) == currentTarget;
   }

   private static void renderHudFrame(
      DrawContext context, MinecraftClient client, PlayerEntity target, float desiredCenterX, float desiredCenterY, float scale, float healthFraction, float damagePulse
   ){
      Theme theme = ThemeManager.active();
      float var9 = 244.0F * scale;
      float var10 = 102.0F * scale;
      float scaledWindowWidth = clamp(desiredCenterX - var9 / 2.0F, 4.0F, context.getScaledWindowWidth() - var9 - 4.0F);
      float scaledWindowHeight = clamp(desiredCenterY - var10 / 2.0F, 4.0F, context.getScaledWindowHeight() - var10 - 4.0F);
      context.getMatrices().pushMatrix();
      context.getMatrices().translate(scaledWindowWidth, scaledWindowHeight);
      context.getMatrices().scale(scale, scale);
      int accent = ColorUtil.scaleAlpha(theme.accent(), 0.55F);
      int surface = ColorUtil.withAlpha(theme.surface(), 232);
      RenderUtil.drawBo(context, -1.0F, -1.0F, 246.0F, 104.0F, 14.0F, accent);
      RenderUtil.drawBo(context, 0.0F, 0.0F, 244.0F, 102.0F, 13.0F, surface);
      renderTargetInfo(context, client, target, 0.0F, 0.0F, theme, damagePulse);
      renderHealthBar(context, target, 0.0F, 0.0F, theme, healthFraction);
      renderArmorWidget(context, target, 0.0F, 0.0F, theme);
      context.getMatrices().popMatrix();
   }

   private static void renderTargetInfo(DrawContext context, MinecraftClient client, PlayerEntity target, float x, float y, Theme theme, float pulse){
      int round3 = Math.max(1, Math.round(42.0F * (1.0F - 0.1F * pulse)));
      int round2 = Math.round(x + 12.0F + (42 - round3) / 2.0F);
      int round = Math.round(y + 16.0F + (42 - round3) / 2.0F);
      RenderUtil.drawBo(context, x + 10.0F, y + 14.0F, 46.0F, 46.0F, 8.0F, ColorUtil.scaleAlpha(theme.accent(), 0.22F));
      SkinTextures skinTextures = DefaultSkinHelper.getSkinTextures(target.getGameProfile());
      if (client.getNetworkHandler() != null) {
         PlayerListEntry playerListEntry = client.getNetworkHandler().getPlayerListEntry(target.getUuid());
         if (playerListEntry != null) {
            skinTextures = playerListEntry.getSkinTextures();
         }
      }

      int lerp = ColorUtil.lerp(-1, -57312, pulse);
      RenderUtil.drawPlayerFace(context, skinTextures, round2, round, round3, lerp);
   }

   private static void renderHealthBar(DrawContext context, PlayerEntity target, float x, float y, Theme theme, float animatedFraction){
      String string = truncateText(target.getName().getString(), 162.0F, 0.92F);
      RenderUtil.drawText(context, string, x + 64.0F, y + 10.0F, theme.text(), true, 0.92F);
      float maxHealth = Math.max(1.0F, target.getMaxHealth());
      float health = Math.max(0.0F, target.getHealth());
      float var9 = ease(health / maxHealth);
      float var10 = x + 64.0F;
      float var11 = y + 31.0F;
      float var12 = 166.0F;
      RenderUtil.drawBo(context, var10, var11, var12, 7.0F, 3.5F, ColorUtil.withAlpha(theme.background(), 210));
      int var13 = colorForFraction(var9);
      RenderUtil.drawBo(context, var10, var11, var12 * animatedFraction, 7.0F, 3.5F, var13);
      String var14 = formatFloat(health) + " / " + formatFloat(maxHealth) + " HP";
      RenderUtil.drawText(context, var14, var10, y + 42.0F, theme.textDim(), false, 0.72F);
   }

   private static void renderArmorWidget(DrawContext context, PlayerEntity target, float x, float y, Theme theme){
      float var5 = y + 59.0F;
      RenderUtil.drawText(context, "HANDS", x + 64.0F, var5, ColorUtil.scaleAlpha(theme.textDim(), 0.75F), false, 0.58F);
      RenderUtil.drawText(context, "ARMOR", x + 124.0F, var5, ColorUtil.scaleAlpha(theme.textDim(), 0.75F), false, 0.58F);
      int round2 = Math.round(y + 73.0F);
      renderItemStack(context, target, target.getMainHandStack(), Math.round(x + 64.0F), round2, 0, theme);
      renderItemStack(context, target, target.getOffHandStack(), Math.round(x + 84.0F), round2, 1, theme);

      for (int index = 0; index < ARMOR_SLOTS.length; index++) {
         ItemStack stack = target.getEquippedStack(ARMOR_SLOTS[index]);
         int round = Math.round(x + 124.0F + index * 20.0F);
         renderItemStack(context, target, stack, round, round2, 2 + index, theme);
      }
   }

   private static void renderItemStack(DrawContext context, PlayerEntity target, ItemStack stack, int x, int y, int seed, Theme theme){
      if (stack != null && !stack.isEmpty()) {
         RenderUtil.drawItemStack(context, target, stack, x, y, seed);
         if (stack.isDamageable() && stack.getMaxDamage() > 0) {
            float maxDamage = ease(1.0F - (float)stack.getDamage() / stack.getMaxDamage());
            int lerp = ColorUtil.lerp(-49841, -12189814, maxDamage);
            RenderUtil.drawBo(context, x + 1.0F, y + 17.0F, 14.0F, 2.0F, 1.0F, ColorUtil.withAlpha(theme.background(), 220));
            RenderUtil.drawBo(context, x + 1.0F, y + 17.0F, 14.0F * maxDamage, 2.0F, 1.0F, lerp);
         }
      } else {
         RenderUtil.drawBo(context, x + 1.0F, y + 1.0F, 14.0F, 14.0F, 3.0F, ColorUtil.scaleAlpha(theme.border(), 0.22F));
      }
   }

   private static void clearTargetState(){
      lastTarget = null;
      lastTargetId = null;
      lastTargetHealth = -1.0F;
      targetTicks = 0;
      targetInAnimation.snapTo(0.0F);
      targetOutAnimation.snapTo(0.0F);
      targetVisible = false;
   }

   private static void resetAnimation(){
      currentTarget = null;
      targetVisible = false;
      pulseAnimation.snapTo(0.0F);
      clearTargetState();
   }

   private static int colorForFraction(float fraction){
      return fraction < 0.5F ? ColorUtil.lerp(-49841, -15801, fraction * 2.0F) : ColorUtil.lerp(-15801, -12323446, (fraction - 0.5F) * 2.0F);
   }

   private static float ease(float value){
      return value < 0.0F ? 0.0F : Math.min(1.0F, value);
   }

   private static float clamp(float value, float min, float max){
      return max < min ? min : Math.max(min, Math.min(max, value));
   }

   private static String formatFloat(float value){
      int max = Math.round(Math.max(0.0F, value) * 10.0F);
      return max / 10 + "." + max % 10;
   }

   private static String truncateText(String text, float maxWidth, float scale){
      if (RenderUtil.getTextWidth(text, scale) <= maxWidth) {
         return text;
      } else {
         String var3 = "...";

         for (int index = text.length() - 1; index > 0; index--) {
            String substring = text.substring(0, index) + var3;
            if (RenderUtil.getTextWidth(substring, scale) <= maxWidth) {
               return substring;
            }
         }

         return var3;
      }
   }
}

