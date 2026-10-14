// Ported from Skysoft (https://github.com/Akinsoft/Skysoft), src/main/java/com/skysoft/mixin/ItemInHandRendererMixin.java.
// SPDX-License-Identifier: LGPL-3.0-only
package com.epic60869.sky2m.mixin;

import com.epic60869.sky2m.features.helditem.HeldItemSwing;
import com.epic60869.sky2m.features.helditem.HeldItemTextures;
import com.epic60869.sky2m.features.helditem.HeldItemTransforms;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Misc > Held Item: the first-person item's transform, swing style, and the Held Item Update Fix. */
@Mixin(ItemInHandRenderer.class)
public abstract class Sky2MHeldItemMixin {
    /** Held Item Update Fix: Hypixel updating the same item doesn't play the re-equip animation. */
    @ModifyReturnValue(method = "shouldInstantlyReplaceVisibleItem", at = @At("RETURN"))
    private boolean sky2m$keepUpdatedItemVisible(boolean original, ItemStack currentlyVisible, ItemStack expected) {
        if (original) return true;
        try {
            return HeldItemTextures.shouldPreserveUpdate(currentlyVisible, expected);
        } catch (Throwable e) {
            return false;
        }
    }

    @Inject(method = "renderItem", at = @At("HEAD"))
    private void sky2m$transformHeldItem(LivingEntity entity, ItemStack stack, ItemDisplayContext context, PoseStack pose,
                                            SubmitNodeCollector collector, int light, CallbackInfo ci) {
        if (context != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND && context != ItemDisplayContext.FIRST_PERSON_LEFT_HAND) return;
        try {
            HeldItemTransforms.apply(stack, pose);
        } catch (Throwable ignored) {}
        try {
            HeldItemSwing.apply(stack, pose);
        } catch (Throwable ignored) {}
    }

    @WrapMethod(method = "submitArmWithItem")
    private void sky2m$renderWithHeldItemSwing(AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand,
                                                  float attack, ItemStack stack, float inverseArmHeight, PoseStack pose,
                                                  SubmitNodeCollector collector, int light, Operation<Void> original) {
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        HeldItemSwing.renderWithSwing(stack, attack, arm,
            () -> original.call(player, frameInterp, xRot, hand, attack, stack, inverseArmHeight, pose, collector, light));
    }

    /** Swing In Place: the swing still turns the item, without its slide across the screen. */
    @WrapOperation(method = "swingArm", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
    private void sky2m$swingInPlace(PoseStack pose, float x, float y, float z, Operation<Void> original) {
        boolean skip;
        try {
            skip = HeldItemSwing.skipSwingSlide();
        } catch (Throwable e) {
            skip = false;
        }
        if (!skip) original.call(pose, x, y, z);
    }

    /** Swing style Item Only: the arm doesn't swing (the item does, in renderItem). */
    @Inject(method = "swingArm", at = @At("HEAD"), cancellable = true)
    private void sky2m$replaceHeldItemSwing(float attack, PoseStack pose, int invert, HumanoidArm arm, CallbackInfo ci) {
        boolean replaced;
        try {
            replaced = HeldItemSwing.replaceVanillaSwing();
        } catch (Throwable e) {
            replaced = false;
        }
        if (replaced) ci.cancel();
    }
}
