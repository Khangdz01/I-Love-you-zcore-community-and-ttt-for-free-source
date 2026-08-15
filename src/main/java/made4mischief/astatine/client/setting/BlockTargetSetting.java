package made4mischief.astatine.client.setting;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;

@Environment(EnvType.CLIENT)
public final class BlockTargetSetting extends Setting {
   private final Set<Block> selectedBlocks = new LinkedHashSet<>();
   private final Set<Block> allowedBlocks;

   public BlockTargetSetting(String name, Block... defaultBlocks){
      super(name);
      this.allowedBlocks = null;
      Collections.addAll(this.selectedBlocks, defaultBlocks);
   }

   public BlockTargetSetting(String name, Collection<Block> allowedBlocks, Block... defaultBlocks){
      super(name);
      this.allowedBlocks = new LinkedHashSet<>(allowedBlocks);

      for (Block block : defaultBlocks) {
         if (this.allowedBlocks.contains(block)) {
            this.selectedBlocks.add(block);
         }
      }
   }

   public boolean isSelected(Block block){
      return this.selectedBlocks.contains(block);
   }

   public boolean isBlockAllowed(Block block){
      return this.allowedBlocks == null || this.allowedBlocks.contains(block);
   }

   public int getSelectedCount(){
      return this.selectedBlocks.size();
   }

   public Set<Block> getSelectedBlocks(){
      return Collections.unmodifiableSet(this.selectedBlocks);
   }

   public void setSelectedBlocks(Collection<Block> blocks){
      this.selectedBlocks.clear();

      for (Block block : blocks) {
         if (this.isBlockAllowed(block)) {
            this.selectedBlocks.add(block);
         }
      }
   }
}
