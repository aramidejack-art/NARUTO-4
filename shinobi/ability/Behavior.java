package com.mayhem.shinobi.ability;

@FunctionalInterface
public interface Behavior {
    void cast(CastContext c);
}
