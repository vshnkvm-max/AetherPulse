package dev.aetherpulse.interaction;

import dev.aetherpulse.monument.*;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import java.util.*;

/** Both Interaction hit and right-click paths share a monotonic cooldown. */
public final class InteractionListener implements Listener {
    private final MonumentManager manager;
    private final Map<UUID, Long> cooldown = new HashMap<>();
    public InteractionListener(MonumentManager manager) { this.manager = manager; }
    @EventHandler(ignoreCancelled = true) public void click(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof Interaction) || e.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        if (owned(e.getRightClicked())) { e.setCancelled(true); interact(e.getPlayer(), e.getRightClicked()); }
    }
    @EventHandler(ignoreCancelled = true) public void clickAt(PlayerInteractAtEntityEvent e) { click(e); }
    @EventHandler(ignoreCancelled = true) public void hit(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Interaction && e.getDamager() instanceof Player p && owned(e.getEntity())) {
            e.setCancelled(true); interact(p, e.getEntity());
        }
    }
    @EventHandler public void quit(PlayerQuitEvent e) { cooldown.remove(e.getPlayer().getUniqueId()); }
    private boolean owned(Entity e) { return e.getPersistentDataContainer().has(manager.key(), PersistentDataType.STRING); }
    private void interact(Player player, Entity entity) {
        if (!manager.ready() || !player.hasPermission("aetherpulse.interact")) return;
        String id = entity.getPersistentDataContainer().get(manager.key(), PersistentDataType.STRING);
        Monument m = manager.find(id);
        if (m == null) return;
        long now = System.nanoTime();
        Long previous = cooldown.get(player.getUniqueId());
        if (previous != null && now - previous < manager.settings().cooldownTicks() * 50_000_000L) return;
        cooldown.put(player.getUniqueId(), now);
        long remaining = manager.settings().curve().remaining(m.points());
        player.sendMessage("§dAetherPulse §8» §f" + m.id() + " §7— " + m.stage().title() + " (" + m.stage().number() + "/5)\n§7Progress: §f" + m.points()
                + " §7| " + (remaining == 0 ? "Ascended" : remaining + " points to next natural stage")
                + (m.overridden() ? " §e[visual stage override]" : ""));
        if (manager.settings().sounds()) player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, .6f, 1.3f);
        if (m.visual() != null) m.visual().react();
    }
}
