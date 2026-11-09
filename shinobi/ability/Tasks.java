package com.mayhem.shinobi.ability;

import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntConsumer;

/** Tiny server-tick scheduler for delayed and repeating effects (telegraphs, zones, delayed explosions). */
public final class Tasks {
    private record T(long due, Runnable r) {}
    private static final List<T> QUEUE = new ArrayList<>();

    private Tasks() {}

    public static void later(MinecraftServer s, int delay, Runnable r) {
        QUEUE.add(new T(s.getTickCount() + Math.max(0, delay), r));
    }

    /** Runs {@code step(i)} for i in 0..times-1, every {@code period} ticks, starting after one period. */
    public static void every(MinecraftServer s, int period, int times, IntConsumer step) {
        for (int i = 0; i < times; i++) {
            final int k = i;
            later(s, period * (i + 1), () -> step.accept(k));
        }
    }

    public static void tick(MinecraftServer s) {
        if (QUEUE.isEmpty()) return;
        long now = s.getTickCount();
        List<Runnable> run = new ArrayList<>();
        for (Iterator<T> it = QUEUE.iterator(); it.hasNext(); ) {
            T t = it.next();
            if (t.due <= now) { run.add(t.r); it.remove(); }
        }
        for (Runnable r : run) {
            try { r.run(); } catch (RuntimeException e) { /* a broken effect must never crash the server tick */ }
        }
    }

    public static void clear() { QUEUE.clear(); }
}
