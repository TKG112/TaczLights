package net.tkg.taczlights.mixin;

import com.google.gson.JsonElement;
import com.tacz.guns.client.resource.manager.DisplayManager;
import com.tacz.guns.resource.manager.JsonDataManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.tkg.taczlights.TaczLights;
import net.tkg.taczlights.client.display.DisplayLights;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Reads the {@code "taczlights"} block of each display file once TaCZ has loaded it.
 */
@Mixin(value = DisplayManager.class, remap = false)
public abstract class DisplayManagerMixin {
    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("TAIL"))
    private void taczlights$readLights(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci) {
        JsonDataManager<?> manager = (JsonDataManager<?>) (Object) this;
        int count = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            Object display = manager.getData(entry.getKey());
            if (display != null && DisplayLights.read(entry.getKey(), display, entry.getValue())) {
                count++;
            }
        }
        if (count > 0) {
            TaczLights.LOGGER.info("Loaded lights from {} {} display file(s)", count, manager.getDataClass().getSimpleName());
        }
    }
}
