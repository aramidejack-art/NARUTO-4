package com.mayhem.client;

import com.mayhem.MayhemMod;
import com.mayhem.config.MayhemClientConfig;
import com.mayhem.network.ImpactPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/** Client impact feedback state: camera shake, FOV punch, flashes, speed lines and comic text pops. */
@Mod.EventBusSubscriber(modid = MayhemMod.MOD_ID, value = Dist.CLIENT)
public final class CameraEffects {
    public static final String[] TEXTS = {
            "", "WHAM!", "BAM!", "ドン!", "ORA!", "BOOM!", "ゴゴゴ", "CRACK!", "ZAP!",
            "ドゴォォ!", "FLASH"
    };

    public static final class Pop {
        public final String text;
        public final float x, y, rot, scale;
        public final int life;
        public final int color;
        public int age;

        Pop(String text, float x, float y, float rot, float scale, int life, int color) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.rot = rot;
            this.scale = scale;
            this.life = life;
            this.color = color;
        }
    }

    private static float trauma;
    private static float fovKick;
    private static float flashAlpha;
    private static int flashType;
    private static float lineTicks;
    public static final List<Pop> POPS = new ArrayList<>();

    private CameraEffects() {}

    public static float flashAlpha() { return flashAlpha; }
    public static int flashType() { return flashType; }
    public static float lineStrength() { return Mth.clamp(lineTicks / 8F, 0F, 1F); }

    public static void impact(ImpactPacket p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.gameRenderer == null) return;
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        float atten = 1F;
        if (p.radius > 0F) {
            atten = Mth.clamp(1F - (float) cam.distanceTo(new Vec3(p.x, p.y, p.z)) / p.radius, 0F, 1F);
        }
        if (atten <= 0F) return;

        float shake = MayhemClientConfig.SHAKE_SCALE.get().floatValue();
        float fovScale = MayhemClientConfig.FOV_KICK_SCALE.get().floatValue();
        trauma = Math.min(1F, trauma + p.trauma * atten * shake);
        fovKick = Mth.clamp(fovKick + p.fov * atten * fovScale, -18F, 18F);

        if (atten > 0.35F && p.flash != ImpactPacket.FLASH_NONE && MayhemClientConfig.FLASHES.get()) {
            flashType = p.flash;
            flashAlpha = Math.max(flashAlpha, p.flash == ImpactPacket.FLASH_DARK ? 0.85F : 0.8F * atten);
        }
        if (p.lines > 0 && MayhemClientConfig.SPEED_LINES.get()) {
            lineTicks = Math.max(lineTicks, p.lines);
        }
        if (p.text > 0 && p.text < TEXTS.length && atten > 0.4F && MayhemClientConfig.IMPACT_TEXT.get()
                && POPS.size() < 6) {
            float x = 0.25F + rand() * 0.5F;
            float y = 0.22F + rand() * 0.45F;
            float rot = (rand() - 0.5F) * 24F;
            int color = switch (p.text) {
                case 5 -> 0xFFE066;                              // BOOM
                case 8 -> 0x66E0FF;                              // ZAP
                case 6 -> 0xFF5050;                              // rumble
                default -> 0xFFFFFF;
            };
            POPS.add(new Pop(TEXTS[p.text], x, y, rot, 2.2F + rand() * 1.4F, 14, color));
        }
    }

    // cosmetic-only randomness (never affects gameplay)
    private static final java.util.Random RNG = new java.util.Random();
    private static float rand() { return RNG.nextFloat(); }

    public static void reset() {
        trauma = 0F;
        fovKick = 0F;
        flashAlpha = 0F;
        lineTicks = 0F;
        POPS.clear();
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        trauma = Math.max(0F, trauma - 0.045F);
        fovKick *= 0.8F;
        if (Math.abs(fovKick) < 0.05F) fovKick = 0F;
        flashAlpha = Math.max(0F, flashAlpha - 0.16F);
        lineTicks = Math.max(0F, lineTicks - 1F);
        POPS.removeIf(p -> ++p.age > p.life);
    }

    @SubscribeEvent
    public static void onAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        float t = (mc.level != null ? mc.level.getGameTime() : 0) + (float) event.getPartialTick();

        float s = trauma * trauma;
        float yaw = 0F, pitch = 0F, roll = 0F;
        if (s > 0.0001F) {
            yaw = (Mth.sin(t * 2.3F) + 0.5F * Mth.sin(t * 5.7F)) * 5F * s;
            pitch = (Mth.sin(t * 3.1F + 1F) + 0.5F * Mth.sin(t * 6.3F)) * 4F * s;
            roll = (Mth.sin(t * 2.9F + 2F) + 0.5F * Mth.sin(t * 7.1F)) * 5F * s;
        }

        // first-person dodge feel: a lean/dip instead of a full spin (kinder than a literal barrel roll)
        if (MayhemClientConfig.FIRST_PERSON_FX.get() && mc.player != null && !CinematicDirector.isActive()
                && mc.options.getCameraType().isFirstPerson()) {
            ClientAnimations.Active a = ClientAnimations.active(mc.player);
            if (a != null && a.move == com.mayhem.combat.MoveType.DODGE) {
                float prog = Mth.clamp((mc.level.getGameTime() + (float) event.getPartialTick() - a.start)
                        / a.move.ticks, 0F, 1F);
                float k = Mth.sin(prog * Mth.PI);
                switch (a.variant) {
                    case 0 -> pitch += 18F * k;
                    case 1 -> pitch -= 12F * k;
                    case 2 -> roll -= 16F * k;
                    default -> roll += 16F * k;
                }
            }
        }
        event.setYaw(event.getYaw() + yaw);
        event.setPitch(event.getPitch() + pitch);
        event.setRoll(event.getRoll() + roll);
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        double fov = event.getFOV();
        if (CinematicDirector.fovOverride() > 0) {
            fov = CinematicDirector.fovOverride();
        }
        if (fovKick != 0F) {
            fov += fovKick;
        }
        event.setFOV(Mth.clamp(fov, 20.0, 110.0));
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
    }
}
