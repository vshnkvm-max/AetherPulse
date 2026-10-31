package dev.aetherpulse.animation;

import dev.aetherpulse.AetherPulsePlugin;
import dev.aetherpulse.monument.*;
import dev.aetherpulse.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import java.util.ArrayList;

/** One central scheduler, distance culling and a fair global particle budget. */
public final class AnimationManager implements AutoCloseable {
    private final MonumentManager manager;
    private final BukkitTask task;
    private long ticks;
    private int cursor;
    public AnimationManager(AetherPulsePlugin plugin, MonumentManager manager) {
        this.manager = manager;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1, 1);
    }
    private void tick() {
        ticks++;
        manager.pollLoad();
        if (!manager.ready()) return;
        Settings s = manager.settings();
        if (ticks % 100 == 0) manager.flush();
        boolean animate = ticks % s.animationInterval() == 0;
        boolean particles = ticks % s.particleInterval() == 0;
        if (!animate && !particles) return;
        var active = new ArrayList<CrystalCore>();
        for (Monument m : manager.all()) {
            if (!manager.loaded(m)) { manager.detach(m); continue; }
            if (!manager.viewed(m)) continue;
            if (m.visual() != null && !m.visual().valid()) manager.detach(m);
            manager.attach(m);
            if (m.visual() == null) continue;
            if (animate) m.visual().update(s.animationInterval() / 20.0);
            active.add(m.visual());
        }
        if (particles && !active.isEmpty()) {
            int budget = s.particleBudget();
            int start = Math.floorMod(cursor++, active.size());
            for (int i = 0; i < active.size() && budget > 0; i++) budget -= active.get((start + i) % active.size()).particles(budget);
        }
    }
    @Override public void close() { task.cancel(); }
}
