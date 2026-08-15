package made4mischief.astatine.client.utils.combat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.Difficulty;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.world.World;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.world.explosion.ExplosionImpl;

@Environment(EnvType.CLIENT)
public final class CrystalDamageUtil {
   private static final float EXPLOSION_POWER = 6.0F;
   private static final float MAX_DISTANCE = 12.0F;
   private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

   private CrystalDamageUtil(){
   }

   public static float calculate(World world, Vec3d explosionPos, LivingEntity entity){
      return calculate(world, explosionPos, entity, world.getDifficulty());
   }

   public static float calculate(World world, Vec3d explosionPos, LivingEntity entity, Difficulty difficulty){
      double squaredDistanceTo = Math.sqrt(entity.squaredDistanceTo(explosionPos)) / 12.0;
      if (squaredDistanceTo > 1.0) {
         return 0.0F;
      } else {
         float calculateReceivedDamage = ExplosionImpl.calculateReceivedDamage(explosionPos, entity);
         double var7 = (1.0 - squaredDistanceTo) * calculateReceivedDamage;
         float attributeValue = (float)((var7 * var7 + var7) / 2.0 * 7.0 * 12.0 + 1.0);
         attributeValue = applyDifficultyMultiplier(difficulty, attributeValue);
         DamageSource damageSource = world.getDamageSources().explosion(null, null);
         attributeValue = DamageUtil.getDamageLeft(entity, attributeValue, damageSource, entity.getArmor(), (float)entity.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS));
         StatusEffectInstance statusEffectInstance = entity.getStatusEffect(StatusEffects.RESISTANCE);
         if (statusEffectInstance != null) {
            float amplifier = (statusEffectInstance.getAmplifier() + 1) * 0.2F;
            attributeValue *= Math.max(0.0F, 1.0F - amplifier);
         }

         int var15 = countArmorEnchantments(entity);
         return Math.max(0.0F, DamageUtil.getInflictedDamage(attributeValue, var15));
      }
   }

   private static float applyDifficultyMultiplier(Difficulty difficulty, float damage){
      return switch (difficulty) {
         case PEACEFUL -> 0.0F;
         case EASY -> Math.min(damage * 0.5F + 1.0F, damage);
         case NORMAL -> damage;
         case HARD -> damage * 1.5F;
         default -> throw new MatchException(null, null);
      };
   }

   private static int countArmorEnchantments(LivingEntity entity){
      int var1 = 0;

      for (EquipmentSlot equipmentSlot : ARMOR_SLOTS) {
         ItemStack stack = entity.getEquippedStack(equipmentSlot);
         ItemEnchantmentsComponent itemEnchantmentsComponent = EnchantmentHelper.getEnchantments(stack);

         for (RegistryEntry registryEntry : itemEnchantmentsComponent.getEnchantments()) {
            int level = itemEnchantmentsComponent.getLevel(registryEntry);
            if (registryEntry.matchesKey(Enchantments.PROTECTION)) {
               var1 += level;
            } else if (registryEntry.matchesKey(Enchantments.BLAST_PROTECTION)) {
               var1 += level * 2;
            }
         }
      }

      return Math.min(20, var1);
   }
}

