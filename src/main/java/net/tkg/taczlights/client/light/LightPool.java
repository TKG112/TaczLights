package net.tkg.taczlights.client.light;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.light.data.LightData;
import foundry.veil.api.client.render.light.renderer.LightRenderHandle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Reuses Veil light handles across frames, so lights that move every frame aren't re-added each time.
 */
public final class LightPool<T extends LightData> {
    private final Supplier<T> factory;
    private final Function<T, LightRenderHandle<T>> adder;
    private final List<LightRenderHandle<T>> handles = new ArrayList<>();

    /**
     * A pool of ordinary lights, added to Veil's light renderer.
     */
    public LightPool(Supplier<T> factory) {
        this(factory, light -> VeilRenderSystem.renderer().getLightRenderer().addLight(light));
    }

    /**
     * @param adder Adds a new light wherever this pool's lights belong (e.g. {@link IRLightBridge#addLight})
     */
    public LightPool(Supplier<T> factory, Function<T, LightRenderHandle<T>> adder) {
        this.factory = factory;
        this.adder = adder;
    }

    /**
     * @return The light at the given index, added to Veil if it doesn't exist yet. Indices must be used in order from 0
     */
    public T get(int index) {
        if (index < this.handles.size()) {
            LightRenderHandle<T> handle = this.handles.get(index);
            if (handle.isValid()) {
                return handle.getLightData();
            }
            // Veil dropped the light, e.g. after its renderer was recreated
            LightRenderHandle<T> readded = this.add();
            this.handles.set(index, readded);
            return readded.getLightData();
        }
        LightRenderHandle<T> handle = this.add();
        this.handles.add(handle);
        return handle.getLightData();
    }

    /**
     * Removes every light past the first {@code keep}.
     */
    public void trim(int keep) {
        while (this.handles.size() > keep) {
            LightRenderHandle<T> handle = this.handles.removeLast();
            if (handle.isValid()) {
                handle.free();
            }
        }
    }

    private LightRenderHandle<T> add() {
        return this.adder.apply(this.factory.get());
    }
}
