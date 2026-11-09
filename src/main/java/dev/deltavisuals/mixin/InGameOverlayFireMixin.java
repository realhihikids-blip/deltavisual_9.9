package dev.deltavisuals.mixin;

import dev.deltavisuals.core.Modules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Clean View: skips drawing the fire on the screen. Skipped silently if the class or method is not found. */
@Mixin(targets = "net.minecraft.client.gui.hud.InGameOverlayRenderer")
public abstract class InGameOverlayFireMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private static void deltaVisuals$noFire(CallbackInfo ci) {
        try {
            if (Modules.CLEAN_VIEW != null && Modules.CLEAN_VIEW.noFire()) ci.cancel();
        } catch (Throwable ignored) { }
    }
}
