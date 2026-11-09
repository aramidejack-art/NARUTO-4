package com.mayhem.shinobi.client;

import com.mayhem.shinobi.net.ShinobiNet.Op;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** NPC dialogue: text plus numbered options. The client sends only the option index; the server re-derives and validates it. */
public class DialogueScreen extends Screen {
    public DialogueScreen() { super(Component.literal("Dialogue")); }

    @Override protected void init() { refresh(); }

    public void refresh() {
        clearWidgets();
        List<String> opts = ClientShinobi.dlgOptions;
        int w = Math.min(width - 40, 340), x = (width - w) / 2, y = height - 30 - opts.size() * 22;
        for (int i = 0; i < opts.size(); i++) {
            final int idx = i;
            addRenderableWidget(Button.builder(Component.literal(opts.get(i)), b -> ClientShinobi.send(Op.DIALOGUE, "", ClientShinobi.dlgEntity, idx))
                    .bounds(x, y + i * 22, w, 20).build());
        }
    }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int w = Math.min(width - 40, 340), x = (width - w) / 2;
        int boxTop = height - 40 - ClientShinobi.dlgOptions.size() * 22 - 70;
        g.fill(x - 6, boxTop, x + w + 6, boxTop + 66, 0xDD0B0F1A);
        g.drawString(font, ClientShinobi.dlgName, x, boxTop + 4, 0xFFE6C27A, false);
        int ly = boxTop + 18;
        for (var line : font.split(Component.literal(ClientShinobi.dlgText), w)) { g.drawString(font, line, x, ly, 0xFFFFFFFF, false); ly += 10; }
        super.render(g, mx, my, pt);
    }

    @Override public boolean isPauseScreen() { return false; }
}
