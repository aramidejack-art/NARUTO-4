package com.mayhem.shinobi.identity;

import com.mayhem.shinobi.ability.AbilityDef;
import com.mayhem.shinobi.ability.BeastStages;
import com.mayhem.shinobi.ability.Category;
import com.mayhem.shinobi.data.ShinobiData;
import net.minecraft.nbt.CompoundTag;

/** Pure numeric rules for clan, bloodline-mode and beast-cloak effects. No Minecraft world access (unit-testable). */
public final class Passives {
    private Passives() {}

    private static boolean clan(CompoundTag t, String id) { return id.equals(ShinobiData.clan(t)); }
    private static boolean mode(CompoundTag t, String id) { return id.equals(ShinobiData.mode(t)); }

    public static double chakraMul(CompoundTag t) { return clan(t, "spiralborn") ? 1.25 : 1.0; }
    public static double staminaMul(CompoundTag t) { return clan(t, "stonebrow") ? 1.3 : 1.0; }

    public static double chakraRegenMul(CompoundTag t) {
        double m = 1.0;
        if (clan(t, "spiralborn")) m *= 1.4;
        if (mode(t, "wood_release")) m *= 1.25;
        if (ShinobiData.cloak(t) && BeastStages.stage(t) >= 2) m *= 1.3;
        return m;
    }

    public static double damageMul(CompoundTag t, AbilityDef d) {
        double m = 1.0;
        String e = d.element;
        if (clan(t, "emberfang") && "fire".equals(e)) m *= 1.15;
        if (clan(t, "thunderfang") && "lightning".equals(e)) m *= 1.15;
        if (clan(t, "mistbane") && "water".equals(e)) m *= 1.10;
        if (clan(t, "bonecrest") && d.category == Category.TAIJUTSU) m *= 1.15;
        if (ShinobiData.cloak(t)) {
            int s = BeastStages.stage(t);
            if (s >= 3) m *= 1.0 + 0.1 * (s - 2);
        }
        return m;
    }

    public static double cooldownMul(CompoundTag t, AbilityDef d) {
        double m = 1.0;
        if (clan(t, "shadoweave") && d.category == Category.DEFENSE) m *= 0.8;
        if (clan(t, "thunderfang") && d.category == Category.MOVEMENT) m *= 0.7;
        if (mode(t, "mirror_eye")) m *= 0.85;
        return m;
    }

    public static double costMul(CompoundTag t, AbilityDef d) {
        double m = 1.0;
        if (clan(t, "windrider") && d.category == Category.MOVEMENT) m *= 0.7;
        return m;
    }

    public static double incomingMul(CompoundTag t) {
        double m = 1.0;
        if (clan(t, "stonebrow")) m *= 0.9;
        if (mode(t, "mirror_eye")) m *= 0.85;
        if (mode(t, "lava_release")) m *= 0.85;
        if (ShinobiData.cloak(t)) {
            int s = BeastStages.stage(t);
            if (s >= 3) m *= 1.0 - 0.05 * (s - 2);
        }
        return m;
    }

    public static double speedBonus(CompoundTag t) {
        double b = 0;
        if (clan(t, "windrider")) b += 0.08;
        if (clan(t, "thunderfang")) b += 0.04;
        if (mode(t, "shadow_weave")) b += 0.10;
        return b;
    }

    public static double restrainLengthMul(CompoundTag t) { return clan(t, "spiralborn") ? 1.5 : 1.0; }

    /** Chakra needed to keep a mode running (per second); beast cloak drain grows with its stage. */
    public static int cloakDrainPerSec(int stage) { return stage <= 1 ? 0 : 2 + stage * 2; }
}
