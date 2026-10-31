package dev.aetherpulse.progress;

import org.bukkit.entity.Player;

/** Integration contract. Implementations grant points through ProgressManager on the server thread. */
public interface ProgressProvider {
    String id();
    void award(Player player, String monumentId, long points);
}
