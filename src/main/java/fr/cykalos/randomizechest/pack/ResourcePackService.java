package fr.cykalos.randomizechest.pack;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import fr.cykalos.randomizechest.RandomizeChestPlugin;
import fr.cykalos.randomizechest.PluginSettings;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Pack de ressources du plugin.
 * <ul>
 *   <li>Le pack embarqué dans le .jar est extrait dans {@code plugins/Randomize_Chest/resourcepack/}
 *       (sans écraser les fichiers déjà présents : vos textures restent les vôtres).</li>
 *   <li>Ce dossier est compressé en {@code Randomize_Chest-pack.zip}, utilisable aussi pour une fusion (RSPM...).</li>
 *   <li>En mode {@code builtin}, un petit serveur HTTP intégré sert ce zip et le pack est envoyé
 *       à chaque joueur à la connexion, en plus des autres packs du serveur.</li>
 * </ul>
 */
public final class ResourcePackService {

    public static final String ZIP_NAME = "Randomize_Chest-pack.zip";
    private static final String JAR_PREFIX = "resourcepack/";
    /** Identifiant fixe : un nouvel envoi remplace l'ancien pack du plugin sans toucher aux autres. */
    private static final UUID PACK_ID = UUID.nameUUIDFromBytes("randomize_chest:resourcepack".getBytes(StandardCharsets.UTF_8));
    /** Date fixe dans le zip : même contenu = même empreinte = pas de re-téléchargement. */
    private static final FileTime FIXED_TIME = FileTime.fromMillis(946684800000L);
    /**
     * Révision des modèles du pack embarqué. Quand elle augmente, les fichiers .json / .mcmeta
     * extraits sont mis à jour ; les textures (.png) présentes ne sont jamais écrasées.
     */
    private static final int PACK_REVISION = 2;
    private static final String REVISION_FILE = ".randomize_chest_revision";
    /** Fichiers devenus inutiles, supprimés lors de la mise à jour vers la révision 2. */
    private static final List<String> OBSOLETE = List.of(
            "assets/randomize_chest/models/item/loot_chest.json",
            "assets/randomize_chest/models/item/loot_chest_opened.json",
            "assets/randomize_chest/models/item/loot_trapped_chest.json",
            "assets/randomize_chest/models/item/loot_trapped_chest_opened.json",
            "assets/randomize_chest/textures/item/loot_chest.png",
            "assets/randomize_chest/textures/item/loot_chest_opened.png",
            "assets/randomize_chest/textures/item/loot_trapped_chest.png",
            "assets/randomize_chest/textures/item/loot_trapped_chest_opened.png");

    private final RandomizeChestPlugin plugin;
    private final File jarFile;
    private final File folder;
    private final File zipFile;

    private volatile byte[] zipBytes;
    private volatile String sha1;
    private HttpServer server;
    private ExecutorService executor;
    private boolean warnedNoHost;

    public ResourcePackService(RandomizeChestPlugin plugin, File jarFile) {
        this.plugin = plugin;
        this.jarFile = jarFile;
        this.folder = new File(plugin.getDataFolder(), "resourcepack");
        this.zipFile = new File(plugin.getDataFolder(), ZIP_NAME);
    }

    private PluginSettings settings() {
        return plugin.settings();
    }

    // ------------------------------------------------------------------ cycle de vie

    public void start() {
        extractMissing();
        build();
        startServer();
    }

    /** /rchest reload : reconstruit le zip, redémarre le serveur et renvoie le pack s'il a changé. */
    public void reload() {
        String previous = sha1;
        extractMissing();
        build();
        stopServer();
        startServer();
        if (isActive() && sha1 != null && !sha1.equals(previous)) sendAll();
    }

    public void stop() {
        stopServer();
    }

    public boolean isActive() {
        return settings().packMode() == PluginSettings.PackMode.BUILTIN && settings().visualEnabled() && server != null;
    }

    // ------------------------------------------------------------------ fichiers

