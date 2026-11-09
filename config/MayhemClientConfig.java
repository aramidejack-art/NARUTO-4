package com.mayhem.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Client-side visual settings ({@code config/mayhem-client.toml}). */
public final class MayhemClientConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ANIMATIONS;
    public static final ForgeConfigSpec.BooleanValue FIRST_PERSON_FX;
    public static final ForgeConfigSpec.BooleanValue CINEMATICS;
    public static final ForgeConfigSpec.BooleanValue LETTERBOX;
    public static final ForgeConfigSpec.BooleanValue SPEED_LINES;
    public static final ForgeConfigSpec.BooleanValue IMPACT_TEXT;
    public static final ForgeConfigSpec.BooleanValue FLASHES;
    public static final ForgeConfigSpec.DoubleValue SHAKE_SCALE;
    public static final ForgeConfigSpec.DoubleValue FOV_KICK_SCALE;
    public static final ForgeConfigSpec.DoubleValue LOCK_ON_ASSIST;
    public static final ForgeConfigSpec.BooleanValue HOLD_TO_ATTACK;
    public static final ForgeConfigSpec.BooleanValue SHOW_HUD;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("visuals");
        ANIMATIONS = b.comment("Procedural combat body animations.").define("animations", true);
        FIRST_PERSON_FX = b.comment("Punch motion on your first-person arm.").define("firstPersonFx", true);
        CINEMATICS = b.comment("Take over the camera for ultimates and finishing blows.").define("cinematics", true);
        LETTERBOX = b.comment("Cinematic black bars during cinematics.").define("letterbox", true);
        SPEED_LINES = b.define("speedLines", true);
        IMPACT_TEXT = b.comment("Comic-style impact text pops.").define("impactText", true);
        FLASHES = b.comment("Full-screen impact flashes. Turn off if you are light sensitive.").define("flashes", true);
        SHAKE_SCALE = b.comment("Camera shake strength (0 disables).").defineInRange("shakeScale", 1.0D, 0.0D, 2.0D);
        FOV_KICK_SCALE = b.comment("FOV punch strength (0 disables).").defineInRange("fovKickScale", 1.0D, 0.0D, 2.0D);
        b.pop();
        b.push("controls");
        LOCK_ON_ASSIST = b.comment("How strongly the view is pulled toward your lock-on target (0 = none).")
                .defineInRange("lockOnAssist", 0.25D, 0.0D, 1.0D);
        HOLD_TO_ATTACK = b.comment("Holding the attack key repeats strikes (handy on touch controls).")
                .define("holdToAttack", true);
        b.pop();
        b.push("hud");
        SHOW_HUD = b.define("showHud", true);
        b.pop();
        SPEC = b.build();
    }

    private MayhemClientConfig() {}
}
