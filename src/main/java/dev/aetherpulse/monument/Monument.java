package dev.aetherpulse.monument;

import dev.aetherpulse.progress.ProgressCurve;
import dev.aetherpulse.storage.MonumentRecord;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import java.util.UUID;

/** Main-thread domain state with an immutable persistence boundary. */
public final class Monument {
    private final String id;
    private final UUID world;
    private final double x, y, z;
    private long points, revision;
    private int override;
    private MonumentStage stage;
    private CrystalCore visual;
    public Monument(MonumentRecord r, ProgressCurve curve) {
        id = r.id(); world = r.world(); x = r.x(); y = r.y(); z = r.z(); points = r.points();
        override = r.stageOverride(); revision = r.revision();
        stage = override == 0 ? curve.stage(points) : MonumentStage.fromNumber(override);
    }
    public String id() { return id; }
    public UUID worldId() { return world; }
    public long points() { return points; }
    public MonumentStage stage() { return stage; }
    public boolean overridden() { return override != 0; }
    public int chunkX() { return ((int) Math.floor(x)) >> 4; }
    public int chunkZ() { return ((int) Math.floor(z)) >> 4; }
    public Location location() { World w = Bukkit.getWorld(world); return w == null ? null : new Location(w, x, y, z); }
    public CrystalCore visual() { return visual; }
    public void visual(CrystalCore value) { visual = value; }
    public void points(long value, ProgressCurve curve) { points = Math.max(0, value); override = 0; refresh(curve); }
    public void override(int number, ProgressCurve curve) { override = number; refresh(curve); }
    public void refresh(ProgressCurve curve) {
        MonumentStage old = stage;
        stage = override == 0 ? curve.stage(points) : MonumentStage.fromNumber(override);
        revision++;
        if (visual != null && old != stage) visual.transition(old, stage);
    }
    public MonumentRecord snapshot() { return new MonumentRecord(id, world, x, y, z, points, stage.number(), override, revision); }
}
