package com.HiWord9.RPRenames.mod.mixin;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(DrawContext.class)
public abstract class DrawContextMixin {
    @Shadow
    @Nullable
    private Runnable tooltipDrawer;

    @Unique
    private final List<Runnable> extraTooltipDrawers = new ArrayList<>();

    @Inject(
            method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;IILnet/minecraft/client/gui/tooltip/TooltipPositioner;Lnet/minecraft/util/Identifier;Z)V",
            at = @At("HEAD"), cancellable = true
    )
    private void onDrawTooltip(TextRenderer textRenderer, List<TooltipComponent> components, int x, int y, TooltipPositioner positioner, @Nullable Identifier texture, boolean focused, CallbackInfo ci) {
        if (!components.isEmpty()) {
            if (tooltipDrawer == null || focused) {
                tooltipDrawer = () -> ((DrawContext)(Object)this)
                        .drawTooltipImmediately(textRenderer, components, x, y, positioner, texture);
            } else {
                extraTooltipDrawers.add(() -> ((DrawContext)(Object)this)
                        .drawTooltipImmediately(textRenderer, components, x, y, positioner, texture));
            }
            ci.cancel();
        }
    }

    @Inject(method = "drawDeferredElements", at = @At("TAIL"))
    private void afterRenderTooltip(CallbackInfo ci) {
        if (!extraTooltipDrawers.isEmpty()) {
            DrawContext ctx = (DrawContext)(Object)this;
            for (Runnable drawer : extraTooltipDrawers) {
                ctx.createNewRootLayer();
                drawer.run();
            }
            extraTooltipDrawers.clear();
        }
    }
}
