package com.mayhem.shinobi.ability;

import com.mayhem.combat.MoveType;
import net.minecraft.world.entity.EntityType;

import static com.mayhem.shinobi.ability.Cat.*;

/** Fire, Water, Wind, Lightning and Earth techniques. Each element has distinct shapes, not recolours. */
final class ElementalCatalog {
    private ElementalCatalog() {}
    private static final Category E = Category.ELEMENTAL;

    static void register() {
        fire(); water(); wind(); lightning(); earth();
    }

    private static void fire() {
        reg(b("fire_jet", "Flame Jet", E, "fire", 14, 0, 90, 7, 4, Behaviors.jet(Vis.FIRE, 7, 0.9, 20, 0.35, true),
                "Channel a narrow stream of flame that follows your aim. Roots you unless the Mobile variant is unlocked.", "minecraft:blaze_powder")
                .req(Req.ELEMENT, "fire", 1)
                .variant(v("longer", "Long Jet", "Longer reach, slightly hotter.", 25, 1.2, 1.1, 1.15, 1.35, Flag.CHARGED))
                .variant(v("mobile", "Mobile Jet", "Keep moving while you burn, at reduced damage.", 80, 1.1, 1.0, 0.8, 1.0, Flag.MOBILE)));
        reg(b("fire_ball", "Great Fireball", E, "fire", 22, 0, 120, 26, 7, Behaviors.bolt(Vis.FIRE, 1.2, 40, 0.8, 0.012, 3.2, 0.9, true, true, MoveType.JAB),
                "A heavy fireball that explodes on impact, burning everything caught and scorching the ground.", "minecraft:fire_charge")
                .req(Req.ELEMENT, "fire", 1)
                .variant(v("fan", "Fire Fan", "Three smaller fireballs in a fan.", 30, 1.3, 1.1, 0.8, 1.0, Flag.SPREAD))
                .variant(v("heavy", "Heavy Ball", "Bigger blast radius, slower recovery.", 90, 1.3, 1.3, 1.1, 1.0, Flag.AREA)));
        reg(b("fire_arc", "Flame Arc", E, "fire", 16, 0, 70, 5.5, 5, Behaviors.arc(Vis.FIRE, 5.5, 75, 1.0, 0.7, 0.3, MoveType.SWEEP),
                "A sweeping crescent of fire that shoves enemies back.", "minecraft:flint_and_steel").req(Req.ELEMENT, "fire", 1));
        reg(b("fire_zone", "Burning Ash Field", E, "fire", 28, 0, 200, 14, 3, Behaviors.zone(Vis.FIRE, 3.6, 100, 10, 0.35, 0),
                "Seed the ground with embers; anything standing inside keeps burning.", "minecraft:campfire")
                .req(Req.ELEMENT, "fire", 12).variant(v("wide", "Wide Field", "Larger zone.", 40, 1.3, 1.1, 0.9, 1.0, Flag.AREA)));
        reg(b("fire_phoenix", "Phoenix Flowers", E, "fire", 24, 0, 130, 22, 3, Behaviors.volley(Vis.FIRE, 6, 1.3, 40, 0.15, 0.55, MoveType.CROSS),
                "A volley of small homing embers.", "minecraft:firework_star").req(Req.ELEMENT, "fire", 10));
        reg(b("fire_dragon", "Fire Dragon Missile", E, "fire", 34, 0, 190, 28, 6, Behaviors.bolt(Vis.FIRE, 1.5, 45, 0.9, 0, 2.6, 0.6, true, false, MoveType.CROSS),
                "A long piercing dragon of flame.", "minecraft:dragon_breath").req(Req.ELEMENT, "fire", 22)
                .variant(v("pierce", "Piercing Dragon", "Passes through targets.", 60, 1.2, 1.1, 0.8, 1.0, Flag.PIERCE)));
        reg(b("fire_burst", "Flame Burst", E, "fire", 20, 0, 100, 5, 4, Behaviors.pushBurst(Vis.FIRE, 3.8, 0.9, 0.3, 0.6),
                "An explosive ring of flame to break out of melee.", "minecraft:gunpowder").req(Req.ELEMENT, "fire", 6));
        reg(b("fire_mantle", "Ember Mantle", E, "fire", 24, 0, 450, 0, 0, Utility.empower(Vis.FIRE, 400, 1.15, 0, -1, -1, -1),
                "Wrap yourself in embers: more jutsu damage and speed for a while.", "minecraft:magma_cream").req(Req.ELEMENT, "fire", 14).style(StyleReq.ANY));
    }

