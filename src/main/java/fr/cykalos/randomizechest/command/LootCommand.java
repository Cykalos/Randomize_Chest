package fr.cykalos.randomizechest.command;

import fr.cykalos.randomizechest.RandomizeChestPlugin;
import fr.cykalos.randomizechest.PluginSettings;
import fr.cykalos.randomizechest.loot.LootData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** /rchest reload | info | reset [joueur] | scan | visuals refresh */
public final class LootCommand implements TabExecutor {

    private static final String PERM_ADMIN = "randomizechest.admin";
    private static final List<String> SUBS = List.of("reload", "info", "reset", "scan", "visuals");

    private final RandomizeChestPlugin plugin;

    public LootCommand(RandomizeChestPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        PluginSettings s = plugin.settings();
        if (!sender.hasPermission(PERM_ADMIN)) {
            sender.sendMessage(s.msg("no-permission"));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(s.msg("usage"));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.reloadSettings();
                // Applique les nouveaux modèles (ou retire les habillages si désactivés)
                plugin.visuals().refreshLoaded();
                sender.sendMessage(plugin.settings().msg("reloaded"));
            }
            case "scan" -> sender.sendMessage(s.msg("scan-done",
                    "count", String.valueOf(plugin.lootService().scanLoaded())));
            case "visuals" -> {
                if (!s.visualEnabled()) sender.sendMessage(s.msg("visuals-disabled"));
                int count = plugin.visuals().refreshLoaded();
                if (s.visualEnabled()) sender.sendMessage(s.msg("visuals-refreshed", "count", String.valueOf(count)));
            }
            case "info" -> {
                LootData data = target(sender);
                if (data == null) {
                    sender.sendMessage(s.msg("not-a-loot-container"));
                    return true;
                }
                plugin.database().countPlayers(data.id()).thenAccept(count ->
                        Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(s.msg("info",
                                "id", data.id(), "table", data.table().toString(), "count", String.valueOf(count)))));
            }
            case "reset" -> {
                LootData data = target(sender);
                if (data == null) {
                    sender.sendMessage(s.msg("not-a-loot-container"));
                    return true;
                }
                if (args.length < 2) {
                    plugin.sessions().reset(data.id(), null);
                    sender.sendMessage(s.msg("reset-all"));
                    return true;
                }
                UUID uuid = resolvePlayer(args[1]);
                if (uuid == null) {
                    sender.sendMessage(s.msg("unknown-player", "player", args[1]));
                    return true;
                }
                plugin.sessions().reset(data.id(), uuid);
                sender.sendMessage(s.msg("reset-player", "player", args[1]));
            }
            default -> sender.sendMessage(s.msg("usage"));
        }
        return true;
    }

    /** Conteneur visé par le joueur (bloc ou wagonnet, 6 blocs max). */
    private @Nullable LootData target(CommandSender sender) {
        if (!(sender instanceof Player player)) return null;
        Entity entity = player.getTargetEntity(6);
        if (entity != null) {
            LootData data = plugin.lootService().getOrConvert(entity);
            if (data != null) return data;
        }
        Block block = player.getTargetBlockExact(6);
        return block == null ? null : plugin.lootService().getOrConvert(block);
    }

    private static @Nullable UUID resolvePlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online.getUniqueId();
        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(name);
        return cached == null ? null : cached.getUniqueId();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(PERM_ADMIN)) return List.of();
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String sub : SUBS) if (sub.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(sub);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("visuals")) {
            out.add("refresh");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(p.getName());
            }
        }
        return out;
    }
}
