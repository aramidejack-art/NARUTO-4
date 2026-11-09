package com.mayhem.shinobi.ability;

import java.util.ArrayList;
import java.util.List;

/** Immutable ability definition. Built with the fluent {@link Builder}; registered in {@link Abilities}. */
public final class AbilityDef {
    public final String id, name, desc, element, icon, reqArg;
    public final Category category;
    public final StyleReq style;
    public final int chakra, stamina, cooldown, castTicks, learnCost, rarity, reqLevel;
    public final double range, damage;
    public final Req req;
    public final Behavior behavior;
    public final List<Variant> variants;
    public final boolean interruptible, toggle;

    private AbilityDef(Builder b) {
        id = b.id; name = b.name; desc = b.desc; element = b.element; icon = b.icon; category = b.category; style = b.style;
        chakra = b.chakra; stamina = b.stamina; cooldown = b.cooldown; castTicks = b.castTicks; learnCost = b.learnCost;
        rarity = b.rarity; range = b.range; damage = b.damage; req = b.req; reqArg = b.reqArg; reqLevel = b.reqLevel;
        behavior = b.behavior; variants = List.copyOf(b.variants); interruptible = b.interruptible; toggle = b.toggle;
    }

    public Variant variant(String vid) {
        if (vid == null || vid.isEmpty()) return null;
        for (Variant v : variants) if (v.id().equals(vid)) return v;
        return null;
    }

    public static Builder builder(String id, String name, Category c) { return new Builder(id, name, c); }

    public static final class Builder {
        final String id, name; final Category category;
        String desc = "", element = "", icon = "minecraft:paper", reqArg = "";
        StyleReq style = StyleReq.NIN; int chakra, stamina, cooldown = 100, castTicks, learnCost = 1, rarity, reqLevel = 1;
        double range = 8, damage = 4; Req req = Req.NONE; Behavior behavior = c -> {};
        final List<Variant> variants = new ArrayList<>(); boolean interruptible = true, toggle;

        Builder(String id, String name, Category c) { this.id = id; this.name = name; this.category = c; }
        public Builder desc(String d) { desc = d; return this; }
        public Builder element(String e) { element = e; return this; }
        public Builder icon(String i) { icon = i; return this; }
        public Builder style(StyleReq s) { style = s; return this; }
        public Builder cost(int chakra, int stamina) { this.chakra = chakra; this.stamina = stamina; return this; }
        public Builder cd(int ticks) { cooldown = ticks; return this; }
        public Builder cast(int ticks) { castTicks = ticks; return this; }
        public Builder learn(int ap) { learnCost = ap; return this; }
        public Builder rarity(int r) { rarity = r; return this; }
        public Builder range(double r) { range = r; return this; }
        public Builder damage(double d) { damage = d; return this; }
        public Builder req(Req r, String arg, int level) { req = r; reqArg = arg; reqLevel = level; return this; }
        public Builder level(int l) { reqLevel = l; return this; }
        public Builder uninterruptible() { interruptible = false; return this; }
        public Builder toggle() { toggle = true; return this; }
        public Builder does(Behavior b) { behavior = b; return this; }
        public Builder variant(Variant v) { variants.add(v); return this; }
        public AbilityDef build() { return new AbilityDef(this); }
    }
}
