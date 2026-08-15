package made4mischief.astatine.client.modules.movement;

import java.util.ArrayList;
import java.util.List;
import made4mischief.astatine.client.mixin.InputAccessor;
import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.ModeSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import made4mischief.astatine.client.utils.world.HoleScanner;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos.Mutable;

@Environment(EnvType.CLIENT)
public final class HoleSnapModule extends Module {
   private static final double EPSILON = 1.0E-4;
   private static final float[][] DIRECTIONS = new float[][]{
      {0.0F, 1.0F}, {1.0F, 1.0F}, {1.0F, 0.0F}, {1.0F, -1.0F}, {0.0F, -1.0F}, {-1.0F, -1.0F}, {-1.0F, 0.0F}, {-1.0F, 1.0F}
   };
   private static HoleSnapModule instance;
   private final ModeSetting movementModeSetting = this.addMode("Movement Mode", "Legit", new String[]{"Legit", "Velocity"});
   private final NumberSetting rangeSetting = this.addNumber("Range", 6.0, 2.0, 12.0, 0.5);
   private final NumberSetting maxDropSetting = this.addNumber("max Drop", 4.0, 1.0, 8.0, 1.0);
   private final NumberSetting speedSetting = this.addNumber("Speed", 0.32, 0.05, 0.8, 0.01);
   private final NumberSetting stopDistanceSetting = this.addNumber("Stop Distance", 0.08, 0.02, 0.3, 0.01);
   private final BooleanSetting sprintSetting = this.addBoolean("Sprint", true);
   private final BooleanSetting doubleHolesSetting = this.addBoolean("Double Holes", true);
   private final BooleanSetting autoDisableSetting = this.addBoolean("Auto Disable", true);
   private final BooleanSetting disableNoTargetSetting = this.addBoolean("Disable No Target", true);
   private final List<HoleScanner.Hole> holes = new ArrayList<>();
   private final Mutable probePos = new Mutable();
   private HoleScanner.Hole target;

   public HoleSnapModule(){
      super("HoleSnap", Category.MOVEMENT, "Tá»± di chuyá»ƒn vÃ o há»‘ an toÃ n gáº§n nháº¥t.", -1, true);
      this.speedSetting.visibleWhen(() -> this.movementModeSetting.is("Velocity"));
      this.sprintSetting.visibleWhen(() -> this.movementModeSetting.is("Legit"));
      instance = this;
   }

   @Override
   protected void onEnable(){
      this.holes.clear();
      this.target = null;
   }

   @Override
   protected void onDisable(){
      this.holes.clear();
      this.target = null;
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      ClientPlayerEntity player = client.player;
      if (!this.canSnap(client, player)) {
         this.disable();
      } else {
         HoleScanner.scan(
            client.world,
            player.getBlockPos(),
            (int)Math.ceil(this.rangeSetting.getValue()),
            this.maxDropSetting.getValueInt(),
            this.doubleHolesSetting.getValue(),
            this.holes
         );
         if (this.target == null || !this.holes.contains(this.target) || !this.isPlayerAligned(client.world, player, this.target)) {
            this.target = this.findNearestHole(client.world, player);
         }

         if (this.target == null) {
            if (this.disableNoTargetSetting.getValue()) {
               this.disable();
            }
         } else {
            if (this.movementModeSetting.is("Velocity")) {
               this.moveToHoleX(player, this.target);
            } else if (this.autoDisableSetting.getValue() && this.snapToHoleCenterX(player, this.target) && this.isPlayerInHoleColumn(player, this.target)) {
               this.disable();
            }
         }
      }
   }

   public static void applyMovementInput(ClientPlayerEntity player){
      HoleSnapModule holeSnapModule = instance;
      if (holeSnapModule != null && holeSnapModule.isEnabled() && holeSnapModule.movementModeSetting.is("Legit") && holeSnapModule.target != null && player != null && MinecraftClient.getInstance().currentScreen == null
         )
       {
         double x = getHoleCenterX(holeSnapModule.target) - player.getX();
         double z = getHoleCenterZ(holeSnapModule.target) - player.getZ();
         double var6 = x * x + z * z;
         double velocity = Math.sqrt(
            player.getVelocity().x * player.getVelocity().x + player.getVelocity().z * player.getVelocity().z
         );
         double value2 = Math.max(holeSnapModule.stopDistanceSetting.getValue(), velocity * 1.35);
         PlayerInput playerInput = player.input.playerInput;
         if (var6 <= value2 * value2) {
            snapPlayerMovement(player, playerInput, 0.0F, 0.0F, false);
         } else {
            double sqrt = 1.0 / Math.sqrt(var6);
            double var15 = x * sqrt;
            double var17 = z * sqrt;
            double yaw = Math.toRadians(player.getYaw());
            float sin = (float)(var15 * Math.cos(yaw) + var17 * Math.sin(yaw));
            float cos = (float)(-var15 * Math.sin(yaw) + var17 * Math.cos(yaw));
            float var23 = 0.0F;
            float var24 = 0.0F;
            float var25 = -Float.MAX_VALUE;

            for (float[] var29 : DIRECTIONS) {
               float sqrt2 = (float)Math.sqrt(var29[0] * var29[0] + var29[1] * var29[1]);
               float var31 = var29[0] / sqrt2;
               float var32 = var29[1] / sqrt2;
               float var33 = sin * var31 + cos * var32;
               if (var33 > var25) {
                  var25 = var33;
                  var23 = var29[0];
                  var24 = var29[1];
               }
            }

            boolean value = holeSnapModule.sprintSetting.getValue() && var24 > 0.0F;
            snapPlayerMovement(player, playerInput, var23, var24, value);
         }
      }
   }

