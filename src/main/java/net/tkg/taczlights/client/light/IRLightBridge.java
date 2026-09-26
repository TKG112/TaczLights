package net.tkg.taczlights.client.light;

import foundry.veil.api.client.render.light.data.LightData;
import foundry.veil.api.client.render.light.renderer.LightRenderHandle;
import net.neoforged.fml.ModList;
import net.tkg.taczlights.TaczLights;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Infrared lights, which only ModernMayhem's night vision goggles can see. They go to ModernMayhem's
 * {@code net.tkg.ModernMayhem.api.IRLights} instead of Veil's light renderer, so the naked eye never sees them.
 * <p>
 * Found by reflection so neither mod needs the other to build. Without ModernMayhem there is nothing that could see
 * an IR light, so IR lights are simply not created.
 */
public final class IRLightBridge {
    private static final String MM_ID = "mm";
    private static final String API_CLASS = "net.tkg.ModernMayhem.api.IRLights";

    @Nullable
    private static final MethodHandle ADD_LIGHT = findAddLight();
    @Nullable
    private static final MethodHandle RENDER_IR_LASER = find("renderIRLaser", MethodType.methodType(void.class, Runnable.class));

    private IRLightBridge() {
    }

    public static boolean isAvailable() {
        return ADD_LIGHT != null;
    }

    /**
     * Adds an IR light to ModernMayhem. Only call when {@link #isAvailable()}.
     */
    @SuppressWarnings("unchecked")
    public static <T extends LightData> LightRenderHandle<T> addLight(T light) {
        try {
            return (LightRenderHandle<T>) ADD_LIGHT.invoke(light);
        } catch (Throwable t) {
            throw new IllegalStateException("ModernMayhem failed to add an IR light", t);
        }
    }

    /**
     * Draws laser beams as infrared through ModernMayhem, which only shows them through its night vision. Without
     * ModernMayhem nothing is drawn: nobody could see them.
     *
     * @param draw The call that renders the beams
     */
    public static void renderIRLaser(Runnable draw) {
        if (RENDER_IR_LASER == null) {
            return;
        }
        try {
            RENDER_IR_LASER.invoke(draw);
        } catch (Throwable t) {
            throw new IllegalStateException("ModernMayhem failed to draw an IR laser", t);
        }
    }

    @Nullable
    private static MethodHandle findAddLight() {
        MethodHandle handle = find("addLight", MethodType.methodType(LightRenderHandle.class, LightData.class));
        if (handle != null) {
            TaczLights.LOGGER.info("ModernMayhem found: IR lights will be visible through its night vision");
        }
        return handle;
    }

    @Nullable
    private static MethodHandle find(String name, MethodType type) {
        if (!ModList.get().isLoaded(MM_ID)) {
            return null;
        }
        try {
            return MethodHandles.publicLookup().findStatic(Class.forName(API_CLASS), name, type);
        } catch (ReflectiveOperationException e) {
            TaczLights.LOGGER.warn("ModernMayhem is installed but its IR API has no {} ({}); that IR feature is disabled", name, e.toString());
            return null;
        }
    }
}
