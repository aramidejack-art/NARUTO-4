package com.mayhem.shinobi.client;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Shinobi key mappings. All rebindable under the "MAYHEM Shinobi" category. */
public final class ShinobiKeys {
    private static final String CAT = "key.categories.mayhem_shinobi";
    public static final KeyMapping MENU = new KeyMapping("key.mayhem.shinobi_menu", GLFW.GLFW_KEY_M, CAT);
    public static final KeyMapping STYLE = new KeyMapping("key.mayhem.style", GLFW.GLFW_KEY_F, CAT);
    public static final KeyMapping CHARGE = new KeyMapping("key.mayhem.charge", GLFW.GLFW_KEY_C, CAT);
    public static final KeyMapping MODE = new KeyMapping("key.mayhem.mode", GLFW.GLFW_KEY_Y, CAT);
    public static final KeyMapping CLOAK = new KeyMapping("key.mayhem.cloak", GLFW.GLFW_KEY_H, CAT);
    public static final KeyMapping AWAKEN = new KeyMapping("key.mayhem.awaken", GLFW.GLFW_KEY_J, CAT);
    public static final KeyMapping GUARDIAN = new KeyMapping("key.mayhem.guardian", GLFW.GLFW_KEY_N, CAT);
    public static final KeyMapping STANCE = new KeyMapping("key.mayhem.avatar_stance", GLFW.GLFW_KEY_L, CAT);
    public static final KeyMapping TACKLE = new KeyMapping("key.mayhem.avatar_tackle", GLFW.GLFW_KEY_O, CAT);
    public static final KeyMapping BEAST_MODE = new KeyMapping("key.mayhem.beast_mode", GLFW.GLFW_KEY_I, CAT);
    public static final KeyMapping SKILL1 = new KeyMapping("key.mayhem.mode_skill1", GLFW.GLFW_KEY_P, CAT);
    public static final KeyMapping SKILL2 = new KeyMapping("key.mayhem.mode_skill2", GLFW.GLFW_KEY_U, CAT);
    public static final KeyMapping CAST_SELECTED = new KeyMapping("key.mayhem.cast_selected", GLFW.GLFW_KEY_LEFT_ALT, CAT);
    public static final KeyMapping WEAPON = new KeyMapping("key.mayhem.slot_weapon", GLFW.GLFW_KEY_UNKNOWN, CAT);
    public static final KeyMapping SUPPORT = new KeyMapping("key.mayhem.slot_support", GLFW.GLFW_KEY_UNKNOWN, CAT);
    public static final KeyMapping SHINOBI_ULT = new KeyMapping("key.mayhem.slot_ultimate", GLFW.GLFW_KEY_UNKNOWN, CAT);

    private ShinobiKeys() {}
}
