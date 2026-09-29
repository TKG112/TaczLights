package net.tkg.taczlights.client;

import com.tacz.guns.api.event.common.GunFireEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.tkg.taczlights.TaczLights;
import net.tkg.taczlights.api.LaserGlow;
import net.tkg.taczlights.client.beam.LaserBloomTarget;
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
        MuzzleFlashTracker.flush();
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            // The camera is set up for this frame by now, and the level's lights aren't drawn yet
            AttachmentLightTracker.setWorldProjection(event.getProjectionMatrix());
            AttachmentLightTracker.flush();
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            // Unless another mod adds the glow itself, e.g. between its darkness and goggle shaders
            if (!LaserGlow.isCompositedExternally()) {
                LaserBloomTarget.WORLD.composite();
            }
        }
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
