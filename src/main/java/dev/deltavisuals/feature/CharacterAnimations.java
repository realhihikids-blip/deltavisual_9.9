package dev.deltavisuals.feature;

import dev.deltavisuals.core.Category;
import dev.deltavisuals.core.Module;
import dev.deltavisuals.core.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;

/**
 * Character Animations: short emotes for your own character, seen in third person (and by nobody else).
 * The body (and the head with it) turns on your screen only: the look direction, the movement and everything the
 * server knows stay exactly as they are, nothing is sent anywhere.
 *
 * Emotes: Spin, Fast Spin and Sway. Press the key of the module to play one; "Spin On Jump" plays a spin
 * automatically when you jump.
 */
public final class CharacterAnimations extends Module {
    public static final String[] EMOTES = {"Spin", "Fast Spin", "Sway", "Double Spin"};

    private final Setting.Mode emote, direction;
    private final Setting.Num speed;
    private final Setting.Bool onJump, stopOnMove;

    private boolean playing, wasGround = true;
    private int tick, length, kind;
    private float startYaw, dir = 1f;

    public CharacterAnimations() {
        super("Character Animations", "Emotes for your character in third person: spin, fast spin, sway. Only on your screen",
            Category.PLAYER, false);
        action = true;                 // the key plays the emote, it does not switch the module off
        emote = add(new Setting.Mode("Emote", EMOTES, 0));
        direction = add(new Setting.Mode("Direction", new String[]{"Right", "Left"}, 0));
        speed = add(new Setting.Num("Speed", 0.5, 2.5, 0.1, 1.0, "x"));
        onJump = add(new Setting.Bool("Spin On Jump", false));
        stopOnMove = add(new Setting.Bool("Stop When Moving", false));
        add(new Setting.Info("Note", () -> "Press the keybind of this module to play the emote. Only you see it"));
    }

    @Override
    public void onBind(MinecraftClient mc) { play(mc.player, emote.index()); }

    private void play(ClientPlayerEntity p, int which) {
        if (p == null) return;
        kind = which;
        float base = switch (which) { case 1 -> 14f; case 3 -> 24f; case 2 -> 28f; default -> 20f; };
        length = Math.max(6, Math.round(base / speed.floatValue()));
        tick = 0;
        startYaw = p.bodyYaw;
        dir = direction.index() == 0 ? 1f : -1f;
        playing = true;
    }

    @Override public void onDisable() { playing = false; }

    @Override
    public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) { playing = false; return; }
        boolean ground = p.isOnGround();
        if (onJump.get() && wasGround && !ground && p.getVelocity().y > 0.1 && !playing) play(p, 0);
        wasGround = ground;
        if (!playing || mc.isPaused()) return;
        if (stopOnMove.get() && Math.hypot(p.getVelocity().x, p.getVelocity().z) > 0.08) {
            finish(p);
            return;
        }

        float t = (float) tick / length;
        float angle;
        if (kind == 2) {                                   // sway: left-right wobble that fades out
            angle = (float) (Math.sin(t * Math.PI * 4.0) * 40.0 * (1.0 - t));
        } else {
            float turns = kind == 3 ? 2f : 1f;
            float e = t < 0.5f ? 2f * t * t : 1f - (float) Math.pow(-2f * t + 2f, 2) / 2f;   // slow start and end
            angle = 360f * turns * e * dir;
        }
        float yaw = startYaw + angle;
     //   p.prevBodyYaw = p.bodyYaw;                         // the render interpolates between these two values
          p.bodyYaw = yaw;
        //  p.prevHeadYaw = p.headYaw;
         p.headYaw = yaw;
        if (++tick > length) finish(p);
    }

    private void finish(ClientPlayerEntity p) {
        playing = false;
        float y = MathHelper.wrapDegrees(p.getYaw());
        p.bodyYaw = y; p.headYaw = y;
    }
}
