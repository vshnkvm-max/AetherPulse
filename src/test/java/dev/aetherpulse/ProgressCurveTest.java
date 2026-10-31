package dev.aetherpulse;

import dev.aetherpulse.monument.MonumentStage;
import dev.aetherpulse.progress.ProgressCurve;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class ProgressCurveTest {
    private final ProgressCurve curve = new ProgressCurve(new long[]{0, 100, 400, 1000, 2500});
    @ParameterizedTest @CsvSource({"0,1", "99,1", "100,2", "399,2", "400,3", "999,3", "1000,4", "2499,4", "2500,5", "9223372036854775807,5"})
    void exactBoundaries(long points, int stage) { assertEquals(stage, curve.stage(points).number()); }
    @Test void remainderAndFraction() {
        assertEquals(300, curve.remaining(100)); assertEquals(.5, curve.fraction(250), 1e-9);
        assertEquals(0, curve.remaining(Long.MAX_VALUE)); assertEquals(1, curve.fraction(Long.MAX_VALUE));
    }
    @Test void saturation() {
        assertEquals(Long.MAX_VALUE, ProgressCurve.add(Long.MAX_VALUE - 1, 10));
        assertEquals(150, ProgressCurve.add(100, 50));
        assertThrows(IllegalArgumentException.class, () -> ProgressCurve.add(1, -1));
    }
    @Test void rejectsInvalidThresholds() {
        assertThrows(IllegalArgumentException.class, () -> new ProgressCurve(new long[]{0, 1}));
        assertThrows(IllegalArgumentException.class, () -> new ProgressCurve(new long[]{1, 2, 3, 4, 5}));
        assertThrows(IllegalArgumentException.class, () -> new ProgressCurve(new long[]{0, 1, 1, 4, 5}));
    }
    @Test void defensiveCopy() {
        long[] values = {0, 1, 2, 3, 4}; ProgressCurve c = new ProgressCurve(values); values[4] = 999;
        assertEquals(MonumentStage.ASCENDED, c.stage(4));
    }
    @Test void stageValidation() {
        assertThrows(IllegalArgumentException.class, () -> MonumentStage.fromNumber(0));
        assertEquals(3, MonumentStage.ASCENDED.rings());
    }
}
