package made4mischief.astatine.client.modules.combat;

import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.modules.ModuleManager;
import made4mischief.astatine.client.modules.player.PacketMineModule;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import made4mischief.astatine.client.utils.combat.TargetUtil;
import made4mischief.astatine.loader.api.event.EventTarget;
import made4mischief.astatine.loader.api.event.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public final class AutoCityModule extends Module {
   private static final Direction[] DIRECTIONS = new Direction[]{
      Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
   };
   private final NumberSetting targetRangeSetting = this.addNumber("Target Range", 6.0, 1.0, 10.0, 0.5);
   private final NumberSetting mineRangeSetting = this.addNumber("Mine Range", 5.0, 2.0, 6.0, 0.25);
   private final BooleanSetting pauseOnEatSetting = this.addBoolean("Pause On Eat", true);
   private final BooleanSetting autoDisableSetting = this.addBoolean("Auto Disable", true);
   private PlayerEntity target;
   private BlockPos targetPos;
   private boolean mining;

   public AutoCityModule(){
      super("AutoCity", Category.COMBAT, "DÃ¹ng PacketMine Ä‘á»ƒ phÃ¡ báº£o vá»‡ cá»§a má»¥c tiÃªu.", -1, true);
   }

   @Override
   protected void onEnable(){
      this.target = null;
      this.targetPos = null;
      this.mining = false;
   }

   @Override
   protected void onDisable(){
      this.executeCity();
      PacketMineModule packetMineModule = getPacketMine();
      if (this.mining && packetMineModule != null && packetMineModule.isEnabled()) {
         packetMineModule.disable();
      }

      this.target = null;
      this.mining = false;
   }

   @EventTarget
   public void onTick(TickEvent event){
      MinecraftClient client = event.getClient();
      if (!canAct(client)) {
         this.executeCity();
         this.target = null;
      } else if (this.pauseOnEatSetting.getValue() && isTargetValid(client.player)) {
         this.executeCity();
      } else {
         this.target = TargetUtil.getClosestTarget(client, this.targetRangeSetting.getValue());
         if (this.target == null) {
            this.tickCity();
         } else {
            AutoCityModule.CityBlock var3 = this.findCityBlock(client, this.target);
            if (var3 == null) {
               this.tickCity();
            } else {
               if (this.targetPos != null && !this.targetPos.equals(var3.pos())) {
                  PacketMineModule.cancelMine(this.targetPos);
                  this.targetPos = null;
               }

               PacketMineModule packetMineModule = getPacketMine();
               if (packetMineModule != null) {
                  if (!packetMineModule.isEnabled()) {
                     packetMineModule.enable();
                     this.mining = true;
                  }

                  if (PacketMineModule.requestMine(var3.pos(), var3.side())) {
                     this.targetPos = var3.pos();
                  }
               }
            }
         }
      }
   }

   private AutoCityModule.CityBlock findCityBlock(MinecraftClient client, PlayerEntity playerTarget){
      BlockPos pos2 = playerTarget.getBlockPos();
      AutoCityModule.CityBlock var4 = null;
      double squaredMagnitude2 = Double.MAX_VALUE;
      double value = this.mineRangeSetting.getValue() * this.mineRangeSetting.getValue();
      Vec3d vec = client.player.getEyePos();

      for (Direction direction : DIRECTIONS) {
         BlockPos pos = pos2.offset(direction);
         BlockState state = client.world.getBlockState(pos);
         if (!state.isAir()
            && !state.isReplaceable()
            && !(state.getHardness(client.world, pos) < 0.0F)
            && client.player.canInteractWithBlockAt(pos, 1.0)) {
            double squaredMagnitude = new Box(pos).squaredMagnitude(vec);
            if (!(squaredMagnitude > value) && !(squaredMagnitude >= squaredMagnitude2)) {
               var4 = new AutoCityModule.CityBlock(pos.toImmutable(), getFacingDirection(vec, pos));
               squaredMagnitude2 = squaredMagnitude;
            }
         }
      }

      return var4;
   }

   private void tickCity(){
      this.executeCity();
      this.target = null;
      if (this.autoDisableSetting.getValue()) {
         this.disable();
      }
   }

   private void executeCity(){
      if (this.targetPos != null) {
         PacketMineModule.cancelMine(this.targetPos);
         this.targetPos = null;
      }
   }

   private static Direction getFacingDirection(Vec3d eye, BlockPos block){
      Vec3d vec = Vec3d.ofCenter(block);
      double var3 = eye.x - vec.x;
      double var5 = eye.y - vec.y;
      double var7 = eye.z - vec.z;
      double abs = Math.abs(var3);
      double abs3 = Math.abs(var5);
      double abs2 = Math.abs(var7);
      if (abs3 >= abs && abs3 >= abs2) {
         return var5 >= 0.0 ? Direction.UP : Direction.DOWN;
      } else if (abs >= abs2) {
         return var3 >= 0.0 ? Direction.EAST : Direction.WEST;
      } else {
         return var7 >= 0.0 ? Direction.SOUTH : Direction.NORTH;
      }
   }

   private static boolean isTargetValid(PlayerEntity player){
      if (!player.isUsingItem()) {
         return false;
      } else {
         UseAction useAction = player.getActiveItem().getUseAction();
         return useAction == UseAction.EAT || useAction == UseAction.DRINK;
      }
   }

   private static PacketMineModule getPacketMine(){
      return ModuleManager.INSTANCE.getModule(PacketMineModule.class);
   }

   private static boolean canAct(MinecraftClient client){
      return client.player != null && client.world != null && client.player.networkHandler != null && !client.player.isDead();
   }

   @Environment(EnvType.CLIENT)
   private record CityBlock(BlockPos pos, Direction side){
   }
}

