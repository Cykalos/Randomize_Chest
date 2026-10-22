package fr.cykalos.randomizechest;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/** Clés des PersistentDataContainer utilisées par le plugin. */
public final class Keys {

    /** Identifiant unique d'un conteneur à butin. */
    public final NamespacedKey containerId;
    /** Table de loot d'origine. */
    public final NamespacedKey lootTable;
    /** Graine d'origine. */
    public final NamespacedKey lootSeed;
    /** Marqueur posé sur les conteneurs placés par un joueur (jamais convertis). */
    public final NamespacedKey playerPlaced;
    /** Marqueur des ItemDisplay d'habillage (valeur = id du conteneur). */
    public final NamespacedKey visualOwner;
    /** État représenté par un ItemDisplay : "unopened" ou "opened". */
    public final NamespacedKey visualState;
    /** Partie représentée par un ItemDisplay : "body" ou "lid" (couvercle). */
    public final NamespacedKey visualPart;
    /** Révision de l'habillage : une révision plus ancienne est recréée. */
    public final NamespacedKey visualRevision;

    public Keys(Plugin plugin) {
        containerId = new NamespacedKey(plugin, "container_id");
        lootTable = new NamespacedKey(plugin, "loot_table");
        lootSeed = new NamespacedKey(plugin, "loot_seed");
        playerPlaced = new NamespacedKey(plugin, "player_placed");
        visualOwner = new NamespacedKey(plugin, "visual_owner");
        visualState = new NamespacedKey(plugin, "visual_state");
        visualPart = new NamespacedKey(plugin, "visual_part");
        visualRevision = new NamespacedKey(plugin, "visual_revision");
    }
}
