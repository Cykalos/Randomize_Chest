package fr.cykalos.randomizechest.visual;

import fr.cykalos.randomizechest.RandomizeChestPlugin;
import fr.cykalos.randomizechest.PluginSettings;
import fr.cykalos.randomizechest.loot.LootData;
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
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
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
 * Chaque conteneur porte des ItemDisplay superposés, pour deux états :
 * <ul>
 *   <li>« non ouvert » : visibles par défaut ;</li>
 *   <li>« déjà ouvert » : cachés par défaut.</li>
 * </ul>
 * Pour un joueur qui a déjà ouvert le conteneur, les premiers sont cachés et les seconds montrés,
 * grâce à la visibilité d'entités par joueur de Paper.
 * <p>
 * Les coffres sont en deux parties (corps + couvercle). Le couvercle a sa charnière à l'origine de
 * son entité : il pivote par une transformation que le client interpole lui-même, image par image.
 */
public final class VisualManager {

    private static final String UNOPENED = "unopened";
    private static final String OPENED = "opened";
    private static final String[] STATES = {UNOPENED, OPENED};
    private static final String BODY = "body";
    private static final String LID = "lid";

    /** À augmenter quand la position, l'orientation, la taille ou la découpe des habillages change. */
    private static final int REVISION = 5;

    /** Durée de l'ouverture en ticks (0,5 s, comme en vanilla). */
    private static final int LID_TICKS = 10;

    private final RandomizeChestPlugin plugin;

    /** Displays chargés : « id|état|partie » → UUID de l'entité. */
    private final Map<String, UUID> displays = new HashMap<>();

    /** Joueurs connectés : conteneurs déjà ouverts (id → date du tirage). */
    private final Map<UUID, Map<String, Long>> openedByPlayer = new ConcurrentHashMap<>();

    /** Avancement du couvercle par conteneur, en ticks de 0 à 10 (absent = fermé), et animations en cours. */
    private final Map<String, Integer> lidTicks = new HashMap<>();
    private final Map<String, BukkitTask> lidAnimations = new HashMap<>();

    public VisualManager(RandomizeChestPlugin plugin) {
        this.plugin = plugin;
    }

    private PluginSettings settings() {
        return plugin.settings();
    }

    private static String key(String containerId, String state, String part) {
        return containerId + "|" + state + "|" + part;
    }

    private static boolean hasLid(Material type) {
        return type == Material.CHEST || type == Material.TRAPPED_CHEST;
    }

    // ------------------------------------------------------------------ création / suppression

    /** Pose les habillages qui manquent (et remplace ceux d'anciennes versions). */
    public void ensure(Block block, String containerId) {
        PluginSettings s = settings();
        if (!s.visualEnabled() || !block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)) return;
        boolean lid = hasLid(block.getType());

        Map<String, ItemDisplay> found = new HashMap<>();
        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        for (ItemDisplay d : block.getWorld().getNearbyEntitiesByType(ItemDisplay.class, center, 1.5)) {
            if (!d.isValid() || !containerId.equals(owner(d))) continue;
            boolean current = Integer.valueOf(REVISION).equals(d.getPersistentDataContainer()
                    .get(plugin.keys().visualRevision, PersistentDataType.INTEGER));
            String k = key(containerId, String.valueOf(state(d)), String.valueOf(part(d)));
            if (current && !found.containsKey(k) && (lid || BODY.equals(part(d)))) {
                found.put(k, d);
            } else {
                // Habillage d'une version précédente, ou doublon : recréé avec les réglages actuels
                unregister(d);
                d.remove();
            }
        }

