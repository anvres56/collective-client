package me.collective.mixin;

import me.collective.module.render.HandsShader;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {

    @Inject(method = "renderWorld", at = @At("TAIL"))
    private void collective$onRenderWorldTail(RenderTickCounter tickCounter, CallbackInfo ci) {
        HandsShader module = HandsShader.INSTANCE;
        if (module != null && module.isToggled()) {
            module.render();
        }
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void collective$onTiltViewWhenHurt(net.minecraft.client.util.math.MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        me.collective.module.render.NoHurtCam noHurtCam = me.collective.module.render.NoHurtCam.INSTANCE;
        if (noHurtCam != null && noHurtCam.isToggled()) {
            ci.cancel();
        }
    }
}
