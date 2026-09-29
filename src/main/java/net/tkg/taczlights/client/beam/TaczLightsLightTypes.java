package net.tkg.taczlights.client.beam;

import foundry.veil.api.client.registry.LightTypeRegistry;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tkg.taczlights.TaczLights;

/**
 * Light types this mod adds to Veil. Client only: Veil's light types reference client classes.
 */
public final class TaczLightsLightTypes {
    public static final DeferredRegister<LightTypeRegistry.LightType<?>> REGISTER = DeferredRegister.create(LightTypeRegistry.REGISTRY_KEY, TaczLights.MODID);

    /**
     * The debug factory lets Veil's light editor spawn a beam at the camera, handy for tuning the shader.
     */
    public static final DeferredHolder<LightTypeRegistry.LightType<?>, LightTypeRegistry.LightType<BeamLightData>> BEAM = REGISTER.register("beam",
            () -> new LightTypeRegistry.LightType<>(BeamLightRenderer::new,
                    (level, camera) -> new BeamLightData().setRadius(0.25F).setLength(16.0F).setTo(camera)));

    private TaczLightsLightTypes() {
    }
}
