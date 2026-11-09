package com.mayhem.shinobi.ability;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import org.joml.Vector3f;

/** Visual/audio identity of an element or effect family. */
public record Vis(ParticleOptions main, ParticleOptions accent, ParticleOptions dust, SoundEvent cast, SoundEvent hit) {
    private static DustParticleOptions dust(float r, float g, float b, float s) { return new DustParticleOptions(new Vector3f(r, g, b), s); }

    public static final Vis FIRE = new Vis(ParticleTypes.FLAME, ParticleTypes.LAVA, dust(1f, 0.4f, 0.05f, 1.6f),
            SoundEvents.FIRECHARGE_USE, SoundEvents.GENERIC_EXPLODE);
    public static final Vis WATER = new Vis(ParticleTypes.SPLASH, ParticleTypes.BUBBLE_POP, dust(0.2f, 0.5f, 1f, 1.5f),
            SoundEvents.PLAYER_SPLASH, SoundEvents.GENERIC_SPLASH);
    public static final Vis WIND = new Vis(ParticleTypes.CLOUD, ParticleTypes.SWEEP_ATTACK, dust(0.8f, 1f, 0.9f, 1.2f),
            SoundEvents.ENDER_DRAGON_FLAP, SoundEvents.PLAYER_ATTACK_SWEEP);
    public static final Vis LIGHTNING = new Vis(ParticleTypes.ELECTRIC_SPARK, ParticleTypes.END_ROD, dust(0.7f, 0.8f, 1f, 1.3f),
            SoundEvents.LIGHTNING_BOLT_IMPACT, SoundEvents.LIGHTNING_BOLT_THUNDER);
    public static final Vis EARTH = new Vis(ParticleTypes.POOF, ParticleTypes.CRIT, dust(0.5f, 0.38f, 0.2f, 1.6f),
            SoundEvents.GRAVEL_BREAK, SoundEvents.GENERIC_EXPLODE);
    public static final Vis SHADOW = new Vis(ParticleTypes.SMOKE, ParticleTypes.SOUL, dust(0.12f, 0.05f, 0.2f, 1.5f),
            SoundEvents.ENDERMAN_TELEPORT, SoundEvents.PLAYER_ATTACK_KNOCKBACK);
    public static final Vis CHAKRA = new Vis(ParticleTypes.END_ROD, ParticleTypes.ENCHANTED_HIT, dust(0.35f, 0.7f, 1f, 1.2f),
            SoundEvents.AMETHYST_BLOCK_CHIME, SoundEvents.PLAYER_ATTACK_STRONG);
    public static final Vis BODY = new Vis(ParticleTypes.CRIT, ParticleTypes.POOF, dust(1f, 1f, 1f, 1f),
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.PLAYER_ATTACK_STRONG);
    public static final Vis BEAST = new Vis(ParticleTypes.SOUL_FIRE_FLAME, ParticleTypes.FLAME, dust(1f, 0.25f, 0.1f, 2.0f),
            SoundEvents.WARDEN_ROAR, SoundEvents.GENERIC_EXPLODE);
    public static final Vis ICE = new Vis(ParticleTypes.SNOWFLAKE, ParticleTypes.CLOUD, dust(0.7f, 0.95f, 1f, 1.4f),
            SoundEvents.GLASS_BREAK, SoundEvents.GLASS_BREAK);
    public static final Vis WOOD = new Vis(ParticleTypes.COMPOSTER, ParticleTypes.HAPPY_VILLAGER, dust(0.2f, 0.7f, 0.2f, 1.4f),
            SoundEvents.GRASS_BREAK, SoundEvents.WOOD_BREAK);
    public static final Vis LAVA = new Vis(ParticleTypes.LAVA, ParticleTypes.FLAME, dust(1f, 0.25f, 0f, 1.8f),
            SoundEvents.LAVA_POP, SoundEvents.GENERIC_EXPLODE);

    /** Index of this family for the 3D jutsu renderer (order: FIRE, WATER, WIND, LIGHTNING, EARTH, SHADOW, CHAKRA, BODY, BEAST, ICE, WOOD, LAVA). */
    public int id() {
        Vis[] all = {FIRE, WATER, WIND, LIGHTNING, EARTH, SHADOW, CHAKRA, BODY, BEAST, ICE, WOOD, LAVA};
        for (int i = 0; i < all.length; i++) if (all[i] == this) return i;
        return 6;
    }
}
