package dev.deltavisuals.feature;

import dev.deltavisuals.core.Category;
import dev.deltavisuals.core.Module;
import dev.deltavisuals.core.Setting;
import dev.deltavisuals.render.Gfx;
import dev.deltavisuals.util.ColorUtil;
import dev.deltavisuals.util.Ease;
import dev.deltavisuals.util.Hash;
import dev.deltavisuals.util.Palette;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Hit Bubbles: glassy bubbles float up from the target you hit, wobble, swell and pop. Consecutive hits (a combo)
 * make more and bigger bubbles. Every bubble is a closed-form function of the burst age, so there is no per-tick
 * state and no allocation; drawn through {@link Gfx} (no Minecraft particles).
 */
public final class HitBubbles extends Module {
    private static final String[] COLOR_MODES = {"Auto", "Static", "Gradient", "Rainbow", "Neon"};
    private static final int MAX_BURSTS = 8, MAX_BUBBLES = 24;

    private static final class Burst {
        boolean active;
        int seed, n;
        double x, y, z, start, dur, scale;
    }

    private final Setting.Num count, size, rise, spread, duration, opacity, flow;
    private final Setting.Bool glint, glow, combo, playersOnly;
    private final Setting.Mode colorMode;
    private final Setting.Color c1, c2;

    private final Burst[] bursts = new Burst[MAX_BURSTS];
    private final Palette pal = new Palette();
    private int ticks, counter, lastHitTick = -1000, comboCount;

    public HitBubbles() {
        super("Hit Bubbles", "Glassy bubbles float up from the target you hit. Combos make them bigger", Category.VISUALS, false);
        add(new Setting.Header("Bubbles"));
        count = add(new Setting.Num("Bubble Count", 2, 16, 1, 7));
        size = add(new Setting.Num("Bubble Size", 0.3, 3, 0.05, 1.0, "x"));
        rise = add(new Setting.Num("Rise Height", 0.3, 3, 0.1, 1.3, " m"));
        spread = add(new Setting.Num("Spread", 0.2, 2, 0.1, 0.8, " m"));
        duration = add(new Setting.Num("Duration", 0.4, 3, 0.1, 1.4, " s"));
        combo = add(new Setting.Bool("Combo Growth", true));
        playersOnly = add(new Setting.Bool("Players Only", false));
        add(new Setting.Header("Look"));
        colorMode = add(new Setting.Mode("Color Mode", COLOR_MODES, 0));
        c1 = add(new Setting.Color("Main Color", 0xFF6FE3FF, false));
        c2 = add(new Setting.Color("Second Color", 0xFFB48CFF, false));
        c1.visibleIf(() -> !colorMode.is("Auto") && !colorMode.is("Rainbow"));
        c2.visibleIf(() -> colorMode.is("Gradient") || colorMode.is("Neon"));
        flow = add(new Setting.Num("Color Flow Speed", 0.2, 5, 0.1, 1, "x"));
        flow.visibleIf(() -> !colorMode.is("Static"));
        opacity = add(new Setting.Num("Opacity", 10, 100, 5, 70, "%"));
        glint = add(new Setting.Bool("Glint", true));
        glow = add(new Setting.Bool("Glow", false));
        for (int i = 0; i < MAX_BURSTS; i++) bursts[i] = new Burst();
    }

    @Override public void onDisable() { for (Burst b : bursts) b.active = false; comboCount = 0; lastHitTick = -1000; }

