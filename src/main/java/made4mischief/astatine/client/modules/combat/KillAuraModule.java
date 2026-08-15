package made4mischief.astatine.client.modules.combat;

import made4mischief.astatine.client.hud.TargetHudRenderer;
import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.EntityTargetSetting;
import made4mischief.astatine.client.setting.ModeSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import made4mischief.astatine.client.utils.combat.AttackUtil;
import made4mischief.astatine.client.utils.combat.TargetUtil;
import made4mischief.astatine.client.utils.inventory.SilentSlotManager;
import made4mischief.astatine.client.utils.rotation.RotationManager;
import made4mischief.astatine.client.utils.rotation.RotationUtil;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.DataComponentTypes;

@Environment(EnvType.CLIENT)
public final class KillAuraModule extends Module {
   private static final double TARGET_RANGE_MULTIPLIER = 2.0;
   private static final Object ROTATION_STATE = new Object();
   private final NumberSetting rangeSetting = this.addNumber("Range", 5.0, 1.0, 8.0, 0.5);
   private final EntityTargetSetting targetsSetting = this.addSetting(new EntityTargetSetting("Targets", EntityType.PLAYER));
   private final ModeSetting aimPointSetting = this.addMode("Aim Point", "Head", new String[]{"Head", "Body"});
   private final ModeSetting rotationModeSetting = this.addMode("Rotation Mode", "Silent", new String[]{"Client", "Silent"});
   private final BooleanSetting rotateModelSetting = this.addBoolean("Rotate Model", true);
   private final BooleanSetting silentMovementSetting = this.addBoolean("Silent Movement", true);
   private final BooleanSetting autoAttackSetting = this.addBoolean("Auto Attack", true);
   private final NumberSetting attackRangeSetting = this.addNumber("Attack Range", 3.0, 1.0, 6.0, 0.1);
   private final ModeSetting cooldownModeSetting = this.addMode("Cooldown Mode", "Both", new String[]{"Ticks", "Vanilla", "Both"});
   private final NumberSetting attackDelaySetting = this.addNumber("Attack Delay", 10.0, 0.0, 40.0, 1.0);
   private final NumberSetting vanillaCooldownSetting = this.addNumber("Vanilla Cooldown", 0.9, 0.1, 1.0, 0.05);
   private final BooleanSetting requireRotationSetting = this.addBoolean("Require Rotation", false);
   private final NumberSetting rotationToleranceSetting = this.addNumber("Rotation Tolerance", 3.0, 0.5, 20.0, 0.5);
   private final ModeSetting swordSwitchSetting = this.addMode("Sword Switch", "Auto", new String[]{"Off", "Silent", "Auto"});
   private final BooleanSetting pauseOnEatSetting = this.addBoolean("Pause On Eat", true);
   private final BooleanSetting targetHudSetting = this.addBoolean("Target HUD", true);
   private final NumberSetting hudXSetting = this.addNumber("HUD X", 85.0, 5.0, 95.0, 1.0);
   private final NumberSetting hUDYSetting = this.addNumber("HUD Y", 68.0, 5.0, 95.0, 1.0);
   private final NumberSetting hudScaleSetting = this.addNumber("HUD Scale", 0.72, 0.45, 1.2, 0.05);
   private LivingEntity target;
   private int targetId = -1;
   private int attackDelayTicks;
   private PlayerEntity cachedTarget;
   private int cachedSlot = -1;

   public KillAuraModule(){
      super("KillAura", Category.COMBAT, "BÃ¡m má»¥c tiÃªu vÃ  tá»± táº¥n cÃ´ng.", -1);
      this.hudXSetting.visibleWhen(this.targetHudSetting::getValue);
      this.hUDYSetting.visibleWhen(this.targetHudSetting::getValue);
      this.hudScaleSetting.visibleWhen(this.targetHudSetting::getValue);
      this.rotateModelSetting.visibleWhen(() -> this.rotationModeSetting.is("Silent"));
      this.silentMovementSetting.visibleWhen(() -> this.rotationModeSetting.is("Silent"));
      this.attackRangeSetting.visibleWhen(this.autoAttackSetting::getValue);
      this.cooldownModeSetting.visibleWhen(this.autoAttackSetting::getValue);
      this.attackDelaySetting.visibleWhen(() -> this.autoAttackSetting.getValue() && !this.cooldownModeSetting.is("Vanilla"));
      this.vanillaCooldownSetting.visibleWhen(() -> this.autoAttackSetting.getValue() && !this.cooldownModeSetting.is("Ticks"));
      this.requireRotationSetting.visibleWhen(this.autoAttackSetting::getValue);
      this.rotationToleranceSetting.visibleWhen(() -> this.autoAttackSetting.getValue() && this.requireRotationSetting.getValue());
      this.swordSwitchSetting.visibleWhen(this.autoAttackSetting::getValue);
      this.pauseOnEatSetting.visibleWhen(this.autoAttackSetting::getValue);
   }

