package com.mayhem.shinobi.npc;

import com.mayhem.shinobi.net.ShinobiNet;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/** Stateless dialogue for wandering shinobi: options are re-derived from the NPC's mood on every call, the client sends only an index. */
public final class WandererDialogue {
    private static final String ACCEPT = "Accept the duel", ASK = "Ask about the road", SPAR = "Spar with me", WHO = "Who are you?",
            AWAY = "Walk away", BYE = "Farewell";
    private static final String[] RUMOURS = {
            "Beast shrines hide in the wilds. Their guardians test the bold.",
            "A shinobi who walks without a plan is just a wanderer. Take contracts at a mission board and grow stronger.",
            "Watch for the tell before a strike. If you can read it, you can step out of its way.",
            "Charge your chakra before a hard fight, and keep some in reserve for walking on walls and water.",
            "Bond with a tailed beast and its chakra will answer you. The deeper the bond, the more of it you can wear.",
            "Seal a weakened beast with a sealing scroll. A strong one will not stay sealed unless it respects you."};

    private WandererDialogue() {}

    private record View(String text, List<String> options) {}

    private static View view(WandererEntity w) {
        List<String> o = new ArrayList<>();
        String text;
        if (w.mood() == WandererEntity.Mood.HOSTILE) {
            text = "...";
            o.add(BYE);
        } else if (w.mood() == WandererEntity.Mood.DUELIST) {
            text = w.greeting();
            o.add(ACCEPT); o.add(WHO); o.add(AWAY);
        } else {
            text = w.friendlyLine();
            o.add(ASK); o.add(SPAR); o.add(WHO); o.add(BYE);
        }
        return new View(text, o);
    }

    public static void open(ServerPlayer p, WandererEntity w) { open(p, w, null); }

    private static void open(ServerPlayer p, WandererEntity w, String override) {
        View v = view(w);
        ShinobiNet.dialogue(p, w.getId(), w.displayName(), override != null ? override : v.text(), v.options());
    }

    private static void close(ServerPlayer p) { ShinobiNet.dialogue(p, -1, "", "", List.of()); }

    public static void choose(ServerPlayer p, WandererEntity w, int index) {
        if (!w.isAlive() || p.distanceTo(w) > 8.0F) return;
        View v = view(w);
        if (index < 0 || index >= v.options().size()) return;
        switch (v.options().get(index)) {
            case ACCEPT -> { close(p); w.beginDuel(p); }
            case SPAR -> { close(p); w.heal(w.getMaxHealth()); w.beginDuel(p); }
            case ASK -> open(p, w, RUMOURS[p.getRandom().nextInt(RUMOURS.length)]);
            case WHO -> open(p, w, w.introduction());
            default -> close(p);
        }
    }
}
