package com.mayhem.shinobi.client;

import com.mayhem.client.CombatMode;
import com.mayhem.shinobi.ability.Abilities;
import com.mayhem.shinobi.ability.AbilityDef;
import com.mayhem.shinobi.data.ShinobiData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/** Chakra / stamina bars, the 10-slot loadout bar with cooldown sweeps, status line and event banners. */
public final class ShinobiHud {
    private ShinobiHud() {}

    private static final int[] BANNER_COL = {0xFFE0E0E0, 0xFFE6C27A, 0xFFFF7A4A, 0xFF6EE7FF};

    public static void render(ForgeGui gui, GuiGraphics g, float pt, int w, int h) {
        Minecraft mc = gui.getMinecraft();
        if (mc.player == null || mc.options.hideGui || ClientShinobi.tag.isEmpty() || !ClientShinobi.tag.getBoolean("onboarded")) return;
        Font f = mc.font;
        int bw = 120, x = 10, y = h - 48;
        bar(g, x, y, bw, ClientShinobi.chakra / Math.max(1F, ClientShinobi.maxChakra), 0xFF2E86DE, 0xFF0B2545);
        g.drawString(f, "Chakra " + (int) ClientShinobi.chakra + "/" + (int) ClientShinobi.maxChakra, x + 2, y - 10, 0xFF9EC9FF, true);
        bar(g, x, y + 16, bw, ClientShinobi.stamina / Math.max(1F, ClientShinobi.maxStamina), 0xFFE67E22, 0xFF3A1F08);
        g.drawString(f, "Stamina " + (int) ClientShinobi.stamina + "/" + (int) ClientShinobi.maxStamina, x + 2, y + 26, 0xFFFFC48A, true);

        String status = (ClientShinobi.ninjutsu() ? "NINJUTSU" : "TAIJUTSU") + (CombatMode.active() ? "  [combat]" : "")
                + (ClientShinobi.mode.isEmpty() ? "" : "  MODE")
                + (ClientShinobi.awakenTicks > 0 ? "  AWAKENED " + ClientShinobi.awakenTicks / 20 + "s" : "")
                + (ClientShinobi.charging() ? "  CHARGING" : "")
                + (ClientShinobi.waterWalk() ? "  WATER" : "") + (ClientShinobi.wallWalk() ? "  WALL" : "");
        g.drawString(f, status, x, y - 22, 0xFFFFFFFF, true);

        // slot bar (only while in combat mode or when something is assigned)
        int sw = 22, total = ShinobiData.SLOTS * sw, sx = (w - total) / 2, sy = h - 62;
        if (CombatMode.active()) {
            for (int i = 0; i < ShinobiData.SLOTS; i++) {
                int cx = sx + i * sw;
                boolean sel = i == ClientShinobi.selectedSlot;
                g.fill(cx, sy, cx + sw - 2, sy + 20, sel ? 0xCC3A3A55 : 0x99000000);
                g.renderOutline(cx, sy, sw - 2, 20, sel ? 0xFFFFD36E : 0xFF555555);
                String id = ShinobiData.slot(ClientShinobi.tag, i);
                AbilityDef d = Abilities.get(id);
                String lab = d == null ? "-" : d.name.length() > 3 ? d.name.substring(0, 3) : d.name;
                g.drawCenteredString(f, lab, cx + (sw - 2) / 2, sy + 6, d == null ? 0xFF777777 : 0xFFFFFFFF);
                g.drawString(f, i == 9 ? "0" : String.valueOf(i + 1), cx + 2, sy + 1, 0xFF9999AA, false);
                float cd = d == null ? 0F : ClientShinobi.cooldownFrac(id);
                if (cd > 0F) g.fill(cx, sy + 20 - (int) (20 * cd), cx + sw - 2, sy + 20, 0xAA000000);
            }
        }
        // active contract tracker (right side)
        net.minecraft.nbt.CompoundTag ms = ClientShinobi.tag.getCompound("missions");
        int ty = 60;
        boolean header = false;
        for (String key : ms.getAllKeys()) {
            net.minecraft.nbt.CompoundTag m = ms.getCompound(key);
            int st = m.getInt("state");
            if (st > 1) continue;
            com.mayhem.shinobi.story.MissionDef d = com.mayhem.shinobi.story.Missions.get(key);
            if (d == null) continue;
            if (!header) { g.drawString(f, "CONTRACTS", w - 150, ty - 12, 0xFFE6C27A, true); header = true; }
            String line = (d.story() ? "* " : "") + d.title();
            g.drawString(f, f.plainSubstrByWidth(line, 140), w - 150, ty, st == 1 ? 0xFF8CFF8C : 0xFFFFFFFF, true);
            g.drawString(f, st == 1 ? "COMPLETE - turn in" : m.getInt("progress") + "/" + d.target() + "  " + f.plainSubstrByWidth(d.objectiveText(), 90),
                    w - 150, ty + 10, st == 1 ? 0xFF8CFF8C : 0xFFAAB4C8, true);
            ty += 26;
        }
        // banners
        int by = 40;
        for (ClientShinobi.Banner b : ClientShinobi.BANNERS) {
            long age = ClientShinobi.clientTicks - b.born();
            int alpha = age < 80 ? 255 : (int) (255 * (100 - age) / 20F);
            if (alpha < 8) continue;
            int col = (alpha << 24) | (BANNER_COL[Math.floorMod(b.kind(), BANNER_COL.length)] & 0xFFFFFF);
            g.drawCenteredString(f, b.title(), w / 2, by, col);
            g.drawCenteredString(f, b.sub(), w / 2, by + 11, (alpha << 24) | 0xCCCCCC);
            by += 28;
        }
    }

    private static void bar(GuiGraphics g, int x, int y, int w, float frac, int fg, int bg) {
        g.fill(x - 1, y - 1, x + w + 1, y + 11, 0xFF000000);
        g.fill(x, y, x + w, y + 10, bg);
        g.fill(x, y, x + (int) (w * Math.max(0F, Math.min(1F, frac))), y + 10, fg);
    }
}
