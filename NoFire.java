package dev.deltavisuals.feature;

import dev.deltavisuals.core.Category;
import dev.deltavisuals.core.Module;
import dev.deltavisuals.core.Setting;

/**
 * No Fire: hides the flame picture on your screen while you burn. Own switch and keybind; the work is done by
 * InGameOverlayFireMixin, which asks this module (and the older Clean View option). Client side only.
 */
public final class NoFire extends Module {
    public NoFire() {
        super("No Fire", "Hides the fire overlay on your screen while you are burning", Category.VISUALS, false);
        add(new Setting.Info("Note", () -> "Only your screen changes. You still burn and take damage as usual"));
    }
}
