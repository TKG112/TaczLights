package net.tkg.taczlights.client.light;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.tkg.taczlights.TaczLightsComponents;

import java.util.List;
import java.util.Optional;

/**
 * The light modes of one attachment. The light key cycles the attachment through off, mode 1, mode 2, ... and back to off.
 * <p>
 * The display's {@code "taczlights"} block lists {@code "modes"}, each with its own {@code "lights"}, or uses {@code "lights"} directly as a shorthand for a single mode.
 * A laser-only attachment can leave the lights out and just set {@code "ir_laser"} and/or {@code "laser_light"}.
 *
 * @param defaultMode The mode the attachment starts in before it is ever switched, 0 being off
 * @param irLaser     The attachment's laser can be switched to infrared: the laser key then cycles on, IR, off. An IR
 *                    beam is only seen through ModernMayhem's night vision goggles (and not drawn at all without it)
 * @param laserLight  The light the attachment's laser casts on what it hits, see {@link LaserLight}
 */
public record AttachmentLightModes(List<Mode> modes, int defaultMode, boolean irLaser, Optional<LaserLight> laserLight) {
    private static final Codec<AttachmentLightModes> FULL_CODEC = RecordCodecBuilder.<AttachmentLightModes>create(instance -> instance.group(
            ExtraCodecs.nonEmptyList(Mode.CODEC.listOf()).fieldOf("modes").forGetter(AttachmentLightModes::modes),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("default_mode", 1).forGetter(AttachmentLightModes::defaultMode),
            Codec.BOOL.optionalFieldOf("ir_laser", false).forGetter(AttachmentLightModes::irLaser),
            LaserLight.CODEC.optionalFieldOf("laser_light").forGetter(AttachmentLightModes::laserLight)
    ).apply(instance, AttachmentLightModes::new)).validate(AttachmentLightModes::validate);

    private static final Codec<AttachmentLightModes> SINGLE_MODE_CODEC = RecordCodecBuilder.<AttachmentLightModes>create(instance -> instance.group(
            AttachmentLight.CODEC.listOf().optionalFieldOf("lights", List.of()).forGetter(modes -> modes.modes.getFirst().lights()),
            Codec.intRange(0, 1).optionalFieldOf("default_mode", 1).forGetter(AttachmentLightModes::defaultMode),
            Codec.BOOL.optionalFieldOf("ir_laser", false).forGetter(AttachmentLightModes::irLaser),
            LaserLight.CODEC.optionalFieldOf("laser_light").forGetter(AttachmentLightModes::laserLight)
    ).apply(instance, (lights, defaultMode, irLaser, laserLight) -> new AttachmentLightModes(List.of(new Mode(Optional.empty(), lights)), defaultMode, irLaser, laserLight)));

    public static final Codec<AttachmentLightModes> CODEC = Codec.withAlternative(FULL_CODEC, SINGLE_MODE_CODEC);

    /**
     * @param mode The gun's mode, 0 being off
     * @return The lights to show for the mode. A mode past this attachment's last one shows its last mode
     */
    public List<AttachmentLight> getLights(int mode) {
        if (mode <= 0) {
            return List.of();
        }
        return this.modes.get(Math.min(mode, this.modes.size()) - 1).lights();
    }

    /**
     * @return Whether any mode has lights, i.e. whether the light key has anything to switch on this attachment
     */
    public boolean hasLights() {
        return this.modes.stream().anyMatch(mode -> !mode.lights().isEmpty());
    }

    /**
     * @return The mode the attachment is in, 0 being off
     */
    public int getMode(ItemStack attachment) {
        Integer mode = attachment.get(TaczLightsComponents.LIGHT_MODE);
        return mode != null ? mode : this.defaultMode;
    }

    private static DataResult<AttachmentLightModes> validate(AttachmentLightModes modes) {
        if (modes.defaultMode > modes.modes.size()) {
            return DataResult.error(() -> "default_mode " + modes.defaultMode + " is past the last mode " + modes.modes.size());
        }
        return DataResult.success(modes);
    }

    /**
     * @param name Shown to the player when switching to this mode. Can be a translation key
     */
    public record Mode(Optional<String> name, List<AttachmentLight> lights) {
        public static final Codec<Mode> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("name").forGetter(Mode::name),
                AttachmentLight.CODEC.listOf().fieldOf("lights").forGetter(Mode::lights)
        ).apply(instance, Mode::new));
    }
}
