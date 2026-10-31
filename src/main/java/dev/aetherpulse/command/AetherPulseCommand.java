package dev.aetherpulse.command;

import dev.aetherpulse.AetherPulsePlugin;
import dev.aetherpulse.monument.*;
import dev.aetherpulse.progress.ProgressManager;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;

/** Administrative command router; all mutations execute on Bukkit's main thread. */
public final class AetherPulseCommand implements CommandExecutor, TabCompleter {
    private static final List<String> COMMANDS = List.of("create", "remove", "list", "info", "addpoints", "setprogress", "stage", "reload", "help");
    private final AetherPulsePlugin plugin;
    private final MonumentManager monuments;
    private final ProgressManager progress;
    public AetherPulseCommand(AetherPulsePlugin plugin, MonumentManager monuments, ProgressManager progress) {
        this.plugin = plugin; this.monuments = monuments; this.progress = progress;
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("aetherpulse.admin")) { sender.sendMessage("§cMissing permission: aetherpulse.admin"); return true; }
        String action = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        if (action.equals("help")) { help(sender); return true; }
        if (!monuments.ready()) { sender.sendMessage("§eStorage is loading; try again shortly."); return true; }
        try {
            switch (action) {
                case "create" -> {
                    count(args, 2);
                    if (!(sender instanceof Player player)) throw new IllegalArgumentException("This command requires a player");
                    Monument m = monuments.create(CommandInput.id(args[1]), player.getLocation());
                    monuments.attach(m); sender.sendMessage("§aCreated Crystal Core: " + m.id());
                }
                case "remove" -> { count(args, 2); Monument m = require(args[1]); monuments.remove(m); sender.sendMessage("§aRemoved " + m.id()); }
                case "list" -> {
                    count(args, 1);
                    sender.sendMessage("§dMonuments (" + monuments.all().size() + "): §f" + String.join(", ", monuments.all().stream().map(Monument::id).toList()));
                }
                case "info" -> {
                    count(args, 2); Monument m = require(args[1]); var r = m.snapshot();
                    sender.sendMessage("§d" + m.id() + " §f" + m.stage().title() + " (" + m.stage().number() + "/5) | " + m.points() + " points"
                            + (m.overridden() ? " [override]" : "") + "\n§7World " + m.worldId() + " @ " + String.format(Locale.ROOT, "%.1f %.1f %.1f", r.x(), r.y(), r.z())
                            + "\n§7Next natural stage: " + monuments.settings().curve().remaining(m.points()) + " points | Scene: " + (m.visual() == null ? "inactive" : m.visual().entityCount() + " entities"));
                }
                case "addpoints", "setprogress" -> {
                    count(args, 3); Monument m = require(args[1]); long points = CommandInput.points(args[2]);
                    if (action.equals("addpoints")) progress.add(m, points); else progress.set(m, points);
                    sender.sendMessage("§a" + m.id() + ": " + m.points() + " points, " + m.stage().title());
                }
                case "stage" -> {
                    count(args, 3); Monument m = require(args[1]); progress.stage(m, CommandInput.stage(args[2]));
                    sender.sendMessage("§aVisual stage: " + m.stage().title() + ". Changing points clears the override.");
                }
                case "reload" -> { count(args, 1); plugin.reloadSettings(); sender.sendMessage("§aConfiguration applied. Scenes rebuild without loading chunks."); }
                default -> throw new IllegalArgumentException("Unknown subcommand. Use /ap help");
            }
        } catch (IllegalArgumentException e) { sender.sendMessage("§c" + e.getMessage()); }
        return true;
    }
    private void count(String[] args, int expected) { if (args.length != expected) throw new IllegalArgumentException("Invalid arguments. Use /ap help"); }
    private Monument require(String id) {
        Monument m = monuments.find(CommandInput.id(id));
        if (m == null) throw new IllegalArgumentException("Unknown monument: " + id);
        return m;
    }
    private void help(CommandSender sender) {
        sender.sendMessage("§dAetherPulse §f/ap create <id> | remove <id> | list | info <id>\n§f/ap addpoints <id> <amount> | setprogress <id> <amount>\n§f/ap stage <id> <1-5> | reload | help");
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("aetherpulse.admin")) return List.of();
        List<String> options = List.of();
        if (args.length == 1) options = COMMANDS;
        else if (args.length == 2 && List.of("remove", "info", "addpoints", "setprogress", "stage").contains(args[0].toLowerCase(Locale.ROOT))) options = monuments.all().stream().map(Monument::id).toList();
        else if (args.length == 3 && args[0].equalsIgnoreCase("stage")) options = List.of("1", "2", "3", "4", "5");
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.startsWith(prefix)).sorted().toList();
    }
}
