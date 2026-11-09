package com.mayhem.client;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Key mapping definitions. Only ever loaded on the client. All are rebindable under "MAYHEM Combat". */
public final class MayhemKeys {
    private static final String CAT = "key.categories.mayhem";

    public static final KeyMapping ATTACK = new KeyMapping("key.mayhem.unarmed_attack", GLFW.GLFW_KEY_R, CAT);
    public static final KeyMapping DODGE = new KeyMapping("key.mayhem.dodge", GLFW.GLFW_KEY_V, CAT);
    public static final KeyMapping GUARD = new KeyMapping("key.mayhem.guard", GLFW.GLFW_KEY_B, CAT);
    public static final KeyMapping SLAM = new KeyMapping("key.mayhem.slam", GLFW.GLFW_KEY_G, CAT);
    public static final KeyMapping ULTIMATE = new KeyMapping("key.mayhem.ultimate", GLFW.GLFW_KEY_X, CAT);
    public static final KeyMapping LOCK_ON = new KeyMapping("key.mayhem.lock_on", GLFW.GLFW_KEY_Z, CAT);
    public static final KeyMapping STATUS = new KeyMapping("key.mayhem.status", GLFW.GLFW_KEY_K, CAT);

    private MayhemKeys() {}
}
