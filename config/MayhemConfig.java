package com.mayhem.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Server-side (per-world) gameplay configuration. Values live in
 * {@code serverconfig/mayhem-server.toml} inside each world.
 */
public final class MayhemConfig {
    public static final ForgeConfigSpec SPEC;

    // --- Combat ---
    public static final ForgeConfigSpec.IntValue ATTACK_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue COMBO_WINDOW_TICKS;
    public static final ForgeConfigSpec.DoubleValue BASE_REACH;
    public static final ForgeConfigSpec.DoubleValue DAMAGE_SCALE;
    public static final ForgeConfigSpec.DoubleValue KNOCKBACK_SCALE;
    public static final ForgeConfigSpec.IntValue FINISHERS_PER_MASTERY;
    public static final ForgeConfigSpec.DoubleValue BLITZ_RANGE;
    public static final ForgeConfigSpec.BooleanValue HIT_STOP;
    public static final ForgeConfigSpec.BooleanValue PVP_SPECIALS;

    // --- Defensive / movement ---
    public static final ForgeConfigSpec.IntValue DODGE_COOLDOWN;
    public static final ForgeConfigSpec.IntValue DODGE_IFRAMES;
    public static final ForgeConfigSpec.IntValue PARRY_WINDOW;
    public static final ForgeConfigSpec.DoubleValue GUARD_REDUCTION;
    public static final ForgeConfigSpec.IntValue SLAM_COOLDOWN;

    // --- Ultimates ---
    public static final ForgeConfigSpec.BooleanValue ULTIMATES_ENABLED;
    public static final ForgeConfigSpec.DoubleValue ULTIMATE_DAMAGE_SCALE;
    public static final ForgeConfigSpec.BooleanValue ULTIMATES_HIT_PLAYERS;

    // --- Destruction ---
    public static final ForgeConfigSpec.BooleanValue DESTRUCTION_ENABLED;
    public static final ForgeConfigSpec.DoubleValue WEAK_HARDNESS;
    public static final ForgeConfigSpec.DoubleValue SLAM_HARDNESS;
    public static final ForgeConfigSpec.DoubleValue ULTIMATE_HARDNESS;
    public static final ForgeConfigSpec.IntValue MAX_BLOCKS_PER_MOVE;
    public static final ForgeConfigSpec.IntValue MAX_DEBRIS_PER_MOVE;
    public static final ForgeConfigSpec.IntValue CRATER_DEPTH;
    public static final ForgeConfigSpec.DoubleValue DROP_CHANCE;
    public static final ForgeConfigSpec.BooleanValue RESPECT_MOB_GRIEFING;

    // --- Mob levels / behaviour ---
    public static final ForgeConfigSpec.BooleanValue MOB_LEVELS_ENABLED;
    public static final ForgeConfigSpec.IntValue MOB_MIN_LEVEL;
    public static final ForgeConfigSpec.IntValue MOB_MAX_LEVEL;
    public static final ForgeConfigSpec.DoubleValue HEALTH_PER_LEVEL;
    public static final ForgeConfigSpec.DoubleValue DAMAGE_PER_LEVEL;
    public static final ForgeConfigSpec.BooleanValue LEVEL_PASSIVE_MOBS;
    public static final ForgeConfigSpec.BooleanValue LEVEL_BOSSES;
    public static final ForgeConfigSpec.BooleanValue MOB_LUNGES;

    // --- Feedback ---
    public static final ForgeConfigSpec.BooleanValue SHOW_HIT_INFO;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("combat");
        ATTACK_COOLDOWN_TICKS = b
                .comment("Minimum ticks between strikes (20 ticks = 1 second).")
                .defineInRange("attackCooldownTicks", 8, 4, 40);
        COMBO_WINDOW_TICKS = b
                .comment("Ticks after a landed hit during which the combo continues.")
                .defineInRange("comboWindowTicks", 28, 6, 100);
        BASE_REACH = b
                .comment("Base strike reach in blocks (before mastery bonus).")
                .defineInRange("baseReach", 3.2D, 2.0D, 6.0D);
        DAMAGE_SCALE = b
                .comment("Global multiplier applied to all MAYHEM damage.")
                .defineInRange("damageScale", 1.0D, 0.1D, 10.0D);
        KNOCKBACK_SCALE = b
                .comment("Global multiplier for knockback / launch strength.")
                .defineInRange("knockbackScale", 1.0D, 0.0D, 3.0D);
        FINISHERS_PER_MASTERY = b
                .comment("Heavy finisher hits required for +1 mastery (mastery increases reach, max 30).")
                .defineInRange("finishersPerMastery", 5, 1, 1000);
        BLITZ_RANGE = b
                .comment("Max distance (blocks) the lock-on gap-closing flash step can cover when you strike.")
                .defineInRange("blitzRange", 9.0D, 0.0D, 24.0D);
        HIT_STOP = b
                .comment("Briefly freeze targets on heavy hits (anime-style hit stop).")
                .define("hitStop", true);
        PVP_SPECIALS = b
                .comment("Allow flash step strikes, launches and spikes against other players.")
                .define("pvpSpecials", false);
        b.pop();

