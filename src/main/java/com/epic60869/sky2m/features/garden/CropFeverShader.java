package com.epic60869.sky2m.features.garden;

import com.epic60869.sky2m.mixin.render.Sky2MGameRendererInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;

/**
 * Turns the crop fever hue shift on and off. On 26.2 the game renderer has one post effect (set through
 * Sky2MGameRendererInvoker, as SkyOcean does); the 26.3 version uses the player's list of post effects. Each speed is
 * its own post effect file, {@code assets/sky2m/post_effect/crop_fever_hue_shift_<speed>.json}.
 */
final class CropFeverShader {
    private static Identifier current;

    private CropFeverShader() {}

    static void apply(String speed) {
        GameRenderer renderer = Minecraft.getInstance().gameRenderer;
        if (renderer == null) return;
        current = Identifier.fromNamespaceAndPath("sky2m", "crop_fever_hue_shift_" + speed);
        ((Sky2MGameRendererInvoker) renderer).sky2m$setPostEffect(current);
    }

    static void clear() {
        GameRenderer renderer = Minecraft.getInstance().gameRenderer;
        if (current != null && renderer != null && current.equals(renderer.currentPostEffect())) renderer.clearPostEffect();
        current = null;
    }
}
