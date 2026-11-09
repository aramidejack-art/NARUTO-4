package com.mayhem.shinobi.npc;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

/** Strength tiers of the wandering shinobi you can run into out in the world. */
public enum Rank {
    //     title      colour    lvl+  hp/lvl armor kbRes power  emeralds
    GENIN ("Genin",   0x9BE564, -4,   1.0,   2,    0.0,  1.0,   1),
    CHUNIN("Chunin",  0x6FB1FF,  0,   1.3,   4,    0.0,  1.2,   2),
    JONIN ("Jonin",   0xC77DFF,  6,   1.8,   7,    0.2,  1.5,   4),
    ELITE ("Elite",   0xFF8C42, 12,   2.4,  10,    0.5,  2.0,   7),
    LEGEND("Legend",  0xFFD84A, 20,   3.2,  14,    0.8,  3.0,  12);

    public final String title;
    public final int color, levelOffset, armor, emeralds;
    public final double hpPerLevel, kbResist, powerMult;

    Rank(String title, int color, int levelOffset, double hpPerLevel, int armor, double kbResist, double powerMult, int emeralds) {
        this.title = title; this.color = color; this.levelOffset = levelOffset; this.hpPerLevel = hpPerLevel;
        this.armor = armor; this.kbResist = kbResist; this.powerMult = powerMult; this.emeralds = emeralds;
    }

    /** Spawn weights per player-level band (genin, chunin, jonin, elite, legend). */
    private static final int[][] WEIGHTS = {
            {70, 28, 2, 0, 0},     // level < 8
            {30, 45, 22, 3, 0},    // level < 20
            {10, 30, 40, 17, 3},   // level < 40
            {5, 20, 38, 27, 10}    // 40+
    };

    public static Rank byIndex(int i) {
        Rank[] v = values();
        return v[Math.max(0, Math.min(v.length - 1, i))];
    }

    public static Rank roll(int playerLevel, RandomSource rnd) {
        int[] w = WEIGHTS[playerLevel < 8 ? 0 : playerLevel < 20 ? 1 : playerLevel < 40 ? 2 : 3];
        int total = 0;
        for (int x : w) total += x;
        int roll = rnd.nextInt(total);
        for (int i = 0; i < w.length; i++) { roll -= w[i]; if (roll < 0) return values()[i]; }
        return GENIN;
    }

    /** A rough "power level" number shown to the player so they can size someone up. */
    public static int basePower(int level) { return (int) (60 + level * level * 6.5 + level * 30); }

    public int power(int level) { return (int) (basePower(level) * powerMult); }

    public Component tag() { return Component.literal("[" + title + "]").withStyle(s -> s.withColor(color)); }
}
