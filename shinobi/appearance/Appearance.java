package com.mayhem.shinobi.appearance;

import com.mayhem.shinobi.data.ShinobiData;
import com.mayhem.shinobi.net.ShinobiNet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCA-style body customisation data: proportions, skin tone, hair, eyes and face marks.
 * The same sanitiser runs on the server (authoritative) and the client (preview); values are always clamped.
 */
public final class Appearance {
    public static final float SCALE_MIN = 0.88F, SCALE_MAX = 1.12F;
    public static final String[] HAIR_STYLES = {"Bald", "Short", "Spiky", "Long", "Ponytail", "Bun", "Mohawk", "Bob"};
    public static final String[] MARKS = {"None", "Cheek lines", "Scar", "Forehead band", "Face paint"};
    public static final int[] SKIN_TONES = {0xFFE6D2, 0xF5CBA7, 0xE0AC69, 0xC68642, 0x8D5524, 0x6B3F1D, 0xFCE5E0, 0xD8B4A0};
    public static final int[] HAIR_COLORS = {0x1B1B1B, 0x3B2A1E, 0x6B4423, 0xB5651D, 0xE6C27A, 0xF2F2F2, 0xC0392B, 0xFF7A00, 0x2E86DE, 0x6C3483, 0x1E8449, 0xFF6FA5};
    public static final int[] EYE_COLORS = {0x2B1B12, 0x6B4423, 0x2E86DE, 0x1E8449, 0x7F8C8D, 0xC0392B, 0x8E44AD, 0xF1C40F};

    /** Shared by server and client so the entity-size event and renderers can read any player's look. */
    public static final Map<UUID, CompoundTag> CACHE = new ConcurrentHashMap<>();

    private Appearance() {}

    public static CompoundTag defaults() {
        CompoundTag t = new CompoundTag();
        t.putFloat("height", 1F); t.putFloat("width", 1F);
        t.putInt("skin", SKIN_TONES[1]); t.putFloat("skinAmt", 0F);
        t.putInt("hairStyle", 1); t.putInt("hairColor", HAIR_COLORS[1]);
        t.putInt("eyeColor", EYE_COLORS[1]); t.putInt("eyeStyle", 0); t.putInt("marks", 0);
        return t;
    }

    private static float clampF(float v, float lo, float hi) { return Float.isNaN(v) ? 1F : Math.max(lo, Math.min(hi, v)); }
    private static int rgb(int v) { return v & 0xFFFFFF; }

    public static CompoundTag sanitize(CompoundTag in) {
        CompoundTag d = defaults();
        CompoundTag out = new CompoundTag();
        out.putFloat("height", clampF(in.contains("height") ? in.getFloat("height") : d.getFloat("height"), SCALE_MIN, SCALE_MAX));
        out.putFloat("width", clampF(in.contains("width") ? in.getFloat("width") : d.getFloat("width"), SCALE_MIN, SCALE_MAX));
        out.putInt("skin", rgb(in.contains("skin") ? in.getInt("skin") : d.getInt("skin")));
        out.putFloat("skinAmt", clampF(in.getFloat("skinAmt"), 0F, 0.6F));
        out.putInt("hairStyle", Math.max(0, Math.min(HAIR_STYLES.length - 1, in.contains("hairStyle") ? in.getInt("hairStyle") : 1)));
        out.putInt("hairColor", rgb(in.contains("hairColor") ? in.getInt("hairColor") : d.getInt("hairColor")));
        out.putInt("eyeColor", rgb(in.contains("eyeColor") ? in.getInt("eyeColor") : d.getInt("eyeColor")));
        out.putInt("eyeStyle", Math.max(0, Math.min(3, in.getInt("eyeStyle"))));
        out.putInt("marks", Math.max(0, Math.min(MARKS.length - 1, in.getInt("marks"))));
        return out;
    }

    public static CompoundTag of(UUID id) {
        CompoundTag t = CACHE.get(id);
        return t == null ? defaults() : t;
    }

    public static boolean has(UUID id) { return CACHE.containsKey(id); }

    /** Server: validate, persist, cache and broadcast a new look. */
    public static void apply(ServerPlayer p, CompoundTag raw) {
        CompoundTag clean = sanitize(raw);
        CompoundTag root = ShinobiData.of(p);
        root.put("appearance", clean);
        root.putBoolean("appearanceSet", true);
        CACHE.put(p.getUUID(), clean);
        p.refreshDimensions();
        ShinobiNet.appearanceToTrackers(p);
        ShinobiNet.syncTag(p);
    }

    /** Server: load persisted look into the cache (login / respawn). */
    public static void load(ServerPlayer p) {
        CompoundTag root = ShinobiData.of(p);
        if (root.getBoolean("appearanceSet")) CACHE.put(p.getUUID(), sanitize(root.getCompound("appearance")));
        p.refreshDimensions();
    }
}
