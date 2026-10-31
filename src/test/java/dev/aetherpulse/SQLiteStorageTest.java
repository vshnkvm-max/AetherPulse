package dev.aetherpulse;

import dev.aetherpulse.storage.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class SQLiteStorageTest {
    @TempDir Path temp;
    private final UUID world = UUID.randomUUID();
    private MonumentRecord row(String id, long points, int stage, int override, long rev) {
        return new MonumentRecord(id, world, -16.5, 80, 32.25, points, stage, override, rev);
    }
    @Test void persistsRestartsOverridesAndDeletion() throws Exception {
        List<Throwable> errors = new CopyOnWriteArrayList<>(); Path path = temp.resolve("data.db");
        try (SQLiteStorage storage = new SQLiteStorage(errors::add)) {
            assertTrue(storage.open(path).get(5, TimeUnit.SECONDS).isEmpty());
            storage.save(List.of(row("crystal", 500, 4, 4, 2), row("deleted", 0, 1, 0, 0))).get(5, TimeUnit.SECONDS);
            storage.delete("deleted").get(5, TimeUnit.SECONDS);
        }
        try (SQLiteStorage storage = new SQLiteStorage(errors::add)) {
            assertEquals(List.of(row("crystal", 500, 4, 4, 2)), storage.open(path).get(5, TimeUnit.SECONDS));
        }
        assertTrue(errors.isEmpty(), errors.toString());
    }
    @Test void staleSnapshotsCannotOverwriteNewerProgress() throws Exception {
        List<Throwable> errors = new CopyOnWriteArrayList<>(); Path path = temp.resolve("data.db");
        try (SQLiteStorage storage = new SQLiteStorage(errors::add)) {
            storage.open(path).get(5, TimeUnit.SECONDS);
            storage.save(List.of(row("c", 2500, 5, 0, 10)));
            storage.save(List.of(row("c", 100, 2, 0, 9)));
        }
        try (SQLiteStorage storage = new SQLiteStorage(errors::add)) {
            assertEquals(2500, storage.open(path).get(5, TimeUnit.SECONDS).getFirst().points());
        }
        assertTrue(errors.isEmpty());
    }
    @Test void orderedDeleteAndRecreate() throws Exception {
        Path path = temp.resolve("data.db"); List<Throwable> errors = new CopyOnWriteArrayList<>();
        try (SQLiteStorage s = new SQLiteStorage(errors::add)) {
            s.open(path).get(5, TimeUnit.SECONDS);
            s.save(List.of(row("c", 500, 3, 0, 10))); s.delete("c"); s.save(List.of(row("c", 0, 1, 0, 0)));
        }
        try (SQLiteStorage s = new SQLiteStorage(errors::add)) { assertEquals(0, s.open(path).get(5, TimeUnit.SECONDS).getFirst().points()); }
        assertTrue(errors.isEmpty());
    }
    @Test void failurePropagatesInsteadOfReturningEmptyState() throws Exception {
        List<Throwable> errors = new CopyOnWriteArrayList<>();
        try (SQLiteStorage s = new SQLiteStorage(errors::add)) {
            var failure = s.open(temp); // directory is not a database file
            assertThrows(java.util.concurrent.ExecutionException.class, () -> failure.get(5, TimeUnit.SECONDS));
        }
        assertFalse(errors.isEmpty());
    }
    @Test void rejectsCorruptDomainState() {
        assertThrows(IllegalArgumentException.class, () -> row("c", -1, 1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> row("c", 0, 9, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> row("../x", 0, 1, 0, 0));
    }
}
