package net.tkg.taczlights.client.beam;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.client.resource.pojo.display.LaserConfig;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.util.LaserColorUtil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.tkg.taczlights.TaczLights;
import net.tkg.taczlights.TaczLightsConfig;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.util.List;

/**
 * Makes TaCZ's laser beams glow: draws each beam a second time into a bloom buffer (see {@link LaserBloomTarget}),
 * which gets blurred and added back onto the screen. Uses the same shape, color and fade as TaCZ's own beam.
 * <p>
 * TaCZ draws a laser before the attachment around it, the rest of the gun and the arms holding it, so at that point
 * none of them would hide the glow yet. The beams are only recorded while rendering and drawn into the bloom buffer
 * once everything they're drawn with is in the depth buffer.
 */
public final class LaserBloom {
    private static final ResourceLocation RENDER_TYPE = ResourceLocation.fromNamespaceAndPath(TaczLights.MODID, "laser_bloom");
    private static final ResourceLocation SHADER = ResourceLocation.fromNamespaceAndPath(TaczLights.MODID, "laser_bloom");
    private static final LaserConfig DEFAULT_LASER_CONFIG = new LaserConfig();

    private LaserBloom() {
    }

    /**
     * Records the glow of a laser beam TaCZ just drew.
     *
     * @param stack The attachment, or the gun for a laser built into its model
     * @param path  The bones from the model root down to the {@code laser_beam} bone
     */
    public static void render(ItemStack stack, PoseStack poseStack, ItemDisplayContext transformType, List<BedrockPart> path) {
        // TaCZ only draws beams in these views
        boolean firstPerson = transformType.firstPerson();
        if (!firstPerson && transformType != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            return;
        }
        if (!TaczLightsConfig.LASER_BLOOM.get() || !LaserBloomTarget.isAvailable() || TaczLightsConfig.LASER_BLOOM_STRENGTH.get() <= 0.0) {
            return;
        }

        LaserConfig config = getLaserConfig(stack);
        int color = LaserColorUtil.getLaserColor(stack, config);
        float length = firstPerson ? config.getLength() : config.getLengthThird();
        float width = (firstPerson ? config.getWidth() : config.getWidthThird()) * TaczLightsConfig.LASER_BLOOM_WIDTH.get().floatValue();

        poseStack.pushPose();
        for (BedrockPart part : path) {
            part.translateAndRotateAndScale(poseStack);
        }
        // Everything needed to draw it later exactly where it is now
        Matrix4f toView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(poseStack.last().pose());
        poseStack.popPose();

        LaserBloomTarget target = LaserBloomTarget.forView(firstPerson);
        target.prepare();
        target.add(new Beam(toView, new Matrix4f(RenderSystem.getProjectionMatrix()), -length, width, color, RenderConfig.ENABLE_LASER_FADE_OUT.get()));
    }

    /**
     * Draws recorded beams into the laser bloom framebuffer, with the projection they were recorded with.
     */
    static void draw(List<Beam> beams) {
        RenderType renderType = VeilRenderType.get(RENDER_TYPE);
        if (renderType == null || beams.isEmpty()) {
            return;
        }
        ShaderProgram shader = VeilRenderSystem.renderer().getShaderManager().getShader(SHADER);
        if (shader != null) {
            shader.getUniformSafe("BloomStrength").setFloat(TaczLightsConfig.LASER_BLOOM_STRENGTH.get().floatValue());
        }

        Matrix4f previousProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting previousSorting = RenderSystem.getVertexSorting();
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        RenderSystem.applyModelViewMatrix();

        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        Matrix4f projection = null;
        for (Beam beam : beams) {
            if (!beam.projection.equals(projection)) {
                // Beams from the hand and from the level use different projections
                if (projection != null) {
                    buffers.endBatch(renderType);
                }
                projection = beam.projection;
                RenderSystem.setProjectionMatrix(projection, VertexSorting.DISTANCE_TO_ORIGIN);
            }
            addBeam(buffers.getBuffer(renderType), beam);
        }
        buffers.endBatch(renderType);

        modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(previousProjection, previousSorting);
    }

    /**
     * The same four sided beam TaCZ draws, along -Z from the bone.
     */
    private static void addBeam(VertexConsumer consumer, Beam beam) {
        float half = beam.width / 2.0F;
        int red = (beam.color >> 16) & 0xFF;
        int green = (beam.color >> 8) & 0xFF;
        int blue = beam.color & 0xFF;
        int endAlpha = beam.fadeOut ? 0 : 255;

        addSide(consumer, beam.toView, -half, -half, -half, half, beam.z, red, green, blue, endAlpha);
        addSide(consumer, beam.toView, -half, half, half, half, beam.z, red, green, blue, endAlpha);
        addSide(consumer, beam.toView, half, half, half, -half, beam.z, red, green, blue, endAlpha);
        addSide(consumer, beam.toView, half, -half, -half, -half, beam.z, red, green, blue, endAlpha);
    }

    private static void addSide(VertexConsumer consumer, Matrix4f pose, float x1, float y1, float x2, float y2, float z,
                                int red, int green, int blue, int endAlpha) {
        consumer.addVertex(pose, x1, y1, 0).setColor(red, green, blue, 255).setUv(0, 0).setLight(LightTexture.FULL_BRIGHT);
        consumer.addVertex(pose, x2, y2, 0).setColor(red, green, blue, 255).setUv(0, 1).setLight(LightTexture.FULL_BRIGHT);
        consumer.addVertex(pose, x2, y2, z).setColor(red, green, blue, endAlpha).setUv(1, 1).setLight(LightTexture.FULL_BRIGHT);
        consumer.addVertex(pose, x1, y1, z).setColor(red, green, blue, endAlpha).setUv(1, 0).setLight(LightTexture.FULL_BRIGHT);
    }

    /**
     * @param stack The attachment, or the gun for a laser built into its model
     * @return The laser settings from its display, or TaCZ's defaults
     */
    public static LaserConfig getLaserConfig(ItemStack stack) {
        if (stack.getItem() instanceof IAttachment attachment) {
            return TimelessAPI.getClientAttachmentIndex(attachment.getAttachmentId(stack))
                    .map(ClientAttachmentIndex::getLaserConfig)
                    .orElse(DEFAULT_LASER_CONFIG);
        }
        if (stack.getItem() instanceof IGun) {
            return TimelessAPI.getGunDisplay(stack)
                    .map(GunDisplayInstance::getLaserConfig)
                    .orElse(DEFAULT_LASER_CONFIG);
        }
        return DEFAULT_LASER_CONFIG;
    }

    /**
     * A beam waiting to be drawn.
     *
     * @param toView     From the {@code laser_beam} bone's space to view space
     * @param projection The projection it was drawn with; the hand uses its own field of view
     * @param z          Where it ends along the bone's Z axis
     */
    record Beam(Matrix4f toView, Matrix4f projection, float z, float width, int color, boolean fadeOut) {
    }
}
