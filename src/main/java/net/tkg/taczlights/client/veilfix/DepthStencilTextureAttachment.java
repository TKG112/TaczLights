package net.tkg.taczlights.client.veilfix;

import foundry.veil.impl.client.render.framebuffer.AdvancedFboMutableTextureAttachment;
import org.jetbrains.annotations.Nullable;

import static org.lwjgl.opengl.GL30C.GL_DEPTH_STENCIL;
import static org.lwjgl.opengl.GL30C.GL_DEPTH_STENCIL_ATTACHMENT;

/**
 * Wraps an existing depth-stencil texture as a framebuffer's depth-stencil attachment.
 * <p>
 * Veil's mutable texture attachment always reports a format of 0, so a framebuffer built with it never knows it has
 * stencil. Reporting {@code GL_DEPTH_STENCIL} lets it clear and compare the attachment correctly.
 */
public class DepthStencilTextureAttachment extends AdvancedFboMutableTextureAttachment {
    public DepthStencilTextureAttachment(int textureId, @Nullable String name) {
        super(GL_DEPTH_STENCIL_ATTACHMENT, textureId, -1, name);
    }

    @Override
    public int getFormat() {
        return GL_DEPTH_STENCIL;
    }

    @Override
    public DepthStencilTextureAttachment clone() {
        return new DepthStencilTextureAttachment(this.getId(), this.getName());
    }
}
