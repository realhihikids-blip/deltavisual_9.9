package dev.deltavisuals.feature;

import dev.deltavisuals.core.Category;
import dev.deltavisuals.core.Module;
import dev.deltavisuals.core.Setting;
import dev.deltavisuals.render.Gfx;
import dev.deltavisuals.util.Ease;
import dev.deltavisuals.util.Palette;
import net.minecraft.client.MinecraftClient;

/**
 * Changes the hue of the world lighting on your screen (for example a pink-violet mood). A faint colour wash is
 * drawn over the 3D view right in front of the camera. It does not change the real light level, so it never hides
 * anything: low strength gives a gentle tint, high strength a strong colour grade. Hands, GUI and HUD stay untouched.
 */
public final class LightTint extends Module {
    private static final String[] PRESETS = {"Pink Violet", "Sunset", "Ocean", "Toxic", "Custom"};
    private static final int[][] PRESET_RGB = {
        {0xFF7AD9, 0x8A4DFF}, {0xFF9A3C, 0xFF3C6E}, {0x38BDF8, 0x1D4ED8}, {0x7CFF5A, 0xE6FF3C}, {0, 0}
    };

    private final Setting.Mode preset, colorMode;
    private final Setting.Color c1, c2;
    private final Setting.Num strength, flow, pulseSpeed, pulseAmount;
    private final Setting.Bool pulse;
    private final Palette pal = new Palette();
    private float fade;
    private double time;
    private long lastNanos;

    public LightTint() {
        super("Light Tint", "Changes the hue of the world light on your screen, e.g. pink-violet. Only visual", Category.WORLD, false);
        add(new Setting.Header("Tint"));
        preset = add(new Setting.Mode("Tint Preset", PRESETS, 0));
        colorMode = add(new Setting.Mode("Color Mode", new String[]{"Static", "Gradient", "Rainbow"}, 1));
        c1 = add(new Setting.Color("Main Color", 0xFFFF7AD9, false));
        c2 = add(new Setting.Color("Second Color", 0xFF8A4DFF, false));
        c1.visibleIf(() -> preset.is("Custom") && !colorMode.is("Rainbow"));
        c2.visibleIf(() -> preset.is("Custom") && colorMode.is("Gradient"));
        strength = add(new Setting.Num("Strength", 3, 60, 1, 16, "%"));
        flow = add(new Setting.Num("Color Flow Speed", 0.2, 5, 0.1, 1, "x"));
        flow.visibleIf(() -> !colorMode.is("Static"));
        add(new Setting.Header("Animation"));
        pulse = add(new Setting.Bool("Pulse", false));
        pulseSpeed = add(new Setting.Num("Pulse Speed", 0.2, 4, 0.1, 1, "x"));
        pulseSpeed.visibleIf(pulse::get);
        pulseAmount = add(new Setting.Num("Pulse Amount", 5, 80, 5, 30, "%"));
        pulseAmount.visibleIf(pulse::get);
        add(new Setting.Info("Note", () -> "Colour wash over the 3D view only. The real light level of the world is not changed"));
    }

    @Override public void onDisable() { fade = 0f; lastNanos = 0; }

    @Override
    public void onWorldRender() {
        if (MinecraftClient.getInstance().world == null) return;
        long now = System.nanoTime();
        float dt = lastNanos == 0 ? 0f : Math.min(0.1f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;
        time += dt;
        fade = Ease.approach(fade, 1f, dt, 4f);

        float a = strength.floatValue() / 100f * Ease.smooth(fade);
        if (pulse.get()) a *= 1f - pulseAmount.floatValue() / 100f * (0.5f + 0.5f * (float) Math.sin(time * 2.0 * pulseSpeed.get()));
        if (a <= 0.003f) return;

        int i = preset.index();
        boolean custom = preset.is("Custom");
        int r1 = custom ? c1.rgb() : PRESET_RGB[i][0];
        int r2 = custom ? c2.rgb() : PRESET_RGB[i][1];
        pal.set(colorMode.get(), r1, r2, flow.get(), time * 20.0);
        Gfx.use(false, false);
        Gfx.screenTint(0.11, pal.at(0.0, a));
    }
}
