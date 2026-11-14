package com.brawlcore.combat;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-side balance values. Edit in saves/<world>/serverconfig/brawlcore-server.toml */
public final class CombatConfig {
    public enum M1Mode { ALWAYS, EMPTY_HAND, OFF }

    public static final ModConfigSpec SPEC;

    // ---- dash ----
    public static final ModConfigSpec.IntValue DASH_COOLDOWN;
    public static final ModConfigSpec.DoubleValue DASH_FRONT_SPEED;
    public static final ModConfigSpec.DoubleValue DASH_SIDE_SPEED;
    public static final ModConfigSpec.DoubleValue DASH_BACK_SPEED;
    public static final ModConfigSpec.IntValue DASH_IFRAMES_FRONT;
    public static final ModConfigSpec.IntValue DASH_IFRAMES_SIDE;
    public static final ModConfigSpec.IntValue DASH_IFRAMES_BACK;
    public static final ModConfigSpec.IntValue DASH_END_TICKS;
    public static final ModConfigSpec.BooleanValue DASH_AIR;

    // ---- M1 ----
    public static final ModConfigSpec.EnumValue<M1Mode> M1_MODE;
    public static final ModConfigSpec.DoubleValue M1_DAMAGE;
    public static final ModConfigSpec.DoubleValue M1_FINISHER_MULT;
    public static final ModConfigSpec.DoubleValue M1_RANGE;
    public static final ModConfigSpec.IntValue M1_ARC;
    public static final ModConfigSpec.IntValue M1_DELAY;
    public static final ModConfigSpec.IntValue M1_FINISHER_DELAY;
    public static final ModConfigSpec.IntValue M1_COMBO_RESET;
    public static final ModConfigSpec.IntValue M1_HITSTUN;
    public static final ModConfigSpec.DoubleValue M1_FINISHER_KNOCKBACK;
    public static final ModConfigSpec.DoubleValue M1_FINISHER_LAUNCH;

    // ---- block ----
    public static final ModConfigSpec.DoubleValue BLOCK_REDUCTION;
    public static final ModConfigSpec.IntValue GUARD_MAX;
    public static final ModConfigSpec.DoubleValue GUARD_DRAIN_PER_DAMAGE;
    public static final ModConfigSpec.IntValue PERFECT_WINDOW;
    public static final ModConfigSpec.IntValue PERFECT_STAGGER;
    public static final ModConfigSpec.IntValue GUARD_BREAK_STUN;
    public static final ModConfigSpec.IntValue BLOCK_RELEASE_COOLDOWN;
    public static final ModConfigSpec.IntValue GUARD_REGEN_DELAY;
    public static final ModConfigSpec.DoubleValue GUARD_REGEN_PER_TICK;
    public static final ModConfigSpec.BooleanValue BLOCK_SLOWNESS;
    public static final ModConfigSpec.BooleanValue BLOCK_FRONT_ONLY;
    public static final ModConfigSpec.IntValue BLOCK_FRONT_ARC;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("dash");
        DASH_COOLDOWN = b.comment("Dash cooldown in ticks (20 = 1 second).").defineInRange("cooldownTicks", 40, 0, 1200);
        DASH_FRONT_SPEED = b.comment("Forward dash speed (blocks/tick impulse).").defineInRange("frontSpeed", 1.55, 0.1, 5.0);
        DASH_SIDE_SPEED = b.defineInRange("sideSpeed", 1.35, 0.1, 5.0);
        DASH_BACK_SPEED = b.defineInRange("backSpeed", 1.2, 0.1, 5.0);
        DASH_IFRAMES_FRONT = b.comment("Invulnerability ticks granted by each dash direction.").defineInRange("iFramesFront", 3, 0, 40);
        DASH_IFRAMES_SIDE = b.defineInRange("iFramesSide", 8, 0, 40);
        DASH_IFRAMES_BACK = b.defineInRange("iFramesBack", 8, 0, 40);
        DASH_END_TICKS = b.comment("Ticks until dash momentum is cut (gives the snappy Battlegrounds feel).").defineInRange("momentumTicks", 5, 1, 20);
        DASH_AIR = b.comment("Allow dashing while airborne.").define("airDash", true);
        b.pop();

