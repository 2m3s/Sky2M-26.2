package com.epic60869.sky2m.mixin.render;

import com.epic60869.sky2m.features.qol.VisualTweaks;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Misc > Visual Tweaks > Fullbright. Ported from NoFrills' LightmapRenderStateExtractorMixin. */
@Mixin(LightmapRenderStateExtractor.class)
public abstract class Sky2MVisualLightmapMixin {
    @ModifyExpressionValue(method = "extract", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ARGB;vector3fFromRGB24(I)Lorg/joml/Vector3f;", ordinal = 2))
    private Vector3f sky2m$ambientLight(Vector3f original) {
        return VisualTweaks.ambientLight() ? new Vector3f(1f, 1f, 1f) : original;
    }

    @ModifyExpressionValue(method = "extract", at = @At(value = "INVOKE", target = "Ljava/lang/Double;floatValue()F", ordinal = 0))
    private float sky2m$gamma(float original) {
        return VisualTweaks.gammaLight() ? 1600f : original;
    }
}
