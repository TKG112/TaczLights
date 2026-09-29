package net.tkg.taczlights.mixin;

import foundry.veil.impl.client.render.pipeline.VeilBloomRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fixes a crash when the window is resized after something was drawn into the bloom buffer.
 * <p>
 * Resizing frees Veil's bloom framebuffer but leaves it marked as drawn to, so the next bloom pass clears a
 * framebuffer that no longer exists. Whatever was drawn went with the old framebuffer, so there's nothing left to apply.
 */
@Mixin(value = VeilBloomRenderer.class, remap = false)
public abstract class VeilBloomRendererMixin {
    @Shadow
    private static boolean rendered;

    @Inject(method = "free", at = @At("TAIL"))
    private static void taczlights$forgetFreedBloom(CallbackInfo ci) {
        rendered = false;
    }
}
