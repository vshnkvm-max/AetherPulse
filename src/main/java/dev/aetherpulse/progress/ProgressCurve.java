package dev.aetherpulse.progress;

import dev.aetherpulse.monument.MonumentStage;

/** Immutable validated progression curve. All point arithmetic is saturating. */
public final class ProgressCurve {
    private final long[] thresholds;
    public ProgressCurve(long[] thresholds) {
        if (thresholds.length != 5 || thresholds[0] != 0) throw new IllegalArgumentException("Five thresholds starting at 0 required");
        for (int i = 1; i < 5; i++) if (thresholds[i] <= thresholds[i - 1]) throw new IllegalArgumentException("Thresholds must strictly increase");
        this.thresholds = thresholds.clone();
    }
    public MonumentStage stage(long points) {
        for (int i = 4; i > 0; i--) if (points >= thresholds[i]) return MonumentStage.values()[i];
        return MonumentStage.DORMANT;
    }
    public long threshold(MonumentStage stage) { return thresholds[stage.ordinal()]; }
    public long remaining(long points) {
        MonumentStage s = stage(points);
        return s == MonumentStage.ASCENDED ? 0 : thresholds[s.ordinal() + 1] - Math.max(0, points);
    }
    public double fraction(long points) {
        MonumentStage s = stage(points);
        if (s == MonumentStage.ASCENDED) return 1;
        long lo = threshold(s), hi = thresholds[s.ordinal() + 1];
        return Math.clamp((double) (Math.max(0, points) - lo) / (hi - lo), 0, 1);
    }
    public static long add(long current, long amount) {
        if (current < 0 || amount < 0) throw new IllegalArgumentException("Points must be nonnegative");
        return current > Long.MAX_VALUE - amount ? Long.MAX_VALUE : current + amount;
    }
}
