package fr.cykalos.ramdomizechest.loot;

import fr.cykalos.ramdomizechest.Keys;
import fr.cykalos.ramdomizechest.RamdomizeChestPlugin;
import fr.cykalos.ramdomizechest.PluginSettings;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.type.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.inventory.Inventory;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.Lootable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/**
 * Détection et conversion des conteneurs générés naturellement.
 * <p>
 * Un conteneur généré par le monde possède une table de loot tant que personne ne l'a ouvert.
 * On mémorise cette table dans son PDC, puis on la retire : le vrai conteneur reste vide pour
 * toujours et chaque joueur reçoit à la place un inventaire virtuel tiré depuis la table.
 * Un conteneur posé par un joueur n'a pas de table de loot : il n'est donc jamais converti.
 */
public final class LootContainerService {

    private final RamdomizeChestPlugin plugin;
    private final Keys keys;

    public LootContainerService(RamdomizeChestPlugin plugin) {
        this.plugin = plugin;
        this.keys = plugin.keys();
    }

    private PluginSettings settings() {
        return plugin.settings();
    }

    // ------------------------------------------------------------------ lecture

    public @Nullable LootData read(PersistentDataContainer pdc) {
        String id = pdc.get(keys.containerId, PersistentDataType.STRING);
        String table = pdc.get(keys.lootTable, PersistentDataType.STRING);
        if (id == null || table == null) return null;
        NamespacedKey key = NamespacedKey.fromString(table);
        if (key == null) return null;
        Long seed = pdc.get(keys.lootSeed, PersistentDataType.LONG);
        return new LootData(id, key, seed == null ? 0L : seed);
    }

    public @Nullable LootData read(PersistentDataHolder holder) {
        return read(holder.getPersistentDataContainer());
    }

    public boolean isSupported(Block block) {
        return settings().blockTypes().contains(block.getType()) && settings().isWorldEnabled(block.getWorld());
    }

    /** Lecture seule : renvoie les données si le bloc est déjà un conteneur à butin. */
    public @Nullable LootData getData(Block block) {
        if (!settings().blockTypes().contains(block.getType())) return null;
        if (!(block.getState(false) instanceof TileState tile)) return null;
        return read(tile.getPersistentDataContainer());
    }

    // ------------------------------------------------------------------ conversion

    /** Renvoie les données du bloc, en le convertissant s'il est encore un coffre naturel intact. */
    public @Nullable LootData getOrConvert(Block block) {
        if (!isSupported(block)) return null;
        return convert(block.getState());
    }

