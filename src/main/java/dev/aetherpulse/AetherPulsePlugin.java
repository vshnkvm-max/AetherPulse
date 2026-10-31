package dev.aetherpulse;

import dev.aetherpulse.animation.AnimationManager;
import dev.aetherpulse.command.AetherPulseCommand;
import dev.aetherpulse.config.Settings;
import dev.aetherpulse.interaction.InteractionListener;
import dev.aetherpulse.monument.MonumentManager;
import dev.aetherpulse.progress.ProgressManager;
import dev.aetherpulse.storage.SQLiteStorage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.Objects;
import java.util.logging.Level;

/** Plugin composition root. Bukkit state never crosses the SQLite thread boundary. */
public final class AetherPulsePlugin extends JavaPlugin {
    private SQLiteStorage storage;
    private MonumentManager monuments;
    private ProgressManager progress;
    private AnimationManager animation;
    @Override public void onEnable() {
        try {
            saveDefaultConfig();
            Settings settings = Settings.read(getConfig());
            var logger = getLogger();
            storage = new SQLiteStorage(e -> logger.log(Level.SEVERE, "SQLite operation failed", e));
            monuments = new MonumentManager(this, storage, settings);
            progress = new ProgressManager(monuments);
            Bukkit.getPluginManager().registerEvents(monuments, this);
            Bukkit.getPluginManager().registerEvents(progress, this);
            Bukkit.getPluginManager().registerEvents(new InteractionListener(monuments), this);
            var router = new AetherPulseCommand(this, monuments, progress);
            var command = Objects.requireNonNull(getCommand("ap"));
            command.setExecutor(router); command.setTabCompleter(router);
            animation = new AnimationManager(this, monuments);
            monuments.startLoading(storage.open(getDataFolder().toPath().resolve("monuments.db")));
        } catch (RuntimeException e) { getLogger().log(Level.SEVERE, "Cannot enable AetherPulse", e); Bukkit.getPluginManager().disablePlugin(this); }
    }
    /** Validates an independent YAML document before replacing live settings. */
    public void reloadSettings() {
        YamlConfiguration config = new YamlConfiguration();
        try { config.load(new File(getDataFolder(), "config.yml")); }
        catch (Exception e) { throw new IllegalArgumentException("Cannot parse config.yml: " + e.getMessage(), e); }
        Settings next = Settings.read(config);
        monuments.reload(next);
        reloadConfig();
    }
    /** Integrations call this API on the primary server thread after startup has completed. */
    public ProgressManager progress() { return progress; }
    @Override public void onDisable() {
        if (animation != null) animation.close();
        if (monuments != null) monuments.close();
        if (storage != null) storage.close();
        Bukkit.getScheduler().cancelTasks(this);
    }
}
