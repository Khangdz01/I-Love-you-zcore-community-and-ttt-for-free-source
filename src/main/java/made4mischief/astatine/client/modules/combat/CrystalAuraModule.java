package made4mischief.astatine.client.modules.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import made4mischief.astatine.client.mixin.ClientWorldAccessor;
import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.modules.player.FriendModule;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.ColorSetting;
import made4mischief.astatine.client.setting.ModeSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import made4mischief.astatine.client.utils.combat.CrystalDamageUtil;
import made4mischief.astatine.client.utils.combat.TargetUtil;
import made4mischief.astatine.client.utils.inventory.InventoryUtil;
import made4mischief.astatine.client.utils.inventory.SilentSlotManager;
import made4mischief.astatine.client.utils.render.RenderUtil;
import made4mischief.astatine.client.utils.render.core.ColorUtil;
import made4mischief.astatine.client.utils.rotation.RotationManager;
import made4mischief.astatine.client.utils.rotation.RotationUtil;
import made4mischief.astatine.client.utils.world.BlockPlacementUtil;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.PacketEvent;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@Environment(EnvType.CLIENT)
public class CrystalAuraModule extends Module {
   private static CrystalAuraModule instance;
   private final ModeSetting modeSetting = this.addMode("Mode", "SMP", new String[]{"SMP", "Rage"});
   private final BooleanSetting autoObsidianSetting = this.addBoolean("Auto Obsidian", true);
   private final BooleanSetting movementFixSetting = this.addBoolean("Movement Fi", true);
   private final BooleanSetting swingHandSetting = this.addBoolean("Swing Hand", true);
   private final BooleanSetting raytraceSetting = this.addBoolean("Raytrace", false);
   private final NumberSetting targetRangeSetting = this.addNumber("Target Range", 5.0, 1.0, 7.0, 0.5);
   private final NumberSetting placeRadiusSetting = this.addNumber("Place Radius", 3.0, 1.0, 5.0, 1.0);
   private final NumberSetting breakRadiusSetting = this.addNumber("Break Radius", 4.0, 1.0, 6.0, 1.0);
   private final NumberSetting placeRangeSetting = this.addNumber("Place Range", 5.0, 2.0, 6.0, 1.0);
   private final NumberSetting breakRangeSetting = this.addNumber("Break Range", 5.0, 2.0, 6.0, 1.0);
   private final NumberSetting minDamageSetting = this.addNumber("Min Damage", 6.0, 0.0, 20.0, 0.5);
   private final NumberSetting maxSelfDamageSetting = this.addNumber("max Self Damage", 6.0, 0.0, 20.0, 0.5);
   private final NumberSetting safetyMarginSetting = this.addNumber("Safety Margin", 1.0, 0.0, 10.0, 0.5);
   private final NumberSetting facePlaceHealthSetting = this.addNumber("Face Place Health", 8.0, 0.0, 20.0, 0.5);
   private final BooleanSetting lethalOverrideSetting = this.addBoolean("Lethal Override", true);
   private final BooleanSetting verticalPlaceSetting = this.addBoolean("Vertical Place", true);
   private final ModeSetting placeModeSetting = this.addMode("Place Mode", "Auto", new String[]{"Top", "Side", "Auto"});
   private final ModeSetting swapModeSetting = this.addMode("Swap Mode", "MainHand", new String[]{"MainHand", "OffHand", "Silent"});
   private final BooleanSetting multiTaskSetting = this.addBoolean("MultiTask", false);
   private final NumberSetting placeDelaySetting = this.addNumber("Place Delay", 2.0, 0.0, 10.0, 1.0);
   private final NumberSetting breakDelaySetting = this.addNumber("Break Delay", 2.0, 0.0, 10.0, 1.0);
   private final NumberSetting rageDelaySetting = this.addNumber("Rage Delay", 1.0, 1.0, 10.0, 1.0);
   private final NumberSetting rotateRandomizeSetting = this.addNumber("Rotate Randomize", 0.5, 0.0, 2.0, 0.1);
   private final BooleanSetting autoRefillSetting = this.addBoolean("Auto Refill", true);
   private final BooleanSetting pauseOnEatSetting = this.addBoolean("Pause On Eat", true);
   private final BooleanSetting renderPlacementSetting = this.addBoolean("Render Placement", true);
   private final ModeSetting renderModeSetting = this.addMode("Render Mode", "Fill", new String[]{"Fill", "Outline", "Both"});
   private final ColorSetting renderColorSetting = this.addColor("Render Color", -4879105);
   private final NumberSetting fillAlphaSetting = this.addNumber("Fill Alpha", 70.0, 0.0, 255.0, 5.0);
   private final NumberSetting outlineAlphaSetting = this.addNumber("Outline Alpha", 220.0, 0.0, 255.0, 5.0);
   private final NumberSetting lineWidthSetting = this.addNumber("Line Width", 2.0, 1.0, 5.0, 0.5);
   private final BooleanSetting renderThroughWallsSetting = this.addBoolean("Render Through Walls", true);
   private final BooleanSetting renderAnimationSetting = this.addBoolean("Render Animation", true);
   private final NumberSetting renderMoveTimeSetting = this.addNumber("Render Move Time", 180.0, 50.0, 500.0, 10.0);
   private static final Object ROTATION_STATE = new Object();
   private static final Random RANDOM = new Random();
   private static final Direction[] PLACE_FACES = new Direction[]{
      Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
   };
   private static final float SCORE_WEIGHT = 1.0F;
   private static final int MAX_RADIUS = 6;
   private static final int MIN_RADIUS = 2;
   private static final int MIN_DISTANCE = 4;
   private static final int PLACE_RADIUS_DEFAULT = 6;
   private static final int TICK_TIMEOUT = 10;
   private static final double SNAP_THRESHOLD = 0.2;
   private static final double MAX_BASE_HEIGHT = 3.0;
   private static final double SAFETY_MARGIN = 0.15;
   private static final double CRYSTAL_EXPAND = 0.75;
   private static final double TARGET_RANGE_PAD = 1.5;
   private static final double EPSILON = 0.01;
   private static int lastSpawnedCrystalId = -1;
   private static boolean obsidianPlacementFlag;
   private int lastPlaceTick = -1;
   private int lastBreakTick = -1;
   private boolean attackPending;
   private int attackDelayTick;
   private int respawnGraceTick;
   private BlockPos pendingSpawnBase;
   private int rotationTickCounter;
   private CrystalAuraModule.PendingPlacement pendingPlacement;
   private BlockPos currentBase;
   private BlockPos renderPlacementPos;
   private BlockPos renderAnimStart;
   private Vec3d renderAnimFrom;
   private Vec3d renderAnimTo;
   private long renderAnimStartNanos;
   private int pendingSwapTick = -1;
   private int pendingSwapUntil = -1;
   private int preferredHotbarSlot = -1;
   private int handSwitchCooldown = -1;
   private PlayerEntity lockedTarget;
   private String lastTargetName;
   private PlayerEntity lastTickPlayer;
   private String lastMode;
   private int lockTargetId = -1;
   private int lockStartTick = -1;
   private double lockStartY;
   private boolean lockFalling;
   private BlockPos lockBase;
   private CrystalAuraModule.PendingObsidianPlacement pendingObsidianPlacement;
   private int swapLockTick;

