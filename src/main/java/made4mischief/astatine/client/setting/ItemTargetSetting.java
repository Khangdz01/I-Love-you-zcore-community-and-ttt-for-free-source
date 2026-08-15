package made4mischief.astatine.client.setting;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Item;

@Environment(EnvType.CLIENT)
public final class ItemTargetSetting extends Setting {
   private final Set<Item> selectedItems = new LinkedHashSet<>();
   private final int maxSelections;

   public ItemTargetSetting(String name, Item... defaultItems){
      this(name, Integer.MAX_VALUE, defaultItems);
   }

   public ItemTargetSetting(String name, int maximumSelections, Item... defaultItems){
      super(name);
      this.maxSelections = Math.max(1, maximumSelections);
      this.setSelectedItems(List.of(defaultItems));
   }

   public boolean isSelected(Item item){
      return this.selectedItems.contains(item);
   }

   public int getSelectedCount(){
      return this.selectedItems.size();
   }

   public Set<Item> getSelectedItems(){
      return Collections.unmodifiableSet(this.selectedItems);
   }

   public int getMaximumSelections(){
      return this.maxSelections;
   }

   public void setSelectedItems(Collection<Item> items){
      this.selectedItems.clear();
      if (items != null) {
         for (Item item : items) {
            if (item == null || this.selectedItems.size() >= this.maxSelections) {
               break;
            }

            this.selectedItems.add(item);
         }
      }
   }
}

