package fr.cykalos.ramdomizechest;

import fr.cykalos.ramdomizechest.command.LootCommand;
import fr.cykalos.ramdomizechest.listener.LootListener;
import fr.cykalos.ramdomizechest.listener.ProtectionListener;
import fr.cykalos.ramdomizechest.loot.LootContainerService;
import fr.cykalos.ramdomizechest.loot.LootSessionManager;
import fr.cykalos.ramdomizechest.storage.LootDatabase;
import fr.cykalos.ramdomizechest.visual.VisualManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

public final class RamdomizeChestPlugin extends JavaPlugin {

    private volatile PluginSettings settings;
    private Keys keys;
    private LootDatabase database;
    private LootContainerService lootService;
    private LootSessionManager sessions;
    private VisualManager visuals;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = PluginSettings.load(getConfig(), getLogger());
        keys = new Keys(this);

        database = new LootDatabase(getLogger());
        try {
            database.open(new File(getDataFolder(), "data.db"));
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Impossible d'ouvrir la base SQLite, désactivation du plugin.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        lootService = new LootContainerService(this);
        sessions = new LootSessionManager(this);
        visuals = new VisualManager(this);

        getServer().getPluginManager().registerEvents(new LootListener(this), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);

        PluginCommand command = getCommand("ramdomizechest");
        if (command != null) {
            LootCommand executor = new LootCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        // Chunks déjà chargés au démarrage (ex. spawn)
        int converted = lootService.scanLoaded();
        getLogger().info("Ramdomize_Chest activé — " + converted + " conteneur(s) converti(s) au démarrage.");
    }

    @Override
    public void onDisable() {
        if (sessions != null) sessions.closeAll();
        if (database != null) database.close();
    }

    public void reloadSettings() {
        reloadConfig();
        settings = PluginSettings.load(getConfig(), getLogger());
    }

    public PluginSettings settings() {
        return settings;
    }

    public Keys keys() {
        return keys;
    }

    public LootDatabase database() {
        return database;
    }

    public LootContainerService lootService() {
        return lootService;
    }

    public LootSessionManager sessions() {
        return sessions;
    }

    public VisualManager visuals() {
        return visuals;
    }
}
