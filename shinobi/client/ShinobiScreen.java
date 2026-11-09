package com.mayhem.shinobi.client;

import com.mayhem.progression.PlayerProgression;
import com.mayhem.shinobi.ability.Abilities;
import com.mayhem.shinobi.ability.AbilityDef;
import com.mayhem.shinobi.ability.BeastStages;
import com.mayhem.shinobi.ability.Category;
import com.mayhem.shinobi.ability.Unlocks;
import com.mayhem.shinobi.ability.Variant;
import com.mayhem.shinobi.beast.Beasts;
import com.mayhem.shinobi.data.ShinobiData;
import com.mayhem.shinobi.identity.Bloodline;
import com.mayhem.shinobi.identity.Bloodlines;
import com.mayhem.shinobi.identity.Clan;
import com.mayhem.shinobi.identity.Clans;
import com.mayhem.shinobi.identity.Elements;
import com.mayhem.shinobi.net.ShinobiNet.Op;
import com.mayhem.shinobi.story.MissionDef;
import com.mayhem.shinobi.story.MissionTracker;
import com.mayhem.shinobi.story.Missions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The Shinobi menu (M). Every tab is a scrolling list of rows with up to two actions; each action is only a REQUEST to the
 * server, which validates it. The screen draws from the synced mirror, so it updates when the server answers.
 */
public class ShinobiScreen extends Screen {
    private enum Tab {
        CHARACTER("Character"), JUTSU("Jutsu"), ELEMENTS("Elements"), CLANS("Clans"), BLOODLINES("Bloodlines"),
        BEASTS("Beasts"), MISSIONS("Missions"), GUIDE("Guide");
        final String label;
        Tab(String l) { label = l; }
    }

    private record Row(String title, String sub, int color, String b1, Runnable a1, String b2, Runnable a2) {}

    private static Tab tab = Tab.CHARACTER;
    private static Category filter = Category.ELEMENTAL;
    private static int scroll;
    private int slotSel = 0;
    private final List<Row> rows = new ArrayList<>();
    private static final int ROW_H = 34;

    public ShinobiScreen() { super(Component.literal("Shinobi")); }

    @Override protected void init() { rebuild(); }

    private CompoundTag t() { return ClientShinobi.tag; }

    private int level() { return minecraft != null && minecraft.player != null ? PlayerProgression.getLevel(minecraft.player) : 1; }

