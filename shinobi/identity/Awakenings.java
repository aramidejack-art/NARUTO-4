package com.mayhem.shinobi.identity;

import com.mayhem.shinobi.data.Resources;
import com.mayhem.shinobi.data.ShinobiData;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Clan awakenings: timed, chakra-powered forms with two stages. Stage II needs level 20 and awakening mastery.
 * Adapted from the ShinobiCraft "Clans/Transformations" prototype, rebuilt on the shared data model.
 */
public final class Awakenings {
    public static final int S1_COST = 28, S2_COST = 48, S1_DUR = 600, S2_DUR = 500, S2_LEVEL = 20, S2_MASTERY = 120, MIN_LEVEL = 5;

    private Awakenings() {}

    public static int mastery(CompoundTag t) { return t.getInt("awakenMastery"); }
    public static int selected(CompoundTag t) { return t.getInt("awakenSel") == 2 ? 2 : 1; }
    public static int remaining(CompoundTag t) { return t.getInt("awakenTicks"); }
    public static boolean stage2(CompoundTag t, int level) { return level >= S2_LEVEL && mastery(t) >= S2_MASTERY; }

    private static void msg(ServerPlayer p, String s) { p.displayClientMessage(Component.literal(s), true); }

    public static void cycle(ServerPlayer p, int level) {
        CompoundTag t = ShinobiData.of(p);
        if (remaining(t) > 0) { msg(p, "End your awakening before changing stages"); return; }
        if (selected(t) == 1) {
            if (!stage2(t, level)) { msg(p, "Stage II needs level " + S2_LEVEL + " and " + S2_MASTERY + " awakening mastery"); return; }
            t.putInt("awakenSel", 2);
            msg(p, "Stage II selected: stronger, costlier, shorter");
        } else {
            t.putInt("awakenSel", 1);
            msg(p, "Stage I selected");
        }
    }

    public static void toggle(ServerPlayer p, int level) {
        CompoundTag t = ShinobiData.of(p);
        if (remaining(t) > 0) { t.putInt("awakenTicks", 0); msg(p, "Awakening ended"); return; }
        Clan clan = Clans.get(ShinobiData.clan(t));
        if (clan == null) { msg(p, "Choose a clan first (M > Clans)"); return; }
        if (level < MIN_LEVEL) { msg(p, "Awakenings unlock at level " + MIN_LEVEL); return; }
        int stage = selected(t);
        if (stage == 2 && !stage2(t, level)) { stage = 1; t.putInt("awakenSel", 1); }
        int cost = stage == 2 ? S2_COST : S1_COST;
        if (!Resources.spendChakra(p, t, cost)) { msg(p, "Awakening needs " + cost + " chakra"); return; }
        t.putInt("awakenActive", stage);
        t.putInt("awakenTicks", stage == 2 ? S2_DUR : S1_DUR);
        msg(p, clan.name() + " awakening - stage " + stage);
        p.serverLevel().sendParticles(particle(clan.id()), p.getX(), p.getY() + 1, p.getZ(), 40, 0.8, 0.9, 0.8, 0.08);
    }

    public static void tick(ServerPlayer p, CompoundTag t) {
        int ticks = remaining(t);
        if (ticks <= 0) return;
        Clan clan = Clans.get(ShinobiData.clan(t));
        if (clan == null) { t.putInt("awakenTicks", 0); return; }
        long now = p.server.getTickCount();
        if (now % 20 == 0) {
            if (!Resources.spendChakra(p, t, 2)) { t.putInt("awakenTicks", 0); msg(p, "Your awakening fades - chakra spent"); return; }
            t.putInt("awakenMastery", Math.min(10000, mastery(t) + 1));
        }
        t.putInt("awakenTicks", ticks - 1);
        int stage = Math.max(1, t.getInt("awakenActive"));
        if (now % 10 == 0) effects(p, clan.id(), stage);
        if (now % 5 == 0) p.serverLevel().sendParticles(particle(clan.id()), p.getX(), p.getY() + 1, p.getZ(), 5, 0.4, 0.55, 0.4, 0.025);
        if (ticks == 1) msg(p, "Awakening ended");
    }

    private static void eff(ServerPlayer p, MobEffect e, int amp) {
        if (amp < 0) return;
        p.addEffect(new MobEffectInstance(e, 15, amp, true, false, true));
    }

    private static void effects(ServerPlayer p, String clan, int stage) {
        int a = stage == 2 ? 2 : 1;
        switch (clan) {
            case "emberfang" -> { eff(p, MobEffects.DAMAGE_BOOST, a); eff(p, MobEffects.MOVEMENT_SPEED, a - 1); eff(p, MobEffects.FIRE_RESISTANCE, 0); }
            case "gentleveil" -> { eff(p, MobEffects.MOVEMENT_SPEED, a); eff(p, MobEffects.DAMAGE_RESISTANCE, stage - 1); eff(p, MobEffects.NIGHT_VISION, 0); }
            case "spiralborn" -> { eff(p, MobEffects.DAMAGE_RESISTANCE, stage - 1); eff(p, MobEffects.ABSORPTION, a); eff(p, MobEffects.DAMAGE_BOOST, a - 1); }
            case "shadoweave" -> { eff(p, MobEffects.MOVEMENT_SPEED, 0); eff(p, MobEffects.DAMAGE_RESISTANCE, stage - 1); eff(p, MobEffects.NIGHT_VISION, 0); }
            case "stonebrow" -> { eff(p, MobEffects.DAMAGE_BOOST, a); eff(p, MobEffects.DAMAGE_RESISTANCE, a); }
            case "windrider" -> { eff(p, MobEffects.MOVEMENT_SPEED, a + 1); eff(p, MobEffects.JUMP, a); eff(p, MobEffects.SLOW_FALLING, 0); }
            case "mistbane" -> { eff(p, MobEffects.MOVEMENT_SPEED, a); eff(p, MobEffects.WATER_BREATHING, 0); eff(p, MobEffects.DOLPHINS_GRACE, 0); }
            case "bonecrest" -> { eff(p, MobEffects.DAMAGE_BOOST, a); eff(p, MobEffects.DAMAGE_RESISTANCE, a - 1); }
            case "thunderfang" -> { eff(p, MobEffects.MOVEMENT_SPEED, a + 1); eff(p, MobEffects.DAMAGE_BOOST, a - 1); eff(p, MobEffects.DIG_SPEED, 0); }
            default -> { }
        }
    }

    private static ParticleOptions particle(String clan) {
        return switch (clan) {
            case "emberfang" -> ParticleTypes.FLAME;
            case "gentleveil" -> ParticleTypes.ELECTRIC_SPARK;
            case "spiralborn" -> ParticleTypes.SOUL_FIRE_FLAME;
            case "shadoweave" -> ParticleTypes.SMOKE;
            case "stonebrow" -> ParticleTypes.CRIT;
            case "windrider" -> ParticleTypes.CLOUD;
            case "mistbane" -> ParticleTypes.SPLASH;
            case "bonecrest" -> ParticleTypes.POOF;
            case "thunderfang" -> ParticleTypes.ELECTRIC_SPARK;
            default -> ParticleTypes.ENCHANT;
        };
    }
}
