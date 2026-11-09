package com.mayhem.shinobi.client;

import com.mayhem.network.MayhemNetwork;
import com.mayhem.shinobi.appearance.Appearance;
import com.mayhem.shinobi.net.ShinobiNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** MCA-style body customisation: height/width sliders, skin, hair, eyes and marks with a live preview. Server re-validates. */
public class AppearanceScreen extends Screen {
    private CompoundTag work;

    public AppearanceScreen() { super(Component.literal("Appearance")); }

    private static final class Slide extends AbstractSliderButton {
        private final String label; private final java.util.function.DoubleConsumer on;
        Slide(int x, int y, int w, String label, double v, java.util.function.DoubleConsumer on) {
            super(x, y, w, 18, Component.literal(label), v); this.label = label; this.on = on; updateMessage();
        }
        @Override protected void updateMessage() {
            setMessage(Component.literal(label + ": " + String.format("%.2f", Appearance.SCALE_MIN + value * (Appearance.SCALE_MAX - Appearance.SCALE_MIN))));
        }
        @Override protected void applyValue() { on.accept(value); }
    }

    @Override protected void init() {
        if (work == null) {
            work = minecraft != null && minecraft.player != null ? Appearance.of(minecraft.player.getUUID()).copy() : Appearance.defaults();
        }
        int x = width / 2 - 20, y = 30;
        addRenderableWidget(new Slide(x, y, 150, "Height", norm(work.getFloat("height")), v -> set("height", (float) (Appearance.SCALE_MIN + v * (Appearance.SCALE_MAX - Appearance.SCALE_MIN)))));
        addRenderableWidget(new Slide(x, y + 22, 150, "Width", norm(work.getFloat("width")), v -> set("width", (float) (Appearance.SCALE_MIN + v * (Appearance.SCALE_MAX - Appearance.SCALE_MIN)))));
        cycle(x, y + 50, "Skin tone", "skin", Appearance.SKIN_TONES, -1);
        cycle(x, y + 74, "Hair colour", "hairColor", Appearance.HAIR_COLORS, -1);
        cycle(x, y + 98, "Eye colour", "eyeColor", Appearance.EYE_COLORS, -1);
        cycleIdx(x, y + 122, "Hair style", "hairStyle", Appearance.HAIR_STYLES);
        cycleIdx(x, y + 146, "Marks", "marks", Appearance.MARKS);
        cycleIdx(x, y + 170, "Eye style", "eyeStyle", new String[]{"Round", "Sharp", "Soft", "Wide"});
        addRenderableWidget(Button.builder(Component.literal("Apply"), b -> {
            MayhemNetwork.CHANNEL.sendToServer(new ShinobiNet.AppearSet(work));
            ClientShinobi.send(ShinobiNet.Op.ONBOARD_DONE);
            minecraft.setScreen(new ShinobiScreen());
        }).bounds(x, y + 198, 72, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> minecraft.setScreen(new ShinobiScreen())).bounds(x + 78, y + 198, 72, 18).build());
    }

    private static double norm(float v) { return Math.max(0, Math.min(1, (v - Appearance.SCALE_MIN) / (Appearance.SCALE_MAX - Appearance.SCALE_MIN))); }

    private void set(String k, float v) { work.putFloat(k, v); preview(); }

    private void preview() {
        if (minecraft != null && minecraft.player != null) Appearance.CACHE.put(minecraft.player.getUUID(), Appearance.sanitize(work));
    }

    private void cycle(int x, int y, String label, String key, int[] palette, int unused) {
        addRenderableWidget(Button.builder(Component.literal(label + " >"), b -> {
            int cur = work.getInt(key), idx = 0;
            for (int i = 0; i < palette.length; i++) if (palette[i] == cur) idx = i;
            work.putInt(key, palette[(idx + 1) % palette.length]);
            preview();
        }).bounds(x, y, 150, 18).build());
    }

    private void cycleIdx(int x, int y, String label, String key, String[] names) {
        addRenderableWidget(Button.builder(Component.literal(label + " >"), b -> {
            work.putInt(key, (work.getInt(key) + 1) % names.length);
            b.setMessage(Component.literal(label + ": " + names[work.getInt(key)] + " >"));
            preview();
        }).bounds(x, y, 150, 18).build());
    }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.drawCenteredString(font, "Customise your shinobi", width / 2, 12, 0xFFFFFFFF);
        if (minecraft != null && minecraft.player != null)
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, width / 2 - 100, height / 2 + 60, 60, width / 2 - 100 - mx, height / 2 - mx * 0 - my, minecraft.player);
        super.render(g, mx, my, pt);
    }

    @Override public boolean isPauseScreen() { return false; }
}
