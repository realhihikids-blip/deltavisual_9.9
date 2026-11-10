package dev.deltavisuals.util;

/** Shared easing helpers for animations (UI, HUD and world effects). Pure functions, no allocation. */
public final class Ease {
    private Ease() {}

    public static float clamp01(float t) { return t < 0f ? 0f : Math.min(t, 1f); }

    public static float smooth(float t) { t = clamp01(t); return t * t * (3f - 2f * t); }
    public static float outCubic(float t) { t = clamp01(t); float u = 1f - t; return 1f - u * u * u; }
    public static float inCubic(float t) { t = clamp01(t); return t * t * t; }
    public static float inOutCubic(float t) {
        t = clamp01(t);
        return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
    }
    /** Slight overshoot, good for pop-in animations. */
    public static float outBack(float t) {
        t = clamp01(t);
        float c1 = 1.70158f, c3 = c1 + 1f, u = t - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    }

    /** Frame-rate independent exponential approach of cur towards target. */
    public static float approach(float cur, float target, float dt, float speed) {
        return cur + (target - cur) * Math.min(1f, dt * speed);
    }
}
