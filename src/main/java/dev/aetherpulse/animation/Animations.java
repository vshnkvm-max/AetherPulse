package dev.aetherpulse.animation;

import org.joml.Vector3f;

/** Reusable rotations, orbit, sine, pulse, scale and scale-based appearance tracks. */
public final class Animations {
    private Animations() { }
    public static Animation rotation(Vector3f axis, double radiansPerSecond) {
        Vector3f unit = new Vector3f(axis);
        if (unit.lengthSquared() < 1e-8) throw new IllegalArgumentException("Zero axis");
        unit.normalize();
        return (t, s) -> t.rotation().rotateAxis((float) (s * radiansPerSecond), unit.x, unit.y, unit.z);
    }
    public static Animation orbit(double radius, double speed, double phase, double height) {
        return (t, s) -> t.position().add((float) (radius * Math.cos(s * speed + phase)),
                (float) (height * Math.sin(s * speed * 1.3 + phase)), (float) (radius * Math.sin(s * speed + phase)));
    }
    public static Animation sine(double amplitude, double frequency) {
        return (t, s) -> t.position().y += (float) (amplitude * Math.sin(s * frequency));
    }
    public static Animation pulse(double amplitude, double frequency) {
        return (t, s) -> t.scale().mul((float) (1 + amplitude * Math.sin(s * frequency)));
    }
    public static Animation scale(Vector3f from, Vector3f to, double duration, Easing easing) {
        if (!(duration > 0)) throw new IllegalArgumentException("Positive duration required");
        Vector3f a = new Vector3f(from), b = new Vector3f(to);
        return (t, s) -> t.scale().mul(a.lerp(b, (float) easing.apply(s / duration), new Vector3f()));
    }
    /** Blocks have no alpha; use a smooth zero-to-full scale reveal. */
    public static Animation appearance(double duration) { return scale(new Vector3f(), new Vector3f(1), duration, Easing.SMOOTHSTEP); }
}
