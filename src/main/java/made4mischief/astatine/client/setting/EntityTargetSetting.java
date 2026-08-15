package made4mischief.astatine.client.setting;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.EntityType;

@Environment(EnvType.CLIENT)
public class EntityTargetSetting extends Setting {
   private final Set<EntityType<?>> selectedTypes = new LinkedHashSet<>();

   public EntityTargetSetting(String name, EntityType<?>... defaultTypes){
      super(name);
      Collections.addAll(this.selectedTypes, defaultTypes);
   }

   public boolean isSelected(EntityType<?> entityType){
      return this.selectedTypes.contains(entityType);
   }

   public int getSelectedCount(){
      return this.selectedTypes.size();
   }

   public Set<EntityType<?>> getSelectedTypes(){
      return Collections.unmodifiableSet(this.selectedTypes);
   }

   public void setSelectedTypes(Collection<EntityType<?>> entityTypes){
      this.selectedTypes.clear();
      this.selectedTypes.addAll(entityTypes);
   }
}
