package com.mayhem.shinobi.story;

import com.mayhem.progression.PlayerProgression;
import com.mayhem.shinobi.ShinobiConfig;
import com.mayhem.shinobi.data.ShinobiData;
import com.mayhem.shinobi.net.ShinobiNet;
import com.mayhem.shinobi.npc.EscortClientEntity;
import com.mayhem.shinobi.registry.ShinobiRegistry;
import com.mayhem.shinobi.story.MissionDef.Objective;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Server-authoritative mission state machine. States: 0 active, 1 complete (unclaimed), 2 claimed.
 * Rewards are paid only on the 1 -> 2 transition, so they cannot be duplicated.
 */
public final class MissionTracker {
    public static final int MAX_ACTIVE = 3;

    private MissionTracker() {}

    // ------------------------------------------------------------------ pure state accessors (also used by the client GUI)
    public static boolean has(CompoundTag t, String id) { return ShinobiData.missions(t).contains(id); }
    public static int state(CompoundTag t, String id) { return has(t, id) ? ShinobiData.missions(t).getCompound(id).getInt("state") : -1; }
    public static int progress(CompoundTag t, String id) { return has(t, id) ? ShinobiData.missions(t).getCompound(id).getInt("progress") : 0; }
    public static long doneAt(CompoundTag t, String id) { return has(t, id) ? ShinobiData.missions(t).getCompound(id).getLong("doneAt") : 0; }
    public static int storyDone(CompoundTag t) { return t.getInt("storyDone"); }

    public static int activeCount(CompoundTag t) {
        int n = 0;
        for (MissionDef d : Missions.ALL.values()) if (state(t, d.id()) == 0) n++;
        return n;
    }

    public static MissionDef nextStory(CompoundTag t) {
        int i = storyDone(t);
        return i >= 0 && i < Missions.STORY.size() ? Missions.STORY.get(i) : null;
    }

    private static void put(CompoundTag t, String id, CompoundTag m) {
        CompoundTag all = t.getCompound("missions");
        all.put(id, m);
        t.put("missions", all);
    }

    private static void msg(ServerPlayer p, String s) { p.displayClientMessage(Component.literal(s), true); }

    // ------------------------------------------------------------------ transitions
    public static boolean accept(ServerPlayer p, String id) {
        MissionDef d = Missions.get(id);
        CompoundTag t = ShinobiData.of(p);
        if (d == null) return false;
        int level = PlayerProgression.getLevel(p);
        if (level < d.minLevel()) { msg(p, "Requires level " + d.minLevel()); return false; }
        if (d.story()) {
            MissionDef next = nextStory(t);
            if (next == null || !next.id().equals(id)) { msg(p, "That chapter is not available yet"); return false; }
        }
        int st = state(t, id);
        long now = p.level().getGameTime();
        if (st == 0) { msg(p, "Already in progress"); return false; }
        if (st == 1) { msg(p, "Complete - claim your reward"); return false; }
        if (st == 2) {
            if (d.repeatCooldownTicks() <= 0) { msg(p, "Already completed"); return false; }
            if (now - doneAt(t, id) < d.repeatCooldownTicks()) {
                msg(p, "Available again in " + ((d.repeatCooldownTicks() - (now - doneAt(t, id))) / 1200 + 1) + " min");
                return false;
            }
        }
        if (!d.story() && activeCount(t) >= MAX_ACTIVE) { msg(p, "You can only hold " + MAX_ACTIVE + " contracts at once"); return false; }
        CompoundTag m = new CompoundTag();
        m.putInt("state", 0);
        m.putInt("progress", 0);
        if (d.type() == Objective.ESCORT || d.type() == Objective.PROTECT) {
            spawnEscortClient(p);
        }
        m.putInt("base", ShinobiData.set(t, "owned").size());
        put(t, id, m);
        ShinobiNet.banner(p, d.story() ? 1 : 0, d.title(), d.objectiveText());
        poll(p);
        ShinobiNet.syncTag(p);
        return true;
    }

