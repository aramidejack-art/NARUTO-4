package com.mayhem.shinobi.ability;

import com.mayhem.shinobi.data.ShinobiData;
import net.minecraft.nbt.CompoundTag;

/** Pure bond-stage rules: 0 none, 1 sealed, 2 chakra access, 3 partial manifestation, 4 advanced cloak, 5 full avatar. */
public final class BeastStages {
    /** Play-tuned (Full Avatar at 1600 — was 4500). */
    public static final int[] BOND_NEEDED = {0, 0, 80, 250, 700, 1600};
    public static final String[] NAMES = {"None", "Sealed", "Chakra Access", "Partial Manifestation", "Advanced Cloak", "Full Avatar"};

    private BeastStages() {}

    public static int stageForBond(int bond, boolean bonded) {
        if (!bonded) return 0;
        int s = 1;
        for (int i = 2; i < BOND_NEEDED.length; i++) if (bond >= BOND_NEEDED[i]) s = i;
        return s;
    }

    public static int stage(CompoundTag t) {
        return stageForBond(ShinobiData.bond(t), !ShinobiData.beast(t).isEmpty());
    }

    public static int toNext(CompoundTag t) {
        int s = stage(t);
        return s >= 5 || s == 0 ? 0 : BOND_NEEDED[s + 1] - ShinobiData.bond(t);
    }
}
