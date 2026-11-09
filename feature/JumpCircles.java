package dev.deltavisuals.feature;

import dev.deltavisuals.core.Category;
import dev.deltavisuals.core.Module;
import dev.deltavisuals.core.Setting;
import dev.deltavisuals.render.Gfx;
import dev.deltavisuals.util.ColorUtil;
import dev.deltavisuals.util.Palette;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

/**
 * Jump Circles: when you jump (and, optionally, when you land) a circle spreads out on the ground under your feet
 * like a ripple in a puddle and fades. Flat solid geometry drawn through {@link Gfx}, no particles, only on your screen.
 */
public final class JumpCircles extends Module {
    private static final double TAU = Math.PI * 2;
    private static final int MAX = 12;

    private static final class Ring {
        boolean active;
        int start;
        double x, y, z, scale;
    }

    private final Setting.Bool onJump, onLand, fill, glow;
    private final Setting.Num duration, radius, rings, opacity, flow;
    private final Setting.Mode colorMode;
    private final Setting.Color c1, c2;

    private final Ring[] list = new Ring[MAX];
    private final Palette pal = new Palette();
    private int ticks;
    private boolean wasGround = true;
    private double lastVy;

    public JumpCircles() {
        super("Jump Circles", "A circle spreads out on the ground like a ripple in a puddle when you jump or land",
            Category.VISUALS, false);
        onJump = add(new Setting.Bool("On Jump", true));
        onLand = add(new Setting.Bool("On Landing", false));
        duration = add(new Setting.Num("Duration", 0.4, 2.0, 0.05, 0.9, " s"));
        radius = add(new Setting.Num("Radius", 0.5, 4, 0.1, 1.6, " m"));
        rings = add(new Setting.Num("Rings", 1, 3, 1, 2));
        fill = add(new Setting.Bool("Soft Fill", true));
        opacity = add(new Setting.Num("Opacity", 10, 100, 5, 80, "%"));
        colorMode = add(new Setting.Mode("Color Mode", Palette.PLAIN, 1));
        c1 = add(new Setting.Color("Main Color", 0xFF33CCFF, false));
        c2 = add(new Setting.Color("Second Color", 0xFF8A3CFF, false));
        c1.visibleIf(() -> !colorMode.is("Rainbow") && !colorMode.is("Fire"));
        c2.visibleIf(() -> colorMode.is("Gradient") || colorMode.is("Neon"));
        flow = add(new Setting.Num("Color Flow Speed", 0.2, 5, 0.1, 1, "x"));
        flow.visibleIf(() -> !colorMode.is("Static"));
        glow = add(new Setting.Bool("Glow", true));
        for (int i = 0; i < MAX; i++) list[i] = new Ring();
    }

    @Override public void onDisable() { for (Ring r : list) r.active = false; }

    @Override
    public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null) { wasGround = true; return; }
        if (mc.isPaused()) return;
        ticks++;
        boolean ground = p.isOnGround();
        double vy = p.getVelocity().y;
        if (wasGround && !ground && vy > 0.1 && onJump.get()) spawn(p.getX(), p.getY(), p.getZ(), 1.0);
        if (!wasGround && ground && lastVy < -0.45 && onLand.get()) spawn(p.getX(), p.getY(), p.getZ(), 0.75);
        wasGround = ground;
        lastVy = vy;
    }

    private void spawn(double x, double y, double z, double scale) {
        Ring r = null;
        for (Ring i : list) if (!i.active) { r = i; break; }
        if (r == null) r = list[0];
        r.active = true; r.start = ticks; r.x = x; r.y = y; r.z = z; r.scale = scale;
    }

    @Override
    public void onWorldRender() {
        double durTicks = duration.get() * 20.0;
        int n = rings.intValue();
        float maxA = opacity.floatValue() / 100f;
        pal.set(colorMode.get(), c1.rgb(), c2.rgb(), flow.get(), ticks + Gfx.partial);
        boolean used = false;
        for (Ring r : list) {
            if (!r.active) continue;
            double age = ticks - r.start + Gfx.partial;
            if (age >= durTicks + n * 3.0) { r.active = false; continue; }
            if (!used) { Gfx.use(glow.get(), false); used = true; }
            for (int k = 0; k < n; k++) {
                double t = (age - k * 3.0) / durTicks;          // each next ring starts a little later
                if (t <= 0 || t >= 1) continue;
                double ease = 1.0 - Math.pow(1.0 - t, 3.0);     // fast start, slow end: like a ripple
                double rad = radius.get() * r.scale * ease * (1.0 - k * 0.22);
                double w = Math.max(0.03, 0.14 * (1.0 - t) * r.scale);
                float fade = (float) Math.pow(1.0 - t, 1.4) * maxA;
                double y = r.y + 0.03 + k * 0.004;
                int edge = pal.at(k * 0.25 + t * 0.3, fade);
                if (fill.get()) {
                    Gfx.ringFlat(r.x, y, r.z, 0.0, rad, 0.0, TAU, 40,
                        ColorUtil.withAlpha(edge, 0), ColorUtil.withAlpha(edge, Math.round(255f * fade * 0.35f)));
                }
                Gfx.ringFlat(r.x, y + 0.002, r.z, Math.max(0.0, rad - w), rad, 0.0, TAU, 48,
                    ColorUtil.withAlpha(edge, Math.round(255f * fade * 0.4f)), edge);
            }
        }
    }
}
