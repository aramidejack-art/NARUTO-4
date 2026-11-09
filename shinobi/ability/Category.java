package com.mayhem.shinobi.ability;

public enum Category {
    ELEMENTAL("Elemental"), TAIJUTSU("Taijutsu"), BLOODLINE("Bloodline"), CLAN("Clan"), MEDICAL("Medical"),
    WEAPON("Weapon"), SUMMON("Summon"), DEFENSE("Defense"), MOVEMENT("Movement"), ULTIMATE("Ultimate"), BEAST("Beast");

    public final String label;
    Category(String label) { this.label = label; }
}
