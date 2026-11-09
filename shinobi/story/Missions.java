package com.mayhem.shinobi.story;

import com.mayhem.shinobi.story.MissionDef.Objective;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Mission board contracts and the story saga. */
public final class Missions {
    public static final Map<String, MissionDef> ALL = new LinkedHashMap<>();
    public static final List<MissionDef> STORY = new ArrayList<>();

    private static void board(String id, String title, String rank, String desc, Objective o, String arg, int target, int minLevel,
                              int xp, int ap, int sp, String item, int count, int cd) {
        ALL.put(id, new MissionDef(id, title, rank, desc, o, arg, target, minLevel, xp, ap, sp, item, count, cd, false, "", ""));
    }

    private static void story(String id, String title, String rank, String desc, Objective o, String arg, int target, int minLevel,
                              int xp, int ap, int sp, String item, int count, String intro, String outro) {
        MissionDef d = new MissionDef(id, title, rank, desc, o, arg, target, minLevel, xp, ap, sp, item, count, 0, true, intro, outro);
        ALL.put(id, d);
        STORY.add(d);
    }

    static {
        // ---- repeatable mission board contracts
        board("d_hunt", "Pest Control", "D", "Clear out the creatures lurking near the village.", Objective.KILL, "hostile", 6, 1, 60, 0, 0, "minecraft:emerald", 2, 6000);
        board("d_zombies", "Undead Sweep", "D", "Put down the restless dead.", Objective.KILL_TYPE, "minecraft:zombie", 5, 1, 70, 0, 0, "minecraft:emerald", 2, 6000);
        board("d_practice", "Technique Practice", "D", "Hone your jutsu on live targets.", Objective.CAST, "any", 12, 1, 50, 0, 0, "", 0, 4800);
        board("c_skeletons", "Bone Collector", "C", "Thin out the skeletons haunting the roads.", Objective.KILL_TYPE, "minecraft:skeleton", 8, 8, 140, 1, 0, "minecraft:emerald", 4, 9000);
        board("c_supplies", "Supply Run", "C", "Gather iron for the village smith.", Objective.COLLECT, "minecraft:iron_ingot", 12, 8, 120, 1, 0, "minecraft:emerald", 3, 9000);
        board("c_ambush", "Road Security", "C", "Defeat an ambush when one strikes.", Objective.EVENT, "ambush", 1, 10, 200, 1, 1, "", 0, 12000);
        board("b_creepers", "Demolition Duty", "B", "Neutralise the creepers before they reach the village.", Objective.KILL_TYPE, "minecraft:creeper", 10, 18, 300, 2, 1, "minecraft:diamond", 1, 12000);
        board("b_bounty", "Wanted: Rogue Captain", "B", "Take down a bounty target.", Objective.EVENT, "bounty", 1, 18, 420, 2, 1, "minecraft:diamond", 2, 18000);
        board("b_mastery", "Master the Arts", "B", "Use jutsu in battle to improve mastery.", Objective.CAST, "any", 60, 15, 260, 2, 0, "", 0, 12000);
        board("a_hunt", "Elite Purge", "A", "Eliminate a large horde.", Objective.KILL, "hostile", 40, 30, 700, 3, 2, "minecraft:diamond", 3, 24000);
        board("a_levels", "Rising Star", "A", "Prove your growth.", Objective.LEVEL, "", 40, 35, 900, 4, 2, "", 0, 0);
        board("s_beast", "Beast Hunter", "S", "Bring a tailed beast to its knees.", Objective.BEAST_DEFEAT, "any", 1, 45, 1500, 5, 3, "minecraft:netherite_scrap", 2, 36000);

        board("d_talk", "Meet the Locals", "D", "Speak with the village trainer.", Objective.TALK, "training", 1, 1, 40, 0, 0, "", 0, 0);
        board("d_water", "Walk on Water", "D", "Use Water Walking and cross the surface for a while.", Objective.WATER, "", 30, 1, 80, 1, 0, "", 0, 6000);
        board("d_wall", "Wall Runner", "D", "Use Wall Climbing and scale a wall.", Objective.WALL, "", 20, 3, 100, 1, 0, "", 0, 6000);
        board("d_escort", "Escort the Merchant", "D", "Stay close to a village NPC or villager while you travel.", Objective.ESCORT, "npc", 45, 2, 90, 1, 0, "minecraft:emerald", 2, 8000);
        board("c_protect", "Protect the Client", "C", "Keep a nearby civilian safe while clearing hostiles.", Objective.PROTECT, "npc", 6, 6, 160, 1, 1, "minecraft:emerald", 4, 10000);
        board("c_village", "Find a Village", "C", "Travel to a shinobi village.", Objective.VISIT, "shinobi_village", 1, 5, 120, 1, 0, "minecraft:emerald", 4, 0);
        board("b_shrine", "Pilgrimage", "B", "Reach a Beast Shrine.", Objective.VISIT, "beast_shrine", 1, 12, 350, 2, 1, "mayhem:sealing_scroll", 2, 0);

        // ---- story saga (in order)
        story("st_1", "Awakening: First Steps", "D", "Show you can use chakra.", Objective.CAST, "any", 5, 1, 120, 3, 2, "", 0,
                "Listen well, young shinobi. Chakra is life and will. Open the menu with M, choose an element and a clan, then show me five techniques.",
                "Good. Your body remembers how to mould chakra. This is only the beginning.");
        story("st_2", "Awakening: Trial of Bandits", "D", "Defeat hostile creatures.", Objective.KILL, "hostile", 8, 3, 200, 2, 1, "minecraft:emerald", 3,
                "Bandits and beasts have been testing the village. Drive off eight of them and prove you can fight, not just train.",
                "Eight down. You fight with more than strength. You will go far.");
        story("st_3", "Rogue Shadow", "C", "Defeat a rogue ambush.", Objective.EVENT, "ambush", 1, 6, 320, 3, 1, "", 0,
                "A rogue squad has been sighted. When they strike you, finish them. Stay alert while you travel: they will come to you.",
                "The rogues scattered. But their captain spoke of a beast that sleeps in the wild...");
        story("st_4", "Echoes of the Beast", "B", "Weaken a tailed beast.", Objective.BEAST_DEFEAT, "any", 1, 12, 500, 3, 2, "mayhem:sealing_scroll", 2,
                "The beasts are real. Open the Beasts tab in the menu and challenge one. Wear it down until it kneels, then use a Sealing Scroll.",
                "You brought one to its knees. Few can claim that.");
        story("st_5", "The Bond", "B", "Seal a tailed beast.", Objective.BEAST_SEAL, "", 1, 12, 800, 4, 3, "", 0,
                "A beast cannot be killed. It must be sealed. Hold your nerve and perform the ritual.",
                "The bond is sealed. Its chakra now runs beside yours. Treat it with respect.");
        story("st_6", "Tournament of Shadows", "B", "Defeat a bounty target.", Objective.EVENT, "bounty", 1, 18, 700, 3, 2, "minecraft:diamond", 2,
                "A wanted rogue prowls the roads. Hunt him down and show the village what you have become.",
                "The bounty is claimed. Word of your strength is spreading.");
        story("st_7", "Rising Storm", "A", "Reach level 30.", Objective.LEVEL, "", 30, 20, 1000, 4, 3, "", 0,
                "A great war is coming. Grow stronger. Reach level thirty and I will tell you of the Nine.",
                "You stand on the edge of legend.");
        story("st_8", "The Final Chakra War", "S", "Defeat the Nine-Tail.", Objective.BEAST_DEFEAT, "nine_fox", 1, 35, 3000, 8, 6, "minecraft:nether_star", 1,
                "The Nine-Tail stirs. Challenge it from the Beasts tab. Be ready for the end of the world as we know it.",
                "It is done. You are a legend. The saga is complete - for now.");
    }

    public static MissionDef get(String id) { return ALL.get(id); }

    private Missions() {}
}
