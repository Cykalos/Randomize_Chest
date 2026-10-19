package fr.cykalos.randomizechest;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ItemDisplay;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/** Valeurs lues depuis config.yml (immuable, rechargée en bloc). */
public record PluginSettings(
        boolean worldWhitelist,
        Set<String> worlds,
        Set<Material> blockTypes,
        boolean minecarts,
        SeedMode seedMode,
        long refillMillis,
        String inventoryTitle,
        boolean protectBreak,
        boolean protectExplosions,
        boolean protectHoppers,
        boolean visualEnabled,
        String visualModel,
        ItemDisplay.ItemDisplayTransform visualTransform,
        float visualScale,
        double visualYOffset,
        Map<String, String> messages
) {

    public enum SeedMode { RANDOM, PER_PLAYER, SHARED }

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static PluginSettings load(FileConfiguration cfg, Logger log) {
        boolean whitelist = "whitelist".equalsIgnoreCase(cfg.getString("worlds.mode", "blacklist"));
        Set<String> worlds = new HashSet<>();
        for (String w : cfg.getStringList("worlds.list")) worlds.add(w.toLowerCase(Locale.ROOT));

        Set<Material> types = EnumSet.noneOf(Material.class);
        for (String name : cfg.getStringList("containers.blocks")) {
            Material m = Material.matchMaterial(name);
            if (m == null || !m.isBlock()) {
                log.warning("Type de bloc inconnu dans containers.blocks : " + name);
                continue;
            }
            types.add(m);
        }

        SeedMode seedMode;
        try {
            seedMode = SeedMode.valueOf(cfg.getString("loot.seed-mode", "random")
                    .toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException e) {
            log.warning("loot.seed-mode invalide, utilisation de 'random'.");
            seedMode = SeedMode.RANDOM;
        }

        ItemDisplay.ItemDisplayTransform transform;
        try {
            transform = ItemDisplay.ItemDisplayTransform.valueOf(
                    cfg.getString("visual.transform", "NONE").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            transform = ItemDisplay.ItemDisplayTransform.NONE;
        }

        Map<String, String> messages = new HashMap<>();
        ConfigurationSection sec = cfg.getConfigurationSection("messages");
        if (sec != null) {
            for (String key : sec.getKeys(false)) messages.put(key, sec.getString(key, ""));
        }

        return new PluginSettings(
                whitelist,
                worlds,
                types,
                cfg.getBoolean("containers.minecarts", true),
                seedMode,
                Math.max(0L, cfg.getLong("loot.refill-hours", 0L)) * 3_600_000L,
                cfg.getString("loot.inventory-title", "Butin personnel"),
                cfg.getBoolean("protection.break", true),
                cfg.getBoolean("protection.explosions", true),
                cfg.getBoolean("protection.hoppers", true),
                cfg.getBoolean("visual.enabled", false),
                cfg.getString("visual.item-model", "randomize_chest:loot_chest"),
                transform,
                (float) cfg.getDouble("visual.scale", 1.0),
                cfg.getDouble("visual.y-offset", 0.5),
                messages
        );
    }

    public boolean isWorldEnabled(World world) {
        boolean listed = worlds.contains(world.getName().toLowerCase(Locale.ROOT));
        return worldWhitelist == listed;
    }

    public Component title() {
        return MM.deserialize(inventoryTitle);
    }

    /** Message avec préfixe. Les placeholders sont passés par paires clé/valeur. */
    public Component msg(String key, String... placeholders) {
        String raw = messages.getOrDefault(key, key);
        TagResolver.Builder resolvers = TagResolver.builder();
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            resolvers.resolver(Placeholder.unparsed(placeholders[i], placeholders[i + 1]));
        }
        return MM.deserialize(messages.getOrDefault("prefix", "") + raw, resolvers.build());
    }
}
