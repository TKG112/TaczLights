package net.tkg.taczlights.client.light;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;

/**
 * One light emitted by an attachment, as declared by a gunpack in the {@code "taczlights"} block of the
 * attachment's display file.
 * <p>
 * The light sits at the pivot of {@link #bone} in the attachment's model and points along the bone's -Z axis,
 * the same convention TaCZ uses for {@code laser_beam} bones.
 *
 * @param angle   Spot lights only: the angle from the center of the cone to its edge, in degrees
 * @param falloff Spot lights only: how many degrees inside the edge the light starts fading out
 * @param ir      Infrared: invisible to the naked eye, only seen through ModernMayhem's night vision goggles (and not
 *                created at all without ModernMayhem)
 */
public record AttachmentLight(
        Type type,
        String bone,
        int color,
        float brightness,
        float distance,
        float angle,
        float falloff,
        float radius,
        boolean occlusion,
        float inscattering,
        boolean firstPerson,
        boolean thirdPerson,
        boolean ir
) {
    public static final Codec<Integer> HEX_COLOR = Codec.STRING.comapFlatMap(AttachmentLight::parseColor,
            color -> String.format("#%06X", color & 0xFFFFFF));

    public static final Codec<AttachmentLight> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Type.CODEC.optionalFieldOf("type", Type.SPOT).forGetter(AttachmentLight::type),
            Codec.STRING.optionalFieldOf("bone", "light_pos").forGetter(AttachmentLight::bone),
            HEX_COLOR.optionalFieldOf("color", 0xFFFFFF).forGetter(AttachmentLight::color),
            Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("brightness", 1.0F).forGetter(AttachmentLight::brightness),
            Codec.floatRange(0, 256).optionalFieldOf("distance", 24.0F).forGetter(AttachmentLight::distance),
            Codec.floatRange(0, 180).optionalFieldOf("angle", 30.0F).forGetter(AttachmentLight::angle),
            Codec.floatRange(0, 180).optionalFieldOf("falloff", 10.0F).forGetter(AttachmentLight::falloff),
            Codec.floatRange(0, 64).optionalFieldOf("radius", 4.0F).forGetter(AttachmentLight::radius),
            Codec.BOOL.optionalFieldOf("occlusion", false).forGetter(AttachmentLight::occlusion),
            Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("inscattering", 0.0F).forGetter(AttachmentLight::inscattering),
            Codec.BOOL.optionalFieldOf("first_person", true).forGetter(AttachmentLight::firstPerson),
            Codec.BOOL.optionalFieldOf("third_person", true).forGetter(AttachmentLight::thirdPerson),
            Codec.BOOL.optionalFieldOf("ir", false).forGetter(AttachmentLight::ir)
    ).apply(instance, AttachmentLight::new));

    private static DataResult<Integer> parseColor(String text) {
        String hex = text.startsWith("#") ? text.substring(1) : text;
        if (hex.length() != 6) {
            return DataResult.error(() -> "Color must be in #RRGGBB format: " + text);
        }
        try {
            return DataResult.success(Integer.parseInt(hex, 16));
        } catch (NumberFormatException e) {
            return DataResult.error(() -> "Invalid color: " + text);
        }
    }

    public enum Type implements StringRepresentable {
        /** A cone of light, for flashlights. Uses distance, angle and size. */
        SPOT("spot"),
        /** A light that shines in every direction, for glows. Uses radius. */
        POINT("point");

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        private final String name;

        Type(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
