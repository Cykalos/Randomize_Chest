package fr.cykalos.ramdomizechest.loot;

import org.bukkit.NamespacedKey;

/**
 * Données d'un conteneur à butin personnel, stockées dans son PersistentDataContainer.
 *
 * @param id    identifiant unique du conteneur (clé en base de données)
 * @param table table de loot d'origine (ex. minecraft:chests/simple_dungeon)
 * @param seed  graine d'origine générée par le monde (0 = aléatoire)
 */
public record LootData(String id, NamespacedKey table, long seed) {
}
