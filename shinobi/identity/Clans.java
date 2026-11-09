package com.mayhem.shinobi.identity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Clans {
    public static final Map<String, Clan> ALL = new LinkedHashMap<>();

    private static void add(Clan c) { ALL.put(c.id(), c); }

    static {
        add(new Clan("emberfang", "Emberfang", 2, 5,
                "Fire-eyed duelists who read every move before it lands.",
                "Fire jutsu +15% damage; fire resistance.",
                "Ember Brand: fire jutsu leave targets burning longer and weaken their guard.",
                "Taijutsu: flame-wrapped knuckles ignite enemies on heavy hits.",
                List.of("ember_wheel", "ember_phoenix", "ember_mantle", "ember_dragon", "ember_shuriken", "ember_inferno"),
                "minecraft:blaze_powder"));
        add(new Clan("gentleveil", "Gentleveil", 2, 5,
                "Palm-fighters who strike the chakra network, not the body. Hyūga-style specialists.",
                "Jutsu hits apply Chakra Seal (Weakness) to targets.",
                "Gentle Sight: nearby living things glow while you sneak.",
                "Taijutsu: strikes drain target chakra-equivalent (Slowness).",
                List.of("veil_palm", "veil_trigrams", "veil_rotation", "veil_air_palm", "veil_128", "veil_lions", "veil_byakugan", "veil_vacuum"),
                "minecraft:amethyst_shard"));
        add(new Clan("spiralborn", "Spiralborn", 3, 10,
                "Sealing masters with colossal life-force.",
                "+25% max chakra, +40% chakra regeneration.",
                "Sealing Chains: restraint jutsu last 50% longer.",
                "Taijutsu: spiral-charged strikes knock enemies back further.",
                List.of("spiral_sphere", "spiral_barrage", "spiral_chains", "spiral_shuriken", "spiral_heal", "spiral_burst"),
                "minecraft:nautilus_shell"));
        add(new Clan("shadoweave", "Shadoweave", 1, 1,
                "Tacticians who bind foes with their own shadows.",
                "Defense/control jutsu cooldown -20%.",
                "Umbral Stalk: stay invisible while sneaking in low light.",
                "Taijutsu: strikes from behind deal bonus damage.",
                List.of("weave_bind", "weave_needles", "weave_pull", "weave_strangle", "weave_field", "weave_clone"),
                "minecraft:ink_sac"));
        add(new Clan("stonebrow", "Stonebrow", 0, 1,
                "Mountain-born bruisers who shrug off punishment.",
                "+30% max stamina, incoming damage -10%.",
                "Bedrock Stance: standing still builds knockback resistance.",
                "Taijutsu: heavy hits stagger and slow enemies.",
                List.of("brow_fist", "brow_guard", "brow_quake", "brow_pillars", "brow_armor", "brow_avalanche"),
                "minecraft:stone"));
        add(new Clan("windrider", "Windrider", 0, 1,
                "Wandering couriers who treat the air as a road.",
                "+8% movement speed; movement jutsu cost -30%.",
                "Slipstream: sprint grants a short jump boost.",
                "Taijutsu: aerial strikes reach further.",
                List.of("rider_slip", "rider_cutter", "rider_gale", "rider_tempest", "rider_wall", "rider_storm"),
                "minecraft:feather"));
        add(new Clan("mistbane", "Mistbane", 1, 1,
                "Hidden-mist assassins at home in rain and rivers.",
                "Water jutsu +10% damage; Speed in rain or water.",
                "Veil of Mist: mist abilities hide you from mob targeting.",
                "Taijutsu: wet targets take extra damage.",
                List.of("mist_veil", "mist_blade", "mist_surge", "mist_prison", "mist_shark", "mist_tidal"),
                "minecraft:prismarine_shard"));
        add(new Clan("bonecrest", "Bonecrest", 2, 8,
                "Brutal warriors who grow weapons from their own skeleton.",
                "Taijutsu +15% damage.",
                "Marrow Spikes: heavy hits cause bleeding (poison).",
                "Taijutsu: bone-reinforced combos strike harder at low health.",
                List.of("crest_spikes", "crest_pillars", "crest_barrage", "crest_blade", "crest_armor", "crest_drill"),
                "minecraft:bone"));
        add(new Clan("thunderfang", "Thunderfang", 1, 1,
                "Storm-speed strikers who live one heartbeat ahead.",
                "Lightning jutsu +15% damage; dash cooldown -30%.",
                "Static Lock: lightning hits briefly stun targets.",
                "Taijutsu: dash strikes chain a free follow-up hit.",
                List.of("fang_dash", "fang_chain", "fang_strike", "fang_stun", "fang_armor", "fang_kirin"),
                "minecraft:lightning_rod"));
    }

    public static Clan get(String id) { return ALL.get(id); }

    private Clans() {}
}
