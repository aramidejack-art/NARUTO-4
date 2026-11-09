package com.mayhem.combat;

import com.mayhem.config.MayhemConfig;
import com.mayhem.fx.Fx;
import com.mayhem.fx.MayhemSounds;
import com.mayhem.network.CinematicPacket;
import com.mayhem.network.ImpactPacket;
import com.mayhem.network.MayhemNetwork;
import com.mayhem.progression.MobLevelHandler;
import com.mayhem.progression.PlayerProgression;
import com.mayhem.world.Destruction;
import com.mayhem.world.Dynamics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

/**
 * Server-authoritative striking: lock-on blitz, the 5-hit chain, context specials
 * (dash strike, uppercut, sweep, aerial spike), air juggles, hit-stop and rewards.
 */
public final class UnarmedCombat {
    private static final int STATUS_COOLDOWN_TICKS = 20;

    private record Spec(float dmg, double reach, double cone, int max, double knock, double lift, int extraCd) {}

    private UnarmedCombat() {}

    public static float baseDamage(ServerPlayer p) {
        return (float) p.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue() + PlayerProgression.getStrengthBonus(p);
    }

    /** Rate limit for the status key so a client cannot spam chat/packets. */
    public static boolean allowStatusRequest(ServerPlayer player) {
        CombatState s = CombatState.of(player);
        int now = player.server.getTickCount();
        if (now < s.statusReady) return false;
        s.statusReady = now + STATUS_COOLDOWN_TICKS;
        return true;
    }

    private static Spec spec(MoveType m) {
        return switch (m) {
            case JAB -> new Spec(1.0F, 3.2, 26, 1, 0.20, 0.0, 0);
            case CROSS -> new Spec(1.1F, 3.2, 26, 1, 0.30, 0.0, 0);
            case HOOK -> new Spec(1.25F, 3.0, 60, 3, 0.45, 0.0, 2);
            case KNEE -> new Spec(1.15F, 3.0, 30, 1, 0.20, 0.45, 2);
            case ROUNDHOUSE -> new Spec(1.8F, 3.4, 100, 6, 1.0, 0.30, 6);
            case UPPERCUT -> new Spec(1.35F, 3.2, 32, 1, 0.12, 0.90, 4);
            case SWEEP -> new Spec(0.85F, 3.2, 85, 6, 0.10, 0.38, 4);
            case DASH_STRIKE -> new Spec(1.7F, 4.8, 28, 1, 1.0, 0.16, 8);
            case SPIKE -> new Spec(1.9F, 3.6, 50, 1, 0.0, 0.0, 6);
            default -> new Spec(1.0F, 3.2, 26, 1, 0.3, 0.0, 0);
        };
    }

    private static boolean heavy(MoveType m) {
        return m == MoveType.ROUNDHOUSE || m == MoveType.UPPERCUT || m == MoveType.DASH_STRIKE
                || m == MoveType.SPIKE || m == MoveType.SPIN_KICK || m == MoveType.AXE_KICK
                || m == MoveType.WHIRLWIND || m == MoveType.FLYING_KICK || m == MoveType.HEEL_STOMP
                || m == MoveType.BACKFLIP || m == MoveType.FRONTFLIP || m == MoveType.AIR_TWIST;
    }

