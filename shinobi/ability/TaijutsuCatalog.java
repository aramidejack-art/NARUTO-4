package com.mayhem.shinobi.ability;

import com.mayhem.combat.MoveType;
import com.mayhem.fx.Fx;
import com.mayhem.shinobi.data.ShinobiState;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static com.mayhem.shinobi.ability.Cat.*;

/** Hand-to-hand techniques (stamina-led). Castable in Taijutsu style. */
final class TaijutsuCatalog {
    private TaijutsuCatalog() {}
    private static final Category T = Category.TAIJUTSU;

    static void register() {
        reg(b("tai_knee", "Rising Knee", T, "", 0, 14, 60, 4.5, 6, risingKnee(),
                "Close in on a target and launch it skyward with a knee.", "minecraft:iron_boots"));
        reg(b("tai_spin", "Spinning Heel", T, "", 0, 18, 80, 3.4, 5, Behaviors.arc(Vis.BODY, 3.4, 180, 1.0, 0.9, 0.15, MoveType.ROUNDHOUSE),
                "A full-circle kick that hits everything around you.", "minecraft:leather_boots")
                .variant(v("wide", "Whirlwind Heel", "Bigger circle.", 40, 1.2, 1.1, 0.9, 1.0, Flag.AREA)));
        reg(b("tai_kick", "Shadowless Kick", T, "", 0, 20, 90, 5.5, 6, Behaviors.dashStrike(Vis.BODY, 5.5, 1.1, 0.9, false),
                "A blinding dash-kick that sends enemies flying.", "minecraft:gold_ingot"));
        reg(b("tai_palm", "Boulder Palm", T, "", 0, 24, 110, 3.5, 7, Behaviors.stunCone(Vis.BODY, 3.2, 35, 25, 1.6),
                "A heavy open palm that staggers everyone in reach.", "minecraft:anvil").level(5));
        reg(b("tai_barrage", "Afterimage Barrage", T, "", 0, 34, 180, 12, 6, Behaviors.clones(Vis.BODY, 6, 1.6),
                "Strike from several angles in a heartbeat, then launch.", "minecraft:blaze_rod").level(10)
                .variant(v("long", "Endless Barrage", "More hits.", 60, 1.4, 1.2, 1.2, 1.0, Flag.AREA)));
        reg(b("tai_guardbreak", "Guard Break", T, "", 0, 30, 120, 4, 9, guardBreak(),
                "A committed overhead strike that crushes guards and weakens the target.", "minecraft:netherite_scrap").level(8));
        reg(b("tai_counter", "Counter Stance", Category.DEFENSE, "", 0, 20, 160, 0, 6, Utility.counter(Vis.BODY, 30, 2.0),
                "Brace for impact. If hit within the window, negate it and strike back from behind.", "minecraft:shield").level(6).style(StyleReq.TAI));
        reg(b("tai_gate", "Gate Surge", T, "", 20, 40, 900, 0, 0, gateSurge(),
                "Burst past your limits: big speed and strength for 15 seconds, then crippling exhaustion.", "minecraft:nether_star").level(18).style(StyleReq.TAI).rarity(2));
    }

    private static Behavior risingKnee() {
        return c -> {
            c.swing(MoveType.KNEE);
            LivingEntity e = c.primaryTarget(c.range);
            if (e == null) return;
            Vec3 near = e.position().subtract(c.flatLook().scale(1.2));
            if (com.mayhem.combat.Teleports.validSpot(c.level, c.p, near, true)) com.mayhem.combat.Teleports.blink(c.p, near, e.position());
            c.castSound(Vis.BODY);
            if (c.hurt(e, 1.2)) {
                c.push(e, c.flatLook(), 0.25, 0.95);
                Fx.particle(c.level, Vis.BODY.accent(), e.position().add(0, 1, 0), 12, 0.3, 0.1);
            }
        };
    }

    private static Behavior guardBreak() {
        return c -> {
            c.swing(MoveType.HOOK);
            LivingEntity e = c.primaryTarget(c.range);
            c.castSound(Vis.BODY);
            if (e == null) return;
            if (c.hurt(e, 1.0)) {
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1));
                c.push(e, c.flatLook(), 1.1, 0.3);
                c.blast(e.position().add(0, 0.5, 0), 1.8, 0.2, 0.3, 0.1, Vis.BODY, false);
            }
        };
    }

    private static Behavior gateSurge() {
        Behavior buff = Utility.empower(Vis.BODY, 300, 1.35, 2, 1, -1, -1);
        return c -> {
            buff.cast(c);
            ShinobiState st = ShinobiState.of(c.p.getUUID());
            Tasks.later(c.p.server, 300, () -> {
                if (!c.p.isAlive()) return;
                c.p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 1));
                c.p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1));
                st.exhaustedUntil = c.p.server.getTickCount() + 400;
            });
        };
    }
}
