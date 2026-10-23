package com.epic60869.sky2m.features.qol;

import com.epic60869.sky2m.Sky2MConfig;
import com.epic60869.sky2m.Sky2MTabWidgetManager;
import com.epic60869.sky2m.custom.util.Compat;
import com.epic60869.sky2m.features.core.Sky2MHuds;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Tab list extras, after Skyblocker's compact tab list and tab widgets (https://github.com/SkyblockerMod/Skyblocker,
 * LGPL-3.0): the compact tab list drops Hypixel's blank filler lines (Sky2MCompactTabMixin), and the widgets HUD shows
 * chosen tab widgets (Sky2M's own tab parser) without holding tab. Display only.
 */
public final class TabListExtras {
    private static final String ID = "tab_widgets";

    private TabListExtras() {}

    private static QolConfigs.TabListExtras config() {
        Sky2MConfig c = Sky2MConfig.current();
        return c == null ? null : c.misc.tabListExtras;
    }

    private static boolean hudOn() {
        QolConfigs.TabListExtras c = config();
        return c != null && c.widgetsHud;
    }

    public static void init() {
        Sky2MHuds.setting(ID, TabListExtras::hudOn);
        Sky2MHuds.setSettingsSearch(ID, "Tab Widgets HUD");
        Sky2MHuds.register(ID, "Tab Widgets", () -> hudOn() && Compat.isOnSkyblock(), TabListExtras::lines,
            List.of(Component.literal("Pet").withStyle(ChatFormatting.YELLOW), Component.literal(" [Lvl 100] Golden Dragon")), 20, 150);
    }

    /** Whether the compact tab list is on (read by the mixin on every frame the tab list is open). */
    public static boolean compact() {
        QolConfigs.TabListExtras c = config();
        return c != null && c.compact && Compat.isOnSkyblock();
    }

    /** True for a tab line with nothing to read: Hypixel pads its columns with them. */
    public static boolean blank(PlayerInfo info) {
        Component name = Compat.rawTabName(info);
        if (name == null) return info.getProfile() == null || info.getProfile().name().isBlank();
        return name.getString().isBlank();
    }

    private static List<Component> lines() {
        QolConfigs.TabListExtras c = config();
        if (c == null) return List.of();
        List<Component> out = new ArrayList<>();
        for (String raw : c.widgets.split(",")) {
            String name = raw.trim();
            if (name.isEmpty()) continue;
            Sky2MTabWidgetManager.Widget widget = Sky2MTabWidgetManager.get(name);
            if (widget == Sky2MTabWidgetManager.Widget.EMPTY || (widget.lines().isEmpty() && widget.detail().getString().isBlank())) continue;
            out.add(Component.literal(name).withColor(widget.color()));
            if (!widget.detail().getString().isBlank()) out.add(widget.detail());
            out.addAll(widget.lines());
        }
        return out;
    }
}
