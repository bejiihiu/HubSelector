package kz.bejiihiu.hub.bootstrap;

import kz.bejiihiu.hub.cloud.CloudNetFacade;
import kz.bejiihiu.hub.cloud.ServiceFilter;
import kz.bejiihiu.hub.command.LobbySelectorCommand;
import kz.bejiihiu.hub.config.ConfigDefaultsFactory;
import kz.bejiihiu.hub.config.ConfigLoader;
import kz.bejiihiu.hub.config.SelectorConfig;
import kz.bejiihiu.hub.inventory.LobbyInventoryListener;
import kz.bejiihiu.hub.inventory.LobbyInventoryService;
import kz.bejiihiu.hub.inventory.PlaceholderService;
import kz.bejiihiu.hub.inventory.SelectorItemFactory;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Composition root for the forked HubSelector architecture.
 * Wires modules together and registers command/listener endpoints.
 */
public record Main(JavaPlugin plugin) {

    /**
     * Initializes config, services and Bukkit registrations.
     */
    public void enable() {
        ConfigLoader configLoader = new ConfigLoader(this.plugin, ConfigDefaultsFactory.createDefaultConfig());
        SelectorConfig selectorConfig = configLoader.load();

        NamespacedKey serviceKey = new NamespacedKey(this.plugin, SelectorConfig.KEY_SERVICE);
        CloudNetFacade cloudNetFacade = new CloudNetFacade();
        ServiceFilter serviceFilter = new ServiceFilter(selectorConfig);
        PlaceholderService placeholderService = new PlaceholderService();
        SelectorItemFactory itemFactory = new SelectorItemFactory(this.plugin, selectorConfig, placeholderService, serviceKey);
        LobbyInventoryService inventoryService = new LobbyInventoryService(
                this.plugin,
                selectorConfig,
                cloudNetFacade,
                serviceFilter,
                itemFactory
        );

        LobbyInventoryListener listener = new LobbyInventoryListener(cloudNetFacade, serviceFilter, serviceKey);
        this.plugin.getServer().getPluginManager().registerEvents(listener, this.plugin);

        PluginCommand command = this.plugin.getCommand("hub");
        if (command != null) {
            LobbySelectorCommand lobbySelectorCommand = new LobbySelectorCommand(inventoryService);
            command.setExecutor(lobbySelectorCommand);
            command.setTabCompleter(lobbySelectorCommand);
        }
    }

    /**
     * Reserved for future lifecycle-managed resources.
     */
    public void disable() {
        // No lifecycle resources to dispose right now.
    }
}
