package made4mischief.astatine.client.modules.smp;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.ItemTargetSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.PacketEvent;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.entity.Entity.RemovalReason;

@Environment(EnvType.CLIENT)
public final class AntiLagModule extends Module {
   private static final int MAX_FORGOTTEN_IDS = 262144;
   private static AntiLagModule instance;
   private final BooleanSetting pistonsSetting = this.addBoolean("Pistons", true);
   private final BooleanSetting droppedItemsSetting = this.addBoolean("Dropped Items", true);
   private final ItemTargetSetting itemsSetting = this.addSetting(
      new ItemTargetSetting("Items", Items.KELP, Items.BAMBOO, Items.COBBLESTONE)
   );
   private final BooleanSetting minecartsSetting = this.addBoolean("Minecarts", true);
   private final BooleanSetting armorStandsSetting = this.addBoolean("Armor Stands", true);
   private final BooleanSetting smokerRenderSetting = this.addBoolean("Smoker Render", true);
   private final BooleanSetting redstoneBlocksSetting = this.addBoolean("Redstone Blocks", true);
   private final BooleanSetting redstoneParticlesSetting = this.addBoolean("Redstone Particles", true);
   private final BooleanSetting pistonParticlesSetting = this.addBoolean("Piston Particles", true);
   private final BooleanSetting smokeParticlesSetting = this.addBoolean("Smoke Particles", true);
   private final BooleanSetting hideFarmBlocksSetting = this.addBoolean("Hide Farm Blocks", true);
   private final BooleanSetting redstoneRebuildsSetting = this.addBoolean("Redstone Rebuilds", true);
   private final NumberSetting particleBudgetSetting = this.addNumber("Particle Budget", 64.0, 0.0, 512.0, 8.0);
   private final NumberSetting itemDistanceSetting = this.addNumber("Item Distance", 24.0, 4.0, 128.0, 4.0);
   private final NumberSetting itemLimitSetting = this.addNumber("Item Limit", 48.0, 0.0, 256.0, 8.0);
   private final BooleanSetting blockEntitiesSetting = this.addBoolean("Block Entities", true);
   private final NumberSetting blockEntityDistanceSetting = this.addNumber("Block Entity Distance", 24.0, 4.0, 128.0, 4.0);
   private final NumberSetting blockEntityLimitSetting = this.addNumber("Block Entity Limit", 48.0, 0.0, 256.0, 8.0);
   private final BooleanSetting adaptiveSetting = this.addBoolean("Adaptive", true);
   private final NumberSetting targetFPSSetting = this.addNumber("Target FPS", 30.0, 10.0, 120.0, 5.0);
   private final BooleanSetting blockRedstonePacketsSetting = this.addBoolean("Block Redstone Packets", true);
   private final BooleanSetting blockSoundSpamSetting = this.addBoolean("Block Sound Spam", true);
   private final BooleanSetting blockItemPacketsSetting = this.addBoolean("Block Item Packets", true);
   private final BooleanSetting purgeRAMItemsSetting = this.addBoolean("Purge RAM Items", true);
   private final NumberSetting maxRAMItemsSetting = this.addNumber("max RAM Items", 48.0, 10.0, 256.0, 10.0);
   private boolean lastSmokerRenderSetting;
   private boolean lastRedstoneBlocksSetting;
   private boolean lastHideFarmBlocksSetting;
   private int particleFrameCount;
   private int itemFrameCount;
   private int blockEntityFrameCount;
   private float adaptiveScale = 1.0F;
   private final Object entityLock = new Object();
   private final IntOpenHashSet trackedItemIds = new IntOpenHashSet();
   private final IntOpenHashSet forgottenItemIds = new IntOpenHashSet();
   private final IntArrayFIFOQueue forgottenIdQueue = new IntArrayFIFOQueue();
   private volatile ClientWorld trackedWorld;