    private static MoveType chooseMove(ServerPlayer p, CombatState s, LivingEntity primary) {
        boolean primaryAirborne = primary != null && !primary.onGround() && !primary.isInWater();
        boolean airborne = !p.onGround() && !p.isInWater();
        boolean sprint = p.isSprinting();
        boolean sneak = p.isShiftKeyDown();
        float pitch = p.getXRot();

        // Aerial premium
        if (airborne) {
            if (pitch > 40F) return MoveType.HEEL_STOMP;
            if (pitch < -30F) return MoveType.FRONTFLIP;
            if (sneak) return MoveType.BACKFLIP;
            if (sprint) return MoveType.FLYING_KICK;
            if (s.combo % 3 == 2) return MoveType.AIR_TWIST;
            return primaryAirborne ? MoveType.SPIKE : MoveType.FLYING_KICK;
        }

        // Ground context
        if (sprint && !primaryAirborne) {
            if (s.combo % 4 == 3) return MoveType.SPIN_KICK;
            return MoveType.DASH_STRIKE;
        }
        if (pitch < -40F && !primaryAirborne) return MoveType.UPPERCUT;
        if (pitch > 50F) return MoveType.AXE_KICK;
        if (sneak) {
            if (s.combo % 2 == 0) return MoveType.SWEEP;
            return MoveType.CARTWHEEL;
        }

        // Extended stylish chain
        MoveType[] chain = {
                MoveType.JAB, MoveType.CROSS, MoveType.HOOK, MoveType.ELBOW, MoveType.KNEE,
                MoveType.TWIST_PUNCH, MoveType.SIDE_KICK, MoveType.SPINNING_BACKFIST,
                MoveType.DOUBLE_PALM, MoveType.ROUNDHOUSE, MoveType.SPIN_KICK, MoveType.WHIRLWIND
        };
        MoveType m = chain[s.combo % chain.length];
        if ((m == MoveType.ROUNDHOUSE || m == MoveType.SPIN_KICK) && primaryAirborne) return MoveType.SPIKE;
        return m;
    }

    public static void tryAttack(ServerPlayer player, int lockId) {
        if (!player.isAlive() || player.isSpectator() || player.isSleeping()) return;

        CombatState s = CombatState.of(player);
        int now = player.server.getTickCount();
        if (now < s.attackReady || s.busy() || s.slam != CombatState.Slam.NONE) return;

        SpecialMoves.dropGuard(player, s);

        ServerLevel level = player.serverLevel();
        LivingEntity lock = TargetFinder.byId(player, lockId, MayhemConfig.BLITZ_RANGE.get() + 6.0);
        boolean lockSpecial = lock != null && TargetFinder.validForSpecials(player, lock, false);

        if (now > s.comboUntil) {
            s.combo = 0;
            s.juggle = 0;
        }

        LivingEntity primary = lock;
        if (primary == null) {
            List<LivingEntity> quick = TargetFinder.find(player, 3.6, 40, 1);
            primary = quick.isEmpty() ? null : quick.get(0);
        }

        MoveType move = chooseMove(player, s, primary);
        Spec sp = spec(move);
        double reachScale = (MayhemConfig.BASE_REACH.get() + PlayerProgression.getReachBonus(player)) / 3.2;
        double reach = sp.reach() * reachScale;

        // ---- lock-on gap closer ("blitz"): flash-step next to the target, then strike ----
        boolean blitzed = false;
        if (lockSpecial && now >= s.blitzReady && MayhemConfig.BLITZ_RANGE.get() > 0) {
            double dist = TargetFinder.distance(player, lock);
            if (dist > reach * 0.85 && dist <= MayhemConfig.BLITZ_RANGE.get()) {
                Vec3 spot = Teleports.findSpot(level, player, lock, Teleports.angleToPlayer(player, lock),
                        new double[]{0, 20, -20, 45, -45, 90, -90, 180}, new double[]{1.7, 2.3, 3.0});
                if (spot != null) {
                    Teleports.blink(player, spot, lock.getBoundingBox().getCenter());
                    s.blitzReady = now + 8;
                    blitzed = true;
                    Fx.impactTo(player, spot, 0.1F, 6F, ImpactPacket.FLASH_NONE, Fx.TXT_NONE, 7);
                }
            }
        }

        // ---- startup: animation, swing, whoosh, dash impulse, destruction ----
        s.attackReady = now + MayhemConfig.ATTACK_COOLDOWN_TICKS.get() + sp.extraCd();
        Fx.anim(player, move, s.combo & 1);
        player.swing(InteractionHand.MAIN_HAND, true);
        Fx.sound(level, player.position(), heavy(move) ? MayhemSounds.WHOOSH_HEAVY.get()
                : MayhemSounds.WHOOSH_LIGHT.get(), heavy(move) ? 0.8F : 0.6F, 0.95F + level.random.nextFloat() * 0.2F);

        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize();

        if (move == MoveType.DASH_STRIKE && !blitzed) {
            player.setDeltaMovement(flat.x * 1.25, 0.1, flat.z * 1.25);
            player.hurtMarked = true;
            player.fallDistance = 0F;
            Fx.trail(level, player.position(), player.position().add(flat.scale(4.0)));
            Fx.impactTo(player, player.position(), 0.12F, 6F, ImpactPacket.FLASH_NONE, Fx.TXT_NONE, 8);
        }
        if (move == MoveType.ROUNDHOUSE || move == MoveType.DASH_STRIKE) {
            Destruction.breakCone(player, reach, move == MoveType.ROUNDHOUSE ? 95 : 35,
                    MayhemConfig.WEAK_HARDNESS.get(), new Destruction.Budget());
        }

        // ---- targets: cone + soft lock ----
        List<LivingEntity> targets = TargetFinder.find(player, reach, sp.cone(), sp.max());
        if (lock != null && !targets.contains(lock) && TargetFinder.distance(player, lock) <= reach
                && TargetFinder.hasLineOfSight(player, lock)) {
            targets.add(0, lock);
            while (targets.size() > sp.max()) targets.remove(targets.size() - 1);
        }
        if (targets.isEmpty()) {
            Fx.sound(level, player.position(), net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_NODAMAGE, 0.4F, 1.4F);
            return;
        }

        boolean landed = false;
        LivingEntity first = null;
        for (int i = 0; i < targets.size(); i++) {
            LivingEntity t = targets.get(i);
            if (hit(player, s, t, move, sp, now, i)) {
                landed = true;
                if (first == null) first = t;
            }
        }
        if (!landed) return;

        // ---- post-hit bookkeeping ----
        if (move.isChain()) s.combo = (s.combo + 1) % 5;
        s.comboUntil = now + MayhemConfig.COMBO_WINDOW_TICKS.get();
        if (now <= s.counterUntil) s.counterUntil = -1;
        CombatState.addMomentum(player, s, heavy(move) ? 12F : 6F);

        if (move == MoveType.UPPERCUT) {
            player.setDeltaMovement(flat.x * 0.1, 0.46, flat.z * 0.1);
            player.hurtMarked = true;
        }
        if (move == MoveType.SPIKE) {
            player.setDeltaMovement(0, -1.2, 0);
            player.hurtMarked = true;
        }
        if (move == MoveType.ROUNDHOUSE || move == MoveType.DASH_STRIKE || move == MoveType.SPIKE) {
            PlayerProgression.recordFinisher(player);
        }

        if (MayhemConfig.SHOW_HIT_INFO.get() && first != null) {
            player.displayClientMessage(Component.translatable("message.mayhem.hit",
                    Component.translatable(move.langKey()),
                    s.combo == 0 ? 5 : s.combo,
                    first.getDisplayName(),
                    MobLevelHandler.getLevel(first),
                    Math.max(0, (int) Math.ceil(first.getHealth())),
                    (int) Math.ceil(first.getMaxHealth())), true);
        }
    }

