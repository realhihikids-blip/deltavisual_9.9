package dev.deltavisuals.feature;

import dev.deltavisuals.core.Category;
import dev.deltavisuals.core.Module;
import dev.deltavisuals.core.Setting;
import dev.deltavisuals.render.Gfx;
import dev.deltavisuals.util.ColorUtil;
import dev.deltavisuals.util.Palette;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

/**
 * Eat Effect: when you finish eating (food, chorus fruit and, optionally, drinking) glowing rings rise around your
 * body. A chorus fruit gets a bigger effect, also at the place you teleport to. Solid flat geometry through
 * {@link Gfx}, no particles, only on your screen. Nothing is sent anywhere.
 */
public final class EatEffect extends Module {
    private static final double TAU = Math.PI * 2;
    private static final int MAX = 6;

    private static final class Fx {
        boolean active, follow;
        int start;
        double x, y, z, scale;
    }

    private final Setting.Bool food, chorus, drinks, glow, firstPerson;
    private final Setting.Num duration, size, rings, opacity, flow;
    private final Setting.Mode colorMode;
    private final Setting.Color c1, c2;

    private final Fx[] list = new Fx[MAX];
    private final Palette pal = new Palette();
    private int ticks;

    // item use tracking
    private boolean wasUsing, wasChorus;
    private int lastLeft = 99;
    private boolean lastEdible, lastDrink;
    private int chorusWatch;
    private double chorusX, chorusY, chorusZ;

    public EatEffect() {
        super("Eat Effect", "Rings rise around you when you finish eating: food, chorus fruit and drinks",
            Category.VISUALS, false);
        food = add(new Setting.Bool("Food", true));
        chorus = add(new Setting.Bool("Chorus Fruit (Bigger)", true));
        drinks = add(new Setting.Bool("Drinks", false));
        duration = add(new Setting.Num("Duration", 0.4, 2.0, 0.05, 1.0, " s"));
        size = add(new Setting.Num("Size", 0.5, 2.5, 0.05, 1.0));
        rings = add(new Setting.Num("Rings", 1, 4, 1, 3));
        opacity = add(new Setting.Num("Opacity", 10, 100, 5, 75, "%"));
        colorMode = add(new Setting.Mode("Color Mode", Palette.PLAIN, 1));
        c1 = add(new Setting.Color("Main Color", 0xFF7CFF8A, false));
        c2 = add(new Setting.Color("Second Color", 0xFFFFE066, false));
        c1.visibleIf(() -> !colorMode.is("Rainbow") && !colorMode.is("Fire"));
        c2.visibleIf(() -> colorMode.is("Gradient") || colorMode.is("Neon"));
        flow = add(new Setting.Num("Color Flow Speed", 0.2, 5, 0.1, 1, "x"));
        flow.visibleIf(() -> !colorMode.is("Static"));
        glow = add(new Setting.Bool("Glow", true));
        firstPerson = add(new Setting.Bool("Show In First Person", true));
        for (int i = 0; i < MAX; i++) list[i] = new Fx();
    }

    @Override public void onDisable() { for (Fx f : list) f.active = false; wasUsing = false; chorusWatch = 0; }

    @Override
    public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null) { wasUsing = false; return; }
        if (mc.isPaused()) return;
        ticks++;

        boolean using = p.isUsingItem();
        if (using) {
            ItemStack s = p.getActiveItem();
            String a = String.valueOf(s.getUseAction());
            lastEdible = a.equals("EAT");
            lastDrink = a.equals("DRINK");
            wasChorus = s.isOf(Items.CHORUS_FRUIT);
            lastLeft = p.getItemUseTimeLeft();
        } else if (wasUsing && lastLeft <= 2) {            // the use ran to its end: the item was really consumed
            boolean ok = wasChorus ? chorus.get() : (lastEdible && food.get()) || (lastDrink && drinks.get());
            if (ok) {
                spawn(p.getX(), p.getY(), p.getZ(), wasChorus ? 1.5 : 1.0, true);
                if (wasChorus) { chorusWatch = 6; chorusX = p.getX(); chorusY = p.getY(); chorusZ = p.getZ(); }
            }
        }
        wasUsing = using;

        if (chorusWatch > 0) {                              // the fruit teleported you: show the effect there too
            chorusWatch--;
            double dx = p.getX() - chorusX, dy = p.getY() - chorusY, dz = p.getZ() - chorusZ;
            if (dx * dx + dy * dy + dz * dz > 4.0) {
                spawn(p.getX(), p.getY(), p.getZ(), 1.5, true);
                chorusWatch = 0;
            }
        }
    }

    private void spawn(double x, double y, double z, double scale, boolean follow) {
        Fx f = null;
        for (Fx i : list) if (!i.active) { f = i; break; }
        if (f == null) f = list[0];
        f.active = true; f.start = ticks; f.x = x; f.y = y; f.z = z; f.scale = scale; f.follow = follow;
    }

    @Override
    public void onWorldRender() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (!firstPerson.get() && mc.options.getPerspective().isFirstPerson()) return;
        double durTicks = duration.get() * 20.0;
        int n = rings.intValue();
        float maxA = opacity.floatValue() / 100f;
        pal.set(colorMode.get(), c1.rgb(), c2.rgb(), flow.get(), ticks + Gfx.partial);
        boolean used = false;
        for (Fx f : list) {
            if (!f.active) continue;
            double age = ticks - f.start + Gfx.partial;
            if (age >= durTicks + n * 3.0) { f.active = false; continue; }
            if (!used) { Gfx.use(glow.get(), false); used = true; }
            double bx = f.x, by = f.y, bz = f.z;
            if (f.follow) {
                Vec3d lp = mc.player.getLerpedPos(Gfx.partial);
                // the effect stays on the player, except right after a chorus teleport where it was spawned
                if (Math.abs(lp.x - f.x) < 3 && Math.abs(lp.z - f.z) < 3) { bx = lp.x; by = lp.y; bz = lp.z; }
            }
            for (int k = 0; k < n; k++) {
                double t = (age - k * 3.0) / durTicks;
                if (t <= 0 || t >= 1) continue;
                double rise = 1.0 - Math.pow(1.0 - t, 2.0);
                double y = by + 0.05 + rise * 1.9 * size.get();                 // from the feet up over the head
                double rad = (0.55 + 0.25 * Math.sin(t * Math.PI)) * size.get() * f.scale;
                double w = 0.07 * size.get() * (1.0 - 0.5 * t);
                float fade = (float) Math.sin(Math.min(1.0, t) * Math.PI) * maxA;
                int edge = pal.at(k * 0.3 + t * 0.4, fade);
                Gfx.ringFlat(bx, y, bz, Math.max(0.0, rad - w), rad, 0.0, TAU, 40,
                    ColorUtil.withAlpha(edge, Math.round(255f * fade * 0.3f)), edge);
                // a soft wall under the ring so it reads as a rising glow
                Gfx.band(bx, y, bz, rad, -0.18 * size.get(), 0.0, 24,
                    ColorUtil.withAlpha(edge, 0), ColorUtil.withAlpha(edge, Math.round(255f * fade * 0.35f)));
            }
        }
    }
}
