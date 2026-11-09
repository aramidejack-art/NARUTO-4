package com.mayhem.shinobi;

import com.mayhem.progression.PlayerProgression;
import com.mayhem.shinobi.ability.AbilityCaster;
import com.mayhem.shinobi.ability.AbilityDef;
import com.mayhem.shinobi.ability.Abilities;
import com.mayhem.shinobi.ability.Category;
import com.mayhem.shinobi.ability.Unlocks;
import com.mayhem.shinobi.beast.BeastBond;
import com.mayhem.shinobi.data.ShinobiData;
import com.mayhem.shinobi.data.ShinobiState;
import com.mayhem.shinobi.data.Style;
import com.mayhem.shinobi.identity.Awakenings;
import com.mayhem.shinobi.identity.Bloodline;
import com.mayhem.shinobi.identity.Bloodlines;
import com.mayhem.shinobi.identity.Clan;
import com.mayhem.shinobi.identity.Clans;
import com.mayhem.shinobi.identity.Elements;
import com.mayhem.shinobi.identity.Guardian;
import com.mayhem.shinobi.identity.Modes;
import com.mayhem.shinobi.net.ShinobiNet;
import com.mayhem.shinobi.net.ShinobiNet.Op;
import com.mayhem.shinobi.npc.Dialogues;
import com.mayhem.shinobi.story.MissionTracker;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** The ONLY entry point for client requests. Every request is validated here; the client is never trusted. */
public final class ShinobiActions {
    public static final int STAT_CAP = 50, ADD_ELEMENT_AP = 3, ADD_ELEMENT_LEVEL = 15, CLAN_CHANGE_AP = 5;

    private ShinobiActions() {}

    private static void msg(ServerPlayer p, String s) { p.displayClientMessage(Component.literal(s), true); }

    public static void handle(ServerPlayer p, Op op, String s, int a, int b) {
        if (!p.isAlive() && op != Op.REQUEST_SYNC) return;
        CompoundTag t = ShinobiData.of(p);
        ShinobiState st = ShinobiState.of(p.getUUID());
        int level = PlayerProgression.getLevel(p);
        switch (op) {
            case REQUEST_SYNC -> { ShinobiNet.syncTag(p); ShinobiNet.sendResources(p, true); return; }
            case SELECT_ELEMENT -> selectElement(p, t, s);
            case ADD_ELEMENT -> addElement(p, t, s, level);
            case SELECT_CLAN -> selectClan(p, t, s, level);
            case SELECT_BLOODLINE -> selectBloodline(p, t, s, level);
            case LEARN -> learn(p, t, s, level);
            case ASSIGN -> assign(p, t, s, a);
            case CLEAR_SLOT -> ShinobiData.setSlot(t, a, "");
            case LOAD_PRESET -> { if (!ShinobiData.loadPreset(t, a)) msg(p, "Preset " + (a + 1) + " is empty"); }
            case SAVE_PRESET -> { ShinobiData.savePreset(t, a); msg(p, "Saved preset " + (a + 1)); }
            case SWITCH_STYLE -> {
                Style ns = ShinobiData.style(t).other();
                ShinobiData.style(t, ns);
                AbilityCaster.interrupt(p);
                msg(p, "Style: " + ns.label);
            }
            case CAST -> { AbilityCaster.castSlot(p, a, b); return; } // cast sync is handled by the caster
            case SET_VARIANT -> setVariant(p, t, s);
            case SPEND_STAT -> spendStat(p, t, s);
            case ACCEPT_MISSION -> { MissionTracker.accept(p, s); return; }
            case ABANDON_MISSION -> { MissionTracker.abandon(p, s); return; }
            case CLAIM_MISSION -> { MissionTracker.claim(p, s); return; }
            case TOGGLE_MODE -> Modes.toggle(p);
            case TOGGLE_CLOAK -> BeastBond.toggleCloak(p);
            case CHARGE_ON -> { if (st.casting.isEmpty()) st.charging = true; }
            case CHARGE_OFF -> st.charging = false;
            case CHALLENGE_BEAST -> BeastBond.challenge(p, s);
            case DIALOGUE -> { Dialogues.choose(p, a, b); return; }
            case AWAKEN -> Awakenings.toggle(p, level);
            case AWAKEN_STAGE -> Awakenings.cycle(p, level);
            case GUARDIAN_TOGGLE -> Guardian.toggle(p, level);
            case GUARDIAN_STAGE -> Guardian.cycle(p, level);
            case AVATAR_STANCE -> com.mayhem.shinobi.avatar.AvatarManager.stance(p);
            case AVATAR_PUNCH -> { com.mayhem.shinobi.avatar.AvatarManager.punch(p); return; }
            case BEAST_MODE -> com.mayhem.shinobi.avatar.ChakraMode.toggle(p);
            case MODE_SKILL -> { com.mayhem.shinobi.avatar.ChakraMode.cast(p, a == 0 ? 0 : 1); return; }
            case AVATAR_TACKLE -> com.mayhem.shinobi.avatar.AvatarManager.tackle(p);
            case ONBOARD_DONE -> { if (!ShinobiData.element(t).isEmpty()) t.putBoolean("onboarded", true); }
            case SELECT_BEAST_INFO -> { return; }
        }
        ShinobiNet.syncTag(p);
        ShinobiNet.sendResources(p, true);
    }