    private void rebuild() {
        clearWidgets();
        int x0 = (width - 420) / 2, y0 = 16;
        Tab[] tabs = Tab.values();
        for (int i = 0; i < tabs.length; i++) {
            final Tab tb = tabs[i];
            Button b = Button.builder(Component.literal(tb.label), btn -> { tab = tb; scroll = 0; rebuild(); })
                    .bounds(x0 + i * 52, y0, 51, 16).build();
            b.active = tb != tab;
            addRenderableWidget(b);
        }
        rows.clear();
        switch (tab) {
            case CHARACTER -> character();
            case JUTSU -> jutsu();
            case ELEMENTS -> elements();
            case CLANS -> clans();
            case BLOODLINES -> bloodlines();
            case BEASTS -> beasts();
            case MISSIONS -> missions();
            case GUIDE -> guide();
        }
        int vis = visibleRows();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - vis)));
        int top = listTop();
        for (int i = 0; i < vis && i + scroll < rows.size(); i++) {
            Row r = rows.get(i + scroll);
            int y = top + i * ROW_H;
            if (r.b1() != null) addRenderableWidget(Button.builder(Component.literal(r.b1()), b -> { r.a1().run(); }).bounds(x0 + 330, y + 2, 88, 14).build());
            if (r.b2() != null) addRenderableWidget(Button.builder(Component.literal(r.b2()), b -> { r.a2().run(); }).bounds(x0 + 330, y + 18, 88, 14).build());
        }
        if (tab == Tab.JUTSU) {
            for (int i = 0; i < Category.values().length; i++) {
                final Category c = Category.values()[i];
                Button b = Button.builder(Component.literal(c.label.substring(0, Math.min(4, c.label.length()))), btn -> { filter = c; scroll = 0; rebuild(); })
                        .bounds(x0 + i * 38, y0 + 100, 37, 14).build();
                b.active = c != filter;
                addRenderableWidget(b);
            }
        }
    }

    private int listTop() { return tab == Tab.JUTSU ? 140 : 44; }
    private int visibleRows() { return Math.max(1, (height - listTop() - 10) / ROW_H); }

    /** Called after a server answer so lists reflect new state. */
    @Override public void tick() {
        if (t().hashCode() != lastHash) { lastHash = t().hashCode(); rebuild(); }
    }
    private int lastHash;

    @Override public boolean mouseScrolled(double mx, double my, double d) {
        scroll = Math.max(0, scroll - (int) Math.signum(d));
        rebuild();
        return true;
    }

    // ------------------------------------------------------------------ tabs
    private void character() {
        CompoundTag t = t();
        rows.add(new Row("Level " + level() + "   AP " + ShinobiData.ap(t) + "   SP " + ShinobiData.sp(t),
                "Style: " + (ClientShinobi.ninjutsu() ? "Ninjutsu" : "Taijutsu") + " (F)", 0xFFFFFFFF,
                "Switch style", () -> ClientShinobi.send(Op.SWITCH_STYLE), "Appearance", () -> minecraft.setScreen(new AppearanceScreen())));
        for (String s : ShinobiData.STATS) {
            final String stat = s;
            rows.add(new Row(Character.toUpperCase(s.charAt(0)) + s.substring(1) + ": " + ShinobiData.stat(t, s),
                    statHelp(s), 0xFFCCE0FF, "+1 (1 SP)", () -> ClientShinobi.send(Op.SPEND_STAT, stat, 0, 0), null, null));
        }
        String bl = ShinobiData.bloodline(t);
        rows.add(new Row("Element: " + ShinobiData.element(t) + "   Clan: " + ShinobiData.clan(t) + "   Bloodline: " + bl,
                "Chakra Charge: hold C. Mode: Y. Awaken: J. Guardian: N. Beast Cloak: H.", 0xFFAAAAAA,
                "Toggle mode", () -> ClientShinobi.send(Op.TOGGLE_MODE), "Charge on/off", () -> ClientShinobi.send(ClientShinobi.charging() ? Op.CHARGE_OFF : Op.CHARGE_ON)));
        rows.add(new Row("Awakening / Guardian", "Awaken (J) boosts you for a while; Guardian (N) is the Mirror Eye avatar.", 0xFFAAAAAA,
                "Awaken", () -> ClientShinobi.send(Op.AWAKEN), "Guardian", () -> ClientShinobi.send(Op.GUARDIAN_TOGGLE)));
        rows.add(new Row("Awakening stage / Guardian stage", "Cycle which stage the next activation uses.", 0xFFAAAAAA,
                "Awaken stage", () -> ClientShinobi.send(Op.AWAKEN_STAGE), "Guardian stage", () -> ClientShinobi.send(Op.GUARDIAN_STAGE)));
    }

    private static String statHelp(String s) {
        return switch (s) {
            case "chakra" -> "More maximum chakra"; case "stamina" -> "More maximum stamina";
            case "power" -> "+1% damage per point"; default -> "Faster movement";
        };
    }

    private void jutsu() {
        CompoundTag t = t();
        for (AbilityDef d : Abilities.byCategory(filter)) {
            boolean owned = ShinobiData.owns(t, d.id);
            String miss = Unlocks.missing(d, t, level());
            int mastery = ShinobiData.mastery(t, d.id);
            String sub = d.desc.length() > 60 ? d.desc.substring(0, 58) + ".." : d.desc;
            String info = owned ? "Mastery " + mastery : (miss != null ? miss : "Cost " + d.learnCost + " AP");
            Runnable equip = () -> ClientShinobi.send(Op.ASSIGN, d.id, slotSel, 0);
            String b2 = null; Runnable a2 = null;
            if (owned && !d.variants.isEmpty()) {
                String cur = ShinobiData.variant(t, d.id);
                Variant next = nextVariant(d, cur, mastery);
                b2 = next == null ? "Base form" : next.name();
                final String vid = next == null ? "" : next.id();
                a2 = () -> ClientShinobi.send(Op.SET_VARIANT, d.id + ":" + vid, 0, 0);
            }
            rows.add(new Row(d.name + "  [" + info + "]", sub, owned ? 0xFFFFFFFF : miss != null ? 0xFF777777 : 0xFFFFE08A,
                    owned ? "Equip slot " + (slotSel + 1) : "Learn", owned ? equip : () -> ClientShinobi.send(Op.LEARN, d.id, 0, 0), b2, a2));
        }
    }

    private static Variant nextVariant(AbilityDef d, String cur, int mastery) {
        List<Variant> unlocked = new ArrayList<>();
        for (Variant v : d.variants) if (mastery >= v.needMastery()) unlocked.add(v);
        if (unlocked.isEmpty()) return null;
        int idx = -1;
        for (int i = 0; i < unlocked.size(); i++) if (unlocked.get(i).id().equals(cur)) idx = i;
        return idx + 1 >= unlocked.size() ? null : unlocked.get(idx + 1);
    }

    private void elements() {
        CompoundTag t = t();
        for (Elements.Element e : Elements.ALL.values()) {
            boolean has = ShinobiData.has(t, "elements", e.id());
            boolean primary = ShinobiData.element(t).isEmpty();
            rows.add(new Row(e.name() + (has ? "  (owned)" : ""), e.identity(), 0xFF000000 | e.color(),
                    has ? null : primary ? "Choose" : "Add (3 AP, Lv15)", () -> ClientShinobi.send(primary ? Op.SELECT_ELEMENT : Op.ADD_ELEMENT, e.id(), 0, 0), null, null));
        }
    }

    private void clans() {
        CompoundTag t = t();
        for (Clan c : Clans.ALL.values()) {
            boolean cur = c.id().equals(ShinobiData.clan(t));
            rows.add(new Row(c.name() + (cur ? "  (current)" : "  Lv" + c.unlockLevel()), c.passive(), cur ? 0xFFFFE08A : 0xFFFFFFFF,
                    cur ? null : "Join", () -> ClientShinobi.send(Op.SELECT_CLAN, c.id(), 0, 0), null, null));
        }
    }

    private void bloodlines() {
        CompoundTag t = t();
        rows.add(new Row("No bloodline", "Unequip your bloodline", 0xFFAAAAAA, "Unequip", () -> ClientShinobi.send(Op.SELECT_BLOODLINE, "", 0, 0), null, null));
        for (Bloodline b : Bloodlines.ALL.values()) {
            boolean cur = b.id().equals(ShinobiData.bloodline(t));
            rows.add(new Row(b.name() + (cur ? "  (equipped)" : "  Lv" + b.unlockLevel() + " / ch" + b.unlockChapter()),
                    b.modeName() + ": " + b.modeDesc(), cur ? 0xFFFFE08A : 0xFFFFFFFF,
                    cur ? null : "Equip", () -> ClientShinobi.send(Op.SELECT_BLOODLINE, b.id(), 0, 0), null, null));
        }
    }

    private void beasts() {
        CompoundTag t = t();
        String mine = ShinobiData.beast(t);
        rows.add(new Row("Bond: " + (mine.isEmpty() ? "none" : Beasts.get(mine).name()) + "  stage " + BeastStages.stage(t) + " (" + BeastStages.NAMES[BeastStages.stage(t)] + ")",
                "Bond " + ShinobiData.bond(t) + ". Weaken a beast to its knees, then use a Sealing Scroll.", 0xFFFFFFFF,
                "Cloak (H)", () -> ClientShinobi.send(Op.TOGGLE_CLOAK), null, null));
        for (Beasts.Beast b : Beasts.ALL.values()) {
            rows.add(new Row(b.name() + "  Lv" + b.minLevel(), b.epithet() + " - " + b.perk(), 0xFF000000 | b.color(),
                    "Challenge", () -> ClientShinobi.send(Op.CHALLENGE_BEAST, b.id(), 0, 0), null, null));
        }
    }

    private void missions() {
        CompoundTag t = t();
        MissionDef next = MissionTracker.nextStory(t);
        for (MissionDef d : Missions.ALL.values()) {
            if (d.story() && (next == null || !next.id().equals(d.id())) && MissionTracker.state(t, d.id()) < 0) continue;
            int st = MissionTracker.state(t, d.id());
            String state = st == 0 ? " [" + MissionTracker.progress(t, d.id()) + "/" + d.target() + "]" : st == 1 ? " [COMPLETE]" : st == 2 ? " [done]" : "";
            String b1 = st == 1 ? "Claim" : st == 0 ? "Abandon" : "Accept";
            Op op = st == 1 ? Op.CLAIM_MISSION : st == 0 ? Op.ABANDON_MISSION : Op.ACCEPT_MISSION;
            rows.add(new Row((d.story() ? "STORY: " : "") + d.rank() + " " + d.title() + state, d.objectiveText() + "  (+" + d.xp() + " xp)",
                    st == 1 ? 0xFF8CFF8C : d.story() ? 0xFFFFE08A : 0xFFFFFFFF, b1, () -> ClientShinobi.send(op, d.id(), 0, 0), null, null));
        }
    }

    private void guide() {
        String[][] g = {
                {"R", "Toggle Combat Mode: M1 becomes the strike, held items are ignored"},
                {"1-9 / Wheel / Alt", "In combat mode: cast slots 1-9; wheel selects a slot; Alt casts it"},
                {"F", "Switch Taijutsu / Ninjutsu style (some jutsu need a style)"},
                {"C (hold)", "Charge chakra"}, {"Y", "Bloodline mode"}, {"H", "Beast cloak"}, {"J", "Awakening"}, {"N", "Mirror Eye Guardian"},
                {"V B G X Z K", "Dodge, Guard, Slam, Ultimate, Lock-on, Status"},
                {"M", "This menu"}};
        for (String[] r : g) rows.add(new Row(r[0], r[1], 0xFFFFFFFF, null, null, null, null));
    }

    // ------------------------------------------------------------------ drawing
    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int x0 = (width - 420) / 2;
        g.fill(x0 - 6, 10, x0 + 426, height - 6, 0xCC0B0F1A);
        if (tab == Tab.JUTSU) drawLoadout(g, x0, mx, my);
        int top = listTop();
        int vis = visibleRows();
        for (int i = 0; i < vis && i + scroll < rows.size(); i++) {
            Row r = rows.get(i + scroll);
            int y = top + i * ROW_H;
            g.fill(x0, y, x0 + 324, y + ROW_H - 2, 0x66222B44);
            g.drawString(font, r.title(), x0 + 4, y + 5, r.color(), false);
            g.drawString(font, font.plainSubstrByWidth(r.sub(), 316), x0 + 4, y + 18, 0xFF9AA4BF, false);
        }
        g.drawString(font, "AP " + ShinobiData.ap(t()) + "  SP " + ShinobiData.sp(t()) + "  Lv " + level(), x0, height - 16, 0xFFE6C27A, false);
        super.render(g, mx, my, pt);
    }

    private void drawLoadout(GuiGraphics g, int x0, int mx, int my) {
        g.drawString(font, "Loadout (click a slot, then Equip). Presets:", x0, 40, 0xFFFFFFFF, false);
        for (int i = 0; i < ShinobiData.ALL_SLOTS; i++) {
            int cx = x0 + (i % 10) * 40, cy = 54 + (i / 10) * 24;
            g.fill(cx, cy, cx + 38, cy + 20, i == slotSel ? 0xFF3A3A66 : 0xFF1B2238);
            AbilityDef d = Abilities.get(ShinobiData.slot(t(), i));
            String lab = i >= 10 ? new String[]{"Wpn", "Sup", "Ult"}[i - 10] : String.valueOf(i + 1);
            g.drawString(font, lab, cx + 2, cy + 1, 0xFF8899BB, false);
            if (d != null) g.drawString(font, font.plainSubstrByWidth(d.name, 36), cx + 2, cy + 11, 0xFFFFFFFF, false);
        }
        for (int i = 0; i < ShinobiData.PRESETS; i++) {
            g.drawString(font, "P" + (i + 1) + (ShinobiData.presetSaved(t(), i) ? "*" : ""), x0 + 220 + i * 40, 40, 0xFFE6C27A, false);
        }
    }

    @Override public boolean mouseClicked(double mx, double my, int btn) {
        if (tab == Tab.JUTSU) {
            int x0 = (width - 420) / 2;
            for (int i = 0; i < ShinobiData.ALL_SLOTS; i++) {
                int cx = x0 + (i % 10) * 40, cy = 54 + (i / 10) * 24;
                if (mx >= cx && mx < cx + 38 && my >= cy && my < cy + 20) {
                    if (btn == 1) ClientShinobi.send(Op.CLEAR_SLOT, "", i, 0);
                    else slotSel = i;
                    rebuild();
                    return true;
                }
            }
            for (int i = 0; i < ShinobiData.PRESETS; i++) {
                int px = x0 + 220 + i * 40;
                if (my >= 38 && my < 50 && mx >= px && mx < px + 36) {
                    ClientShinobi.send(btn == 1 ? Op.SAVE_PRESET : Op.LOAD_PRESET, "", i, 0);
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override public boolean isPauseScreen() { return false; }
}
