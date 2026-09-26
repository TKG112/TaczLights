package net.tkg.taczlights.client;

import com.tacz.guns.api.event.common.GunFireEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.tkg.taczlights.TaczLights;
import net.tkg.taczlights.client.flash.MuzzleFlashTracker;
import net.tkg.taczlights.client.light.AttachmentLightTracker;

@EventBusSubscriber(modid = TaczLights.MODID, value = Dist.CLIENT)
public final class TaczLightsClient {
    private TaczLightsClient() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(LightToggleKey.TOGGLE_LIGHTS);
        event.register(LaserToggleKey.TOGGLE_LASER);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        LightToggleKey.tick();
        LaserToggleKey.tick();
    }

    @SubscribeEvent
    public static void onRenderFramePre(RenderFrameEvent.Pre event) {
        AttachmentLightTracker.flush();
        MuzzleFlashTracker.flush();
    }

    @SubscribeEvent
    public static void onGunFire(GunFireEvent event) {
        MuzzleFlashTracker.onGunFire(event);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        AttachmentLightTracker.clear();
        MuzzleFlashTracker.clear();
    }
}
