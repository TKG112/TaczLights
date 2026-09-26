package net.tkg.taczlights;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.tkg.taczlights.network.SetLaserPayload;
import net.tkg.taczlights.network.SetLightModePayload;
import org.slf4j.Logger;

@Mod(TaczLights.MODID)
public class TaczLights {
    public static final String MODID = "taczlights";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TaczLights(IEventBus modEventBus, ModContainer modContainer) {
        TaczLightsComponents.REGISTER.register(modEventBus);
        modEventBus.addListener(TaczLights::registerPayloads);
        modContainer.registerConfig(ModConfig.Type.CLIENT, TaczLightsConfig.SPEC);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToServer(SetLightModePayload.TYPE, SetLightModePayload.STREAM_CODEC, SetLightModePayload::handle)
                .playToServer(SetLaserPayload.TYPE, SetLaserPayload.STREAM_CODEC, SetLaserPayload::handle);
    }
}
