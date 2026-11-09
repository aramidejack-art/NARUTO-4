package com.mayhem.client;

import com.mayhem.config.MayhemClientConfig;
import com.mayhem.network.ImpactPacket;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.joml.Vector3f;

/** Everything drawn on top of the screen: HUD, lock-on UI, speed lines, flashes, comic text, letterbox. */
public final class HudRenderer {
    private HudRenderer() {}

    private static int argb(float alpha, int rgb) {
        int a = Mth.clamp((int) (alpha * 255F), 4, 255);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    public static void render(ForgeGui gui, GuiGraphics g, float partialTick, int w, int h) {
        Minecraft mc = gui.getMinecraft();
        if (mc.player == null || mc.level == null) return;
        boolean cinematic = CinematicDirector.isActive();
        float time = mc.level.getGameTime() + partialTick;

        drawSpeedLines(g, w, h, time, cinematic);

        if (!cinematic && !mc.options.hideGui && !mc.player.isSpectator()
                && MayhemClientConfig.SHOW_HUD.get()) {
            drawProgress(g, mc.font);
            if (CombatMode.active()) {
                float blink = 0.75F + 0.25F * Mth.sin(time * 0.3F);
                int a = (int) (255 * blink) << 24;
                g.drawString(mc.font, Component.translatable("hud.mayhem.mode"),
                        w / 2 - mc.font.width(Component.translatable("hud.mayhem.mode")) / 2,
                        h - 62, a | 0xFF4040, true);
            }
            drawLockOn(g, mc, w, h, time, partialTick);
        }

        drawTextPops(g, mc.font, w, h, partialTick);
        drawLetterbox(g, w, h);
        drawFlash(g, w, h);
    }

    // ------------------------------------------------------------------ progression + momentum

    private static void drawProgress(GuiGraphics g, Font font) {
        if (!ClientProgressionData.hasData()) return;
        int x = 6, y = 6, barW = 90;

        g.drawString(font, Component.translatable("hud.mayhem.level", ClientProgressionData.level()),
                x, y, 0xFFFFFF, true);

        int xpY = y + font.lineHeight + 2;
        int next = ClientProgressionData.xpToNext();
        g.fill(x - 1, xpY - 1, x + barW + 1, xpY + 5, 0xFF000000);
        g.fill(x, xpY, x + barW, xpY + 4, 0xFF3A3A3A);
        int xpFill = next > 0 ? Mth.clamp((int) ((long) barW * ClientProgressionData.xp() / next), 0, barW) : barW;
        g.fill(x, xpY, x + xpFill, xpY + 4, 0xFFE0A030);

        g.drawString(font, Component.translatable("hud.mayhem.stats",
                ClientProgressionData.strength(), ClientProgressionData.mastery()), x, xpY + 8, 0xC0C0C0, true);

        // momentum meter
        int mY = xpY + 8 + font.lineHeight + 4;
        int mom = ClientProgressionData.momentum();
        boolean ready = mom >= 100;
        Minecraft mc = Minecraft.getInstance();
        float pulse = ready ? 0.65F + 0.35F * Mth.sin((mc.level.getGameTime() + mc.getFrameTime()) * 0.5F) : 1F;

        g.drawString(font, Component.translatable("hud.mayhem.momentum"), x, mY, ready ? 0xFFE066 : 0x9FB8FF, true);
        int barY = mY + font.lineHeight + 1;
        g.fill(x - 1, barY - 1, x + barW + 1, barY + 6, 0xFF000000);
        g.fill(x, barY, x + barW, barY + 5, 0xFF26263A);
        int fill = Mth.clamp(barW * mom / 100, 0, barW);
        int r = Mth.clamp(80 + mom * 175 / 100, 0, 255);
        int gr = Mth.clamp(160 - mom * 90 / 100, 0, 255);
        int b = Mth.clamp(255 - mom * 215 / 100, 0, 255);
        int color = ready ? argb(pulse, 0xFFD040) : (0xFF000000 | (r << 16) | (gr << 8) | b);
        g.fill(x, barY, x + fill, barY + 5, color);

        if (ready) {
            Component hint = Component.translatable("hud.mayhem.ult_ready",
                    MayhemKeys.ULTIMATE.getTranslatedKeyMessage());
            g.drawString(font, hint, x, barY + 9, argb(pulse, 0xFFE066), true);
        }
    }

    // ------------------------------------------------------------------ lock-on card + reticle

    private static void drawLockOn(GuiGraphics g, Minecraft mc, int w, int h, float time, float partial) {
        LivingEntity t = LockOn.target();
        if (t == null) return;
        Font font = mc.font;

        // target card
        int cardW = 130;
        int cx = w / 2 - cardW / 2;
        int cy = 22;
        Component name = t.getDisplayName();
        g.drawString(font, name, w / 2 - font.width(name) / 2, cy, 0xFFFFFF, true);
        g.fill(cx - 1, cy + 11, cx + cardW + 1, cy + 17, 0xFF000000);
        g.fill(cx, cy + 12, cx + cardW, cy + 16, 0xFF3A1010);
        float ratio = Mth.clamp(t.getHealth() / Math.max(1F, t.getMaxHealth()), 0F, 1F);
        g.fill(cx, cy + 12, cx + (int) (cardW * ratio), cy + 16, 0xFFE03030);

        // reticle projected onto the screen
        Camera cam = mc.gameRenderer.getMainCamera();
        Vec3 d = t.getPosition(partial).add(0, t.getBbHeight() * 0.5, 0).subtract(cam.getPosition());
        Vector3f look = cam.getLookVector(), up = cam.getUpVector(), left = cam.getLeftVector();
        double z = d.x * look.x() + d.y * look.y() + d.z * look.z();
        if (z <= 0.2) return;
        double dl = d.x * left.x() + d.y * left.y() + d.z * left.z();
        double du = d.x * up.x() + d.y * up.y() + d.z * up.z();
        double tanHalf = Math.tan(Math.toRadians(mc.options.fov().get()) * 0.5);
        double ndcX = (-dl / z) / (tanHalf * ((double) w / h));
        double ndcY = (du / z) / tanHalf;
        if (Math.abs(ndcX) > 1.1 || Math.abs(ndcY) > 1.1) return;
        float sx = (float) (w * 0.5 * (1.0 + ndcX));
        float sy = (float) (h * 0.5 * (1.0 - ndcY));

        g.pose().pushPose();
        g.pose().translate(sx, sy, 0F);
        g.pose().mulPose(Axis.ZP.rotationDegrees(time * 4F + 45F));
        int c = 0xFFFF3030;
        g.fill(-9, -9, 9, -8, c);
        g.fill(-9, 8, 9, 9, c);
        g.fill(-9, -9, -8, 9, c);
        g.fill(8, -9, 9, 9, c);
        g.pose().popPose();
    }

    // ------------------------------------------------------------------ screen effects

    private static void drawSpeedLines(GuiGraphics g, int w, int h, float time, boolean cinematic) {
        if (!MayhemClientConfig.SPEED_LINES.get()) return;
        float s = CameraEffects.lineStrength();
        if (s <= 0.01F) return;
        int n = 54;
        float inner = Math.min(w, h) * 0.28F;
        float outer = (float) Math.hypot(w, h) * 0.55F;
        g.pose().pushPose();
        g.pose().translate(w * 0.5F, h * 0.5F, 0F);
        for (int i = 0; i < n; i++) {
            float jitter = Mth.sin(i * 12.9898F + time * 0.9F) * 0.5F + 0.5F;
            float ang = i * (360F / n) + jitter * 3F;
            float r0 = inner + jitter * inner * 0.7F;
            g.pose().pushPose();
            g.pose().mulPose(Axis.ZP.rotationDegrees(ang));
            g.fill((int) r0, -1, (int) outer, i % 3 == 0 ? 2 : 1, argb(0.55F * s * (0.5F + jitter * 0.5F), 0xFFFFFF));
            g.pose().popPose();
        }
        g.pose().popPose();
    }

    private static void drawTextPops(GuiGraphics g, Font font, int w, int h, float partial) {
        for (CameraEffects.Pop p : CameraEffects.POPS) {
            float prog = Mth.clamp((p.age + partial) / p.life, 0F, 1F);
            float punch = 1.5F - 0.5F * Mth.clamp(prog * 5F, 0F, 1F);
            float alpha = prog < 0.6F ? 1F : 1F - (prog - 0.6F) / 0.4F;
            g.pose().pushPose();
            g.pose().translate(w * p.x, h * p.y, 0F);
            g.pose().mulPose(Axis.ZP.rotationDegrees(p.rot));
            g.pose().scale(p.scale * punch, p.scale * punch, 1F);
            int tw = font.width(p.text);
            // dark outline then colour, for a comic-book feel
            int dark = argb(alpha, 0x000000);
            g.drawString(font, p.text, -tw / 2 - 1, -4, dark, false);
            g.drawString(font, p.text, -tw / 2 + 1, -4, dark, false);
            g.drawString(font, p.text, -tw / 2, -5, dark, false);
            g.drawString(font, p.text, -tw / 2, -3, dark, false);
            g.drawString(font, p.text, -tw / 2, -4, argb(alpha, p.color), false);
            g.pose().popPose();
        }
    }

    private static void drawLetterbox(GuiGraphics g, int w, int h) {
        float bars = CinematicDirector.bars();
        if (bars <= 0.01F) return;
        int bh = (int) (h * 0.12F * bars);
        g.fill(0, 0, w, bh, 0xFF000000);
        g.fill(0, h - bh, w, h, 0xFF000000);
    }

    private static void drawFlash(GuiGraphics g, int w, int h) {
        float a = CameraEffects.flashAlpha();
        if (a <= 0.01F) return;
        int rgb = switch (CameraEffects.flashType()) {
            case ImpactPacket.FLASH_DARK -> 0x000000;
            case ImpactPacket.FLASH_RED -> 0xC01010;
            default -> 0xFFFFFF;
        };
        g.fill(0, 0, w, h, argb(a, rgb));
    }
}
