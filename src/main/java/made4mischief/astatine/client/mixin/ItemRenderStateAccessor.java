package made4mischief.astatine.client.mixin;

import net.minecraft.client.render.item.ItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemRenderState.class)
public interface ItemRenderStateAccessor {
    @Accessor("layers")
    ItemRenderState.LayerRenderState[] astatine$getLayers();

    @Accessor("layerCount")
    int astatine$getLayerCount();
}