   public AntiLagModule(){
      super("AntiLag", Category.SMP, "Giáº£m lag tá»« piston, váº­t pháº©m, xe má» vÃ  redstone.", -1);
      instance = this;
      this.itemsSetting.visibleWhen(this.droppedItemsSetting::getValue);
      this.itemDistanceSetting.visibleWhen(this.droppedItemsSetting::getValue);
      this.itemLimitSetting.visibleWhen(this.droppedItemsSetting::getValue);
      this.blockEntityDistanceSetting.visibleWhen(this.blockEntitiesSetting::getValue);
      this.blockEntityLimitSetting.visibleWhen(this.blockEntitiesSetting::getValue);
      this.targetFPSSetting.visibleWhen(this.adaptiveSetting::getValue);
      this.maxRAMItemsSetting.visibleWhen(() -> this.purgeRAMItemsSetting.getValue() || this.blockItemPacketsSetting.getValue());
      this.lastSmokerRenderSetting = this.smokerRenderSetting.getValue();
      this.lastRedstoneBlocksSetting = this.redstoneBlocksSetting.getValue();
      this.lastHideFarmBlocksSetting = this.hideFarmBlocksSetting.getValue();
   }

   @Override
   protected void onEnable(){
      this.lastSmokerRenderSetting = this.smokerRenderSetting.getValue();
      this.lastRedstoneBlocksSetting = this.redstoneBlocksSetting.getValue();
      this.lastHideFarmBlocksSetting = this.hideFarmBlocksSetting.getValue();
      this.resetParticleCount();
      this.cleanupEntities(MinecraftClient.getInstance().world);
      this.tickParticleControl();
      requestRenderReload();
   }

   @Override
   protected void onDisable(){
      this.cleanupEntities(null);
      requestRenderReload();
   }

   @EventTarget
   public void onTick(TickEvent event){
      this.particleFrameCount = 0;
      MinecraftClient client = event.getClient();
      if (client.player != null && client.world != null) {
         if (this.trackedWorld != client.world) {
            this.cleanupEntities(client.world);
         }

         if (this.purgeRAMItemsSetting.getValue()) {
            List<ItemEntity> var3 = new ArrayList<>();
            int index = 0;
            int valueInt = this.maxRAMItemsSetting.getValueInt();

            for (Entity entity : client.world.getEntities()) {
               if (entity instanceof ItemEntity var8) {
                  if (index++ < valueInt) {
                     synchronized (this.entityLock) {
                        this.trackedItemIds.add(var8.getId());
                     }
                  } else {
                     var3.add(var8);
                  }
               }
            }

            for (ItemEntity itemEntity : var3) {
               client.world.removeEntity(itemEntity.getId(), RemovalReason.DISCARDED);
            }
         }

         boolean value = this.smokerRenderSetting.getValue();
         boolean value2 = this.redstoneBlocksSetting.getValue();
         boolean value3 = this.hideFarmBlocksSetting.getValue();
         if (value != this.lastSmokerRenderSetting || value2 != this.lastRedstoneBlocksSetting || value3 != this.lastHideFarmBlocksSetting) {
            this.lastSmokerRenderSetting = value;
            this.lastRedstoneBlocksSetting = value2;
            this.lastHideFarmBlocksSetting = value3;
            requestRenderReload();
         }
      }
   }

   @EventTarget
   public void onPacket(PacketEvent event){
      if (this.isEnabled() && event.isReceive()) {
         Packet packet = event.getPacket();
         if (this.blockRedstonePacketsSetting.getValue()) {
            if (packet instanceof BlockUpdateS2CPacket var3) {
               Block block = var3.getState().getBlock();
               if (isLagCausingBlock(block) || isUpdateSuppressor(block) || block == Blocks.COBBLESTONE) {
                  event.cancel();
                  return;
               }
            } else if (packet instanceof ChunkDeltaUpdateS2CPacket var4) {
               boolean[] var8 = new boolean[]{false};
               var4.visitUpdates((pos, state) -> {
                  Block var3 = state.getBlock();
                  if (!isLagCausingBlock(var3) && !isUpdateSuppressor(var3) && var3 != Blocks.COBBLESTONE) {
                     var8[0] = true;
                  }
               });
               if (!var8[0]) {
                  event.cancel();
                  return;
               }
            }
         }

         if (this.blockSoundSpamSetting.getValue()) {
            if (packet instanceof PlaySoundS2CPacket var6) {
               String path = ((SoundEvent)var6.getSound().value()).id().getPath();
               if (path.contains("piston") || path.contains("extinguish") || path.contains("dispenser") || path.contains("explosion")) {
                  event.cancel();
                  return;
               }
            } else if (packet instanceof WorldEventS2CPacket var7) {
               int eventId = var7.getEventId();
               if (eventId == 1045 || eventId == 1030 || eventId == 1031 || eventId == 2001) {
                  event.cancel();
               }
            }
         }
      }
   }

