package fr.cykalos.ramdomizechest.loot;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Holder de l'inventaire virtuel propre à un joueur pour un conteneur donné. */
public final class LootInventoryHolder implements InventoryHolder {

    private final LootData data;
    private final UUID playerId;
    /** Position du bloc (pour l'animation du couvercle), null pour un wagonnet. */
    private final @Nullable Location blockLocation;
    private final long createdAt;
    private Inventory inventory;

    public LootInventoryHolder(LootData data, UUID playerId, @Nullable Location blockLocation, long createdAt) {
        this.data = data;
        this.playerId = playerId;
        this.blockLocation = blockLocation;
        this.createdAt = createdAt;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public LootData data() {
        return data;
    }

    public UUID playerId() {
        return playerId;
    }

    public @Nullable Location blockLocation() {
        return blockLocation;
    }

    public long createdAt() {
        return createdAt;
    }
}