   @Override
   protected void onEnable(){
      this.resetTarget();
      this.handleWeaponSwitch(MinecraftClient.getInstance());
   }

   @Override
   protected void onDisable(){
      this.switchToCachedSlot(MinecraftClient.getInstance());
      this.resetTarget();
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      this.tickAttackCooldown();
      this.handleWeaponSwitch(client);
      if (!this.canEngage(client)) {
         this.resetTarget();
      } else {
         LivingEntity entity = this.findTarget(client);
         TargetHudRenderer.syncTarget(entity);
         if (entity == null) {
            this.targetId = -1;
            this.clearRotation();
         } else {
            if (entity.getId() != this.targetId) {
               this.targetId = entity.getId();
            }

            KillAuraModule.AimRotation var4 = this.computeAimRotation(client, entity);
            this.applyRotation(client, var4);
            this.attackTarget(client, entity, var4);
         }
      }
   }

   private LivingEntity findTarget(MinecraftClient client){
      double value = this.rangeSetting.getValue();
      this.target = TargetUtil.getLowestHealthTarget(client, value, this.target, value * 2.0, entity -> this.targetsSetting.isSelected(entity.getType()));
      return this.target;
   }

   private KillAuraModule.AimRotation computeAimRotation(MinecraftClient client, LivingEntity target){
      Vec3d vec2 = this.aimPointSetting.is("Body") ? target.getEyePos().add(0.0, -target.getHeight() * 0.25, 0.0) : target.getEyePos();
      Vec3d vec = client.player.getEyePos();
      return new KillAuraModule.AimRotation(RotationUtil.getYaw(vec, vec2), RotationUtil.getPitch(vec, vec2));
   }

   private void applyRotation(MinecraftClient client, KillAuraModule.AimRotation rotation){
      if (this.rotationModeSetting.is("Silent")) {
         RotationManager.setRotation(ROTATION_STATE, rotation.yaw(), rotation.pitch(), this.rotateModelSetting.getValue(), this.silentMovementSetting.getValue());
      } else {
         this.clearRotation();
         client.player.setYaw(rotation.yaw());
         client.player.setPitch(rotation.pitch());
      }
   }

