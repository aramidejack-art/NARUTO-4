package com.mayhem.shinobi.identity;

import java.util.List;

/** A bloodline: a moveset plus a toggleable Mode with its own resource drain. */
public record Bloodline(String id, String name, int rarity, int unlockLevel, int unlockChapter, String lore,
                        String modeName, String modeDesc, int modeDrainPerSec, List<String> abilities, String icon) {}
