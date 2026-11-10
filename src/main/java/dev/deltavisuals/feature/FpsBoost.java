package dev.deltavisuals.feature;

import dev.deltavisuals.core.Category;
import dev.deltavisuals.core.Module;
import dev.deltavisuals.core.Setting;
import dev.deltavisuals.render.Gfx;
import net.minecraft.client.MinecraftClient;

/**
 * FPS Boost: lowers the cost of every client effect drawn through {@link Gfx}. It scales the number of segments of
 * rings, bands and spheres (level of detail), caps the number of shapes per frame and can skip effects that are far
 * away. With "Adaptive Quality" the detail follows your FPS by itself: it drops quickly when the game gets slow and
 * climbs back slowly when there is spare time. It does not change the vanilla renderer.
 */
public final class FpsBoost extends Module {
    private static final String[] PRESETS = {"Ultra", "High", "Balanced", "Low", "Potato"};
    private static final float[] LOD = {1f, 1f, 0.7f, 0.5f, 0.35f};
    private static final int[] BUDGET = {Gfx.MAX_QUADS_PER_FRAME, 30000, 20000, 12000, 6000};
    private static final double[] DIST = {0, 0, 0, 64, 32};

    private final Setting.Mode preset;
    private final Setting.Bool adaptive;
    private final Setting.Num targetFps, minQuality, distance;
    private float ad = 1f;      // adaptive factor 0.25..1

    public FpsBoost() {
        super("FPS Boost", "Lowers the detail of client effects and limits how many shapes are drawn, so the game stays smooth",
            Category.VISUALS, false);
        add(new Setting.Header("Quality"));
        preset = add(new Setting.Mode("Quality Preset", PRESETS, 2));
        adaptive = add(new Setting.Bool("Adaptive Quality", true));
        targetFps = add(new Setting.Num("Target FPS", 30, 240, 5, 60));
        targetFps.visibleIf(adaptive::get);
        minQuality = add(new Setting.Num("Min Quality", 25, 100, 5, 40, "%"));
        minQuality.visibleIf(adaptive::get);
        distance = add(new Setting.Num("Effect Distance", 0, 256, 8, 0, " m"));
        add(new Setting.Info("Current Detail", () -> Math.round(Gfx.lod * 100f) + "%"));
        add(new Setting.Info("Note", () -> "0 m = limit by preset. Only client effects change, vanilla graphics stay as they are"));
    }

    @Override public void onEnable() { ad = 1f; apply(); }
    @Override public void onDisable() { ad = 1f; Gfx.lod = 1f; Gfx.budget = Gfx.MAX_QUADS_PER_FRAME; Gfx.effectDist = 0; }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.isPaused()) return;
        if (adaptive.get()) {
            int fps = mc.getCurrentFps();
            float floor = minQuality.floatValue() / 100f;
            float target = targetFps.floatValue();
            if (fps > 0 && fps < target * 0.92f) ad -= 0.03f;            // slow: drop detail fast
            else if (fps > target * 1.15f) ad += 0.01f;                   // spare time: recover slowly
            ad = Math.max(floor, Math.min(1f, ad));
        } else {
            ad = 1f;
        }
        apply();
    }

    private void apply() {
        int i = preset.index();
        Gfx.lod = Math.max(0.3f, LOD[i] * ad);
        Gfx.budget = Math.max(6000, Math.round(BUDGET[i] * Math.max(0.4f, ad)));
        Gfx.effectDist = distance.get() > 0 ? distance.get() : DIST[i];
    }
}
