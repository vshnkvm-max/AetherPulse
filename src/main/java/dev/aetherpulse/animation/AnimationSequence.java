package dev.aetherpulse.animation;

import dev.aetherpulse.visual.Transform;
import java.util.List;

/** Sequential clips; each clip receives elapsed time local to that clip. */
public final class AnimationSequence implements Animation {
    public record Clip(double duration, Animation animation) {
        public Clip { if (!(duration > 0) || !Double.isFinite(duration) || animation == null) throw new IllegalArgumentException("Invalid clip"); }
    }
    private final List<Clip> clips;
    private final double duration;
    public AnimationSequence(List<Clip> clips) {
        if (clips.isEmpty()) throw new IllegalArgumentException("Empty sequence");
        this.clips = List.copyOf(clips);
        duration = clips.stream().mapToDouble(Clip::duration).sum();
    }
    public double duration() { return duration; }
    @Override public void apply(Transform target, double seconds) {
        double time = Math.clamp(seconds, 0, duration);
        for (int i = 0; i < clips.size(); i++) {
            Clip c = clips.get(i);
            if (time < c.duration || i == clips.size() - 1) { c.animation.apply(target, time); return; }
            time -= c.duration;
        }
    }
}
