package com.mayhem.shinobi.beast;

import java.util.LinkedHashMap;
import java.util.Map;

/** The nine tailed beasts: stats, attack kits per phase, unique signature technique and bond perks. */
public final class Beasts {
    public enum Atk { SLAM, CHARGE, BREATH, ROAR, BOMB }

    public record Beast(String id, String name, int tails, int color, String epithet, int minLevel, double health, double damage,
                        double scale, Atk[][] phases, String signature, String lore, String perk) {}

    public static final Map<String, Beast> ALL = new LinkedHashMap<>();

    private static void add(Beast b) { ALL.put(b.id(), b); }

    private static Atk[] a(Atk... x) { return x; }

    static {
        add(new Beast("sand_tanuki", "One-Tail Sand Tanuki", 1, 0xD9B96A, "The Sandwraith", 10, 260, 8, 1.0,
                new Atk[][]{a(Atk.SLAM, Atk.CHARGE), a(Atk.SLAM, Atk.CHARGE, Atk.ROAR), a(Atk.SLAM, Atk.ROAR, Atk.BOMB)},
                "sig_sand", "A frenzied spirit of living sand that crushes what it grabs.", "Sand Coffin: area restraint."));
        add(new Beast("blue_flame_cat", "Two-Tail Blue Flame Cat", 2, 0x4FA8FF, "The Ember Cat", 14, 300, 9, 1.0,
                new Atk[][]{a(Atk.CHARGE, Atk.BREATH), a(Atk.CHARGE, Atk.BREATH, Atk.SLAM), a(Atk.BREATH, Atk.ROAR, Atk.BOMB)},
                "sig_flame", "Agile and burning cold-blue; strikes in pounces and fire.", "Blue Flame Breath: long burning jet."));
        add(new Beast("giant_turtle", "Three-Tail Giant Turtle", 3, 0x4F8F6A, "The Shellback", 18, 380, 10, 1.1,
                new Atk[][]{a(Atk.SLAM, Atk.ROAR), a(Atk.SLAM, Atk.ROAR, Atk.CHARGE), a(Atk.SLAM, Atk.ROAR, Atk.BOMB)},
                "sig_shell", "A slow, enormous shell-wearer that hits like a landslide.", "Shell Spikes: ring of eruptions."));
        add(new Beast("lava_ape", "Four-Tail Lava Ape", 4, 0xD6501E, "The Magma King", 22, 420, 12, 1.15,
                new Atk[][]{a(Atk.SLAM, Atk.CHARGE), a(Atk.SLAM, Atk.BREATH, Atk.ROAR), a(Atk.SLAM, Atk.BOMB, Atk.BREATH)},
                "sig_magma", "A hot-tempered ape wrapped in cooling lava.", "Magma Eruption: telegraphed ground burst."));
        add(new Beast("horse_dolphin", "Five-Tail Horse-Dolphin", 5, 0xE8E8F0, "The Steam Runner", 26, 440, 12, 1.1,
                new Atk[][]{a(Atk.CHARGE, Atk.SLAM), a(Atk.CHARGE, Atk.BREATH, Atk.SLAM), a(Atk.CHARGE, Atk.ROAR, Atk.BOMB)},
                "sig_steam", "Impossibly fast; boils everything in its wake.", "Steam Dash: scalding rush."));
        add(new Beast("six_slug", "Six-Tail Slug", 6, 0x9DD0A8, "The Acid Veil", 30, 460, 11, 1.0,
                new Atk[][]{a(Atk.BREATH, Atk.SLAM), a(Atk.BREATH, Atk.SLAM, Atk.ROAR), a(Atk.BREATH, Atk.BOMB, Atk.ROAR)},
                "sig_acid", "Split-bodied and slippery, spits corrosive bubbles.", "Corrosive Spit: splash projectiles."));
        add(new Beast("horned_beetle", "Seven-Tail Horned Beetle", 7, 0x70C850, "The Scale Wing", 34, 480, 12, 1.1,
                new Atk[][]{a(Atk.CHARGE, Atk.ROAR), a(Atk.CHARGE, Atk.BREATH, Atk.ROAR), a(Atk.CHARGE, Atk.BOMB, Atk.ROAR)},
                "sig_scale", "A winged beetle that clouds the air with dazzling powder.", "Scale Powder: blind and confuse."));
        add(new Beast("ox_octopus", "Eight-Tail Ox-Octopus", 8, 0x6A3DA8, "The Ink Lord", 38, 560, 14, 1.25,
                new Atk[][]{a(Atk.SLAM, Atk.BREATH), a(Atk.SLAM, Atk.BREATH, Atk.ROAR), a(Atk.SLAM, Atk.ROAR, Atk.BOMB)},
                "sig_ink", "Eight thrashing limbs and a flood of ink.", "Ink Twister: vortex that drags enemies."));
        add(new Beast("nine_fox", "Nine-Tail Fox", 9, 0xFF8A1E, "The Great Fox", 42, 900, 18, 1.5,
                new Atk[][]{a(Atk.SLAM, Atk.CHARGE, Atk.BREATH), a(Atk.SLAM, Atk.ROAR, Atk.BREATH, Atk.BOMB), a(Atk.SLAM, Atk.ROAR, Atk.BOMB, Atk.BREATH)},
                "sig_nine", "The strongest of the nine. A calamity in fur and flame.", "Fox Fire Barrage: chakra flame rain."));
    }

    public static Beast get(String id) { return ALL.get(id); }

    private Beasts() {}
}