    // ------------------------------------------------------------------ identity
    private static void selectElement(ServerPlayer p, CompoundTag t, String id) {
        if (!ShinobiData.element(t).isEmpty()) { msg(p, "Your primary element is already chosen"); return; }
        if (!Elements.STARTERS.contains(id)) return;
        t.putString("element", id);
        ShinobiData.add(t, "elements", id);
        grantStarters(t, id);
        msg(p, Elements.get(id).name() + " affinity awakened");
    }

    /** Starter kit: the first three level-1 jutsu of the element and the basic taijutsu. Auto-slotted. */
    private static void grantStarters(CompoundTag t, String element) {
        int slot = 0;
        for (AbilityDef d : Abilities.byCategory(Category.ELEMENTAL)) {
            if (!element.equals(d.element) || d.reqLevel > 1 || slot >= 3) continue;
            ShinobiData.add(t, "owned", d.id);
            if (ShinobiData.slot(t, slot).isEmpty()) ShinobiData.setSlot(t, slot, d.id);
            slot++;
        }
        for (String mv : new String[]{"mv_water", "mv_wall"}) {
            if (Abilities.get(mv) != null) ShinobiData.add(t, "owned", mv);
        }
        if (ShinobiData.slot(t, 8).isEmpty()) ShinobiData.setSlot(t, 8, "mv_water");
        if (ShinobiData.slot(t, 9).isEmpty()) ShinobiData.setSlot(t, 9, "mv_wall");
        int n = 0;
        for (AbilityDef d : Abilities.byCategory(Category.TAIJUTSU)) {
            if (d.reqLevel > 1 || n >= 3) continue;
            ShinobiData.add(t, "owned", d.id);
            if (ShinobiData.slot(t, 5 + n).isEmpty()) ShinobiData.setSlot(t, 5 + n, d.id);
            n++;
        }
    }

    private static void addElement(ServerPlayer p, CompoundTag t, String id, int level) {
        if (!Elements.ALL.containsKey(id) || ShinobiData.has(t, "elements", id)) return;
        if (level < ADD_ELEMENT_LEVEL) { msg(p, "A second element needs level " + ADD_ELEMENT_LEVEL); return; }
        if (ShinobiData.ap(t) < ADD_ELEMENT_AP) { msg(p, "Needs " + ADD_ELEMENT_AP + " ability points"); return; }
        t.putInt("ap", ShinobiData.ap(t) - ADD_ELEMENT_AP);
        ShinobiData.add(t, "elements", id);
        msg(p, Elements.get(id).name() + " affinity added");
    }

    private static void selectClan(ServerPlayer p, CompoundTag t, String id, int level) {
        Clan c = Clans.ALL.get(id);
        if (c == null || id.equals(ShinobiData.clan(t))) return;
        if (level < c.unlockLevel()) { msg(p, c.name() + " needs level " + c.unlockLevel()); return; }
        boolean first = ShinobiData.clan(t).isEmpty();
        if (!first) {
            if (ShinobiData.ap(t) < CLAN_CHANGE_AP) { msg(p, "Changing clan costs " + CLAN_CHANGE_AP + " ability points"); return; }
            t.putInt("ap", ShinobiData.ap(t) - CLAN_CHANGE_AP);
        }
        t.putString("clan", id);
        msg(p, "Clan: " + c.name());
    }

