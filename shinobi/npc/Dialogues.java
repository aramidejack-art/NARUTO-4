package com.mayhem.shinobi.npc;

import com.mayhem.progression.PlayerProgression;
import com.mayhem.shinobi.data.Resources;
import com.mayhem.shinobi.data.ShinobiData;
import com.mayhem.shinobi.net.ShinobiNet;
import com.mayhem.shinobi.registry.ShinobiRegistry;
import com.mayhem.shinobi.story.MissionDef;
import com.mayhem.shinobi.story.MissionTracker;
import com.mayhem.shinobi.story.Missions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Stateless NPC dialogue. The option list is derived from (role, player state) each time, so the server never trusts the
 * client's idea of what was offered: the client sends only an index, which is re-derived and validated here.
 */
public final class Dialogues {
    public static final int TRAIN_COST = 1, SCROLL_COST = 8;
    private static final String BYE = "Farewell";

    private Dialogues() {}

    private record View(String text, List<String> options) {}

    private static boolean reach(ServerPlayer p, NpcEntity n) { return n != null && n.isAlive() && p.distanceTo(n) < 8.0F; }

    private static String claimable(CompoundTag t) {
        for (MissionDef d : Missions.ALL.values()) if (MissionTracker.state(t, d.id()) == 1) return d.id();
        return null;
    }

    private static boolean offerable(ServerPlayer p, CompoundTag t, MissionDef d) {
        if (d.story() || PlayerProgression.getLevel(p) < d.minLevel()) return false;
        int st = MissionTracker.state(t, d.id());
        if (st == 0 || st == 1) return false;
        if (st == 2) {
            if (d.repeatCooldownTicks() <= 0) return false;
            if (p.level().getGameTime() - MissionTracker.doneAt(t, d.id()) < d.repeatCooldownTicks()) return false;
        }
        return MissionTracker.activeCount(t) < MissionTracker.MAX_ACTIVE;
    }

    private static View view(ServerPlayer p, NpcEntity n) {
        CompoundTag t = ShinobiData.of(p);
        List<String> o = new ArrayList<>();
        String text;
        switch (n.role()) {
            case SENSEI -> {
                MissionDef next = MissionTracker.nextStory(t);
                if (claimable(t) != null) { text = "Well done. Collect what you have earned."; o.add("Claim rewards"); }
                else if (next == null) text = "You have walked every chapter I can teach. The rest of the road is yours.";
                else if (MissionTracker.state(t, next.id()) == 0) text = "Chapter in progress: " + next.title() + ". " + next.objectiveText();
                else { text = next.intro(); o.add("Accept: " + next.title()); }
            }
            case MISSION_GIVER -> {
                text = "Contracts for you, shinobi. Take up to " + MissionTracker.MAX_ACTIVE + " at a time (M > Missions shows progress).";
                if (claimable(t) != null) o.add("Claim rewards");
                int shown = 0;
                for (MissionDef d : Missions.ALL.values()) {
                    if (shown >= 5) break;
                    if (offerable(p, t, d)) { o.add("Take: " + d.rank() + " " + d.title()); shown++; }
                }
                if (shown == 0 && claimable(t) == null) text = "Nothing suits you right now. Come back later, or level up.";
            }
            case TRAINER -> {
                text = "Rest and breathe. A shinobi who fights on empty fights badly. (" + TRAIN_COST + " emerald)";
                o.add("Restore chakra and stamina");
            }
            default -> {
                text = "Scrolls and supplies, fairly priced.";
                o.add("Buy Sealing Scroll (" + SCROLL_COST + " emeralds)");
                o.add("Buy 8 Rations (2 emeralds)");
            }
        }
        o.add(BYE);
        return new View(text, o);
    }

    public static void open(ServerPlayer p, NpcEntity n) {
        MissionTracker.onTalk(p, n.role().tag);
        View v = view(p, n);
        ShinobiNet.dialogue(p, n.getId(), n.displayName(), v.text(), v.options());
    }

    public static void choose(ServerPlayer p, int entityId, int index) {
        Entity e = p.level().getEntity(entityId);
        if (e instanceof WandererEntity w) { WandererDialogue.choose(p, w, index); return; }
        if (!(e instanceof NpcEntity n) || !reach(p, n)) return;
        View v = view(p, n);
        if (index < 0 || index >= v.options().size()) return;
        String pick = v.options().get(index);
        CompoundTag t = ShinobiData.of(p);
        if (pick.equals(BYE)) { ShinobiNet.dialogue(p, -1, "", "", List.of()); return; }
        if (pick.equals("Claim rewards")) {
            String id = claimable(t);
            if (id != null) MissionTracker.claim(p, id);
        } else if (pick.startsWith("Take: ")) {
            for (MissionDef d : Missions.ALL.values()) {
                if (pick.equals("Take: " + d.rank() + " " + d.title()) && offerable(p, t, d)) { MissionTracker.accept(p, d.id()); break; }
            }
        } else if (pick.startsWith("Accept: ")) {
            MissionDef next = MissionTracker.nextStory(t);
            if (next != null) MissionTracker.accept(p, next.id());
        } else if (pick.startsWith("Restore")) {
            if (pay(p, TRAIN_COST)) {
                t.putFloat("chakra", Resources.maxChakra(p, t));
                t.putFloat("stamina", Resources.maxStamina(p, t));
                p.displayClientMessage(Component.literal("You feel renewed"), true);
            }
        } else if (pick.startsWith("Buy Sealing")) {
            if (pay(p, SCROLL_COST)) give(p, new ItemStack(ShinobiRegistry.SEALING_SCROLL.get()));
        } else if (pick.startsWith("Buy 8")) {
            if (pay(p, 2)) give(p, new ItemStack(Items.BREAD, 8));
        }
        ShinobiNet.syncTag(p);
        ShinobiNet.sendResources(p, true);
        open(p, n); // refresh the screen with the new state
    }

    private static boolean pay(ServerPlayer p, int emeralds) {
        int have = p.getInventory().clearOrCountMatchingItems(s -> s.is(Items.EMERALD), 0, p.inventoryMenu.getCraftSlots());
        if (have < emeralds) { p.displayClientMessage(Component.literal("You need " + emeralds + " emeralds"), true); return false; }
        p.getInventory().clearOrCountMatchingItems(s -> s.is(Items.EMERALD), emeralds, p.inventoryMenu.getCraftSlots());
        return true;
    }

    private static void give(ServerPlayer p, ItemStack s) {
        if (!p.getInventory().add(s)) p.drop(s, false);
    }

    /** Used by the level check in the UI only. */
    public static int level(ServerPlayer p) { return PlayerProgression.getLevel(p); }
}
