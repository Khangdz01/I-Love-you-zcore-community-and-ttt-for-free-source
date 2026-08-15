package made4mischief.astatine.client.modules.player;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import made4mischief.astatine.client.mixin.ClientWorldAccessor;
import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.utils.inventory.SilentSlotManager;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.PacketEvent;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.client.util.math.MatrixStack.Entry;

@Environment(EnvType.CLIENT)
public final class PacketMineModule extends Module {
   private static final int BREAK_PACKET_INTERVAL = 4;
   private static final int MAX_BREAK_TICKS = 60;
   private static final int MINING_TIMEOUT = 10;
   private static final int TOTAL_TIMEOUT = 200;
   private static final RenderPipeline PIPELINE = RenderPipeline.builder(new Snippet[]{RenderPipelines.POSITION_COLOR_SNIPPET})
      .withLocation("astatine/packet_mine_fill")
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS)
      .build();
   private static final RenderPipeline RENDER_PIPELINE = RenderPipeline.builder(new Snippet[]{RenderPipelines.RENDERTYPE_LINES_SNIPPET})
      .withLocation("astatine/packet_mine_outline")
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withVertexFormat(VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH, DrawMode.LINES)
      .build();
   private static final RenderLayer BREAK_FRAGMENT_SHADER = RenderLayer.of(
      "astatine_packet_mine", RenderSetup.builder(RENDER_PIPELINE).translucent().expectedBufferSize(256).build()
   );
   private static final RenderLayer HIGHLIGHT_FRAGMENT_SHADER = RenderLayer.of(
      "astatine_packet_mine_fill", RenderSetup.builder(PIPELINE).translucent().expectedBufferSize(256).build()
   );
   private static PacketMineModule instance;
   private final BooleanSetting renderProgressSetting = this.addBoolean("Render Progress", true);
   private volatile BlockPos target;
   private Direction targetFace;
   private int originalSlot = -1;
   private int toolSlot = -1;
   private float progress;
   private boolean swingPending;
   private int breakTicks;
   private int mineTicks;
   private volatile boolean mining;

   public PacketMineModule(){
      super("PacketMine", Category.PLAYER, "ÄÃ o khá»‘i báº±ng má»™t láº§n nháº¥p vÃ  Ä‘á»•i cuá»‘c Ã¢m tháº§m.", -1);
      instance = this;
      WorldRenderEvents.BEFORE_DEBUG_RENDER.register(PacketMineModule::renderBo);
   }

   @Override
   protected void onDisable(){
      this.continueMining(MinecraftClient.getInstance());
   }

   public static boolean handleAttackInput(){
      return instance != null && instance.isEnabled() && instance.isLookingAtTarget();
   }

   public static boolean requestMine(BlockPos pos, Direction side){
      return instance != null && instance.isEnabled() && instance.isTargetValid(pos, side);
   }

   public static void cancelMine(BlockPos pos){
      if (instance != null && instance.isEnabled() && pos != null && pos.equals(instance.target)) {
         instance.continueMining(MinecraftClient.getInstance());
      }
   }

   public static boolean isMining(BlockPos pos){
      return instance != null && instance.isEnabled() && pos != null && pos.equals(instance.target);
   }

   public static boolean isActive(){
      return instance != null && instance.isEnabled() && instance.target != null;
   }

   public static boolean suppressSelectedSlotSync(){
      return isActive();
   }

   @EventTarget
   public void onPacket(PacketEvent event){
      if (event.isReceive() && this.target != null && isSwingPacket(event.getPacket())) {
         this.mining = true;
      }
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      if (isInGame(client) && this.target != null) {
         ClientPlayerEntity player = client.player;
         if (player.getInventory().getSelectedSlot() != this.originalSlot) {
            this.continueMining(client);
         } else {
            this.selectServerSlot(client, this.toolSlot);
            if (client.world.getBlockState(this.target).isAir()) {
               this.restoreSelectedSlot(client);
               this.resetTarget();
            } else {
               boolean interactWithBlockAt = player.canInteractWithBlockAt(this.target, 1.0);
               if (!interactWithBlockAt) {
                  this.mineTicks++;
                  if (this.swingPending) {
                     this.selectServerSlot(client, this.toolSlot);
                     if (this.mineTicks > 200) {
                        this.continueMining(client);
                     }
                  } else if (this.mineTicks > 10) {
                     this.continueMining(client);
                  }
               } else {
                  this.mineTicks = 0;
                  if (this.mining) {
                     this.mining = false;
                     if (!this.swingPending) {
                        this.mineBlock(client);
                        return;
                     }

                     this.breakTicks = 3;
                  }

                  if (this.swingPending) {
                     this.breakTicks++;
                     if (this.breakTicks % 4 == 0) {
                        this.selectServerSlot(client, this.toolSlot);
                        this.sendBlockBreakAction(client, Action.STOP_DESTROY_BLOCK);
                     }

                     if (this.breakTicks > 60) {
                        this.continueMining(client);
                     }
                  } else {
                     float playerBlockBreakSpeed = this.getPlayerBlockBreakSpeed(client, player);
                     this.progress = MathHelper.clamp(this.progress + Math.max(0.0F, playerBlockBreakSpeed), 0.0F, 1.0F);
                     if (this.progress >= 1.0F) {
                        this.selectServerSlot(client, this.toolSlot);
                        this.sendBlockBreakAction(client, Action.STOP_DESTROY_BLOCK);
                        this.swingPending = true;
                        this.breakTicks = 0;
                     }
                  }
               }
            }
         }
      } else {
         if (this.target != null) {
            this.continueMining(client);
         }
      }
   }

   private boolean isLookingAtTarget(){
      MinecraftClient client = MinecraftClient.getInstance();
      return isInGame(client) && client.crosshairTarget instanceof BlockHitResult var2 && var2.getType() == Type.BLOCK
         ? this.isTargetValid(var2.getBlockPos(), var2.getSide())
         : false;
   }

   private boolean isTargetValid(BlockPos pos, Direction side){
      MinecraftClient client = MinecraftClient.getInstance();
      if (!isInGame(client)
         || pos == null
         || side == null
         || client.world.getBlockState(pos).isAir()
         || client.world.getBlockState(pos).getHardness(client.world, pos) < 0.0F
         || !client.player.canInteractWithBlockAt(pos, 1.0)) {
         return false;
      } else if (this.target != null && this.target.equals(pos)) {
         return true;
      } else {
         if (this.target != null) {
            this.continueMining(client);
         }

         ClientPlayerEntity player = client.player;
         this.originalSlot = player.getInventory().getSelectedSlot();
         this.toolSlot = findBestToolSlot(player);
         if (this.toolSlot < 0) {
            this.toolSlot = this.originalSlot;
         }

         this.target = pos.toImmutable();
         this.targetFace = side;
         this.progress = 0.0F;
         this.swingPending = false;
         this.breakTicks = 0;
         this.mineTicks = 0;
         this.mining = false;
         this.selectServerSlot(client, this.toolSlot);
         this.sendBlockBreakAction(client, Action.START_DESTROY_BLOCK);
         return true;
      }
   }

   private void mineBlock(MinecraftClient client){
      this.selectServerSlot(client, this.toolSlot);
      this.sendBlockBreakAction(client, Action.ABORT_DESTROY_BLOCK);
      this.sendBlockBreakAction(client, Action.START_DESTROY_BLOCK);
      this.progress = 0.0F;
      this.swingPending = false;
      this.breakTicks = 0;
      this.mineTicks = 0;
   }

   private float getPlayerBlockBreakSpeed(MinecraftClient client, PlayerEntity player){
      int selectedSlot = player.getInventory().getSelectedSlot();
      if (selectedSlot != this.toolSlot) {
         player.getInventory().setSelectedSlot(this.toolSlot);
      }

      float calcBlockBreakingDelta;
      try {
         calcBlockBreakingDelta = client.world.getBlockState(this.target).calcBlockBreakingDelta(player, client.world, this.target);
      } finally {
         if (selectedSlot != this.toolSlot) {
            player.getInventory().setSelectedSlot(selectedSlot);
         }
      }

      return calcBlockBreakingDelta;
   }

   private void sendBlockBreakAction(MinecraftClient client, Action action){
      if (isInGame(client)) {
         if (client.world instanceof ClientWorldAccessor var3) {
            PendingUpdateManager pendingUpdateManager = var3.astatine$getPendingUpdateManager().incrementSequence();

            try {
               client.player.networkHandler.sendPacket(new PlayerActionC2SPacket(action, this.target, this.targetFace, pendingUpdateManager.getSequence()));
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
         } else {
            client.player.networkHandler.sendPacket(new PlayerActionC2SPacket(action, this.target, this.targetFace));
         }
      }
   }

   private void continueMining(MinecraftClient client){
      if (this.target != null && isInGame(client)) {
         this.sendBlockBreakAction(client, Action.ABORT_DESTROY_BLOCK);
      }

      this.restoreSelectedSlot(client);
      this.resetTarget();
   }

   private void restoreSelectedSlot(MinecraftClient client){
      int selectedSlot = this.originalSlot;
      if (client.player != null && client.player.getInventory().getSelectedSlot() != this.originalSlot) {
         selectedSlot = client.player.getInventory().getSelectedSlot();
      }

      this.selectServerSlot(client, selectedSlot);
   }

   private void selectServerSlot(MinecraftClient client, int slot){
      SilentSlotManager.selectServerSlot(client, slot);
   }

   private void resetTarget(){
      this.target = null;
      this.targetFace = null;
      this.originalSlot = -1;
      this.toolSlot = -1;
      this.progress = 0.0F;
      this.swingPending = false;
      this.breakTicks = 0;
      this.mineTicks = 0;
      this.mining = false;
   }

   private static boolean isSwingPacket(Packet<?> packet){
      if (packet instanceof PlayerPositionLookS2CPacket) {
         return true;
      } else {
         if (packet instanceof BundleS2CPacket var1) {
            for (Packet packet2 : var1.getPackets()) {
               if (isSwingPacket(packet2)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   private static int findBestToolSlot(PlayerEntity player){
      for (int index = 0; index < 9; index++) {
         ItemStack stack = player.getInventory().getStack(index);
         if (!stack.isEmpty() && stack.isIn(ItemTags.PICKAXES)) {
            return index;
         }
      }

      return -1;
   }

   private static boolean isInGame(MinecraftClient client){
      return client.player != null && client.world != null && client.player.networkHandler != null;
   }

   private static void renderBo(WorldRenderContext context){
      PacketMineModule packetMineModule = instance;
      if (packetMineModule != null && packetMineModule.isEnabled() && packetMineModule.target != null && packetMineModule.renderProgressSetting.getValue()) {
         if (context.worldState().cameraRenderState != null) {
            Vec3d vec = context.worldState().cameraRenderState.pos;
            if (vec != null) {
               float clamp = 0.12F + 0.88F * MathHelper.clamp(packetMineModule.progress, 0.0F, 1.0F);
               boolean var4 = packetMineModule.progress >= 0.8F;
               int var5 = var4 ? -649204861 : -638565308;
               int var6 = var4 ? 1714997615 : 1726168381;
               MatrixStack matrices = context.matrices();
               matrices.push();
               matrices.translate(
                  packetMineModule.target.getX() + 0.5 - vec.x,
                  packetMineModule.target.getY() + 0.5 - vec.y,
                  packetMineModule.target.getZ() + 0.5 - vec.z
               );
               context.commandQueue().submitCustom(matrices, HIGHLIGHT_FRAGMENT_SHADER, (entry, vertices) -> drawLine(entry, vertices, clamp, var6));
               context.commandQueue().submitCustom(matrices, BREAK_FRAGMENT_SHADER, (entry, vertices) -> drawBo(entry, vertices, clamp, var5));
               matrices.pop();
            }
         }
      }
   }

   private static void drawLine(Entry entry, VertexConsumer vertices, float size, int color){
      float var4 = size * 0.5F;
      float var5 = -var4;
      drawBoxOutline(entry, vertices, var5, var5, var5, var4, var5, var5, var4, var5, var4, var5, var5, var4, color);
      drawBoxOutline(entry, vertices, var5, var4, var5, var5, var4, var4, var4, var4, var4, var4, var4, var5, color);
      drawBoxOutline(entry, vertices, var5, var5, var5, var5, var4, var5, var4, var4, var5, var4, var5, var5, color);
      drawBoxOutline(entry, vertices, var5, var5, var4, var4, var5, var4, var4, var4, var4, var5, var4, var4, color);
      drawBoxOutline(entry, vertices, var5, var5, var5, var5, var5, var4, var5, var4, var4, var5, var4, var5, color);
      drawBoxOutline(entry, vertices, var4, var5, var5, var4, var4, var5, var4, var4, var4, var4, var5, var4, color);
   }

   private static void drawBoxOutline(
      Entry entry,
      VertexConsumer vertices,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      float x3,
      float y3,
      float z3,
      float x4,
      float y4,
      float z4,
      int color
   ){
      vertices.vertex(entry, x1, y1, z1).color(color);
      vertices.vertex(entry, x2, y2, z2).color(color);
      vertices.vertex(entry, x3, y3, z3).color(color);
      vertices.vertex(entry, x4, y4, z4).color(color);
   }

   private static void drawBo(Entry entry, VertexConsumer vertices, float size, int color){
      float var4 = size * 0.5F;
      line(entry, vertices, -var4, -var4, -var4, var4, -var4, -var4, color);
      line(entry, vertices, var4, -var4, -var4, var4, var4, -var4, color);
      line(entry, vertices, var4, var4, -var4, -var4, var4, -var4, color);
      line(entry, vertices, -var4, var4, -var4, -var4, -var4, -var4, color);
      line(entry, vertices, -var4, -var4, var4, var4, -var4, var4, color);
      line(entry, vertices, var4, -var4, var4, var4, var4, var4, color);
      line(entry, vertices, var4, var4, var4, -var4, var4, var4, color);
      line(entry, vertices, -var4, var4, var4, -var4, -var4, var4, color);
      line(entry, vertices, -var4, -var4, -var4, -var4, -var4, var4, color);
      line(entry, vertices, var4, -var4, -var4, var4, -var4, var4, color);
      line(entry, vertices, var4, var4, -var4, var4, var4, var4, color);
      line(entry, vertices, -var4, var4, -var4, -var4, var4, var4, color);
   }

   private static void line(Entry entry, VertexConsumer vertices, float x1, float y1, float z1, float x2, float y2, float z2, int color){
      vertices.vertex(entry, x1, y1, z1).color(color).normal(entry, 0.0F, 1.0F, 0.0F).lineWidth(2.0F);
      vertices.vertex(entry, x2, y2, z2).color(color).normal(entry, 0.0F, 1.0F, 0.0F).lineWidth(2.0F);
   }
}

