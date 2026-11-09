package com.mayhem.client;

/** Latest server-sent progression / momentum snapshot. Plain data: no client-only imports. */
public final class ClientProgressionData {
    private static volatile int level = 1;
    private static volatile int xp = 0;
    private static volatile int xpToNext = 50;
    private static volatile int strength = 0;
    private static volatile int mastery = 0;
    private static volatile int momentum = 0;
    private static volatile boolean received = false;

    private ClientProgressionData() {}

    public static void update(int newLevel, int newXp, int newXpToNext, int newStrength, int newMastery) {
        level = newLevel;
        xp = newXp;
        xpToNext = newXpToNext;
        strength = newStrength;
        mastery = newMastery;
        received = true;
    }

    public static void setMomentum(int value) {
        momentum = Math.max(0, Math.min(100, value));
    }

    public static void reset() {
        received = false;
        momentum = 0;
    }

    public static boolean hasData() { return received; }
    public static int level() { return level; }
    public static int xp() { return xp; }
    public static int xpToNext() { return xpToNext; }
    public static int strength() { return strength; }
    public static int mastery() { return mastery; }
    public static int momentum() { return momentum; }
}
