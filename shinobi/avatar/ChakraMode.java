package com.mayhem.shinobi.avatar;

import com.mayhem.fx.Fx;
import com.mayhem.shinobi.beast.BeastStages;
import com.mayhem.shinobi.beast.Beasts;
import com.mayhem.shinobi.data.Resources;
import com.mayhem.shinobi.data.ShinobiData;
import com.mayhem.shinobi.data.ShinobiState;
import com.mayhem.shinobi.net.ShinobiNet;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Beast Chakra Mode: a unique, much stronger transformation for each of the nine beasts. The player's body is wrapped in
 * glowing beast chakra (see ChakraAvatarRenderer), every chakra tail is drawn, strong potion-style buffs apply, a passive aura
 * hurts or helps nearby creatures, and two new techniques (keys P and U) become available.
 */
public final class ChakraMode {
    public static final int MIN_STAGE = 3;
    public static final int ENTER_COST = 40;

    private enum Kind { SLAM, DASH, BEAM, PULL, BUFF, HEAL, LEAP, MIST }

    /** a, b, c meaning depends on kind (see {@link #cast}). */
    private record Skill(String name, Kind kind, int cost, int cdTicks, float dmg, float a, float b, int fire) {}

    private record Buff(MobEffect effect, int amp) {}

    private record Def(String title, String passive, Buff[] buffs, Skill s1, Skill s2) {}

    private static Buff b(MobEffect e, int amp) { return new Buff(e, amp); }

    private static final java.util.Map<String, Def> DEFS = new java.util.HashMap<>();

