package made4mischief.astatine.client.modules.player;

import com.google.common.collect.ArrayListMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import java.util.UUID;
import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.modules.render.PopChamsModule;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.StringSetting;
import made4mischief.astatine.client.utils.combat.CrystalDamageUtil;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.Difficulty;
import net.minecraft.util.Hand;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.world.explosion.ExplosionImpl;
import net.minecraft.entity.Entity.RemovalReason;

@Environment(EnvType.CLIENT)
public final class FakePlayerModule extends Module {
   private static final int FAKE_ENTITY_ID = -1337;
   private static final int MAX_NAME_LENGTH = 16;
   private static final float SPRINT_KNOCKBACK_THRESHOLD = 0.9F;
   private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   private static FakePlayerModule instance;
   private final StringSetting nameSetting = this.addString("Name", "KingMC.VN", 16);
   private final BooleanSetting knockbackSetting = this.addBoolean("Knockback", true);
   private FakePlayerModule.FakePlayerEntity fakePlayer;
   private ClientWorld fakePlayerId;

   public FakePlayerModule(){
      super("FakePlayer", Category.PLAYER, "Táº¡o ngÆ°á»i chÆ¡i giáº£ vá»›i mÃ¡u vÃ  váº­t tá»• vÃ´ háº¡n.");
      instance = this;
   }

   @Override
   protected void onEnable(){
      this.spawnFakePlayer(MinecraftClient.getInstance());
   }

   @Override
   protected void onDisable(){
      this.removeFakePlayer();
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      if (!this.canSpawn(client)) {
         this.removeFakePlayer();
      } else if (this.fakePlayerId == client.world && this.fakePlayer != null && !this.fakePlayer.isRemoved()) {
         this.giveTotemOfUndying();
         this.setInvulnerable();
         this.applyStatusEffects();
         this.checkFakeHealth();
      } else {
         this.removeFakePlayer();
         this.spawnFakePlayer(client);
      }
   }

   private void spawnFakePlayer(MinecraftClient client){
      if (this.canSpawn(client) && this.fakePlayer == null) {
         GameProfile profile2 = client.player.getGameProfile();
         PropertyMap propertyMap = new PropertyMap(ArrayListMultimap.create(profile2.properties()));
         GameProfile profile = new GameProfile(UUID.randomUUID(), this.resolveName(profile2.name()), propertyMap);
         FakePlayerModule.FakePlayerEntity var5 = new FakePlayerModule.FakePlayerEntity(client.world, profile);
         int pickEntityId = this.pickEntityId(client.world);
         var5.setId(pickEntityId);
         this.copyPose(var5, client.player);
         var5.setHeadYaw(client.player.getHeadYaw());
         var5.setBodyYaw(client.player.getBodyYaw());
         var5.setVelocity(Vec3d.ZERO);
         var5.noClip = !this.knockbackSetting.getValue();
         var5.setEnabledState(this.knockbackSetting.getValue());
         var5.setHealth(var5.getMaxHealth());
         this.copyEquipment(client.player, var5);
         client.world.addEntity(var5);
         if (client.world.getEntityById(pickEntityId) == var5) {
            this.copyPose(var5, client.player);
            this.fakePlayer = var5;
            this.fakePlayerId = client.world;
         }
      }
   }

   private void setInvulnerable(){
      this.fakePlayer.noClip = !this.knockbackSetting.getValue();
      this.fakePlayer.setEnabledState(this.knockbackSetting.getValue());
      if (!this.knockbackSetting.getValue()) {
         this.fakePlayer.setVelocity(Vec3d.ZERO);
      }
   }

   private String resolveName(String fallbackName){
      String trim = this.nameSetting.getValue().trim();
      return trim.isEmpty() ? fallbackName : trim;
   }

   private int pickEntityId(ClientWorld world){
      int index = -1337;

      while (world.getEntityById(index) != null) {
         index--;
      }

      return index;
   }

   private void copyPose(OtherClientPlayerEntity created, PlayerEntity source){
      created.refreshPositionAndAngles(source.getX(), source.getY(), source.getZ(), source.getYaw(), source.getPitch());
      created.setHeadYaw(source.getHeadYaw());
      created.setBodyYaw(source.getBodyYaw());
      created.setOnGround(source.isOnGround());
   }

