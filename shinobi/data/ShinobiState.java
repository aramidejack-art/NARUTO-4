package com.mayhem.shinobi.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Transient per-player runtime state (cooldowns, windows, sync bookkeeping). Persistent data lives in ShinobiData. */
public final class ShinobiState {
    private static final Map<UUID, ShinobiState> STATES = new HashMap<>();

    public final Map<String, Long> cooldowns = new HashMap<>();
    public final Map<String, Long> lastMasteryCount = new HashMap<>();
    public long gcdUntil;
    public long guardUntil;            // jutsu guard (absorb) active until
    public double guardReduction = 0;  // 0..0.9 incoming damage reduction while guardUntil
    public long counterUntil;          // counter stance active until
    public double counterDamage = 0;
    public long boostUntil;            // damage buff until
    public double boostMul = 1.0;
    public long lastHurt = -1000;
    public int castToken;              // incremented to cancel a pending cast
    public String casting = "";
    public boolean charging;
    public long staminaRegenAt;
    public int summons;
    public long ritualStart = -1;      // beast sealing ritual
    public int ritualBeast = -1;
    public long lastEventRoll;
    public long exhaustedUntil;
    // sync bookkeeping
    public float sentChakra = -1, sentStamina = -1;
    public int sentFlags = -1;
    public String sentMode = "";
    public long lastResourceSync;
    public int apLevel = -1;
    // chakra avatar moveset
    public long punchCd, tackleCd;
    public int punchSide, tackleTicks;
    // chakra mode techniques
    public final long[] skillCd = new long[2];
    public int dashTicks, dashFire;
    public float dashSpeed, dashDmg;
    public final java.util.Set<Integer> tackleHit = new java.util.HashSet<>();

    private ShinobiState() {}

    public static ShinobiState of(UUID id) { return STATES.computeIfAbsent(id, k -> new ShinobiState()); }
    public static ShinobiState peek(UUID id) { return STATES.get(id); }
    public static void remove(UUID id) { STATES.remove(id); }
    public static void clear() { STATES.clear(); }
}
