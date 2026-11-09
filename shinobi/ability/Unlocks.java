package com.mayhem.shinobi.ability;

import com.mayhem.shinobi.data.ShinobiData;
import net.minecraft.nbt.CompoundTag;

/** Requirement checks shared by the server (authoritative) and the GUI (display). */
public final class Unlocks {
    private Unlocks() {}

    /** Returns null when the requirement is met, else a short human reason. */
    public static String missing(AbilityDef d, CompoundTag t, int level) {
        if (level < d.reqLevel) return "Requires level " + d.reqLevel;
        switch (d.req) {
            case ELEMENT -> { if (!ShinobiData.has(t, "elements", d.reqArg) && !d.reqArg.equals(ShinobiData.element(t))) return "Requires " + d.reqArg + " affinity"; }
            case CLAN -> { if (!d.reqArg.equals(ShinobiData.clan(t))) return "Requires clan: " + d.reqArg; }
            case BLOODLINE -> { if (!ShinobiData.has(t, "bloodlines", d.reqArg) && !d.reqArg.equals(ShinobiData.bloodline(t))) return "Requires bloodline: " + d.reqArg; }
            case BEAST_STAGE -> { if (BeastStages.stage(t) < Integer.parseInt(d.reqArg)) return "Requires beast bond stage " + d.reqArg; }
            case BEAST_ID -> { if (!d.reqArg.equals(ShinobiData.beast(t))) return "Requires beast: " + d.reqArg; }
            case STORY -> { if (t.getInt("storyDone") < Integer.parseInt(d.reqArg)) return "Requires story chapter " + d.reqArg; }
            default -> { }
        }
        return null;
    }
}
