package dev.aetherpulse.animation;

import dev.aetherpulse.visual.Transform;
import java.util.List;

/** Composable, absolute-time animation; targets are reset by the caller before evaluation. */
@FunctionalInterface
public interface Animation {
    void apply(Transform target, double seconds);
    static Animation together(Animation... animations) {
        List<Animation> parts = List.of(animations);
        return (target, seconds) -> parts.forEach(a -> a.apply(target, seconds));
    }
}
