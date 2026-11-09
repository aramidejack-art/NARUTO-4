package com.mayhem.combat;

import java.util.Locale;

/** Strike / reaction animation ids. Duration is in ticks. Order is the wire id. */
public enum MoveType {
    // Core chain
    JAB(8), CROSS(9), HOOK(12), KNEE(11), ROUNDHOUSE(16),
    // Context specials
    UPPERCUT(13), SWEEP(14), DASH_STRIKE(12), SPIKE(14),
    BARRAGE_L(5), BARRAGE_R(5),
    SLAM_LEAP(30), SLAM_DIVE(60), SLAM_LAND(14),
    DODGE(12), FLASH_STEP(8), PARRY(10), COUNTER(14), CHARGE(36),
    JOLT(7), TUMBLE(24),
    GUARD_ON(0), GUARD_OFF(0),
    // Premium anime kit
    SPIN_KICK(18),
    AXE_KICK(16),
    TWIST_PUNCH(14),
    BACKFLIP(20),
    FRONTFLIP(18),
    CARTWHEEL(16),
    WHIRLWIND(22),
    FLYING_KICK(15),
    ELBOW(10),
    DOUBLE_PALM(12),
    SIDE_KICK(13),
    HEEL_STOMP(14),
    SPINNING_BACKFIST(15),
    AIR_TWIST(16);

    public final int ticks;
    private static final MoveType[] VALUES = values();

    MoveType(int ticks) {
        this.ticks = ticks;
    }

    public static MoveType byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : JAB;
    }

    public boolean isChain() {
        return this == JAB || this == CROSS || this == HOOK || this == KNEE || this == ROUNDHOUSE;
    }

    public boolean isAerial() {
        return this == BACKFLIP || this == FRONTFLIP || this == CARTWHEEL || this == FLYING_KICK
                || this == AIR_TWIST || this == SPIKE || this == SLAM_LEAP || this == SLAM_DIVE;
    }

    public boolean isKick() {
        return this == KNEE || this == ROUNDHOUSE || this == SPIN_KICK || this == AXE_KICK
                || this == SIDE_KICK || this == HEEL_STOMP || this == FLYING_KICK || this == SWEEP;
    }

    public String langKey() {
        return "move.mayhem." + name().toLowerCase(Locale.ROOT);
    }
}
