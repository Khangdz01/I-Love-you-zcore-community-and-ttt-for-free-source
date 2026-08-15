package made4mischief.astatine.client.modules.player;

import java.util.ArrayList;
import java.util.List;
import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.FriendListSetting;
import made4mischief.astatine.client.setting.StringSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Formatting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public final class FriendModule extends Module {
   private static final int spawnPosition = 16;
   private static final int lastDamageTick = 2048;
   private static FriendModule instance;
   private final StringSetting friendNameSetting;
   private final StringSetting friendsSetting;

   public FriendModule(){
      super("Friend", Category.PLAYER, "Quáº£n lÃ½ ngÆ°á»i chÆ¡i Ä‘Æ°á»£c báº£o vá»‡ khá»i module chiáº¿n Ä‘áº¥u.", -1, true);
      instance = this;
      this.friendNameSetting = this.addString("Friend Name", "", 16);
      this.friendsSetting = this.addString("Friends", "", 2048);
      this.friendsSetting.visibleWhen(() -> false);
      this.addAction("Add Friend", "Add", this::addFriend);
      this.addAction("Remove Friend", "Remove", this::removeFriend);
      this.addAction("Clear Friends", "Clear", this::clearFriends);
      this.addSetting(new FriendListSetting("Friend List", this::getFriendList));
   }

   public static boolean isFriend(Entity entity){
      return entity instanceof PlayerEntity var1 && isFriend(var1);
   }

   public static boolean isFriend(PlayerEntity player){
      return player != null && isFriend(player.getGameProfile().name());
   }

   public static boolean isFriend(String playerName){
      FriendModule friendModule = instance;
      return friendModule != null && friendModule.isEnabled() && friendModule.checkIsFriend(playerName);
   }

   @Override
   public String getHudName(){
      return this.getName() + " [" + this.getFriendList().size() + "]";
   }

   private void addFriend(){
      String inputName = this.getInputName();
      if (!isValidName(inputName)) {
         this.notify("TÃªn khÃ´ng há»£p lá»‡.", Formatting.RED);
      } else if (this.checkIsFriend(inputName)) {
         this.notify(inputName + " Ä‘Ã£ cÃ³ trong danh sÃ¡ch.", Formatting.YELLOW);
         this.friendNameSetting.setValue("");
      } else {
         List list = this.getFriendList();
         list.add(inputName);
         list.sort(String.CASE_INSENSITIVE_ORDER);
         String join = String.join(",", list);
         if (join.length() > this.friendsSetting.getMaxLength()) {
            this.notify("Danh sÃ¡ch friend Ä‘Ã£ Ä‘áº§y.", Formatting.RED);
         } else {
            this.friendsSetting.setValue(join);
            this.friendNameSetting.setValue("");
            this.notify("ÄÃ£ thÃªm " + inputName + ".", Formatting.GREEN);
            this.notify("Ä Ã£ thÃªm " + inputName + ".", Formatting.GREEN);
         }
      }
   }

   private void removeFriend(){
      String inputName = this.getInputName();
      List<String> list = this.getFriendList();
      boolean equalsIgnoreCase = list.removeIf(friend -> friend.equalsIgnoreCase(inputName));
      if (!equalsIgnoreCase) {
         this.notify(inputName.isBlank() ? "Hãy nhập tên cần xóa." : inputName + " không có trong danh sách.", Formatting.YELLOW);
      } else {
         this.friendsSetting.setValue(String.join(",", list));
         this.friendNameSetting.setValue("");
         this.notify("Đã xóa " + inputName + ".", Formatting.GREEN);
      }
   }

   private void clearFriends(){
      this.friendsSetting.setValue("");
      this.friendNameSetting.setValue("");
      this.notify("Ä Ã£ xÃ³a toÃ n bá»™ friend.", Formatting.GREEN);
   }

   private boolean checkIsFriend(String name){
      return name != null && !name.isBlank() ? this.getFriendList().stream().anyMatch(friend -> friend.equalsIgnoreCase(name.trim())) : false;
   }

   private List<String> getFriendList(){
      List<String> var1 = new ArrayList<>();

      for (String var5 : this.friendsSetting.getValue().split(",")) {
         String trim = var5.trim();
         if (isValidName(trim) && var1.stream().noneMatch(friend -> friend.equalsIgnoreCase(trim))) {
            var1.add(trim);
         }
      }

      var1.sort(String.CASE_INSENSITIVE_ORDER);
      return var1;
   }

   private String getInputName(){
      return this.friendNameSetting.getValue().trim();
   }

   private static boolean isValidName(String name){
      if (name != null && !name.isBlank() && name.length() <= 16) {
         for (int index = 0; index < name.length(); index++) {
            char charAt = name.charAt(index);
            if (!Character.isLetterOrDigit(charAt) && charAt != '_') {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private void notify(String message, Formatting color){
      if (mc.player != null) {
         mc.player
            .sendMessage(
               Text.literal("[Friend] ").formatted(Formatting.AQUA).append(Text.literal(message).formatted(color)),
               false
            );
      }
   }
}

