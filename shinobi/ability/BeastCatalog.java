package com.mayhem.shinobi.ability;

import com.mayhem.combat.MoveType;

import static com.mayhem.shinobi.ability.Cat.*;

/** Tailed-beast techniques. Shared ones scale with bond stage; each beast has multiple signatures. */
final class BeastCatalog {
    private BeastCatalog() {}
    private static final Category B = Category.BEAST;

    private static AbilityDef.Builder stage(String id, String name, int stage, int chakra, int cd, double range, double dmg, Behavior beh, String desc, String icon) {
        return b(id, name, B, "", chakra, 0, cd, range, dmg, beh, desc, icon).req(Req.BEAST_STAGE, String.valueOf(stage), 1);
    }

    private static AbilityDef.Builder sig(String id, String name, String beastId, int chakra, int cd, double range, double dmg, Behavior beh, String desc, String icon) {
        return b(id, name, B, "", chakra, 0, cd, range, dmg, beh, desc, icon).req(Req.BEAST_ID, beastId, 1).rarity(2);
    }

    static void register() {
        // Shared stage moves
        reg(stage("beast_claw", "Beast Claw Rush", 2, 26, 110, 6, 8, Behaviors.dashStrike(Vis.BEAST, 6, 1.6, 1.3, false),
                "Lunge in a rush of beast chakra.", "minecraft:iron_axe"));
        reg(stage("beast_sweep", "Chakra Tail Sweep", 3, 32, 150, 6, 9, Behaviors.arc(Vis.BEAST, 6, 180, 1.5, 1.5, 0.4, MoveType.SWEEP),
                "A tail of chakra sweeps all around you.", "minecraft:lead"));
        reg(stage("beast_roar", "Beast Roar", 3, 34, 190, 8, 4, Behaviors.pushBurst(Vis.BEAST, 8, 1.8, 0.6, 0.8),
                "A roar that blasts everything away.", "minecraft:goat_horn"));
        reg(stage("beast_blast", "Chakra Blast", 3, 40, 200, 28, 11, Behaviors.beam(Vis.BEAST, 1.4, 2.6, 1.6, 10),
                "A thick beam of chakra after a short charge.", "minecraft:end_crystal"));
        reg(stage("beast_stomp", "Avatar Stomp", 5, 60, 360, 8, 14, Behaviors.seismic(Vis.BEAST, 8, 2.2, 1.6),
                "A giant footfall that shatters the ground.", "minecraft:netherite_boots").rarity(3));
        reg(stage("beast_bomb", "Tailed Beast Bomb", 4, 100, 1500, 30, 22, Behaviors.telegraphBlast(Vis.BEAST, 9, 40, 6.0, true, true, 2),
                "Charge and loose a concentrated sphere of chakra. Huge blast, huge telegraph, huge cost.", "minecraft:dragon_egg").rarity(3).uninterruptible());

        // Shukaku (sand_tanuki)
        reg(sig("sig_sand", "Sand Coffin", "sand_tanuki", 30, 220, 14, 4, Behaviors.restrain(Vis.EARTH, 14, 90, true, 1.0), "Crush enemies in a shell of sand.", "minecraft:sand"));
        reg(sig("sig_sand_bullet", "Sand Bullets", "sand_tanuki", 22, 120, 16, 4, Behaviors.volley(Vis.EARTH, 8, 1.5, 20, 0.08, 0.4, MoveType.JAB), "Hardened sand shots.", "minecraft:sandstone"));
        reg(sig("sig_sand_shield", "Sand Shield", "sand_tanuki", 26, 200, 0, 0, Utility.shield(Vis.EARTH, 120, 0.5, 1, 3), "Automatic sand defense.", "minecraft:shield"));
        reg(sig("sig_desert_wave", "Desert Wave", "sand_tanuki", 36, 240, 10, 6, Behaviors.arc(Vis.EARTH, 7, 160, 1.3, 1.2, 0.5, MoveType.SWEEP), "A tidal wave of sand.", "minecraft:sand"));

        // Matatabi (blue_flame_cat)
        reg(sig("sig_flame", "Blue Flame Breath", "blue_flame_cat", 34, 200, 9, 5, Behaviors.jet(Vis.BEAST, 9, 1.4, 30, 0.5, true), "Breathe a stream of blue fire.", "minecraft:soul_torch"));
        reg(sig("sig_cat_claw", "Cat Claw Rush", "blue_flame_cat", 28, 140, 5, 8, Behaviors.dashStrike(Vis.BEAST, 5, 1.7, 1.3, true), "Blazing claw lunge.", "minecraft:soul_lantern"));
        reg(sig("sig_blue_fireball", "Blue Fireball", "blue_flame_cat", 30, 160, 18, 7, Behaviors.bolt(Vis.BEAST, 1.3, 32, 0.6, 0.02, 2.8, 0.8, true, false, MoveType.JAB), "A dense blue fireball.", "minecraft:fire_charge"));
        reg(sig("sig_flame_cloak", "Blue Flame Cloak", "blue_flame_cat", 32, 280, 0, 0, Utility.empower(Vis.BEAST, 240, 1.2, 1, 0, -1, -1), "Wreathe yourself in Matatabi fire.", "minecraft:soul_campfire"));

        // Isobu (giant_turtle)
        reg(sig("sig_shell", "Shell Spikes", "giant_turtle", 32, 220, 6, 6, Behaviors.spikeLine(Vis.WATER, 6, 1.2, 1.0, true), "Spines burst from the ground around you.", "minecraft:turtle_scute"));
        reg(sig("sig_coral", "Coral Prison", "giant_turtle", 30, 200, 8, 2, Behaviors.restrain(Vis.WATER, 8, 100, true, 0.4), "Grow coral to bind a target.", "minecraft:brain_coral"));
        reg(sig("sig_tidal_armor", "Tidal Armor", "giant_turtle", 28, 220, 0, 0, Utility.shield(Vis.WATER, 140, 0.55, 1, 0), "Hard shell of water and coral.", "minecraft:turtle_helmet"));
        reg(sig("sig_miasma", "Water Miasma", "giant_turtle", 34, 180, 7, 4, Behaviors.zone(Vis.WATER, 5, 100, 8, 0.4, 1), "A choking mist of sea water.", "minecraft:ink_sac"));

        // Son Goku (lava_ape)
        reg(sig("sig_magma", "Magma Eruption", "lava_ape", 44, 300, 20, 8, Behaviors.telegraphBlast(Vis.LAVA, 6, 24, 4.0, true, true, 1), "Ground erupts after a short warning.", "minecraft:magma_block"));
        reg(sig("sig_lava_fist", "Lava Fist", "lava_ape", 32, 160, 4, 10, Behaviors.dashStrike(Vis.LAVA, 4, 1.9, 1.5, true), "Molten haymaker.", "minecraft:magma_cream"));
        reg(sig("sig_lava_spit", "Lava Spit", "lava_ape", 28, 140, 16, 6, Behaviors.bolt(Vis.LAVA, 1.4, 30, 0.7, 0.03, 2.6, 0.9, true, true, MoveType.JAB), "Lobbed magma that burns the ground.", "minecraft:lava_bucket"));
        reg(sig("sig_scorch", "Scorching Eruption", "lava_ape", 40, 260, 8, 8, Behaviors.zone(Vis.LAVA, 5, 90, 14, 0.7, 1), "Area becomes a magma hellscape.", "minecraft:magma_block"));

        // Kokuo (horse_dolphin)
        reg(sig("sig_steam", "Steam Dash", "horse_dolphin", 28, 120, 11, 7, Behaviors.dashStrike(Vis.WATER, 11, 1.4, 1.0, true), "A scalding rush that hits everything on the path.", "minecraft:cauldron"));
        reg(sig("sig_boil", "Boil Release Mist", "horse_dolphin", 32, 180, 7, 5, Behaviors.zone(Vis.WATER, 5, 100, 10, 0.5, 1), "Superheated mist that cooks foes.", "minecraft:potion"));
        reg(sig("sig_horn", "Horn Charge", "horse_dolphin", 30, 150, 8, 9, Behaviors.dashStrike(Vis.WATER, 8, 1.8, 1.4, false), "A brutal horned tackle.", "minecraft:goat_horn"));
        reg(sig("sig_steam_burst", "Steam Burst", "horse_dolphin", 26, 130, 5, 6, Behaviors.pushBurst(Vis.WATER, 5, 1.5, 0.7, 0.9), "Explosive steam knockback.", "minecraft:water_bucket"));

        // Saiken (six_slug)
        reg(sig("sig_acid", "Corrosive Spit", "six_slug", 26, 150, 22, 6, Behaviors.bolt(Vis.WOOD, 1.8, 36, 0.7, 0.02, 2.4, 0.5, false, false, MoveType.JAB), "Acidic globs that splash.", "minecraft:slime_ball"));
        reg(sig("sig_acid_mist", "Acid Mist", "six_slug", 30, 180, 6, 4, Behaviors.zone(Vis.WOOD, 5, 110, 8, 0.4, 1), "Corrosive fog.", "minecraft:poisonous_potato"));
        reg(sig("sig_bubble", "Bubble Prison", "six_slug", 28, 200, 7, 2, Behaviors.restrain(Vis.WATER, 7, 90, true, 0.3), "Trap in a sticky acid bubble.", "minecraft:slime_block"));
        reg(sig("sig_acid_wave", "Acid Wave", "six_slug", 34, 200, 9, 5, Behaviors.arc(Vis.WOOD, 6, 160, 1.2, 1.0, 0.4, MoveType.SWEEP), "A sweeping wave of acid.", "minecraft:slime_ball"));

        // Chomei (horned_beetle)
        reg(sig("sig_scale", "Scale Powder", "horned_beetle", 30, 240, 7, 0, Utility.genjutsu(Vis.WIND, 7, 120), "A glittering powder that blinds and confuses.", "minecraft:glowstone_dust"));
        reg(sig("sig_flight", "Flight Burst", "horned_beetle", 22, 100, 10, 0, Behaviors.dashStrike(Vis.WIND, 10, 0.9, 0.4, false), "A winged dash through the air.", "minecraft:elytra"));
        reg(sig("sig_wing", "Wing Slash", "horned_beetle", 28, 140, 6, 7, Behaviors.arc(Vis.WIND, 5, 140, 1.3, 1.1, 0.4, MoveType.SWEEP), "Cutting wing strikes.", "minecraft:feather"));
        reg(sig("sig_powder_storm", "Powder Storm", "horned_beetle", 36, 220, 10, 3, Behaviors.zone(Vis.WIND, 6, 100, 6, 0.3, 2), "Blinding scale storm.", "minecraft:glowstone"));

        // Gyuki (ox_octopus) — ink + tentacles
        reg(sig("sig_ink", "Ink Twister", "ox_octopus", 36, 220, 16, 3, Behaviors.vortex(Vis.SHADOW, 6, 0.5, 60, 0.5), "A whirling ink tornado.", "minecraft:ink_sac"));
        reg(sig("sig_ink_clone", "Ink Clone", "ox_octopus", 24, 160, 0, 0, Utility.empower(Vis.SHADOW, 180, 1.05, 0, 0, -1, -1), "Fight with ink-double support.", "minecraft:black_dye"));
        reg(sig("sig_tentacle_lash", "Tentacle Lash", "ox_octopus", 28, 140, 7, 7, Behaviors.arc(Vis.SHADOW, 6, 160, 1.4, 1.2, 0.5, MoveType.SWEEP), "Tentacles strike in a wide arc.", "minecraft:ink_sac"));
        reg(sig("sig_tentacle_storm", "Tentacle Storm", "ox_octopus", 40, 240, 8, 6, Behaviors.zone(Vis.SHADOW, 6, 100, 12, 0.6, 1), "Tentacles erupt from everywhere.", "minecraft:black_concrete"));
        reg(sig("sig_ink_needles", "Ink Needle Barrage", "ox_octopus", 26, 130, 15, 4, Behaviors.volley(Vis.SHADOW, 10, 1.7, 18, 0.07, 0.35, MoveType.JAB), "Hardened ink needles.", "minecraft:arrow"));
        reg(sig("sig_ink_rasen", "Ink Rasenshuriken", "ox_octopus", 55, 320, 16, 10, Behaviors.bolt(Vis.SHADOW, 1.3, 34, 0.8, 0.02, 3.2, 1.1, false, true, MoveType.CROSS), "A massive spinning sphere of ink chakra.", "minecraft:nether_star").rarity(3));
        reg(sig("sig_ink_bind", "Ink Binding", "ox_octopus", 30, 180, 10, 2, Behaviors.restrain(Vis.SHADOW, 10, 90, true, 0.4), "Ink solidifies into binding seals.", "minecraft:lead"));

        // Kurama (nine_fox) — chakra arms / shape-shift
        reg(sig("sig_nine", "Fox Fire Barrage", "nine_fox", 46, 260, 22, 4, Behaviors.rain(Vis.BEAST, 8, 12, 60, 0.9, false), "Rain of chakra flames.", "minecraft:fire_charge"));
        reg(sig("sig_chakra_arm", "Chakra Arm", "nine_fox", 28, 130, 7, 8, Behaviors.dashStrike(Vis.BEAST, 7, 1.6, 1.3, false), "A long chakra arm grabs and smashes.", "minecraft:blaze_rod"));
        reg(sig("sig_giant_fist", "Giant Chakra Fist", "nine_fox", 36, 180, 8, 11, Behaviors.dashStrike(Vis.BEAST, 8, 2.0, 1.6, false), "An oversized fist of pure chakra.", "minecraft:netherite_ingot"));
        reg(sig("sig_back_arms", "Back Arm Barrage", "nine_fox", 34, 200, 10, 5, Behaviors.volley(Vis.BEAST, 6, 1.5, 20, 0.1, 0.5, MoveType.JAB), "Arms erupt from the back to strike.", "minecraft:blaze_powder"));
        reg(sig("sig_massive_rasengan", "Massive Rasengan", "nine_fox", 48, 280, 6, 14, Behaviors.dashStrike(Vis.BEAST, 6, 2.2, 1.7, false), "A colossal spiral sphere.", "minecraft:end_crystal").rarity(3));
        reg(sig("sig_mini_shuriken", "Mini Rasenshuriken", "nine_fox", 32, 160, 18, 6, Behaviors.bolt(Vis.BEAST, 1.8, 30, 0.5, 0.02, 2.4, 0.7, false, false, MoveType.JAB), "Small, fast spiral shuriken.", "minecraft:snowball"));
        reg(sig("sig_kurama_roar", "Kurama Roar", "nine_fox", 38, 220, 10, 5, Behaviors.pushBurst(Vis.BEAST, 10, 2.0, 0.8, 1.0), "A fox roar that flattens the field.", "minecraft:goat_horn"));
    }
}
