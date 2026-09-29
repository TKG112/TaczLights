package net.tkg.taczlights.client.beam;

import com.mojang.blaze3d.pipeline.RenderTarget;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.FramebufferAttachmentDefinition;
import foundry.veil.api.client.render.framebuffer.FramebufferStack;
import foundry.veil.api.client.render.framebuffer.VeilFramebuffers;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.post.PostProcessingManager;
import foundry.veil.api.compat.IrisCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.tkg.taczlights.TaczLights;
import net.tkg.taczlights.client.veilfix.VeilDepthFixes;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11C.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE;
import static org.lwjgl.opengl.GL30C.*;

/**
 * A bloom buffer laser beams glow into, and the pass that adds that glow to the screen.
 * <p>
 * Each buffer shares the depth of what its beams are drawn with, so only the parts of a beam that are actually visible
 * glow. Veil's own bloom is applied right after block entities, before translucent blocks, clouds and particles, which
 * would cover a laser's glow even with the beam in front of them, so these run later:
 * <ul>
 *     <li>{@link #WORLD}: beams held by others and in third person, sharing the level's depth and applied at the end of
 *     the level</li>
 *     <li>{@link #FIRST_PERSON}: first person beams, sharing the depth of the hand they're drawn with and applied after
 *     it, so a beam in front of the gun glows over it and one behind it doesn't</li>
 * </ul>
 * Both go through the same framebuffer name, render type and pipeline; each points the name at its own buffer while
 * drawing into it and while applying it.
 */
public final class LaserBloomTarget {
    public static final ResourceLocation FRAMEBUFFER = ResourceLocation.fromNamespaceAndPath(TaczLights.MODID, "laser_bloom");
    private static final ResourceLocation PIPELINE = ResourceLocation.fromNamespaceAndPath(TaczLights.MODID, "laser_bloom");
    /** The same, but without glow wherever the first person hand was drawn. */
    private static final ResourceLocation HAND_MASKED_PIPELINE = ResourceLocation.fromNamespaceAndPath(TaczLights.MODID, "laser_bloom_hand_masked");

    public static final LaserBloomTarget WORLD = new LaserBloomTarget("TaCZ Lights Laser Bloom", false);
    public static final LaserBloomTarget FIRST_PERSON = new LaserBloomTarget("TaCZ Lights First Person Laser Bloom", true);

    private static boolean warnedMissingPipeline;

    private final String debugLabel;
    private final boolean useBoundDepth;
    @Nullable
    private AdvancedFbo framebuffer;
    private int wrappedDepthTexture = -1;
    private final List<LaserBloom.Beam> beams = new ArrayList<>();

    private LaserBloomTarget(String debugLabel, boolean useBoundDepth) {
        this.debugLabel = debugLabel;
        this.useBoundDepth = useBoundDepth;
    }

    /**
     * @return Whether the glow can be drawn at all. Veil's bloom doesn't work with shader packs
     */
    public static boolean isAvailable() {
        return IrisCompat.INSTANCE == null || !IrisCompat.INSTANCE.areShadersLoaded();
    }

    /**
     * @return The buffer for beams drawn in the given view
     */
    public static LaserBloomTarget forView(boolean firstPerson) {
        return firstPerson ? FIRST_PERSON : WORLD;
    }

    /**
     * Makes sure the buffer exists, matches the screen and shares the right depth, and points the laser bloom
     * framebuffer at it. Call before drawing into it, while the target the beams are drawn with is bound.
     */
    public void prepare() {
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        int depthTexture = this.useBoundDepth ? boundDepthTexture(main) : main.getDepthTextureId();
        if (this.framebuffer == null || this.framebuffer.getWidth() != main.width || this.framebuffer.getHeight() != main.height
                || this.wrappedDepthTexture != depthTexture) {
            this.free();
            AdvancedFbo.Builder builder = AdvancedFbo.withSize(main.width, main.height)
                    .setFormat(FramebufferAttachmentDefinition.Format.RGBA16F)
                    .addColorTextureBuffer();
            if (VeilDepthFixes.hasStencil(depthTexture)) {
                VeilDepthFixes.setDepthStencilTextureWrapper(builder, depthTexture);
            } else {
                builder.setDepthTextureWrapper(depthTexture);
            }
            this.framebuffer = builder.setDebugLabel(this.debugLabel).build(true);
            this.wrappedDepthTexture = depthTexture;
        }
        VeilRenderSystem.renderer().getFramebufferManager().setFramebuffer(FRAMEBUFFER, this.framebuffer);
    }

    /**
     * Queues a beam to draw into the buffer when it's applied. Call {@link #prepare()} first.
     */
    void add(LaserBloom.Beam beam) {
        this.beams.add(beam);
    }

    /**
     * Draws the queued beams into the buffer, now that everything in front of them is in the depth buffer, then blurs
     * them, adds the glow to the screen and clears the buffer.
     */
    public void composite() {
        this.composite(false);
    }

    /**
     * @param behindHand Whether the first person hand has already been drawn in front of these beams. Beams in the
     *                   world don't share its depth, so the hand then masks their glow out
     */
    public void composite(boolean behindHand) {
        if (this.beams.isEmpty() || this.framebuffer == null) {
            this.beams.clear();
            return;
        }
        List<LaserBloom.Beam> beams = List.copyOf(this.beams);
        this.beams.clear();
        PostProcessingManager postProcessingManager = VeilRenderSystem.renderer().getPostProcessingManager();
        ResourceLocation pipelineId = behindHand && VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(VeilFramebuffers.FIRST_PERSON) != null
                ? HAND_MASKED_PIPELINE
                : PIPELINE;
        PostPipeline pipeline = postProcessingManager.getPipeline(pipelineId);
        if (pipeline == null) {
            if (!warnedMissingPipeline) {
                TaczLights.LOGGER.warn("Laser bloom pipeline {} is missing, lasers won't glow", pipelineId);
                warnedMissingPipeline = true;
            }
            return;
        }
        VeilRenderSystem.renderer().getFramebufferManager().setFramebuffer(FRAMEBUFFER, this.framebuffer);
        LaserBloom.draw(beams);
        FramebufferStack.push(null);
        postProcessingManager.runPipeline(pipeline);
        this.framebuffer.clear(GL_COLOR_BUFFER_BIT);
        FramebufferStack.pop(null);
    }

    public void free() {
        if (this.framebuffer != null) {
            if (VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(FRAMEBUFFER) == this.framebuffer) {
                VeilRenderSystem.renderer().getFramebufferManager().removeFramebuffer(FRAMEBUFFER);
            }
            this.framebuffer.free();
            this.framebuffer = null;
        }
        this.wrappedDepthTexture = -1;
        this.beams.clear();
    }

    /**
     * @return The depth texture of the framebuffer being drawn into, e.g. the first person buffer Veil draws the hand
     * into, or the main target's if it can't be read
     */
    private static int boundDepthTexture(RenderTarget main) {
        if (glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING) == 0) {
            return main.getDepthTextureId();
        }
        int type = glGetFramebufferAttachmentParameteri(GL_DRAW_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE);
        if (type != GL_TEXTURE) {
            return main.getDepthTextureId();
        }
        return glGetFramebufferAttachmentParameteri(GL_DRAW_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
    }
}
