package made4mischief.astatine.client.modules.movement;

import made4mischief.astatine.client.mixin.ClientWorldAccessor;
import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.utils.inventory.InventoryUtil;
import made4mischief.astatine.client.utils.rotation.RotationUtil;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Hand;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PendingUpdateManager;

@Environment(EnvType.CLIENT)
public final class PearlPhaseModule extends Module {
   private static final Direction[] HORIZONTAL_DIRECTIONS = new Direction[]{
      Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
   };
   private static final double MAX_WALL_OFFSET = 0.28;
   private static final double PHASE_RANGE_SQUARED = 1.0;
   private static final double PHASE_POINT_Y_OFFSET = 0.035;
   private static final double PHASE_POINT_DEPTH = 0.501;
   private static final double WALL_ANGLE_PENALTY = 0.35;
   private static final int PHASE_TIMEOUT_TICKS = 40;
   private PearlPhaseModule.PhaseTarget phaseTarget;
   private int phaseTimeoutTicks;
   private boolean phaseActive;

   public PearlPhaseModule(){
      super("PearlPhase", Category.MOVEMENT, "NÃ©m ngá»c Ã¢m tháº§m vÃ o cáº¡nh khá»‘i gáº§n Ä‘Ã³.");
   }

   @Override
   protected void onEnable(){
      this.clearTarget();
      this.phaseActive = true;
   }

   @Override
   protected void onDisable(){
      this.clearTarget();
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      if (this.isInGame(client)) {
         if (!this.shouldPhase()) {
            this.disable();
         } else if (this.phaseActive) {
            this.tickPhase(client);
         } else if (this.phaseTarget != null && this.isWallBlock(client, this.phaseTarget.getPhaseTarget())) {
            boolean phaseTarget = client.player.getBoundingBox().intersects(new Box(this.phaseTarget.getPhaseTarget()));
            if (phaseTarget) {
               this.disable();
            } else {
               if (--this.phaseTimeoutTicks <= 0) {
                  this.disable();
               }
            }
         } else {
            this.disable();
         }
      }
   }

   private void tickPhase(MinecraftClient client){
      PearlPhaseModule.PhaseTarget var2 = this.findPhaseTarget(client);
      PearlPhaseModule.PearlSource var3 = this.findPearlSource(client);
      if (var2 != null && var3 != null && this.isPhaseOffsetReached(client, var2)) {
         float lowerEdge2 = RotationUtil.getYaw(client.player.getEyePos(), var2.lowerEdge());
         float lowerEdge = RotationUtil.getPitch(client.player.getEyePos(), var2.lowerEdge());
         this.throwPearl(client, var3, lowerEdge2, lowerEdge);
         this.phaseTarget = var2;
         this.phaseTimeoutTicks = 40;
         this.phaseActive = false;
      } else {
         this.disable();
      }
   }

   private PearlPhaseModule.PhaseTarget findPhaseTarget(MinecraftClient client){
      BlockPos pos2 = client.player.getBlockPos();
      Box Box = client.player.getBoundingBox();
      double yaw = Math.toRadians(client.player.getYaw());
      double sin = -Math.sin(yaw);
      double cos = Math.cos(yaw);
      PearlPhaseModule.PhaseTarget var10 = null;
      double abs2 = Double.MAX_VALUE;

      for (Direction direction : HORIZONTAL_DIRECTIONS) {
         for (int var20 : new int[]{0, 1}) {
            BlockPos pos = pos2.up(var20).offset(direction);
            if (this.isWallBlock(client, pos)) {
               double wallOffset = this.getWallOffset(Box, pos, direction);
               if (!(wallOffset < -0.08) && !(wallOffset > 0.28)) {
                  double offsetZ = sin * direction.getOffsetX() + cos * direction.getOffsetZ();
                  double var26 = (1.0 - offsetZ) * 0.35;
                  double var28 = var20 == 0 ? 0.0 : 0.45;
                  double abs = Math.abs(wallOffset) + var26 + var28;
                  if (!(abs >= abs2)) {
                     var10 = new PearlPhaseModule.PhaseTarget(pos.toImmutable(), this.getPhasePoint(pos, direction));
                     abs2 = abs;
                  }
               }
            }
         }
      }

      return var10;
   }

