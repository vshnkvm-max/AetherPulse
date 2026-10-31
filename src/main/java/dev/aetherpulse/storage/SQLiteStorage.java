package dev.aetherpulse.storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** A single ordered writer owns the connection. No Bukkit API is used on its thread. */
public final class SQLiteStorage implements AutoCloseable {
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "AetherPulse-SQLite"); t.setDaemon(true); return t;
    });
    private final Consumer<Throwable> onError;
    private Connection connection;
    public SQLiteStorage(Consumer<Throwable> onError) { this.onError = onError; }
    public CompletableFuture<List<MonumentRecord>> open(Path path) {
        return submit(() -> {
            Files.createDirectories(path.toAbsolutePath().getParent());
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL"); st.execute("PRAGMA busy_timeout=5000");
                st.execute("CREATE TABLE IF NOT EXISTS monuments (id TEXT PRIMARY KEY, world TEXT NOT NULL, x REAL NOT NULL, y REAL NOT NULL, z REAL NOT NULL, points INTEGER NOT NULL, stage INTEGER NOT NULL, stage_override INTEGER NOT NULL DEFAULT 0, revision INTEGER NOT NULL DEFAULT 0)");
            }
            List<MonumentRecord> rows = new ArrayList<>();
            try (Statement st = connection.createStatement(); ResultSet r = st.executeQuery("SELECT * FROM monuments ORDER BY id")) {
                while (r.next()) rows.add(new MonumentRecord(r.getString("id"), UUID.fromString(r.getString("world")),
                        r.getDouble("x"), r.getDouble("y"), r.getDouble("z"), r.getLong("points"),
                        r.getInt("stage"), r.getInt("stage_override"), r.getLong("revision")));
            }
            return rows;
        });
    }
    public CompletableFuture<Void> save(List<MonumentRecord> records) {
        List<MonumentRecord> snapshot = List.copyOf(records);
        return submit(() -> {
            connection.setAutoCommit(false);
            try (PreparedStatement st = connection.prepareStatement("INSERT INTO monuments VALUES (?,?,?,?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET world=excluded.world,x=excluded.x,y=excluded.y,z=excluded.z,points=excluded.points,stage=excluded.stage,stage_override=excluded.stage_override,revision=excluded.revision WHERE excluded.revision >= monuments.revision")) {
                for (MonumentRecord r : snapshot) {
                    st.setString(1, r.id()); st.setString(2, r.world().toString()); st.setDouble(3, r.x());
                    st.setDouble(4, r.y()); st.setDouble(5, r.z()); st.setLong(6, r.points()); st.setInt(7, r.stage());
                    st.setInt(8, r.stageOverride()); st.setLong(9, r.revision()); st.addBatch();
                }
                st.executeBatch(); connection.commit();
            } catch (Exception e) { connection.rollback(); throw e; }
            finally { connection.setAutoCommit(true); }
            return null;
        });
    }
    public CompletableFuture<Void> delete(String id) {
        return submit(() -> {
            try (PreparedStatement st = connection.prepareStatement("DELETE FROM monuments WHERE id=? COLLATE NOCASE")) { st.setString(1, id); st.executeUpdate(); }
            return null;
        });
    }
    private <T> CompletableFuture<T> submit(Callable<T> work) {
        CompletableFuture<T> result = new CompletableFuture<>();
        try {
            executor.execute(() -> {
                try { result.complete(work.call()); }
                catch (Throwable e) {
                    result.completeExceptionally(e);
                    try { onError.accept(e); } catch (RuntimeException ignored) { /* Preserve the original future outcome. */ }
                }
            });
        } catch (RejectedExecutionException e) { result.completeExceptionally(e); }
        return result;
    }
    /** Drains queued writes and closes the DB; call only during shutdown, never per tick. */
    @Override public void close() {
        submit(() -> { if (connection != null) connection.close(); return null; });
        executor.shutdown();
        try { if (!executor.awaitTermination(15, TimeUnit.SECONDS)) onError.accept(new TimeoutException("SQLite shutdown exceeded 15 seconds; queued writes may be incomplete")); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); onError.accept(e); }
    }
}
