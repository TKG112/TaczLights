package net.tkg.taczlights.mixin;

import com.tacz.guns.client.resource.pojo.display.gun.GunDisplay;
import net.tkg.taczlights.client.display.LightDataHolder;
import net.tkg.taczlights.client.flash.GunLights;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = GunDisplay.class, remap = false)
public abstract class GunDisplayMixin implements LightDataHolder<GunLights> {
    @Unique
    @Nullable
    private transient GunLights taczlights$lightData;

    @Override
    @Nullable
    public GunLights taczlights$getLightData() {
        return this.taczlights$lightData;
    }

    @Override
    public void taczlights$setLightData(@Nullable GunLights data) {
        this.taczlights$lightData = data;
    }
}