   private void attackTarget(MinecraftClient client, LivingEntity target, KillAuraModule.AimRotation rotation){
      if (this.autoAttackSetting.getValue()) {
         if (this.cooldownModeSetting.is("Vanilla") || this.attackDelayTicks <= 0) {
            if (!this.pauseOnEatSetting.getValue() || !this.canUseWeapon(client.player)) {
               if (!(client.player.distanceTo(target) > this.attackRangeSetting.getValue())) {
                  if (this.isAttackCooldownReady(client)) {
                     if (!this.requireRotationSetting.getValue() || RotationManager.wasRotationSent(rotation.yaw(), rotation.pitch(), this.rotationToleranceSetting.getValueFloat())) {
                        if (this.tryWeaponSwitch(client, target)) {
                           this.attackDelayTicks = this.cooldownModeSetting.is("Vanilla") ? 0 : this.attackDelaySetting.getValueInt();
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private boolean tryWeaponSwitch(MinecraftClient client, LivingEntity target){
      if (!this.swordSwitchSetting.is("Off") && !this.swordSwitchSetting.is("Auto")) {
         int findBestWeaponSlot = this.findBestWeaponSlot(client);
         if (findBestWeaponSlot == -1) {
            return AttackUtil.attackTarget(client, target, this.attackRangeSetting.getValue());
         } else {
            boolean[] var4 = new boolean[]{false};
            boolean value = SilentSlotManager.runWithSlot(client, findBestWeaponSlot, () -> var4[0] = AttackUtil.attackTarget(client, target, this.attackRangeSetting.getValue()));
            return value && var4[0];
         }
      } else {
         return AttackUtil.attackTarget(client, target, this.attackRangeSetting.getValue());
      }
   }

   private void handleWeaponSwitch(MinecraftClient client){
      if (!this.swordSwitchSetting.is("Auto")) {
         this.switchToCachedSlot(client);
      } else if (client != null && client.player != null && client.world != null && client.player.networkHandler != null) {
         if (this.cachedTarget != null && this.cachedTarget != client.player) {
            this.clearCachedSlot();
         }

         int findBestWeaponSlot = this.findBestWeaponSlot(client);
         if (findBestWeaponSlot != -1) {
            if (this.cachedSlot == -1) {
               this.cachedTarget = client.player;
               this.cachedSlot = client.player.getInventory().getSelectedSlot();
            }

            SilentSlotManager.selectServerSlot(client, findBestWeaponSlot);
            if (client.player.getInventory().getSelectedSlot() != findBestWeaponSlot) {
               client.player.getInventory().setSelectedSlot(findBestWeaponSlot);
            }
         }
      } else {
         this.clearCachedSlot();
      }
   }

   private void switchToCachedSlot(MinecraftClient client){
      int var2 = this.cachedSlot;
      PlayerEntity player = this.cachedTarget;
      this.clearCachedSlot();
      if (var2 >= 0
         && client != null
         && client.player != null
         && client.world != null
         && client.player.networkHandler != null
         && client.player == player) {
         SilentSlotManager.selectServerSlot(client, var2);
         client.player.getInventory().setSelectedSlot(var2);
      }
   }

   private void clearCachedSlot(){
      this.cachedTarget = null;
      this.cachedSlot = -1;
   }

   private int findBestWeaponSlot(MinecraftClient client){
      int index2 = -1;
      double applyOperations2 = Double.NEGATIVE_INFINITY;

      for (int index = 0; index < 9; index++) {
         ItemStack stack = client.player.getInventory().getStack(index);
         if (stack.isIn(ItemTags.SWORDS)) {
            AttributeModifiersComponent attributeModifiersComponent = (AttributeModifiersComponent)stack.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
            double applyOperations = attributeModifiersComponent.applyOperations(EntityAttributes.ATTACK_DAMAGE, 0.0, EquipmentSlot.MAINHAND);
            if (applyOperations > applyOperations2) {
               applyOperations2 = applyOperations;
               index2 = index;
            }
         }
      }

      return index2;
   }

   private boolean canUseWeapon(PlayerEntity player){
      if (!player.isUsingItem()) {
         return false;
      } else {
         UseAction useAction = player.getActiveItem().getUseAction();
         return useAction == UseAction.EAT || useAction == UseAction.DRINK;
      }
   }

   private boolean isAttackCooldownReady(MinecraftClient client){
      return this.cooldownModeSetting.is("Ticks") ? true : client.player.getAttackCooldownProgress(0.0F) >= this.vanillaCooldownSetting.getValueFloat();
   }

   private void tickAttackCooldown(){
      if (this.attackDelayTicks > 0) {
         this.attackDelayTicks--;
      }
   }

   private boolean canEngage(MinecraftClient client){
      return client.player != null && client.world != null && !client.player.isDead();
   }

   private void resetTarget(){
      this.target = null;
      this.targetId = -1;
      this.attackDelayTicks = 0;
      this.clearRotation();
      TargetHudRenderer.syncTarget(null);
   }

   private void clearRotation(){
      RotationManager.clearRotatingState(ROTATION_STATE);
   }

   public boolean isTargetHudEnabled(){
      return this.targetHudSetting.getValue();
   }

   public float getTargetHudPositionX(){
      return this.hudXSetting.getValueFloat() / 100.0F;
   }

   public float getTargetHudPositionY(){
      return this.hUDYSetting.getValueFloat() / 100.0F;
   }

   public float getTargetHudScale(){
      return this.hudScaleSetting.getValueFloat();
   }

   @Environment(EnvType.CLIENT)
   private record AimRotation(float yaw, float pitch){
   }
}