    /** @param state état « snapshot » du bloc */
    public @Nullable LootData convert(BlockState state) {
        if (!(state instanceof TileState tile) || !(state instanceof Lootable lootable)) return null;
        PersistentDataContainer pdc = tile.getPersistentDataContainer();

        LootData existing = read(pdc);
        if (existing != null) return existing;
        if (pdc.has(keys.playerPlaced, PersistentDataType.BYTE)) return null;

        LootTable table = lootable.getLootTable();
        if (table == null) return null; // déjà pillé, ou posé par un joueur

        LootData data = new LootData(UUID.randomUUID().toString(), table.getKey(), lootable.getSeed());
        write(pdc, data);
        lootable.setLootTable(null); // le vrai conteneur ne se remplira jamais
        tile.update(true, false);

        if (settings().visualEnabled()) {
            Location loc = state.getLocation();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.visuals().ensure(loc.getBlock(), data.id()));
        }
        return data;
    }

    public @Nullable LootData getOrConvert(Entity entity) {
        if (!settings().minecarts() || !(entity instanceof StorageMinecart cart)) return null;
        if (!settings().isWorldEnabled(entity.getWorld())) return null;

        PersistentDataContainer pdc = cart.getPersistentDataContainer();
        LootData existing = read(pdc);
        if (existing != null) return existing;

        LootTable table = cart.getLootTable();
        if (table == null) return null;

        LootData data = new LootData(UUID.randomUUID().toString(), table.getKey(), cart.getSeed());
        write(pdc, data);
        cart.setLootTable(null);
        return data;
    }

    private void write(PersistentDataContainer pdc, LootData data) {
        pdc.set(keys.containerId, PersistentDataType.STRING, data.id());
        pdc.set(keys.lootTable, PersistentDataType.STRING, data.table().toString());
        pdc.set(keys.lootSeed, PersistentDataType.LONG, data.seed());
    }

    /** Convertit tous les conteneurs naturels d'un chunk. Renvoie le nombre de nouveaux conteneurs. */
    public int scanChunk(Chunk chunk) {
        if (!settings().isWorldEnabled(chunk.getWorld())) return 0;
        int converted = 0;
        for (BlockState state : chunk.getTileEntities(b -> settings().blockTypes().contains(b.getType()), true)) {
            if (state instanceof TileState tile && tile.getPersistentDataContainer().has(keys.containerId)) continue;
            if (convert(state) != null) converted++;
        }
        return converted;
    }

    public int scanLoaded() {
        int total = 0;
        for (World world : Bukkit.getWorlds()) {
            if (!settings().isWorldEnabled(world)) continue;
            for (Chunk chunk : world.getLoadedChunks()) total += scanChunk(chunk);
            if (settings().minecarts()) {
                for (StorageMinecart cart : world.getEntitiesByClass(StorageMinecart.class)) {
                    if (!cart.getPersistentDataContainer().has(keys.containerId) && getOrConvert(cart) != null) total++;
                }
            }
        }
        return total;
    }

    // ------------------------------------------------------------------ blocs posés par des joueurs

    /** Marque un conteneur posé par un joueur pour qu'il ne soit jamais converti. */
    public void markPlayerPlaced(Block block) {
        if (!(block.getState() instanceof TileState tile)) return;
        tile.getPersistentDataContainer().set(keys.playerPlaced, PersistentDataType.BYTE, (byte) 1);
        tile.update(true, false);
    }

    /**
     * Empêche un coffre posé par un joueur de former un double coffre avec un coffre à butin
     * (sinon le joueur pourrait cacher des objets dans le vrai inventaire, toujours vide).
     */
    public void preventDoubleChest(Block placed) {
        if (!(placed.getBlockData() instanceof Chest chestData) || chestData.getType() == Chest.Type.SINGLE) return;
        Block other = placed.getRelative(connectedFace(chestData));
        if (getData(other) == null) return;

        chestData.setType(Chest.Type.SINGLE);
        placed.setBlockData(chestData, false);
        if (other.getBlockData() instanceof Chest otherData) {
            otherData.setType(Chest.Type.SINGLE);
            other.setBlockData(otherData, false);
        }
    }

    /** Même logique que ChestBlock#getConnectedDirection en vanilla. */
    private static BlockFace connectedFace(Chest data) {
        BlockFace facing = data.getFacing();
        boolean left = data.getType() == Chest.Type.LEFT;
        return switch (facing) {
            case NORTH -> left ? BlockFace.EAST : BlockFace.WEST;
            case EAST -> left ? BlockFace.SOUTH : BlockFace.NORTH;
            case SOUTH -> left ? BlockFace.WEST : BlockFace.EAST;
            case WEST -> left ? BlockFace.NORTH : BlockFace.SOUTH;
            default -> BlockFace.SELF;
        };
    }

    // ------------------------------------------------------------------ génération du butin

    /** Remplit l'inventaire virtuel d'un joueur à partir de la table de loot d'origine. */
    public void fill(Inventory inventory, LootData data, Player player, Location origin) {
        LootTable table = Bukkit.getLootTable(data.table());
        if (table == null) {
            plugin.getLogger().warning("Table de loot introuvable : " + data.table() + " (conteneur " + data.id() + ")");
            return;
        }

        long seed = switch (settings().seedMode()) {
            case RANDOM -> ThreadLocalRandom.current().nextLong();
            case PER_PLAYER -> baseSeed(data) ^ player.getUniqueId().getMostSignificantBits()
                    ^ Long.rotateLeft(player.getUniqueId().getLeastSignificantBits(), 17);
            case SHARED -> baseSeed(data);
        };

        float luck = 0f;
        AttributeInstance luckAttr = player.getAttribute(Attribute.LUCK);
        if (luckAttr != null) luck = (float) luckAttr.getValue();

        LootContext context = new LootContext.Builder(origin)
                .lootedEntity(player)
                .luck(luck)
                .build();
        try {
            table.fillInventory(inventory, new Random(seed), context);
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.WARNING, "Échec du tirage de " + data.table(), e);
        }
    }

    private static long baseSeed(LootData data) {
        return data.seed() != 0L ? data.seed() : data.id().hashCode() * 0x9E3779B97F4A7C15L;
    }
}