    private static void selectBloodline(ServerPlayer p, CompoundTag t, String id, int level) {
        if (id.isEmpty()) { t.putString("bloodline", ""); t.putString("mode", ""); return; }
        Bloodline b = Bloodlines.get(id);
        if (b == null) return;
        if (!ShinobiData.has(t, "bloodlines", id)) {
            if (level < b.unlockLevel()) { msg(p, b.name() + " needs level " + b.unlockLevel()); return; }
            if (t.getInt("storyDone") < b.unlockChapter()) { msg(p, b.name() + " needs story chapter " + b.unlockChapter()); return; }
            ShinobiData.add(t, "bloodlines", id);
        }
        t.putString("bloodline", id);
        t.putString("mode", "");
        msg(p, "Bloodline: " + b.name());
    }

    // ------------------------------------------------------------------ abilities
    private static void learn(ServerPlayer p, CompoundTag t, String id, int level) {
        AbilityDef d = Abilities.get(id);
        if (d == null || ShinobiData.owns(t, id)) return;
        String miss = Unlocks.missing(d, t, level);
        if (miss != null) { msg(p, miss); return; }
        if (ShinobiData.ap(t) < d.learnCost) { msg(p, "Needs " + d.learnCost + " ability points"); return; }
        t.putInt("ap", ShinobiData.ap(t) - d.learnCost);
        ShinobiData.add(t, "owned", id);
        ShinobiNet.banner(p, 3, d.name, "New technique learned");
    }

    private static void assign(ServerPlayer p, CompoundTag t, String id, int slot) {
        AbilityDef d = Abilities.get(id);
        if (d == null || !ShinobiData.owns(t, id) || slot < 0 || slot >= ShinobiData.ALL_SLOTS) return;
        boolean ok;
        if (slot == ShinobiData.SLOT_WEAPON) ok = d.category == Category.WEAPON;
        else if (slot == ShinobiData.SLOT_SUPPORT) ok = d.category == Category.MEDICAL || d.category == Category.SUMMON || d.category == Category.DEFENSE;
        else if (slot == ShinobiData.SLOT_ULTIMATE) ok = d.category == Category.ULTIMATE;
        else ok = d.category != Category.ULTIMATE;
        if (!ok) { msg(p, d.name + " does not fit that slot"); return; }
        // one copy per loadout: moving an ability empties its old slot
        for (int i = 0; i < ShinobiData.ALL_SLOTS; i++) if (id.equals(ShinobiData.slot(t, i))) ShinobiData.setSlot(t, i, "");
        ShinobiData.setSlot(t, slot, id);
    }

    /** s = "abilityId:variantId" (empty variant = base). */
    private static void setVariant(ServerPlayer p, CompoundTag t, String s) {
        int i = s.indexOf(':');
        if (i < 0) return;
        String id = s.substring(0, i), vid = s.substring(i + 1);
        AbilityDef d = Abilities.get(id);
        if (d == null || !ShinobiData.owns(t, id)) return;
        if (vid.isEmpty()) { ShinobiData.setVariant(t, id, ""); return; }
        var v = d.variant(vid);
        if (v == null) return;
        if (ShinobiData.mastery(t, id) < v.needMastery()) { msg(p, "Variant needs " + v.needMastery() + " mastery uses"); return; }
        ShinobiData.setVariant(t, id, vid);
    }

    private static void spendStat(ServerPlayer p, CompoundTag t, String name) {
        boolean known = false;
        for (String k : ShinobiData.STATS) if (k.equals(name)) known = true;
        if (!known || ShinobiData.sp(t) <= 0 || ShinobiData.stat(t, name) >= STAT_CAP) return;
        t.putInt("sp", ShinobiData.sp(t) - 1);
        ShinobiData.addStat(t, name);
    }
}
