package com.mayhem.shinobi.beast;

import com.mayhem.progression.PlayerProgression;
import com.mayhem.shinobi.ShinobiConfig;
import com.mayhem.shinobi.ability.BeastStages;
import com.mayhem.shinobi.beast.Beasts.Beast;
import com.mayhem.shinobi.data.Resources;
import com.mayhem.shinobi.data.ShinobiData;
import com.mayhem.shinobi.data.ShinobiState;
import com.mayhem.shinobi.identity.Passives;
import com.mayhem.shinobi.net.ShinobiNet;
import com.mayhem.shinobi.registry.ShinobiRegistry;
import com.mayhem.shinobi.story.MissionTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Jinchuriki progression: encounters, the sealing ritual, bond stages and the chakra cloak. */
public final class BeastBond {
    public static final int RITUAL_TICKS = 100;
    public static final int CHALLENGE_COOLDOWN = 12000;

    private BeastBond() {}

    private static void msg(ServerPlayer p, String s) { p.displayClientMessage(Component.literal(s), true); }

    private static CompoundTag beastTag(CompoundTag t) { return t.getCompound("beast"); }

    // ------------------------------------------------------------------ bond progression
    public static void addBond(ServerPlayer p, int amount) {
        CompoundTag t = ShinobiData.of(p);
        if (ShinobiData.beast(t).isEmpty() || amount <= 0) return;
        int before = BeastStages.stage(t);
        CompoundTag b = beastTag(t);
        b.putInt("bond", b.getInt("bond") + amount);
        t.put("beast", b);
        int after = BeastStages.stage(t);
        if (after > before) {
            t.putInt("ap", t.getInt("ap") + 2);
            ShinobiNet.banner(p, 3, "Bond deepens: " + BeastStages.NAMES[after], Beasts.get(ShinobiData.beast(t)).name() + " answers your call");
            p.serverLevel().sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX(), p.getY() + 1, p.getZ(), 60, 0.8, 1.0, 0.8, 0.1);
            ShinobiNet.syncTag(p);
        }
    }

    // ------------------------------------------------------------------ challenge
    public static void challenge(ServerPlayer p, String beastId) {
        CompoundTag t = ShinobiData.of(p);
        Beast b = Beasts.get(beastId);
        if (b == null) return;
        if (!ShinobiConfig.BEAST_ENCOUNTERS.get()) { msg(p, "Beast encounters are disabled on this server"); return; }
        if (MissionTracker.storyDone(t) < 3) { msg(p, "Finish story chapter 3 to learn where the beasts sleep"); return; }
        if (PlayerProgression.getLevel(p) < b.minLevel()) { msg(p, b.name() + " needs level " + b.minLevel()); return; }
        long now = p.level().getGameTime();
        if (now < t.getLong("beastCd")) { msg(p, "The beasts are quiet. Try again in " + ((t.getLong("beastCd") - now) / 1200 + 1) + " min"); return; }
        ServerLevel sl = p.serverLevel();
        if (!sl.getEntitiesOfClass(BeastEntity.class, p.getBoundingBox().inflate(80)).isEmpty()) { msg(p, "A beast is already nearby"); return; }
        Vec3 look = p.getLookAngle().multiply(1, 0, 1).normalize();
        BlockPos at = sl.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(p.position().add(look.scale(18))));
        BeastEntity e = ShinobiRegistry.BEAST.get().create(sl);
        if (e == null) return;
        e.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getYRot() + 180F, 0);
        e.setBeast(b.id());
        sl.addFreshEntity(e);
        e.setTarget(p);
        t.putLong("beastCd", now + CHALLENGE_COOLDOWN);
        ShinobiNet.banner(p, 2, b.name(), "Weaken it, then seal it. It cannot be killed.");
        ShinobiNet.syncTag(p);
    }

    // ------------------------------------------------------------------ sealing ritual
    public static void startRitual(ServerPlayer p, BeastEntity beast, ItemStack scroll) {
        CompoundTag t = ShinobiData.of(p);
        ShinobiState st = ShinobiState.of(p.getUUID());
        if (!beast.isWeakened()) { msg(p, "It is still too strong to seal"); return; }
        String cur = ShinobiData.beast(t);
        if (!cur.isEmpty() && !cur.equals(beast.beastId())) { msg(p, "Your chakra already holds " + Beasts.get(cur).name()); return; }
        if (st.ritualStart >= 0) return;
        st.ritualStart = p.server.getTickCount();
        st.ritualBeast = beast.getId();
        ShinobiNet.banner(p, 2, "Sealing ritual", "Stand still and do not take damage...");
    }

    private static void cancelRitual(ServerPlayer p, ShinobiState st, String why) {
        st.ritualStart = -1;
        st.ritualBeast = -1;
        msg(p, "Ritual broken: " + why);
    }

    private static void tickRitual(ServerPlayer p, CompoundTag t, ShinobiState st, long now) {
        if (st.ritualStart < 0) return;
        Entity e = p.level().getEntity(st.ritualBeast);
        if (!(e instanceof BeastEntity beast) || !beast.isAlive() || !beast.isWeakened()) { cancelRitual(p, st, "the beast is gone"); return; }
        if (p.distanceTo(beast) > 9) { cancelRitual(p, st, "you moved away"); return; }
        if (now - st.lastHurt < 4) { cancelRitual(p, st, "you were hit"); return; }
        ItemStack hand = p.getMainHandItem().is(ShinobiRegistry.SEALING_SCROLL.get()) ? p.getMainHandItem()
                : p.getOffhandItem().is(ShinobiRegistry.SEALING_SCROLL.get()) ? p.getOffhandItem() : ItemStack.EMPTY;
        if (hand.isEmpty()) { cancelRitual(p, st, "keep the scroll in hand"); return; }
        long done = now - st.ritualStart;
        ServerLevel sl = p.serverLevel();
        if (done % 5 == 0) {
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8 + done * 0.2;
                sl.sendParticles(ParticleTypes.ENCHANT, beast.getX() + Math.cos(a) * 2, beast.getY() + 1 + done * 0.02, beast.getZ() + Math.sin(a) * 2, 1, 0, 0.3, 0, 0.4);
            }
        }
        if (done < RITUAL_TICKS) return;
        // success
        hand.shrink(1);
        st.ritualStart = -1;
        st.ritualBeast = -1;
        String id = beast.beastId();
        CompoundTag b = beastTag(t);
        boolean first = ShinobiData.beast(t).isEmpty();
        b.putString("id", id);
        b.putInt("bond", first ? 1 : b.getInt("bond") + 60);
        t.put("beast", b);
        ShinobiData.add(t, "beastsDefeated", id);
        t.putInt("ap", t.getInt("ap") + 4);
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, beast.getX(), beast.getY() + 2, beast.getZ(), 120, 1.5, 2, 1.5, 0.15);
        net.minecraft.world.entity.LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(sl);
        if (bolt != null) { bolt.moveTo(p.position()); bolt.setVisualOnly(true); sl.addFreshEntity(bolt); }
        beast.discard();
        MissionTracker.onBeast(p, id, true);
        ShinobiNet.banner(p, 3, "Bond sealed", Beasts.get(id).name() + " now dwells within you");
        ShinobiNet.syncTag(p);
    }

    // ------------------------------------------------------------------ cloak
    public static void toggleCloak(ServerPlayer p) {
        CompoundTag t = ShinobiData.of(p);
        int stage = BeastStages.stage(t);
        CompoundTag b = beastTag(t);
        if (b.getBoolean("cloak")) { b.putBoolean("cloak", false); t.put("beast", b); msg(p, "Cloak released"); return; }
        if (stage < 2) { msg(p, stage == 0 ? "You have no beast bond" : "Deepen the bond to reach Chakra Access"); return; }
        if (Resources.chakra(t) < 25) { msg(p, "Need 25 chakra"); return; }
        b.putBoolean("cloak", true);
        t.put("beast", b);
        msg(p, BeastStages.NAMES[stage] + " engaged");
        p.serverLevel().sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX(), p.getY() + 1, p.getZ(), 50, 0.7, 1, 0.7, 0.12);
    }

    public static void tick(ServerPlayer p, CompoundTag t, ShinobiState st, long now) {
        tickRitual(p, t, st, now);
        if (!ShinobiData.cloak(t)) return;
        int stage = BeastStages.stage(t);
        if (stage < 2) { CompoundTag b = beastTag(t); b.putBoolean("cloak", false); t.put("beast", b); return; }
        if (now % 20 == 0) {
            if (!Resources.spendChakra(p, t, Passives.cloakDrainPerSec(stage))) {
                CompoundTag b = beastTag(t); b.putBoolean("cloak", false); t.put("beast", b);
                p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 1));
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1));
                st.exhaustedUntil = now + 400;
                ShinobiNet.banner(p, 0, "Chakra exhausted", "The beast's cloak collapses and leaves you drained");
                return;
            }
            boolean fight = !p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(16), x -> x instanceof Enemy && x.isAlive()).isEmpty();
            if (fight && now % 200 == 0) addBond(p, 1);
        }
        if (now % 10 == 0) {
            int a = stage - 2;
            eff(p, MobEffects.MOVEMENT_SPEED, stage >= 4 ? 1 : stage >= 3 ? 0 : -1);
            eff(p, MobEffects.DAMAGE_BOOST, stage >= 5 ? 2 : stage >= 4 ? 1 : stage >= 3 ? 0 : -1);
            eff(p, MobEffects.DAMAGE_RESISTANCE, stage >= 5 ? 1 : stage >= 4 ? 0 : -1);
            eff(p, MobEffects.REGENERATION, stage >= 3 ? 0 : -1);
            if (stage >= 5) eff(p, MobEffects.JUMP, 1);
            p.serverLevel().sendParticles(cloakParticle(ShinobiData.beast(t)), p.getX(), p.getY() + 1, p.getZ(), 4 + a * 3, 0.4 + a * 0.15, 0.7, 0.4 + a * 0.15, 0.03);
        }
        if (stage < 5 || t.getBoolean("beastMode")) cloakTails(p, t, stage, now);
    }

    /** Beast-coloured chakra tails (1 tail at Chakra Access, growing to the beast's full count at Advanced Cloak) and cloak aura. */
    private static void cloakTails(ServerPlayer p, CompoundTag t, int stage, long now) {
        if (now % 2 != 0) return;
        Beasts.Beast b = Beasts.get(ShinobiData.beast(t));
        if (b == null) return;
        int n = stage >= 4 || t.getBoolean("beastMode") ? b.tails() : Math.min(b.tails(), stage - 1);
        float rr = ((b.color() >> 16) & 255) / 255F, gg = ((b.color() >> 8) & 255) / 255F, bb = (b.color() & 255) / 255F;
        net.minecraft.core.particles.DustParticleOptions dust = new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(rr, gg, bb), 1.1F + stage * 0.15F);
        double yaw = Math.toRadians(p.yBodyRot);
        ServerLevel sl = p.serverLevel();
        for (int k = 0; k < n; k++) {
            double spread = (k - (n - 1) / 2.0) * (n > 5 ? 0.22 : 0.34);
            double phase = now * 0.12 + k * 1.3;
            double ang = yaw + spread;
            double dx = Math.sin(ang), dz = -Math.cos(ang);
            for (int i = 1; i <= 7; i++) {
                double d = 0.35 * i;
                double sway = Math.sin(phase - i * 0.55) * 0.28 * (i / 7.0);
                double px = p.getX() + dx * d + (-dz) * sway;
                double pz = p.getZ() + dz * d + dx * sway;
                double py = p.getY() + 0.9 + Math.sin(i * 0.4 + phase * 0.5) * 0.25 + i * 0.06;
                sl.sendParticles(dust, px, py, pz, 1, 0.01, 0.01, 0.01, 0);
            }
        }
        if (stage >= 3 && now % 4 == 0) {
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6 + now * 0.1;
                sl.sendParticles(dust, p.getX() + Math.cos(a) * 0.6, p.getY() + 0.1 + (i % 3) * 0.7, p.getZ() + Math.sin(a) * 0.6, 1, 0.02, 0.02, 0.02, 0);
            }
        }
    }

    /** Each beast's cloak has its own look. */
    public static net.minecraft.core.particles.ParticleOptions cloakParticle(String beastId) {
        return switch (beastId) {
            case "sand_tanuki" -> ParticleTypes.POOF;
            case "blue_flame_cat" -> ParticleTypes.SOUL_FIRE_FLAME;
            case "giant_turtle" -> ParticleTypes.BUBBLE_POP;
            case "lava_ape" -> ParticleTypes.LAVA;
            case "horse_dolphin" -> ParticleTypes.CLOUD;
            case "six_slug" -> ParticleTypes.SPORE_BLOSSOM_AIR;
            case "horned_beetle" -> ParticleTypes.GLOW;
            case "ox_octopus" -> ParticleTypes.SQUID_INK;
            default -> ParticleTypes.FLAME;
        };
    }

    private static void eff(ServerPlayer p, net.minecraft.world.effect.MobEffect e, int amp) {
        if (amp >= 0) p.addEffect(new MobEffectInstance(e, 15, amp, true, false, true));
    }
}
