package com.mayhem.shinobi.events;

import com.mayhem.progression.PlayerProgression;
import com.mayhem.shinobi.ShinobiConfig;
import com.mayhem.shinobi.ability.AbilityCaster;
import com.mayhem.shinobi.ability.Behaviors;
import com.mayhem.shinobi.ability.Projectiles;
import com.mayhem.shinobi.ability.Tasks;
import com.mayhem.shinobi.ability.Utility;
import com.mayhem.shinobi.ability.Vis;
import com.mayhem.shinobi.appearance.Appearance;
import com.mayhem.shinobi.beast.BeastBond;
import com.mayhem.shinobi.data.Resources;
import com.mayhem.shinobi.data.ShinobiData;
import com.mayhem.shinobi.data.ShinobiState;
import com.mayhem.shinobi.identity.Awakenings;
import com.mayhem.shinobi.identity.Guardian;
import com.mayhem.shinobi.identity.Modes;
import com.mayhem.shinobi.identity.Passives;
import com.mayhem.shinobi.move.Traversal;
import com.mayhem.shinobi.net.ShinobiNet;
import com.mayhem.shinobi.story.MissionTracker;
import com.mayhem.shinobi.combat.EnemyCombat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;

/** Forge-bus glue: the per-tick pipeline, damage hooks, kill credit and lifecycle for all shinobi systems. */
public final class ShinobiEvents {
    private static final UUID SPEED_ID = UUID.fromString("5c1d1f4e-7a52-4f0a-9b0e-3a6f9c2b7e11");

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Tasks.tick(e.getServer());
        Projectiles.tick(e.getServer());
        EnemyCombat.tick(e.getServer());
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || !p.isAlive()) return;
        long now = p.server.getTickCount();
        CompoundTag t = ShinobiData.of(p);
        ShinobiState st = ShinobiState.of(p.getUUID());
        Resources.ensureInit(p, t);
        if (now % 20 == 0) {
            grantPoints(p, t, st);
            MissionTracker.poll(p);
            // EscortClientEntity reports progress itself; this is a backup for protect proximity
            boolean nearEscort = !p.serverLevel().getEntitiesOfClass(com.mayhem.shinobi.npc.EscortClientEntity.class,
                    p.getBoundingBox().inflate(10), LivingEntity::isAlive).isEmpty();
            if (nearEscort) MissionTracker.onEscortTick(p, true);
        }
        Resources.regen(p, t, st);
        Awakenings.tick(p, t);
        Modes.tick(p, t, st);
        Guardian.tick(p, t, st);
        Traversal.tick(p, t);
        BeastBond.tick(p, t, st, now);
        com.mayhem.shinobi.avatar.AvatarManager.tick(p, t, st, now);
        if (now % 10 == 0) applySpeed(p, t);
        if (st.charging && (!st.casting.isEmpty() || p.isSprinting())) st.charging = false;
        if (now % 4 == 0) ShinobiNet.sendResources(p, false);
        WorldEvents.tick(p, now);
        com.mayhem.shinobi.npc.Encounters.tick(p, now);
    }

    /** +2 AP and +2 SP for every level gained since the last poll. Levels are the existing MAYHEM progression. */
    private void grantPoints(ServerPlayer p, CompoundTag t, ShinobiState st) {
        int level = PlayerProgression.getLevel(p);
        int stored = t.contains("apLevel") ? t.getInt("apLevel") : 1;
        if (level > stored) {
            int gain = (level - stored) * 2;
            t.putInt("ap", ShinobiData.ap(t) + gain);
            t.putInt("sp", ShinobiData.sp(t) + gain);
            t.putInt("apLevel", level);
            ShinobiNet.banner(p, 3, "Level " + level, "+" + gain + " ability points, +" + gain + " stat points");
            ShinobiNet.syncTag(p);
        } else if (!t.contains("apLevel")) {
            t.putInt("apLevel", level);
        }
    }

    private void applySpeed(ServerPlayer p, CompoundTag t) {
        AttributeInstance a = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (a == null) return;
        double bonus = Passives.speedBonus(t) + ShinobiData.stat(t, "speed") * 0.004;
        AttributeModifier cur = a.getModifier(SPEED_ID);
        if (cur != null && Math.abs(cur.getAmount() - bonus) < 1e-6) return;
        if (cur != null) a.removeModifier(SPEED_ID);
        if (bonus > 0) a.addTransientModifier(new AttributeModifier(SPEED_ID, "mayhem_shinobi_speed", bonus, AttributeModifier.Operation.MULTIPLY_BASE));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onHurt(LivingHurtEvent e) {
        LivingEntity victim = e.getEntity();
        // attacker side: boosts and power stat
        if (e.getSource().getEntity() instanceof ServerPlayer atk) {
            ShinobiState as = ShinobiState.of(atk.getUUID());
            long now = atk.server.getTickCount();
            CompoundTag at = ShinobiData.of(atk);
            float mul = 1F + ShinobiData.stat(at, "power") * 0.01F;
            if (now < as.boostUntil) mul *= (float) as.boostMul;
            e.setAmount(e.getAmount() * mul);
        }
        if (!(victim instanceof ServerPlayer p)) return;
        ShinobiState st = ShinobiState.of(p.getUUID());
        CompoundTag t = ShinobiData.of(p);
        long now = p.server.getTickCount();
        float amt = e.getAmount() * (float) Passives.incomingMul(t);
        if (now < st.guardUntil) amt *= (float) (1.0 - Math.max(0, Math.min(0.9, st.guardReduction)));
        if (now < st.counterUntil && e.getSource().getEntity() instanceof LivingEntity attacker) {
            Utility.triggerCounter(p, attacker, st.counterDamage, Vis.CHAKRA);
            e.setCanceled(true);
            return;
        }
        e.setAmount(amt);
        st.lastHurt = now;
        AbilityCaster.interrupt(p);
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent e) {
        if (e.getSource().getEntity() instanceof ServerPlayer killer && e.getEntity() != killer) MissionTracker.onKill(killer, e.getEntity());
        if (e.getEntity() instanceof ServerPlayer dead) {
            ShinobiState st = ShinobiState.of(dead.getUUID());
            st.charging = false; st.casting = ""; st.castToken++;
            CompoundTag t = ShinobiData.of(dead);
            t.putBoolean("waterWalk", false); t.putBoolean("wallWalk", false); t.putString("mode", ""); t.putInt("guardTicks", 0);
        }
    }

    @SubscribeEvent
    public void onClone(PlayerEvent.Clone e) {
        ShinobiData.copy(e.getOriginal(), e.getEntity());
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        Resources.ensureInit(p, ShinobiData.of(p));
        Appearance.load(p);
        ShinobiNet.syncTag(p);
        ShinobiNet.sendResources(p, true);
        ShinobiNet.appearanceToTrackers(p);
        // send the looks of everyone already cached so the new player sees them
        for (var en : Appearance.CACHE.entrySet()) if (!en.getKey().equals(p.getUUID())) ShinobiNet.appearanceTo(p, en.getKey(), en.getValue());
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        CompoundTag t = ShinobiData.of(p);
        t.putFloat("chakra", Resources.maxChakra(p, t));
        t.putFloat("stamina", Resources.maxStamina(p, t));
        Appearance.load(p);
        ShinobiNet.syncTag(p);
        ShinobiNet.sendResources(p, true);
        ShinobiNet.appearanceToTrackers(p);
    }

    @SubscribeEvent
    public void onChangeDim(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) ShinobiNet.appearanceToTrackers(p);
    }

    @SubscribeEvent
    public void onStartTracking(net.minecraftforge.event.entity.player.PlayerEvent.StartTracking e) {
        if (e.getTarget() instanceof ServerPlayer target && e.getEntity() instanceof ServerPlayer viewer && Appearance.has(target.getUUID()))
            ShinobiNet.appearanceTo(viewer, target.getUUID(), Appearance.of(target.getUUID()));
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        UUID id = e.getEntity().getUUID();
        ShinobiState.remove(id);
        Appearance.CACHE.remove(id);
    }

    /** Appearance scale changes the player's hitbox slightly. Clamped 0.88-1.12 so combat stays fair. */
    @SubscribeEvent
    public void onSize(EntityEvent.Size e) {
        if (!(e.getEntity() instanceof net.minecraft.world.entity.player.Player pl) || !Appearance.has(pl.getUUID())) return;
        CompoundTag ap = Appearance.of(pl.getUUID());
        float w = ap.getFloat("width"), h = ap.getFloat("height");
        if (w < Appearance.SCALE_MIN || w > Appearance.SCALE_MAX || h < Appearance.SCALE_MIN || h > Appearance.SCALE_MAX) return;
        e.setNewSize(e.getNewSize().scale(w, h));
        e.setNewEyeHeight(e.getNewEyeHeight() * h);
    }

    @SubscribeEvent
    public void onServerStop(ServerStoppingEvent e) {
        Behaviors.revertAllWalls(e.getServer());
        Tasks.clear();
        Projectiles.clear();
        WorldEvents.clear();
        com.mayhem.shinobi.npc.Encounters.clear();
        ShinobiState.clear();
        Appearance.CACHE.clear();
    }
}
