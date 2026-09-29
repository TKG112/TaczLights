package net.tkg.taczlights.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.light.renderer.LightRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fixes lights disappearing when more than one light type is on screen.
 * <p>
 * After drawing each light type, Veil draws its in-scattering into another framebuffer and then unbinds it, without
 * binding the light framebuffer back. Every light type drawn after that one ends up in the wrong framebuffer. Which
 * types break depends on the order they were first used in, e.g. a laser's beam light breaking flashlights.
 */
@Mixin(value = LightRenderer.class, remap = false)
public abstract class VeilLightRendererMixin {
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lfoundry/veil/api/client/render/light/renderer/LightTypeRenderer;renderLights(Lfoundry/veil/api/client/render/light/renderer/LightRenderer;)V"))
    private void taczlights$bindLightFramebuffer(CallbackInfoReturnable<Boolean> cir, @Local(argsOnly = true, ordinal = 0) AdvancedFbo lightFbo) {
        lightFbo.bind(true);
    }
}
