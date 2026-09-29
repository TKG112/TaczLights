package net.tkg.taczlights;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class TaczLightsConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Whether attachments defined by gunpacks emit Veil lights")
            .define("enabled", true);

    public static final ModConfigSpec.IntValue MAX_LIGHTS = BUILDER
            .comment("Maximum number of attachment lights active at once. The closest ones to the camera are kept")
            .defineInRange("maxLights", 16, 0, 256);

    public static final ModConfigSpec.BooleanValue MUZZLE_FLASHES = BUILDER
            .comment("Whether guns briefly light up their surroundings when fired")
            .define("muzzleFlashes", true);

    public static final ModConfigSpec.IntValue MAX_MUZZLE_FLASHES = BUILDER
            .comment("Maximum number of muzzle flash lights active at once. The closest ones to the camera are kept")
            .defineInRange("maxMuzzleFlashes", 16, 0, 256);

    public static final ModConfigSpec.BooleanValue LASER_POINTER_LIGHTS = BUILDER
            .comment("Whether every laser casts a dot of light where it hits. Lasers whose gunpack sets a \"laser_light\" always use their own")
            .define("laserPointerLights", true);

    public static final ModConfigSpec.BooleanValue LASER_BLOOM = BUILDER
            .comment("Whether visible TaCZ laser beams glow, through Veil's bloom")
            .define("laserBloom", true);

    public static final ModConfigSpec.DoubleValue LASER_BLOOM_STRENGTH = BUILDER
            .comment("How strongly laser beams glow. Can be changed while the game is running")
            .defineInRange("laserBloomStrength", 1.0, 0.0, 16.0);

    public static final ModConfigSpec.DoubleValue LASER_BLOOM_WIDTH = BUILDER
            .comment("How wide the glowing part of a laser beam is, relative to the beam itself. Wider spreads the glow further")
            .defineInRange("laserBloomWidth", 1.5, 0.1, 16.0);

    static final ModConfigSpec SPEC = BUILDER.build();

    private TaczLightsConfig() {
    }
}
