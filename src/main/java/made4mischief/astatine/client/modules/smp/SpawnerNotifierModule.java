package made4mischief.astatine.client.modules.smp;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import made4mischief.astatine.Astatine;
import made4mischief.astatine.client.hud.NotificationRenderer;
import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.ColorSetting;
import made4mischief.astatine.client.setting.ModeSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import made4mischief.astatine.client.utils.render.RenderUtil;
import made4mischief.astatine.client.utils.render.core.ColorUtil;
import made4mischief.astatine.client.utils.render.core.SoundUtil;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.PacketEvent;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.render.RenderTickCounter;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public final class SpawnerNotifierModule extends Module {
   private static final double BOX_INSET = 0.002;
   private static final float SCREEN_MARGIN = 2.0F;
   private static SpawnerNotifierModule instance;
   private final ModeSetting detectionModeSetting = this.addMode("Detection Mode", "Packet", new String[]{"Packet", "World"});
   private final NumberSetting rangeSetting = this.addNumber("Range", 256.0, 16.0, 256.0, 8.0);
   private final ModeSetting renderModeSetting = this.addMode("Render Mode", "Both", new String[]{"Both", "Fill", "Outline"});
   private final ColorSetting eSPColorSetting = this.addColor("ESP Color", -675775);
   private final BooleanSetting colorByTypeSetting = this.addBoolean("Color By Type", true);
   private final NumberSetting fillAlphaSetting = this.addNumber("Fill Alpha", 40.0, 0.0, 255.0, 5.0);
   private final NumberSetting outlineAlphaSetting = this.addNumber("Outline Alpha", 220.0, 0.0, 255.0, 5.0);
   private final NumberSetting lineWidthSetting = this.addNumber("Line Width", 2.0, 1.0, 5.0, 0.5);
   private final BooleanSetting throughWallsSetting = this.addBoolean("Through Walls", true);
   private final BooleanSetting tracersSetting = this.addBoolean("Tracers", false);
   private final NumberSetting tracerWidthSetting = this.addNumber("Tracer Width", 1.0, 0.5, 3.0, 0.5);
   private final BooleanSetting chatAlertSetting = this.addBoolean("Chat Alert", true);
   private final BooleanSetting screenAlertSetting = this.addBoolean("Screen Alert", true);
   private final BooleanSetting playSoundSetting = this.addBoolean("Play Sound", true);
   private final Vector3f forward = new Vector3f();
   private final Vector3f right = new Vector3f();
   private final Vector3f up = new Vector3f();
   private final Map<BlockPos, SpawnerNotifierModule.SpawnerEntry> spawnerEntries = new ConcurrentHashMap<>();
   private final Map<BlockPos, Integer> particleHits = new ConcurrentHashMap<>();
   private ClientWorld trackedWorld;
   private int scanTimerTicks;

   public SpawnerNotifierModule(){
      super("SpawnerNotifier", Category.SMP, "PhÃ¡t hiá»‡n vÃ  lÃ m ná»•i báº­t cÃ¡c loáº¡i lá»“ng quÃ¡i.", -1);
      this.fillAlphaSetting.visibleWhen(() -> !this.renderModeSetting.is("Outline"));
      this.outlineAlphaSetting.visibleWhen(() -> !this.renderModeSetting.is("Fill"));
      this.lineWidthSetting.visibleWhen(() -> !this.renderModeSetting.is("Fill"));
      this.tracerWidthSetting.visibleWhen(this.tracersSetting::getValue);
      instance = this;
      WorldRenderEvents.END_MAIN.register(SpawnerNotifierModule::renderSpawnerMarkers);
      HudElementRegistry.attachElementBefore(VanillaHudElements.CROSSHAIR, Astatine.id("spawner_tracers"), SpawnerNotifierModule::renderSpawnerList);
   }

   @Override
   protected void onEnable(){
      this.spawnerEntries.clear();
      this.particleHits.clear();
      this.trackedWorld = null;
      this.scanTimerTicks = 0;
   }

   @Override
   protected void onDisable(){
      this.spawnerEntries.clear();
      this.particleHits.clear();
      this.trackedWorld = null;
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      if (client.player != null && client.world != null) {
         if (this.trackedWorld != client.world) {
            this.spawnerEntries.clear();
            this.particleHits.clear();
            this.trackedWorld = client.world;
            this.scanTimerTicks = 0;
         }

         if (--this.scanTimerTicks <= 0) {
            this.scanTimerTicks = 10;
            this.scanParticles(client.world, client.player);
         }
      } else {
         this.spawnerEntries.clear();
         this.particleHits.clear();
         this.trackedWorld = null;
      }
   }

   @EventTarget
   public void onPacket(PacketEvent event){
      if (this.isEnabled() && event.isReceive()) {
         if (this.detectionModeSetting.is("Packet")) {
            Packet packet = event.getPacket();
            if (packet instanceof BlockEntityUpdateS2CPacket var3) {
               BlockEntityType blockEntityType = var3.getBlockEntityType();
               if (blockEntityType == BlockEntityType.MOB_SPAWNER || blockEntityType == BlockEntityType.TRIAL_SPAWNER) {
                  BlockPos pos2 = var3.getPos();
                  NbtCompound nbt = var3.getNbt();
                  String var12 = getSpawnerTypeFromNbt(nbt);
                  if (blockEntityType == BlockEntityType.TRIAL_SPAWNER && ("Unknown".equals(var12) || "Item".equals(var12))) {
                     var12 = "Trial";
                  }

                  this.recordSpawner(pos2, var12);
               }
            } else if (packet instanceof BlockUpdateS2CPacket var4) {
               BlockState state = var4.getState();
               if (state.isOf(Blocks.SPAWNER)) {
                  this.recordSpawner(var4.getPos(), "Item");
               } else if (state.isOf(Blocks.TRIAL_SPAWNER)) {
                  this.recordSpawner(var4.getPos(), "Trial");
               }
            } else if (packet instanceof ChunkDeltaUpdateS2CPacket var5) {
               var5.visitUpdates((pos, state) -> {
                  if (state.isOf(Blocks.SPAWNER)) {
                     this.recordSpawner(pos, "Item");
                  } else if (state.isOf(Blocks.TRIAL_SPAWNER)) {
                     this.recordSpawner(pos, "Trial");
                  }
               });
            } else if (packet instanceof WorldEventS2CPacket var6) {
               int eventId = var6.getEventId();
               if (eventId == 1045) {
                  this.recordSpawner(var6.getPos(), "Spawner");
               }
            } else if (packet instanceof ParticleS2CPacket var7) {
               ParticleEffect particleEffect = var7.getParameters();
               if (isSpawnerParticle(particleEffect)) {
                  BlockPos pos = BlockPos.ofFloored(var7.getX(), var7.getY(), var7.getZ());
                  int merge = this.particleHits.merge(pos, 1, Integer::sum);
                  if (merge >= 2) {
                     this.recordSpawner(pos, "Item");
                  }
               }
            } else if (packet instanceof ChunkDataS2CPacket var8) {
               MinecraftClient client = MinecraftClient.getInstance();
               if (client.world != null && client.player != null) {
                  WorldChunk worldChunk = client.world.getChunkManager().getWorldChunk(var8.getChunkX(), var8.getChunkZ(), false);
                  if (worldChunk != null) {
                     this.scanChunkEntities(worldChunk, client.world, client.player);
                  }
               }
            }
         }
      }
   }

   private static boolean isSpawnerParticle(ParticleEffect particle){
      if (particle == null) {
         return false;
      } else {
         ParticleType particleType = particle.getType();
         return particleType == ParticleTypes.FLAME
            || particleType == ParticleTypes.SOUL_FIRE_FLAME
            || particleType == ParticleTypes.SMOKE
            || particleType == ParticleTypes.TRIAL_SPAWNER_DETECTION
            || particleType == ParticleTypes.TRIAL_SPAWNER_DETECTION_OMINOUS;
      }
   }

   private void scanParticles(ClientWorld world, PlayerEntity player){
      double value2 = this.rangeSetting.getValue() * this.rangeSetting.getValue();
      int blockX = player.getBlockX() >> 4;
      int blockZ = player.getBlockZ() >> 4;
      int value = (int)Math.ceil(this.rangeSetting.getValue() / 16.0) + 1;

      for (int index2 = blockX - value; index2 <= blockX + value; index2++) {
         for (int index = blockZ - value; index <= blockZ + value; index++) {
            WorldChunk worldChunk = world.getChunkManager().getWorldChunk(index2, index, false);
            if (worldChunk != null) {
               this.scanChunkEntities(worldChunk, world, player);
            }
         }
      }

      if (this.detectionModeSetting.is("World")) {
         this.spawnerEntries.entrySet().removeIf(entry -> {
            BlockPos var5 = entry.getKey();
            if (player.squaredDistanceTo(Vec3d.ofCenter(var5)) > value2) {
               return true;
            } else {
               WorldChunk var6 = world.getChunkManager().getWorldChunk(var5.getX() >> 4, var5.getZ() >> 4, false);
               if (var6 == null) {
                  return true;
               } else {
                  BlockState var7 = world.getBlockState(var5);
                  return !var7.isOf(Blocks.SPAWNER) && !var7.isOf(Blocks.TRIAL_SPAWNER);
               }
            }
         });
      }
   }

   private void scanChunkEntities(WorldChunk chunk, ClientWorld world, PlayerEntity player){
      double value = this.rangeSetting.getValue() * this.rangeSetting.getValue();

      for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
         BlockPos pos = blockEntity.getPos();
         if (!(player.squaredDistanceTo(Vec3d.ofCenter(pos)) > value)) {
            BlockState state = blockEntity.getCachedState();
            if (state.isOf(Blocks.SPAWNER) || state.isOf(Blocks.TRIAL_SPAWNER)) {
               String var10 = "Item";
               if (state.isOf(Blocks.TRIAL_SPAWNER)) {
                  var10 = "Trial";
               } else {
                  try {
                     NbtCompound nbt = blockEntity.createNbt(world.getRegistryManager());
                     var10 = getSpawnerTypeFromNbt(nbt);
                  } catch (Exception e) {
                     var10 = "Item";
                  }
               }

               this.recordSpawner(pos, var10);
            }
         }
      }
   }

   private void recordSpawner(BlockPos pos, String spawnerType){
      SpawnerNotifierModule.SpawnerEntry var3 = this.spawnerEntries.get(pos);
      if (var3 != null) {
         if (("Unknown".equals(var3.type) || "Spawner".equals(var3.type) || "Mob".equals(var3.type) || "Item".equals(var3.type))
            && !"Unknown".equals(spawnerType)
            && !"Spawner".equals(spawnerType)
            && !"Item".equals(spawnerType)) {
            var3.type = spawnerType;
         }
      } else {
         String blank = spawnerType != null && !spawnerType.isBlank() ? spawnerType : "Item";
         SpawnerNotifierModule.SpawnerEntry var5 = new SpawnerNotifierModule.SpawnerEntry(pos, blank, System.currentTimeMillis(), true);
         this.spawnerEntries.put(pos, var5);
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.player != null) {
            double ofCenter = Math.sqrt(client.player.squaredDistanceTo(Vec3d.ofCenter(pos)));
            int round = (int)Math.round(ofCenter);
            if (this.screenAlertSetting.getValue()) {
               NotificationRenderer.showAlert(
                  "Spawner Discovered!",
                  blank + " Spawner at (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ") [" + round + "m]",
                  NotificationRenderer.NotificationType.ALERT
               );
            }

            if (this.playSoundSetting.getValue()) {
               SoundUtil.playNotification();
            }

            if (this.chatAlertSetting.getValue()) {
               String z = "Â§8[Â§aSpawnerNotifierÂ§8] Â§fFound Â§e"
                  + blank
                  + " Spawner Â§fat Â§aX: "
                  + pos.getX()
                  + ", Y: "
                  + pos.getY()
                  + ", Z: "
                  + pos.getZ()
                  + " Â§7("
                  + round
                  + "m)";
               client.player.sendMessage(Text.literal(z), false);
            }
         }
      }
   }

   private static String getSpawnerTypeFromNbt(NbtCompound nbt){
      if (nbt == null) {
         return "Item";
      } else {
         if (nbt.contains("CustomName")) {
            String orElse6 = (String)nbt.getString("CustomName").orElse(null);
            if (orElse6 != null && !orElse6.isBlank()) {
               String var2 = prettySpawnerType(orElse6);
               if (!var2.isBlank() && !"Unknown".equalsIgnoreCase(var2)) {
                  return var2;
               }
            }
         }

         if (nbt.contains("SpawnData")) {
            NbtCompound nbt6 = (NbtCompound)nbt.getCompound("SpawnData").orElse(null);
            if (nbt6 != null) {
               if (nbt6.contains("item")) {
                  NbtCompound nbt4 = (NbtCompound)nbt6.getCompound("item").orElse(null);
                  if (nbt4 != null && nbt4.contains("id")) {
                     String orElse4 = (String)nbt4.getString("id").orElse(null);
                     if (orElse4 != null) {
                        return entityNameFromId(orElse4);
                     }
                  }
               }

               if (nbt6.contains("Item")) {
                  NbtCompound nbt3 = (NbtCompound)nbt6.getCompound("Item").orElse(null);
                  if (nbt3 != null && nbt3.contains("id")) {
                     String orElse5 = (String)nbt3.getString("id").orElse(null);
                     if (orElse5 != null) {
                        return entityNameFromId(orElse5);
                     }
                  }
               }

               if (nbt6.contains("entity")) {
                  NbtCompound nbt2 = (NbtCompound)nbt6.getCompound("entity").orElse(null);
                  if (nbt2 != null && nbt2.contains("id")) {
                     String orElse = (String)nbt2.getString("id").orElse(null);
                     if (orElse != null) {
                        return entityNameFromId(orElse);
                     }
                  }
               }
            }
         }

         if (nbt.contains("Item")) {
            NbtCompound nbt5 = (NbtCompound)nbt.getCompound("Item").orElse(null);
            if (nbt5 != null && nbt5.contains("id")) {
               String orElse3 = (String)nbt5.getString("id").orElse(null);
               if (orElse3 != null) {
                  return entityNameFromId(orElse3);
               }
            }
         }

         if (nbt.contains("id")) {
            String orElse2 = (String)nbt.getString("id").orElse(null);
            if (orElse2 != null && !orElse2.toLowerCase(Locale.ROOT).contains("spawner")) {
               return entityNameFromId(orElse2);
            }
         }

         return "Item";
      }
   }

   private static String prettySpawnerType(String input){
      if (input == null) {
         return "Unknown";
      } else {
         String trim = input.trim();
         if (trim.startsWith("{") && trim.endsWith("}")) {
            int indexOf2 = trim.indexOf("\"text\":\"");
            if (indexOf2 != -1) {
               int var3 = indexOf2 + 8;
               int indexOf = trim.indexOf("\"", var3);
               if (indexOf != -1) {
                  trim = trim.substring(var3, indexOf);
               }
            }
         }

         trim = trim.replaceAll("Â§[0-9a-fk-or]", "").trim();
         return trim.isEmpty() ? "Unknown" : trim;
      }
   }

   private static String entityNameFromId(String rawId){
      if (rawId != null && !rawId.isBlank()) {
         String toLowerCase = rawId.toLowerCase(Locale.ROOT);
         if (toLowerCase.startsWith("minecraft:")) {
            toLowerCase = toLowerCase.substring("minecraft:".length());
         }

         String[] var2 = toLowerCase.split("_");
         StringBuilder builder = new StringBuilder();

         for (String var7 : var2) {
            if (!var7.isEmpty()) {
               if (builder.length() > 0) {
                  builder.append(" ");
               }

               builder.append(Character.toUpperCase(var7.charAt(0))).append(var7.substring(1));
            }
         }

         return builder.toString();
      } else {
         return "Item";
      }
   }

   public int getColorForType(String type){
      if (this.colorByTypeSetting.getValue() && type != null) {
         String toLowerCase = type.toLowerCase(Locale.ROOT);
         if (toLowerCase.contains("blaze")) {
            return -22016;
         } else if (toLowerCase.contains("golem") || toLowerCase.contains("iron")) {
            return -2039584;
         } else if (toLowerCase.contains("creeper")) {
            return -11141291;
         } else if (toLowerCase.contains("skeleton")) {
            return -1;
         } else if (toLowerCase.contains("pig")) {
            return -38476;
         } else {
            return toLowerCase.contains("cow") ? -7508381 : this.eSPColorSetting.getValue();
         }
      } else {
         return this.eSPColorSetting.getValue();
      }
   }

   private static void renderSpawnerMarkers(WorldRenderContext context){
      SpawnerNotifierModule spawnerNotifierModule = instance;
      if (spawnerNotifierModule != null && spawnerNotifierModule.isEnabled() && !spawnerNotifierModule.spawnerEntries.isEmpty()) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.player != null && client.world != null) {
            double value = spawnerNotifierModule.rangeSetting.getValue() * spawnerNotifierModule.rangeSetting.getValue();
            boolean is2 = !spawnerNotifierModule.renderModeSetting.is("Outline");
            boolean is = !spawnerNotifierModule.renderModeSetting.is("Fill");
            int valueInt2 = spawnerNotifierModule.outlineAlphaSetting.getValueInt();
            int valueInt = spawnerNotifierModule.fillAlphaSetting.getValueInt();

            for (SpawnerNotifierModule.SpawnerEntry spawnerEntry : spawnerNotifierModule.spawnerEntries.values()) {
               BlockPos pos = spawnerEntry.pos;
               if (!(client.player.squaredDistanceTo(Vec3d.ofCenter(pos)) > value)) {
                  int colorForType = spawnerNotifierModule.getColorForType(spawnerEntry.type);
                  RenderUtil.drawWorldBo(
                     context,
                     pos.getX() - 0.002,
                     pos.getY() - 0.002,
                     pos.getZ() - 0.002,
                     pos.getX() + 1.0 + 0.002,
                     pos.getY() + 1.0 + 0.002,
                     pos.getZ() + 1.0 + 0.002,
                     ColorUtil.withAlpha(colorForType, valueInt),
                     ColorUtil.withAlpha(colorForType, valueInt2),
                     is2,
                     is,
                     spawnerNotifierModule.throughWallsSetting.getValue(),
                     spawnerNotifierModule.lineWidthSetting.getValueFloat()
                  );
               }
            }
         }
      }
   }

   private static void renderSpawnerList(DrawContext context, RenderTickCounter tickCounter){
      SpawnerNotifierModule spawnerNotifierModule = instance;
      MinecraftClient client = MinecraftClient.getInstance();
      if (spawnerNotifierModule != null && spawnerNotifierModule.isEnabled() && spawnerNotifierModule.tracersSetting.getValue() && !spawnerNotifierModule.spawnerEntries.isEmpty() && client.player != null && client.world != null) {
         Camera camera = client.gameRenderer.getCamera();
         Vec3d vec2 = camera.getCameraPos();
         Quaternionf quaternion = camera.getRotation();
         spawnerNotifierModule.forward.set(0.0F, 0.0F, -1.0F).rotate(quaternion);
         spawnerNotifierModule.right.set(1.0F, 0.0F, 0.0F).rotate(quaternion);
         spawnerNotifierModule.up.set(0.0F, 1.0F, 0.0F).rotate(quaternion);
         float scaledWindowWidth2 = context.getScaledWindowWidth() * 0.5F;
         float scaledWindowHeight = context.getScaledWindowHeight() * 0.5F;
         float scaledWindowWidth = context.getScaledWindowWidth();
         float scaledWindowHeight2 = context.getScaledWindowHeight();
         double value = spawnerNotifierModule.rangeSetting.getValue() * spawnerNotifierModule.rangeSetting.getValue();

         for (SpawnerNotifierModule.SpawnerEntry spawnerEntry : spawnerNotifierModule.spawnerEntries.values()) {
            BlockPos pos = spawnerEntry.pos;
            Vec3d vec3 = Vec3d.ofCenter(pos);
            if (!(client.player.squaredDistanceTo(vec3) > value)) {
               double var17 = vec3.x - vec2.x;
               double var19 = vec3.y - vec2.y;
               double var21 = vec3.z - vec2.z;
               double var23 = var17 * spawnerNotifierModule.forward.x + var19 * spawnerNotifierModule.forward.y + var21 * spawnerNotifierModule.forward.z;
               float clamp;
               float clamp2;
               if (var23 > 0.001) {
                  Vec3d vec = client.gameRenderer.project(vec3);
                  if (!isPositionValid(vec)) {
                     continue;
                  }

                  clamp = (float)((vec.x + 1.0) * scaledWindowWidth * 0.5);
                  clamp2 = (float)((1.0 - vec.y) * scaledWindowHeight2 * 0.5);
                  float var28 = clampElementToBounds(scaledWindowWidth2, scaledWindowHeight, clamp, clamp2, scaledWindowWidth, scaledWindowHeight2, 2.0F);
                  clamp = scaledWindowWidth2 + (clamp - scaledWindowWidth2) * var28;
                  clamp2 = scaledWindowHeight + (clamp2 - scaledWindowHeight) * var28;
               } else {
                  double var46 = var17 * spawnerNotifierModule.right.x + var19 * spawnerNotifierModule.right.y + var21 * spawnerNotifierModule.right.z;
                  double var29 = var17 * spawnerNotifierModule.up.x + var19 * spawnerNotifierModule.up.y + var21 * spawnerNotifierModule.up.z;
                  double sqrt = Math.sqrt(var46 * var46 + var29 * var29);
                  if (sqrt < 1.0E-4) {
                     var46 = 0.0;
                     var29 = -1.0;
                     sqrt = 1.0;
                  }

                  double var33 = var46 / sqrt;
                  double var35 = var29 / sqrt;
                  float var37 = scaledWindowWidth * 0.5F - 2.0F;
                  float var38 = scaledWindowHeight2 * 0.5F - 2.0F;
                  float abs2 = Math.abs(var33) > 1.0E-4 ? (float)(var37 / Math.abs(var33)) : Float.MAX_VALUE;
                  float abs = Math.abs(var35) > 1.0E-4 ? (float)(var38 / Math.abs(var35)) : Float.MAX_VALUE;
                  float min = Math.min(abs2, abs);
                  clamp = scaledWindowWidth2 + (float)(var33 * min);
                  clamp2 = scaledWindowHeight - (float)(var35 * min);
               }

               clamp = MathHelper.clamp(clamp, 2.0F, scaledWindowWidth - 2.0F);
               clamp2 = MathHelper.clamp(clamp2, 2.0F, scaledWindowHeight2 - 2.0F);
               int colorForType = spawnerNotifierModule.getColorForType(spawnerEntry.type);
               RenderUtil.drawLine(context, scaledWindowWidth2, scaledWindowHeight, clamp, clamp2, spawnerNotifierModule.tracerWidthSetting.getValueFloat(), colorForType);
            }
         }
      }
   }

   private static boolean isPositionValid(Vec3d position){
      return Double.isFinite(position.x) && Double.isFinite(position.y) && Double.isFinite(position.z);
   }

   private static float clampElementToBounds(float centerX, float centerY, float targetX, float targetY, float width, float height, float margin){
      float var7 = targetX - centerX;
      float var8 = targetY - centerY;
      float abs = 1.0F;
      if (Math.abs(var7) > 1.0E-4F) {
         float var10 = var7 > 0.0F ? width - margin - centerX : centerX - margin;
         abs = Math.min(abs, var10 / Math.abs(var7));
      }

      if (Math.abs(var8) > 1.0E-4F) {
         float var11 = var8 > 0.0F ? height - margin - centerY : centerY - margin;
         abs = Math.min(abs, var11 / Math.abs(var8));
      }

      return MathHelper.clamp(abs, 0.0F, 1.0F);
   }

   @Environment(EnvType.CLIENT)
   public static final class SpawnerEntry {
      public final BlockPos pos;
      public String type;
      public final long timestamp;
      public boolean notified;

      public SpawnerEntry(BlockPos pos, String type, long timestamp, boolean notified){
         this.pos = pos;
         this.type = type;
         this.timestamp = timestamp;
         this.notified = notified;
      }
   }
}

