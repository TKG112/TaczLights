package net.tkg.taczlights.client.veilfix;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.FramebufferAttachmentDefinition;
import foundry.veil.api.client.render.framebuffer.FramebufferDefinition;
import foundry.veil.api.client.render.framebuffer.FramebufferManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.tkg.taczlights.mixin.veilfix.AdvancedFboBuilderAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL30C.*;

/**
 * Helpers for the Veil depth and stencil fixes in {@code net.tkg.taczlights.mixin.veilfix}.
 * <p>
 * TaCZ enables stencil on Minecraft's main render target, which changes its depth format. Veil assumed a plain depth
 * buffer in a few places, which breaks its deferred lights: blitting depth between framebuffers needs the formats to
 * match exactly, and wrapping a depth-stencil texture as depth-only makes clearing it fail.
 */
public final class VeilDepthFixes {
    private VeilDepthFixes() {
    }

    /**
     * @param fallback The format to use when the main render target can't be inspected
     * @return The concrete depth format of Minecraft's main render target
     */
    public static FramebufferAttachmentDefinition.Format getMainDepthFormat(@Nullable FramebufferAttachmentDefinition.Format fallback) {
        Minecraft client = Minecraft.getInstance();
        RenderTarget mainRenderTarget = client != null ? client.getMainRenderTarget() : null;
        if (mainRenderTarget == null || !mainRenderTarget.useDepth) {
            return fallback;
        }

        int depthTexture = mainRenderTarget.getDepthTextureId();
        if (depthTexture <= 0) {
            return fallback;
        }

        int previousTexture = glGetInteger(GL_TEXTURE_BINDING_2D);
        glBindTexture(GL_TEXTURE_2D, depthTexture);
        int internalFormat = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_INTERNAL_FORMAT);
        glBindTexture(GL_TEXTURE_2D, previousTexture);

        for (FramebufferAttachmentDefinition.Format format : FramebufferAttachmentDefinition.Format.VALUES) {
            if (format.getInternalFormat() == internalFormat && (format.getFormat() == GL_DEPTH_COMPONENT || format.getFormat() == GL_DEPTH_STENCIL)) {
                return format;
            }
        }
        return fallback;
    }

    /**
     * @return Whether the texture has a stencil component
     */
    public static boolean hasStencil(int depthTexture) {
        int previousTexture = glGetInteger(GL_TEXTURE_BINDING_2D);
        GlStateManager._bindTexture(depthTexture);
        int format = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_INTERNAL_FORMAT);
        GlStateManager._bindTexture(previousTexture);
        return format == GL_DEPTH_STENCIL || format == GL_DEPTH24_STENCIL8 || format == GL_DEPTH32F_STENCIL8;
    }

    /**
     * Wraps a depth-stencil texture as the builder's depth-stencil attachment, keeping its stencil.
     */
    public static AdvancedFbo.Builder setDepthStencilTextureWrapper(AdvancedFbo.Builder builder, int textureId) {
        return builder.setDepthBuffer(new DepthStencilTextureAttachment(textureId, ((AdvancedFboBuilderAccessor) builder).taczlights$getName()));
    }

    /**
     * Gets a framebuffer that exchanges depth with the main render target, first rebuilding it with the main target's
     * depth format if it doesn't match.
     *
     * @param getFramebuffer Gets the framebuffer as it currently is
     */
    @Nullable
    public static AdvancedFbo matchDepthFormat(FramebufferManager framebufferManager, ResourceLocation name,
                                               @Nullable FramebufferAttachmentDefinition.Format format, Supplier<AdvancedFbo> getFramebuffer) {
        AdvancedFbo fbo = getFramebuffer.get();
        if (format == null || fbo == null || !fbo.hasDepthAttachment() || fbo.getDepthAttachment().getFormat() == format.getInternalFormat()) {
            return fbo;
        }

        FramebufferDefinition definition = framebufferManager.getFramebufferDefinition(name);
        FramebufferAttachmentDefinition depth = definition != null ? definition.depthBuffer() : null;
        if (depth == null) {
            return fbo;
        }

        framebufferManager.setDefinition(name, new FramebufferDefinition(
                definition.width(),
                definition.height(),
                definition.colorBuffers(),
                new FramebufferAttachmentDefinition(depth.type(), format, true, depth.filter(), depth.levels(), depth.name()),
                definition.autoClear()));
        return framebufferManager.getFramebuffer(name);
    }
}
