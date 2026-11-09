package com.mayhem.shinobi.identity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Bloodlines {
    public static final Map<String, Bloodline> ALL = new LinkedHashMap<>();

    private static void add(Bloodline b) { ALL.put(b.id(), b); }

    static {
        add(new Bloodline("mirror_eye", "Mirror Eye", 3, 15, 2,
                "A legendary ocular bloodline that perceives and bends intent.",
                "Mirror Awakening", "Incoming damage -15%, cooldowns -15%, perfect-dodge counters hit harder. Drains chakra.", 5,
                List.of("mirror_counter", "mirror_illusion", "mirror_focus", "mirror_inferno"), "minecraft:ender_eye"));
        add(new Bloodline("gentle_sight", "Gentle Sight", 2, 10, 1,
                "A near-omniscient sight that reveals every living thing.",
                "360 Sight", "Reveals living things within 24 blocks (glowing) and boosts chakra-strike reach. Drains chakra.", 3,
                List.of("sight_palm", "sight_trigrams", "sight_airpalm", "sight_rotation"), "minecraft:spyglass"));
        add(new Bloodline("ice_release", "Ice Release", 2, 12, 1,
                "Water and wind fused into killing frost.",
                "Frozen Aura", "Enemies within 6 blocks are slowed; frost armor on hit. Drains chakra.", 4,
                List.of("ice_lance", "ice_mirrors", "ice_prison", "ice_flurry"), "minecraft:blue_ice"));
        add(new Bloodline("wood_release", "Wood Release", 3, 18, 3,
                "A forbidden mix of earth and water that makes forests obey.",
                "Verdant Growth", "Regeneration and +25% chakra regen. Low drain.", 2,
                List.of("wood_roots", "wood_forest", "wood_guardian", "wood_seeds"), "minecraft:oak_sapling"));
        add(new Bloodline("shadow_weave", "Shadow Weave", 1, 8, 1,
                "Shadows answer to the user like extra limbs.",
                "Shadow Veil", "Speed boost; invisible while sneaking. Drains chakra.", 3,
                List.of("shadow_step", "shadow_field", "shadow_needles", "shadow_bind2"), "minecraft:black_dye"));
        add(new Bloodline("lava_release", "Lava Release", 3, 20, 3,
                "Fire and earth melted into living magma.",
                "Molten Skin", "Fire immunity, incoming damage -15%, burning contact aura. Heavy drain.", 7,
                List.of("lava_flow", "lava_burst", "lava_armor", "lava_rain"), "minecraft:magma_block"));
    }

    public static Bloodline get(String id) { return ALL.get(id); }

    private Bloodlines() {}
}
