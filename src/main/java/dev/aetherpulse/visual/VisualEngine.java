package dev.aetherpulse.visual;

import org.bukkit.entity.Entity;
import java.util.ArrayList;
import java.util.List;

/** Bounded scene resource owner. Closing is idempotent, including partial spawn failures. */
public final class VisualEngine implements AutoCloseable {
    private final int limit;
    private final List<VisualObject> objects = new ArrayList<>();
    private final List<Entity> auxiliary = new ArrayList<>();
    public VisualEngine(int limit) { this.limit = limit; }
    public VisualObject add(VisualObject object) {
        if (objects.size() == limit) { object.remove(); throw new IllegalStateException("Display entity budget exceeded"); }
        objects.add(object); return object;
    }
    public void own(Entity entity) { auxiliary.add(entity); }
    public boolean valid() { return objects.stream().allMatch(o -> o.display().isValid()) && auxiliary.stream().allMatch(Entity::isValid); }
    public int size() { return objects.size(); }
    @Override public void close() { objects.forEach(VisualObject::remove); auxiliary.forEach(Entity::remove); objects.clear(); auxiliary.clear(); }
}