   public CrystalAuraModule(){
      super("CrystalAura", Category.COMBAT, "Tá»± Ä‘áº·t vÃ  phÃ¡ crystal.");
      this.placeDelaySetting.visibleWhen(() -> this.modeSetting.is("SMP"));
      this.breakDelaySetting.visibleWhen(() -> this.modeSetting.is("SMP"));
      this.rageDelaySetting.visibleWhen(() -> this.modeSetting.is("Rage"));
      this.multiTaskSetting.visibleWhen(() -> this.swapModeSetting.is("Silent"));
      this.pauseOnEatSetting.visibleWhen(() -> !this.swapModeSetting.is("Silent") || !this.multiTaskSetting.getValue());
      this.fillAlphaSetting.visibleWhen(() -> this.renderPlacementSetting.getValue() && !this.renderModeSetting.is("Outline"));
      this.outlineAlphaSetting.visibleWhen(() -> this.renderPlacementSetting.getValue() && !this.renderModeSetting.is("Fill"));
      this.lineWidthSetting.visibleWhen(() -> this.renderPlacementSetting.getValue() && !this.renderModeSetting.is("Fill"));
      this.renderModeSetting.visibleWhen(this.renderPlacementSetting::getValue);
      this.renderColorSetting.visibleWhen(this.renderPlacementSetting::getValue);
      this.renderThroughWallsSetting.visibleWhen(this.renderPlacementSetting::getValue);
      this.renderAnimationSetting.visibleWhen(this.renderPlacementSetting::getValue);
      this.renderMoveTimeSetting.visibleWhen(() -> this.renderPlacementSetting.getValue() && this.renderAnimationSetting.getValue());
      instance = this;
      WorldRenderEvents.BEFORE_DEBUG_RENDER.register(CrystalAuraModule::renderWireframes);
   }

   public static void onClientTickStart(MinecraftClient client){
      CrystalAuraModule crystalAuraModule = instance;
      if (crystalAuraModule != null && crystalAuraModule.pendingSwapTick >= 0) {
         crystalAuraModule.resetPendingSwap(client);
      }
   }

   public static void beginManualAttackInput(){
      obsidianPlacementFlag = true;
   }

   public static void endManualAttackInput(){
      obsidianPlacementFlag = false;
   }