    private static void water() {
        reg(b("water_bullet", "Water Bullet", E, "water", 10, 0, 40, 22, 3.5, Behaviors.bolt(Vis.WATER, 2.2, 28, 0.5, 0, 0, 0.35, false, false, MoveType.JAB),
                "A fast pressurised bullet. Cheap and reliable.", "minecraft:heart_of_the_sea").req(Req.ELEMENT, "water", 1)
                .variant(v("burst", "Triple Burst", "Three bullets in a fan.", 30, 1.5, 1.2, 0.9, 1.0, Flag.SPREAD)));
        reg(b("water_wave", "Rising Wave", E, "water", 22, 0, 120, 9, 4, Behaviors.wave(Vis.WATER, 9, 2.2, 0.8, 0.6),
                "A wave that sweeps the ground ahead and lifts enemies off their feet.", "minecraft:prismarine_crystals").req(Req.ELEMENT, "water", 1));
        reg(b("water_dragon", "Water Dragon", E, "water", 30, 0, 170, 26, 6, Behaviors.bolt(Vis.WATER, 1.5, 40, 0.8, 0, 2.4, 0.9, false, false, MoveType.CROSS),
                "A water dragon that detonates into a soaking blast.", "minecraft:nautilus_shell").req(Req.ELEMENT, "water", 18));
        reg(b("water_current", "Defensive Current", E, "water", 20, 0, 200, 4, 0, Utility.shield(Vis.WATER, 80, 0.45, -1, 2.5),
                "A flowing barrier that cuts incoming damage and pushes attackers away.", "minecraft:blue_ice").req(Req.ELEMENT, "water", 4).style(StyleReq.ANY));
        reg(b("water_prison", "Water Prison", E, "water", 26, 0, 220, 12, 2, Behaviors.restrain(Vis.WATER, 12, 60, false, 0.5),
                "Trap a target in a sphere of water.", "minecraft:cauldron").req(Req.ELEMENT, "water", 14)
                .variant(v("area", "Prison Field", "Catches everything near the aim point.", 50, 1.5, 1.3, 0.8, 1.0, Flag.AREA)));
        reg(b("water_sharks", "Hungry Sharks", E, "water", 28, 0, 150, 22, 3, Behaviors.volley(Vis.WATER, 5, 1.2, 50, 0.14, 0.6, MoveType.CROSS),
                "Homing shark-shaped surges.", "minecraft:cod").req(Req.ELEMENT, "water", 12));
        reg(b("water_mist", "Mist Veil", E, "water", 18, 0, 200, 0, 0, Utility.decoy(Vis.WATER, 80),
                "Vanish into mist: drop aggro, gain speed, leave an afterimage.", "minecraft:glass_bottle").req(Req.ELEMENT, "water", 8).style(StyleReq.ANY));
    }

    private static void wind() {
        reg(b("wind_cutter", "Wind Cutter", E, "wind", 12, 0, 50, 18, 3.2, Behaviors.bolt(Vis.WIND, 2.6, 36, 0.35, 0, 0, 0.2, false, false, MoveType.CROSS),
                "A thin razor of air with long range.", "minecraft:feather").req(Req.ELEMENT, "wind", 1)
                .variant(v("pierce", "Piercing Cutter", "Slices through every target in line.", 30, 1.2, 1.2, 0.9, 1.0, Flag.PIERCE)));
        reg(b("wind_burst", "Vacuum Burst", E, "wind", 18, 0, 90, 4.5, 3, Behaviors.pushBurst(Vis.WIND, 4.5, 1.4, 0.35, 0.6),
                "A close-range shockwave that clears space.", "minecraft:phantom_membrane").req(Req.ELEMENT, "wind", 1));
        reg(b("wind_vortex", "Cyclone Vortex", E, "wind", 28, 0, 180, 18, 2, Behaviors.vortex(Vis.WIND, 5, 0.35, 60, 0.3),
                "A spinning current that drags enemies in.", "minecraft:elytra").req(Req.ELEMENT, "wind", 12));
        reg(b("wind_scythe", "Wind Scythe", E, "wind", 24, 0, 110, 7, 5, Behaviors.arc(Vis.WIND, 7, 110, 1.0, 0.6, 0.1, MoveType.SWEEP),
                "A wide horizontal reaping arc.", "minecraft:iron_hoe").req(Req.ELEMENT, "wind", 9));
        reg(b("wind_stream", "Slipstream", E, "wind", 14, 0, 70, 0, 0, Utility.airDash(Vis.WIND, 1.6, 0.5),
                "Ride a gust forward and upward.", "minecraft:white_dye").req(Req.ELEMENT, "wind", 3).style(StyleReq.ANY));
        reg(b("wind_breakthrough", "Great Breakthrough", E, "wind", 30, 0, 150, 16, 6, Behaviors.beam(Vis.WIND, 1.0, 1.1, 1.2, 0),
                "A thundering gale that flattens a line of enemies.", "minecraft:snowball").req(Req.ELEMENT, "wind", 16));
    }

