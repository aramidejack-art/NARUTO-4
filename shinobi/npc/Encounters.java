package com.mayhem.shinobi.npc;

import com.mayhem.progression.PlayerProgression;
import com.mayhem.shinobi.ShinobiConfig;
import com.mayhem.shinobi.net.ShinobiNet;
import com.mayhem.shinobi.registry.ShinobiRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** Rolls wandering-shinobi encounters around each player on a timer, so strong people turn up while you travel. */
public final class Encounters {
    private static final Map<UUID, Long> NEXT = new HashMap<>();
    private static final Random RNG = new Random();

    private Encounters() {}

    public static void clear() { NEXT.clear(); }

    public static void tick(ServerPlayer p, long now) {
        if (now % 20 != 0 || !ShinobiConfig.WANDERERS.get() || p.isSpectator()) return;
        if (p.level().dimension() != Level.OVERWORLD) return;
        long due = NEXT.computeIfAbsent(p.getUUID(), k -> now + 600L);       // first roll ~30 s after joining
        if (now < due) return;
        NEXT.put(p.getUUID(), now + ShinobiConfig.WANDERER_INTERVAL_SECONDS.get() * 20L);
        if (RNG.nextInt(100) >= ShinobiConfig.WANDERER_CHANCE.get()) return;
        ServerLevel sl = p.serverLevel();
        if (sl.getEntitiesOfClass(WandererEntity.class, p.getBoundingBox().inflate(110)).size() >= ShinobiConfig.WANDERER_MAX.get()) return;
        spawnNear(sl, p);
    }

    public static WandererEntity spawnNear(ServerLevel sl, ServerPlayer p) {
        for (int tries = 0; tries < 10; tries++) {
            double a = RNG.nextDouble() * Math.PI * 2, r = 38 + RNG.nextDouble() * 24;
            BlockPos b = BlockPos.containing(p.getX() + Math.cos(a) * r, p.getY(), p.getZ() + Math.sin(a) * r);
            if (!sl.isLoaded(b)) continue;
            BlockPos top = sl.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, b);
            if (!sl.getFluidState(top).isEmpty() || !sl.getFluidState(top.below()).isEmpty()) continue;
            if (!sl.getBlockState(top.below()).isSolid()) continue;
            if (Math.abs(top.getY() - p.getY()) > 24) continue;
            WandererEntity w = ShinobiRegistry.WANDERER.get().create(sl);
            if (w == null) return null;
            int pl = PlayerProgression.getLevel(p);
            w.setup(Rank.roll(pl, sl.random), pl);
            w.moveTo(top.getX() + 0.5, top.getY(), top.getZ() + 0.5, RNG.nextFloat() * 360F, 0F);
            sl.addFreshEntity(w);
            tell(p, w);
            return w;
        }
        return null;
    }

    private static void tell(ServerPlayer p, WandererEntity w) {
        Rank r = w.rank();
        if (r.ordinal() < Rank.JONIN.ordinal()) return;
        double dx = w.getX() - p.getX(), dz = w.getZ() - p.getZ();
        String dir = Math.abs(dx) > Math.abs(dz) * 2 ? (dx > 0 ? "east" : "west")
                : Math.abs(dz) > Math.abs(dx) * 2 ? (dz > 0 ? "south" : "north")
                : (dz > 0 ? "south" : "north") + "-" + (dx > 0 ? "east" : "west");
        if (r == Rank.JONIN) p.displayClientMessage(Component.literal("A strong presence to the " + dir + "..."), true);
        else if (r == Rank.ELITE) ShinobiNet.banner(p, 2, "An Elite shinobi", "Strong chakra to the " + dir);
        else ShinobiNet.banner(p, 2, "A Legend walks the land", "Overwhelming chakra to the " + dir);
    }
}
