package com.mayhem.client;

import com.mayhem.combat.MoveType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Which entities are currently playing which procedural animation (and who is guarding). */
public final class ClientAnimations {
    public static final class Active {
        public final MoveType move;
        public final int variant;
        public final long start;

        Active(MoveType move, int variant, long start) {
            this.move = move;
            this.variant = variant;
            this.start = start;
        }
    }

    private static final Map<Integer, Active> ACTIVE = new HashMap<>();
    private static final Set<Integer> GUARDING = new HashSet<>();

    private ClientAnimations() {}

    public static void play(int entityId, MoveType move, int variant) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (move == MoveType.GUARD_ON) {
            GUARDING.add(entityId);
            return;
        }
        if (move == MoveType.GUARD_OFF) {
            GUARDING.remove(entityId);
            return;
        }
        // Don't let a tiny hit reaction interrupt a big launch tumble.
        Active cur = ACTIVE.get(entityId);
        if (move == MoveType.JOLT && cur != null && cur.move == MoveType.TUMBLE) return;
        ACTIVE.put(entityId, new Active(move, variant, mc.level.getGameTime()));
    }

    public static boolean isGuarding(int entityId) {
        return GUARDING.contains(entityId);
    }

    /** The live animation for an entity, or null (expired animations are dropped here). */
    public static Active active(Entity e) {
        Active a = ACTIVE.get(e.getId());
        if (a == null) return null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        long age = mc.level.getGameTime() - a.start;
        boolean expired;
        switch (a.move) {
            case TUMBLE -> expired = age > a.move.ticks + 16 || (age > 6 && e.onGround());
            case SLAM_DIVE -> expired = age > 70;
            default -> expired = age >= a.move.ticks;
        }
        if (expired || !e.isAlive()) {
            ACTIVE.remove(e.getId());
            return null;
        }
        return a;
    }

    public static void clear() {
        ACTIVE.clear();
        GUARDING.clear();
    }
}
