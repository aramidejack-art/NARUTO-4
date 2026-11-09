package com.mayhem.shinobi.ability;

import com.mayhem.combat.MoveType;
import com.mayhem.combat.Teleports;
import com.mayhem.fx.Fx;
import com.mayhem.shinobi.ShinobiConfig;
import com.mayhem.shinobi.identity.Passives;
import com.mayhem.world.Dynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Offensive and control mechanics. Each factory returns a Behavior with its own geometry and timing:
 * bolts, jets, arcs, zones, waves, dashes, chains, beams, spike lines, walls, traps, rains, blasts.
 */
public final class Behaviors {
    private static final Random RNG = new Random();
    private Behaviors() {}

    // ------------------------------------------------------------------ projectiles
    /** A travelling projectile. Variants: SPREAD fires a 3-way fan, FOCUSED is faster and thinner, PIERCE passes through. */
    public static Behavior bolt(Vis vis, double speed, int life, double radius, double gravity, double aoe, double knock,
                                boolean ignite, boolean crater, MoveType anim) {
        return c -> {
            if (anim != null) c.swing(anim);
            c.castSound(vis);
            double sp = c.flag == Flag.FOCUSED ? speed * 1.35 : speed;
            double rad = c.flag == Flag.FOCUSED ? radius * 0.7 : radius;
            int shots = c.flag == Flag.SPREAD ? 3 : 1;
            double mult = c.flag == Flag.SPREAD ? 0.65 : 1.0;
            Vec3 look = c.look();
            for (int i = 0; i < shots; i++) {
                double yaw = shots == 1 ? 0 : (i - 1) * Math.toRadians(13);
                Vec3 dir = rotY(look, yaw).normalize();
                Projectiles.P pr = new Projectiles.P(c, c.eye().add(dir.scale(0.8)).add(0, -0.25, 0), dir.scale(sp), vis)
                        .gravity(gravity).radius(rad).life(life).pierce(c.flag == Flag.PIERCE);
                final double m = mult;
                pr.onHit((cc, e, at) -> {
                    if (aoe > 0) { cc.blast(at, aoe * (cc.flag == Flag.AREA ? 1.3 : 1.0), m, knock, 0.3, vis, crater); }
                    else if (cc.hurt(e, m, true)) { cc.push(e, cc.look(), knock, 0.1); cc.fx(vis.accent(), at, 8, 0.3, 0.08); cc.sound(vis.hit(), at, 0.8F, 1.2F); }
                    if (ignite) e.setSecondsOnFire(4);
                });
                pr.onEnd((cc, at, why) -> { if (aoe > 0 && why != Projectiles.Reason.ENTITY) cc.blast(at, aoe, m, knock, 0.3, vis, crater); });
                Projectiles.spawn(pr);
            }
        };
    }

    /** Homing volley: {@code count} small projectiles that curve toward the target. */
    public static Behavior volley(Vis vis, int count, double speed, double spreadDeg, double homing, double mult, MoveType anim) {
        return c -> {
            if (anim != null) c.swing(anim);
            c.castSound(vis);
            LivingEntity tgt = c.primaryTarget(c.range);
            int n = c.flag == Flag.SPREAD ? count + 3 : count;
            for (int i = 0; i < n; i++) {
                final int k = i;
                Tasks.later(c.p.server, i * 2, () -> {
                    if (!c.p.isAlive()) return;
                    Vec3 dir = rotY(c.look(), Math.toRadians((RNG.nextDouble() - 0.5) * spreadDeg));
                    dir = new Vec3(dir.x, dir.y + (RNG.nextDouble() - 0.5) * 0.15, dir.z).normalize();
                    Projectiles.Reason unused = Projectiles.Reason.EXPIRE;
                    Projectiles.P pr = new Projectiles.P(c, c.eye().add(dir.scale(0.7)), dir.scale(speed), vis)
                            .life((int) (c.range / speed) + 6).radius(0.5).homing(homing, tgt)
                            .onHit((cc, e, at) -> { if (cc.hurt(e, mult, true)) { cc.fx(vis.accent(), at, 5, 0.2, 0.05); } });
                    Projectiles.spawn(pr);
                });
            }
        };
    }

