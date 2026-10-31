package dev.aetherpulse.animation;

/** Normalized interpolation curves, clamped to [0, 1]. */
public enum Easing {
    LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT, SMOOTHSTEP;
    public double apply(double value) {
        double t = Math.clamp(value, 0, 1);
        return switch (this) {
            case LINEAR -> t;
            case EASE_IN -> t * t;
            case EASE_OUT -> 1 - (1 - t) * (1 - t);
            case EASE_IN_OUT -> t < .5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
            case SMOOTHSTEP -> t * t * (3 - 2 * t);
        };
    }
}