    private static boolean hit(ServerPlayer p, CombatState s, LivingEntity t, MoveType move, Spec sp,
                               int now, int index) {
        ServerLevel level = p.serverLevel();
        boolean airborne = !t.onGround() && !t.isInWater();
        boolean isHeavy = heavy(move);

        float mult = sp.dmg();
        if (now <= s.counterUntil) mult *= 1.6F;
        if (airborne && move.isChain()) mult *= 1.1F;
        if (index > 0) mult *= 0.75F;
        float momentumBonus = 1F + s.momentum / 100F * 0.25F;
        float damage = Math.max(1F, baseDamage(p) * mult * momentumBonus
                * MayhemConfig.DAMAGE_SCALE.get().floatValue());

        t.invulnerableTime = 0; // every landed strike must connect so combos feel consistent
        if (!t.hurt(p.damageSources().playerAttack(p), damage)) return false;

        double kb = sp.knock() * MayhemConfig.KNOCKBACK_SCALE.get();
        double lift = sp.lift() * Math.max(0.3, MayhemConfig.KNOCKBACK_SCALE.get());
        boolean juggling = airborne && move.isChain() && s.juggle < 6;
        if (juggling) {
            lift = Math.max(lift, 0.30);
            kb *= 0.3;
            s.juggle++;
        }
        if (kb > 0) {
            t.knockback(kb, p.getX() - t.getX(), p.getZ() - t.getZ());
        }
        if (lift > 0) {
            Vec3 m = t.getDeltaMovement();
            t.setDeltaMovement(m.x, Math.max(m.y, lift), m.z);
        }
        t.hurtMarked = true;

        if (move == MoveType.SWEEP) {
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        }

        if (move == MoveType.SPIKE) {
            Dynamics.spike(t, p);
            Fx.anim(t, MoveType.TUMBLE, 0);
        } else if (kb >= 0.6 || lift >= 0.6) {
            Dynamics.launch(t, p, lift >= 0.6);
            Fx.anim(t, MoveType.TUMBLE, 0);
        } else {
            if (airborne && move.isChain()) Dynamics.suspend(t, 7);
            Fx.anim(t, MoveType.JOLT, s.combo & 1);
        }

        // ---- impact feedback ----
        Vec3 c = t.position().add(0, t.getBbHeight() * 0.55, 0);
        Fx.sound(level, c, isHeavy ? MayhemSounds.HIT_HEAVY.get() : MayhemSounds.HIT_LIGHT.get(),
                isHeavy ? 1.0F : 0.8F, 0.9F + 0.06F * s.combo + level.random.nextFloat() * 0.1F);
        if (isHeavy) {
            Fx.particle(level, ParticleTypes.EXPLOSION, c, 1, 0, 0);
            Fx.particle(level, ParticleTypes.CRIT, c, 16, 0.35, 0.25);
            Fx.particle(level, ParticleTypes.SWEEP_ATTACK, c, 1, 0, 0);
        } else {
            Fx.particle(level, ParticleTypes.CRIT, c, 6, 0.25, 0.15);
            Fx.particle(level, ParticleTypes.DAMAGE_INDICATOR, c, 3, 0.2, 0.1);
        }

        float trauma;
        float fov;
        byte flash = ImpactPacket.FLASH_NONE;
        byte text = Fx.TXT_NONE;
        int lines = 0;
        switch (move) {
            case JAB -> { trauma = 0.08F; fov = 1.0F; }
            case CROSS -> { trauma = 0.12F; fov = 1.5F; }
            case HOOK -> { trauma = 0.18F; fov = 2.0F; text = level.random.nextInt(3) == 0 ? Fx.TXT_CRACK : Fx.TXT_NONE; }
            case KNEE -> { trauma = 0.2F; fov = 2.0F; }
            case ROUNDHOUSE -> { trauma = 0.5F; fov = 5.0F; flash = ImpactPacket.FLASH_WHITE; text = Fx.TXT_DON; lines = 6; }
            case UPPERCUT -> { trauma = 0.4F; fov = 4.0F; text = Fx.TXT_BAM; lines = 6; }
            case DASH_STRIKE -> { trauma = 0.45F; fov = 6.0F; text = Fx.TXT_WHAM; lines = 8; }
            case SWEEP -> { trauma = 0.18F; fov = 2.0F; }
            default -> { trauma = 0.3F; fov = 3.0F; }
        }
        if (index == 0) {
            Fx.impact(level, c, trauma, fov, 14F, flash, text, lines);
        }

        // ---- rewards ----
        if (t instanceof Enemy) {
            PlayerProgression.awardCombat(p, Math.max(1, (int) damage));
            if (t.isDeadOrDying()) {
                PlayerProgression.awardDefeat(p, t);
                maybeFinisherCinematic(p, s, t, isHeavy, now);
            }
        }

        // ---- hit-stop (must come last: it restores the launch velocity set above) ----
        if (isHeavy && MayhemConfig.HIT_STOP.get() && !t.isDeadOrDying()) {
            Dynamics.freeze(t, 3);
        }
        return true;
    }

    private static void maybeFinisherCinematic(ServerPlayer p, CombatState s, LivingEntity t,
                                               boolean heavyHit, int now) {
        if (!heavyHit || now - s.lastFinisherCinematic < 600) return;
        if (MobLevelHandler.getLevel(t) < 3 && t.getMaxHealth() < 40F) return;
        s.lastFinisherCinematic = now;
        MayhemNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                new CinematicPacket(CinematicPacket.START, CinematicPacket.FINISHER, p.getId(), t.getId(),
                        CinematicPacket.OPEN));
        Fx.impactTo(p, t.position().add(0, 1, 0), 0.7F, 8F, ImpactPacket.FLASH_WHITE, Fx.TXT_BOOM, 10);
    }
}
