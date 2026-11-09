package com.mayhem.shinobi.data;

/** Active combat style. Gates which ability slots can be cast and which resource regenerates faster. */
public enum Style {
    TAIJUTSU("Taijutsu"),
    NINJUTSU("Ninjutsu");

    public final String label;

    Style(String label) { this.label = label; }

    public Style other() { return this == TAIJUTSU ? NINJUTSU : TAIJUTSU; }
}