   public static void recordManualAttack(PlayerEntity player, Entity target){
      CrystalAuraModule crystalAuraModule = instance;
      if (obsidianPlacementFlag
         && crystalAuraModule != null
         && crystalAuraModule.isEnabled()
         && crystalAuraModule.autoObsidianSetting.getValue()
         && target instanceof PlayerEntity var3
         && var3.isAlive()
         && !FriendModule.isFriend(var3)) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.player == player && client.world != null && client.world.getEntityById(var3.getId()) == var3) {
            crystalAuraModule.resetAllState();
            crystalAuraModule.resetLockState();
            crystalAuraModule.lockTargetId = var3.getId();
            crystalAuraModule.lockStartTick = player.age;
            crystalAuraModule.lockStartY = var3.getY();
            crystalAuraModule.setLockedTarget(var3);
         }
      }
   }

   @Override
   public String getHudName(){
      String var1 = this.lastTargetName;
      return var1 != null && !var1.isBlank() ? this.getName() + "[" + var1 + "]" : this.getName();
   }

   @Override
   protected void onEnable(){
      MinecraftClient client = MinecraftClient.getInstance();
      this.resetFullState(client);
      this.lastTickPlayer = client.player;
      this.lastMode = this.modeSetting.getValue();
      this.syncRenderRotation(client);
   }

   @Override
   protected void onDisable(){
      MinecraftClient client = MinecraftClient.getInstance();
      this.resetFullState(client);
      this.lastTickPlayer = null;
      this.lastMode = null;
      this.syncRenderRotation(client);
   }

   private void syncRenderRotation(MinecraftClient client){
      if (client.player != null) {
         float yaw = client.player.getYaw();
         float pitch = client.player.getPitch();
         client.player.renderYaw = yaw;
         client.player.lastRenderYaw = yaw;
         client.player.renderPitch = pitch;
         client.player.lastRenderPitch = pitch;
      }
   }

   @EventTarget
   public void onPacket(PacketEvent event){
      if (event.isReceive()) {
         this.handlePacket(event.getPacket());
      }
   }

   private void handlePacket(Packet<?> packet){
      if (packet instanceof BundleS2CPacket var2) {
         for (Packet packet2 : var2.getPackets()) {
            this.handleEntitySpawnPacket(packet2);
         }
      } else {
         this.handleEntitySpawnPacket(packet);
      }
   }

   private void handleEntitySpawnPacket(Packet<?> packet){
      if (packet instanceof EntitySpawnS2CPacket var2 && var2.getEntityType() == EntityType.END_CRYSTAL && this.pendingSpawnBase != null) {
         Vec3d vec = getCrystalCenter(this.pendingSpawnBase);
         double x = var2.getX() - vec.x;
         double y = var2.getY() - vec.y;
         double z = var2.getZ() - vec.z;
         if (x * x + y * y + z * z <= 0.25) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (this.modeSetting.is("Rage") && client.player != null && client.world != null && client.player.networkHandler != null) {
               lastSpawnedCrystalId = -1;
               this.attackPending = false;
               this.pendingSpawnBase = null;
               client.execute(
                  () -> {
                     if (this.isEnabled()
                        && this.modeSetting.is("Rage")
                        && client.player != null
                        && client.world != null
                        && client.player.networkHandler != null) {
                        EndCrystalEntity var3 = new EndCrystalEntity(client.world, var2.getX(), var2.getY(), var2.getZ());
                        var3.setId(var2.getEntityId());
                        client.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(var3, client.player.isSneaking()));
                        this.lastBreakTick = client.player.age;
                        if (this.swingHandSetting.getValue() && !this.shouldSkipSwing(client)) {
                           client.player.swingHand(Hand.MAIN_HAND);
                        }
                     }
                  }
               );
               return;
            }

            lastSpawnedCrystalId = var2.getEntityId();
            this.attackPending = false;
            this.pendingSpawnBase = null;
            if (client.player != null) {
               this.respawnGraceTick = client.player.age + 2;
            }
         }
      }
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      if (client.player != null && client.world != null && client.interactionManager != null && !client.player.isDead()) {
         this.handleModeChange(client);
         this.finishPendingSwap(client);
         if (!this.isLockedOnTarget(client)) {
            double value = this.targetRangeSetting.getValue();
            PlayerEntity player = TargetUtil.getClosestTarget(client, value, this.lockedTarget, value + 1.5);
            this.setLockedTarget(player);
            if (player == null) {
               this.resetAllState();
            } else if (client.player.squaredDistanceTo(player) > value * value) {
               this.resetAllState();
            } else if (this.pauseOnEatSetting.getValue() && isPlayerUsingItem(client.player) && !this.shouldSkipSwing(client)) {
               RotationManager.clearRotatingState(ROTATION_STATE);
            } else if (this.modeSetting.is("Rage")) {
               this.tickRageFlow(client, player);
            } else {
               this.tickSMPFlow(client, player);
            }
         }
      } else {
         this.resetFullState(client);
         this.lastTickPlayer = null;
         this.lastMode = null;
      }
   }

   private void tickSMPFlow(MinecraftClient client, PlayerEntity target){
      this.rotationTickCounter++;
      if (this.attackPending && client.player.age >= this.attackDelayTick) {
         this.attackPending = false;
         this.pendingSpawnBase = null;
      }

      this.updateTargetBase(client, target);
      if (!this.hasValidPendingPlacement(client, target)) {
         if (lastSpawnedCrystalId != -1) {
            Entity entity = client.world.getEntityById(lastSpawnedCrystalId);
            if (entity == null) {
               if (client.player.age < this.respawnGraceTick) {
                  return;
               }

               lastSpawnedCrystalId = -1;
            } else {
               if (!entity.isRemoved()) {
                  if (this.canAttackCrystal(client, entity)) {
                     this.attackCrystal(client, entity);
                     return;
                  }

                  return;
               }

               lastSpawnedCrystalId = -1;
            }
         }

         List list = getCrystalsInRadius(client, target, this.breakRadiusSetting.getValueInt());
         EndCrystalEntity crystal = this.getClosestCrystal(client, list, target);
         if (crystal != null) {
            this.attackCrystal(client, crystal);
         } else if (list.isEmpty()) {
            int placeDelay = this.getPlaceDelay();
            if (!isWithinDelay(client.player.age, this.lastPlaceTick, placeDelay)) {
               if (!this.attackPending) {
                  this.executeRagePlacement(client, target);
               }
            }
         }
      }
   }

   private void tickRageFlow(MinecraftClient client, PlayerEntity target){
      lastSpawnedCrystalId = -1;
      this.rotationTickCounter++;
      if (this.attackPending && client.player.age >= this.attackDelayTick) {
         this.attackPending = false;
         this.pendingSpawnBase = null;
      }

      this.updateTargetBase(client, target);
      if (!this.attackPending && this.pendingPlacement == null) {
         int valueInt = this.rageDelaySetting.getValueInt();
         if (!isWithinDelay(client.player.age, this.lastPlaceTick, valueInt)) {
            this.executeRagePlacement(client, target);
         }
      }
   }

   private boolean isLockedOnTarget(MinecraftClient client){
      if (this.lockTargetId == -1) {
         return false;
      } else if (client.world.getEntityById(this.lockTargetId) instanceof PlayerEntity var3 && this.isValidTarget(client, var3)) {
         if (!this.lockFalling) {
            boolean y = var3.getY() > this.lockStartY + 0.2;
            boolean velocity = var3.getVelocity().y > 0.08;
            if (!y && !velocity) {
               if (client.player.age - this.lockStartTick > 10) {
                  this.resetLockState();
                  return false;
               }

               return false;
            }

            this.lockFalling = true;
         }

         if (this.lockBase != null
            || this.pendingObsidianPlacement != null
            || this.getCrystalPlacements(client, var3).isEmpty() && getCrystalsInRadius(client, var3, this.breakRadiusSetting.getValueInt()).isEmpty()) {
            this.setLockedTarget(var3);
            if (this.pauseOnEatSetting.getValue() && isPlayerUsingItem(client.player) && !this.shouldSkipSwing(client)) {
               RotationManager.clearRotatingState(ROTATION_STATE);
               return true;
            } else if (!this.preparePlacement(client, var3)) {
               return true;
            } else {
               this.currentBase = this.lockBase;
               this.updateTargetBase(client, var3);
               if (this.modeSetting.is("Rage")) {
                  this.tickRageFlow(client, var3);
               } else {
                  this.tickSMPPlacement(client, var3);
               }

               return true;
            }
         } else {
            this.resetLockState();
            return false;
         }
      } else {
         this.clearLockedState(client);
         return false;
      }
   }

   private boolean isValidTarget(MinecraftClient client, PlayerEntity target){
      return target != client.player
         && !FriendModule.isFriend(target)
         && target.isAlive()
         && !target.isSpectator()
         && client.world.getEntityById(target.getId()) == target
         && client.player.squaredDistanceTo(target) <= this.targetRangeSetting.getValue() * this.targetRangeSetting.getValue();
   }

   private boolean preparePlacement(MinecraftClient client, PlayerEntity target){
      if (this.lockBase != null) {
         if (client.world.getBlockState(this.lockBase).isOf(Blocks.OBSIDIAN)) {
            this.renderPlacementPos = this.lockBase;
            return true;
         }

         this.lockBase = null;
         this.resetAllState();
      }

      if (this.pendingObsidianPlacement != null) {
         return this.validatePendingObsidianPlacement(client, target);
      } else {
         BlockPos pos3 = target.getBlockPos();
         BlockPos pos2 = null;
         double squaredDistanceTo2 = Double.MAX_VALUE;
         BlockPlacementUtil.Placement var7 = null;
         CrystalAuraModule.CrystalPlacement var8 = null;

         for (Direction direction : PLACE_FACES) {
            BlockPos pos = pos3.offset(direction);
            if (canPlaceCrystalE(client, pos, true)) {
               CrystalAuraModule.CrystalPlacement var14 = this.createPlacement(client, target, pos);
               if (var14 != null) {
                  if (client.world.getBlockState(pos).isOf(Blocks.OBSIDIAN)) {
                     double squaredDistanceTo = client.player.getEyePos().squaredDistanceTo(getCrystalCenter(pos));
                     if (squaredDistanceTo < squaredDistanceTo2) {
                        pos2 = pos.toImmutable();
                        squaredDistanceTo2 = squaredDistanceTo;
                     }
                  } else {
                     BlockPlacementUtil.Placement var20 = BlockPlacementUtil.find(client, pos, this.placeRangeSetting.getValue(), this.raytraceSetting.getValue());
                     if (var20 != null && (var8 == null || var14.score() > var8.score())) {
                        var7 = var20;
                        var8 = var14;
                     }
                  }
               }
            }
         }

         if (pos2 != null) {
            this.lockBase = pos2;
            this.renderPlacementPos = this.lockBase;
            return true;
         } else if (var7 != null && InventoryUtil.findHotBarItem(client, Items.OBSIDIAN) != -1 && this.hasCrystalAvailable(client)) {
            Vec3d vec = var7.hitResult().getPos();
            float eyePos = this.randomizeRotation(RotationUtil.getYaw(client.player.getEyePos(), vec));
            float eyePos2 = this.randomizeRotation(RotationUtil.getPitch(client.player.getEyePos(), vec));
            this.pendingObsidianPlacement = new CrystalAuraModule.PendingObsidianPlacement(target.getId(), var7, eyePos, eyePos2);
            this.swapLockTick = -1;
            this.renderPlacementPos = var7.target();
            this.rotate(eyePos, eyePos2);
            return false;
         } else {
            this.renderPlacementPos = null;
            this.clearRotationIfIdle();
            return false;
         }
      }
   }

   private boolean validatePendingObsidianPlacement(MinecraftClient client, PlayerEntity target){
      CrystalAuraModule.PendingObsidianPlacement var3 = this.pendingObsidianPlacement;
      if (var3 == null) {
         return false;
      } else {
         BlockPos pos = var3.placement().target();
         if (client.world.getBlockState(pos).isOf(Blocks.OBSIDIAN)) {
            this.lockBase = pos;
            this.pendingObsidianPlacement = null;
            this.swapLockTick = -1;
            this.renderPlacementPos = pos;
            return true;
         } else if (this.swapLockTick >= 0) {
            if (client.player.age < this.swapLockTick) {
               return false;
            } else {
               this.pendingObsidianPlacement = null;
               this.swapLockTick = -1;
               return false;
            }
         } else if (target.getId() != var3.targetId()) {
            this.pendingObsidianPlacement = null;
            return false;
         } else {
            BlockPlacementUtil.Placement var5 = BlockPlacementUtil.find(client, pos, this.placeRangeSetting.getValue(), this.raytraceSetting.getValue());
            if (BlockPlacementUtil.sameFace(var3.placement(), var5) && canPlaceCrystalE(client, pos, true)) {
               this.rotate(var3.yaw(), var3.pitch());
               if (!RotationManager.wasRotationSent(var3.yaw(), var3.pitch(), 1.0F)) {
                  return false;
               } else {
                  int findHotBarItem = InventoryUtil.findHotBarItem(client, Items.OBSIDIAN);
                  if (findHotBarItem == -1) {
                     this.pendingObsidianPlacement = null;
                     return false;
                  } else {
                     ActionResult[] var7 = new ActionResult[]{ActionResult.PASS};
                     boolean hitResult = this.swapAndRun(client, findHotBarItem, () -> {
                        var7[0] = client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, var3.placement().hitResult());
                        if (this.swingHandSetting.getValue() && !this.shouldSkipSwing(client)) {
                           client.player.swingHand(Hand.MAIN_HAND);
                        }
                     });
                     if (!hitResult) {
                        return false;
                     } else {
                        if (var7[0].isAccepted()) {
                           this.swapLockTick = client.player.age + 6;
                        } else {
                           this.pendingObsidianPlacement = null;
                        }

                        return false;
                     }
                  }
               }
            } else {
               this.pendingObsidianPlacement = null;
               return false;
            }
         }
      }
   }

   private boolean hasCrystalAvailable(MinecraftClient client){
      return client.player.getMainHandStack().isOf(Items.END_CRYSTAL)
         || client.player.getOffHandStack().isOf(Items.END_CRYSTAL)
         || InventoryUtil.findHotBarItem(client, Items.END_CRYSTAL) != -1
         || this.autoRefillSetting.getValue() && InventoryUtil.findInventoryItem(client, Items.END_CRYSTAL) != -1;
   }

   private void tickSMPPlacement(MinecraftClient client, PlayerEntity target){
      this.rotationTickCounter++;
      if (this.attackPending && client.player.age >= this.attackDelayTick) {
         this.attackPending = false;
         this.pendingSpawnBase = null;
      }

      if (!this.hasValidPendingPlacement(client, target)) {
         if (lastSpawnedCrystalId != -1) {
            Entity entity = client.world.getEntityById(lastSpawnedCrystalId);
            if (entity == null) {
               if (client.player.age < this.respawnGraceTick) {
                  return;
               }

               lastSpawnedCrystalId = -1;
            } else {
               if (!entity.isRemoved()) {
                  if (isCrystalAtBase(entity, this.lockBase) && this.canAttackCrystal(client, entity)) {
                     this.attackCrystal(client, entity);
                     return;
                  }

                  return;
               }

               lastSpawnedCrystalId = -1;
            }
         }

         List list = getCrystalsAtBase(client, this.lockBase);
         if (!list.isEmpty()) {
            EndCrystalEntity crystal = (EndCrystalEntity)list.getFirst();
            if (this.canAttackCrystal(client, crystal)) {
               this.attackCrystal(client, crystal);
            }
         } else {
            int placeDelay = this.getPlaceDelay();
            if (!isWithinDelay(client.player.age, this.lastPlaceTick, placeDelay) && !this.attackPending) {
               this.placeCrystalOnBase(client, target);
            }
         }
      }
   }

   private void placeCrystalOnBase(MinecraftClient client, PlayerEntity target){
      BlockPos pos = this.lockBase;
      if (pos != null && isBaseBlock(client, pos) && this.canPlaceOnBase(client, pos) && this.createPlacement(client, target, pos) != null) {
         CrystalAuraModule.PreparedHand var4 = this.prepareCrystalHand(client);
         if (var4 != null) {
            Direction direction = this.getPlaceDirection(client, pos);
            if (direction != null) {
               Vec3d vec2 = getFaceCenterOffset(pos, direction);
               Vec3d vec = getFaceCenterOffset(pos, Direction.UP);
               float eyePos2 = RotationUtil.getYaw(client.player.getEyePos(), vec);
               float eyePos = RotationUtil.getPitch(client.player.getEyePos(), vec);
               BlockHitResult hitResult = new BlockHitResult(vec2, direction, pos, false);
               this.pendingPlacement = new CrystalAuraModule.PendingPlacement(target.getId(), pos, var4.hand(), var4.silentSlot(), hitResult, eyePos2, eyePos);
               this.currentBase = pos;
               this.renderPlacementPos = pos;
               this.rotate(eyePos2, eyePos);
               if (this.getPlaceDelay() == 0) {
                  this.sendLookPacket(client, eyePos2, eyePos);
                  this.hasValidPendingPlacement(client, target);
               }
            }
         }
      } else {
         this.renderPlacementPos = null;
         this.clearRotationIfIdle();
      }
   }

   private static List<EndCrystalEntity> getCrystalsAtBase(MinecraftClient client, BlockPos base){
      if (base == null) {
         return List.of();
      } else {
         Box Box = new Box(base.up()).expand(0.75, 1.0, 0.75);
         return client.world.getEntitiesByClass(EndCrystalEntity.class, Box, crystal -> !crystal.isRemoved() && isCrystalAtBase(crystal, base));
      }
   }

   private static boolean isCrystalAtBase(Entity crystal, BlockPos base){
      return crystal instanceof EndCrystalEntity && base != null && crystal.getBlockPos().down().equals(base);
   }

   private void clearLockedState(MinecraftClient client){
      this.resetAllState();
      this.resetLockState();
      this.syncRenderRotation(client);
   }

   private void resetLockState(){
      this.lockTargetId = -1;
      this.lockStartTick = -1;
      this.lockStartY = 0.0;
      this.lockFalling = false;
      this.lockBase = null;
      this.pendingObsidianPlacement = null;
      this.swapLockTick = -1;
      this.setLockedTarget(null);
   }

   private void setLockedTarget(PlayerEntity target){
      this.lockedTarget = target;
      this.lastTargetName = target == null ? null : target.getName().getString();
   }

   private float randomizeRotation(float value){
      float valueFloat = this.rotateRandomizeSetting.getValueFloat();
      return valueFloat <= 0.0F ? value : value + (RANDOM.nextFloat() - 0.5F) * 2.0F * valueFloat;
   }

   private void executeRagePlacement(MinecraftClient client, PlayerEntity target){
      CrystalAuraModule.PreparedHand var3 = this.prepareCrystalHand(client);
      if (var3 == null) {
         this.renderPlacementPos = null;
      } else {
         for (CrystalAuraModule.CrystalPlacement crystalPlacement : this.getCrystalPlacementsE(client, target, this.modeSetting.is("Rage"))) {
            Direction direction = this.getPlaceDirection(client, crystalPlacement.base());
            if (direction != null) {
               Vec3d vec2 = getFaceCenterOffset(crystalPlacement.base(), direction);
               Vec3d vec = getFaceCenterOffset(crystalPlacement.base(), Direction.UP);
               float eyePos = RotationUtil.getYaw(client.player.getEyePos(), vec);
               float eyePos2 = RotationUtil.getPitch(client.player.getEyePos(), vec);
               BlockHitResult hitResult = new BlockHitResult(vec2, direction, crystalPlacement.base(), false);
               this.pendingPlacement = new CrystalAuraModule.PendingPlacement(target.getId(), crystalPlacement.base(), var3.hand(), var3.silentSlot(), hitResult, eyePos, eyePos2);
               this.currentBase = crystalPlacement.base();
               this.renderPlacementPos = crystalPlacement.base();
               this.rotate(eyePos, eyePos2);
               if (this.modeSetting.is("Rage")) {
                  this.sendLookPacket(client, eyePos, eyePos2);
                  this.executePendingPlacement(client);
               } else if (this.getPlaceDelay() == 0) {
                  this.sendLookPacket(client, eyePos, eyePos2);
                  this.hasValidPendingPlacement(client, target);
               }

               return;
            }
         }

         this.currentBase = null;
         this.renderPlacementPos = null;
         this.clearRotationIfIdle();
      }
   }

   private List<CrystalAuraModule.CrystalPlacement> getCrystalPlacements(MinecraftClient client, PlayerEntity target){
      return this.getCrystalPlacementsE(client, target, false);
   }

   private List<CrystalAuraModule.CrystalPlacement> getCrystalPlacementsE(MinecraftClient client, PlayerEntity target, boolean ignoreCrystals){
      List<BlockPos> list = this.findBasePositions(client, target, this.placeRadiusSetting.getValueInt(), ignoreCrystals);
      List<CrystalAuraModule.CrystalPlacement> var5 = new ArrayList<>();

      for (BlockPos pos : list) {
         CrystalAuraModule.CrystalPlacement var8 = this.createPlacement(client, target, pos);
         if (var8 != null) {
            var5.add(var8);
         }
      }

      var5.sort((a, b) -> {
         if (a.lethal() != b.lethal()) {
            return a.lethal() ? -1 : 1;
         } else {
            int score = Double.compare(b.score(), a.score());
            return score != 0 ? score : Double.compare(a.attackDistance(), b.attackDistance());
         }
      });
      return var5;
   }

   private CrystalAuraModule.PreparedHand prepareCrystalHand(MinecraftClient client){
      if (client.player.age < this.handSwitchCooldown) {
         return null;
      } else {
         String value = this.swapModeSetting.getValue();

         return switch (value) {
            case "OffHand" -> this.prepareOffHandCrystal(client);
            case "Silent" -> this.prepareAnyHandCrystal(client);
            default -> this.prepareMainHandCrystal(client);
         };
      }
   }

   private CrystalAuraModule.PreparedHand prepareMainHandCrystal(MinecraftClient client){
      if (!client.player.getMainHandStack().isOf(Items.END_CRYSTAL)) {
         int findHotBarItem = InventoryUtil.findHotBarItem(client, Items.END_CRYSTAL);
         if (findHotBarItem == -1 && this.autoRefillSetting.getValue()) {
            findHotBarItem = this.findCrystalForSlot(client, client.player.getInventory().getSelectedSlot());
            if (findHotBarItem != -1) {
               this.setPreferredHotbarSlot(client, findHotBarItem);
            }

            return null;
         }

         if (findHotBarItem == -1) {
            return null;
         }

         this.preferredHotbarSlot = findHotBarItem;
         this.selectSlot(client, findHotBarItem);
      } else {
         this.preferredHotbarSlot = client.player.getInventory().getSelectedSlot();
      }

      return new CrystalAuraModule.PreparedHand(Hand.MAIN_HAND, -1);
   }

   private CrystalAuraModule.PreparedHand prepareOffHandCrystal(MinecraftClient client){
      if (client.player.getOffHandStack().isOf(Items.END_CRYSTAL)) {
         return new CrystalAuraModule.PreparedHand(Hand.OFF_HAND, -1);
      } else {
         int findHotBarItem = InventoryUtil.findHotBarItem(client, Items.END_CRYSTAL);
         if (findHotBarItem == -1 && this.autoRefillSetting.getValue()) {
            int var3 = findCrystalInInventory(client);
            if (var3 != -1 && this.swapOffHandWithInventory(client, var3)) {
               this.setPreferredHotbarSlot(client, -1);
            }

            return null;
         } else {
            if (findHotBarItem != -1) {
               this.moveItemToHotbar(client, findHotBarItem);
               if (client.player.getOffHandStack().isOf(Items.END_CRYSTAL)) {
                  this.preferredHotbarSlot = findHotBarItem;
                  return new CrystalAuraModule.PreparedHand(Hand.OFF_HAND, -1);
               }
            }

            return null;
         }
      }
   }

   private CrystalAuraModule.PreparedHand prepareAnyHandCrystal(MinecraftClient client){
      int findHotBarItem = InventoryUtil.findHotBarItem(client, Items.END_CRYSTAL);
      if (findHotBarItem == -1 && this.autoRefillSetting.getValue()) {
         int var3 = this.getPreferredHotbarSlot(client);
         findHotBarItem = this.findCrystalForSlot(client, var3);
         if (findHotBarItem != -1) {
            this.setPreferredHotbarSlot(client, findHotBarItem);
         }

         return null;
      } else if (findHotBarItem == -1) {
         return null;
      } else {
         this.preferredHotbarSlot = findHotBarItem;
         return new CrystalAuraModule.PreparedHand(Hand.MAIN_HAND, findHotBarItem);
      }
   }

   private int findCrystalForSlot(MinecraftClient client, int targetHotbarSlot){
      int var3 = findCrystalInInventory(client);
      if (var3 != -1 && targetHotbarSlot >= 0 && targetHotbarSlot <= 8) {
         return this.swapItemToSlot(client, var3, 36 + targetHotbarSlot)
               && client.player.getInventory().getStack(targetHotbarSlot).isOf(Items.END_CRYSTAL)
            ? targetHotbarSlot
            : -1;
      } else {
         return -1;
      }
   }

   private int getPreferredHotbarSlot(MinecraftClient client){
      if (this.preferredHotbarSlot >= 0 && this.preferredHotbarSlot <= 8 && client.player.getInventory().getStack(this.preferredHotbarSlot).isEmpty()) {
         return this.preferredHotbarSlot;
      } else {
         int findEmptyHotbarSlot = this.findEmptyHotbarSlot(client);
         return findEmptyHotbarSlot != -1 ? findEmptyHotbarSlot : client.player.getInventory().getSelectedSlot();
      }
   }

   private static int findCrystalInInventory(MinecraftClient client){
      for (int index = 9; index < 36; index++) {
         if (client.player.getInventory().getStack(index).isOf(Items.END_CRYSTAL)) {
            return index;
         }
      }

      return -1;
   }

   private void setPreferredHotbarSlot(MinecraftClient client, int hotbarSlot){
      if (hotbarSlot >= 0 && hotbarSlot <= 8) {
         this.preferredHotbarSlot = hotbarSlot;
      }

      this.handSwitchCooldown = client.player.age + 1;
   }

   private int findEmptyHotbarSlot(MinecraftClient client){
      for (int index = 0; index < 9; index++) {
         if (client.player.getInventory().getStack(index).isEmpty()) {
            return index;
         }
      }

      return -1;
   }

   private void moveItemToHotbar(MinecraftClient client, int hotbarSlot){
      if (hotbarSlot >= 0 && hotbarSlot <= 8) {
         int var3 = 36 + hotbarSlot;
         int var4 = client.player.playerScreenHandler.syncId;
         client.interactionManager.clickSlot(var4, var3, 40, SlotActionType.SWAP, client.player);
      }
   }

   private boolean swapOffHandWithInventory(MinecraftClient client, int inventoryInde){
      if (inventoryInde >= 9 && inventoryInde < 36) {
         int var4 = client.player.playerScreenHandler.syncId;
         client.interactionManager.clickSlot(var4, inventoryInde, 40, SlotActionType.SWAP, client.player);
         return client.player.getOffHandStack().isOf(Items.END_CRYSTAL);
      } else {
         return false;
      }
   }

   private boolean swapItemToSlot(MinecraftClient client, int inventoryInde, int targetContainerSlot){
      if (inventoryInde >= 0
         && inventoryInde < 36
         && targetContainerSlot >= 0
         && targetContainerSlot < client.player.playerScreenHandler.slots.size()
         && client.player.playerScreenHandler.getCursorStack().isEmpty()) {
         int var4 = inventoryInde < 9 ? 36 + inventoryInde : inventoryInde;
         int var5 = client.player.playerScreenHandler.syncId;
         client.interactionManager.clickSlot(var5, var4, 0, SlotActionType.PICKUP, client.player);
         client.interactionManager.clickSlot(var5, targetContainerSlot, 0, SlotActionType.PICKUP, client.player);
         if (!client.player.playerScreenHandler.getCursorStack().isEmpty()) {
            client.interactionManager.clickSlot(var5, var4, 0, SlotActionType.PICKUP, client.player);
         }

         return client.player.playerScreenHandler.getCursorStack().isEmpty();
      } else {
         return false;
      }
   }

   private List<BlockPos> findBasePositions(MinecraftClient client, PlayerEntity target, int radius, boolean ignoreCrystals){
      BlockPos pos = target.getBlockPos();
      ArrayList var6 = new ArrayList();

      for (int index3 = pos.getX() - radius; index3 <= pos.getX() + radius; index3++) {
         for (int index2 = pos.getY() - radius; index2 <= pos.getY() + radius; index2++) {
            for (int index = pos.getZ() - radius; index <= pos.getZ() + radius; index++) {
               BlockPos pos2 = new BlockPos(index3, index2, index);
               if (isBaseBlock(client, pos2) && canPlaceCrystalE(client, pos2, ignoreCrystals)) {
                  var6.add(pos2);
               }
            }
         }
      }

      return var6;
   }

   private CrystalAuraModule.CrystalPlacement createPlacement(MinecraftClient client, PlayerEntity target, BlockPos pos){
      Vec3d vec = getCrystalCenter(pos);
      Box Box = getCrystalBo(pos);
      if (!this.isInBreakRange(client, Box, 0.15)) {
         return null;
      } else {
         double eyePos = Math.sqrt(Box.squaredMagnitude(client.player.getEyePos()));
         float calculate2 = CrystalDamageUtil.calculate(client.world, vec, target);
         if (calculate2 <= 0.0F) {
            return null;
         } else {
            float calculate = CrystalDamageUtil.calculate(client.world, vec, client.player);
            float absorptionAmount = target.getHealth() + target.getAbsorptionAmount();
            float absorptionAmount2 = client.player.getHealth() + client.player.getAbsorptionAmount();
            boolean var12 = calculate2 >= absorptionAmount;
            boolean value = absorptionAmount <= this.facePlaceHealthSetting.getValue();
            boolean y = this.verticalPlaceSetting.getValue() && target.getY() - client.player.getY() >= 1.5 && calculate2 > calculate;
            if (calculate >= absorptionAmount2 - this.safetyMarginSetting.getValue()) {
               return null;
            } else if (!var12 && !value && !y && calculate2 < this.minDamageSetting.getValue()) {
               return null;
            } else if (calculate > this.maxSelfDamageSetting.getValue() && (!var12 || !this.lethalOverrideSetting.getValue()) && !y) {
               return null;
            } else {
               double var15 = calculate2 - calculate - eyePos * 0.75;
               return new CrystalAuraModule.CrystalPlacement(pos, calculate2, calculate, eyePos, var15, var12);
            }
         }
      }
   }

   private boolean hasValidPendingPlacement(MinecraftClient client, PlayerEntity target){
      CrystalAuraModule.PendingPlacement var3 = this.pendingPlacement;
      if (var3 == null) {
         return false;
      } else if (target.getId() == var3.targetId() && isBaseBlock(client, var3.base()) && canPlaceCrystal(client, var3.base()) && isPendingPlacementValid(client, var3)) {
         Direction direction = this.getPlaceDirection(client, var3.base());
         if (direction != var3.hitResult().getSide()) {
            this.resetPlacementState();
            return false;
         } else {
            this.rotate(var3.yaw(), var3.pitch());
            if (!RotationManager.wasRotationSent(var3.yaw(), var3.pitch(), 1.0F)) {
               return true;
            } else {
               boolean silentSlot2 = this.isSilentSwapNeeded(client, var3.silentSlot());
               ActionResult[] var6 = new ActionResult[]{ActionResult.PASS};
               Runnable runnable = () -> {
                  var6[0] = silentSlot2
                     ? this.interactBlock(client, var3.hand(), var3.hitResult())
                     : client.interactionManager.interactBlock(client.player, var3.hand(), var3.hitResult());
                  if (this.swingHandSetting.getValue() && !silentSlot2) {
                     client.player.swingHand(Hand.MAIN_HAND);
                  }
               };
               if (var3.silentSlot() >= 0) {
                  boolean silentSlot = this.swapAndRun(client, var3.silentSlot(), runnable);
                  if (!silentSlot) {
                     return true;
                  }
               }

               if (var3.silentSlot() < 0) {
                  runnable.run();
               }

               if (var6[0].isAccepted()) {
                  this.lastPlaceTick = client.player.age;
                  this.attackPending = true;
                  this.attackDelayTick = client.player.age + 6;
                  this.pendingSpawnBase = var3.base();
               }

               this.pendingPlacement = null;
               this.currentBase = var3.base();
               this.lookAtBase(client, this.currentBase);
               return true;
            }
         }
      } else {
         this.resetPlacementState();
         return false;
      }
   }

   private void executePendingPlacement(MinecraftClient client){
      CrystalAuraModule.PendingPlacement var2 = this.pendingPlacement;
      if (var2 != null && isBaseBlock(client, var2.base()) && canPlaceCrystalE(client, var2.base(), true) && isPendingPlacementValid(client, var2)) {
         boolean silentSlot = this.isSilentSwapNeeded(client, var2.silentSlot());
         ActionResult[] var4 = new ActionResult[]{ActionResult.PASS};
         Runnable runnable = () -> var4[0] = silentSlot
            ? this.interactBlock(client, var2.hand(), var2.hitResult())
            : client.interactionManager.interactBlock(client.player, var2.hand(), var2.hitResult());
         if (var2.silentSlot() < 0 || this.swapAndRun(client, var2.silentSlot(), runnable)) {
            if (var2.silentSlot() < 0) {
               runnable.run();
            }

            if (this.swingHandSetting.getValue() && !silentSlot) {
               client.player.swingHand(var2.hand());
            }

            if (var4[0].isAccepted()) {
               this.lastPlaceTick = client.player.age;
               this.attackPending = true;
               this.attackDelayTick = client.player.age + 6;
               this.pendingSpawnBase = var2.base();
            }

            this.pendingPlacement = null;
            this.currentBase = var2.base();
            this.lookAtBase(client, this.currentBase);
         }
      } else {
         this.resetPlacementState();
      }
   }

   private boolean canReachPlaceFace(MinecraftClient client, BlockPos base, Direction face){
      Vec3d vec3 = getFaceCenterOffset(base, face);
      double blockInteractionRange = Math.min(this.placeRangeSetting.getValue(), client.player.getBlockInteractionRange());
      if (client.player.getEyePos().squaredDistanceTo(vec3) > blockInteractionRange * blockInteractionRange) {
         return false;
      } else if (!this.modeSetting.is("Rage") && this.raytraceSetting.getValue()) {
         Vec3d vec2 = Vec3d.of(face.getVector());
         Vec3d vec = vec3.subtract(vec2.multiply(0.01));
         RaycastContext raycastContext = new RaycastContext(client.player.getEyePos(), vec, ShapeType.COLLIDER, FluidHandling.NONE, client.player);
         BlockHitResult hitResult = client.world.raycast(raycastContext);
         return hitResult.getType() == Type.BLOCK && hitResult.getBlockPos().equals(base) && hitResult.getSide() == face;
      } else {
         return true;
      }
   }

   private Direction getPlaceDirection(MinecraftClient client, BlockPos pos){
      String value = this.placeModeSetting.getValue();
      if (value.equals("Top")) {
         return this.canReachPlaceFace(client, pos, Direction.UP) ? Direction.UP : null;
      } else if (value.equals("Side")) {
         return this.getBestPlaceDirection(client, pos);
      } else if (!this.raytraceSetting.getValue()) {
         Direction direction = getClosestFaceTo(client.player.getEyePos(), pos);
         return this.canReachPlaceFace(client, pos, direction) ? direction : null;
      } else if (this.canReachPlaceFace(client, pos, Direction.UP)) {
         return Direction.UP;
      } else {
         Direction direction2 = this.getBestPlaceDirection(client, pos);
         if (direction2 != null) {
            return direction2;
         } else {
            return this.canReachPlaceFace(client, pos, Direction.DOWN) ? Direction.DOWN : null;
         }
      }
   }

   private static Direction getClosestFaceTo(Vec3d eye, BlockPos base){
      Vec3d vec = Vec3d.ofCenter(base);
      double var3 = eye.x - vec.x;
      double var5 = eye.y - vec.y;
      double var7 = eye.z - vec.z;
      double abs = Math.abs(var3);
      double abs3 = Math.abs(var5);
      double abs2 = Math.abs(var7);
      if (abs3 >= abs && abs3 >= abs2) {
         return var5 >= 0.0 ? Direction.UP : Direction.DOWN;
      } else if (abs >= abs2) {
         return var3 >= 0.0 ? Direction.EAST : Direction.WEST;
      } else {
         return var7 >= 0.0 ? Direction.SOUTH : Direction.NORTH;
      }
   }

   private Direction getBestPlaceDirection(MinecraftClient client, BlockPos pos){
      Direction direction2 = null;
      double squaredDistanceTo2 = Double.MAX_VALUE;
      Vec3d vec = client.player.getEyePos();

      for (Direction direction : Direction.values()) {
         if (direction != Direction.UP && direction != Direction.DOWN) {
            Vec3d vec2 = getFaceCenterOffset(pos, direction);
            double squaredDistanceTo = vec.squaredDistanceTo(vec2);
            if (!(squaredDistanceTo >= squaredDistanceTo2) && this.canReachPlaceFace(client, pos, direction)) {
               direction2 = direction;
               squaredDistanceTo2 = squaredDistanceTo;
            }
         }
      }

      return direction2;
   }

   private void rotate(float yaw, float pitch){
      this.rotationTickCounter = 0;
      RotationManager.setRotation(ROTATION_STATE, yaw, pitch, true, this.movementFixSetting.getValue());
   }

   private void updateTargetBase(MinecraftClient client, PlayerEntity target){
      if (this.pendingPlacement == null) {
         BlockPos pos = this.getPreferredBase(client, target);
         if (pos == null) {
            this.currentBase = null;
         } else {
            this.currentBase = pos.toImmutable();
            this.lookAtBase(client, this.currentBase);
         }
      }
   }

   private BlockPos getPreferredBase(MinecraftClient client, PlayerEntity target){
      if (this.lockBase != null && isBaseBlock(client, this.lockBase)) {
         return this.lockBase;
      } else if (this.pendingSpawnBase != null && isBaseBlock(client, this.pendingSpawnBase)) {
         return this.pendingSpawnBase;
      } else if (this.isBasePlaceable(client, target, this.currentBase)) {
         return this.currentBase;
      } else {
         Entity entity = lastSpawnedCrystalId == -1 ? null : client.world.getEntityById(lastSpawnedCrystalId);
         if (entity instanceof EndCrystalEntity && !entity.isRemoved()) {
            BlockPos pos2 = entity.getBlockPos().down();
            if (this.isBasePlaceable(client, target, pos2)) {
               return pos2;
            }
         }

         CrystalAuraModule.CrystalPlacement var10 = null;
         int breakRadius = Math.max(this.placeRadiusSetting.getValueInt(), this.getBreakRadius());

         for (EndCrystalEntity crystal : getCrystalsInRadius(client, target, breakRadius)) {
            BlockPos pos = crystal.getBlockPos().down();
            if (isBaseBlock(client, pos)) {
               CrystalAuraModule.CrystalPlacement var9 = this.createPlacement(client, target, pos);
               if (var9 != null && (var10 == null || var9.score() > var10.score())) {
                  var10 = var9;
               }
            }
         }

         if (var10 != null) {
            return var10.base();
         } else {
            List list = this.getCrystalPlacements(client, target);
            return list.isEmpty() ? null : ((CrystalAuraModule.CrystalPlacement)list.getFirst()).base();
         }
      }
   }

   private boolean isBasePlaceable(MinecraftClient client, PlayerEntity target, BlockPos base){
      return base != null && isBaseBlock(client, base) && this.createPlacement(client, target, base) != null;
   }

   private void lookAtBase(MinecraftClient client, BlockPos base){
      Vec3d vec = getFaceCenterOffset(base, Direction.UP);
      this.rotate(RotationUtil.getYaw(client.player.getEyePos(), vec), RotationUtil.getPitch(client.player.getEyePos(), vec));
   }

   private static Vec3d getFaceCenterOffset(BlockPos pos, Direction face){
      return Vec3d.ofCenter(pos).add(face.getOffsetX() * 0.5, face.getOffsetY() * 0.5, face.getOffsetZ() * 0.5);
   }

   private static List<EndCrystalEntity> getCrystalsInRadius(MinecraftClient client, PlayerEntity target, int radius){
      Box Box = target.getBoundingBox().expand(radius);
      return client.world.getEntitiesByClass(EndCrystalEntity.class, Box, entity -> !entity.isRemoved());
   }

   private EndCrystalEntity getClosestCrystal(MinecraftClient client, List<EndCrystalEntity> crystals, PlayerEntity target){
      EndCrystalEntity crystal2 = null;
      double distanceTo2 = Double.MAX_VALUE;

      for (EndCrystalEntity crystal : crystals) {
         if (this.canAttackCrystal(client, crystal)) {
            double distanceTo = target.distanceTo(crystal);
            if (distanceTo < distanceTo2) {
               distanceTo2 = distanceTo;
               crystal2 = crystal;
            }
         }
      }

      return crystal2;
   }

   private boolean canAttackCrystal(MinecraftClient client, Entity crystal){
      if (crystal != null && !crystal.isRemoved()) {
         int breakDelay = this.getBreakDelay();
         if (isWithinDelay(client.player.age, this.lastBreakTick, breakDelay)) {
            return false;
         } else {
            Box Box = crystal.getBoundingBox();
            if (!this.isInBreakRange(client, Box, 0.0)) {
               return false;
            } else if (!this.raytraceSetting.getValue()) {
               return true;
            } else {
               Vec3d vec = Box.getCenter();
               BlockHitResult hitResult = client.world
                  .raycast(new RaycastContext(client.player.getEyePos(), vec, ShapeType.COLLIDER, FluidHandling.NONE, client.player));
               return hitResult.getType() == Type.MISS;
            }
         }
      } else {
         return false;
      }
   }

   private void attackCrystal(MinecraftClient client, Entity crystal){
      this.attackCrystalPacket(client, crystal);
   }

   private void attackCrystalPacket(MinecraftClient client, Entity crystal){
      client.interactionManager.attackEntity(client.player, crystal);
      if (this.swingHandSetting.getValue() && !this.shouldSkipSwing(client)) {
         client.player.swingHand(Hand.MAIN_HAND);
      }

      this.lastBreakTick = client.player.age;
   }

   private int getBreakRadius(){
      return this.breakRadiusSetting.getValueInt();
   }

   private void sendLookPacket(MinecraftClient client, float yaw, float pitch){
      client.player.networkHandler.sendPacket(new LookAndOnGround(yaw, pitch, client.player.isOnGround(), client.player.horizontalCollision));
      RotationManager.markRotationSent(yaw, pitch);
   }

   private int getPlaceDelay(){
      return this.placeDelaySetting.getValueInt();
   }

   private int getBreakDelay(){
      return this.breakDelaySetting.getValueInt();
   }

   private static boolean isWithinDelay(int currentTick, int previousTick, int delay){
      return delay > 0 && previousTick >= 0 && currentTick >= previousTick && (long)currentTick - previousTick < delay;
   }

   private static Vec3d getCrystalCenter(BlockPos base){
      return new Vec3d(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5);
   }

   private static Box getCrystalBo(BlockPos base){
      return EntityType.END_CRYSTAL.getDimensions().getBoxAt(getCrystalCenter(base));
   }

   private boolean isInBreakRange(MinecraftClient client, Box Box, double safety){
      double value = Math.max(0.0, this.breakRangeSetting.getValue() - safety);
      return Box.squaredMagnitude(client.player.getEyePos()) > value * value ? false : client.player.canAttackEntityIn(Box, Math.max(0.0, 3.0 - safety));
   }

   private static boolean isBaseBlock(MinecraftClient client, BlockPos pos){
      BlockState state = client.world.getBlockState(pos);
      return state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.BEDROCK);
   }

   private boolean canPlaceOnBase(MinecraftClient client, BlockPos base){
      return canPlaceCrystal(client, base);
   }

   private static boolean canPlaceCrystal(MinecraftClient client, BlockPos base){
      return canPlaceCrystalE(client, base, false);
   }

   private static boolean canPlaceCrystalE(MinecraftClient client, BlockPos base, boolean ignoreCrystals){
      BlockPos pos = base.up();
      if (!client.world.getBlockState(pos).isAir()) {
         return false;
      } else {
         Box Box = new Box(
            pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 2.0, pos.getZ() + 1.0
         );
         return client.world.getOtherEntities(null, Box, entity -> !ignoreCrystals || !(entity instanceof EndCrystalEntity)).isEmpty();
      }
   }

   private void resetPlacementState(){
      this.pendingPlacement = null;
      this.currentBase = null;
      this.renderPlacementPos = null;
   }

   private void clearRotationIfIdle(){
      if (this.pendingPlacement == null && !this.attackPending && this.rotationTickCounter >= 4) {
         RotationManager.clearRotatingState(ROTATION_STATE);
      }
   }

   private void resetAllState(){
      this.resetPendingSwap(MinecraftClient.getInstance());
      this.pendingPlacement = null;
      lastSpawnedCrystalId = -1;
      this.attackPending = false;
      this.pendingSpawnBase = null;
      this.rotationTickCounter = 0;
      this.currentBase = null;
      this.renderPlacementPos = null;
      RotationManager.clearRotatingState(ROTATION_STATE);
   }

   private void resetFullState(MinecraftClient client){
      this.resetAllState();
      this.resetLockState();
      this.lastPlaceTick = -1;
      this.lastBreakTick = -1;
      this.attackDelayTick = -1;
      this.respawnGraceTick = -1;
      this.preferredHotbarSlot = -1;
      this.handSwitchCooldown = -1;
      obsidianPlacementFlag = false;
      this.resetRenderAnim();
   }

   private void handleModeChange(MinecraftClient client){
      String value = this.modeSetting.getValue();
      if (this.lastTickPlayer != client.player || !value.equals(this.lastMode)) {
         this.resetFullState(client);
         this.lastTickPlayer = client.player;
         this.lastMode = value;
      }
   }

   private boolean swapAndRun(MinecraftClient client, int slot, Runnable action){
      if (slot >= 0 && slot <= 8) {
         int selectedSlot = client.player.getInventory().getSelectedSlot();
         boolean silentSwapNeeded = this.isSilentSwapNeeded(client, slot);
         int serverSlot = this.pendingSwapTick >= 0 ? this.pendingSwapTick : SilentSlotManager.getServerSlot(client);
         if (serverSlot < 0 || serverSlot > 8) {
            serverSlot = selectedSlot;
         }

         SilentSlotManager.selectServerSlot(client, slot);

         try {
            if (!silentSwapNeeded && selectedSlot != slot) {
               client.player.getInventory().setSelectedSlot(slot);
            }

            action.run();
         } finally {
            if (!silentSwapNeeded && selectedSlot != slot && client.player != null) {
               client.player.getInventory().setSelectedSlot(selectedSlot);
            }

            if (silentSwapNeeded) {
               this.pendingSwapTick = -1;
               this.pendingSwapUntil = -1;
               SilentSlotManager.selectServerSlot(client, serverSlot);
            } else {
               this.pendingSwapTick = serverSlot;
               this.pendingSwapUntil = client.player.age + 1;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private boolean shouldSkipSwing(MinecraftClient client){
      if (this.multiTaskSetting.getValue() && this.swapModeSetting.is("Silent") && client != null && client.player != null) {
         if (client.player.isUsingItem() && client.player.getActiveHand() == Hand.MAIN_HAND) {
            return true;
         } else if (client.options != null && client.options.useKey.isPressed()) {
            UseAction useAction = client.player.getMainHandStack().getUseAction();
            return useAction == UseAction.EAT || useAction == UseAction.DRINK;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean isSilentSwapNeeded(MinecraftClient client, int silentSlot){
      return this.shouldSkipSwing(client) && silentSlot >= 0 && silentSlot != client.player.getInventory().getSelectedSlot();
   }

   private ActionResult interactBlock(MinecraftClient client, Hand hand, BlockHitResult hitResult){
      PendingUpdateManager pendingUpdateManager = ((ClientWorldAccessor)client.world).astatine$getPendingUpdateManager().incrementSequence();

      try {
         client.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(hand, hitResult, pendingUpdateManager.getSequence()));
      } catch (Throwable e) {
         if (pendingUpdateManager != null) {
            try {
               pendingUpdateManager.close();
            } catch (Throwable e2) {
               e.addSuppressed(e2);
            }
         }

         throw e;
      }

      if (pendingUpdateManager != null) {
         pendingUpdateManager.close();
      }

      return ActionResult.SUCCESS;
   }

   private void finishPendingSwap(MinecraftClient client){
      if (this.pendingSwapTick >= 0 && client.player.age >= this.pendingSwapUntil) {
         this.resetPendingSwap(client);
      }
   }

   private void resetPendingSwap(MinecraftClient client){
      int var2 = this.pendingSwapTick;
      this.pendingSwapTick = -1;
      this.pendingSwapUntil = -1;
      if (var2 >= 0 && var2 <= 8 && client != null && client.player != null && client.world != null) {
         SilentSlotManager.selectServerSlot(client, var2);
      }
   }

   private static void renderWireframes(WorldRenderContext context){
      CrystalAuraModule crystalAuraModule = instance;
      if (crystalAuraModule != null && crystalAuraModule.isEnabled() && crystalAuraModule.renderPlacementSetting.getValue()) {
         if (crystalAuraModule.renderPlacementPos == null) {
            crystalAuraModule.resetRenderAnim();
         } else {
            BlockPos pos = crystalAuraModule.renderPlacementPos;
            Vec3d vec;
            if (crystalAuraModule.renderAnimationSetting.getValue()) {
               vec = crystalAuraModule.getRenderAnimPosition(pos, System.nanoTime());
            } else {
               crystalAuraModule.resetRenderAnim();
               vec = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
            }

            boolean is2 = !crystalAuraModule.renderModeSetting.is("Outline");
            boolean is = !crystalAuraModule.renderModeSetting.is("Fill");
            int value = crystalAuraModule.renderColorSetting.getValue();
            double var7 = 0.01;
            RenderUtil.drawWorldBo(
               context,
               vec.x + var7,
               vec.y + var7,
               vec.z + var7,
               vec.x + 1.0 - var7,
               vec.y + 1.0 - var7,
               vec.z + 1.0 - var7,
               ColorUtil.withAlpha(value, crystalAuraModule.fillAlphaSetting.getValueInt()),
               ColorUtil.withAlpha(value, crystalAuraModule.outlineAlphaSetting.getValueInt()),
               is2,
               is,
               crystalAuraModule.renderThroughWallsSetting.getValue(),
               crystalAuraModule.lineWidthSetting.getValueFloat()
            );
         }
      } else {
         if (crystalAuraModule != null) {
            crystalAuraModule.resetRenderAnim();
         }
      }
   }

   private Vec3d getRenderAnimPosition(BlockPos target, long nowNanos){
      Vec3d vec2 = new Vec3d(target.getX(), target.getY(), target.getZ());
      if (this.renderAnimStart != null && this.renderAnimFrom != null && this.renderAnimTo != null) {
         if (!target.equals(this.renderAnimStart)) {
            Vec3d vec = this.getRenderAnimProgress(nowNanos);
            this.renderAnimStart = target;
            this.renderAnimFrom = vec;
            this.renderAnimTo = vec2;
            this.renderAnimStartNanos = nowNanos;
         }

         return this.getRenderAnimProgress(nowNanos);
      } else {
         this.renderAnimStart = target;
         this.renderAnimFrom = vec2;
         this.renderAnimTo = vec2;
         this.renderAnimStartNanos = nowNanos;
         return vec2;
      }
   }

   private Vec3d getRenderAnimProgress(long nowNanos){
      if (this.renderAnimFrom != null && this.renderAnimTo != null) {
         double value = this.renderMoveTimeSetting.getValue() * 1000000.0;
         double max = value <= 0.0 ? 1.0 : (nowNanos - this.renderAnimStartNanos) / value;
         if (max >= 1.0) {
            this.renderAnimFrom = this.renderAnimTo;
            return this.renderAnimTo;
         } else {
            max = Math.max(0.0, max);
            double var7 = 1.0 - max;
            double var9 = 1.0 - var7 * var7 * var7;
            return this.renderAnimFrom.lerp(this.renderAnimTo, var9);
         }
      } else {
         return Vec3d.ZERO;
      }
   }

   private void resetRenderAnim(){
      this.renderAnimStart = null;
      this.renderAnimFrom = null;
      this.renderAnimTo = null;
      this.renderAnimStartNanos = 0L;
   }

   private void selectSlot(MinecraftClient client, int slot){
      if (client.player.getInventory().getSelectedSlot() != slot) {
         client.player.getInventory().setSelectedSlot(slot);
         client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
      }
   }

   private static boolean isPlayerUsingItem(PlayerEntity player){
      if (!player.isUsingItem()) {
         return false;
      } else {
         UseAction useAction = player.getActiveItem().getUseAction();
         return useAction == UseAction.EAT || useAction == UseAction.DRINK;
      }
   }

   private static boolean isPendingPlacementValid(MinecraftClient client, CrystalAuraModule.PendingPlacement pending){
      return pending.silentSlot() >= 0
         ? client.player.getInventory().getStack(pending.silentSlot()).isOf(Items.END_CRYSTAL)
         : client.player.getStackInHand(pending.hand()).isOf(Items.END_CRYSTAL);
   }

   @Environment(EnvType.CLIENT)
   private record CrystalPlacement(BlockPos base, float targetDamage, float selfDamage, double attackDistance, double score, boolean lethal){
   }

   @Environment(EnvType.CLIENT)
   private record PendingObsidianPlacement(int targetId, BlockPlacementUtil.Placement placement, float yaw, float pitch){
   }

   @Environment(EnvType.CLIENT)
   private record PendingPlacement(int targetId, BlockPos base, Hand hand, int silentSlot, BlockHitResult hitResult, float yaw, float pitch){
   }

   @Environment(EnvType.CLIENT)
   private record PreparedHand(Hand hand, int silentSlot){
   }
}

