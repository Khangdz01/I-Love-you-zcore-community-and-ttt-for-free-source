package made4mischief.astatine.client.utils.world;

import java.util.Iterator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@Environment(EnvType.CLIENT)
public final class BlockPlacementUtil {
   private static final Direction[] DIRECTIONS = new Direction[]{
      Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP
   };

   private BlockPlacementUtil(){
   }

   public static BlockPlacementUtil.Placement find(MinecraftClient client, BlockPos target, double configuredRange, boolean strictDirection){
      if (isClientReady(client) && canPlaceAt(client, target)) {
         double blockInteractionRange = Math.min(configuredRange, client.player.getBlockInteractionRange());
         double var7 = blockInteractionRange * blockInteractionRange;
         BlockHitResult hitResult = null;
         double squaredDistanceTo2 = Double.MAX_VALUE;

         for (Direction direction2 : DIRECTIONS) {
            BlockPos pos = target.offset(direction2);
            if (!client.world.getBlockState(pos).isReplaceable()) {
               Direction direction = direction2.getOpposite();
               Vec3d vec = Vec3d.ofCenter(pos).add(direction.getOffsetX() * 0.5, direction.getOffsetY() * 0.5, direction.getOffsetZ() * 0.5);
               double squaredDistanceTo = client.player.getEyePos().squaredDistanceTo(vec);
               if (!(squaredDistanceTo > var7) && !(squaredDistanceTo >= squaredDistanceTo2)) {
                  BlockHitResult hitResult2 = new BlockHitResult(vec, direction, pos, false);
                  if (!strictDirection || isPlacementBlocked(client, hitResult2)) {
                     hitResult = hitResult2;
                     squaredDistanceTo2 = squaredDistanceTo;
                  }
               }
            }
         }

         return hitResult == null ? null : new BlockPlacementUtil.Placement(target.toImmutable(), hitResult);
      } else {
         return null;
      }
   }

   public static boolean canPlaceAt(MinecraftClient client, BlockPos target){
      if (isClientReady(client) && client.world.getBlockState(target).isReplaceable()) {
         Iterator iterator = client.world
            .getOtherEntities(null, new Box(target), candidate -> !candidate.isRemoved() && !candidate.isSpectator() && candidate.canHit())
            .iterator();
         if (iterator.hasNext()) {
            Entity entity = (Entity)iterator.next();
            return false;
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   public static boolean sameFace(BlockPlacementUtil.Placement first, BlockPlacementUtil.Placement second){
      return first != null
         && second != null
         && first.target().equals(second.target())
         && first.hitResult().getBlockPos().equals(second.hitResult().getBlockPos())
         && first.hitResult().getSide() == second.hitResult().getSide();
   }

   private static boolean isPlacementBlocked(MinecraftClient client, BlockHitResult hit){
      Vec3d vec2 = Vec3d.of(hit.getSide().getVector());
      Vec3d vec = hit.getPos().subtract(vec2.multiply(0.01));
      BlockHitResult hitResult = client.world
         .raycast(new RaycastContext(client.player.getEyePos(), vec, ShapeType.OUTLINE, FluidHandling.NONE, client.player));
      return hitResult.getBlockPos().equals(hit.getBlockPos()) && hitResult.getSide() == hit.getSide();
   }

   private static boolean isClientReady(MinecraftClient client){
      return client != null && client.player != null && client.world != null;
   }

   @Environment(EnvType.CLIENT)
   public record Placement(BlockPos target, BlockHitResult hitResult){
   }
}

