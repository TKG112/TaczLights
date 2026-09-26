package net.tkg.taczlights.client.flash;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.tkg.taczlights.client.light.AttachmentLight;

/**
 * The light a gun flashes when it fires. Every gun uses {@link #DEFAULT} unless a gunpack overrides it in the gun's
 * display file, under {@code "taczlights": {"muzzle_flash": {...}}}.
 *
 * @param offset   How far in front of the shooter's eyes the light appears, in blocks
 * @param duration How long the flash takes to fade out, in milliseconds
 */
public record MuzzleFlashLight(
        boolean enabled,
        int color,
        float brightness,
        float radius,
        float offset,
        int duration,
        boolean occlusion
) {
    public static final MuzzleFlashLight DEFAULT = new MuzzleFlashLight(true, 0xFFB463, 2.0F, 20.0F, 1.0F, 60, false);

    public static final Codec<MuzzleFlashLight> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("enabled", DEFAULT.enabled).forGetter(MuzzleFlashLight::enabled),
            AttachmentLight.HEX_COLOR.optionalFieldOf("color", DEFAULT.color).forGetter(MuzzleFlashLight::color),
            Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("brightness", DEFAULT.brightness).forGetter(MuzzleFlashLight::brightness),
            Codec.floatRange(0, 64).optionalFieldOf("radius", DEFAULT.radius).forGetter(MuzzleFlashLight::radius),
            Codec.floatRange(0, 8).optionalFieldOf("offset", DEFAULT.offset).forGetter(MuzzleFlashLight::offset),
            Codec.intRange(1, 1000).optionalFieldOf("duration", DEFAULT.duration).forGetter(MuzzleFlashLight::duration),
            Codec.BOOL.optionalFieldOf("occlusion", DEFAULT.occlusion).forGetter(MuzzleFlashLight::occlusion)
    ).apply(instance, MuzzleFlashLight::new));
}