    private static void lightning() {
        reg(b("light_lance", "Piercing Thrust", E, "lightning", 18, 0, 80, 18, 6, Behaviors.beam(Vis.LIGHTNING, 0.7, 1.2, 0.9, 4),
                "A charged lightning thrust that pierces everything in line after a tiny wind-up.", "minecraft:lightning_rod").req(Req.ELEMENT, "lightning", 1)
                .variant(v("charged", "Charged Thrust", "Longer, harder, slower.", 40, 1.4, 1.3, 1.3, 1.3, Flag.CHARGED)));
        reg(b("light_dash", "Lightning Dash", E, "lightning", 16, 0, 70, 9, 5, Behaviors.dashStrike(Vis.LIGHTNING, 9, 1.0, 0.8, true),
                "Blink forward at lightning speed, striking everything on the path.", "minecraft:copper_ingot").req(Req.ELEMENT, "lightning", 1).style(StyleReq.ANY)
                .variant(v("long", "Long Dash", "Cover more ground.", 40, 1.3, 1.2, 0.9, 1.4, Flag.CHARGED)));
        reg(b("light_chain", "Chain Lightning", E, "lightning", 24, 0, 120, 14, 4, Behaviors.chain(Vis.LIGHTNING, 4, 6, 0.8, 12),
                "Arcs between enemies, briefly stunning each.", "minecraft:chain").req(Req.ELEMENT, "lightning", 10));
        reg(b("light_stun", "Static Palm", E, "lightning", 20, 0, 120, 4, 3.5, Behaviors.stunCone(Vis.LIGHTNING, 4, 40, 30, 0.7),
                "A short-range shock that locks enemies in place.", "minecraft:redstone").req(Req.ELEMENT, "lightning", 5));
        reg(b("light_armor", "Chakra Armor", E, "lightning", 30, 0, 400, 0, 0, Utility.empower(Vis.LIGHTNING, 400, 1.2, 1, -1, -1, -1),
                "Lightning around the body: faster and harder-hitting.", "minecraft:iron_chestplate").req(Req.ELEMENT, "lightning", 16).style(StyleReq.ANY));
        reg(b("light_net", "Thunder Net", E, "lightning", 28, 0, 180, 14, 3, Behaviors.zone(Vis.LIGHTNING, 4, 80, 10, 0.4, 1),
                "A crackling field that shocks and slows.", "minecraft:cobweb").req(Req.ELEMENT, "lightning", 14));
    }

    private static void earth() {
        reg(b("earth_spikes", "Earth Spikes", E, "earth", 16, 0, 80, 9, 4, Behaviors.spikeLine(Vis.EARTH, 9, 1.0, 0.9, false),
                "Stone spikes erupt in a line, flinging enemies upward.", "minecraft:pointed_dripstone").req(Req.ELEMENT, "earth", 1));
        reg(b("earth_wall", "Stone Wall", E, "earth", 20, 0, 200, 4, 0, Behaviors.wall(Vis.EARTH, 2, 3, 160),
                "Raise a temporary wall in front of you. Reverts on its own.", "minecraft:stone_bricks").req(Req.ELEMENT, "earth", 1).style(StyleReq.ANY)
                .variant(v("wide", "Wide Wall", "A broader wall.", 30, 1.3, 1.2, 1.0, 1.0, Flag.AREA)));
        reg(b("earth_quake", "Tearing Earth Palm", E, "earth", 24, 0, 130, 5, 6, Behaviors.seismic(Vis.EARTH, 4.5, 1.0, 1.0),
                "Slam the ground to crack it and fling everything near.", "minecraft:iron_pickaxe").req(Req.ELEMENT, "earth", 9));
        reg(b("earth_skin", "Stone Skin", E, "earth", 24, 0, 260, 0, 0, Utility.shield(Vis.EARTH, 160, 0.6, 1, 0),
                "Harden your body for heavy damage reduction.", "minecraft:cobblestone").req(Req.ELEMENT, "earth", 6).style(StyleReq.ANY));
        reg(b("earth_golem", "Mud Golem", Category.SUMMON, "earth", 40, 0, 600, 0, 0,
                Utility.summon(() -> EntityType.IRON_GOLEM, 1, 600, "Mud Golem", Vis.EARTH),
                "Summon a bodyguard golem for thirty seconds.", "minecraft:clay").req(Req.ELEMENT, "earth", 20));
        reg(b("earth_fist", "Boulder Fist", E, "earth", 18, 0, 80, 3.5, 7, Behaviors.dashStrike(Vis.EARTH, 3.5, 1.6, 1.4, false),
                "Close the gap and land a crushing stone-wrapped punch.", "minecraft:dirt").req(Req.ELEMENT, "earth", 4));
    }
}
