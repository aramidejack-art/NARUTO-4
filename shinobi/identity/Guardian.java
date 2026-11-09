package com.mayhem.shinobi.identity;

import com.mayhem.fx.Fx;
import com.mayhem.shinobi.data.Resources;
import com.mayhem.shinobi.data.ShinobiData;
import com.mayhem.shinobi.data.ShinobiState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Spectral Guardian: a staged chakra avatar for the Mirror Eye bloodline (adapted from the ShinobiCraft "Susanoo" prototype).
 * 1 Ribcage (guard) - 2 Skeletal (auto-slash) - 3 Armoured (spectral bow) - 4 Complete (Perfect Slash).
 */
public final class Guardian {
    public static final int[] REQ_LEVEL = {0, 8, 12, 20, 30};
    public static final int[] REQ_MASTERY = {0, 0, 40, 100, 180};
    public static final String[] NAMES = {"", "Ribcage", "Skeletal Guardian", "Armoured Guardian", "Complete Guardian"};
    public static final int DURATION = 600;

    private Guardian() {}

    public static int selected(CompoundTag t) { return Math.max(1, Math.min(4, t.getInt("guardStage"))); }
    public static int remaining(CompoundTag t) { return t.getInt("guardTicks"); }
    public static int mastery(CompoundTag t) { return t.getInt("guardMastery"); }
    public static boolean available(CompoundTag t) { return "mirror_eye".equals(ShinobiData.bloodline(t)); }

    private static void msg(ServerPlayer p, String s) { p.displayClientMessage(Component.literal(s), true); }

    public static void cycle(ServerPlayer p, int level) {
        CompoundTag t = ShinobiData.of(p);
        if (!available(t)) { msg(p, "Only the Mirror Eye bloodline can manifest a Guardian"); return; }
        if (remaining(t) > 0) { msg(p, "Dismiss the Guardian before changing stage"); return; }
        int next = selected(t) >= 4 ? 1 : selected(t) + 1;
        if (level < REQ_LEVEL[next] || mastery(t) < REQ_MASTERY[next]) {
            msg(p, NAMES[next] + " needs level " + REQ_LEVEL[next] + " and " + REQ_MASTERY[next] + " guardian mastery");
            return;
        }
        t.putInt("guardStage", next);
        msg(p, NAMES[next] + " selected");
    }

