package net.tkg.taczlights.client.beam;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import foundry.veil.api.client.render.light.renderer.InstancedLightRenderer;
import foundry.veil.api.client.render.light.renderer.LightRenderHandle;
import foundry.veil.api.client.render.light.renderer.LightTypeRenderer;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import foundry.veil.api.client.render.vertex.VertexArray;
import foundry.veil.api.client.render.vertex.VertexArrayBuilder;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.tkg.taczlights.TaczLights;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Draws {@link BeamLightData} as instanced boxes around each beam, shaded by {@code taczlights:light/beam}.
 * Modelled on Veil's own spot light renderer.
 */
public class BeamLightRenderer extends InstancedLightRenderer<BeamLightData> {
    private static final ResourceLocation RENDER_TYPE = ResourceLocation.fromNamespaceAndPath(TaczLights.MODID, "light/beam");

    public BeamLightRenderer() {
        super(Float.BYTES * 22);
    }

    @Override
    protected MeshData createMesh() {
        BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION);
        LightTypeRenderer.createInvertedCube(builder);
        return builder.buildOrThrow();
    }

    @Override
    protected void setupBufferState(VertexArrayBuilder builder) {
        builder.setVertexAttribute(1, VertexArray.INSTANCE_BUFFER, 4, VertexArrayBuilder.DataType.FLOAT, false, 0);
        builder.setVertexAttribute(2, VertexArray.INSTANCE_BUFFER, 4, VertexArrayBuilder.DataType.FLOAT, false, Float.BYTES * 4);
        builder.setVertexAttribute(3, VertexArray.INSTANCE_BUFFER, 4, VertexArrayBuilder.DataType.FLOAT, false, Float.BYTES * 8);
        builder.setVertexAttribute(4, VertexArray.INSTANCE_BUFFER, 4, VertexArrayBuilder.DataType.FLOAT, false, Float.BYTES * 12); // matrix
        builder.setVertexAttribute(5, VertexArray.INSTANCE_BUFFER, 3, VertexArrayBuilder.DataType.FLOAT, false, Float.BYTES * 16); // color
        builder.setVertexAttribute(6, VertexArray.INSTANCE_BUFFER, 3, VertexArrayBuilder.DataType.FLOAT, false, Float.BYTES * 19); // radius/length/softness
    }

    @Override
    protected @Nullable RenderType getRenderType(List<? extends LightRenderHandle<BeamLightData>> lights) {
        return VeilRenderType.get(RENDER_TYPE);
    }

    @Override
    protected @Nullable RenderType getInscatteringRenderType(List<? extends LightRenderHandle<BeamLightData>> lights) {
        // No in-scattering (visible beam in the air) yet
        return null;
    }
}
