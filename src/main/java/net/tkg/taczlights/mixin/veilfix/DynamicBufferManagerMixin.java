package net.tkg.taczlights.mixin.veilfix;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderTarget;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.impl.client.render.dynamicbuffer.DynamicBufferManager;
import net.tkg.taczlights.client.veilfix.VeilDepthFixes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Keeps the stencil attachment when Veil wraps a depth-stencil target, since clearing a depth-stencil texture as
 * depth-only fails.
 */
@Mixin(value = DynamicBufferManager.class, remap = false)
public abstract class DynamicBufferManagerMixin {
    @Unique
    private static final String SET_DEPTH_TEXTURE_WRAPPER = "Lfoundry/veil/api/client/render/framebuffer/AdvancedFbo$Builder;setDepthTextureWrapper(I)Lfoundry/veil/api/client/render/framebuffer/AdvancedFbo$Builder;";

    @Shadow
    @Final
    private List<AdvancedFbo> dynamicFramebuffers;

    @Shadow
    private int dynamicFboPointer;

    @WrapOperation(method = "setupRenderState", at = @At(value = "INVOKE", target = SET_DEPTH_TEXTURE_WRAPPER))
    private AdvancedFbo.Builder taczlights$keepMainStencil(AdvancedFbo.Builder builder, int depthTexture, Operation<AdvancedFbo.Builder> original,
                                                           @Local(argsOnly = true) RenderTarget renderTarget) {
        if (renderTarget.useDepth && VeilDepthFixes.hasStencil(depthTexture)) {
            return VeilDepthFixes.setDepthStencilTextureWrapper(builder, depthTexture);
        }
        return original.call(builder, depthTexture);
    }

    @WrapOperation(method = "getDynamicFbo", at = @At(value = "INVOKE", target = SET_DEPTH_TEXTURE_WRAPPER))
    private AdvancedFbo.Builder taczlights$keepDynamicStencil(AdvancedFbo.Builder builder, int depthTexture, Operation<AdvancedFbo.Builder> original,
                                                              @Local(argsOnly = true) AdvancedFbo framebuffer) {
        if (framebuffer.hasStencilAttachment()) {
            return VeilDepthFixes.setDepthStencilTextureWrapper(builder, depthTexture);
        }
        return original.call(builder, depthTexture);
    }

    /**
     * Veil reuses the next cached framebuffer when only its size matches. One built for the other stencil setting would
     * wrap the depth texture the wrong way, so drop it and let Veil build a new one.
     */
    @Inject(method = "getDynamicFbo", at = @At("HEAD"))
    private void taczlights$dropStencilMismatch(AdvancedFbo framebuffer, CallbackInfoReturnable<AdvancedFbo> cir) {
        while (this.dynamicFboPointer < this.dynamicFramebuffers.size()) {
            AdvancedFbo cached = this.dynamicFramebuffers.get(this.dynamicFboPointer);
            if (cached.hasStencilAttachment() == framebuffer.hasStencilAttachment()) {
                return;
            }
            this.dynamicFramebuffers.remove(this.dynamicFboPointer);
            cached.free();
        }
    }
}
