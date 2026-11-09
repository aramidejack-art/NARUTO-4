package com.mayhem.shinobi.avatar;

import com.mayhem.fx.Fx;
import com.mayhem.shinobi.ability.BeastStages;
import com.mayhem.shinobi.beast.BeastEntity;
import com.mayhem.shinobi.data.Resources;
import com.mayhem.shinobi.data.ShinobiData;
import com.mayhem.shinobi.data.ShinobiState;
import com.mayhem.shinobi.identity.Guardian;
import com.mayhem.shinobi.npc.EscortClientEntity;
import com.mayhem.shinobi.npc.NpcEntity;
import com.mayhem.shinobi.registry.ShinobiRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Server side of the chakra avatars: spawns/removes the avatar body, and runs the avatar moveset:
 * stance (all fours / hind legs), punches, shoulder tackles and the roar pose for beast techniques.
 * Works for the hollow tailed-beast avatar (bond stage 5 with the cloak on) and the Mirror Eye Guardian.
 */
public final class AvatarManager {
    public static final int PUNCH_CD = 9;
    public static final int TACKLE_CD = 50;
    public static final int TACKLE_TICKS = 9;

    private AvatarManager() {}

    /** Which avatar form the player should currently wear: a beast id, "guardian", or "" for none. */
    public static String formFor(Player pl) {
        if (!(pl instanceof ServerPlayer p)) return "";
        CompoundTag t = ShinobiData.of(p);
        if (ShinobiData.cloak(t) && BeastStages.stage(t) >= 5 && !ShinobiData.beast(t).isEmpty()) return ShinobiData.beast(t);
        if (Guardian.remaining(t) > 0 && Guardian.available(t)) return "guardian";
        return "";
    }

    /** Chakra Mode skin form ("mode:<beast id>") or "" when not transformed. */
    public static String modeFor(Player pl) {
        if (!(pl instanceof ServerPlayer p)) return "";
        CompoundTag t = ShinobiData.of(p);
        return ChakraMode.active(t) ? "mode:" + ShinobiData.beast(t) : "";
    }

    public static String wanted(Player pl, String form) { return form.startsWith("mode:") ? modeFor(pl) : formFor(pl); }

    public static ChakraAvatarEntity find(ServerPlayer p) {
        List<ChakraAvatarEntity> l = p.serverLevel().getEntitiesOfClass(ChakraAvatarEntity.class, p.getBoundingBox().inflate(8), e -> p.getUUID().equals(e.ownerId()) && !e.isMode());
        return l.isEmpty() ? null : l.get(0);
    }

