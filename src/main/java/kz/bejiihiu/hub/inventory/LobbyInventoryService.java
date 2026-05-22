package kz.bejiihiu.hub.inventory;

import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.bridge.BridgeDocProperties;
import kz.bejiihiu.hub.cloud.CloudNetFacade;
import kz.bejiihiu.hub.cloud.ServiceFilter;
import kz.bejiihiu.hub.config.SelectorConfig;
import kz.bejiihiu.hub.model.PlayerContext;
import kz.bejiihiu.hub.model.ServiceView;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Builds selector inventories from CloudNet service snapshots.
 */
public record LobbyInventoryService(JavaPlugin plugin, SelectorConfig config, CloudNetFacade cloudNetFacade,
                                    ServiceFilter serviceFilter, SelectorItemFactory itemFactory) {
    private static final int MIN_INVENTORY_SIZE = 9;
    private static final int MAX_INVENTORY_SIZE = 54;
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    /**
     * Creates inventory for one player context, including optional silent-lobby exposure.
     */
    public Inventory buildFor(PlayerContext context) {
        // Fail safe: keep UI openable even when CloudNet bridge dependencies are absent.
        if (this.cloudNetFacade.cloudServiceProvider().isEmpty() || this.cloudNetFacade.wrapperConfiguration().isEmpty()) {
            return fallbackInventory();
        }

        List<ServiceInfoSnapshot> services = this.collectServices(context.allowSilentLobby());
        int size = computeInventorySize(services.size());
        LobbyInventoryHolder holder = new LobbyInventoryHolder();
        Inventory inventory = this.plugin.getServer().createInventory(
                holder,
                size,
                LEGACY_SERIALIZER.deserialize(this.config.inventoryTitle())
        );
        holder.setInventory(inventory);

        List<ItemStack> items = services.stream()
                .map(this::toServiceView)
                .filter(Objects::nonNull)
                .filter(view -> !(this.config.hideFullServices() && view.players() >= view.maxPlayers() && !view.currentService()))
                .map(view -> this.itemFactory.create(view, this.serviceFilter.isSilentHubService(view.service())))
                .filter(Objects::nonNull)
                .limit(size)
                .toList();

        for (int slot = 0; slot < items.size(); slot++) {
            inventory.setItem(slot, items.get(slot));
        }

        return inventory;
    }

    /**
     * Minimal fallback inventory used when CloudNet runtime is unavailable.
     */
    private Inventory fallbackInventory() {
        LobbyInventoryHolder holder = new LobbyInventoryHolder();
        Inventory inventory = this.plugin.getServer().createInventory(
                holder,
                MIN_INVENTORY_SIZE,
                LEGACY_SERIALIZER.deserialize("&c&mLobby Selector")
        );
        holder.setInventory(inventory);
        return inventory;
    }

    /**
     * Collects services by configured task order and reverses list to preserve prior behavior.
     */
    private List<ServiceInfoSnapshot> collectServices(boolean allowSilentLobby) {
        List<ServiceInfoSnapshot> services = new ArrayList<>();
        if (allowSilentLobby && this.config.enableSilentLobby()) {
            services.addAll(this.cloudNetFacade.servicesByTask(this.config.silentLobbyTask()));
        }
        services.addAll(this.cloudNetFacade.servicesByTask(this.config.lobbyTask()));
        Collections.reverse(services);
        return services;
    }

    /**
     * Extracts rendering state and filters offline services out.
     */
    private ServiceView toServiceView(ServiceInfoSnapshot service) {
        if (!service.readProperty(BridgeDocProperties.IS_ONLINE)) {
            return null;
        }
        boolean current = this.cloudNetFacade.currentServiceName().map(name -> name.equals(service.name())).orElse(false);
        int players = service.readProperty(BridgeDocProperties.PLAYERS).size();
        int maxPlayers = service.readProperty(BridgeDocProperties.MAX_PLAYERS);
        return new ServiceView(service, current, players, maxPlayers);
    }

    /**
     * Keeps inventory size aligned to Bukkit row constraints (9..54).
     */
    private static int computeInventorySize(int serviceCount) {
        int inventorySize = ((serviceCount / MIN_INVENTORY_SIZE) + 1) * MIN_INVENTORY_SIZE;
        if (inventorySize > MAX_INVENTORY_SIZE) {
            return MAX_INVENTORY_SIZE;
        }
        return Math.max(inventorySize, MIN_INVENTORY_SIZE);
    }
}
