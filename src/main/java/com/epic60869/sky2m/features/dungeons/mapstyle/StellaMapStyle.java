package com.epic60869.sky2m.features.dungeons.mapstyle;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** TODO: Stella's dungeon map look. Until it is written this delegates to the Skyblocker style. */
public final class StellaMapStyle implements MapStyleRenderer {
    private final SkyblockerMapStyle fallback = new SkyblockerMapStyle();

    @Override
    public String id() {
        return "stella";
    }

    @Override
    public int width() {
        return fallback.width();
    }

    @Override
    public int height() {
        return fallback.height();
    }

    @Override
    public void render(GuiGraphicsExtractor g, MapView view, boolean preview) {
        fallback.render(g, view, preview); // TODO: replace with Stella's renderer
    }
}
