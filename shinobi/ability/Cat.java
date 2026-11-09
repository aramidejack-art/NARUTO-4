package com.mayhem.shinobi.ability;

/** Compact builder helpers shared by the catalog classes. */
final class Cat {
    private Cat() {}

    static StyleReq defaultStyle(Category c) {
        return switch (c) {
            case TAIJUTSU, WEAPON -> StyleReq.TAI;
            case DEFENSE, MOVEMENT, ULTIMATE, BEAST -> StyleReq.ANY;
            default -> StyleReq.NIN;
        };
    }

    static AbilityDef.Builder b(String id, String name, Category c, String element, int chakra, int stamina, int cd,
                                double range, double dmg, Behavior beh, String desc, String icon) {
        return AbilityDef.builder(id, name, c).element(element).cost(chakra, stamina).cd(cd).range(range).damage(dmg)
                .does(beh).desc(desc).icon(icon).style(defaultStyle(c));
    }

    static Variant v(String id, String name, String desc, int need, double cost, double cd, double dmg, double range, Flag f) {
        return Variant.of(id, name, desc, need, cost, cd, dmg, range, f);
    }

    static void reg(AbilityDef.Builder b) { Abilities.add(b.build()); }
}