    // ------------------------------------------------------------------ channelled / instant shapes
    /** Channelled jet in front of the caster. Rooted unless the variant is MOBILE; follows the caster's aim. */
    public static Behavior jet(Vis vis, double length, double halfWidth, int ticks, double perHit, boolean ignite) {
        return c -> {
            c.swing(MoveType.JAB);
            c.castSound(vis);
            int dur = c.flag == Flag.CHARGED ? ticks + 10 : ticks;
            if (c.flag != Flag.MOBILE) c.p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 3, false, false));
            Tasks.every(c.p.server, 2, dur / 2, i -> {
                if (!c.p.isAlive()) return;
                Vec3 eye = c.eye(), look = c.look();
                Vec3 end = c.rayEnd(look, c.range);
                double len = Math.min(c.range, eye.distanceTo(end));
                for (double d = 1.0; d <= len; d += 0.7) {
                    Vec3 pt = eye.add(look.scale(d)).add(0, -0.3, 0);
                    c.fx(vis.main(), pt, 3, halfWidth * 0.4 * (d / len + 0.3), 0.04);
                    if (((int) (d * 10)) % 3 == 0) c.fx(vis.dust(), pt, 1, 0.2, 0);
                }
                for (LivingEntity e : c.inLine(eye, eye.add(look.scale(len)), halfWidth)) {
                    if (c.hurt(e, perHit, true)) { if (ignite) e.setSecondsOnFire(3); c.push(e, look, 0.18, 0.02); }
                }
                if (i % 2 == 0) c.sound(vis.cast(), c.p.position(), 0.6F, 1.4F);
            });
        };
    }

    /** Instant sweeping arc (or full circle). */
    public static Behavior arc(Vis vis, double radius, double halfDeg, double mult, double knock, double lift, MoveType anim) {
        return c -> {
            c.swing(anim == null ? MoveType.SWEEP : anim);
            double r = c.flag == Flag.AREA ? radius * 1.3 : radius;
            c.castSound(vis);
            Vec3 f = c.flatLook();
            int pts = halfDeg >= 170 ? 28 : 18;
            for (int i = 0; i < pts; i++) {
                double a = halfDeg >= 170 ? Math.PI * 2 * i / pts : Math.toRadians(-halfDeg + (2 * halfDeg) * i / (pts - 1.0));
                Vec3 d = rotY(f, a);
                for (double k = 1.0; k <= r; k += 1.0) {
                    c.fx(vis.main(), c.p.position().add(d.scale(k)).add(0, 0.9, 0), 1, 0.08, 0.02);
                }
                c.fx(vis.accent(), c.p.position().add(d.scale(r)).add(0, 1.0, 0), 1, 0.1, 0.05);
            }
            Vec3 pos = c.p.position();
            for (LivingEntity e : c.inRadius(pos, r)) {
                Vec3 d = e.position().subtract(pos);
                Vec3 flat = new Vec3(d.x, 0, d.z);
                if (halfDeg < 170 && flat.lengthSqr() > 0.01 && c.flatLook().dot(flat.normalize()) < Math.cos(Math.toRadians(halfDeg))) continue;
                if (c.hurt(e, mult)) c.push(e, flat, knock, lift);
            }
        };
    }

    /** A lingering damaging zone placed where the caster aims. */
    public static Behavior zone(Vis vis, double radius, int duration, int period, double mult, int slow) {
        return c -> {
            c.swing(MoveType.SLAM_LAND);
            Vec3 at = c.groundBelow(c.aimPoint(c.range)).add(0, 0.1, 0);
            double r = c.flag == Flag.AREA ? radius * 1.4 : radius;
            c.castSound(vis);
            Fx.ring(c.level, vis.dust(), at, r, (int) (r * 10), 0, 0);
            Tasks.every(c.p.server, period, duration / period, i -> {
                if (!c.p.isAlive()) return;
                for (int k = 0; k < 14; k++) {
                    double a = RNG.nextDouble() * Math.PI * 2, rr = Math.sqrt(RNG.nextDouble()) * r;
                    c.fx(vis.main(), at.add(Math.cos(a) * rr, 0.1, Math.sin(a) * rr), 1, 0.1, 0.03);
                }
                Fx.ring(c.level, vis.dust(), at, r, (int) (r * 6), 0, 0);
                for (LivingEntity e : c.inRadius(at, r)) {
                    if (c.hurt(e, mult, true) && slow > 0) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, slow));
                }
            });
        };
    }

    /** A ground-hugging wave that sweeps forward and heaves enemies into the air. */
    public static Behavior wave(Vis vis, double length, double halfWidth, double mult, double lift) {
        return c -> {
            c.swing(MoveType.UPPERCUT);
            c.castSound(vis);
            Vec3 start = c.p.position(), dir = c.flatLook();
            Set<Integer> done = new HashSet<>();
            int steps = (int) (length);
            for (int i = 1; i <= steps; i++) {
                final int k = i;
                Tasks.later(c.p.server, i, () -> {
                    Vec3 pt = start.add(dir.scale(k));
                    for (double s = -halfWidth; s <= halfWidth; s += 0.8) {
                        Vec3 side = pt.add(-dir.z * s, 0, dir.x * s);
                        c.fx(vis.main(), side.add(0, 0.5 + 0.35 * Math.sin(k * 0.8), 0), 3, 0.15, 0.05);
                        c.fx(vis.accent(), side.add(0, 1.0, 0), 1, 0.2, 0.02);
                    }
                    for (LivingEntity e : c.inRadius(pt.add(0, 0.8, 0), halfWidth + 0.6)) {
                        if (done.add(e.getId()) && c.hurt(e, mult)) c.push(e, dir, 0.7, lift);
                    }
                });
            }
        };
    }

    /** Instant piercing beam from the eyes. */
    public static Behavior beam(Vis vis, double halfWidth, double mult, double knock, int windup) {
        return c -> {
            c.swing(MoveType.CROSS);
            c.castSound(vis);
            Tasks.later(c.p.server, windup, () -> {
                if (!c.p.isAlive()) return;
                Vec3 eye = c.eye(), look = c.look();
                Vec3 end = c.rayEnd(look, c.range);
                double len = eye.distanceTo(end);
                for (double d = 0.8; d <= len; d += 0.35) {
                    Vec3 pt = eye.add(look.scale(d)).add(0, -0.2, 0);
                    c.fx(vis.main(), pt, 1, 0.05, 0);
                    c.fx(vis.dust(), pt, 1, 0.08, 0);
                }
                for (LivingEntity e : c.inLine(eye, end, halfWidth)) {
                    if (c.hurt(e, mult)) c.push(e, look, knock, 0.1);
                }
                c.blast(end, 1.6, mult * 0.4, 0.4, 0.15, vis, false);
            });
        };
    }

    /** Dash through enemies in a line, striking each once. */
    public static Behavior dashStrike(Vis vis, double distance, double mult, double knock, boolean followUp) {
        return c -> {
            c.swing(MoveType.DASH_STRIKE);
            c.castSound(vis);
            Vec3 dir = c.look();
            dir = new Vec3(dir.x, Math.max(-0.3, Math.min(0.3, dir.y)), dir.z).normalize();
            double dist = c.flag == Flag.CHARGED ? distance * 1.35 : distance;
            Vec3 start = c.p.position();
            Vec3 land = Utility.dashTo(c, dir, dist);
            if (land == null) { c.sound(vis.cast(), start, 0.5F, 0.6F); return; }
            Vec3 lineEnd = land.add(0, 1, 0);
            Fx.trail(c.level, start, land);
            for (LivingEntity e : c.inLine(start.add(0, 1, 0), lineEnd, 1.2)) {
                if (c.hurt(e, mult)) { c.push(e, dir, knock, 0.2); c.fx(vis.accent(), e.position().add(0, 1, 0), 10, 0.3, 0.1); }
            }
            Teleports.blink(c.p, land, land.add(dir));
            c.p.fallDistance = 0;
            if (followUp && "thunderfang".equals(com.mayhem.shinobi.data.ShinobiData.clan(c.t))) {
                for (LivingEntity e : c.inRadius(land, 3.0)) { c.hurt(e, mult * 0.5, true); break; }
            }
        };
    }

    /** Chain between targets, stunning each briefly. */
    public static Behavior chain(Vis vis, int jumps, double jumpRange, double falloff, int stun) {
        return c -> {
            c.swing(MoveType.JAB);
            c.castSound(vis);
            LivingEntity cur = c.primaryTarget(c.range);
            if (cur == null) { c.fx(vis.main(), c.eye().add(c.look().scale(2)), 8, 0.3, 0.1); return; }
            Set<Integer> seen = new HashSet<>();
            Vec3 from = c.eye();
            double m = 1.0;
            int n = c.flag == Flag.SPREAD ? jumps + 2 : jumps;
            for (int i = 0; i < n && cur != null; i++) {
                Vec3 to = cur.position().add(0, cur.getBbHeight() * 0.5, 0);
                for (double d = 0; d < from.distanceTo(to); d += 0.4) {
                    c.fx(vis.main(), from.add(to.subtract(from).normalize().scale(d)), 1, 0.05, 0);
                }
                seen.add(cur.getId());
                if (c.hurt(cur, m, true)) {
                    c.freeze(cur, stun);
                    c.fx(vis.accent(), to, 8, 0.3, 0.1);
                    c.sound(vis.hit(), to, 0.7F, 1.4F);
                }
                m *= falloff;
                from = to;
                LivingEntity next = null; double bd = Double.MAX_VALUE;
                for (LivingEntity e : c.inRadius(to, jumpRange)) {
                    if (seen.contains(e.getId())) continue;
                    double d = e.distanceToSqr(cur);
                    if (d < bd) { bd = d; next = e; }
                }
                cur = next;
            }
        };
    }

    /** Short-range cone that stuns everything caught. */
    public static Behavior stunCone(Vis vis, double reach, double halfDeg, int ticks, double mult) {
        return c -> {
            c.swing(MoveType.HOOK);
            c.castSound(vis);
            for (LivingEntity e : c.inCone(reach, halfDeg)) {
                if (c.hurt(e, mult)) { c.freeze(e, ticks); c.fx(vis.main(), e.position().add(0, 1, 0), 12, 0.4, 0.1); }
            }
            Vec3 f = c.look();
            for (int i = 0; i < 12; i++) c.fx(vis.main(), c.eye().add(f.scale(1 + i * reach / 12)), 3, 0.35 + i * 0.05, 0.05);
        };
    }

    /** Spikes erupt in sequence along a line, launching whatever stands there. */
    public static Behavior spikeLine(Vis vis, double length, double mult, double lift, boolean ring) {
        return c -> {
            c.swing(MoveType.SLAM_LAND);
            c.castSound(vis);
            Vec3 start = c.p.position(), dir = c.flatLook();
            Set<Integer> done = new HashSet<>();
            if (ring) {
                for (int i = 0; i < 12; i++) {
                    double a = Math.PI * 2 * i / 12;
                    final Vec3 pt = start.add(Math.cos(a) * 3.2, 0, Math.sin(a) * 3.2);
                    Tasks.later(c.p.server, 2 + i / 2, () -> eruptAt(c, vis, pt, mult, lift, done));
                }
                return;
            }
            for (int i = 1; i <= (int) length; i++) {
                final Vec3 pt = start.add(dir.scale(i * 1.0));
                Tasks.later(c.p.server, i, () -> eruptAt(c, vis, pt, mult, lift, done));
            }
        };
    }

    private static void eruptAt(CastContext c, Vis vis, Vec3 pt, double mult, double lift, Set<Integer> done) {
        if (!c.p.isAlive()) return;
        Vec3 g = c.groundBelow(pt.add(0, 2, 0));
        BlockState bs = c.level.getBlockState(BlockPos.containing(g.subtract(0, 0.2, 0)));
        if (!bs.isAir()) Fx.blockDust(c.level, bs, g.add(0, 0.5, 0), 14);
        for (int k = 0; k < 4; k++) c.fx(vis.main(), g.add(0, 0.4 + k * 0.5, 0), 3, 0.2, 0.05);
        c.sound(vis.cast(), g, 0.5F, 0.7F);
        for (LivingEntity e : c.inRadius(g.add(0, 0.8, 0), 1.3)) {
            if (done.add(e.getId()) && c.hurt(e, mult)) c.push(e, e.position().subtract(c.p.position()), 0.2, lift);
        }
    }

    /** Temporary wall of blocks in front of the caster (reverted automatically). */
    public static Behavior wall(Vis vis, int halfWidth, int height, int duration) {
        return c -> {
            c.swing(MoveType.SLAM_LAND);
            c.castSound(vis);
            Vec3 f = c.flatLook();
            Vec3 center = c.p.position().add(f.scale(3.0));
            if (!ShinobiConfig.TEMP_WALLS.get()) {
                Utility.shieldFx(c, vis);
                return;
            }
            List<BlockPos> placed = new ArrayList<>();
            int w = c.flag == Flag.AREA ? halfWidth + 2 : halfWidth;
            BlockState block = Blocks.DRIPSTONE_BLOCK.defaultBlockState();
            for (int s = -w; s <= w; s++) {
                for (int h = 0; h < height; h++) {
                    BlockPos pos = BlockPos.containing(center.x - f.z * s, center.y + h, center.z + f.x * s);
                    if (c.level.getBlockState(pos).isAir() && c.level.mayInteract(c.p, pos)) {
                        c.level.setBlock(pos, block, 3);
                        placed.add(pos);
                        c.fx(vis.main(), Vec3.atCenterOf(pos), 3, 0.3, 0.05);
                    }
                }
            }
            for (LivingEntity e : c.inRadius(center, w + 1.5)) c.push(e, e.position().subtract(c.p.position()), 0.5, 0.2);
            WALLS.addAll(placed);
            Tasks.later(c.p.server, duration, () -> revert(c.level, placed, block));
        };
    }

    private static final List<BlockPos> WALLS = new ArrayList<>();

    private static void revert(ServerLevel level, List<BlockPos> placed, BlockState block) {
        for (BlockPos pos : placed) {
            if (level.getBlockState(pos).is(block.getBlock())) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            WALLS.remove(pos);
        }
    }

    /** Called when the server stops so temporary walls never become permanent. */
    public static void revertAllWalls(MinecraftServer server) {
        for (ServerLevel l : server.getAllLevels()) {
            for (BlockPos pos : new ArrayList<>(WALLS)) {
                if (l.getBlockState(pos).is(Blocks.DRIPSTONE_BLOCK)) l.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        WALLS.clear();
    }

    /** Stomp that cracks the ground and flings everything nearby. */
    public static Behavior seismic(Vis vis, double radius, double mult, double knock) {
        return c -> {
            c.swing(MoveType.SLAM_LAND);
            c.castSound(vis);
            c.blast(c.p.position(), c.flag == Flag.AREA ? radius * 1.3 : radius, mult, knock, 0.55, vis, true);
        };
    }

    /** Close-range burst that pushes everything away from the caster. */
    public static Behavior pushBurst(Vis vis, double radius, double strength, double lift, double mult) {
        return c -> {
            c.swing(MoveType.JOLT);
            c.castSound(vis);
            Vec3 pos = c.p.position();
            Fx.ring(c.level, vis.main(), pos.add(0, 0.5, 0), 0.5, 22, 0.5, 0.05);
            Fx.ring(c.level, vis.accent(), pos.add(0, 1.1, 0), 0.5, 22, 0.4, 0.05);
            for (LivingEntity e : c.inRadius(pos, radius)) {
                if (mult <= 0 || c.hurt(e, mult)) c.push(e, e.position().subtract(pos), strength, lift);
            }
        };
    }

    /** Pulling vortex at the aim point. */
    public static Behavior vortex(Vis vis, double radius, double pull, int duration, double mult) {
        return c -> {
            c.swing(MoveType.JOLT);
            c.castSound(vis);
            Vec3 at = c.aimPoint(c.range);
            Tasks.every(c.p.server, 2, duration / 2, i -> {
                if (!c.p.isAlive()) return;
                for (int k = 0; k < 16; k++) {
                    double a = (i * 0.9 + k) * 0.6, rr = radius * (k / 16.0);
                    c.fx(vis.main(), at.add(Math.cos(a) * rr, 0.3 + k * 0.12, Math.sin(a) * rr), 1, 0.05, 0.02);
                }
                for (LivingEntity e : c.inRadius(at, radius)) {
                    Vec3 d = at.subtract(e.position().add(0, e.getBbHeight() * 0.5, 0));
                    c.push(e, d, pull, 0.04);
                    if (i % 3 == 0) c.hurt(e, mult, true);
                }
            });
        };
    }

    /** Pulls the primary target to the caster and holds it. */
    public static Behavior grapple(Vis vis, double mult, int hold) {
        return c -> {
            c.swing(MoveType.CROSS);
            c.castSound(vis);
            LivingEntity e = c.primaryTarget(c.range);
            if (e == null) return;
            Vec3 a = c.eye(), b = e.position().add(0, e.getBbHeight() * 0.5, 0);
            for (double d = 0; d < a.distanceTo(b); d += 0.4) c.fx(vis.main(), a.add(b.subtract(a).normalize().scale(d)), 1, 0.03, 0);
            if (c.hurt(e, mult)) { c.push(e, c.p.position().subtract(e.position()), 1.1, 0.25); c.freeze(e, (int) (hold * Passives.restrainLengthMul(c.t))); }
        };
    }

    /** Roots enemies in place with visible binding rings. */
    public static Behavior restrain(Vis vis, double reach, int ticks, boolean area, double mult) {
        return c -> {
            c.swing(MoveType.CROSS);
            c.castSound(vis);
            int len = (int) (ticks * Passives.restrainLengthMul(c.t) * (c.flag == Flag.ROOTED ? 1.5 : 1.0));
            List<LivingEntity> targets = (area || c.flag == Flag.AREA) ? c.inRadius(c.aimPoint(reach), 4.0) : new ArrayList<>();
            if (targets.isEmpty()) { LivingEntity e = c.primaryTarget(reach); if (e != null) targets.add(e); }
            for (LivingEntity e : targets) {
                if (mult > 0) c.hurt(e, mult);
                c.freeze(e, len);
                final LivingEntity fe = e;
                Tasks.every(c.p.server, 4, len / 4, i -> {
                    if (fe.isAlive()) Fx.ring(c.level, vis.dust(), fe.position().add(0, 0.2 + (i % 4) * 0.45, 0), 0.9, 10, 0, 0);
                });
            }
        };
    }

    /** A hidden charge placed at the aim point that detonates when an enemy comes close. */
    public static Behavior trap(Vis vis, double triggerR, double blastR, double mult, int armDelay) {
        return c -> {
            c.swing(MoveType.SLAM_LAND);
            Vec3 at = c.groundBelow(c.aimPoint(c.range)).add(0, 0.2, 0);
            c.sound(vis.cast(), at, 0.5F, 1.6F);
            c.fx(vis.accent(), at, 6, 0.2, 0.02);
            int checks = 150;
            Tasks.every(c.p.server, 4, checks, i -> {
                if (!c.p.isAlive() || c.p.level() != c.level) return;
                if (i * 4 < armDelay) return;
                if (i % 5 == 0) c.fx(vis.main(), at, 1, 0.15, 0);
                if (!c.inRadius(at, triggerR).isEmpty() && !TRIGGERED.contains(at)) {
                    TRIGGERED.add(at);
                    c.blast(at, blastR, mult, 0.9, 0.5, vis, false);
                }
            });
        };
    }

    private static final Set<Vec3> TRIGGERED = new HashSet<>();

    /** Delayed volley of explosive tags that land on every target in a cone. */
    public static Behavior tags(Vis vis, int delay, double mult, double reach) {
        return c -> {
            c.swing(MoveType.JAB);
            c.castSound(vis);
            for (LivingEntity e : c.inCone(reach, 40)) {
                c.fx(vis.accent(), e.position().add(0, 1.2, 0), 6, 0.25, 0.02);
                Tasks.later(c.p.server, delay, () -> {
                    if (e.isAlive() && c.p.isAlive()) c.blast(e.position().add(0, 0.6, 0), 2.2, mult, 0.5, 0.4, vis, false);
                });
            }
        };
    }

    /** Ring of afterimage strikes: the caster appears to hit from several angles in a heartbeat. */
    public static Behavior clones(Vis vis, int count, double mult) {
        return c -> {
            c.swing(MoveType.BARRAGE_L);
            c.castSound(vis);
            LivingEntity e = c.primaryTarget(c.range);
            if (e == null) return;
            for (int i = 0; i < count; i++) {
                final int k = i;
                Tasks.later(c.p.server, 2 + i * 2, () -> {
                    if (!e.isAlive() || !c.p.isAlive()) return;
                    double a = Math.PI * 2 * k / count;
                    Vec3 pt = e.position().add(Math.cos(a) * 2.2, 0.1, Math.sin(a) * 2.2);
                    Fx.trail(c.level, pt, e.position());
                    c.fx(vis.main(), pt.add(0, 1, 0), 10, 0.3, 0.05);
                    if (c.hurt(e, mult / count * 1.6, true)) c.push(e, e.position().subtract(pt), 0.25, 0.05);
                });
            }
            Tasks.later(c.p.server, 4 + count * 2, () -> { if (e.isAlive()) c.push(e, c.flatLook(), 0.9, 0.5); });
        };
    }

    /** Telegraphed area attack: a warning ring, then a heavy blast. Counterplay = leave the ring. */
    public static Behavior telegraphBlast(Vis vis, double radius, int windup, double mult, boolean atAim, boolean crater, int pulses) {
        return c -> {
            c.swing(MoveType.SLAM_LEAP);
            c.castSound(vis);
            Vec3 at = atAim ? c.groundBelow(c.aimPoint(c.range)).add(0, 0.1, 0) : c.p.position();
            Vec3 centre = at;
            Tasks.every(c.p.server, 4, Math.max(1, windup / 4), i -> {
                if (!c.p.isAlive()) return;
                double r = radius * (0.35 + 0.65 * (i + 1) / Math.max(1, windup / 4));
                Fx.ring(c.level, vis.dust(), centre, radius, (int) (radius * 9), 0, 0);
                Fx.ring(c.level, vis.main(), centre, r, (int) (r * 8), 0.02, 0.05);
                c.sound(com.mayhem.fx.MayhemSounds.CHARGE.get(), centre, 0.6F, 0.8F + i * 0.1F);
            });
            for (int p = 0; p < pulses; p++) {
                final int k = p;
                Tasks.later(c.p.server, windup + p * 6, () -> {
                    if (!c.p.isAlive()) return;
                    c.blast(centre, radius * (1.0 - k * 0.1), mult / Math.max(1, pulses) * 1.6, 1.3, 0.8, vis, crater && k == 0);
                });
            }
        };
    }

    /** Scattered strikes over an area for a duration (thousand bolts, meteor rain, ash storm). */
    public static Behavior rain(Vis vis, double radius, int strikes, int duration, double mult, boolean lightning) {
        return c -> {
            c.swing(MoveType.SLAM_LEAP);
            c.castSound(vis);
            Vec3 centre = c.groundBelow(c.aimPoint(c.range)).add(0, 0.1, 0);
            Fx.ring(c.level, vis.dust(), centre, radius, (int) (radius * 9), 0, 0);
            Tasks.every(c.p.server, Math.max(1, duration / strikes), strikes, i -> {
                if (!c.p.isAlive()) return;
                double a = RNG.nextDouble() * Math.PI * 2, rr = Math.sqrt(RNG.nextDouble()) * radius;
                Vec3 pt = centre.add(Math.cos(a) * rr, 0, Math.sin(a) * rr);
                if (lightning) {
                    net.minecraft.world.entity.LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(c.level);
                    if (bolt != null) { bolt.moveTo(pt); bolt.setVisualOnly(true); c.level.addFreshEntity(bolt); }
                }
                c.blast(pt, 2.6, mult, 0.7, 0.45, vis, i % 4 == 0);
            });
        };
    }

    // ------------------------------------------------------------------ helpers
    static Vec3 rotY(Vec3 v, double angle) {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        return new Vec3(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos);
    }

    @SuppressWarnings("unused")
    private static void unused(Mob m, HitResult h, BlockHitResult b, ClipContext cc, Dynamics d) {}
}
