package com.epic60869.sky2m.mixin.render;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** GameRenderer's post effect, for the crop fever hue shift (26.2; 26.3 sets it on the player instead). */
@Mixin(GameRenderer.class)
public interface Sky2MGameRendererInvoker {
    @Invoker("setPostEffect")
    void sky2m$setPostEffect(Identifier id);
}
