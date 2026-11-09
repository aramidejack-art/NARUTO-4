package com.mayhem.shinobi.ability;

/** A mastery-unlocked alternative form of a jutsu that materially changes how it plays. */
public record Variant(String id, String name, String desc, int needMastery,
                      double costMul, double cdMul, double dmgMul, double rangeMul, Flag flag) {
    public static Variant of(String id, String name, String desc, int need, double cost, double cd, double dmg, double range, Flag f) {
        return new Variant(id, name, desc, need, cost, cd, dmg, range, f);
    }
}