    public static void toggle(ServerPlayer p, int level) {
        CompoundTag t = ShinobiData.of(p);
        if (remaining(t) > 0) { t.putInt("guardTicks", 0); msg(p, "Guardian dismissed"); return; }
        if (!available(t)) { msg(p, "Only the Mirror Eye bloodline can manifest a Guardian"); return; }
        int st = selected(t);
        if (level < REQ_LEVEL[st] || mastery(t) < REQ_MASTERY[st]) { st = 1; t.putInt("guardStage", 1); }
        int cost = 30 + st * 10;
        if (level < REQ_LEVEL[1]) { msg(p, "Needs level " + REQ_LEVEL[1]); return; }
        if (!Resources.spendChakra(p, t, cost)) { msg(p, "Guardian needs " + cost + " chakra"); return; }
        t.putInt("guardActive", st);
        t.putInt("guardTicks", DURATION);
        msg(p, NAMES[st] + " manifests");
        p.serverLevel().sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX(), p.getY() + 1, p.getZ(), 60, 1.0, 1.4, 1.0, 0.1);
    }

    public static void tick(ServerPlayer p, CompoundTag t, ShinobiState st) {
        int ticks = remaining(t);
        if (ticks <= 0) return;
        long now = p.server.getTickCount();
        int stage = Math.max(1, t.getInt("guardActive"));
        if (!available(t)) { t.putInt("guardTicks", 0); return; }
        if (now % 20 == 0) {
            if (!Resources.spendChakra(p, t, 3 + stage * 2)) { t.putInt("guardTicks", 0); msg(p, "The Guardian fades - chakra spent"); return; }
            t.putInt("guardMastery", Math.min(10000, mastery(t) + 1));
        }
        t.putInt("guardTicks", ticks - 1);
        st.guardUntil = now + 4;
        st.guardReduction = Math.max(st.guardReduction, 0.2 + 0.1 * stage);
        ServerLevel sl = p.serverLevel();
        if (now % 5 == 0) shape(sl, p, stage, now);
        if (stage >= 2 && now % 40 == 0) slash(sl, p, stage);
        if (stage >= 3 && now % 60 == 0) bow(sl, p, stage);
        if (stage >= 4 && now % 100 == 0) perfect(sl, p);
    }

    private static void shape(ServerLevel sl, ServerPlayer p, int stage, long now) {
        double r = 1.4 + stage * 0.35;
        int ribs = 6 + stage * 3;
        for (int i = 0; i < ribs; i++) {
            double a = Math.PI * 2 * i / ribs + now * 0.03;
            double h = 0.4 + (i % 4) * 0.45 + stage * 0.15;
            sl.sendParticles(stage >= 3 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.END_ROD,
                    p.getX() + Math.cos(a) * r, p.getY() + h, p.getZ() + Math.sin(a) * r, 1, 0.02, 0.02, 0.02, 0);
        }
        if (stage >= 4) sl.sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 2.6, p.getZ(), 6, 0.6, 0.5, 0.6, 0.2);
    }

    private static List<Monster> near(ServerLevel sl, ServerPlayer p, double r) {
        return sl.getEntitiesOfClass(Monster.class, p.getBoundingBox().inflate(r), m -> m.isAlive());
    }

    private static void hurt(ServerPlayer p, LivingEntity e, float dmg) {
        e.invulnerableTime = 0;
        e.hurt(p.damageSources().indirectMagic(p, p), dmg);
    }

    private static void slash(ServerLevel sl, ServerPlayer p, int stage) {
        for (Monster m : near(sl, p, 5.5)) {
            hurt(p, m, 3 + stage * 2.5F);
            Vec3 d = m.position().subtract(p.position()).multiply(1, 0, 1).normalize();
            m.setDeltaMovement(m.getDeltaMovement().add(d.scale(0.5)).add(0, 0.2, 0));
            m.hurtMarked = true;
        }
        Fx.ring(sl, ParticleTypes.SWEEP_ATTACK, p.position().add(0, 1.2, 0), 1.5, 10, 0.2, 0);
    }

    private static void bow(ServerLevel sl, ServerPlayer p, int stage) {
        LivingEntity best = null; double bd = Double.MAX_VALUE;
        for (Monster m : near(sl, p, 20)) { double d = p.distanceToSqr(m); if (d < bd) { bd = d; best = m; } }
        if (best == null) return;
        Vec3 a = p.getEyePosition(), b = best.position().add(0, best.getBbHeight() * 0.5, 0);
        for (double d = 0; d < a.distanceTo(b); d += 0.4) {
            Vec3 pt = a.add(b.subtract(a).normalize().scale(d));
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pt.x, pt.y, pt.z, 1, 0.02, 0.02, 0.02, 0);
        }
        hurt(p, best, 6 + stage * 2F);
    }

    private static void perfect(ServerLevel sl, ServerPlayer p) {
        Fx.ring(sl, ParticleTypes.END_ROD, p.position().add(0, 1.0, 0), 1.0, 40, 0.9, 0.02);
        Fx.sound(sl, p.position(), com.mayhem.fx.MayhemSounds.BOOM.get(), 1.4F, 0.9F);
        Fx.impact(sl, p.position(), 0.7F, 0.04F, 30F);
        for (Monster m : near(sl, p, 9)) {
            hurt(p, m, 22F);
            Vec3 d = m.position().subtract(p.position()).multiply(1, 0, 1).normalize();
            m.setDeltaMovement(m.getDeltaMovement().add(d.scale(1.2)).add(0, 0.5, 0));
            m.hurtMarked = true;
        }
    }
}