    static {
        DEFS.put("sand_tanuki", new Def("Dune Mantle", "Sand armour: heavy damage reduction and a thick absorption shell",
                new Buff[]{b(MobEffects.DAMAGE_RESISTANCE, 2), b(MobEffects.ABSORPTION, 2), b(MobEffects.DAMAGE_BOOST, 1), b(MobEffects.REGENERATION, 0)},
                new Skill("Quicksand Maw", Kind.PULL, 25, 160, 7F, 11F, 2F, 0),
                new Skill("Dune Crash", Kind.SLAM, 35, 220, 16F, 7F, 0.9F, 0)));
        DEFS.put("blue_flame_cat", new Def("Azure Pyre Mantle", "Blue flame aura burns everything within 3 blocks",
                new Buff[]{b(MobEffects.MOVEMENT_SPEED, 2), b(MobEffects.FIRE_RESISTANCE, 0), b(MobEffects.DAMAGE_BOOST, 1), b(MobEffects.JUMP, 1), b(MobEffects.REGENERATION, 0)},
                new Skill("Pyre Dash", Kind.DASH, 25, 120, 13F, 1.7F, 7F, 6),
                new Skill("Blue Inferno", Kind.SLAM, 40, 240, 12F, 8F, 0.7F, 10)));
        DEFS.put("giant_turtle", new Def("Tide-Shell Mantle", "Shell of chakra: very high resistance, fast regeneration, breathe underwater",
                new Buff[]{b(MobEffects.DAMAGE_RESISTANCE, 2), b(MobEffects.REGENERATION, 1), b(MobEffects.ABSORPTION, 3), b(MobEffects.WATER_BREATHING, 0), b(MobEffects.DAMAGE_BOOST, 0)},
                new Skill("Shell Bulwark", Kind.BUFF, 30, 300, 0F, 100F, 4F, 0),
                new Skill("Tsunami Crush", Kind.SLAM, 40, 240, 14F, 9F, 1.4F, 0)));
        DEFS.put("lava_ape", new Def("Magma Mantle", "Magma aura scorches nearby enemies; immune to fire",
                new Buff[]{b(MobEffects.DAMAGE_BOOST, 3), b(MobEffects.DAMAGE_RESISTANCE, 1), b(MobEffects.FIRE_RESISTANCE, 0), b(MobEffects.ABSORPTION, 1)},
                new Skill("Magma Fist", Kind.BEAM, 30, 140, 18F, 9F, 1.3F, 8),
                new Skill("Eruption", Kind.SLAM, 45, 260, 20F, 7F, 1.3F, 8)));
        DEFS.put("horse_dolphin", new Def("Tidal Mantle", "Swim like a dolphin, run like a horse",
                new Buff[]{b(MobEffects.MOVEMENT_SPEED, 3), b(MobEffects.DOLPHINS_GRACE, 0), b(MobEffects.WATER_BREATHING, 0), b(MobEffects.JUMP, 2), b(MobEffects.DAMAGE_BOOST, 1)},
                new Skill("Riptide Charge", Kind.DASH, 25, 100, 11F, 2.0F, 8F, 0),
                new Skill("Surge Wave", Kind.BEAM, 35, 180, 10F, 16F, 1.8F, 0)));
        DEFS.put("six_slug", new Def("Veil Mantle", "Acid aura poisons nearby enemies; strong regeneration",
                new Buff[]{b(MobEffects.REGENERATION, 2), b(MobEffects.DAMAGE_RESISTANCE, 1), b(MobEffects.ABSORPTION, 1), b(MobEffects.DAMAGE_BOOST, 0)},
                new Skill("Acid Mist", Kind.MIST, 30, 180, 6F, 7F, 3F, 0),
                new Skill("Mending Veil", Kind.HEAL, 40, 300, 0F, 0.45F, 12F, 0)));
        DEFS.put("horned_beetle", new Def("Gale-Wing Mantle", "Glide on chakra wings: no fall damage, huge jumps",
                new Buff[]{b(MobEffects.MOVEMENT_SPEED, 2), b(MobEffects.JUMP, 3), b(MobEffects.SLOW_FALLING, 0), b(MobEffects.DAMAGE_BOOST, 1), b(MobEffects.DAMAGE_RESISTANCE, 0)},
                new Skill("Wing Burst", Kind.LEAP, 20, 80, 0F, 1.3F, 1.15F, 0),
                new Skill("Horn Lance", Kind.DASH, 30, 140, 15F, 1.9F, 9F, 0)));
        DEFS.put("ox_octopus", new Def("Deep-Maw Mantle", "Tentacles of chakra: brutal strength and a tough hide",
                new Buff[]{b(MobEffects.DAMAGE_BOOST, 2), b(MobEffects.DAMAGE_RESISTANCE, 1), b(MobEffects.ABSORPTION, 2), b(MobEffects.REGENERATION, 0)},
                new Skill("Tentacle Snare", Kind.PULL, 30, 160, 6F, 13F, 3F, 0),
                new Skill("Crushing Coil", Kind.SLAM, 40, 220, 22F, 5.5F, 0.6F, 0)));
        DEFS.put("nine_fox", new Def("Gilded Chakra Mode", "Golden chakra skin: top-tier speed, strength, resistance and regeneration",
                new Buff[]{b(MobEffects.MOVEMENT_SPEED, 2), b(MobEffects.DAMAGE_BOOST, 3), b(MobEffects.DAMAGE_RESISTANCE, 2), b(MobEffects.REGENERATION, 2),
                        b(MobEffects.JUMP, 2), b(MobEffects.ABSORPTION, 3), b(MobEffects.NIGHT_VISION, 0)},
                new Skill("Chakra Grasp", Kind.PULL, 35, 140, 11F, 15F, 4F, 0),
                new Skill("Tailed Roar", Kind.BEAM, 60, 300, 30F, 22F, 2.6F, 6)));
    }

    private ChakraMode() {}

    // ------------------------------------------------------------------ helpers
    private static void msg(ServerPlayer p, String s) { p.displayClientMessage(Component.literal(s), true); }

    public static boolean active(CompoundTag t) { return t.getBoolean("beastMode") && !ShinobiData.beast(t).isEmpty(); }

    private static DustParticleOptions dust(String id, float size) {
        Beasts.Beast be = Beasts.get(id);
        int c = be == null ? 0xFFFFFF : be.color();
        return new DustParticleOptions(new Vector3f(((c >> 16) & 255) / 255F, ((c >> 8) & 255) / 255F, (c & 255) / 255F), size);
    }

    private static void eff(ServerPlayer p, MobEffect e, int amp, int dur) {
        MobEffectInstance cur = p.getEffect(e);
        if (cur == null || cur.getAmplifier() < amp || cur.getDuration() < dur / 2) p.addEffect(new MobEffectInstance(e, dur, amp, true, false, true));
    }

    private static float power(CompoundTag t) { return 1F + Math.min(1F, ShinobiData.bond(t) / 6000F); }