   private static void snapPlayerMovement(ClientPlayerEntity player, PlayerInput current, float strafe, float forward, boolean sprint){
      boolean var5 = forward > 0.0F;
      boolean var6 = forward < 0.0F;
      boolean var7 = strafe > 0.0F;
      boolean var8 = strafe < 0.0F;
      player.input.playerInput = new PlayerInput(var5, var6, var7, var8, false, current.sneak(), sprint && var5 && !var6);
      ((InputAccessor)player.input)
         .astatine$setMovementVector(strafe == 0.0F && forward == 0.0F ? Vec2f.ZERO : new Vec2f(strafe, forward).normalize());
   }

   private HoleScanner.Hole findNearestHole(ClientWorld world, ClientPlayerEntity player){
      HoleScanner.Hole var3 = null;
      double value = this.rangeSetting.getValue() * this.rangeSetting.getValue();

      for (int index = 0; index < this.holes.size(); index++) {
         HoleScanner.Hole var7 = this.holes.get(index);
         if (this.isPlayerAligned(world, player, var7)) {
            double x = getHoleCenterX(var7) - player.getX();
            double z = getHoleCenterZ(var7) - player.getZ();
            double var12 = x * x + z * z;
            if (var12 < value) {
               var3 = var7;
               value = var12;
            }
         }
      }

      return var3;
   }

   private boolean isPlayerAligned(ClientWorld world, ClientPlayerEntity player, HoleScanner.Hole hole){
      int y2 = MathHelper.floor(player.getY() + 1.0E-4);
      double y = player.getY() - hole.y();
      if (hole.y() > y2 || y > this.maxDropSetting.getValue() + 1.0) {
         return false;
      } else {
         return hole.y() == y2 && !this.shouldSnapX(player, hole) ? false : this.isHoleClearAbove(world, hole, y2);
      }
   }

   private boolean isHoleClearAbove(ClientWorld world, HoleScanner.Hole hole, int feetY){
      int var4 = feetY + 1;

      for (int index3 = hole.y() + 2; index3 <= var4; index3++) {
         for (int index2 = hole.x(); index2 < hole.x() + hole.sizeX(); index2++) {
            for (int index = hole.z(); index < hole.z() + hole.sizeZ(); index++) {
               this.probePos.set(index2, index3, index);
               if (!world.getBlockState(this.probePos).isAir()) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   private void moveToHoleX(ClientPlayerEntity player, HoleScanner.Hole hole){
      double x = getHoleCenterX(hole) - player.getX();
      double z = getHoleCenterZ(hole) - player.getZ();
      double sqrt = Math.sqrt(x * x + z * z);
      Vec3d vec = player.getVelocity();
      if (sqrt <= this.stopDistanceSetting.getValue()) {
         player.setVelocity(0.0, vec.y, 0.0);
         if (this.autoDisableSetting.getValue() && this.isPlayerInHoleColumn(player, hole)) {
            this.disable();
         }
      } else {
         double value = Math.min(this.speedSetting.getValue(), sqrt);
         player.setVelocity(x / sqrt * value, vec.y, z / sqrt * value);
      }
   }

   private boolean isPlayerInHoleColumn(ClientPlayerEntity player, HoleScanner.Hole hole){
      int y = MathHelper.floor(player.getY() + 1.0E-4);
      return y == hole.y() && this.shouldSnapX(player, hole);
   }

   private boolean snapToHoleCenterX(ClientPlayerEntity player, HoleScanner.Hole hole){
      double x = getHoleCenterX(hole) - player.getX();
      double z = getHoleCenterZ(hole) - player.getZ();
      double value = this.stopDistanceSetting.getValue();
      return x * x + z * z <= value * value;
   }

   private boolean shouldSnapX(ClientPlayerEntity player, HoleScanner.Hole hole){
      return player.getX() > hole.x() + 1.0E-4
         && player.getX() < hole.x() + hole.sizeX() - 1.0E-4
         && player.getZ() > hole.z() + 1.0E-4
         && player.getZ() < hole.z() + hole.sizeZ() - 1.0E-4;
   }

   private boolean canSnap(MinecraftClient client, ClientPlayerEntity player){
      return player != null
         && client.world != null
         && !player.isDead()
         && !player.hasVehicle()
         && !player.getAbilities().flying
         && !player.isGliding()
         && !player.isClimbing()
         && !player.isTouchingWater()
         && !player.isInLava()
         && !AirStuckModule.shouldFreeze(player);
   }

   private static double getHoleCenterX(HoleScanner.Hole hole){
      return hole.x() + hole.sizeX() * 0.5;
   }

   private static double getHoleCenterZ(HoleScanner.Hole hole){
      return hole.z() + hole.sizeZ() * 0.5;
   }
}

