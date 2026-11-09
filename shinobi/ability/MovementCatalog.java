package com.mayhem.shinobi.ability;

import static com.mayhem.shinobi.ability.Cat.*;

/** Traversal techniques: flickers, dashes, glides and toggled chakra-surface walking. */
final class MovementCatalog {
    private MovementCatalog() {}
    private static final Category M = Category.MOVEMENT;

    static void register() {
        reg(b("mv_flicker", "Body Flicker", M, "", 16, 0, 80, 7, 0, Utility.blink(Vis.SHADOW, 7),
                "A short blink forward. Respects walls and ledges.", "minecraft:ender_pearl")
                .variant(v("far", "Far Flicker", "Longer blink.", 40, 1.3, 1.2, 1.0, 1.0, Flag.CHARGED)));
        reg(b("mv_behind", "Flicker Behind", M, "", 20, 0, 140, 9, 6, Utility.behindStrike(Vis.SHADOW, 9, 1.3),
                "Vanish and reappear behind a target with a strike.", "minecraft:ender_eye").level(8));
        reg(b("mv_airdash", "Air Dash", M, "", 12, 0, 70, 0, 0, Utility.airDash(Vis.WIND, 1.5, 0.45),
                "A burst of speed in the direction you look; works mid-air.", "minecraft:rabbit_foot"));
        reg(b("mv_glide", "Leaf Glide", M, "", 10, 0, 160, 0, 0, Utility.glide(Vis.WIND, 100),
                "Rise and drift down slowly.", "minecraft:feather").level(5));
        reg(b("mv_water", "Water Walking", M, "", 0, 0, 20, 0, 0, Utility.toggleFlag("waterWalk", "Water walking"),
                "Toggle: stand on water. Drains chakra over time and ends when you run dry or sneak.", "minecraft:lily_pad").toggle().level(1).style(StyleReq.ANY));
        reg(b("mv_wall", "Wall Climbing", M, "", 0, 0, 20, 0, 0, Utility.toggleFlag("wallWalk", "Wall climbing"),
                "Toggle: cling to and climb walls. Drains chakra; jump to detach.", "minecraft:vine").toggle().level(1).style(StyleReq.ANY));
        reg(b("mv_teleport", "Short Teleport", M, "", 30, 0, 260, 14, 0, Utility.blink(Vis.CHAKRA, 14),
                "A long safe teleport along your line of sight.", "minecraft:chorus_fruit").level(25).rarity(2));
    }
}
