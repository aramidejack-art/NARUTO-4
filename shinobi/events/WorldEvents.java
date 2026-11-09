package com.mayhem.shinobi.events;

import com.mayhem.progression.PlayerProgression;
import com.mayhem.shinobi.ShinobiConfig;
import com.mayhem.shinobi.data.Resources;
import com.mayhem.shinobi.data.ShinobiData;
import com.mayhem.shinobi.data.ShinobiState;
import com.mayhem.shinobi.net.ShinobiNet;
import com.mayhem.shinobi.story.MissionTracker;
import com.mayhem.shinobi.registry.ShinobiRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Random world events rolled per player on a timer: Ambush, Bounty, Chakra Storm, Festival. Every event has a boss bar or
 * banner, a clear end condition and a reward. Disabled entirely by the EVENTS_ENABLED server config.
 */
public final class WorldEvents {
    private static final Random RNG = new Random();

    private static final class Ev {
        final String kind; final UUID owner; final List<UUID> mobs = new ArrayList<>(); final ServerBossEvent bar;
        long endAt; int total;
        Ev(String kind, UUID owner, ServerBossEvent bar) { this.kind = kind; this.owner = owner; this.bar = bar; }
    }

    private static final Map<UUID, Ev> ACTIVE = new HashMap<>();

    private WorldEvents() {}

    public static void clear() {
        for (Ev e : ACTIVE.values()) e.bar.removeAllPlayers();
        ACTIVE.clear();
    }

    public static void tick(ServerPlayer p, long now) {
        if (!ShinobiConfig.EVENTS_ENABLED.get()) return;
        Ev ev = ACTIVE.get(p.getUUID());
        if (ev != null) { update(p, ev, now); return; }
        ShinobiState st = ShinobiState.of(p.getUUID());
        if (st.lastEventRoll == 0) st.lastEventRoll = now;
        if (now - st.lastEventRoll < ShinobiConfig.EVENT_INTERVAL_MINUTES.get() * 1200L) return;
        st.lastEventRoll = now;
        if (PlayerProgression.getLevel(p) < 3 || p.isCreative() || p.isSpectator()) return;
        if (RNG.nextInt(3) == 0) return; // not every roll fires
        switch (RNG.nextInt(4)) {
            case 0 -> startAmbush(p, now);
            case 1 -> startBounty(p, now);
            case 2 -> startStorm(p, now);
            default -> startFestival(p, now);
        }
    }

    private static ServerBossEvent bar(String title, BossEvent.BossBarColor c, ServerPlayer p) {
        ServerBossEvent b = new ServerBossEvent(Component.literal(title), c, BossEvent.BossBarOverlay.PROGRESS);
        b.addPlayer(p);
        return b;
    }

