package dev.aetherpulse.progress;

import dev.aetherpulse.monument.*;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;

/** Public main-thread integration API plus the built-in nearest-monument block provider. */
public final class ProgressManager implements ProgressProvider, Listener {
    private final MonumentManager monuments;
    public ProgressManager(MonumentManager monuments) { this.monuments = monuments; }
    @Override public String id() { return "blocks"; }
    @Override public void award(Player player, String monumentId, long points) { add(require(monumentId), points); }
    private Monument require(String id) {
        if (!monuments.ready()) throw new IllegalStateException("Storage is still loading");
        Monument m = monuments.find(id);
        if (m == null) throw new IllegalArgumentException("Unknown monument: " + id);
        return m;
    }
    public void add(Monument monument, long amount) { set(monument, ProgressCurve.add(monument.points(), amount)); }
    public void set(Monument monument, long points) {
        mainThread();
        if (points < 0) throw new IllegalArgumentException("Points must be nonnegative");
        monument.points(points, monuments.settings().curve()); monuments.changed(monument);
    }
    public void stage(Monument monument, int stage) {
        mainThread(); MonumentStage.fromNumber(stage);
        monument.override(stage, monuments.settings().curve()); monuments.changed(monument);
    }
    private void mainThread() { if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Progress API must run on the server thread"); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void blockBreak(BlockBreakEvent event) {
        var settings = monuments.settings();
        if (!monuments.ready() || !settings.blockProgress() || event.getPlayer().getGameMode() != GameMode.SURVIVAL) return;
        Long points = settings.blockPoints().get(event.getBlock().getType());
        if (points == null) return;
        Location location = event.getBlock().getLocation();
        Monument nearest = null;
        double distance = settings.blockRadius() * settings.blockRadius();
        for (Monument m : monuments.all()) {
            if (!m.worldId().equals(location.getWorld().getUID())) continue;
            double d = m.location().distanceSquared(location);
            if (d <= distance) { nearest = m; distance = d; }
        }
        if (nearest != null) add(nearest, points);
    }
}
