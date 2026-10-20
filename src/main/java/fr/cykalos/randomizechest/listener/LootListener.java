package fr.cykalos.randomizechest.listener;

import fr.cykalos.randomizechest.RandomizeChestPlugin;
import fr.cykalos.randomizechest.loot.LootData;
import fr.cykalos.randomizechest.loot.LootInventoryHolder;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Conversion des conteneurs naturels et ouverture des inventaires personnels. */
public final class LootListener implements Listener {

    private static final String PERM_USE = "randomizechest.use";

    private final RandomizeChestPlugin plugin;

    public LootListener(RandomizeChestPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ conversion

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        plugin.lootService().scanChunk(event.getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        if (plugin.settings().minecarts()) {
            for (Entity entity : event.getEntities()) {
                if (entity instanceof StorageMinecart) plugin.lootService().getOrConvert(entity);
            }
        }
        // Les entités d'un chunk se chargent après ses blocs : c'est le bon moment pour
        // enregistrer les habillages existants et poser ceux qui manquent.
        plugin.visuals().onEntitiesLoaded(event.getEntities());
        Chunk chunk = event.getChunk();
        if (chunk.isLoaded()) plugin.visuals().ensureChunk(chunk);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        plugin.visuals().onEntitiesUnloaded(event.getEntities());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        plugin.visuals().loadPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        plugin.visuals().unloadPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (!plugin.settings().blockTypes().contains(block.getType())) return;
        // Au tick suivant : les composants de l'item (ex. table de loot copiée en créatif) sont appliqués
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!plugin.settings().blockTypes().contains(block.getType())) return;
            plugin.lootService().markPlayerPlaced(block);
            plugin.lootService().preventDoubleChest(block);
        });
    }

    // ------------------------------------------------------------------ ouverture

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.useInteractedBlock() == Event.Result.DENY) return; // bloqué par une protection (WorldGuard...)
        Block block = event.getClickedBlock();
        if (block == null || !plugin.lootService().isSupported(block)) return;

        Player player = event.getPlayer();
        // Accroupi avec un objet en main : le vanilla pose le bloc au lieu d'ouvrir
        if (player.isSneaking() && (!player.getInventory().getItemInMainHand().isEmpty()
                || !player.getInventory().getItemInOffHand().isEmpty())) return;

        LootData data = plugin.lootService().getOrConvert(block);
        if (data == null) return;

        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
        if (event.getHand() != EquipmentSlot.HAND) return;

        if (!player.hasPermission(PERM_USE)) {
            player.sendMessage(plugin.settings().msg("no-permission"));
            return;
        }

        InventoryType type = block.getState(false) instanceof Container c
                ? c.getInventory().getType() : InventoryType.CHEST;
        plugin.sessions().open(player, data, block.getLocation(), block.getLocation().add(0.5, 0.5, 0.5), type);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof StorageMinecart cart)) return;
        LootData data = plugin.lootService().getOrConvert(cart);
        if (data == null) return;

        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        if (!player.hasPermission(PERM_USE)) {
            player.sendMessage(plugin.settings().msg("no-permission"));
            return;
        }
        plugin.sessions().open(player, data, null, cart.getLocation(), InventoryType.CHEST);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder(false) instanceof LootInventoryHolder holder) {
            plugin.sessions().handleClose(holder);
        }
    }
}
