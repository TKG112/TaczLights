package net.tkg.taczlights.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.tkg.taczlights.TaczLights;
import net.tkg.taczlights.client.beam.TaczLightsLightTypes;

/**
 * Client-only setup. Veil's light types reference client classes, so they can't be registered from the common entry point.
 */
@Mod(value = TaczLights.MODID, dist = Dist.CLIENT)
public class TaczLightsClientMod {
    public TaczLightsClientMod(IEventBus modEventBus) {
        TaczLightsLightTypes.REGISTER.register(modEventBus);
    }
}
