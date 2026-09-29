package net.tkg.taczlights.client.light;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.tkg.taczlights.TaczLightsConfig;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * The light a TaCZ laser casts, declared as {@code "laser_light"} in the attachment's {@code "taczlights"} block.
 * Every {@code laser_beam} bone gets a beam light that follows the laser: on while the laser is on, infrared while it
 * is in IR, and in the laser's color unless {@link #color} overrides it. It stops at the first block hit.
 *
 * @param radius     How far from the beam's center line it lights, in blocks
 * @param softness   How much of the radius fades out, from 0 (hard edge) to 1 (fades all the way to the center)
 * @param color      Overrides the laser's color
 * @param distance   How far the light reaches, in blocks. Defaults to the laser's {@code length}. Stops at the first block hit
 * @param startOffset How far along the beam from the {@code laser_beam} bone the light starts, in blocks, so it
 *                    doesn't light up the gun's own barrel
 * @param firstPersonOffset In first person the laser comes from the center of the screen, pointing the way the bone
 *                          does, so it sits on the crosshair and still follows the gun's animations. Its light
 *                          starts this many blocks ahead of the camera, so it doesn't light up the gun
 */
public record LaserLight(
        float radius,
        float softness,
        float brightness,
        Optional<Integer> color,
        Optional<Float> distance,
        boolean firstPerson,
        boolean thirdPerson,
        float startOffset,
        float firstPersonOffset
) {
    /**
     * Used for lasers whose display doesn't declare a {@code laser_light}, when the {@code laserPointerLights} client
     * config is on.
     */
    public static final LaserLight DEFAULT = new LaserLight(0.06F, 0.6F, 2.0F, Optional.empty(), Optional.empty(), true, true, 0.5F, 1.5F);

    public static final Codec<LaserLight> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.floatRange(0.001F, 4).optionalFieldOf("radius", DEFAULT.radius).forGetter(LaserLight::radius),
            Codec.floatRange(0, 1).optionalFieldOf("softness", DEFAULT.softness).forGetter(LaserLight::softness),
            Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("brightness", DEFAULT.brightness).forGetter(LaserLight::brightness),
            AttachmentLight.HEX_COLOR.optionalFieldOf("color").forGetter(LaserLight::color),
            Codec.floatRange(0, 256).optionalFieldOf("distance").forGetter(LaserLight::distance),
            Codec.BOOL.optionalFieldOf("first_person", true).forGetter(LaserLight::firstPerson),
            Codec.BOOL.optionalFieldOf("third_person", true).forGetter(LaserLight::thirdPerson),
            Codec.floatRange(0, 8).optionalFieldOf("start_offset", DEFAULT.startOffset).forGetter(LaserLight::startOffset),
            Codec.floatRange(0, 8).optionalFieldOf("first_person_offset", DEFAULT.firstPersonOffset).forGetter(LaserLight::firstPersonOffset)
    ).apply(instance, LaserLight::new));

    /**
     * @param declared The {@code laser_light} a laser's display declares, if any
     * @return The light the laser casts, or null if it has none
     */
    @Nullable
    public static LaserLight forLaser(Optional<LaserLight> declared) {
        return declared.orElse(TaczLightsConfig.LASER_POINTER_LIGHTS.get() ? DEFAULT : null);
    }
}
