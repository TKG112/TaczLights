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

    static final ModConfigSpec SPEC = BUILDER.build();

    private TaczLightsConfig() {
    }
}