    public static void abandon(ServerPlayer p, String id) {
        CompoundTag t = ShinobiData.of(p);
        if (state(t, id) != 0) return;
        CompoundTag all = t.getCompound("missions");
        all.remove(id);
        t.put("missions", all);
        msg(p, "Contract abandoned");
        ShinobiNet.syncTag(p);
    }

    public static boolean claim(ServerPlayer p, String id) {
        MissionDef d = Missions.get(id);
        CompoundTag t = ShinobiData.of(p);
        if (d == null || state(t, id) != 1) { msg(p, "Nothing to claim"); return false; }
        CompoundTag m = ShinobiData.missions(t).getCompound(id);
        m.putInt("state", 2);
        m.putLong("doneAt", p.level().getGameTime());
        put(t, id, m);                                       // state flips first: a second claim sees state 2
        double sc = ShinobiConfig.MISSION_REWARD_SCALE.get();
        if (d.xp() > 0) PlayerProgression.awardCombat(p, (int) Math.round(d.xp() * sc));
        t.putInt("ap", t.getInt("ap") + (int) Math.round(d.ap() * sc));
        t.putInt("sp", t.getInt("sp") + (int) Math.round(d.sp() * sc));
        if (!d.rewardItem().isEmpty() && d.rewardCount() > 0) {
            Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(d.rewardItem()));
            if (item != null) {
                ItemStack st = new ItemStack(item, d.rewardCount());
                if (!p.getInventory().add(st)) p.drop(st, false);
            }
        }
        if (d.story()) t.putInt("storyDone", storyDone(t) + 1);
        ShinobiNet.banner(p, 3, "Reward claimed: " + d.title(), "+" + d.xp() + " XP  +" + d.ap() + " AP  +" + d.sp() + " SP");
        ShinobiNet.syncTag(p);
        return true;
    }

    private static void advance(ServerPlayer p, MissionDef d, int by) {
        CompoundTag t = ShinobiData.of(p);
        if (state(t, d.id()) != 0) return;
        CompoundTag m = ShinobiData.missions(t).getCompound(d.id());
        int pr = Math.min(d.target(), m.getInt("progress") + by);
        m.putInt("progress", pr);
        if (pr >= d.target()) {
            m.putInt("state", 1);
            ShinobiNet.banner(p, d.story() ? 1 : 0, "Mission complete: " + d.title(), d.story() ? "Return to your sensei" : "Claim your reward in the menu");
        } else {
            msg(p, d.title() + ": " + pr + "/" + d.target());
        }
        put(t, d.id(), m);
        ShinobiNet.syncTag(p);
    }

    // ------------------------------------------------------------------ event hooks
    public static void onKill(ServerPlayer p, LivingEntity victim) {
        if (victim instanceof net.minecraft.world.entity.monster.Monster) onProtectKill(p);
        CompoundTag t = ShinobiData.of(p);
        String key = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString();
        for (MissionDef d : Missions.ALL.values()) {
            if (state(t, d.id()) != 0) continue;
            if (d.type() == Objective.KILL && victim instanceof Enemy && !victim.getTags().contains("mayhem_beast")) advance(p, d, 1);
            else if (d.type() == Objective.KILL_TYPE && d.arg().equals(key)) advance(p, d, 1);
        }
    }

    public static void onCast(ServerPlayer p, String abilityId) {
        CompoundTag t = ShinobiData.of(p);
        for (MissionDef d : Missions.ALL.values()) {
            if (state(t, d.id()) == 0 && d.type() == Objective.CAST && ("any".equals(d.arg()) || d.arg().equals(abilityId))) advance(p, d, 1);
        }
    }

    public static void onEvent(ServerPlayer p, String kind) {
        CompoundTag t = ShinobiData.of(p);
        for (MissionDef d : Missions.ALL.values()) {
            if (state(t, d.id()) == 0 && d.type() == Objective.EVENT && d.arg().equals(kind)) advance(p, d, 1);
        }
    }

    /** Called when the player opens a dialogue with an NPC of the given role tag. */
    public static void onTalk(ServerPlayer p, String roleTag) {
        CompoundTag t = ShinobiData.of(p);
        for (MissionDef d : Missions.ALL.values()) {
            if (state(t, d.id()) == 0 && d.type() == Objective.TALK && d.arg().equals(roleTag)) advance(p, d, 1);
        }
    }

    /** Called once per second while the player is water-walking ("water") or wall-climbing ("wall"). */
    public static void onTraverse(ServerPlayer p, String kind) {
        CompoundTag t = ShinobiData.of(p);
        Objective o = kind.equals("water") ? Objective.WATER : Objective.WALL;
        for (MissionDef d : Missions.ALL.values()) {
            if (state(t, d.id()) == 0 && d.type() == o) advance(p, d, 1);
        }
    }


    /** Called once per second while escorting: player stays near an escorted NPC (arg unused, any nearby villager/NPC counts). */
    public static void failEscort(ServerPlayer p) {
        CompoundTag t = ShinobiData.of(p);
        for (MissionDef d : Missions.ALL.values()) {
            if (state(t, d.id()) == 0 && (d.type() == Objective.ESCORT || d.type() == Objective.PROTECT)) {
                ShinobiData.missions(t).remove(d.id());
                msg(p, "Contract failed: " + d.title());
            }
        }
    }

    public static void onEscortTick(ServerPlayer p, boolean clientNearby) {
        if (!clientNearby) return;
        CompoundTag t = ShinobiData.of(p);
        for (MissionDef d : Missions.ALL.values()) {
            if (state(t, d.id()) == 0 && d.type() == Objective.ESCORT) advance(p, d, 1);
        }
    }

    /** Progress protect missions when a hostile near the protected client is killed. */
    public static void onProtectKill(ServerPlayer p) {
        CompoundTag t = ShinobiData.of(p);
        for (MissionDef d : Missions.ALL.values()) {
            if (state(t, d.id()) == 0 && d.type() == Objective.PROTECT) advance(p, d, 1);
        }
    }

    public static void onBeast(ServerPlayer p, String beastId, boolean sealed) {
        CompoundTag t = ShinobiData.of(p);
        for (MissionDef d : Missions.ALL.values()) {
            if (state(t, d.id()) != 0) continue;
            if (!sealed && d.type() == Objective.BEAST_DEFEAT && ("any".equals(d.arg()) || d.arg().equals(beastId))) advance(p, d, 1);
            if (sealed && d.type() == Objective.BEAST_SEAL) advance(p, d, 1);
        }
    }

    /** Periodic check for objectives that read state (inventory, level, learned jutsu). Call about once a second. */
    public static void poll(ServerPlayer p) {
        CompoundTag t = ShinobiData.of(p);
        int level = PlayerProgression.getLevel(p);
        for (MissionDef d : Missions.ALL.values()) {
            if (state(t, d.id()) != 0) continue;
            int want = -1;
            switch (d.type()) {
                case LEVEL -> want = level >= d.target() ? d.target() : 0;
                case COLLECT -> {
                    Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(d.arg()));
                    int n = 0;
                    for (ItemStack s : p.getInventory().items) if (s.is(item)) n += s.getCount();
                    want = Math.min(d.target(), n);
                }
                case VISIT -> {
                    var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE, new ResourceLocation("mayhem", d.arg()));
                    want = p.serverLevel().structureManager().getStructureWithPieceAt(p.blockPosition(), key).isValid() ? d.target() : 0;
                }
                case LEARN -> want = Math.max(0, ShinobiData.set(t, "owned").size() - ShinobiData.missions(t).getCompound(d.id()).getInt("base"));
                default -> { }
            }
            if (want < 0) continue;
            int cur = progress(t, d.id());
            if (want != cur) advance(p, d, want - cur);
        }
    }

    private static void spawnEscortClient(ServerPlayer p) {
        var level = p.serverLevel();
        EscortClientEntity e = ShinobiRegistry.ESCORT_CLIENT.get().create(level);
        if (e == null) return;
        e.moveTo(p.getX() + 1.5, p.getY(), p.getZ() + 1.5, p.getYRot(), 0);
        e.setOwner(p);
        level.addFreshEntity(e);
        msg(p, "Escort client is following you — stay close!");
    }
}
