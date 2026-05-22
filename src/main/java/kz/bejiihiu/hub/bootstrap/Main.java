package kz.bejiihiu.hub.bootstrap;

import kz.bejiihiu.hub.cloud.CloudNetFacade;
import kz.bejiihiu.hub.cloud.ServiceFilter;
import kz.bejiihiu.hub.command.LobbySelectorCommand;
import kz.bejiihiu.hub.config.ConfigDefaultsFactory;
import kz.bejiihiu.hub.config.ConfigLoader;
import kz.bejiihiu.hub.config.SelectorConfig;
import kz.bejiihiu.hub.connection.ConnectionService;
import kz.bejiihiu.hub.inventory.LobbyInventoryListener;
import kz.bejiihiu.hub.inventory.LobbyInventoryService;
import kz.bejiihiu.hub.inventory.PlaceholderService;
import kz.bejiihiu.hub.inventory.SelectorItemFactory;
import kz.bejiihiu.hub.selection.LobbySelectionPolicy;
import kz.bejiihiu.hub.telemetry.TelemetryService;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public record Main(JavaPlugin plugin) {

	public void enable() {
		ConfigLoader configLoader = new ConfigLoader(this.plugin, ConfigDefaultsFactory.createDefaultConfig());
		AtomicReference<SelectorConfig> configRef = new AtomicReference<>(configLoader.load());
		Supplier<SelectorConfig> configSupplier = configRef::get;
		AtomicBoolean debugFlag = new AtomicBoolean(configSupplier.get().features().debug());

		NamespacedKey serviceKey = new NamespacedKey(this.plugin, SelectorConfig.KEY_SERVICE);
		NamespacedKey actionKey = new NamespacedKey(this.plugin, "action");
		CloudNetFacade cloudNetFacade = new CloudNetFacade();
		ServiceFilter serviceFilter = new ServiceFilter(configSupplier);
		PlaceholderService placeholderService = new PlaceholderService();
		SelectorItemFactory itemFactory = new SelectorItemFactory(this.plugin, configSupplier, placeholderService, serviceKey);
		LobbySelectionPolicy selectionPolicy = new LobbySelectionPolicy();
		TelemetryService telemetryService = new TelemetryService();
		LobbyInventoryService inventoryService = new LobbyInventoryService(
				this.plugin,
				configSupplier,
				cloudNetFacade,
				serviceFilter,
				itemFactory,
				selectionPolicy,
				telemetryService,
				actionKey
		);
		ConnectionService connectionService = new ConnectionService(this.plugin, cloudNetFacade, configSupplier, telemetryService);

		LobbyInventoryListener listener = new LobbyInventoryListener(
				this.plugin,
				configSupplier,
				cloudNetFacade,
				serviceFilter,
				inventoryService,
				connectionService,
				telemetryService,
				serviceKey,
				actionKey
		);
		this.plugin.getServer().getPluginManager().registerEvents(listener, this.plugin);

		Runnable reloadAction = () -> {
			SelectorConfig newConfig = configLoader.load();
			configRef.set(newConfig);
			debugFlag.set(newConfig.features().debug());
			if (debugFlag.get()) {
				this.plugin.getLogger().info("HubSelector config reloaded");
			}
		};

		PluginCommand command = this.plugin.getCommand("lobbyselector");
		if (command != null) {
			LobbySelectorCommand lobbySelectorCommand = new LobbySelectorCommand(
					this.plugin,
					inventoryService,
					telemetryService,
					reloadAction,
					configSupplier,
					debugFlag
			);
			command.setExecutor(lobbySelectorCommand);
			command.setTabCompleter(lobbySelectorCommand);
		}
	}

	public void disable() {
	}
}
