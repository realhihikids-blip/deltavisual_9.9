package dev.deltavisuals.feature;

import dev.deltavisuals.core.Category;
import dev.deltavisuals.core.Module;
import dev.deltavisuals.core.Setting;
import dev.deltavisuals.render.Gfx;
import dev.deltavisuals.util.ColorUtil;
import dev.deltavisuals.util.Palette;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * China Hat: a flat cone hat above the head with a colour gradient that flows around it and a bright rim ring.
 * Solid geometry through {@link Gfx}, no textures and no particles. Detail follows the FPS Boost level of detail.
 */
public final class ChinaHat extends Module {
    private static final double TAU = Math.PI * 2;

    private final Setting.Num size, height, lift, opacity, flow, pulseAmount;
    private final Setting.Bool rim, glow, firstPerson, others, pulse;
    private final Setting.Mode colorMode;
    private final Setting.Color c1, c2;
    private final Palette pal = new Palette();
    private int ticks;

    public ChinaHat() {
        super("China Hat", "A cone hat with a flowing colour gradient and a glowing rim above your head. Only visual",
            Category.VISUALS, false);
        add(new Setting.Header("Shape"));
        size = add(new Setting.Num("Hat Size", 0.3, 1.2, 0.01, 0.62, " m"));
        height = add(new Setting.Num("Hat Height", 0.1, 0.8, 0.01, 0.3, " m"));
        lift = add(new Setting.Num("Hat Lift", -0.1, 0.4, 0.01, 0.02, " m"));
        rim = add(new Setting.Bool("Rim Ring", true));
        add(new Setting.Header("Color"));
        colorMode = add(new Setting.Mode("Color Mode", Palette.PLAIN, 1));
        c1 = add(new Setting.Color("Main Color", 0xFFFF4DA6, false));
        c2 = add(new Setting.Color("Second Color", 0xFF4D9CFF, false));
        c1.visibleIf(() -> !colorMode.is("Rainbow") && !colorMode.is("Fire"));
        c2.visibleIf(() -> colorMode.is("Gradient") || colorMode.is("Neon"));
        flow = add(new Setting.Num("Color Flow Speed", 0.2, 5, 0.1, 1, "x"));
        flow.visibleIf(() -> !colorMode.is("Static"));
        opacity = add(new Setting.Num("Opacity", 10, 100, 5, 70, "%"));
        glow = add(new Setting.Bool("Glow", true));
        add(new Setting.Header("Animation"));
        pulse = add(new Setting.Bool("Pulse", true));
        pulseAmount = add(new Setting.Num("Pulse Amount", 1, 15, 1, 4, "%"));
        pulseAmount.visibleIf(pulse::get);
        add(new Setting.Header("Visibility"));
        firstPerson = add(new Setting.Bool("Show In First Person", false));
        others = add(new Setting.Bool("Other Players", false));
    }

    @Override
    public void onTick(MinecraftClient mc) { if (!mc.isPaused()) ticks++; }

    @Override
    public void onWorldRender() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        double time = ticks + Gfx.partial;
        pal.set(colorMode.get(), c1.rgb(), c2.rgb(), flow.get(), time);
        Gfx.use(glow.get(), false);
        if (!mc.options.getPerspective().isFirstPerson() || firstPerson.get()) draw(mc.player, time);
        if (others.get()) {
            for (PlayerEntity pl : mc.world.getPlayers()) {
                if (pl == mc.player || pl.isInvisible() || Gfx.far(pl.getX(), pl.getY(), pl.getZ())) continue;
                draw(pl, time);
            }
        }
    }

    private void draw(PlayerEntity p, double time) {
        Vec3d pos = p.getLerpedPos(Gfx.partial);
        double baseY = pos.y + p.getHeight() + lift.get();
        double apexY = baseY + height.get();
        double r = size.get() * (pulse.get() ? 1.0 + pulseAmount.get() / 100.0 * Math.sin(time * 0.12) : 1.0);
        int segs = Math.max(10, Math.min(64, Math.round(32f * Gfx.lod)));
        if (!Gfx.hasBudget(segs * 2)) return;
        float a = opacity.floatValue() / 100f;
        float rimA = Math.min(1f, a * 1.4f);

        double ax = pos.x, az = pos.z;
        double cos0 = 1.0, sin0 = 0.0;
        int edge0 = pal.at(0.0, a);
        for (int i = 0; i < segs; i++) {
            double ang1 = TAU * (i + 1) / segs;
            double cos1 = Math.cos(ang1), sin1 = Math.sin(ang1);
            int edge1 = pal.at((i + 1) / (double) segs, a);
            int apex = ColorUtil.withAlpha(ColorUtil.lerp(edge0, 0xFFFFFFFF, 0.35f), Math.round(255f * a * 0.85f));
            double x0 = ax + cos0 * r, z0 = az + sin0 * r, x1 = ax + cos1 * r, z1 = az + sin1 * r;
            Gfx.quad(x0, baseY, z0, edge0, x1, baseY, z1, edge1, ax, apexY, az, apex, ax, apexY, az, apex);
            if (rim.get()) {
                double ri = r - 0.05, ro = r + 0.015;
                int in0 = pal.at(i / (double) segs, rimA), in1 = pal.at((i + 1) / (double) segs, rimA);
                Gfx.quad(ax + cos0 * ri, baseY + 0.004, az + sin0 * ri, in0, ax + cos0 * ro, baseY + 0.004, az + sin0 * ro, in0,
                         ax + cos1 * ro, baseY + 0.004, az + sin1 * ro, in1, ax + cos1 * ri, baseY + 0.004, az + sin1 * ri, in1);
            }
            cos0 = cos1; sin0 = sin1; edge0 = edge1;
        }
    }
}