    // ------------------------------------------------------------------ toggle + tick
    public static void toggle(ServerPlayer p) {
        CompoundTag t = ShinobiData.of(p);
        String id = ShinobiData.beast(t);
        if (id.isEmpty()) { msg(p, "You have no beast bond"); return; }
        if (active(t)) { t.putBoolean("beastMode", false); msg(p, DEFS.get(id).title() + " released"); ShinobiNet.syncTag(p); return; }
        if (BeastStages.stage(t) < MIN_STAGE) { msg(p, "Chakra Mode needs Partial Manifestation (stage " + MIN_STAGE + ")"); return; }
        if (Resources.chakra(t) < ENTER_COST) { msg(p, "Need " + ENTER_COST + " chakra"); return; }
        Resources.spendChakra(p, t, ENTER_COST);
        t.putBoolean("beastMode", true);
        net.minecraft.nbt.CompoundTag b = t.getCompound("beast");
        b.putBoolean("cloak", true);
        t.put("beast", b);
        Def d = DEFS.get(id);
        ServerLevel sl = p.serverLevel();
        sl.sendParticles(dust(id, 2.2F), p.getX(), p.getY() + 1, p.getZ(), 120, 1.0, 1.2, 1.0, 0.1);
        sl.playSound(null, p.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.9F, 1.4F);
        Fx.impact(sl, p.position(), 0.5F, 0.03F, 20F);
        ShinobiNet.banner(p, 3, d.title(), d.passive());
        ShinobiNet.syncTag(p);
    }

