package com.mayhem.shinobi.ability;

import com.mayhem.combat.MoveType;
import net.minecraft.world.entity.EntityType;

import static com.mayhem.shinobi.ability.Cat.*;

/** Clan techniques and bloodline movesets. Ids must match Clans / Bloodlines. */
final class IdentityCatalog {
    private IdentityCatalog() {}

    private static AbilityDef.Builder clan(String id, String name, String clanId, int level, String el, int chakra, int stam,
                                           int cd, double range, double dmg, Behavior beh, String desc, String icon) {
        return b(id, name, Category.CLAN, el, chakra, stam, cd, range, dmg, beh, desc, icon).req(Req.CLAN, clanId, level);
    }

    private static AbilityDef.Builder bl(String id, String name, String bloodline, int level, String el, int chakra, int cd,
                                         double range, double dmg, Behavior beh, String desc, String icon) {
        return b(id, name, Category.BLOODLINE, el, chakra, 0, cd, range, dmg, beh, desc, icon).req(Req.BLOODLINE, bloodline, level).style(StyleReq.ANY);
    }

    static void register() {
        clans();
        bloodlines();
    }

    private static void clans() {
        // ========== Emberfang (Uchiha-like fire) ==========
        reg(clan("ember_wheel", "Crimson Flame Wheel", "emberfang", 5, "fire", 26, 0, 130, 4, 6, Behaviors.arc(Vis.FIRE, 4, 180, 1.2, 1.0, 0.3, MoveType.ROUNDHOUSE),
                "A spinning ring of fire around you.", "minecraft:fire_charge"));
        reg(clan("ember_phoenix", "Ember Phoenix", "emberfang", 8, "fire", 30, 0, 160, 24, 3, Behaviors.volley(Vis.FIRE, 7, 1.3, 50, 0.15, 0.5, MoveType.CROSS),
                "Seven homing embers.", "minecraft:firework_star"));
        reg(clan("ember_mantle", "Brand of Embers", "emberfang", 12, "fire", 30, 0, 500, 0, 0, Utility.empower(Vis.FIRE, 500, 1.2, 0, 0, -1, -1),
                "Burn with clan flame: strength and speed for 25 seconds.", "minecraft:blaze_rod").style(StyleReq.ANY));
        reg(clan("ember_dragon", "Fire Dragon Flame", "emberfang", 16, "fire", 40, 0, 200, 18, 5, Behaviors.jet(Vis.FIRE, 12, 1.4, 40, 0.5, true),
                "A roaring dragon of flame that scorches a long path.", "minecraft:dragon_breath"));
        reg(clan("ember_shuriken", "Flame Shuriken", "emberfang", 10, "fire", 22, 0, 100, 16, 4, Behaviors.bolt(Vis.FIRE, 1.6, 30, 0.5, 0.02, 2.2, 0.6, true, false, MoveType.JAB),
                "A burning shuriken that sticks and ignites.", "minecraft:fire_charge"));
        reg(clan("ember_inferno", "Great Inferno", "emberfang", 22, "fire", 48, 0, 280, 10, 8, Behaviors.zone(Vis.FIRE, 5, 100, 12, 0.6, 1),
                "A wide field of clan fire that keeps burning.", "minecraft:lava_bucket").rarity(2));

        // ========== Gentleveil (Hyūga) ==========
        reg(clan("veil_palm", "Gentle Palm", "gentleveil", 5, "", 18, 0, 90, 3.2, 6, Utility.palm(Vis.CHAKRA, 3.2, 1.2, 100, 25),
                "Strike the chakra network: weakens and drains.", "minecraft:amethyst_shard"));
        reg(clan("veil_trigrams", "Sixty-Four Palms", "gentleveil", 10, "", 32, 0, 220, 12, 5, Behaviors.clones(Vis.CHAKRA, 8, 1.8),
                "A rapid chain of precise strikes from every angle.", "minecraft:end_rod"));
        reg(clan("veil_rotation", "Heavenly Rotation", "gentleveil", 8, "", 28, 0, 260, 4, 0, Utility.shield(Vis.CHAKRA, 30, 0.8, -1, 3.5),
                "A spinning dome that repels everything.", "minecraft:heart_of_the_sea").style(StyleReq.ANY));
        reg(clan("veil_air_palm", "Air Palm", "gentleveil", 12, "", 24, 0, 110, 14, 5, Behaviors.bolt(Vis.CHAKRA, 2.0, 28, 0.4, 0.01, 2.5, 0.8, false, false, MoveType.JAB),
                "A vacuum palm blast that hits from range.", "minecraft:glass"));
        reg(clan("veil_128", "One Hundred Twenty-Eight Palms", "gentleveil", 18, "", 42, 0, 280, 14, 6, Behaviors.clones(Vis.CHAKRA, 12, 2.0),
                "An overwhelming storm of gentle-fist strikes.", "minecraft:nether_star").rarity(2));
        reg(clan("veil_lions", "Twin Lion Fists", "gentleveil", 20, "", 38, 0, 240, 4.5, 9, Behaviors.dashStrike(Vis.CHAKRA, 4.5, 1.9, 1.5, false),
                "Chakra lion heads form on both fists for a crushing close assault.", "minecraft:golden_sword").rarity(2));
        reg(clan("veil_byakugan", "Byakugan Focus", "gentleveil", 6, "", 16, 0, 300, 28, 0, Utility.sense(Vis.CHAKRA, 28, 240),
                "Near-360 vision: living things glow; you move slightly faster.", "minecraft:ender_eye").style(StyleReq.ANY));
        reg(clan("veil_vacuum", "Vacuum Palm Barrage", "gentleveil", 15, "", 34, 0, 180, 12, 4, Behaviors.volley(Vis.CHAKRA, 5, 1.5, 24, 0.08, 0.4, MoveType.JAB),
                "Several air-palm shots in a tight fan.", "minecraft:phantom_membrane"));

        // ========== Spiralborn (Uzumaki) ==========
        reg(clan("spiral_sphere", "Spiral Sphere", "spiralborn", 10, "", 30, 0, 145, 5, 9, Behaviors.dashStrike(Vis.CHAKRA, 5, 1.8, 1.4, false),
                "Drive a spinning chakra sphere through the target.", "minecraft:nautilus_shell"));
        reg(clan("spiral_barrage", "Spiral Barrage", "spiralborn", 20, "", 40, 0, 220, 12, 8, Behaviors.clones(Vis.CHAKRA, 5, 2.2),
                "Several spheres from afterimages at once.", "minecraft:sea_lantern").rarity(2));
        reg(clan("spiral_chains", "Sealing Chains", "spiralborn", 14, "", 30, 0, 260, 14, 2, Behaviors.restrain(Vis.CHAKRA, 14, 80, true, 0.3),
                "Chains erupt around the aim point and bind everything inside.", "minecraft:chain").style(StyleReq.ANY));
        reg(clan("spiral_shuriken", "Spiral Shuriken", "spiralborn", 22, "", 48, 0, 260, 18, 7, Behaviors.bolt(Vis.CHAKRA, 1.4, 36, 0.7, 0.02, 3.0, 1.0, false, true, MoveType.CROSS),
                "A massive spinning sphere thrown like a shuriken.", "minecraft:trident").rarity(2));
        reg(clan("spiral_heal", "Adamantine Healing", "spiralborn", 8, "", 28, 0, 200, 0, 0, Utility.heal(Vis.CHAKRA, 0.12, 0, true, 4),
                "Rapid self-repair drawn from vast life force.", "minecraft:ghast_tear").style(StyleReq.ANY));
        reg(clan("spiral_burst", "Chakra Burst", "spiralborn", 16, "", 34, 0, 160, 6, 6, Behaviors.arc(Vis.CHAKRA, 5, 360, 1.3, 1.0, 0.4, MoveType.ROUNDHOUSE),
                "A full-body spiral shockwave.", "minecraft:prismarine_crystals"));

        // ========== Shadoweave (Nara) ==========
        reg(clan("weave_bind", "Shadow Bind", "shadoweave", 1, "", 24, 0, 160, 12, 2, Behaviors.restrain(Vis.SHADOW, 12, 70, false, 0.3),
                "Pin a target with its own shadow.", "minecraft:ink_sac"));
        reg(clan("weave_needles", "Shadow Needles", "shadoweave", 6, "", 22, 0, 120, 14, 3, Behaviors.volley(Vis.SHADOW, 8, 1.8, 18, 0.06, 0.35, MoveType.JAB),
                "Needles of shadow that pierce and slow.", "minecraft:arrow"));
        reg(clan("weave_pull", "Shadow Pull", "shadoweave", 8, "", 26, 0, 140, 14, 1, Behaviors.vortex(Vis.SHADOW, 5, 0.6, 40, 0.4),
                "Drag enemies toward your shadow.", "minecraft:lead"));
        reg(clan("weave_strangle", "Shadow Strangle", "shadoweave", 12, "", 32, 0, 200, 10, 5, Behaviors.restrain(Vis.SHADOW, 10, 90, true, 0.5),
                "Crushing shadow that damages while binding.", "minecraft:wither_rose"));
        reg(clan("weave_field", "Shadow Possession Field", "shadoweave", 16, "", 40, 0, 260, 8, 2, Behaviors.zone(Vis.SHADOW, 6, 120, 8, 0.3, 2),
                "A wide area of living shadow.", "minecraft:black_concrete").rarity(2));
        reg(clan("weave_clone", "Shadow Mimicry", "shadoweave", 10, "", 28, 0, 180, 0, 0, Utility.empower(Vis.SHADOW, 200, 0, 1, 0, -1, -1),
                "Move and strike with shadow-enhanced speed.", "minecraft:black_dye").style(StyleReq.ANY));

        // ========== Stonebrow ==========
        reg(clan("brow_fist", "Stone Fist", "stonebrow", 1, "earth", 18, 8, 90, 3.5, 7, Behaviors.dashStrike(Vis.EARTH, 3.5, 1.5, 1.3, false),
                "A rock-hard closing punch.", "minecraft:cobblestone"));
        reg(clan("brow_guard", "Bedrock Guard", "stonebrow", 5, "earth", 22, 0, 200, 0, 0, Utility.shield(Vis.EARTH, 120, 0.55, 1, 0),
                "Shrug off heavy blows for a short time.", "minecraft:shield").style(StyleReq.ANY));
        reg(clan("brow_quake", "Quake Stomp", "stonebrow", 8, "earth", 28, 10, 150, 5, 6, Behaviors.seismic(Vis.EARTH, 5, 1.1, 1.1),
                "Stomp and crack the ground.", "minecraft:iron_boots"));
        reg(clan("brow_pillars", "Stone Pillars", "stonebrow", 12, "earth", 30, 0, 180, 10, 5, Behaviors.spikeLine(Vis.EARTH, 10, 1.1, 1.0, false),
                "Pillars erupt in a line ahead.", "minecraft:pointed_dripstone"));
        reg(clan("brow_armor", "Mountain Armor", "stonebrow", 15, "earth", 34, 0, 280, 0, 0, Utility.empower(Vis.EARTH, 300, 0.9, -1, 1, 1, -1),
                "Heavy defense and knockback resistance.", "minecraft:iron_chestplate").style(StyleReq.ANY));
        reg(clan("brow_avalanche", "Avalanche", "stonebrow", 20, "earth", 44, 0, 260, 8, 8, Behaviors.zone(Vis.EARTH, 5.5, 80, 14, 0.8, 1),
                "A collapsing mass of stone around you.", "minecraft:stone").rarity(2));

        // ========== Windrider ==========
        reg(clan("rider_slip", "Slipstream", "windrider", 1, "wind", 14, 0, 80, 8, 0, Behaviors.dashStrike(Vis.WIND, 8, 0.8, 0.5, false),
                "A long, light dash through the air.", "minecraft:feather"));
        reg(clan("rider_cutter", "Wind Cutter", "windrider", 5, "wind", 20, 0, 100, 14, 4, Behaviors.bolt(Vis.WIND, 2.2, 28, 0.3, 0.01, 2.0, 0.5, false, false, MoveType.JAB),
                "A razor gust projectile.", "minecraft:string"));
        reg(clan("rider_gale", "Gale Burst", "windrider", 8, "wind", 26, 0, 140, 6, 5, Behaviors.arc(Vis.WIND, 5, 200, 1.2, 1.0, 0.5, MoveType.ROUNDHOUSE),
                "A wide fan of cutting wind.", "minecraft:white_dye"));
        reg(clan("rider_tempest", "Tempest Step", "windrider", 12, "wind", 24, 0, 120, 10, 3, Behaviors.dashStrike(Vis.WIND, 10, 1.2, 0.9, false),
                "Dash and cut everything on the path.", "minecraft:elytra"));
        reg(clan("rider_wall", "Wind Wall", "windrider", 10, "wind", 28, 0, 200, 0, 0, Utility.shield(Vis.WIND, 80, 0.7, -1, 4),
                "A barrier of roaring wind.", "minecraft:white_banner").style(StyleReq.ANY));
        reg(clan("rider_storm", "Sky Storm", "windrider", 18, "wind", 42, 0, 240, 16, 5, Behaviors.rain(Vis.WIND, 6, 10, 50, 0.7, false),
                "Cutting winds fall from above.", "minecraft:phantom_membrane").rarity(2));

        // ========== Mistbane ==========
        reg(clan("mist_veil", "Hidden Mist", "mistbane", 1, "water", 20, 0, 200, 0, 0, Utility.empower(Vis.WATER, 200, 0, 0, 0, -1, 1),
                "Cloak the area in concealing mist.", "minecraft:white_stained_glass").style(StyleReq.ANY));
        reg(clan("mist_blade", "Mist Blade", "mistbane", 5, "water", 22, 0, 100, 5, 6, Behaviors.dashStrike(Vis.WATER, 5, 1.4, 1.1, false),
                "A silent closing cut from the fog.", "minecraft:iron_sword"));
        reg(clan("mist_surge", "Water Surge", "mistbane", 8, "water", 28, 0, 140, 8, 5, Behaviors.jet(Vis.WATER, 8, 1.2, 25, 0.4, false),
                "A pressurized water jet.", "minecraft:water_bucket"));
        reg(clan("mist_prison", "Water Prison", "mistbane", 12, "water", 32, 0, 200, 6, 2, Behaviors.restrain(Vis.WATER, 6, 90, true, 0.4),
                "Trap a target in a sphere of water.", "minecraft:heart_of_the_sea"));
        reg(clan("mist_shark", "Mist Shark", "mistbane", 16, "water", 36, 0, 180, 16, 6, Behaviors.bolt(Vis.WATER, 1.5, 32, 0.6, 0.02, 2.8, 0.8, false, false, MoveType.JAB),
                "A shark-shaped water projectile.", "minecraft:cod"));
        reg(clan("mist_tidal", "Tidal Ambush", "mistbane", 20, "water", 44, 0, 240, 10, 7, Behaviors.zone(Vis.WATER, 5, 90, 12, 0.7, 1),
                "A sudden flood zone that batters foes.", "minecraft:prismarine").rarity(2));

        // ========== Bonecrest (Kaguya-like) ==========
        reg(clan("crest_spikes", "Bone Spikes", "bonecrest", 8, "", 22, 0, 100, 4, 6, Behaviors.spikeLine(Vis.SHADOW, 6, 1.0, 0.9, false),
                "Bones erupt from the ground.", "minecraft:bone"));
        reg(clan("crest_pillars", "Bone Forest", "bonecrest", 14, "", 34, 0, 180, 8, 5, Behaviors.spikeLine(Vis.SHADOW, 10, 1.2, 1.1, false),
                "A longer line of piercing bone pillars.", "minecraft:bone_block"));
        reg(clan("crest_barrage", "Bone Bullet Barrage", "bonecrest", 12, "", 28, 0, 140, 14, 4, Behaviors.volley(Vis.SHADOW, 10, 1.6, 20, 0.07, 0.4, MoveType.JAB),
                "Hardened bone bullets.", "minecraft:arrow"));
        reg(clan("crest_blade", "Bone Blade", "bonecrest", 10, "", 24, 0, 110, 4, 8, Behaviors.dashStrike(Vis.SHADOW, 4, 1.7, 1.2, false),
                "Grow a blade of bone for a brutal slash.", "minecraft:iron_sword"));
        reg(clan("crest_armor", "Bone Armor", "bonecrest", 16, "", 30, 0, 260, 0, 0, Utility.shield(Vis.SHADOW, 140, 0.5, 1, 0),
                "Skeletal plating for heavy defense.", "minecraft:chainmail_chestplate").style(StyleReq.ANY));
        reg(clan("crest_drill", "Bone Drill", "bonecrest", 22, "", 42, 0, 220, 6, 10, Behaviors.dashStrike(Vis.SHADOW, 6, 2.0, 1.6, false),
                "A spinning bone drill charge.", "minecraft:netherite_scrap").rarity(2));

        // ========== Thunderfang ==========
        reg(clan("fang_dash", "Lightning Dash", "thunderfang", 1, "lightning", 16, 0, 70, 9, 5, Behaviors.dashStrike(Vis.LIGHTNING, 9, 1.3, 1.0, false),
                "Close the gap at lightning speed.", "minecraft:lightning_rod"));
        reg(clan("fang_chain", "Chain Lightning", "thunderfang", 6, "lightning", 26, 0, 140, 12, 4, Behaviors.volley(Vis.LIGHTNING, 5, 1.4, 22, 0.1, 0.5, MoveType.JAB),
                "Arcs that jump between targets.", "minecraft:redstone"));
        reg(clan("fang_strike", "Thunder Strike", "thunderfang", 10, "lightning", 30, 0, 160, 14, 7, Behaviors.bolt(Vis.LIGHTNING, 2.0, 26, 0.4, 0.02, 2.5, 0.7, false, false, MoveType.CROSS),
                "A focused bolt from the sky path.", "minecraft:copper_ingot"));
        reg(clan("fang_stun", "Static Palm", "thunderfang", 8, "lightning", 20, 0, 120, 4, 3, Behaviors.stunCone(Vis.LIGHTNING, 4, 40, 30, 0.7),
                "A short-range shock that locks enemies.", "minecraft:redstone_torch"));
        reg(clan("fang_armor", "Lightning Cloak", "thunderfang", 14, "lightning", 32, 0, 300, 0, 0, Utility.empower(Vis.LIGHTNING, 280, 1.25, 1, -1, -1, -1),
                "Body wrapped in lightning: speed and power.", "minecraft:golden_chestplate").style(StyleReq.ANY));
        reg(clan("fang_kirin", "Storm Fang", "thunderfang", 22, "lightning", 50, 0, 300, 18, 12, Behaviors.telegraphBlast(Vis.LIGHTNING, 5, 20, 3.5, false, false, 1),
                "Call a delayed lightning catastrophe on the target area.", "minecraft:nether_star").rarity(3));
    }

