package fr.cykalos.randomizechest.visual;

import fr.cykalos.randomizechest.RandomizeChestPlugin;
import fr.cykalos.randomizechest.PluginSettings;
import fr.cykalos.randomizechest.loot.LootData;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Apparence des conteneurs à butin, propre à chaque joueur (comme Lootr).
 * <p>
 * Chaque conteneur porte deux ItemDisplay superposés :
 * <ul>
 *   <li>« non ouvert » : visible par défaut ;</li>
 *   <li>« déjà ouvert » : caché par défaut.</li>
 * </ul>
 * Pour un joueur qui a déjà ouvert le conteneur, le premier est caché et le second montré,
 * grâce à la visibilité d'entités par joueur de Paper. Les autres joueurs ne voient aucun changement.
 */
public final class VisualManager {

    private static final String UNOPENED = "unopened";
    private static final String OPENED = "opened";
    /** À augmenter quand la position, l'orientation ou la taille des habillages change. */
    private static final int REVISION = 2;
    /** Nombre d'images de l'animation du couvercle (1 par tick = 0,5 s, comme en vanilla). */
    private static final int LID_FRAMES = 10;

    private final RandomizeChestPlugin plugin;

    /** Displays chargés : id du conteneur → UUID de l'entité. */
    private final Map<String, UUID> unopenedDisplays = new HashMap<>();
    private final Map<String, UUID> openedDisplays = new HashMap<>();

    /** Joueurs connectés : conteneurs déjà ouverts (id → date du tirage). */
    private final Map<UUID, Map<String, Long>> openedByPlayer = new ConcurrentHashMap<>();

    /** Image actuelle du couvercle par conteneur (absent = fermé) et animations en cours. */
    private final Map<String, Integer> lidFrames = new HashMap<>();
    private final Map<String, BukkitTask> lidAnimations = new HashMap<>();

    public VisualManager(RandomizeChestPlugin plugin) {
        this.plugin = plugin;
    }

    private PluginSettings settings() {
        return plugin.settings();
    }

    // ------------------------------------------------------------------ création / suppression

    /** Pose les deux habillages s'ils manquent (et remplace ceux d'anciennes versions). */
    public void ensure(Block block, String containerId) {
        PluginSettings s = settings();
        if (!s.visualEnabled() || !block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)) return;

        ItemDisplay unopened = null;
        ItemDisplay opened = null;
        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        for (ItemDisplay d : block.getWorld().getNearbyEntitiesByType(ItemDisplay.class, center, 1.0)) {
            if (!d.isValid() || !containerId.equals(owner(d))) continue;
            String state = state(d);
            boolean current = Integer.valueOf(REVISION).equals(d.getPersistentDataContainer()
                    .get(plugin.keys().visualRevision, PersistentDataType.INTEGER));
            if (!current) {
                // Habillage d'une version précédente : recréé avec les réglages actuels
                unregister(d);
                d.remove();
            } else if (UNOPENED.equals(state) && unopened == null) {
                unopened = d;
            } else if (OPENED.equals(state) && opened == null) {
                opened = d;
            } else {
                // Doublon, ou habillage de la version 1.0.0 (sans état)
                unregister(d);
                d.remove();
            }
        }

