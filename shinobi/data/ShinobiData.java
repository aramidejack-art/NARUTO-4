package com.mayhem.shinobi.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * All persistent shinobi state for one player lives in a single CompoundTag. Every helper takes the CompoundTag so the
 * server (authoritative) and the client mirror (read-only, synced by packet) use identical accessors.
 */
public final class ShinobiData {
    public static final String ROOT = "mayhem_shinobi";
    public static final int SLOTS = 10;          // ordinary jutsu slots
    public static final int SLOT_WEAPON = 10, SLOT_SUPPORT = 11, SLOT_ULTIMATE = 12;
    public static final int ALL_SLOTS = 13;
    public static final int PRESETS = 5;
    public static final String[] STATS = {"chakra", "stamina", "power", "speed"};

    private ShinobiData() {}

    // ------------------------------------------------------------------ root handling
    public static CompoundTag of(Player p) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(ROOT, Tag.TAG_COMPOUND)) pd.put(ROOT, new CompoundTag());
        return pd.getCompound(ROOT);
    }

    public static void copy(Player from, Player to) {
        CompoundTag old = from.getPersistentData();
        if (old.contains(ROOT, Tag.TAG_COMPOUND)) to.getPersistentData().put(ROOT, old.getCompound(ROOT).copy());
    }

    // ------------------------------------------------------------------ simple fields
    public static String str(CompoundTag t, String k) { return t.getString(k); }
    public static String element(CompoundTag t) { return t.getString("element"); }
    public static String clan(CompoundTag t) { return t.getString("clan"); }
    public static String bloodline(CompoundTag t) { return t.getString("bloodline"); }
    public static String mode(CompoundTag t) { return t.getString("mode"); }
    public static boolean onboarded(CompoundTag t) { return t.getBoolean("onboarded"); }
    public static Style style(CompoundTag t) { return "NINJUTSU".equals(t.getString("style")) ? Style.NINJUTSU : Style.TAIJUTSU; }
    public static void style(CompoundTag t, Style s) { t.putString("style", s.name()); }

    public static int ap(CompoundTag t) { return t.getInt("ap"); }
    public static int sp(CompoundTag t) { return t.getInt("sp"); }
    public static int stat(CompoundTag t, String name) { return t.getCompound("stats").getInt(name); }
    public static void addStat(CompoundTag t, String name) {
        CompoundTag s = t.getCompound("stats");
        s.putInt(name, s.getInt(name) + 1);
        t.put("stats", s);
    }

    // ------------------------------------------------------------------ string sets (owned abilities, unlocked bloodlines, etc.)
    public static Set<String> set(CompoundTag t, String key) {
        Set<String> out = new LinkedHashSet<>();
        ListTag l = t.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < l.size(); i++) out.add(l.getString(i));
        return out;
    }

    public static boolean has(CompoundTag t, String key, String value) { return set(t, key).contains(value); }

    public static boolean add(CompoundTag t, String key, String value) {
        Set<String> s = set(t, key);
        if (!s.add(value)) return false;
        ListTag l = new ListTag();
        for (String v : s) l.add(StringTag.valueOf(v));
        t.put(key, l);
        return true;
    }

    public static boolean remove(CompoundTag t, String key, String value) {
        Set<String> s = set(t, key);
        if (!s.remove(value)) return false;
        ListTag l = new ListTag();
        for (String v : s) l.add(StringTag.valueOf(v));
        t.put(key, l);
        return true;
    }

    public static boolean owns(CompoundTag t, String abilityId) { return has(t, "owned", abilityId); }

    // ------------------------------------------------------------------ mastery and variants
    public static int mastery(CompoundTag t, String abilityId) { return t.getCompound("mastery").getInt(abilityId); }
    public static void addMastery(CompoundTag t, String abilityId, int n) {
        CompoundTag m = t.getCompound("mastery");
        m.putInt(abilityId, m.getInt(abilityId) + n);
        t.put("mastery", m);
    }
    public static String variant(CompoundTag t, String abilityId) { return t.getCompound("variant").getString(abilityId); }
    public static void setVariant(CompoundTag t, String abilityId, String variantId) {
        CompoundTag m = t.getCompound("variant");
        m.putString(abilityId, variantId);
        t.put("variant", m);
    }

    // ------------------------------------------------------------------ slots + presets
    public static String slot(CompoundTag t, int i) {
        if (i < 0 || i >= ALL_SLOTS) return "";
        ListTag l = t.getList("slots", Tag.TAG_STRING);
        return i < l.size() ? l.getString(i) : "";
    }

    public static void setSlot(CompoundTag t, int i, String id) {
        if (i < 0 || i >= ALL_SLOTS) return;
        List<String> cur = new ArrayList<>();
        for (int k = 0; k < ALL_SLOTS; k++) cur.add(slot(t, k));
        cur.set(i, id == null ? "" : id);
        ListTag l = new ListTag();
        for (String s : cur) l.add(StringTag.valueOf(s));
        t.put("slots", l);
    }

    public static void savePreset(CompoundTag t, int idx) {
        if (idx < 0 || idx >= PRESETS) return;
        CompoundTag p = t.getCompound("presets");
        p.put("p" + idx, t.getList("slots", Tag.TAG_STRING).copy());
        t.put("presets", p);
    }

    public static boolean loadPreset(CompoundTag t, int idx) {
        if (idx < 0 || idx >= PRESETS) return false;
        CompoundTag p = t.getCompound("presets");
        if (!p.contains("p" + idx, Tag.TAG_LIST)) return false;
        ListTag l = p.getList("p" + idx, Tag.TAG_STRING);
        ListTag out = new ListTag();
        for (int i = 0; i < ALL_SLOTS; i++) out.add(StringTag.valueOf(i < l.size() ? l.getString(i) : ""));
        t.put("slots", out);
        t.putInt("preset", idx);
        return true;
    }

    public static boolean presetSaved(CompoundTag t, int idx) { return t.getCompound("presets").contains("p" + idx); }
    public static int activePreset(CompoundTag t) { return t.getInt("preset"); }

    // ------------------------------------------------------------------ beast
    public static String beast(CompoundTag t) { return t.getCompound("beast").getString("id"); }
    public static int bond(CompoundTag t) { return t.getCompound("beast").getInt("bond"); }
    public static boolean cloak(CompoundTag t) { return t.getCompound("beast").getBoolean("cloak"); }

    // ------------------------------------------------------------------ story + missions (raw access; logic in story package)
    public static CompoundTag story(CompoundTag t) { return t.getCompound("story"); }
    public static CompoundTag missions(CompoundTag t) { return t.getCompound("missions"); }
    public static CompoundTag appearance(CompoundTag t) { return t.getCompound("appearance"); }
}
