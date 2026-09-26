package net.tkg.taczlights.mixin;

import com.tacz.guns.client.resource.pojo.display.attachment.AttachmentDisplay;
import net.tkg.taczlights.client.display.LightDataHolder;
import net.tkg.taczlights.client.light.AttachmentLightModes;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = AttachmentDisplay.class, remap = false)
public abstract class AttachmentDisplayMixin implements LightDataHolder<AttachmentLightModes> {
    @Unique
    @Nullable
    private transient AttachmentLightModes taczlights$lightData;

    @Override
    @Nullable
    public AttachmentLightModes taczlights$getLightData() {
        return this.taczlights$lightData;
    }

    @Override
    public void taczlights$setLightData(@Nullable AttachmentLightModes data) {
        this.taczlights$lightData = data;
    }
}