        PluginSettings.VisualModels models = s.modelsFor(block.getType());
        if (unopened == null) unopened = spawn(block, containerId, UNOPENED, models.unopened(), true);
        if (opened == null) opened = spawn(block, containerId, OPENED, models.opened(), false);
        register(unopened);
        register(opened);
        applyToOnline(containerId);
    }

    private ItemDisplay spawn(Block block, String containerId, String state,
                              org.bukkit.NamespacedKey model, boolean visibleByDefault) {
        PluginSettings s = settings();

        float yaw = s.visualYawOffset();
        if (block.getBlockData() instanceof Directional dir) {
            // Face avant du modèle = face sud, comme le coffre vanilla
            yaw += switch (dir.getFacing()) {
                case SOUTH -> 180f;
                case EAST -> 90f;
                case WEST -> -90f;
                default -> 0f;
            };
        }

        // Une entité prend la lumière du bloc où elle se trouve : dans un bloc opaque (tonneau),
        // elle serait toute noire. On la place alors sur la face du dessus et on décale le modèle.
        double entityY = block.getType().isOccluding() ? 1.0 : s.visualYOffset();
        float translateY = (float) (s.visualYOffset() - entityY);

        Location spawn = block.getLocation().add(0.5, entityY, 0.5);
        spawn.setYaw(yaw);

        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setItemModel(model);
        item.setItemMeta(meta);

        // Légèrement plus grand que le bloc pour le recouvrir sans scintillement
        float scale = s.visualScale() * 1.01f;
        return block.getWorld().spawn(spawn, ItemDisplay.class, d -> {
            d.setItemStack(item);
            d.setItemDisplayTransform(s.visualTransform());
            d.setTransformation(new Transformation(new Vector3f(0f, translateY, 0f), new AxisAngle4f(),
                    new Vector3f(scale, scale, scale), new AxisAngle4f()));
            d.setPersistent(true);
            d.setVisibleByDefault(visibleByDefault);
            PersistentDataContainer pdc = d.getPersistentDataContainer();
            pdc.set(plugin.keys().visualOwner, PersistentDataType.STRING, containerId);
            pdc.set(plugin.keys().visualState, PersistentDataType.STRING, state);
            pdc.set(plugin.keys().visualRevision, PersistentDataType.INTEGER, REVISION);
        });
    }

    /** Retire les habillages d'un conteneur détruit. */
    public void remove(Block block, String containerId) {
        BukkitTask anim = lidAnimations.remove(containerId);
        if (anim != null) anim.cancel();
        lidFrames.remove(containerId);
        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        for (ItemDisplay d : block.getWorld().getNearbyEntitiesByType(ItemDisplay.class, center, 1.5)) {
            if (containerId.equals(owner(d))) d.remove();
        }
        for (UUID id : new UUID[]{unopenedDisplays.remove(containerId), openedDisplays.remove(containerId)}) {
            if (id == null) continue;
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.remove();
        }
    }

    /** Conteneur détruit : on oublie aussi qui l'avait ouvert. */
    public void forget(String containerId) {
        for (Map<String, Long> map : openedByPlayer.values()) map.remove(containerId);
    }

    /** Supprime puis recrée tous les habillages des chunks chargés. Renvoie le nombre de conteneurs habillés. */
    public int refreshLoaded() {
        for (World world : Bukkit.getWorlds()) {
            for (ItemDisplay d : world.getEntitiesByClass(ItemDisplay.class)) {
                if (owner(d) != null) d.remove();
            }
        }
        unopenedDisplays.clear();
        openedDisplays.clear();

        int count = 0;
        if (!settings().visualEnabled()) return 0;
        for (World world : Bukkit.getWorlds()) {
            if (!settings().isWorldEnabled(world)) continue;
            for (Chunk chunk : world.getLoadedChunks()) count += ensureChunk(chunk);
        }
        return count;
    }

    /** Pose les habillages manquants des conteneurs à butin d'un chunk. */
    public int ensureChunk(Chunk chunk) {
        if (!settings().visualEnabled() || !settings().isWorldEnabled(chunk.getWorld())) return 0;
        int count = 0;
        for (BlockState state : chunk.getTileEntities(b -> settings().blockTypes().contains(b.getType()), false)) {
            LootData data = plugin.lootService().getData(state.getBlock());
            if (data != null) {
                ensure(state.getBlock(), data.id());
                count++;
            }
        }
        return count;
    }

    // ------------------------------------------------------------------ registre des entités chargées

    public void onEntitiesLoaded(Collection<? extends Entity> entities) {
        for (Entity e : entities) {
            if (!(e instanceof ItemDisplay d)) continue;
            String id = owner(d);
            if (id == null) continue;
            if (!settings().visualEnabled()) {
                d.remove();
                continue;
            }
            register(d);
            // Couvercle resté ouvert (arrêt brutal du serveur) : on le referme
            if (!lidFrames.containsKey(id)) setFrame(d, 0);
            applyToOnline(id);
        }
    }

    public void onEntitiesUnloaded(Collection<? extends Entity> entities) {
        for (Entity e : entities) {
            if (e instanceof ItemDisplay d) unregister(d);
        }
    }

    /** Démarrage du plugin : enregistre les habillages déjà chargés. */
    public void registerLoaded() {
        for (World world : Bukkit.getWorlds()) onEntitiesLoaded(world.getEntitiesByClass(ItemDisplay.class));
    }

    private void register(ItemDisplay d) {
        String id = owner(d);
        if (id == null) return;
        String state = state(d);
        if (UNOPENED.equals(state)) unopenedDisplays.put(id, d.getUniqueId());
        else if (OPENED.equals(state)) openedDisplays.put(id, d.getUniqueId());
    }

    private void unregister(ItemDisplay d) {
        String id = owner(d);
        if (id == null) return;
        unopenedDisplays.remove(id, d.getUniqueId());
        openedDisplays.remove(id, d.getUniqueId());
    }

    // ------------------------------------------------------------------ animation du couvercle

    /**
     * Ouvre ou referme le couvercle des deux habillages, une image par tick.
     * Une animation en cours est reprise depuis l'image où elle en est.
     */
    public void animateLid(String containerId, boolean open) {
        // Pendant l'arrêt du plugin, aucune tâche ne peut être planifiée (voir closeAllLids)
        if (!settings().visualEnabled() || !plugin.isEnabled()) return;
        BukkitTask previous = lidAnimations.remove(containerId);
        if (previous != null) previous.cancel();
        int target = open ? LID_FRAMES : 0;
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                int frame = lidFrames.getOrDefault(containerId, 0);
                if (frame == target) {
                    cancel();
                    BukkitTask current = lidAnimations.get(containerId);
                    if (current != null && current.getTaskId() == getTaskId()) lidAnimations.remove(containerId);
                    return;
                }
                frame += open ? 1 : -1;
                if (frame == 0) lidFrames.remove(containerId);
                else lidFrames.put(containerId, frame);
                for (UUID id : new UUID[]{unopenedDisplays.get(containerId), openedDisplays.get(containerId)}) {
                    if (entity(id) instanceof ItemDisplay d) setFrame(d, frame);
                }
            }
        };
        lidAnimations.put(containerId, runnable.runTaskTimer(plugin, 0L, 1L));
    }

    /** Arrêt du plugin : referme instantanément tous les couvercles. */
    public void closeAllLids() {
        lidAnimations.values().forEach(BukkitTask::cancel);
        lidAnimations.clear();
        for (String containerId : lidFrames.keySet()) {
            for (UUID id : new UUID[]{unopenedDisplays.get(containerId), openedDisplays.get(containerId)}) {
                if (entity(id) instanceof ItemDisplay d) setFrame(d, 0);
            }
        }
        lidFrames.clear();
    }

    /** L'image est choisie par le pack via custom_model_data (0 = fermé). */
    private static void setFrame(ItemDisplay display, int frame) {
        ItemStack item = display.getItemStack();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        CustomModelDataComponent cmd = meta.getCustomModelDataComponent();
        List<Float> floats = frame == 0 ? List.of() : List.of((float) frame);
        if (cmd.getFloats().equals(floats)) return;
        cmd.setFloats(floats);
        meta.setCustomModelDataComponent(cmd);
        item.setItemMeta(meta);
        display.setItemStack(item);
    }

    // ------------------------------------------------------------------ état par joueur

    /** Connexion : charge la liste des conteneurs déjà ouverts puis applique l'apparence. */
    public void loadPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        plugin.database().openedBy(uuid).thenAccept(map -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            Map<String, Long> current = openedByPlayer.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
            // Ne pas écraser ce qui a été ouvert pendant le chargement
            map.forEach(current::putIfAbsent);
            for (String id : current.keySet()) apply(player, id);
        }));
    }

    public void unloadPlayer(UUID uuid) {
        openedByPlayer.remove(uuid);
    }

    /** Le joueur vient d'ouvrir le conteneur. */
    public void markOpened(Player player, String containerId, long createdAt) {
        openedByPlayer.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>()).put(containerId, createdAt);
        apply(player, containerId);
    }

    /** Butin réinitialisé : le conteneur redevient « non ouvert » (pour tous si player == null). */
    public void markReset(String containerId, @Nullable UUID player) {
        for (Map.Entry<UUID, Map<String, Long>> entry : openedByPlayer.entrySet()) {
            if (player != null && !player.equals(entry.getKey())) continue;
            if (entry.getValue().remove(containerId) == null) continue;
            Player online = Bukkit.getPlayer(entry.getKey());
            if (online != null) apply(online, containerId);
        }
    }

    public boolean isOpened(UUID player, String containerId) {
        Map<String, Long> map = openedByPlayer.get(player);
        Long createdAt = map == null ? null : map.get(containerId);
        if (createdAt == null) return false;
        long refill = settings().refillMillis();
        return refill <= 0 || System.currentTimeMillis() - createdAt < refill;
    }

    /** Toutes les minutes : les butins régénérés repassent en « non ouvert ». */
    public void tickRefill() {
        long refill = settings().refillMillis();
        if (refill <= 0) return;
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Map<String, Long>> entry : openedByPlayer.entrySet()) {
            Player online = Bukkit.getPlayer(entry.getKey());
            List<String> expired = new ArrayList<>();
            Iterator<Map.Entry<String, Long>> it = entry.getValue().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Long> e = it.next();
                if (now - e.getValue() >= refill) {
                    expired.add(e.getKey());
                    it.remove();
                }
            }
            if (online != null) for (String id : expired) apply(online, id);
        }
    }

    private void applyToOnline(String containerId) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isOpened(p.getUniqueId(), containerId)) apply(p, containerId);
        }
    }

    /** Montre au joueur le bon modèle pour ce conteneur. */
    private void apply(Player player, String containerId) {
        Entity unopened = entity(unopenedDisplays.get(containerId));
        Entity opened = entity(openedDisplays.get(containerId));
        if (isOpened(player.getUniqueId(), containerId)) {
            if (unopened != null) player.hideEntity(plugin, unopened);
            if (opened != null) player.showEntity(plugin, opened);
        } else {
            if (unopened != null) player.showEntity(plugin, unopened);
            if (opened != null) player.hideEntity(plugin, opened);
        }
    }

    private static @Nullable Entity entity(@Nullable UUID id) {
        return id == null ? null : Bukkit.getEntity(id);
    }

    private @Nullable String owner(Entity e) {
        return e.getPersistentDataContainer().get(plugin.keys().visualOwner, PersistentDataType.STRING);
    }

    private @Nullable String state(Entity e) {
        return e.getPersistentDataContainer().get(plugin.keys().visualState, PersistentDataType.STRING);
    }
}