    /**
     * Copie depuis le .jar les fichiers du pack qui manquent sur le disque.
     * Après une mise à jour des modèles, les .json / .mcmeta sont remplacés (jamais les .png).
     */
    private void extractMissing() {
        int copied = 0;
        File revisionFile = new File(folder, REVISION_FILE);
        int installed = readRevision(revisionFile);
        boolean upgrade = folder.isDirectory() && installed < PACK_REVISION;
        if (upgrade && installed < 2) {
            for (String obsolete : OBSOLETE) new File(folder, obsolete).delete();
        }
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith(JAR_PREFIX) || entry.isDirectory()) continue;
                File target = new File(folder, name.substring(JAR_PREFIX.length()));
                if (!target.toPath().normalize().startsWith(folder.toPath().normalize())) continue;
                boolean replace = upgrade && !name.toLowerCase().endsWith(".png");
                if (target.exists() && !replace) continue;
                target.getParentFile().mkdirs();
                try (InputStream in = jar.getInputStream(entry)) {
                    Files.copy(in, target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    copied++;
                }
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Extraction du pack de ressources impossible", e);
        }
        if (copied > 0) plugin.getLogger().info(copied + " fichier(s) du pack de ressources extrait(s) dans " + folder.getPath());
        if (installed < PACK_REVISION) {
            try {
                folder.mkdirs();
                Files.writeString(revisionFile.toPath(), String.valueOf(PACK_REVISION));
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Écriture de " + REVISION_FILE + " impossible", e);
            }
        }
    }

    private static int readRevision(File file) {
        try {
            return Integer.parseInt(Files.readString(file.toPath()).trim());
        } catch (IOException | NumberFormatException e) {
            return 1; // pack extrait par la version 1.2.0 (ou dossier absent)
        }
    }

    /** Compresse le dossier du pack et calcule son empreinte SHA-1. */
    private void build() {
        Path root = folder.toPath();
        if (!Files.isDirectory(root)) return;
        try {
            List<Path> files = new ArrayList<>();
            try (Stream<Path> walk = Files.walk(root)) {
                walk.filter(Files::isRegularFile).filter(ResourcePackService::isPackFile).sorted().forEach(files::add);
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(buffer)) {
                for (Path file : files) {
                    String name = root.relativize(file).toString().replace(File.separatorChar, '/');
                    ZipEntry entry = new ZipEntry(name);
                    entry.setLastModifiedTime(FIXED_TIME);
                    zip.putNextEntry(entry);
                    Files.copy(file, zip);
                    zip.closeEntry();
                }
            }
            byte[] bytes = buffer.toByteArray();
            Files.write(zipFile.toPath(), bytes);
            zipBytes = bytes;
            sha1 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes));
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Création du pack de ressources impossible", e);
        }
    }

    private static boolean isPackFile(Path p) {
        String name = p.getFileName().toString();
        return !name.startsWith(".") && !name.equalsIgnoreCase("Thumbs.db") && !name.toLowerCase().endsWith(".md");
    }

    // ------------------------------------------------------------------ serveur HTTP

    private void startServer() {
        PluginSettings s = settings();
        if (s.packMode() != PluginSettings.PackMode.BUILTIN || !s.visualEnabled() || zipBytes == null) return;
        try {
            server = HttpServer.create(new InetSocketAddress(s.packBind(), s.packPort()), 16);
            server.createContext("/", this::handle);
            executor = Executors.newFixedThreadPool(2, r -> {
                Thread t = new Thread(r, "Randomize_Chest-Pack");
                t.setDaemon(true);
                return t;
            });
            server.setExecutor(executor);
            server.start();
            plugin.getLogger().info("Pack de ressources servi sur le port " + s.packPort()
                    + " (pensez à ouvrir ce port en TCP dans votre pare-feu / panel).");
        } catch (IOException | RuntimeException | LinkageError e) {
            server = null;
            plugin.getLogger().log(Level.SEVERE, "Impossible de démarrer le serveur du pack sur le port "
                    + s.packPort() + ". Changez resource-pack.port ou passez resource-pack.mode à disabled.", e);
        }
    }

    private void stopServer() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    /** Ne sert que le zip du pack (GET/HEAD), tout le reste répond 404. */
    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            String method = exchange.getRequestMethod();
            byte[] bytes = zipBytes;
            boolean ok = bytes != null && ("/" + ZIP_NAME).equals(exchange.getRequestURI().getPath())
                    && ("GET".equals(method) || "HEAD".equals(method));
            if (!ok) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            exchange.getResponseHeaders().set("Content-Type", "application/zip");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            if ("HEAD".equals(method)) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
    }

    // ------------------------------------------------------------------ envoi aux joueurs

    public void send(Player player) {
        if (!isActive() || sha1 == null) return;
        String url = url(player);
        if (url == null) {
            if (!warnedNoHost) {
                warnedNoHost = true;
                plugin.getLogger().warning("Adresse du serveur inconnue : renseignez resource-pack.public-host dans config.yml.");
            }
            return;
        }
        ResourcePackInfo info = ResourcePackInfo.resourcePackInfo(PACK_ID, URI.create(url), sha1);
        ResourcePackRequest request = ResourcePackRequest.resourcePackRequest()
                .packs(info)
                .replace(false) // s'ajoute aux autres packs du serveur au lieu de les remplacer
                .required(settings().packRequired())
                .prompt(settings().packPrompt())
                .build();
        player.sendResourcePacks(request);
    }

    public void sendAll() {
        for (Player p : Bukkit.getOnlinePlayers()) send(p);
    }

    public boolean isOurPack(UUID id) {
        return PACK_ID.equals(id);
    }

    /** Adresse publique du pack, ou null si aucune adresse n'est connue. */
    public @Nullable String url(@Nullable Player player) {
        PluginSettings s = settings();
        String host = s.packPublicHost();
        if (host.isBlank() && player != null && player.getVirtualHost() != null) {
            // Adresse que le joueur a tapée pour se connecter (domaine ou IP)
            host = player.getVirtualHost().getHostString();
            int nul = host.indexOf('\0');
            if (nul >= 0) host = host.substring(0, nul);
            if (host.endsWith(".")) host = host.substring(0, host.length() - 1);
        }
        if (host.isBlank()) host = Bukkit.getIp();
        if (host == null || host.isBlank()) return null;
        if (host.contains(":") && !host.startsWith("[")) host = "[" + host + "]"; // IPv6
        String shortHash = sha1 == null ? "0" : sha1.substring(0, 8);
        return "http://" + host + ":" + s.packPublicPort() + "/" + ZIP_NAME + "?v=" + shortHash;
    }

    // ------------------------------------------------------------------ infos (/rchest pack)

    public @Nullable String sha1() {
        return sha1;
    }

    public int size() {
        byte[] b = zipBytes;
        return b == null ? 0 : b.length;
    }

    public File folder() {
        return folder;
    }

    public File zipFile() {
        return zipFile;
    }
}
