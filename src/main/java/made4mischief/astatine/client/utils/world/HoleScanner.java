package made4mischief.astatine.client.utils.world;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos.Mutable;

@Environment(EnvType.CLIENT)
public final class HoleScanner {
   private static final int BEDROCK = 1;
   private static final int OBSIDIAN = 2;

   private HoleScanner(){
   }

   public static void scan(
      ClientWorld world, BlockPos center, int horizontalRange, int verticalRange, boolean includeDoubleHoles, List<HoleScanner.Hole> output
   ){
      output.clear();
      if (world != null && center != null && horizontalRange >= 1 && verticalRange >= 0) {
         int x = center.getX();
         int y = center.getY();
         int z = center.getZ();
         int var9 = horizontalRange * horizontalRange;
         Mutable mutable = new Mutable();

         for (int index3 = y - verticalRange; index3 <= y + verticalRange; index3++) {
            for (int index = x - horizontalRange; index <= x + horizontalRange; index++) {
               int var13 = index - x;

               for (int index2 = z - horizontalRange; index2 <= z + horizontalRange; index2++) {
                  int var15 = index2 - z;
                  if (var13 * var13 + var15 * var15 <= var9 && isHoleBlock(world, mutable, index, index3, index2)) {
                     HoleScanner.HoleType var16 = classifyHole(world, mutable, index, index3, index2);
                     if (var16 != null) {
                        output.add(new HoleScanner.Hole(index, index3, index2, 1, 1, var16));
                     } else if (includeDoubleHoles) {
                        HoleScanner.HoleType var17 = checkHoleWithNeighbors(world, mutable, index, index3, index2);
                        if (var17 != null) {
                           output.add(new HoleScanner.Hole(index, index3, index2, 2, 1, var17));
                        } else {
                           HoleScanner.HoleType var18 = checkHoleAxis(world, mutable, index, index3, index2);
                           if (var18 != null) {
                              output.add(new HoleScanner.Hole(index, index3, index2, 1, 2, var18));
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static HoleScanner.HoleType classifyHole(ClientWorld world, Mutable cursor, int x, int y, int z){
      int var5 = 0;
      var5 = buildHoleMask(world, cursor, x, y - 1, z, var5);
      var5 = buildHoleMask(world, cursor, x - 1, y, z, var5);
      var5 = buildHoleMask(world, cursor, x + 1, y, z, var5);
      var5 = buildHoleMask(world, cursor, x, y, z - 1, var5);
      var5 = buildHoleMask(world, cursor, x, y, z + 1, var5);
      return holeTypeFromMask(var5);
   }

   private static HoleScanner.HoleType checkHoleWithNeighbors(ClientWorld world, Mutable cursor, int x, int y, int z){
      if (!isHoleBlock(world, cursor, x + 1, y, z)) {
         return null;
      } else {
         int var5 = 0;
         var5 = buildHoleMask(world, cursor, x, y - 1, z, var5);
         var5 = buildHoleMask(world, cursor, x + 1, y - 1, z, var5);
         var5 = buildHoleMask(world, cursor, x - 1, y, z, var5);
         var5 = buildHoleMask(world, cursor, x + 2, y, z, var5);
         var5 = buildHoleMask(world, cursor, x, y, z - 1, var5);
         var5 = buildHoleMask(world, cursor, x + 1, y, z - 1, var5);
         var5 = buildHoleMask(world, cursor, x, y, z + 1, var5);
         var5 = buildHoleMask(world, cursor, x + 1, y, z + 1, var5);
         return holeTypeFromMask(var5);
      }
   }

   private static HoleScanner.HoleType checkHoleAxis(ClientWorld world, Mutable cursor, int x, int y, int z){
      if (!isHoleBlock(world, cursor, x, y, z + 1)) {
         return null;
      } else {
         int var5 = 0;
         var5 = buildHoleMask(world, cursor, x, y - 1, z, var5);
         var5 = buildHoleMask(world, cursor, x, y - 1, z + 1, var5);
         var5 = buildHoleMask(world, cursor, x, y, z - 1, var5);
         var5 = buildHoleMask(world, cursor, x, y, z + 2, var5);
         var5 = buildHoleMask(world, cursor, x - 1, y, z, var5);
         var5 = buildHoleMask(world, cursor, x - 1, y, z + 1, var5);
         var5 = buildHoleMask(world, cursor, x + 1, y, z, var5);
         var5 = buildHoleMask(world, cursor, x + 1, y, z + 1, var5);
         return holeTypeFromMask(var5);
      }
   }

   private static boolean isHoleBlock(ClientWorld world, Mutable cursor, int x, int y, int z){
      cursor.set(x, y, z);
      if (!world.getBlockState(cursor).isAir()) {
         return false;
      } else {
         cursor.set(x, y + 1, z);
         return world.getBlockState(cursor).isAir();
      }
   }

   private static int buildHoleMask(ClientWorld world, Mutable cursor, int x, int y, int z, int mask){
      if (mask == -1) {
         return -1;
      } else {
         cursor.set(x, y, z);
         Block block = world.getBlockState(cursor).getBlock();
         if (block == Blocks.BEDROCK) {
            return mask | 1;
         } else {
            return block != Blocks.OBSIDIAN && block != Blocks.CRYING_OBSIDIAN ? -1 : mask | 2;
         }
      }
   }

   private static HoleScanner.HoleType holeTypeFromMask(int mask){
      return switch (mask) {
         case 1 -> HoleScanner.HoleType.BEDROCK;
         case 2 -> HoleScanner.HoleType.OBSIDIAN;
         case 3 -> HoleScanner.HoleType.MIXED;
         default -> null;
      };
   }

   @Environment(EnvType.CLIENT)
   public record Hole(int x, int y, int z, int sizeX, int sizeZ, HoleScanner.HoleType type){
   }

   @Environment(EnvType.CLIENT)
   public static enum HoleType {
      BEDROCK,
      OBSIDIAN,
      MIXED;
   }
}

