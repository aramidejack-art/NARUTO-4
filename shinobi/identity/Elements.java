package com.mayhem.shinobi.identity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Elements {
    public record Element(String id, String name, String identity, String icon, int color) {}

    public static final Map<String, Element> ALL = new LinkedHashMap<>();
    public static final List<String> STARTERS = List.of("fire", "water", "wind", "lightning", "earth");

    static {
        ALL.put("fire", new Element("fire", "Fire", "Pressure and area denial: jets, delayed fireballs, sweeping arcs, burning zones.", "minecraft:blaze_powder", 0xFF6A1A));
        ALL.put("water", new Element("water", "Water", "Control and flow: fast bullets, rising waves, defensive currents, water paths.", "minecraft:heart_of_the_sea", 0x3A8CFF));
        ALL.put("wind", new Element("wind", "Wind", "Range and displacement: cutting gusts, bursts, pull/push currents, movement streams.", "minecraft:feather", 0xB8FFE0));
        ALL.put("lightning", new Element("lightning", "Lightning", "Fast commitment and precision: dashes, chains, stuns, piercing thrusts.", "minecraft:lightning_rod", 0xB0C8FF));
        ALL.put("earth", new Element("earth", "Earth", "Defence and terrain: stone guard, spike lines, walls, seismic impacts.", "minecraft:stone", 0x9A7B4F));
    }

    public static Element get(String id) { return ALL.get(id); }

    private Elements() {}
}