        b.push("defense");
        DODGE_COOLDOWN = b.defineInRange("dodgeCooldownTicks", 24, 6, 200);
        DODGE_IFRAMES = b
                .comment("Ticks of invulnerability after a dodge / flash step.")
                .defineInRange("dodgeInvulnerabilityTicks", 8, 2, 30);
        PARRY_WINDOW = b
                .comment("Ticks after pressing guard in which an incoming hit is a perfect parry.")
                .defineInRange("parryWindowTicks", 8, 2, 20);
        GUARD_REDUCTION = b
                .comment("Fraction of frontal damage blocked while guarding (non-parry).")
                .defineInRange("guardReduction", 0.7D, 0.0D, 1.0D);
        SLAM_COOLDOWN = b.defineInRange("slamCooldownTicks", 80, 20, 600);
        b.pop();

        b.push("ultimates");
        ULTIMATES_ENABLED = b
                .comment("Enable the full-meter ultimates (Barrage and Meteor).")
                .define("enabled", true);
        ULTIMATE_DAMAGE_SCALE = b
                .comment("Extra multiplier applied only to ultimate damage.")
                .defineInRange("damageScale", 1.0D, 0.1D, 10.0D);
        ULTIMATES_HIT_PLAYERS = b
                .comment("Allow ultimates to target and damage other players.")
                .define("hitPlayers", false);
        b.pop();

        b.push("destruction");
        DESTRUCTION_ENABLED = b
                .comment("Powerful moves can shatter blocks, carve craters and throw debris.")
                .define("enabled", true);
        WEAK_HARDNESS = b
                .comment("Max block hardness broken by finishers and dash strikes (dirt 0.5, glass 0.3, planks 2.0).")
                .defineInRange("weakHardness", 0.8D, 0.0D, 50.0D);
        SLAM_HARDNESS = b
                .comment("Max block hardness broken by ground slams and spikes (stone 1.5, ores 3.0).")
                .defineInRange("slamHardness", 2.5D, 0.0D, 50.0D);
        ULTIMATE_HARDNESS = b
                .comment("Max block hardness broken by ultimates (obsidian is 50).")
                .defineInRange("ultimateHardness", 3.5D, 0.0D, 50.0D);
        MAX_BLOCKS_PER_MOVE = b
                .comment("Hard cap on blocks removed by a single move (keeps servers smooth).")
                .defineInRange("maxBlocksPerMove", 90, 1, 1000);
        MAX_DEBRIS_PER_MOVE = b
                .comment("Max flying debris entities spawned per move.")
                .defineInRange("maxDebrisPerMove", 40, 0, 200);
        CRATER_DEPTH = b
                .comment("Depth of ground slam craters (ultimates dig deeper).")
                .defineInRange("craterDepth", 2, 1, 6);
        DROP_CHANCE = b
                .comment("Chance that a destroyed block drops its item.")
                .defineInRange("dropChance", 0.08D, 0.0D, 1.0D);
        RESPECT_MOB_GRIEFING = b
                .comment("Destruction is disabled when the mobGriefing gamerule is false.")
                .define("respectMobGriefing", true);
        b.pop();

        b.push("mobs");
        MOB_LEVELS_ENABLED = b
                .comment("Roll a persistent level for mobs when they first spawn.")
                .define("levelsEnabled", true);
        MOB_MIN_LEVEL = b.defineInRange("minLevel", 1, 1, 100);
        MOB_MAX_LEVEL = b.defineInRange("maxLevel", 5, 1, 100);
        HEALTH_PER_LEVEL = b
                .comment("Fractional max-health increase per level above 1.")
                .defineInRange("healthPerLevel", 0.10D, 0.0D, 2.0D);
        DAMAGE_PER_LEVEL = b
                .comment("Fractional attack-damage increase per level above 1.")
                .defineInRange("damagePerLevel", 0.06D, 0.0D, 2.0D);
        LEVEL_PASSIVE_MOBS = b.define("levelPassiveMobs", false);
        LEVEL_BOSSES = b.define("levelBosses", false);
        MOB_LUNGES = b
                .comment("Level 2+ hostile mobs telegraph and perform lunge attacks you can dodge or parry.")
                .define("telegraphedLunges", true);
        b.pop();

        b.push("feedback");
        SHOW_HIT_INFO = b
                .comment("Show move name, combo count and target level/health on the action bar.")
                .define("showHitInfo", true);
        b.pop();

        SPEC = b.build();
    }

    private MayhemConfig() {}
}
