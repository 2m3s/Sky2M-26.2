package com.epic60869.sky2m.mixin.render;

import com.epic60869.sky2m.features.qol.VisualTweaks;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Misc > Visual Tweaks > Hide Clouds: skips the cloud render pass. */
@Mixin(LevelRenderer.class)
public abstract class Sky2MVisualCloudMixin {
    @Inject(method = "addCloudsPass", at = @At("HEAD"), cancellable = true)
    private void sky2m$noClouds(FrameGraphBuilder frame, CloudStatus status, Vec3 camera, long time, float partial, int color, float height, int renderDistance, CallbackInfo ci) {
        if (VisualTweaks.hideClouds()) ci.cancel();
    }
}
