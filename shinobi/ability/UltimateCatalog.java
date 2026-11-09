package com.mayhem.shinobi.ability;

import static com.mayhem.shinobi.ability.Cat.*;

/** Ultimates: high cost, long telegraph, big payoff, long cooldown. Use the ultimate slot. */
final class UltimateCatalog {
    private UltimateCatalog() {}
    private static final Category U = Category.ULTIMATE;

    static void register() {
        reg(b("ult_fire", "Great Fire Annihilation", U, "fire", 90, 0, 1800, 24, 20, Behaviors.telegraphBlast(Vis.FIRE, 10, 50, 7.0, false, true, 3),
                "A colossal wave of fire erupts around you after a long wind-up. Leave the ring or burn.", "minecraft:fire_charge")
                .req(Req.ELEMENT, "fire", 35).learn(6).rarity(3).uninterruptible());
        reg(b("ult_water", "Great Tsunami", U, "water", 90, 0, 1800, 20, 18, Behaviors.wave(Vis.WATER, 18, 5, 2.0, 1.1),
                "Summon a towering wave that sweeps the field.", "minecraft:heart_of_the_sea")
                .req(Req.ELEMENT, "water", 35).learn(6).rarity(3).uninterruptible());
        reg(b("ult_wind", "Cyclone Cataclysm", U, "wind", 90, 0, 1800, 24, 16, Behaviors.vortex(Vis.WIND, 9, 0.6, 80, 0.7),
                "A storm that rips everything toward its eye.", "minecraft:elytra")
                .req(Req.ELEMENT, "wind", 35).learn(6).rarity(3).uninterruptible());
        reg(b("ult_lightning", "Thousand Bolts", U, "lightning", 95, 0, 1800, 26, 16, Behaviors.rain(Vis.LIGHTNING, 9, 18, 80, 1.1, true),
                "Call down a barrage of bolts over a wide area.", "minecraft:lightning_rod")
                .req(Req.ELEMENT, "lightning", 35).learn(6).rarity(3).uninterruptible());
        reg(b("ult_earth", "Mountain Crush", U, "earth", 90, 0, 1800, 24, 20, Behaviors.telegraphBlast(Vis.EARTH, 9, 45, 6.5, true, true, 2),
                "The ground heaves and slams down after a clear warning.", "minecraft:deepslate")
                .req(Req.ELEMENT, "earth", 35).learn(6).rarity(3).uninterruptible());
        reg(b("ult_meteor", "Meteor Rain", U, "fire", 110, 0, 2400, 28, 22, Behaviors.rain(Vis.FIRE, 10, 12, 90, 1.4, false),
                "Burning meteors streak over a huge area.", "minecraft:fire_charge")
                .req(Req.ELEMENT, "fire", 45).learn(8).rarity(3).uninterruptible());
    }
}