   private boolean isWallBlock(MinecraftClient client, BlockPos pos){
      BlockState state = client.world.getBlockState(pos);
      return !state.isReplaceable() && !state.getCollisionShape(client.world, pos).isEmpty();
   }

   private double getWallOffset(Box player, BlockPos wall, Direction direction){
      return switch (direction) {
         case EAST -> wall.getX() - player.maxX;
         case WEST -> player.minX - (wall.getX() + 1.0);
         case SOUTH -> wall.getZ() - player.maxZ;
         case NORTH -> player.minZ - (wall.getZ() + 1.0);
         default -> Double.MAX_VALUE;
      };
   }

   private Vec3d getPhasePoint(BlockPos wall, Direction wallDirection){
      Direction direction = wallDirection.getOpposite();
      return new Vec3d(
         wall.getX() + 0.5 + direction.getOffsetX() * 0.501, wall.getY() + 0.035, wall.getZ() + 0.5 + direction.getOffsetZ() * 0.501
      );
   }

   private boolean isPhaseOffsetReached(MinecraftClient client, PearlPhaseModule.PhaseTarget target){
      double x = target.lowerEdge().x - client.player.getX();
      double z = target.lowerEdge().z - client.player.getZ();
      return x * x + z * z <= 1.0;
   }

   private PearlPhaseModule.PearlSource findPearlSource(MinecraftClient client){
      if (client.player.getOffHandStack().isOf(Items.ENDER_PEARL)) {
         return new PearlPhaseModule.PearlSource(Hand.OFF_HAND, -1);
      } else {
         int findHotBarItem = InventoryUtil.findHotBarItem(client, Items.ENDER_PEARL);
         return findHotBarItem == -1 ? null : new PearlPhaseModule.PearlSource(Hand.MAIN_HAND, findHotBarItem);
      }
   }

   private void throwPearl(MinecraftClient client, PearlPhaseModule.PearlSource pearl, float yaw, float pitch){
      int selectedSlot = client.player.getInventory().getSelectedSlot();
      boolean hotbarSlot = pearl.hand() == Hand.MAIN_HAND && pearl.hotbarSlot() != selectedSlot;
      if (hotbarSlot) {
         client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(pearl.hotbarSlot()));
      }

      try {
         PendingUpdateManager pendingUpdateManager = ((ClientWorldAccessor)client.world).astatine$getPendingUpdateManager().incrementSequence();

         try {
            client.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(pearl.hand(), pendingUpdateManager.getSequence(), yaw, pitch));
         } catch (Throwable e2) {
            if (pendingUpdateManager != null) {
               try {
                  pendingUpdateManager.close();
               } catch (Throwable e) {
                  e2.addSuppressed(e);
               }
            }

            throw e2;
         }

         if (pendingUpdateManager != null) {
            pendingUpdateManager.close();
         }
      } finally {
         if (hotbarSlot) {
            client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(selectedSlot));
         }
      }
   }

   private boolean isInGame(MinecraftClient client){
      return client.player != null && client.world != null && !client.player.isDead();
   }

   private boolean shouldPhase(){
      return VelocityModule.shouldPreventBlockPush() ? true : this.phaseActive && VelocityModule.prepareForPearlPhase();
   }

   private void clearTarget(){
      this.phaseTarget = null;
      this.phaseTimeoutTicks = 0;
      this.phaseActive = false;
   }

   @Environment(EnvType.CLIENT)
   private record PearlSource(Hand hand, int hotbarSlot){
   }

   @Environment(EnvType.CLIENT)
   private record PhaseTarget(BlockPos wall, Vec3d lowerEdge){
      public BlockPos getPhaseTarget(){
         return this.wall;
      }
   }
}

