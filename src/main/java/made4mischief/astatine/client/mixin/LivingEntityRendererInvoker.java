package made4mischief.astatine.client.mixin;

import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererInvoker {
    @Invoker("setupTransforms")
    void astatine$setupTransforms(LivingEntityRenderState state, MatrixStack matrices, float bodyYaw, float baseScale);

    @Invoker("scale")
    void astatine$scale(LivingEntityRenderState state, MatrixStack matrices);
}
