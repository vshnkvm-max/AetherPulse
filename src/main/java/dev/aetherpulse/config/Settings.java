package dev.aetherpulse.config;

import dev.aetherpulse.progress.ProgressCurve;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.EnumMap;
import java.util.Map;

/** Immutable snapshot: reload validates everything before replacing live settings. */
public record Settings(int animationInterval, int particleInterval, double activationRadius, int particleBudget,
                       int entityLimit, float coreSize, int segments, int fragments, double[] radii,
                       double[] speeds, boolean particles, double intensity, Color dustColor,
                       boolean spiral, boolean streams, boolean bursts, boolean sounds,
                       ProgressCurve curve, boolean blockProgress, double blockRadius,
                       Map<Material, Long> blockPoints, int cooldownTicks) {
    public Settings { radii = radii.clone(); speeds = speeds.clone(); blockPoints = Map.copyOf(blockPoints); }
    @Override public double[] radii() { return radii.clone(); }
    @Override public double[] speeds() { return speeds.clone(); }
    public static Settings read(FileConfiguration c) {
        int segments = integer(c, "visual.segments", 20, 8, 48);
        int fragments = integer(c, "visual.fragments", 6, 0, 16);
        int limit = integer(c, "performance.max-display-entities", 100, 8, 256);
        if (4 + 3 * segments + fragments + 1 > limit) throw new IllegalArgumentException("max-display-entities must cover 5 + 3 * segments + fragments");
        String[] names = {"outer", "middle", "inner"};
        double[] radii = new double[3], speeds = new double[3];
        for (int i = 0; i < 3; i++) {
            radii[i] = number(c, "visual.rings." + names[i] + ".radius", 2.4 - i * .55, .4, 6);
            speeds[i] = number(c, "visual.rings." + names[i] + ".speed", .2 + i * .2, -5, 5);
        }
        var list = c.getLongList("progress.thresholds");
        ProgressCurve curve = new ProgressCurve(list.stream().mapToLong(Long::longValue).toArray());
        EnumMap<Material, Long> points = new EnumMap<>(Material.class);
        var section = c.getConfigurationSection("progress.blocks.points");
        if (section != null) for (String name : section.getKeys(false)) {
            Material material = Material.matchMaterial(name);
            long value = section.getLong(name);
            if (material == null || !material.isBlock() || value <= 0) throw new IllegalArgumentException("Invalid block points: " + name);
            points.put(material, value);
        }
        Color color;
        try { color = Color.fromRGB(Integer.parseInt(c.getString("visual.particles.dust-color", "#A967FF").replace("#", ""), 16)); }
        catch (RuntimeException e) { throw new IllegalArgumentException("Invalid dust-color", e); }
        return new Settings(integer(c, "performance.animation-update-interval", 2, 1, 20),
                integer(c, "performance.particle-update-interval", 4, 1, 100),
                number(c, "performance.activation-radius", 48, 4, 128),
                integer(c, "performance.max-particles-per-cycle", 100, 0, 2000), limit,
                (float) number(c, "visual.core-size", .85, .1, 3), segments, fragments, radii, speeds,
                c.getBoolean("visual.particles.enabled", true), number(c, "visual.particles.intensity", 1, 0, 3), color,
                c.getBoolean("visual.particles.spiral", true), c.getBoolean("visual.particles.streams", true),
                c.getBoolean("visual.particles.bursts", true), c.getBoolean("visual.sounds", true), curve,
                c.getBoolean("progress.blocks.enabled", true), number(c, "progress.blocks.radius", 32, 1, 128), points,
                integer(c, "interaction.cooldown-ticks", 30, 1, 1200));
    }
    private static int integer(FileConfiguration c, String key, int fallback, int min, int max) {
        int v = c.getInt(key, fallback);
        if (v < min || v > max) throw new IllegalArgumentException(key + " must be " + min + "–" + max);
        return v;
    }
    private static double number(FileConfiguration c, String key, double fallback, double min, double max) {
        double v = c.getDouble(key, fallback);
        if (!Double.isFinite(v) || v < min || v > max) throw new IllegalArgumentException(key + " must be " + min + "–" + max);
        return v;
    }
}
