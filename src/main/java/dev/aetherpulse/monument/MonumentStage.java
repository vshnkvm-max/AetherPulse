package dev.aetherpulse.monument;

/** The five visual states; thresholds are configured separately. */
public enum MonumentStage {
    DORMANT("Dormant", 0), AWAKENING("Awakening", 1), ENERGIZED("Energized", 2),
    UNSTABLE("Unstable", 3), ASCENDED("Ascended", 3);
    private final String title;
    private final int rings;
    MonumentStage(String title, int rings) { this.title = title; this.rings = rings; }
    public String title() { return title; }
    public int number() { return ordinal() + 1; }
    public int rings() { return rings; }
    public static MonumentStage fromNumber(int n) {
        if (n < 1 || n > 5) throw new IllegalArgumentException("Stage must be 1–5");
        return values()[n - 1];
    }
}
