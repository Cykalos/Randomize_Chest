package fr.cykalos.randomizechest.loot;

import fr.cykalos.randomizechest.RandomizeChestPlugin;
import fr.cykalos.randomizechest.storage.LootDatabase;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Lidded;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/** Ouverture, fermeture et sauvegarde des inventaires personnels. */
public final class LootSessionManager {

    private record ViewKey(String containerId, UUID player) {
    }

    private final RandomizeChestPlugin plugin;
    /** Sauvegardes envoyées à la base mais pas encore écrites : prioritaires sur la lecture. */
    private final Map<ViewKey, LootDatabase.StoredLoot> pending = new ConcurrentHashMap<>();
    /** Chargements en cours (évite un double clic qui ouvrirait deux fois). */
    private final Set<ViewKey> opening = new HashSet<>();
    /** Nombre de joueurs regardant chaque bloc (pour l'animation du couvercle). */
    private final Map<Location, Integer> lidViewers = new HashMap<>();

    public LootSessionManager(RandomizeChestPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Ouvre l'inventaire personnel du joueur.
     *
     * @param blockLocation position du bloc, ou null pour un wagonnet
     * @param origin        position utilisée pour le contexte de loot
     */
    public void open(Player player, LootData data, @Nullable Location blockLocation, Location origin, InventoryType type) {
        ViewKey key = new ViewKey(data.id(), player.getUniqueId());
        if (!opening.add(key)) return;

        LootDatabase.StoredLoot cached = pending.get(key);
        if (cached != null) {
            show(key, player, data, blockLocation, origin, type, cached);
            return;
        }

        plugin.database().load(data.id(), player.getUniqueId()).whenComplete((stored, error) ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (error != null) {
                        opening.remove(key);
                        plugin.getLogger().log(Level.SEVERE, "Chargement du butin impossible", error);
                        return;
                    }
                    show(key, player, data, blockLocation, origin, type, stored);
                }));
    }

    private void show(ViewKey key, Player player, LootData data, @Nullable Location blockLocation,
                      Location origin, InventoryType type, @Nullable LootDatabase.StoredLoot stored) {
        opening.remove(key);
        if (!player.isOnline() || player.getWorld() != origin.getWorld()) return;

        long now = System.currentTimeMillis();
        long refill = plugin.settings().refillMillis();
        boolean regenerate = stored == null || (refill > 0 && now - stored.createdAt() >= refill);

        LootInventoryHolder holder = new LootInventoryHolder(data, player.getUniqueId(), blockLocation,
                regenerate ? now : stored.createdAt());
        Inventory inventory = createInventory(holder, type);
        holder.setInventory(inventory);

        if (regenerate) {
            plugin.lootService().fill(inventory, data, player, origin);
        } else {
            ItemStack[] items = ItemStack.deserializeItemsFromBytes(stored.contents());
            ItemStack[] contents = new ItemStack[inventory.getSize()];
            System.arraycopy(items, 0, contents, 0, Math.min(items.length, contents.length));
            inventory.setContents(contents);
        }

        player.openInventory(inventory);
        plugin.visuals().markOpened(player, data.id(), holder.createdAt());
        if (blockLocation != null) lidOpened(blockLocation, data.id());
    }

    private Inventory createInventory(LootInventoryHolder holder, InventoryType type) {
        // Coffres et tonneaux : 27 cases. Autres types (distributeur...) : leur format natif.
        if (type == InventoryType.CHEST || type == InventoryType.BARREL || !type.isCreatable()) {
            return Bukkit.createInventory(holder, 27, plugin.settings().title());
        }
        return Bukkit.createInventory(holder, type, plugin.settings().title());
    }

    /** Appelé à la fermeture de l'inventaire : sauvegarde asynchrone. */
    public void handleClose(LootInventoryHolder holder) {
        ViewKey key = new ViewKey(holder.data().id(), holder.playerId());
        ItemStack[] contents = holder.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] == null) contents[i] = ItemStack.empty();
        }
        LootDatabase.StoredLoot snapshot = new LootDatabase.StoredLoot(
                ItemStack.serializeItemsAsBytes(contents), holder.createdAt());
        pending.put(key, snapshot);
        plugin.database().save(key.containerId(), key.player(), snapshot.contents(), snapshot.createdAt())
                .whenComplete((v, e) -> pending.remove(key, snapshot));

        if (holder.blockLocation() != null) lidClosed(holder.blockLocation(), holder.data().id());
    }

    // ------------------------------------------------------------------ couvercle

    /**
     * Comme en vanilla : effet au premier joueur qui ouvre, et au dernier qui referme.
     * Le vrai couvercle s'ouvre toujours (il joue aussi le son vanilla). Avec l'habillage,
     * le couvercle du modèle s'anime en même temps et recouvre le vrai : on ne voit plus
     * le coffre vanilla fermé à l'intérieur.
     */
    private void lidOpened(Location loc, String containerId) {
        int count = lidViewers.merge(loc, 1, Integer::sum);
        if (count != 1 || !loc.isChunkLoaded()) return;
        if (loc.getBlock().getState(false) instanceof Lidded lidded) lidded.open();
        if (plugin.settings().visualEnabled()) plugin.visuals().animateLid(containerId, true);
    }

    private void lidClosed(Location loc, String containerId) {
        if (!lidViewers.containsKey(loc)) return;
        Integer count = lidViewers.computeIfPresent(loc, (l, c) -> c <= 1 ? null : c - 1);
        if (count != null || !loc.isChunkLoaded()) return;
        if (loc.getBlock().getState(false) instanceof Lidded lidded) lidded.close();
        if (plugin.settings().visualEnabled()) plugin.visuals().animateLid(containerId, false);
    }

    // ------------------------------------------------------------------ suppression / réinitialisation

    /** Ferme l'inventaire de tous les joueurs qui regardent ce conteneur (et sauvegarde). */
    public void closeViewers(String containerId, @Nullable UUID onlyPlayer) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getOpenInventory().getTopInventory().getHolder(false) instanceof LootInventoryHolder h
                    && h.data().id().equals(containerId)
                    && (onlyPlayer == null || onlyPlayer.equals(online.getUniqueId()))) {
                online.closeInventory();
            }
        }
    }

    /** Réinitialise le butin (tous les joueurs si player == null). */
    public void reset(String containerId, @Nullable UUID player) {
        closeViewers(containerId, player);
        pending.keySet().removeIf(k -> k.containerId().equals(containerId)
                && (player == null || k.player().equals(player)));
        // Exécuté après les sauvegardes déclenchées par closeViewers (même thread DB)
        plugin.database().delete(containerId, player);
        plugin.visuals().markReset(containerId, player);
    }

    /** Conteneur détruit : on oublie toutes ses données. */
    public void destroyed(String containerId, @Nullable Location blockLocation) {
        reset(containerId, null);
        plugin.visuals().forget(containerId);
        if (blockLocation != null) lidViewers.remove(blockLocation);
    }

    /** Arrêt du serveur : ferme proprement tous les inventaires ouverts. */
    public void closeAll() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getOpenInventory().getTopInventory().getHolder(false) instanceof LootInventoryHolder) {
                online.closeInventory();
            }
        }
    }
}