    public static void tick(ServerPlayer p, CompoundTag t, ShinobiState st, long now) {
        String form = formFor(p);
        if (now % 5 == 0) {
            ensure(p, t, form, false);
            ensure(p, t, modeFor(p), true);
        }
        ChakraMode.tick(p, t, st, now);
        if (form.isEmpty()) { st.tackleTicks = 0; return; }
        if (now % 10 == 0 && !"guardian".equals(form)) {
            // stance bonuses: fast and low on all fours, sturdy and heavy-handed on two legs
            if (t.getBoolean("avStand")) p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 15, 1, true, false, true));
            else p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 15, 2, true, false, true));
        }
        if (st.tackleTicks > 0) tackleTick(p, t, st);
    }

    private static void ensure(ServerPlayer p, CompoundTag t, String form, boolean mode) {
        List<ChakraAvatarEntity> mine = p.serverLevel().getEntitiesOfClass(ChakraAvatarEntity.class, p.getBoundingBox().inflate(8), e -> p.getUUID().equals(e.ownerId()) && e.isMode() == mode);
        for (int i = 1; i < mine.size(); i++) mine.get(i).discard();
        if (!form.isEmpty() && mine.isEmpty()) {
            ChakraAvatarEntity a = new ChakraAvatarEntity(ShinobiRegistry.AVATAR.get(), p.serverLevel());
            a.setOwner(p.getUUID());
            a.setForm(form);
            a.setPos(p.getX(), p.getY(), p.getZ());
            a.setStanding(t.getBoolean("avStand"));
            p.serverLevel().addFreshEntity(a);
            p.serverLevel().sendParticles(ParticleTypes.POOF, p.getX(), p.getY() + 1, p.getZ(), 40, 1.2, 1.2, 1.2, 0.08);
        } else if (!mine.isEmpty() && !mode) {
            ChakraAvatarEntity a = mine.get(0);
            a.setStage("guardian".equals(form) ? Math.max(1, t.getInt("guardActive")) : 5);
            if (a.standing() != t.getBoolean("avStand")) a.setStanding(t.getBoolean("avStand"));
        }
    }

    // ------------------------------------------------------------------ actions
    private static void msg(ServerPlayer p, String s) { p.displayClientMessage(Component.literal(s), true); }

    public static void stance(ServerPlayer p) {
        CompoundTag t = ShinobiData.of(p);
        if (formFor(p).isEmpty() || "guardian".equals(formFor(p))) { msg(p, "Stance changes need your tailed-beast avatar"); return; }
        boolean up = !t.getBoolean("avStand");
        t.putBoolean("avStand", up);
        ChakraAvatarEntity a = find(p);
        if (a != null) a.setStanding(up);
        msg(p, up ? "Standing: heavy punches, tougher hide" : "All fours: fast charges, shoulder tackles");
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.8F, up ? 0.8F : 1.1F);
    }

    public static boolean victim(ServerPlayer p, LivingEntity e) {
        return e != p && e.isAlive() && !(e instanceof Player) && !(e instanceof NpcEntity) && !(e instanceof com.mayhem.shinobi.npc.WandererEntity wd && wd.isPeaceful()) && !(e instanceof EscortClientEntity) && !(e instanceof TamableAnimal);
    }

    private static float baseDamage(ServerPlayer p, CompoundTag t, String form) {
        float d = "guardian".equals(form) ? 6F + 2.5F * Math.max(1, t.getInt("guardActive")) : 9F;
        return d + Math.min(8F, ShinobiData.bond(t) / 600F);
    }

    public static void punch(ServerPlayer p) {
        String form = formFor(p);
        if (form.isEmpty()) return;
        ShinobiState st = ShinobiState.of(p.getUUID());
        long now = p.server.getTickCount();
        if (now < st.punchCd || st.tackleTicks > 0) return;
        st.punchCd = now + PUNCH_CD;
        CompoundTag t = ShinobiData.of(p);
        boolean up = t.getBoolean("avStand") || "guardian".equals(form);
        st.punchSide ^= 1;
        ChakraAvatarEntity a = find(p);
        if (a != null) a.startAct(st.punchSide == 0 ? 1 : 2);
        double reach = ("guardian".equals(form) ? 5.0 : up ? 5.8 : 4.6);
        float dmg = baseDamage(p, t, form) * (up ? 1.3F : 1.0F);
        ServerLevel sl = p.serverLevel();
        Vec3 look = p.getLookAngle().multiply(1, 0.35, 1).normalize();
        Vec3 side = new Vec3(-look.z, 0, look.x).scale(st.punchSide == 0 ? 1.4 : -1.4);
        Vec3 fist = p.position().add(0, 1.4, 0).add(look.scale(reach * 0.75)).add(side.scale(0.4));
        boolean any = false;
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(reach), x -> victim(p, x))) {
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(p.getEyePosition());
            double dist = to.length();
            if (dist > reach + e.getBbWidth() * 0.5 || to.normalize().dot(p.getLookAngle()) < 0.45) continue;
            e.invulnerableTime = 0;
            if (e.hurt(p.damageSources().playerAttack(p), dmg)) {
                Vec3 push = look.scale(0.9).add(0, 0.28, 0);
                e.setDeltaMovement(e.getDeltaMovement().add(push));
                e.hurtMarked = true;
                any = true;
            }
        }
        sl.sendParticles(any ? ParticleTypes.CRIT : ParticleTypes.SWEEP_ATTACK, fist.x, fist.y, fist.z, any ? 18 : 2, 0.5, 0.5, 0.5, 0.2);
        sl.playSound(null, p.blockPosition(), any ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.1F, any ? 0.7F : 1.0F);
        if (any) Fx.impact(sl, fist, 0.22F, 0.015F, 12F);
    }

    public static void tackle(ServerPlayer p) {
        String form = formFor(p);
        if (form.isEmpty()) return;
        ShinobiState st = ShinobiState.of(p.getUUID());
        long now = p.server.getTickCount();
        if (now < st.tackleCd) { msg(p, "Tackle recovering"); return; }
        CompoundTag t = ShinobiData.of(p);
        if (!Resources.spendChakra(p, t, 10)) { msg(p, "Tackle needs 10 chakra"); return; }
        st.tackleCd = now + TACKLE_CD;
        st.tackleTicks = TACKLE_TICKS;
        st.tackleHit.clear();
        ChakraAvatarEntity a = find(p);
        if (a != null) a.startAct(3);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.RAVAGER_ATTACK, SoundSource.PLAYERS, 1.2F, 0.7F);
    }

    private static void tackleTick(ServerPlayer p, CompoundTag t, ShinobiState st) {
        st.tackleTicks--;
        String form = formFor(p);
        boolean fours = !t.getBoolean("avStand") && !"guardian".equals(form);
        Vec3 dir = p.getLookAngle().multiply(1, 0, 1).normalize();
        double speed = fours ? 1.55 : 1.15;
        p.setDeltaMovement(dir.x * speed, Math.max(p.getDeltaMovement().y, p.onGround() ? 0.06 : -0.1), dir.z * speed);
        p.hurtMarked = true;
        p.fallDistance = 0;
        ServerLevel sl = p.serverLevel();
        sl.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.2, p.getZ(), 4, 0.6, 0.1, 0.6, 0.02);
        float dmg = baseDamage(p, t, form) * (fours ? 1.5F : 1.1F);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(2.2, 0.8, 2.2), x -> victim(p, x))) {
            if (!st.tackleHit.add(e.getId())) continue;
            e.invulnerableTime = 0;
            if (e.hurt(p.damageSources().playerAttack(p), dmg)) {
                e.setDeltaMovement(e.getDeltaMovement().add(dir.scale(1.7)).add(0, 0.45, 0));
                e.hurtMarked = true;
                Fx.impact(sl, e.position(), 0.45F, 0.03F, 16F);
                sl.sendParticles(ParticleTypes.EXPLOSION, e.getX(), e.getY() + 1, e.getZ(), 1, 0, 0, 0, 0);
            }
        }
    }

    /** Called when the player successfully casts a beast/guardian technique: plays the roar/cast pose. */
    public static void onCast(ServerPlayer p) {
        if (formFor(p).isEmpty()) return;
        ChakraAvatarEntity a = find(p);
        if (a != null && a.act() == 0) a.startAct(4);
    }

    /** Beast bond stage helper for the wild beast entity (unused by avatar, kept for symmetry with BeastEntity poses). */
    static boolean isWild(LivingEntity e) { return e instanceof BeastEntity; }
}