        b.push("m1");
        M1_MODE = b.comment("ALWAYS = left click (when not aiming at a block) is an M1 combo hit.",
                "EMPTY_HAND = only when the main hand is empty. OFF = disable M1 entirely.")
                .defineEnum("mode", M1Mode.ALWAYS);
        M1_DAMAGE = b.comment("Damage of a normal M1 hit (2.0 = one heart).").defineInRange("damage", 3.0, 0.0, 100.0);
        M1_FINISHER_MULT = b.comment("Damage multiplier for the 4th (finisher) hit.").defineInRange("finisherMultiplier", 1.5, 1.0, 10.0);
        M1_RANGE = b.defineInRange("range", 3.6, 1.0, 8.0);
        M1_ARC = b.comment("Width of the hit cone in degrees (acts as light aim-assist).").defineInRange("arcDegrees", 110, 10, 360);
        M1_DELAY = b.comment("Ticks between M1 hits.").defineInRange("hitDelayTicks", 6, 1, 40);
        M1_FINISHER_DELAY = b.comment("Endlag in ticks after the 4th hit.").defineInRange("finisherDelayTicks", 16, 1, 100);
        M1_COMBO_RESET = b.comment("Ticks without swinging before the combo resets.").defineInRange("comboResetTicks", 25, 5, 200);
        M1_HITSTUN = b.comment("Ticks a victim cannot start their own M1 after being hit (lets combos connect).").defineInRange("hitStunTicks", 6, 0, 40);
        M1_FINISHER_KNOCKBACK = b.defineInRange("finisherKnockback", 1.1, 0.0, 5.0);
        M1_FINISHER_LAUNCH = b.defineInRange("finisherLaunch", 0.45, 0.0, 3.0);
        b.pop();

        b.push("block");
        BLOCK_REDUCTION = b.comment("Fraction of damage removed while blocking (0.8 = 80%).").defineInRange("damageReduction", 0.8, 0.0, 1.0);
        GUARD_MAX = b.comment("Guard meter size.").defineInRange("guardMax", 50, 5, 1000);
        GUARD_DRAIN_PER_DAMAGE = b.comment("Guard lost per point of incoming damage.").defineInRange("guardDrainPerDamage", 3.0, 0.0, 50.0);
        PERFECT_WINDOW = b.comment("Ticks after pressing block in which a hit is a perfect block (parry).").defineInRange("perfectWindowTicks", 6, 0, 20);
        PERFECT_STAGGER = b.comment("Ticks the attacker is staggered after being parried.").defineInRange("perfectStaggerTicks", 30, 0, 200);
        GUARD_BREAK_STUN = b.comment("Stun ticks when the guard meter hits zero.").defineInRange("guardBreakStunTicks", 50, 0, 400);
        BLOCK_RELEASE_COOLDOWN = b.comment("Ticks before block can be re-raised after releasing (anti parry-spam).").defineInRange("releaseCooldownTicks", 12, 0, 200);
        GUARD_REGEN_DELAY = b.defineInRange("guardRegenDelayTicks", 40, 0, 400);
        GUARD_REGEN_PER_TICK = b.defineInRange("guardRegenPerTick", 0.5, 0.0, 10.0);
        BLOCK_SLOWNESS = b.comment("Slow the player slightly while blocking.").define("slowWhileBlocking", true);
        BLOCK_FRONT_ONLY = b.comment("If true, block only stops hits from the front.").define("frontOnly", false);
        BLOCK_FRONT_ARC = b.defineInRange("frontArcDegrees", 140, 10, 360);
        b.pop();

        SPEC = b.build();
    }

    /** Safe getter: the client may ask before the server config has loaded. */
    public static M1Mode m1Mode() {
        try {
            return M1_MODE.get();
        } catch (IllegalStateException e) {
            return M1Mode.ALWAYS;
        }
    }

    private CombatConfig() {}
}
