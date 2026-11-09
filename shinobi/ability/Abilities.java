package com.mayhem.shinobi.ability;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registry of every ability. Catalog classes register into it; ids are unique and stable (they are saved in player data). */
public final class Abilities {
    private static final Map<String, AbilityDef> ALL = new LinkedHashMap<>();
    private static boolean loaded;

    private Abilities() {}

    public static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        ElementalCatalog.register();
        TaijutsuCatalog.register();
        SupportCatalog.register();
        MovementCatalog.register();
        IdentityCatalog.register();
        BeastCatalog.register();
        UltimateCatalog.register();
    }

    public static void add(AbilityDef d) {
        if (ALL.putIfAbsent(d.id, d) != null) throw new IllegalStateException("Duplicate ability id " + d.id);
    }

    public static AbilityDef get(String id) { ensureLoaded(); return id == null ? null : ALL.get(id); }
    public static Collection<AbilityDef> all() { ensureLoaded(); return ALL.values(); }

    public static List<AbilityDef> byCategory(Category c) {
        List<AbilityDef> out = new ArrayList<>();
        for (AbilityDef d : all()) if (d.category == c) out.add(d);
        return out;
    }
}
