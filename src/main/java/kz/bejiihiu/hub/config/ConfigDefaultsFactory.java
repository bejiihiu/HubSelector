package kz.bejiihiu.hub.config;

import org.bukkit.Material;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Provides default JSON config payload used on first startup.
 */
public final class ConfigDefaultsFactory {
	private ConfigDefaultsFactory() {
	}

	/**
	 * Creates top-level config with default task and item template values.
	 */
	public static JSONObject createDefaultConfig() {
		JSONObject config = new JSONObject();

		config.put(SelectorConfig.KEY_LOBBY_TASK, SelectorConfig.TASK_LOBBY_DEFAULT);
		config.put(SelectorConfig.KEY_INVENTORY_TITLE, "&lLobby Selector:");
		config.put(SelectorConfig.KEY_HIDE_FULL_SERVICES, false);
		config.put(SelectorConfig.KEY_ENABLE_SILENT_LOBBY, false);
		config.put(SelectorConfig.KEY_SILENT_LOBBY_TASK, SelectorConfig.TASK_SILENT_LOBBY_DEFAULT);

		JSONObject serverItemConfig = new JSONObject();
		serverItemConfig.put("default", createItem(Material.CHICKEN_SPAWN_EGG.name(), "&7{service} ({players}/{max_players})", false));
		serverItemConfig.put("silentHub", createItem(Material.TNT.name(), "&7{service} ({players}/{max_players})", false));
		serverItemConfig.put("full", createItem(Material.BARRIER.name(), "&c{service} ({players}/{max_players})", false));
		serverItemConfig.put("current", createItem(Material.EMERALD.name(), "&a{service} (current) ({players}/{max_players})", true));

		config.put(SelectorConfig.KEY_SERVER_ITEMS, serverItemConfig);
		config.put(SelectorConfig.KEY_FEATURES, createFeatures());
		config.put(SelectorConfig.KEY_SELECTION_POLICY, createSelectionPolicy());
		config.put(SelectorConfig.KEY_MESSAGES, createMessages());
		return config;
	}

	private static JSONObject createFeatures() {
		JSONObject features = new JSONObject();
		features.put("pagination", true);
		features.put("smartJoin", true);
		features.put("retryConnect", true);
		features.put("fallbackConnect", true);
		features.put("telemetry", true);
		features.put("debug", false);
		return features;
	}

	private static JSONObject createSelectionPolicy() {
		JSONObject selectionPolicy = new JSONObject();
		selectionPolicy.put("mode", "manual");
		selectionPolicy.put("strategy", "leastPlayers");
		return selectionPolicy;
	}

	private static JSONObject createMessages() {
		JSONObject messages = new JSONObject();
		messages.put("connecting", "&aConnecting to {service}...");
		messages.put("alreadyConnecting", "&eConnection already in progress");
		messages.put("serviceFull", "&cService is full");
		messages.put("serviceOffline", "&cService is offline");
		messages.put("retry", "&eRetrying connection...");
		messages.put("fallback", "&eSelected lobby unavailable. Trying {service}");
		messages.put("failed", "&cConnection failed. Try again");
		return messages;
	}

	/**
	 * Creates one server item template object.
	 */
	private static JSONObject createItem(String material, String name, boolean enchanted) {
		JSONObject item = new JSONObject();
		item.put(SelectorConfig.KEY_MATERIAL, material);
		item.put(SelectorConfig.KEY_NAME, name);
		item.put(SelectorConfig.KEY_LORE, new JSONArray());
		item.put(SelectorConfig.KEY_ENCHANTED, enchanted);
		item.put(SelectorConfig.KEY_CUSTOM_MODEL_DATA, -1);
		return item;
	}
}
