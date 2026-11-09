package com.mayhem.shinobi.ability;

import com.mayhem.combat.MoveType;
import net.minecraft.world.entity.EntityType;

import static com.mayhem.shinobi.ability.Cat.*;

/** Medical, defence/control, weapon and summon techniques. */
final class SupportCatalog {
    private SupportCatalog() {}

    static void register() {
        reg(b("med_pulse", "Medical Chakra Pulse", Category.MEDICAL, "", 30, 0, 300, 6, 0, Utility.heal(Vis.CHAKRA, 0.07, 6, false, 5),
                "Heal yourself and nearby allies over five pulses.", "minecraft:glistering_melon_slice").level(6)
                .variant(v("wide", "Field Pulse", "Wider healing radius.", 40, 1.4, 1.2, 1.0, 1.0, Flag.AREA)));
        reg(b("med_cleanse", "Purge Chakra", Category.MEDICAL, "", 22, 0, 240, 5, 0, Utility.heal(Vis.CHAKRA, 0.02, 5, true, 2),
                "Cleanse poisons and curses from you and nearby allies.", "minecraft:milk_bucket").level(8));
        reg(b("med_scalpel", "Chakra Scalpel", Category.MEDICAL, "", 20, 0, 110, 3, 5, Utility.palm(Vis.CHAKRA, 3, 1.4, 60, 0),
                "A precise short-range cut that weakens the target.", "minecraft:shears").level(7).style(StyleReq.NIN));
        reg(b("med_siphon", "Chakra Siphon", Category.MEDICAL, "", 8, 0, 200, 5, 4, Utility.siphon(Vis.CHAKRA, 5, 0.9, 22),
                "Strike a target and steal chakra from it.", "minecraft:amethyst_cluster").level(12));
        reg(b("def_decoy", "Clone Feint", Category.DEFENSE, "", 20, 0, 220, 0, 0, Utility.decoy(Vis.SHADOW, 80),
                "Leave an afterimage and slip away: aggro drops, speed rises.", "minecraft:ender_pearl").level(4));
        reg(b("def_genjutsu", "Illusion Pulse", Category.DEFENSE, "", 28, 0, 280, 6, 0, Utility.genjutsu(Vis.SHADOW, 6, 100),
                "Blind and confuse everything nearby and break mob aggro.", "minecraft:fermented_spider_eye").level(14).style(StyleReq.NIN));
        reg(b("def_sense", "Sensory Focus", Category.DEFENSE, "", 18, 0, 400, 24, 0, Utility.sense(Vis.CHAKRA, 24, 200),
                "Reveal living things within 24 blocks and see in the dark.", "minecraft:spyglass").level(3));
        reg(b("def_barrier", "Chakra Barrier", Category.DEFENSE, "", 24, 0, 260, 3, 0, Utility.shield(Vis.CHAKRA, 60, 0.7, -1, 2),
                "A short, strong barrier that repels attackers.", "minecraft:glass").level(7));
        reg(b("def_chains", "Chakra Chains", Category.DEFENSE, "", 26, 0, 220, 10, 2, Behaviors.restrain(Vis.CHAKRA, 10, 60, false, 0.2),
                "Bind a target with glowing chains.", "minecraft:lead").level(11));
        reg(b("wpn_kunai", "Kunai Fan", Category.WEAPON, "", 0, 12, 65, 20, 3, Behaviors.volley(Vis.BODY, 5, 1.6, 35, 0.0, 0.5, MoveType.JAB),
                "Throw a fan of kunai.", "minecraft:iron_nugget").level(2));
        reg(b("wpn_shuriken", "Shuriken Storm", Category.WEAPON, "", 0, 20, 120, 22, 3, Behaviors.volley(Vis.WIND, 9, 1.8, 25, 0.08, 0.4, MoveType.CROSS),
                "A flurry of homing shuriken.", "minecraft:iron_ingot").level(8));
        reg(b("wpn_tags", "Explosive Tag Volley", Category.WEAPON, "fire", 14, 8, 130, 9, 4, Behaviors.tags(Vis.FIRE, 25, 1.2, 9),
                "Stick tags on every enemy in a cone; they detonate after a short delay.", "minecraft:paper").level(9));
        reg(b("wpn_blade", "Blade Slash", Category.WEAPON, "", 0, 16, 70, 4.2, 6, Behaviors.arc(Vis.BODY, 4.2, 60, 1.5, 0.6, 0.1, MoveType.SWEEP),
                "A wide slash that ignores whatever is in your hand.", "minecraft:iron_sword").level(5));
        reg(b("wpn_hook", "Chain Hook", Category.WEAPON, "", 0, 18, 140, 14, 2, Behaviors.grapple(Vis.BODY, 0.6, 25),
                "Yank a target to you and hold it briefly.", "minecraft:fishing_rod").level(7));
        reg(b("wpn_mine", "Hidden Blast Mine", Category.WEAPON, "fire", 14, 10, 200, 10, 6, Behaviors.trap(Vis.FIRE, 2.2, 3.0, 2.0, 20),
                "Plant a mine that detonates when an enemy steps near.", "minecraft:tnt").level(12));
        reg(b("sum_wolves", "Summon: Hound Pack", Category.SUMMON, "", 32, 0, 500, 0, 0,
                Utility.summon(() -> EntityType.WOLF, 2, 500, "Summoned Hound", Vis.SHADOW),
                "Call two loyal hounds that fight your target for 25 seconds.", "minecraft:bone").level(10));
    }
}
