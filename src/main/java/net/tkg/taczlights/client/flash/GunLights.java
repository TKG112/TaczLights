package net.tkg.taczlights.client.flash;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The {@code "taczlights"} block of a TaCZ gun display file.
 */
public record GunLights(MuzzleFlashLight muzzleFlash) {
    public static final GunLights DEFAULT = new GunLights(MuzzleFlashLight.DEFAULT);

    public static final Codec<GunLights> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MuzzleFlashLight.CODEC.optionalFieldOf("muzzle_flash", MuzzleFlashLight.DEFAULT).forGetter(GunLights::muzzleFlash)
    ).apply(instance, GunLights::new));
}
