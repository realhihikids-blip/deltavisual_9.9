package dev.deltavisuals.feature;

import dev.deltavisuals.core.Category;
import dev.deltavisuals.core.Module;
import dev.deltavisuals.core.Setting;
import dev.deltavisuals.render.Gfx;
import dev.deltavisuals.util.ColorUtil;
import dev.deltavisuals.util.Ease;
import dev.deltavisuals.util.Palette;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.World;

/**
 * Soft custom fog: several translucent haze walls around the camera, thicker with distance and fading upwards, so
 * the horizon melts into the sky. Drawn as normal world geometry (depth tested: terrain and entities always stay in
 * front of it). It does not touch the vanilla fog, so a thin band of vanilla fog colour may remain at the very edge
 * of the render distance. No particles, no textures.
 */
public final class CustomFog extends Module {
    private static final int LAYERS = 5, SEGS = 40;

    private final Setting.Num distance, density, height, sink, skySoften, breathe, flow;
    private final Setting.Bool animated, overworldOnly;
    private final Setting.Mode colorMode;
    private final Setting.Color c1, c2;
    private final Palette pal = new Palette();
    private float fade;
    private double time;
    private long lastNanos;

    public CustomFog() {
        super("Custom Fog", "Soft coloured haze on the horizon that smooths the transition to the sky", Category.WORLD, false);
        add(new Setting.Header("Fog"));
        distance = add(new Setting.Num("Fog Distance", 16, 256, 4, 80, " m"));
        density = add(new Setting.Num("Density", 5, 100, 1, 45, "%"));
        height = add(new Setting.Num("Fog Height", 4, 80, 1, 28, " m"));
        sink = add(new Setting.Num("Fog Depth Below", 4, 60, 1, 24, " m"));
        skySoften = add(new Setting.Num("Sky Softening", 0, 100, 1, 35, "%"));
        overworldOnly = add(new Setting.Bool("Only In Overworld", false));
        add(new Setting.Header("Animation"));
        animated = add(new Setting.Bool("Breathing", true));
        breathe = add(new Setting.Num("Breathing Amount", 0, 50, 1, 12, "%"));
        breathe.visibleIf(animated::get);
        add(new Setting.Header("Color"));
        colorMode = add(new Setting.Mode("Color Mode", Palette.PLAIN, 0));
        c1 = add(new Setting.Color("Main Color", 0xFFC9B8FF, false));
        c2 = add(new Setting.Color("Second Color", 0xFF8FD0FF, false));
        c1.visibleIf(() -> !colorMode.is("Rainbow") && !colorMode.is("Fire"));
        c2.visibleIf(() -> colorMode.is("Gradient") || colorMode.is("Neon"));
        flow = add(new Setting.Num("Color Flow Speed", 0.2, 5, 0.1, 1, "x"));
        flow.visibleIf(() -> !colorMode.is("Static"));
        add(new Setting.Info("Note", () -> "Vanilla terrain fog is not changed, only a soft haze is added on top"));
    }

    @Override public void onDisable() { fade = 0f; lastNanos = 0; }

    @Override
    public void onWorldRender() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        if (overworldOnly.get() && !World.OVERWORLD.equals(mc.world.getRegistryKey())) return;

        long now = System.nanoTime();
        float dt = lastNanos == 0 ? 0f : Math.min(0.1f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;
        time += dt;
        fade = Ease.approach(fade, 1f, dt, 3f);          // soft fade-in when switched on
        float k = Ease.smooth(fade);
        if (k <= 0.01f) return;

        float dens = density.floatValue() / 100f;
        if (animated.get()) dens *= 1f + breathe.floatValue() / 100f * (float) Math.sin(time * 0.35);
        dens = Ease.clamp01(dens) * k;
        float soft = skySoften.floatValue() / 100f;
        pal.set(colorMode.get(), c1.rgb(), c2.rgb(), flow.get(), time * 20.0);

        double cx = Gfx.camX, cy = Gfx.camY, cz = Gfx.camZ;
        double above = height.get(), below = sink.get(), dist = distance.get();
        if (!Gfx.hasBudget(LAYERS * SEGS + 4)) return;
        Gfx.use(false, false);
        for (int i = 0; i < LAYERS; i++) {
            float t = (i + 1) / (float) LAYERS;
            double r = dist * (0.45 + 0.55 * t);
            float a = dens * 0.32f * (0.35f + 0.65f * t * t);
            int rgb = pal.at(t * 0.6);
            int bottom = ColorUtil.withAlpha(rgb, Math.round(255f * Math.min(1f, a)));
            int top = ColorUtil.withAlpha(rgb, Math.round(255f * Math.min(1f, a * soft * 0.7f)));
            Gfx.band(cx, cy, cz, r, -below, above, SEGS, bottom, top);
        }
    }
}