   public static boolean shouldBlockInboundPacket(Packet<?> packet){
      AntiLagModule antiLagModule = instance;
      if (antiLagModule != null && antiLagModule.isEnabled() && antiLagModule.blockItemPacketsSetting.getValue()) {
         if (packet instanceof BundleS2CPacket var9) {
            boolean hasBlocked = false;
            for (Packet packet2 : var9.getPackets()) {
               if (shouldBlockInboundPacket(packet2)) {
                  hasBlocked = true;
                  break;
               }
            }
            return hasBlocked;
         } else {
            synchronized (antiLagModule.entityLock) {
               if (packet instanceof EntitySpawnS2CPacket var3 && var3.getEntityType() == EntityType.ITEM) {
                  int entityId = var3.getEntityId();
                  if (antiLagModule.trackedItemIds.size() < antiLagModule.maxRAMItemsSetting.getValueInt()) {
                     antiLagModule.trackedItemIds.add(entityId);
                     return false;
                  } else {
                     antiLagModule.forgetEntity(entityId);
                     return true;
                  }
               } else if (packet instanceof EntitiesDestroyS2CPacket var11) {
                  IntListIterator intListIterator = var11.getEntityIds().iterator();

                  while (intListIterator.hasNext()) {
                     int next = (Integer)intListIterator.next();
                     antiLagModule.trackedItemIds.remove(next);
                     antiLagModule.forgottenItemIds.remove(next);
                  }

                  return false;
               } else {
                  int var10 = getPacketEntityId(packet);
                  return var10 != Integer.MIN_VALUE && antiLagModule.forgottenItemIds.contains(var10);
               }
            }
         }
      } else {
         return false;
      }
   }

