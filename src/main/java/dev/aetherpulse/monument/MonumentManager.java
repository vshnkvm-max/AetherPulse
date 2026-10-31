package dev.aetherpulse.monument;

import dev.aetherpulse.AetherPulsePlugin;
import dev.aetherpulse.config.Settings;
import dev.aetherpulse.storage.*;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.event.*;
import org.bukkit.event.world.*;
import org.bukkit.persistence.PersistentDataType;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Main-thread registry and chunk-aware scene lifecycle. Never requests chunk loading. */
public final class MonumentManager implements Listener, AutoCloseable {
    private final AetherPulsePlugin plugin;
    private final SQLiteStorage storage;
    private final NamespacedKey key;
    private final Map<String, Monument> monuments = new LinkedHashMap<>();
    private final Set<String> dirty = new HashSet<>();
    private final Map<String, CompletableFuture<Void>> deletions = new HashMap<>();
    private Settings settings;
    private boolean ready;
    private CompletableFuture<Void> pendingSave = CompletableFuture.completedFuture(null);
    private Set<String> pendingBatch = Set.of();
    private CompletableFuture<List<MonumentRecord>> loading;
    public void startLoading(CompletableFuture<List<MonumentRecord>> future) { loading = future; }
    /** Poll completion on the server thread; worker callbacks never access Bukkit. */
    public void pollLoad() {
        if (loading == null || !loading.isDone()) return;
        var completed = loading; loading = null;
        try {
            var records = completed.join(); load(records);
            plugin.getLogger().info("Loaded " + records.size() + " monuments. AetherPulse is ready.");
        } catch (RuntimeException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Storage could not be loaded; disabling to protect data", e);
            Bukkit.getPluginManager().disablePlugin(plugin);
        }
    }
    public MonumentManager(AetherPulsePlugin plugin, SQLiteStorage storage, Settings settings) {
        this.plugin = plugin; this.storage = storage; this.settings = settings; key = new NamespacedKey(plugin, "monument");
    }
    public NamespacedKey key() { return key; }
    public Settings settings() { return settings; }
    public boolean ready() { return ready; }
    public Collection<Monument> all() { return Collections.unmodifiableCollection(monuments.values()); }
    public Monument find(String id) { return monuments.get(id.toLowerCase(Locale.ROOT)); }
    public void load(List<MonumentRecord> rows) {
        for (MonumentRecord row : rows) {
            Monument m = new Monument(row, settings.curve());
            if (monuments.putIfAbsent(m.id().toLowerCase(Locale.ROOT), m) != null) throw new IllegalStateException("Duplicate monument ID in storage: " + m.id());
        }
        for (World w : Bukkit.getWorlds()) for (Chunk c : w.getLoadedChunks()) cleanup(c);
        ready = true;
        all().forEach(this::changed);
    }
    public Monument create(String id, Location location) {
        if (find(id) != null) throw new IllegalArgumentException("Monument already exists");
        var deletion = deletions.get(id.toLowerCase(Locale.ROOT));
        if (deletion != null) {
            if (!deletion.isDone() || deletion.isCompletedExceptionally()) throw new IllegalArgumentException("Previous deletion is still pending; try again shortly");
            deletions.remove(id.toLowerCase(Locale.ROOT));
        }
        Monument m = new Monument(new MonumentRecord(id, location.getWorld().getUID(), location.getX(), location.getY(), location.getZ(), 0, 1, 0, 0), settings.curve());
        monuments.put(id.toLowerCase(Locale.ROOT), m); changed(m); return m;
    }
    public void changed(Monument m) { dirty.add(m.id()); }
    public void remove(Monument m) {
        detach(m); monuments.remove(m.id().toLowerCase(Locale.ROOT)); dirty.remove(m.id());
        deletions.put(m.id().toLowerCase(Locale.ROOT), storage.delete(m.id()));
    }
    public boolean loaded(Monument m) {
        World world = Bukkit.getWorld(m.worldId());
        return world != null && world.isChunkLoaded(m.chunkX(), m.chunkZ());
    }
    public boolean viewed(Monument m) {
        Location base = m.location();
        if (base == null) return false;
        double radiusSquared = settings.activationRadius() * settings.activationRadius();
        return base.getWorld().getPlayers().stream().anyMatch(p -> p.getLocation().distanceSquared(base) <= radiusSquared);
    }
    public void attach(Monument m) {
        if (m.visual() != null || !loaded(m)) return;
        try { m.visual(new CrystalCore(m, settings, key)); }
        catch (RuntimeException e) { plugin.getLogger().log(java.util.logging.Level.SEVERE, "Cannot spawn " + m.id(), e); }
    }
    public void detach(Monument m) { if (m.visual() != null) { m.visual().close(); m.visual(null); } }
    public void reload(Settings value) {
        for (Monument m : all()) detach(m);
        settings = value;
        for (Monument m : all()) { m.refresh(settings.curve()); changed(m); }
    }
    public void flush() {
        deletions.entrySet().removeIf(e -> e.getValue().isDone() && !e.getValue().isCompletedExceptionally());
        deletions.replaceAll((id, future) -> future.isCompletedExceptionally() ? storage.delete(id) : future);
        if (!pendingSave.isDone()) return;
        if (pendingSave.isCompletedExceptionally()) {
            for (String id : pendingBatch) if (find(id) != null) dirty.add(id);
        }
        pendingBatch = Set.of();
        if (dirty.isEmpty()) return;
        List<MonumentRecord> records = dirty.stream().map(this::find).filter(Objects::nonNull).map(Monument::snapshot).toList();
        pendingBatch = Set.copyOf(dirty); dirty.clear();
        pendingSave = storage.save(records);
    }

    private void cleanup(Chunk chunk) {
        for (Entity e : chunk.getEntities()) if (e.getPersistentDataContainer().has(key, PersistentDataType.STRING)) e.remove();
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void chunkUnload(ChunkUnloadEvent e) {
        for (Monument m : all()) if (m.worldId().equals(e.getWorld().getUID()) && m.chunkX() == e.getChunk().getX() && m.chunkZ() == e.getChunk().getZ()) detach(m);
        cleanup(e.getChunk());
    }
    @EventHandler public void chunkLoad(ChunkLoadEvent e) {
        if (ready) cleanup(e.getChunk());
    }
    @EventHandler public void worldUnload(WorldUnloadEvent e) {
        for (Monument m : all()) if (m.worldId().equals(e.getWorld().getUID())) detach(m);
    }
    @Override public void close() {
        // Ordered final snapshot also retries any failed or in-flight batch before connection close.
        deletions.forEach((id, future) -> { if (future.isCompletedExceptionally()) storage.delete(id); });
        if (ready) storage.save(all().stream().map(Monument::snapshot).toList());
        ready = false;
        all().forEach(this::detach);
        monuments.clear(); dirty.clear();
    }
}
