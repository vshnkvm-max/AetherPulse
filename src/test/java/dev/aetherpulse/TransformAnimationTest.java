package dev.aetherpulse;

import dev.aetherpulse.animation.*;
import dev.aetherpulse.visual.Transform;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TransformAnimationTest {
    @Test void nestedCoordinates() {
        Transform parent = new Transform(); parent.position().set(3, 0, 0); parent.rotation().rotateZ((float) (Math.PI / 2)); parent.scale().set(2);
        Transform child = new Transform().parent(parent); child.position().set(1, 0, 0);
        Vector3f world = child.worldMatrix().transformPosition(new Vector3f());
        assertEquals(3, world.x, 1e-5); assertEquals(2, world.y, 1e-5);
    }
    @Test void rejectsIndirectCycles() {
        Transform a = new Transform(), b = new Transform().parent(a), c = new Transform().parent(b);
        assertThrows(IllegalArgumentException.class, () -> a.parent(c));
        assertThrows(IllegalArgumentException.class, () -> a.parent(a));
    }
    @Test void quaternionInterpolationAndClamping() {
        Transform a = new Transform(), b = new Transform(); b.position().set(10, 0, 0); b.rotation().rotateY((float) Math.PI / 2);
        Transform mid = a.interpolate(b, .5f);
        assertEquals(5, mid.position().x, 1e-5);
        assertEquals(1, mid.rotation().lengthSquared(), 1e-5);
        assertEquals(10, a.interpolate(b, 2).position().x);
    }
    @ParameterizedTest @EnumSource(Easing.class) void easingEndpointsAndMonotonicity(Easing easing) {
        assertEquals(0, easing.apply(-1)); assertEquals(1, easing.apply(2));
        double previous = 0;
        for (int i = 0; i <= 100; i++) { double v = easing.apply(i / 100.0); assertTrue(v >= previous); previous = v; }
    }
    @Test void compositeTracksDoNotEraseEachOther() {
        Transform t = new Transform();
        Animation.together(Animations.rotation(new Vector3f(0, 1, 0), 1), Animations.sine(.2, 1), Animations.pulse(.1, 1)).apply(t, Math.PI / 2);
        assertEquals(.2, t.position().y, 1e-5); assertEquals(1.1, t.scale().x, 1e-5);
        Vector3f rotated = t.rotation().transform(new Vector3f(1, 0, 0)); assertEquals(-1, rotated.z, 1e-5);
    }
    @Test void orbitRadiusAndPhase() {
        Transform t = new Transform(); Animations.orbit(2, 1, 0, .5).apply(t, Math.PI / 2);
        assertEquals(0, t.position().x, 1e-5); assertEquals(2, t.position().z, 1e-5);
    }
    @Test void appearanceAndSequence() {
        Transform hidden = new Transform(); Animations.appearance(2).apply(hidden, 0); assertEquals(0, hidden.scale().x);
        Transform shown = new Transform(); Animations.appearance(2).apply(shown, 2); assertEquals(1, shown.scale().x);
        AnimationSequence seq = new AnimationSequence(List.of(new AnimationSequence.Clip(1, (t, s) -> t.position().x = (float) s),
                new AnimationSequence.Clip(2, (t, s) -> t.position().y = (float) s)));
        Transform t = new Transform(); seq.apply(t, 1.5); assertEquals(.5, t.position().y);
        t = new Transform(); seq.apply(t, 50); assertEquals(2, t.position().y);
        assertThrows(IllegalArgumentException.class, () -> new AnimationSequence(List.of()));
    }
}
