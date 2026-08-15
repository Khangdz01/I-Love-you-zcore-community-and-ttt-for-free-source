package made4mischief.astatine.client.setting;

import java.util.List;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class FriendListSetting extends Setting {
   private final Supplier<List<String>> friendsSupplier;

   public FriendListSetting(String name, Supplier<List<String>> friends){
      super(name);
      this.friendsSupplier = friends;
   }

   public List<String> getFriends(){
      if (this.friendsSupplier == null) {
         return List.of();
      } else {
         List list = this.friendsSupplier.get();
         return list == null ? List.of() : List.copyOf(list);
      }
   }
}