    public static void tick(ServerPlayer p, CompoundTag t, ShinobiState st, long now) {
        if (st.dashTicks > 0) dashTick(p, st);
        if (!t.getBoolean("beastMode")) return;
        String id = ShinobiData.beast(t);
        Def d = DEFS.get(id);
        if (d == null || BeastStages.stage(t) < MIN_STAGE) { t.putBoolean("beastMode", false); ShinobiNet.syncTag(p); return; }
        if (now % 20 == 0 && !Resources.spendChakra(p, t, 5 + BeastStages.stage(t))) {
            t.putBoolean("beastMode", false);
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
            ShinobiNet.banner(p, 0, "Chakra exhausted", d.title() + " collapses");
            ShinobiNet.syncTag(p);
            return;
        }
        if (now % 10 != 0) return;
        for (Buff bf : d.buffs()) eff(p, bf.effect(), bf.amp(), 40);
        ServerLevel sl = p.serverLevel();
        sl.sendParticles(dust(id, 1.6F), p.getX(), p.getY() + 1, p.getZ(), 10, 0.5, 0.9, 0.5, 0.02);
        // passive auras
        switch (id) {
            case "blue_flame_cat", "lava_ape" -> {
                for (LivingEntity e : near(p, 3.2)) { e.setSecondsOnFire(3); e.hurt(p.damageSources().playerAttack(p), 1.5F * power(t)); }
                sl.sendParticles(id.equals("lava_ape") ? ParticleTypes.LAVA : ParticleTypes.SOUL_FIRE_FLAME, p.getX(), p.getY() + 0.5, p.getZ(), 8, 1.4, 0.4, 1.4, 0.02);
            }
            case "six_slug" -> {
                for (LivingEntity e : near(p, 3.5)) e.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1));
                sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, p.getX(), p.getY() + 1, p.getZ(), 10, 1.5, 0.8, 1.5, 0.0);
            }
            case "nine_fox" -> {
                if (now % 40 == 0) for (Player o : sl.getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(8), x -> x != p)) o.heal(1.0F);
            }
            default -> { }
        }
    }

    private static List<LivingEntity> near(ServerPlayer p, double r) {
        return p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r), e -> AvatarManager.victim(p, e));
    }

    // ------------------------------------------------------------------ techniques
    public static void cast(ServerPlayer p, int idx) {
        CompoundTag t = ShinobiData.of(p);
        if (!active(t)) { msg(p, "Enter Chakra Mode first (I)"); return; }
        Def d = DEFS.get(ShinobiData.beast(t));
        if (d == null) return;
        Skill s = idx == 0 ? d.s1() : d.s2();
        ShinobiState st = ShinobiState.of(p.getUUID());
        long now = p.server.getTickCount();
        if (now < st.skillCd[idx]) { msg(p, s.name() + " recovering (" + ((st.skillCd[idx] - now) / 20 + 1) + "s)"); return; }
        if (!Resources.spendChakra(p, t, s.cost())) { msg(p, s.name() + " needs " + s.cost() + " chakra"); return; }
        st.skillCd[idx] = now + s.cdTicks();
        ServerLevel sl = p.serverLevel();
        float dmg = s.dmg() * power(t);
        String id = ShinobiData.beast(t);
        Vec3 look = p.getLookAngle();
        switch (s.kind()) {
            case SLAM -> {
                float r = s.a();
                for (LivingEntity e : near(p, r)) {
                    e.invulnerableTime = 0;
                    if (e.hurt(p.damageSources().playerAttack(p), dmg)) {
                        Vec3 away = e.position().subtract(p.position()).multiply(1, 0, 1).normalize();
                        e.setDeltaMovement(e.getDeltaMovement().add(away.scale(1.1 * s.b() + 0.4)).add(0, 0.55 + 0.3 * s.b(), 0));
                        e.hurtMarked = true;
                        if (s.fire() > 0) e.setSecondsOnFire(s.fire());
                    }
                }
                for (int i = 0; i < 48; i++) {
                    double a = Math.PI * 2 * i / 48;
                    for (double k = 1; k <= r; k += r / 4) sl.sendParticles(dust(id, 2F), p.getX() + Math.cos(a) * k, p.getY() + 0.15, p.getZ() + Math.sin(a) * k, 1, 0.05, 0.05, 0.05, 0);
                }
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, p.getX(), p.getY() + 0.5, p.getZ(), 1, 0, 0, 0, 0);
                sl.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2F, 0.7F);
                Fx.impact(sl, p.position(), 0.7F, 0.04F, r * 3);
            }
            case DASH -> {
                st.dashTicks = (int) s.b();
                st.dashSpeed = s.a();
                st.dashDmg = dmg;
                st.dashFire = s.fire();
                st.tackleHit.clear();
                p.setDeltaMovement(look.x * s.a(), Math.max(0.1, look.y * s.a() * 0.4), look.z * s.a());
                p.hurtMarked = true;
                sl.playSound(null, p.blockPosition(), SoundEvents.RAVAGER_ATTACK, SoundSource.PLAYERS, 1F, 1.2F);
            }
            case BEAM -> {
                double len = s.a(), width = s.b();
                Vec3 from = p.getEyePosition().add(0, -0.3, 0);
                Vec3 to = from.add(look.scale(len));
                var hit = sl.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
                if (hit.getType() != HitResult.Type.MISS) to = hit.getLocation();
                double real = from.distanceTo(to);
                Set<Integer> done = new HashSet<>();
                for (double k = 0.5; k <= real; k += 0.5) {
                    Vec3 pos = from.add(look.scale(k));
                    sl.sendParticles(dust(id, (float) (1.2 + width)), pos.x, pos.y, pos.z, 2, width * 0.25, width * 0.25, width * 0.25, 0);
                    sl.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 1, 0.1, 0.1, 0.1, 0.02);
                    AABB box = new AABB(pos, pos).inflate(width);
                    for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box, x -> AvatarManager.victim(p, x))) {
                        if (!done.add(e.getId())) continue;
                        e.invulnerableTime = 0;
                        if (e.hurt(p.damageSources().playerAttack(p), dmg)) {
                            e.setDeltaMovement(e.getDeltaMovement().add(look.scale(1.4)).add(0, 0.3, 0));
                            e.hurtMarked = true;
                            if (s.fire() > 0) e.setSecondsOnFire(s.fire());
                        }
                    }
                }
                sl.sendParticles(ParticleTypes.EXPLOSION, to.x, to.y, to.z, 3, 0.5, 0.5, 0.5, 0);
                sl.playSound(null, p.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.1F, 0.9F);
                Fx.impact(sl, to, 0.5F, 0.03F, 24F);
            }
            case PULL -> {
                Vec3 c = p.position();
                for (LivingEntity e : near(p, s.a())) {
                    Vec3 to = c.subtract(e.position()).normalize();
                    e.setDeltaMovement(to.scale(1.5).add(0, 0.25, 0));
                    e.hurtMarked = true;
                    e.hurt(p.damageSources().playerAttack(p), dmg);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, (int) s.b()));
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1));
                    Vec3 mid = e.position().add(0, e.getBbHeight() * 0.5, 0);
                    for (int i = 1; i < 10; i++) {
                        Vec3 q = c.add(0, 1, 0).lerp(mid, i / 10.0);
                        sl.sendParticles(dust(id, 1.4F), q.x, q.y, q.z, 1, 0, 0, 0, 0);
                    }
                }
                sl.playSound(null, p.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 0.8F, 1.4F);
            }
            case BUFF -> {
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, (int) s.a(), 4, true, false, true));
                p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, (int) s.a() + 100, (int) s.b(), true, false, true));
                sl.sendParticles(dust(id, 2.4F), p.getX(), p.getY() + 1, p.getZ(), 80, 1, 1.2, 1, 0.05);
                sl.playSound(null, p.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.2F, 0.6F);
            }
            case HEAL -> {
                p.heal(p.getMaxHealth() * s.a());
                for (Player o : sl.getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(s.b()), x -> x != p)) o.heal(o.getMaxHealth() * s.a() * 0.7F);
                sl.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.5, p.getZ(), 12, 1.5, 0.8, 1.5, 0);
                sl.sendParticles(dust(id, 2F), p.getX(), p.getY() + 1, p.getZ(), 60, 1.5, 1, 1.5, 0.05);
                sl.playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1F, 1.3F);
            }
            case LEAP -> {
                p.setDeltaMovement(look.x * s.a(), s.b(), look.z * s.a());
                p.hurtMarked = true;
                p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 160, 0, true, false, true));
                sl.sendParticles(dust(id, 2F), p.getX(), p.getY(), p.getZ(), 60, 1, 0.2, 1, 0.1);
                sl.playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.2F, 1.2F);
            }
            case MIST -> {
                for (LivingEntity e : near(p, s.a())) {
                    e.hurt(p.damageSources().playerAttack(p), dmg);
                    e.addEffect(new MobEffectInstance(MobEffects.POISON, 160, (int) s.b()));
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
                }
                for (int i = 0; i < 60; i++) {
                    double a = Math.PI * 2 * i / 60, k = 1 + (i % 6);
                    sl.sendParticles(dust(id, 2.4F), p.getX() + Math.cos(a) * k, p.getY() + 0.5 + (i % 3) * 0.5, p.getZ() + Math.sin(a) * k, 1, 0.1, 0.1, 0.1, 0);
                }
                sl.playSound(null, p.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.4F, 0.6F);
            }
        }
        ShinobiNet.banner(p, 1, s.name(), d.title());
        AvatarManager.onCast(p);
    }

    private static void dashTick(ServerPlayer p, ShinobiState st) {
        st.dashTicks--;
        Vec3 dir = p.getLookAngle().multiply(1, 0, 1).normalize();
        p.setDeltaMovement(dir.x * st.dashSpeed, Math.max(p.getDeltaMovement().y, -0.1), dir.z * st.dashSpeed);
        p.hurtMarked = true;
        p.fallDistance = 0;
        ServerLevel sl = p.serverLevel();
        sl.sendParticles(dust(ShinobiData.beast(ShinobiData.of(p)), 2F), p.getX(), p.getY() + 1, p.getZ(), 6, 0.5, 0.6, 0.5, 0.02);
        for (LivingEntity e : near(p, 1.8)) {
            if (!st.tackleHit.add(e.getId())) continue;
            e.invulnerableTime = 0;
            if (e.hurt(p.damageSources().playerAttack(p), st.dashDmg)) {
                e.setDeltaMovement(e.getDeltaMovement().add(dir.scale(1.5)).add(0, 0.4, 0));
                e.hurtMarked = true;
                if (st.dashFire > 0) e.setSecondsOnFire(st.dashFire);
                Fx.impact(sl, e.position(), 0.35F, 0.02F, 12F);
            }
        }
    }

    public static String title(String beastId) { Def d = DEFS.get(beastId); return d == null ? "" : d.title(); }
    public static String skillName(String beastId, int idx) { Def d = DEFS.get(beastId); return d == null ? "" : (idx == 0 ? d.s1() : d.s2()).name(); }
}
