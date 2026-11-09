package com.mayhem.shinobi.ability;

import com.mayhem.combat.TargetFinder;
import com.mayhem.progression.PlayerProgression;
import com.mayhem.shinobi.ShinobiConfig;
import com.mayhem.shinobi.beast.BeastBond;
import com.mayhem.shinobi.data.Resources;
import com.mayhem.shinobi.data.ShinobiData;
import com.mayhem.shinobi.data.ShinobiState;
import com.mayhem.shinobi.data.Style;
import com.mayhem.shinobi.identity.Passives;
import com.mayhem.shinobi.net.ShinobiNet;
import com.mayhem.shinobi.story.MissionTracker;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;

/** Server-authoritative cast pipeline: validation, costs, cooldowns, wind-up, execution, mastery. */
public final class AbilityCaster {
    private AbilityCaster() {}

    private static void msg(ServerPlayer p, String s) { p.displayClientMessage(Component.literal(s), true); }

    public static void castSlot(ServerPlayer p, int slot, int lockId) {
        CompoundTag t = ShinobiData.of(p);
        String id = ShinobiData.slot(t, slot);
        if (id.isEmpty()) { msg(p, "Slot " + (slot + 1) + " is empty - open the Shinobi menu (M) to assign a jutsu"); return; }
        castId(p, id, lockId);
    }

    public static void castId(ServerPlayer p, String id, int lockId) {
        if (!p.isAlive() || p.isSpectator() || p.isSleeping()) return;
        AbilityDef def = Abilities.get(id);
        if (def == null) return;
        CompoundTag t = ShinobiData.of(p);
        ShinobiState st = ShinobiState.of(p.getUUID());
        long now = p.server.getTickCount();

        if (!ShinobiData.owns(t, id)) { msg(p, "You haven't learned " + def.name); return; }
        String miss = Unlocks.missing(def, t, PlayerProgression.getLevel(p));
        if (miss != null) { msg(p, miss); return; }
        Style style = ShinobiData.style(t);
        if (def.style == StyleReq.TAI && style != Style.TAIJUTSU) { msg(p, def.name + " needs Taijutsu style (press F)"); return; }
        if (def.style == StyleReq.NIN && style != Style.NINJUTSU) { msg(p, def.name + " needs Ninjutsu style (press F)"); return; }
        if (now < st.gcdUntil) return;
        if (!st.casting.isEmpty()) return;
        Long ready = st.cooldowns.get(id);
        if (ready != null && now < ready) { msg(p, def.name + " cooling down (" + (ready - now + 19) / 20 + "s)"); return; }

        Variant v = def.variant(ShinobiData.variant(t, id));
        int mastery = ShinobiData.mastery(t, id);
        if (v != null && mastery < v.needMastery()) v = null;

        double discount = 1.0 - Math.min(mastery / 300.0, 0.2);
        double vc = v == null ? 1.0 : v.costMul();
        double scale = ShinobiConfig.COST_SCALE.get() * Passives.costMul(t, def) * vc * discount;
        int chakra = (int) Math.ceil(def.chakra * scale), stamina = (int) Math.ceil(def.stamina * scale);
        if (Resources.chakra(t) < chakra) { msg(p, "Not enough chakra (" + chakra + ")"); return; }
        if (Resources.stamina(t) < stamina) { msg(p, "Not enough stamina (" + stamina + ")"); return; }
        Resources.spendChakra(p, t, chakra);
        Resources.spendStamina(p, t, stamina);

        int cd = (int) Math.max(5, def.cooldown * ShinobiConfig.COOLDOWN_SCALE.get() * Passives.cooldownMul(t, def) * (v == null ? 1.0 : v.cdMul()));
        LivingEntity lock = lockId >= 0 && p.level().getEntity(lockId) instanceof LivingEntity le && TargetFinder.valid(p, le) ? le : null;
        final Variant fv = v;
        int windup = fv != null && fv.flag() == Flag.QUICK ? 0 : def.castTicks;
        st.gcdUntil = now + 4;

        if (windup > 0) {
            int token = ++st.castToken;
            st.casting = id;
            handSigns(p, windup);
            final int refundC = chakra;
            com.mayhem.shinobi.ability.Tasks.later(p.server, windup, () -> {
                if (st.castToken != token || !p.isAlive()) { st.casting = ""; Resources.addChakra(p, t, refundC * 0.5F); return; }
                st.casting = "";
                execute(p, def, fv, lock, cd);
            });
        } else {
            execute(p, def, fv, lock, cd);
        }
        ShinobiNet.sendResources(p, true);
    }

    private static void handSigns(ServerPlayer p, int ticks) {
        com.mayhem.shinobi.ability.Tasks.every(p.server, 2, Math.max(1, ticks / 2), i -> {
            if (p.isAlive()) p.serverLevel().sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1.2, p.getZ(), 6, 0.4, 0.3, 0.4, 0.2);
        });
    }

    private static void execute(ServerPlayer p, AbilityDef def, Variant v, LivingEntity lock, int cd) {
        ShinobiState st = ShinobiState.of(p.getUUID());
        long now = p.server.getTickCount();
        CompoundTag t = ShinobiData.of(p);
        st.cooldowns.put(def.id, now + cd);
        CastContext ctx = new CastContext(p, def, v, lock);
        def.behavior.cast(ctx);
        if (def.category == Category.BEAST) com.mayhem.shinobi.avatar.AvatarManager.onCast(p);

        // mastery: only counts with something to practise on, and no faster than the configured interval (anti-AFK)
        Long last = st.lastMasteryCount.get(def.id);
        boolean target = def.damage <= 0 || !p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(24), e -> e instanceof Enemy && e.isAlive()).isEmpty();
        if (target && (last == null || now - last >= ShinobiConfig.MASTERY_COUNT_INTERVAL_TICKS.get())) {
            st.lastMasteryCount.put(def.id, now);
            int before = ShinobiData.mastery(t, def.id);
            ShinobiData.addMastery(t, def.id, Math.max(1, (int) Math.round(ShinobiConfig.XP_SCALE.get())));
            for (Variant vv : def.variants) {
                if (before < vv.needMastery() && ShinobiData.mastery(t, def.id) >= vv.needMastery()) {
                    ShinobiNet.banner(p, 3, "Variant unlocked", def.name + ": " + vv.name());
                }
            }
        }
        if (def.category == Category.BEAST) BeastBond.addBond(p, 2);
        MissionTracker.onCast(p, def.id);
        ShinobiNet.ack(p, def.id, cd);
        ShinobiNet.syncTag(p);
    }

    /** Interrupts a pending wind-up (called when the caster is hurt). */
    public static void interrupt(ServerPlayer p) {
        ShinobiState st = ShinobiState.peek(p.getUUID());
        if (st == null || st.casting.isEmpty()) return;
        AbilityDef def = Abilities.get(st.casting);
        if (def != null && !def.interruptible) return;
        st.castToken++;
        msg(p, "Cast interrupted!");
    }
}
