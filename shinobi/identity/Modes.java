package com.mayhem.shinobi.identity;

import com.mayhem.shinobi.data.Resources;
import com.mayhem.shinobi.data.ShinobiData;
import com.mayhem.shinobi.data.ShinobiState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;

/** Bloodline modes: a toggled state with chakra drain and its own effects. */
public final class Modes {
    private Modes() {}

    public static void toggle(ServerPlayer p) {
        CompoundTag t = ShinobiData.of(p);
        String bl = ShinobiData.bloodline(t);
        if (!ShinobiData.mode(t).isEmpty()) {
            t.putString("mode", "");
            p.displayClientMessage(Component.literal("Mode ended"), true);
            return;
        }
        Bloodline b = Bloodlines.get(bl);
        if (b == null) { p.displayClientMessage(Component.literal("Equip a bloodline first (M > Bloodlines)"), true); return; }
        if (Resources.chakra(t) < 20) { p.displayClientMessage(Component.literal("Need 20 chakra to enter " + b.modeName()), true); return; }
        t.putString("mode", bl);
        p.displayClientMessage(Component.literal(b.modeName() + " activated"), true);
        p.serverLevel().sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX(), p.getY() + 1, p.getZ(), 30, 0.6, 0.8, 0.6, 0.08);
    }

    public static void tick(ServerPlayer p, CompoundTag t, ShinobiState st) {
        String mode = ShinobiData.mode(t);
        if (mode.isEmpty()) return;
        long now = p.server.getTickCount();
        Bloodline b = Bloodlines.get(mode);
        if (b == null || !mode.equals(ShinobiData.bloodline(t))) { t.putString("mode", ""); return; }
        if (now % 20 == 0) {
            if (!Resources.spendChakra(p, t, b.modeDrainPerSec())) {
                t.putString("mode", "");
                p.displayClientMessage(Component.literal(b.modeName() + " fades - out of chakra"), true);
                return;
            }
        }
        if (now % 10 != 0) return;
        var level = p.serverLevel();
        switch (mode) {
            case "mirror_eye" -> {
                p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, false, false));
                level.sendParticles(ParticleTypes.SOUL, p.getX(), p.getY() + 1.7, p.getZ(), 2, 0.2, 0.1, 0.2, 0.01);
            }
            case "gentle_sight" -> {
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(24), x -> x != p)) {
                    e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false));
                }
            }
            case "ice_release" -> {
                for (Monster m : level.getEntitiesOfClass(Monster.class, p.getBoundingBox().inflate(6))) {
                    m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, false, false));
                    level.sendParticles(ParticleTypes.SNOWFLAKE, m.getX(), m.getY() + 1, m.getZ(), 3, 0.3, 0.4, 0.3, 0.01);
                }
                level.sendParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 1, p.getZ(), 6, 1.2, 0.6, 1.2, 0.01);
            }
            case "wood_release" -> {
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 30, 0, false, false));
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1, p.getZ(), 4, 0.5, 0.6, 0.5, 0.01);
            }
            case "shadow_veil" , "shadow_weave" -> {
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 0, false, false));
                if (p.isShiftKeyDown()) p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 30, 0, false, false));
                level.sendParticles(ParticleTypes.SMOKE, p.getX(), p.getY() + 0.8, p.getZ(), 5, 0.4, 0.6, 0.4, 0.01);
            }
            case "lava_release" -> {
                p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 30, 0, false, false));
                for (Monster m : level.getEntitiesOfClass(Monster.class, p.getBoundingBox().inflate(3))) {
                    m.setSecondsOnFire(3);
                    if (now % 20 == 0) m.hurt(p.damageSources().indirectMagic(p, p), 1.5F);
                }
                level.sendParticles(ParticleTypes.LAVA, p.getX(), p.getY() + 1, p.getZ(), 3, 0.6, 0.5, 0.6, 0.0);
            }
            default -> { }
        }
    }
}
