package fr.cykalos.ramdomizechest.listener;

import fr.cykalos.ramdomizechest.RamdomizeChestPlugin;
import fr.cykalos.ramdomizechest.loot.LootData;
import org.bukkit.block.Block;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.persistence.PersistentDataHolder;

import java.util.List;

/** Protection des conteneurs à butin (casse, explosions, entonnoirs). */
public final class ProtectionListener implements Listener {

    private static final String PERM_BREAK = "ramdomizechest.break";

    private final RamdomizeChestPlugin plugin;

    public ProtectionListener(RamdomizeChestPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        LootData data = plugin.lootService().getOrConvert(block);
        if (data == null) return;

        Player player = event.getPlayer();
        if (plugin.settings().protectBreak() && !(player.isSneaking() && player.hasPermission(PERM_BREAK))) {
            event.setCancelled(true);
            player.sendMessage(plugin.settings().msg("break-protected"));
            if (player.hasPermission(PERM_BREAK)) player.sendMessage(plugin.settings().msg("break-hint"));
            return;
        }
        destroyed(block, data);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    private void handleExplosion(List<Block> blocks) {
        blocks.removeIf(block -> {
            LootData data = plugin.lootService().getData(block);
            if (data == null) return false;
            if (plugin.settings().protectExplosions()) return true;
            destroyed(block, data);
            return false;
        });
    }

    private void destroyed(Block block, LootData data) {
        plugin.sessions().destroyed(data.id(), block.getLocation());
        plugin.visuals().remove(block, data.id());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        if (!(event.getVehicle() instanceof StorageMinecart cart)) return;
        LootData data = plugin.lootService().read(cart);
        if (data == null) return;

        if (plugin.settings().protectBreak()
                && !(event.getAttacker() instanceof Player p && p.isSneaking() && p.hasPermission(PERM_BREAK))) {
            event.setCancelled(true);
            if (event.getAttacker() instanceof Player p) p.sendMessage(plugin.settings().msg("break-protected"));
            return;
        }
        plugin.sessions().destroyed(data.id(), null);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHopper(InventoryMoveItemEvent event) {
        if (!plugin.settings().protectHoppers()) return;
        if (isLootInventory(event.getSource()) || isLootInventory(event.getDestination())) {
            event.setCancelled(true);
        }
    }

    private boolean isLootInventory(Inventory inventory) {
        InventoryHolder holder = inventory.getHolder(false);
        if (holder instanceof DoubleChest dc) {
            return isLootHolder(dc.getLeftSide(false)) || isLootHolder(dc.getRightSide(false));
        }
        return isLootHolder(holder);
    }

    private boolean isLootHolder(InventoryHolder holder) {
        if (holder instanceof TileState tile) {
            return tile.getPersistentDataContainer().has(plugin.keys().containerId);
        }
        if (holder instanceof StorageMinecart cart) {
            return ((PersistentDataHolder) cart).getPersistentDataContainer().has(plugin.keys().containerId);
        }
        return false;
    }
}
