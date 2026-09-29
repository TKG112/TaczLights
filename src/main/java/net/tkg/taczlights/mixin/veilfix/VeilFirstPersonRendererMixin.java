package net.tkg.taczlights.mixin.veilfix;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.FramebufferAttachmentDefinition;
import foundry.veil.impl.client.render.pipeline.VeilFirstPersonRenderer;
import net.tkg.taczlights.client.veilfix.VeilDepthFixes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The first person buffer stands in for the main render target, so its depth format has to match the main target's,
 * otherwise the depth blit in the light renderer fails with {@code GL_INVALID_OPERATION}.
 */
@Mixin(value = VeilFirstPersonRenderer.class, remap = false)
public abstract class VeilFirstPersonRendererMixin {
    @Shadow
    private static AdvancedFbo firstPerson;

    @Shadow
    public static void free() {
    }

    /**
     * Veil only rebuilds the buffer when the stencil changes, not when the exact depth format does.
     */
    @Inject(method = "bind", at = @At("HEAD"))
    private static void taczlights$rebuildOnDepthFormatChange(int mask, CallbackInfo ci) {
        if (firstPerson == null) {
            return;
        }
        boolean stencil = AdvancedFbo.getMainFramebuffer().hasStencilAttachment();
        FramebufferAttachmentDefinition.Format depthFormat = VeilDepthFixes.getMainDepthFormat(
                stencil ? FramebufferAttachmentDefinition.Format.DEPTH32F_STENCIL8 : FramebufferAttachmentDefinition.Format.DEPTH_COMPONENT);
        if (!firstPerson.hasDepthAttachment() || firstPerson.getDepthAttachment().getFormat() != depthFormat.getInternalFormat()) {
            free();
        }
    }

    @WrapOperation(method = "bind", at = @At(value = "INVOKE", target = "Lfoundry/veil/api/client/render/framebuffer/AdvancedFbo$Builder;setFormat(Lfoundry/veil/api/client/render/framebuffer/FramebufferAttachmentDefinition$Format;)Lfoundry/veil/api/client/render/framebuffer/AdvancedFbo$Builder;"))
    private static AdvancedFbo.Builder taczlights$useMainDepthFormat(AdvancedFbo.Builder builder, FramebufferAttachmentDefinition.Format format,
                                                                     Operation<AdvancedFbo.Builder> original) {
        return original.call(builder, VeilDepthFixes.getMainDepthFormat(format));
    }
}
