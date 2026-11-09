package dev.deltavisuals.mixin;

import dev.deltavisuals.core.Modules;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Clean View: cancels the camera tilt when you are hurt and, optionally, the view bobbing.
 * Both injections are skipped silently if the method cannot be found (require = 0).
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererShakeMixin {
    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true, require = 0)
    private void deltaVisuals$noHurtShake(CallbackInfo ci) {
        try {
            if (Modules.CLEAN_VIEW != null && Modules.CLEAN_VIEW.noHurtShake()) ci.cancel();
        } catch (Throwable ignored) { /* never break rendering over a cosmetic effect */ }
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true, require = 0)
    private void deltaVisuals$noBobbing(CallbackInfo ci) {
        try {
            if (Modules.CLEAN_VIEW != null && Modules.CLEAN_VIEW.noBobbing()) ci.cancel();
        } catch (Throwable ignored) { }
    }
}
