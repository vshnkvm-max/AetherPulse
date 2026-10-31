package dev.aetherpulse.storage;

import java.util.UUID;

/** Bukkit-free persistent snapshot. Stage override zero means automatic progression. */
public record MonumentRecord(String id, UUID world, double x, double y, double z, long points,
                             int stage, int stageOverride, long revision) {
    public MonumentRecord {
        if (!id.matches("[a-zA-Z0-9_-]{1,32}")) throw new IllegalArgumentException("Invalid monument ID");
        if (world == null || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || Math.abs(x) > 30_000_000 || Math.abs(z) > 30_000_000 || Math.abs(y) > 4096
                || points < 0 || stage < 1 || stage > 5 || stageOverride < 0 || stageOverride > 5 || revision < 0)
            throw new IllegalArgumentException("Invalid monument state");
    }
}
