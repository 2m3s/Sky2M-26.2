package com.epic60869.sky2m.mixin;

import com.epic60869.sky2m.features.qol.TabListExtras;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Compact Tab List: the tab list without Hypixel's blank filler lines. */
@Mixin(PlayerTabOverlay.class)
public class Sky2MCompactTabMixin {
    @Inject(method = "getPlayerInfos", at = @At("RETURN"), cancellable = true)
    private void sky2m$compact(CallbackInfoReturnable<List<PlayerInfo>> cir) {
        if (!TabListExtras.compact()) return;
        List<PlayerInfo> kept = cir.getReturnValue().stream().filter(info -> !TabListExtras.blank(info)).toList();
        cir.setReturnValue(kept);
    }
}
