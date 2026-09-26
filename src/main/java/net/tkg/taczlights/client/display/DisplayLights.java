package net.tkg.taczlights.client.display;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.resource.pojo.display.attachment.AttachmentDisplay;
import com.tacz.guns.client.resource.pojo.display.gun.GunDisplay;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.tkg.taczlights.TaczLights;
import net.tkg.taczlights.client.flash.GunLights;
import net.tkg.taczlights.client.light.AttachmentLightModes;
import net.tkg.taczlights.mixin.ClientAttachmentIndexAccessor;
import net.tkg.taczlights.mixin.GunDisplayInstanceAccessor;
import org.jetbrains.annotations.Nullable;

/**
 * Reads and looks up the {@code "taczlights"} block that gunpacks add to TaCZ display files:
 * {@code display/attachments/*.json} for attachment lights and {@code display/guns/*.json} for muzzle flashes.
 */
public final class DisplayLights {
    public static final String FIELD = "taczlights";

    private DisplayLights() {
    }

    /**
     * @return The light modes of the attachment, or null if its display declares none
     */
    @Nullable
    public static AttachmentLightModes getAttachmentModes(ResourceLocation attachmentId) {
        return TimelessAPI.getClientAttachmentIndex(attachmentId)
                .map(index -> ((ClientAttachmentIndexAccessor) index).taczlights$getDisplay())
                .map(DisplayLights::<AttachmentLightModes>getLightData)
                .orElse(null);
    }

    /**
     * @return The lights of the gun, using the display the stack currently shows
     */
    public static GunLights getGunLights(ItemStack gun) {
        return TimelessAPI.getGunDisplay(gun)
                .map(instance -> ((GunDisplayInstanceAccessor) instance).taczlights$getDisplay())
                .map(DisplayLights::<GunLights>getLightData)
                .orElse(GunLights.DEFAULT);
    }

    /**
     * Reads the {@code "taczlights"} block of a display file TaCZ just loaded into {@code display}.
     *
     * @return Whether the file had a valid block
     */
    public static boolean read(ResourceLocation displayId, Object display, JsonElement json) {
        if (display instanceof AttachmentDisplay) {
            return read(displayId, display, json, AttachmentLightModes.CODEC);
        } else if (display instanceof GunDisplay) {
            return read(displayId, display, json, GunLights.CODEC);
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T> boolean read(ResourceLocation displayId, Object display, JsonElement json, Codec<T> codec) {
        LightDataHolder<T> holder = (LightDataHolder<T>) display;
        holder.taczlights$setLightData(null);
        if (!(json instanceof JsonObject object) || !object.has(FIELD)) {
            return false;
        }
        return codec.parse(JsonOps.INSTANCE, object.get(FIELD))
                .ifSuccess(holder::taczlights$setLightData)
                .ifError(error -> TaczLights.LOGGER.error("Invalid \"{}\" block in display {}: {}", FIELD, displayId, error.message()))
                .isSuccess();
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <T> T getLightData(Object display) {
        return display instanceof LightDataHolder<?> holder ? ((LightDataHolder<T>) holder).taczlights$getLightData() : null;
    }
}