        PluginSettings.VisualModels models = s.modelsFor(block.getType());
        for (String state : STATES) {
            NamespacedKey model = UNOPENED.equals(state) ? models.unopened() : models.opened();
            boolean visible = UNOPENED.equals(state);
            String bodyKey = key(containerId, state, BODY);
            ItemDisplay body = found.get(bodyKey);
            if (body == null) body = spawn(block, containerId, state, BODY, model, visible);
            register(body);
            if (lid) {
                ItemDisplay lidDisplay = found.get(key(containerId, state, LID));
                if (lidDisplay == null) {
                    NamespacedKey lidModel = new NamespacedKey(model.getNamespace(), model.getKey() + "_lid");
                    lidDisplay = spawn(block, containerId, state, LID, lidModel, visible);
                    // Coffre ouvert à ce moment-là : le nouveau couvercle prend la même position
                    if (lidTicks.containsKey(containerId)) setLid(lidDisplay, eased(lidTicks.get(containerId)), 0);
                }
                register(lidDisplay);
            }
        }
        applyToOnline(containerId);
    }

    private ItemDisplay spawn(Block block, String containerId, String state, String part,
                              NamespacedKey model, boolean visibleByDefault) {
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
        // Légèrement plus grand que le bloc pour le recouvrir sans scintillement. Le couvercle
        // l'est un peu plus, pour cacher le vrai couvercle qui s'ouvre en même temps à l'intérieur.
        float scale = s.visualScale() * (LID.equals(part) ? 1.015f : 1.01f);

        Location spawn;
        float translateY = 0f;
        if (LID.equals(part)) {
            // Charnière du couvercle vanilla : 1 px au-dessus du centre, 7 px vers l'arrière
            double rad = Math.toRadians(yaw);
            Vector back = new Vector(-Math.sin(rad), 0, Math.cos(rad)).multiply(7.0 * scale / 16.0);
            spawn = block.getLocation().add(0.5, s.visualYOffset() + scale / 16.0, 0.5).add(back);
        } else {
            // Une entité prend la lumière du bloc où elle se trouve : dans un bloc opaque (tonneau),
            // elle serait toute noire. On la place alors sur la face du dessus et on décale le modèle.
            double entityY = block.getType().isOccluding() ? 1.0 : s.visualYOffset();
            translateY = (float) (s.visualYOffset() - entityY);
            spawn = block.getLocation().add(0.5, entityY, 0.5);
        }
        spawn.setYaw(yaw);

        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setItemModel(model);
        item.setItemMeta(meta);

        float ty = translateY;
        return block.getWorld().spawn(spawn, ItemDisplay.class, d -> {
            d.setItemStack(item);
            d.setItemDisplayTransform(s.visualTransform());
            d.setTransformation(new Transformation(new Vector3f(0f, ty, 0f), new AxisAngle4f(),
                    new Vector3f(scale, scale, scale), new AxisAngle4f()));
            d.setPersistent(true);
            d.setVisibleByDefault(visibleByDefault);
            PersistentDataContainer pdc = d.getPersistentDataContainer();
            pdc.set(plugin.keys().visualOwner, PersistentDataType.STRING, containerId);
            pdc.set(plugin.keys().visualState, PersistentDataType.STRING, state);
            pdc.set(plugin.keys().visualPart, PersistentDataType.STRING, part);
            pdc.set(plugin.keys().visualRevision, PersistentDataType.INTEGER, REVISION);
        });
    }

    /** Retire les habillages d'un conteneur détruit. */
    public void remove(Block block, String containerId) {
        BukkitTask anim = lidAnimations.remove(containerId);
        if (anim != null) anim.cancel();
        lidTicks.remove(containerId);
        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        for (ItemDisplay d : block.getWorld().getNearbyEntitiesByType(ItemDisplay.class, center, 1.5)) {
            if (containerId.equals(owner(d))) d.remove();
        }
        for (Entity e : all(containerId)) e.remove();
        displays.keySet().removeIf(k -> k.startsWith(containerId + "|"));
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
        displays.clear();

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
            if (LID.equals(part(d)) && !lidTicks.containsKey(id)) setLid(d, 0f, 0);
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
        String state = state(d);
        String part = part(d);
        if (id == null || state == null || part == null) return;
        displays.put(key(id, state, part), d.getUniqueId());
    }

    private void unregister(ItemDisplay d) {
        String id = owner(d);
        String state = state(d);
        String part = part(d);
        if (id == null || state == null || part == null) return;
        displays.remove(key(id, state, part), d.getUniqueId());
    }

    private List<Entity> all(String containerId) {
        List<Entity> out = new ArrayList<>();
        for (String state : STATES) out.addAll(ofState(containerId, state));
        return out;
    }

    private List<Entity> ofState(String containerId, String state) {
        List<Entity> out = new ArrayList<>(2);
        for (String part : new String[]{BODY, LID}) {
            Entity e = entity(displays.get(key(containerId, state, part)));
            if (e != null) out.add(e);
        }
        return out;
    }

    private List<ItemDisplay> lids(String containerId) {
        List<ItemDisplay> out = new ArrayList<>(2);
        for (String state : STATES) {
            if (entity(displays.get(key(containerId, state, LID))) instanceof ItemDisplay d) out.add(d);
        }
        return out;
    }

    // ------------------------------------------------------------------ animation du couvercle

    /**
     * Ouvre ou referme le couvercle. Le serveur n'envoie que quelques étapes ; entre deux étapes,
     * c'est le client qui fait pivoter le couvercle de façon fluide (interpolation des Display).
     * Une animation en cours est reprise depuis l'endroit où elle en est.
     */
    public void animateLid(String containerId, boolean open) {
        // Pendant l'arrêt du plugin, aucune tâche ne peut être planifiée (voir closeAllLids)
        if (!settings().visualEnabled() || !plugin.isEnabled()) return;
        BukkitTask previous = lidAnimations.remove(containerId);
        if (previous != null) previous.cancel();
        int target = open ? LID_TICKS : 0;
        int direction = open ? 1 : -1;
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                int t = lidTicks.getOrDefault(containerId, 0);
                if (t == target) {
                    cancel();
                    BukkitTask current = lidAnimations.get(containerId);
                    if (current != null && current.getTaskId() == getTaskId()) lidAnimations.remove(containerId);
                    return;
                }
                // Une étape par tick, atteinte au tick suivant : exactement le rythme du couvercle
                // vanilla, qui avance de 1/10 par tick. Le client lisse le mouvement entre deux ticks.
                int next = t + direction;
                for (ItemDisplay d : lids(containerId)) setLid(d, eased(next), 1);
                if (next == 0) lidTicks.remove(containerId);
                else lidTicks.put(containerId, next);
            }
        };
        lidAnimations.put(containerId, runnable.runTaskTimer(plugin, 0L, 1L));
    }

    /** Ouverture du couvercle (0 à 1) après t ticks, avec la même courbe qu'en vanilla. */
    private static float eased(int t) {
        float x = 1f - (float) t / LID_TICKS;
        return 1f - x * x * x;
    }

    /** Fait pivoter le couvercle autour de sa charnière (0 = fermé, 1 = ouvert à 90°). */
    private void setLid(ItemDisplay d, float openness, int durationTicks) {
        Transformation current = d.getTransformation();
        Vector3f scale = current.getScale();
        d.setInterpolationDelay(0);
        d.setInterpolationDuration(durationTicks);
        d.setTransformation(new Transformation(new Vector3f(),
                new AxisAngle4f((float) (Math.PI / 2 * openness), 1f, 0f, 0f),
                scale, new AxisAngle4f()));
    }

    /** Arrêt du plugin : referme instantanément tous les couvercles. */
    public void closeAllLids() {
        lidAnimations.values().forEach(BukkitTask::cancel);
        lidAnimations.clear();
        for (String containerId : lidTicks.keySet()) {
            for (ItemDisplay d : lids(containerId)) setLid(d, 0f, 0);
        }
        lidTicks.clear();
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

    /** Montre au joueur les bons modèles pour ce conteneur. */
    private void apply(Player player, String containerId) {
        boolean opened = isOpened(player.getUniqueId(), containerId);
        for (Entity e : ofState(containerId, opened ? OPENED : UNOPENED)) player.showEntity(plugin, e);
        for (Entity e : ofState(containerId, opened ? UNOPENED : OPENED)) player.hideEntity(plugin, e);
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

    private @Nullable String part(Entity e) {
        return e.getPersistentDataContainer().getOrDefault(plugin.keys().visualPart, PersistentDataType.STRING, BODY);
    }
}