   private void copyEquipment(PlayerEntity source, OtherClientPlayerEntity destination){
      destination.equipStack(EquipmentSlot.HEAD, source.getEquippedStack(EquipmentSlot.HEAD).copy());
      destination.equipStack(EquipmentSlot.CHEST, source.getEquippedStack(EquipmentSlot.CHEST).copy());
      destination.equipStack(EquipmentSlot.LEGS, source.getEquippedStack(EquipmentSlot.LEGS).copy());
      destination.equipStack(EquipmentSlot.FEET, source.getEquippedStack(EquipmentSlot.FEET).copy());
      destination.setStackInHand(Hand.MAIN_HAND, source.getMainHandStack().copy());
      destination.setStackInHand(Hand.OFF_HAND, Items.TOTEM_OF_UNDYING.getDefaultStack());
   }

   private void giveTotemOfUndying(){
      if (!this.fakePlayer.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
         this.fakePlayer.setStackInHand(Hand.OFF_HAND, Items.TOTEM_OF_UNDYING.getDefaultStack());
      }
   }

   private void checkFakeHealth(){
      if (!(this.fakePlayer.getHealth() > 0.0F)) {
         this.giveTotemOfUndying();
         this.tickFakePlayer();
         this.fakePlayer.clearStatusEffects();
         this.fakePlayer.setHealth(1.0F);
         this.fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 900, 1));
         this.fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 100, 1));
         this.fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 800, 0));
         this.fakePlayer.setAbsorptionAmount(this.fakePlayer.getMaxAbsorption());
         this.fakePlayer.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
      }
   }

   private void tickFakePlayer(){
      MinecraftClient client = MinecraftClient.getInstance();
      client.particleManager.addEmitter(this.fakePlayer, ParticleTypes.TOTEM_OF_UNDYING, 30);
      this.fakePlayerId
         .playSoundClient(
            this.fakePlayer.getX(),
            this.fakePlayer.getY(),
            this.fakePlayer.getZ(),
            SoundEvents.ITEM_TOTEM_USE,
            this.fakePlayer.getSoundCategory(),
            1.0F,
            1.0F,
            false
         );
      PopChamsModule.captureLocalPop(this.fakePlayer);
   }

   private void applyStatusEffects(){
      StatusEffectInstance statusEffectInstance = this.fakePlayer.getStatusEffect(StatusEffects.REGENERATION);
      if (statusEffectInstance != null && !(this.fakePlayer.getHealth() >= this.fakePlayer.getMaxHealth())) {
         int amplifier = Math.max(50 >> statusEffectInstance.getAmplifier(), 1);
         if (statusEffectInstance.getDuration() % amplifier == 0) {
            this.fakePlayer.heal(1.0F);
         }
      }
   }

   private void removeFakePlayer(){
      if (this.fakePlayer != null && this.fakePlayerId != null && !this.fakePlayer.isRemoved()) {
         this.fakePlayerId.removeEntity(this.fakePlayer.getId(), RemovalReason.DISCARDED);
      }

      this.fakePlayer = null;
      this.fakePlayerId = null;
   }

   private boolean canSpawn(MinecraftClient client){
      return client.player != null && client.world != null && !client.player.isDead();
   }

   public static boolean handleClientAttack(PlayerEntity attacker, Entity target){
      FakePlayerModule fakePlayerModule = instance;
      if (fakePlayerModule != null && fakePlayerModule.isEnabled() && target == fakePlayerModule.fakePlayer) {
         float attackCooldownProgress = attacker.getAttackCooldownProgress(0.5F);
         boolean var4 = shouldCrit(attacker, attackCooldownProgress);
         boolean sprinting = attackCooldownProgress > 0.9F && attacker.isSprinting();
         float attributeValue2 = (float)attacker.getAttributeValue(EntityAttributes.ATTACK_KNOCKBACK) * 0.5F + (sprinting ? 0.5F : 0.0F);
         float var7 = 0.2F + attackCooldownProgress * attackCooldownProgress * 0.8F;
         float attributeValue = (float)attacker.getAttributeValue(EntityAttributes.ATTACK_DAMAGE) * var7;
         float var9 = getCritMultiplier(attacker) * attackCooldownProgress;
         if (var4) {
            attributeValue *= 1.5F;
         }

         DamageSource damageSource = fakePlayerModule.fakePlayerId.getDamageSources().playerAttack(attacker);
         float var11 = modifyDamage(fakePlayerModule.fakePlayer, damageSource, attributeValue + var9);
         fakePlayerModule.applyDamage(var11, attacker.getYaw());
         fakePlayerModule.damageFakePlayer(attacker, var11);
         calculateAttackEffects(fakePlayerModule, attacker, var4, sprinting, var9 > 0.0F, attackCooldownProgress, attributeValue2);
         attacker.resetTicksSinceLastAttack();
         return true;
      } else {
         return false;
      }
   }

   private void applyDamage(float damage, float yaw){
      if (!(damage <= 0.0F)) {
         float absorptionAmount = Math.min(this.fakePlayer.getAbsorptionAmount(), damage);
         this.fakePlayer.setAbsorptionAmount(this.fakePlayer.getAbsorptionAmount() - absorptionAmount);
         this.fakePlayer.setHealth(this.fakePlayer.getHealth() - (damage - absorptionAmount));
         this.fakePlayer.animateDamage(yaw);
         this.fakePlayerId
            .playSoundClient(
               this.fakePlayer.getX(),
               this.fakePlayer.getY(),
               this.fakePlayer.getZ(),
               SoundEvents.ENTITY_PLAYER_HURT,
               SoundCategory.PLAYERS,
               1.0F,
               this.fakePlayer.getSoundPitch(),
               false
            );
         this.checkFakeHealth();
      }
   }

   private static boolean shouldCrit(PlayerEntity attacker, float cooldown){
      return cooldown > 0.9F
         && attacker.fallDistance > 0.0
         && !attacker.isOnGround()
         && !attacker.isClimbing()
         && !attacker.isTouchingWater()
         && !attacker.hasVehicle()
         && !attacker.hasStatusEffect(StatusEffects.BLINDNESS)
         && !attacker.isSprinting();
   }

   private static float getCritMultiplier(PlayerEntity attacker){
      ItemEnchantmentsComponent itemEnchantmentsComponent = EnchantmentHelper.getEnchantments(attacker.getMainHandStack());

      for (RegistryEntry registryEntry : itemEnchantmentsComponent.getEnchantments()) {
         if (registryEntry.matchesKey(Enchantments.SHARPNESS)) {
            int level = itemEnchantmentsComponent.getLevel(registryEntry);
            return level > 0 ? 0.5F * level + 0.5F : 0.0F;
         }
      }

      return 0.0F;
   }

   private static float modifyDamage(OtherClientPlayerEntity target, DamageSource source, float damage){
      damage = DamageUtil.getDamageLeft(target, damage, source, target.getArmor(), (float)target.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS));
      StatusEffectInstance statusEffectInstance = target.getStatusEffect(StatusEffects.RESISTANCE);
      if (statusEffectInstance != null) {
         damage *= Math.max(0.0F, 1.0F - (statusEffectInstance.getAmplifier() + 1) * 0.2F);
      }

      return DamageUtil.getInflictedDamage(damage, getTotalArmor(target));
   }

   private void damageFakePlayer(PlayerEntity attacker, float damage){
      if (this.knockbackSetting.getValue() && !(damage <= 0.0F)) {
         this.fakePlayer.noClip = false;
         this.fakePlayer.setEnabledState(true);
         this.applyKnockback(0.4F, attacker.getX() - this.fakePlayer.getX(), attacker.getZ() - this.fakePlayer.getZ());
      }
   }

   private void applyKnockback(double strength, double directionX, double directionZ){
      Vec3d vec2 = this.fakePlayer.getVelocity();
      boolean next = this.fakePlayer.isOnGround()
         || this.fakePlayerId
            .getBlockCollisions(this.fakePlayer, this.fakePlayer.getBoundingBox().contract(1.0E-4, 0.0, 1.0E-4).offset(0.0, -0.05, 0.0))
            .iterator()
            .hasNext();
      if (next) {
         this.fakePlayer.setOnGround(true);
      }

      this.fakePlayer.takeKnockback(strength, directionX, directionZ);
      if (next) {
         Vec3d vec = this.fakePlayer.getVelocity();
         double min = Math.min(0.4, vec2.y * 0.5 + strength);
         this.fakePlayer.setVelocity(vec.x, min, vec.z);
      }
   }

   private static int getTotalArmor(PlayerEntity target){
      int var1 = 0;

      for (EquipmentSlot equipmentSlot : ARMOR_SLOTS) {
         ItemEnchantmentsComponent itemEnchantmentsComponent = EnchantmentHelper.getEnchantments(target.getEquippedStack(equipmentSlot));

         for (RegistryEntry registryEntry : itemEnchantmentsComponent.getEnchantments()) {
            if (registryEntry.matchesKey(Enchantments.PROTECTION)) {
               var1 += itemEnchantmentsComponent.getLevel(registryEntry);
            }
         }
      }

      return Math.min(20, var1);
   }

   private static void calculateAttackEffects(
      FakePlayerModule module, PlayerEntity attacker, boolean critical, boolean sprintKnockback, boolean enchanted, float cooldown, float knockbackStrength
   ){
      SoundEvent sound;
      if (critical) {
         sound = SoundEvents.ENTITY_PLAYER_ATTACK_CRIT;
         attacker.addCritParticles(module.fakePlayer);
      } else if (sprintKnockback) {
         sound = SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK;
         attacker.setSprinting(false);
      } else if (cooldown > 0.9F) {
         sound = SoundEvents.ENTITY_PLAYER_ATTACK_STRONG;
      } else {
         sound = SoundEvents.ENTITY_PLAYER_ATTACK_WEAK;
      }

      if (module.knockbackSetting.getValue() && knockbackStrength > 0.0F) {
         module.fakePlayer.noClip = false;
         module.fakePlayer.setEnabledState(true);
         float yaw = attacker.getYaw() * (float) (Math.PI / 180.0);
         module.applyKnockback(knockbackStrength, MathHelper.sin(yaw), -MathHelper.cos(yaw));
      }

      if (enchanted) {
         attacker.addEnchantedHitParticles(module.fakePlayer);
      }

      module.fakePlayerId.playSoundClient(attacker.getX(), attacker.getY(), attacker.getZ(), sound, SoundCategory.PLAYERS, 1.0F, 1.0F, false);
   }

   public static PlayerEntity getActiveFakePlayer(){
      FakePlayerModule fakePlayerModule = instance;
      return fakePlayerModule != null && fakePlayerModule.isEnabled() && fakePlayerModule.fakePlayer != null && !fakePlayerModule.fakePlayer.isRemoved() ? fakePlayerModule.fakePlayer : null;
   }

   public static boolean isActiveFakePlayer(Entity entity){
      FakePlayerModule fakePlayerModule = instance;
      return fakePlayerModule != null && fakePlayerModule.isEnabled() && entity == fakePlayerModule.fakePlayer;
   }

   public static void applyCrystalExplosion(Vec3d explosionPos){
      FakePlayerModule fakePlayerModule = instance;
      if (fakePlayerModule != null && fakePlayerModule.isEnabled() && fakePlayerModule.fakePlayer != null && fakePlayerModule.fakePlayerId != null && !fakePlayerModule.fakePlayer.isRemoved()) {
         Difficulty difficulty = fakePlayerModule.fakePlayerId.getDifficulty() == Difficulty.PEACEFUL ? Difficulty.NORMAL : fakePlayerModule.fakePlayerId.getDifficulty();
         float calculate = CrystalDamageUtil.calculate(fakePlayerModule.fakePlayerId, explosionPos, fakePlayerModule.fakePlayer, difficulty);
         if (!(calculate <= 0.0F)) {
            fakePlayerModule.applyDamage(calculate, 0.0F);
            fakePlayerModule.applyExplosionDamage(explosionPos);
         }
      }
   }

   private void applyExplosionDamage(Vec3d explosionPos){
      if (this.knockbackSetting.getValue()) {
         this.fakePlayer.noClip = false;
         this.fakePlayer.setEnabledState(true);
         double var2 = 12.0;
         double squaredDistanceTo = Math.sqrt(this.fakePlayer.squaredDistanceTo(explosionPos)) / var2;
         if (!(squaredDistanceTo > 1.0)) {
            Vec3d vec = this.fakePlayer.getEyePos().subtract(explosionPos).normalize();
            float calculateReceivedDamage = ExplosionImpl.calculateReceivedDamage(explosionPos, this.fakePlayer);
            double attributeValue = MathHelper.clamp(this.fakePlayer.getAttributeValue(EntityAttributes.EXPLOSION_KNOCKBACK_RESISTANCE), 0.0, 1.0);
            double var10 = (1.0 - squaredDistanceTo) * calculateReceivedDamage * (1.0 - attributeValue);
            if (var10 > 0.0) {
               this.fakePlayer.addVelocity(vec.multiply(var10));
            }
         }
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class FakePlayerEntity extends OtherClientPlayerEntity {
      private boolean enabled;

      private FakePlayerEntity(ClientWorld world, GameProfile profile){
         super(world, profile);
      }

      private void setEnabledState(boolean enabled){
         this.enabled = enabled;
      }

      @Override
      public void tickMovement(){
         super.tickMovement();
         if (this.enabled) {
            this.travel(Vec3d.ZERO);
         }
      }
   }
}

