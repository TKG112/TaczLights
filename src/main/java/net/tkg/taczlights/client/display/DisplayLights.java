package net.tkg.taczlights.client.display;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.resource.pojo.display.attachment.AttachmentDisplay;
import com.tacz.guns.client.resource.pojo.display.gun.GunDisplay;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.tkg.taczlights.TaczLights;
import net.tkg.taczlights.client.flash.GunLights;
import net.tkg.taczlights.client.light.AttachmentLightModes;
import net.tkg.taczlights.mixin.ClientAttachmentIndexAccessor;
import net.tkg.taczlights.mixin.GunDisplayInstanceAccessor;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.util.Optional;

/**
 * Reads and looks up the {@code "taczlights"} block that gunpacks add to TaCZ display files:
 * {@code display/attachments/*.json} for attachment lights and {@code display/guns/*.json} for muzzle flashes.
 * A display without its own block can get one from a fallback file instead, see {@link #readFallback}.
 */
public final class DisplayLights {
    public static final String FIELD = "taczlights";
    private static final String FALLBACK_DIRECTORY = "taczlights/displays/";
    private static final Gson GSON = new Gson();

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
    public static boolean read(ResourceLocation displayId, Object display, JsonElement json, ResourceManager resourceManager) {
        if (display instanceof AttachmentDisplay) {
            return read(displayId, display, json, resourceManager, "attachments", AttachmentLightModes.CODEC);
        } else if (display instanceof GunDisplay) {
            return read(displayId, display, json, resourceManager, "guns", GunLights.CODEC);
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T> boolean read(ResourceLocation displayId, Object display, JsonElement json, ResourceManager resourceManager,
                                    String folder, Codec<T> codec) {
        LightDataHolder<T> holder = (LightDataHolder<T>) display;
        holder.taczlights$setLightData(null);
        JsonElement block = json instanceof JsonObject object && object.has(FIELD)
                ? object.get(FIELD)
                : readFallback(displayId, resourceManager, folder);
        if (block == null) {
            return false;
        }
        return codec.parse(JsonOps.INSTANCE, block)
                .ifSuccess(holder::taczlights$setLightData)
                .ifError(error -> TaczLights.LOGGER.error("Invalid \"{}\" block in display {}: {}", FIELD, displayId, error.message()))
                .isSuccess();
    }

    /**
     * Reads the fallback block for a display that doesn't have its own, from
     * {@code assets/<namespace>/taczlights/displays/<folder>/<display_path>.json}. It holds only what would go in the
     * display's {@code "taczlights"} block, so lights can be added to a gunpack's displays without editing or copying
     * them.
     */
    @Nullable
    private static JsonElement readFallback(ResourceLocation displayId, ResourceManager resourceManager, String folder) {
        ResourceLocation location = displayId.withPath(path -> FALLBACK_DIRECTORY + folder + "/" + path + ".json");
        Optional<Resource> resource = resourceManager.getResource(location);
        if (resource.isEmpty()) {
            return null;
        }
        try (Reader reader = resource.get().openAsReader()) {
            return GsonHelper.fromJson(GSON, reader, JsonElement.class, true);
        } catch (IOException | JsonParseException e) {
            TaczLights.LOGGER.error("Couldn't read fallback lights {} for display {}", location, displayId, e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <T> T getLightData(Object display) {
        return display instanceof LightDataHolder<?> holder ? ((LightDataHolder<T>) holder).taczlights$getLightData() : null;
    }
}
