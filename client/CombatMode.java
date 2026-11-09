package com.mayhem.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Client-side combat mode. Toggled with the Combat Mode key (R). While it is on, the vanilla attack button (M1)
 * is swallowed before Minecraft sees it and becomes the anime strike instead, whatever the player is holding.
 */
public final class CombatMode {
    private static boolean active;
    private static boolean pendingTap;
    private static boolean held;

    private CombatMode() {}

    public static boolean active() { return active; }
    public static void toggle() { setActive(!active); }

    public static void setActive(boolean on) {
        active = on;
        pendingTap = false;
        held = false;
    }

    public static void reset() { setActive(false); }

    /** True once per click; clears the flag. */
    public static boolean takeTap() {
        boolean t = pendingTap;
        pendingTap = false;
        return t;
    }

    public static boolean isHeld() { return held; }

    /**
     * Called at the START of each client tick, before vanilla reads the keys. Steals M1 clicks and the held state
     * so vanilla neither swings, attacks, nor mines.
     */
    public static void stealAttackInput(Minecraft mc) {
        if (!active) return;
        KeyMapping atk = mc.options.keyAttack;
        while (atk.consumeClick()) pendingTap = true;
        held = mc.screen == null && mc.mouseHandler.isMouseGrabbed() && rawDown(mc, atk);
        atk.setDown(false);
    }

    private static boolean rawDown(Minecraft mc, KeyMapping km) {
        InputConstants.Key k = km.getKey();
        long win = mc.getWindow().getWindow();
        if (k.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(win, k.getValue()) == GLFW.GLFW_PRESS;
        }
        if (k.getType() == InputConstants.Type.KEYSYM) {
            return InputConstants.isKeyDown(win, k.getValue());
        }
        return false;
    }
}
