package com.mayhem.combat;

import com.mayhem.MayhemMod;
import com.mayhem.config.MayhemConfig;
import com.mayhem.world.Destruction;
import com.mayhem.world.Dynamics;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/** Forge-bus hooks that tie dodge, guard, parry, slam and ultimates into vanilla damage and ticking. */
@Mod.EventBusSubscriber(modid = MayhemMod.MOD_ID)
public final class CombatEvents {
    private CombatEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttacked(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        CombatState s = CombatState.peek(p);
        if (s == null) return;
        DamageSource src = event.getSource();
        if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;

        int now = p.server.getTickCount();
        // Ultimates and slams are untouchable while they play out.
        if (s.ult != CombatState.Ult.NONE || s.slam != CombatState.Slam.NONE) {
            event.setCanceled(true);
            return;
        }
        if (now <= s.dodgeUntil) {
            event.setCanceled(true);
            SpecialMoves.onDodged(p, s, src, now);
            return;
        }
        if (s.guarding && now <= s.parryUntil && SpecialMoves.isFrontal(p, src)) {
            event.setCanceled(true);
            SpecialMoves.parry(p, s, src);
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        CombatState s = CombatState.peek(p);
        if (s == null || !s.guarding) return;
        DamageSource src = event.getSource();
        if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || src.is(DamageTypeTags.BYPASSES_ARMOR)) return;
        if (!SpecialMoves.isFrontal(p, src)) return;

        // Blocking costs stamina in proportion to the hit. Run dry and the guard breaks.
        net.minecraft.nbt.CompoundTag st = com.mayhem.shinobi.data.ShinobiData.of(p);
        com.mayhem.shinobi.data.Resources.ensureInit(p, st);
        float dmg = event.getAmount();
        float cost = dmg * 1.5F;
        float have = com.mayhem.shinobi.data.Resources.stamina(st);
        float red = MayhemConfig.GUARD_REDUCTION.get().floatValue();
        if (have >= cost) {
            com.mayhem.shinobi.data.Resources.spendStamina(p, st, cost);
            event.setAmount(dmg * (1F - red));
            CombatState.addMomentum(p, s, 4F);
            SpecialMoves.guardBlockFx(p);
        } else {
            // partial block from what stamina is left, then the guard shatters
            float part = cost <= 0 ? 0F : have / cost;
            com.mayhem.shinobi.data.Resources.spendStamina(p, st, have);
            event.setAmount(dmg * (1F - red * part));
            SpecialMoves.dropGuard(p, s);
            s.parryUntil = -1;
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 50, 1));
            p.displayClientMessage(net.minecraft.network.chat.Component.literal("GUARD BROKEN - out of stamina"), true);
            p.serverLevel().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.SHIELD_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.9F);
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        CombatState s = CombatState.peek(p);
        if (s != null && SpecialMoves.noFall(s, p.server.getTickCount())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Ultimates.abort(p);
            CombatState.remove(p.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) return;
        if (event.player instanceof ServerPlayer p) {
            SpecialMoves.tick(p);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        Dynamics.tick(server);
        Destruction.tickDebris(server);
        MobBrain.tick(server);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        CombatState.clear();
        Dynamics.clear();
        Destruction.clear();
        MobBrain.clear();
    }
}