    private static void bloodlines() {
        // Mirror Eye (Sharingan-like)
        reg(bl("mirror_counter", "Mirror Counter", "mirror_eye", 15, "", 26, 200, 0, 8, Utility.counter(Vis.SHADOW, 40, 2.4),
                "Parry and answer with a mirrored strike.", "minecraft:glass"));
        reg(bl("mirror_illusion", "Mirror Illusion", "mirror_eye", 18, "", 34, 300, 8, 0, Utility.genjutsu(Vis.SHADOW, 8, 140),
                "Trap nearby foes in a brief illusion.", "minecraft:ender_eye"));
        reg(bl("mirror_focus", "Mirror Focus", "mirror_eye", 15, "", 20, 400, 28, 0, Utility.sense(Vis.CHAKRA, 28, 240),
                "Predictive sight: targets glow; your reactions sharpen.", "minecraft:spyglass"));
        reg(bl("mirror_inferno", "Black Flame", "mirror_eye", 28, "fire", 44, 340, 16, 5, Behaviors.zone(Vis.SHADOW, 3.8, 120, 10, 0.5, 1),
                "Unquenchable black fire in a small area.", "minecraft:soul_torch").rarity(3));
        reg(bl("mirror_copy", "Technique Mimic", "mirror_eye", 20, "", 30, 400, 0, 0, Utility.empower(Vis.CHAKRA, 200, 1.1, 0, 0, -1, -1),
                "Temporarily heighten all jutsu power as if reading the opponent.", "minecraft:book").rarity(2));
        reg(bl("mirror_blade", "Spectral Blade", "mirror_eye", 22, "", 36, 220, 6, 9, Behaviors.dashStrike(Vis.SHADOW, 6, 1.8, 1.4, false),
                "A chakra blade formed from mirrored insight.", "minecraft:netherite_sword"));

        // Ice Release
        reg(bl("ice_spears", "Ice Spears", "ice_release", 12, "water", 24, 110, 14, 5, Behaviors.volley(Vis.ICE, 6, 1.5, 22, 0.08, 0.4, MoveType.JAB),
                "Spears of ice that slow on hit.", "minecraft:ice"));
        reg(bl("ice_mirrors", "Crystal Ice Mirrors", "ice_release", 18, "water", 34, 220, 14, 4, Behaviors.zone(Vis.ICE, 4, 120, 10, 0.3, 2),
                "Mirrors of ice that punish movement.", "minecraft:blue_ice"));
        reg(bl("ice_dome", "Ice Dome", "ice_release", 14, "water", 28, 200, 0, 0, Utility.shield(Vis.ICE, 100, 0.6, 1, 3),
                "Encased in a protective shell of ice.", "minecraft:packed_ice"));
        reg(bl("ice_dragon", "Ice Dragon", "ice_release", 24, "water", 42, 260, 16, 7, Behaviors.jet(Vis.ICE, 12, 1.3, 35, 0.5, false),
                "A dragon of ice that freezes the path.", "minecraft:powder_snow_bucket").rarity(2));
        reg(bl("ice_prison", "Frozen Prison", "ice_release", 16, "water", 30, 180, 8, 2, Behaviors.restrain(Vis.ICE, 8, 100, true, 0.3),
                "Lock a target in solid ice.", "minecraft:snowball"));
        reg(bl("ice_age", "Frost Field", "ice_release", 28, "water", 48, 320, 10, 4, Behaviors.zone(Vis.ICE, 7, 140, 8, 0.4, 2),
                "A wide freezing field.", "minecraft:blue_ice").rarity(2));

        // Wood Release
        reg(bl("wood_spike", "Wood Spikes", "wood_release", 14, "earth", 24, 120, 10, 5, Behaviors.spikeLine(Vis.WOOD, 9, 1.0, 0.9, true),
                "Thorny wood erupts in a line.", "minecraft:oak_log"));
        reg(bl("wood_ring", "Wood Burial", "wood_release", 18, "earth", 36, 200, 6, 4, Behaviors.spikeLine(Vis.WOOD, 6, 1.0, 0.9, true),
                "A ring of trunks bursts out of the earth.", "minecraft:oak_log"));
        reg(bl("wood_guardian", "Wooden Guardian", "wood_release", 26, "earth", 46, 700, 0, 0, Utility.summon(() -> EntityType.IRON_GOLEM, 1, 800, "Wooden Guardian", Vis.WOOD),
                "Grow a guardian that protects you for 40 seconds.", "minecraft:oak_sapling").rarity(2));
        reg(bl("wood_seeds", "Seed Bombs", "wood_release", 20, "earth", 24, 150, 10, 4, Behaviors.tags(Vis.WOOD, 25, 1.0, 10),
                "Plant seeds on enemies that burst a moment later.", "minecraft:wheat_seeds"));
        reg(bl("wood_dragon", "Wood Dragon", "wood_release", 28, "earth", 50, 320, 14, 8, Behaviors.jet(Vis.WOOD, 11, 1.4, 40, 0.5, false),
                "A dragon of living wood that crushes and binds.", "minecraft:jungle_log").rarity(3));
        reg(bl("wood_domain", "Deep Forest Emergence", "wood_release", 30, "earth", 55, 400, 12, 5, Behaviors.zone(Vis.WOOD, 7, 120, 12, 0.5, 1),
                "The forest rises to claim the battlefield.", "minecraft:moss_block").rarity(3));

        // Shadow Weave bloodline
        reg(bl("shadow_step", "Umbral Step", "shadow_weave", 8, "", 20, 120, 10, 7, Utility.behindStrike(Vis.SHADOW, 10, 1.2),
                "Slip through shadows to a target's back.", "minecraft:ender_eye"));
        reg(bl("shadow_field", "Shadow Field", "shadow_weave", 14, "", 38, 280, 14, 3, Behaviors.zone(Vis.SHADOW, 6, 140, 10, 0.3, 3),
                "A wide field of living shadow that saps movement.", "minecraft:black_concrete"));
        reg(bl("shadow_needles", "Umbral Needles", "shadow_weave", 10, "", 26, 120, 16, 3, Behaviors.volley(Vis.SHADOW, 12, 2.0, 16, 0.06, 0.3, MoveType.JAB),
                "A dozen shadow needles.", "minecraft:spider_eye"));
        reg(bl("shadow_bind2", "Deep Bind", "shadow_weave", 12, "", 30, 240, 12, 2, Behaviors.restrain(Vis.SHADOW, 12, 100, false, 0.2),
                "A long, firm shadow bind on one target.", "minecraft:lead"));
        reg(bl("shadow_army", "Shadow Army", "shadow_weave", 22, "", 45, 400, 0, 0, Utility.empower(Vis.SHADOW, 240, 1.15, 1, 0, -1, -1),
                "Fight as if backed by many shadows.", "minecraft:wither_skeleton_skull").rarity(2));
        reg(bl("shadow_eclipse", "Eclipse", "shadow_weave", 26, "", 50, 360, 10, 6, Behaviors.zone(Vis.SHADOW, 8, 100, 14, 0.6, 2),
                "Plunge the area into crushing darkness.", "minecraft:obsidian").rarity(3));

        // Lava
        reg(bl("lava_flow", "Magma Flow", "lava_release", 20, "fire", 26, 100, 8, 5, Behaviors.jet(Vis.LAVA, 8, 1.2, 30, 0.45, true),
                "A flowing jet of molten rock.", "minecraft:magma_cream"));
        reg(bl("lava_burst", "Lava Burst", "lava_release", 24, "fire", 38, 190, 26, 8, Behaviors.bolt(Vis.LAVA, 1.2, 40, 0.9, 0.03, 3.4, 1.0, true, true, MoveType.JAB),
                "A lobbed glob of lava that explodes and scorches the ground.", "minecraft:lava_bucket"));
        reg(bl("lava_armor", "Magma Armor", "lava_release", 22, "fire", 30, 260, 0, 0, Utility.shield(Vis.LAVA, 160, 0.5, 1, 2.5),
                "A crust of cooling lava that burns attackers back.", "minecraft:netherite_chestplate"));
        reg(bl("lava_rain", "Cinder Rain", "lava_release", 30, "fire", 50, 400, 20, 5, Behaviors.rain(Vis.LAVA, 7, 10, 70, 0.8, false),
                "Molten droplets fall over an area.", "minecraft:blaze_rod").rarity(3));
        reg(bl("lava_fist", "Magma Fist", "lava_release", 18, "fire", 28, 130, 4, 9, Behaviors.dashStrike(Vis.LAVA, 4, 1.8, 1.4, true),
                "Close in with a molten haymaker.", "minecraft:magma_block"));
        reg(bl("lava_river", "River of Lava", "lava_release", 28, "fire", 46, 300, 14, 6, Behaviors.jet(Vis.LAVA, 14, 1.3, 45, 0.55, true),
                "A long river of lava across the field.", "minecraft:magma_cream").rarity(2));
    }
}