    /** Called from the AttackEntityCallback in Modules. */
    public void onAttack(Entity e, Vec3d hitPos) {
        if (!isEnabled() || !(e instanceof LivingEntity le)) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (le == mc.player) return;
        if (playersOnly.get() && !(le instanceof PlayerEntity)) return;
        if (ticks - lastHitTick < 3) return;                       // swing spam / invulnerability frames
        comboCount = ticks - lastHitTick < 60 ? comboCount + 1 : 1;
        lastHitTick = ticks;
        double x, y, z;
        if (hitPos != null) { x = hitPos.x; y = hitPos.y; z = hitPos.z; }
        else { x = le.getX(); y = le.getY() + le.getHeight() * 0.6; z = le.getZ(); }
        spawn(x, y, z);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) { for (Burst b : bursts) b.active = false; return; }
        if (mc.isPaused()) return;
        ticks++;
        for (Burst b : bursts) if (b.active && ticks - b.start > b.dur + 2) b.active = false;
    }

    private void spawn(double x, double y, double z) {
        Burst b = null;
        for (Burst c : bursts) if (!c.active) { b = c; break; }
        if (b == null) { b = bursts[0]; for (Burst c : bursts) if (c.start < b.start) b = c; }
        int extra = combo.get() ? Math.min(comboCount - 1, 6) : 0;
        b.active = true;
        b.seed = ++counter * 7919 + 17;
        b.n = Math.max(1, Math.min(MAX_BUBBLES, count.intValue() + extra));
        b.x = x; b.y = y; b.z = z;
        b.start = ticks;
        b.dur = duration.get() * 20.0;
        b.scale = size.get() * (1.0 + (combo.get() ? Math.min(comboCount - 1, 10) * 0.06 : 0.0));
    }

    @Override
    public void onWorldRender() {
        boolean any = false;
        for (Burst b : bursts) if (b.active) { any = true; break; }
        if (!any) return;
        double time = ticks + Gfx.partial;
        if (colorMode.is("Auto")) pal.set("Gradient", 0x6FE3FF, 0xB48CFF, 1.0, time);
        else pal.set(colorMode.get(), c1.rgb(), c2.rgb(), flow.get(), time);
        float maxA = opacity.floatValue() / 100f;
        double riseM = rise.get(), spreadM = spread.get();
        Gfx.use(glow.get(), false);
        for (Burst b : bursts) {
            if (!b.active || Gfx.far(b.x, b.y, b.z)) continue;
            double age = ticks - b.start + Gfx.partial;
            double t = age / b.dur;
            if (t >= 1.0) continue;
            for (int i = 0; i < b.n; i++) {
                float delay = Hash.f(b.seed, i * 6 + 4) * 0.25f;
                float tt = (float) ((t - delay) / (1.0 - delay));
                if (tt <= 0f || tt >= 1f) continue;
                float e = Ease.outCubic(tt);
                float lift = 0.25f + 0.75f * Hash.f(b.seed, i * 6 + 3);
                double sway = Math.sin(age * 0.25 + i * 1.7) * 0.08 * tt;
                double cx = b.x + Hash.s(b.seed, i * 6 + 1) * spreadM * e + sway;
                double cy = b.y + riseM * lift * e;
                double cz = b.z + Hash.s(b.seed, i * 6 + 2) * spreadM * e - sway;
                double r = 0.07 * b.scale * (0.6 + 0.8 * Hash.f(b.seed, i * 6 + 5));
                if (tt < 0.15f) r *= Ease.outBack(tt / 0.15f);                 // pop in
                else if (tt > 0.85f) r *= 1.0 + (tt - 0.85f) / 0.15f * 0.35;   // swell before the pop
                float fadeIn = Math.min(1f, tt / 0.12f);
                float fadeOut = tt > 0.8f ? 1f - (tt - 0.8f) / 0.2f : 1f;
                float a = maxA * fadeIn * fadeOut;
                if (a <= 0.01f) continue;
                int rgb = pal.at(Hash.f(b.seed, i * 6 + 6));
                int top = ColorUtil.withAlpha(rgb, Math.round(255f * a * 0.6f));
                int bot = ColorUtil.withAlpha(rgb, Math.round(255f * a * 0.25f));
                Gfx.sphere(cx, cy, cz, r, r, r, 8, 12, top, bot, 0.06, age * 0.2);
                if (glint.get()) {
                    double gr = r * 0.22;
                    double gx = cx + (-Gfx.rightX * 0.38 + Gfx.upX * 0.42) * r;
                    double gy = cy + (-Gfx.rightY * 0.38 + Gfx.upY * 0.42) * r;
                    double gz = cz + (-Gfx.rightZ * 0.38 + Gfx.upZ * 0.42) * r;
                    int w = ColorUtil.withAlpha(0xFFFFFFFF, Math.round(255f * a * 0.9f));
                    Gfx.sphere(gx, gy, gz, gr, gr, gr, 4, 6, w, w, 0.0, 0.0);
                }
            }
        }
    }
}
