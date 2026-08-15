package made4mischief.astatine.client.utils.combat;

import java.util.function.Predicate;
import made4mischief.astatine.client.modules.player.FriendModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public final class TargetUtil {
   private static final float EPSILON = 0.001F;

   private TargetUtil(){
   }

   public static PlayerEntity getClosestTarget(MinecraftClient client, double range){
      return getClosestTarget(client, range, null, range);
   }

   public static PlayerEntity getClosestTarget(MinecraftClient client, double acquisitionRange, PlayerEntity currentTarget, double releaseRange){
      if (client.world != null && client.player != null) {
         double max2 = Math.max(0.0, acquisitionRange);
         double max = Math.max(max2, releaseRange);
         if (isValidPlayerTarget(client, currentTarget) && isWithinRange(client, currentTarget, max)) {
            return currentTarget;
         } else {
            PlayerEntity player2 = null;
            double squaredDistanceTo2 = max2 * max2;

            for (PlayerEntity player : client.world
               .getEntitiesByClass(PlayerEntity.class, client.player.getBoundingBox().expand(max2), candidate -> isValidPlayerTarget(client, candidate))) {
               double squaredDistanceTo = client.player.squaredDistanceTo(player);
               if (squaredDistanceTo <= squaredDistanceTo2) {
                  player2 = player;
                  squaredDistanceTo2 = squaredDistanceTo;
               }
            }

            return player2;
         }
      } else {
         return null;
      }
   }

   public static LivingEntity getLowestHealthTarget(
      MinecraftClient client, double acquisitionRange, LivingEntity currentTarget, double releaseRange, Predicate<LivingEntity> targetFilter
   ){
      if (client.world != null && client.player != null && targetFilter != null) {
         double max2 = Math.max(0.0, acquisitionRange);
         double max = Math.max(max2, releaseRange);
         if (isValidTarget(client, currentTarget, targetFilter) && isWithinRange(client, currentTarget, max)) {
            return currentTarget;
         } else {
            LivingEntity entity2 = null;
            float health2 = Float.MAX_VALUE;
            double squaredDistanceTo2 = Double.MAX_VALUE;

            for (LivingEntity entity : client.world
               .getEntitiesByClass(LivingEntity.class, client.player.getBoundingBox().expand(max2), entity -> isValidTarget(client, entity, targetFilter))) {
               double squaredDistanceTo = client.player.squaredDistanceTo(entity);
               if (!(squaredDistanceTo > max2 * max2)) {
                  float health = entity.getHealth();
                  boolean var20 = health < health2 - 0.001F;
                  boolean abs = Math.abs(health - health2) <= 0.001F && squaredDistanceTo < squaredDistanceTo2;
                  if (var20 || abs) {
                     entity2 = entity;
                     health2 = health;
                     squaredDistanceTo2 = squaredDistanceTo;
                  }
               }
            }

            return entity2;
         }
      } else {
         return null;
      }
   }

   public static LivingEntity getLowestHealthTarget(MinecraftClient client, double range, Predicate<LivingEntity> targetFilter){
      return getLowestHealthTarget(client, range, null, range, targetFilter);
   }

   private static boolean isValidPlayerTarget(MinecraftClient client, PlayerEntity target){
      return target != null
         && target != client.player
         && !FriendModule.isFriend(target)
         && client.world.getEntityById(target.getId()) == target
         && !target.isRemoved()
         && target.isAlive()
         && !target.isSpectator();
   }

   private static boolean isValidTarget(MinecraftClient client, LivingEntity target, Predicate<LivingEntity> targetFilter){
      return target != null
         && target != client.player
         && !FriendModule.isFriend(target)
         && client.world.getEntityById(target.getId()) == target
         && !target.isRemoved()
         && target.isAlive()
         && !target.isSpectator()
         && targetFilter.test(target);
   }

   private static boolean isWithinRange(MinecraftClient client, LivingEntity target, double range){
      return client.player.squaredDistanceTo(target) <= range * range;
   }
}