    private static BlockPos around(ServerLevel sl, ServerPlayer p, double r) {
        double a = RNG.nextDouble() * Math.PI * 2;
        BlockPos b = BlockPos.containing(p.getX() + Math.cos(a) * r, p.getY(), p.getZ() + Math.sin(a) * r);
        return sl.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, b);
    }

    private static void startAmbush(ServerPlayer p, long now) {
        ServerLevel sl = p.serverLevel();
        int level = PlayerProgression.getLevel(p);
        Ev ev = new Ev("ambush", p.getUUID(), bar("Rogue Ambush", BossEvent.BossBarColor.RED, p));
        int n = 3 + Math.min(level / 8, 6);
        double r = Math.min(ShinobiConfig.EVENT_RADIUS.get(), 22.0);
        for (int i = 0; i < n; i++) {
            Mob m = i % 2 == 0 ? EntityType.ZOMBIE.create(sl) : EntityType.SKELETON.create(sl);
            if (m == null) continue;
            BlockPos at = around(sl, p, r);
            m.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
            m.setCustomName(Component.literal("Rogue Shinobi"));
            m.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20 + level * 0.6);
            m.setHealth(m.getMaxHealth());
            m.setPersistenceRequired();
            m.setTarget(p);
            sl.addFreshEntity(m);
            ev.mobs.add(m.getUUID());
        }
        ev.total = ev.mobs.size();
        ev.endAt = now + 6000;
        ACTIVE.put(p.getUUID(), ev);
        ShinobiNet.banner(p, 2, "Ambush!", "Rogue shinobi close in from every side");
    }

    private static void startBounty(ServerPlayer p, long now) {
        ServerLevel sl = p.serverLevel();
        int level = PlayerProgression.getLevel(p);
        Zombie z = EntityType.ZOMBIE.create(sl);
        if (z == null) return;
        BlockPos at = around(sl, p, 26);
        z.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        z.setCustomName(Component.literal("Rogue Captain"));
        z.setCustomNameVisible(true);
        z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(80 + level * 3);
        z.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(6 + level * 0.15);
        z.setHealth(z.getMaxHealth());
        z.setPersistenceRequired();
        z.setTarget(p);
        sl.addFreshEntity(z);
        Ev ev = new Ev("bounty", p.getUUID(), bar("Bounty: Rogue Captain", BossEvent.BossBarColor.YELLOW, p));
        ev.mobs.add(z.getUUID());
        ev.total = 1;
        ev.endAt = now + 9000;
        ACTIVE.put(p.getUUID(), ev);
        ShinobiNet.banner(p, 2, "Bounty Posted", "A Rogue Captain stalks nearby. Find and defeat him");
    }

    private static void startStorm(ServerPlayer p, long now) {
        Ev ev = new Ev("storm", p.getUUID(), bar("Chakra Storm", BossEvent.BossBarColor.PURPLE, p));
        ev.endAt = now + 1200;
        ACTIVE.put(p.getUUID(), ev);
        ShinobiNet.banner(p, 2, "Chakra Storm", "Chakra surges through the air. Fight while it lasts");
    }

    private static void startFestival(ServerPlayer p, long now) {
        Ev ev = new Ev("festival", p.getUUID(), bar("Village Festival", BossEvent.BossBarColor.GREEN, p));
        ev.endAt = now + 1200;
        ACTIVE.put(p.getUUID(), ev);
        p.addEffect(new MobEffectInstance(MobEffects.LUCK, 1200, 0));
        ShinobiNet.banner(p, 2, "Festival", "Lanterns light the sky. Enjoy the bonus luck");
    }

    private static void finish(ServerPlayer p, Ev ev, boolean won) {
        ev.bar.removeAllPlayers();
        ACTIVE.remove(p.getUUID());
        if (!won) { if (ev.total > 0) ShinobiNet.banner(p, 0, "Event ended", "The threat slipped away"); return; }
        double sc = ShinobiConfig.MISSION_REWARD_SCALE.get();
        int level = PlayerProgression.getLevel(p);
        PlayerProgression.awardCombat(p, (int) Math.round((40 + level * 4) * sc));
        ItemStack loot = new ItemStack(Items.EMERALD, (int) Math.max(1, Math.round((2 + level / 10) * sc)));
        if (!p.getInventory().add(loot)) p.drop(loot, false);
        CompoundTag t = ShinobiData.of(p);
        t.putInt("ap", ShinobiData.ap(t) + (ev.kind.equals("bounty") ? 1 : 0));
        MissionTracker.onEvent(p, ev.kind);
        ShinobiNet.banner(p, 2, "Event cleared", "Rewards granted");
        ShinobiNet.syncTag(p);
    }

    private static void update(ServerPlayer p, Ev ev, long now) {
        if (!p.isAlive()) { finish(p, ev, false); return; }
        long left = Math.max(0, ev.endAt - now);
        if (ev.mobs.isEmpty() && ev.total == 0) {
            ev.bar.setProgress(Math.min(1F, left / (ev.kind.equals("storm") || ev.kind.equals("festival") ? 1200F : 1F)));
            if (ev.kind.equals("storm") && now % 20 == 0) {
                Resources.addChakra(p, ShinobiData.of(p), 6);
                p.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 1.2, p.getZ(), 14, 1.6, 1.2, 1.6, 0.3);
            }
            if (ev.kind.equals("festival") && now % 20 == 0)
                p.serverLevel().sendParticles(ParticleTypes.FIREWORK, p.getX(), p.getY() + 3, p.getZ(), 6, 3, 1, 3, 0.05);
            if (left <= 0) finish(p, ev, true);
            return;
        }
        if (now % 10 != 0) return;
        int alive = 0;
        ServerLevel sl = p.serverLevel();
        for (UUID id : ev.mobs) {
            var e = sl.getEntity(id);
            if (e instanceof LivingEntity le && le.isAlive()) alive++;
        }
        ev.bar.setProgress(ev.total == 0 ? 0F : (float) alive / ev.total);
        if (alive == 0) { finish(p, ev, true); return; }
        if (left <= 0) {
            for (UUID id : ev.mobs) { var e = sl.getEntity(id); if (e != null) e.discard(); }
            finish(p, ev, false);
        }
    }
}
