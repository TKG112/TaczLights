package net.tkg.taczlights.client.display;

import org.jetbrains.annotations.Nullable;

/**
 * Implemented on TaCZ's display objects by mixin, to carry the lights read from their {@code "taczlights"} block.
 */
public interface LightDataHolder<T> {
    @Nullable
    T taczlights$getLightData();

    void taczlights$setLightData(@Nullable T data);
}
