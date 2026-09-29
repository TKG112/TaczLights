package net.tkg.taczlights.mixin.veilfix;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.FramebufferManager;
import net.minecraft.resources.ResourceLocation;
import net.tkg.taczlights.client.veilfix.VeilDepthFixes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The main depth buffer is blitted into the light buffers, which needs the depth formats to match exactly. Checked
 * every frame, since TaCZ can enable stencil on the main target at any time.
 */
@Mixin(value = VeilRenderSystem.class, remap = false)
public abstract class VeilRenderSystemMixin {
    @WrapOperation(method = "drawLights", at = @At(value = "INVOKE", target = "Lfoundry/veil/api/client/render/framebuffer/FramebufferManager;getFramebuffer(Lnet/minecraft/resources/ResourceLocation;)Lfoundry/veil/api/client/render/framebuffer/AdvancedFbo;"))
    private static AdvancedFbo taczlights$matchMainDepthFormat(FramebufferManager framebufferManager, ResourceLocation name, Operation<AdvancedFbo> original) {
        return VeilDepthFixes.matchDepthFormat(framebufferManager, name, VeilDepthFixes.getMainDepthFormat(null),
                () -> original.call(framebufferManager, name));
    }
}
