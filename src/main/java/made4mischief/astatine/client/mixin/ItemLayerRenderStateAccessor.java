package made4mischief.astatine.client.mixin;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.item.ItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemRenderState.LayerRenderState.class)
public interface ItemLayerRenderStateAccessor {
    @Accessor("renderLayer")
    RenderLayer astatine$getRenderLayer();
}
