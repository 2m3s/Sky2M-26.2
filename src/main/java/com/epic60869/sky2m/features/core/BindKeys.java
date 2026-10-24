package com.epic60869.sky2m.features.core;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Whether a MoulConfig keybind setting (a key, or a mouse button stored the MoulConfig way) is held. The Minecraft 26.2
 * and 26.3 versions of this class differ (GLFW and SDL); features use this so their own code is the same in both.
 */
public final class BindKeys {
    private BindKeys() {}

    /** Whether the bind is set at all. */
    public static boolean isSet(int bind) {
        return bind != GLFW.GLFW_KEY_UNKNOWN && bind != 0;
    }

    /**
     * The GLFW mouse button a bind stands for, or -1 for a keyboard key or no bind. MoulConfig 4 stores mouse buttons
     * as the button number (1-9; 0 is treated as unset), older versions as -100 + the button. Keyboard keys start at
     * 32, so the two never overlap.
     */
    public static int mouseButton(int bind) {
        if (bind >= 1 && bind <= GLFW.GLFW_MOUSE_BUTTON_LAST) return bind;
        if (bind >= -100 && bind <= -100 + GLFW.GLFW_MOUSE_BUTTON_LAST) return bind + 100;
        return -1;
    }

    public static boolean down(int bind) {
        if (!isSet(bind) || Minecraft.getInstance().getWindow() == null) return false;
        long window = Minecraft.getInstance().getWindow().handle();
        int button = mouseButton(bind);
        if (button >= 0) return GLFW.glfwGetMouseButton(window, button) == GLFW.GLFW_PRESS;
        return bind >= GLFW.GLFW_KEY_SPACE && bind <= GLFW.GLFW_KEY_LAST && GLFW.glfwGetKey(window, bind) == GLFW.GLFW_PRESS;
    }
}