   private static int getPacketEntityId(Packet<?> packet){
      if (packet instanceof EntityTrackerUpdateS2CPacket var4) {
         return var4.id();
      } else if (packet instanceof EntityVelocityUpdateS2CPacket var3) {
         return var3.getEntityId();
      } else if (packet instanceof EntityPositionS2CPacket var2) {
         return var2.entityId();
      } else if (packet instanceof EntityS2CPacket var1) {
         try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world != null) {
               Entity entity = var1.getEntity(client.world);
               if (entity != null) {
                  return entity.getId();
               }
            }
         } catch (Throwable ignored) {}
         return Integer.MIN_VALUE;
      } else {
         return Integer.MIN_VALUE;
      }
   }

   private void forgetEntity(int entityId){
      this.trackedItemIds.remove(entityId);
      if (this.forgottenItemIds.add(entityId)) {
         this.forgottenIdQueue.enqueue(entityId);

         while (this.forgottenItemIds.size() > 262144 && !this.forgottenIdQueue.isEmpty()) {
            this.forgottenItemIds.remove(this.forgottenIdQueue.dequeueInt());
         }
      }
   }

   private void cleanupEntities(ClientWorld world){
      synchronized (this.entityLock) {
         this.trackedItemIds.clear();
         this.forgottenItemIds.clear();
         this.forgottenIdQueue.clear();
         this.trackedWorld = world;
      }
   }

   public static void beginRenderFrame(){
      AntiLagModule antiLagModule = instance;
      if (antiLagModule != null) {
         antiLagModule.itemFrameCount = 0;
         antiLagModule.blockEntityFrameCount = 0;
         antiLagModule.tickParticleControl();
      }
   }

   public static boolean shouldHidePistonMovement(){
      return enabled(module -> module.pistonsSetting.getValue());
   }

   public static boolean shouldHideMinecarts(){
      return enabled(module -> module.minecartsSetting.getValue());
   }

   public static boolean shouldHideArmorStands(){
      return enabled(module -> module.armorStandsSetting.getValue());
   }

   public static boolean shouldDropParticle(ParticleEffect effect){
      AntiLagModule antiLagModule = instance;
      if (antiLagModule != null && antiLagModule.isEnabled()) {
         if (!antiLagModule.redstoneParticlesSetting.getValue() || effect.getType() != ParticleTypes.DUST && effect.getType() != ParticleTypes.DUST_COLOR_TRANSITION) {
            if (antiLagModule.pistonParticlesSetting.getValue() && isLagCausingEffect(effect)) {
               return true;
            } else if (antiLagModule.smokeParticlesSetting.getValue() && isBadEffectType(effect)) {
               return true;
            } else {
               int settingInt = antiLagModule.getSettingInt(antiLagModule.particleBudgetSetting);
               if (antiLagModule.particleFrameCount >= settingInt) {
                  return true;
               } else {
                  antiLagModule.particleFrameCount++;
                  return false;
               }
            }
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   public static boolean shouldHideDroppedItem(ItemStack stack, double squaredDistanceToCamera){
      AntiLagModule antiLagModule = instance;
      if (antiLagModule != null && antiLagModule.isEnabled() && antiLagModule.droppedItemsSetting.getValue() && stack != null && !stack.isEmpty()) {
         if (antiLagModule.itemsSetting.isSelected(stack.getItem())) {
            return true;
         } else {
            float settingFloat = antiLagModule.getSettingFloat(antiLagModule.itemDistanceSetting);
            if (squaredDistanceToCamera > settingFloat * settingFloat) {
               return true;
            } else {
               int settingInt = antiLagModule.getSettingInt(antiLagModule.itemLimitSetting);
               if (antiLagModule.itemFrameCount >= settingInt) {
                  return true;
               } else {
                  antiLagModule.itemFrameCount++;
                  return false;
               }
            }
         }
      } else {
         return false;
      }
   }

   public static boolean shouldHideBlockModel(Block block){
      AntiLagModule antiLagModule = instance;
      return antiLagModule != null && antiLagModule.isEnabled()
         ? antiLagModule.smokerRenderSetting.getValue() && block == Blocks.SMOKER
            || antiLagModule.redstoneBlocksSetting.getValue() && isLagCausingBlock(block)
            || antiLagModule.hideFarmBlocksSetting.getValue() && isUpdateSuppressor(block)
         : false;
   }

   public static boolean shouldSuppressRedstoneRerender(BlockState oldState, BlockState newState){
      AntiLagModule antiLagModule = instance;
      return antiLagModule != null
         && antiLagModule.isEnabled()
         && antiLagModule.redstoneRebuildsSetting.getValue()
         && antiLagModule.redstoneBlocksSetting.getValue()
         && oldState.getBlock() == newState.getBlock()
         && isLagCausingBlock(newState.getBlock())
         && oldState.getLuminance() == newState.getLuminance();
   }

   public static boolean shouldHideBlockEntity(BlockPos pos){
      AntiLagModule antiLagModule = instance;
      if (antiLagModule != null && antiLagModule.isEnabled() && antiLagModule.blockEntitiesSetting.getValue()) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.player == null) {
            return false;
         } else {
            double x = pos.getX() + 0.5 - client.player.getX();
            double y = pos.getY() + 0.5 - client.player.getY();
            double z = pos.getZ() + 0.5 - client.player.getZ();
            float settingFloat = antiLagModule.getSettingFloat(antiLagModule.blockEntityDistanceSetting);
            if (x * x + y * y + z * z > settingFloat * settingFloat) {
               return true;
            } else {
               int settingInt = antiLagModule.getSettingInt(antiLagModule.blockEntityLimitSetting);
               if (antiLagModule.blockEntityFrameCount >= settingInt) {
                  return true;
               } else {
                  antiLagModule.blockEntityFrameCount++;
                  return false;
               }
            }
         }
      } else {
         return false;
      }
   }

   private static boolean enabled(Predicate<AntiLagModule> predicate){
      AntiLagModule antiLagModule = instance;
      return antiLagModule != null && antiLagModule.isEnabled() && predicate.test(antiLagModule);
   }

   private static boolean isLagCausingBlock(Block block){
      BlockState state = block.getDefaultState();
      return state.isIn(BlockTags.BUTTONS)
         || state.isIn(BlockTags.PRESSURE_PLATES)
         || state.isIn(BlockTags.RAILS)
         || block == Blocks.REDSTONE_WIRE
         || block == Blocks.REDSTONE_TORCH
         || block == Blocks.REDSTONE_WALL_TORCH
         || block == Blocks.REDSTONE_BLOCK
         || block == Blocks.REPEATER
         || block == Blocks.COMPARATOR
         || block == Blocks.OBSERVER
         || block == Blocks.PISTON
         || block == Blocks.STICKY_PISTON
         || block == Blocks.DISPENSER
         || block == Blocks.DROPPER
         || block == Blocks.HOPPER
         || block == Blocks.REDSTONE_LAMP
         || block == Blocks.TARGET
         || block == Blocks.LEVER
         || block == Blocks.DAYLIGHT_DETECTOR
         || block == Blocks.TRIPWIRE_HOOK
         || block == Blocks.TRIPWIRE
         || block == Blocks.NOTE_BLOCK
         || block == Blocks.TRAPPED_CHEST
         || block == Blocks.TNT
         || block == Blocks.CRAFTER
         || block == Blocks.LIGHTNING_ROD
         || block == Blocks.SCULK_SENSOR
         || block == Blocks.CALIBRATED_SCULK_SENSOR;
   }

   private static boolean isUpdateSuppressor(Block block){
      return block == Blocks.WHEAT
         || block == Blocks.CARROTS
         || block == Blocks.POTATOES
         || block == Blocks.BEETROOTS
         || block == Blocks.NETHER_WART
         || block == Blocks.SUGAR_CANE
         || block == Blocks.CACTUS
         || block == Blocks.BAMBOO
         || block == Blocks.BAMBOO_SAPLING
         || block == Blocks.KELP
         || block == Blocks.KELP_PLANT
         || block == Blocks.COCOA
         || block == Blocks.MELON_STEM
         || block == Blocks.ATTACHED_MELON_STEM
         || block == Blocks.PUMPKIN_STEM
         || block == Blocks.ATTACHED_PUMPKIN_STEM
         || block == Blocks.SWEET_BERRY_BUSH;
   }

   private static boolean isLagCausingEffect(ParticleEffect effect){
      if (!(effect instanceof BlockStateParticleEffect var1)) {
         return false;
      } else {
         Block block = var1.getBlockState().getBlock();
         return block == Blocks.PISTON || block == Blocks.STICKY_PISTON || block == Blocks.PISTON_HEAD;
      }
   }

   private static boolean isBadEffectType(ParticleEffect effect){
      return effect.getType() == ParticleTypes.SMOKE
         || effect.getType() == ParticleTypes.CAMPFIRE_COSY_SMOKE
         || effect.getType() == ParticleTypes.CAMPFIRE_SIGNAL_SMOKE;
   }

   private float getSettingFloat(NumberSetting setting){
      float valueFloat = setting.getValueFloat();
      return Math.max(4.0F, valueFloat * Math.max(0.25F, this.adaptiveScale));
   }

   private int getSettingInt(NumberSetting setting){
      int valueInt = setting.getValueInt();
      return Math.round(valueInt * this.adaptiveScale);
   }

   private void tickParticleControl(){
      if (!this.adaptiveSetting.getValue()) {
         this.adaptiveScale = 1.0F;
      } else {
         int currentFps = MinecraftClient.getInstance().getCurrentFps();
         int valueInt = this.targetFPSSetting.getValueInt();
         if (currentFps > 0 && currentFps < valueInt) {
            this.adaptiveScale = Math.max(0.125F, (float)currentFps / valueInt);
         } else {
            this.adaptiveScale = 1.0F;
         }
      }
   }

   private void resetParticleCount(){
      this.particleFrameCount = 0;
      this.itemFrameCount = 0;
      this.blockEntityFrameCount = 0;
      this.adaptiveScale = 1.0F;
   }

   private static void requestRenderReload(){
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.world != null && client.worldRenderer != null) {
         client.worldRenderer.scheduleTerrainUpdate();
      }
   }
}

