package net.jandie1505.lobbyselector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.bridge.BridgeDocProperties;
import eu.cloudnetservice.modules.bridge.player.CloudPlayer;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.modules.bridge.player.executor.PlayerExecutor;
import eu.cloudnetservice.wrapper.configuration.WrapperConfiguration;
import net.jandie1505.configmanager.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class LobbySelector extends JavaPlugin implements Listener, InventoryHolder, TabExecutor {
	private static final String KEY_SERVICE = "service";
	private static final String KEY_ENABLE_SILENT_LOBBY = "enableSilentLobby";
	private static final String KEY_SILENT_LOBBY_TASK = "silentLobbyTask";
	private static final String KEY_LOBBY_TASK = "lobbyTask";
	private static final String KEY_INVENTORY_TITLE = "inventoryTitle";
	private static final String KEY_HIDE_FULL_SERVICES = "hideFullServices";
	private static final String KEY_SERVER_ITEMS = "serverItems";
	private static final String KEY_MATERIAL = "material";
	private static final String KEY_NAME = "name";
	private static final String KEY_LORE = "lore";
	private static final String KEY_ENCHANTED = "enchanted";
	private static final String KEY_CUSTOM_MODEL_DATA = "customModelData";

	private static final String TASK_LOBBY_DEFAULT = "Lobby";
	private static final String TASK_SILENT_LOBBY_DEFAULT = "SilentLobby";
	private static final int MIN_INVENTORY_SIZE = 9;
	private static final int MAX_INVENTORY_SIZE = 54;

	private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

	private ConfigManager configManager;
	private NamespacedKey serviceKey;

	@Override
	public void onEnable() {
		this.configManager = new ConfigManager(this.getDefaultConfigValues(), false, this.getDataFolder(),
				"config.json");
		this.configManager.reloadConfig();
		this.serviceKey = new NamespacedKey(this, KEY_SERVICE);

		this.getServer().getPluginManager().registerEvents(this, this);

		PluginCommand command = this.getCommand("lobbyselector");
		if (command != null) {
			command.setExecutor(this);
			command.setTabCompleter(this);
		}
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (event.getInventory().getHolder() != this) {
			return;
		}
		if (!(event.getWhoClicked() instanceof Player)) {
			return;
		}
		event.setCancelled(true);

		ItemMeta clickedMeta = event.getCurrentItem() == null ? null : event.getCurrentItem().getItemMeta();
		if (clickedMeta == null || !clickedMeta.getPersistentDataContainer().has(this.serviceKey)) {
			return;
		}

		CloudServiceProvider cloudServiceProvider = InjectionLayer.ext().instance(CloudServiceProvider.class);
		WrapperConfiguration wrapperConfiguration = InjectionLayer.ext().instance(WrapperConfiguration.class);
		if (cloudServiceProvider == null || wrapperConfiguration == null) {
			return;
		}

		String serviceName = clickedMeta.getPersistentDataContainer().getOrDefault(this.serviceKey,
				PersistentDataType.STRING, "");
		ServiceInfoSnapshot service = cloudServiceProvider.serviceByName(serviceName);
		if (service == null) {
			return;
		}

		if (!this.isValidLobbyService(service, event.getWhoClicked().hasPermission("lobbyselector.silenthub"))) {
			return;
		}
		if (wrapperConfiguration.serviceInfoSnapshot().name().equals(service.name())) {
			return;
		}

		ServiceRegistry serviceRegistry = InjectionLayer.ext().instance(ServiceRegistry.class);
		if (serviceRegistry == null) {
			return;
		}

		PlayerManager playerManager = serviceRegistry.defaultInstance(PlayerManager.class);
		if (playerManager == null) {
			return;
		}

		CloudPlayer player = playerManager.onlinePlayer(event.getWhoClicked().getUniqueId());
		if (player == null) {
			return;
		}

		PlayerExecutor playerExecutor = playerManager.playerExecutor(player.uniqueId());
		if (playerExecutor == null) {
			return;
		}

		event.getWhoClicked().closeInventory();
		playerExecutor.connect(service.name());
	}

	@EventHandler
	public void onInventoryDrag(InventoryDragEvent event) {
		if (event.getInventory().getHolder() != this) {
			return;
		}
		event.setCancelled(true);
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label,
			@NotNull String @NotNull [] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(LEGACY_SERIALIZER.deserialize("&cCommand can only be executed by players"));
			return true;
		}
		if (!sender.hasPermission("lobbyselector.use")) {
			sender.sendMessage(LEGACY_SERIALIZER.deserialize("&cNo permission"));
			return true;
		}

		player.openInventory(this.getLobbySelector(sender.hasPermission("lobbyselector.silentlobby")));
		return true;
	}

	@Override
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label,
			@NotNull String @NotNull [] args) {
		return List.of();
	}

	@Override
	public @NotNull Inventory getInventory() {
		return this.getServer().createInventory(this, MIN_INVENTORY_SIZE, this.toComponent("&c&mLobby Selector"));
	}

	public Inventory getLobbySelector(boolean allowSilentLobby) {
		CloudServiceProvider cloudServiceProvider = InjectionLayer.ext().instance(CloudServiceProvider.class);
		WrapperConfiguration wrapperConfiguration = InjectionLayer.ext().instance(WrapperConfiguration.class);
		if (cloudServiceProvider == null || wrapperConfiguration == null) {
			return this.getInventory();
		}

		List<ServiceInfoSnapshot> services = this.collectServices(cloudServiceProvider, allowSilentLobby);
		int inventorySize = this.computeInventorySize(services.size());
		Inventory inventory = this.getServer().createInventory(
				this,
				inventorySize,
				this.toComponent(this.configManager.getConfig().optString(KEY_INVENTORY_TITLE, "Lobby Selector:")));

		List<ItemStack> selectorItems = services.stream()
				.map(service -> this.createSelectorItem(service, wrapperConfiguration))
				.filter(Objects::nonNull)
				.limit(inventorySize)
				.toList();

		for (int slot = 0; slot < selectorItems.size(); slot++) {
			inventory.setItem(slot, selectorItems.get(slot));
		}

		return inventory;
	}

	private List<ServiceInfoSnapshot> collectServices(CloudServiceProvider cloudServiceProvider,
			boolean allowSilentLobby) {
		List<ServiceInfoSnapshot> services = new ArrayList<>();

		if (allowSilentLobby && this.configManager.getConfig().optBoolean(KEY_ENABLE_SILENT_LOBBY, false)) {
			services.addAll(cloudServiceProvider.servicesByTask(
					this.configManager.getConfig().optString(KEY_SILENT_LOBBY_TASK, TASK_SILENT_LOBBY_DEFAULT)));
		}
		services.addAll(cloudServiceProvider
				.servicesByTask(this.configManager.getConfig().optString(KEY_LOBBY_TASK, TASK_LOBBY_DEFAULT)));

		Collections.reverse(services);
		return services;
	}

	private int computeInventorySize(int serviceCount) {
		int inventorySize = ((serviceCount / MIN_INVENTORY_SIZE) + 1) * MIN_INVENTORY_SIZE;
		if (inventorySize > MAX_INVENTORY_SIZE) {
			return MAX_INVENTORY_SIZE;
		}
		return Math.max(inventorySize, MIN_INVENTORY_SIZE);
	}

	private ItemStack createSelectorItem(ServiceInfoSnapshot service, WrapperConfiguration wrapperConfiguration) {
		if (!service.readProperty(BridgeDocProperties.IS_ONLINE)) {
			return null;
		}

		boolean currentService = wrapperConfiguration.serviceInfoSnapshot().name().equals(service.name());
		int players = service.readProperty(BridgeDocProperties.PLAYERS).size();
		int maxPlayers = service.readProperty(BridgeDocProperties.MAX_PLAYERS);

		if (this.configManager.getConfig().optBoolean(KEY_HIDE_FULL_SERVICES, false) && players >= maxPlayers
				&& !currentService) {
			return null;
		}

		JSONObject serverItemsConfig = this.configManager.getConfig().optJSONObject(KEY_SERVER_ITEMS, new JSONObject());
		JSONObject itemConfig = this.selectServerItemConfig(service, serverItemsConfig, currentService, players,
				maxPlayers);
		ItemStack item = this.buildSelectorItem(itemConfig, service.name(), players, maxPlayers);

		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			meta = this.getServer().getItemFactory().getItemMeta(item.getType());
		}
		if (meta == null) {
			return null;
		}

		meta.getPersistentDataContainer().set(this.serviceKey, PersistentDataType.STRING, service.name());
		item.setItemMeta(meta);
		return item;
	}

	private JSONObject selectServerItemConfig(
			ServiceInfoSnapshot service,
			JSONObject serverItemsConfig,
			boolean currentService,
			int players,
			int maxPlayers) {
		if (currentService) {
			return serverItemsConfig.optJSONObject("current", new JSONObject());
		}
		if (players >= maxPlayers) {
			return serverItemsConfig.optJSONObject("full", new JSONObject());
		}
		if (this.isSilentHubService(service)) {
			return serverItemsConfig.optJSONObject("silentHub", new JSONObject());
		}
		return serverItemsConfig.optJSONObject("default", new JSONObject());
	}

	private ItemStack buildSelectorItem(JSONObject json, String serviceName, int players, int maxPlayers) {
		Material type = Material.getMaterial(json.optString(KEY_MATERIAL, ""));
		if (type == null) {
			ItemStack errorItem = new ItemStack(Material.BARRIER);
			ItemMeta errorMeta = this.getServer().getItemFactory().getItemMeta(errorItem.getType());
			if (errorMeta != null) {
				errorMeta.displayName(Component.text("INVALID ITEM CONFIGURATION"));
				errorItem.setItemMeta(errorMeta);
			}
			return errorItem;
		}

		ItemStack itemStack = new ItemStack(type);
		ItemMeta itemMeta = this.getServer().getItemFactory().getItemMeta(itemStack.getType());
		if (itemMeta == null) {
			return itemStack;
		}

		if (json.has(KEY_NAME)) {
			String name = this.handlePlaceholders(json.optString(KEY_NAME, "INVALID STRING VALUE"), serviceName,
					players, maxPlayers);
			itemMeta.displayName(this.toComponent(name));
		}

		if (json.has(KEY_LORE)) {
			List<Component> lore = new ArrayList<>();
			for (Object value : json.optJSONArray(KEY_LORE, new JSONArray())) {
				if (value instanceof String stringValue) {
					lore.add(this.toComponent(this.handlePlaceholders(stringValue, serviceName, players, maxPlayers)));
				} else {
					lore.add(Component.text("INVALID STRING VALUE"));
				}
			}
			itemMeta.lore(lore);
		}

		if (json.optBoolean(KEY_ENCHANTED, false)) {
			itemMeta.addEnchant(Enchantment.FORTUNE, 1, true);
			itemMeta.addItemFlags(ItemFlag.values());
		}

		if (json.optInt(KEY_CUSTOM_MODEL_DATA, -1) >= 0) {
			itemMeta.setCustomModelData(json.optInt(KEY_CUSTOM_MODEL_DATA, 0));
		}

		itemStack.setItemMeta(itemMeta);
		return itemStack.clone();
	}

	private String handlePlaceholders(String string, String serviceName, int players, int maxPlayers) {
		string = string.replace("{service}", serviceName);
		string = string.replace("{players}", String.valueOf(players));
		string = string.replace("{max_players}", String.valueOf(maxPlayers));
		return string;
	}

	private Component toComponent(String text) {
		return LEGACY_SERIALIZER.deserialize(text);
	}

	private boolean isValidLobbyService(ServiceInfoSnapshot service, boolean enableSilentHub) {
		String lobbyTask = this.configManager.getConfig().optString(KEY_LOBBY_TASK, TASK_LOBBY_DEFAULT);
		if (service.serviceId().taskName().equals(lobbyTask)) {
			return true;
		}
		return enableSilentHub && this.isSilentHubService(service);
	}

	private boolean isSilentHubService(ServiceInfoSnapshot service) {
		String silentLobbyTask = this.configManager.getConfig().optString(KEY_SILENT_LOBBY_TASK,
				TASK_SILENT_LOBBY_DEFAULT);
		return this.configManager.getConfig().optBoolean(KEY_ENABLE_SILENT_LOBBY, false)
				&& service.serviceId().taskName().equals(silentLobbyTask);
	}

	private JSONObject getDefaultConfigValues() {
		JSONObject config = new JSONObject();

		config.put(KEY_LOBBY_TASK, TASK_LOBBY_DEFAULT);
		config.put(KEY_INVENTORY_TITLE, "&lLobby Selector:");
		config.put(KEY_HIDE_FULL_SERVICES, false);
		config.put(KEY_ENABLE_SILENT_LOBBY, false);
		config.put(KEY_SILENT_LOBBY_TASK, TASK_SILENT_LOBBY_DEFAULT);

		JSONObject serverItemConfig = new JSONObject();

		JSONObject defItemConfig = new JSONObject();
		defItemConfig.put(KEY_MATERIAL, Material.CHICKEN_SPAWN_EGG.name());
		defItemConfig.put(KEY_NAME, "&7{service} ({players}/{max_players})");
		defItemConfig.put(KEY_LORE, new JSONArray());
		defItemConfig.put(KEY_ENCHANTED, false);
		defItemConfig.put(KEY_CUSTOM_MODEL_DATA, -1);
		serverItemConfig.put("default", defItemConfig);

		JSONObject silentHubConfig = new JSONObject();
		silentHubConfig.put(KEY_MATERIAL, Material.TNT.name());
		silentHubConfig.put(KEY_NAME, "&7{service} ({players}/{max_players})");
		silentHubConfig.put(KEY_LORE, new JSONArray());
		silentHubConfig.put(KEY_ENCHANTED, false);
		silentHubConfig.put(KEY_CUSTOM_MODEL_DATA, -1);
		serverItemConfig.put("silentHub", silentHubConfig);

		JSONObject fullItemConfig = new JSONObject();
		fullItemConfig.put(KEY_MATERIAL, Material.BARRIER.name());
		fullItemConfig.put(KEY_NAME, "&c{service} ({players}/{max_players})");
		fullItemConfig.put(KEY_LORE, new JSONArray());
		fullItemConfig.put(KEY_ENCHANTED, false);
		fullItemConfig.put(KEY_CUSTOM_MODEL_DATA, -1);
		serverItemConfig.put("full", fullItemConfig);

		JSONObject currentItemConfig = new JSONObject();
		currentItemConfig.put(KEY_MATERIAL, Material.EMERALD.name());
		currentItemConfig.put(KEY_NAME, "&a{service} (current) ({players}/{max_players})");
		currentItemConfig.put(KEY_LORE, new JSONArray());
		currentItemConfig.put(KEY_ENCHANTED, true);
		currentItemConfig.put(KEY_CUSTOM_MODEL_DATA, -1);
		serverItemConfig.put("current", currentItemConfig);

		config.put(KEY_SERVER_ITEMS, serverItemConfig);
		return config;
	}
}
