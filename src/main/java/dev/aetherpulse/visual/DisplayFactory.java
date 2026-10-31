package dev.aetherpulse.visual;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/** Centralizes ownership, interpolation and nonpersistent entity lifecycle. Main thread only. */
public final class DisplayFactory {
    private final NamespacedKey key;
    private final String id;
    private final int interval;
    public DisplayFactory(NamespacedKey key, String id, int interval) { this.key = key; this.id = id; this.interval = interval; }
    public void tag(Entity entity) {
        entity.setPersistent(false);
        entity.getPersistentDataContainer().set(key, PersistentDataType.STRING, id);
    }
    private void setup(Display entity) {
        tag(entity); entity.setInterpolationDuration(interval); entity.setInterpolationDelay(0);
        entity.setTeleportDuration(interval); entity.setBrightness(new Display.Brightness(15, 15));
        entity.setViewRange(1.5f); entity.setDisplayWidth(16); entity.setDisplayHeight(16);
    }
    public BlockDisplay block(Location origin, Material material) {
        return origin.getWorld().spawn(origin, BlockDisplay.class, e -> { setup(e); e.setBlock(material.createBlockData()); });
    }
    public ItemDisplay item(Location origin, Material material) {
        return origin.getWorld().spawn(origin, ItemDisplay.class, e -> {
            setup(e); e.setItemStack(new ItemStack(material)); e.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        });
    }
    public TextDisplay text(Location origin) {
        return origin.getWorld().spawn(origin, TextDisplay.class, e -> {
            setup(e); e.setBillboard(Display.Billboard.CENTER); e.setSeeThrough(false); e.setShadowed(true);
            e.setDefaultBackground(false); e.setBackgroundColor(org.bukkit.Color.fromARGB(50, 18, 8, 30)); e.setLineWidth(240);
        });
    }
    public Interaction interaction(Location origin) {
        return origin.getWorld().spawn(origin, Interaction.class, e -> {
            tag(e); e.setInteractionWidth(1.8f); e.setInteractionHeight(2.6f); e.setResponsive(true);
        });
    }
}
