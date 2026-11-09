package com.mayhem.shinobi.identity;

import java.util.List;

/** A clan: passive trait, signature mechanic, taijutsu modifier and an exclusive technique tree. */
public record Clan(String id, String name, int rarity, int unlockLevel, String lore, String passive,
                   String signature, String taijutsuMod, List<String> techniques, String icon) {}
