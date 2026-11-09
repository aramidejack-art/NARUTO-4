package com.mayhem.combat;

import com.mayhem.network.MayhemNetwork;
import com.mayhem.network.MomentumPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Per-player transient combat state. Never saved; cleared on logout / server stop. */
public final class CombatState {
    private static final Map<UUID, CombatState> STATES = new HashMap<>();

    public enum Slam { NONE, LEAP, DIVE }
    public enum Ult { NONE, BARRAGE, METEOR }

    // cooldown timestamps are "ready at server tick N"
    int attackReady, statusReady, dodgeReady, slamReady, blitzReady;
    int comboUntil;
    int combo;
    int juggle;

    int dodgeStart = -1000, dodgeUntil = -1;
    boolean perfectUsed;

    boolean guarding;
    int parryUntil = -1, parryReady, counterUntil = -1;

    Slam slam = Slam.NONE;
    int slamStart, diveStart;
    double lastY;
    int noFallUntil = -1;

    float momentum;
    int lastMomentumGain, syncedMomentum;

    Ult ult = Ult.NONE;
    int ultStart;
    int ultPhase;
    int ultPhaseStart;
    UUID ultTarget;
    Vec3 ultAnchor = Vec3.ZERO;

    int lastFinisherCinematic = -100000;

    private CombatState() {}

    public static CombatState of(ServerPlayer p) {
        return STATES.computeIfAbsent(p.getUUID(), id -> new CombatState());
    }

    public static CombatState peek(ServerPlayer p) {
        return STATES.get(p.getUUID());
    }

    public static void remove(UUID id) {
        STATES.remove(id);
    }

    public static void clear() {
        STATES.clear();
    }

    public boolean busy() {
        return ult != Ult.NONE;
    }

    /** Adds momentum (0..100) and syncs the meter to the owning client when the whole number changes. */
    public static void addMomentum(ServerPlayer p, CombatState s, float amount) {
        s.momentum = Math.max(0F, Math.min(100F, s.momentum + amount));
        if (amount > 0) {
            s.lastMomentumGain = p.server.getTickCount();
        }
        syncMomentum(p, s);
    }

    public static void syncMomentum(ServerPlayer p, CombatState s) {
        int whole = Math.round(s.momentum);
        if (whole != s.syncedMomentum) {
            s.syncedMomentum = whole;
            MayhemNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new MomentumPacket(whole));
        }
    }
}
