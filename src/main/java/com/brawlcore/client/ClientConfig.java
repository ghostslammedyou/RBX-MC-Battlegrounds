package com.brawlcore.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Per-player client options: config/brawlcore-client.toml */
public final class ClientConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue DOUBLE_TAP_DASH;
    public static final ModConfigSpec.BooleanValue HOLD_TO_M1;
    public static final ModConfigSpec.BooleanValue SHOW_HUD;
    public static final ModConfigSpec.BooleanValue QUICK_HOST_OPEN_LAN;
    public static final ModConfigSpec.ConfigValue<String> QUICK_WORLD_NAME;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        DOUBLE_TAP_DASH = b.comment("Also dash by double-tapping W/A/S/D (conflicts with double-tap sprint).")
                .define("doubleTapDash", false);
        HOLD_TO_M1 = b.comment("Holding left click keeps swinging M1 (otherwise one swing per click).")
                .define("holdToM1", true);
        SHOW_HUD = b.comment("Show the dash / guard / combo bars under the crosshair.")
                .define("showHud", true);
        QUICK_HOST_OPEN_LAN = b.comment("Quick-host button also opens the new world to LAN automatically.",
                "Turn this off if you prefer to use Essential's own invite menu to open the world.")
                .define("quickHostOpensLan", true);
        QUICK_WORLD_NAME = b.comment("Base name for worlds created by the quick-host button.")
                .define("quickHostWorldName", "Essential World");
        SPEC = b.build();
    }

    private ClientConfig() {}
}
