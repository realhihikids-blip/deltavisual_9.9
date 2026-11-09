package dev.deltavisuals.feature;

import dev.deltavisuals.core.Category;
import dev.deltavisuals.core.Module;
import dev.deltavisuals.core.Setting;

/**
 * Clean View: removes things that shake or cover the screen. Purely client side and visual.
 * The work is done by small mixins (GameRendererShakeMixin, InGameOverlayFireMixin) which ask this module.
 */
public final class CleanView extends Module {
    private final Setting.Bool hurtShake, bobbing, fire;

    public CleanView() {
        super("Clean View", "Removes screen shake when you are hit, view bobbing and the fire overlay on your screen",
            Category.VISUALS, false);
        hurtShake = add(new Setting.Bool("Remove Hit Shake", true));
        bobbing = add(new Setting.Bool("Remove View Bobbing", false));
        fire = add(new Setting.Bool("Remove Fire Overlay", true));
        add(new Setting.Info("Note", () -> "Only changes your own screen. Nothing is sent to the server"));
    }

    public boolean noHurtShake() { return isEnabled() && hurtShake.get(); }
    public boolean noBobbing() { return isEnabled() && bobbing.get(); }
    public boolean noFire() { return isEnabled() && fire.get(); }
}
