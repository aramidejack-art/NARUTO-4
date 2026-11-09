package com.mayhem.shinobi.data;

import com.mayhem.progression.PlayerProgression;
import com.mayhem.shinobi.ShinobiConfig;
import com.mayhem.shinobi.ability.BeastStages;
import com.mayhem.shinobi.identity.Passives;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** Server-side chakra and stamina: maxima, spending and regeneration. Values persist in the player's shinobi tag. */
public final class Resources {
    private Resources() {}

    public static int level(ServerPlayer p) { return PlayerProgression.getLevel(p); }

    public static float maxChakra(ServerPlayer p, CompoundTag t) {
        double base = 100 + level(p) * 5 + ShinobiData.stat(t, "chakra") * 10 + BeastStages.stage(t) * 25;
        return (float) (base * Passives.chakraMul(t));
    }

    public static float maxStamina(ServerPlayer p, CompoundTag t) {
        double base = 100 + level(p) * 2 + ShinobiData.stat(t, "stamina") * 8;
        return (float) (base * Passives.staminaMul(t));
    }

    public static float chakra(CompoundTag t) { return t.getFloat("chakra"); }
    public static float stamina(CompoundTag t) { return t.getFloat("stamina"); }

    public static void ensureInit(ServerPlayer p, CompoundTag t) {
        if (!t.contains("chakra")) t.putFloat("chakra", maxChakra(p, t));
        if (!t.contains("stamina")) t.putFloat("stamina", maxStamina(p, t));
    }

    public static boolean hasChakra(CompoundTag t, float amount) { return chakra(t) >= amount; }

    public static boolean spendChakra(ServerPlayer p, CompoundTag t, float amount) {
        if (amount <= 0) return true;
        if (chakra(t) < amount) return false;
        t.putFloat("chakra", chakra(t) - amount);
        return true;
    }

    public static boolean spendStamina(ServerPlayer p, CompoundTag t, float amount) {
        if (amount <= 0) return true;
        if (stamina(t) < amount) return false;
        t.putFloat("stamina", stamina(t) - amount);
        ShinobiState.of(p.getUUID()).staminaRegenAt = p.server.getTickCount() + 25;
        return true;
    }

    public static void addChakra(ServerPlayer p, CompoundTag t, float amount) {
        t.putFloat("chakra", Math.max(0F, Math.min(maxChakra(p, t), chakra(t) + amount)));
    }

    public static void addStamina(ServerPlayer p, CompoundTag t, float amount) {
        t.putFloat("stamina", Math.max(0F, Math.min(maxStamina(p, t), stamina(t) + amount)));
    }

    /** Called every server tick for each player. */
    public static void regen(ServerPlayer p, CompoundTag t, ShinobiState st) {
        long now = p.server.getTickCount();
        boolean nin = ShinobiData.style(t) == Style.NINJUTSU;
        double cr = ShinobiConfig.CHAKRA_REGEN_PER_SEC.get() / 20.0 * Passives.chakraRegenMul(t) * (nin ? 1.5 : 1.0);
        if (st.charging) cr += ShinobiConfig.CHAKRA_CHARGE_PER_SEC.get() / 20.0;
        addChakra(p, t, (float) cr);
        if (now >= st.staminaRegenAt) {
            double sr = ShinobiConfig.STAMINA_REGEN_PER_SEC.get() / 20.0 * (nin ? 1.0 : 1.5);
            if (now < st.exhaustedUntil) sr *= 0.4;
            addStamina(p, t, (float) sr);
        }
        // clamp if max dropped (e.g. clan change)
        float mc = maxChakra(p, t), ms = maxStamina(p, t);
        if (chakra(t) > mc) t.putFloat("chakra", mc);
        if (stamina(t) > ms) t.putFloat("stamina", ms);
    }
}
