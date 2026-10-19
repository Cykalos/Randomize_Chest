package fr.cykalos.randomizechest.storage;

import java.io.File;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stockage SQLite des inventaires personnels.
 * Toutes les requêtes passent par un unique thread : l'ordre des opérations est garanti
 * (une sauvegarde puis une suppression s'exécutent toujours dans cet ordre).
 */
public final class LootDatabase {

    /** Contenu sauvegardé d'un joueur pour un conteneur. */
    public record StoredLoot(byte[] contents, long createdAt) {
    }

    private final Logger logger;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Randomize_Chest-DB");
        t.setDaemon(true);
        return t;
    });
    private Connection connection;

    public LootDatabase(Logger logger) {
        this.logger = logger;
    }

    public void open(File file) throws Exception {
        file.getParentFile().mkdirs();
        // Driver chargé via "libraries" (plugin.yml) : on l'instancie directement
        Driver driver = (Driver) Class.forName("org.sqlite.JDBC").getDeclaredConstructor().newInstance();
        CompletableFuture.runAsync(() -> {
            try {
                connection = driver.connect("jdbc:sqlite:" + file.getAbsolutePath(), new Properties());
                try (Statement st = connection.createStatement()) {
                    st.execute("PRAGMA journal_mode=WAL");
                    st.execute("PRAGMA synchronous=NORMAL");
                    st.execute("""
                            CREATE TABLE IF NOT EXISTS loot_inventories (
                                container_id TEXT NOT NULL,
                                player_uuid  TEXT NOT NULL,
                                contents     BLOB NOT NULL,
                                created_at   INTEGER NOT NULL,
                                updated_at   INTEGER NOT NULL,
                                PRIMARY KEY (container_id, player_uuid)
                            )""");
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, executor).join();
    }

    public CompletableFuture<StoredLoot> load(String containerId, UUID player) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT contents, created_at FROM loot_inventories WHERE container_id = ? AND player_uuid = ?")) {
                ps.setString(1, containerId);
                ps.setString(2, player.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? new StoredLoot(rs.getBytes(1), rs.getLong(2)) : null;
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Lecture impossible du butin " + containerId, e);
                throw new RuntimeException(e);
            }
        }, executor);
    }

    public CompletableFuture<Void> save(String containerId, UUID player, byte[] contents, long createdAt) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO loot_inventories (container_id, player_uuid, contents, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT(container_id, player_uuid)
                    DO UPDATE SET contents = excluded.contents,
                                  created_at = excluded.created_at,
                                  updated_at = excluded.updated_at""")) {
                ps.setString(1, containerId);
                ps.setString(2, player.toString());
                ps.setBytes(3, contents);
                ps.setLong(4, createdAt);
                ps.setLong(5, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Sauvegarde impossible du butin " + containerId, e);
            }
        }, executor);
    }

    /** Supprime le butin de tous les joueurs (null) ou d'un seul joueur. */
    public CompletableFuture<Integer> delete(String containerId, UUID playerOrNull) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = playerOrNull == null
                    ? "DELETE FROM loot_inventories WHERE container_id = ?"
                    : "DELETE FROM loot_inventories WHERE container_id = ? AND player_uuid = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, containerId);
                if (playerOrNull != null) ps.setString(2, playerOrNull.toString());
                return ps.executeUpdate();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Suppression impossible du butin " + containerId, e);
                return 0;
            }
        }, executor);
    }

    public CompletableFuture<Integer> countPlayers(String containerId) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT COUNT(*) FROM loot_inventories WHERE container_id = ?")) {
                ps.setString(1, containerId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            } catch (SQLException e) {
                return 0;
            }
        }, executor);
    }

    /** Termine les écritures en attente puis ferme la connexion. */
    public void close() {
        executor.execute(() -> {
            try {
                if (connection != null) connection.close();
            } catch (SQLException ignored) {
            }
        });
        executor.shutdown();
        try {
            if (!executor.awaitTermination(15, TimeUnit.SECONDS)) {
                logger.warning("La base de données n'a pas fini ses écritures à temps.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
