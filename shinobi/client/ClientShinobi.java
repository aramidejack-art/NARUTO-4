package com.mayhem.shinobi.client;

import com.mayhem.network.MayhemNetwork;
import com.mayhem.shinobi.appearance.Appearance;
import com.mayhem.shinobi.net.ShinobiNet;
import com.mayhem.shinobi.net.ShinobiNet.Op;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Client-side mirror of the server's shinobi state. Read-only: every change is a request to the server. */
public final class ClientShinobi {
    public static CompoundTag tag = new CompoundTag();
    public static float chakra, maxChakra = 100, stamina, maxStamina = 100;
    public static int flags, awakenTicks;
    public static String mode = "";
    public static int selectedSlot = 0;

    /** ability id -> client tick when ready again; and the total duration for the HUD sweep. */
    public static final Map<String, long[]> COOLDOWNS = new HashMap<>();
    public static final List<Banner> BANNERS = new ArrayList<>();
    public static long clientTicks;

    // dialogue
    public static int dlgEntity = -1;
    public static String dlgName = "", dlgText = "";
    public static List<String> dlgOptions = List.of();

    public record Banner(int kind, String title, String sub, long born) {}

    private ClientShinobi() {}

    public static boolean flag(int bit) { return (flags & bit) != 0; }
    public static boolean ninjutsu() { return flag(16); }
    public static boolean charging() { return flag(1); }
    public static boolean waterWalk() { return flag(2); }
    public static boolean wallWalk() { return flag(4); }

    public static void send(Op op, String s, int a, int b) {
        MayhemNetwork.CHANNEL.sendToServer(new ShinobiNet.Action(op, s, a, b));
    }

    public static void send(Op op) { send(op, "", 0, 0); }

    public static float cooldownFrac(String id) {
        long[] c = COOLDOWNS.get(id);
        if (c == null || c[1] <= 0) return 0F;
        long left = c[0] - clientTicks;
        return left <= 0 ? 0F : Math.min(1F, left / (float) c[1]);
    }

    // ------------------------------------------------------------------ packet handlers (client thread)
    public static void sync(CompoundTag t) {
        boolean first = tag.isEmpty();
        tag = t;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && t.contains("appearance")) Appearance.CACHE.put(mc.player.getUUID(), Appearance.sanitize(t.getCompound("appearance")));
        if (first && mc.screen == null && !t.getBoolean("onboarded") && mc.player != null) mc.setScreen(new ShinobiScreen());
    }

    public static void resources(float c, float mc, float s, float ms, int fl, String m, int awaken) {
        chakra = c; maxChakra = mc; stamina = s; maxStamina = ms; flags = fl; mode = m; awakenTicks = awaken;
    }

    public static void ack(String id, int cd) { COOLDOWNS.put(id, new long[]{clientTicks + cd, cd}); }

    public static void banner(int kind, String title, String sub) {
        BANNERS.add(new Banner(kind, title, sub, clientTicks));
        if (BANNERS.size() > 4) BANNERS.remove(0);
    }

    public static void dialogue(int entity, String name, String text, List<String> options) {
        dlgEntity = entity; dlgName = name; dlgText = text; dlgOptions = options;
        Minecraft mc = Minecraft.getInstance();
        if (entity < 0) { if (mc.screen instanceof DialogueScreen) mc.setScreen(null); return; }
        if (!(mc.screen instanceof DialogueScreen)) mc.setScreen(new DialogueScreen());
        else ((DialogueScreen) mc.screen).refresh();
    }

    public static void appearance(UUID who, CompoundTag t) { Appearance.CACHE.put(who, Appearance.sanitize(t)); }

    public static void reset() {
        tag = new CompoundTag(); COOLDOWNS.clear(); BANNERS.clear(); flags = 0; mode = ""; awakenTicks = 0; dlgEntity = -1;
    }
}
