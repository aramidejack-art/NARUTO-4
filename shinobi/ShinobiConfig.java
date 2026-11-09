package com.mayhem.shinobi;

import net.minecraftforge.common.ForgeConfigSpec;

/** Server-side shinobi configuration (serverconfig/mayhem-shinobi.toml). */
public final class ShinobiConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue JUTSU_DAMAGE_SCALE, CHAKRA_REGEN_PER_SEC, STAMINA_REGEN_PER_SEC,
            CHAKRA_CHARGE_PER_SEC, COOLDOWN_SCALE, COST_SCALE, XP_SCALE, MISSION_REWARD_SCALE;
    public static final ForgeConfigSpec.BooleanValue PVP_JUTSU, EVENTS_ENABLED, NPC_SPAWNS, TEMP_WALLS, BEAST_ENCOUNTERS, WANDERERS;
    public static final ForgeConfigSpec.IntValue EVENT_INTERVAL_MINUTES, EVENT_RADIUS, MAX_SUMMONS, WALL_DRAIN_PER_SEC,
            WATER_DRAIN_PER_SEC, MASTERY_COUNT_INTERVAL_TICKS, WANDERER_INTERVAL_SECONDS, WANDERER_CHANCE, WANDERER_MAX;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("balance");
        JUTSU_DAMAGE_SCALE = b.comment("Global multiplier on all jutsu damage").defineInRange("jutsuDamageScale", 1.0, 0.05, 10.0);
        COOLDOWN_SCALE = b.comment("Global multiplier on ability cooldowns").defineInRange("cooldownScale", 1.0, 0.1, 5.0);
        COST_SCALE = b.comment("Global multiplier on chakra/stamina costs").defineInRange("costScale", 1.0, 0.1, 5.0);
        XP_SCALE = b.comment("Multiplier on shinobi mastery gain").defineInRange("masteryScale", 1.0, 0.1, 10.0);
        PVP_JUTSU = b.comment("Jutsu can hurt other players (also needs the server's pvp setting)").define("pvpJutsu", true);
        MASTERY_COUNT_INTERVAL_TICKS = b.comment("Minimum ticks between mastery counts for one ability (anti-AFK)").defineInRange("masteryIntervalTicks", 60, 0, 6000);
        b.pop();
        b.push("resources");
        CHAKRA_REGEN_PER_SEC = b.defineInRange("chakraRegenPerSecond", 3.0, 0.0, 100.0);
        STAMINA_REGEN_PER_SEC = b.defineInRange("staminaRegenPerSecond", 12.0, 0.0, 200.0);
        CHAKRA_CHARGE_PER_SEC = b.comment("Extra chakra per second while charging (C)").defineInRange("chakraChargePerSecond", 28.0, 0.0, 500.0);
        WALL_DRAIN_PER_SEC = b.defineInRange("wallWalkChakraPerSecond", 6, 0, 100);
        WATER_DRAIN_PER_SEC = b.defineInRange("waterWalkChakraPerSecond", 3, 0, 100);
        b.pop();
        b.push("world");
        EVENTS_ENABLED = b.comment("Random world events (ambushes, bounties, storms, festivals)").define("worldEvents", true);
        EVENT_INTERVAL_MINUTES = b.defineInRange("eventIntervalMinutes", 12, 1, 600);
        EVENT_RADIUS = b.defineInRange("eventSpawnRadius", 22, 8, 64);
        NPC_SPAWNS = b.comment("Shinobi NPCs may spawn naturally via the biome modifier").define("npcSpawns", true);
        BEAST_ENCOUNTERS = b.comment("Allow tailed beast encounters").define("beastEncounters", true);
        WANDERERS = b.comment("Strong wandering shinobi (genin to legend) turn up while you travel").define("wanderers", true);
        WANDERER_INTERVAL_SECONDS = b.comment("Seconds between encounter rolls per player").defineInRange("wandererIntervalSeconds", 90, 10, 3600);
        WANDERER_CHANCE = b.comment("Percent chance each roll spawns someone").defineInRange("wandererChancePercent", 35, 0, 100);
        WANDERER_MAX = b.comment("Max wanderers near one player at a time").defineInRange("wandererMaxNearby", 2, 0, 10);
        TEMP_WALLS = b.comment("Earth/stone wall jutsu place temporary blocks").define("temporaryWalls", true);
        MAX_SUMMONS = b.defineInRange("maxSummonsPerPlayer", 4, 0, 20);
        MISSION_REWARD_SCALE = b.defineInRange("missionRewardScale", 1.0, 0.0, 10.0);
        b.pop();
        SPEC = b.build();
    }

    private ShinobiConfig() {}
}
