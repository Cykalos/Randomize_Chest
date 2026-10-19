package fr.cykalos.ramdomizechest.visual;

import fr.cykalos.ramdomizechest.RamdomizeChestPlugin;
import fr.cykalos.ramdomizechest.PluginSettings;
import fr.cykalos.ramdomizechest.loot.LootData;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * Habillage visuel : un ItemDisplay portant le modèle du resource pack est posé
 * sur chaque bloc à butin pour le distinguer d'un coffre normal.
 */
public final class VisualManager {

    private final RamdomizeChestPlugin plugin;

    public VisualManager(RamdomizeChestPlugin plugin) {
        this.plugin = plugin;
    }

    /** Pose l'habillage s'il est activé et absent. */
    public void ensure(Block block, String containerId) {
        PluginSettings s = plugin.settings();
        if (!s.visualEnabled() || !block.getChunk().isLoaded()) return;
        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        for (ItemDisplay d : block.getWorld().getNearbyEntitiesByType(ItemDisplay.class, center, 0.75)) {
            if (containerId.equals(owner(d))) return;
        }

        float yaw = 0f;
        if (block.getBlockData() instanceof Directional dir) {
            yaw = switch (dir.getFacing()) {
                case NORTH -> 180f;
                case EAST -> -90f;
                case WEST -> 90f;
                default -> 0f;
            };
        }
        Location spawn = block.getLocation().add(0.5, s.visualYOffset(), 0.5);
        spawn.setYaw(yaw);

        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        NamespacedKey model = NamespacedKey.fromString(s.visualModel());
        if (model != null) meta.setItemModel(model);
        item.setItemMeta(meta);

        float scale = s.visualScale();
        block.getWorld().spawn(spawn, ItemDisplay.class, d -> {
            d.setItemStack(item);
            d.setItemDisplayTransform(s.visualTransform());
            d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
                    new Vector3f(scale, scale, scale), new AxisAngle4f()));
            d.setPersistent(true);
            d.getPersistentDataContainer().set(plugin.keys().visualOwner, PersistentDataType.STRING, containerId);
        });
    }

    /** Retire l'habillage d'un conteneur. */
    public void remove(Block block, String containerId) {
        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        for (ItemDisplay d : block.getWorld().getNearbyEntitiesByType(ItemDisplay.class, center, 1.5)) {
            if (containerId.equals(owner(d))) d.remove();
        }
    }

    /** Supprime puis recrée tous les habillages des chunks chargés. Renvoie le nombre créé. */
    public int refreshLoaded() {
        int created = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity e : world.getEntitiesByClass(ItemDisplay.class)) {
                if (owner(e) != null) e.remove();
            }
            if (!plugin.settings().visualEnabled() || !plugin.settings().isWorldEnabled(world)) continue;
            for (Chunk chunk : world.getLoadedChunks()) {
                for (BlockState state : chunk.getTileEntities(
                        b -> plugin.settings().blockTypes().contains(b.getType()), false)) {
                    LootData data = plugin.lootService().getData(state.getBlock());
                    if (data != null) {
                        ensure(state.getBlock(), data.id());
                        created++;
                    }
                }
            }
        }
        return created;
    }

    private String owner(Entity e) {
        return e.getPersistentDataContainer().get(plugin.keys().visualOwner, PersistentDataType.STRING);
    }
}
